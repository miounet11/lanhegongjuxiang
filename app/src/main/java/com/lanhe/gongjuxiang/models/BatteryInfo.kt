package com.lanhe.gongjuxiang.models

/**
 * 电池信息数据类
 */
data class BatteryInfo(
    val level: Int,                    // 电池电量百分比 (0-100)
    val temperature: Float,           // 电池温度 (°C)
    val voltage: Float,               // 电池电压 (V)
    val current: Float,               // 电池电流 (mA)
    val status: Int,                  // 电池状态
    val health: Int,                  // 电池健康状态
    val technology: String,           // 电池技术
    val capacity: Long,               // 电池容量 (mAh)
    val isCharging: Boolean,          // 是否正在充电
    val chargeType: String,           // 充电类型 (AC/USB/Wireless)
    val timeToFull: Long,             // 充满电所需时间 (分钟)
    val timeToEmpty: Long             // 电量耗尽时间 (分钟)
)
