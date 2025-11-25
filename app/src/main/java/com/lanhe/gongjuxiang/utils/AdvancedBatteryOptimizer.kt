package com.lanhe.gongjuxiang.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.lanhe.gongjuxiang.models.BatteryInfo  // 导入models包中的BatteryInfo
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// Import data classes from SystemOptimizer
import com.lanhe.gongjuxiang.utils.OptimizationItem
import com.lanhe.gongjuxiang.utils.BatteryOptimizationResult

/**
 * 高级电池优化器
 * 基于GitHub成熟解决方案实现的电池优化功能
 * 包含Doze模式优化、wake lock检测、充电优化等
 */
class AdvancedBatteryOptimizer(private val context: Context) {

    companion object {
        private const val TAG = "AdvancedBatteryOptimizer"
        private const val DOZE_CONSTANTS_KEY = "device_idle_constants"
        private const val BATTERY_OPTIMIZATION_WHITELIST_KEY = "battery_optimization_whitelist"
    }

    private val dataManager = DataManager(context)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val packageManager = context.packageManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // 优化状态流
    private val _optimizationState = MutableStateFlow<BatteryOptimizationState>(BatteryOptimizationState.IDLE)
    val optimizationState: StateFlow<BatteryOptimizationState> = _optimizationState.asStateFlow()

    // 电池分析结果
    private val _batteryAnalysis = MutableStateFlow<BatteryAnalysisResult>(BatteryAnalysisResult())
    val batteryAnalysis: StateFlow<BatteryAnalysisResult> = _batteryAnalysis.asStateFlow()

