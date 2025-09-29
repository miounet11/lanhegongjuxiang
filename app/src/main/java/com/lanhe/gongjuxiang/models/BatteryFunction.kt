package com.lanhe.gongjuxiang.models

data class BatteryFunction(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val isEnabled: Boolean,
    var currentValue: String
)
