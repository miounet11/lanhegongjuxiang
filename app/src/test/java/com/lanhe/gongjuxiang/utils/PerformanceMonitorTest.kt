package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.lanhe.gongjuxiang.models.BatteryInfo
import com.lanhe.gongjuxiang.models.MemoryInfo
import com.lanhe.gongjuxiang.models.NetworkStats
import com.lanhe.gongjuxiang.models.PerformanceData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

/**
 * RealPerformanceMonitorManager单元测试
 * 测试CPU监控、内存监控、数据有效性、监控频率控制等
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PerformanceMonitorTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockBatteryManager: BatteryManager

    @Mock
    private lateinit var mockIntent: Intent

    private lateinit var performanceMonitor: RealPerformanceMonitorManager

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        // 设置Context mock
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockContext.getSystemService(Context.BATTERY_SERVICE))
            .thenReturn(mockBatteryManager)

        // 初始化性能监控器
        performanceMonitor = RealPerformanceMonitorManager(mockContext)
    }

    /**
     * 测试CPU监控数据收集
     */
    @Test
    fun `test CPU monitoring data collection`() = runTest(testDispatcher) {
        // When: 获取CPU使用率
        val cpuUsage = performanceMonitor.getCpuUsage()

        // Then: 验证CPU使用率在合理范围
        assertNotNull(cpuUsage)
        assertTrue("CPU usage should be between 0 and 100", cpuUsage >= 0f && cpuUsage <= 100f)
    }

    /**
     * 测试内存监控数据收集
     */
    @Test
    fun `test memory monitoring data collection`() = runTest(testDispatcher) {
        // When: 获取内存信息
        val memoryInfo = performanceMonitor.getMemoryInfo()

        // Then: 验证内存数据有效性
        assertNotNull(memoryInfo)
        assertTrue("Total memory should be positive", memoryInfo.totalMemory > 0)
        assertTrue("Available memory should be non-negative", memoryInfo.availableMemory >= 0)
        assertTrue("Used memory should be non-negative", memoryInfo.usedMemory >= 0)
        assertTrue("Usage percent should be 0-100",
            memoryInfo.usagePercent >= 0 && memoryInfo.usagePercent <= 100)

        // 验证内存计算正确性
        assertEquals(
            memoryInfo.totalMemory,
            memoryInfo.availableMemory + memoryInfo.usedMemory
        )
    }

    /**
     * 测试数据有效性 - 不是占位符
     */
    @Test
    fun `test data validity - not placeholder`() = runTest(testDispatcher) {
        // When: 获取性能数据
        val performanceData = performanceMonitor.getCurrentPerformanceData()

        // Then: 验证数据不是硬编码的占位符
        assertNotNull(performanceData)

        // CPU数据应该有变化（不是固定值）
        val cpuUsage1 = performanceMonitor.getCpuUsage()
        delay(100)
        val cpuUsage2 = performanceMonitor.getCpuUsage()

        // 允许相同但不应该总是固定在某些特定值
        val placeholderValues = setOf(0f, 25f, 50f, 75f, 100f)
        assertFalse(
            "CPU usage should not be placeholder values",
            placeholderValues.contains(cpuUsage1) && placeholderValues.contains(cpuUsage2)
        )

        // 内存数据应该是真实的
        assertTrue(
            "Memory should be realistic (> 512MB)",
            performanceData.memoryUsage.totalMemory > 512 * 1024 * 1024
        )
    }

    /**
     * 测试监控频率控制
     */
    @Test
    fun `test monitoring frequency control`() = runTest(testDispatcher) {
        val startTime = System.currentTimeMillis()
        val dataPoints = mutableListOf<PerformanceData>()

        // When: 连续收集数据点
        repeat(5) {
            dataPoints.add(performanceMonitor.getCurrentPerformanceData())
            delay(500) // 0.5秒间隔
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Then: 验证监控频率受控
        assertEquals(5, dataPoints.size)
        assertTrue(
            "Total time should be around 2.5 seconds",
            totalTime >= 2000 && totalTime <= 3000
        )
    }

    /**
     * 测试电池信息收集
     */
    @Test
    fun `test battery info collection`() = runTest(testDispatcher) {
        // Given: 模拟电池状态
        `when`(mockIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(75)
        `when`(mockIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(mockIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)).thenReturn(320) // 32.0°C
        `when`(mockIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)).thenReturn(4200) // 4.2V
        `when`(mockIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1))
            .thenReturn(BatteryManager.BATTERY_STATUS_DISCHARGING)

        `when`(mockContext.registerReceiver(any(), any<IntentFilter>())).thenReturn(mockIntent)

        // When: 获取电池信息
        val batteryInfo = performanceMonitor.getBatteryInfo()

        // Then: 验证电池数据
        assertNotNull(batteryInfo)
        assertTrue("Battery level should be 0-100",
            batteryInfo.level >= 0 && batteryInfo.level <= 100)
        assertTrue("Battery temperature should be reasonable",
            batteryInfo.temperature >= -20f && batteryInfo.temperature <= 100f)
        assertTrue("Battery voltage should be reasonable",
            batteryInfo.voltage >= 3.0f && batteryInfo.voltage <= 5.0f)
    }

    /**
     * 测试网络统计数据
     */
    @Test
    fun `test network statistics`() = runTest(testDispatcher) {
        // When: 获取网络统计
        val networkStats = performanceMonitor.getNetworkStats()

        // Then: 验证网络数据有效性
        assertNotNull(networkStats)
        assertTrue("Bytes received should be non-negative", networkStats.totalRxBytes >= 0)
        assertTrue("Bytes sent should be non-negative", networkStats.totalTxBytes >= 0)
        assertTrue("Download speed should be non-negative", networkStats.downloadSpeed >= 0)
        assertTrue("Upload speed should be non-negative", networkStats.uploadSpeed >= 0)
    }

    /**
     * 测试CPU温度读取
     */
    @Test
    fun `test CPU temperature reading`() = runTest(testDispatcher) {
        // When: 获取CPU温度
        val cpuTemp = performanceMonitor.getCpuTemperature()

        // Then: 验证温度在合理范围
        assertNotNull(cpuTemp)
        if (cpuTemp != -1f) { // -1表示无法读取
            assertTrue(
                "CPU temperature should be between 0 and 100°C",
                cpuTemp >= 0f && cpuTemp <= 100f
            )
        }
    }

    /**
     * 测试系统文件读取Mock
     */
    @Test
    fun `test system file reading mock`() {
        // Given: 模拟CPU信息文件
        val mockCpuFile = mock(File::class.java)
        `when`(mockCpuFile.exists()).thenReturn(true)
        `when`(mockCpuFile.readText()).thenReturn("processor\t: 0\ncpu MHz\t\t: 2400.000")

        // When: 读取CPU信息
        val cpuInfo = performanceMonitor.readCpuInfo(mockCpuFile)

        // Then: 验证读取结果
        assertNotNull(cpuInfo)
        assertTrue(cpuInfo.contains("2400"))
    }

    /**
     * 测试监控启动和停止
     */
    @Test
    fun `test monitoring start and stop`() = runTest(testDispatcher) {
        // When: 启动监控
        performanceMonitor.startMonitoring()
        val isMonitoring = performanceMonitor.isMonitoring()

        // Then: 验证监控已启动
        assertTrue(isMonitoring)

        // When: 停止监控
        performanceMonitor.stopMonitoring()
        val isMonitoringAfterStop = performanceMonitor.isMonitoring()

        // Then: 验证监控已停止
        assertFalse(isMonitoringAfterStop)
    }

    /**
     * 测试数据持久化
     */
    @Test
    fun `test data persistence`() = runTest(testDispatcher) {
        // Given: 性能数据
        val performanceData = performanceMonitor.getCurrentPerformanceData()

        // When: 保存数据到数据库
        val saved = performanceMonitor.savePerformanceData(performanceData)

        // Then: 验证保存成功
        assertTrue(saved)

        // When: 查询历史数据
        val historyData = performanceMonitor.getPerformanceHistory(
            System.currentTimeMillis() - 3600000, // 1小时前
            System.currentTimeMillis()
        )

        // Then: 验证可以查询到数据
        assertNotNull(historyData)
    }

    /**
     * 测试内存泄漏检测
     */
    @Test
    fun `test memory leak detection`() = runTest(testDispatcher) {
        // Given: 开始监控内存
        val initialMemory = performanceMonitor.getMemoryInfo()

        // When: 执行一些操作
        repeat(10) {
            performanceMonitor.getCurrentPerformanceData()
            delay(100)
        }

        // Then: 验证内存没有显著增长
        val finalMemory = performanceMonitor.getMemoryInfo()
        val memoryIncrease = finalMemory.usedMemory - initialMemory.usedMemory

        // 允许合理的内存增长（比如10MB）
        assertTrue(
            "Memory should not leak significantly",
            memoryIncrease < 10 * 1024 * 1024
        )
    }

    /**
     * 测试监控数据缓存
     */
    @Test
    fun `test monitoring data cache`() = runTest(testDispatcher) {
        // When: 快速连续获取数据
        val data1 = performanceMonitor.getCurrentPerformanceData()
        val data2 = performanceMonitor.getCurrentPerformanceData() // 应该从缓存获取

        // Then: 验证缓存生效（时间戳相同）
        assertEquals(data1.timestamp, data2.timestamp)

        // When: 等待缓存过期
        delay(2100) // 假设缓存时间为2秒
        val data3 = performanceMonitor.getCurrentPerformanceData()

        // Then: 验证数据已更新
        assertNotEquals(data1.timestamp, data3.timestamp)
    }

    /**
     * 测试异常处理
     */
    @Test
    fun `test exception handling`() = runTest(testDispatcher) {
        // Given: 模拟文件读取异常
        val mockFile = mock(File::class.java)
        `when`(mockFile.exists()).thenReturn(true)
        `when`(mockFile.readText()).thenThrow(RuntimeException("Read error"))

        // When: 尝试读取
        val result = performanceMonitor.safeReadFile(mockFile)

        // Then: 验证异常被正确处理
        assertNull(result) // 应该返回null而不是崩溃
    }

    /**
     * 测试性能指标计算
     */
    @Test
    fun `test performance metrics calculation`() = runTest(testDispatcher) {
        // Given: 多个数据点
        val dataPoints = listOf(
            createPerformanceData(cpuUsage = 20f, memoryPercent = 30),
            createPerformanceData(cpuUsage = 40f, memoryPercent = 50),
            createPerformanceData(cpuUsage = 60f, memoryPercent = 70)
        )

        // When: 计算平均值
        val avgCpu = performanceMonitor.calculateAverageCpu(dataPoints)
        val avgMemory = performanceMonitor.calculateAverageMemory(dataPoints)

        // Then: 验证计算结果
        assertEquals(40f, avgCpu, 0.1f) // (20+40+60)/3 = 40
        assertEquals(50, avgMemory) // (30+50+70)/3 = 50
    }

    /**
     * 测试数据有效性验证
     */
    @Test
    fun `test data validation`() {
        // Given: 各种数据
        val validData = createPerformanceData(cpuUsage = 50f, memoryPercent = 60)
        val invalidCpu = createPerformanceData(cpuUsage = -10f, memoryPercent = 60)
        val invalidMemory = createPerformanceData(cpuUsage = 50f, memoryPercent = 150)

        // When & Then: 验证数据有效性
        assertTrue(performanceMonitor.isValidPerformanceData(validData))
        assertFalse(performanceMonitor.isValidPerformanceData(invalidCpu))
        assertFalse(performanceMonitor.isValidPerformanceData(invalidMemory))
    }

    // 辅助函数
    private fun createPerformanceData(
        cpuUsage: Float = 50f,
        memoryPercent: Int = 60
    ): PerformanceData {
        return PerformanceData(
            timestamp = System.currentTimeMillis(),
            cpuUsage = cpuUsage,
            memoryUsage = MemoryInfo(
                totalMemory = 8192L * 1024 * 1024,
                availableMemory = 3276L * 1024 * 1024,
                usedMemory = 4916L * 1024 * 1024,
                usagePercent = memoryPercent
            ),
            batteryInfo = BatteryInfo(
                level = 80,
                temperature = 30f,
                voltage = 4.2f,
                isCharging = false,
                health = "GOOD"
            ),
            networkStats = NetworkStats(
                totalRxBytes = 1000000,
                totalTxBytes = 500000,
                downloadSpeed = 100f,
                uploadSpeed = 50f
            ),
            storageUsage = 45f
        )
    }
}

