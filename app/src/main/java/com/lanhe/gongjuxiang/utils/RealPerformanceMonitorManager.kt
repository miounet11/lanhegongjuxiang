package com.lanhe.gongjuxiang.utils

import android.Manifest
import android.app.usage.NetworkStatsManager
import android.app.usage.NetworkStats as AndroidNetworkStats
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.lanhe.gongjuxiang.models.BatteryInfo
import com.lanhe.gongjuxiang.models.MemoryInfo
import com.lanhe.gongjuxiang.models.NetworkStats
import com.lanhe.gongjuxiang.models.PerformanceData
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 真实的性能监控管理器
 * 替换硬编码数据，实现真实的系统性能监控
 */
class RealPerformanceMonitorManager(private val context: Context) {

    companion object {
        private const val TAG = "RealPerformanceMonitor"
        private const val MONITORING_INTERVAL = 2000L // 2秒监控间隔
        private const val TEMP_UPDATE_THRESHOLD = 2.0f // 温度变化阈值
        private const val NETWORK_STATS_INTERVAL = 5000L // 网络统计间隔
        
        // CPU温度文件路径
        private val CPU_TEMP_PATHS = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone2/temp",
            "/sys/class/hwmon/hwmon0/temp1_input",
            "/sys/devices/virtual/thermal/thermal_zone0/temp"
        )
    }

    private val performanceMonitor = PerformanceMonitor(context)
    private val database = AppDatabase.getDatabase(context)
    private val performanceDataDao = database.performanceDataDao()
    
    private var monitoringJob: Job? = null
    private var networkStatsJob: Job? = null
    private val isMonitoring = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    
    // 网络统计缓存
    private val lastNetworkStats = AtomicLong(0)
    private val lastNetworkTimestamp = AtomicLong(0)
    
    // 电池状态缓存
    private var lastBatteryLevel = -1
    private var lastBatteryTemp = 0f
    private var batteryHistory = mutableListOf<BatterySnapshot>()
    
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
     * 电池快照数据类
     */
    private data class BatterySnapshot(
        val timestamp: Long,
        val level: Int,
        val temperature: Float,
        val isCharging: Boolean
    )
    
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
        if (isMonitoring.get()) return
        
        isMonitoring.set(true)
        callback?.onMonitoringStarted()
        
        // 启动主监控任务
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring.get() && isActive) {
                try {
                    val performanceData = collectRealPerformanceData()
                    
                    // 保存到数据库
                    saveToDatabase(performanceData)
                    
                    // 通知回调
                    withContext(Dispatchers.Main) {
                        callback?.onPerformanceUpdate(performanceData)
                    }
                    
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "性能监控异常", e)
                    withContext(Dispatchers.Main) {
                        callback?.onError(e)
                    }
                    delay(MONITORING_INTERVAL)
                }
            }
        }
        
        // 启动网络统计任务
        startNetworkStatsMonitoring()
    }
    
    /**
     * 停止性能监控
     */
    fun stopMonitoring() {
        if (!isMonitoring.get()) return
        
        isMonitoring.set(false)
        monitoringJob?.cancel()
        networkStatsJob?.cancel()
        
        monitoringJob = null
        networkStatsJob = null
        callback?.onMonitoringStopped()
        
        Log.i(TAG, "性能监控已停止")
    }
    
    /**
     * 获取监控状态
     */
    fun isMonitoring(): Boolean = isMonitoring.get()
    
    /**
     * 收集真实的性能数据
     */
    private suspend fun collectRealPerformanceData(): PerformanceData = withContext(Dispatchers.IO) {
        // 获取CPU使用率（增强版）
        val cpuUsage = getRealCpuUsage()
        
        // 获取真实内存信息
        val memoryInfo = getRealMemoryInfo()
        
        // 获取真实存储信息
        val storageInfo = performanceMonitor.getStorageInfo()
        
        // 获取真实电池信息
        val batteryInfo = getRealBatteryInfo()
        
        // 获取网络统计
        val networkStats = getRealNetworkStats()
        
        // 获取设备温度
        val deviceTemperature = getDeviceTemperature()
        
        // 获取网络类型
        val networkType = getNetworkType()
        
        PerformanceData(
            timestamp = System.currentTimeMillis(),
            cpuUsage = cpuUsage,
            memoryUsage = memoryInfo,
            storageUsage = storageInfo.usagePercent,
            batteryInfo = batteryInfo,
            networkType = networkType,
            deviceTemperature = deviceTemperature
        )
    }
    
    /**
     * 获取真实的CPU使用率
     */
    private suspend fun getRealCpuUsage(): Float = withContext(Dispatchers.IO) {
        try {
            val cpuStats = readCpuStats()
            val totalCores = Runtime.getRuntime().availableProcessors()
            
            // 计算CPU使用率
            val totalDiff = cpuStats.total - cpuStats.idle
            val totalUsage = if (cpuStats.total > 0) {
                (totalDiff.toFloat() / cpuStats.total.toFloat()) * 100
            } else {
                0f
            }
            
            // 尝试使用Shizuku获取更精确的CPU数据
            val shizukuCpuUsage = try {
                if (ShizukuManager.isShizukuAvailable()) {
                    ShizukuManager.getCpuUsage()
                } else {
                    0f
                }
            } catch (e: Exception) {
                0f
            }
            
            // 优先使用Shizuku数据，否则使用系统数据
            if (shizukuCpuUsage > 0) {
                shizukuCpuUsage.coerceIn(0f, 100f)
            } else {
                totalUsage.coerceIn(0f, 100f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取CPU使用率失败", e)
            0f
        }
    }
    
    /**
     * 读取CPU统计数据
     */
    private data class CpuStats(val total: Long, val idle: Long)
    
    private fun readCpuStats(): CpuStats {
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(FileReader("/proc/stat"))
            val line = reader.readLine()
            if (line != null && line.startsWith("cpu ")) {
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 5) {
                    val user = parts[1].toLong()
                    val nice = parts[2].toLong()
                    val system = parts[3].toLong()
                    val idle = parts[4].toLong()
                    val iowait = if (parts.size > 5) parts[5].toLong() else 0
                    val irq = if (parts.size > 6) parts[6].toLong() else 0
                    val softirq = if (parts.size > 7) parts[7].toLong() else 0
                    
                    val total = user + nice + system + idle + iowait + irq + softirq
                    return CpuStats(total, idle)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "读取CPU统计失败", e)
        } finally {
            try {
                reader?.close()
            } catch (e: Exception) {
                // 忽略关闭异常
            }
        }
        return CpuStats(0, 0)
    }
    
    /**
     * 获取真实的内存信息
     */
    private suspend fun getRealMemoryInfo(): MemoryInfo = withContext(Dispatchers.IO) {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val totalMemory = memoryInfo.totalMem
            val availableMemory = memoryInfo.availMem
            val usedMemory = totalMemory - availableMemory
            val usagePercent = if (totalMemory > 0) {
                (usedMemory.toFloat() / totalMemory.toFloat()) * 100
            } else {
                0f
            }
            
            // 尝试从/proc/meminfo获取更详细的信息
            val memInfo = readMemInfo()
            val adjustedAvailable = memInfo["MemAvailable"] ?: availableMemory
            
            MemoryInfo(
                total = totalMemory,
                available = adjustedAvailable,
                used = totalMemory - adjustedAvailable,
                usagePercent = usagePercent
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取内存信息失败", e)
            // 返回默认值
            MemoryInfo(0, 0, 0, 0f)
        }
    }
    
    /**
     * 从/proc/meminfo读取内存信息
     */
    private fun readMemInfo(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        var reader: BufferedReader? = null
        
        try {
            reader = BufferedReader(FileReader("/proc/meminfo"))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split("\\s+".toRegex())
                if (parts.size >= 2) {
                    val key = parts[0].replace(":", "")
                    val value = parts[1].toLong() * 1024 // 转换为字节
                    result[key] = value
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "读取meminfo失败", e)
        } finally {
            try {
                reader?.close()
            } catch (e: Exception) {
                // 忽略关闭异常
            }
        }
        
        return result
    }
    
    /**
     * 获取真实的电池信息
     */
    private fun getRealBatteryInfo(): BatteryInfo {
        return try {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            
            // 基本电池信息
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val batteryPct = if (scale > 0) (level * 100 / scale.toFloat()).toInt() else 0
            
            val temperature = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f
            val voltage = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000.0f
            
            // 充电状态
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) 
                ?: BatteryManager.BATTERY_STATUS_UNKNOWN
            val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
            val technology = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
            
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                           status == BatteryManager.BATTERY_STATUS_FULL
            
            // 充电类型
            val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            val chargeType = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "None"
            }
            
            // 获取电池容量
            val capacity = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.div(1000)
                } else {
                    null
                } ?: getBatteryCapacityFromSystem()
            } catch (e: Exception) {
                getBatteryCapacityFromSystem()
            }
            
            // 计算电流（需要支持的平台）
            val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.toFloat()?.div(1000) ?: 0f
                } catch (e: Exception) {
                    0f
                }
            } else {
                0f
            }
            
            // 估算充电和放电时间
            val (timeToFull, timeToEmpty) = calculateBatteryTimes(batteryPct, isCharging, temperature)
            
            // 更新历史记录
            updateBatteryHistory(batteryPct, temperature, isCharging)
            
            BatteryInfo(
                level = batteryPct,
                temperature = temperature,
                voltage = voltage,
                current = current,
                status = status,
                health = health,
                technology = technology,
                capacity = capacity,
                isCharging = isCharging,
                chargeType = chargeType,
                timeToFull = timeToFull,
                timeToEmpty = timeToEmpty
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取电池信息失败", e)
            // 返回默认值
            BatteryInfo(0, 0f, 0f, 0f, 0, 0, "Unknown", 0, false, "None", 0, 0)
        }
    }
    
    /**
     * 从系统获取电池容量
     */
    private fun getBatteryCapacityFromSystem(): Long {
        return try {
            // 尝试从PowerProfile获取电池容量
            val powerProfile = Class.forName("com.android.internal.os.PowerProfile")
            val powerProfileInstance = powerProfile.getConstructor(Context::class.java).newInstance(context)
            val getBatteryCapacity = powerProfile.getMethod("getBatteryCapacity")
            val capacity = getBatteryCapacity.invoke(powerProfileInstance) as Double
            capacity.toLong()
        } catch (e: Exception) {
            Log.w(TAG, "无法获取电池容量，使用默认值", e)
            4000L // 默认容量
        }
    }
    
    /**
     * 计算电池充电和放电时间
     */
    private fun calculateBatteryTimes(level: Int, isCharging: Boolean, temperature: Float): Pair<Long, Long> {
        if (batteryHistory.size < 2) {
            return Pair(0, 0)
        }
        
        try {
            val recent = batteryHistory.takeLast(5) // 取最近5个数据点
            if (recent.size < 2) return Pair(0, 0)
            
            // 计算平均变化率
            val levelDiff = recent.last().level - recent.first().level
            val timeDiff = (recent.last().timestamp - recent.first().timestamp) / (1000 * 60) // 分钟
            
            if (timeDiff <= 0) return Pair(0, 0)
            
            val rate = levelDiff.toDouble() / timeDiff // 每分钟变化百分比
            
            return if (isCharging) {
                // 计算充满时间
                val remaining = 100 - level
                val estimatedMinutes = if (rate > 0) (remaining / rate) else 0
                Pair(estimatedMinutes.toLong(), 0)
            } else {
                // 计算用尽时间
                val estimatedMinutes = if (rate < 0) (level / -rate) else 0
                Pair(0, estimatedMinutes.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "计算电池时间失败", e)
            return Pair(0, 0)
        }
    }
    
    /**
     * 更新电池历史记录
     */
    private fun updateBatteryHistory(level: Int, temperature: Float, isCharging: Boolean) {
        val now = System.currentTimeMillis()
        batteryHistory.add(BatterySnapshot(now, level, temperature, isCharging))
        
        // 保留最近100条记录
        if (batteryHistory.size > 100) {
            batteryHistory.removeAt(0)
        }
    }
    
    /**
     * 获取网络统计
     */
    private fun getRealNetworkStats(): NetworkStats {
        return try {
            val now = System.currentTimeMillis()
            val lastTimestamp = lastNetworkTimestamp.get()
            val timeDiff = now - lastTimestamp
            
            // 获取总流量
            val totalRxBytes = TrafficStats.getTotalRxBytes()
            val totalTxBytes = TrafficStats.getTotalTxBytes()
            
            // 计算速度
            var rxSpeed = 0.0
            var txSpeed = 0.0
            
            if (timeDiff > 0 && lastTimestamp > 0) {
                val lastRxBytes = lastNetworkStats.get() shr 32 // 取高32位
                val lastTxBytes = lastNetworkStats.get() and 0xFFFFFFFFL // 取低32位
                
                if (totalRxBytes > lastRxBytes) {
                    rxSpeed = (totalRxBytes - lastRxBytes).toDouble() * 1000 / timeDiff // bytes/s
                }
                if (totalTxBytes > lastTxBytes) {
                    txSpeed = (totalTxBytes - lastTxBytes).toDouble() * 1000 / timeDiff // bytes/s
                }
            }
            
            // 更新缓存
            lastNetworkStats.set((totalRxBytes shl 32) or totalTxBytes)
            lastNetworkTimestamp.set(now)
            
            NetworkStats(
                interfaceName = "total",
                rxBytes = totalRxBytes,
                txBytes = totalTxBytes,
                rxPackets = TrafficStats.getTotalRxPackets(),
                txPackets = TrafficStats.getTotalTxPackets(),
                rxErrors = 0, // 无法直接获取
                txErrors = 0, // 无法直接获取
                rxDropped = 0, // 无法直接获取
                txDropped = 0, // 无法直接获取
                timestamp = now,
                rxSpeed = rxSpeed,
                txSpeed = txSpeed,
                totalSpeed = rxSpeed + txSpeed
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取网络统计失败", e)
            NetworkStats("total", 0, 0, 0, 0, 0, 0, 0, 0, System.currentTimeMillis())
        }
    }
    
    /**
     * 启动网络统计监控
     */
    private fun startNetworkStatsMonitoring() {
        networkStatsJob = CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring.get() && isActive) {
                try {
                    // 定期更新网络统计
                    getRealNetworkStats()
                    delay(NETWORK_STATS_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "网络统计异常", e)
                    delay(NETWORK_STATS_INTERVAL)
                }
            }
        }
    }
    
    /**
     * 获取设备温度
     */
    private fun getDeviceTemperature(): Float {
        // 尝试从多个路径读取CPU温度
        for (path in CPU_TEMP_PATHS) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val temp = file.readText().trim().toFloat()
                    // 某些系统返回的温度需要除以1000
                    val adjustedTemp = if (temp > 100) temp / 1000f else temp
                    return adjustedTemp
                }
            } catch (e: Exception) {
                continue // 尝试下一个路径
            }
        }
        
        // 尝试从电池温度估算
        return try {
            val batteryInfo = getRealBatteryInfo()
            batteryInfo.temperature
        } catch (e: Exception) {
            0f
        }
    }
    
    /**
     * 获取网络类型
     */
    private fun getNetworkType(): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetworkInfo
            
            when (activeNetwork?.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> {
                    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    when (telephonyManager.networkType) {
                        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                        else -> {
                            // NETWORK_TYPE_LTE_CA requires API 25+
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                if (telephonyManager.networkType == 19) { // 19 is NETWORK_TYPE_LTE_CA
                                    "4G"
                                } else if (telephonyManager.networkType == 20) { // 20 is NETWORK_TYPE_NR
                                    "5G"
                                } else {
                                    when (telephonyManager.networkType) {
                                        TelephonyManager.NETWORK_TYPE_HSPA,
                                        TelephonyManager.NETWORK_TYPE_HSPAP,
                                        TelephonyManager.NETWORK_TYPE_HSUPA,
                                        TelephonyManager.NETWORK_TYPE_HSDPA -> "3G"
                                        TelephonyManager.NETWORK_TYPE_EDGE,
                                        TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                                        else -> "Mobile"
                                    }
                                }
                            } else {
                                when (telephonyManager.networkType) {
                                    TelephonyManager.NETWORK_TYPE_HSPA,
                                    TelephonyManager.NETWORK_TYPE_HSPAP,
                                    TelephonyManager.NETWORK_TYPE_HSUPA,
                                    TelephonyManager.NETWORK_TYPE_HSDPA -> "3G"
                                    TelephonyManager.NETWORK_TYPE_EDGE,
                                    TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
                                    else -> "Mobile"
                                }
                            }
                        }
                    }
                }
                ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * 保存性能数据到数据库
     */
    private suspend fun saveToDatabase(performanceData: PerformanceData) {
        try {
            val entity = PerformanceDataEntity(
                timestamp = performanceData.timestamp,
                cpuUsage = performanceData.cpuUsage,
                memoryUsagePercent = performanceData.memoryUsage.usagePercent.toInt(),
                memoryUsedMB = performanceData.memoryUsage.used / (1024 * 1024),
                memoryTotalMB = performanceData.memoryUsage.total / (1024 * 1024),
                batteryLevel = performanceData.batteryInfo.level,
                batteryTemperature = performanceData.batteryInfo.temperature,
                batteryVoltage = performanceData.batteryInfo.voltage,
                batteryIsCharging = performanceData.batteryInfo.isCharging,
                batteryIsPlugged = performanceData.batteryInfo.chargeType != "None",
                deviceTemperature = performanceData.deviceTemperature,
                isScreenOn = isScreenOn(),
                dataType = "performance"
            )
            
            val id = performanceDataDao.insert(entity)
            
            // 通知数据已保存
            withContext(Dispatchers.Main) {
                callback?.onDataSaved(id)
            }
            
            // 定期清理旧数据（保留30天）
            cleanupOldData()
            
        } catch (e: Exception) {
            Log.e(TAG, "保存性能数据失败", e)
        }
    }
    
    /**
     * 检查屏幕是否亮着
     */
    private fun isScreenOn(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                powerManager.isInteractive
            } else {
                @Suppress("DEPRECATION")
                powerManager.isScreenOn
            }
        } catch (e: Exception) {
            true // 默认认为屏幕亮着
        }
    }
    
    /**
     * 清理旧数据
     */
    private suspend fun cleanupOldData() {
        try {
            val cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) // 30天前
            performanceDataDao.cleanupOldData(cutoffTime)
        } catch (e: Exception) {
            Log.e(TAG, "清理旧数据失败", e)
        }
    }
    
    /**
     * 获取当前性能快照
     */
    suspend fun getCurrentPerformance(): PerformanceData? {
        return try {
            collectRealPerformanceData()
        } catch (e: Exception) {
            Log.e(TAG, "获取当前性能数据失败", e)
            null
        }
    }
    
    /**
     * 获取电池信息（公共方法）
     */
    fun getBatteryInfo(): BatteryInfo {
        return getRealBatteryInfo()
    }
    
    /**
     * 获取网络统计（公共方法）
     */
    fun getNetworkStats(): NetworkStats {
        return getRealNetworkStats()
    }
    
    /**
     * 获取内存信息（公共方法）
     */
    suspend fun getMemoryInfo(): MemoryInfo {
        return getRealMemoryInfo()
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        stopMonitoring()
        callback = null
        batteryHistory.clear()
        Log.i(TAG, "性能监控管理器已清理")
    }
}
