package com.lanhe.gongjuxiang.performance

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.lanhe.gongjuxiang.utils.AppDatabase
import com.lanhe.gongjuxiang.utils.PerformanceMonitorManager
import com.lanhe.gongjuxiang.core.TestDataFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * 性能和压力测试
 * 测试应用在不同负载下的性能表现
 */
@ExperimentalCoroutinesApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class PerformanceTest {

    private lateinit var context: android.content.Context
    private lateinit var performanceMonitor: PerformanceMonitorManager
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        performanceMonitor = PerformanceMonitorManager(context)
        
        // 创建内存数据库用于性能测试
        database = androidx.room.Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        performanceMonitor.stopMonitoring()
        database.close()
    }

    @Test
    fun `test memory usage under monitoring stress`() = runBlocking {
        val runtime = Runtime.getRuntime()
        
        // 强制垃圾回收获取基准内存使用
        System.gc()
        Thread.sleep(1000)
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // 启动性能监控
        performanceMonitor.startMonitoring()
        
        // 运行监控1分钟
        val monitoringDuration = 60000L // 1分钟
        Thread.sleep(monitoringDuration)
        
        // 停止监控
        performanceMonitor.stopMonitoring()
        
        // 再次强制垃圾回收
        System.gc()
        Thread.sleep(1000)
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        
        val memoryIncrease = memoryAfter - memoryBefore
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)
        
        // 内存增长不应该超过50MB
        assertTrue(memoryIncreaseMB < 50, 
                  "内存增长不应该超过50MB，实际增长：${memoryIncreaseMB}MB")
    }

    @Test
    fun `test database performance under load`() = runBlocking {
        val performanceDataDao = database.performanceDataDao()
        
        // 测试大量数据插入性能
        val insertStartTime = System.currentTimeMillis()
        
        for (i in 1..1000) {
            val testData = TestDataFactory.createTestPerformanceData(
                cpuUsage = (i % 100).toFloat(),
                memoryUsagePercent = (i % 100),
                batteryLevel = (i % 100),
                timestamp = System.currentTimeMillis() + i * 1000
            )
            performanceDataDao.insert(testData)
        }
        
        val insertEndTime = System.currentTimeMillis()
        val insertDuration = insertEndTime - insertStartTime
        
        // 1000条记录插入时间不应该超过5秒
        assertTrue(insertDuration < 5000, 
                  "1000条记录插入时间不应该超过5秒，实际时间：${insertDuration}ms")
        
        // 测试查询性能
        val queryStartTime = System.currentTimeMillis()
        
        val allData = performanceDataDao.getAllPerformanceData()
        val recentData = performanceDataDao.getLatestPerformanceData()
        val timeRangeData = performanceDataDao.getPerformanceDataByTimeRange(
            System.currentTimeMillis() - 60000,
            System.currentTimeMillis()
        )
        
        val queryEndTime = System.currentTimeMillis()
        val queryDuration = queryEndTime - queryStartTime
        
        // 查询时间不应该超过1秒
        assertTrue(queryDuration < 1000, 
                  "查询时间不应该超过1秒，实际时间：${queryDuration}ms")
        
        // 验证数据完整性
        assertEquals(1000, allData.size, "应该插入1000条记录")
        assertNotNull(recentData, "应该有最新数据")
        assertTrue(timeRangeData.isNotEmpty(), "时间范围查询应该有结果")
    }

    @Test
    fun `test cpu usage during heavy operations`() = runBlocking {
        val runtime = Runtime.getRuntime()
        val threadCount = Runtime.getRuntime().availableProcessors()
        
        // 获取基准CPU使用率
        Thread.sleep(1000) // 等待系统稳定
        val cpuBefore = performanceMonitor.getCpuUsage()
        
        // 执行CPU密集型操作
        val startTime = System.currentTimeMillis()
        val heavyJobs = mutableListOf<Thread>()
        
        repeat(threadCount) { i ->
            val thread = Thread {
                val endTime = startTime + 10000 // 运行10秒
                var counter = 0
                while (System.currentTimeMillis() < endTime) {
                    counter += Math.random().toInt()
                    // 简单的CPU密集型计算
                    for (j in 1..1000) {
                        counter = (counter * 1.1).toInt()
                    }
                }
            }
            heavyJobs.add(thread)
            thread.start()
        }
        
        // 等待所有线程完成
        heavyJobs.forEach { it.join() }
        
        // 获取压力测试后的CPU使用率
        Thread.sleep(1000) // 等待CPU使用率稳定
        val cpuAfter = performanceMonitor.getCpuUsage()
        
        // 验证CPU使用率变化合理
        assertTrue(cpuBefore >= 0 && cpuAfter >= 0, 
                  "CPU使用率应该为有效值")
        
        // 系统应该能够在压力测试后恢复正常
        Thread.sleep(3000)
        val cpuRecovery = performanceMonitor.getCpuUsage()
        assertTrue(cpuRecovery >= 0, "CPU使用率应该能够恢复")
    }

    @Test
    fun `test performance monitor under concurrent access`() = runBlocking {
        // 测试性能监控器在并发访问下的表现
        val concurrentAccessCount = 10
        val operationsPerThread = 100
        val threads = mutableListOf<Thread>()
        val exceptions = mutableListOf<Exception>()
        
        repeat(concurrentAccessCount) { threadIndex ->
            val thread = Thread {
                try {
                    repeat(operationsPerThread) { operationIndex ->
                        // 并发启动和停止监控
                        if (operationIndex % 2 == 0) {
                            performanceMonitor.startMonitoring()
                        } else {
                            performanceMonitor.stopMonitoring()
                        }
                        
                        Thread.sleep(10) // 短暂延迟
                    }
                } catch (e: Exception) {
                    synchronized(exceptions) {
                        exceptions.add(e)
                    }
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        // 等待所有线程完成
        threads.forEach { it.join() }
        
        // 验证没有异常发生
        assertTrue(exceptions.isEmpty(), 
                  "并发访问不应该产生异常，异常数量：${exceptions.size}")
        
        // 清理状态
        performanceMonitor.stopMonitoring()
    }

    @Test
    fun `test battery optimization performance`() = runBlocking {
        // 测试电池优化功能的性能影响
        val startTime = System.currentTimeMillis()
        
        // 模拟多次电池优化操作
        repeat(50) {
            // 创建测试数据
            val batteryData = TestDataFactory.createTestBatteryStats(
                packageName = "com.test.app$it",
                batteryUsage = (it * 0.5f)
            )
            
            database.batteryStatsDao().insert(batteryData)
            
            // 模拟优化操作
            Thread.sleep(10)
        }
        
        val optimizationEndTime = System.currentTimeMillis()
        val optimizationDuration = optimizationEndTime - startTime
        
        // 50次优化操作不应该超过5秒
        assertTrue(optimizationDuration < 5000, 
                  "电池优化操作时间不应该超过5秒，实际时间：${optimizationDuration}ms")
        
        // 验证数据完整性
        val allStats = database.batteryStatsDao().getAllBatteryStats()
        assertEquals(50, allStats.size, "应该有50条电池统计数据")
    }

    @Test
    fun `test memory leak detection`() = runBlocking {
        val runtime = Runtime.getRuntime()
        val memorySnapshots = mutableListOf<Long>()
        
        // 多次创建和销毁组件，检测内存泄漏
        repeat(10) { iteration ->
            // 创建组件
            val monitor = PerformanceMonitorManager(context)
            monitor.startMonitoring()
            
            // 运行一段时间
            Thread.sleep(1000)
            
            // 停止并销毁组件
            monitor.stopMonitoring()
            
            // 强制垃圾回收
            System.gc()
            Thread.sleep(500)
            
            // 记录内存使用
            val memoryUsed = runtime.totalMemory() - runtime.freeMemory()
            memorySnapshots.add(memoryUsed)
        }
        
        // 分析内存使用趋势
        val firstMemory = memorySnapshots.first()
        val lastMemory = memorySnapshots.last()
        val memoryIncrease = lastMemory - firstMemory
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)
        
        // 内存增长不应该超过10MB（考虑正常波动）
        assertTrue(memoryIncreaseMB < 10, 
                  "不应该有明显的内存泄漏，内存增长：${memoryIncreaseMB}MB")
    }

    @Test
    fun `test system stability under stress`() = runBlocking {
        val operationCount = 1000
        val errors = mutableListOf<Exception>()
        
        // 执行大量操作测试系统稳定性
        repeat(operationCount) { i ->
            try {
                when (i % 5) {
                    0 -> {
                        // 数据库操作
                        val testData = TestDataFactory.createTestPerformanceData(
                            cpuUsage = (i % 100).toFloat()
                        )
                        database.performanceDataDao().insert(testData)
                    }
                    1 -> {
                        // 查询操作
                        database.performanceDataDao().getAllPerformanceData()
                    }
                    2 -> {
                        // 性能监控操作
                        val cpuUsage = performanceMonitor.getCpuUsage()
                        assertTrue(cpuUsage >= -1f, "CPU使用率应该有效")
                    }
                    3 -> {
                        // 内存操作
                        val memoryInfo = performanceMonitor.getMemoryInfo()
                        assertNotNull(memoryInfo, "内存信息不应该为空")
                    }
                    4 -> {
                        // 电池操作
                        val batteryStats = TestDataFactory.createTestBatteryStats(
                            packageName = "com.test.stress$i"
                        )
                        database.batteryStatsDao().insert(batteryStats)
                    }
                }
            } catch (e: Exception) {
                errors.add(e)
            }
        }
        
        // 验证错误率在可接受范围内（<1%）
        val errorRate = errors.size.toDouble() / operationCount
        assertTrue(errorRate < 0.01, 
                  "错误率不应该超过1%，实际错误率：${(errorRate * 100).toFixed(2)}%")
    }
}

/**
 * 扩展函数：格式化Double为指定小数位数
 */
private fun Double.toFixed(digits: Int): String {
    return "%.${digits}f".format(this)
}
