package com.lanhe.gongjuxiang.utils

/**
 * 系统信息数据类
 */
data class SystemInfo(
    val kernelVersion: String = "Unknown",
    val uptime: Long = 0L,
    val cpuCores: Int = 0,
    val totalMemory: Long = 0L,
    val batteryLevel: Int = 0,
    val androidVersion: String = android.os.Build.VERSION.RELEASE,
    val deviceModel: String = android.os.Build.MODEL,
    val manufacturer: String = android.os.Build.MANUFACTURER
)