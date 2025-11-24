package com.lanhe.mokuai.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import android.Manifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * 通知助手 - 通知管理和自定义通知工具
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val DEFAULT_CHANNEL_ID = "default_channel"
        private const val DEFAULT_CHANNEL_NAME = "默认通知"
        private const val HIGH_PRIORITY_CHANNEL_ID = "high_priority_channel"
        private const val HIGH_PRIORITY_CHANNEL_NAME = "重要通知"
        private const val SILENT_CHANNEL_ID = "silent_channel"
        private const val SILENT_CHANNEL_NAME = "静音通知"

        private const val KEY_TEXT_REPLY = "key_text_reply"
        private const val NOTIFICATION_HISTORY_KEY = "notification_history"
        private const val MAX_HISTORY_ITEMS = 100
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val notificationManagerCompat: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    private val sharedPrefs by lazy {
        context.getSharedPreferences("notification_helper", Context.MODE_PRIVATE)
    }

    init {
        createNotificationChannels()
    }

    data class NotificationConfig(
        val id: Int = System.currentTimeMillis().toInt(),
        val title: String,
        val content: String,
        val channelId: String = DEFAULT_CHANNEL_ID,
        val smallIcon: Int,
        val largeIcon: Bitmap? = null,
        val priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        val category: String? = null,
        val autoCancel: Boolean = true,
        val ongoing: Boolean = false,
        val showWhen: Boolean = true,
        val`when`: Long = System.currentTimeMillis(),
        val vibrate: LongArray? = null,
        val sound: Uri? = null,
        val lights: Int? = null,
        val actions: List<NotificationAction> = emptyList(),
        val style: NotificationStyle? = null,
        val progress: ProgressConfig? = null,
        val groupKey: String? = null,
        val sortKey: String? = null,
        val extras: Bundle? = null
    )

    data class NotificationAction(
        val icon: Int,
        val title: String,
        val pendingIntent: PendingIntent,
        val isReply: Boolean = false,
        val replyLabel: String? = null
    )

    sealed class NotificationStyle {
        data class BigText(val bigText: String, val summaryText: String? = null) : NotificationStyle()
        data class BigPicture(val picture: Bitmap, val summaryText: String? = null) : NotificationStyle()
        data class Inbox(val lines: List<String>, val summaryText: String? = null) : NotificationStyle()
        data class Messaging(
            val messages: List<Message>,
            val conversationTitle: String? = null,
            val isGroupConversation: Boolean = false
        ) : NotificationStyle()
    }

    data class Message(
        val text: String,
        val timestamp: Long,
        val sender: String? = null
    )

    data class ProgressConfig(
        val max: Int,
        val progress: Int,
        val indeterminate: Boolean = false
    )

    data class NotificationHistory(
        val id: Int,
        val title: String,
        val content: String,
        val timestamp: Long,
        val packageName: String,
        val isRead: Boolean = false,
        val isCancelled: Boolean = false
    )

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 默认渠道
            val defaultChannel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                DEFAULT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "应用的默认通知"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }

            // 高优先级渠道
            val highPriorityChannel = NotificationChannel(
                HIGH_PRIORITY_CHANNEL_ID,
                HIGH_PRIORITY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "重要的通知，会发出提示音"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            // 静音渠道
            val silentChannel = NotificationChannel(
                SILENT_CHANNEL_ID,
                SILENT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "不会发出声音的通知"
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannels(
                listOf(defaultChannel, highPriorityChannel, silentChannel)
            )
        }
    }

    /**
     * 创建自定义通知渠道
     */
    fun createCustomChannel(
        id: String,
        name: String,
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        description: String? = null,
        soundUri: Uri? = null,
        vibrationPattern: LongArray? = null,
        lightColor: Int? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, importance).apply {
                description?.let { this.description = it }

                soundUri?.let {
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    setSound(it, audioAttributes)
                }

                vibrationPattern?.let {
                    enableVibration(true)
                    this.vibrationPattern = it
                }

                lightColor?.let {
                    enableLights(true)
                    this.lightColor = it
                }
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 显示通知
     */
    fun showNotification(config: NotificationConfig): Int {
        val builder = NotificationCompat.Builder(context, config.channelId)
            .setContentTitle(config.title)
            .setContentText(config.content)
            .setSmallIcon(config.smallIcon)
            .setPriority(config.priority)
            .setAutoCancel(config.autoCancel)
            .setOngoing(config.ongoing)
            .setShowWhen(config.showWhen)
            .setWhen(config.`when`)

        // 设置大图标
        config.largeIcon?.let { builder.setLargeIcon(it) }

        // 设置分类
        config.category?.let { builder.setCategory(it) }

        // 设置振动
        config.vibrate?.let { builder.setVibrate(it) }

        // 设置声音
        config.sound?.let { builder.setSound(it) }

        // 设置LED灯
        config.lights?.let { builder.setLights(it, 1000, 1000) }

        // 添加操作按钮
        config.actions.forEach { action ->
            if (action.isReply && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY).run {
                    setLabel(action.replyLabel ?: "回复")
                    build()
                }

                val replyAction = NotificationCompat.Action.Builder(
                    action.icon,
                    action.title,
                    action.pendingIntent
                )
                    .addRemoteInput(remoteInput)
                    .build()

                builder.addAction(replyAction)
            } else {
                builder.addAction(action.icon, action.title, action.pendingIntent)
            }
        }

        // 设置样式
        when (val style = config.style) {
            is NotificationStyle.BigText -> {
                builder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(style.bigText)
                        .setSummaryText(style.summaryText)
                )
            }
            is NotificationStyle.BigPicture -> {
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(style.picture)
                        .setSummaryText(style.summaryText)
                )
            }
            is NotificationStyle.Inbox -> {
                val inboxStyle = NotificationCompat.InboxStyle()
                style.lines.forEach { inboxStyle.addLine(it) }
                style.summaryText?.let { inboxStyle.setSummaryText(it) }
                builder.setStyle(inboxStyle)
            }
            is NotificationStyle.Messaging -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val messagingStyle = NotificationCompat.MessagingStyle("Me")
                    style.conversationTitle?.let { messagingStyle.setConversationTitle(it) }
                    messagingStyle.isGroupConversation = style.isGroupConversation

                    style.messages.forEach { message ->
                        messagingStyle.addMessage(
                            message.text,
                            message.timestamp,
                            message.sender
                        )
                    }
                    builder.setStyle(messagingStyle)
                }
            }
            null -> { /* No style */ }
        }

        // 设置进度条
        config.progress?.let { progress ->
            builder.setProgress(progress.max, progress.progress, progress.indeterminate)
        }

        // 设置分组
        config.groupKey?.let { builder.setGroup(it) }
        config.sortKey?.let { builder.setSortKey(it) }

        // 添加额外数据
        config.extras?.let { builder.extras.putAll(it) }

        val notification = builder.build()

        // 检查权限（Android 13+需要POST_NOTIFICATIONS权限）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationManagerCompat.notify(config.id, notification)
            }
        } else {
            notificationManagerCompat.notify(config.id, notification)
        }

        // 保存到历史记录
        saveToHistory(config)

        return config.id
    }

    /**
     * 取消通知
     */
    fun cancelNotification(id: Int) {
        notificationManagerCompat.cancel(id)
        updateHistoryAsCancelled(id)
    }

    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        notificationManagerCompat.cancelAll()
    }

    /**
     * 更新通知
     */
    fun updateNotification(config: NotificationConfig) {
        showNotification(config)
    }

    /**
     * 获取活动的通知
     */
    fun getActiveNotifications(): List<StatusBarNotification> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications.toList()
        } else {
            emptyList()
        }
    }

    /**
     * 检查通知权限
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManagerCompat.areNotificationsEnabled()
    }

    /**
     * 检查通知渠道是否启用
     */
    fun isChannelEnabled(channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        return true
    }

    /**
     * 获取所有通知渠道
     */
    fun getNotificationChannels(): List<NotificationChannel> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.notificationChannels
        } else {
            emptyList()
        }
    }

    /**
     * 删除通知渠道
     */
    fun deleteNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(channelId)
        }
    }

    /**
     * 创建进度通知
     */
    fun showProgressNotification(
        title: String,
        content: String,
        max: Int = 100,
        progress: Int = 0,
        indeterminate: Boolean = false,
        id: Int = System.currentTimeMillis().toInt()
    ): Int {
        val config = NotificationConfig(
            id = id,
            title = title,
            content = content,
            smallIcon = android.R.drawable.stat_sys_download,
            ongoing = true,
            autoCancel = false,
            progress = ProgressConfig(max, progress, indeterminate)
        )
        showNotification(config)
        return id
    }

    /**
     * 更新进度通知
     */
    fun updateProgress(id: Int, progress: Int, max: Int = 100) {
        val builder = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
            .setProgress(max, progress, false)
            .setOnlyAlertOnce(true)

        notificationManagerCompat.notify(id, builder.build())
    }

    /**
     * 创建定时通知
     */
    suspend fun scheduleNotification(
        config: NotificationConfig,
        delayMillis: Long
    ) = withContext(Dispatchers.IO) {
        kotlinx.coroutines.delay(delayMillis)
        withContext(Dispatchers.Main) {
            showNotification(config)
        }
    }

    /**
     * 创建分组通知
     */
    fun showGroupedNotifications(
        notifications: List<NotificationConfig>,
        groupKey: String,
        groupSummaryTitle: String,
        groupSummaryContent: String
    ) {
        // 显示各个通知
        notifications.forEach { config ->
            showNotification(config.copy(groupKey = groupKey))
        }

        // 显示分组摘要
        val summaryConfig = NotificationConfig(
            id = groupKey.hashCode(),
            title = groupSummaryTitle,
            content = groupSummaryContent,
            smallIcon = notifications.firstOrNull()?.smallIcon ?: android.R.drawable.ic_dialog_info,
            groupKey = groupKey,
            style = NotificationStyle.Inbox(
                lines = notifications.map { it.title },
                summaryText = "${notifications.size} 条通知"
            )
        )

        val summaryBuilder = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
            .setContentTitle(summaryConfig.title)
            .setContentText(summaryConfig.content)
            .setSmallIcon(summaryConfig.smallIcon)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)

        notificationManagerCompat.notify(summaryConfig.id, summaryBuilder.build())
    }

    // ========== 历史记录管理 ==========

    private fun saveToHistory(config: NotificationConfig) {
        val history = NotificationHistory(
            id = config.id,
            title = config.title,
            content = config.content,
            timestamp = System.currentTimeMillis(),
            packageName = context.packageName,
            isRead = false,
            isCancelled = false
        )

        val historyList = getNotificationHistory().toMutableList()
        historyList.add(0, history)

        // 限制历史记录数量
        if (historyList.size > MAX_HISTORY_ITEMS) {
            historyList.subList(MAX_HISTORY_ITEMS, historyList.size).clear()
        }

        saveHistoryToPrefs(historyList)
    }

    private fun updateHistoryAsCancelled(id: Int) {
        val historyList = getNotificationHistory().toMutableList()
        val index = historyList.indexOfFirst { it.id == id }
        if (index != -1) {
            historyList[index] = historyList[index].copy(isCancelled = true)
            saveHistoryToPrefs(historyList)
        }
    }

    fun markAsRead(id: Int) {
        val historyList = getNotificationHistory().toMutableList()
        val index = historyList.indexOfFirst { it.id == id }
        if (index != -1) {
            historyList[index] = historyList[index].copy(isRead = true)
            saveHistoryToPrefs(historyList)
        }
    }

    fun getNotificationHistory(): List<NotificationHistory> {
        val json = sharedPrefs.getString(NOTIFICATION_HISTORY_KEY, "[]")
        return parseHistoryFromJson(json ?: "[]")
    }

    fun clearHistory() {
        sharedPrefs.edit().remove(NOTIFICATION_HISTORY_KEY).apply()
    }

    private fun saveHistoryToPrefs(history: List<NotificationHistory>) {
        val json = convertHistoryToJson(history)
        sharedPrefs.edit().putString(NOTIFICATION_HISTORY_KEY, json).apply()
    }

    private fun parseHistoryFromJson(json: String): List<NotificationHistory> {
        val list = mutableListOf<NotificationHistory>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(
                    NotificationHistory(
                        id = item.getInt("id"),
                        title = item.getString("title"),
                        content = item.getString("content"),
                        timestamp = item.getLong("timestamp"),
                        packageName = item.getString("packageName"),
                        isRead = item.getBoolean("isRead"),
                        isCancelled = item.getBoolean("isCancelled")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun convertHistoryToJson(history: List<NotificationHistory>): String {
        val jsonArray = JSONArray()
        history.forEach { item ->
            val json = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("content", item.content)
                put("timestamp", item.timestamp)
                put("packageName", item.packageName)
                put("isRead", item.isRead)
                put("isCancelled", item.isCancelled)
            }
            jsonArray.put(json)
        }
        return jsonArray.toString()
    }

    /**
     * 获取统计信息
     */
    fun getStatistics(): NotificationStatistics {
        val history = getNotificationHistory()
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)

        return NotificationStatistics(
            totalCount = history.size,
            readCount = history.count { it.isRead },
            unreadCount = history.count { !it.isRead && !it.isCancelled },
            cancelledCount = history.count { it.isCancelled },
            todayCount = history.count { it.timestamp >= oneDayAgo },
            weekCount = history.count { it.timestamp >= oneWeekAgo }
        )
    }

    data class NotificationStatistics(
        val totalCount: Int,
        val readCount: Int,
        val unreadCount: Int,
        val cancelledCount: Int,
        val todayCount: Int,
        val weekCount: Int
    )

    // ========== 预设模板 ==========

    object Templates {
        fun downloadComplete(
            fileName: String,
            fileSize: String,
            smallIcon: Int = android.R.drawable.stat_sys_download_done
        ): NotificationConfig {
            return NotificationConfig(
                title = "下载完成",
                content = "$fileName ($fileSize)",
                smallIcon = smallIcon,
                channelId = DEFAULT_CHANNEL_ID,
                priority = NotificationCompat.PRIORITY_DEFAULT,
                category = NotificationCompat.CATEGORY_PROGRESS
            )
        }

        fun reminder(
            title: String,
            content: String,
            smallIcon: Int = android.R.drawable.ic_popup_reminder
        ): NotificationConfig {
            return NotificationConfig(
                title = title,
                content = content,
                smallIcon = smallIcon,
                channelId = HIGH_PRIORITY_CHANNEL_ID,
                priority = NotificationCompat.PRIORITY_HIGH,
                category = NotificationCompat.CATEGORY_REMINDER,
                vibrate = longArrayOf(0, 500, 200, 500)
            )
        }

        fun message(
            sender: String,
            messageText: String,
            smallIcon: Int = android.R.drawable.ic_dialog_email
        ): NotificationConfig {
            return NotificationConfig(
                title = sender,
                content = messageText,
                smallIcon = smallIcon,
                channelId = DEFAULT_CHANNEL_ID,
                priority = NotificationCompat.PRIORITY_HIGH,
                category = NotificationCompat.CATEGORY_MESSAGE
            )
        }

        fun alarm(
            title: String,
            smallIcon: Int = android.R.drawable.ic_popup_reminder
        ): NotificationConfig {
            return NotificationConfig(
                title = "闹钟",
                content = title,
                smallIcon = smallIcon,
                channelId = HIGH_PRIORITY_CHANNEL_ID,
                priority = NotificationCompat.PRIORITY_MAX,
                category = NotificationCompat.CATEGORY_ALARM,
                ongoing = true,
                vibrate = longArrayOf(0, 1000, 500, 1000, 500, 1000)
            )
        }
    }
}