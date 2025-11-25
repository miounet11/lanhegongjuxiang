package com.lanhe.gongjuxiang.models

/**
 * 功能介绍信息数据模型
 *
 * 用于在功能界面展示详细的优化逻辑、技术原理和实现细节
 */
data class FeatureInfo(
    /** 功能ID */
    val id: String,

    /** 功能名称 */
    val name: String,

    /** 功能图标emoji */
    val icon: String,

    /** 功能简介（一句话说明） */
    val brief: String,

    /** 📖 功能说明（详细介绍功能用途） */
    val description: String,

    /** 💡 优化逻辑（说明如何优化、优化什么） */
    val optimizationLogic: String,

    /** ⚙️ 技术原理（底层实现原理） */
    val technicalPrinciple: String,

    /** 🔧 实现细节（具体技术栈和实现方式） */
    val implementationDetails: String,

    /** 📊 预期效果（用户能获得什么提升） */
    val expectedResults: String,

    /** ⚠️ 注意事项（使用时需要注意的问题） */
    val warnings: String = ""
)

/**
 * 功能介绍部分枚举
 */
enum class FeatureInfoSection {
    DESCRIPTION,        // 功能说明
    LOGIC,             // 优化逻辑
    PRINCIPLE,         // 技术原理
    IMPLEMENTATION,    // 实现细节
    RESULTS,           // 预期效果
    WARNINGS           // 注意事项
}
