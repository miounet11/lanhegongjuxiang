package com.lanhe.gongjuxiang.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Service生命周期测试
 * 测试Service启动和停止、BroadcastReceiver注册/注销、CoroutineScope管理、异常恢复
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ServiceLifecycleTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockIntent: Intent

    private lateinit var testService: TestableMonitoringService

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        testService = TestableMonitoringService()
        testService.attachBaseContext(mockContext)
    }

    @After
    fun tearDown() {
        testService.onDestroy()
    }

    /**
     * 测试Service启动
     */
    @Test
    fun `test service start`() = runTest(testDispatcher) {
        // When: 启动服务
        testService.onCreate()
        val result = testService.onStartCommand(mockIntent, 0, 1)

        // Then: 验证服务已启动
        assertEquals(Service.START_STICKY, result)
        assertTrue(testService.isRunning())
        assertNotNull(testService.getCoroutineScope())
    }

    /**
     * 测试Service停止
     */
    @Test
    fun `test service stop`() = runTest(testDispatcher) {
        // Given: 服务已启动
        testService.onCreate()
        testService.onStartCommand(mockIntent, 0, 1)
        assertTrue(testService.isRunning())

        // When: 停止服务
        testService.onDestroy()

        // Then: 验证服务已停止
        assertFalse(testService.isRunning())
        assertTrue(testService.getCoroutineScope().isCompleted)
    }

    /**
     * 测试BroadcastReceiver注册
     */
    @Test
    fun `test broadcast receiver registration`() {
        // Given: 准备广播接收器
        val receiver = TestBroadcastReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        // When: 注册接收器
        testService.onCreate()
        testService.registerBroadcastReceiver(receiver, filter)

        // Then: 验证接收器已注册
        assertTrue(testService.isBroadcastReceiverRegistered(receiver))
        assertEquals(3, testService.getRegisteredReceiverCount())
    }

    /**
     * 测试BroadcastReceiver注销
     */
    @Test
    fun `test broadcast receiver unregistration`() {
        // Given: 已注册的接收器
        val receiver = TestBroadcastReceiver()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        testService.onCreate()
        testService.registerBroadcastReceiver(receiver, filter)
        assertTrue(testService.isBroadcastReceiverRegistered(receiver))

        // When: 注销接收器
        testService.unregisterBroadcastReceiver(receiver)

        // Then: 验证接收器已注销
        assertFalse(testService.isBroadcastReceiverRegistered(receiver))
        assertEquals(0, testService.getRegisteredReceiverCount())
    }

    /**
     * 测试Service销毁时自动注销所有接收器
     */
    @Test
    fun `test auto unregister receivers on destroy`() {
        // Given: 注册多个接收器
        val receiver1 = TestBroadcastReceiver()
        val receiver2 = TestBroadcastReceiver()

        testService.onCreate()
        testService.registerBroadcastReceiver(receiver1, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        testService.registerBroadcastReceiver(receiver2, IntentFilter(Intent.ACTION_SCREEN_ON))

        assertEquals(2, testService.getRegisteredReceiverCount())

        // When: 销毁服务
        testService.onDestroy()

        // Then: 验证所有接收器已注销
        assertEquals(0, testService.getRegisteredReceiverCount())
    }

    /**
     * 测试CoroutineScope管理
     */
    @Test
    fun `test coroutine scope management`() = runTest(testDispatcher) {
        // Given: 启动服务
        testService.onCreate()
        val scope = testService.getCoroutineScope()

        // When: 启动协程任务
        val job1 = scope.launch { delay(100) }
        val job2 = scope.launch { delay(200) }

        // Then: 验证任务正在运行
        assertTrue(job1.isActive)
        assertTrue(job2.isActive)

        // When: 销毁服务
        testService.onDestroy()

        // Then: 验证所有任务已取消
        assertTrue(job1.isCancelled)
        assertTrue(job2.isCancelled)
    }

    /**
     * 测试异常恢复 - Service崩溃重启
     */
    @Test
    fun `test exception recovery - service crash restart`() = runTest(testDispatcher) {
        // Given: 服务运行中发生异常
        testService.onCreate()
        testService.simulateException()

        // When: 处理异常并重启
        val recovered = testService.handleExceptionAndRecover()

        // Then: 验证服务已恢复
        assertTrue(recovered)
        assertTrue(testService.isRunning())
        assertEquals(1, testService.getRestartCount())
    }

    /**
     * 测试异常恢复 - 协程异常处理
     */
    @Test
    fun `test exception recovery - coroutine exception`() = runTest(testDispatcher) {
        // Given: 设置异常处理器
        var caughtException: Throwable? = null
        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
            caughtException = exception
        }

        testService.onCreate()
        testService.setExceptionHandler(exceptionHandler)

        // When: 协程中抛出异常
        val job = testService.getCoroutineScope().launch {
            throw RuntimeException("Test exception")
        }

        job.join()

        // Then: 验证异常被捕获
        assertNotNull(caughtException)
        assertEquals("Test exception", caughtException?.message)
        assertTrue(testService.isRunning()) // 服务仍在运行
    }

    /**
     * 测试Service重启限制
     */
    @Test
    fun `test service restart limit`() = runTest(testDispatcher) {
        // Given: 设置最大重启次数
        testService.setMaxRestartCount(3)
        testService.onCreate()

        // When: 多次崩溃重启
        repeat(4) {
            testService.simulateException()
            testService.handleExceptionAndRecover()
        }

        // Then: 验证达到限制后不再重启
        assertEquals(3, testService.getRestartCount())
        assertFalse(testService.isRunning())
    }

    /**
     * 测试前台服务通知
     */
    @Test
    fun `test foreground service notification`() {
        // When: 启动前台服务
        testService.onCreate()
        testService.startForegroundService()

        // Then: 验证前台服务已启动
        assertTrue(testService.isForeground())
        assertNotNull(testService.getForegroundNotification())
    }

    /**
     * 测试服务绑定
     */
    @Test
    fun `test service binding`() {
        // When: 绑定服务
        testService.onCreate()
        val binder = testService.onBind(mockIntent)

        // Then: 验证绑定成功
        assertNotNull(binder)
        assertTrue(testService.isBound())

        // When: 解绑服务
        val rebind = testService.onUnbind(mockIntent)

        // Then: 验证解绑成功
        assertFalse(testService.isBound())
        assertTrue(rebind) // 允许重新绑定
    }

    /**
     * 测试内存不足时的处理
     */
    @Test
    fun `test low memory handling`() {
        // Given: 服务运行中
        testService.onCreate()
        testService.startMonitoring()

        // When: 系统内存不足
        testService.onLowMemory()

        // Then: 验证服务进行了清理
        assertTrue(testService.hasPerformedLowMemoryCleanup())
        assertTrue(testService.isRunningInReducedMode())
    }

    /**
     * 测试任务调度
     */
    @Test
    fun `test task scheduling`() = runTest(testDispatcher) {
        // Given: 启动服务
        testService.onCreate()

        // When: 调度定时任务
        val taskId = testService.scheduleRepeatingTask(1000L) {
            // 执行任务
        }

        // Then: 验证任务已调度
        assertTrue(testService.isTaskScheduled(taskId))

        // When: 取消任务
        testService.cancelTask(taskId)

        // Then: 验证任务已取消
        assertFalse(testService.isTaskScheduled(taskId))
    }

    /**
     * 测试服务状态持久化
     */
    @Test
    fun `test service state persistence`() {
        // Given: 服务运行并有状态
        testService.onCreate()
        testService.updateState("monitoring", true)
        testService.updateState("optimization_level", 5)

        // When: 保存状态
        testService.saveState()

        // Then: 验证状态已保存
        val savedState = testService.getSavedState()
        assertEquals(true, savedState["monitoring"])
        assertEquals(5, savedState["optimization_level"])

        // When: 恢复状态
        testService.restoreState()

        // Then: 验证状态已恢复
        assertEquals(true, testService.getState("monitoring"))
        assertEquals(5, testService.getState("optimization_level"))
    }
}

