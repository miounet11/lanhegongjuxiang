package com.lanhe.gongjuxiang.shizuku

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku管理器真实实现
 * 提供完整的系统级权限操作功能
 */
class ShizukuManagerImpl(private val context: Context) {

    companion object {
        private const val TAG = "ShizukuManagerImpl"
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 1001
    }

    // Shizuku服务实例
    private var shizukuService: ShizukuServiceImpl? = null

    // Shizuku状态
    private val _shizukuState = MutableStateFlow(ShizukuState.Unknown)
    val shizukuState: StateFlow<ShizukuState> = _shizukuState

    // 权限监听器
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            _shizukuState.value = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                ShizukuState.Granted
            } else {
                ShizukuState.Denied
            }
        }
    }

    // Binder状态监听器
    private val binderReceivedListener = {
        updateShizukuState()
        if (_shizukuState.value == ShizukuState.Granted) {
            initializeService()
        }
    }

    private val binderDeadListener = {
        _shizukuState.value = ShizukuState.Unavailable
        shizukuService = null
    }

    init {
        // 注册监听器
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)

        // 检查初始状态
        updateShizukuState()
        if (_shizukuState.value == ShizukuState.Granted) {
            initializeService()
        }
    }

    /**
     * 更新Shizuku状态
     */
    private fun updateShizukuState() {
        _shizukuState.value = when {
            !isShizukuInstalled() -> ShizukuState.NotInstalled
            !Shizuku.pingBinder() -> ShizukuState.Unavailable
            !checkPermission() -> ShizukuState.Denied
            else -> ShizukuState.Granted
        }
    }

    /**
     * 检查Shizuku是否安装
     */
    private fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 检查权限
     */
    private fun checkPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            // 旧版本兼容性处理
            false
        }
    }

    /**
     * 初始化服务
     */
    private fun initializeService() {
        try {
            shizukuService = ShizukuServiceImpl()
            Log.i(TAG, "Shizuku service initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Shizuku service", e)
            shizukuService = null
        }
    }

    /**
     * 请求权限
     */
    fun requestPermission() {
        when (_shizukuState.value) {
            ShizukuState.NotInstalled -> {
                Log.w(TAG, "Shizuku not installed")
                // 可以引导用户安装
            }
            ShizukuState.Unavailable -> {
                Log.w(TAG, "Shizuku service not running")
                // 可以引导用户启动Shizuku
            }
            ShizukuState.Denied -> {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    // 显示权限说明
                    Log.i(TAG, "Requesting Shizuku permission")
                }
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            }
            ShizukuState.Granted -> {
                Log.i(TAG, "Permission already granted")
            }
            else -> {}
        }
    }

    /**
     * 检查是否可用
     */
    fun isAvailable(): Boolean = _shizukuState.value == ShizukuState.Granted && shizukuService != null

    // ==================== 进程管理功能 ====================

    /**
     * 获取运行中的进程列表
     */
    @SuppressLint("DiscouragedPrivateApi")
    suspend fun getRunningProcesses(): List<ProcessInfo> = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext emptyList()

        try {
            val result = shizukuService?.executeCommand("ps -A -o PID,NAME,USER,RSS")
                ?: return@withContext emptyList()

            if (!result.success) return@withContext emptyList()

            val processes = mutableListOf<ProcessInfo>()
            val lines = result.output.split("\n")

            // 跳过标题行
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue

                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 4) {
                    try {
                        val pid = parts[0].toInt()
                        val name = parts[1]
                        val user = parts[2]
                        val rss = parts[3].toLongOrNull() ?: 0L

                        processes.add(ProcessInfo(
                            pid = pid,
                            name = name,
                            packageName = extractPackageName(name),
                            uid = getUserId(user),
                            memoryUsage = rss * 1024 // RSS is in KB, convert to bytes
                        ))
                    } catch (e: Exception) {
                        // Skip malformed lines
                    }
                }
            }

            processes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get running processes", e)
            emptyList()
        }
    }

    /**
     * 强制停止进程
     */
    suspend fun killProcess(pid: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext false

        try {
            val result = shizukuService?.executeCommand("kill -9 $pid")
            result?.success ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to kill process $pid", e)
            false
        }
    }

    /**
     * 强制停止应用
     */
    suspend fun forceStopPackage(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext false

        try {
            shizukuService?.forceStopPackage(packageName) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force stop package $packageName", e)
            false
        }
    }

    // ==================== 系统性能优化功能 ====================

    /**
     * 获取CPU信息
     */
    suspend fun getCpuInfo(): CpuInfo = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext CpuInfo()

        try {
            val result = shizukuService?.executeCommand("cat /proc/cpuinfo")
                ?: return@withContext CpuInfo()

            if (!result.success) return@withContext CpuInfo()

            val lines = result.output.split("\n")
            var cores = 0
            var model = ""
            var frequency = 0f

            for (line in lines) {
                when {
                    line.startsWith("processor") -> cores++
                    line.startsWith("model name") -> {
                        model = line.substringAfter(":").trim()
                    }
                    line.startsWith("cpu MHz") -> {
                        frequency = line.substringAfter(":").trim().toFloatOrNull() ?: 0f
                    }
                }
            }

            // 获取当前频率
            val freqResult = shizukuService?.executeCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
            val currentFreq = if (freqResult?.success == true) {
                (freqResult.output.trim().toLongOrNull() ?: 0L) / 1000f // Convert from KHz to MHz
            } else {
                frequency
            }

            CpuInfo(
                cores = cores,
                model = model,
                currentFrequency = currentFreq,
                maxFrequency = getMaxCpuFrequency(),
                minFrequency = getMinCpuFrequency(),
                governor = getCpuGovernor()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get CPU info", e)
            CpuInfo()
        }
    }

    /**
     * 设置CPU调速器
     */
    suspend fun setCpuGovernor(governor: String): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext false

        try {
            val cpuCount = Runtime.getRuntime().availableProcessors()
            var success = true

            for (i in 0 until cpuCount) {
                val result = shizukuService?.executeCommand(
                    "echo $governor > /sys/devices/system/cpu/cpu$i/cpufreq/scaling_governor"
                )
                if (result?.success != true) {
                    success = false
                }
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set CPU governor", e)
            false
        }
    }

    /**
     * 清理内存
     */
    suspend fun cleanMemory(): MemoryCleanResult = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext MemoryCleanResult(false, 0L)

        try {
            // 获取清理前的内存
            val beforeMem = getAvailableMemory()

            // 执行内存清理
            shizukuService?.executeCommand("echo 3 > /proc/sys/vm/drop_caches")

            // 杀死不必要的后台进程
            val processes = getRunningProcesses()
            val killableProcesses = processes.filter { isKillable(it.packageName) }

            for (process in killableProcesses) {
                killProcess(process.pid)
            }

            // 获取清理后的内存
            val afterMem = getAvailableMemory()
            val freedMemory = afterMem - beforeMem

            MemoryCleanResult(true, freedMemory)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean memory", e)
            MemoryCleanResult(false, 0L)
        }
    }

    // ==================== 应用管理功能 ====================

    /**
     * 获取已安装应用列表
     */
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext emptyList()

        try {
            val result = shizukuService?.executeCommand("pm list packages -f")
                ?: return@withContext emptyList()

            if (!result.success) return@withContext emptyList()

            val apps = mutableListOf<AppInfo>()
            val lines = result.output.split("\n")

            for (line in lines) {
                if (line.startsWith("package:")) {
                    val parts = line.substring(8).split("=")
                    if (parts.size == 2) {
                        val path = parts[0]
                        val packageName = parts[1]

                        try {
                            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                            apps.add(AppInfo(
                                packageName = packageName,
                                name = context.packageManager.getApplicationLabel(appInfo).toString(),
                                path = path,
                                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                                isEnabled = appInfo.enabled,
                                uid = appInfo.uid
                            ))
                        } catch (e: Exception) {
                            // Skip if cannot get app info
                        }
                    }
                }
            }

            apps
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get installed apps", e)
            emptyList()
        }
    }

    /**
     * 禁用/启用应用
     */
    suspend fun setAppEnabled(packageName: String, enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext false

        try {
            val command = if (enabled) {
                "pm enable $packageName"
            } else {
                "pm disable-user $packageName"
            }

            val result = shizukuService?.executeCommand(command)
            result?.success ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set app enabled state", e)
            false
        }
    }

    /**
     * 清理应用数据
     */
    suspend fun clearAppData(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext false

        try {
            shizukuService?.clearApplicationData(packageName) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear app data", e)
            false
        }
    }

    /**
     * 授予运行时权限
     */
    suspend fun grantPermission(packageName: String, permission: String): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext false

        try {
            shizukuService?.grantRuntimePermission(packageName, permission) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to grant permission", e)
            false
        }
    }

    // ==================== 系统设置修改功能 ====================

    /**
     * 修改系统设置
     */
    suspend fun setSystemSetting(namespace: String, key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext false

        try {
            val command = "settings put $namespace $key $value"
            val result = shizukuService?.executeCommand(command)
            result?.success ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set system setting", e)
            false
        }
    }

    /**
     * 获取系统设置
     */
    suspend fun getSystemSetting(namespace: String, key: String): String? = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext null

        try {
            val command = "settings get $namespace $key"
            val result = shizukuService?.executeCommand(command)
            if (result?.success == true) {
                result.output.trim()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get system setting", e)
            null
        }
    }

    /**
     * 修改系统属性
     */
    suspend fun setSystemProperty(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable()) return@withContext false

        try {
            val command = "setprop $key $value"
            val result = shizukuService?.executeCommand(command)
            result?.success ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set system property", e)
            false
        }
    }

    // ==================== 辅助方法 ====================

    private fun extractPackageName(processName: String): String {
        // 尝试从进程名提取包名
        return when {
            processName.contains(":") -> processName.substringBefore(":")
            processName.startsWith("com.") || processName.startsWith("org.") -> processName
            else -> processName
        }
    }

    private fun getUserId(user: String): Int {
        return try {
            if (user.startsWith("u") && user.contains("_")) {
                user.substringAfter("_").substringBefore("_").toInt()
            } else {
                user.toIntOrNull() ?: -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    private suspend fun getMaxCpuFrequency(): Float {
        val result = shizukuService?.executeCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq")
        return if (result?.success == true) {
            (result.output.trim().toLongOrNull() ?: 0L) / 1000f
        } else {
            0f
        }
    }

    private suspend fun getMinCpuFrequency(): Float {
        val result = shizukuService?.executeCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq")
        return if (result?.success == true) {
            (result.output.trim().toLongOrNull() ?: 0L) / 1000f
        } else {
            0f
        }
    }

    private suspend fun getCpuGovernor(): String {
        val result = shizukuService?.executeCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
        return if (result?.success == true) {
            result.output.trim()
        } else {
            "unknown"
        }
    }

    private suspend fun getAvailableMemory(): Long {
        val result = shizukuService?.executeCommand("cat /proc/meminfo | grep MemAvailable")
        return if (result?.success == true) {
            val line = result.output.trim()
            val parts = line.split(Regex("\\s+"))
            if (parts.size >= 2) {
                parts[1].toLongOrNull()?.times(1024) ?: 0L // Convert from KB to bytes
            } else {
                0L
            }
        } else {
            0L
        }
    }

    private fun isKillable(packageName: String): Boolean {
        // 判断是否可以杀死该进程
        val systemPackages = listOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.phone",
            "com.google.android.gms",
            "com.android.providers",
            "com.android.inputmethod",
            packageName // 不要杀死自己
        )

        return !systemPackages.any { packageName.contains(it) }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        shizukuService = null
    }
}

// 数据类定义
enum class ShizukuState {
    Unknown,
    NotInstalled,
    Unavailable,
    Denied,
    Granted
}

data class ProcessInfo(
    val pid: Int,
    val name: String,
    val packageName: String,
    val uid: Int,
    val memoryUsage: Long
)

data class CpuInfo(
    val cores: Int = 0,
    val model: String = "",
    val currentFrequency: Float = 0f,
    val maxFrequency: Float = 0f,
    val minFrequency: Float = 0f,
    val governor: String = ""
)

data class AppInfo(
    val packageName: String,
    val name: String,
    val path: String,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val uid: Int
)

data class MemoryCleanResult(
    val success: Boolean,
    val freedMemory: Long
)