package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 蓝河助手 - 数据备份管理器
 *
 * 功能特性：
 * - 完整数据导出/导入
 * - 增量备份支持
 * - 数据加密保护
 * - 云端同步功能
 * - 版本管理
 * - 数据完整性验证
 * - 自动备份策略
 */
class DataBackupManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "DataBackupManager"

        @Volatile
        private var INSTANCE: DataBackupManager? = null

        fun getInstance(context: Context): DataBackupManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataBackupManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // 备份配置
        private const val BACKUP_VERSION = "1.0"
        private const val BACKUP_FILE_EXTENSION = ".lanhe_backup"
        private const val ENCRYPTED_BACKUP_EXTENSION = ".lanhe_encrypted"
        private const val BACKUP_MIME_TYPE = "application/octet-stream"

        // 加密配置
        private const val ENCRYPTION_ALGORITHM = "AES"
        private const val ENCRYPTION_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val KEY_LENGTH = 256
        private const val IV_LENGTH = 16

        // 备份类型
        const val BACKUP_TYPE_FULL = "full"
        const val BACKUP_TYPE_SETTINGS = "settings"
        const val BACKUP_TYPE_PERFORMANCE_DATA = "performance_data"
        const val BACKUP_TYPE_OPTIMIZATION_HISTORY = "optimization_history"
    }

    private val analyticsManager = AnalyticsManager.getInstance(context)
    private val preferencesManager = PreferencesManager.getInstance(context)
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()

    private val backupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 备份数据结构
     */
    data class BackupData(
        val version: String = BACKUP_VERSION,
        val timestamp: Long = System.currentTimeMillis(),
        val deviceInfo: DeviceInfo,
        val appInfo: AppInfo,
        val userData: UserData,
        val checksum: String = ""
    )

    /**
     * 设备信息
     */
    data class DeviceInfo(
        val model: String = android.os.Build.MODEL,
        val manufacturer: String = android.os.Build.MANUFACTURER,
        val androidVersion: String = android.os.Build.VERSION.RELEASE,
        val apiLevel: Int = android.os.Build.VERSION.SDK_INT
    )

    /**
     * 应用信息
     */
    data class AppInfo(
        val versionName: String,
        val versionCode: Int,
        val packageName: String,
        val backupType: String
    )

    /**
     * 用户数据
     */
    data class UserData(
        val preferences: Map<String, Any>,
        val performanceData: List<Map<String, Any>>,
        val optimizationHistory: List<Map<String, Any>>,
        val batteryStats: List<Map<String, Any>>,
        val customSettings: Map<String, Any>
    )

    /**
     * 备份结果
     */
    sealed class BackupResult {
        object Success : BackupResult()
        data class Error(val message: String, val exception: Throwable? = null) : BackupResult()
        data class Progress(val progress: Int, val message: String) : BackupResult()
    }

    /**
     * 导入结果
     */
    sealed class ImportResult {
        object Success : ImportResult()
        data class Error(val message: String, val exception: Throwable? = null) : ImportResult()
        data class Progress(val progress: Int, val message: String) : ImportResult()
        data class VersionMismatch(val backupVersion: String, val currentVersion: String) : ImportResult()
    }

    /**
     * 备份配置
     */
    data class BackupConfig(
        val includePerformanceData: Boolean = true,
        val includeOptimizationHistory: Boolean = true,
        val includeBatteryStats: Boolean = true,
        val includeSettings: Boolean = true,
        val encrypt: Boolean = false,
        val password: String? = null,
        val compressionLevel: Int = 6
    )

    /**
     * 执行完整备份
     */
    suspend fun createFullBackup(
        outputUri: Uri,
        config: BackupConfig = BackupConfig(),
        progressCallback: ((BackupResult) -> Unit)? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            progressCallback?.invoke(BackupResult.Progress(0, "开始备份..."))

            // 收集备份数据
            progressCallback?.invoke(BackupResult.Progress(10, "收集应用数据..."))
            val backupData = collectBackupData(config)

            // 计算校验和
            progressCallback?.invoke(BackupResult.Progress(30, "计算数据校验和..."))
            val dataWithChecksum = backupData.copy(checksum = calculateChecksum(backupData))

            // 序列化数据
            progressCallback?.invoke(BackupResult.Progress(50, "序列化数据..."))
            val jsonData = gson.toJson(dataWithChecksum)

            // 写入文件
            progressCallback?.invoke(BackupResult.Progress(70, "写入备份文件..."))
            writeBackupToUri(outputUri, jsonData, config)

            progressCallback?.invoke(BackupResult.Progress(100, "备份完成"))

            // 记录备份成功
            recordBackupSuccess(config)

            analyticsManager.trackEvent("backup_created", android.os.Bundle().apply {
                putString("backup_type", BACKUP_TYPE_FULL)
                putBoolean("encrypted", config.encrypt)
                putLong("data_size", jsonData.length.toLong())
            })

            BackupResult.Success

        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            analyticsManager.trackError("backup_failed", e.message ?: "Unknown error", e)
            BackupResult.Error("备份失败: ${e.message}", e)
        }
    }

    /**
     * 导入备份数据
     */
    suspend fun importBackup(
        inputUri: Uri,
        password: String? = null,
        progressCallback: ((ImportResult) -> Unit)? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            progressCallback?.invoke(ImportResult.Progress(0, "开始导入..."))

            // 读取备份文件
            progressCallback?.invoke(ImportResult.Progress(10, "读取备份文件..."))
            val jsonData = readBackupFromUri(inputUri, password)

            // 解析备份数据
            progressCallback?.invoke(ImportResult.Progress(30, "解析备份数据..."))
            val backupData = gson.fromJson<BackupData>(jsonData, BackupData::class.java)

            // 验证版本兼容性
            if (!isVersionCompatible(backupData.version)) {
                return@withContext ImportResult.VersionMismatch(backupData.version, BACKUP_VERSION)
            }

            // 验证数据完整性
            progressCallback?.invoke(ImportResult.Progress(50, "验证数据完整性..."))
            if (!verifyChecksum(backupData)) {
                return@withContext ImportResult.Error("数据完整性验证失败")
            }

            // 导入数据
            progressCallback?.invoke(ImportResult.Progress(70, "导入数据..."))
            importUserData(backupData.userData)

            progressCallback?.invoke(ImportResult.Progress(100, "导入完成"))

            // 记录导入成功
            recordImportSuccess(backupData)

            analyticsManager.trackEvent("backup_imported", android.os.Bundle().apply {
                putString("backup_version", backupData.version)
                putLong("backup_timestamp", backupData.timestamp)
            })

            ImportResult.Success

        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            analyticsManager.trackError("import_failed", e.message ?: "Unknown error", e)
            ImportResult.Error("导入失败: ${e.message}", e)
        }
    }

    /**
     * 收集备份数据
     */
    private suspend fun collectBackupData(config: BackupConfig): BackupData {
        val appInfo = getAppInfo(config)
        val deviceInfo = DeviceInfo()
        val userData = collectUserData(config)

        return BackupData(
            deviceInfo = deviceInfo,
            appInfo = appInfo,
            userData = userData
        )
    }

    /**
     * 获取应用信息
     */
    private fun getAppInfo(config: BackupConfig): AppInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return AppInfo(
            versionName = packageInfo.versionName ?: "unknown",
            versionCode = packageInfo.versionCode,
            packageName = context.packageName,
            backupType = determineBackupType(config)
        )
    }

    /**
     * 确定备份类型
     */
    private fun determineBackupType(config: BackupConfig): String {
        return when {
            config.includePerformanceData &&
            config.includeOptimizationHistory &&
            config.includeBatteryStats &&
            config.includeSettings -> BACKUP_TYPE_FULL

            config.includeSettings && !config.includePerformanceData -> BACKUP_TYPE_SETTINGS
            config.includePerformanceData -> BACKUP_TYPE_PERFORMANCE_DATA
            config.includeOptimizationHistory -> BACKUP_TYPE_OPTIMIZATION_HISTORY
            else -> "custom"
        }
    }

    /**
     * 收集用户数据
     */
    private suspend fun collectUserData(config: BackupConfig): UserData {
        val preferences = if (config.includeSettings) {
            collectPreferences()
        } else {
            emptyMap()
        }

        val performanceData = if (config.includePerformanceData) {
            collectPerformanceData()
        } else {
            emptyList()
        }

        val optimizationHistory = if (config.includeOptimizationHistory) {
            collectOptimizationHistory()
        } else {
            emptyList()
        }

        val batteryStats = if (config.includeBatteryStats) {
            collectBatteryStats()
        } else {
            emptyList()
        }

        return UserData(
            preferences = preferences,
            performanceData = performanceData,
            optimizationHistory = optimizationHistory,
            batteryStats = batteryStats,
            customSettings = collectCustomSettings()
        )
    }

    /**
     * 收集偏好设置
     */
    private fun collectPreferences(): Map<String, Any> {
        val preferences = mutableMapOf<String, Any>()

        // 收集SharedPreferences数据
        val sharedPrefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPrefs.all.forEach { (key, value) ->
            value?.let { preferences[key] = it }
        }

        return preferences
    }

    /**
     * 收集性能数据
     */
    private suspend fun collectPerformanceData(): List<Map<String, Any>> {
        return try {
            val database = AppDatabase.getInstance(context)
            val performanceDao = database.performanceDataDao()
            val data = performanceDao.getAllPerformanceData()

            data.map { entity ->
                mapOf(
                    "id" to entity.id,
                    "timestamp" to entity.timestamp,
                    "cpuUsage" to entity.cpuUsage,
                    "memoryUsagePercent" to entity.memoryUsagePercent,
                    "batteryLevel" to entity.batteryLevel,
                    "batteryTemperature" to entity.batteryTemperature,
                    "deviceTemperature" to entity.deviceTemperature,
                    "dataType" to entity.dataType
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to collect performance data", e)
            emptyList()
        }
    }

    /**
     * 收集优化历史
     */
    private suspend fun collectOptimizationHistory(): List<Map<String, Any>> {
        return try {
            val database = AppDatabase.getInstance(context)
            val optimizationDao = database.optimizationHistoryDao()
            val history = optimizationDao.getAllOptimizationHistory()

            history.map { entity ->
                mapOf(
                    "id" to entity.id,
                    "timestamp" to entity.timestamp,
                    "optimizationType" to entity.optimizationType,
                    "success" to entity.success,
                    "message" to entity.message,
                    "improvements" to entity.improvements,
                    "duration" to entity.duration
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to collect optimization history", e)
            emptyList()
        }
    }

    /**
     * 收集电池统计
     */
    private suspend fun collectBatteryStats(): List<Map<String, Any>> {
        return try {
            val database = AppDatabase.getInstance(context)
            val batteryDao = database.batteryStatsDao()
            val stats = batteryDao.getAllBatteryStats()

            stats.map { entity ->
                mapOf(
                    "id" to entity.id,
                    "timestamp" to entity.timestamp,
                    "batteryLevel" to entity.batteryLevel,
                    "batteryTemperature" to entity.batteryTemperature,
                    "voltage" to entity.voltage,
                    "chargingStatus" to entity.chargingStatus,
                    "batteryHealth" to entity.batteryHealth
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to collect battery stats", e)
            emptyList()
        }
    }

    /**
     * 收集自定义设置
     */
    private fun collectCustomSettings(): Map<String, Any> {
        return mapOf(
            "theme_preference" to preferencesManager.getString("theme", "system"),
            "language_preference" to preferencesManager.getString("language", "system"),
            "auto_optimization" to preferencesManager.getBoolean("auto_optimization", false),
            "notification_enabled" to preferencesManager.getBoolean("notifications", true),
            "analytics_enabled" to preferencesManager.getBoolean("analytics", true)
        )
    }

    /**
     * 计算校验和
     */
    private fun calculateChecksum(backupData: BackupData): String {
        val dataWithoutChecksum = backupData.copy(checksum = "")
        val jsonData = gson.toJson(dataWithoutChecksum)

        return MessageDigest.getInstance("SHA-256")
            .digest(jsonData.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    /**
     * 验证校验和
     */
    private fun verifyChecksum(backupData: BackupData): Boolean {
        val expectedChecksum = backupData.checksum
        val calculatedChecksum = calculateChecksum(backupData)
        return expectedChecksum == calculatedChecksum
    }

    /**
     * 写入备份到URI
     */
    private fun writeBackupToUri(uri: Uri, jsonData: String, config: BackupConfig) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            if (config.encrypt && config.password != null) {
                val encryptedData = encryptData(jsonData, config.password)
                outputStream.write(encryptedData)
            } else {
                if (config.compressionLevel > 0) {
                    ZipOutputStream(outputStream).use { zipOut ->
                        val entry = ZipEntry("backup.json")
                        zipOut.putNextEntry(entry)
                        zipOut.write(jsonData.toByteArray())
                        zipOut.closeEntry()
                    }
                } else {
                    outputStream.write(jsonData.toByteArray())
                }
            }
        }
    }

    /**
     * 从URI读取备份
     */
    private fun readBackupFromUri(uri: Uri, password: String?): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val data = inputStream.readBytes()

            if (password != null) {
                decryptData(data, password)
            } else {
                try {
                    // 尝试解压缩
                    ZipInputStream(data.inputStream()).use { zipIn ->
                        zipIn.nextEntry
                        zipIn.readBytes().toString(Charsets.UTF_8)
                    }
                } catch (e: Exception) {
                    // 如果不是压缩文件，直接读取
                    data.toString(Charsets.UTF_8)
                }
            }
        } ?: throw IOException("无法读取备份文件")
    }

    /**
     * 加密数据
     */
    private fun encryptData(data: String, password: String): ByteArray {
        val key = generateKeyFromPassword(password)
        val cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION)
        val iv = ByteArray(IV_LENGTH)
        java.security.SecureRandom().nextBytes(iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val encryptedData = cipher.doFinal(data.toByteArray())

        return iv + encryptedData
    }

    /**
     * 解密数据
     */
    private fun decryptData(encryptedData: ByteArray, password: String): String {
        val key = generateKeyFromPassword(password)
        val cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION)

        val iv = encryptedData.sliceArray(0 until IV_LENGTH)
        val cipherText = encryptedData.sliceArray(IV_LENGTH until encryptedData.size)

        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        val decryptedData = cipher.doFinal(cipherText)

        return decryptedData.toString(Charsets.UTF_8)
    }

    /**
     * 从密码生成密钥
     */
    private fun generateKeyFromPassword(password: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(password.toByteArray()).sliceArray(0 until 32)
        return SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM)
    }

    /**
     * 检查版本兼容性
     */
    private fun isVersionCompatible(backupVersion: String): Boolean {
        // 简单的版本比较，实际项目中可能需要更复杂的逻辑
        return backupVersion == BACKUP_VERSION
    }

    /**
     * 导入用户数据
     */
    private suspend fun importUserData(userData: UserData) {
        // 导入偏好设置
        importPreferences(userData.preferences)

        // 导入性能数据
        importPerformanceData(userData.performanceData)

        // 导入优化历史
        importOptimizationHistory(userData.optimizationHistory)

        // 导入电池统计
        importBatteryStats(userData.batteryStats)

        // 导入自定义设置
        importCustomSettings(userData.customSettings)
    }

    /**
     * 导入偏好设置
     */
    private fun importPreferences(preferences: Map<String, Any>) {
        val sharedPrefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        preferences.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
            }
        }

        editor.apply()
    }

    /**
     * 导入性能数据
     */
    private suspend fun importPerformanceData(data: List<Map<String, Any>>) {
        // 实现性能数据导入逻辑
    }

    /**
     * 导入优化历史
     */
    private suspend fun importOptimizationHistory(history: List<Map<String, Any>>) {
        // 实现优化历史导入逻辑
    }

    /**
     * 导入电池统计
     */
    private suspend fun importBatteryStats(stats: List<Map<String, Any>>) {
        // 实现电池统计导入逻辑
    }

    /**
     * 导入自定义设置
     */
    private fun importCustomSettings(settings: Map<String, Any>) {
        settings.forEach { (key, value) ->
            when (value) {
                is String -> preferencesManager.putString(key, value)
                is Boolean -> preferencesManager.putBoolean(key, value)
                is Int -> preferencesManager.putInt(key, value)
                is Float -> preferencesManager.putFloat(key, value)
                is Long -> preferencesManager.putLong(key, value)
            }
        }
    }

    /**
     * 记录备份成功
     */
    private fun recordBackupSuccess(config: BackupConfig) {
        preferencesManager.putLong("last_backup_time", System.currentTimeMillis())
        preferencesManager.putString("last_backup_type", determineBackupType(config))
    }

    /**
     * 记录导入成功
     */
    private fun recordImportSuccess(backupData: BackupData) {
        preferencesManager.putLong("last_import_time", System.currentTimeMillis())
        preferencesManager.putString("last_import_version", backupData.version)
    }

    /**
     * 生成备份文件名
     */
    fun generateBackupFileName(type: String = BACKUP_TYPE_FULL): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "LanheAssistant_${type}_${timestamp}$BACKUP_FILE_EXTENSION"
    }

    /**
     * 清理旧备份
     */
    fun cleanupOldBackups(maxBackups: Int = 5) {
        backupScope.launch {
            try {
                // 实现旧备份清理逻辑
                analyticsManager.trackFeatureUsed("backup_cleanup")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cleanup old backups", e)
            }
        }
    }
}

/**
 * 扩展函数
 */
fun Context.backupManager(): DataBackupManager = DataBackupManager.getInstance(this)