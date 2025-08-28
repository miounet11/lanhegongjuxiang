package com.lanhe.gongjuxiang.models

data class QuickSetting(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val currentValue: String,
    val options: List<String> = emptyList()
)
