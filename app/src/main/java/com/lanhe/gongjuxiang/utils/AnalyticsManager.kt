package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 蓝河助手 - 分析统计管理器
 *
 * 功能特性：
 * - 事件追踪统计
 * - 性能监控记录
 * - 用户行为分析
 * - 崩溃报告收集
 * - 自定义事件日志
 * - 会话时长统计
 */
class AnalyticsManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AnalyticsManager"

        @Volatile
        private var INSTANCE: AnalyticsManager? = null

        fun getInstance(context: Context): AnalyticsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnalyticsManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // 预定义事件名称
        const val EVENT_APP_LAUNCH = "app_launch"
        const val EVENT_FEATURE_USED = "feature_used"
        const val EVENT_OPTIMIZATION_PERFORMED = "optimization_performed"
        const val EVENT_SYSTEM_SCAN = "system_scan"
        const val EVENT_PERFORMANCE_BOOST = "performance_boost"
        const val EVENT_SECURITY_CHECK = "security_check"
        const val EVENT_NETWORK_DIAGNOSTIC = "network_diagnostic"
        const val EVENT_BATTERY_OPTIMIZATION = "battery_optimization"
        const val EVENT_MEMORY_CLEANUP = "memory_cleanup"
        const val EVENT_STORAGE_CLEANUP = "storage_cleanup"
        const val EVENT_APP_CRASH = "app_crash"
        const val EVENT_ERROR_OCCURRED = "error_occurred"
        const val EVENT_USER_ENGAGEMENT = "user_engagement"
        const val EVENT_FEATURE_DISCOVERY = "feature_discovery"
        const val EVENT_TUTORIAL_COMPLETED = "tutorial_completed"

        // 预定义参数名称
        const val PARAM_FEATURE_NAME = "feature_name"
        const val PARAM_OPTIMIZATION_TYPE = "optimization_type"
        const val PARAM_SUCCESS = "success"
        const val PARAM_DURATION = "duration"
        const val PARAM_IMPROVEMENT = "improvement"
        const val PARAM_ERROR_TYPE = "error_type"
        const val PARAM_ERROR_MESSAGE = "error_message"
        const val PARAM_SCREEN_NAME = "screen_name"
        const val PARAM_ACTION_TYPE = "action_type"
        const val PARAM_USER_TYPE = "user_type"
    }

    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics
    private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance()

    // 会话管理
    private var sessionStartTime: Long = 0
    private val sessionEvents: MutableList<String> = mutableListOf()

    // 事件缓存
    private val eventCache = ConcurrentHashMap<String, AtomicInteger>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        initializeAnalytics()
    }

    /**
     * 初始化分析统计
     */
    private fun initializeAnalytics() {
        try {
            // 设置用户属性
            setUserProperties()

            // 开始会话
            startSession()

            Log.d(TAG, "Analytics initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize analytics", e)
        }
    }

    /**
     * 设置用户属性
     */
    private fun setUserProperties() {
        try {
            val preferences = PreferencesManager.getInstance(context)

            firebaseAnalytics.setUserProperty("app_version", getAppVersion())
            firebaseAnalytics.setUserProperty("device_model", android.os.Build.MODEL)
            firebaseAnalytics.setUserProperty("android_version", android.os.Build.VERSION.RELEASE)
            firebaseAnalytics.setUserProperty("user_type", getUserType())
            firebaseAnalytics.setUserProperty("shizuku_enabled",
                if (ShizukuManager.isShizukuAvailable()) "true" else "false")

            // 设置Crashlytics用户ID
            val userId = preferences.getString("user_id", "anonymous_${System.currentTimeMillis()}")
            crashlytics.setUserId(userId)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user properties", e)
        }
    }

    /**
     * 开始会话
     */
    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
        sessionEvents.clear()

        trackEvent(EVENT_APP_LAUNCH, Bundle().apply {
            putLong("session_start_time", sessionStartTime)
            putString("launch_mode", "normal")
        })
    }

    /**
     * 结束会话
     */
    fun endSession() {
        val sessionDuration = System.currentTimeMillis() - sessionStartTime

        trackEvent(EVENT_USER_ENGAGEMENT, Bundle().apply {
            putLong("session_duration", sessionDuration)
            putInt("events_count", sessionEvents.size)
            putString("events_list", sessionEvents.joinToString(","))
        })

        // 上传缓存的事件
        uploadCachedEvents()
    }

    /**
     * 追踪事件
     */
    fun trackEvent(eventName: String, parameters: Bundle? = null) {
        try {
            // 添加通用参数
            val eventBundle = Bundle().apply {
                parameters?.let { putAll(it) }
                putLong("timestamp", System.currentTimeMillis())
                putString("session_id", getSessionId())
            }

            // 发送到Firebase Analytics
            firebaseAnalytics.logEvent(eventName, eventBundle)

            // 添加到会话事件列表
            sessionEvents.add(eventName)

            // 更新事件计数缓存
            eventCache.getOrPut(eventName) { AtomicInteger(0) }.incrementAndGet()

            Log.d(TAG, "Event tracked: $eventName with parameters: $parameters")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to track event: $eventName", e)
        }
    }

    /**
     * 追踪功能使用
     */
    fun trackFeatureUsed(featureName: String, success: Boolean = true, duration: Long? = null) {
        trackEvent(EVENT_FEATURE_USED, Bundle().apply {
            putString(PARAM_FEATURE_NAME, featureName)
            putBoolean(PARAM_SUCCESS, success)
            duration?.let { putLong(PARAM_DURATION, it) }
        })
    }

    /**
     * 追踪优化操作
     */
    fun trackOptimization(optimizationType: String, success: Boolean, improvement: String? = null, duration: Long? = null) {
        trackEvent(EVENT_OPTIMIZATION_PERFORMED, Bundle().apply {
            putString(PARAM_OPTIMIZATION_TYPE, optimizationType)
            putBoolean(PARAM_SUCCESS, success)
            improvement?.let { putString(PARAM_IMPROVEMENT, it) }
            duration?.let { putLong(PARAM_DURATION, it) }
        })
    }

    /**
     * 追踪屏幕浏览
     */
    fun trackScreenView(screenName: String, screenClass: String? = null) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
        })
    }

    /**
     * 追踪错误
     */
    fun trackError(errorType: String, errorMessage: String, throwable: Throwable? = null) {
        // Firebase Analytics事件
        trackEvent(EVENT_ERROR_OCCURRED, Bundle().apply {
            putString(PARAM_ERROR_TYPE, errorType)
            putString(PARAM_ERROR_MESSAGE, errorMessage)
        })

        // Firebase Crashlytics记录
        crashlytics.recordException(throwable ?: Exception("$errorType: $errorMessage"))

        // 设置自定义键值
        crashlytics.setCustomKey("error_type", errorType)
        crashlytics.setCustomKey("error_context", errorMessage)
    }

    /**
     * 追踪性能数据
     */
    fun trackPerformance(metricName: String, value: Double, unit: String? = null) {
        coroutineScope.launch {
            try {
                trackEvent("performance_metric", Bundle().apply {
                    putString("metric_name", metricName)
                    putDouble("metric_value", value)
                    unit?.let { putString("metric_unit", it) }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to track performance metric", e)
            }
        }
    }

    /**
     * 追踪用户行为
     */
    fun trackUserBehavior(action: String, target: String? = null, value: String? = null) {
        trackEvent("user_behavior", Bundle().apply {
            putString("action", action)
            target?.let { putString("target", it) }
            value?.let { putString("value", it) }
        })
    }

    /**
     * 设置自定义维度
     */
    fun setCustomDimension(key: String, value: String) {
        firebaseAnalytics.setUserProperty(key, value)
        crashlytics.setCustomKey(key, value)
    }

    /**
     * 记录崩溃信息
     */
    fun recordCrash(throwable: Throwable, context: String? = null) {
        context?.let { crashlytics.log(it) }
        crashlytics.recordException(throwable)

        trackEvent(EVENT_APP_CRASH, Bundle().apply {
            putString("crash_type", throwable.javaClass.simpleName)
            putString("crash_message", throwable.message ?: "Unknown error")
            context?.let { putString("crash_context", it) }
        })
    }

    /**
     * 获取事件统计
     */
    fun getEventStats(): Map<String, Int> {
        return eventCache.mapValues { it.value.get() }
    }

    /**
     * 清除缓存统计
     */
    fun clearStats() {
        eventCache.clear()
    }

    /**
     * 上传缓存的事件
     */
    private fun uploadCachedEvents() {
        coroutineScope.launch {
            try {
                // 这里可以实现批量上传逻辑
                Log.d(TAG, "Uploading cached events: ${eventCache.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload cached events", e)
            }
        }
    }

    /**
     * 获取会话ID
     */
    private fun getSessionId(): String {
        return "session_$sessionStartTime"
    }

    /**
     * 获取应用版本
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * 获取用户类型
     */
    private fun getUserType(): String {
        return if (ShizukuManager.isShizukuAvailable()) {
            "advanced"
        } else {
            "basic"
        }
    }

    /**
     * 启用/禁用分析统计
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }

    /**
     * 构建器模式
     */
    class EventBuilder(private val analyticsManager: AnalyticsManager) {
        private var eventName: String = ""
        private val parameters = Bundle()

        fun event(name: String): EventBuilder {
            this.eventName = name
            return this
        }

        fun param(key: String, value: String): EventBuilder {
            parameters.putString(key, value)
            return this
        }

        fun param(key: String, value: Int): EventBuilder {
            parameters.putInt(key, value)
            return this
        }

        fun param(key: String, value: Long): EventBuilder {
            parameters.putLong(key, value)
            return this
        }

        fun param(key: String, value: Boolean): EventBuilder {
            parameters.putBoolean(key, value)
            return this
        }

        fun param(key: String, value: Double): EventBuilder {
            parameters.putDouble(key, value)
            return this
        }

        fun track() {
            analyticsManager.trackEvent(eventName, parameters)
        }
    }

    /**
     * 创建事件构建器
     */
    fun buildEvent(): EventBuilder {
        return EventBuilder(this)
    }
}

/**
 * 扩展函数，简化使用
 */
fun Context.analytics(): AnalyticsManager = AnalyticsManager.getInstance(this)

/**
 * 简化的事件追踪函数
 */
inline fun AnalyticsManager.event(name: String, builder: Bundle.() -> Unit = {}) {
    val bundle = Bundle().apply(builder)
    trackEvent(name, bundle)
}