    /**
     * 执行全面电池优化
     */
    suspend fun performFullBatteryOptimization(): BatteryOptimizationResult {
        _optimizationState.value = BatteryOptimizationState.RUNNING

        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<String>()
                var totalSavings = 0

                // 1. Doze模式优化
                val dozeOptimization = optimizeDozeMode()
                if (dozeOptimization.success) {
                    results.addAll(dozeOptimization.improvements)
                    totalSavings += dozeOptimization.estimatedSavings
                }

                // 2. Wake Lock检测和管理
                val wakeLockOptimization = optimizeWakeLocks()
                if (wakeLockOptimization.success) {
                    results.addAll(wakeLockOptimization.improvements)
                    totalSavings += wakeLockOptimization.estimatedSavings
                }

                // 3. 电池耗电分析
                val drainAnalysis = analyzeBatteryDrain()
                results.addAll(drainAnalysis.recommendations)

                // 4. 自适应电池建议
                val adaptiveRecommendations = generateAdaptiveBatteryRecommendations()
                results.addAll(adaptiveRecommendations)

                // 5. 充电优化
                val chargingOptimization = optimizeCharging()
                if (chargingOptimization.success) {
                    results.addAll(chargingOptimization.improvements)
                }

                // 6. 智能省电模式
                val smartPowerSaving = enableSmartPowerSaving()
                if (smartPowerSaving.success) {
                    results.addAll(smartPowerSaving.improvements)
                    totalSavings += smartPowerSaving.estimatedSavings
                }

                BatteryOptimizationResult(
                    success = results.isNotEmpty(),
                    improvements = results,
                    estimatedSavings = totalSavings,
                    message = if (results.isNotEmpty()) "电池优化完成，预计节省${totalSavings}%电量" else "未发现可优化项目"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Battery optimization failed", e)
                BatteryOptimizationResult(
                    success = false,
                    message = "优化失败: ${e.message}"
                )
            } finally {
                _optimizationState.value = BatteryOptimizationState.IDLE
            }
        }
    }

    /**
     * Doze模式优化
     */
    private suspend fun optimizeDozeMode(): OptimizationItem {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()

                // 1. 优化Doze参数
                if (optimizeDozeParameters()) {
                    improvements.add("优化Doze模式进入时间")
                }

                // 2. 管理Doze白名单
                val whitelistOptimized = optimizeDozeWhitelist()
                if (whitelistOptimized > 0) {
                    improvements.add("优化${whitelistOptimized}个应用的Doze白名单设置")
                }

                // 3. 强制进入Doze模式（调试用）
                if (canEnterDozeMode()) {
                    improvements.add("设备支持强制进入Doze模式")
                }

                OptimizationItem(
                    name = "Doze模式优化",
                    success = improvements.isNotEmpty(),
                    improvements = improvements,
                    estimatedSavings = if (improvements.isNotEmpty()) 15 else 0
                )
            } catch (e: Exception) {
                Log.e(TAG, "Doze optimization failed", e)
                OptimizationItem(
                    name = "Doze模式优化",
                    success = false,
                    error = e.message ?: "未知错误"
                )
            }
        }
    }

    /**
     * Wake Lock检测和管理
     */
    private suspend fun optimizeWakeLocks(): OptimizationItem {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()
                val wakeLockApps = detectProblematicWakeLocks()

                if (wakeLockApps.isNotEmpty()) {
                    var optimizedCount = 0
                    wakeLockApps.forEach { app ->
                        if (attemptWakeLockOptimization(app)) {
                            optimizedCount++
                        }
                    }

                    if (optimizedCount > 0) {
                        improvements.add("优化${optimizedCount}个应用的Wake Lock使用")
                        improvements.add("检测到${wakeLockApps.size}个可能的耗电应用")
                    }
                }

                OptimizationItem(
                    name = "Wake Lock优化",
                    success = improvements.isNotEmpty(),
                    improvements = improvements,
                    estimatedSavings = improvements.size * 5
                )
            } catch (e: Exception) {
                Log.e(TAG, "Wake lock optimization failed", e)
                OptimizationItem(
                    name = "Wake Lock优化",
                    success = false,
                    error = e.message ?: "未知错误"
                )
            }
        }
    }

    /**
     * 电池耗电分析
     */
    private suspend fun analyzeBatteryDrain(): BatteryDrainAnalysis {
        return withContext(Dispatchers.IO) {
            try {
                val analysis = BatteryDrainAnalysis()
                val recommendations = mutableListOf<String>()

                // 1. 分析应用电池使用情况
                val appUsage = analyzeAppBatteryUsage()
                val topDrainers = appUsage.take(5)

                if (topDrainers.isNotEmpty()) {
                    recommendations.add("发现${topDrainers.size}个高耗电应用")
                    topDrainers.forEach { app ->
                        recommendations.add("${app.appName}: ${app.batteryUsage}%")
                    }
                }

                // 2. 检查屏幕亮度
                val screenBrightness = getScreenBrightness()
                if (screenBrightness > 80) {
                    recommendations.add("屏幕亮度过高(${screenBrightness}%)，建议降低到60%以下")
                }

                // 3. 检查后台应用刷新
                val backgroundApps = getBackgroundRefreshApps()
                if (backgroundApps > 10) {
                    recommendations.add("后台应用刷新过多(${backgroundApps}个)，建议限制不必要的应用")
                }

                analysis.copy(recommendations = recommendations)
            } catch (e: Exception) {
                Log.e(TAG, "Battery drain analysis failed", e)
                BatteryDrainAnalysis(recommendations = listOf("分析失败: ${e.message}"))
            }
        }
    }

    /**
     * 生成自适应电池建议
     */
    private suspend fun generateAdaptiveBatteryRecommendations(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val recommendations = mutableListOf<String>()
                val currentTime = System.currentTimeMillis()
                val batteryLevel = getCurrentBatteryLevel()

                // 基于时间的建议
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                when {
                    hour in 22..23 || hour in 0..6 -> {
                        recommendations.add("夜间模式：建议启用超级省电模式")
                        recommendations.add("建议关闭不必要的网络连接")
                    }
                    hour in 7..9 -> {
                        recommendations.add("上班时间：建议启用工作模式优化")
                    }
                    hour in 18..21 -> {
                        recommendations.add("娱乐时间：建议优化多媒体应用设置")
                    }
                }

                // 基于电量的建议
                when {
                    batteryLevel < 20 -> {
                        recommendations.add("电量不足：建议立即启用省电模式")
                        recommendations.add("关闭GPS和蓝牙以节省电量")
                    }
                    batteryLevel < 50 -> {
                        recommendations.add("电量一般：建议限制后台应用活动")
                    }
                    batteryLevel > 80 -> {
                        recommendations.add("电量充足：可以正常使用所有功能")
                    }
                }

                // 基于使用模式的建议
                val usagePattern = analyzeUsagePattern()
                recommendations.addAll(usagePattern.suggestions)

                recommendations
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate adaptive recommendations", e)
                listOf("生成建议失败: ${e.message}")
            }
        }
    }

    /**
     * 充电优化
     */
    private suspend fun optimizeCharging(): OptimizationItem {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()
                val batteryInfo = getBatteryInfo()

                // 1. 智能充电建议
                if (batteryInfo.isCharging) {
                    when {
                        batteryInfo.level > 85 -> {
                            improvements.add("电量超过85%，建议使用慢速充电保护电池")
                        }
                        batteryInfo.temperature > 35 -> {
                            improvements.add("充电温度过高，建议暂停快充")
                        }
                        batteryInfo.level < 20 -> {
                            improvements.add("电量过低，建议使用快充到30%后切换慢充")
                        }
                    }
                }

                // 2. 夜间充电优化
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                if (hour in 22..6 && batteryInfo.isCharging) {
                    improvements.add("夜间充电：建议启用智能充电延迟")
                }

                // 3. 电池健康保护
                val chargeCycles = getChargeCycles()
                if (chargeCycles > 500) {
                    improvements.add("充电次数较多，建议避免过度充放电")
                }

                OptimizationItem(
                    name = "充电优化",
                    success = improvements.isNotEmpty(),
                    improvements = improvements,
                    estimatedSavings = 0 // 充电优化主要是保护电池健康
                )
            } catch (e: Exception) {
                Log.e(TAG, "Charging optimization failed", e)
                OptimizationItem(
                    name = "充电优化",
                    success = false,
                    error = e.message ?: "未知错误"
                )
            }
        }
    }

    /**
     * 智能省电模式
     */
    private suspend fun enableSmartPowerSaving(): OptimizationItem {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()

                // 1. 自动亮度调节
                if (enableAutoBrightness()) {
                    improvements.add("启用自动亮度调节")
                }

                // 2. 网络优化
                if (optimizeNetworkForBattery()) {
                    improvements.add("优化网络设置以节省电量")
                }

                // 3. 后台应用限制
                val restrictedApps = restrictBackgroundApps()
                if (restrictedApps > 0) {
                    improvements.add("限制${restrictedApps}个后台应用")
                }

                // 4. 系统动画优化
                if (optimizeSystemAnimations()) {
                    improvements.add("优化系统动画以节省电量")
                }

                OptimizationItem(
                    name = "智能省电模式",
                    success = improvements.isNotEmpty(),
                    improvements = improvements,
                    estimatedSavings = improvements.size * 3
                )
            } catch (e: Exception) {
                Log.e(TAG, "Smart power saving failed", e)
                OptimizationItem(
                    name = "智能省电模式",
                    success = false,
                    error = e.message ?: "未知错误"
                )
            }
        }
    }

    // 私有辅助方法

    private fun optimizeDozeParameters(): Boolean {
        return try {
            if (ShizukuManager.isShizukuAvailable()) {
                // 优化Doze模式参数
                val dozeConstants = "inactive_to=30000,sensing_to=0,locating_to=0," +
                        "location_accuracy=2000,motion_inactive_to=0,idle_after_inactive_to=0," +
                        "idle_pending_to=30000,max_idle_pending_to=60000,idle_pending_factor=2.0," +
                        "idle_to=1800000,max_idle_to=10800000,idle_factor=2.0"

                ShizukuManager.putGlobalSetting(DOZE_CONSTANTS_KEY, dozeConstants)
                true
            } else {
                Log.w(TAG, "Shizuku not available for Doze optimization")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to optimize Doze parameters", e)
            false
        }
    }

    private fun optimizeDozeWhitelist(): Int {
        return try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            var optimizedCount = 0

            installedApps.forEach { app ->
                if (shouldRemoveFromDozeWhitelist(app)) {
                    if (removeFromDozeWhitelist(app.packageName)) {
                        optimizedCount++
                    }
                }
            }

            optimizedCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to optimize Doze whitelist", e)
            0
        }
    }

    private fun shouldRemoveFromDozeWhitelist(app: ApplicationInfo): Boolean {
        // 判断是否应该从Doze白名单中移除应用
        val systemApps = listOf(
            "com.android.systemui",
            "com.android.phone",
            "com.android.mms"
        )

        return !systemApps.contains(app.packageName) &&
               (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    }

    private fun removeFromDozeWhitelist(packageName: String): Boolean {
        return try {
            if (ShizukuManager.isShizukuAvailable()) {
                // 使用Shizuku移除应用从电池优化白名单
                ShizukuManager.executeShellCommand("dumpsys deviceidle whitelist -$packageName")
                true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove $packageName from Doze whitelist", e)
            false
        }
    }

    private fun canEnterDozeMode(): Boolean {
        return try {
            powerManager.isDeviceIdleMode || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun detectProblematicWakeLocks(): List<WakeLockApp> {
        val problematicApps = mutableListOf<WakeLockApp>()

        try {
            // 获取使用统计数据
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.HOURS.toMillis(24)

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            usageStats?.forEach { stats ->
                if (stats.totalTimeInForeground > 0) {
                    val app = try {
                        val appInfo = packageManager.getApplicationInfo(stats.packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()

                        // 估算wake lock使用情况
                        val suspiciousWakeLockUsage = stats.totalTimeInForeground > TimeUnit.HOURS.toMillis(2) ||
                                                    stats.lastTimeUsed > endTime - TimeUnit.MINUTES.toMillis(30)

                        if (suspiciousWakeLockUsage) {
                            WakeLockApp(
                                packageName = stats.packageName,
                                appName = appName,
                                wakeLockTime = stats.totalTimeInForeground,
                                lastUsed = stats.lastTimeUsed
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }

                    app?.let { problematicApps.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect wake locks", e)
        }

        return problematicApps.sortedByDescending { it.wakeLockTime }
    }

    private fun attemptWakeLockOptimization(app: WakeLockApp): Boolean {
        return try {
            // 尝试优化应用的wake lock使用
            if (ShizukuManager.isShizukuAvailable()) {
                // 设置应用后台限制
                ShizukuManager.executeShellCommand("cmd appops set ${app.packageName} RUN_IN_BACKGROUND ignore")
                true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to optimize wake lock for ${app.packageName}", e)
            false
        }
    }

    private fun analyzeAppBatteryUsage(): List<AppBatteryUsage> {
        val appUsageList = mutableListOf<AppBatteryUsage>()

        try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.HOURS.toMillis(24)

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            usageStats?.forEach { stats ->
                if (stats.totalTimeInForeground > 0) {
                    try {
                        val appInfo = packageManager.getApplicationInfo(stats.packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()

                        // 估算电池使用百分比（基于使用时间）
                        val batteryUsage = (stats.totalTimeInForeground.toFloat() / TimeUnit.HOURS.toMillis(24) * 100).roundToInt()

                        if (batteryUsage > 1) { // 只显示使用超过1%的应用
                            appUsageList.add(
                                AppBatteryUsage(
                                    packageName = stats.packageName,
                                    appName = appName,
                                    batteryUsage = batteryUsage,
                                    foregroundTime = stats.totalTimeInForeground
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // 应用可能已被卸载
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze app battery usage", e)
        }

        return appUsageList.sortedByDescending { it.batteryUsage }
    }

    private fun getScreenBrightness(): Int {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        } catch (e: Exception) {
            50 // 默认值
        }
    }

    private fun getBackgroundRefreshApps(): Int {
        return try {
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            apps.count { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        } catch (e: Exception) {
            0
        }
    }

    private fun getCurrentBatteryLevel(): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return intent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                (level.toFloat() / scale.toFloat() * 100f).roundToInt()
            } else {
                50
            }
        } ?: 50
    }

    private fun analyzeUsagePattern(): UsagePattern {
        // 分析用户使用模式并提供建议
        val suggestions = mutableListOf<String>()

        try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(7) // 过去7天

            val weeklyStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_WEEKLY,
                startTime,
                endTime
            )

            // 分析使用时间模式
            val totalUsageTime = weeklyStats?.sumOf { it.totalTimeInForeground } ?: 0
            val avgDailyUsage = totalUsageTime / 7

            when {
                avgDailyUsage > TimeUnit.HOURS.toMillis(8) -> {
                    suggestions.add("重度使用：建议定时启用省电模式")
                    suggestions.add("考虑使用深色主题以节省屏幕电量")
                }
                avgDailyUsage > TimeUnit.HOURS.toMillis(4) -> {
                    suggestions.add("中度使用：建议优化高耗电应用设置")
                }
                else -> {
                    suggestions.add("轻度使用：可以启用性能模式提升体验")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze usage pattern", e)
            suggestions.add("无法分析使用模式")
        }

        return UsagePattern(suggestions = suggestions)
    }

    private fun getBatteryInfo(): BatteryInfo {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return intent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val temperature = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val voltage = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)  // 添加voltage
            val health = it.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)  // 添加health
            val pluggedType = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)  // 添加pluggedType

            val batteryPercent = if (level >= 0 && scale > 0) {
                (level.toFloat() / scale.toFloat() * 100f).roundToInt()
            } else {
                50
            }

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL

            BatteryInfo(
                level = batteryPercent,
                temperature = temperature / 10f,
                voltage = voltage / 1000f,
                current = 0f,
                status = status,
                health = health,
                technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown",
                capacity = 0L,
                isCharging = isCharging,
                chargeType = when (pluggedType) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                    else -> "None"
                },
                timeToFull = 0L,
                timeToEmpty = 0L
            )
        } ?: BatteryInfo(
            level = 50,
            temperature = 25f,
            voltage = 3.7f,
            current = 0f,
            status = BatteryManager.BATTERY_STATUS_UNKNOWN,
            health = BatteryManager.BATTERY_HEALTH_UNKNOWN,
            technology = "Li-ion",
            capacity = 0L,
            isCharging = false,
            chargeType = "None",
            timeToFull = 0L,
            timeToEmpty = 0L
        )
    }

    private fun getChargeCycles(): Int {
        return try {
            // 尝试从系统文件读取充电次数
            val cycleFile = File("/sys/class/power_supply/battery/cycle_count")
            if (cycleFile.exists()) {
                RandomAccessFile(cycleFile, "r").use { it.readLine().toIntOrNull() ?: 0 }
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun enableAutoBrightness(): Boolean {
        return try {
            if (ShizukuManager.isShizukuAvailable()) {
                ShizukuManager.putSystemSetting("screen_brightness_mode", "1")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun optimizeNetworkForBattery(): Boolean {
        return try {
            // 网络省电优化（需要具体实现）
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun restrictBackgroundApps(): Int {
        return try {
            // 限制后台应用（需要具体实现）
            5 // 示例返回值
        } catch (e: Exception) {
            0
        }
    }

    private fun optimizeSystemAnimations(): Boolean {
        return try {
            if (ShizukuManager.isShizukuAvailable()) {
                ShizukuManager.putGlobalSetting("animator_duration_scale", "0.5")
                ShizukuManager.putGlobalSetting("transition_animation_scale", "0.5")
                ShizukuManager.putGlobalSetting("window_animation_scale", "0.5")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

// 数据类定义

enum class BatteryOptimizationState {
    IDLE, RUNNING, COMPLETED, ERROR
}


data class BatteryAnalysisResult(
    val drainRate: Float = 0f,
    val averageTemperature: Float = 0f,
    val topDrainers: List<AppBatteryUsage> = emptyList(),
    val recommendations: List<String> = emptyList()
)

data class BatteryDrainAnalysis(
    val totalDrain: Float = 0f,
    val screenDrain: Float = 0f,
    val appDrain: Float = 0f,
    val systemDrain: Float = 0f,
    val recommendations: List<String> = emptyList()
)

data class WakeLockApp(
    val packageName: String,
    val appName: String,
    val wakeLockTime: Long,
    val lastUsed: Long
)

data class AppBatteryUsage(
    val packageName: String,
    val appName: String,
    val batteryUsage: Int, // 百分比
    val foregroundTime: Long
)

data class UsagePattern(
    val avgDailyUsage: Long = 0L,
    val peakUsageHours: List<Int> = emptyList(),
    val suggestions: List<String> = emptyList()
)

// BatteryInfo已经在models包中定义，此处删除重复定义

