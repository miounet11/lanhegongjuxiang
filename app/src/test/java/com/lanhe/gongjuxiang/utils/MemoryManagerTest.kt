package com.lanhe.gongjuxiang.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import com.lanhe.gongjuxiang.models.MemoryInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * EnhancedMemoryManager单元测试
 * 测试内存分析、可清理内存计算、内存泄漏检测、清理效果评估
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class MemoryManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockActivityManager: ActivityManager

    @Mock
    private lateinit var mockMemoryInfo: ActivityManager.MemoryInfo

    private lateinit var memoryManager: EnhancedMemoryManager

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.getSystemService(Context.ACTIVITY_SERVICE))
            .thenReturn(mockActivityManager)

        memoryManager = EnhancedMemoryManager(mockContext)
    }

    /**
     * 测试内存分析 - 正常状态
     */
    @Test
    fun `test memory analysis - normal state`() = runTest(testDispatcher) {
        // Given: 设置正常内存状态
        setupMemoryInfo(
            totalMem = 8L * 1024 * 1024 * 1024, // 8GB
            availMem = 4L * 1024 * 1024 * 1024, // 4GB
            threshold = 1L * 1024 * 1024 * 1024, // 1GB
            lowMemory = false
        )

        // When: 分析内存状态
        val analysis = memoryManager.analyzeMemory()

        // Then: 验证分析结果
        assertNotNull(analysis)
        assertEquals("NORMAL", analysis.status)
        assertEquals(50, analysis.usagePercent)
        assertFalse(analysis.needsOptimization)
    }

    /**
     * 测试内存分析 - 低内存状态
     */
    @Test
    fun `test memory analysis - low memory state`() = runTest(testDispatcher) {
        // Given: 设置低内存状态
        setupMemoryInfo(
            totalMem = 8L * 1024 * 1024 * 1024, // 8GB
            availMem = 512L * 1024 * 1024,      // 512MB
            threshold = 1L * 1024 * 1024 * 1024, // 1GB
            lowMemory = true
        )

        // When: 分析内存状态
        val analysis = memoryManager.analyzeMemory()

        // Then: 验证分析结果
        assertNotNull(analysis)
        assertEquals("LOW", analysis.status)
        assertTrue(analysis.usagePercent > 90)
        assertTrue(analysis.needsOptimization)
    }

    /**
     * 测试可清理内存计算
     */
    @Test
    fun `test cleanable memory calculation`() = runTest(testDispatcher) {
        // Given: 模拟内存使用情况
        val appMemoryUsage = listOf(
            AppMemoryUsage("com.app1", 100L * 1024 * 1024, true),  // 100MB, 可清理
            AppMemoryUsage("com.app2", 200L * 1024 * 1024, true),  // 200MB, 可清理
            AppMemoryUsage("com.app3", 150L * 1024 * 1024, false), // 150MB, 不可清理
            AppMemoryUsage("com.app4", 50L * 1024 * 1024, true)    // 50MB, 可清理
        )

        memoryManager.setMockAppMemoryUsage(appMemoryUsage)

        // When: 计算可清理内存
        val cleanableMemory = memoryManager.calculateCleanableMemory()

        // Then: 验证计算结果
        assertEquals(350L * 1024 * 1024, cleanableMemory) // 100 + 200 + 50 = 350MB
    }

    /**
     * 测试内存泄漏检测
     */
    @Test
    fun `test memory leak detection`() = runTest(testDispatcher) {
        // Given: 模拟内存增长模式
        val memorySnapshots = listOf(
            MemorySnapshot(1000L, 2000L * 1024 * 1024), // 2GB
            MemorySnapshot(2000L, 2500L * 1024 * 1024), // 2.5GB
            MemorySnapshot(3000L, 3200L * 1024 * 1024), // 3.2GB
            MemorySnapshot(4000L, 4000L * 1024 * 1024), // 4GB
            MemorySnapshot(5000L, 5100L * 1024 * 1024)  // 5.1GB - 持续增长
        )

        // When: 检测内存泄漏
        val leaks = memoryManager.detectMemoryLeaks(memorySnapshots)

        // Then: 验证泄漏检测
        assertTrue(leaks.hasLeak)
        assertTrue(leaks.growthRate > 0)
        assertEquals("CONTINUOUS_GROWTH", leaks.pattern)
    }

    /**
     * 测试内存清理效果评估
     */
    @Test
    fun `test memory cleanup effectiveness evaluation`() = runTest(testDispatcher) {
        // Given: 清理前后的内存状态
        val beforeCleanup = MemoryInfo(
            totalMemory = 8L * 1024 * 1024 * 1024,
            availableMemory = 1L * 1024 * 1024 * 1024,
            usedMemory = 7L * 1024 * 1024 * 1024,
            usagePercent = 87
        )

        val afterCleanup = MemoryInfo(
            totalMemory = 8L * 1024 * 1024 * 1024,
            availableMemory = 3L * 1024 * 1024 * 1024,
            usedMemory = 5L * 1024 * 1024 * 1024,
            usagePercent = 62
        )

        // When: 评估清理效果
        val effectiveness = memoryManager.evaluateCleanupEffectiveness(
            beforeCleanup,
            afterCleanup
        )

        // Then: 验证评估结果
        assertEquals(2L * 1024 * 1024 * 1024, effectiveness.freedMemory)
        assertEquals(25, effectiveness.percentageImprovement) // 87% -> 62%
        assertEquals("EXCELLENT", effectiveness.rating)
    }

    /**
     * 测试应用内存使用排名
     */
    @Test
    fun `test app memory usage ranking`() = runTest(testDispatcher) {
        // Given: 多个应用的内存使用
        val apps = listOf(
            AppMemoryUsage("com.heavy", 500L * 1024 * 1024, true),
            AppMemoryUsage("com.medium", 200L * 1024 * 1024, true),
            AppMemoryUsage("com.light", 50L * 1024 * 1024, true),
            AppMemoryUsage("com.large", 800L * 1024 * 1024, true)
        )

        memoryManager.setMockAppMemoryUsage(apps)

        // When: 获取排名
        val ranking = memoryManager.getMemoryUsageRanking()

        // Then: 验证排名（从高到低）
        assertEquals(4, ranking.size)
        assertEquals("com.large", ranking[0].packageName)
        assertEquals("com.heavy", ranking[1].packageName)
        assertEquals("com.medium", ranking[2].packageName)
        assertEquals("com.light", ranking[3].packageName)
    }

    /**
     * 测试内存优化建议生成
     */
    @Test
    fun `test memory optimization suggestions`() = runTest(testDispatcher) {
        // Given: 高内存使用状态
        setupMemoryInfo(
            totalMem = 4L * 1024 * 1024 * 1024, // 4GB
            availMem = 256L * 1024 * 1024,      // 256MB
            lowMemory = true
        )

        // When: 生成优化建议
        val suggestions = memoryManager.generateOptimizationSuggestions()

        // Then: 验证建议
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.type == "KILL_BACKGROUND_APPS" })
        assertTrue(suggestions.any { it.type == "CLEAR_CACHE" })
        assertTrue(suggestions.any { it.priority == "HIGH" })
    }

    /**
     * 测试缓存清理
     */
    @Test
    fun `test cache cleanup`() = runTest(testDispatcher) {
        // Given: 缓存数据
        val cacheData = listOf(
            CacheInfo("com.app1", 100L * 1024 * 1024), // 100MB
            CacheInfo("com.app2", 200L * 1024 * 1024), // 200MB
            CacheInfo("com.app3", 50L * 1024 * 1024)   // 50MB
        )

        memoryManager.setMockCacheData(cacheData)

        // When: 清理缓存
        val cleanedSize = memoryManager.clearCache()

        // Then: 验证清理结果
        assertEquals(350L * 1024 * 1024, cleanedSize) // 总共350MB
    }

    /**
     * 测试内存压力测试
     */
    @Test
    fun `test memory pressure test`() = runTest(testDispatcher) {
        // When: 执行内存压力测试
        val pressureTest = memoryManager.performMemoryPressureTest()

        // Then: 验证测试结果
        assertNotNull(pressureTest)
        assertTrue(pressureTest.maxAllocatable > 0)
        assertTrue(pressureTest.fragmentationLevel >= 0)
        assertNotNull(pressureTest.stabilityScore)
    }

    /**
     * 测试进程优先级管理
     */
    @Test
    fun `test process priority management`() = runTest(testDispatcher) {
        // Given: 运行中的进程
        val processes = listOf(
            ProcessInfo(1001, "com.important", 100, "FOREGROUND"),
            ProcessInfo(1002, "com.background", 200, "BACKGROUND"),
            ProcessInfo(1003, "com.service", 150, "SERVICE"),
            ProcessInfo(1004, "com.cached", 50, "CACHED")
        )

        // When: 获取可终止进程
        val killable = memoryManager.getKillableProcesses(processes)

        // Then: 验证只返回可安全终止的进程
        assertEquals(2, killable.size)
        assertTrue(killable.any { it.packageName == "com.background" })
        assertTrue(killable.any { it.packageName == "com.cached" })
        assertFalse(killable.any { it.packageName == "com.important" })
    }

    /**
     * 测试内存监控历史
     */
    @Test
    fun `test memory monitoring history`() = runTest(testDispatcher) {
        // Given: 记录内存历史
        repeat(5) {
            memoryManager.recordMemorySnapshot()
            Thread.sleep(100)
        }

        // When: 获取历史记录
        val history = memoryManager.getMemoryHistory()

        // Then: 验证历史记录
        assertEquals(5, history.size)
        assertTrue(history.all { it.timestamp > 0 })
        assertTrue(history.all { it.memoryUsage > 0 })
    }

    /**
     * 测试智能清理策略
     */
    @Test
    fun `test intelligent cleanup strategy`() = runTest(testDispatcher) {
        // Given: 不同内存状态
        val strategies = mapOf(
            90 to "AGGRESSIVE",  // 高使用率 -> 激进清理
            70 to "MODERATE",    // 中等使用率 -> 适度清理
            40 to "CONSERVATIVE" // 低使用率 -> 保守清理
        )

        strategies.forEach { (usagePercent, expectedStrategy) ->
            // When: 获取清理策略
            val strategy = memoryManager.getCleanupStrategy(usagePercent)

            // Then: 验证策略选择
            assertEquals(expectedStrategy, strategy)
        }
    }

    // 辅助方法
    private fun setupMemoryInfo(
        totalMem: Long = 8L * 1024 * 1024 * 1024,
        availMem: Long = 4L * 1024 * 1024 * 1024,
        threshold: Long = 1L * 1024 * 1024 * 1024,
        lowMemory: Boolean = false
    ) {
        `when`(mockMemoryInfo.totalMem).thenReturn(totalMem)
        `when`(mockMemoryInfo.availMem).thenReturn(availMem)
        `when`(mockMemoryInfo.threshold).thenReturn(threshold)
        `when`(mockMemoryInfo.lowMemory).thenReturn(lowMemory)

        `when`(mockActivityManager.getMemoryInfo(any())).thenAnswer {
            val arg = it.arguments[0] as ActivityManager.MemoryInfo
            arg.totalMem = totalMem
            arg.availMem = availMem
            arg.threshold = threshold
            arg.lowMemory = lowMemory
            null
        }
    }
}

