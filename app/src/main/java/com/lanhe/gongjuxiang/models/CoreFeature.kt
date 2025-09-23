package com.lanhe.gongjuxiang.models

data class CoreFeature(
    val id: String,
    val title: String,
    val description: String,
    val icon: Int,
    val category: String,
    val isEnabled: Boolean = true,
    val badgeCount: Int = 0
)
