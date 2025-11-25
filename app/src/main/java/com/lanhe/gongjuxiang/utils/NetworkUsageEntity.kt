package com.lanhe.gongjuxiang.utils

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * 网络使用实体类
 * 用于存储应用程序的网络使用统计数据
 */
@Entity(tableName = "network_usage")
data class NetworkUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val appPackageName: String,
    val appName: String,
    val rxBytes: Long,  // 接收字节数
    val txBytes: Long,  // 发送字节数
    val rxPackets: Long,  // 接收数据包数
    val txPackets: Long,  // 发送数据包数
    val isWifi: Boolean,
    val isMobile: Boolean,
    val networkType: String,  // WIFI, 4G, 5G等
    val connectionSpeed: Float = 0f,  // Mbps
    val latency: Float = 0f  // ms
) : Serializable