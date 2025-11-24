package com.lanhe.gongjuxiang.utils

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

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
    val isPlugged: Boolean = false,
    val screenOnTime: Long = 0,
    val screenOffTime: Long = 0,
    val estimatedLifeHours: Int = 0,
    val drainRate: Float = 0f,
    val healthStatus: String
) : Serializable
