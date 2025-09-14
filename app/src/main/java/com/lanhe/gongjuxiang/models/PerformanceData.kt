package com.lanhe.gongjuxiang.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 性能数据实体类
 * 用于存储性能监控的历史数据
 */
@Entity(tableName = "performance_data")
data class PerformanceData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 时间戳
    val timestamp: Long,

    // CPU使用率
    val cpuUsage: Float,

    // 内存使用情况
    val memoryUsage: MemoryInfo,
    val totalMemory: Long,
    val availableMemory: Long,

    // 存储使用情况
    val storageUsage: Float,
    val totalStorage: Long,
    val availableStorage: Long,

    // 电池信息
    val batteryInfo: BatteryInfo,
    val batteryLevel: Int,
    val batteryTemperature: Float,
    val isCharging: Boolean,

    // 网络信息
    val networkType: String,
    val wifiSignalStrength: Int,
    val mobileSignalStrength: Int,

    // 设备温度
    val deviceTemperature: Float
)
