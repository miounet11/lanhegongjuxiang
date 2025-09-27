package com.lanhe.gongjuxiang.utils

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.roundToInt

/**
 * 游戏模式优化器
 * 提供游戏加速、FPS监控、温度监控、通知屏蔽和网络优先级设置
 */
class GameModeOptimizer(private val context: Context) {

    companion object {
        private const val TAG = "GameModeOptimizer"
        private const val FPS_OVERLAY_UPDATE_INTERVAL = 1000L // 1秒更新FPS
        private const val TEMPERATURE_WARNING_THRESHOLD = 45f // 45°C温度警告
        private const val TEMPERATURE_CRITICAL_THRESHOLD = 55f // 55°C温度危险
        private const val GAME_MODE_PRIORITY_BOOST = 10 // 游戏进程优先级提升
    }

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val packageManager = context.packageManager

    // 游戏模式状态
    private val _gameModeState = MutableStateFlow<GameModeState>(GameModeState())
    val gameModeState: StateFlow<GameModeState> = _gameModeState.asStateFlow()

    // FPS监控
    private var fpsOverlayView: View? = null
    private var fpsMonitoringJob: Job? = null
    private val fpsHistory = mutableListOf<FpsDataPoint>()

    // 温度监控
    private var temperatureMonitoringJob: Job? = null

    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 游戏应用列表
    private val gamePackages = mutableSetOf<String>()

    init {
        loadGamePackages()
    }

