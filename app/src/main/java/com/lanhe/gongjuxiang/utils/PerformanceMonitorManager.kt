package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import com.lanhe.gongjuxiang.models.BatteryInfo
import com.lanhe.gongjuxiang.models.NetworkStats
import com.lanhe.gongjuxiang.models.PerformanceData
import kotlinx.coroutines.*

/**
 * 性能监控管理器
 * 负责统一管理所有性能监控任务
 */
class PerformanceMonitorManager(private val context: Context) {

    private val performanceMonitor = PerformanceMonitor(context)
    private val wifiOptimizer = WifiOptimizer(context)

    private var monitoringJob: Job? = null
    private var isMonitoring = false
    private val handler = Handler(Looper.getMainLooper())

    // 监控间隔 (毫秒)
    private val MONITORING_INTERVAL = 2000L

    // 回调接口
    interface PerformanceCallback {
        fun onPerformanceUpdate(data: PerformanceData)
        fun onMonitoringStarted()
        fun onMonitoringStopped()
        fun onError(error: Exception)
    }

    private var callback: PerformanceCallback? = null

    /**
     * 设置监控回调
     */
    fun setCallback(callback: PerformanceCallback) {
        this.callback = callback
    }

    /**
     * 开始性能监控
     */
    fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        callback?.onMonitoringStarted()

        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring && isActive) {
                try {
                    val performanceData = collectPerformanceData()
                    withContext(Dispatchers.Main) {
                        callback?.onPerformanceUpdate(performanceData)
                    }
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback?.onError(e)
                    }
                    delay(MONITORING_INTERVAL)
                }
            }
        }
    }

    /**
     * 停止性能监控
     */
    fun stopMonitoring() {
        if (!isMonitoring) return

        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        callback?.onMonitoringStopped()
    }

    /**
     * 获取监控状态
     */
    fun isMonitoring(): Boolean = isMonitoring

    /**
     * 收集性能数据
     */
    private suspend fun collectPerformanceData(): PerformanceData = withContext(Dispatchers.IO) {
        // 获取CPU使用率
        val cpuUsage = performanceMonitor.getCpuUsage()

        // 获取内存信息
        val memoryInfo = performanceMonitor.getMemoryInfo()

        // 获取存储信息
        val storageInfo = performanceMonitor.getStorageInfo()

        // 获取WiFi信息
        val wifiInfo = wifiOptimizer.getCurrentWifiInfo()

        // 创建电池信息 (这里可以扩展为实际的电池监控)
        val batteryInfo = BatteryInfo(
            level = 50, // 需要实际实现
            temperature = 25.0f,
            voltage = 4.2f,
            current = 0.0f,
            status = 0,
            health = 0,
            technology = "Li-ion",
            capacity = 4000L,
            isCharging = false,
            chargeType = "None",
            timeToFull = 0L,
            timeToEmpty = 0L
        )

        // 创建网络统计 (这里可以扩展为实际的网络监控)
        val networkStats = NetworkStats(
            interfaceName = "wlan0",
            rxBytes = 0L,
            txBytes = 0L,
            rxPackets = 0L,
            txPackets = 0L,
            rxErrors = 0L,
            txErrors = 0L,
            rxDropped = 0L,
            txDropped = 0L,
            timestamp = System.currentTimeMillis()
        )

        return@withContext PerformanceData(
            timestamp = System.currentTimeMillis(),
            cpuUsage = cpuUsage.totalUsage,
            memoryUsage = com.lanhe.gongjuxiang.models.MemoryInfo(
                total = memoryInfo.totalMemory,
                available = memoryInfo.availableMemory,
                used = memoryInfo.usedMemory,
                usagePercent = memoryInfo.usagePercent
            ),
            storageUsage = storageInfo.usagePercent,
            batteryInfo = batteryInfo,
            networkType = wifiInfo?.networkType ?: "Unknown",
            deviceTemperature = 0f
        )
    }

    /**
     * 获取当前性能快照
     */
    suspend fun getCurrentPerformance(): PerformanceData? {
        return try {
            collectPerformanceData()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取电池信息
     */
    fun getBatteryInfo(): BatteryInfo {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = (level * 100 / scale.toFloat()).toInt()

        val temperature = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f
        val voltage = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000.0f

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: 0
        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: 0
        val technology = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val chargeType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "None"
        }

        // 获取电池容量（如果可用）
        val capacity = try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.div(1000) ?: 4000L
            } else {
                4000L // 默认值
            }
        } catch (e: Exception) {
            4000L // 默认值
        }

        return BatteryInfo(
            level = batteryPct,
            temperature = temperature,
            voltage = voltage,
            current = 0.0f, // 这里可以扩展为获取真实电流
            status = status,
            health = health,
            technology = technology,
            capacity = capacity,
            isCharging = isCharging,
            chargeType = chargeType,
            timeToFull = 0L, // 这里可以扩展为计算时间
            timeToEmpty = 0L // 这里可以扩展为计算时间
        )
    }

    /**
     * 获取网络统计
     */
    fun getNetworkStats(): NetworkStats {
        return NetworkStats(
            interfaceName = "wlan0",
            rxBytes = 0L,
            txBytes = 0L,
            rxPackets = 0L,
            txPackets = 0L,
            rxErrors = 0L,
            txErrors = 0L,
            rxDropped = 0L,
            txDropped = 0L,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * 获取内存信息
     */
    fun getMemoryInfo() = performanceMonitor.getMemoryInfo()

    /**
     * 清理资源
     */
    fun cleanup() {
        stopMonitoring()
        callback = null
    }
}
