package com.lanhe.gongjuxiang.utils

import android.app.ActivityManager
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
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 真实电池优化实现
 * 提供真实的电池分析和优化功能
 */
class RealBatteryOptimizer(private val context: Context) {

    companion object {
        private const val TAG = "RealBatteryOptimizer"
        private const val HIGH_DRAIN_THRESHOLD = 10.0f // 高耗电阈值（%/小时）
        private const val WAKE_LOCK_THRESHOLD = 60000L // Wake Lock阈值（1分钟）
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    } else {
        null
    }
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    // 电池历史记录
    private val batteryHistory = mutableListOf<BatterySnapshot>()
    private var lastBatterySnapshot: BatterySnapshot? = null

    /**
     * 获取真实的电池百分比
     */
    fun getRealBatteryPercentage(): Int {
        val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        } ?: return 0

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).roundToInt()
        } else {
            0
        }
    }

    /**
     * 获取真实的电池温度
     */
    fun getRealBatteryTemperature(): Float {
        val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        } ?: return 0f

        val temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        return temperature / 10.0f // 转换为摄氏度
    }

    /**
     * 计算真实的剩余时间
     */
    suspend fun calculateRealRemainingTime(): Long = withContext(Dispatchers.IO) {
        val currentLevel = getRealBatteryPercentage()
        val isCharging = isCharging()

        if (isCharging) {
            // 充电时间估算
            return@withContext calculateChargingTime(currentLevel)
        } else {
            // 使用时间估算
            return@withContext calculateDrainTime(currentLevel)
        }
    }

    /**
     * 计算充电时间
     */
    private fun calculateChargingTime(currentLevel: Int): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && batteryManager != null) {
            val timeToFull = batteryManager.computeChargeTimeRemaining()
            if (timeToFull > 0) {
                return timeToFull
            }
        }

        // 基于充电速度估算
        val chargingRate = getChargingRate()
        if (chargingRate > 0) {
            val remainingPercent = 100 - currentLevel
            val hours = remainingPercent / chargingRate
            return (hours * 3600 * 1000).toLong()
        }

        // 默认估算：快充约1小时，普通充电约2小时
        val remainingPercent = 100 - currentLevel
        val minutesPerPercent = if (isFastCharging()) 0.6 else 1.2
        return (remainingPercent * minutesPerPercent * 60 * 1000).toLong()
    }

    /**
     * 计算放电时间
     */
    private fun calculateDrainTime(currentLevel: Int): Long {
        // 收集电池历史数据
        updateBatteryHistory()

        // 基于历史数据计算平均放电率
        val drainRate = calculateAverageDrainRate()
        if (drainRate > 0) {
            val hours = currentLevel / drainRate
            return (hours * 3600 * 1000).toLong()
        }

        // 基于屏幕状态的默认估算
        val isScreenOn = powerManager.isInteractive
        val hoursPerPercent = if (isScreenOn) 0.1 else 0.3 // 屏幕开启时消耗更快
        return (currentLevel * hoursPerPercent * 3600 * 1000).toLong()
    }

    /**
     * 更新电池历史记录
     */
    private fun updateBatteryHistory() {
        val currentSnapshot = BatterySnapshot(
            timestamp = System.currentTimeMillis(),
            level = getRealBatteryPercentage(),
            temperature = getRealBatteryTemperature(),
            isCharging = isCharging(),
            voltage = getBatteryVoltage()
        )

        batteryHistory.add(currentSnapshot)

        // 保留最近1小时的数据
        val oneHourAgo = System.currentTimeMillis() - 3600000
        batteryHistory.removeAll { it.timestamp < oneHourAgo }

        lastBatterySnapshot = currentSnapshot
    }

    /**
     * 计算平均放电率
     */
    private fun calculateAverageDrainRate(): Float {
        if (batteryHistory.size < 2) return 0f

        val nonChargingSnapshots = batteryHistory.filter { !it.isCharging }
        if (nonChargingSnapshots.size < 2) return 0f

        val first = nonChargingSnapshots.first()
        val last = nonChargingSnapshots.last()
        val timeDiffHours = (last.timestamp - first.timestamp) / 3600000.0f

        return if (timeDiffHours > 0) {
            abs(first.level - last.level) / timeDiffHours
        } else {
            0f
        }
    }

    /**
     * 获取充电速率
     */
    private fun getChargingRate(): Float {
        val chargingSnapshots = batteryHistory.filter { it.isCharging }
        if (chargingSnapshots.size < 2) return 0f

        val first = chargingSnapshots.first()
        val last = chargingSnapshots.last()
        val timeDiffHours = (last.timestamp - first.timestamp) / 3600000.0f

        return if (timeDiffHours > 0 && last.level > first.level) {
            (last.level - first.level) / timeDiffHours
        } else {
            0f
        }
    }

    /**
     * 分析真实的耗电应用排行
     */
    suspend fun analyzeRealBatteryDrain(): List<BatteryDrainApp> = withContext(Dispatchers.IO) {
        val drainApps = mutableListOf<BatteryDrainApp>()

        try {
            // 获取应用使用统计
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 24 * 60 * 60 * 1000 // 过去24小时

            val usageStats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                beginTime,
                endTime
            )

            // 获取运行中的进程
            val runningApps = activityManager.runningAppProcesses ?: emptyList()
            val runningPackages = runningApps.map { it.processName }.toSet()

            // 分析每个应用的耗电情况
            for (stats in usageStats) {
                if (stats.totalTimeInForeground > 0) {
                    val appInfo = try {
                        packageManager.getApplicationInfo(stats.packageName, 0)
                    } catch (e: PackageManager.NameNotFoundException) {
                        continue
                    }

                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    // 计算耗电量（基于使用时间和是否在后台运行）
                    val foregroundTime = stats.totalTimeInForeground / 1000 / 60 // 转换为分钟
                    val isRunning = runningPackages.contains(stats.packageName)
                    val backgroundDrain = if (isRunning && stats.lastTimeUsed < endTime - 300000) {
                        5.0f // 后台运行加5%耗电
                    } else {
                        0f
                    }

                    val estimatedDrain = calculateAppBatteryDrain(
                        foregroundTime,
                        backgroundDrain,
                        isSystemApp
                    )

                    drainApps.add(
                        BatteryDrainApp(
                            packageName = stats.packageName,
                            appName = appName,
                            drainPercentage = estimatedDrain,
                            foregroundTime = foregroundTime,
                            isSystemApp = isSystemApp,
                            isRunning = isRunning,
                            lastUsedTime = stats.lastTimeUsed
                        )
                    )
                }
            }

            // 按耗电量排序
            drainApps.sortByDescending { it.drainPercentage }
            drainApps.take(20) // 返回前20个耗电应用

        } catch (e: Exception) {
            Log.e(TAG, "分析耗电应用失败", e)
        }

        return@withContext drainApps
    }

    /**
     * 计算应用耗电量
     */
    private fun calculateAppBatteryDrain(
        foregroundMinutes: Long,
        backgroundDrain: Float,
        isSystemApp: Boolean
    ): Float {
        // 基于应用类型和使用时间估算耗电量
        val baseDrain = foregroundMinutes * 0.1f // 每分钟前台使用约0.1%电量
        val typeFactor = if (isSystemApp) 0.5f else 1.0f // 系统应用通常更省电
        return (baseDrain * typeFactor + backgroundDrain).coerceAtMost(100f)
    }

    /**
     * 返回真实的优化建议
     */
    suspend fun generateRealOptimizationSuggestions(): List<BatteryOptimizationSuggestion> =
        withContext(Dispatchers.IO) {
            val suggestions = mutableListOf<BatteryOptimizationSuggestion>()

            // 1. 分析高耗电应用
            val drainApps = analyzeRealBatteryDrain()
            drainApps.filter { it.drainPercentage > HIGH_DRAIN_THRESHOLD && !it.isSystemApp }
                .take(3)
                .forEach { app ->
                    suggestions.add(
                        BatteryOptimizationSuggestion(
                            type = "限制应用",
                            title = "限制 ${app.appName}",
                            description = "该应用在过去24小时消耗了${app.drainPercentage.roundToInt()}%的电量",
                            estimatedSaving = (app.drainPercentage * 0.5f).roundToInt(),
                            priority = SuggestionPriority.HIGH,
                            action = "restrict_app",
                            targetPackage = app.packageName
                        )
                    )
                }

            // 2. 检查屏幕亮度
            val brightness = getScreenBrightness()
            if (brightness > 70) {
                suggestions.add(
                    BatteryOptimizationSuggestion(
                        type = "调整设置",
                        title = "降低屏幕亮度",
                        description = "当前亮度${brightness}%，建议降低到50%以节省电量",
                        estimatedSaving = 10,
                        priority = SuggestionPriority.MEDIUM,
                        action = "reduce_brightness"
                    )
                )
            }

            // 3. 检查WiFi和蓝牙状态
            if (!isWifiEnabled() && isMobileDataEnabled()) {
                suggestions.add(
                    BatteryOptimizationSuggestion(
                        type = "网络优化",
                        title = "使用WiFi代替移动数据",
                        description = "WiFi比移动数据更省电",
                        estimatedSaving = 5,
                        priority = SuggestionPriority.LOW,
                        action = "enable_wifi"
                    )
                )
            }

            // 4. 检查定位服务
            if (isHighAccuracyLocationEnabled()) {
                suggestions.add(
                    BatteryOptimizationSuggestion(
                        type = "定位优化",
                        title = "降低定位精度",
                        description = "高精度定位会消耗更多电量",
                        estimatedSaving = 8,
                        priority = SuggestionPriority.MEDIUM,
                        action = "reduce_location_accuracy"
                    )
                )
            }

            // 5. 检查电池温度
            val temperature = getRealBatteryTemperature()
            if (temperature > 40) {
                suggestions.add(
                    BatteryOptimizationSuggestion(
                        type = "温度管理",
                        title = "电池温度过高",
                        description = "当前温度${temperature}°C，建议停止使用让设备降温",
                        estimatedSaving = 15,
                        priority = SuggestionPriority.HIGH,
                        action = "cool_down"
                    )
                )
            }

            // 6. 省电模式建议
            if (!powerManager.isPowerSaveMode && getRealBatteryPercentage() < 30) {
                suggestions.add(
                    BatteryOptimizationSuggestion(
                        type = "省电模式",
                        title = "开启省电模式",
                        description = "电量低于30%，建议开启省电模式",
                        estimatedSaving = 20,
                        priority = SuggestionPriority.HIGH,
                        action = "enable_power_save"
                    )
                )
            }

            // 按优先级排序
            suggestions.sortWith(compareByDescending<BatteryOptimizationSuggestion> { it.priority.value }
                .thenByDescending { it.estimatedSaving })

            suggestions
        }

    /**
     * 检查是否在充电
     */
    private fun isCharging(): Boolean {
        val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        } ?: return false

        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL
    }

    /**
     * 检查是否快充
     */
    private fun isFastCharging(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            } ?: return false

            val chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            // USB 3.0或AC充电通常是快充
            return chargePlug == BatteryManager.BATTERY_PLUGGED_AC
        }
        return false
    }

    /**
     * 获取电池电压
     */
    private fun getBatteryVoltage(): Float {
        val batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        } ?: return 0f

        val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        return voltage / 1000.0f // 转换为伏特
    }

    /**
     * 获取屏幕亮度百分比
     */
    private fun getScreenBrightness(): Int {
        return try {
            val brightness = android.provider.Settings.System.getInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS
            )
            (brightness * 100 / 255)
        } catch (e: Exception) {
            50 // 默认值
        }
    }

    /**
     * 检查WiFi是否启用
     */
    private fun isWifiEnabled(): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        return wifiManager.isWifiEnabled
    }

    /**
     * 检查移动数据是否启用
     */
    private fun isMobileDataEnabled(): Boolean {
        // 这个需要系统权限，简化处理
        return true
    }

    /**
     * 检查是否使用高精度定位
     */
    private fun isHighAccuracyLocationEnabled(): Boolean {
        return try {
            val locationMode = android.provider.Settings.Secure.getInt(
                context.contentResolver,
                android.provider.Settings.Secure.LOCATION_MODE
            )
            locationMode == android.provider.Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 电池快照数据
     */
    private data class BatterySnapshot(
        val timestamp: Long,
        val level: Int,
        val temperature: Float,
        val isCharging: Boolean,
        val voltage: Float
    )
}

/**
 * 电池耗电应用信息
 */
data class BatteryDrainApp(
    val packageName: String,
    val appName: String,
    val drainPercentage: Float,
    val foregroundTime: Long, // 分钟
    val isSystemApp: Boolean,
    val isRunning: Boolean,
    val lastUsedTime: Long
)

/**
 * 电池优化建议
 */
data class BatteryOptimizationSuggestion(
    val type: String,
    val title: String,
    val description: String,
    val estimatedSaving: Int, // 预计节省的电量百分比
    val priority: SuggestionPriority,
    val action: String,
    val targetPackage: String? = null
)

/**
 * 建议优先级
 */
enum class SuggestionPriority(val value: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3)
}