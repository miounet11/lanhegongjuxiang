package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.roundToInt

/**
 * 电池监控器
 * 提供详细的电池使用情况监控、耗电统计和优化建议
 */
class BatteryMonitor(private val context: Context) {

    private val dataManager = DataManager(context)

    // 电池状态流
    private val _batteryStats = MutableStateFlow<BatteryStats>(BatteryStats())
    val batteryStats: StateFlow<BatteryStats> = _batteryStats.asStateFlow()

    // 电池历史数据
    private val batteryHistory = mutableListOf<BatteryDataPoint>()

    // 监控任务
    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 电池容量相关
    private var designCapacity = 0
    private var lastBatteryLevel = -1
    private var lastUpdateTime = 0L

    // 耗电统计
    private var screenOnTime = 0L
    private var screenOffTime = 0L
    private var lastScreenState = false

    init {
        loadBatteryCapacity()
        startBatteryMonitoring()
    }

    /**
     * 获取当前电池统计信息
     */
    fun getCurrentBatteryStats(): BatteryStats {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryStats()

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)

        val batteryPercent = if (level >= 0 && scale > 0) {
            (level.toFloat() / scale.toFloat() * 100f).roundToInt()
        } else {
            0
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                       status == BatteryManager.BATTERY_STATUS_FULL
        val isPlugged = plugged != 0

        val chargingMethod = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "交流电"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线充电"
            else -> "未充电"
        }

