package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

/**
 * 系统诊断器
 * 负责系统健康检查和问题诊断
 */
class SystemDiagnostic(private val context: Context) {

    private val performanceMonitor = PerformanceMonitor(context)
    private val appFreezeManager = AppFreezeManager(context)

    /**
     * 执行全面系统诊断
     */
    suspend fun performFullDiagnostic(): DiagnosticResult {
        return withContext(Dispatchers.IO) {
            val issues = mutableListOf<DiagnosticIssue>()
            val recommendations = mutableListOf<String>()
            val healthScore = calculateHealthScore()

            // 检查内存使用情况
            val memoryIssue = checkMemoryUsage()
            if (memoryIssue != null) {
                issues.add(memoryIssue)
                recommendations.add("建议清理内存，关闭不必要的后台应用")
            }

            // 检查存储空间
            val storageIssue = checkStorageSpace()
            if (storageIssue != null) {
                issues.add(storageIssue)
                recommendations.add("建议清理存储空间，删除不需要的文件")
            }

            // 检查电池健康
            val batteryIssue = checkBatteryHealth()
            if (batteryIssue != null) {
                issues.add(batteryIssue)
                recommendations.add("建议优化电池使用，启用省电模式")
            }

            // 检查CPU温度
            val temperatureIssue = checkCpuTemperature()
            if (temperatureIssue != null) {
                issues.add(temperatureIssue)
                recommendations.add("建议降低CPU负载，关闭高性能模式")
            }

            // 检查应用状态
            val appIssues = checkAppStatus()
            issues.addAll(appIssues)
            if (appIssues.isNotEmpty()) {
                recommendations.add("建议清理或冻结不常用的应用")
            }

            // 检查系统稳定性
            val stabilityIssue = checkSystemStability()
            if (stabilityIssue != null) {
                issues.add(stabilityIssue)
                recommendations.add("建议重启设备或进行系统优化")
            }

            DiagnosticResult(
                healthScore = healthScore,
                issues = issues,
                recommendations = recommendations,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * 检查内存使用情况
     */
    private suspend fun checkMemoryUsage(): DiagnosticIssue? {
        return withContext(Dispatchers.IO) {
            try {
                val memInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memInfo)
                
                val totalMemory = Runtime.getRuntime().totalMemory()
                val freeMemory = Runtime.getRuntime().freeMemory()
                val usedMemory = totalMemory - freeMemory
                val usagePercent = (usedMemory.toFloat() / totalMemory.toFloat() * 100f).toInt()

                when {
                    usagePercent > 90 -> {
                        DiagnosticIssue(
                            type = IssueType.MEMORY,
                            severity = IssueSeverity.HIGH,
                            title = "内存使用率过高",
                            description = "当前内存使用率: ${usagePercent}%，可能导致系统卡顿",
                            suggestion = "建议立即清理内存"
                        )
                    }
                    usagePercent > 80 -> {
                        DiagnosticIssue(
                            type = IssueType.MEMORY,
                            severity = IssueSeverity.MEDIUM,
                            title = "内存使用率较高",
                            description = "当前内存使用率: ${usagePercent}%，建议关注",
                            suggestion = "建议清理内存以提升性能"
                        )
                    }
                    else -> null
                }
            } catch (e: Exception) {
                Log.e("SystemDiagnostic", "检查内存使用失败", e)
                null
            }
        }
    }

    /**
     * 检查存储空间
     */
    private suspend fun checkStorageSpace(): DiagnosticIssue? {
        return withContext(Dispatchers.IO) {
            try {
                val stat = StatFs(android.os.Environment.getDataDirectory().path)
                val totalBytes = stat.totalBytes
                val availableBytes = stat.availableBytes
                val usedBytes = totalBytes - availableBytes
                val usagePercent = (usedBytes.toFloat() / totalBytes.toFloat() * 100f).toInt()

                when {
                    usagePercent > 95 -> {
                        DiagnosticIssue(
                            type = IssueType.STORAGE,
                            severity = IssueSeverity.CRITICAL,
                            title = "存储空间严重不足",
                            description = "存储使用率: ${usagePercent}%，可用空间: ${formatBytes(availableBytes)}",
                            suggestion = "立即清理存储空间"
                        )
                    }
                    usagePercent > 85 -> {
                        DiagnosticIssue(
                            type = IssueType.STORAGE,
                            severity = IssueSeverity.HIGH,
                            title = "存储空间不足",
                            description = "存储使用率: ${usagePercent}%，可用空间: ${formatBytes(availableBytes)}",
                            suggestion = "建议清理存储空间"
                        )
                    }
                    usagePercent > 75 -> {
                        DiagnosticIssue(
                            type = IssueType.STORAGE,
                            severity = IssueSeverity.MEDIUM,
                            title = "存储空间紧张",
                            description = "存储使用率: ${usagePercent}%，可用空间: ${formatBytes(availableBytes)}",
                            suggestion = "建议清理不必要的文件"
                        )
                    }
                    else -> null
                }
            } catch (e: Exception) {
                Log.e("SystemDiagnostic", "检查存储空间失败", e)
                null
            }
        }
    }

    /**
     * 检查电池健康
     */
    private suspend fun checkBatteryHealth(): DiagnosticIssue? {
        return withContext(Dispatchers.IO) {
            try {
                val batteryInfo = performanceMonitor.getBatteryInfo()
                
                when {
                    batteryInfo.temperature > 45f -> {
                        DiagnosticIssue(
                            type = IssueType.BATTERY,
                            severity = IssueSeverity.HIGH,
                            title = "电池温度过高",
                            description = "当前电池温度: ${batteryInfo.temperature}°C",
                            suggestion = "建议停止充电，让设备降温"
                        )
                    }
                    batteryInfo.level < 20 && !batteryInfo.isCharging -> {
                        DiagnosticIssue(
                            type = IssueType.BATTERY,
                            severity = IssueSeverity.MEDIUM,
                            title = "电池电量低",
                            description = "当前电量: ${batteryInfo.level}%",
                            suggestion = "建议充电或启用省电模式"
                        )
                    }
                    else -> null
                }
            } catch (e: Exception) {
                Log.e("SystemDiagnostic", "检查电池健康失败", e)
                null
            }
        }
    }

    /**
     * 检查CPU温度
     */
    private suspend fun checkCpuTemperature(): DiagnosticIssue? {
        return withContext(Dispatchers.IO) {
            try {
                val temperature = performanceMonitor.getDeviceTemperature()
                
                when {
                    temperature > 70f -> {
                        DiagnosticIssue(
                            type = IssueType.TEMPERATURE,
                            severity = IssueSeverity.CRITICAL,
                            title = "CPU温度过高",
                            description = "当前CPU温度: ${temperature}°C",
                            suggestion = "立即停止高负载任务，让设备降温"
                        )
                    }
                    temperature > 60f -> {
                        DiagnosticIssue(
                            type = IssueType.TEMPERATURE,
                            severity = IssueSeverity.HIGH,
                            title = "CPU温度较高",
                            description = "当前CPU温度: ${temperature}°C",
                            suggestion = "建议降低CPU负载"
                        )
                    }
                    temperature > 50f -> {
                        DiagnosticIssue(
                            type = IssueType.TEMPERATURE,
                            severity = IssueSeverity.MEDIUM,
                            title = "CPU温度偏高",
                            description = "当前CPU温度: ${temperature}°C",
                            suggestion = "建议关注设备温度"
                        )
                    }
                    else -> null
                }
            } catch (e: Exception) {
                Log.e("SystemDiagnostic", "检查CPU温度失败", e)
                null
            }
        }
    }

    /**
     * 检查应用状态
     */
    private suspend fun checkAppStatus(): List<DiagnosticIssue> {
        return withContext(Dispatchers.IO) {
            val issues = mutableListOf<DiagnosticIssue>()
            
            try {
                val packageManager = context.packageManager
                val packages = packageManager.getInstalledPackages(0)
                
                // 检查是否有大量应用
                if (packages.size > 200) {
                    issues.add(
                        DiagnosticIssue(
                            type = IssueType.APPS,
                            severity = IssueSeverity.MEDIUM,
                            title = "应用数量过多",
                            description = "已安装${packages.size}个应用，可能影响性能",
                            suggestion = "建议卸载不常用的应用"
                        )
                    )
                }
                
                // 检查是否有异常应用
                val suspiciousApps = packages.filter { packageInfo ->
                    val appInfo = packageInfo.applicationInfo
                    !isSystemApp(appInfo) && 
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                }
                
                if (suspiciousApps.isNotEmpty()) {
                    issues.add(
                        DiagnosticIssue(
                            type = IssueType.SECURITY,
                            severity = IssueSeverity.MEDIUM,
                            title = "发现调试应用",
                            description = "发现${suspiciousApps.size}个调试模式应用",
                            suggestion = "建议检查应用安全性"
                        )
                    )
                }
                
            } catch (e: Exception) {
                Log.e("SystemDiagnostic", "检查应用状态失败", e)
            }
            
            issues
        }
    }

    /**
     * 检查系统稳定性
     */
    private suspend fun checkSystemStability(): DiagnosticIssue? {
        return withContext(Dispatchers.IO) {
            try {
                // 检查系统运行时间
                val uptime = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
                val uptimeHours = uptime / (1000 * 60 * 60)
                
                if (uptimeHours > 168) { // 7天
                    return@withContext DiagnosticIssue(
                        type = IssueType.STABILITY,
                        severity = IssueSeverity.MEDIUM,
                        title = "系统运行时间过长",
                        description = "系统已运行${uptimeHours}小时，建议重启",
                        suggestion = "建议重启设备以提升稳定性"
                    )
                }
                
                // 检查内存泄漏
                val memInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memInfo)
                
                if (memInfo.totalPss > 2 * 1024 * 1024 * 1024) { // 2GB
                    return@withContext DiagnosticIssue(
                        type = IssueType.MEMORY,
                        severity = IssueSeverity.MEDIUM,
                        title = "可能存在内存泄漏",
                        description = "系统内存使用异常，总PSS: ${formatBytes(memInfo.totalPss.toLong())}",
                        suggestion = "建议重启设备或进行内存清理"
                    )
                }
                
                null
            } catch (e: Exception) {
                Log.e("SystemDiagnostic", "检查系统稳定性失败", e)
                null
            }
        }
    }

    /**
     * 计算系统健康分数
     */
    private fun calculateHealthScore(): Int {
        var score = 100
        
        try {
            // 内存使用率影响
            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            val totalMemory = Runtime.getRuntime().totalMemory()
            val freeMemory = Runtime.getRuntime().freeMemory()
            val usagePercent = ((totalMemory - freeMemory).toFloat() / totalMemory.toFloat() * 100f).toInt()
            
            when {
                usagePercent > 90 -> score -= 30
                usagePercent > 80 -> score -= 20
                usagePercent > 70 -> score -= 10
            }
            
            // 存储空间影响
            val stat = StatFs(android.os.Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes
            val availableBytes = stat.availableBytes
            val storageUsagePercent = ((totalBytes - availableBytes).toFloat() / totalBytes.toFloat() * 100f).toInt()
            
            when {
                storageUsagePercent > 95 -> score -= 25
                storageUsagePercent > 85 -> score -= 15
                storageUsagePercent > 75 -> score -= 10
            }
            
            // 温度影响
            val temperature = performanceMonitor.getDeviceTemperature()
            when {
                temperature > 70f -> score -= 20
                temperature > 60f -> score -= 15
                temperature > 50f -> score -= 10
            }
            
        } catch (e: Exception) {
            Log.e("SystemDiagnostic", "计算健康分数失败", e)
        }
        
        return score.coerceIn(0, 100)
    }

    /**
     * 判断是否为系统应用
     */
    private fun isSystemApp(appInfo: android.content.pm.ApplicationInfo): Boolean {
        return (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
               (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    /**
     * 格式化字节数
     */
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
 * 诊断结果类
 */
data class DiagnosticResult(
    val healthScore: Int,
    val issues: List<DiagnosticIssue>,
    val recommendations: List<String>,
    val timestamp: Long
) {
    val isHealthy: Boolean
        get() = healthScore >= 80 && issues.isEmpty()
    
    val criticalIssues: List<DiagnosticIssue>
        get() = issues.filter { it.severity == IssueSeverity.CRITICAL }
    
    val highPriorityIssues: List<DiagnosticIssue>
        get() = issues.filter { it.severity in listOf(IssueSeverity.CRITICAL, IssueSeverity.HIGH) }
}

/**
 * 诊断问题类
 */
data class DiagnosticIssue(
    val type: IssueType,
    val severity: IssueSeverity,
    val title: String,
    val description: String,
    val suggestion: String
)

/**
 * 问题类型枚举
 */
enum class IssueType {
    MEMORY,      // 内存问题
    STORAGE,     // 存储问题
    BATTERY,     // 电池问题
    TEMPERATURE, // 温度问题
    APPS,        // 应用问题
    SECURITY,    // 安全问题
    STABILITY    // 稳定性问题
}

/**
 * 问题严重程度枚举
 */
enum class IssueSeverity {
    LOW,      // 低
    MEDIUM,   // 中
    HIGH,     // 高
    CRITICAL  // 严重
}