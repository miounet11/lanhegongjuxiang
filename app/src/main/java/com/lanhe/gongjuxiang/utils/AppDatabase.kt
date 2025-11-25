package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

/**
 * 应用数据库
 * 使用Room数据库存储性能监控数据和优化历史
 *
 * 版本历史：
 * v1: 初始版本 - PerformanceDataEntity, OptimizationHistoryEntity, BatteryStatsEntity
 * v2: 新增字段和表 - 扩展现有实体，新增NetworkUsageEntity和SystemEventsEntity
 * v3: 浏览器功能集成 - 新增BrowserHistoryEntity, BrowserTabEntity, BrowserDownloadEntity
 *
 * 并发安全改进：
 * - 使用标准DCL（Double-Checked Locking）模式
 * - @Volatile确保INSTANCE的内存可见性
 * - synchronized块确保线程安全的单例创建
 */
@Database(
    entities = [
        PerformanceDataEntity::class,
        OptimizationHistoryEntity::class,
        BatteryStatsEntity::class,
        NetworkUsageEntity::class,
        SystemEventsEntity::class,
        BrowserHistoryEntity::class,
        BrowserTabEntity::class,
        BrowserDownloadEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun performanceDataDao(): PerformanceDataDao
    abstract fun optimizationHistoryDao(): OptimizationHistoryDao
    abstract fun batteryStatsDao(): BatteryStatsDao
    abstract fun networkUsageDao(): NetworkUsageDao
    abstract fun systemEventsDao(): SystemEventsDao
    abstract fun browserHistoryDao(): BrowserHistoryDao
    abstract fun browserTabDao(): BrowserTabDao
    abstract fun browserDownloadDao(): BrowserDownloadDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DATABASE_NAME = "lanhe_gongjuxiang_database"

        /**
         * @Volatile注解确保INSTANCE变量的内存可见性
         * 当一个线程修改了INSTANCE的值，其他线程能立即看到这个变化
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 迁移2到3版本
         * 主要变更：
         * 1. 新增browser_history表 - 浏览历史记录
         * 2. 新增browser_tabs表 - 多标签页状态
         * 3. 新增browser_downloads表 - 下载记录
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Starting migration from version 2 to 3")

                try {
                    // 1. 创建browser_history表
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS browser_history (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            url TEXT NOT NULL UNIQUE,
                            title TEXT NOT NULL DEFAULT '',
                            visitTime INTEGER NOT NULL,
                            visitCount INTEGER NOT NULL DEFAULT 1,
                            favicon BLOB,
                            isBookmarked INTEGER NOT NULL DEFAULT 0,
                            searchTerm TEXT,
                            lastUpdated INTEGER NOT NULL DEFAULT 0
                        )
                    """)

                    // 创建browser_history索引
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_browser_history_url ON browser_history(url)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_browser_history_visitTime ON browser_history(visitTime)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_browser_history_isBookmarked ON browser_history(isBookmarked)")

                    // 2. 创建browser_tabs表
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS browser_tabs (
                            tabId TEXT PRIMARY KEY NOT NULL,
                            url TEXT NOT NULL DEFAULT 'about:blank',
                            title TEXT NOT NULL DEFAULT '',
                            favicon BLOB,
                            scrollY INTEGER NOT NULL DEFAULT 0,
                            createTime INTEGER NOT NULL DEFAULT 0,
                            lastAccessTime INTEGER NOT NULL DEFAULT 0,
                            isActive INTEGER NOT NULL DEFAULT 0,
                            isIncognito INTEGER NOT NULL DEFAULT 0,
                            thumbnailPath TEXT,
                            webViewState TEXT
                        )
                    """)

                    // 创建browser_tabs索引
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_browser_tabs_createTime ON browser_tabs(createTime)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_browser_tabs_lastAccessTime ON browser_tabs(lastAccessTime)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_browser_tabs_isActive ON browser_tabs(isActive)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_browser_tabs_isIncognito ON browser_tabs(isIncognito)")

                    // 3. 创建browser_downloads表
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS browser_downloads (
                            downloadId TEXT PRIMARY KEY NOT NULL,
                            url TEXT NOT NULL UNIQUE,
                            fileName TEXT NOT NULL,
                            filePath TEXT NOT NULL,
                            fileSize INTEGER NOT NULL DEFAULT 0,
                            downloadedSize INTEGER NOT NULL DEFAULT 0,
                            status TEXT NOT NULL DEFAULT 'PENDING',
                            mimeType TEXT,
                            createTime INTEGER NOT NULL DEFAULT 0,
                            completeTime INTEGER,
                            retryCount INTEGER NOT NULL DEFAULT 0
                        )
                    """)

                    // 创建browser_downloads索引
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_browser_downloads_status ON browser_downloads(status)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_browser_downloads_createTime ON browser_downloads(createTime)")
                    database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_browser_downloads_url ON browser_downloads(url)")

                    Log.d(TAG, "Migration from version 2 to 3 completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during migration from version 2 to 3", e)
                    throw e
                }
            }
        }

        /**
         * 迁移1到2版本
         * 主要变更：
         * 1. performance_data表新增字段：memoryUsedMB, memoryTotalMB, batteryVoltage等
         * 2. optimization_history表新增字段：beforeDataId, afterDataId
         * 3. battery_stats表新增字段：isPlugged, screenOnTime, screenOffTime等
         * 4. 新增network_usage表
         * 5. 新增system_events表
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Starting migration from version 1 to 2")

                try {
                    // 1. 为performance_data表添加新字段（使用默认值）
                    database.execSQL("ALTER TABLE performance_data ADD COLUMN memoryUsedMB INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE performance_data ADD COLUMN memoryTotalMB INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE performance_data ADD COLUMN batteryVoltage REAL NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE performance_data ADD COLUMN batteryIsCharging INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE performance_data ADD COLUMN batteryIsPlugged INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE performance_data ADD COLUMN isScreenOn INTEGER NOT NULL DEFAULT 0")

                    // 2. 为optimization_history表添加新字段
                    database.execSQL("ALTER TABLE optimization_history ADD COLUMN beforeDataId INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE optimization_history ADD COLUMN afterDataId INTEGER NOT NULL DEFAULT 0")

                    // 3. 为battery_stats表添加新字段
                    database.execSQL("ALTER TABLE battery_stats ADD COLUMN isPlugged INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE battery_stats ADD COLUMN screenOnTime INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE battery_stats ADD COLUMN screenOffTime INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE battery_stats ADD COLUMN estimatedLifeHours INTEGER NOT NULL DEFAULT 0")
                    database.execSQL("ALTER TABLE battery_stats ADD COLUMN drainRate REAL NOT NULL DEFAULT 0")

                    // 4. 创建network_usage表
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS network_usage (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            appPackageName TEXT NOT NULL,
                            appName TEXT NOT NULL,
                            rxBytes INTEGER NOT NULL,
                            txBytes INTEGER NOT NULL,
                            rxPackets INTEGER NOT NULL,
                            txPackets INTEGER NOT NULL,
                            isWifi INTEGER NOT NULL,
                            isMobile INTEGER NOT NULL,
                            networkType TEXT NOT NULL,
                            connectionSpeed REAL NOT NULL DEFAULT 0,
                            latency REAL NOT NULL DEFAULT 0
                        )
                    """)

                    // 创建network_usage索引以提高查询性能
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_network_usage_timestamp ON network_usage(timestamp)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_network_usage_appPackageName ON network_usage(appPackageName)")

                    // 5. 创建system_events表
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS system_events (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            eventType TEXT NOT NULL,
                            severity TEXT NOT NULL,
                            category TEXT NOT NULL,
                            title TEXT NOT NULL,
                            description TEXT NOT NULL,
                            affectedComponent TEXT NOT NULL DEFAULT '',
                            metrics TEXT NOT NULL DEFAULT '',
                            stackTrace TEXT NOT NULL DEFAULT '',
                            actionTaken TEXT NOT NULL DEFAULT '',
                            userNotified INTEGER NOT NULL DEFAULT 0,
                            resolved INTEGER NOT NULL DEFAULT 0,
                            resolvedTimestamp INTEGER
                        )
                    """)

                    // 创建system_events索引
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_system_events_timestamp ON system_events(timestamp)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_system_events_eventType ON system_events(eventType)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_system_events_severity ON system_events(severity)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_system_events_resolved ON system_events(resolved)")

                    Log.d(TAG, "Migration from version 1 to 2 completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during migration from version 1 to 2", e)
                    throw e
                }
            }
        }

        /**
         * 数据库回调，用于监控迁移过程和错误处理
         */
        private val databaseCallback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.d(TAG, "Database created for the first time")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Log.d(TAG, "Database opened, version: ${db.version}")
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                Log.e(TAG, "DESTRUCTIVE MIGRATION PERFORMED! Data has been lost!")
                // 这里可以记录到崩溃报告系统
                // CrashReporter.reportDatabaseDestructiveMigration()
            }
        }

        /**
         * 备份数据库文件
         * @param context 应用上下文
         * @return 备份是否成功
         */
        fun backupDatabase(context: Context): Boolean {
            return try {
                val currentDB = context.getDatabasePath(DATABASE_NAME)
                if (currentDB.exists()) {
                    val backupDB = File(context.filesDir, "${DATABASE_NAME}_backup_${System.currentTimeMillis()}")
                    currentDB.copyTo(backupDB, overwrite = true)
                    Log.d(TAG, "Database backed up to: ${backupDB.absolutePath}")

                    // 清理旧备份（保留最近3个）
                    cleanupOldBackups(context)
                    true
                } else {
                    Log.w(TAG, "Database file does not exist, cannot backup")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to backup database", e)
                false
            }
        }

        /**
         * 清理旧的备份文件，只保留最近的3个
         */
        private fun cleanupOldBackups(context: Context) {
            try {
                val backupFiles = context.filesDir.listFiles { file ->
                    file.name.startsWith("${DATABASE_NAME}_backup_")
                }?.sortedByDescending { it.lastModified() }

                backupFiles?.drop(3)?.forEach { oldBackup ->
                    if (oldBackup.delete()) {
                        Log.d(TAG, "Deleted old backup: ${oldBackup.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cleanup old backups", e)
            }
        }

        /**
         * 获取数据库单例实例
         * 使用标准的DCL（Double-Checked Locking）模式
         * 实现了安全的迁移路径，避免数据丢失
         *
         * 1. 第一次检查：避免不必要的同步
         * 2. synchronized块：确保只有一个线程可以创建实例
         * 3. 第二次检查：防止多个线程同时进入synchronized块后重复创建
         */
        fun getDatabase(context: Context): AppDatabase {
            // 第一次检查，避免不必要的同步
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            // 同步块，确保线程安全
            synchronized(this) {
                // 第二次检查，防止多个线程同时进入synchronized块
                val instance = INSTANCE
                if (instance != null) {
                    return instance
                }

                // 在创建数据库前尝试备份（如果已存在）
                if (context.getDatabasePath(DATABASE_NAME).exists()) {
                    backupDatabase(context)
                }

                // 创建新的数据库实例
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)  // 添加迁移路径
                .addCallback(databaseCallback)  // 添加回调监听
                // 移除了 fallbackToDestructiveMigration() 以保护用户数据
                .build()

                // 保存实例并返回
                INSTANCE = newInstance
                return newInstance
            }
        }

        /**
         * 验证数据库迁移的完整性
         * 可以在应用启动时调用，确保迁移成功
         */
        suspend fun validateMigration(context: Context): Boolean {
            return try {
                val db = getDatabase(context)

                // 测试各个DAO是否能正常工作
                db.performanceDataDao().getRecentPerformanceData(1)
                db.optimizationHistoryDao().getRecentOptimizations(1)
                db.batteryStatsDao().getRecentBatteryStats(1)
                db.networkUsageDao().getLatestUsage(1)
                db.systemEventsDao().getLatestEvents(1)

                // 测试浏览器相关DAO
                db.browserHistoryDao().getHistoryCount()
                db.browserTabDao().getTabCount()
                db.browserDownloadDao().getDownloadCount()

                Log.d(TAG, "Database migration validation successful")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Database migration validation failed", e)
                false
            }
        }

        /**
         * 关闭数据库连接（用于测试）
         * 同步方法确保线程安全
         */
        @Synchronized
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

        /**
         * 检查数据库是否已初始化
         * 使用@Volatile确保读取最新值
         */
        fun isInitialized(): Boolean {
            return INSTANCE != null
        }
    }
}