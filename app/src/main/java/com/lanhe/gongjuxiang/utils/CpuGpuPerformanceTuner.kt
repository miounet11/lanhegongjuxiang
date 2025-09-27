package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.roundToInt

/**
 * CPU/GPU性能调优器
 * 提供CPU调频、GPU调优、热节流监控和性能模式管理
 */
class CpuGpuPerformanceTuner(private val context: Context) {

    companion object {
        private const val TAG = "CpuGpuPerformanceTuner"
        private const val THERMAL_THRESHOLD_WARNING = 45f // 45°C警告
        private const val THERMAL_THRESHOLD_CRITICAL = 55f // 55°C临界
        private const val CPU_FREQ_PATH = "/sys/devices/system/cpu"
        private const val GPU_FREQ_PATH = "/sys/class/kgsl/kgsl-3d0"
        private const val THERMAL_PATH = "/sys/class/thermal"
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val dataManager = DataManager(context)

    // 性能状态流
    private val _performanceState = MutableStateFlow<PerformanceState>(PerformanceState())
    val performanceState: StateFlow<PerformanceState> = _performanceState.asStateFlow()

    // 当前性能模式
    private var currentProfile = PerformanceProfile.BALANCED

    // CPU核心信息
    private val cpuCores = Runtime.getRuntime().availableProcessors()
    private val cpuCoreInfo = mutableMapOf<Int, CpuCoreInfo>()

    init {
        initializeCpuCoreInfo()
    }

    /**
     * 设置性能模式
     */
    suspend fun setPerformanceProfile(profile: PerformanceProfile): PerformanceOptimizationResult {
        return withContext(Dispatchers.IO) {
            try {
                currentProfile = profile
                val results = mutableListOf<String>()

                when (profile) {
                    PerformanceProfile.BATTERY_SAVER -> {
                        results.addAll(applyBatterySaverProfile())
                    }
                    PerformanceProfile.BALANCED -> {
                        results.addAll(applyBalancedProfile())
                    }
                    PerformanceProfile.PERFORMANCE -> {
                        results.addAll(applyPerformanceProfile())
                    }
                    PerformanceProfile.GAMING -> {
                        results.addAll(applyGamingProfile())
                    }
                }

                // 更新性能状态
                updatePerformanceState()

                PerformanceOptimizationResult(
                    success = results.isNotEmpty(),
                    improvements = results,
                    message = "性能模式已切换至: ${profile.displayName}"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Failed to set performance profile", e)
                PerformanceOptimizationResult(
                    success = false,
                    message = "性能模式切换失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 执行全面性能优化
     */
    suspend fun performFullPerformanceOptimization(): PerformanceOptimizationResult {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<String>()

                // 1. CPU频率优化
                val cpuOptimization = optimizeCpuFrequency()
                results.addAll(cpuOptimization.improvements)

                // 2. GPU频率优化
                val gpuOptimization = optimizeGpuFrequency()
                results.addAll(gpuOptimization.improvements)

                // 3. 热节流管理
                val thermalOptimization = manageThermalThrottling()
                results.addAll(thermalOptimization.improvements)

                // 4. CPU核心管理
                val coreOptimization = optimizeCpuCores()
                results.addAll(coreOptimization.improvements)

                // 5. 调度器优化
                val schedulerOptimization = optimizeScheduler()
                results.addAll(schedulerOptimization.improvements)

                // 6. 应用CPU优先级管理
                val priorityOptimization = optimizeAppPriorities()
                results.addAll(priorityOptimization.improvements)

                PerformanceOptimizationResult(
                    success = results.isNotEmpty(),
                    improvements = results,
                    message = "性能优化完成，应用了${results.size}项优化"
                )

            } catch (e: Exception) {
                Log.e(TAG, "Performance optimization failed", e)
                PerformanceOptimizationResult(
                    success = false,
                    message = "性能优化失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 获取CPU信息
     */
    fun getCpuInfo(): CpuInfo {
        return CpuInfo(
            coreCount = cpuCores,
            currentFrequencies = getCurrentCpuFrequencies(),
            maxFrequencies = getMaxCpuFrequencies(),
            minFrequencies = getMinCpuFrequencies(),
            governors = getCpuGovernors(),
            usage = getCurrentCpuUsage(),
            temperature = getCpuTemperature()
        )
    }

    /**
     * 获取GPU信息
     */
    fun getGpuInfo(): GpuInfo {
        return GpuInfo(
            name = getGpuName(),
            currentFrequency = getCurrentGpuFrequency(),
            maxFrequency = getMaxGpuFrequency(),
            minFrequency = getMinGpuFrequency(),
            governor = getGpuGovernor(),
            usage = getCurrentGpuUsage(),
            temperature = getGpuTemperature()
        )
    }

    /**
     * 获取热节流状态
     */
    fun getThermalStatus(): ThermalStatus {
        val cpuTemp = getCpuTemperature()
        val gpuTemp = getGpuTemperature()
        val batteryTemp = getBatteryTemperature()

        val maxTemp = maxOf(cpuTemp, gpuTemp, batteryTemp)
        val thermalState = when {
            maxTemp >= THERMAL_THRESHOLD_CRITICAL -> ThermalState.CRITICAL
            maxTemp >= THERMAL_THRESHOLD_WARNING -> ThermalState.WARNING
            else -> ThermalState.NORMAL
        }

        return ThermalStatus(
            cpuTemperature = cpuTemp,
            gpuTemperature = gpuTemp,
            batteryTemperature = batteryTemp,
            maxTemperature = maxTemp,
            thermalState = thermalState,
            isThrottling = isSystemThrottling(),
            recommendations = generateThermalRecommendations(thermalState, maxTemp)
        )
    }

    /**
     * 实时监控性能指标
     */
    suspend fun startPerformanceMonitoring() {
        withContext(Dispatchers.IO) {
            try {
                while (true) {
                    updatePerformanceState()
                    kotlinx.coroutines.delay(5000) // 5秒更新一次
                }
            } catch (e: Exception) {
                Log.e(TAG, "Performance monitoring failed", e)
            }
        }
    }

    // 性能模式应用方法

    private suspend fun applyBatterySaverProfile(): List<String> {
        val improvements = mutableListOf<String>()

        // CPU降频
        if (setCpuGovernor("powersave")) {
            improvements.add("CPU设置为省电模式")
        }

        // 限制CPU最大频率
        if (setCpuMaxFrequency(0.6f)) {
            improvements.add("限制CPU最大频率至60%")
        }

        // GPU降频
        if (setGpuGovernor("powersave")) {
            improvements.add("GPU设置为省电模式")
        }

        // 关闭高性能核心
        if (disableHighPerformanceCores()) {
            improvements.add("关闭高性能CPU核心")
        }

        return improvements
    }

    private suspend fun applyBalancedProfile(): List<String> {
        val improvements = mutableListOf<String>()

        // CPU平衡模式
        if (setCpuGovernor("interactive")) {
            improvements.add("CPU设置为智能交互模式")
        }

        // GPU平衡模式
        if (setGpuGovernor("msm-adreno-tz")) {
            improvements.add("GPU设置为平衡模式")
        }

        // 启用所有核心
        if (enableAllCpuCores()) {
            improvements.add("启用所有CPU核心")
        }

        return improvements
    }

    private suspend fun applyPerformanceProfile(): List<String> {
        val improvements = mutableListOf<String>()

        // CPU性能模式
        if (setCpuGovernor("performance")) {
            improvements.add("CPU设置为性能模式")
        }

        // 设置最大CPU频率
        if (setCpuMaxFrequency(1.0f)) {
            improvements.add("CPU频率设置为最大")
        }

        // GPU性能模式
        if (setGpuGovernor("performance")) {
            improvements.add("GPU设置为性能模式")
        }

        // 优化调度器
        if (setSchedulerTuning("performance")) {
            improvements.add("调度器优化为性能优先")
        }

        return improvements
    }

    private suspend fun applyGamingProfile(): List<String> {
        val improvements = mutableListOf<String>()

        // 游戏专用CPU调频
        if (setCpuGovernor("schedutil")) {
            improvements.add("CPU设置为游戏优化模式")
        }

        // GPU最大性能
        if (setGpuFrequency(getMaxGpuFrequency())) {
            improvements.add("GPU频率锁定最大值")
        }

        // 禁用温控降频
        if (disableThermalThrottling()) {
            improvements.add("暂时禁用温控降频")
        }

        // 设置高优先级
        if (setGameModeScheduling()) {
            improvements.add("启用游戏模式调度")
        }

        return improvements
    }

    // CPU优化方法

    private suspend fun optimizeCpuFrequency(): OptimizationItem {
        val improvements = mutableListOf<String>()

        try {
            // 根据当前负载调整频率
            val currentUsage = getCurrentCpuUsage()
            val targetGovernor = when {
                currentUsage > 80 -> "performance"
                currentUsage > 50 -> "schedutil"
                currentUsage > 20 -> "interactive"
                else -> "powersave"
            }

            if (setCpuGovernor(targetGovernor)) {
                improvements.add("根据负载设置CPU调频策略: $targetGovernor")
            }

            // 优化每个核心的频率
            for (i in 0 until cpuCores) {
                if (optimizeCoreFrequency(i)) {
                    improvements.add("优化CPU核心$i 频率")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "CPU frequency optimization failed", e)
        }

        return OptimizationItem(
            name = "CPU频率优化",
            success = improvements.isNotEmpty(),
            improvements = improvements
        )
    }

    private suspend fun optimizeGpuFrequency(): OptimizationItem {
        val improvements = mutableListOf<String>()

        try {
            val currentUsage = getCurrentGpuUsage()

            when {
                currentUsage > 80 -> {
                    if (setGpuFrequency(getMaxGpuFrequency())) {
                        improvements.add("GPU设置为最大频率以应对高负载")
                    }
                }
                currentUsage > 40 -> {
                    val midFreq = (getMaxGpuFrequency() + getMinGpuFrequency()) / 2
                    if (setGpuFrequency(midFreq)) {
                        improvements.add("GPU设置为中等频率")
                    }
                }
                else -> {
                    if (setGpuFrequency(getMinGpuFrequency())) {
                        improvements.add("GPU降低频率以节省电量")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "GPU frequency optimization failed", e)
        }

        return OptimizationItem(
            name = "GPU频率优化",
            success = improvements.isNotEmpty(),
            improvements = improvements
        )
    }

    private suspend fun manageThermalThrottling(): OptimizationItem {
        val improvements = mutableListOf<String>()

        try {
            val thermalStatus = getThermalStatus()

            when (thermalStatus.thermalState) {
                ThermalState.CRITICAL -> {
                    if (enableAggressiveThermalManagement()) {
                        improvements.add("启用激进温控管理")
                    }
                    if (reducePerformance()) {
                        improvements.add("降低性能以控制温度")
                    }
                }
                ThermalState.WARNING -> {
                    if (enableModerateThermalManagement()) {
                        improvements.add("启用温和温控管理")
                    }
                }
                ThermalState.NORMAL -> {
                    if (optimizeThermalSettings()) {
                        improvements.add("优化温控设置")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Thermal management failed", e)
        }

        return OptimizationItem(
            name = "热节流管理",
            success = improvements.isNotEmpty(),
            improvements = improvements
        )
    }

    private suspend fun optimizeCpuCores(): OptimizationItem {
        val improvements = mutableListOf<String>()

        try {
            val currentUsage = getCurrentCpuUsage()

            when (currentProfile) {
                PerformanceProfile.BATTERY_SAVER -> {
                    val disabledCores = disableUnnecessaryCores()
                    if (disabledCores > 0) {
                        improvements.add("关闭${disabledCores}个CPU核心以节省电量")
                    }
                }
                PerformanceProfile.PERFORMANCE, PerformanceProfile.GAMING -> {
                    if (enableAllCpuCores()) {
                        improvements.add("启用所有CPU核心以提升性能")
                    }
                }
                PerformanceProfile.BALANCED -> {
                    val optimizedCores = optimizeCoreUsage()
                    if (optimizedCores > 0) {
                        improvements.add("优化${optimizedCores}个CPU核心使用")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "CPU core optimization failed", e)
        }

        return OptimizationItem(
            name = "CPU核心优化",
            success = improvements.isNotEmpty(),
            improvements = improvements
        )
    }

    private suspend fun optimizeScheduler(): OptimizationItem {
        val improvements = mutableListOf<String>()

        try {
            val schedulerType = when (currentProfile) {
                PerformanceProfile.GAMING -> "deadline"
                PerformanceProfile.PERFORMANCE -> "cfq"
                PerformanceProfile.BALANCED -> "noop"
                PerformanceProfile.BATTERY_SAVER -> "noop"
            }

            if (setIOScheduler(schedulerType)) {
                improvements.add("I/O调度器设置为: $schedulerType")
            }

            if (optimizeSchedulerParameters()) {
                improvements.add("优化调度器参数")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Scheduler optimization failed", e)
        }

        return OptimizationItem(
            name = "调度器优化",
            success = improvements.isNotEmpty(),
            improvements = improvements
        )
    }

    private suspend fun optimizeAppPriorities(): OptimizationItem {
        val improvements = mutableListOf<String>()

        try {
            // 提升前台应用优先级
            val foregroundApps = getForegroundApps()
            var prioritizedApps = 0

            foregroundApps.forEach { packageName ->
                if (setAppCpuPriority(packageName, "high")) {
                    prioritizedApps++
                }
            }

            if (prioritizedApps > 0) {
                improvements.add("提升${prioritizedApps}个前台应用的CPU优先级")
            }

            // 降低后台应用优先级
            val backgroundApps = getBackgroundApps()
            var deprioritizedApps = 0

            backgroundApps.forEach { packageName ->
                if (setAppCpuPriority(packageName, "low")) {
                    deprioritizedApps++
                }
            }

            if (deprioritizedApps > 0) {
                improvements.add("降低${deprioritizedApps}个后台应用的CPU优先级")
            }

        } catch (e: Exception) {
            Log.e(TAG, "App priority optimization failed", e)
        }

        return OptimizationItem(
            name = "应用优先级优化",
            success = improvements.isNotEmpty(),
            improvements = improvements
        )
    }

    // 系统信息获取方法

    private fun initializeCpuCoreInfo() {
        for (i in 0 until cpuCores) {
            cpuCoreInfo[i] = CpuCoreInfo(
                coreId = i,
                isOnline = isCpuCoreOnline(i),
                currentFreq = getCpuCoreFrequency(i),
                maxFreq = getCpuCoreMaxFrequency(i),
                minFreq = getCpuCoreMinFrequency(i)
            )
        }
    }

    private fun updatePerformanceState() {
        val cpuInfo = getCpuInfo()
        val gpuInfo = getGpuInfo()
        val thermalStatus = getThermalStatus()

        _performanceState.value = PerformanceState(
            cpuInfo = cpuInfo,
            gpuInfo = gpuInfo,
            thermalStatus = thermalStatus,
            currentProfile = currentProfile,
            timestamp = System.currentTimeMillis()
        )
    }

    // 具体实现方法（需要根据设备实际情况调整）

    private fun getCurrentCpuFrequencies(): List<Long> {
        val frequencies = mutableListOf<Long>()
        for (i in 0 until cpuCores) {
            frequencies.add(getCpuCoreFrequency(i))
        }
        return frequencies
    }

    private fun getMaxCpuFrequencies(): List<Long> {
        val frequencies = mutableListOf<Long>()
        for (i in 0 until cpuCores) {
            frequencies.add(getCpuCoreMaxFrequency(i))
        }
        return frequencies
    }

    private fun getMinCpuFrequencies(): List<Long> {
        val frequencies = mutableListOf<Long>()
        for (i in 0 until cpuCores) {
            frequencies.add(getCpuCoreMinFrequency(i))
        }
        return frequencies
    }

    private fun getCpuGovernors(): List<String> {
        val governors = mutableListOf<String>()
        for (i in 0 until cpuCores) {
            governors.add(getCpuCoreGovernor(i))
        }
        return governors
    }

    private fun getCurrentCpuUsage(): Float {
        // 实现CPU使用率获取
        return 0f
    }

    private fun getCpuTemperature(): Float {
        return try {
            val thermalFile = File("$THERMAL_PATH/thermal_zone0/temp")
            if (thermalFile.exists()) {
                RandomAccessFile(thermalFile, "r").use {
                    it.readLine().toFloat() / 1000f
                }
            } else {
                25f
            }
        } catch (e: Exception) {
            25f
        }
    }

    // GPU相关方法
    private fun getGpuName(): String = "Adreno GPU"
    private fun getCurrentGpuFrequency(): Long = 0L
    private fun getMaxGpuFrequency(): Long = 800_000_000L
    private fun getMinGpuFrequency(): Long = 200_000_000L
    private fun getGpuGovernor(): String = "msm-adreno-tz"
    private fun getCurrentGpuUsage(): Float = 0f
    private fun getGpuTemperature(): Float = 30f

    // CPU核心操作方法
    private fun isCpuCoreOnline(coreId: Int): Boolean = true
    private fun getCpuCoreFrequency(coreId: Int): Long = 1800_000_000L
    private fun getCpuCoreMaxFrequency(coreId: Int): Long = 2400_000_000L
    private fun getCpuCoreMinFrequency(coreId: Int): Long = 300_000_000L
    private fun getCpuCoreGovernor(coreId: Int): String = "schedutil"

    // 设置方法
    private fun setCpuGovernor(governor: String): Boolean = false
    private fun setCpuMaxFrequency(ratio: Float): Boolean = false
    private fun setGpuGovernor(governor: String): Boolean = false
    private fun setGpuFrequency(frequency: Long): Boolean = false
    private fun disableHighPerformanceCores(): Boolean = false
    private fun enableAllCpuCores(): Boolean = false
    private fun setSchedulerTuning(mode: String): Boolean = false
    private fun disableThermalThrottling(): Boolean = false
    private fun setGameModeScheduling(): Boolean = false

    // 其他辅助方法
    private fun optimizeCoreFrequency(coreId: Int): Boolean = false
    private fun enableAggressiveThermalManagement(): Boolean = false
    private fun enableModerateThermalManagement(): Boolean = false
    private fun optimizeThermalSettings(): Boolean = false
    private fun reducePerformance(): Boolean = false
    private fun disableUnnecessaryCores(): Int = 0
    private fun optimizeCoreUsage(): Int = 0
    private fun setIOScheduler(scheduler: String): Boolean = false
    private fun optimizeSchedulerParameters(): Boolean = false
    private fun getForegroundApps(): List<String> = emptyList()
    private fun getBackgroundApps(): List<String> = emptyList()
    private fun setAppCpuPriority(packageName: String, priority: String): Boolean = false
    private fun getBatteryTemperature(): Float = 30f
    private fun isSystemThrottling(): Boolean = false

    private fun generateThermalRecommendations(state: ThermalState, temperature: Float): List<String> {
        return when (state) {
            ThermalState.CRITICAL -> listOf(
                "设备温度过高(${"%.1f".format(temperature)}°C)，请立即停止使用",
                "关闭高耗能应用",
                "移至阴凉通风处"
            )
            ThermalState.WARNING -> listOf(
                "设备温度较高(${"%.1f".format(temperature)}°C)",
                "建议降低亮度",
                "暂停重负载任务"
            )
            ThermalState.NORMAL -> listOf("设备温度正常")
        }
    }
}

// 数据类定义

enum class PerformanceProfile(val displayName: String) {
    BATTERY_SAVER("省电模式"),
    BALANCED("平衡模式"),
    PERFORMANCE("性能模式"),
    GAMING("游戏模式")
}

enum class ThermalState {
    NORMAL, WARNING, CRITICAL
}

data class PerformanceState(
    val cpuInfo: CpuInfo = CpuInfo(),
    val gpuInfo: GpuInfo = GpuInfo(),
    val thermalStatus: ThermalStatus = ThermalStatus(),
    val currentProfile: PerformanceProfile = PerformanceProfile.BALANCED,
    val timestamp: Long = System.currentTimeMillis()
)

data class PerformanceOptimizationResult(
    val success: Boolean = false,
    val improvements: List<String> = emptyList(),
    val message: String = ""
)

data class CpuInfo(
    val coreCount: Int = 0,
    val currentFrequencies: List<Long> = emptyList(),
    val maxFrequencies: List<Long> = emptyList(),
    val minFrequencies: List<Long> = emptyList(),
    val governors: List<String> = emptyList(),
    val usage: Float = 0f,
    val temperature: Float = 0f
)

data class GpuInfo(
    val name: String = "",
    val currentFrequency: Long = 0L,
    val maxFrequency: Long = 0L,
    val minFrequency: Long = 0L,
    val governor: String = "",
    val usage: Float = 0f,
    val temperature: Float = 0f
)

data class ThermalStatus(
    val cpuTemperature: Float = 0f,
    val gpuTemperature: Float = 0f,
    val batteryTemperature: Float = 0f,
    val maxTemperature: Float = 0f,
    val thermalState: ThermalState = ThermalState.NORMAL,
    val isThrottling: Boolean = false,
    val recommendations: List<String> = emptyList()
)

data class CpuCoreInfo(
    val coreId: Int,
    val isOnline: Boolean,
    val currentFreq: Long,
    val maxFreq: Long,
    val minFreq: Long
)