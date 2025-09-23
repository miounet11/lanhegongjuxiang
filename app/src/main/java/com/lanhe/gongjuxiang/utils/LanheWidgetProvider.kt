package com.lanhe.gongjuxiang.utils

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.MainActivity
import kotlinx.coroutines.*

/**
 * 蓝河助手 - 桌面小部件提供器
 *
 * 功能特性：
 * - 系统状态显示
 * - 快速优化按钮
 * - 实时性能监控
 * - 电池状态显示
 * - 内存使用情况
 * - 一键清理功能
 * - 智能更新机制
 */
class LanheWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "LanheWidgetProvider"

        // 小部件动作
        const val ACTION_QUICK_OPTIMIZE = "com.lanhe.gongjuxiang.ACTION_QUICK_OPTIMIZE"
        const val ACTION_MEMORY_CLEAN = "com.lanhe.gongjuxiang.ACTION_MEMORY_CLEAN"
        const val ACTION_REFRESH = "com.lanhe.gongjuxiang.ACTION_REFRESH"
        const val ACTION_OPEN_APP = "com.lanhe.gongjuxiang.ACTION_OPEN_APP"

        // 更新间隔
        private const val UPDATE_INTERVAL = 30000L // 30秒
    }

    private val widgetScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        Log.d(TAG, "Widget update requested for ${appWidgetIds.size} widgets")

        // 更新所有小部件
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }

        // 记录分析事件
        AnalyticsManager.getInstance(context).trackEvent("widget_updated", Bundle().apply {
            putInt("widget_count", appWidgetIds.size)
        })
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val analyticsManager = AnalyticsManager.getInstance(context)

        when (intent.action) {
            ACTION_QUICK_OPTIMIZE -> {
                Log.d(TAG, "Quick optimize action received")
                performQuickOptimize(context)
                analyticsManager.trackFeatureUsed("widget_quick_optimize")

                // 刷新小部件显示
                updateAllWidgets(context, appWidgetManager)
            }

            ACTION_MEMORY_CLEAN -> {
                Log.d(TAG, "Memory clean action received")
                performMemoryClean(context)
                analyticsManager.trackFeatureUsed("widget_memory_clean")

                // 刷新小部件显示
                updateAllWidgets(context, appWidgetManager)
            }

            ACTION_REFRESH -> {
                Log.d(TAG, "Refresh action received")
                updateAllWidgets(context, appWidgetManager)
                analyticsManager.trackFeatureUsed("widget_refresh")
            }

            ACTION_OPEN_APP -> {
                Log.d(TAG, "Open app action received")
                openMainApp(context)
                analyticsManager.trackFeatureUsed("widget_open_app")
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d(TAG, "Widget enabled")

        AnalyticsManager.getInstance(context).trackFeatureUsed("widget_enabled")
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d(TAG, "Widget disabled")

        AnalyticsManager.getInstance(context).trackFeatureUsed("widget_disabled")
        widgetScope.cancel()
    }

    /**
     * 更新单个小部件
     */
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        widgetScope.launch {
            try {
                val views = createWidgetViews(context)
                appWidgetManager.updateAppWidget(widgetId, views)

                Log.d(TAG, "Widget $widgetId updated successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widget $widgetId", e)
                AnalyticsManager.getInstance(context).trackError(
                    "widget_update_failed",
                    e.message ?: "Unknown error",
                    e
                )
            }
        }
    }

    /**
     * 更新所有小部件
     */
    private fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager
    ) {
        val thisWidget = ComponentName(context, LanheWidgetProvider::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        allWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    /**
     * 创建小部件视图
     */
    private suspend fun createWidgetViews(context: Context): RemoteViews = withContext(Dispatchers.IO) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        try {
            // 获取系统状态
            val systemStatus = getSystemStatus(context)

            // 更新系统信息显示
            updateSystemInfo(context, views, systemStatus)

            // 设置按钮点击事件
            setupButtonClickEvents(context, views)

            // 设置应用标题点击事件
            setupAppClickEvent(context, views)

            views

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create widget views", e)
            createErrorView(context)
        }
    }

    /**
     * 获取系统状态
     */
    private suspend fun getSystemStatus(context: Context): SystemStatus = withContext(Dispatchers.IO) {
        try {
            val performanceMonitor = PerformanceMonitorManager.getInstance(context)
            val batteryMonitor = BatteryMonitor.getInstance(context)

            val memoryInfo = performanceMonitor.getMemoryUsage()
            val batteryInfo = batteryMonitor.getBatteryInfo()
            val cpuUsage = performanceMonitor.getCpuUsage()

            SystemStatus(
                memoryUsage = memoryInfo.usedMemoryPercent,
                batteryLevel = batteryInfo.level,
                batteryTemperature = batteryInfo.temperature,
                cpuUsage = cpuUsage,
                isCharging = batteryInfo.isCharging,
                networkType = getNetworkType(context)
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get system status", e)
            SystemStatus() // 返回默认状态
        }
    }

    /**
     * 系统状态数据类
     */
    data class SystemStatus(
        val memoryUsage: Int = 0,
        val batteryLevel: Int = 0,
        val batteryTemperature: Float = 0f,
        val cpuUsage: Float = 0f,
        val isCharging: Boolean = false,
        val networkType: String = "未知"
    )

    /**
     * 更新系统信息显示
     */
    private fun updateSystemInfo(
        context: Context,
        views: RemoteViews,
        status: SystemStatus
    ) {
        // 更新内存使用率
        views.setTextViewText(R.id.tv_memory_usage, "${status.memoryUsage}%")
        views.setProgressBar(R.id.progress_memory, 100, status.memoryUsage, false)

        // 更新电池信息
        views.setTextViewText(R.id.tv_battery_level, "${status.batteryLevel}%")
        views.setProgressBar(R.id.progress_battery, 100, status.batteryLevel, false)

        // 设置电池图标
        val batteryIcon = when {
            status.isCharging -> R.drawable.ic_battery_charging
            status.batteryLevel >= 80 -> R.drawable.ic_battery_full
            status.batteryLevel >= 60 -> R.drawable.ic_battery_high
            status.batteryLevel >= 40 -> R.drawable.ic_battery_medium
            status.batteryLevel >= 20 -> R.drawable.ic_battery_low
            else -> R.drawable.ic_battery_critical
        }
        views.setImageViewResource(R.id.iv_battery_icon, batteryIcon)

        // 更新CPU使用率
        views.setTextViewText(R.id.tv_cpu_usage, "${status.cpuUsage.toInt()}%")

        // 更新网络类型
        views.setTextViewText(R.id.tv_network_type, status.networkType)

        // 更新最后更新时间
        val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        views.setTextViewText(R.id.tv_last_update, "更新: $currentTime")
    }

    /**
     * 设置按钮点击事件
     */
    private fun setupButtonClickEvents(context: Context, views: RemoteViews) {
        // 快速优化按钮
        val quickOptimizeIntent = Intent(context, LanheWidgetProvider::class.java).apply {
            action = ACTION_QUICK_OPTIMIZE
        }
        val quickOptimizePendingIntent = PendingIntent.getBroadcast(
            context, 0, quickOptimizeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_quick_optimize, quickOptimizePendingIntent)

        // 内存清理按钮
        val memoryCleanIntent = Intent(context, LanheWidgetProvider::class.java).apply {
            action = ACTION_MEMORY_CLEAN
        }
        val memoryCleanPendingIntent = PendingIntent.getBroadcast(
            context, 1, memoryCleanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_memory_clean, memoryCleanPendingIntent)

        // 刷新按钮
        val refreshIntent = Intent(context, LanheWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 2, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)
    }

    /**
     * 设置应用点击事件
     */
    private fun setupAppClickEvent(context: Context, views: RemoteViews) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 3, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.tv_app_title, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.iv_app_icon, openAppPendingIntent)
    }

    /**
     * 执行快速优化
     */
    private fun performQuickOptimize(context: Context) {
        widgetScope.launch {
            try {
                val systemOptimizer = SystemOptimizer(context)
                val result = systemOptimizer.performQuickOptimization()

                Log.d(TAG, "Quick optimization result: ${result.success}")

                // 显示优化结果通知
                showOptimizationNotification(context, "快速优化", result.success)

            } catch (e: Exception) {
                Log.e(TAG, "Quick optimization failed", e)
                showOptimizationNotification(context, "快速优化", false)
            }
        }
    }

    /**
     * 执行内存清理
     */
    private fun performMemoryClean(context: Context) {
        widgetScope.launch {
            try {
                val smartCleaner = SmartCleaner(context)
                val result = smartCleaner.performMemoryCleanup()

                Log.d(TAG, "Memory cleanup result: ${result.success}")

                // 显示清理结果通知
                showOptimizationNotification(
                    context,
                    "内存清理",
                    result.success,
                    "清理了 ${result.cleanedMemory}MB 内存"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Memory cleanup failed", e)
                showOptimizationNotification(context, "内存清理", false)
            }
        }
    }

    /**
     * 打开主应用
     */
    private fun openMainApp(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    /**
     * 获取网络类型
     */
    private fun getNetworkType(context: Context): String {
        return try {
            val networkHelper = NetworkInfoHelper.getInstance(context)
            networkHelper.getNetworkType()
        } catch (e: Exception) {
            "未知"
        }
    }

    /**
     * 显示优化结果通知
     */
    private fun showOptimizationNotification(
        context: Context,
        operationType: String,
        success: Boolean,
        details: String? = null
    ) {
        val notificationHelper = NotificationHelper.getInstance(context)
        val message = if (success) {
            "$operationType 完成${details?.let { " - $it" } ?: ""}"
        } else {
            "$operationType 失败"
        }

        notificationHelper.showOptimizationResult(operationType, message, success)
    }

    /**
     * 创建错误视图
     */
    private fun createErrorView(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_error_layout)
        views.setTextViewText(R.id.tv_error_message, "小部件加载失败")

        // 设置重试按钮
        val refreshIntent = Intent(context, LanheWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 99, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_retry, refreshPendingIntent)

        return views
    }
}

/**
 * 小部件管理器
 */
class WidgetManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: WidgetManager? = null

        fun getInstance(context: Context): WidgetManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val appWidgetManager = AppWidgetManager.getInstance(context)

    /**
     * 强制更新所有小部件
     */
    fun forceUpdateAllWidgets() {
        val thisWidget = ComponentName(context, LanheWidgetProvider::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

        val updateIntent = Intent(context, LanheWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
        }

        context.sendBroadcast(updateIntent)
    }

    /**
     * 检查是否有小部件被添加
     */
    fun hasWidgetsAdded(): Boolean {
        val thisWidget = ComponentName(context, LanheWidgetProvider::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        return allWidgetIds.isNotEmpty()
    }

    /**
     * 获取小部件数量
     */
    fun getWidgetCount(): Int {
        val thisWidget = ComponentName(context, LanheWidgetProvider::class.java)
        return appWidgetManager.getAppWidgetIds(thisWidget).size
    }
}

/**
 * 扩展函数
 */
fun Context.widgetManager(): WidgetManager = WidgetManager.getInstance(this)