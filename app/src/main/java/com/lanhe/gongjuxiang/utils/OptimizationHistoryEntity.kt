package com.lanhe.gongjuxiang.utils

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * 优化历史实体类
 * 用于存储系统优化的历史记录
 */
@Entity(tableName = "optimization_history")
data class OptimizationHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val optimizationType: String,
    val success: Boolean,
    val message: String,
    val improvements: String,
    val duration: Long,
    val beforeDataId: Long = 0,
    val afterDataId: Long = 0
) : Serializable