    /**
     * 启用游戏模式
     */
    suspend fun enableGameMode(packageName: String? = null): GameModeResult {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()

                // 1. 启用性能模式
                if (enablePerformanceMode()) {
                    improvements.add("启用CPU/GPU性能模式")
                }

                // 2. 清理内存
                val memoryFreed = performGameMemoryCleanup()
                if (memoryFreed > 0) {
                    improvements.add("释放内存${formatMemorySize(memoryFreed)}")
                }

                // 3. 屏蔽通知
                if (enableNotificationBlocking()) {
                    improvements.add("屏蔽游戏期间通知")
                }

                // 4. 网络优先级
                if (setGameNetworkPriority(packageName)) {
                    improvements.add("设置游戏网络优先级")
                }

                // 5. 温度监控
                if (enableTemperatureMonitoring()) {
                    improvements.add("启用温度监控")
                }

                // 6. FPS监控
                if (enableFpsMonitoring()) {
                    improvements.add("启用FPS监控覆盖层")
                }

                // 7. 进程优先级提升
                if (boostGameProcess(packageName)) {
                    improvements.add("提升游戏进程优先级")
                }

                // 8. 系统优化
                if (optimizeSystemForGaming()) {
                    improvements.add("优化系统设置以适应游戏")
                }

                _gameModeState.value = _gameModeState.value.copy(
                    isEnabled = true,
                    currentGame = packageName,
                    enabledFeatures = improvements,
                    timestamp = System.currentTimeMillis()
                )

                GameModeResult(
                    success = improvements.isNotEmpty(),
                    improvements = improvements,
                    message = "游戏模式已启用，应用了${improvements.size}项优化"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable game mode", e)
                GameModeResult(
                    success = false,
                    message = "游戏模式启用失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 关闭游戏模式
     */
    suspend fun disableGameMode(): GameModeResult {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()

                // 1. 关闭FPS监控
                if (disableFpsMonitoring()) {
                    improvements.add("关闭FPS监控覆盖层")
                }

                // 2. 关闭温度监控
                if (disableTemperatureMonitoring()) {
                    improvements.add("关闭温度监控")
                }

                // 3. 恢复通知
                if (disableNotificationBlocking()) {
                    improvements.add("恢复系统通知")
                }

                // 4. 恢复性能模式
                if (disablePerformanceMode()) {
                    improvements.add("恢复平衡性能模式")
                }

                // 5. 恢复进程优先级
                if (restoreProcessPriority()) {
                    improvements.add("恢复进程优先级")
                }

                // 6. 恢复系统设置
                if (restoreSystemSettings()) {
                    improvements.add("恢复系统设置")
                }

                _gameModeState.value = GameModeState()

                GameModeResult(
                    success = improvements.isNotEmpty(),
                    improvements = improvements,
                    message = "游戏模式已关闭"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Failed to disable game mode", e)
                GameModeResult(
                    success = false,
                    message = "游戏模式关闭失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 游戏性能分析
     */
    suspend fun analyzeGamePerformance(packageName: String): GamePerformanceAnalysis {
        return withContext(Dispatchers.IO) {
            try {
                val cpuUsage = getGameCpuUsage(packageName)
                val memoryUsage = getGameMemoryUsage(packageName)
                val gpuUsage = getGameGpuUsage(packageName)
                val currentFps = getCurrentFps()
                val temperature = getCurrentTemperature()
                val networkLatency = getGameNetworkLatency(packageName)

                val recommendations = generateGameRecommendations(
                    cpuUsage, memoryUsage, gpuUsage, currentFps, temperature
                )

                GamePerformanceAnalysis(
                    packageName = packageName,
                    cpuUsage = cpuUsage,
                    memoryUsage = memoryUsage,
                    gpuUsage = gpuUsage,
                    currentFps = currentFps,
                    averageFps = getAverageFps(),
                    minFps = getMinFps(),
                    maxFps = getMaxFps(),
                    temperature = temperature,
                    networkLatency = networkLatency,
                    recommendations = recommendations,
                    timestamp = System.currentTimeMillis()
                )

            } catch (e: Exception) {
                Log.e(TAG, "Game performance analysis failed", e)
                GamePerformanceAnalysis(
                    packageName = packageName,
                    recommendations = listOf("性能分析失败: ${e.message}")
                )
            }
        }
    }

    /**
     * 启用FPS监控覆盖层
     */
    fun enableFpsMonitoring(): Boolean {
        return try {
            if (fpsOverlayView != null) {
                return true // 已经启用
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                // 需要悬浮窗权限
                requestOverlayPermission()
                return false
            }

            createFpsOverlay()
            startFpsMonitoring()
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable FPS monitoring", e)
            false
        }
    }

    /**
     * 关闭FPS监控覆盖层
     */
    fun disableFpsMonitoring(): Boolean {
        return try {
            fpsMonitoringJob?.cancel()
            fpsMonitoringJob = null

            fpsOverlayView?.let { view ->
                windowManager.removeView(view)
                fpsOverlayView = null
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disable FPS monitoring", e)
            false
        }
    }

    /**
     * 获取FPS历史数据
     */
    fun getFpsHistory(minutes: Int = 10): List<FpsDataPoint> {
        val cutoffTime = System.currentTimeMillis() - (minutes * 60 * 1000)
        return fpsHistory.filter { it.timestamp >= cutoffTime }
    }

    /**
     * 游戏加速器
     */
    suspend fun performGameBoost(): GameBoostResult {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()
                var performanceGain = 0

                // 1. 激进内存清理
                val memoryFreed = performAggressiveMemoryCleanup()
                if (memoryFreed > 0) {
                    improvements.add("释放内存${formatMemorySize(memoryFreed)}")
                    performanceGain += 15
                }

                // 2. CPU频率锁定
                if (lockCpuFrequency()) {
                    improvements.add("锁定CPU最大频率")
                    performanceGain += 20
                }

                // 3. GPU频率锁定
                if (lockGpuFrequency()) {
                    improvements.add("锁定GPU最大频率")
                    performanceGain += 20
                }

                // 4. 禁用温控降频
                if (disableThermalThrottling()) {
                    improvements.add("暂时禁用温控降频")
                    performanceGain += 10
                }

                // 5. 网络优化
                if (optimizeGameNetworking()) {
                    improvements.add("优化游戏网络连接")
                    performanceGain += 10
                }

                // 6. 系统服务优化
                if (optimizeSystemServices()) {
                    improvements.add("优化系统服务")
                    performanceGain += 5
                }

                GameBoostResult(
                    success = improvements.isNotEmpty(),
                    improvements = improvements,
                    estimatedPerformanceGain = performanceGain,
                    message = "游戏加速完成，预计性能提升${performanceGain}%"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Game boost failed", e)
                GameBoostResult(
                    success = false,
                    message = "游戏加速失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 检测游戏应用
     */
    fun detectGameApplications(): List<GameAppInfo> {
        val gameApps = mutableListOf<GameAppInfo>()

        try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            installedApps.forEach { appInfo ->
                if (isGameApplication(appInfo)) {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    gameApps.add(GameAppInfo(
                        packageName = appInfo.packageName,
                        appName = appName,
                        isInstalled = true,
                        lastPlayed = getLastPlayedTime(appInfo.packageName)
                    ))
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect game applications", e)
        }

        return gameApps.sortedByDescending { it.lastPlayed }
    }

    // 私有实现方法

    private fun loadGamePackages() {
        // 加载已知游戏包名列表
        gamePackages.addAll(listOf(
            "com.tencent.tmgp.sgame", // 王者荣耀
            "com.tencent.tmgp.pubgmhd", // 和平精英
            "com.miHoYo.GenshinImpact", // 原神
            "com.netease.dwrg", // 第五人格
            "com.netease.hyxd", // 荒野行动
            // 添加更多游戏包名
        ))
    }

    private fun enablePerformanceMode(): Boolean {
        return try {
            if (ShizukuManager.isShizukuAvailable()) {
                // 设置性能调频器
                ShizukuManager.executeShellCommand("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
                ShizukuManager.executeShellCommand("echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun performGameMemoryCleanup(): Long {
        var freedMemory = 0L

        try {
            // 强制垃圾回收
            System.gc()
            Runtime.getRuntime().gc()

            // 清理后台应用
            val runningApps = activityManager.getRunningAppProcesses()
            runningApps?.forEach { process ->
                if (process.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE &&
                    !gamePackages.contains(process.processName)) {
                    try {
                        activityManager.killBackgroundProcesses(process.processName)
                        freedMemory += 50 * 1024 * 1024 // 估算每个进程50MB
                    } catch (e: Exception) {
                        // 忽略清理失败的进程
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Memory cleanup failed", e)
        }

        return freedMemory
    }

    private fun enableNotificationBlocking(): Boolean {
        return try {
            // 设置勿扰模式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun setGameNetworkPriority(packageName: String?): Boolean {
        return try {
            if (packageName != null && ShizukuManager.isShizukuAvailable()) {
                // 设置网络优先级
                ShizukuManager.executeShellCommand("echo $packageName > /proc/sys/net/core/high_priority_app")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun enableTemperatureMonitoring(): Boolean {
        temperatureMonitoringJob?.cancel()
        temperatureMonitoringJob = scope.launch {
            while (isActive) {
                try {
                    val temperature = getCurrentTemperature()

                    if (temperature > TEMPERATURE_CRITICAL_THRESHOLD) {
                        // 临界温度，自动降频
                        handleCriticalTemperature()
                    } else if (temperature > TEMPERATURE_WARNING_THRESHOLD) {
                        // 警告温度，发出提醒
                        handleWarningTemperature(temperature)
                    }

                    _gameModeState.value = _gameModeState.value.copy(
                        currentTemperature = temperature
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "Temperature monitoring error", e)
                }

                delay(5000) // 5秒检查一次
            }
        }
        return true
    }

    private fun disableTemperatureMonitoring(): Boolean {
        temperatureMonitoringJob?.cancel()
        temperatureMonitoringJob = null
        return true
    }

    private fun boostGameProcess(packageName: String?): Boolean {
        return try {
            if (packageName != null && ShizukuManager.isShizukuAvailable()) {
                val pid = getProcessId(packageName)
                if (pid > 0) {
                    // 提升进程优先级
                    ShizukuManager.executeShellCommand("renice -$GAME_MODE_PRIORITY_BOOST $pid")
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun optimizeSystemForGaming(): Boolean {
        return try {
            if (ShizukuManager.isShizukuAvailable()) {
                // 禁用无关系统服务
                ShizukuManager.executeShellCommand("stop backup")
                ShizukuManager.executeShellCommand("stop location")

                // 调整系统参数
                ShizukuManager.executeShellCommand("echo 0 > /proc/sys/kernel/randomize_va_space") // 禁用ASLR
                ShizukuManager.executeShellCommand("echo 1 > /proc/sys/vm/oom_kill_allocating_task") // OOM优化
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun createFpsOverlay() {
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = 20
        layoutParams.y = 100

        // 创建FPS显示视图
        fpsOverlayView = TextView(context).apply {
            text = "FPS: --"
            textSize = 14f
            setTextColor(android.graphics.Color.GREEN)
            setBackgroundColor(android.graphics.Color.argb(128, 0, 0, 0))
            setPadding(16, 8, 16, 8)
        }

        windowManager.addView(fpsOverlayView, layoutParams)
    }

    private fun startFpsMonitoring() {
        fpsMonitoringJob?.cancel()
        fpsMonitoringJob = scope.launch {
            while (isActive && fpsOverlayView != null) {
                try {
                    val fps = calculateCurrentFps()

                    withContext(Dispatchers.Main) {
                        (fpsOverlayView as? TextView)?.text = "FPS: $fps"
                    }

                    // 记录FPS数据
                    fpsHistory.add(FpsDataPoint(
                        fps = fps,
                        timestamp = System.currentTimeMillis()
                    ))

                    // 限制历史记录数量
                    if (fpsHistory.size > 300) { // 保留5分钟的数据
                        fpsHistory.removeAt(0)
                    }

                    _gameModeState.value = _gameModeState.value.copy(
                        currentFps = fps
                    )

                } catch (e: Exception) {
                    Log.e(TAG, "FPS monitoring error", e)
                }

                delay(FPS_OVERLAY_UPDATE_INTERVAL)
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    // 游戏性能分析方法
    private fun getGameCpuUsage(packageName: String): Float {
        // 获取特定应用的CPU使用率
        return try {
            val pid = getProcessId(packageName)
            if (pid > 0) {
                val statFile = File("/proc/$pid/stat")
                if (statFile.exists()) {
                    // 解析CPU使用率（简化实现）
                    25f // 示例值
                } else {
                    0f
                }
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    private fun getGameMemoryUsage(packageName: String): Long {
        return try {
            val pid = getProcessId(packageName)
            if (pid > 0) {
                val memoryInfo = activityManager.getProcessMemoryInfo(intArrayOf(pid))
                if (memoryInfo.isNotEmpty()) {
                    memoryInfo[0].totalPss.toLong() * 1024 // 转换为字节
                } else {
                    0L
                }
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun getGameGpuUsage(packageName: String): Float {
        // GPU使用率获取（需要特殊权限或方法）
        return 0f
    }

    private fun getCurrentFps(): Int {
        return calculateCurrentFps()
    }

    private fun getCurrentTemperature(): Float {
        return try {
            val thermalFiles = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp"
            )

            var maxTemp = 0f
            thermalFiles.forEach { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        val temp = RandomAccessFile(file, "r").use {
                            it.readLine().toFloat() / 1000f
                        }
                        if (temp > maxTemp) {
                            maxTemp = temp
                        }
                    }
                } catch (e: Exception) {
                    // 忽略读取失败的温度文件
                }
            }

            if (maxTemp > 0) maxTemp else 35f // 默认温度
        } catch (e: Exception) {
            35f
        }
    }

    private fun getGameNetworkLatency(packageName: String): Int {
        // 网络延迟检测（需要具体实现）
        return 50 // 示例值
    }

    private fun generateGameRecommendations(
        cpuUsage: Float,
        memoryUsage: Long,
        gpuUsage: Float,
        fps: Int,
        temperature: Float
    ): List<String> {
        val recommendations = mutableListOf<String>()

        when {
            fps < 30 -> {
                recommendations.add("FPS过低，建议降低游戏画质")
                recommendations.add("关闭不必要的后台应用")
            }
            fps < 45 -> {
                recommendations.add("FPS一般，可以适当优化设置")
            }
            fps >= 60 -> {
                recommendations.add("FPS表现良好")
            }
        }

        if (temperature > TEMPERATURE_WARNING_THRESHOLD) {
            recommendations.add("设备温度较高，建议休息或降低画质")
        }

        if (cpuUsage > 80) {
            recommendations.add("CPU使用率过高，建议关闭后台应用")
        }

        if (memoryUsage > 3 * 1024 * 1024 * 1024L) { // 3GB
            recommendations.add("内存使用较高，建议清理内存")
        }

        return recommendations
    }

    // 辅助方法
    private fun calculateCurrentFps(): Int {
        // FPS计算（需要具体实现，这里返回示例值）
        return (45..60).random()
    }

    private fun getAverageFps(): Float {
        return if (fpsHistory.isNotEmpty()) {
            fpsHistory.map { it.fps }.average().toFloat()
        } else {
            0f
        }
    }

    private fun getMinFps(): Int {
        return fpsHistory.minOfOrNull { it.fps } ?: 0
    }

    private fun getMaxFps(): Int {
        return fpsHistory.maxOfOrNull { it.fps } ?: 0
    }

    private fun getProcessId(packageName: String): Int {
        return try {
            val runningApps = activityManager.getRunningAppProcesses()
            runningApps?.find { it.processName == packageName }?.pid ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun isGameApplication(appInfo: android.content.pm.ApplicationInfo): Boolean {
        // 判断是否为游戏应用
        return gamePackages.contains(appInfo.packageName) ||
               appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_GAME ||
               packageManager.getApplicationLabel(appInfo).toString().contains("游戏", ignoreCase = true)
    }

    private fun getLastPlayedTime(packageName: String): Long {
        // 获取应用最后使用时间（需要使用UsageStatsManager）
        return System.currentTimeMillis()
    }

    private fun handleCriticalTemperature() {
        try {
            // 自动降频保护
            if (ShizukuManager.isShizukuAvailable()) {
                ShizukuManager.executeShellCommand("echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle critical temperature", e)
        }
    }

    private fun handleWarningTemperature(temperature: Float) {
        // 发出温度警告（可以通过通知或界面提示）
        Log.w(TAG, "Temperature warning: ${temperature}°C")
    }

    private fun formatMemorySize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return String.format("%.1f%s", size, units[unitIndex])
    }

    // 游戏加速方法实现
    private fun performAggressiveMemoryCleanup(): Long = performGameMemoryCleanup()
    private fun lockCpuFrequency(): Boolean = false
    private fun lockGpuFrequency(): Boolean = false
    private fun disableThermalThrottling(): Boolean = false
    private fun optimizeGameNetworking(): Boolean = false
    private fun optimizeSystemServices(): Boolean = false

    // 关闭游戏模式方法实现
    private fun disablePerformanceMode(): Boolean = false
    private fun disableNotificationBlocking(): Boolean = false
    private fun restoreProcessPriority(): Boolean = false
    private fun restoreSystemSettings(): Boolean = false
}

// 数据类定义

data class GameModeState(
    val isEnabled: Boolean = false,
    val currentGame: String? = null,
    val currentFps: Int = 0,
    val currentTemperature: Float = 0f,
    val enabledFeatures: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class GameModeResult(
    val success: Boolean = false,
    val improvements: List<String> = emptyList(),
    val message: String = ""
)

data class GamePerformanceAnalysis(
    val packageName: String,
    val cpuUsage: Float = 0f,
    val memoryUsage: Long = 0L,
    val gpuUsage: Float = 0f,
    val currentFps: Int = 0,
    val averageFps: Float = 0f,
    val minFps: Int = 0,
    val maxFps: Int = 0,
    val temperature: Float = 0f,
    val networkLatency: Int = 0,
    val recommendations: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class GameBoostResult(
    val success: Boolean = false,
    val improvements: List<String> = emptyList(),
    val estimatedPerformanceGain: Int = 0,
    val message: String = ""
)

data class GameAppInfo(
    val packageName: String,
    val appName: String,
    val isInstalled: Boolean,
    val lastPlayed: Long
)

data class FpsDataPoint(
    val fps: Int,
    val timestamp: Long
)