        val healthStatus = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
            BatteryManager.BATTERY_HEALTH_DEAD -> "损坏"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "电压过高"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "未知故障"
            BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
            else -> "未知"
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        return BatteryStats(
            level = batteryPercent,
            temperature = temperature / 10f, // 转换为摄氏度
            voltage = voltage / 1000f, // 转换为伏特
            isCharging = isCharging,
            isPlugged = isPlugged,
            chargingMethod = chargingMethod,
            healthStatus = healthStatus,
            isScreenOn = isScreenOn,
            designCapacity = designCapacity,
            currentCapacity = calculateCurrentCapacity(batteryPercent)
        )
    }

    /**
     * 开始电池监控
     */
    fun startBatteryMonitoring(interval: Long = 5000) {
        stopBatteryMonitoring()
        monitoringJob = scope.launch {
            while (isActive) {
                val currentStats = getCurrentBatteryStats()
                _batteryStats.value = currentStats

                // 添加到历史记录
                addToHistory(currentStats)

                // 更新屏幕状态统计
                updateScreenTimeStats(currentStats.isScreenOn)

                // 保存到数据库
                try {
                    val lifeEstimate = estimateBatteryLife()
                    dataManager.saveBatteryStats(
                        batteryLevel = currentStats.level,
                        temperature = currentStats.temperature,
                        voltage = currentStats.voltage,
                        isCharging = currentStats.isCharging,
                        isPlugged = currentStats.isPlugged,
                        screenOnTime = screenOnTime,
                        screenOffTime = screenOffTime,
                        estimatedLifeHours = lifeEstimate.remainingHours,
                        drainRate = calculateBatteryDrainRate(),
                        healthStatus = currentStats.healthStatus
                    )
                } catch (e: Exception) {
                    // 记录保存失败，但不影响监控继续进行
                    println("Failed to save battery stats: ${e.message}")
                }

                delay(interval)
            }
        }
    }

    /**
     * 停止电池监控
     */
    fun stopBatteryMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * 获取电池使用统计
     */
    fun getBatteryUsageStats(): BatteryUsageStats {
        val totalTime = SystemClock.elapsedRealtime()
        val screenOnRatio = if (totalTime > 0) {
            (screenOnTime.toFloat() / totalTime.toFloat() * 100f).roundToInt()
        } else {
            0
        }

        return BatteryUsageStats(
            totalUptime = totalTime,
            screenOnTime = screenOnTime,
            screenOffTime = screenOffTime,
            screenOnRatio = screenOnRatio,
            averageTemperature = calculateAverageTemperature(),
            batteryDrainRate = calculateBatteryDrainRate()
        )
    }

    /**
     * 获取电池优化建议
     */
    fun getBatteryOptimizationTips(): List<BatteryTip> {
        val tips = mutableListOf<BatteryTip>()
        val currentStats = _batteryStats.value

        // 高温警告
        if (currentStats.temperature > 40f) {
            tips.add(BatteryTip(
                title = "电池温度过高",
                description = "当前电池温度为${String.format("%.1f", currentStats.temperature)}°C，建议降低设备负载",
                severity = TipSeverity.HIGH,
                action = "减少后台应用，关闭不必要的服务"
            ))
        }

        // 屏幕使用时间过长
        val usageStats = getBatteryUsageStats()
        if (usageStats.screenOnRatio > 70) {
            tips.add(BatteryTip(
                title = "屏幕使用时间过长",
                description = "屏幕亮屏时间占比${usageStats.screenOnRatio}%，建议降低屏幕亮度",
                severity = TipSeverity.MEDIUM,
                action = "降低屏幕亮度，缩短自动锁定时间"
            ))
        }

        // 电池健康检查
        if (currentStats.healthStatus != "良好") {
            tips.add(BatteryTip(
                title = "电池健康异常",
                description = "电池状态：${currentStats.healthStatus}",
                severity = TipSeverity.HIGH,
                action = "建议检查电池或联系售后服务"
            ))
        }

        // 充电优化
        if (currentStats.isCharging && currentStats.level > 80) {
            tips.add(BatteryTip(
                title = "建议断开充电器",
                description = "电池电量已超过80%，长时间充电会影响电池寿命",
                severity = TipSeverity.LOW,
                action = "电量超过80%后可断开充电器"
            ))
        }

        return tips
    }

    /**
     * 执行电池优化
     */
    suspend fun performBatteryOptimization(): List<String> {
        val optimizations = mutableListOf<String>()

        withContext(Dispatchers.IO) {
            try {
                // 1. 启用电池优化
                if (enableBatteryOptimization()) {
                    optimizations.add("启用系统电池优化")
                }

                // 2. 调整屏幕设置
                if (optimizeScreenSettings()) {
                    optimizations.add("优化屏幕亮度和超时设置")
                }

                // 3. 限制后台应用
                val restrictedApps = restrictBackgroundApps()
                if (restrictedApps > 0) {
                    optimizations.add("限制${restrictedApps}个后台应用的电池使用")
                }

                // 4. 启用省电模式
                if (enablePowerSavingMode()) {
                    optimizations.add("启用智能省电模式")
                }

                // 5. vivo设备特殊优化
                if (isVivoDevice()) {
                    if (optimizeVivoBattery()) {
                        optimizations.add("应用vivo设备专用电池优化")
                    }
                }

            } catch (e: Exception) {
                Log.e("BatteryMonitor", "Battery optimization failed", e)
            }
        }

        return optimizations
    }

    /**
     * 获取电池历史数据
     */
    fun getBatteryHistory(hours: Int = 24): List<BatteryDataPoint> {
        val cutoffTime = System.currentTimeMillis() - (hours * 60 * 60 * 1000)
        return batteryHistory.filter { it.timestamp >= cutoffTime }
    }

    /**
     * 计算电池剩余使用时间
     */
    fun estimateBatteryLife(): BatteryLifeEstimate {
        val history = getBatteryHistory(1) // 最近1小时的数据
        if (history.size < 2) {
            return BatteryLifeEstimate(0, 0, "数据不足")
        }

        val drainRate = calculateBatteryDrainRate()
        val currentLevel = _batteryStats.value.level

        val remainingHours = if (drainRate > 0) {
            currentLevel / drainRate
        } else {
            0
        }

        val remainingMinutes = (remainingHours * 60).roundToInt()

        val status = when {
            remainingHours > 8 -> "电量充足"
            remainingHours > 4 -> "电量良好"
            remainingHours > 2 -> "电量一般"
            remainingHours > 1 -> "电量不足"
            else -> "电量严重不足"
        }

        return BatteryLifeEstimate(remainingHours.roundToInt(), remainingMinutes, status)
    }

    // 私有方法

    private fun loadBatteryCapacity() {
        try {
            // 尝试从系统文件中读取电池容量
            val capacityFile = File("/sys/class/power_supply/battery/charge_full_design")
            if (capacityFile.exists()) {
                designCapacity = RandomAccessFile(capacityFile, "r").use { it.readLine().toInt() }
            } else {
                // 使用默认值
                designCapacity = 4000 // mAh
            }
        } catch (e: Exception) {
            designCapacity = 4000 // 默认值
        }
    }

    private fun calculateCurrentCapacity(percentage: Int): Int {
        return (designCapacity * percentage / 100.0).roundToInt()
    }

    private fun addToHistory(stats: BatteryStats) {
        val dataPoint = BatteryDataPoint(
            timestamp = System.currentTimeMillis(),
            level = stats.level,
            temperature = stats.temperature,
            isCharging = stats.isCharging,
            isScreenOn = stats.isScreenOn
        )

        batteryHistory.add(dataPoint)

        // 限制历史记录数量，防止内存泄漏
        if (batteryHistory.size > 1000) {
            batteryHistory.removeAt(0)
        }
    }

    private fun updateScreenTimeStats(isScreenOn: Boolean) {
        val currentTime = SystemClock.elapsedRealtime()

        if (lastUpdateTime > 0) {
            val timeDiff = currentTime - lastUpdateTime
            if (lastScreenState) {
                screenOnTime += timeDiff
            } else {
                screenOffTime += timeDiff
            }
        }

        lastScreenState = isScreenOn
        lastUpdateTime = currentTime
    }

    private fun calculateAverageTemperature(): Float {
        val recentHistory = getBatteryHistory(1)
        if (recentHistory.isEmpty()) return 0f

        return recentHistory.map { it.temperature }.average().toFloat()
    }

    private fun calculateBatteryDrainRate(): Float {
        val history = getBatteryHistory(1)
        if (history.size < 2) return 0f

        val timeDiff = (history.last().timestamp - history.first().timestamp) / (1000 * 60 * 60f) // 小时
        val levelDiff = history.first().level - history.last().level

        return if (timeDiff > 0) levelDiff / timeDiff else 0f
    }

    private fun enableBatteryOptimization(): Boolean {
        return try {
            ShizukuManager.putGlobalSetting("adaptive_battery_management_enabled", "1")
            ShizukuManager.putGlobalSetting("app_standby_enabled", "1")
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun optimizeScreenSettings(): Boolean {
        return try {
            ShizukuManager.putSystemSetting("screen_brightness_mode", "0")
            ShizukuManager.putSystemSetting("screen_off_timeout", "300000") // 5分钟
            ShizukuManager.putSystemSetting("screen_brightness", "80") // 降低亮度
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun restrictBackgroundApps(): Int {
        // 这里应该实现限制后台应用的逻辑
        return 0
    }

    private fun enablePowerSavingMode(): Boolean {
        return try {
            ShizukuManager.putGlobalSetting("low_power", "1")
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isVivoDevice(): Boolean {
        return android.os.Build.MANUFACTURER.lowercase().contains("vivo") ||
               android.os.Build.BRAND.lowercase().contains("vivo")
    }

    private fun optimizeVivoBattery(): Boolean {
        return try {
            // vivo设备专用电池优化
            ShizukuManager.putGlobalSetting("vivo_super_power_save", "1")
            ShizukuManager.putGlobalSetting("vivo_battery_optimization", "1")
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 电池统计数据类
 */
data class BatteryStats(
    val level: Int = 0,
    val temperature: Float = 0f,
    val voltage: Float = 0f,
    val isCharging: Boolean = false,
    val isPlugged: Boolean = false,
    val chargingMethod: String = "",
    val healthStatus: String = "",
    val isScreenOn: Boolean = false,
    val designCapacity: Int = 0,
    val currentCapacity: Int = 0
)

/**
 * 电池使用统计类
 */
data class BatteryUsageStats(
    val totalUptime: Long = 0L,
    val screenOnTime: Long = 0L,
    val screenOffTime: Long = 0L,
    val screenOnRatio: Int = 0,
    val averageTemperature: Float = 0f,
    val batteryDrainRate: Float = 0f
)

/**
 * 电池优化建议类
 */
data class BatteryTip(
    val title: String = "",
    val description: String = "",
    val severity: TipSeverity = TipSeverity.LOW,
    val action: String = ""
)

/**
 * 建议严重程度枚举
 */
enum class TipSeverity {
    LOW, MEDIUM, HIGH
}

/**
 * 电池数据点类
 */
data class BatteryDataPoint(
    val timestamp: Long = 0L,
    val level: Int = 0,
    val temperature: Float = 0f,
    val isCharging: Boolean = false,
    val isScreenOn: Boolean = false
)

/**
 * 电池寿命估算类
 */
data class BatteryLifeEstimate(
    val remainingHours: Int = 0,
    val remainingMinutes: Int = 0,
    val status: String = ""
)
