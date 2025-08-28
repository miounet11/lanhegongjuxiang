package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.system.Os
import android.system.StructStat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.lang.management.ManagementFactory
import kotlin.math.roundToInt

/**
 * 性能监控器
 * 负责收集系统性能数据，包括CPU、内存、电池等信息
 */
class PerformanceMonitor(private val context: Context) {

    private val dataManager = DataManager(context)

    // 性能数据状态
    private val _performanceData = MutableStateFlow<PerformanceData>(PerformanceData())
    val performanceData: StateFlow<PerformanceData> = _performanceData.asStateFlow()

    // 监控任务
    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // CPU使用率计算相关
    private var lastCpuTime: Long = 0
    private var lastAppCpuTime: Long = 0

    // 内存信息
    private var totalMemory: Long = 0
    private var availableMemory: Long = 0

    init {
        updateMemoryInfo()
    }

    /**
     * 开始性能监控
     */
    fun startMonitoring(interval: Long = 1000) {
        stopMonitoring()
        monitoringJob = scope.launch {
            while (isActive) {
                updatePerformanceData()
                delay(interval)
            }
        }
    }

    /**
     * 停止性能监控
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * 更新性能数据
     */
    private suspend fun updatePerformanceData() = withContext(Dispatchers.Default) {
        val currentData = _performanceData.value

        val newData = currentData.copy(
            cpuUsage = getCpuUsage(),
            memoryUsage = getMemoryUsage(),
            batteryInfo = getBatteryInfo(),
            networkStats = getNetworkStats(),
            timestamp = System.currentTimeMillis()
        )

        _performanceData.value = newData

        // 保存到数据库
        try {
            dataManager.savePerformanceData(newData, "realtime")
        } catch (e: Exception) {
            // 记录保存失败，但不影响监控继续进行
            println("Failed to save performance data: ${e.message}")
        }
    }

    /**
     * 获取CPU使用率
     */
    private fun getCpuUsage(): Float {
        return try {
            val statFile = File("/proc/stat")
            if (!statFile.exists()) return 0f

            val reader = BufferedReader(FileReader(statFile))
            val cpuLine = reader.readLine()
            reader.close()

            val parts = cpuLine.split("\\s+".toRegex())
            if (parts.size < 8) return 0f

            val user = parts[1].toLong()
            val nice = parts[2].toLong()
            val system = parts[3].toLong()
            val idle = parts[4].toLong()
            val iowait = parts[5].toLong()
            val irq = parts[6].toLong()
            val softirq = parts[7].toLong()

            val totalCpuTime = user + nice + system + idle + iowait + irq + softirq
            val totalDiff = totalCpuTime - lastCpuTime
            val idleDiff = idle - (lastCpuTime - totalCpuTime + idle)

            lastCpuTime = totalCpuTime

            if (totalDiff == 0L) return 0f
            val usage = ((totalDiff - idleDiff).toFloat() / totalDiff.toFloat()) * 100f
            usage.coerceIn(0f, 100f)

        } catch (e: Exception) {
            0f
        }
    }

    /**
     * 获取内存使用率
     */
    private fun getMemoryUsage(): MemoryInfo {
        return try {
            val memInfoFile = File("/proc/meminfo")
            if (!memInfoFile.exists()) return MemoryInfo()

            val reader = BufferedReader(FileReader(memInfoFile))
            var totalMem = 0L
            var availableMem = 0L

            reader.useLines { lines ->
                lines.forEach { line ->
                    when {
                        line.startsWith("MemTotal:") -> {
                            totalMem = parseMemValue(line)
                        }
                        line.startsWith("MemAvailable:") -> {
                            availableMem = parseMemValue(line)
                        }
                    }
                }
            }

            if (totalMem > 0) {
                val usedMem = totalMem - availableMem
                val usagePercent = (usedMem.toFloat() / totalMem.toFloat() * 100f).roundToInt()
                MemoryInfo(totalMem, availableMem, usedMem, usagePercent)
            } else {
                MemoryInfo()
            }
        } catch (e: Exception) {
            MemoryInfo()
        }
    }

