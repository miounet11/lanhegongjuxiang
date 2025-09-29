package com.lanhe.gongjuxiang.models

data class SecurityFeature(
    val id: String,
    val title: String,
    val description: String,
    val icon: Int,
    val category: String,
    var status: String
)
