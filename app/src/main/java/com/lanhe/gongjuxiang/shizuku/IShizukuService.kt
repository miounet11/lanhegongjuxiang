package com.lanhe.gongjuxiang.shizuku

import android.os.IBinder
import android.os.IInterface
import android.util.Log
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import com.lanhe.gongjuxiang.security.CommandValidator
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * Shizuku服务接口
 * 提供与系统服务的交互能力
 */
interface IShizukuService : IInterface {

    /**
     * 获取ActivityManager服务
     */
    fun getActivityManager(): Any?

    /**
     * 获取PackageManager服务
     */
    fun getPackageManager(): Any?

    /**
     * 执行Shell命令
     */
    fun executeCommand(command: String): ShellResult

    /**
     * 强制停止应用
     */
    fun forceStopPackage(packageName: String): Boolean

    /**
     * 清理应用数据
     */
    fun clearApplicationData(packageName: String): Boolean

    /**
     * 设置应用权限
     */
    fun grantRuntimePermission(packageName: String, permission: String): Boolean

    /**
     * 禁用应用组件
     */
    fun setComponentEnabled(packageName: String, componentName: String, enabled: Boolean): Boolean
}

/**
 * Shell执行结果
 */
data class ShellResult(
    val exitCode: Int,
    val output: String,
    val error: String,
    val success: Boolean = exitCode == 0
)

/**
 * Shizuku服务实现（安全增强版）
 */
class ShizukuServiceImpl : IShizukuService {

    private val TAG = "ShizukuServiceImpl"
    private val commandValidator = CommandValidator()

    private var activityManager: Any? = null
    private var packageManager: Any? = null

    init {
        initializeSystemServices()
    }

    private fun initializeSystemServices() {
        try {
            // 获取ActivityManager
            val amBinder = SystemServiceHelper.getSystemService("activity")
            if (amBinder != null) {
                // 保存原始Binder对象，避免直接使用隐藏API
                activityManager = ShizukuBinderWrapper(amBinder)
            }

            // 获取PackageManager
            val pmBinder = SystemServiceHelper.getSystemService("package")
            if (pmBinder != null) {
                // 保存原始Binder对象
                packageManager = ShizukuBinderWrapper(pmBinder)
            }
        } catch (e: Exception) {
            Log.e(TAG, "初始化系统服务失败", e)
        }
    }

    override fun getActivityManager(): Any? = activityManager

    override fun getPackageManager(): Any? = packageManager

    override fun executeCommand(command: String): ShellResult {
        val startTime = System.currentTimeMillis()

        // 1. 验证命令安全性
        if (!commandValidator.validateCommand(command)) {
            val error = "命令被安全策略拒绝: $command"
            Log.e(TAG, error)
            commandValidator.auditCommandExecution(command, false, 0)
            return ShellResult(-1, "", error, false)
        }

        return try {
            // 2. 验证超时时间（30秒）
            val timeout = commandValidator.validateTimeout(30000L)

            // 3. 执行命令（带超时控制）
            val result = runBlocking {
                withTimeoutOrNull(timeout) {
                    executeCommandInternal(command)
                }
            }

            if (result == null) {
                val error = "命令执行超时: $command"
                Log.e(TAG, error)
                commandValidator.auditCommandExecution(command, false, timeout)
                return ShellResult(-1, "", error, false)
            }

            // 4. 记录审计日志
            val executionTime = System.currentTimeMillis() - startTime
            commandValidator.auditCommandExecution(command, result.success, executionTime)

            result

        } catch (e: Exception) {
            val error = "命令执行异常: ${e.message}"
            Log.e(TAG, error, e)
            val executionTime = System.currentTimeMillis() - startTime
            commandValidator.auditCommandExecution(command, false, executionTime)
            ShellResult(-1, "", error, false)
        }
    }

    /**
     * 内部命令执行方法
     */
    private suspend fun executeCommandInternal(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = rikka.shizuku.Shizuku.newProcess(
                arrayOf("sh", "-c", command),
                null,
                null
            )

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            ShellResult(exitCode, output, error, exitCode == 0)
        } catch (e: Exception) {
            ShellResult(-1, "", e.message ?: "Unknown error", false)
        }
    }

    override fun forceStopPackage(packageName: String): Boolean {
        return try {
            // 验证包名
            if (!commandValidator.validatePackageName(packageName)) {
                Log.e(TAG, "包名验证失败，无法强制停止: $packageName")
                return false
            }

            // 使用Shell命令来强制停止应用
            val result = executeCommand("am force-stop $packageName")
            if (result.success) {
                Log.i(TAG, "成功强制停止应用: $packageName")
            } else {
                Log.e(TAG, "强制停止失败: ${result.error}")
            }
            result.success
        } catch (e: Exception) {
            Log.e(TAG, "强制停止应用异常", e)
            false
        }
    }

    override fun clearApplicationData(packageName: String): Boolean {
        return try {
            // 验证包名
            if (!commandValidator.validatePackageName(packageName)) {
                Log.e(TAG, "包名验证失败，无法清理数据: $packageName")
                return false
            }

            val result = executeCommand("pm clear $packageName")
            if (result.success) {
                Log.i(TAG, "成功清理应用数据: $packageName")
            } else {
                Log.e(TAG, "清理数据失败: ${result.error}")
            }
            result.success
        } catch (e: Exception) {
            Log.e(TAG, "清理应用数据异常", e)
            false
        }
    }

    override fun grantRuntimePermission(packageName: String, permission: String): Boolean {
        return try {
            // 验证包名
            if (!commandValidator.validatePackageName(packageName)) {
                Log.e(TAG, "包名验证失败: $packageName")
                return false
            }

            // 验证权限格式
            if (!isValidPermission(permission)) {
                Log.e(TAG, "无效的权限名称: $permission")
                return false
            }

            // 使用Shell命令来授予权限
            val result = executeCommand("pm grant $packageName $permission")
            if (result.success) {
                Log.i(TAG, "成功授予权限: $packageName - $permission")
            } else {
                Log.e(TAG, "授权失败: ${result.error}")
            }
            result.success
        } catch (e: Exception) {
            Log.e(TAG, "授予权限异常", e)
            false
        }
    }

    override fun setComponentEnabled(packageName: String, componentName: String, enabled: Boolean): Boolean {
        return try {
            // 验证包名
            if (!commandValidator.validatePackageName(packageName)) {
                Log.e(TAG, "包名验证失败: $packageName")
                return false
            }

            val command = if (enabled) {
                "pm enable $packageName/$componentName"
            } else {
                "pm disable $packageName/$componentName"
            }

            val result = executeCommand(command)
            if (result.success) {
                Log.i(TAG, "成功${if (enabled) "启用" else "禁用"}组件: $packageName/$componentName")
            } else {
                Log.e(TAG, "组件操作失败: ${result.error}")
            }
            result.success
        } catch (e: Exception) {
            Log.e(TAG, "设置组件状态异常", e)
            false
        }
    }

    /**
     * 验证权限名称格式
     */
    private fun isValidPermission(permission: String): Boolean {
        return permission.startsWith("android.permission.") ||
               permission.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*\\.permission\\.[A-Z_]+$"))
    }

    override fun asBinder(): IBinder? = null
}