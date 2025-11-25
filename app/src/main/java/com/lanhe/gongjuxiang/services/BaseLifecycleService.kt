package com.lanhe.gongjuxiang.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 基础生命周期管理Service
 * 提供完整的资源管理、异常处理、协程管理和恢复机制
 */
abstract class BaseLifecycleService : Service() {

    companion object {
        private const val INIT_TIMEOUT_MS = 30000L // 初始化超时时间30秒
    }

    // 协程作用域 - 使用SupervisorJob确保一个协程失败不影响其他
    protected val serviceScope = CoroutineScope(
        Dispatchers.IO +
        SupervisorJob() +
        CoroutineExceptionHandler { _, throwable ->
            handleCoroutineException(throwable)
        }
    )

    // Handler管理
    private val mainHandler = Handler(Looper.getMainLooper())
    private val handlerTasks = mutableListOf<Runnable>()

    // BroadcastReceiver管理
    private val registeredReceivers = ConcurrentHashMap<BroadcastReceiver, Boolean>()

    // 监听器管理
    private val listeners = mutableListOf<Any>()

    // 生命周期标记
    @Volatile
    protected var isServiceRunning = false

    @Volatile
    protected var isInitialized = false

    protected abstract fun getServiceTag(): String

    /**
     * 子类需要实现的初始化逻辑
     * @return 初始化是否成功
     */
    protected abstract suspend fun onInitialize(): Boolean

    /**
     * 子类需要实现的清理逻辑
     */
    protected abstract fun onCleanup()

