package com.lanhe.gongjuxiang.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.MainActivity

/**
 * 通知助手类
 * 处理所有通知相关的操作
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "charging_reminder_channel"
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "充电提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "充电状态监控和提醒通知"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun createServiceNotification(): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("蓝河工具箱服务")
            .setContentText("正在监控系统状态")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showNotification(title: String, message: String, type: NotificationType) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type.name)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            type.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(getNotificationIcon(type))
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(getNotificationPriority(type))
            .setAutoCancel(true)
            .setCategory(getNotificationCategory(type))
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(type.id, notification)
    }

    private fun getNotificationIcon(type: NotificationType): Int {
        return when (type) {
            // 充电相关
            NotificationType.CHARGING_CONNECTED -> android.R.drawable.ic_lock_idle_lock
            NotificationType.CHARGING_DISCONNECTED -> android.R.drawable.ic_lock_idle_lock
            NotificationType.TEMPERATURE_WARNING -> android.R.drawable.ic_menu_manage
            NotificationType.TEMPERATURE_DANGER -> android.R.drawable.ic_delete
            NotificationType.TEMPERATURE_LOW -> android.R.drawable.ic_menu_manage
            NotificationType.VOLTAGE_WARNING -> android.R.drawable.ic_popup_sync
            NotificationType.VOLTAGE_LOW -> android.R.drawable.ic_delete
            NotificationType.AC_CHARGING -> android.R.drawable.ic_lock_idle_lock
            NotificationType.USB_CHARGING -> android.R.drawable.ic_menu_manage
            NotificationType.WIRELESS_CHARGING -> android.R.drawable.ic_menu_manage
            NotificationType.BATTERY_FULL -> android.R.drawable.checkbox_on_background
            NotificationType.BATTERY_HIGH -> android.R.drawable.checkbox_on_background
            NotificationType.BATTERY_LOW -> android.R.drawable.ic_delete
            NotificationType.BATTERY_CRITICAL -> android.R.drawable.ic_delete
            NotificationType.BATTERY_HEALTH -> android.R.drawable.checkbox_on_background
            NotificationType.SAFETY_WARNING -> android.R.drawable.ic_delete
            NotificationType.LONG_CHARGING -> android.R.drawable.ic_popup_sync
            NotificationType.FAST_CHARGING -> android.R.drawable.ic_popup_sync
            NotificationType.COMPUTER_CHARGING -> android.R.drawable.ic_menu_manage

            // WiFi相关
            NotificationType.WIFI_CONNECTED -> android.R.drawable.ic_menu_manage
            NotificationType.WIFI_DISCONNECTED -> android.R.drawable.ic_delete
            NotificationType.WIFI_SIGNAL_WEAK -> android.R.drawable.ic_delete
            NotificationType.WIFI_SIGNAL_STRONG -> android.R.drawable.checkbox_on_background

            // 短信相关
            NotificationType.SMS_RECEIVED -> android.R.drawable.ic_menu_manage
            NotificationType.SMS_SPAM_DETECTED -> android.R.drawable.ic_delete

            // 文件相关
            NotificationType.FILE_BACKUP_COMPLETED -> android.R.drawable.checkbox_on_background
            NotificationType.FILE_CLEANUP_COMPLETED -> android.R.drawable.ic_menu_manage

            // 号码卫士
            NotificationType.CALL_BLOCKED -> android.R.drawable.ic_delete
            NotificationType.CALL_FROM_BLACKLIST -> android.R.drawable.ic_lock_idle_lock

            // AI助手
            NotificationType.AI_RESPONSE_RECEIVED -> android.R.drawable.ic_menu_manage
            NotificationType.AI_ANALYSIS_COMPLETED -> android.R.drawable.checkbox_on_background
            NotificationType.AI_ERROR_OCCURRED -> android.R.drawable.ic_delete
        }
    }

    private fun getNotificationPriority(type: NotificationType): Int {
        return when (type) {
            // 高优先级 - 紧急情况
            NotificationType.TEMPERATURE_DANGER,
            NotificationType.SAFETY_WARNING,
            NotificationType.BATTERY_CRITICAL,
            NotificationType.CALL_FROM_BLACKLIST -> NotificationCompat.PRIORITY_HIGH

            // 默认优先级 - 重要提醒
            NotificationType.TEMPERATURE_WARNING,
            NotificationType.VOLTAGE_WARNING,
            NotificationType.BATTERY_FULL,
            NotificationType.BATTERY_LOW,
            NotificationType.WIFI_DISCONNECTED,
            NotificationType.SMS_SPAM_DETECTED,
            NotificationType.CALL_BLOCKED -> NotificationCompat.PRIORITY_DEFAULT

            // 低优先级 - 一般信息
            else -> NotificationCompat.PRIORITY_LOW
        }
    }

    private fun getNotificationCategory(type: NotificationType): String {
        return when (type) {
            // 报警类
            NotificationType.TEMPERATURE_DANGER,
            NotificationType.SAFETY_WARNING,
            NotificationType.BATTERY_CRITICAL -> NotificationCompat.CATEGORY_ALARM

            // 状态类
            NotificationType.CHARGING_CONNECTED,
            NotificationType.CHARGING_DISCONNECTED,
            NotificationType.WIFI_CONNECTED,
            NotificationType.WIFI_DISCONNECTED -> NotificationCompat.CATEGORY_STATUS

            // 消息类
            NotificationType.SMS_RECEIVED,
            NotificationType.SMS_SPAM_DETECTED -> NotificationCompat.CATEGORY_MESSAGE

            // 来电类
            NotificationType.CALL_BLOCKED,
            NotificationType.CALL_FROM_BLACKLIST -> NotificationCompat.CATEGORY_CALL

            // 其他
            else -> NotificationCompat.CATEGORY_SERVICE
        }
    }
}
