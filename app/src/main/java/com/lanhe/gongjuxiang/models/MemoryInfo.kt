package com.lanhe.gongjuxiang.models

/**
 * 内存信息数据类
 */
data class MemoryInfo(
    val total: Long,         // 总内存 (bytes)
    val available: Long,     // 可用内存 (bytes)
    val used: Long,          // 已用内存 (bytes)
    val usagePercent: Float  // 使用率百分比 (0-100)
)
