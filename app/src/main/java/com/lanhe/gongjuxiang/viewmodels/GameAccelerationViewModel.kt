package com.lanhe.gongjuxiang.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * 游戏加速ViewModel
 * 管理系统游戏加速的状态和数据
 */
class GameAccelerationViewModel : ViewModel() {

    // 性能指标数据
    private val _performanceMetrics = MutableLiveData<PerformanceMetrics>()
    val performanceMetrics: LiveData<PerformanceMetrics> = _performanceMetrics

    // 游戏模式状态
    private val _gameModeEnabled = MutableLiveData<Boolean>()
    val gameModeEnabled: LiveData<Boolean> = _gameModeEnabled

    // 图片加速状态
    private val _imageAccelerationEnabled = MutableLiveData<Boolean>()
    val imageAccelerationEnabled: LiveData<Boolean> = _imageAccelerationEnabled

    init {
        // 初始化默认状态
        _gameModeEnabled.value = false
        _imageAccelerationEnabled.value = false
        
        // 初始化默认性能指标
        _performanceMetrics.value = PerformanceMetrics(
            cpuUsage = 25f,
            memoryUsed = 1024L * 1024 * 1024, // 1GB
            networkLatency = 45L,
            imageLoadTime = 1.2,
            performanceBoost = "30-50%",
            batteryImpact = "+10-15%"
        )
    }

    /**
     * 性能指标数据类
     */
    data class PerformanceMetrics(
        val cpuUsage: Float = 0f,
        val memoryUsed: Long = 0L,
        val networkLatency: Long = 0L,
        val imageLoadTime: Double = 0.0,
        val performanceBoost: String = "",
        val batteryImpact: String = ""
    )

    /**
     * 更新性能指标
     */
    fun updatePerformanceMetrics(metrics: PerformanceMetrics) {
        _performanceMetrics.value = metrics
    }

    /**
     * 设置游戏模式状态
     */
    fun setGameModeEnabled(enabled: Boolean) {
        _gameModeEnabled.value = enabled
        
        // 当启用游戏模式时，更新性能指标
        if (enabled) {
            val currentMetrics = _performanceMetrics.value ?: PerformanceMetrics()
            val optimizedMetrics = currentMetrics.copy(
                cpuUsage = (currentMetrics.cpuUsage * 0.7f).coerceAtMost(100f), // CPU使用率降低30%
                networkLatency = (currentMetrics.networkLatency * 0.6).toLong(), // 延迟降低40%
                imageLoadTime = currentMetrics.imageLoadTime * 0.65, // 图片加载加快35%
                performanceBoost = "30-50%",
                batteryImpact = "+10-15%"
            )
            _performanceMetrics.value = optimizedMetrics
        }
    }

    /**
     * 设置图片加速状态
     */
    fun setImageAccelerationEnabled(enabled: Boolean) {
        _imageAccelerationEnabled.value = enabled
        
        // 当启用图片加速时，更新图片加载时间
        if (enabled) {
            val currentMetrics = _performanceMetrics.value ?: PerformanceMetrics()
            val optimizedMetrics = currentMetrics.copy(
                imageLoadTime = currentMetrics.imageLoadTime * 0.6 // 图片加载加快40%
            )
            _performanceMetrics.value = optimizedMetrics
        }
    }

    /**
     * 执行游戏优化
     */
    fun performGameOptimization(): Boolean {
        try {
            // 模拟优化过程
            val currentMetrics = _performanceMetrics.value ?: PerformanceMetrics()
            
            // 应用优化效果
            val optimizedMetrics = currentMetrics.copy(
                cpuUsage = (currentMetrics.cpuUsage * 0.75f).coerceAtMost(100f),
                networkLatency = (currentMetrics.networkLatency * 0.5).toLong(),
                imageLoadTime = currentMetrics.imageLoadTime * 0.5,
                performanceBoost = "40-60%",
                batteryImpact = "+15-20%"
            )
            
            _performanceMetrics.value = optimizedMetrics
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 重置为基准性能
     */
    fun resetToBaseline() {
        _performanceMetrics.value = PerformanceMetrics(
            cpuUsage = 35f,
            memoryUsed = 1024L * 1024 * 1024 * 2, // 2GB
            networkLatency = 60L,
            imageLoadTime = 1.5,
            performanceBoost = "0%",
            batteryImpact = "0%"
        )
        
        _gameModeEnabled.value = false
        _imageAccelerationEnabled.value = false
    }

    /**
     * 获取优化建议
     */
    fun getOptimizationSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        
        val metrics = _performanceMetrics.value ?: return suggestions
        
        if (metrics.cpuUsage > 80) {
            suggestions.add("⚠️ CPU使用率过高，建议启用游戏模式")
        }
        
        if (metrics.networkLatency > 100) {
            suggestions.add("🌐 网络延迟较高，建议优化网络设置")
        }
        
        if (metrics.imageLoadTime > 2.0) {
            suggestions.add("🖼️ 图片加载较慢，建议启用图片加速")
        }
        
        if (suggestions.isEmpty()) {
            suggestions.add("✅ 系统性能良好，无需额外优化")
        }
        
        return suggestions
    }
}
