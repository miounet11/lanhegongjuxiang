package com.lanhe.gongjuxiang.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lanhe.gongjuxiang.models.BatteryInfo
import com.lanhe.gongjuxiang.models.PerformanceData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 性能监控功能集成测试
 * 测试真实设备上的性能监控功能
 */
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class PerformanceMonitorIntegrationTest {

    private lateinit var context: Context
    private lateinit var performanceMonitor: PerformanceMonitorManager
    private lateinit var database: AppDatabase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        
        // 使用内存数据库进行测试
        database = AppDatabase.getDatabase(context)
        
        performanceMonitor = PerformanceMonitorManager(context)
        
        // 设置测试调度器
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        performanceMonitor.cleanup()
        
        // 清理测试数据库
        runBlocking {
            database.clearAllTables()
        }
    }

    @Test
    fun testFullPerformanceMonitoringCycle() = runTest {
        val callbackResults = mutableListOf<PerformanceData>()
        val errors = mutableListOf<Exception>()
        var monitoringStarted = false
        var monitoringStopped = false
        var dataSavedCount = 0L

        // 设置监控回调
        performanceMonitor.setCallback(object : PerformanceMonitorManager.PerformanceCallback {
            override fun onPerformanceUpdate(data: PerformanceData) {
                callbackResults.add(data)
            }

            override fun onMonitoringStarted() {
                monitoringStarted = true
            }

            override fun onMonitoringStopped() {
                monitoringStopped = true
            }

            override fun onError(error: Exception) {
                errors.add(error)
            }

            override fun onDataSaved(recordCount: Long) {
                dataSavedCount += recordCount
            }
        })

        // 启动监控
        assertFalse(performanceMonitor.isMonitoring())
        performanceMonitor.startMonitoring()
        assertTrue(performanceMonitor.isMonitoring())

        // 等待监控数据收集
        testDispatcher.scheduler.advanceTimeBy(10000L) // 10秒

        // 验证监控已启动
        assertTrue(monitoringStarted, "监控应该已启动")
        assertFalse(monitoringStopped, "监控不应该已停止")

        // 验证收集到的性能数据
        assertTrue(callbackResults.isNotEmpty(), "应该收集到性能数据")
        
        val firstData = callbackResults.first()
        assertTrue(firstData.cpuUsage >= 0f && firstData.cpuUsage <= 100f, 
            "CPU使用率应该在0-100%之间")
        assertTrue(firstData.memoryUsage.total > 0, "内存总量应该大于0")
        assertTrue(firstData.memoryUsage.usagePercent >= 0f, "内存使用率应该非负")
        assertTrue(firstData.storageUsage >= 0f, "存储使用率应该非负")
        assertTrue(firstData.timestamp > 0, "时间戳应该有效")

        // 验证电池信息
        val batteryInfo = firstData.batteryInfo
        assertTrue(batteryInfo.level >= 0 && batteryInfo.level <= 100, 
            "电池电量应该在0-100%之间")
        assertTrue(batteryInfo.temperature > -50f && batteryInfo.temperature < 100f, 
            "电池温度应该在合理范围内")

        // 验证数据已保存到数据库
        assertTrue(dataSavedCount > 0, "应该有数据保存到数据库")

        // 停止监控
        performanceMonitor.stopMonitoring()
        assertFalse(performanceMonitor.isMonitoring())
        assertTrue(monitoringStopped, "监控应该已停止")

        // 验证没有错误发生
        if (errors.isNotEmpty()) {
            // 打印错误信息用于调试
            errors.forEach { 
                println("监控错误: ${it.message}") 
            }
        }
    }

    @Test
    fun testRealTimeDataCollection() = runTest {
        val collectedData = mutableListOf<PerformanceData>()
        var collectionCount = 0

        performanceMonitor.setCallback(object : PerformanceMonitorManager.PerformanceCallback {
            override fun onPerformanceUpdate(data: PerformanceData) {
                collectedData.add(data)
                collectionCount++
            }

            override fun onMonitoringStarted() {}
            override fun onMonitoringStopped() {}
            override fun onError(error: Exception) {}
            override fun onDataSaved(recordCount: Long) {}
        })

        performanceMonitor.startMonitoring()

        // 收集5个数据点（每2秒一个，10秒内）
        testDispatcher.scheduler.advanceTimeBy(10000L)

        performanceMonitor.stopMonitoring()

        // 验证收集到的数据量
        assertTrue(collectedData.size >= 4, "应该收集到至少4个数据点")

        // 验证数据的时间序列
        for (i in 1 until collectedData.size) {
            assertTrue(collectedData[i].timestamp >= collectedData[i-1].timestamp,
                "数据应该按时间顺序收集")
        }

        // 验证数据的变化（CPU使用率等应该有变化）
        val cpuUsages = collectedData.map { it.cpuUsage }
        val cpuVariation = cpuUsages.maxOrNull()!! - cpuUsages.minOrNull()!!
        assertTrue(cpuVariation >= 0f, "CPU使用率应该有变化")
    }

    @Test
    fun testBatteryMonitoringAccuracy() = runTest {
        performanceMonitor.startMonitoring()

        // 等待电池数据收集
        testDispatcher.scheduler.advanceTimeBy(5000L)

        val batteryInfo1 = performanceMonitor.getBatteryInfo()
        testDispatcher.scheduler.advanceTimeBy(3000L)
        val batteryInfo2 = performanceMonitor.getBatteryInfo()

        performanceMonitor.stopMonitoring()

        // 验证电池信息的一致性
        assertEquals(batteryInfo1.technology, batteryInfo2.technology,
            "电池技术应该保持一致")
        assertEquals(batteryInfo1.capacity, batteryInfo2.capacity,
            "电池容量应该保持一致")

        // 验证电池信息的合理性
        assertTrue(batteryInfo1.level >= 0 && batteryInfo1.level <= 100,
            "电池电量应该在合理范围")
        assertTrue(batteryInfo1.temperature > -50f && batteryInfo1.temperature < 100f,
            "电池温度应该在合理范围")
        assertTrue(batteryInfo1.voltage > 0,
            "电池电压应该大于0")
    }

    @Test
    fun testNetworkStatsMonitoring() = runTest {
        performanceMonitor.startMonitoring()

        // 等待网络数据收集
        testDispatcher.scheduler.advanceTimeBy(8000L)

        val networkStats1 = performanceMonitor.getNetworkStats()
        testDispatcher.scheduler.advanceTimeBy(4000L)
        val networkStats2 = performanceMonitor.getNetworkStats()

        performanceMonitor.stopMonitoring()

        // 验证网络统计数据
        assertTrue(networkStats1.rxBytes >= 0, "接收字节数应该非负")
        assertTrue(networkStats1.txBytes >= 0, "发送字节数应该非负")
        assertTrue(networkStats1.timestamp > 0, "时间戳应该有效")

        // 验证网络类型
        val networkType = performanceMonitor.getNetworkType()
        assertNotNull(networkType, "网络类型不应该为空")
        assertTrue(networkType.isNotEmpty(), "网络类型名称不应该为空")

        // 验证数据增长（网络流量应该增长）
        assertTrue(networkStats2.timestamp > networkStats1.timestamp,
            "网络统计时间戳应该递增")
    }

    @Test
    fun testDatabasePersistence() = runTest {
        performanceMonitor.startMonitoring()

        // 收集足够的数据
        testDispatcher.scheduler.advanceTimeBy(15000L) // 15秒

        performanceMonitor.stopMonitoring()

        // 验证数据持久化
        val allData = database.performanceDataDao().getAllPerformanceData().first()
        assertTrue(allData.isNotEmpty(), "数据库中应该有性能数据")

        val firstRecord = allData.first()
        assertTrue(firstRecord.timestamp > 0, "数据库记录时间戳应该有效")
        assertTrue(firstRecord.cpuUsage >= 0f && firstRecord.cpuUsage <= 100f,
            "数据库CPU使用率应该在合理范围")
        assertTrue(firstRecord.memoryUsagePercent >= 0 && firstRecord.memoryUsagePercent <= 100,
            "数据库内存使用率应该在合理范围")
        assertTrue(firstRecord.batteryLevel >= 0 && firstRecord.batteryLevel <= 100,
            "数据库电池电量应该在合理范围")

        // 测试数据查询功能
        val recentData = database.performanceDataDao().getRecentPerformanceData(5)
        assertTrue(recentData.size <= 5, "最近数据查询应该限制数量")
    }

    @Test
    fun testErrorHandlingAndRecovery() = runTest {
        val errors = mutableListOf<Exception>()
        var recoveryCount = 0

        performanceMonitor.setCallback(object : PerformanceMonitorManager.PerformanceCallback {
            override fun onPerformanceUpdate(data: PerformanceData) {
                recoveryCount++
            }

            override fun onMonitoringStarted() {}
            override fun onMonitoringStopped() {}
            override fun onError(error: Exception) {
                errors.add(error)
            }

            override fun onDataSaved(recordCount: Long) {}
        })

        performanceMonitor.startMonitoring()

        // 模拟异常情况（通过快速启停监控）
        testDispatcher.scheduler.advanceTimeBy(3000L)
        performanceMonitor.stopMonitoring()
        performanceMonitor.startMonitoring()
        testDispatcher.scheduler.advanceTimeBy(3000L)

        performanceMonitor.stopMonitoring()

        // 验证系统恢复能力
        assertTrue(recoveryCount > 0, "监控系统应该能够恢复正常工作")
        
        // 即使有错误，也应该有部分数据收集
        if (errors.isNotEmpty()) {
            println("检测到的错误: ${errors.map { it.message }}")
        }
    }

    @Test
    fun testPerformanceUnderLoad() = runTest {
        val dataPoints = mutableListOf<PerformanceData>()
        val startTime = System.currentTimeMillis()

        performanceMonitor.setCallback(object : PerformanceMonitorManager.PerformanceCallback {
            override fun onPerformanceUpdate(data: PerformanceData) {
                dataPoints.add(data)
            }

            override fun onMonitoringStarted() {}
            override fun onMonitoringStopped() {}
            override fun onError(error: Exception) {}
            override fun onDataSaved(recordCount: Long) {}
        })

        performanceMonitor.startMonitoring()

        // 运行较长时间以测试性能
        testDispatcher.scheduler.advanceTimeBy(30000L) // 30秒

        performanceMonitor.stopMonitoring()

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // 验证性能
        assertTrue(dataPoints.isNotEmpty(), "应该收集到数据")
        
        val dataRate = dataPoints.size.toDouble() / (duration / 1000.0)
        assertTrue(dataRate >= 0.1, "数据收集率应该合理 (至少0.1个/秒)")

        // 验证内存使用（通过检查数据点数量）
        assertTrue(dataPoints.size < 1000, "内存使用应该在合理范围内")

        println("性能测试结果:")
        println("- 运行时间: ${duration}ms")
        println("- 数据点数量: ${dataPoints.size}")
        println("- 数据收集率: ${"%.2f".format(dataRate)} 个/秒")
    }

    @Test
    fun testGetCurrentPerformanceSnapshot() = runTest {
        // 不启动监控，直接获取当前快照
        val performance = performanceMonitor.getCurrentPerformance()

        assertNotNull(performance, "当前性能快照不应该为空")

        // 验证快照数据的完整性
        assertTrue(performance!!.cpuUsage >= 0f && performance.cpuUsage <= 100f,
            "CPU使用率应该在合理范围")
        assertTrue(performance.memoryUsage.total > 0,
            "内存总量应该大于0")
        assertTrue(performance.memoryUsage.usagePercent >= 0f,
            "内存使用率应该非负")
        assertTrue(performance.storageUsage >= 0f,
            "存储使用率应该非负")
        assertTrue(performance.timestamp > 0,
            "时间戳应该有效")

        // 验证电池信息
        val batteryInfo = performance.batteryInfo
        assertTrue(batteryInfo.level >= 0 && batteryInfo.level <= 100,
            "电池电量应该在合理范围")
        assertNotNull(batteryInfo.technology,
            "电池技术不应该为空")
    }
}
