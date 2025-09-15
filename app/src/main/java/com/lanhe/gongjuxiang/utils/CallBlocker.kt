package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.lanhe.gongjuxiang.services.NotificationHelper
import com.lanhe.gongjuxiang.services.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * 号码卫士 - 电话拦截器
 * 提供来电拦截、黑名单管理和防火墙功能
 */
@RequiresApi(29) // 需要API 29+支持CallScreeningService
class CallBlocker(private val context: Context) : CallScreeningService() {

    private val notificationHelper = NotificationHelper(context)
    private val sharedPrefs = context.getSharedPreferences("call_blocker_prefs", Context.MODE_PRIVATE)

    /**
     * 黑名单条目数据类
     */
    data class BlacklistEntry(
        val id: Long,
        val number: String,
        val name: String? = null,
        val blockType: BlockType,
        val addedTime: Long,
        val blockCount: Int = 0
    )

    /**
     * 拦截类型枚举
     */
    enum class BlockType {
        EXACT_MATCH,    // 精确匹配
        STARTS_WITH,    // 前缀匹配
        CONTAINS,       // 包含匹配
        REGEX           // 正则表达式
    }

    /**
     * 拦截记录数据类
     */
    data class BlockRecord(
        val id: Long,
        val number: String,
        val name: String? = null,
        val blockTime: Long,
        val blockReason: String
    )

    /**
     * 拦截统计数据类
     */
    data class BlockStatistics(
        val totalBlocked: Int,
        val todayBlocked: Int,
        val weekBlocked: Int,
        val monthBlocked: Int,
        val topBlockers: List<Pair<String, Int>>
    )

    // 黑名单缓存
    private var blacklistCache: MutableList<BlacklistEntry> = mutableListOf()
    private var lastCacheUpdate: Long = 0
    private val CACHE_VALID_TIME = 5 * 60 * 1000 // 5分钟

    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: return

        // 检查是否应该拦截
        val shouldBlock = shouldBlockCall(phoneNumber)

