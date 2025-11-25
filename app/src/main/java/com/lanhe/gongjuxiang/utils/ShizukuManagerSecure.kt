package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.widget.Toast
import android.app.AlertDialog
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.SystemServiceHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.lanhe.gongjuxiang.LanheApplication
import com.lanhe.gongjuxiang.security.CommandValidator
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * 安全增强版Shizuku管理器
 * 集成命令验证、超时控制和审计日志
 */
object ShizukuManagerSecure {

    private const val TAG = "ShizukuManagerSecure"

    // 命令验证器
    private val commandValidator = CommandValidator()

    // Shizuku状态
    private val _shizukuState = MutableStateFlow<ShizukuState>(ShizukuState.Unavailable)
    val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

    // 系统服务管理器
    private var systemServicesAvailable = false

    /**
     * 执行系统命令（带安全验证和超时控制）
     * @param command 要执行的命令
     * @param timeoutMs 超时时间（毫秒），默认30秒
     * @return 命令执行结果
     */
    fun executeCommand(command: String, timeoutMs: Long = 30000L): CommandResult {
        val startTime = System.currentTimeMillis()

        try {
            // 1. 验证命令安全性
            if (!commandValidator.validateCommand(command)) {
                val error = "命令被安全策略拒绝: $command"
                Log.e(TAG, error)
                commandValidator.auditCommandExecution(command, false, 0)
                return CommandResult(false, null, error)
            }

            // 2. 检查Shizuku可用性
            if (!isShizukuAvailable()) {
                val error = "Shizuku服务不可用"
                Log.w(TAG, "$error - 命令: $command")
                commandValidator.auditCommandExecution(command, false, 0)
                return CommandResult(false, null, error)
            }

            // 3. 验证超时时间
            val validTimeout = commandValidator.validateTimeout(timeoutMs)

            // 4. 执行命令（带超时）
            val result = runBlocking {
                withTimeoutOrNull(validTimeout) {
                    executeCommandInternal(command)
                }
            }

            if (result == null) {
                val error = "命令执行超时 (${validTimeout}ms): $command"
                Log.e(TAG, error)
                commandValidator.auditCommandExecution(command, false, validTimeout)
                return CommandResult(false, null, error)
            }

            // 5. 记录审计日志
            val executionTime = System.currentTimeMillis() - startTime
            commandValidator.auditCommandExecution(command, result.isSuccess, executionTime)

            return result

        } catch (e: Exception) {
            val error = "命令执行异常: ${e.message}"
            Log.e(TAG, error, e)
            val executionTime = System.currentTimeMillis() - startTime
            commandValidator.auditCommandExecution(command, false, executionTime)
            return CommandResult(false, null, error)
        }
    }

    /**
     * 执行Shell命令（简化版，返回输出字符串）
     * @param command 要执行的命令
     * @param timeoutMs 超时时间（毫秒）
     * @return 命令输出字符串
     */
    fun executeShellCommand(command: String, timeoutMs: Long = 30000L): String {
        val result = executeCommand(command, timeoutMs)
        return result.output ?: ""
    }

    /**
     * 安装应用（带路径验证）
     * @param apkPath APK文件路径
     * @return 是否安装成功
     */
    suspend fun installPackage(apkPath: String): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            // 1. 验证文件路径安全性
            if (!commandValidator.validateFilePath(apkPath)) {
                Log.e(TAG, "APK路径验证失败: $apkPath")
                showToastSafely("APK文件路径不安全")
                return@withContext false
            }

            // 2. 检查文件是否存在
            val apkFile = File(apkPath)
            if (!apkFile.exists()) {
                Log.e(TAG, "APK文件不存在: $apkPath")
                showToastSafely("APK文件不存在")
                return@withContext false
            }

            // 3. 检查文件是否是APK
            if (!apkPath.endsWith(".apk", ignoreCase = true)) {
                Log.e(TAG, "文件不是APK格式: $apkPath")
                showToastSafely("文件不是APK格式")
                return@withContext false
            }

            // 4. 使用pm install命令安装
            val command = "pm install -r \"$apkPath\""
            val result = executeCommand(command, 60000L) // 安装可能需要更长时间

            if (result.isSuccess) {
                Log.i(TAG, "应用安装成功: $apkPath")
                showToastSafely("应用安装成功")
            } else {
                Log.e(TAG, "应用安装失败: ${result.error}")
                showToastSafely("应用安装失败: ${result.error}")
            }

