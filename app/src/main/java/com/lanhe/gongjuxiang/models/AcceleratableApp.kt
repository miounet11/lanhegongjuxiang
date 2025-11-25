package com.lanhe.gongjuxiang.models

/**
 * 可加速应用数据类
 */
data class AcceleratableApp(
    val packageName: String,
    val appName: String,
    val isGame: Boolean = false,
    val currentPerformance: Int = 0,
    val potentialBoost: String = "0%",
    val iconResId: Int = 0,
    val isAccelerated: Boolean = false
)