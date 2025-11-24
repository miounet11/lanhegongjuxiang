package com.lanhe.gongjuxiang.utils

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * 性能监控功能的性能测试
 * 确保监控功能本身不影响应用性能
 */
@RunWith(AndroidJUnit4::class)
class PerformanceMonitorPerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var context: Context
    private lateinit var performanceMonitor: RealPerformanceMonitorManager
    private lateinit var enhancedBatteryMonitor: EnhancedBatteryMonitor
    private lateinit var enhancedNetworkStats: EnhancedNetworkStatsManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        
        // 初始化性能监控组件
        performanceMonitor = RealPerformanceMonitorManager(context)
        enhancedBatteryMonitor = EnhancedBatteryMonitor(context)
        enhancedNetworkStats = EnhancedNetworkStatsManager(context)
    }

    @Test
    fun benchmarkGetCurrentPerformance() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val performance = performanceMonitor.getCurrentPerformance()
                assertNotNull(performance)
            }
        }
    }

    @Test
    fun benchmarkGetBatteryInfo() {
        benchmarkRule.measureRepeated {
            val batteryInfo = enhancedBatteryMonitor.getCurrentBatteryInfo()
            assertNotNull(batteryInfo)
        }
    }

    @Test
    fun benchmarkGetNetworkStats() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val networkStats = enhancedNetworkStats.getDetailedNetworkStats()
                assertNotNull(networkStats)
            }
        }
    }

    @Test
    fun testMemoryUsageUnderMonitoring() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // 启动监控
        performanceMonitor.startMonitoring()
        
        runBlocking {
            // 运行一段时间
            delay(10000) // 10秒
        }
        
        val memoryAfterMonitoring = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = memoryAfterMonitoring - initialMemory
        
        // 验证内存增长在合理范围内（小于50MB）
        assertTrue(
            memoryIncrease < 50 * 1024 * 1024,
            "监控期间内存增长过大: ${memoryIncrease / (1024 * 1024)}MB"
        )
        
        performanceMonitor.stopMonitoring()
    }

    @Test
    fun testConcurrentMonitoringOperations() {
        val startTime = System.currentTimeMillis()
        
        // 并发执行多个监控操作
        val jobs = listOf(
            async {
                repeat(10) {
                    performanceMonitor.getCurrentPerformance()
                }
            },
            async {
                repeat(10) {
                    enhancedBatteryMonitor.getCurrentBatteryInfo()
                }
            },
            async {
                repeat(10) {
                    runBlocking { enhancedNetworkStats.getDetailedNetworkStats() }
                }
            }
        )
        
        runBlocking {
            jobs.awaitAll()
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // 验证并发操作在合理时间内完成（小于10秒）
        assertTrue(
            duration < 10000,
            "并发监控操作耗时过长: ${duration}ms"
        )
    }

    private fun measureTimeMillis(block: () -> Unit): Long {
        val startTime = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - startTime
    }
}
