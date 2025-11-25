package com.lanhe.gongjuxiang.utils

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 网络使用数据访问对象
 */
@Dao
interface NetworkUsageDao {
    @Query("SELECT * FROM network_usage ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestUsage(limit: Int = 100): Flow<List<NetworkUsageEntity>>

    @Query("SELECT * FROM network_usage WHERE appPackageName = :packageName ORDER BY timestamp DESC LIMIT :limit")
    fun getUsageByApp(packageName: String, limit: Int = 100): Flow<List<NetworkUsageEntity>>

    @Query("SELECT * FROM network_usage WHERE timestamp > :startTime AND timestamp < :endTime ORDER BY timestamp DESC")
    fun getUsageInTimeRange(startTime: Long, endTime: Long): Flow<List<NetworkUsageEntity>>

    @Query("SELECT appPackageName, appName, SUM(rxBytes) as totalRx, SUM(txBytes) as totalTx FROM network_usage WHERE timestamp > :startTime GROUP BY appPackageName ORDER BY totalRx + totalTx DESC")
    suspend fun getTopAppsByUsage(startTime: Long): List<AppNetworkSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: NetworkUsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usages: List<NetworkUsageEntity>)

    @Delete
    suspend fun delete(usage: NetworkUsageEntity)

    @Query("DELETE FROM network_usage WHERE timestamp < :timestamp")
    suspend fun deleteOldData(timestamp: Long)

    @Query("DELETE FROM network_usage")
    suspend fun clearAll()
}

/**
 * 应用网络使用汇总
 */
data class AppNetworkSummary(
    val appPackageName: String,
    val appName: String,
    val totalRx: Long,
    val totalTx: Long
)