package com.lanhe.gongjuxiang.utils

/**
 * Shizuku相关数据类定义
 * 存放所有Shizuku功能使用的数据类
 */

/**
 * 进程信息
 */
data class ProcessInfo(
    val pid: Int,
    val processName: String,
    val packageName: String,
    val uid: Int,
    val memoryUsage: Long
)

/**
 * 系统信息类
 */
data class SystemInfo(
    val kernelVersion: String = "Unknown",
    val uptime: Long = 0L,
    val cpuCores: Int = 0,
    val totalMemory: Long = 0L,
    val availableMemory: Long = 0L,
    val batteryLevel: Int = 0,
    val deviceBrand: String = "Unknown",
    val deviceModel: String = "Unknown",
    val androidVersion: String = "Unknown",
    val performanceBoost: String = "0-30%",
    val batteryOptimization: String = "+10-15%"
)

/**
 * 网络信息类
 */
data class NetworkInfo(
    val type: String = "Unknown",
    val downloadSpeed: Double = 0.0,
    val uploadSpeed: Double = 0.0,
    val latency: Long = 0L,
    val signalStrength: Int = 0,
    val isConnected: Boolean = false
)

/**
 * 性能指标类
 */
data class PerformanceMetrics(
    val cpuUsage: Float = 0f,
    val memoryUsed: Long = 0L,
    val networkLatency: Long = 0L,
    val imageLoadTime: Double = 0.0,
    val networkEfficiency: Float = 0f,
    val batteryEfficiency: Float = 0f,
    val cacheSize: Long = 0L,
    val uptime: Long = 0L
)

/**
 * 可加速应用类
 */
data class AcceleratableApp(
    val name: String,
    val packageName: String,
    val icon: String = "",
    val latencyReduction: Long = 0L,
    val speedIncrease: Double = 0.0,
    val isAccelerated: Boolean = false
)

/**
 * 性能提升结果类
 */
data class PerformanceBoostResult(
    val success: Boolean,
    val performanceIncrease: String = "",
    val batteryImpact: String = "",
    val message: String = ""
)

/**
 * 电池优化结果类
 */
data class BatteryOptimizationResult(
    val success: Boolean,
    val batteryLifeIncrease: String = "",
    val performanceImpact: String = "",
    val message: String = ""
)

/**
 * Shizuku状态枚举
 */
enum class ShizukuState {
    Unavailable,  // Shizuku不可用
    Denied,       // 权限被拒绝
    Granted       // 权限已授予
}
