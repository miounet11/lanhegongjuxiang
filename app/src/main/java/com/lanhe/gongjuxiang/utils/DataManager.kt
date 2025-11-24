package com.lanhe.gongjuxiang.utils

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.TimeUnit
import com.lanhe.gongjuxiang.models.PerformanceData
import com.lanhe.gongjuxiang.models.MemoryInfo
import com.lanhe.gongjuxiang.models.BatteryInfo

/**
 * 数据管理器
 * 统一管理性能数据、优化历史和电池统计的存储和查询
 */
class DataManager(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val performanceDataDao = database.performanceDataDao()
    private val optimizationHistoryDao = database.optimizationHistoryDao()
    private val batteryStatsDao = database.batteryStatsDao()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * 保存性能数据
     */
    suspend fun savePerformanceData(data: PerformanceData, dataType: String = "performance"): Long {
        return withContext(Dispatchers.IO) {
            val entity = PerformanceDataEntity(
                timestamp = data.timestamp,
                cpuUsage = data.cpuUsage,
                memoryUsagePercent = (data.memoryUsage.used.toFloat() / data.memoryUsage.total * 100).toInt(),
                memoryUsedMB = data.memoryUsage.used / (1024 * 1024),
                memoryTotalMB = data.memoryUsage.total / (1024 * 1024),
                batteryLevel = data.batteryInfo.level,
                batteryTemperature = data.batteryInfo.temperature.toFloat(),
                batteryVoltage = 0f, // 暂时不支持电池电压
                batteryIsCharging = data.batteryInfo.isCharging,
                batteryIsPlugged = false, // 暂时不支持充电状态
                deviceTemperature = data.deviceTemperature,
                isScreenOn = false, // 暂时不支持屏幕状态
                dataType = dataType
            )
            performanceDataDao.insert(entity)
        }
    }

    /**
     * 批量保存性能数据
     */
    suspend fun savePerformanceDataBatch(dataList: List<PerformanceData>, dataType: String = "performance"): List<Long> {
        return withContext(Dispatchers.IO) {
            val entities = dataList.map { data ->
                PerformanceDataEntity(
                    timestamp = data.timestamp,
                    cpuUsage = data.cpuUsage,
                    memoryUsagePercent = (data.memoryUsage.used.toFloat() / data.memoryUsage.total * 100).toInt(),
                    memoryUsedMB = data.memoryUsage.used / (1024 * 1024),
                    memoryTotalMB = data.memoryUsage.total / (1024 * 1024),
                    batteryLevel = data.batteryInfo.level,
                    batteryTemperature = data.batteryInfo.temperature.toFloat(),
                    batteryVoltage = 0f,
                    batteryIsCharging = data.batteryInfo.isCharging,
                    batteryIsPlugged = false,
                    deviceTemperature = data.deviceTemperature,
                    isScreenOn = false,
                    dataType = dataType
                )
            }
            performanceDataDao.insertAll(entities)
        }
    }

    /**
     * 保存优化历史记录
     */
    suspend fun saveOptimizationHistory(
        type: String,
        success: Boolean,
        message: String,
        beforeDataId: Long? = null,
        afterDataId: Long? = null,
        improvements: List<String> = emptyList(),
        duration: Long
    ): Long {
        return withContext(Dispatchers.IO) {
            val entity = OptimizationHistoryEntity(
                timestamp = System.currentTimeMillis(),
                optimizationType = type,
                success = success,
                message = message,
                beforeDataId = beforeDataId ?: 0,
                afterDataId = afterDataId ?: 0,
                improvements = improvements.joinToString(","),
                duration = duration
            )
            optimizationHistoryDao.insert(entity)
        }
    }

    /**
     * 保存电池统计数据
     */
    suspend fun saveBatteryStats(
        batteryLevel: Int,
        temperature: Float,
        voltage: Float,
        isCharging: Boolean,
        isPlugged: Boolean,
        screenOnTime: Long,
        screenOffTime: Long,
        estimatedLifeHours: Int,
        drainRate: Float,
        healthStatus: String
    ): Long {
        return withContext(Dispatchers.IO) {
            val entity = BatteryStatsEntity(
                timestamp = System.currentTimeMillis(),
                batteryLevel = batteryLevel,
                temperature = temperature,
                voltage = voltage,
                isCharging = isCharging,
                isPlugged = isPlugged,
                screenOnTime = screenOnTime,
                screenOffTime = screenOffTime,
                estimatedLifeHours = estimatedLifeHours,
                drainRate = drainRate,
                healthStatus = healthStatus
            )
            batteryStatsDao.insert(entity)
        }
    }

    /**
     * 获取性能数据流
     */
    fun getPerformanceDataFlow(): Flow<List<PerformanceDataEntity>> {
        return performanceDataDao.getAllPerformanceData().flowOn(Dispatchers.IO)
    }

    /**
     * 获取指定时间范围的性能数据
     */
    fun getPerformanceDataByTimeRange(startTime: Long, endTime: Long): Flow<List<PerformanceDataEntity>> {
        return performanceDataDao.getPerformanceDataByTimeRange(startTime, endTime).flowOn(Dispatchers.IO)
    }

    /**
     * 获取优化历史流
     */
    fun getOptimizationHistoryFlow(): Flow<List<OptimizationHistoryEntity>> {
        return optimizationHistoryDao.getAllOptimizationHistory().flowOn(Dispatchers.IO)
    }

    /**
     * 获取电池统计数据流
     */
    fun getBatteryStatsFlow(): Flow<List<BatteryStatsEntity>> {
        return batteryStatsDao.getAllBatteryStats().flowOn(Dispatchers.IO)
    }

    /**
     * 获取最近的性能数据
     */
    suspend fun getRecentPerformanceData(limit: Int = 100): List<PerformanceDataEntity> {
        return withContext(Dispatchers.IO) {
            performanceDataDao.getRecentPerformanceData(limit)
        }
    }

    /**
     * 获取最近的优化历史
     */
    suspend fun getRecentOptimizationHistory(limit: Int = 50): List<OptimizationHistoryEntity> {
        return withContext(Dispatchers.IO) {
            optimizationHistoryDao.getRecentOptimizations(limit)
        }
    }

    /**
     * 获取性能统计信息
     */
    suspend fun getPerformanceStatistics(hours: Int = 24): PerformanceStatistics {
        return withContext(Dispatchers.IO) {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.HOURS.toMillis(hours.toLong())

            val avgCpuUsage = performanceDataDao.getAverageCpuUsage(startTime, endTime) ?: 0f
            val avgMemoryUsage = performanceDataDao.getAverageMemoryUsage(startTime, endTime) ?: 0f
            val tempStats = performanceDataDao.getBatteryTemperatureStats(startTime, endTime)

            PerformanceStatistics(
                averageCpuUsage = avgCpuUsage,
                averageMemoryUsage = avgMemoryUsage,
                minTemperature = tempStats?.minTemp ?: 0f,
                maxTemperature = tempStats?.maxTemp ?: 0f,
                avgTemperature = tempStats?.avgTemp ?: 0f,
                dataPoints = 0, // 可以后续计算
                timeRangeHours = hours
            )
        }
    }

    /**
     * 获取优化统计信息
     */
    suspend fun getOptimizationStatistics(): OptimizationStatistics {
        return withContext(Dispatchers.IO) {
            val successStats = optimizationHistoryDao.getOptimizationSuccessStats()
            val avgDuration = optimizationHistoryDao.getAverageOptimizationDuration() ?: 0L
            val totalCount = optimizationHistoryDao.getHistoryCount()

            OptimizationStatistics(
                totalOptimizations = totalCount,
                successRate = if (successStats.isNotEmpty()) {
                    val total = successStats.sumOf { it.total }
                    val successful = successStats.sumOf { it.successful }
                    if (total > 0) (successful.toFloat() / total.toFloat()) * 100f else 0f
                } else 0f,
                averageDuration = avgDuration,
                optimizationsByType = successStats
            )
        }
    }

    /**
     * 获取电池统计信息
     */
    suspend fun getBatteryStatistics(hours: Int = 24): BatteryStatistics {
        return withContext(Dispatchers.IO) {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.HOURS.toMillis(hours.toLong())

            val avgTemperature = batteryStatsDao.getAverageTemperature(startTime, endTime) ?: 0f
            val avgBatteryLife = batteryStatsDao.getAverageBatteryLife() ?: 0f
            val drainRateStats = batteryStatsDao.getDrainRateStats()
            val screenTimeStats = batteryStatsDao.getScreenTimeStats(startTime, endTime)
            val healthDistribution = batteryStatsDao.getBatteryHealthDistribution()

            BatteryStatistics(
                averageTemperature = avgTemperature,
                averageBatteryLife = avgBatteryLife,
                avgDrainRate = drainRateStats?.avgDrainRate ?: 0f,
                minDrainRate = drainRateStats?.minDrainRate ?: 0f,
                maxDrainRate = drainRateStats?.maxDrainRate ?: 0f,
                screenOnRatio = screenTimeStats?.screenOnRatio ?: 0f,
                healthStatusDistribution = healthDistribution,
                timeRangeHours = hours
            )
        }
    }

    /**
     * 清理过期数据
     */
    suspend fun cleanupOldData() {
        withContext(Dispatchers.IO) {
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            val ninetyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)

            performanceDataDao.cleanupOldData(thirtyDaysAgo)
            batteryStatsDao.cleanupOldData(thirtyDaysAgo)
            optimizationHistoryDao.cleanupOldData(ninetyDaysAgo)
        }
    }

    /**
     * 清空所有数据
     */
    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            performanceDataDao.clearAllData()
            optimizationHistoryDao.clearAllHistory()
            batteryStatsDao.clearAllStats()
        }
    }

    /**
     * 获取数据库统计信息
     */
    suspend fun getDatabaseStats(): DatabaseStats {
        return withContext(Dispatchers.IO) {
            val performanceCount = performanceDataDao.getDataCount()
            val optimizationCount = optimizationHistoryDao.getHistoryCount()
            val batteryCount = batteryStatsDao.getStatsCount()

            DatabaseStats(
                performanceDataCount = performanceCount,
                optimizationHistoryCount = optimizationCount,
                batteryStatsCount = batteryCount,
                totalRecords = performanceCount + optimizationCount + batteryCount
            )
        }
    }

    /**
     * 自动清理任务（建议定期调用）
     */
    fun scheduleCleanup() {
        scope.launch {
            while (isActive) {
                delay(TimeUnit.HOURS.toMillis(24)) // 每天清理一次
                cleanupOldData()
            }
        }
    }

    /**
     * 关闭数据管理器
     */
    fun close() {
        scope.cancel()
    }
}

