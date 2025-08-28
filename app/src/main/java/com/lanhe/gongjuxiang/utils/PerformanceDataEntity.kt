package com.lanhe.gongjuxiang.utils

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * 性能数据实体类
 * 用于存储性能监控的历史数据
 */
@Entity(tableName = "performance_data")
data class PerformanceDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val cpuUsage: Float,
    val memoryUsagePercent: Int,
    val memoryUsedMB: Long,
    val memoryTotalMB: Long,
    val batteryLevel: Int,
    val batteryTemperature: Float,
    val batteryVoltage: Float,
    val batteryIsCharging: Boolean,
    val batteryIsPlugged: Boolean,
    val deviceTemperature: Float,
    val isScreenOn: Boolean,
    val dataType: String = "performance" // 数据类型：performance, optimization_before, optimization_after
) : Serializable

/**
 * 优化历史实体类
 * 用于存储系统优化的历史记录
 */
@Entity(tableName = "optimization_history")
data class OptimizationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val optimizationType: String, // 优化类型：full, battery, memory, cpu, network, system
    val success: Boolean,
    val message: String,
    val beforeDataId: Long? = null, // 优化前的数据ID
    val afterDataId: Long? = null,  // 优化后的数据ID
    val improvements: String, // 改进内容，JSON格式
    val duration: Long // 优化耗时
) : Serializable

/**
 * 电池统计实体类
 * 用于存储电池使用统计数据
 */
@Entity(tableName = "battery_stats")
data class BatteryStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val batteryLevel: Int,
    val temperature: Float,
    val voltage: Float,
    val isCharging: Boolean,
    val isPlugged: Boolean,
    val screenOnTime: Long,
    val screenOffTime: Long,
    val estimatedLifeHours: Int,
    val drainRate: Float,
    val healthStatus: String
) : Serializable
