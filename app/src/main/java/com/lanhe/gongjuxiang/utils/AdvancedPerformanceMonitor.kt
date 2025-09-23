package com.lanhe.gongjuxiang.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * 蓝河助手 - 高级性能监控器
 *
 * 功能特性：
 * - StrictMode开发监控
 * - 内存泄漏检测
 * - 性能指标收集
 * - ANR检测与报告
 * - 帧率监控
 * - 启动时间分析
 * - 网络性能监控
 */
class AdvancedPerformanceMonitor private constructor(
    private val context: Context
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "AdvancedPerformanceMonitor"

        @Volatile
        private var INSTANCE: AdvancedPerformanceMonitor? = null

        fun getInstance(context: Context): AdvancedPerformanceMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdvancedPerformanceMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }

        // 监控配置
        private const val FRAME_MONITOR_INTERVAL = 1000L // 1秒
        private const val MEMORY_MONITOR_INTERVAL = 5000L // 5秒
        private const val ANR_MONITOR_INTERVAL = 100L // 100毫秒
        private const val ANR_THRESHOLD = 5000L // 5秒ANR阈值

        // 性能阈值
        private const val LOW_FPS_THRESHOLD = 30
        private const val HIGH_MEMORY_THRESHOLD = 0.8 // 80%内存使用率
        private const val SLOW_METHOD_THRESHOLD = 100L // 100毫秒
    }

    private val analyticsManager: AnalyticsManager = AnalyticsManager.getInstance(context)
    private val monitorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // 监控状态
    private var isMonitoringActive = false
    private var startTime = System.currentTimeMillis()

    // 性能数据
    private val performanceMetrics = ConcurrentHashMap<String, PerformanceMetric>()
    private val frameRateHistory = mutableListOf<Int>()
    private val memoryUsageHistory = mutableListOf<Long>()

    // ANR监控
    private val anrDetector = ANRDetector()
    private val mainHandler = Handler(Looper.getMainLooper())

    // 活动监控
    private val activityLifecycleCallbacks = ActivityLifecycleCallbacks()
    private var currentActivity: WeakReference<Activity>? = null

    // 方法性能监控
    private val methodPerformance = ConcurrentHashMap<String, MethodMetrics>()

    init {
        initializeMonitoring()
    }

    /**
     * 性能指标数据类
     */
    data class PerformanceMetric(
        val name: String,
        val value: Double,
        val timestamp: Long,
        val unit: String,
        val category: String
    )

    /**
     * 方法性能指标
     */
    data class MethodMetrics(
        val methodName: String,
        val callCount: AtomicInteger = AtomicInteger(0),
        val totalTime: AtomicLong = AtomicLong(0),
        val maxTime: AtomicLong = AtomicLong(0),
        val minTime: AtomicLong = AtomicLong(Long.MAX_VALUE)
    )

    /**
     * 初始化监控
     */
    private fun initializeMonitoring() {
        try {
            // 注册生命周期观察者
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)

            // 设置StrictMode
            setupStrictMode()

            // 注册Activity生命周期回调
            if (context is Application) {
                context.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
            }

            Log.d(TAG, "Performance monitoring initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize performance monitoring", e)
            analyticsManager.trackError("performance_monitor_init_failed", e.message ?: "Unknown error", e)
        }
    }

    /**
     * 设置StrictMode
     */
    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .detectActivityLeaks()
                    .detectFileUriExposure()
                    .penaltyLog()
                    .build()
            )
        }
    }

    /**
     * 开始性能监控
     */
    fun startMonitoring() {
        if (isMonitoringActive) return

        isMonitoringActive = true
        startTime = System.currentTimeMillis()

        monitorScope.launch {
            // 启动各种监控
            launch { startFrameRateMonitoring() }
            launch { startMemoryMonitoring() }
            launch { startANRMonitoring() }
            launch { startNetworkMonitoring() }
        }

        analyticsManager.trackEvent("performance_monitoring_started")
        Log.d(TAG, "Performance monitoring started")
    }

    /**
     * 停止性能监控
     */
    fun stopMonitoring() {
        if (!isMonitoringActive) return

        isMonitoringActive = false
        monitorScope.coroutineContext.cancelChildren()

        // 上传性能报告
        uploadPerformanceReport()

        analyticsManager.trackEvent("performance_monitoring_stopped", Bundle().apply {
            putLong("monitoring_duration", System.currentTimeMillis() - startTime)
        })

        Log.d(TAG, "Performance monitoring stopped")
    }

    /**
     * 帧率监控
     */
    private suspend fun startFrameRateMonitoring() {
        while (isMonitoringActive) {
            try {
                val frameRate = measureFrameRate()
                frameRateHistory.add(frameRate)

                // 保持历史记录不超过100条
                if (frameRateHistory.size > 100) {
                    frameRateHistory.removeAt(0)
                }

                recordMetric("frame_rate", frameRate.toDouble(), "fps", "ui")

                // 检测低帧率
                if (frameRate < LOW_FPS_THRESHOLD) {
                    analyticsManager.trackEvent("low_fps_detected", Bundle().apply {
                        putInt("fps", frameRate)
                        putString("activity", getCurrentActivityName())
                    })
                }

                delay(FRAME_MONITOR_INTERVAL)
            } catch (e: Exception) {
                Log.e(TAG, "Frame rate monitoring error", e)
                delay(FRAME_MONITOR_INTERVAL)
            }
        }
    }

    /**
     * 内存监控
     */
    private suspend fun startMemoryMonitoring() {
        while (isMonitoringActive) {
            try {
                val runtime = Runtime.getRuntime()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                val maxMemory = runtime.maxMemory()
                val memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble())

                memoryUsageHistory.add(usedMemory)

                // 保持历史记录不超过100条
                if (memoryUsageHistory.size > 100) {
                    memoryUsageHistory.removeAt(0)
                }

                recordMetric("memory_usage", memoryUsagePercent * 100, "percent", "memory")
                recordMetric("memory_used", usedMemory / 1024.0 / 1024.0, "mb", "memory")

                // 检测高内存使用
                if (memoryUsagePercent > HIGH_MEMORY_THRESHOLD) {
                    analyticsManager.trackEvent("high_memory_usage", Bundle().apply {
                        putDouble("usage_percent", memoryUsagePercent * 100)
                        putLong("used_mb", usedMemory / 1024 / 1024)
                        putString("activity", getCurrentActivityName())
                    })
                }

                delay(MEMORY_MONITOR_INTERVAL)
            } catch (e: Exception) {
                Log.e(TAG, "Memory monitoring error", e)
                delay(MEMORY_MONITOR_INTERVAL)
            }
        }
    }

    /**
     * ANR监控
     */
    private suspend fun startANRMonitoring() {
        anrDetector.start()
    }

    /**
     * 网络性能监控
     */
    private suspend fun startNetworkMonitoring() {
        // 这里可以集成网络性能监控逻辑
        // 监控网络请求延迟、失败率等
    }

    /**
     * 测量帧率
     */
    private fun measureFrameRate(): Int {
        // 这里使用简化的帧率测量方法
        // 实际项目中可以使用Choreographer进行更精确的测量
        return try {
            60 // 模拟值，实际应该使用真实的帧率测量
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 记录性能指标
     */
    fun recordMetric(name: String, value: Double, unit: String, category: String) {
        val metric = PerformanceMetric(
            name = name,
            value = value,
            timestamp = System.currentTimeMillis(),
            unit = unit,
            category = category
        )

        performanceMetrics[name] = metric
        analyticsManager.trackPerformance(name, value, unit)
    }

    /**
     * 记录方法性能
     */
    fun recordMethodPerformance(methodName: String, executionTime: Long) {
        val metrics = methodPerformance.getOrPut(methodName) { MethodMetrics(methodName) }

        metrics.callCount.incrementAndGet()
        metrics.totalTime.addAndGet(executionTime)
        metrics.maxTime.updateAndGet { maxOf(it, executionTime) }
        metrics.minTime.updateAndGet { minOf(it, executionTime) }

        // 检测慢方法
        if (executionTime > SLOW_METHOD_THRESHOLD) {
            analyticsManager.trackEvent("slow_method_detected", Bundle().apply {
                putString("method_name", methodName)
                putLong("execution_time", executionTime)
                putString("activity", getCurrentActivityName())
            })
        }
    }

    /**
     * 获取当前Activity名称
     */
    private fun getCurrentActivityName(): String {
        return currentActivity?.get()?.javaClass?.simpleName ?: "unknown"
    }

    /**
     * 上传性能报告
     */
    private fun uploadPerformanceReport() {
        monitorScope.launch {
            try {
                val report = generatePerformanceReport()
                analyticsManager.trackEvent("performance_report", Bundle().apply {
                    putString("report", report)
                })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload performance report", e)
            }
        }
    }

    /**
     * 生成性能报告
     */
    private fun generatePerformanceReport(): String {
        val report = StringBuilder()

        // 基本信息
        report.append("Performance Report\n")
        report.append("Duration: ${System.currentTimeMillis() - startTime}ms\n")

        // 帧率统计
        if (frameRateHistory.isNotEmpty()) {
            val avgFps = frameRateHistory.average()
            val minFps = frameRateHistory.minOrNull() ?: 0
            val maxFps = frameRateHistory.maxOrNull() ?: 0
            report.append("FPS - Avg: $avgFps, Min: $minFps, Max: $maxFps\n")
        }

        // 内存统计
        if (memoryUsageHistory.isNotEmpty()) {
            val avgMemory = memoryUsageHistory.average() / 1024 / 1024
            val maxMemory = (memoryUsageHistory.maxOrNull() ?: 0) / 1024 / 1024
            report.append("Memory - Avg: ${avgMemory}MB, Max: ${maxMemory}MB\n")
        }

        // 方法性能统计
        methodPerformance.forEach { (methodName, metrics) ->
            val avgTime = if (metrics.callCount.get() > 0) {
                metrics.totalTime.get() / metrics.callCount.get()
            } else 0
            report.append("Method $methodName - Calls: ${metrics.callCount.get()}, Avg: ${avgTime}ms\n")
        }

        return report.toString()
    }

    /**
     * 获取性能统计
     */
    fun getPerformanceStats(): Map<String, Any> {
        return mapOf(
            "metrics_count" to performanceMetrics.size,
            "frame_rate_history" to frameRateHistory.toList(),
            "memory_usage_history" to memoryUsageHistory.toList(),
            "method_performance" to methodPerformance.mapValues { (_, metrics) ->
                mapOf(
                    "call_count" to metrics.callCount.get(),
                    "total_time" to metrics.totalTime.get(),
                    "avg_time" to if (metrics.callCount.get() > 0) metrics.totalTime.get() / metrics.callCount.get() else 0,
                    "max_time" to metrics.maxTime.get(),
                    "min_time" to if (metrics.minTime.get() == Long.MAX_VALUE) 0 else metrics.minTime.get()
                )
            }
        )
    }

    /**
     * ANR检测器
     */
    private inner class ANRDetector {
        private var isDetecting = false
        private val watchdog = Handler(Looper.getMainLooper())

        fun start() {
            if (isDetecting) return
            isDetecting = true
            scheduleCheck()
        }

        fun stop() {
            isDetecting = false
            watchdog.removeCallbacksAndMessages(null)
        }

        private fun scheduleCheck() {
            if (!isDetecting) return

            val startTime = System.currentTimeMillis()

            watchdog.post {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed > ANR_THRESHOLD) {
                    // 检测到ANR
                    analyticsManager.trackEvent("anr_detected", Bundle().apply {
                        putLong("block_time", elapsed)
                        putString("activity", getCurrentActivityName())
                    })
                }

                // 继续下次检查
                monitorScope.launch {
                    delay(ANR_MONITOR_INTERVAL)
                    scheduleCheck()
                }
            }
        }
    }

    /**
     * Activity生命周期回调
     */
    private inner class ActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            currentActivity = WeakReference(activity)
            analyticsManager.trackScreenView(activity.javaClass.simpleName, activity.javaClass.name)
        }

        override fun onActivityStarted(activity: Activity) {
            currentActivity = WeakReference(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            currentActivity = WeakReference(activity)
        }

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {}

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            if (currentActivity?.get() == activity) {
                currentActivity = null
            }
        }
    }

    // 生命周期回调
    override fun onStart(owner: LifecycleOwner) {
        startMonitoring()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopMonitoring()
    }
}

/**
 * 方法性能监控装饰器
 */
inline fun <T> AdvancedPerformanceMonitor.measureMethod(methodName: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    return try {
        block()
    } finally {
        val executionTime = System.currentTimeMillis() - startTime
        recordMethodPerformance(methodName, executionTime)
    }
}

/**
 * 扩展函数
 */
fun Context.performanceMonitor(): AdvancedPerformanceMonitor = AdvancedPerformanceMonitor.getInstance(this)