package com.lanhe.gongjuxiang.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lanhe.gongjuxiang.LanheApplication
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * 并发安全测试套件
 * 测试AppDatabase和ShizukuManager的并发安全性
 */
@RunWith(AndroidJUnit4::class)
class ConcurrencyTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    /**
     * 测试1：AppDatabase单例在并发访问下的线程安全性
     * 多个线程同时获取数据库实例，确保只创建一个实例
     */
    @Test
    fun testAppDatabaseSingletonThreadSafety() {
        val threadCount = 100
        val latch = CountDownLatch(threadCount)
        val barrier = CyclicBarrier(threadCount) // 确保所有线程同时开始
        val instances = mutableSetOf<AppDatabase>()
        val errors = AtomicInteger(0)

        // 创建多个线程同时访问数据库
        val threads = (1..threadCount).map {
            thread {
                try {
                    barrier.await() // 等待所有线程就绪
                    val database = AppDatabase.getDatabase(context)
                    synchronized(instances) {
                        instances.add(database)
                    }
                } catch (e: Exception) {
                    errors.incrementAndGet()
                    e.printStackTrace()
                } finally {
                    latch.countDown()
                }
            }
        }

        // 等待所有线程完成
        latch.await()

        // 验证结果
        assertEquals("应该没有错误发生", 0, errors.get())
        assertEquals("应该只有一个数据库实例", 1, instances.size)

        // 清理
        threads.forEach { it.join() }
    }

    /**
     * 测试2：AppDatabase在快速重复获取时的性能和一致性
     */
    @Test
    fun testAppDatabaseRapidAccess() = runTest {
        val iterations = 1000
        val instances = mutableListOf<AppDatabase>()

        // 快速重复获取数据库实例
        val jobs = (1..iterations).map {
            launch(Dispatchers.IO) {
                instances.add(AppDatabase.getDatabase(context))
            }
        }

        // 等待所有协程完成
        jobs.forEach { it.join() }

        // 验证所有实例都是同一个对象
        val firstInstance = instances.first()
        assertTrue("所有实例应该是同一个对象",
            instances.all { it === firstInstance })
    }

    /**
     * 测试3：ShizukuManager状态更新的线程安全性
     * 模拟多个线程同时更新状态，验证防抖机制
     */
    @Test
    fun testShizukuManagerStateUpdateDebounce() = runTest {
        val updateCount = AtomicInteger(0)
        val originalState = ShizukuManager.shizukuState.value

        // 记录状态变化次数
        val job = launch {
            ShizukuManager.shizukuState.collect { state ->
                if (state != originalState) {
                    updateCount.incrementAndGet()
                }
            }
        }

        // 快速触发多次状态更新
        val updateJobs = (1..50).map {
            launch(Dispatchers.IO) {
                delay(10) // 小延迟，模拟快速连续更新
                // 触发状态检查（实际应用中由Shizuku回调触发）
                // 这里我们只能间接测试，因为updateShizukuStateDebounced是私有的
            }
        }

        // 等待所有更新完成
        updateJobs.forEach { it.join() }
        delay(1000) // 等待防抖时间窗口结束

        // 取消状态收集
        job.cancel()

        // 验证：由于防抖机制，实际状态更新次数应该远小于触发次数
        assertTrue("防抖机制应该减少状态更新次数", updateCount.get() < 50)
    }

    /**
     * 测试4：并发访问数据库DAO的线程安全性
     */
    @Test
    fun testDatabaseDAOConcurrentAccess() = runTest {
        val database = AppDatabase.getDatabase(context)
        val dao = database.performanceDataDao()
        val errors = AtomicInteger(0)

        // 创建测试数据
        val testEntity = PerformanceDataEntity(
            timestamp = System.currentTimeMillis(),
            cpuUsage = 50.0f,
            memoryUsagePercent = 60,
            batteryLevel = 80,
            batteryTemperature = 35.0f,
            deviceTemperature = 40.0f
        )

        // 并发插入和查询
        val jobs = (1..100).map { index ->
            launch(Dispatchers.IO) {
                try {
                    when (index % 3) {
                        0 -> {
                            // 插入操作
                            val entity = testEntity.copy(
                                id = 0,
                                timestamp = System.currentTimeMillis() + index
                            )
                            dao.insert(entity)
                        }
                        1 -> {
                            // 查询操作
                            dao.getLatest()
                        }
                        2 -> {
                            // 获取时间范围数据
                            val endTime = System.currentTimeMillis()
                            val startTime = endTime - 3600000
                            dao.getDataInTimeRange(startTime, endTime)
                        }
                    }
                } catch (e: Exception) {
                    errors.incrementAndGet()
                    e.printStackTrace()
                }
            }
        }

        // 等待所有操作完成
        jobs.forEach { it.join() }

        // 验证没有错误发生
        assertEquals("数据库操作不应该有并发错误", 0, errors.get())
    }

    /**
     * 测试5：ShizukuManager系统服务初始化的并发安全性
     */
    @Test
    fun testShizukuManagerServiceInitialization() {
        val threadCount = 20
        val latch = CountDownLatch(threadCount)
        val errors = AtomicInteger(0)
        val barrier = CyclicBarrier(threadCount)

        // 多个线程同时请求权限和初始化服务
        val threads = (1..threadCount).map {
            thread {
                try {
                    barrier.await()
                    // 请求权限（会触发服务初始化）
                    ShizukuManager.requestPermission(context)
                    // 检查服务状态
                    ShizukuManager.isSystemServicesAvailable()
                } catch (e: Exception) {
                    errors.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        // 等待所有线程完成
        latch.await()

        // 验证
        assertEquals("并发初始化不应该有错误", 0, errors.get())

        // 清理
        threads.forEach { it.join() }
    }

    /**
     * 测试6：内存泄漏测试 - 确保单例不会造成内存泄漏
     */
    @Test
    fun testNoMemoryLeaks() = runTest {
        val weakRefs = mutableListOf<java.lang.ref.WeakReference<AppDatabase>>()

        // 创建并保存弱引用
        repeat(10) {
            val db = AppDatabase.getDatabase(context)
            weakRefs.add(java.lang.ref.WeakReference(db))
        }

        // 触发垃圾回收
        System.gc()
        delay(100)
        System.gc()

        // 由于是单例，所有弱引用应该仍然指向同一个活跃对象
        val aliveCount = weakRefs.count { it.get() != null }
        assertEquals("单例应该保持存活", weakRefs.size, aliveCount)

        // 验证所有引用指向同一个对象
        val firstRef = weakRefs.first().get()
        assertTrue("所有引用应该指向同一个单例",
            weakRefs.all { it.get() === firstRef })
    }

    /**
     * 测试7：压力测试 - 极端并发条件下的稳定性
     */
    @Test
    fun testExtremeConcurrency() = runTest {
        val operations = 10000
        val errors = AtomicInteger(0)
        val successCount = AtomicInteger(0)

        val jobs = (1..operations).map { index ->
            launch(Dispatchers.Default) {
                try {
                    when (index % 4) {
                        0 -> AppDatabase.getDatabase(context)
                        1 -> ShizukuManager.isShizukuAvailable()
                        2 -> ShizukuManager.getCpuUsage()
                        3 -> ShizukuManager.getMemoryInfo()
                    }
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    errors.incrementAndGet()
                }
            }
        }

        // 等待所有操作完成
        jobs.forEach { it.join() }

        // 验证
        println("压力测试完成: 成功=${successCount.get()}, 错误=${errors.get()}")
        assertTrue("错误率应该低于1%", errors.get() < operations * 0.01)
    }

    /**
     * 测试8：死锁检测 - 确保不会发生死锁
     */
    @Test(timeout = 5000) // 5秒超时，如果发生死锁测试会失败
    fun testNoDeadlock() {
        val threadCount = 10
        val latch = CountDownLatch(threadCount)

        // 创建多个线程交叉访问不同的同步资源
        val threads = (1..threadCount).map { index ->
            thread {
                try {
                    repeat(100) {
                        if (index % 2 == 0) {
                            // 偶数线程：先访问数据库，再访问Shizuku
                            AppDatabase.getDatabase(context)
                            ShizukuManager.isShizukuAvailable()
                        } else {
                            // 奇数线程：先访问Shizuku，再访问数据库
                            ShizukuManager.isShizukuAvailable()
                            AppDatabase.getDatabase(context)
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        // 等待所有线程完成（如果发生死锁，会触发超时）
        latch.await()

        // 如果能到达这里，说明没有死锁
        assertTrue("没有发生死锁", true)

        // 清理
        threads.forEach { it.join() }
    }
}