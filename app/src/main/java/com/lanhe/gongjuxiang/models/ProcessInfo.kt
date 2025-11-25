package com.lanhe.gongjuxiang.models

/**
 * 进程信息数据类
 * 统一的ProcessInfo定义，包含所有需要的字段
 */
data class ProcessInfo(
    val pid: Int,
    val uid: Int,
    val processName: String,
    val packageName: String,      // 添加包名字段
    val importance: Int,
    val memoryUsage: Long         // 添加内存使用字段(bytes)
)