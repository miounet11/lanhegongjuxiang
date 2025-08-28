package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.os.Build

/**
 * 设备适配器
 * 根据不同手机品牌、型号和Android版本提供相应的优化方案
 */
object DeviceAdapter {

    /**
     * 设备信息数据类
     */
    data class DeviceInfo(
        val brand: String,
        val model: String,
        val androidVersion: Int,
        val androidVersionName: String,
        val isSupported: Boolean,
        val recommendedOptimizations: List<String>
    )

    /**
     * 获取当前设备信息
     */
    fun getDeviceInfo(context: Context): DeviceInfo {
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL
        val androidVersion = Build.VERSION.SDK_INT
        val androidVersionName = Build.VERSION.RELEASE

        val recommendedOptimizations = getRecommendedOptimizations(brand, androidVersion)

        return DeviceInfo(
            brand = brand,
            model = model,
            androidVersion = androidVersion,
            androidVersionName = androidVersionName,
            isSupported = isDeviceSupported(brand),
            recommendedOptimizations = recommendedOptimizations
        )
    }

    /**
     * 检查设备是否受支持
     */
    private fun isDeviceSupported(brand: String): Boolean {
        val supportedBrands = listOf(
            "huawei", "honor", "xiaomi", "redmi", "oppo", "realme",
            "vivo", "samsung", "oneplus", "meizu", "lenovo", "zte",
            "coolpad", "gionee", "smartisan", "nubia"
        )
        return supportedBrands.contains(brand)
    }

    /**
     * 获取推荐的优化方案
     */
    private fun getRecommendedOptimizations(brand: String, androidVersion: Int): List<String> {
        val optimizations = mutableListOf<String>()

        // 基础优化（适用于所有设备）
        optimizations.add("内存优化")
        optimizations.add("电池管理")
        optimizations.add("存储清理")

        // 根据品牌添加特定优化
        when (brand) {
            "huawei", "honor" -> {
                optimizations.add("华为EMUI优化")
                optimizations.add("鸿蒙系统适配")
                optimizations.add("华为电池管理")
            }
            "xiaomi", "redmi" -> {
                optimizations.add("MIUI优化")
                optimizations.add("小米电池管理")
                optimizations.add("MIUI性能模式")
            }
            "oppo", "realme" -> {
                optimizations.add("ColorOS优化")
                optimizations.add("OPPO电池管理")
                optimizations.add("游戏空间优化")
            }
            "vivo" -> {
                optimizations.add("Funtouch OS优化")
                optimizations.add("vivo电池管理")
                optimizations.add("多窗口优化")
            }
            "samsung" -> {
                optimizations.add("One UI优化")
                optimizations.add("三星电池管理")
                optimizations.add("Bixby禁用")
            }
            "oneplus" -> {
                optimizations.add("OxygenOS优化")
                optimizations.add("一加电池管理")
                optimizations.add("禅定模式")
            }
            else -> {
                optimizations.add("通用系统优化")
            }
        }

        // 根据Android版本添加优化
        when {
            androidVersion >= 33 -> { // Android 13+
                optimizations.add("Android 13+特性优化")
                optimizations.add("隐私权限优化")
            }
            androidVersion >= 31 -> { // Android 12+
                optimizations.add("Android 12+特性优化")
                optimizations.add("Material You适配")
            }
            androidVersion >= 29 -> { // Android 10+
                optimizations.add("Android 10+特性优化")
                optimizations.add("深色模式优化")
            }
        }

        return optimizations
    }

    /**
     * 获取设备特定的省电方案
     */
    fun getPowerSavingStrategy(brand: String, androidVersion: Int): List<String> {
        val strategies = mutableListOf<String>()

        when (brand) {
            "huawei", "honor" -> {
                strategies.add("华为省电模式")
                strategies.add("智能省电调度")
                strategies.add("EMUI电池优化")
            }
            "xiaomi", "redmi" -> {
                strategies.add("小米超省电模式")
                strategies.add("MIUI电池优化")
                strategies.add("神隐模式")
            }
            "oppo", "realme" -> {
                strategies.add("OPPO超级省电")
                strategies.add("ColorOS省电优化")
                strategies.add("睡眠省电模式")
            }
            "vivo" -> {
                strategies.add("vivo超级省电")
                strategies.add("Funtouch省电模式")
                strategies.add("智能省电调度")
            }
            "samsung" -> {
                strategies.add("三星超省电模式")
                strategies.add("One UI电池优化")
                strategies.add("Bixby省电模式")
            }
            else -> {
                strategies.add("系统省电模式")
                strategies.add("应用休眠优化")
                strategies.add("后台限制优化")
            }
        }

        return strategies
    }

