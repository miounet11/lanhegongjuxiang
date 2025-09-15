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
            .setSmallIcon(R.drawable.ic_fluent_battery_0_24_regular)
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
            NotificationType.CHARGING_CONNECTED -> R.drawable.ic_fluent_battery_charge_24_regular
            NotificationType.CHARGING_DISCONNECTED -> R.drawable.ic_fluent_battery_0_24_regular
            NotificationType.TEMPERATURE_WARNING -> R.drawable.ic_fluent_temperature_24_regular
            NotificationType.TEMPERATURE_DANGER -> R.drawable.ic_fluent_warning_24_regular
            NotificationType.TEMPERATURE_LOW -> R.drawable.ic_fluent_weather_snowflake_24_regular
            NotificationType.VOLTAGE_WARNING -> R.drawable.ic_fluent_flash_24_regular
            NotificationType.VOLTAGE_LOW -> R.drawable.ic_fluent_battery_warning_24_regular
            NotificationType.AC_CHARGING -> R.drawable.ic_fluent_plug_connected_24_regular
            NotificationType.USB_CHARGING -> R.drawable.ic_fluent_usb_stick_24_regular
            NotificationType.WIRELESS_CHARGING -> R.drawable.ic_fluent_phone_24_regular
            NotificationType.BATTERY_FULL -> R.drawable.ic_fluent_battery_10_24_regular
            NotificationType.BATTERY_HIGH -> R.drawable.ic_fluent_battery_7_24_regular
            NotificationType.BATTERY_LOW -> R.drawable.ic_fluent_battery_3_24_regular
            NotificationType.BATTERY_CRITICAL -> R.drawable.ic_fluent_battery_1_24_regular
            NotificationType.BATTERY_HEALTH -> R.drawable.ic_fluent_heart_24_regular
            NotificationType.SAFETY_WARNING -> R.drawable.ic_fluent_shield_error_24_regular
            NotificationType.LONG_CHARGING -> R.drawable.ic_fluent_clock_24_regular
            NotificationType.FAST_CHARGING -> R.drawable.ic_fluent_lightning_24_regular
            NotificationType.COMPUTER_CHARGING -> R.drawable.ic_fluent_desktop_24_regular

            // WiFi相关
            NotificationType.WIFI_CONNECTED -> R.drawable.ic_fluent_wifi_1_24_regular
            NotificationType.WIFI_DISCONNECTED -> R.drawable.ic_fluent_wifi_off_24_regular
            NotificationType.WIFI_SIGNAL_WEAK -> R.drawable.ic_fluent_wifi_warning_24_regular
            NotificationType.WIFI_SIGNAL_STRONG -> R.drawable.ic_fluent_wifi_4_24_regular

            // 短信相关
            NotificationType.SMS_RECEIVED -> R.drawable.ic_fluent_comment_24_regular
            NotificationType.SMS_SPAM_DETECTED -> R.drawable.ic_fluent_mail_inbox_dismiss_24_regular

            // 文件相关
            NotificationType.FILE_BACKUP_COMPLETED -> R.drawable.ic_fluent_cloud_backup_24_regular
            NotificationType.FILE_CLEANUP_COMPLETED -> R.drawable.ic_fluent_broom_24_regular

            // 号码卫士
            NotificationType.CALL_BLOCKED -> R.drawable.ic_fluent_call_blocked_24_regular
            NotificationType.CALL_FROM_BLACKLIST -> R.drawable.ic_fluent_shield_person_24_regular

            // AI助手
            NotificationType.AI_RESPONSE_RECEIVED -> R.drawable.ic_fluent_bot_24_regular
            NotificationType.AI_ANALYSIS_COMPLETED -> R.drawable.ic_fluent_data_trending_24_regular
            NotificationType.AI_ERROR_OCCURRED -> R.drawable.ic_fluent_error_circle_24_regular
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
