package com.lanhe.gongjuxiang.utils

/**
 * 网络信息数据类
 */
data class NetworkInfo(
    val isConnected: Boolean = false,
    val networkType: String = "Unknown",
    val signalStrength: Int = 0,
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val ipAddress: String = "",
    val gateway: String = "",
    val dns: String = "",
    val ssid: String = ""
)