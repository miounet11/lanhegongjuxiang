package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 数据库迁移辅助工具
 * 提供数据库迁移相关的辅助功能
 */
object DatabaseMigrationHelper {
    private const val TAG = "DatabaseMigrationHelper"

    /**
     * 执行迁移前的准备工作
     */
    suspend fun prepareMigration(context: Context) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Preparing for database migration...")

            // 1. 备份当前数据库
            val backupSuccess = AppDatabase.backupDatabase(context)
            if (!backupSuccess) {
                Log.w(TAG, "Database backup failed, but continuing with migration")
            }

            // 2. 清理临时文件
            cleanupTempFiles(context)

            Log.d(TAG, "Migration preparation completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing migration", e)
        }
    }

    /**
     * 验证迁移后的数据完整性
     */
    suspend fun verifyMigration(context: Context): MigrationVerificationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Verifying database migration...")

            val db = AppDatabase.getDatabase(context)
            val results = mutableListOf<String>()
            var success = true

            // 验证各个表的存在性和数据
            try {
                val performanceData = db.performanceDataDao().getLatestData().first()
                val recordCount = if (performanceData != null) 1 else 0
                results.add("PerformanceData表验证成功，记录数: $recordCount")
            } catch (e: Exception) {
                success = false
                results.add("PerformanceData表验证失败: ${e.message}")
            }

            try {
                val optimizationHistory = db.optimizationHistoryDao().getLatestHistory()
                val recordCount = if (optimizationHistory != null) 1 else 0
                results.add("OptimizationHistory表验证成功，记录数: $recordCount")
            } catch (e: Exception) {
                success = false
                results.add("OptimizationHistory表验证失败: ${e.message}")
            }

            try {
                val batteryStats = db.batteryStatsDao().getLatestStats().first()
                val recordCount = if (batteryStats != null) 1 else 0
                results.add("BatteryStats表验证成功，记录数: $recordCount")
            } catch (e: Exception) {
                success = false
                results.add("BatteryStats表验证失败: ${e.message}")
            }

            // 验证新表
            try {
                db.networkUsageDao().getLatestUsage(1)
                results.add("NetworkUsage表（新）验证成功")
            } catch (e: Exception) {
                success = false
                results.add("NetworkUsage表验证失败: ${e.message}")
            }

            try {
                db.systemEventsDao().getLatestEvents(1)
                results.add("SystemEvents表（新）验证成功")
            } catch (e: Exception) {
                success = false
                results.add("SystemEvents表验证失败: ${e.message}")
            }

            Log.d(TAG, "Migration verification completed. Success: $success")
            MigrationVerificationResult(success, results)
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying migration", e)
            MigrationVerificationResult(
                false,
                listOf("迁移验证失败: ${e.message}")
            )
        }
    }

    /**
     * 清理临时文件
     */
    private fun cleanupTempFiles(context: Context) {
        try {
            val tempFiles = context.cacheDir.listFiles { file ->
                file.name.endsWith(".tmp") || file.name.endsWith(".temp")
            }
            tempFiles?.forEach { file ->
                if (file.delete()) {
                    Log.d(TAG, "Deleted temp file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up temp files", e)
        }
    }

    /**
     * 获取数据库版本信息
     */
    suspend fun getDatabaseVersion(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            db.openHelper.readableDatabase.version
        } catch (e: Exception) {
            Log.e(TAG, "Error getting database version", e)
            -1
        }
    }

    /**
     * 检查是否需要迁移
     */
    fun needsMigration(currentVersion: Int, targetVersion: Int): Boolean {
        return currentVersion < targetVersion
    }

    /**
     * 生成迁移报告
     */
    suspend fun generateMigrationReport(context: Context): String = withContext(Dispatchers.IO) {
        val builder = StringBuilder()
        builder.appendLine("=== 数据库迁移报告 ===")
        builder.appendLine("时间: ${System.currentTimeMillis()}")

        val currentVersion = getDatabaseVersion(context)
        builder.appendLine("当前版本: $currentVersion")
        builder.appendLine("目标版本: 2")

        if (needsMigration(currentVersion, 2)) {
            builder.appendLine("需要迁移: 是")
            builder.appendLine("\n迁移内容:")
            builder.appendLine("1. performance_data表新增6个字段")
            builder.appendLine("2. optimization_history表新增2个字段")
            builder.appendLine("3. battery_stats表新增5个字段")
            builder.appendLine("4. 新增network_usage表")
            builder.appendLine("5. 新增system_events表")
        } else {
            builder.appendLine("需要迁移: 否（已是最新版本）")
        }

        val verificationResult = verifyMigration(context)
        builder.appendLine("\n验证结果: ${if (verificationResult.success) "成功" else "失败"}")
        verificationResult.details.forEach { detail ->
            builder.appendLine("  - $detail")
        }

        builder.toString()
    }
}

/**
 * 迁移验证结果
 */
data class MigrationVerificationResult(
    val success: Boolean,
    val details: List<String>
)