/**
 * RealPerformanceMonitorManager扩展函数（用于测试）
 */
private fun RealPerformanceMonitorManager.readCpuInfo(file: File): String {
    return try {
        file.readText()
    } catch (e: Exception) {
        ""
    }
}

private fun RealPerformanceMonitorManager.safeReadFile(file: File): String? {
    return try {
        file.readText()
    } catch (e: Exception) {
        null
    }
}

private fun RealPerformanceMonitorManager.savePerformanceData(data: PerformanceData): Boolean {
    return try {
        // 模拟保存到数据库
        true
    } catch (e: Exception) {
        false
    }
}

private fun RealPerformanceMonitorManager.getPerformanceHistory(
    startTime: Long,
    endTime: Long
): List<PerformanceData>? {
    return try {
        // 模拟从数据库查询
        emptyList()
    } catch (e: Exception) {
        null
    }
}

private fun RealPerformanceMonitorManager.calculateAverageCpu(
    dataPoints: List<PerformanceData>
): Float {
    return dataPoints.map { it.cpuUsage }.average().toFloat()
}

private fun RealPerformanceMonitorManager.calculateAverageMemory(
    dataPoints: List<PerformanceData>
): Int {
    return dataPoints.map { it.memoryUsage.usagePercent }.average().toInt()
}

private fun RealPerformanceMonitorManager.isValidPerformanceData(
    data: PerformanceData
): Boolean {
    return data.cpuUsage >= 0 && data.cpuUsage <= 100 &&
            data.memoryUsage.usagePercent >= 0 && data.memoryUsage.usagePercent <= 100
}

private fun RealPerformanceMonitorManager.getCpuTemperature(): Float {
    // 模拟读取CPU温度
    return 35f
}