    /**
     * 解析内存值
     */
    private fun parseMemValue(line: String): Long {
        val parts = line.split("\\s+".toRegex())
        return if (parts.size >= 2) {
            parts[1].toLong() * 1024 // KB转字节
        } else {
            0L
        }
    }

    /**
     * 获取电池信息
     */
    private fun getBatteryInfo(): BatteryInfo {
        return try {
            val batteryIntent = context.registerReceiver(null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return BatteryInfo()

            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            val voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            val batteryPercent = if (level >= 0 && scale > 0) {
                (level.toFloat() / scale.toFloat() * 100f).roundToInt()
            } else {
                0
            }

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL
            val isPlugged = plugged != 0

            BatteryInfo(
                level = batteryPercent,
                temperature = temperature / 10f, // 转换为摄氏度
                voltage = voltage / 1000f, // 转换为伏特
                isCharging = isCharging,
                isPlugged = isPlugged
            )
        } catch (e: Exception) {
            BatteryInfo()
        }
    }

    /**
     * 获取网络统计
     */
    private fun getNetworkStats(): NetworkStats {
        // 这里可以集成更详细的网络统计
        // 暂时返回空实现
        return NetworkStats()
    }

    /**
     * 更新内存信息
     */
    private fun updateMemoryInfo() {
        try {
            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            totalMemory = Runtime.getRuntime().totalMemory()
            availableMemory = Runtime.getRuntime().freeMemory()
        } catch (e: Exception) {
            // 忽略异常
        }
    }

    /**
     * 获取设备温度
     */
    fun getDeviceTemperature(): Float {
        return try {
            // 尝试读取CPU温度
            val cpuTempFile = File("/sys/class/thermal/thermal_zone0/temp")
            if (cpuTempFile.exists()) {
                val temp = BufferedReader(FileReader(cpuTempFile)).use { it.readLine().toFloat() }
                temp / 1000f // 转换为摄氏度
            } else {
                0f
            }
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * 获取存储使用情况
     */
    fun getStorageInfo(): StorageInfo {
        return try {
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes
            val availableBytes = stat.availableBytes
            val usedBytes = totalBytes - availableBytes
            val usagePercent = if (totalBytes > 0) {
                (usedBytes.toFloat() / totalBytes.toFloat() * 100f).roundToInt()
            } else {
                0
            }

            StorageInfo(totalBytes, availableBytes, usedBytes, usagePercent)
        } catch (e: Exception) {
            StorageInfo()
        }
    }
}

/**
 * 性能数据类
 */
data class PerformanceData(
    val cpuUsage: Float = 0f,
    val memoryUsage: MemoryInfo = MemoryInfo(),
    val batteryInfo: BatteryInfo = BatteryInfo(),
    val networkStats: NetworkStats = NetworkStats(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 内存信息类
 */
data class MemoryInfo(
    val total: Long = 0L,
    val available: Long = 0L,
    val used: Long = 0L,
    val usagePercent: Int = 0
) {
    fun formatTotalMemory(): String {
        return formatBytes(total)
    }

    fun formatUsedMemory(): String {
        return formatBytes(used)
    }

    fun formatAvailableMemory(): String {
        return formatBytes(available)
    }

    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0

        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }

        return String.format("%.1f %s", value, units[unitIndex])
    }
}

/**
 * 电池信息类
 */
data class BatteryInfo(
    val level: Int = 0,
    val temperature: Float = 0f,
    val voltage: Float = 0f,
    val isCharging: Boolean = false,
    val isPlugged: Boolean = false
)

/**
 * 网络统计类
 */
data class NetworkStats(
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val rxPackets: Long = 0L,
    val txPackets: Long = 0L
)

/**
 * 存储信息类
 */
data class StorageInfo(
    val total: Long = 0L,
    val available: Long = 0L,
    val used: Long = 0L,
    val usagePercent: Int = 0
) {
    fun formatTotalStorage(): String {
        return formatBytes(total)
    }

    fun formatUsedStorage(): String {
        return formatBytes(used)
    }

    fun formatAvailableStorage(): String {
        return formatBytes(available)
    }

    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0

        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }

        return String.format("%.1f %s", value, units[unitIndex])
    }
}
