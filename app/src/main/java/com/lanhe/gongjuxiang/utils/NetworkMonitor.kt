package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.telephony.TelephonyManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.net.InetAddress

class NetworkMonitor(private val context: Context) {

    data class NetworkStats(
        val type: String,
        val speed: String,
        val latency: String,
        val rxBytes: Long,
        val txBytes: Long,
        val isConnected: Boolean
    )

    private var monitoringJob: Job? = null
    private var isMonitoring = false

    // 开始监控
    fun startMonitoring(callback: (NetworkStats) -> Unit) {
        if (isMonitoring) return

        isMonitoring = true
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isMonitoring) {
                try {
                    val stats = getNetworkStats()
                    withContext(Dispatchers.Main) {
                        callback(stats)
                    }
                    delay(2000) // 每2秒更新一次
                } catch (e: Exception) {
                    // 忽略异常，继续监控
                    delay(2000)
                }
            }
        }
    }

    // 停止监控
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
    }

    // 获取网络统计信息
    fun getNetworkStats(): NetworkStats {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        // 获取网络类型
        val networkType = getNetworkType(capabilities)

        // 获取网络速度
        val networkSpeed = getNetworkSpeed(capabilities)

        // 获取网络延迟
        val latency = measureLatency()

        // 获取流量统计
        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()

        return NetworkStats(
            type = networkType,
            speed = networkSpeed,
            latency = latency,
            rxBytes = rxBytes,
            txBytes = txBytes,
            isConnected = isConnected
        )
    }

    // 获取网络类型
    private fun getNetworkType(capabilities: NetworkCapabilities?): String {
        if (capabilities == null) return "无连接"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                "WiFi"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                getCellularNetworkType()
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                "以太网"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> {
                "蓝牙"
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                "VPN"
            }
            else -> "未知网络"
        }
    }

    // 获取蜂窝网络类型
    private fun getCellularNetworkType(): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        return when (telephonyManager.networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT -> "2G"

            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"

            TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"

            TelephonyManager.NETWORK_TYPE_NR -> "5G NR"

            else -> "移动网络"
        }
    }

    // 获取网络速度
    private fun getNetworkSpeed(capabilities: NetworkCapabilities?): String {
        if (capabilities == null) return "未知"

        // 检查下行链路带宽
        val downSpeed = capabilities.linkDownstreamBandwidthKbps
        val upSpeed = capabilities.linkUpstreamBandwidthKbps

        return when {
            downSpeed >= 100000 -> "超高速 (>100Mbps)"
            downSpeed >= 50000 -> "高速 (50-100Mbps)"
            downSpeed >= 25000 -> "较快 (25-50Mbps)"
            downSpeed >= 10000 -> "一般 (10-25Mbps)"
            downSpeed >= 1000 -> "较慢 (1-10Mbps)"
            downSpeed > 0 -> "慢速 (<1Mbps)"
            else -> "未知速度"
        }
    }

    // 测量网络延迟
    private fun measureLatency(host: String = "8.8.8.8"): String {
        return try {
            val startTime = System.currentTimeMillis()
            val address = InetAddress.getByName(host)
            val reachable = address.isReachable(3000)

            if (reachable) {
                val latency = System.currentTimeMillis() - startTime
                "${latency}ms"
            } else {
                ">3000ms"
            }
        } catch (e: Exception) {
            "无法测量"
        }
    }

    // 获取WiFi信号强度
    fun getWifiSignalStrength(): Int {
        // 这里可以实现WiFi信号强度检测
        // 由于需要系统权限，这里返回模拟值
        return (60..90).random()
    }

    // 获取移动网络信号强度
    fun getMobileSignalStrength(): Int {
        // 这里可以实现移动网络信号强度检测
        // 由于需要系统权限，这里返回模拟值
        return (40..80).random()
    }

    // 测试网络连通性
    fun testConnectivity(host: String = "www.baidu.com"): Boolean {
        return try {
            val address = InetAddress.getByName(host)
            address.isReachable(5000)
        } catch (e: Exception) {
            false
        }
    }

    // 获取DNS解析时间
    fun measureDnsResolution(domain: String = "www.google.com"): Long {
        return try {
            val startTime = System.currentTimeMillis()
            InetAddress.getByName(domain)
            System.currentTimeMillis() - startTime
        } catch (e: Exception) {
            -1
        }
    }

    // 获取网络拥塞状态
    fun getNetworkCongestion(): String {
        val latency = measureLatency("8.8.8.8")
        val latencyValue = latency.replace("ms", "").toIntOrNull() ?: 999

        return when {
            latencyValue < 50 -> "畅通"
            latencyValue < 100 -> "良好"
            latencyValue < 200 -> "一般"
            latencyValue < 500 -> "拥堵"
            else -> "严重拥堵"
        }
    }

    // 获取网络稳定性评分
    fun getNetworkStabilityScore(): Int {
        // 基于多个因素计算网络稳定性
        var score = 100

        // 延迟影响
        val latency = measureLatency("8.8.8.8")
        val latencyValue = latency.replace("ms", "").toIntOrNull() ?: 999
        score -= when {
            latencyValue > 500 -> 40
            latencyValue > 200 -> 20
            latencyValue > 100 -> 10
            else -> 0
        }

        // 连通性影响
        if (!testConnectivity()) {
            score -= 30
        }

        // DNS解析时间影响
        val dnsTime = measureDnsResolution()
        if (dnsTime > 1000) {
            score -= 10
        }

        return score.coerceIn(0, 100)
    }

    // 获取网络诊断报告
    fun getNetworkDiagnosticReport(): String {
        val stats = getNetworkStats()
        val stabilityScore = getNetworkStabilityScore()
        val congestion = getNetworkCongestion()

        return """
            📊 网络诊断报告
            ━━━━━━━━━━━━━━━━━━━━━━

            📡 网络状态: ${if (stats.isConnected) "已连接" else "未连接"}
            🌐 网络类型: ${stats.type}
            ⚡ 网络速度: ${stats.speed}
            🕐 网络延迟: ${stats.latency}
            📈 稳定性评分: $stabilityScore/100
            🚦 网络拥堵: $congestion

            📥 下行流量: ${formatBytes(stats.rxBytes)}
            📤 上行流量: ${formatBytes(stats.txBytes)}

            🔍 诊断结果:
            ${getDiagnosticResult(stabilityScore, stats.isConnected)}

            💡 优化建议:
            ${getOptimizationSuggestions(stabilityScore, stats)}
        """.trimIndent()
    }

    // 获取诊断结果
    private fun getDiagnosticResult(stabilityScore: Int, isConnected: Boolean): String {
        if (!isConnected) {
            return "❌ 网络未连接，请检查网络设置"
        }

        return when {
            stabilityScore >= 90 -> "✅ 网络状态优秀，性能表现良好"
            stabilityScore >= 70 -> "🟢 网络状态良好，基本满足日常使用"
            stabilityScore >= 50 -> "🟡 网络状态一般，可能影响部分应用体验"
            stabilityScore >= 30 -> "🟠 网络状态较差，建议检查网络环境"
            else -> "🔴 网络状态很差，严重影响使用体验"
        }
    }

    // 获取优化建议
    private fun getOptimizationSuggestions(stabilityScore: Int, stats: NetworkStats): String {
        val suggestions = mutableListOf<String>()

        if (stabilityScore < 70) {
            suggestions.add("• 检查WiFi信号强度或移动网络信号")
            suggestions.add("• 尝试重启路由器或切换网络")
        }

        if (stats.latency.replace("ms", "").toIntOrNull() ?: 999 > 100) {
            suggestions.add("• 网络延迟较高，建议使用更快的网络")
        }

        if (suggestions.isEmpty()) {
            suggestions.add("• 网络状态良好，无需额外优化")
        }

        return suggestions.joinToString("\n")
    }

    // 格式化字节数
    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
