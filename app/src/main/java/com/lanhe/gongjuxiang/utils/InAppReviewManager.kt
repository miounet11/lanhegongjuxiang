package com.lanhe.gongjuxiang.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 蓝河助手 - 应用内评价管理器
 *
 * 功能特性：
 * - Google Play应用内评价
 * - 智能评价提示策略
 * - 用户行为追踪
 * - 评价时机优化
 * - 评价结果分析
 * - 自定义评价流程
 */
class InAppReviewManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "InAppReviewManager"

        @Volatile
        private var INSTANCE: InAppReviewManager? = null

        fun getInstance(context: Context): InAppReviewManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: InAppReviewManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // 偏好设置键
        private const val PREF_REVIEW_REQUESTED = "review_requested"
        private const val PREF_REVIEW_COMPLETED = "review_completed"
        private const val PREF_REVIEW_DECLINED = "review_declined"
        private const val PREF_REVIEW_POSTPONED = "review_postponed"
        private const val PREF_FIRST_LAUNCH_TIME = "first_launch_time"
        private const val PREF_LAST_REVIEW_REQUEST = "last_review_request"
        private const val PREF_APP_LAUNCH_COUNT = "app_launch_count"
        private const val PREF_FEATURE_USAGE_COUNT = "feature_usage_count"
        private const val PREF_OPTIMIZATION_COUNT = "optimization_count"

        // 评价策略配置
        private const val MIN_DAYS_SINCE_INSTALL = 3 // 安装后最少天数
        private const val MIN_LAUNCH_COUNT = 5 // 最少启动次数
        private const val MIN_FEATURE_USAGE = 10 // 最少功能使用次数
        private const val MIN_OPTIMIZATION_COUNT = 3 // 最少优化操作次数
        private const val REVIEW_COOLDOWN_DAYS = 30 // 评价请求冷却期（天）
        private const val MAX_REVIEW_ATTEMPTS = 3 // 最大评价尝试次数
    }

    private val reviewManager: ReviewManager = ReviewManagerFactory.create(context)
    private val analyticsManager = AnalyticsManager.getInstance(context)
    private val preferencesManager = PreferencesManager.getInstance(context)

    /**
     * 评价结果枚举
     */
    enum class ReviewResult {
        SUCCESS,            // 评价成功
        FAILED,             // 评价失败
        CANCELLED,          // 用户取消
        NOT_ELIGIBLE,       // 不符合条件
        ALREADY_REVIEWED,   // 已经评价过
        QUOTA_EXCEEDED      // 超出配额
    }

    /**
     * 评价配置
     */
    data class ReviewConfig(
        val forceShow: Boolean = false,           // 强制显示评价
        val customMinDays: Int? = null,           // 自定义最少天数
        val customMinLaunches: Int? = null,       // 自定义最少启动次数
        val customMinUsage: Int? = null,          // 自定义最少使用次数
        val trackingEnabled: Boolean = true       // 启用追踪
    )

    init {
        initializeReviewData()
    }

    /**
     * 初始化评价数据
     */
    private fun initializeReviewData() {
        // 记录首次启动时间
        if (preferencesManager.getLong(PREF_FIRST_LAUNCH_TIME, 0) == 0L) {
            preferencesManager.putLong(PREF_FIRST_LAUNCH_TIME, System.currentTimeMillis())
        }

        // 增加启动计数
        val launchCount = preferencesManager.getInt(PREF_APP_LAUNCH_COUNT, 0)
        preferencesManager.putInt(PREF_APP_LAUNCH_COUNT, launchCount + 1)

        analyticsManager.trackEvent("app_launch_for_review", android.os.Bundle().apply {
            putInt("launch_count", launchCount + 1)
        })
    }

    /**
     * 检查是否应该显示评价
     */
    fun shouldShowReview(config: ReviewConfig = ReviewConfig()): Boolean {
        try {
            // 强制显示
            if (config.forceShow) {
                Log.d(TAG, "Review forced to show")
                return true
            }

            // 检查是否已经完成评价
            if (preferencesManager.getBoolean(PREF_REVIEW_COMPLETED, false)) {
                Log.d(TAG, "Review already completed")
                return false
            }

            // 检查评价尝试次数
            val reviewAttempts = preferencesManager.getInt("review_attempts", 0)
            if (reviewAttempts >= MAX_REVIEW_ATTEMPTS) {
                Log.d(TAG, "Max review attempts reached")
                return false
            }

            // 检查冷却期
            val lastReviewRequest = preferencesManager.getLong(PREF_LAST_REVIEW_REQUEST, 0)
            val cooldownPeriod = REVIEW_COOLDOWN_DAYS * 24 * 60 * 60 * 1000L
            if (System.currentTimeMillis() - lastReviewRequest < cooldownPeriod) {
                Log.d(TAG, "Review in cooldown period")
                return false
            }

            // 检查安装天数
            val firstLaunchTime = preferencesManager.getLong(PREF_FIRST_LAUNCH_TIME, System.currentTimeMillis())
            val daysSinceInstall = (System.currentTimeMillis() - firstLaunchTime) / (24 * 60 * 60 * 1000)
            val minDays = config.customMinDays ?: MIN_DAYS_SINCE_INSTALL
            if (daysSinceInstall < minDays) {
                Log.d(TAG, "Not enough days since install: $daysSinceInstall < $minDays")
                return false
            }

            // 检查启动次数
            val launchCount = preferencesManager.getInt(PREF_APP_LAUNCH_COUNT, 0)
            val minLaunches = config.customMinLaunches ?: MIN_LAUNCH_COUNT
            if (launchCount < minLaunches) {
                Log.d(TAG, "Not enough app launches: $launchCount < $minLaunches")
                return false
            }

            // 检查功能使用次数
            val featureUsageCount = preferencesManager.getInt(PREF_FEATURE_USAGE_COUNT, 0)
            val minUsage = config.customMinUsage ?: MIN_FEATURE_USAGE
            if (featureUsageCount < minUsage) {
                Log.d(TAG, "Not enough feature usage: $featureUsageCount < $minUsage")
                return false
            }

            // 检查优化操作次数
            val optimizationCount = preferencesManager.getInt(PREF_OPTIMIZATION_COUNT, 0)
            if (optimizationCount < MIN_OPTIMIZATION_COUNT) {
                Log.d(TAG, "Not enough optimizations: $optimizationCount < $MIN_OPTIMIZATION_COUNT")
                return false
            }

            Log.d(TAG, "All conditions met for showing review")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error checking review eligibility", e)
            analyticsManager.trackError("review_eligibility_check_failed", e.message ?: "Unknown error", e)
            return false
        }
    }

    /**
     * 请求应用内评价
     */
    suspend fun requestReview(
        activity: Activity,
        config: ReviewConfig = ReviewConfig()
    ): ReviewResult = suspendCoroutine { continuation ->

        // 检查是否应该显示评价
        if (!shouldShowReview(config)) {
            continuation.resume(ReviewResult.NOT_ELIGIBLE)
            return@suspendCoroutine
        }

        try {
            // 请求评价信息
            reviewManager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    launchReviewFlow(activity, reviewInfo, config, continuation)
                } else {
                    Log.e(TAG, "Failed to request review flow", task.exception)
                    analyticsManager.trackError(
                        "review_request_failed",
                        task.exception?.message ?: "Unknown error",
                        task.exception
                    )
                    continuation.resume(ReviewResult.FAILED)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during review request", e)
            analyticsManager.trackError("review_request_exception", e.message ?: "Unknown error", e)
            continuation.resume(ReviewResult.FAILED)
        }
    }

    /**
     * 启动评价流程
     */
    private fun launchReviewFlow(
        activity: Activity,
        reviewInfo: ReviewInfo,
        config: ReviewConfig,
        continuation: kotlin.coroutines.Continuation<ReviewResult>
    ) {
        try {
            // 更新最后请求时间
            preferencesManager.putLong(PREF_LAST_REVIEW_REQUEST, System.currentTimeMillis())

            // 增加尝试次数
            val attempts = preferencesManager.getInt("review_attempts", 0)
            preferencesManager.putInt("review_attempts", attempts + 1)

            // 启动评价流程
            reviewManager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 评价流程完成（不代表用户实际评价了）
                    handleReviewCompleted(config)
                    continuation.resume(ReviewResult.SUCCESS)
                } else {
                    Log.e(TAG, "Failed to launch review flow", task.exception)
                    analyticsManager.trackError(
                        "review_launch_failed",
                        task.exception?.message ?: "Unknown error",
                        task.exception
                    )
                    continuation.resume(ReviewResult.FAILED)
                }
            }

            // 记录评价请求事件
            analyticsManager.trackEvent("review_requested", android.os.Bundle().apply {
                putInt("attempt_number", attempts + 1)
                putBoolean("force_show", config.forceShow)
            })

        } catch (e: Exception) {
            Log.e(TAG, "Exception during review launch", e)
            analyticsManager.trackError("review_launch_exception", e.message ?: "Unknown error", e)
            continuation.resume(ReviewResult.FAILED)
        }
    }

    /**
     * 处理评价完成
     */
    private fun handleReviewCompleted(config: ReviewConfig) {
        // 标记评价已请求
        preferencesManager.putBoolean(PREF_REVIEW_REQUESTED, true)

        // 记录评价完成事件
        if (config.trackingEnabled) {
            analyticsManager.trackEvent("review_flow_completed", android.os.Bundle().apply {
                putLong("timestamp", System.currentTimeMillis())
                putInt("launch_count", preferencesManager.getInt(PREF_APP_LAUNCH_COUNT, 0))
                putInt("feature_usage", preferencesManager.getInt(PREF_FEATURE_USAGE_COUNT, 0))
            })
        }
    }

    /**
     * 记录功能使用（用于评价条件判断）
     */
    fun recordFeatureUsage(featureName: String) {
        val count = preferencesManager.getInt(PREF_FEATURE_USAGE_COUNT, 0)
        preferencesManager.putInt(PREF_FEATURE_USAGE_COUNT, count + 1)

        analyticsManager.trackEvent("feature_usage_for_review", android.os.Bundle().apply {
            putString("feature_name", featureName)
            putInt("total_usage_count", count + 1)
        })
    }

    /**
     * 记录优化操作（用于评价条件判断）
     */
    fun recordOptimization() {
        val count = preferencesManager.getInt(PREF_OPTIMIZATION_COUNT, 0)
        preferencesManager.putInt(PREF_OPTIMIZATION_COUNT, count + 1)

        analyticsManager.trackEvent("optimization_for_review", android.os.Bundle().apply {
            putInt("total_optimization_count", count + 1)
        })
    }

    /**
     * 检查是否在合适的时机显示评价
     */
    fun checkOptimalTiming(): Boolean {
        try {
            val lastOptimizationTime = preferencesManager.getLong("last_optimization_time", 0)
            val timeSinceOptimization = System.currentTimeMillis() - lastOptimizationTime

            // 在成功优化后的5分钟内是最佳时机
            val optimalWindow = 5 * 60 * 1000L // 5分钟
            return timeSinceOptimization <= optimalWindow

        } catch (e: Exception) {
            Log.e(TAG, "Error checking optimal timing", e)
            return false
        }
    }

    /**
     * 获取评价统计信息
     */
    fun getReviewStats(): Map<String, Any> {
        return mapOf(
            "review_requested" to preferencesManager.getBoolean(PREF_REVIEW_REQUESTED, false),
            "review_completed" to preferencesManager.getBoolean(PREF_REVIEW_COMPLETED, false),
            "review_declined" to preferencesManager.getBoolean(PREF_REVIEW_DECLINED, false),
            "launch_count" to preferencesManager.getInt(PREF_APP_LAUNCH_COUNT, 0),
            "feature_usage_count" to preferencesManager.getInt(PREF_FEATURE_USAGE_COUNT, 0),
            "optimization_count" to preferencesManager.getInt(PREF_OPTIMIZATION_COUNT, 0),
            "days_since_install" to getDaysSinceInstall(),
            "review_attempts" to preferencesManager.getInt("review_attempts", 0),
            "last_review_request" to preferencesManager.getLong(PREF_LAST_REVIEW_REQUEST, 0)
        )
    }

    /**
     * 获取安装天数
     */
    private fun getDaysSinceInstall(): Long {
        val firstLaunchTime = preferencesManager.getLong(PREF_FIRST_LAUNCH_TIME, System.currentTimeMillis())
        return (System.currentTimeMillis() - firstLaunchTime) / (24 * 60 * 60 * 1000)
    }

    /**
     * 重置评价数据（用于测试）
     */
    fun resetReviewData() {
        preferencesManager.apply {
            remove(PREF_REVIEW_REQUESTED)
            remove(PREF_REVIEW_COMPLETED)
            remove(PREF_REVIEW_DECLINED)
            remove(PREF_REVIEW_POSTPONED)
            remove(PREF_LAST_REVIEW_REQUEST)
            remove("review_attempts")
        }

        analyticsManager.trackFeatureUsed("review_data_reset")
    }

    /**
     * 标记用户拒绝评价
     */
    fun markReviewDeclined() {
        preferencesManager.putBoolean(PREF_REVIEW_DECLINED, true)
        preferencesManager.putLong("review_declined_time", System.currentTimeMillis())

        analyticsManager.trackEvent("review_declined")
    }

    /**
     * 标记评价已完成
     */
    fun markReviewCompleted() {
        preferencesManager.putBoolean(PREF_REVIEW_COMPLETED, true)
        preferencesManager.putLong("review_completed_time", System.currentTimeMillis())

        analyticsManager.trackEvent("review_completed")
    }

    /**
     * 推迟评价
     */
    fun postponeReview(postponeDays: Int = 7) {
        val postponeTime = System.currentTimeMillis() + (postponeDays * 24 * 60 * 60 * 1000L)
        preferencesManager.putLong(PREF_LAST_REVIEW_REQUEST, postponeTime)

        analyticsManager.trackEvent("review_postponed", android.os.Bundle().apply {
            putInt("postpone_days", postponeDays)
        })
    }

    /**
     * 智能评价提示
     * 根据用户行为和使用模式智能决定评价时机
     */
    suspend fun smartReviewPrompt(activity: Activity): ReviewResult {
        return withContext(Dispatchers.Main) {
            try {
                // 检查是否是最佳时机
                val isOptimalTiming = checkOptimalTiming()
                val shouldShow = shouldShowReview()

                if (shouldShow && isOptimalTiming) {
                    requestReview(activity, ReviewConfig(trackingEnabled = true))
                } else {
                    ReviewResult.NOT_ELIGIBLE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Smart review prompt failed", e)
                ReviewResult.FAILED
            }
        }
    }
}

/**
 * 扩展函数
 */
fun Context.reviewManager(): InAppReviewManager = InAppReviewManager.getInstance(this)

/**
 * 简化的评价请求函数
 */
suspend fun Activity.requestInAppReview(): InAppReviewManager.ReviewResult {
    val reviewManager = InAppReviewManager.getInstance(this)
    return reviewManager.requestReview(this)
}