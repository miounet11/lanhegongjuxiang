package com.lanhe.gongjuxiang.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
import android.system.Os
import android.system.OsConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import kotlin.math.roundToInt

/**
 * 性能监控工具类
 * 提供CPU、内存、存储等系统性能指标的实时监控
 */
class PerformanceMonitor(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    /**
     * CPU使用率数据类
     */
    data class CpuUsage(
        val totalUsage: Float,      // 总CPU使用率
        val userUsage: Float,       // 用户空间使用率
        val systemUsage: Float,     // 系统空间使用率
        val cores: Int             // CPU核心数
    )

    /**
     * 内存信息数据类
     */
    data class MemoryInfo(
        val totalMemory: Long,      // 总内存 (MB)
        val availableMemory: Long,  // 可用内存 (MB)
        val usedMemory: Long,       // 已用内存 (MB)
        val usagePercent: Float     // 使用率百分比
    )

    /**
     * 获取CPU使用率
     */
    suspend fun getCpuUsage(): CpuUsage = withContext(Dispatchers.IO) {
        try {
            val cpuInfo = readCpuInfo()
            val cores = Runtime.getRuntime().availableProcessors()

            CpuUsage(
                totalUsage = cpuInfo.total.toFloat(),
                userUsage = cpuInfo.user.toFloat(),
                systemUsage = cpuInfo.system.toFloat(),
                cores = cores
            )
        } catch (e: Exception) {
            CpuUsage(0f, 0f, 0f, Runtime.getRuntime().availableProcessors())
        }
    }

    /**
     * 获取内存信息
     */
    fun getMemoryInfo(): MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalMemory = memoryInfo.totalMem / (1024 * 1024) // MB
        val availableMemory = memoryInfo.availMem / (1024 * 1024) // MB
        val usedMemory = totalMemory - availableMemory
        val usagePercent = (usedMemory.toFloat() / totalMemory.toFloat()) * 100

        return MemoryInfo(
            totalMemory = totalMemory,
            availableMemory = availableMemory,
            usedMemory = usedMemory,
            usagePercent = usagePercent
        )
    }

    /**
     * 获取应用内存使用情况
     */
    fun getAppMemoryUsage(): Long {
        val pid = Process.myPid()
        val memoryInfo = activityManager.getProcessMemoryInfo(intArrayOf(pid))
        return memoryInfo[0].totalPss.toLong() // KB
    }

    /**
     * 获取存储空间信息
     */
    fun getStorageInfo(): StorageInfo {
        val stat = android.os.StatFs(context.filesDir.absolutePath)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalSpace = totalBlocks * blockSize / (1024 * 1024 * 1024) // GB
        val availableSpace = availableBlocks * blockSize / (1024 * 1024 * 1024) // GB
        val usedSpace = totalSpace - availableSpace
        val usagePercent = (usedSpace.toFloat() / totalSpace.toFloat()) * 100

        return StorageInfo(
            totalSpace = totalSpace,
            availableSpace = availableSpace,
            usedSpace = usedSpace,
            usagePercent = usagePercent
        )
    }

    /**
     * 读取CPU信息
     */
    private fun readCpuInfo(): CpuStats {
        var reader: BufferedReader? = null
        try {
            reader = BufferedReader(FileReader("/proc/stat"))
            val line = reader.readLine()
            if (line != null) {
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 8) {
                    val user = parts[1].toLong()
                    val nice = parts[2].toLong()
                    val system = parts[3].toLong()
                    val idle = parts[4].toLong()
                    val iowait = parts[5].toLong()
                    val irq = parts[6].toLong()
                    val softirq = parts[7].toLong()

                    val total = user + nice + system + idle + iowait + irq + softirq
                    val active = total - idle

                    return CpuStats(
                        total = total,
                        active = active,
                        user = user,
                        system = system
                    )
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            reader?.close()
        }
        return CpuStats(0, 0, 0, 0)
    }

    /**
     * CPU统计数据类
     */
    private data class CpuStats(
        val total: Long,
        val active: Long,
        val user: Long,
        val system: Long
    )

    /**
     * 存储信息数据类
     */
    data class StorageInfo(
        val totalSpace: Long,       // 总空间 (GB)
        val availableSpace: Long,   // 可用空间 (GB)
        val usedSpace: Long,        // 已用空间 (GB)
        val usagePercent: Float     // 使用率百分比
    )

    /**
     * 获取设备基本信息
     */
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            model = Build.MODEL,
            brand = Build.BRAND,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            totalMemory = getTotalMemory()
        )
    }

    /**
     * 获取总内存大小
     */
    private fun getTotalMemory(): Long {
        return try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.totalMem / (1024 * 1024) // MB
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 设备信息数据类
     */
    data class DeviceInfo(
        val model: String,
        val brand: String,
        val androidVersion: String,
        val apiLevel: Int,
        val cpuCores: Int,
        val totalMemory: Long // MB
    )
}
