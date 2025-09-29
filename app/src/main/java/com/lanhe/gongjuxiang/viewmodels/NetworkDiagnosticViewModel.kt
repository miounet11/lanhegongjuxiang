package com.lanhe.gongjuxiang.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * 网络诊断ViewModel
 * 管理系统网络诊断的状态和数据
 */
class NetworkDiagnosticViewModel : ViewModel() {

    // 网络信息
    private val _networkInfo = MutableLiveData<NetworkInfo>()
    val networkInfo: LiveData<NetworkInfo> = _networkInfo

    // 延迟测试结果
    private val _latencyResult = MutableLiveData<LatencyResult>()
    val latencyResult: LiveData<LatencyResult> = _latencyResult

    // 位置扫描结果
    private val _positionScanResult = MutableLiveData<PositionScanResult>()
    val positionScanResult: LiveData<PositionScanResult> = _positionScanResult

    // WiFi信号强度列表
    private val _wifiSignals = MutableLiveData<List<WifiSignal>>()
    val wifiSignals: LiveData<List<WifiSignal>> = _wifiSignals

    // 网络占用应用列表
    private val _networkUsageApps = MutableLiveData<List<NetworkUsageApp>>()
    val networkUsageApps: LiveData<List<NetworkUsageApp>> = _networkUsageApps

    // 电池消耗应用列表
    private val _batteryConsumingApps = MutableLiveData<List<BatteryConsumingApp>>()
    val batteryConsumingApps: LiveData<List<BatteryConsumingApp>> = _batteryConsumingApps

    // 诊断状态
    private val _diagnosticStatus = MutableLiveData<String>()
    val diagnosticStatus: LiveData<String> = _diagnosticStatus

    init {
        // 初始化默认网络信息
        _networkInfo.value = NetworkInfo(
            type = "Wi-Fi",
            ssid = "MyWiFi",
            bssid = "00:11:22:33:44:55",
            signalStrength = 4,
            rssi = -45,
            estimatedDistance = 3.5,
            linkSpeed = 150,
            frequency = 2412,
            isConnected = true
        )

        // 初始化延迟测试结果
        _latencyResult.value = LatencyResult(
            averageLatency = 24L,
            minLatency = 18L,
            maxLatency = 32L,
            packetLoss = 0f,
            quality = "优秀"
        )

        // 初始WiFi信号列表为空，等待实际扫描结果
        _wifiSignals.value = emptyList()

        // 初始化网络占用应用
        _networkUsageApps.value = listOf(
            NetworkUsageApp("微信", "com.tencent.mm", 45.2f, 120L),
            NetworkUsageApp("抖音", "com.ss.android.ugc.aweme", 32.8f, 95L),
            NetworkUsageApp("QQ", "com.tencent.mobileqq", 28.5f, 80L),
            NetworkUsageApp("淘宝", "com.taobao.taobao", 18.7f, 65L),
            NetworkUsageApp("微博", "com.sina.weibo", 15.3f, 50L)
        )

        // 初始化电池消耗应用
        _batteryConsumingApps.value = listOf(
            BatteryConsumingApp("抖音", "com.ss.android.ugc.aweme", 25.3f, 180L, true),
            BatteryConsumingApp("微信", "com.tencent.mm", 18.7f, 240L, false),
            BatteryConsumingApp("游戏应用", "com.game.example", 15.2f, 120L, true),
            BatteryConsumingApp("视频播放器", "com.video.player", 12.8f, 90L, false),
            BatteryConsumingApp("音乐播放器", "com.music.player", 8.5f, 60L, false)
        )

        _diagnosticStatus.value = "就绪"
    }

    /**
     * 网络信息数据类
     */
    data class NetworkInfo(
        val type: String = "Unknown",
        val ssid: String = "",
        val bssid: String = "",
        val signalStrength: Int = 0, // 1-5 等级
        val rssi: Int = 0, // 信号强度 dBm
        val estimatedDistance: Double = 0.0, // 预估距离 米
        val linkSpeed: Int = 0, // 连接速度 Mbps
        val frequency: Int = 0, // 频率 MHz
        val isConnected: Boolean = false
    )

    /**
     * 延迟测试结果数据类
     */
    data class LatencyResult(
        val averageLatency: Long = 0L,
        val minLatency: Long = 0L,
        val maxLatency: Long = 0L,
        val packetLoss: Float = 0f,
        val quality: String = "未知"
    )

    /**
     * WiFi信号数据类
     */
    data class WifiSignal(
        val ssid: String,
        val bssid: String,
        val rssi: Int,
        val signalLevel: Int, // 1-5 等级
        val isConnected: Boolean
    )

