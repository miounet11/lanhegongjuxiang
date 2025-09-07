package com.lanhe.gongjuxiang.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 智能通知管理器
 * 负责通知的创建、管理和优化
 */
class SmartNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val packageManager = context.packageManager

    companion object {
        const val CHANNEL_ID_SYSTEM = "system_notifications"
        const val CHANNEL_ID_OPTIMIZATION = "optimization_notifications"
        const val CHANNEL_ID_PERFORMANCE = "performance_notifications"
        const val CHANNEL_ID_SECURITY = "security_notifications"
        
        const val NOTIFICATION_ID_SYSTEM = 1001
        const val NOTIFICATION_ID_OPTIMIZATION = 1002
        const val NOTIFICATION_ID_PERFORMANCE = 1003
        const val NOTIFICATION_ID_SECURITY = 1004
    }

    init {
        createNotificationChannels()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_SYSTEM,
                    "系统通知",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "系统状态和重要信息通知"
                    enableLights(true)
                    lightColor = android.graphics.Color.BLUE
                },
                NotificationChannel(
                    CHANNEL_ID_OPTIMIZATION,
                    "优化通知",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "系统优化和清理完成通知"
                    enableVibration(false)
                },
                NotificationChannel(
                    CHANNEL_ID_PERFORMANCE,
                    "性能监控",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "性能监控和异常检测通知"
                    enableVibration(false)
                },
                NotificationChannel(
                    CHANNEL_ID_SECURITY,
                    "安全通知",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "安全威胁和隐私保护通知"
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * 显示系统状态通知
     */
    suspend fun showSystemStatusNotification(
        title: String,
        message: String,
        isPersistent: Boolean = false
    ) {
        withContext(Dispatchers.Main) {
            try {
                val builder = NotificationCompat.Builder(context, CHANNEL_ID_SYSTEM)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(!isPersistent)
                    .setOngoing(isPersistent)

                if (isPersistent) {
                    builder.addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "关闭",
                        createPendingIntent("close_system_notification")
                    )
                }

                NotificationManagerCompat.from(context).notify(
                    NOTIFICATION_ID_SYSTEM,
                    builder.build()
                )
            } catch (e: Exception) {
                Log.e("SmartNotificationManager", "显示系统状态通知失败", e)
            }
        }
    }

    /**
     * 显示优化完成通知
     */
    suspend fun showOptimizationCompleteNotification(
        optimizationType: String,
        improvements: List<String>,
        duration: Long
    ) {
        withContext(Dispatchers.Main) {
            try {
                val message = buildString {
                    append("$optimizationType 优化完成！\n")
                    append("耗时: ${duration}ms\n")
                    append("改进项目:\n")
                    improvements.take(3).forEach { improvement ->
                        append("• $improvement\n")
                    }
                    if (improvements.size > 3) {
                        append("• 还有${improvements.size - 3}项改进...")
                    }
                }

                val builder = NotificationCompat.Builder(context, CHANNEL_ID_OPTIMIZATION)
                    .setSmallIcon(android.R.drawable.ic_menu_send)
                    .setContentTitle("🚀 优化完成")
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .addAction(
                        android.R.drawable.ic_menu_view,
                        "查看详情",
                        createPendingIntent("view_optimization_details")
                    )

                NotificationManagerCompat.from(context).notify(
                    NOTIFICATION_ID_OPTIMIZATION,
                    builder.build()
                )
            } catch (e: Exception) {
                Log.e("SmartNotificationManager", "显示优化完成通知失败", e)
            }
        }
    }

    /**
     * 显示性能监控通知
     */
    suspend fun showPerformanceNotification(
        cpuUsage: Float,
        memoryUsage: Int,
        batteryLevel: Int,
        temperature: Float
    ) {
        withContext(Dispatchers.Main) {
            try {
                val status = when {
                    cpuUsage > 80f || memoryUsage > 85 || temperature > 60f -> "⚠️ 性能警告"
                    cpuUsage > 60f || memoryUsage > 70 || temperature > 50f -> "📊 性能监控"
                    else -> "✅ 性能良好"
                }

                val message = buildString {
                    append("CPU: ${String.format("%.1f", cpuUsage)}%\n")
                    append("内存: $memoryUsage%\n")
                    append("电池: $batteryLevel%\n")
                    append("温度: ${String.format("%.1f", temperature)}°C")
                }

                val builder = NotificationCompat.Builder(context, CHANNEL_ID_PERFORMANCE)
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setContentTitle(status)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .addAction(
                        android.R.drawable.ic_menu_manage,
                        "优化",
                        createPendingIntent("start_optimization")
                    )

                NotificationManagerCompat.from(context).notify(
                    NOTIFICATION_ID_PERFORMANCE,
                    builder.build()
                )
            } catch (e: Exception) {
                Log.e("SmartNotificationManager", "显示性能监控通知失败", e)
            }
        }
    }

    /**
     * 显示安全警告通知
     */
    suspend fun showSecurityWarningNotification(
        threatType: String,
        description: String,
        severity: String
    ) {
        withContext(Dispatchers.Main) {
            try {
                val icon = when (severity) {
                    "HIGH" -> android.R.drawable.ic_dialog_alert
                    "CRITICAL" -> android.R.drawable.ic_dialog_alert
                    else -> android.R.drawable.ic_dialog_info
                }

                val builder = NotificationCompat.Builder(context, CHANNEL_ID_SECURITY)
                    .setSmallIcon(icon)
                    .setContentTitle("🔒 安全警告: $threatType")
                    .setContentText(description)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(description))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .addAction(
                        android.R.drawable.ic_menu_edit,
                        "立即处理",
                        createPendingIntent("handle_security_threat")
                    )
                    .addAction(
                        android.R.drawable.ic_menu_info_details,
                        "查看详情",
                        createPendingIntent("view_security_details")
                    )

                NotificationManagerCompat.from(context).notify(
                    NOTIFICATION_ID_SECURITY,
                    builder.build()
                )
            } catch (e: Exception) {
                Log.e("SmartNotificationManager", "显示安全警告通知失败", e)
            }
        }
    }

    /**
     * 显示智能建议通知
     */
    suspend fun showSmartSuggestionNotification(
        suggestion: String,
        action: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        withContext(Dispatchers.Main) {
            try {
                val builder = NotificationCompat.Builder(context, CHANNEL_ID_SYSTEM)
                    .setSmallIcon(android.R.drawable.ic_menu_lightbulb)
                    .setContentTitle("💡 智能建议")
                    .setContentText(suggestion)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(suggestion))
                    .setPriority(priority)
                    .setAutoCancel(true)
                    .addAction(
                        android.R.drawable.ic_menu_send,
                        action,
                        createPendingIntent("apply_smart_suggestion")
                    )

                NotificationManagerCompat.from(context).notify(
                    System.currentTimeMillis().toInt(),
                    builder.build()
                )
            } catch (e: Exception) {
                Log.e("SmartNotificationManager", "显示智能建议通知失败", e)
            }
        }
    }

    /**
     * 显示进度通知
     */
    suspend fun showProgressNotification(
        title: String,
        message: String,
        progress: Int,
        maxProgress: Int = 100
    ) {
        withContext(Dispatchers.Main) {
            try {
                val builder = NotificationCompat.Builder(context, CHANNEL_ID_OPTIMIZATION)
                    .setSmallIcon(android.R.drawable.ic_menu_rotate)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setProgress(maxProgress, progress, false)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(false)
                    .setOngoing(true)

                NotificationManagerCompat.from(context).notify(
                    NOTIFICATION_ID_OPTIMIZATION,
                    builder.build()
                )
            } catch (e: Exception) {
                Log.e("SmartNotificationManager", "显示进度通知失败", e)
            }
        }
    }

    /**
     * 取消通知
     */
    fun cancelNotification(notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (e: Exception) {
            Log.e("SmartNotificationManager", "取消通知失败", e)
        }
    }

    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (e: Exception) {
            Log.e("SmartNotificationManager", "取消所有通知失败", e)
        }
    }

    /**
     * 检查通知权限
     */
    fun hasNotificationPermission(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * 请求通知权限
     */
    fun requestNotificationPermission() {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("SmartNotificationManager", "请求通知权限失败", e)
        }
    }

    /**
     * 获取应用通知设置
     */
    fun getAppNotificationSettings(): List<AppNotificationInfo> {
        return try {
            val packages = packageManager.getInstalledPackages(0)
            val notificationInfos = mutableListOf<AppNotificationInfo>()

            packages.forEach { packageInfo ->
                val appInfo = packageInfo.applicationInfo
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                
                // 这里需要检查每个应用的通知权限
                // 由于权限检查需要特殊权限，这里返回模拟数据
                notificationInfos.add(
                    AppNotificationInfo(
                        packageName = appInfo.packageName,
                        appName = appName,
                        isEnabled = true, // 模拟数据
                        canBypassDnd = false, // 模拟数据
                        importance = "DEFAULT" // 模拟数据
                    )
                )
            }

            notificationInfos.sortedBy { it.appName }
        } catch (e: Exception) {
            Log.e("SmartNotificationManager", "获取应用通知设置失败", e)
            emptyList()
        }
    }

    /**
     * 创建PendingIntent
     */
    private fun createPendingIntent(action: String): android.app.PendingIntent {
        val intent = Intent(context, com.lanhe.gongjuxiang.MainActivity::class.java).apply {
            putExtra("notification_action", action)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return android.app.PendingIntent.getActivity(
            context,
            action.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 显示通知统计
     */
    suspend fun showNotificationStats() {
        withContext(Dispatchers.Main) {
            try {
                val stats = getNotificationStats()
                val message = buildString {
                    append("今日通知统计:\n")
                    append("系统通知: ${stats.systemNotifications}\n")
                    append("优化通知: ${stats.optimizationNotifications}\n")
                    append("性能通知: ${stats.performanceNotifications}\n")
                    append("安全通知: ${stats.securityNotifications}")
                }

                val builder = NotificationCompat.Builder(context, CHANNEL_ID_SYSTEM)
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle("📊 通知统计")
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)

                NotificationManagerCompat.from(context).notify(
                    System.currentTimeMillis().toInt(),
                    builder.build()
                )
            } catch (e: Exception) {
                Log.e("SmartNotificationManager", "显示通知统计失败", e)
            }
        }
    }

    /**
     * 获取通知统计
     */
    private fun getNotificationStats(): NotificationStats {
        // 这里应该从数据库或SharedPreferences中获取统计数据
        // 暂时返回模拟数据
        return NotificationStats(
            systemNotifications = 5,
            optimizationNotifications = 12,
            performanceNotifications = 8,
            securityNotifications = 2
        )
    }
}

/**
 * 应用通知信息类
 */
data class AppNotificationInfo(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean,
    val canBypassDnd: Boolean,
    val importance: String
)

/**
 * 通知统计类
 */
data class NotificationStats(
    val systemNotifications: Int,
    val optimizationNotifications: Int,
    val performanceNotifications: Int,
    val securityNotifications: Int
)