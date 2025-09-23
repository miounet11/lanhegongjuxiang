package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.lanhe.gongjuxiang.utils.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

/**
 * 系统日志查看器
 * 提供Android系统日志的查看和分析功能
 */
class SystemLogViewer(private val context: Context) {

    // ShizukuManager 是单例对象，无需创建实例
    // private val shizukuManager = ShizukuManager(context)

    /**
     * 日志条目数据类
     */
    data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val tag: String,
        val pid: Int,
        val tid: Int,
        val message: String,
        val formattedTime: String
    )

    /**
     * 日志级别枚举
     */
    enum class LogLevel(val symbol: String, val priority: Int) {
        VERBOSE("V", 2),
        DEBUG("D", 3),
        INFO("I", 4),
        WARNING("W", 5),
        ERROR("E", 6),
        FATAL("F", 7),
        SILENT("S", 8)
    }

    /**
     * 日志过滤器
     */
    data class LogFilter(
        val minLevel: LogLevel = LogLevel.VERBOSE,
        val includeTags: List<String> = emptyList(),
        val excludeTags: List<String> = emptyList(),
        val includeText: String = "",
        val maxLines: Int = 1000
    )

    /**
     * 日志统计信息
     */
    data class LogStatistics(
        val totalEntries: Int,
        val entriesByLevel: Map<LogLevel, Int>,
        val topTags: List<Pair<String, Int>>,
        val timeRange: Pair<Long, Long>
    )

    /**
     * 获取系统日志
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getSystemLogs(filter: LogFilter = LogFilter()): List<LogEntry> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<LogEntry>()

        try {
            if (!ShizukuManager.isShizukuAvailable()) {
                return@withContext logs
            }

            // 使用logcat命令获取日志
            val command = buildLogcatCommand(filter)
            val result = ShizukuManager.executeCommand(command)

            if (result.isSuccess && result.output != null) {
                val lines = result.output.lines()
                for (line in lines) {
                    if (line.isNotBlank()) {
                        parseLogLine(line)?.let { entry ->
                            if (matchesFilter(entry, filter)) {
                                logs.add(entry)
                                if (logs.size >= filter.maxLines) return@withContext logs
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 处理异常
        }

        logs.sortedByDescending { it.timestamp }
    }

    /**
     * 实时监控日志
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun monitorLogs(
        filter: LogFilter = LogFilter(),
        onNewLog: (LogEntry) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            if (!ShizukuManager.isShizukuAvailable()) {
                return@withContext
            }

            // 使用logcat -v time -T 1 进行实时监控
            val command = "logcat -v time -T 1"
            val result = ShizukuManager.executeCommand(command)

            if (result.isSuccess && result.output != null) {
                val lines = result.output.lines()
                for (line in lines) {
                    if (line.isNotBlank()) {
                        parseLogLine(line)?.let { entry ->
                            if (matchesFilter(entry, filter)) {
                                onNewLog(entry)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 处理异常
        }
    }

    /**
     * 清除系统日志
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun clearSystemLogs(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!ShizukuManager.isShizukuAvailable()) {
                return@withContext false
            }

            val result = ShizukuManager.executeCommand("logcat -c")
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取日志统计信息
     */
    suspend fun getLogStatistics(logs: List<LogEntry>): LogStatistics {
        val entriesByLevel = logs.groupBy { it.level }.mapValues { it.value.size }
        val tagCount = logs.groupBy { it.tag }.mapValues { it.value.size }
        val topTags = tagCount.entries.sortedByDescending { it.value }.take(10).map { it.key to it.value }

        val timestamps = logs.map { it.timestamp }
        val timeRange = if (timestamps.isNotEmpty()) {
            timestamps.min() to timestamps.max()
        } else {
            0L to 0L
        }

        return LogStatistics(
            totalEntries = logs.size,
            entriesByLevel = entriesByLevel,
            topTags = topTags,
            timeRange = timeRange
        )
    }

    /**
     * 导出日志到文件
     */
    suspend fun exportLogsToFile(logs: List<LogEntry>, filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = buildString {
                appendLine("Android System Logs Export")
                appendLine("Export Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine("Total Entries: ${logs.size}")
                appendLine("=" .repeat(80))

                for (log in logs) {
                    appendLine("[${log.formattedTime}] ${log.level.symbol}/${log.tag}(${log.pid}): ${log.message}")
                }
            }

            java.io.File(filePath).writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 搜索日志
     */
    fun searchLogs(logs: List<LogEntry>, query: String, caseSensitive: Boolean = false): List<LogEntry> {
        val searchQuery = if (caseSensitive) query else query.lowercase()
        return logs.filter { log ->
            val searchableText = if (caseSensitive) {
                "${log.tag} ${log.message}"
            } else {
                "${log.tag} ${log.message}".lowercase()
            }
            searchableText.contains(searchQuery)
        }
    }

    /**
     * 按时间范围过滤日志
     */
    fun filterLogsByTimeRange(logs: List<LogEntry>, startTime: Long, endTime: Long): List<LogEntry> {
        return logs.filter { log ->
            log.timestamp in startTime..endTime
        }
    }

    /**
     * 构建logcat命令
     */
    private fun buildLogcatCommand(filter: LogFilter): String {
        val bufferSize = "1000" // 缓冲区大小
        val format = "time" // 时间格式

        var command = "logcat -v $format -t $bufferSize"

        // 添加级别过滤
        val levelPriority = filter.minLevel.priority
        val levelFilter = when (filter.minLevel) {
            LogLevel.VERBOSE -> "*"
            LogLevel.DEBUG -> "D"
            LogLevel.INFO -> "I"
            LogLevel.WARNING -> "W"
            LogLevel.ERROR -> "E"
            LogLevel.FATAL -> "F"
            LogLevel.SILENT -> "S"
        }
        command += " *:$levelFilter"

        return command
    }

    /**
     * 解析日志行
     */
    private fun parseLogLine(line: String): LogEntry? {
        try {
            // 日志格式: 01-15 08:30:45.123  1234  5678 I TagName: message
            val pattern = Regex("""(\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+(\w)\s+(.+?):\s*(.+)""")
            val match = pattern.find(line) ?: return null

            val (timeStr, pidStr, tidStr, levelChar, tag, message) = match.destructured

            val level = when (levelChar[0]) {
                'V' -> LogLevel.VERBOSE
                'D' -> LogLevel.DEBUG
                'I' -> LogLevel.INFO
                'W' -> LogLevel.WARNING
                'E' -> LogLevel.ERROR
                'F' -> LogLevel.FATAL
                'S' -> LogLevel.SILENT
                else -> LogLevel.INFO
            }

            // 解析时间戳
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val dateStr = "$currentYear-$timeStr"
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            val timestamp = sdf.parse(dateStr)?.time ?: System.currentTimeMillis()

            return LogEntry(
                timestamp = timestamp,
                level = level,
                tag = tag.trim(),
                pid = pidStr.toIntOrNull() ?: 0,
                tid = tidStr.toIntOrNull() ?: 0,
                message = message.trim(),
                formattedTime = timeStr
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 检查是否匹配过滤器
     */
    private fun matchesFilter(entry: LogEntry, filter: LogFilter): Boolean {
        // 检查级别
        if (entry.level.priority < filter.minLevel.priority) return false

        // 检查包含标签
        if (filter.includeTags.isNotEmpty() && !filter.includeTags.any { tag ->
            entry.tag.contains(tag, ignoreCase = true)
        }) return false

        // 检查排除标签
        if (filter.excludeTags.any { tag ->
            entry.tag.contains(tag, ignoreCase = true)
        }) return false

        // 检查包含文本
        if (filter.includeText.isNotEmpty() &&
            !entry.message.contains(filter.includeText, ignoreCase = true) &&
            !entry.tag.contains(filter.includeText, ignoreCase = true)) return false

        return true
    }

    /**
     * 获取常用日志标签
     */
    fun getCommonLogTags(): List<String> {
        return listOf(
            "ActivityManager",
            "PackageManager",
            "WindowManager",
            "InputDispatcher",
            "SystemServer",
            "BatteryService",
            "NetworkStats",
            "WifiStateMachine",
            "BluetoothAdapter",
            "AudioService",
            "PowerManagerService",
            "AlarmManager",
            "JobScheduler",
            "NotificationManager",
            "Telephony",
            "LocationManager"
        )
    }

    /**
     * 获取日志级别描述
     */
    fun getLogLevelDescription(level: LogLevel): String {
        return when (level) {
            LogLevel.VERBOSE -> "详细 - 最详细的日志信息"
            LogLevel.DEBUG -> "调试 - 调试信息"
            LogLevel.INFO -> "信息 - 一般信息"
            LogLevel.WARNING -> "警告 - 警告信息"
            LogLevel.ERROR -> "错误 - 错误信息"
            LogLevel.FATAL -> "致命 - 严重错误"
            LogLevel.SILENT -> "静默 - 最高级别，不显示任何日志"
        }
    }
}
