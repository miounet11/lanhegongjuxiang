package com.lanhe.gongjuxiang.utils

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * 应用数据库
 * 使用Room数据库存储性能监控数据和优化历史
 */
@Database(
    entities = [
        PerformanceDataEntity::class,
        OptimizationHistoryEntity::class,
        BatteryStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun performanceDataDao(): PerformanceDataDao
    abstract fun optimizationHistoryDao(): OptimizationHistoryDao
    abstract fun batteryStatsDao(): BatteryStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lanhe_gongjuxiang_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
