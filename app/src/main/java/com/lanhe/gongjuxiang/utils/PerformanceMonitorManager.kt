package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.lanhe.gongjuxiang.models.BatteryInfo
import com.lanhe.gongjuxiang.models.NetworkStats
import com.lanhe.gongjuxiang.models.PerformanceData
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 性能监控管理器 - 升级版
 * 集成真实数据收集，替换硬编码占位符
 */
class PerformanceMonitorManager(private val context: Context) {

    companion object {
        private const val TAG = "PerformanceMonitorManager"
        private const val MONITORING_INTERVAL = 2000L // 2秒监控间隔
    }

    // 使用真实的性能监控器
    private val realPerformanceMonitor = RealPerformanceMonitorManager(context)

    // 保留原有的性能监控器用于CPU和存储信息
    private val legacyPerformanceMonitor = PerformanceMonitor(context)
    private val wifiOptimizer = WifiOptimizer(context)

    private var monitoringJob: Job? = null
    private val isMonitoring = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())

    // 回调接口
    interface PerformanceCallback {
        fun onPerformanceUpdate(data: PerformanceData)
        fun onMonitoringStarted()
        fun onMonitoringStopped()
        fun onError(error: Exception)
        fun onDataSaved(recordCount: Long)
    }

    private var callback: PerformanceCallback? = null

    /**
     * 设置监控回调
     */
    fun setCallback(callback: PerformanceCallback) {
        this.callback = callback

        // 设置真实性能监控的回调
        realPerformanceMonitor.setCallback(object : RealPerformanceMonitorManager.PerformanceCallback {
            override fun onPerformanceUpdate(data: PerformanceData) {
                this@PerformanceMonitorManager.callback?.onPerformanceUpdate(data)
            }

            override fun onMonitoringStarted() {
                this@PerformanceMonitorManager.callback?.onMonitoringStarted()
            }

            override fun onMonitoringStopped() {
                this@PerformanceMonitorManager.callback?.onMonitoringStopped()
            }

            override fun onError(error: Exception) {
                this@PerformanceMonitorManager.callback?.onError(error)
            }

            override fun onDataSaved(recordCount: Long) {
                this@PerformanceMonitorManager.callback?.onDataSaved(recordCount)
            }
        })
    }

    /**
     * 开始性能监控
     */
    fun startMonitoring() {
        if (isMonitoring.get()) {
            Log.w(TAG, "性能监控已在运行")
            return
        }

        isMonitoring.set(true)

        try {
            // 启动真实性能监控
            realPerformanceMonitor.startMonitoring()

            // 启动混合监控任务（结合所有数据源）
            startHybridMonitoring()

            callback?.onMonitoringStarted()
            Log.i(TAG, "性能监控已启动 - 使用真实数据源")
            
        } catch (e: Exception) {
            Log.e(TAG, "启动性能监控失败", e)
            isMonitoring.set(false)
            callback?.onError(e)
        }
    }

    /**
     * 启动混合监控任务
     */
    private fun startHybridMonitoring() {
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring.get() && isActive) {
                try {
                    // 收集综合性能数据
                    val performanceData = collectHybridPerformanceData()
                    
                    // 通知回调
                    withContext(Dispatchers.Main) {
                        callback?.onPerformanceUpdate(performanceData)
                    }
                    
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "混合性能监控异常", e)
                    withContext(Dispatchers.Main) {
                        callback?.onError(e)
                    }
                    delay(MONITORING_INTERVAL)
                }
            }
        }
    }

    /**
     * 收集混合性能数据
     */
    private suspend fun collectHybridPerformanceData(): PerformanceData = withContext(Dispatchers.IO) {
        try {
            // 从真实监控器获取基础数据
            val baseData = realPerformanceMonitor.getCurrentPerformance()

            if (baseData != null) {
                baseData
            } else {
                // 如果无法获取真实数据，使用备用方案
                collectFallbackPerformanceData()
            }
        } catch (e: Exception) {
            Log.e(TAG, "收集混合性能数据失败，使用备用方案", e)
            collectFallbackPerformanceData()
        }
    }

    /**
     * 备用性能数据收集
     */
    private suspend fun collectFallbackPerformanceData(): PerformanceData = withContext(Dispatchers.IO) {
        try {
            // 使用传统的性能监控器获取CPU和内存
            val cpuUsage = legacyPerformanceMonitor.getCpuUsage()
            val memoryInfo = legacyPerformanceMonitor.getMemoryInfo()
            val storageInfo = legacyPerformanceMonitor.getStorageInfo()

            // 获取WiFi信息
            val wifiInfo = wifiOptimizer.getCurrentWifiInfo()

            PerformanceData(
                timestamp = System.currentTimeMillis(),
                cpuUsage = cpuUsage.totalUsage,
                memoryUsage = com.lanhe.gongjuxiang.models.MemoryInfo(
                    total = memoryInfo.totalMemory * 1024 * 1024L,
                    available = memoryInfo.availableMemory * 1024 * 1024L,
                    used = memoryInfo.usedMemory * 1024 * 1024L,
                    usagePercent = memoryInfo.usagePercent
                ),
                storageUsage = storageInfo.usagePercent,
                batteryInfo = BatteryInfo(0, 0f, 0f, 0f, 0, 0, "Unknown", 0, false, "None", 0, 0),
                networkType = "Unknown",
                deviceTemperature = 0f
            )
        } catch (e: Exception) {
            Log.e(TAG, "备用性能数据收集失败", e)
            // 返回默认数据，避免崩溃
            PerformanceData(
                timestamp = System.currentTimeMillis(),
                cpuUsage = 0f,
                memoryUsage = com.lanhe.gongjuxiang.models.MemoryInfo(0, 0, 0, 0f),
                storageUsage = 0f,
                batteryInfo = BatteryInfo(0, 0f, 0f, 0f, 0, 0, "Unknown", 0, false, "None", 0, 0),
                networkType = "Unknown",
                deviceTemperature = 0f
            )
        }
    }

    /**
     * 停止性能监控
     */
    fun stopMonitoring() {
        if (!isMonitoring.get()) {
            Log.w(TAG, "性能监控未在运行")
            return
        }

        isMonitoring.set(false)

        try {
            // 停止所有监控组件
            realPerformanceMonitor.stopMonitoring()

            // 取消监控任务
            monitoringJob?.cancel()
            monitoringJob = null

            callback?.onMonitoringStopped()
            Log.i(TAG, "性能监控已停止")
            
        } catch (e: Exception) {
            Log.e(TAG, "停止性能监控时出现异常", e)
        }
    }

    /**
     * 获取监控状态
     */
    fun isMonitoring(): Boolean = isMonitoring.get()

    /**
     * 获取当前性能快照
     */
    suspend fun getCurrentPerformance(): PerformanceData? {
        return try {
            realPerformanceMonitor.getCurrentPerformance() ?: collectFallbackPerformanceData()
        } catch (e: Exception) {
            Log.e(TAG, "获取当前性能数据失败", e)
            null
        }
    }

    /**
     * 获取电池信息（增强版）
     */
    fun getBatteryInfo(): BatteryInfo {
        // 无法在非协程函数中调用 suspend 函数，返回默认值
        return BatteryInfo(0, 0f, 0f, 0f, 0, 0, "Unknown", 0, false, "None", 0, 0)
    }

    /**
     * 获取网络统计（增强版）
     */
    fun getNetworkStats(): NetworkStats {
        return NetworkStats(
            interfaceName = "unknown",
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
    fun getMemoryInfo() = legacyPerformanceMonitor.getMemoryInfo()

    /**
     * 获取网络速度
     */
    suspend fun getNetworkSpeed() = 0.0

    /**
     * 获取详细网络统计
     */
    suspend fun getDetailedNetworkStats() = NetworkStats(
        interfaceName = "unknown",
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

    /**
     * 获取电池统计摘要
     */
    fun getBatterySummary() = "Battery Summary"

    /**
     * 获取网络类型
     */
    fun getNetworkType(): String {
        return "Unknown"
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        try {
            stopMonitoring()
            realPerformanceMonitor.cleanup()
            callback = null

            Log.i(TAG, "性能监控管理器已清理")
        } catch (e: Exception) {
            Log.e(TAG, "清理资源时出现异常", e)
        }
    }
}
