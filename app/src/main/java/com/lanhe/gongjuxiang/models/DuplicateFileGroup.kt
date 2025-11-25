package com.lanhe.gongjuxiang.models

import java.io.File

/**
 * 重复文件组数据类
 * 用于存储具有相同内容的文件组
 */
data class DuplicateFileGroup(
    val hash: String,              // 文件内容的哈希值
    val files: List<File>,         // 具有相同内容的文件列表
    val totalSize: Long,           // 文件组的总大小
    val wastedSpace: Long = 0L     // 浪费的空间（重复文件占用的空间）
)

/**
 * 重复文件扫描结果
 */
data class DuplicateFileResult(
    val duplicateGroups: List<DuplicateFileGroup> = emptyList(),
    val totalDuplicateSize: Long = 0L,
    val removedSize: Long = 0L,
    val message: String = ""
)