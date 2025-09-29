package com.lanhe.gongjuxiang.shizuku

import android.os.IBinder
import android.os.IInterface
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

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
 * Shizuku服务实现
 */
class ShizukuServiceImpl : IShizukuService {

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
            e.printStackTrace()
        }
    }

    override fun getActivityManager(): Any? = activityManager

    override fun getPackageManager(): Any? = packageManager

    override fun executeCommand(command: String): ShellResult {
        return try {
            val process = rikka.shizuku.Shizuku.newProcess(
                arrayOf("sh", "-c", command),
                null,
                null
            )

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            ShellResult(exitCode, output, error)
        } catch (e: Exception) {
            ShellResult(-1, "", e.message ?: "Unknown error", false)
        }
    }

    override fun forceStopPackage(packageName: String): Boolean {
        return try {
            // 使用Shell命令来强制停止应用
            executeCommand("am force-stop $packageName").success
        } catch (e: Exception) {
            false
        }
    }

    override fun clearApplicationData(packageName: String): Boolean {
        return try {
            executeCommand("pm clear $packageName").success
        } catch (e: Exception) {
            false
        }
    }

    override fun grantRuntimePermission(packageName: String, permission: String): Boolean {
        return try {
            // 使用Shell命令来授予权限
            executeCommand("pm grant $packageName $permission").success
        } catch (e: Exception) {
            false
        }
    }

    override fun setComponentEnabled(packageName: String, componentName: String, enabled: Boolean): Boolean {
        return try {
            val command = if (enabled) {
                "pm enable $packageName/$componentName"
            } else {
                "pm disable $packageName/$componentName"
            }
            executeCommand(command).success
        } catch (e: Exception) {
            false
        }
    }

    override fun asBinder(): IBinder? = null
}