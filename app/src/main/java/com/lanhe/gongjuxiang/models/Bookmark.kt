package com.lanhe.gongjuxiang.models

/**
 * 书签数据类
 */
data class Bookmark(
    val title: String,
    val url: String,
    val category: String,
    val icon: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
