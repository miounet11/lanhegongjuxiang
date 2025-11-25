package com.lanhe.gongjuxiang.utils

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * 系统事件实体类
 * 用于存储系统事件和性能异常记录
 */
@Entity(tableName = "system_events")
data class SystemEventsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val eventType: String,  // PERFORMANCE_ANOMALY, CRASH, FREEZE, HIGH_MEMORY, HIGH_CPU等
    val severity: String,   // LOW, MEDIUM, HIGH, CRITICAL
    val category: String,   // CPU, MEMORY, BATTERY, NETWORK, STORAGE, SYSTEM
    val title: String,
    val description: String,
    val affectedComponent: String = "",  // 受影响的组件/应用
    val metrics: String = "",  // JSON格式的相关指标数据
    val stackTrace: String = "",  // 如果是错误/崩溃，记录堆栈信息
    val actionTaken: String = "",  // 系统采取的自动操作
    val userNotified: Boolean = false,
    val resolved: Boolean = false,
    val resolvedTimestamp: Long? = null
) : Serializable