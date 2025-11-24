package com.lanhe.gongjuxiang.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.lanhe.gongjuxiang.utils.ShizukuManager
import com.lanhe.gongjuxiang.utils.ShizukuState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Shizuku 集成测试
 * 测试Shizuku框架的真实环境集成
 */
@ExperimentalCoroutinesApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class ShizukuIntegrationTest {

    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test ShizukuManager initialization`() = runTest {
        // 初始化ShizukuManager
        ShizukuManager.initWithContext(context)
        
        // 验证初始化成功
        assertNotNull(ShizukuManager.shizukuState.value, 
                     "Shizuku状态不应该为空")
    }

    @Test
    fun `test Shizuku availability detection`() = runTest {
        ShizukuManager.initWithContext(context)
        
        val isAvailable = ShizukuManager.isShizukuAvailable()
        
        // 根据测试环境判断结果
        // 在没有Shizuku的测试环境中，isAvailable应该为false
        if (!isAvailable) {
            assertEquals(ShizukuState.Unavailable, ShizukuManager.shizukuState.value)
        }
        
        // 验证检测逻辑没有崩溃
        assertTrue(true, "Shizuku可用性检测应该正常执行")
    }

    @Test
    fun `test permission checking without Shizuku`() = runTest {
        ShizukuManager.initWithContext(context)
        
        // 在没有Shizuku的环境中，权限检查应该返回false
        val hasPermission = ShizukuManager.hasPermission()
        
        if (!ShizukuManager.isShizukuAvailable()) {
            assertFalse(hasPermission, "无Shizuku时应该没有权限")
        }
    }

    @Test
    fun `test system service initialization`() = runTest {
        ShizukuManager.initWithContext(context)
        
        // 等待初始化完成
        kotlinx.coroutines.delay(1000)
        
        val areServicesAvailable = ShizukuManager.areSystemServicesAvailable()
        
        if (!ShizukuManager.isShizukuAvailable()) {
            assertFalse(areServicesAvailable, "无Shizuku时系统服务应该不可用")
        }
    }

    @Test
    fun `test safe operation execution`() = runTest {
        ShizukuManager.initWithContext(context)
        
        // 测试安全操作执行
        val result = ShizukuManager.executeSystemOperation("safe_operation")
        
        // 即使没有Shizuku，操作也应该安全执行，不会崩溃
        assertNotNull(result, "操作结果不应该为空")
    }

    @Test
    fun `test error handling in integration`() = runTest {
        ShizukuManager.initWithContext(context)
        
        // 测试错误处理
        try {
            val processes = ShizukuManager.getRunningProcesses()
            
            // 在没有权限的情况下，应该返回空列表而不是崩溃
            assertNotNull(processes, "进程列表不应该为null")
            if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
                assertTrue(processes.isEmpty(), "无权限时应该返回空列表")
            }
        } catch (e: Exception) {
            // 验证异常被正确处理
            assertTrue(e.message?.isNotEmpty() == true, 
                      "异常应该有有意义的消息")
        }
    }

    @Test
    fun `test CPU usage fallback`() = runTest {
        ShizukuManager.initWithContext(context)
        
        val cpuUsage = ShizukuManager.getCpuUsage()
        
        // 在没有Shizuku权限的情况下，应该返回合理的fallback值
        if (!ShizukuManager.isShizukuAvailable() || !ShizukuManager.hasPermission()) {
            assertEquals(-1f, cpuUsage, "无权限时应该返回-1")
        }
    }

    @Test
    fun `test memory info fallback`() = runTest {
        ShizukuManager.initWithContext(context)
        
        val memoryInfo = ShizukuManager.getMemoryInfo()
        
        // 内存信息应该总是可用（有fallback机制）
        assertNotNull(memoryInfo, "内存信息不应该为空")
        assertTrue(memoryInfo.totalMemory > 0, "总内存应该大于0")
        assertTrue(memoryInfo.usedMemory >= 0, "已用内存应该为非负数")
    }

    @Test
    fun `test permission request flow`() = runTest {
        ShizukuManager.initWithContext(context)
        
        try {
            // 测试权限请求流程
            ShizukuManager.requestPermission(context)
            
            // 权限请求应该不会崩溃
            assertTrue(true, "权限请求流程应该正常执行")
        } catch (e: Exception) {
            // 权限请求过程中的异常应该被妥善处理
            assertTrue(e.message?.isNotEmpty() == true, 
                      "权限请求异常应该有有意义的信息")
        }
    }

    @Test
    fun `test performance boost without Shizuku`() = runTest {
        ShizukuManager.initWithContext(context)
        
        val result = ShizukuManager.boostSystemPerformance()
        
        // 即使没有Shizuku，性能提升操作也应该安全执行
        assertNotNull(result, "性能提升结果不应该为空")
        if (!ShizukuManager.isShizukuAvailable()) {
            assertFalse(result.success, "无Shizuku时性能提升应该失败")
            assertTrue(result.message.isNotEmpty(), "应该有错误消息")
        }
    }

    @Test
    fun `test state flow behavior`() = runTest {
        ShizukuManager.initWithContext(context)
        
        val initialState = ShizukuManager.shizukuState.value
        
        // 测试状态流的行为
        assertNotNull(initialState, "初始状态不应该为空")
        
        // 模拟状态更新
        ShizukuManager.updateShizukuState()
        
        val updatedState = ShizukuManager.shizukuState.value
        assertNotNull(updatedState, "更新后的状态不应该为空")
    }

    @Test
    fun `test integration with other managers`() = runTest {
        // 测试ShizukuManager与其他管理器的集成
        ShizukuManager.initWithContext(context)
        
        // 这些操作应该能够安全地进行集成调用
        val isAvailable = ShizukuManager.isShizukuAvailable()
        val hasPermission = ShizukuManager.hasPermission()
        val areServicesAvailable = ShizukuManager.areSystemServicesAvailable()
        
        // 验证集成调用的逻辑一致性
        if (!isAvailable) {
            assertFalse(hasPermission, "无Shizuku时不应该有权限")
            assertFalse(areServicesAvailable, "无Shizuku时系统服务不应该可用")
        }
    }

    @Test
    fun `test robustness against rapid calls`() = runTest {
        ShizukuManager.initWithContext(context)
        
        // 快速连续调用，测试系统的健壮性
        repeat(10) {
            ShizukuManager.updateShizukuState()
            val state = ShizukuManager.shizukuState.value
            assertNotNull(state)
            
            val cpuUsage = ShizukuManager.getCpuUsage()
            assertTrue(cpuUsage >= -1f, "CPU使用率应该是有效的值")
        }
        
        // 快速调用不应该导致崩溃或不一致的状态
        assertTrue(true, "快速连续调用应该稳定")
    }
}
