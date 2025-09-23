package com.lanhe.gongjuxiang.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 性能数据实体类
 * 用于存储性能监控的历史数据
 */
data class PerformanceData(
    val timestamp: Long,
    val cpuUsage: Float,
    val memoryUsage: MemoryInfo,
    val storageUsage: Float,
    val batteryInfo: BatteryInfo,
    val networkType: String = "",
    val deviceTemperature: Float = 0f
)
