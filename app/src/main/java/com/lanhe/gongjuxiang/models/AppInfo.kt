package com.lanhe.gongjuxiang.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val size: Long, // 应用大小（字节）
    val installTime: Long,
    val lastUpdateTime: Long
)
