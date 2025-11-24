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
    val memoryUsedMB: Long = 0,
    val memoryTotalMB: Long = 0,
    val batteryLevel: Int,
    val batteryTemperature: Float = 0f,
    val batteryVoltage: Float = 0f,
    val batteryIsCharging: Boolean = false,
    val batteryIsPlugged: Boolean = false,
    val deviceTemperature: Float,
    val isScreenOn: Boolean = false,
    val dataType: String = "performance"
) : Serializable