/**
 * 性能统计数据类
 */
data class PerformanceStatistics(
    val averageCpuUsage: Float = 0f,
    val averageMemoryUsage: Float = 0f,
    val minTemperature: Float = 0f,
    val maxTemperature: Float = 0f,
    val avgTemperature: Float = 0f,
    val dataPoints: Int = 0,
    val timeRangeHours: Int = 24
)

/**
 * 优化统计数据类
 */
data class OptimizationStatistics(
    val totalOptimizations: Int = 0,
    val successRate: Float = 0f,
    val averageDuration: Long = 0L,
    val optimizationsByType: List<OptimizationStats> = emptyList()
)

/**
 * 电池统计数据类
 */
data class BatteryStatistics(
    val averageTemperature: Float = 0f,
    val averageBatteryLife: Float = 0f,
    val avgDrainRate: Float = 0f,
    val minDrainRate: Float = 0f,
    val maxDrainRate: Float = 0f,
    val screenOnRatio: Float = 0f,
    val healthStatusDistribution: List<HealthStatusCount> = emptyList(),
    val timeRangeHours: Int = 24
)

/**
 * 数据库统计数据类
 */
data class DatabaseStats(
    val performanceDataCount: Int = 0,
    val optimizationHistoryCount: Int = 0,
    val batteryStatsCount: Int = 0,
    val totalRecords: Int = 0
)
