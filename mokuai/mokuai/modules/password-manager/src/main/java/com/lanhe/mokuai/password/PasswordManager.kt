package com.lanhe.mokuai.password

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 密码管理器 - 安全存储和管理密码
 */
class PasswordManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "encrypted_passwords"
        private const val KEY_PASSWORDS = "passwords"
        private const val KEY_ALIAS = "PasswordManagerKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    data class PasswordEntry(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val username: String,
        val password: String,
        val url: String = "",
        val notes: String = "",
        val category: String = "默认",
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis(),
        val lastUsed: Long = 0,
        val usageCount: Int = 0,
        val isFavorite: Boolean = false,
        val tags: List<String> = emptyList()
    )

    /**
     * 添加新密码
     */
    fun addPassword(entry: PasswordEntry): Boolean {
        return try {
            val passwords = getAllPasswordsInternal().toMutableList()

            // 检查是否已存在
            if (passwords.any { it.id == entry.id }) {
                return false
            }

            passwords.add(entry)
            savePasswords(passwords)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 更新密码
     */
    fun updatePassword(entry: PasswordEntry): Boolean {
        return try {
            val passwords = getAllPasswordsInternal().toMutableList()
            val index = passwords.indexOfFirst { it.id == entry.id }

            if (index == -1) {
                return false
            }

            passwords[index] = entry.copy(updatedAt = System.currentTimeMillis())
            savePasswords(passwords)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除密码
     */
    fun deletePassword(id: String): Boolean {
        return try {
            val passwords = getAllPasswordsInternal().toMutableList()
            val removed = passwords.removeAll { it.id == id }
            if (removed) {
                savePasswords(passwords)
            }
            removed
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取所有密码
     */
    fun getAllPasswords(): List<PasswordEntry> {
        return getAllPasswordsInternal()
    }

    /**
     * 根据ID获取密码
     */
    fun getPassword(id: String): PasswordEntry? {
        return getAllPasswordsInternal().find { it.id == id }
    }

    /**
     * 搜索密码
     */
    fun searchPasswords(query: String): List<PasswordEntry> {
        val lowercaseQuery = query.lowercase()
        return getAllPasswordsInternal().filter { entry ->
            entry.title.lowercase().contains(lowercaseQuery) ||
            entry.username.lowercase().contains(lowercaseQuery) ||
            entry.url.lowercase().contains(lowercaseQuery) ||
            entry.notes.lowercase().contains(lowercaseQuery) ||
            entry.tags.any { it.lowercase().contains(lowercaseQuery) }
        }
    }

    /**
     * 按类别获取密码
     */
    fun getPasswordsByCategory(category: String): List<PasswordEntry> {
        return getAllPasswordsInternal().filter { it.category == category }
    }

    /**
     * 获取收藏的密码
     */
    fun getFavoritePasswords(): List<PasswordEntry> {
        return getAllPasswordsInternal().filter { it.isFavorite }
    }

    /**
     * 获取最近使用的密码
     */
    fun getRecentPasswords(limit: Int = 10): List<PasswordEntry> {
        return getAllPasswordsInternal()
            .filter { it.lastUsed > 0 }
            .sortedByDescending { it.lastUsed }
            .take(limit)
    }

    /**
     * 获取所有类别
     */
    fun getAllCategories(): List<String> {
        return getAllPasswordsInternal()
            .map { it.category }
            .distinct()
            .sorted()
    }

    /**
     * 获取所有标签
     */
    fun getAllTags(): List<String> {
        return getAllPasswordsInternal()
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }

    /**
     * 记录密码使用
     */
    fun recordPasswordUsage(id: String) {
        val passwords = getAllPasswordsInternal().toMutableList()
        val index = passwords.indexOfFirst { it.id == id }

        if (index != -1) {
            passwords[index] = passwords[index].copy(
                lastUsed = System.currentTimeMillis(),
                usageCount = passwords[index].usageCount + 1
            )
            savePasswords(passwords)
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(id: String): Boolean {
        val passwords = getAllPasswordsInternal().toMutableList()
        val index = passwords.indexOfFirst { it.id == id }

        if (index != -1) {
            passwords[index] = passwords[index].copy(
                isFavorite = !passwords[index].isFavorite
            )
            savePasswords(passwords)
            return passwords[index].isFavorite
        }
        return false
    }

    /**
     * 生成强密码
     */
    fun generateStrongPassword(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true,
        excludeAmbiguous: Boolean = true
    ): String {
        val charset = buildString {
            if (includeLowercase) append("abcdefghijklmnopqrstuvwxyz")
            if (includeUppercase) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            if (includeNumbers) append("0123456789")
            if (includeSymbols) append("!@#\$%^&*()_+-=[]{}|;:,.<>?")
        }

        // 排除易混淆字符
        val finalCharset = if (excludeAmbiguous) {
            charset.replace(Regex("[0OIl1]"), "")
        } else {
            charset
        }

        if (finalCharset.isEmpty()) {
            return ""
        }

        val random = SecureRandom()
        return (1..length)
            .map { finalCharset[random.nextInt(finalCharset.length)] }
            .joinToString("")
    }

    /**
     * 检查密码强度
     */
    fun checkPasswordStrength(password: String): PasswordStrength {
        var score = 0

        // 长度
        when {
            password.length >= 16 -> score += 25
            password.length >= 12 -> score += 20
            password.length >= 8 -> score += 10
            else -> score += 5
        }

        // 包含小写字母
        if (password.any { it.isLowerCase() }) score += 10

        // 包含大写字母
        if (password.any { it.isUpperCase() }) score += 10

        // 包含数字
        if (password.any { it.isDigit() }) score += 10

        // 包含特殊字符
        if (password.any { !it.isLetterOrDigit() }) score += 15

        // 字符种类多样性
        val charTypes = listOf(
            password.any { it.isLowerCase() },
            password.any { it.isUpperCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() }
        ).count { it }

        score += charTypes * 10

        // 连续字符检查
        if (!hasConsecutiveChars(password)) score += 10

        // 常见密码检查
        if (!isCommonPassword(password)) score += 10

        return when {
            score >= 80 -> PasswordStrength.VERY_STRONG
            score >= 60 -> PasswordStrength.STRONG
            score >= 40 -> PasswordStrength.MEDIUM
            score >= 20 -> PasswordStrength.WEAK
            else -> PasswordStrength.VERY_WEAK
        }
    }

    private fun hasConsecutiveChars(password: String): Boolean {
        for (i in 0 until password.length - 2) {
            if (password[i] == password[i + 1] && password[i] == password[i + 2]) {
                return true
            }
        }
        return false
    }

    private fun isCommonPassword(password: String): Boolean {
        val commonPasswords = listOf(
            "password", "123456", "12345678", "qwerty", "abc123",
            "111111", "123123", "admin", "letmein", "welcome"
        )
        return commonPasswords.contains(password.lowercase())
    }

    /**
     * 导出密码到JSON
     */
    fun exportToJson(): String {
        val passwords = getAllPasswordsInternal()
        val jsonArray = JSONArray()

        passwords.forEach { entry ->
            val json = JSONObject().apply {
                put("id", entry.id)
                put("title", entry.title)
                put("username", entry.username)
                put("password", entry.password)
                put("url", entry.url)
                put("notes", entry.notes)
                put("category", entry.category)
                put("createdAt", entry.createdAt)
                put("updatedAt", entry.updatedAt)
                put("lastUsed", entry.lastUsed)
                put("usageCount", entry.usageCount)
                put("isFavorite", entry.isFavorite)
                put("tags", JSONArray(entry.tags))
            }
            jsonArray.put(json)
        }

        return JSONObject().apply {
            put("version", 1)
            put("exportDate", System.currentTimeMillis())
            put("passwords", jsonArray)
        }.toString(2)
    }

    /**
     * 从JSON导入密码
     */
    fun importFromJson(json: String): Boolean {
        return try {
            val jsonObject = JSONObject(json)
            val jsonArray = jsonObject.getJSONArray("passwords")
            val passwords = mutableListOf<PasswordEntry>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val tags = mutableListOf<String>()

                val tagsArray = item.optJSONArray("tags")
                if (tagsArray != null) {
                    for (j in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(j))
                    }
                }

                passwords.add(
                    PasswordEntry(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        username = item.getString("username"),
                        password = item.getString("password"),
                        url = item.optString("url", ""),
                        notes = item.optString("notes", ""),
                        category = item.optString("category", "默认"),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                        lastUsed = item.optLong("lastUsed", 0),
                        usageCount = item.optInt("usageCount", 0),
                        isFavorite = item.optBoolean("isFavorite", false),
                        tags = tags
                    )
                )
            }

            savePasswords(passwords)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清除所有密码
     */
    fun clearAllPasswords() {
        encryptedPrefs.edit().remove(KEY_PASSWORDS).apply()
    }

    private fun getAllPasswordsInternal(): List<PasswordEntry> {
        return try {
            val json = encryptedPrefs.getString(KEY_PASSWORDS, null)
            if (json.isNullOrEmpty()) {
                emptyList()
            } else {
                parsePasswordsFromJson(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun savePasswords(passwords: List<PasswordEntry>) {
        val jsonArray = JSONArray()
        passwords.forEach { entry ->
            val json = JSONObject().apply {
                put("id", entry.id)
                put("title", entry.title)
                put("username", entry.username)
                put("password", entry.password)
                put("url", entry.url)
                put("notes", entry.notes)
                put("category", entry.category)
                put("createdAt", entry.createdAt)
                put("updatedAt", entry.updatedAt)
                put("lastUsed", entry.lastUsed)
                put("usageCount", entry.usageCount)
                put("isFavorite", entry.isFavorite)
                put("tags", JSONArray(entry.tags))
            }
            jsonArray.put(json)
        }
        encryptedPrefs.edit().putString(KEY_PASSWORDS, jsonArray.toString()).apply()
    }

    private fun parsePasswordsFromJson(json: String): List<PasswordEntry> {
        val passwords = mutableListOf<PasswordEntry>()
        val jsonArray = JSONArray(json)

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val tags = mutableListOf<String>()

            val tagsArray = item.optJSONArray("tags")
            if (tagsArray != null) {
                for (j in 0 until tagsArray.length()) {
                    tags.add(tagsArray.getString(j))
                }
            }

            passwords.add(
                PasswordEntry(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    username = item.getString("username"),
                    password = item.getString("password"),
                    url = item.optString("url", ""),
                    notes = item.optString("notes", ""),
                    category = item.optString("category", "默认"),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                    lastUsed = item.optLong("lastUsed", 0),
                    usageCount = item.optInt("usageCount", 0),
                    isFavorite = item.optBoolean("isFavorite", false),
                    tags = tags
                )
            )
        }

        return passwords
    }

    enum class PasswordStrength {
        VERY_WEAK, WEAK, MEDIUM, STRONG, VERY_STRONG
    }
}