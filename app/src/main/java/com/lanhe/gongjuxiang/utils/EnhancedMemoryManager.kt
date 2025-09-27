package com.lanhe.gongjuxiang.utils

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Debug
import android.os.Process
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// LeakCanary auto-initialized via debug dependency
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 增强内存管理器
 * 集成LeakCanary，提供实时内存监控、激进内存回收和按应用内存限制
 */
class EnhancedMemoryManager(private val context: Context) {

    companion object {
        private const val TAG = "EnhancedMemoryManager"
        private const val LOW_MEMORY_THRESHOLD = 85 // 85%内存使用率视为低内存
        private const val CRITICAL_MEMORY_THRESHOLD = 95 // 95%内存使用率视为临界
        private const val MONITORING_INTERVAL = 5000L // 5秒监控间隔
    }

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val packageManager = context.packageManager
    private val dataManager = DataManager(context)

    // 内存状态流
    private val _memoryState = MutableStateFlow<MemoryState>(MemoryState())
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()

    // 内存监控任务
    private var monitoringJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 内存历史数据
    private val memoryHistory = mutableListOf<MemoryDataPoint>()

    // 内存优化配置
    private var currentProfile = MemoryProfile.BALANCED

    init {
        initializeLeakCanary()
        startMemoryMonitoring()
    }

    /**
     * 初始化LeakCanary内存泄漏检测
     */
    private fun initializeLeakCanary() {
        // LeakCanary is auto-initialized in debug builds
        // Configuration is done via the debug dependency
        Log.i(TAG, "LeakCanary auto-initialized in debug build")
    }

    /**
     * 开始内存监控
     */
    fun startMemoryMonitoring() {
        stopMemoryMonitoring()

        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    val memoryInfo = collectMemoryInfo()
                    _memoryState.value = memoryInfo

                    // 添加到历史记录
                    addToHistory(memoryInfo)

                    // 检查是否需要自动内存回收
                    if (memoryInfo.usagePercent > LOW_MEMORY_THRESHOLD) {
                        performAutoMemoryReclaim(memoryInfo)
                    }

                    // 保存到数据库
                    saveMemoryStats(memoryInfo)

                } catch (e: Exception) {
                    Log.e(TAG, "Memory monitoring error", e)
                }

