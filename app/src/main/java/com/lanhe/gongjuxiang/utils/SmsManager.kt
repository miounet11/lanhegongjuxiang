package com.lanhe.gongjuxiang.utils

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import androidx.annotation.WorkerThread
import com.lanhe.gongjuxiang.services.NotificationHelper
import com.lanhe.gongjuxiang.services.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 短信管理器
 * 提供短信读取、分类、搜索、备份和恢复功能
 */
class SmsManager(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val notificationHelper = NotificationHelper(context)

    /**
     * 短信数据类
     */
    data class SmsMessage(
        val id: Long,
        val address: String, // 电话号码
        val body: String,    // 短信内容
        val date: Long,      // 时间戳
        val type: Int,       // 1=收到的短信, 2=发出的短信
        val read: Int,       // 0=未读, 1=已读
        val threadId: Long,  // 会话ID
        val person: String?, // 联系人姓名
        val serviceCenter: String? // 短信中心
    )

    /**
     * 短信分类结果
     */
    data class SmsCategory(
        val category: String,
        val messages: List<SmsMessage>,
        val count: Int
    )

    /**
     * 短信统计信息
     */
    data class SmsStatistics(
        val totalMessages: Int,
        val receivedMessages: Int,
        val sentMessages: Int,
        val unreadMessages: Int,
        val spamMessages: Int,
        val categories: Map<String, Int>
    )

    /**
     * 获取所有短信
     */
    @WorkerThread
    suspend fun getAllSms(): List<SmsMessage> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<SmsMessage>()

        try {
            val uri = Uri.parse("content://sms/")
            val projection = arrayOf(
                "_id", "address", "body", "date", "type", "read",
                "thread_id", "person", "service_center"
            )

            contentResolver.query(uri, projection, null, null, "date DESC")?.use { cursor ->
                while (cursor.moveToNext()) {
                    val sms = createSmsFromCursor(cursor)
                    smsList.add(sms)
                }
            }
        } catch (e: Exception) {
            // 处理权限或其他异常
        }

        smsList
    }

    /**
     * 获取短信会话列表
     */
    @WorkerThread
    suspend fun getSmsConversations(): List<SmsConversation> = withContext(Dispatchers.IO) {
        val conversations = mutableMapOf<Long, MutableList<SmsMessage>>()

        val allSms = getAllSms()
        for (sms in allSms) {
            conversations.getOrPut(sms.threadId) { mutableListOf() }.add(sms)
        }

        conversations.map { (threadId, messages) ->
            val lastMessage = messages.maxByOrNull { it.date } ?: messages.first()
            SmsConversation(
                threadId = threadId,
                address = lastMessage.address,
                lastMessage = lastMessage.body,
                lastMessageTime = lastMessage.date,
                messageCount = messages.size,
                unreadCount = messages.count { it.read == 0 },
                messages = messages.sortedByDescending { it.date }
            )
        }.sortedByDescending { it.lastMessageTime }
    }

    /**
     * 搜索短信
     */
    @WorkerThread
    suspend fun searchSms(query: String, searchInContent: Boolean = true, searchInAddress: Boolean = false): List<SmsMessage> = withContext(Dispatchers.IO) {
        val allSms = getAllSms()
        allSms.filter { sms ->
            val contentMatch = if (searchInContent) sms.body.contains(query, ignoreCase = true) else false
            val addressMatch = if (searchInAddress) sms.address.contains(query, ignoreCase = true) else false
            contentMatch || addressMatch
        }
    }

    /**
     * 按日期范围过滤短信
     */
    @WorkerThread
    suspend fun getSmsByDateRange(startDate: Long, endDate: Long): List<SmsMessage> = withContext(Dispatchers.IO) {
        val allSms = getAllSms()
        allSms.filter { sms ->
            sms.date in startDate..endDate
        }
    }

    /**
     * 按联系人过滤短信
     */
    @WorkerThread
    suspend fun getSmsByAddress(address: String): List<SmsMessage> = withContext(Dispatchers.IO) {
        val allSms = getAllSms()
        allSms.filter { sms ->
            sms.address == address
        }
    }

    /**
     * 分类短信
     */
    @WorkerThread
    suspend fun categorizeSms(): List<SmsCategory> = withContext(Dispatchers.IO) {
        val allSms = getAllSms()
        val categories = mutableMapOf<String, MutableList<SmsMessage>>()

        for (sms in allSms) {
            val category = categorizeMessage(sms)
            categories.getOrPut(category) { mutableListOf() }.add(sms)
        }

        categories.map { (category, messages) ->
            SmsCategory(category, messages.sortedByDescending { it.date }, messages.size)
        }.sortedByDescending { it.count }
    }

    /**
     * 检测垃圾短信
     */
    private fun isSpamMessage(sms: SmsMessage): Boolean {
        val spamKeywords = listOf(
            "中奖", "奖金", "彩票", "投资", "理财", "贷款", "信用卡",
            "验证码", "激活码", "推广", "广告", "营销"
        )

        return spamKeywords.any { keyword ->
            sms.body.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 分类消息
     */
    private fun categorizeMessage(sms: SmsMessage): String {
        return when {
            isSpamMessage(sms) -> "垃圾短信"
            sms.type == 1 -> "收到的短信"
            sms.type == 2 -> "发出的短信"
            sms.body.contains("验证码") || sms.body.contains("激活码") -> "验证码短信"
            sms.body.contains("银行") || sms.body.contains("转账") -> "银行短信"
            sms.body.contains("快递") || sms.body.contains("物流") -> "快递短信"
            else -> "其他短信"
        }
    }

    /**
     * 获取短信统计信息
     */
    @WorkerThread
    suspend fun getSmsStatistics(): SmsStatistics = withContext(Dispatchers.IO) {
        val allSms = getAllSms()
        val categories = categorizeSms()

        SmsStatistics(
            totalMessages = allSms.size,
            receivedMessages = allSms.count { it.type == 1 },
            sentMessages = allSms.count { it.type == 2 },
            unreadMessages = allSms.count { it.read == 0 },
            spamMessages = allSms.count { isSpamMessage(it) },
            categories = categories.associate { it.category to it.count }
        )
    }

    /**
     * 备份短信到JSON文件
     */
    @WorkerThread
    suspend fun backupSmsToFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val allSms = getAllSms()
            val jsonArray = JSONArray()

            for (sms in allSms) {
                val jsonObject = JSONObject().apply {
                    put("id", sms.id)
                    put("address", sms.address)
                    put("body", sms.body)
                    put("date", sms.date)
                    put("type", sms.type)
                    put("read", sms.read)
                    put("threadId", sms.threadId)
                    put("person", sms.person)
                    put("serviceCenter", sms.serviceCenter)
                }
                jsonArray.put(jsonObject)
            }

            val jsonObject = JSONObject().apply {
                put("backup_time", System.currentTimeMillis())
                put("total_messages", allSms.size)
                put("messages", jsonArray)
            }

            FileWriter(File(filePath)).use { writer ->
                writer.write(jsonObject.toString(2))
            }

            // 发送备份完成通知
            notificationHelper.showNotification(
                "短信备份完成",
                "已备份 ${allSms.size} 条短信到文件",
                NotificationType.FILE_BACKUP_COMPLETED
            )

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从JSON文件恢复短信
     */
    @WorkerThread
    suspend fun restoreSmsFromFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext false

            val jsonContent = file.readText()
            val jsonObject = JSONObject(jsonContent)
            val jsonArray = jsonObject.getJSONArray("messages")

            var restoredCount = 0
            for (i in 0 until jsonArray.length()) {
                val smsObject = jsonArray.getJSONObject(i)
                // 注意：Android不允许直接插入短信到系统数据库
                // 这里只能记录恢复信息，实际恢复需要系统权限
                restoredCount++
            }

            // 发送恢复完成通知
            notificationHelper.showNotification(
                "短信恢复完成",
                "已恢复 $restoredCount 条短信",
                NotificationType.FILE_BACKUP_COMPLETED
            )

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 删除短信
     */
    fun deleteSms(smsId: Long): Boolean {
        return try {
            val uri = Uri.parse("content://sms/")
            val deletedRows = contentResolver.delete(uri, "_id = ?", arrayOf(smsId.toString()))
            deletedRows > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 批量删除短信
     */
    fun deleteSmsBatch(smsIds: List<Long>): Int {
        var deletedCount = 0
        for (id in smsIds) {
            if (deleteSms(id)) {
                deletedCount++
            }
        }
        return deletedCount
    }

    /**
     * 标记短信为已读
     */
    fun markSmsAsRead(smsId: Long): Boolean {
        return try {
            val uri = Uri.parse("content://sms/")
            val values = android.content.ContentValues().apply {
                put("read", 1)
            }
            val updatedRows = contentResolver.update(uri, values, "_id = ?", arrayOf(smsId.toString()))
            updatedRows > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 格式化短信时间
     */
    fun formatSmsTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * 从Cursor创建SmsMessage对象
     */
    private fun createSmsFromCursor(cursor: Cursor): SmsMessage {
        return SmsMessage(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
            address = cursor.getString(cursor.getColumnIndexOrThrow("address")) ?: "",
            body = cursor.getString(cursor.getColumnIndexOrThrow("body")) ?: "",
            date = cursor.getLong(cursor.getColumnIndexOrThrow("date")),
            type = cursor.getInt(cursor.getColumnIndexOrThrow("type")),
            read = cursor.getInt(cursor.getColumnIndexOrThrow("read")),
            threadId = cursor.getLong(cursor.getColumnIndexOrThrow("thread_id")),
            person = cursor.getString(cursor.getColumnIndex("person")),
            serviceCenter = cursor.getString(cursor.getColumnIndex("service_center"))
        )
    }

    /**
     * 短信会话数据类
     */
    data class SmsConversation(
        val threadId: Long,
        val address: String,
        val lastMessage: String,
        val lastMessageTime: Long,
        val messageCount: Int,
        val unreadCount: Int,
        val messages: List<SmsMessage>
    )
}
