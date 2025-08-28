package com.lanhe.gongjuxiang.utils

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 性能数据DAO接口
 * 提供性能数据的数据库操作方法
 */
@Dao
interface PerformanceDataDao {

    /**
     * 插入性能数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(performanceData: PerformanceDataEntity): Long

    /**
     * 批量插入性能数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(performanceDataList: List<PerformanceDataEntity>): List<Long>

    /**
     * 更新性能数据
     */
    @Update
    suspend fun update(performanceData: PerformanceDataEntity)

    /**
     * 删除性能数据
     */
    @Delete
    suspend fun delete(performanceData: PerformanceDataEntity)

    /**
     * 根据ID删除性能数据
     */
    @Query("DELETE FROM performance_data WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 获取所有性能数据
     */
    @Query("SELECT * FROM performance_data ORDER BY timestamp DESC")
    fun getAllPerformanceData(): Flow<List<PerformanceDataEntity>>

    /**
     * 根据时间范围获取性能数据
     */
    @Query("SELECT * FROM performance_data WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getPerformanceDataByTimeRange(startTime: Long, endTime: Long): Flow<List<PerformanceDataEntity>>

    /**
     * 获取最新的性能数据
     */
    @Query("SELECT * FROM performance_data ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPerformanceData(): PerformanceDataEntity?

    /**
     * 获取最近N条性能数据
     */
    @Query("SELECT * FROM performance_data ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentPerformanceData(limit: Int): List<PerformanceDataEntity>

    /**
     * 根据数据类型获取性能数据
     */
    @Query("SELECT * FROM performance_data WHERE dataType = :dataType ORDER BY timestamp DESC")
    fun getPerformanceDataByType(dataType: String): Flow<List<PerformanceDataEntity>>

    /**
     * 获取指定时间段内的平均CPU使用率
     */
    @Query("SELECT AVG(cpuUsage) FROM performance_data WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageCpuUsage(startTime: Long, endTime: Long): Float?

    /**
     * 获取指定时间段内的平均内存使用率
     */
    @Query("SELECT AVG(memoryUsagePercent) FROM performance_data WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageMemoryUsage(startTime: Long, endTime: Long): Float?

    /**
     * 获取指定时间段内的电池温度统计
     */
    @Query("SELECT MIN(batteryTemperature) as minTemp, MAX(batteryTemperature) as maxTemp, AVG(batteryTemperature) as avgTemp FROM performance_data WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getBatteryTemperatureStats(startTime: Long, endTime: Long): TemperatureStats?

    /**
     * 清理过期数据（保留最近30天的数据）
     */
    @Query("DELETE FROM performance_data WHERE timestamp < :cutoffTime")
    suspend fun cleanupOldData(cutoffTime: Long)

    /**
     * 获取数据总条数
     */
    @Query("SELECT COUNT(*) FROM performance_data")
    suspend fun getDataCount(): Int

    /**
     * 清空所有性能数据
     */
    @Query("DELETE FROM performance_data")
    suspend fun clearAllData()
}

/**
 * 温度统计数据类
 */
data class TemperatureStats(
    val minTemp: Float,
    val maxTemp: Float,
    val avgTemp: Float
)
