package com.lanhe.gongjuxiang.models

data class ElectromagneticFunction(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val isEnabled: Boolean,
    val currentValue: String
)
