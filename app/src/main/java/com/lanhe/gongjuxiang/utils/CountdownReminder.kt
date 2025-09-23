package com.lanhe.gongjuxiang.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.lanhe.gongjuxiang.services.NotificationHelper
import com.lanhe.gongjuxiang.services.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 倒计时提醒器
 * 提供定时提醒、重复提醒和智能提醒功能
 */
class CountdownReminder(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationHelper = NotificationHelper(context)
    private val sharedPrefs = context.getSharedPreferences("countdown_reminders", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 提醒项数据类
     */
    data class ReminderItem(
        val id: Long,
        val title: String,
        val message: String,
        val targetTime: Long,
        val createdTime: Long,
        val isActive: Boolean,
        val repeatType: RepeatType,
        val category: String,
        val priority: Priority,
        val soundEnabled: Boolean = true,
        val vibrationEnabled: Boolean = true
    )

    /**
     * 重复类型枚举
     */
    enum class RepeatType {
        NONE,           // 不重复
        DAILY,          // 每天
        WEEKLY,         // 每周
        MONTHLY,        // 每月
        YEARLY,         // 每年
        CUSTOM_DAYS    // 自定义天数
    }

    /**
     * 优先级枚举
     */
    enum class Priority {
        LOW, NORMAL, HIGH, URGENT
    }

    /**
     * 提醒统计信息
     */
    data class ReminderStatistics(
        val totalReminders: Int,
        val activeReminders: Int,
        val completedToday: Int,
        val upcomingReminders: Int,
        val overdueReminders: Int
    )

    /**
     * 倒计时信息
     */
    data class CountdownInfo(
        val reminder: ReminderItem,
        val remainingTime: Long,
        val isOverdue: Boolean,
        val formattedTime: String
    )

    /**
     * 创建提醒
     */
    fun createReminder(
        title: String,
        message: String,
        targetTime: Long,
        repeatType: RepeatType = RepeatType.NONE,
        category: String = "默认",
        priority: Priority = Priority.NORMAL,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ): Long {
        val id = System.currentTimeMillis()
        val reminder = ReminderItem(
            id = id,
            title = title,
            message = message,
            targetTime = targetTime,
            createdTime = System.currentTimeMillis(),
            isActive = true,
            repeatType = repeatType,
            category = category,
            priority = priority,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled
        )

        // 保存到存储
        saveReminder(reminder)

        // 设置闹钟
        scheduleReminder(reminder)

        return id
    }

    /**
     * 更新提醒
     */
    fun updateReminder(reminder: ReminderItem): Boolean {
        return try {
            saveReminder(reminder)
            if (reminder.isActive) {
                scheduleReminder(reminder)
            } else {
                cancelReminder(reminder.id)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除提醒
     */
    fun deleteReminder(reminderId: Long): Boolean {
        return try {
            cancelReminder(reminderId)
            removeReminder(reminderId)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取所有提醒
     */
    fun getAllReminders(): List<ReminderItem> {
        return loadAllReminders().sortedBy { it.targetTime }
    }

    /**
     * 获取活动提醒
     */
    fun getActiveReminders(): List<ReminderItem> {
        return getAllReminders().filter { it.isActive }
    }

    /**
     * 获取即将到来的提醒
     */
    fun getUpcomingReminders(hoursAhead: Int = 24): List<CountdownInfo> {
        val currentTime = System.currentTimeMillis()
        val futureTime = currentTime + (hoursAhead * 60 * 60 * 1000L)

        return getActiveReminders()
            .filter { it.targetTime in currentTime..futureTime }
            .map { reminder ->
                val remainingTime = reminder.targetTime - currentTime
                CountdownInfo(
                    reminder = reminder,
                    remainingTime = remainingTime,
                    isOverdue = false,
                    formattedTime = formatRemainingTime(remainingTime)
                )
            }
            .sortedBy { it.remainingTime }
    }

    /**
     * 获取过期提醒
     */
    fun getOverdueReminders(): List<CountdownInfo> {
        val currentTime = System.currentTimeMillis()

        return getActiveReminders()
            .filter { it.targetTime < currentTime }
            .map { reminder ->
                val overdueTime = currentTime - reminder.targetTime
                CountdownInfo(
                    reminder = reminder,
                    remainingTime = -overdueTime,
                    isOverdue = true,
                    formattedTime = formatRemainingTime(-overdueTime) + "前"
                )
            }
            .sortedByDescending { it.remainingTime } // 按过期时间倒序
    }

    /**
     * 获取提醒统计
     */
    fun getReminderStatistics(): ReminderStatistics {
        val allReminders = getAllReminders()
        val activeReminders = allReminders.filter { it.isActive }
        val currentTime = System.currentTimeMillis()
        val todayStart = getStartOfDay(currentTime)

        val completedToday = allReminders.count { reminder ->
            !reminder.isActive && reminder.targetTime >= todayStart && reminder.targetTime <= currentTime
        }

        val upcomingReminders = activeReminders.count { it.targetTime > currentTime }
        val overdueReminders = activeReminders.count { it.targetTime < currentTime }

        return ReminderStatistics(
            totalReminders = allReminders.size,
            activeReminders = activeReminders.size,
            completedToday = completedToday,
            upcomingReminders = upcomingReminders,
            overdueReminders = overdueReminders
        )
    }

    /**
     * 按类别分组提醒
     */
    fun getRemindersByCategory(): Map<String, List<ReminderItem>> {
        return getAllReminders().groupBy { it.category }
    }

    /**
     * 搜索提醒
     */
    fun searchReminders(query: String): List<ReminderItem> {
        val lowerQuery = query.lowercase()
        return getAllReminders().filter { reminder ->
            reminder.title.lowercase().contains(lowerQuery) ||
            reminder.message.lowercase().contains(lowerQuery) ||
            reminder.category.lowercase().contains(lowerQuery)
        }
    }

    /**
     * 批量操作提醒
     */
    fun batchDeleteReminders(reminderIds: List<Long>): Int {
        var deletedCount = 0
        for (id in reminderIds) {
            if (deleteReminder(id)) {
                deletedCount++
            }
        }
        return deletedCount
    }

    /**
     * 批量启用/禁用提醒
     */
    fun batchToggleReminders(reminderIds: List<Long>, enable: Boolean): Int {
        var updatedCount = 0
        for (id in reminderIds) {
            val reminder = getReminderById(id)
            if (reminder != null) {
                val updatedReminder = reminder.copy(isActive = enable)
                if (updateReminder(updatedReminder)) {
                    updatedCount++
                }
            }
        }
        return updatedCount
    }

    /**
     * 智能提醒建议
     */
    fun getSmartSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        val stats = getReminderStatistics()
        val upcoming = getUpcomingReminders(1) // 1小时内的提醒

        if (stats.overdueReminders > 0) {
            suggestions.add("您有 ${stats.overdueReminders} 个过期的提醒需要处理")
        }

        if (upcoming.size > 3) {
            suggestions.add("今天有 ${upcoming.size} 个提醒，建议合理安排时间")
        }

        if (stats.activeReminders == 0) {
            suggestions.add("还没有设置任何提醒，建议添加一些重要事项的提醒")
        }

        // 分析提醒模式
        val categories = getRemindersByCategory()
        val workReminders = categories["工作"]?.size ?: 0
        val personalReminders = categories["个人"]?.size ?: 0

        if (workReminders > personalReminders * 2) {
            suggestions.add("工作提醒较多，记得平衡工作和生活")
        }

        return suggestions
    }

    /**
     * 快速创建提醒
     */
    fun createQuickReminder(title: String, minutesFromNow: Int): Long {
        val targetTime = System.currentTimeMillis() + (minutesFromNow * 60 * 1000L)
        return createReminder(
            title = title,
            message = "快速提醒：$title",
            targetTime = targetTime,
            priority = Priority.NORMAL
        )
    }

    /**
     * 设置闹钟
     */
    private fun scheduleReminder(reminder: ReminderItem) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("reminder_title", reminder.title)
            putExtra("reminder_message", reminder.message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 根据重复类型设置闹钟
        when (reminder.repeatType) {
            RepeatType.NONE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.targetTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.targetTime, pendingIntent)
                }
            }
            RepeatType.DAILY -> {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, reminder.targetTime, AlarmManager.INTERVAL_DAY, pendingIntent)
            }
            RepeatType.WEEKLY -> {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, reminder.targetTime, 7 * AlarmManager.INTERVAL_DAY, pendingIntent)
            }
            RepeatType.MONTHLY -> {
                // 简化处理，使用30天作为近似值
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, reminder.targetTime, 30 * AlarmManager.INTERVAL_DAY, pendingIntent)
            }
            RepeatType.YEARLY -> {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, reminder.targetTime, 365 * AlarmManager.INTERVAL_DAY, pendingIntent)
            }
            RepeatType.CUSTOM_DAYS -> {
                // 暂时不支持，需要额外参数
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.targetTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.targetTime, pendingIntent)
                }
            }
        }
    }

    /**
     * 取消提醒
     */
    private fun cancelReminder(reminderId: Long) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * 保存提醒到存储
     */
    private fun saveReminder(reminder: ReminderItem) {
        val key = "reminder_${reminder.id}"
        sharedPrefs.edit().apply {
            putString("${key}_title", reminder.title)
            putString("${key}_message", reminder.message)
            putLong("${key}_target_time", reminder.targetTime)
            putLong("${key}_created_time", reminder.createdTime)
            putBoolean("${key}_active", reminder.isActive)
            putString("${key}_repeat_type", reminder.repeatType.name)
            putString("${key}_category", reminder.category)
            putString("${key}_priority", reminder.priority.name)
            putBoolean("${key}_sound", reminder.soundEnabled)
            putBoolean("${key}_vibration", reminder.vibrationEnabled)
            apply()
        }
    }

    /**
     * 从存储加载提醒
     */
    private fun loadAllReminders(): List<ReminderItem> {
        val reminders = mutableListOf<ReminderItem>()
        val allEntries = sharedPrefs.all

        val reminderKeys = allEntries.keys.filter { it.startsWith("reminder_") && it.endsWith("_title") }
        for (key in reminderKeys) {
            try {
                val keyPrefix = key.substringBeforeLast("_")
                val id = keyPrefix.substringAfter("reminder_").toLongOrNull() ?: continue

                val title = sharedPrefs.getString("${keyPrefix}_title", "") ?: continue
                val message = sharedPrefs.getString("${keyPrefix}_message", "") ?: ""
                val targetTime = sharedPrefs.getLong("${keyPrefix}_target_time", 0L)
                val createdTime = sharedPrefs.getLong("${keyPrefix}_created_time", 0L)
                val isActive = sharedPrefs.getBoolean("${keyPrefix}_active", true)
                val repeatTypeStr = sharedPrefs.getString("${keyPrefix}_repeat_type", RepeatType.NONE.name)
                val category = sharedPrefs.getString("${keyPrefix}_category", "默认") ?: "默认"
                val priorityStr = sharedPrefs.getString("${keyPrefix}_priority", Priority.NORMAL.name)
                val soundEnabled = sharedPrefs.getBoolean("${keyPrefix}_sound", true)
                val vibrationEnabled = sharedPrefs.getBoolean("${keyPrefix}_vibration", true)

                val repeatType = try {
                    RepeatType.valueOf(repeatTypeStr ?: RepeatType.NONE.name)
                } catch (e: Exception) {
                    RepeatType.NONE
                }

                val priority = try {
                    Priority.valueOf(priorityStr ?: Priority.NORMAL.name)
                } catch (e: Exception) {
                    Priority.NORMAL
                }

                reminders.add(ReminderItem(
                    id = id,
                    title = title,
                    message = message,
                    targetTime = targetTime,
                    createdTime = createdTime,
                    isActive = isActive,
                    repeatType = repeatType,
                    category = category,
                    priority = priority,
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled
                ))
            } catch (e: Exception) {
                // 忽略解析错误的提醒
            }
        }

        return reminders
    }

    /**
     * 删除提醒从存储
     */
    private fun removeReminder(reminderId: Long) {
        val keyPrefix = "reminder_$reminderId"
        sharedPrefs.edit().apply {
            remove("${keyPrefix}_title")
            remove("${keyPrefix}_message")
            remove("${keyPrefix}_target_time")
            remove("${keyPrefix}_created_time")
            remove("${keyPrefix}_active")
            remove("${keyPrefix}_repeat_type")
            remove("${keyPrefix}_category")
            remove("${keyPrefix}_priority")
            remove("${keyPrefix}_sound")
            remove("${keyPrefix}_vibration")
            apply()
        }
    }

    /**
     * 根据ID获取提醒
     */
    private fun getReminderById(id: Long): ReminderItem? {
        return getAllReminders().find { it.id == id }
    }

    /**
     * 格式化剩余时间
     */
    private fun formatRemainingTime(millis: Long): String {
        val absMillis = Math.abs(millis)
        val seconds = absMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}天${hours % 24}小时"
            hours > 0 -> "${hours}小时${minutes % 60}分钟"
            minutes > 0 -> "${minutes}分钟${seconds % 60}秒"
            else -> "${seconds}秒"
        }
    }

    /**
     * 获取一天的开始时间
     */
    private fun getStartOfDay(time: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * 提醒广播接收器
     */
    class ReminderBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context == null || intent == null) return

            val reminderId = intent.getLongExtra("reminder_id", 0L)
            val title = intent.getStringExtra("reminder_title") ?: "提醒"
            val message = intent.getStringExtra("reminder_message") ?: "时间到了！"

            val notificationHelper = NotificationHelper(context)
            notificationHelper.showNotification(
                title = title,
                message = message,
                type = NotificationType.BATTERY_LOW // 使用现有的通知类型，实际应该添加专门的提醒类型
            )

            // 播放提醒音效（如果需要）
            // TODO: 实现音效播放
        }
    }

    /**
     * 获取默认提醒类别
     */
    fun getDefaultCategories(): List<String> {
        return listOf(
            "默认",
            "工作",
            "个人",
            "健康",
            "学习",
            "娱乐",
            "旅行",
            "购物",
            "其他"
        )
    }

    /**
     * 导出提醒到JSON
     */
    fun exportRemindersToJson(): String {
        val reminders = getAllReminders()
        val jsonArray = org.json.JSONArray()

        for (reminder in reminders) {
            val jsonObject = org.json.JSONObject().apply {
                put("id", reminder.id)
                put("title", reminder.title)
                put("message", reminder.message)
                put("targetTime", reminder.targetTime)
                put("createdTime", reminder.createdTime)
                put("isActive", reminder.isActive)
                put("repeatType", reminder.repeatType.name)
                put("category", reminder.category)
                put("priority", reminder.priority.name)
                put("soundEnabled", reminder.soundEnabled)
                put("vibrationEnabled", reminder.vibrationEnabled)
            }
            jsonArray.put(jsonObject)
        }

        return jsonArray.toString(2)
    }

    /**
     * 从JSON导入提醒
     */
    fun importRemindersFromJson(jsonString: String): Int {
        return try {
            val jsonArray = org.json.JSONArray(jsonString)
            var importedCount = 0

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)

                val reminder = ReminderItem(
                    id = System.currentTimeMillis() + i, // 避免ID冲突
                    title = jsonObject.getString("title"),
                    message = jsonObject.getString("message"),
                    targetTime = jsonObject.getLong("targetTime"),
                    createdTime = jsonObject.getLong("createdTime"),
                    isActive = jsonObject.getBoolean("isActive"),
                    repeatType = RepeatType.valueOf(jsonObject.getString("repeatType")),
                    category = jsonObject.getString("category"),
                    priority = Priority.valueOf(jsonObject.getString("priority")),
                    soundEnabled = jsonObject.optBoolean("soundEnabled", true),
                    vibrationEnabled = jsonObject.optBoolean("vibrationEnabled", true)
                )

                saveReminder(reminder)
                if (reminder.isActive) {
                    scheduleReminder(reminder)
                }
                importedCount++
            }

            importedCount
        } catch (e: Exception) {
            0
        }
    }
}
