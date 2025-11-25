package com.lanhe.gongjuxiang.models

/**
 * 优化优先级枚举
 * 统一的优化优先级定义
 */
enum class OptimizationPriority(val value: Int) {
    LOW(1),         // 低优先级
    MEDIUM(2),      // 中优先级
    HIGH(3),        // 高优先级
    CRITICAL(4)     // 关键优先级
}