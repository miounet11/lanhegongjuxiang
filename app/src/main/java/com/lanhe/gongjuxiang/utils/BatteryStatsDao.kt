package com.lanhe.gongjuxiang.utils

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 电池统计DAO接口
 * 提供电池统计数据的数据库操作方法
 */
@Dao
interface BatteryStatsDao {

    /**
     * 插入电池统计数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batteryStats: BatteryStatsEntity): Long

    /**
     * 批量插入电池统计数据
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(batteryStatsList: List<BatteryStatsEntity>): List<Long>

    /**
     * 更新电池统计数据
     */
    @Update
    suspend fun update(batteryStats: BatteryStatsEntity)

    /**
     * 删除电池统计数据
     */
    @Delete
    suspend fun delete(batteryStats: BatteryStatsEntity)

    /**
     * 根据ID删除电池统计数据
     */
    @Query("DELETE FROM battery_stats WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * 获取所有电池统计数据
     */
    @Query("SELECT * FROM battery_stats ORDER BY timestamp DESC")
    fun getAllBatteryStats(): Flow<List<BatteryStatsEntity>>

    /**
     * 根据时间范围获取电池统计数据
     */
    @Query("SELECT * FROM battery_stats WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getBatteryStatsByTimeRange(startTime: Long, endTime: Long): Flow<List<BatteryStatsEntity>>

    /**
     * 获取最新的电池统计数据
     */
    @Query("SELECT * FROM battery_stats ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBatteryStats(): BatteryStatsEntity?

    /**
     * 获取最近N条电池统计数据
     */
    @Query("SELECT * FROM battery_stats ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentBatteryStats(limit: Int): List<BatteryStatsEntity>

    /**
     * 获取充电时的电池数据
     */
    @Query("SELECT * FROM battery_stats WHERE isCharging = 1 ORDER BY timestamp DESC")
    fun getChargingBatteryStats(): Flow<List<BatteryStatsEntity>>

    /**
     * 获取放电时的电池数据
     */
    @Query("SELECT * FROM battery_stats WHERE isCharging = 0 ORDER BY timestamp DESC")
    fun getDischargingBatteryStats(): Flow<List<BatteryStatsEntity>>

    /**
     * 获取指定时间段内的平均电池温度
     */
    @Query("SELECT AVG(temperature) FROM battery_stats WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getAverageTemperature(startTime: Long, endTime: Long): Float?

    /**
     * 获取电池温度统计
     */
    @Query("SELECT MIN(temperature) as minTemp, MAX(temperature) as maxTemp, AVG(temperature) as avgTemp FROM battery_stats WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTemperatureStats(startTime: Long, endTime: Long): TemperatureStats?

    /**
     * 获取平均电池续航时间
     */
    @Query("SELECT AVG(estimatedLifeHours) FROM battery_stats WHERE estimatedLifeHours > 0")
    suspend fun getAverageBatteryLife(): Float?

    /**
     * 获取电池消耗率统计
     */
    @Query("SELECT AVG(drainRate) as avgDrainRate, MIN(drainRate) as minDrainRate, MAX(drainRate) as maxDrainRate FROM battery_stats WHERE drainRate > 0")
    suspend fun getDrainRateStats(): DrainRateStats?

    /**
     * 获取屏幕使用时间统计
     */
    @Query("SELECT SUM(screenOnTime) as totalScreenOnTime, SUM(screenOffTime) as totalScreenOffTime FROM battery_stats WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getScreenTimeStats(startTime: Long, endTime: Long): ScreenTimeStats?

    /**
     * 获取电池健康状态分布
     */
    @Query("SELECT healthStatus, COUNT(*) as count FROM battery_stats GROUP BY healthStatus")
    suspend fun getBatteryHealthDistribution(): List<HealthStatusCount>

    /**
     * 清理过期电池统计数据（保留最近30天的数据）
     */
    @Query("DELETE FROM battery_stats WHERE timestamp < :cutoffTime")
    suspend fun cleanupOldData(cutoffTime: Long)

    /**
     * 获取电池统计数据总条数
     */
    @Query("SELECT COUNT(*) FROM battery_stats")
    suspend fun getStatsCount(): Int

    /**
     * 清空所有电池统计数据
     */
    @Query("DELETE FROM battery_stats")
    suspend fun clearAllStats()

    /**
     * 获取最新统计 - Flow版本（用于Repository）
     */
    @Query("SELECT * FROM battery_stats ORDER BY timestamp DESC LIMIT 1")
    fun getLatestStats(): Flow<BatteryStatsEntity?>

    /**
     * 获取时间范围内的统计 - Flow版本（用于Repository）
     */
    @Query("SELECT * FROM battery_stats WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getStatsBetween(startTime: Long, endTime: Long): Flow<List<BatteryStatsEntity>>

    /**
     * 获取电池健康趋势
     */
    @Query("SELECT batteryLevel as healthPercentage, timestamp FROM battery_stats ORDER BY timestamp DESC LIMIT 100")
    fun getHealthTrend(): Flow<List<BatteryHealthTrend>>
}

/**
 * 电池健康趋势数据类
 */
data class BatteryHealthTrend(
    val timestamp: Long,
    val healthPercentage: Float
)

/**
 * 电池消耗率统计数据类
 */
data class DrainRateStats(
    val avgDrainRate: Float,
    val minDrainRate: Float,
    val maxDrainRate: Float
)

/**
 * 屏幕使用时间统计数据类
 */
data class ScreenTimeStats(
    val totalScreenOnTime: Long,
    val totalScreenOffTime: Long
) {
    val screenOnRatio: Float
        get() {
            val totalTime = totalScreenOnTime + totalScreenOffTime
            return if (totalTime > 0) (totalScreenOnTime.toFloat() / totalTime.toFloat()) * 100f else 0f
        }
}

/**
 * 电池健康状态统计数据类
 */
data class HealthStatusCount(
    val healthStatus: String,
    val count: Int
)