    /**
     * 网络占用应用数据类
     */
    data class NetworkUsageApp(
        val appName: String,
        val packageName: String,
        val usageMB: Float, // 网络使用量 MB
        val activeTime: Long // 活跃时间 分钟
    )

    /**
     * 电池消耗应用数据类
     */
    data class BatteryConsumingApp(
        val appName: String,
        val packageName: String,
        val consumptionPercent: Float, // 电池消耗百分比
        val runningTime: Long, // 运行时间 分钟
        val shouldClose: Boolean // 是否建议关闭
    )

    /**
     * 位置结果数据类
     */
    data class PositionResult(
        val position: String = "",
        val signalStrength: Int = 0,
        val rssi: Int = 0,
        val distance: Double = 0.0,
        val recommended: Boolean = false
    )

    /**
     * 位置扫描结果数据类
     */
    data class PositionScanResult(
        val positions: List<PositionResult> = emptyList(),
        val bestPosition: String = "",
        val recommendedAction: String = ""
    )

    /**
     * 更新网络信息
     */
    fun updateNetworkInfo(networkInfo: NetworkInfo) {
        _networkInfo.value = networkInfo
    }

    /**
     * 更新延迟测试结果
     */
    fun updateLatencyResult(result: LatencyResult) {
        _latencyResult.value = result
    }

    /**
     * 更新位置扫描结果
     */
    fun updatePositionScanResult(result: PositionScanResult) {
        _positionScanResult.value = result
    }

    /**
     * 更新WiFi信号列表
     */
    fun updateWifiSignals(signals: List<WifiSignal>) {
        _wifiSignals.value = signals
            .sortedWith(compareByDescending<WifiSignal> { it.isConnected }.thenByDescending { it.rssi })
    }

    /**
     * 更新网络占用应用列表
     */
    fun updateNetworkUsageApps(apps: List<NetworkUsageApp>) {
        _networkUsageApps.value = apps.sortedByDescending { it.usageMB }
    }

    /**
     * 更新电池消耗应用列表
     */
    fun updateBatteryConsumingApps(apps: List<BatteryConsumingApp>) {
        _batteryConsumingApps.value = apps.sortedByDescending { it.consumptionPercent }
    }

    /**
     * 更新诊断状态
     */
    fun updateDiagnosticStatus(status: String) {
        _diagnosticStatus.value = status
    }

    /**
     * 获取网络质量评分
     */
    fun getNetworkQualityScore(): Int {
        val networkInfo = _networkInfo.value ?: return 0
        val latencyResult = _latencyResult.value ?: return 0

        var score = 0

        // 信号强度评分 (0-25分)
        score += when (networkInfo.signalStrength) {
            5 -> 25
            4 -> 20
            3 -> 15
            2 -> 10
            1 -> 5
            else -> 0
        }

        // 延迟评分 (0-25分)
        score += when {
            latencyResult.averageLatency <= 50 -> 25
            latencyResult.averageLatency <= 100 -> 20
            latencyResult.averageLatency <= 200 -> 15
            latencyResult.averageLatency <= 500 -> 10
            else -> 5
        }

        // 连接速度评分 (0-25分)
        score += when {
            networkInfo.linkSpeed >= 300 -> 25
            networkInfo.linkSpeed >= 150 -> 20
            networkInfo.linkSpeed >= 75 -> 15
            networkInfo.linkSpeed >= 25 -> 10
            else -> 5
        }

        // 距离评分 (0-25分)
        score += when {
            networkInfo.estimatedDistance <= 5 -> 25
            networkInfo.estimatedDistance <= 10 -> 20
            networkInfo.estimatedDistance <= 20 -> 15
            networkInfo.estimatedDistance <= 50 -> 10
            else -> 5
        }

        return score.coerceIn(0, 100)
    }

    /**
     * 获取网络质量描述
     */
    fun getNetworkQualityDescription(): String {
        val score = getNetworkQualityScore()
        return when {
            score >= 90 -> "网络质量极佳，体验完美"
            score >= 80 -> "网络质量优秀，体验流畅"
            score >= 70 -> "网络质量良好，基本满足需求"
            score >= 60 -> "网络质量一般，可能有轻微延迟"
            score >= 50 -> "网络质量较差，建议优化"
            else -> "网络质量很差，强烈建议改善"
        }
    }

    /**
     * 获取需要强行关闭的应用
     */
    fun getAppsToForceClose(): List<BatteryConsumingApp> {
        return _batteryConsumingApps.value?.filter { it.shouldClose } ?: emptyList()
    }

