package com.lanhe.gongjuxiang.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lanhe.gongjuxiang.utils.*
import com.lanhe.gongjuxiang.utils.SystemInfo

/**
 * 系统监控ViewModel
 * 管理系统监控数据的状态和业务逻辑
 */
class SystemMonitorViewModel : ViewModel() {

    // 系统统计数据
    private val _systemStats = MutableLiveData<SystemStats>()
    val systemStats: LiveData<SystemStats> = _systemStats

    // 进程列表
    private val _processList = MutableLiveData<List<com.lanhe.gongjuxiang.models.ProcessInfo>>()
    val processList: LiveData<List<com.lanhe.gongjuxiang.models.ProcessInfo>> = _processList

    // 系统信息
    private val _systemInfo = MutableLiveData<SystemInfo>()
    val systemInfo: LiveData<SystemInfo> = _systemInfo

    // 网络统计
    private val _networkStats = MutableLiveData<com.lanhe.gongjuxiang.models.NetworkStats>()
    val networkStats: LiveData<com.lanhe.gongjuxiang.models.NetworkStats> = _networkStats

    /**
     * 系统统计数据类
     */
    data class SystemStats(
        val cpuUsage: Float = 0f,
        val totalMemory: Long = 0L,
        val availableMemory: Long = 0L,
        val networkRx: Long = 0L,
        val networkTx: Long = 0L,
        val batteryLevel: Int = 0
    )

    /**
     * 更新系统统计数据
     */
    fun updateSystemStats(
        cpuUsage: Float,
        memoryInfo: com.lanhe.gongjuxiang.models.MemoryInfo,
        networkStats: com.lanhe.gongjuxiang.models.NetworkStats
    ) {
        val stats = SystemStats(
            cpuUsage = cpuUsage,
            totalMemory = memoryInfo.total,
            availableMemory = memoryInfo.available,
            networkRx = 0L, // 暂时使用0，之后可以扩展
            networkTx = 0L, // 暂时使用0，之后可以扩展
            batteryLevel = 0 // 这里可以从BatteryManager获取
        )
        _systemStats.value = stats
    }

    /**
     * 更新进程列表
     */
    fun updateProcessList(processes: List<com.lanhe.gongjuxiang.models.ProcessInfo>) {
        _processList.value = processes.sortedByDescending { it.memoryUsage }
    }

    /**
     * 更新系统信息
     */
    fun updateSystemInfo(info: SystemInfo) {
        _systemInfo.value = info
    }

    /**
     * 刷新所有数据
     */
    fun refreshAllData() {
        // 重新获取所有系统数据
        if (ShizukuManager.isShizukuAvailable()) {
            updateSystemStats(
                ShizukuManager.getCpuUsage(),
                ShizukuManager.getMemoryInfo(),
                ShizukuManager.getNetworkStats()
            )
            updateProcessList(ShizukuManager.getRunningProcesses())
        }
        updateSystemInfo(ShizukuManager.getSystemInfo())
    }

    /**
     * 杀死进程
     */
    fun killProcess(pid: Int): Boolean {
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.killProcess(pid)
        } else {
            false
        }
    }

    /**
     * 强制停止应用
     */
    fun forceStopPackage(packageName: String): Boolean {
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.forceStopPackage(packageName)
        } else {
            false
        }
    }

    /**
     * 获取应用列表
     */
    fun getInstalledPackages(): List<String> {
        return if (ShizukuManager.isShizukuAvailable()) {
            ShizukuManager.getInstalledPackages()
        } else {
            emptyList()
        }
    }
}
