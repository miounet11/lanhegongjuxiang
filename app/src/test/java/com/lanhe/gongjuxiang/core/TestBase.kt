package com.lanhe.gongjuxiang.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.mockito.MockitoAnnotations

/**
 * 测试基类
 * 提供通用的测试设置和工具方法
 */
@ExperimentalCoroutinesApi
abstract class TestBase {
    
    // 协程测试调度器
    protected lateinit var testDispatcher: TestDispatcher
    protected lateinit var testScope: TestScope
    
    @Before
    open fun setUp() {
        // 初始化 Mockito
        MockitoAnnotations.openMocks(this)
        
        // 设置协程测试环境
        testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)
    }
    
    /**
     * 验证异常抛出
     */
    protected inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
        var exception: Throwable? = null
        try {
            block()
        } catch (e: Throwable) {
            exception = e
        }
        
        assert(exception is T) {
            "Expected exception ${T::class.java.simpleName} but got ${exception?.javaClass?.simpleName}"
        }
        return exception as T
    }
    
    /**
     * 验证值不为空
     */
    protected fun <T> assertNotNull(value: T?, message: String = "Value should not be null"): T {
        assert(value != null) { message }
        return value!!
    }
    
    /**
     * 验证值为空
     */
    protected fun assertNull(value: Any?, message: String = "Value should be null") {
        assert(value == null) { message }
    }
    
    /**
     * 验证两个值相等
     */
    protected fun <T> assertEquals(expected: T, actual: T, message: String = "Values should be equal") {
        assert(expected == actual) { "$message: expected $expected, but was $actual" }
    }
    
    /**
     * 验证条件为真
     */
    protected fun assertTrue(condition: Boolean, message: String = "Condition should be true") {
        assert(condition) { message }
    }
    
    /**
     * 验证条件为假
     */
    protected fun assertFalse(condition: Boolean, message: String = "Condition should be false") {
        assert(!condition) { message }
    }
}

/**
 * 测试数据工厂
 */
object TestDataFactory {
    
    /**
     * 创建测试性能数据
     */
    fun createTestPerformanceData(
        cpuUsage: Float = 45.5f,
        memoryUsagePercent: Int = 60,
        batteryLevel: Int = 80,
        batteryTemperature: Float = 36.5f,
        deviceTemperature: Float = 38.2f,
        timestamp: Long = System.currentTimeMillis()
    ) = com.lanhe.gongjuxiang.utils.PerformanceDataEntity(
        id = 1L,
        timestamp = timestamp,
        cpuUsage = cpuUsage,
        memoryUsagePercent = memoryUsagePercent,
        batteryLevel = batteryLevel,
        batteryTemperature = batteryTemperature,
        deviceTemperature = deviceTemperature,
        dataType = "performance"
    )
    
    /**
     * 创建测试优化历史
     */
    fun createTestOptimizationHistory(
        optimizationType: String = "battery",
        success: Boolean = true,
        message: String = "优化完成",
        improvements: String = "电池续航提升15%",
        duration: Long = 2500L,
        timestamp: Long = System.currentTimeMillis()
    ) = com.lanhe.gongjuxiang.utils.OptimizationHistoryEntity(
        id = 1L,
        timestamp = timestamp,
        optimizationType = optimizationType,
        success = success,
        message = message,
        improvements = improvements,
        duration = duration
    )
    
    /**
     * 创建测试电池统计
     */
    fun createTestBatteryStats(
        batteryLevel: Int = 75,
        temperature: Float = 35.5f,
        voltage: Float = 4.2f,
        isCharging: Boolean = false,
        timestamp: Long = System.currentTimeMillis(),
        healthStatus: String = "Good"
    ) = com.lanhe.gongjuxiang.utils.BatteryStatsEntity(
        id = 1L,
        timestamp = timestamp,
        batteryLevel = batteryLevel,
        temperature = temperature,
        voltage = voltage,
        isCharging = isCharging,
        healthStatus = healthStatus
    )
}

/**
 * 测试常量
 */
object TestConstants {
    const val TEST_PACKAGE_NAME = "com.lanhe.gongjuxiang.test"
    const val TEST_TIMEOUT_MS = 5000L
    const val TEST_DELAY_MS = 100L
    const val TEST_CPU_THRESHOLD = 80.0f
    const val TEST_MEMORY_THRESHOLD = 85
    const val TEST_BATTERY_THRESHOLD = 20
    const val TEST_TEMPERATURE_THRESHOLD = 45.0f
}
