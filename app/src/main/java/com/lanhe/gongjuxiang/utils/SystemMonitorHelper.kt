package com.lanhe.gongjuxiang.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.RandomAccessFile
import kotlin.math.roundToInt

/**
 * 系统监控帮助类
 * 提供真实的系统资源监控功能
 */
class SystemMonitorHelper(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val batteryManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    } else null

    /**
     * 获取CPU使用率
     */
    fun getCpuUsage(): Float {
        return try {
            val cpuInfo = getCpuInfo()
            val usage = calculateCpuUsage(cpuInfo)
            usage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            e.printStackTrace()
            0f
        }
    }

    /**
     * 获取CPU信息
     */
    private fun getCpuInfo(): CpuInfo {
        try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()

            val toks = load.split(" ").filter { it.isNotEmpty() }

            if (toks[0] == "cpu" && toks.size >= 5) {
                val user = toks[1].toLong()
                val nice = toks[2].toLong()
                val system = toks[3].toLong()
                val idle = toks[4].toLong()
                val iowait = if (toks.size > 5) toks[5].toLongOrNull() ?: 0L else 0L
                val irq = if (toks.size > 6) toks[6].toLongOrNull() ?: 0L else 0L
                val softirq = if (toks.size > 7) toks[7].toLongOrNull() ?: 0L else 0L

                val total = user + nice + system + idle + iowait + irq + softirq
                val active = total - idle - iowait

                return CpuInfo(total, active, idle)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return CpuInfo(100, 50, 50) // 默认50%使用率
    }

    /**
     * 计算CPU使用率
     */
    private fun calculateCpuUsage(currentInfo: CpuInfo): Float {
        val prevInfo = previousCpuInfo ?: currentInfo
        previousCpuInfo = currentInfo

        val totalDiff = currentInfo.total - prevInfo.total
        val activeDiff = currentInfo.active - prevInfo.active

        return if (totalDiff > 0) {
            (activeDiff.toFloat() / totalDiff.toFloat()) * 100
        } else {
            0f
        }
    }

    /**
     * 获取内存使用情况
     */
    fun getMemoryUsage(): MemoryUsage {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalMemory = memInfo.totalMem
        val availableMemory = memInfo.availMem
        val usedMemory = totalMemory - availableMemory
        val usagePercent = ((usedMemory.toFloat() / totalMemory.toFloat()) * 100).roundToInt()

        return MemoryUsage(
            total = totalMemory,
            available = availableMemory,
            used = usedMemory,
            percent = usagePercent,
            totalGB = String.format("%.1f", totalMemory / (1024.0 * 1024.0 * 1024.0)),
            usedGB = String.format("%.1f", usedMemory / (1024.0 * 1024.0 * 1024.0))
        )
    }

    /**
     * 获取电池信息
     */
    fun getBatteryInfo(): BatteryInfo {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = (level * 100 / scale.toFloat()).roundToInt()

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

        val temperature = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f

        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        // 获取充电类型
        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val chargingType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC充电"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB充电"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线充电"
            else -> "未充电"
        }

        return BatteryInfo(
            level = batteryPct,
            isCharging = isCharging,
            temperature = temperature,
            voltage = voltage,
            chargingType = chargingType,
            health = getBatteryHealth(batteryStatus)
        )
    }

    /**
     * 获取电池健康状态
     */
    private fun getBatteryHealth(batteryStatus: Intent?): String {
        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
            BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
            BatteryManager.BATTERY_HEALTH_DEAD -> "损坏"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "电压过高"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "未知错误"
            else -> "未知"
        }
    }

    /**
     * 获取存储使用情况
     */
    fun getStorageUsage(): StorageUsage {
        val stat = StatFs(Environment.getDataDirectory().path)

        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalStorage = totalBlocks * blockSize
        val availableStorage = availableBlocks * blockSize
        val usedStorage = totalStorage - availableStorage
        val usagePercent = ((usedStorage.toFloat() / totalStorage.toFloat()) * 100).roundToInt()

        return StorageUsage(
            total = totalStorage,
            available = availableStorage,
            used = usedStorage,
            percent = usagePercent,
            totalGB = String.format("%.1f", totalStorage / (1024.0 * 1024.0 * 1024.0)),
            availableGB = String.format("%.1f", availableStorage / (1024.0 * 1024.0 * 1024.0))
        )
    }

    /**
     * 获取系统运行时间
     */
    fun getSystemUptime(): String {
        val uptimeMillis = SystemClock.uptimeMillis()
        val hours = uptimeMillis / (1000 * 60 * 60)
        val minutes = (uptimeMillis % (1000 * 60 * 60)) / (1000 * 60)
        return "${hours}小时${minutes}分钟"
    }

    /**
     * 获取设备温度
     */
    fun getDeviceTemperature(): Float {
        return try {
            // 尝试读取CPU温度
            val thermalFiles = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
                "/sys/kernel/debug/tegra_thermal/temp_tj"
            )

            for (file in thermalFiles) {
                val tempFile = File(file)
                if (tempFile.exists() && tempFile.canRead()) {
                    val temp = tempFile.readText().trim()
                    val temperature = temp.toFloatOrNull() ?: continue
                    // 某些设备返回的是毫摄氏度
                    return if (temperature > 1000) {
                        temperature / 1000f
                    } else {
                        temperature
                    }
                }
            }

            // 如果无法读取，返回电池温度作为备选
            getBatteryInfo().temperature
        } catch (e: Exception) {
            e.printStackTrace()
            25.0f // 默认室温
        }
    }

    /**
     * 获取运行中的应用数量
     */
    fun getRunningAppsCount(): Int {
        return try {
            val runningAppProcesses = activityManager.runningAppProcesses
            runningAppProcesses?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 获取系统负载
     */
    fun getSystemLoad(): SystemLoad {
        return try {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("uptime")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val uptimeOutput = reader.readLine()
            reader.close()

            // 解析uptime输出，格式类似：
            // 14:37:10 up 2 days, 4:23, load average: 0.52, 0.58, 0.59
            if (uptimeOutput != null && uptimeOutput.contains("load average:")) {
                val loadPart = uptimeOutput.substringAfter("load average:").trim()
                val loads = loadPart.split(",").map { it.trim().toFloatOrNull() ?: 0f }

                SystemLoad(
                    load1Min = loads.getOrNull(0) ?: 0f,
                    load5Min = loads.getOrNull(1) ?: 0f,
                    load15Min = loads.getOrNull(2) ?: 0f
                )
            } else {
                SystemLoad(0f, 0f, 0f)
            }
        } catch (e: Exception) {
            SystemLoad(0f, 0f, 0f)
        }
    }

    /**
     * 获取可用的CPU核心数
     */
    fun getCpuCores(): Int {
        return try {
            val dir = File("/sys/devices/system/cpu/")
            val files = dir.listFiles { file ->
                file.name.matches(Regex("cpu[0-9]+"))
            }
            files?.size ?: Runtime.getRuntime().availableProcessors()
        } catch (e: Exception) {
            Runtime.getRuntime().availableProcessors()
        }
    }

    // 数据类定义
    data class CpuInfo(
        val total: Long,
        val active: Long,
        val idle: Long
    )

    data class MemoryUsage(
        val total: Long,
        val available: Long,
        val used: Long,
        val percent: Int,
        val totalGB: String,
        val usedGB: String
    )

    data class BatteryInfo(
        val level: Int,
        val isCharging: Boolean,
        val temperature: Float,
        val voltage: Int,
        val chargingType: String,
        val health: String
    )

    data class StorageUsage(
        val total: Long,
        val available: Long,
        val used: Long,
        val percent: Int,
        val totalGB: String,
        val availableGB: String
    )

    data class SystemLoad(
        val load1Min: Float,
        val load5Min: Float,
        val load15Min: Float
    )

    companion object {
        private var previousCpuInfo: CpuInfo? = null
    }
}