        if (shouldBlock) {
            // 拦截来电
            val response = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()

            respondToCall(callDetails, response)

            // 记录拦截
            recordBlockedCall(phoneNumber, "黑名单拦截")

            // 发送通知
            notificationHelper.showNotification(
                "来电已拦截",
                "来自 $phoneNumber 的来电已被拦截",
                NotificationType.CALL_BLOCKED
            )
        } else {
            // 允许来电
            val response = CallResponse.Builder()
                .setDisallowCall(false)
                .build()

            respondToCall(callDetails, response)
        }
    }

    /**
     * 检查是否应该拦截来电
     */
    private fun shouldBlockCall(phoneNumber: String): Boolean {
        updateBlacklistCacheIfNeeded()

        for (entry in blacklistCache) {
            if (matchesBlockRule(phoneNumber, entry)) {
                return true
            }
        }

        return false
    }

    /**
     * 检查号码是否匹配拦截规则
     */
    private fun matchesBlockRule(phoneNumber: String, entry: BlacklistEntry): Boolean {
        return when (entry.blockType) {
            BlockType.EXACT_MATCH -> phoneNumber == entry.number
            BlockType.STARTS_WITH -> phoneNumber.startsWith(entry.number)
            BlockType.CONTAINS -> phoneNumber.contains(entry.number)
            BlockType.REGEX -> {
                try {
                    Pattern.matches(entry.number, phoneNumber)
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    /**
     * 更新黑名单缓存
     */
    private fun updateBlacklistCacheIfNeeded() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCacheUpdate > CACHE_VALID_TIME) {
            blacklistCache = loadBlacklistFromStorage().toMutableList()
            lastCacheUpdate = currentTime
        }
    }

    /**
     * 添加号码到黑名单
     */
    fun addToBlacklist(number: String, name: String? = null, blockType: BlockType = BlockType.EXACT_MATCH): Boolean {
        return try {
            val entry = BlacklistEntry(
                id = System.currentTimeMillis(),
                number = number,
                name = name,
                blockType = blockType,
                addedTime = System.currentTimeMillis()
            )

            // 保存到存储
            saveBlacklistEntry(entry)

            // 更新缓存
            blacklistCache.add(entry)

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 从黑名单移除号码
     */
    fun removeFromBlacklist(number: String): Boolean {
        return try {
            // 从存储中删除
            removeBlacklistEntry(number)

            // 更新缓存
            blacklistCache.removeAll { it.number == number }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取黑名单列表
     */
    fun getBlacklist(): List<BlacklistEntry> {
        updateBlacklistCacheIfNeeded()
        return blacklistCache.toList()
    }

    /**
     * 检查号码是否在黑名单中
     */
    fun isInBlacklist(number: String): Boolean {
        updateBlacklistCacheIfNeeded()
        return blacklistCache.any { matchesBlockRule(number, it) }
    }

    /**
     * 记录被拦截的来电
     */
    private fun recordBlockedCall(number: String, reason: String) {
        val record = BlockRecord(
            id = System.currentTimeMillis(),
            number = number,
            blockTime = System.currentTimeMillis(),
            blockReason = reason
        )

        // 保存到存储
        saveBlockRecord(record)

        // 更新黑名单中的拦截计数
        updateBlockCount(number)
    }

    /**
     * 获取拦截记录
     */
    fun getBlockRecords(limit: Int = 100): List<BlockRecord> {
        return loadBlockRecords(limit)
    }

    /**
     * 获取拦截统计
     */
    fun getBlockStatistics(): BlockStatistics {
        val allRecords = getBlockRecords(1000)
        val currentTime = System.currentTimeMillis()

        val todayStart = currentTime - 24 * 60 * 60 * 1000
        val weekStart = currentTime - 7 * 24 * 60 * 60 * 1000
        val monthStart = currentTime - 30 * 24 * 60 * 60 * 1000

        val todayBlocked = allRecords.count { it.blockTime >= todayStart }
        val weekBlocked = allRecords.count { it.blockTime >= weekStart }
        val monthBlocked = allRecords.count { it.blockTime >= monthStart }

        // 统计最常被拦截的号码
        val numberCount = mutableMapOf<String, Int>()
        allRecords.forEach { record ->
            numberCount[record.number] = numberCount.getOrDefault(record.number, 0) + 1
        }

        val topBlockers = numberCount.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key to it.value }

        return BlockStatistics(
            totalBlocked = allRecords.size,
            todayBlocked = todayBlocked,
            weekBlocked = weekBlocked,
            monthBlocked = monthBlocked,
            topBlockers = topBlockers
        )
    }

    /**
     * 清理过期记录
     */
    fun cleanupOldRecords(daysToKeep: Int = 90) {
        val cutoffTime = System.currentTimeMillis() - daysToKeep * 24 * 60 * 60 * 1000L
        cleanupBlockRecords(cutoffTime)
    }

    /**
     * 导入黑名单
     */
    fun importBlacklist(numbers: List<String>): Int {
        var importedCount = 0
        for (number in numbers) {
            if (addToBlacklist(number.trim())) {
                importedCount++
            }
        }
        return importedCount
    }

    /**
     * 导出黑名单
     */
    fun exportBlacklist(): List<String> {
        return getBlacklist().map { entry ->
            "${entry.number},${entry.name ?: ""},${entry.blockType}"
        }
    }

    /**
     * 号码归属地查询（模拟实现）
     */
    fun getPhoneLocation(phoneNumber: String): String {
        // 这里可以集成号码归属地查询API
        // 暂时返回模拟数据
        return when {
            phoneNumber.startsWith("130") -> "中国联通"
            phoneNumber.startsWith("131") -> "中国联通"
            phoneNumber.startsWith("132") -> "中国联通"
            phoneNumber.startsWith("133") -> "中国联通"
            phoneNumber.startsWith("134") -> "中国移动"
            phoneNumber.startsWith("135") -> "中国移动"
            phoneNumber.startsWith("136") -> "中国移动"
            phoneNumber.startsWith("137") -> "中国移动"
            phoneNumber.startsWith("138") -> "中国移动"
            phoneNumber.startsWith("139") -> "中国移动"
            phoneNumber.startsWith("150") -> "中国移动"
            phoneNumber.startsWith("151") -> "中国移动"
            phoneNumber.startsWith("152") -> "中国移动"
            phoneNumber.startsWith("153") -> "中国联通"
            phoneNumber.startsWith("155") -> "中国联通"
            phoneNumber.startsWith("156") -> "中国联通"
            phoneNumber.startsWith("157") -> "中国移动"
            phoneNumber.startsWith("158") -> "中国移动"
            phoneNumber.startsWith("159") -> "中国移动"
            phoneNumber.startsWith("180") -> "中国电信"
            phoneNumber.startsWith("181") -> "中国电信"
            phoneNumber.startsWith("182") -> "中国移动"
            phoneNumber.startsWith("183") -> "中国移动"
            phoneNumber.startsWith("184") -> "中国移动"
            phoneNumber.startsWith("185") -> "中国联通"
            phoneNumber.startsWith("186") -> "中国联通"
            phoneNumber.startsWith("187") -> "中国移动"
            phoneNumber.startsWith("188") -> "中国移动"
            phoneNumber.startsWith("189") -> "中国电信"
            else -> "未知运营商"
        }
    }

    /**
     * 检测骚扰电话
     */
    fun isSpamCall(phoneNumber: String): Boolean {
        // 简单的骚扰电话检测规则
        val spamPatterns = listOf(
            "400\\d{7}".toRegex(),  // 400开头的客服电话
            "800\\d{7}".toRegex(),  // 800开头的客服电话
            "1[3-9]\\d{9}".toRegex() // 正常的手机号码格式
        )

        // 检查是否是已知骚扰号码特征
        return phoneNumber.length < 7 || // 号码太短
               phoneNumber.all { it.isDigit() } && phoneNumber.length > 11 // 号码太长
    }

    // 存储相关方法（使用SharedPreferences模拟，需要时可改为数据库）

    private fun saveBlacklistEntry(entry: BlacklistEntry) {
        val key = "blacklist_${entry.id}"
        sharedPrefs.edit().apply {
            putString("${key}_number", entry.number)
            putString("${key}_name", entry.name)
            putString("${key}_type", entry.blockType.name)
            putLong("${key}_time", entry.addedTime)
            putInt("${key}_count", entry.blockCount)
            apply()
        }
    }

    private fun removeBlacklistEntry(number: String) {
        val allEntries = sharedPrefs.all
        val keysToRemove = allEntries.keys.filter { key ->
            key.startsWith("blacklist_") && sharedPrefs.getString("${key}_number", "") == number
        }

        sharedPrefs.edit().apply {
            keysToRemove.forEach { remove(it) }
            apply()
        }
    }

    private fun loadBlacklistFromStorage(): List<BlacklistEntry> {
        val entries = mutableListOf<BlacklistEntry>()
        val allEntries = sharedPrefs.all

        val groupedKeys = allEntries.keys
            .filter { it.startsWith("blacklist_") }
            .groupBy { it.substringBeforeLast("_") }

        for ((keyPrefix, _) in groupedKeys) {
            try {
                val number = sharedPrefs.getString("${keyPrefix}_number", "") ?: continue
                val name = sharedPrefs.getString("${keyPrefix}_name", null)
                val typeStr = sharedPrefs.getString("${keyPrefix}_type", BlockType.EXACT_MATCH.name)
                val time = sharedPrefs.getLong("${keyPrefix}_time", 0L)
                val count = sharedPrefs.getInt("${keyPrefix}_count", 0)

                val blockType = try {
                    BlockType.valueOf(typeStr)
                } catch (e: Exception) {
                    BlockType.EXACT_MATCH
                }

                entries.add(BlacklistEntry(
                    id = keyPrefix.substringAfter("blacklist_").toLongOrNull() ?: 0L,
                    number = number,
                    name = name,
                    blockType = blockType,
                    addedTime = time,
                    blockCount = count
                ))
            } catch (e: Exception) {
                // 忽略解析错误的条目
            }
        }

        return entries
    }

    private fun saveBlockRecord(record: BlockRecord) {
        val key = "block_record_${record.id}"
        sharedPrefs.edit().apply {
            putString("${key}_number", record.number)
            putString("${key}_name", record.name)
            putLong("${key}_time", record.blockTime)
            putString("${key}_reason", record.blockReason)
            apply()
        }
    }

    private fun loadBlockRecords(limit: Int): List<BlockRecord> {
        val records = mutableListOf<BlockRecord>()
        val allEntries = sharedPrefs.all

        val recordKeys = allEntries.keys
            .filter { it.startsWith("block_record_") }
            .sortedByDescending { it }
            .take(limit)

        for (key in recordKeys) {
            try {
                val keyPrefix = key.substringBeforeLast("_")
                val number = sharedPrefs.getString("${keyPrefix}_number", "") ?: continue
                val name = sharedPrefs.getString("${keyPrefix}_name", null)
                val time = sharedPrefs.getLong("${keyPrefix}_time", 0L)
                val reason = sharedPrefs.getString("${keyPrefix}_reason", "")

                records.add(BlockRecord(
                    id = keyPrefix.substringAfter("block_record_").toLongOrNull() ?: 0L,
                    number = number,
                    name = name,
                    blockTime = time,
                    blockReason = reason
                ))
            } catch (e: Exception) {
                // 忽略解析错误的记录
            }
        }

        return records.sortedByDescending { it.blockTime }
    }

    private fun updateBlockCount(number: String) {
        val allEntries = sharedPrefs.all
        val blacklistKeys = allEntries.keys.filter { key ->
            key.startsWith("blacklist_") && key.endsWith("_number") &&
            sharedPrefs.getString(key, "") == number
        }

        for (key in blacklistKeys) {
            val keyPrefix = key.substringBeforeLast("_")
            val currentCount = sharedPrefs.getInt("${keyPrefix}_count", 0)
            sharedPrefs.edit().putInt("${keyPrefix}_count", currentCount + 1).apply()
        }
    }

    private fun cleanupBlockRecords(cutoffTime: Long) {
        val allEntries = sharedPrefs.all
        val keysToRemove = allEntries.keys.filter { key ->
            key.startsWith("block_record_") && key.endsWith("_time") &&
            sharedPrefs.getLong(key, 0L) < cutoffTime
        }.map { it.substringBeforeLast("_") }.distinct()

        sharedPrefs.edit().apply {
            keysToRemove.forEach { keyPrefix ->
                remove("${keyPrefix}_number")
                remove("${keyPrefix}_name")
                remove("${keyPrefix}_time")
                remove("${keyPrefix}_reason")
            }
            apply()
        }
    }
}
