package com.lanhe.gongjuxiang.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.lanhe.gongjuxiang.models.PerformanceData
import com.lanhe.gongjuxiang.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 增强的MainViewModel
 * 集成真实的性能监控功能
 */
class EnhancedMainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "EnhancedMainViewModel"
    }

    // 性能监控管理器
    private val performanceMonitor = PerformanceMonitorManager(application)

    // UI状态
    private val _performanceData = MutableStateFlow<PerformanceData?>(null)
    val performanceData: StateFlow<PerformanceData?> = _performanceData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _monitoringActive = MutableStateFlow(false)
    val monitoringActive: StateFlow<Boolean> = _monitoringActive.asStateFlow()

    // 专门的状态流
    private val _cpuUsage = MutableStateFlow(0f)
    val cpuUsage: StateFlow<Float> = _cpuUsage.asStateFlow()

    private val _memoryUsage = MutableStateFlow(0f)
    val memoryUsage: StateFlow<Float> = _memoryUsage.asStateFlow()

    private val _batteryLevel = MutableStateFlow(0)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _deviceTemperature = MutableStateFlow(0f)
    val deviceTemperature: StateFlow<Float> = _deviceTemperature.asStateFlow()

    private val _networkSpeed = MutableStateFlow(0.0)
    val networkSpeed: StateFlow<Double> = _networkSpeed.asStateFlow()

    init {
        initializePerformanceMonitoring()
    }

    /**
     * 初始化性能监控
     */
    private fun initializePerformanceMonitoring() {
        // 设置性能监控回调
        performanceMonitor.setCallback(object : PerformanceMonitorManager.PerformanceCallback {
            override fun onPerformanceUpdate(data: PerformanceData) {
                updatePerformanceData(data)
            }

            override fun onMonitoringStarted() {
                _monitoringActive.value = true
                _isLoading.value = false
                Log.i(TAG, "性能监控已启动")
            }

            override fun onMonitoringStopped() {
                _monitoringActive.value = false
                Log.i(TAG, "性能监控已停止")
            }

            override fun onError(error: Exception) {
                _errorMessage.value = "性能监控错误: ${error.message}"
                Log.e(TAG, "性能监控错误", error)
            }

            override fun onDataSaved(recordCount: Long) {
                Log.d(TAG, "已保存 $recordCount 条性能记录")
            }
        })
    }

    /**
     * 开始性能监控
     */
    fun startPerformanceMonitoring() {
        if (_monitoringActive.value) {
            Log.w(TAG, "性能监控已在运行")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        try {
            performanceMonitor.startMonitoring()
            startPeriodicUpdates()

        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = "启动监控失败: ${e.message}"
            Log.e(TAG, "启动性能监控失败", e)
        }
    }

    /**
     * 停止性能监控
     */
    fun stopPerformanceMonitoring() {
        if (!_monitoringActive.value) {
            Log.w(TAG, "性能监控未在运行")
            return
        }

        try {
            performanceMonitor.stopMonitoring()
            _monitoringActive.value = false
            _isLoading.value = false
        } catch (e: Exception) {
            _errorMessage.value = "停止监控失败: ${e.message}"
            Log.e(TAG, "停止性能监控失败", e)
        }
    }

    /**
     * 切换监控状态
     */
    fun togglePerformanceMonitoring() {
        if (_monitoringActive.value) {
            stopPerformanceMonitoring()
        } else {
            startPerformanceMonitoring()
        }
    }

    /**
     * 获取当前性能快照
     */
    fun getCurrentPerformanceSnapshot() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val performance = performanceMonitor.getCurrentPerformance()
                if (performance != null) {
                    updatePerformanceData(performance)
                } else {
                    _errorMessage.value = "无法获取性能数据"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "获取性能数据失败: ${e.message}"
                Log.e(TAG, "获取性能快照失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * 定期更新统计数据
     */
    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (_monitoringActive.value) {
                try {
                    // 性能数据更新由监控回调处理
                    kotlinx.coroutines.delay(30000)

                } catch (e: Exception) {
                    Log.e(TAG, "定期更新统计数据失败", e)
                    kotlinx.coroutines.delay(30000)
                }
            }
        }
    }

    /**
     * 更新性能数据
     */
    private fun updatePerformanceData(data: PerformanceData) {
        _performanceData.value = data
        
        // 更新具体的指标流
        _cpuUsage.value = data.cpuUsage
        _memoryUsage.value = data.memoryUsage.usagePercent
        _batteryLevel.value = data.batteryInfo.level
        _deviceTemperature.value = data.deviceTemperature
        
        Log.d(TAG, "性能数据已更新 - CPU: ${data.cpuUsage}%, 内存: ${data.memoryUsage.usagePercent}%, 电池: ${data.batteryInfo.level}%")
    }

    /**
     * 更新电池信息
     */
    private fun updateBatteryInfo(batteryInfo: com.lanhe.gongjuxiang.models.BatteryInfo) {
        _batteryLevel.value = batteryInfo.level
        Log.d(TAG, "电池信息已更新 - 电量: ${batteryInfo.level}%, 温度: ${batteryInfo.temperature}°C")
    }

    override fun onCleared() {
        super.onCleared()

        // 清理资源
        try {
            performanceMonitor.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "清理资源时出现异常", e)
        }

        Log.i(TAG, "EnhancedMainViewModel已清理")
    }

    /**
     * 工厂方法
     */
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EnhancedMainViewModel::class.java)) {
                return EnhancedMainViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