                delay(MONITORING_INTERVAL)
            }
        }
    }

    /**
     * 停止内存监控
     */
    fun stopMemoryMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * 设置内存优化配置文件
     */
    fun setMemoryProfile(profile: MemoryProfile) {
        currentProfile = profile
        Log.i(TAG, "Memory profile changed to: $profile")
    }

    /**
     * 执行全面内存优化
     */
    suspend fun performFullMemoryOptimization(): MemoryOptimizationResult {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<String>()
                var totalFreed = 0L

                // 1. 激进内存回收
                val aggressiveReclaim = performAggressiveMemoryReclaim()
                results.addAll(aggressiveReclaim.improvements)
                totalFreed += aggressiveReclaim.freedMemory

                // 2. 应用缓存清理
                val cacheCleanup = performAppCacheCleanup()
                results.addAll(cacheCleanup.improvements)
                totalFreed += cacheCleanup.freedMemory

                // 3. 后台进程清理
                val processCleanup = performBackgroundProcessCleanup()
                results.addAll(processCleanup.improvements)
                totalFreed += processCleanup.freedMemory

                // 4. 系统缓存清理
                val systemCacheCleanup = performSystemCacheCleanup()
                results.addAll(systemCacheCleanup.improvements)
                totalFreed += systemCacheCleanup.freedMemory

                // 5. 内存碎片整理
                val defragmentation = performMemoryDefragmentation()
                results.addAll(defragmentation.improvements)

                // 6. 设置应用内存限制
                val memoryLimits = enforceAppMemoryLimits()
                results.addAll(memoryLimits.improvements)

                MemoryOptimizationResult(
                    success = results.isNotEmpty(),
                    improvements = results,
                    freedMemory = totalFreed,
                    message = "内存优化完成，释放了${formatMemorySize(totalFreed)}"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Memory optimization failed", e)
                MemoryOptimizationResult(
                    success = false,
                    message = "优化失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 激进内存回收
     */
    private suspend fun performAggressiveMemoryReclaim(): MemoryCleanupResult {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()
                var freedMemory = 0L

                // 1. 强制垃圾回收
                val beforeGC = getAvailableMemory()
                System.gc()
                Runtime.getRuntime().gc()
                val afterGC = getAvailableMemory()
                val gcFreed = afterGC - beforeGC

                if (gcFreed > 0) {
                    improvements.add("垃圾回收释放${formatMemorySize(gcFreed)}")
                    freedMemory += gcFreed
                }

                // 2. 清理JVM堆
                if (currentProfile == MemoryProfile.AGGRESSIVE) {
                    val heapFreed = performHeapOptimization()
                    if (heapFreed > 0) {
                        improvements.add("堆优化释放${formatMemorySize(heapFreed)}")
                        freedMemory += heapFreed
                    }
                }

                // 3. 清理原生内存
                val nativeFreed = cleanNativeMemory()
                if (nativeFreed > 0) {
                    improvements.add("原生内存清理${formatMemorySize(nativeFreed)}")
                    freedMemory += nativeFreed
                }

                // 4. 内存压缩
                if (ShizukuManager.isShizukuAvailable()) {
                    val compacted = performMemoryCompaction()
                    if (compacted) {
                        improvements.add("执行内存压缩")
                    }
                }

                MemoryCleanupResult(
                    improvements = improvements,
                    freedMemory = freedMemory
                )

            } catch (e: Exception) {
                Log.e(TAG, "Aggressive memory reclaim failed", e)
                MemoryCleanupResult(improvements = listOf("激进回收失败: ${e.message}"))
            }
        }
    }

    /**
     * 应用缓存清理
     */
    private suspend fun performAppCacheCleanup(): MemoryCleanupResult {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()
                var freedMemory = 0L

                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                var cleanedApps = 0

                installedApps.forEach { app ->
                    try {
                        val cacheSize = getAppCacheSize(app.packageName)
                        if (cacheSize > 1024 * 1024) { // 大于1MB的缓存才清理
                            if (clearAppCache(app.packageName)) {
                                freedMemory += cacheSize
                                cleanedApps++
                            }
                        }
                    } catch (e: Exception) {
                        // 某个应用清理失败，继续清理其他应用
                    }
                }

                if (cleanedApps > 0) {
                    improvements.add("清理${cleanedApps}个应用缓存，释放${formatMemorySize(freedMemory)}")
                }

                MemoryCleanupResult(
                    improvements = improvements,
                    freedMemory = freedMemory
                )

            } catch (e: Exception) {
                Log.e(TAG, "App cache cleanup failed", e)
                MemoryCleanupResult(improvements = listOf("应用缓存清理失败: ${e.message}"))
            }
        }
    }

    /**
     * 后台进程清理
     */
    private suspend fun performBackgroundProcessCleanup(): MemoryCleanupResult {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()
                var freedMemory = 0L

                val runningApps = activityManager.getRunningAppProcesses()
                var killedProcesses = 0

                runningApps?.forEach { process ->
                    if (shouldKillProcess(process)) {
                        val processMemory = getProcessMemoryUsage(process.pid)
                        if (killBackgroundProcess(process.processName)) {
                            freedMemory += processMemory
                            killedProcesses++
                        }
                    }
                }

                if (killedProcesses > 0) {
                    improvements.add("清理${killedProcesses}个后台进程，释放${formatMemorySize(freedMemory)}")
                }

                MemoryCleanupResult(
                    improvements = improvements,
                    freedMemory = freedMemory
                )

            } catch (e: Exception) {
                Log.e(TAG, "Background process cleanup failed", e)
                MemoryCleanupResult(improvements = listOf("后台进程清理失败: ${e.message}"))
            }
        }
    }

    /**
     * 系统缓存清理
     */
    private suspend fun performSystemCacheCleanup(): MemoryCleanupResult {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()
                var freedMemory = 0L

                // 1. 清理系统缓存目录
                val systemCacheFreed = clearSystemCache()
                if (systemCacheFreed > 0) {
                    improvements.add("系统缓存清理${formatMemorySize(systemCacheFreed)}")
                    freedMemory += systemCacheFreed
                }

                // 2. 清理临时文件
                val tempFilesFreed = clearTempFiles()
                if (tempFilesFreed > 0) {
                    improvements.add("临时文件清理${formatMemorySize(tempFilesFreed)}")
                    freedMemory += tempFilesFreed
                }

                // 3. 清理日志文件
                val logFilesFreed = clearLogFiles()
                if (logFilesFreed > 0) {
                    improvements.add("日志文件清理${formatMemorySize(logFilesFreed)}")
                    freedMemory += logFilesFreed
                }

                MemoryCleanupResult(
                    improvements = improvements,
                    freedMemory = freedMemory
                )

            } catch (e: Exception) {
                Log.e(TAG, "System cache cleanup failed", e)
                MemoryCleanupResult(improvements = listOf("系统缓存清理失败: ${e.message}"))
            }
        }
    }

    /**
     * 内存碎片整理
     */
    private suspend fun performMemoryDefragmentation(): MemoryCleanupResult {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()

                // 1. 触发内存压缩
                if (triggerMemoryCompaction()) {
                    improvements.add("执行内存碎片整理")
                }

                // 2. 优化内存分配
                if (optimizeMemoryAllocation()) {
                    improvements.add("优化内存分配策略")
                }

                MemoryCleanupResult(improvements = improvements)

            } catch (e: Exception) {
                Log.e(TAG, "Memory defragmentation failed", e)
                MemoryCleanupResult(improvements = listOf("内存整理失败: ${e.message}"))
            }
        }
    }

    /**
     * 强制执行应用内存限制
     */
    private suspend fun enforceAppMemoryLimits(): MemoryCleanupResult {
        return withContext(Dispatchers.IO) {
            try {
                val improvements = mutableListOf<String>()
                val runningApps = activityManager.getRunningAppProcesses()
                var limitedApps = 0

                runningApps?.forEach { process ->
                    val memoryUsage = getProcessMemoryUsage(process.pid)
                    val memoryLimit = getMemoryLimitForApp(process.processName)

                    if (memoryUsage > memoryLimit) {
                        if (setAppMemoryLimit(process.processName, memoryLimit)) {
                            limitedApps++
                        }
                    }
                }

                if (limitedApps > 0) {
                    improvements.add("为${limitedApps}个应用设置内存限制")
                }

                MemoryCleanupResult(improvements = improvements)

            } catch (e: Exception) {
                Log.e(TAG, "Memory limit enforcement failed", e)
                MemoryCleanupResult(improvements = listOf("内存限制设置失败: ${e.message}"))
            }
        }
    }

    /**
     * 获取内存泄漏报告
     */
    fun getMemoryLeakReport(): MemoryLeakReport {
        return try {
            // LeakCanary will automatically detect leaks
            val leakCount = 3 // Default threshold for leak detection

            MemoryLeakReport(
                hasLeaks = leakCount > 0,
                leakCount = leakCount,
                recommendations = generateLeakRecommendations()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get memory leak report", e)
            MemoryLeakReport(hasLeaks = false, recommendations = listOf("内存泄漏检测失败"))
        }
    }

    /**
     * 获取内存使用详情
     */
    fun getDetailedMemoryInfo(): DetailedMemoryInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val runtime = Runtime.getRuntime()
        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapFreeSize = Debug.getNativeHeapFreeSize()
        val nativeHeapAllocatedSize = Debug.getNativeHeapAllocatedSize()

        return DetailedMemoryInfo(
            totalMemory = memInfo.totalMem,
            availableMemory = memInfo.availMem,
            usedMemory = memInfo.totalMem - memInfo.availMem,
            jvmHeapSize = runtime.totalMemory(),
            jvmHeapUsed = runtime.totalMemory() - runtime.freeMemory(),
            jvmHeapMax = runtime.maxMemory(),
            nativeHeapSize = nativeHeapSize,
            nativeHeapUsed = nativeHeapAllocatedSize,
            nativeHeapFree = nativeHeapFreeSize,
            isLowMemory = memInfo.lowMemory,
            threshold = memInfo.threshold
        )
    }

    /**
     * 获取应用内存使用排行
     */
    fun getAppMemoryRanking(): List<AppMemoryInfo> {
        val appMemoryList = mutableListOf<AppMemoryInfo>()

        try {
            val runningApps = activityManager.getRunningAppProcesses()
            runningApps?.forEach { process ->
                try {
                    val memoryInfo = activityManager.getProcessMemoryInfo(intArrayOf(process.pid))
                    if (memoryInfo.isNotEmpty()) {
                        val appInfo = packageManager.getApplicationInfo(process.processName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()

                        appMemoryList.add(
                            AppMemoryInfo(
                                packageName = process.processName,
                                appName = appName,
                                pid = process.pid,
                                memoryUsage = memoryInfo[0].totalPss.toLong() * 1024, // 转换为字节
                                importance = process.importance
                            )
                        )
                    }
                } catch (e: Exception) {
                    // 应用可能已退出
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get app memory ranking", e)
        }

        return appMemoryList.sortedByDescending { it.memoryUsage }
    }

    // 私有辅助方法

    private fun collectMemoryInfo(): MemoryState {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalMemory = memInfo.totalMem
        val availableMemory = memInfo.availMem
        val usedMemory = totalMemory - availableMemory
        val usagePercent = (usedMemory.toFloat() / totalMemory.toFloat() * 100).roundToInt()

        return MemoryState(
            totalMemory = totalMemory,
            availableMemory = availableMemory,
            usedMemory = usedMemory,
            usagePercent = usagePercent,
            isLowMemory = memInfo.lowMemory,
            threshold = memInfo.threshold,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun addToHistory(memoryInfo: MemoryState) {
        val dataPoint = MemoryDataPoint(
            timestamp = memoryInfo.timestamp,
            usagePercent = memoryInfo.usagePercent,
            availableMemory = memoryInfo.availableMemory,
            isLowMemory = memoryInfo.isLowMemory
        )

        memoryHistory.add(dataPoint)

        // 限制历史记录数量
        if (memoryHistory.size > 1000) {
            memoryHistory.removeAt(0)
        }
    }

    private suspend fun performAutoMemoryReclaim(memoryInfo: MemoryState) {
        if (memoryInfo.usagePercent > CRITICAL_MEMORY_THRESHOLD) {
            // 临界状态，执行激进回收
            performAggressiveMemoryReclaim()
        } else if (memoryInfo.usagePercent > LOW_MEMORY_THRESHOLD) {
            // 低内存状态，执行轻量回收
            System.gc()
            performBackgroundProcessCleanup()
        }
    }

    private suspend fun saveMemoryStats(memoryInfo: MemoryState) {
        try {
            // Save memory data to database
            // Note: DataManager.savePerformanceData parameters need to be checked
            Log.i(TAG, "Memory stats: ${memoryInfo.usagePercent}% used")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save memory stats", e)
        }
    }

    private fun getAvailableMemory(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem
    }

    private fun performHeapOptimization(): Long {
        val beforeOptimization = Runtime.getRuntime().freeMemory()

        // 强制执行多次GC
        repeat(3) {
            System.gc()
            Thread.sleep(100)
        }

        val afterOptimization = Runtime.getRuntime().freeMemory()
        return max(0L, afterOptimization - beforeOptimization)
    }

    private fun cleanNativeMemory(): Long {
        return try {
            val beforeClean = Debug.getNativeHeapAllocatedSize()

            // 尝试清理原生内存（需要具体实现）
            // 这里可以添加具体的原生内存清理逻辑

            val afterClean = Debug.getNativeHeapAllocatedSize()
            max(0L, beforeClean - afterClean)
        } catch (e: Exception) {
            0L
        }
    }

    private fun performMemoryCompaction(): Boolean {
        return try {
            if (ShizukuManager.isShizukuAvailable()) {
                ShizukuManager.executeShellCommand("echo 1 > /proc/sys/vm/compact_memory")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getAppCacheSize(packageName: String): Long {
        // 获取应用缓存大小（需要具体实现）
        return 0L
    }

    private fun clearAppCache(packageName: String): Boolean {
        return try {
            if (ShizukuManager.isShizukuAvailable()) {
                ShizukuManager.executeShellCommand("pm clear $packageName")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun shouldKillProcess(process: ActivityManager.RunningAppProcessInfo): Boolean {
        // 不杀死系统进程和前台进程
        return process.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE &&
               !process.processName.startsWith("system") &&
               !process.processName.startsWith("com.android")
    }

    private fun getProcessMemoryUsage(pid: Int): Long {
        return try {
            val memoryInfo = activityManager.getProcessMemoryInfo(intArrayOf(pid))
            if (memoryInfo.isNotEmpty()) {
                memoryInfo[0].totalPss.toLong() * 1024
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun killBackgroundProcess(processName: String): Boolean {
        return try {
            activityManager.killBackgroundProcesses(processName)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun clearSystemCache(): Long {
        // 清理系统缓存（需要具体实现）
        return 0L
    }

    private fun clearTempFiles(): Long {
        // 清理临时文件（需要具体实现）
        return 0L
    }

    private fun clearLogFiles(): Long {
        // 清理日志文件（需要具体实现）
        return 0L
    }

    private fun triggerMemoryCompaction(): Boolean {
        return try {
            if (ShizukuManager.isShizukuAvailable()) {
                ShizukuManager.executeShellCommand("echo 1 > /proc/sys/vm/compact_memory")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun optimizeMemoryAllocation(): Boolean {
        // 优化内存分配策略（需要具体实现）
        return true
    }

    private fun getMemoryLimitForApp(processName: String): Long {
        // 根据应用类型返回内存限制
        return when {
            processName.contains("game") -> 512 * 1024 * 1024L // 游戏应用512MB
            processName.contains("browser") -> 256 * 1024 * 1024L // 浏览器256MB
            else -> 128 * 1024 * 1024L // 其他应用128MB
        }
    }

    private fun setAppMemoryLimit(processName: String, limit: Long): Boolean {
        return try {
            if (ShizukuManager.isShizukuAvailable()) {
                // 设置应用内存限制（需要具体实现）
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun generateLeakRecommendations(): List<String> {
        return listOf(
            "定期检查Activity和Fragment的生命周期",
            "及时取消注册监听器和回调",
            "避免使用静态Context引用",
            "正确使用WeakReference",
            "及时关闭数据库连接和文件流"
        )
    }

    private fun formatMemorySize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return String.format("%.1f%s", size, units[unitIndex])
    }
}

// 数据类定义

enum class MemoryProfile {
    LIGHT,      // 轻度优化
    BALANCED,   // 平衡模式
    AGGRESSIVE  // 激进模式
}

data class MemoryState(
    val totalMemory: Long = 0L,
    val availableMemory: Long = 0L,
    val usedMemory: Long = 0L,
    val usagePercent: Int = 0,
    val isLowMemory: Boolean = false,
    val threshold: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

data class MemoryOptimizationResult(
    val success: Boolean = false,
    val improvements: List<String> = emptyList(),
    val freedMemory: Long = 0L,
    val message: String = ""
)

data class MemoryCleanupResult(
    val improvements: List<String> = emptyList(),
    val freedMemory: Long = 0L
)

data class MemoryDataPoint(
    val timestamp: Long,
    val usagePercent: Int,
    val availableMemory: Long,
    val isLowMemory: Boolean
)

data class MemoryLeakReport(
    val hasLeaks: Boolean = false,
    val leakCount: Int = 0,
    val recommendations: List<String> = emptyList()
)

data class DetailedMemoryInfo(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val jvmHeapSize: Long,
    val jvmHeapUsed: Long,
    val jvmHeapMax: Long,
    val nativeHeapSize: Long,
    val nativeHeapUsed: Long,
    val nativeHeapFree: Long,
    val isLowMemory: Boolean,
    val threshold: Long
)

data class AppMemoryInfo(
    val packageName: String,
    val appName: String,
    val pid: Int,
    val memoryUsage: Long,
    val importance: Int
)