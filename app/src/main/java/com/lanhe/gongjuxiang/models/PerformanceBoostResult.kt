package com.lanhe.gongjuxiang.models

/**
 * 性能提升结果数据类
 */
data class PerformanceBoostResult(
    val isSuccess: Boolean = false,
    val improvement: String = "0%",
    val errorMessage: String = "",
    val memoryFreed: Long = 0L,
    val cpuOptimized: Boolean = false,
    val batteryOptimized: Boolean = false,
    val networkOptimized: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)