package com.lanhe.gongjuxiang.utils

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 优化历史DAO接口
 * 提供优化历史记录的数据库操作方法
 */
@Dao
interface OptimizationHistoryDao {

    /**
     * 插入优化历史记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(optimizationHistory: OptimizationHistoryEntity): Long

    /**
     * 批量插入优化历史记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(optimizationHistoryList: List<OptimizationHistoryEntity>): List<Long>

    /**
     * 更新优化历史记录
     */
    @Update
    suspend fun update(optimizationHistory: OptimizationHistoryEntity)

    /**
     * 删除优化历史记录
     */
    @Delete
    suspend fun delete(optimizationHistory: OptimizationHistoryEntity)

    /**
     * 根据ID删除优化历史记录
     */
    @Query("DELETE FROM optimization_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 获取所有优化历史记录
     */
    @Query("SELECT * FROM optimization_history ORDER BY timestamp DESC")
    fun getAllOptimizationHistory(): Flow<List<OptimizationHistoryEntity>>

    /**
     * 根据时间范围获取优化历史记录
     */
    @Query("SELECT * FROM optimization_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getOptimizationHistoryByTimeRange(startTime: Long, endTime: Long): Flow<List<OptimizationHistoryEntity>>

    /**
     * 根据优化类型获取历史记录
     */
    @Query("SELECT * FROM optimization_history WHERE optimizationType = :type ORDER BY timestamp DESC")
    fun getOptimizationHistoryByType(type: String): Flow<List<OptimizationHistoryEntity>>

    /**
     * 获取成功的优化记录
     */
    @Query("SELECT * FROM optimization_history WHERE success = 1 ORDER BY timestamp DESC")
    fun getSuccessfulOptimizations(): Flow<List<OptimizationHistoryEntity>>

    /**
     * 获取失败的优化记录
     */
    @Query("SELECT * FROM optimization_history WHERE success = 0 ORDER BY timestamp DESC")
    fun getFailedOptimizations(): Flow<List<OptimizationHistoryEntity>>

    /**
     * 获取最新的优化记录
     */
    @Query("SELECT * FROM optimization_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestOptimization(): OptimizationHistoryEntity?

    /**
     * 获取最近N条优化记录
     */
    @Query("SELECT * FROM optimization_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentOptimizations(limit: Int): List<OptimizationHistoryEntity>

    /**
     * 获取优化成功率统计
     */
    @Query("SELECT optimizationType, COUNT(*) as total, SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful FROM optimization_history GROUP BY optimizationType")
    suspend fun getOptimizationSuccessStats(): List<OptimizationStats>

    /**
     * 获取平均优化耗时
     */
    @Query("SELECT AVG(duration) FROM optimization_history WHERE success = 1")
    suspend fun getAverageOptimizationDuration(): Long?

    /**
     * 清理过期优化记录（保留最近90天的记录）
     */
    @Query("DELETE FROM optimization_history WHERE timestamp < :cutoffTime")
    suspend fun cleanupOldData(cutoffTime: Long)

    /**
     * 获取优化记录总条数
     */
    @Query("SELECT COUNT(*) FROM optimization_history")
    suspend fun getHistoryCount(): Int

    /**
     * 清空所有优化历史记录
     */
    @Query("DELETE FROM optimization_history")
    suspend fun clearAllHistory()
}

/**
 * 优化统计数据类
 */
data class OptimizationStats(
    val optimizationType: String,
    val total: Int,
    val successful: Int
) {
    val successRate: Float
        get() = if (total > 0) (successful.toFloat() / total.toFloat()) * 100f else 0f
}