    /**
     * 获取设备特定的游戏优化方案
     */
    fun getGameOptimizationStrategy(brand: String): List<String> {
        val strategies = mutableListOf<String>()

        when (brand) {
            "huawei", "honor" -> {
                strategies.add("华为GPU加速")
                strategies.add("鸿蒙游戏模式")
                strategies.add("EMUI游戏优化")
            }
            "xiaomi", "redmi" -> {
                strategies.add("MIUI游戏模式")
                strategies.add("游戏加速引擎")
                strategies.add("游戏画质增强")
            }
            "oppo", "realme" -> {
                strategies.add("HyperBoost加速")
                strategies.add("OPPO游戏模式")
                strategies.add("游戏画质优化")
            }
            "vivo" -> {
                strategies.add("Multi-Turbo加速")
                strategies.add("vivo游戏模式")
                strategies.add("游戏画质增强")
            }
            "oneplus" -> {
                strategies.add("Fnatic Mode")
                strategies.add("一加游戏模式")
                strategies.add("游戏画质优化")
            }
            else -> {
                strategies.add("系统游戏模式")
                strategies.add("CPU性能调度")
                strategies.add("GPU加速优化")
            }
        }

        return strategies
    }

    /**
     * 获取设备特定的护眼方案
     */
    fun getEyeProtectionStrategy(brand: String, androidVersion: Int): List<String> {
        val strategies = mutableListOf<String>()

        // 基础护眼功能
        strategies.add("蓝光过滤")
        strategies.add("色温调节")

        if (androidVersion >= 29) { // Android 10+
            strategies.add("系统深色模式")
        }

        when (brand) {
            "huawei", "honor" -> {
                strategies.add("华为护眼模式")
                strategies.add("EMUI护眼设置")
            }
            "xiaomi", "redmi" -> {
                strategies.add("MIUI护眼模式")
                strategies.add("小米护眼设置")
            }
            "oppo", "realme" -> {
                strategies.add("OPPO护眼模式")
                strategies.add("ColorOS护眼设置")
            }
            "vivo" -> {
                strategies.add("vivo护眼模式")
                strategies.add("Funtouch护眼设置")
            }
            else -> {
                strategies.add("系统护眼模式")
            }
        }

        return strategies
    }

    /**
     * 获取设备特定的清理方案
     */
    fun getCleanupStrategy(brand: String): List<String> {
        val strategies = mutableListOf<String>()

        // 基础清理
        strategies.add("缓存清理")
        strategies.add("临时文件清理")
        strategies.add("垃圾文件清理")

        when (brand) {
            "huawei", "honor" -> {
                strategies.add("华为系统清理")
                strategies.add("EMUI垃圾清理")
            }
            "xiaomi", "redmi" -> {
                strategies.add("MIUI安全中心清理")
                strategies.add("小米系统清理")
            }
            "oppo", "realme" -> {
                strategies.add("OPPO手机管家清理")
                strategies.add("ColorOS垃圾清理")
            }
            "vivo" -> {
                strategies.add("vivo系统清理")
                strategies.add("Funtouch垃圾清理")
            }
            else -> {
                strategies.add("系统深度清理")
            }
        }

        return strategies
    }

    /**
     * 获取设备品牌显示名称
     */
    fun getBrandDisplayName(brand: String): String {
        return when (brand.lowercase()) {
            "huawei" -> "华为"
            "honor" -> "荣耀"
            "xiaomi" -> "小米"
            "redmi" -> "红米"
            "oppo" -> "OPPO"
            "realme" -> "Realme"
            "vivo" -> "vivo"
            "samsung" -> "三星"
            "oneplus" -> "一加"
            "meizu" -> "魅族"
            "lenovo" -> "联想"
            "zte" -> "中兴"
            "coolpad" -> "酷派"
            "gionee" -> "金立"
            "smartisan" -> "锤子"
            "nubia" -> "努比亚"
            else -> brand.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * 获取设备支持情况
     */
    fun getDeviceSupportInfo(brand: String): String {
        return when {
            isDeviceSupported(brand) -> "✅ 完全支持"
            else -> "⚠️ 基础支持，建议使用通用优化方案"
        }
    }
}