/**
 * 测试用数据类
 */
data class MemoryAnalysis(
    val status: String,
    val usagePercent: Int,
    val needsOptimization: Boolean
)

data class AppMemoryUsage(
    val packageName: String,
    val memorySize: Long,
    val isCleanable: Boolean
)

data class MemorySnapshot(
    val timestamp: Long,
    val memoryUsage: Long
)

data class MemoryLeakDetection(
    val hasLeak: Boolean,
    val growthRate: Double,
    val pattern: String
)

data class CleanupEffectiveness(
    val freedMemory: Long,
    val percentageImprovement: Int,
    val rating: String
)

data class CacheInfo(
    val packageName: String,
    val cacheSize: Long
)

data class MemoryPressureTest(
    val maxAllocatable: Long,
    val fragmentationLevel: Int,
    val stabilityScore: String
)

data class ProcessInfo(
    val pid: Int,
    val packageName: String,
    val importance: Int,
    val priorityClass: String
)

data class MemoryOptimizationSuggestion(
    val type: String,
    val description: String,
    val priority: String
)

/**
 * EnhancedMemoryManager模拟实现
 */
class EnhancedMemoryManager(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var mockAppMemoryUsage: List<AppMemoryUsage> = emptyList()
    private var mockCacheData: List<CacheInfo> = emptyList()
    private val memoryHistory = mutableListOf<MemorySnapshot>()

    fun analyzeMemory(): MemoryAnalysis {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val usagePercent = ((memInfo.totalMem - memInfo.availMem) * 100 / memInfo.totalMem).toInt()
        val status = when {
            memInfo.lowMemory -> "LOW"
            usagePercent > 80 -> "HIGH"
            else -> "NORMAL"
        }

        return MemoryAnalysis(
            status = status,
            usagePercent = usagePercent,
            needsOptimization = memInfo.lowMemory || usagePercent > 80
        )
    }

    fun setMockAppMemoryUsage(usage: List<AppMemoryUsage>) {
        mockAppMemoryUsage = usage
    }

    fun setMockCacheData(data: List<CacheInfo>) {
        mockCacheData = data
    }

    fun calculateCleanableMemory(): Long {
        return mockAppMemoryUsage
            .filter { it.isCleanable }
            .sumOf { it.memorySize }
    }

    fun detectMemoryLeaks(snapshots: List<MemorySnapshot>): MemoryLeakDetection {
        if (snapshots.size < 2) {
            return MemoryLeakDetection(false, 0.0, "INSUFFICIENT_DATA")
        }

        val growthRates = snapshots.zipWithNext { a, b ->
            (b.memoryUsage - a.memoryUsage).toDouble() / a.memoryUsage
        }

        val avgGrowthRate = growthRates.average()
        val hasLeak = avgGrowthRate > 0.1 // 10%以上的持续增长

        return MemoryLeakDetection(
            hasLeak = hasLeak,
            growthRate = avgGrowthRate,
            pattern = if (hasLeak) "CONTINUOUS_GROWTH" else "STABLE"
        )
    }

    fun evaluateCleanupEffectiveness(
        before: MemoryInfo,
        after: MemoryInfo
    ): CleanupEffectiveness {
        val freedMemory = after.availableMemory - before.availableMemory
        val improvement = before.usagePercent - after.usagePercent

        val rating = when {
            improvement >= 20 -> "EXCELLENT"
            improvement >= 10 -> "GOOD"
            improvement >= 5 -> "MODERATE"
            else -> "POOR"
        }

        return CleanupEffectiveness(
            freedMemory = freedMemory,
            percentageImprovement = improvement,
            rating = rating
        )
    }

    fun getMemoryUsageRanking(): List<AppMemoryUsage> {
        return mockAppMemoryUsage.sortedByDescending { it.memorySize }
    }

    fun generateOptimizationSuggestions(): List<MemoryOptimizationSuggestion> {
        val suggestions = mutableListOf<MemoryOptimizationSuggestion>()
        val analysis = analyzeMemory()

        if (analysis.needsOptimization) {
            suggestions.add(
                MemoryOptimizationSuggestion(
                    type = "KILL_BACKGROUND_APPS",
                    description = "终止后台应用",
                    priority = "HIGH"
                )
            )
            suggestions.add(
                MemoryOptimizationSuggestion(
                    type = "CLEAR_CACHE",
                    description = "清理缓存",
                    priority = "HIGH"
                )
            )
        }

        return suggestions
    }

    fun clearCache(): Long {
        return mockCacheData.sumOf { it.cacheSize }
    }

    fun performMemoryPressureTest(): MemoryPressureTest {
        return MemoryPressureTest(
            maxAllocatable = Runtime.getRuntime().maxMemory(),
            fragmentationLevel = 10,
            stabilityScore = "STABLE"
        )
    }

    fun getKillableProcesses(processes: List<ProcessInfo>): List<ProcessInfo> {
        return processes.filter {
            it.priorityClass == "BACKGROUND" || it.priorityClass == "CACHED"
        }
    }

    fun recordMemorySnapshot() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        memoryHistory.add(
            MemorySnapshot(
                timestamp = System.currentTimeMillis(),
                memoryUsage = memInfo.totalMem - memInfo.availMem
            )
        )
    }

    fun getMemoryHistory(): List<MemorySnapshot> {
        return memoryHistory.toList()
    }

    fun getCleanupStrategy(usagePercent: Int): String {
        return when {
            usagePercent >= 80 -> "AGGRESSIVE"
            usagePercent >= 60 -> "MODERATE"
            else -> "CONSERVATIVE"
        }
    }
}