/**
 * 可测试的监控服务实现
 */
class TestableMonitoringService : Service() {

    private var isRunning = false
    private var isForeground = false
    private var isBound = false
    private var restartCount = 0
    private var maxRestartCount = Int.MAX_VALUE
    private var lowMemoryCleanup = false
    private var reducedMode = false

    private lateinit var serviceScope: CompletableJob
    private lateinit var coroutineScope: CoroutineScope
    private var exceptionHandler: CoroutineExceptionHandler? = null

    private val registeredReceivers = mutableMapOf<BroadcastReceiver, IntentFilter>()
    private val scheduledTasks = mutableMapOf<String, Job>()
    private val serviceState = mutableMapOf<String, Any>()
    private val savedState = mutableMapOf<String, Any>()

    override fun onCreate() {
        super.onCreate()
        serviceScope = SupervisorJob()
        coroutineScope = CoroutineScope(
            Dispatchers.Default + serviceScope + (exceptionHandler ?: EmptyCoroutineContext)
        )
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        isBound = true
        return TestBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isBound = false
        return true // 允许重新绑定
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()

        // 注销所有广播接收器
        registeredReceivers.keys.forEach { receiver ->
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                // 忽略
            }
        }
        registeredReceivers.clear()

        // 取消所有任务
        scheduledTasks.values.forEach { it.cancel() }
        scheduledTasks.clear()

        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        performLowMemoryCleanup()
    }

    fun isRunning() = isRunning
    fun isForeground() = isForeground
    fun isBound() = isBound
    fun getCoroutineScope() = serviceScope
    fun getRestartCount() = restartCount

    fun registerBroadcastReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
        registeredReceivers[receiver] = filter
        // 实际注册需要Context
    }

    fun unregisterBroadcastReceiver(receiver: BroadcastReceiver) {
        registeredReceivers.remove(receiver)
    }

    fun isBroadcastReceiverRegistered(receiver: BroadcastReceiver) =
        registeredReceivers.containsKey(receiver)

    fun getRegisteredReceiverCount() = registeredReceivers.size

    fun setExceptionHandler(handler: CoroutineExceptionHandler) {
        exceptionHandler = handler
        // 重新创建scope
        coroutineScope = CoroutineScope(
            Dispatchers.Default + serviceScope + handler
        )
    }

    fun simulateException() {
        isRunning = false
    }

    fun handleExceptionAndRecover(): Boolean {
        return if (restartCount < maxRestartCount) {
            restartCount++
            isRunning = true
            true
        } else {
            false
        }
    }

    fun setMaxRestartCount(count: Int) {
        maxRestartCount = count
    }

    fun startForegroundService() {
        isForeground = true
    }

    fun getForegroundNotification(): Any? {
        return if (isForeground) "Notification" else null
    }

    fun startMonitoring() {
        // 开始监控
    }

    private fun performLowMemoryCleanup() {
        lowMemoryCleanup = true
        reducedMode = true
    }

    fun hasPerformedLowMemoryCleanup() = lowMemoryCleanup
    fun isRunningInReducedMode() = reducedMode

    fun scheduleRepeatingTask(interval: Long, task: () -> Unit): String {
        val taskId = "task_${System.currentTimeMillis()}"
        val job = coroutineScope.launch {
            while (isActive) {
                task()
                delay(interval)
            }
        }
        scheduledTasks[taskId] = job
        return taskId
    }

    fun cancelTask(taskId: String) {
        scheduledTasks[taskId]?.cancel()
        scheduledTasks.remove(taskId)
    }

    fun isTaskScheduled(taskId: String) = scheduledTasks.containsKey(taskId)

    fun updateState(key: String, value: Any) {
        serviceState[key] = value
    }

    fun getState(key: String) = serviceState[key]

    fun saveState() {
        savedState.clear()
        savedState.putAll(serviceState)
    }

    fun getSavedState() = savedState.toMap()

    fun restoreState() {
        serviceState.clear()
        serviceState.putAll(savedState)
    }

    inner class TestBinder : IBinder {
        override fun getInterfaceDescriptor() = "TestBinder"
        override fun pingBinder() = true
        override fun isBinderAlive() = true
        override fun queryLocalInterface(descriptor: String?) = null
        override fun transact(code: Int, data: android.os.Parcel, reply: android.os.Parcel?, flags: Int) = false
        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) {}
        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int) = false
        override fun dump(fd: java.io.FileDescriptor, args: Array<out String>?) {}
        override fun dumpAsync(fd: java.io.FileDescriptor, args: Array<out String>?) {}
    }
}

/**
 * 测试用广播接收器
 */
class TestBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // 测试用，不做任何处理
    }
}