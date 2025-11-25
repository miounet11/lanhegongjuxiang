package com.lanhe.gongjuxiang.utils

/**
 * 性能指标数据类
 */
data class PerformanceMetrics(
    val cpuUsage: Float = 0f,
    val memoryUsed: Long = 0L,
    val networkLatency: Int = 0,
    val imageLoadTime: Long = 0L,
    val diskUsage: Float = 0f,
    val batteryTemperature: Float = 0f,
    val fps: Int = 60,
    val responseTime: Long = 0L
)