    /**
     * 获取网络占用最多的应用
     */
    fun getTopNetworkUsers(): List<NetworkUsageApp> {
        return _networkUsageApps.value?.take(3) ?: emptyList()
    }

    /**
     * 获取信号最强的WiFi
     */
    fun getStrongestWifiSignal(): WifiSignal? {
        return _wifiSignals.value?.maxByOrNull { it.rssi }
    }

    /**
     * 获取优化建议
     */
    fun getOptimizationSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        val networkInfo = _networkInfo.value ?: return suggestions
        val latencyResult = _latencyResult.value ?: return suggestions
        val batteryApps = _batteryConsumingApps.value ?: emptyList()
        val networkApps = _networkUsageApps.value ?: emptyList()

        if (networkInfo.signalStrength < 3) {
            suggestions.add("📶 信号较弱，建议靠近路由器或减少障碍物")
        }

        if (latencyResult.averageLatency > 100) {
            suggestions.add("⏱️ 网络延迟较高，建议检查网络连接或更换DNS")
        }

        if (networkInfo.linkSpeed < 50) {
            suggestions.add("🐌 连接速度较慢，建议检查WiFi设置或升级网络")
        }

        if (networkInfo.estimatedDistance > 20) {
            suggestions.add("📍 距离路由器较远，建议移动到更近的位置")
        }

        if (networkInfo.frequency < 5000 && networkInfo.linkSpeed < 100) {
            suggestions.add("📡 使用2.4GHz频段但速度较慢，建议切换到5GHz频段")
        }

        // 检查需要关闭的应用
        val appsToClose = batteryApps.filter { it.shouldClose }
        if (appsToClose.isNotEmpty()) {
            suggestions.add("🔋 发现 ${appsToClose.size} 个耗电应用，建议强行关闭以节省电池")
        }

        // 检查网络占用过多的应用
        val highUsageApps = networkApps.filter { it.usageMB > 50 }
        if (highUsageApps.isNotEmpty()) {
            suggestions.add("🌐 发现 ${highUsageApps.size} 个网络占用严重的应用")
        }

        if (suggestions.isEmpty()) {
            suggestions.add("✅ 网络状况良好，无需额外优化")
        }

        return suggestions
    }

    /**
     * 模拟网络优化
     */
    fun simulateNetworkOptimization(): Map<String, String> {
        val optimizations = mutableMapOf<String, String>()

        optimizations["DNS优化"] = "已切换到更快的DNS服务器"
        optimizations["连接池调整"] = "已增加网络连接并发数"
        optimizations["缓存策略"] = "已优化网络请求缓存"
        optimizations["压缩传输"] = "已启用数据压缩传输"
        optimizations["错误重试"] = "已优化网络错误处理"
        optimizations["电池优化"] = "已限制后台网络访问"
        optimizations["应用管理"] = "已优化应用网络权限"

        return optimizations
    }

    /**
     * 执行延迟测试
     */
    fun performLatencyTest(): LatencyResult {
        // 模拟延迟测试
        val latencies = listOf(22L, 25L, 18L, 30L, 24L)
        val average = latencies.average().toLong()
        val min = latencies.minOrNull() ?: 0L
        val max = latencies.maxOrNull() ?: 0L

        val result = LatencyResult(
            averageLatency = average,
            minLatency = min,
            maxLatency = max,
            packetLoss = 0.2f,
            quality = when {
                average <= 50 -> "优秀"
                average <= 100 -> "良好"
                average <= 200 -> "一般"
                else -> "较差"
            }
        )

        _latencyResult.value = result
        return result
    }

    /**
     * 扫描WiFi位置
     */
    fun scanWifiPositions(): PositionScanResult {
        val positions = listOf(
            PositionResult("客厅", 4, -45, 3.5, true),
            PositionResult("卧室", 3, -60, 8.2, false),
            PositionResult("厨房", 2, -75, 12.8, false),
            PositionResult("阳台", 1, -85, 18.5, false)
        )

        val bestPosition = positions.maxByOrNull { it.signalStrength }?.position ?: ""
        val recommendedAction = "建议移动到 $bestPosition 位置获得最佳信号"

        val result = PositionScanResult(positions, bestPosition, recommendedAction)
        _positionScanResult.value = result
        return result
    }

    /**
     * 重置所有数据
     */
    fun resetData() {
        _networkInfo.value = NetworkInfo(type = "Unknown", isConnected = false)
        _latencyResult.value = LatencyResult(quality = "未测试")
        _positionScanResult.value = PositionScanResult()
        _wifiSignals.value = emptyList()
        _networkUsageApps.value = emptyList()
        _batteryConsumingApps.value = emptyList()
        _diagnosticStatus.value = "已重置"
    }
}
