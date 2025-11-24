package com.lanhe.gongjuxiang

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lanhe.gongjuxiang.utils.SystemOptimizer
import com.lanhe.gongjuxiang.utils.PerformanceMonitor
import com.lanhe.gongjuxiang.utils.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * 系统优化集成测试
 * 测试各组件协同工作
 */
@RunWith(AndroidJUnit4::class)
class SystemOptimizationIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var systemOptimizer: SystemOptimizer
    private lateinit var performanceMonitor: PerformanceMonitor
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        systemOptimizer = SystemOptimizer(context)
        performanceMonitor = PerformanceMonitor(context)
    }
    
    @Test
    fun testDatabaseOperations() = runBlocking {
        // 测试数据库操作
        val dao = database.performanceDataDao()
        
        // 创建测试数据
        val testEntity = com.lanhe.gongjuxiang.models.PerformanceDataEntity(
            timestamp = System.currentTimeMillis(),
            cpuUsage = 45.0f,
            memoryUsagePercent = 65,
            batteryLevel = 85,
            batteryTemperature = 35.5f,
            deviceTemperature = 38.2f
        )
        
        // 插入数据
        dao.insert(testEntity)
        
        // 查询数据
        val allData = dao.getAllPerformanceData()
        assertFalse(allData.isEmpty())
        
        // 清理测试数据
        dao.deleteAll()
    }
    
    @Test
    fun testPerformanceMonitoring() {
        // 测试性能监控
        val cpuUsage = performanceMonitor.getCpuUsage()
        assertNotNull(cpuUsage)
        assertTrue(cpuUsage.totalUsage >= 0f)
        
        val memoryInfo = performanceMonitor.getMemoryInfo()
        assertNotNull(memoryInfo)
        assertTrue(memoryInfo.totalMemory > 0L)
        assertTrue(memoryInfo.availableMemory >= 0L)
    }
    
    @Test
    fun testSystemOptimizer() = runBlocking {
        // 测试系统优化器
        val initialOptimizationState = systemOptimizer.optimizationState.value
        assertEquals(
            com.lanhe.gongjuxiang.utils.OptimizationState.IDLE,
            initialOptimizationState
        )
        
        // 执行优化
        systemOptimizer.performFullOptimization()
        
        // 等待优化完成
        Thread.sleep(3000)
        
        // 验证优化结果
        val result = systemOptimizer.optimizationResult.value
        assertNotNull(result)
        assertEquals(result.success, true)
        assertNotNull(result.message)
    }
    
    @Test
    fun testApplicationLifecycle() {
        // 测试应用生命周期
        val app = ApplicationProvider.getApplicationContext() as LanheApplication
        
        // 验证数据库已初始化
        assertNotNull(app.database)
        
        // 验证 DAO 可用
        assertNotNull(app.database.performanceDataDao())
        assertNotNull(app.database.optimizationHistoryDao())
        assertNotNull(app.database.batteryStatsDao())
    }
}