    /**
     * 处理任务被移除的情况
     */
    protected open fun onTaskRemovedHandle() {
        // 默认重启服务
        Log.w(getServiceTag(), "任务被移除，准备重启服务")
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true

        serviceScope.launch {
            try {
                // 使用withTimeout防止ANR
                withTimeout(INIT_TIMEOUT_MS) {
                    Log.d(getServiceTag(), "开始初始化服务")
                    isInitialized = onInitialize()
                    if (isInitialized) {
                        Log.i(getServiceTag(), "服务初始化成功")
                    } else {
                        Log.e(getServiceTag(), "服务初始化失败")
                        handleInitializationFailure()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(getServiceTag(), "服务初始化超时", e)
                handleInitializationFailure()
            } catch (e: Exception) {
                Log.e(getServiceTag(), "服务初始化异常", e)
                handleInitializationFailure()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(getServiceTag(), "onStartCommand called, flags: $flags, startId: $startId")

        // 处理服务重启的情况
        if (flags and START_FLAG_REDELIVERY != 0) {
            Log.i(getServiceTag(), "服务被系统重启")
            handleServiceRestart(intent)
        }

        // 返回START_STICKY确保服务被杀后能自动重启
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(getServiceTag(), "onDestroy called")
        isServiceRunning = false

        // 清理所有资源
        cleanupAllResources()

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(getServiceTag(), "应用从最近任务中被移除")
        onTaskRemovedHandle()
    }

    /**
     * 注册BroadcastReceiver并记录
     */
    protected fun registerReceiverSafely(receiver: BroadcastReceiver, filter: android.content.IntentFilter) {
        try {
            registerReceiver(receiver, filter)
            registeredReceivers[receiver] = true
            Log.d(getServiceTag(), "成功注册BroadcastReceiver: ${receiver.javaClass.simpleName}")
        } catch (e: Exception) {
            Log.e(getServiceTag(), "注册BroadcastReceiver失败", e)
        }
    }

    /**
     * 安全注销BroadcastReceiver
     */
    protected fun unregisterReceiverSafely(receiver: BroadcastReceiver) {
        try {
            if (registeredReceivers.containsKey(receiver)) {
                unregisterReceiver(receiver)
                registeredReceivers.remove(receiver)
                Log.d(getServiceTag(), "成功注销BroadcastReceiver: ${receiver.javaClass.simpleName}")
            }
        } catch (e: IllegalArgumentException) {
            // 接收器未注册，忽略
            Log.w(getServiceTag(), "尝试注销未注册的BroadcastReceiver: ${receiver.javaClass.simpleName}")
        } catch (e: Exception) {
            Log.e(getServiceTag(), "注销BroadcastReceiver失败", e)
        }
    }

    /**
     * 安全地发布延迟任务
     */
    protected fun postDelayedSafely(runnable: Runnable, delayMillis: Long): Boolean {
        return try {
            handlerTasks.add(runnable)
            mainHandler.postDelayed(runnable, delayMillis)
        } catch (e: Exception) {
            Log.e(getServiceTag(), "发布延迟任务失败", e)
            false
        }
    }

    /**
     * 添加监听器到管理列表
     */
    protected fun addListener(listener: Any) {
        listeners.add(listener)
    }

    /**
     * 移除监听器
     */
    protected fun removeListener(listener: Any) {
        listeners.remove(listener)
    }

    /**
     * 清理所有资源
     */
    private fun cleanupAllResources() {
        Log.d(getServiceTag(), "开始清理所有资源")

        // 1. 取消所有协程
        try {
            serviceScope.cancel("Service destroyed")
            Log.d(getServiceTag(), "已取消所有协程")
        } catch (e: Exception) {
            Log.e(getServiceTag(), "取消协程时出错", e)
        }

        // 2. 注销所有BroadcastReceiver
        registeredReceivers.keys.forEach { receiver ->
            unregisterReceiverSafely(receiver)
        }
        registeredReceivers.clear()

        // 3. 清理所有Handler任务
        try {
            handlerTasks.forEach { runnable ->
                mainHandler.removeCallbacks(runnable)
            }
            handlerTasks.clear()
            Log.d(getServiceTag(), "已清理所有Handler任务")
        } catch (e: Exception) {
            Log.e(getServiceTag(), "清理Handler任务时出错", e)
        }

        // 4. 清理所有监听器
        listeners.clear()
        Log.d(getServiceTag(), "已清理所有监听器")

        // 5. 调用子类的清理逻辑
        try {
            onCleanup()
            Log.d(getServiceTag(), "子类清理逻辑执行完成")
        } catch (e: Exception) {
            Log.e(getServiceTag(), "执行子类清理逻辑时出错", e)
        }
    }

    /**
     * 处理初始化失败
     */
    private fun handleInitializationFailure() {
        Log.e(getServiceTag(), "处理初始化失败")
        // 可以选择重试或停止服务
        serviceScope.launch {
            delay(5000) // 5秒后重试
            if (isServiceRunning) {
                Log.i(getServiceTag(), "尝试重新初始化")
                try {
                    withTimeout(INIT_TIMEOUT_MS) {
                        isInitialized = onInitialize()
                    }
                } catch (e: Exception) {
                    Log.e(getServiceTag(), "重新初始化失败，停止服务", e)
                    stopSelf()
                }
            }
        }
    }

    /**
     * 处理服务重启
     */
    private fun handleServiceRestart(intent: Intent?) {
        Log.i(getServiceTag(), "处理服务重启")
        // 重新初始化
        if (!isInitialized) {
            serviceScope.launch {
                try {
                    withTimeout(INIT_TIMEOUT_MS) {
                        isInitialized = onInitialize()
                    }
                } catch (e: Exception) {
                    Log.e(getServiceTag(), "服务重启初始化失败", e)
                }
            }
        }
    }

    /**
     * 处理协程异常
     */
    private fun handleCoroutineException(throwable: Throwable) {
        Log.e(getServiceTag(), "协程异常", throwable)

        // 根据异常类型决定处理方式
        when (throwable) {
            is CancellationException -> {
                // 协程被取消，正常情况
                Log.d(getServiceTag(), "协程被取消")
            }
            is OutOfMemoryError -> {
                // 内存不足，尝试清理
                Log.e(getServiceTag(), "内存不足，尝试清理")
                System.gc()
            }
            else -> {
                // 其他异常，记录并恢复
                Log.e(getServiceTag(), "未处理的异常: ${throwable.message}")
            }
        }
    }

    /**
     * 公共方法：关闭服务
     * 供外部调用，确保正确清理
     */
    fun destroy() {
        Log.i(getServiceTag(), "外部调用destroy()")
        stopSelf()
    }

    /**
     * 公共方法：关闭服务
     * 供外部调用，确保正确清理
     */
    fun close() {
        Log.i(getServiceTag(), "外部调用close()")
        stopSelf()
    }
}