            return@withContext result.isSuccess

        } catch (e: Exception) {
            Log.e(TAG, "安装应用异常", e)
            showToastSafely("安装失败: ${e.message}")
            return@withContext false
        }
    }

    /**
     * 卸载应用（带包名验证和用户确认）
     * @param context Context对象，用于显示对话框
     * @param packageName 要卸载的包名
     * @param onResult 卸载结果回调
     */
    fun uninstallPackage(context: Context, packageName: String, onResult: (Boolean) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                // 1. 验证包名安全性
                if (!commandValidator.validatePackageName(packageName)) {
                    // 如果是系统应用，显示警告对话框
                    if (isSystemApp(packageName)) {
                        showSystemAppWarningDialog(context, packageName) { confirmed ->
                            if (confirmed) {
                                GlobalScope.launch {
                                    performUninstall(packageName, onResult)
                                }
                            } else {
                                onResult(false)
                            }
                        }
                    } else {
                        Log.e(TAG, "包名验证失败: $packageName")
                        showToastSafely("无法卸载此应用")
                        onResult(false)
                    }
                    return@launch
                }

                // 2. 普通应用直接卸载
                performUninstall(packageName, onResult)

            } catch (e: Exception) {
                Log.e(TAG, "卸载应用异常", e)
                showToastSafely("卸载失败: ${e.message}")
                onResult(false)
            }
        }
    }

    /**
     * 执行卸载操作
     */
    private suspend fun performUninstall(packageName: String, onResult: (Boolean) -> Unit) = withContext(Dispatchers.IO) {
        val command = "pm uninstall $packageName"
        val result = executeCommand(command, 30000L)

        withContext(Dispatchers.Main) {
            if (result.isSuccess) {
                Log.i(TAG, "应用卸载成功: $packageName")
                showToastSafely("应用卸载成功")
                onResult(true)
            } else {
                Log.e(TAG, "应用卸载失败: ${result.error}")
                showToastSafely("应用卸载失败: ${result.error}")
                onResult(false)
            }
        }
    }

    /**
     * 加速应用（带包名验证）
     * @param packageName 要加速的包名
     * @return 是否加速成功
     */
    fun accelerateApp(packageName: String): Boolean {
        if (!systemServicesAvailable) {
            Log.w(TAG, "系统服务不可用")
            return false
        }

        return try {
            // 1. 验证包名格式
            if (!isValidPackageName(packageName)) {
                Log.e(TAG, "无效的包名格式: $packageName")
                return false
            }

            // 2. 检查是否在黑名单中
            if (!commandValidator.validatePackageName(packageName)) {
                Log.w(TAG, "应用在保护列表中，跳过加速: $packageName")
                return false
            }

            // 3. 检查应用是否已安装
            if (!isAppInstalled(packageName)) {
                Log.w(TAG, "应用未安装: $packageName")
                return false
            }

            // 4. 执行加速操作
            Log.i(TAG, "正在加速应用: $packageName")

            // 设置应用为性能模式
            val commands = listOf(
                "cmd activity set-standby-bucket $packageName active",
                "cmd deviceidle whitelist +$packageName",
                "settings put global fstrim_mandatory_interval 1"
            )

            var success = true
            for (cmd in commands) {
                val result = executeCommand(cmd, 5000L)
                if (!result.isSuccess) {
                    Log.w(TAG, "加速命令执行失败: $cmd")
                    success = false
                }
            }

            success

        } catch (e: Exception) {
            Log.e(TAG, "加速应用失败", e)
            false
        }
    }

    /**
     * 强制停止应用（带包名验证）
     * @param packageName 要停止的包名
     * @return 是否成功停止
     */
    fun forceStopPackage(packageName: String): Boolean {
        return try {
            // 1. 验证包名
            if (!commandValidator.validatePackageName(packageName)) {
                Log.e(TAG, "包名验证失败，无法强制停止: $packageName")
                return false
            }

            // 2. 执行强制停止
            val command = "am force-stop $packageName"
            val result = executeCommand(command, 5000L)

            if (result.isSuccess) {
                Log.i(TAG, "成功强制停止应用: $packageName")
            } else {
                Log.e(TAG, "强制停止失败: ${result.error}")
            }

            result.isSuccess

        } catch (e: Exception) {
            Log.e(TAG, "强制停止应用异常", e)
            false
        }
    }

    /**
     * 清理应用数据（带包名验证和用户确认）
     * @param context Context对象
     * @param packageName 包名
     * @param onResult 结果回调
     */
    fun clearApplicationData(context: Context, packageName: String, onResult: (Boolean) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                // 1. 验证包名
                if (!commandValidator.validatePackageName(packageName)) {
                    showToastSafely("无法清理此应用数据")
                    onResult(false)
                    return@launch
                }

                // 2. 显示确认对话框
                AlertDialog.Builder(context)
                    .setTitle("清理应用数据")
                    .setMessage("确定要清理 $packageName 的所有数据吗？\n此操作不可恢复！")
                    .setPositiveButton("确定") { _, _ ->
                        GlobalScope.launch(Dispatchers.IO) {
                            val command = "pm clear $packageName"
                            val result = executeCommand(command, 10000L)

                            withContext(Dispatchers.Main) {
                                if (result.isSuccess) {
                                    showToastSafely("应用数据已清理")
                                    onResult(true)
                                } else {
                                    showToastSafely("清理失败: ${result.error}")
                                    onResult(false)
                                }
                            }
                        }
                    }
                    .setNegativeButton("取消") { _, _ ->
                        onResult(false)
                    }
                    .show()

            } catch (e: Exception) {
                Log.e(TAG, "清理应用数据异常", e)
                showToastSafely("清理失败: ${e.message}")
                onResult(false)
            }
        }
    }

    /**
     * 授予运行时权限（带验证）
     * @param packageName 包名
     * @param permission 权限名称
     * @return 是否成功授予
     */
    fun grantRuntimePermission(packageName: String, permission: String): Boolean {
        return try {
            // 1. 验证包名
            if (!commandValidator.validatePackageName(packageName)) {
                Log.e(TAG, "包名验证失败: $packageName")
                return false
            }

            // 2. 验证权限名称格式
            if (!isValidPermission(permission)) {
                Log.e(TAG, "无效的权限名称: $permission")
                return false
            }

            // 3. 执行授权
            val command = "pm grant $packageName $permission"
            val result = executeCommand(command, 5000L)

            if (result.isSuccess) {
                Log.i(TAG, "成功授予权限: $packageName - $permission")
            } else {
                Log.e(TAG, "授权失败: ${result.error}")
            }

            result.isSuccess

        } catch (e: Exception) {
            Log.e(TAG, "授予权限异常", e)
            false
        }
    }

    // ========================================
    // 辅助方法
    // ========================================

    /**
     * 内部命令执行方法
     */
    private suspend fun executeCommandInternal(command: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val process = Shizuku.newProcess(
                arrayOf("sh", "-c", command),
                null,
                null
            )

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            CommandResult(exitCode == 0, output, error)

        } catch (e: Exception) {
            CommandResult(false, null, e.message)
        }
    }

    /**
     * 显示系统应用警告对话框
     */
    private fun showSystemAppWarningDialog(
        context: Context,
        packageName: String,
        onResult: (Boolean) -> Unit
    ) {
        val message = commandValidator.generateWarningMessage(packageName)

        AlertDialog.Builder(context)
            .setTitle("⚠️ 系统应用警告")
            .setMessage(message)
            .setPositiveButton("继续") { _, _ ->
                onResult(true)
            }
            .setNegativeButton("取消") { _, _ ->
                onResult(false)
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 检查是否是系统应用
     */
    private fun isSystemApp(packageName: String): Boolean {
        return packageName.startsWith("android") ||
               packageName.startsWith("com.android.") ||
               packageName.startsWith("com.google.android.")
    }

    /**
     * 验证包名格式
     */
    private fun isValidPackageName(packageName: String): Boolean {
        val pattern = "^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*$"
        return packageName.matches(Regex(pattern))
    }

    /**
     * 验证权限名称格式
     */
    private fun isValidPermission(permission: String): Boolean {
        return permission.startsWith("android.permission.") ||
               permission.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*\\.permission\\.[A-Z_]+$"))
    }

    /**
     * 检查应用是否已安装
     */
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            val command = "pm list packages | grep $packageName"
            val result = executeCommand(command, 5000L)
            result.isSuccess && result.output?.contains(packageName) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 安全地显示Toast
     */
    private fun showToastSafely(message: String) {
        try {
            val context = LanheApplication.getContext()
            if (context != null) {
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "显示Toast失败", e)
        }
    }

    /**
     * 检查Shizuku是否可用
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 命令执行结果数据类
     */
    data class CommandResult(
        val isSuccess: Boolean,
        val output: String?,
        val error: String?
    )
}