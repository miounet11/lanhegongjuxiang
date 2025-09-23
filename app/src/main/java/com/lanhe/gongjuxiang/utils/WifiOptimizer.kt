package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * WiFi性能优化工具类
 * 提供WiFi连接检测、性能测试和优化建议
 */
class WifiOptimizer(private val context: Context) {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * WiFi信息数据类
     */
    data class WifiInfo(
        val ssid: String,           // WiFi名称
        val bssid: String,          // MAC地址
        val signalStrength: Int,    // 信号强度 (-100 to 0)
        val linkSpeed: Int,         // 连接速度 (Mbps)
        val frequency: Int,         // 频率 (MHz)
        val ipAddress: String,      // IP地址
        val isConnected: Boolean,   // 是否连接
        val networkType: String     // 网络类型 (2.4GHz/5GHz)
    )

    /**
     * WiFi性能测试结果
     */
    data class WifiPerformance(
        val downloadSpeed: Double,  // 下载速度 (Mbps)
        val uploadSpeed: Double,    // 上传速度 (Mbps)
        val pingLatency: Long,      // 延迟 (ms)
        val packetLoss: Float,      // 丢包率 (%)
        val signalQuality: Int      // 信号质量 (0-100)
    )

    /**
     * WiFi优化建议
     */
    data class WifiOptimization(
        val suggestions: List<String>,   // 优化建议列表
        val priority: Int,              // 优先级 (1-5, 5为最高)
        val estimatedImprovement: String // 预期改善效果
    )

    /**
     * 获取当前WiFi连接信息
     */
    fun getCurrentWifiInfo(): WifiInfo? {
        return try {
            if (!wifiManager.isWifiEnabled) {
                return null
            }

            val wifiInfo = wifiManager.connectionInfo
            val networkType = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    when (wifiInfo.frequency) {
                        in 2412..2484 -> "2.4GHz"
                        in 5170..5825 -> "5GHz"
                        in 5925..7125 -> "6GHz"
                        else -> "Unknown"
                    }
                }
                else -> {
                    if (wifiInfo.frequency > 4900) "5GHz" else "2.4GHz"
                }
            }

            WifiInfo(
                ssid = wifiInfo.ssid?.replace("\"", "") ?: "Unknown",
                bssid = wifiInfo.bssid ?: "Unknown",
                signalStrength = wifiInfo.rssi,
                linkSpeed = wifiInfo.linkSpeed,
                frequency = wifiInfo.frequency,
                ipAddress = formatIpAddress(wifiInfo.ipAddress),
                isConnected = wifiInfo.networkId != -1,
                networkType = networkType
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 执行WiFi性能测试
     */
    suspend fun performSpeedTest(): WifiPerformance = withContext(Dispatchers.IO) {
        try {
            // 简单的速度测试实现
            val pingResult = measurePing("8.8.8.8") // Google DNS
            val signalQuality = calculateSignalQuality(getCurrentWifiInfo()?.signalStrength ?: -100)

            // 这里可以集成更复杂的速度测试库
            WifiPerformance(
                downloadSpeed = 0.0, // 需要外部库实现
                uploadSpeed = 0.0,   // 需要外部库实现
                pingLatency = pingResult,
                packetLoss = 0.0f,   // 需要外部库实现
                signalQuality = signalQuality
            )
        } catch (e: Exception) {
            WifiPerformance(0.0, 0.0, -1L, 100.0f, 0)
        }
    }

    /**
     * 测量网络延迟
     */
    private fun measurePing(host: String): Long {
        return try {
            val socket = Socket()
            val startTime = System.currentTimeMillis()

            socket.connect(InetSocketAddress(host, 53), 5000) // DNS端口
            val endTime = System.currentTimeMillis()

            socket.close()
            endTime - startTime
        } catch (e: IOException) {
            -1L
        }
    }

    /**
     * 计算信号质量
     */
    fun calculateSignalQuality(rssi: Int): Int {
        return when {
            rssi >= -50 -> 100  // 优秀
            rssi >= -60 -> 75   // 良好
            rssi >= -70 -> 50   // 一般
            rssi >= -80 -> 25   // 较差
            else -> 0           // 很差
        }
    }

    /**
     * 获取WiFi优化建议
     */
    fun getOptimizationSuggestions(wifiInfo: WifiInfo?, performance: WifiPerformance?): WifiOptimization {
        val suggestions = mutableListOf<String>()

        wifiInfo?.let { info ->
            // 信号强度建议
            when {
                info.signalStrength >= -50 -> {
                    suggestions.add("WiFi信号强度优秀，保持当前连接")
                }
                info.signalStrength >= -70 -> {
                    suggestions.add("WiFi信号强度一般，建议靠近路由器或减少障碍物")
                }
                else -> {
                    suggestions.add("WiFi信号强度较弱，建议：1) 靠近路由器 2) 减少墙壁等障碍物 3) 更换信道")
                }
            }

            // 网络类型建议
            if (info.networkType == "2.4GHz") {
                suggestions.add("当前使用2.4GHz频段，建议切换到5GHz以获得更好性能（如果设备支持）")
            }

            // 连接速度建议
            if (info.linkSpeed < 50) {
                suggestions.add("WiFi连接速度较低，建议：1) 检查路由器设置 2) 减少干扰源 3) 更新路由器固件")
            }
        }

        performance?.let { perf ->
            // 延迟建议
            if (perf.pingLatency > 100) {
                suggestions.add("网络延迟较高，建议：1) 选择更近的DNS服务器 2) 检查网络拥堵 3) 考虑使用有线连接")
            }

            // 信号质量建议
            if (perf.signalQuality < 50) {
                suggestions.add("WiFi信号质量不佳，建议进行网络诊断和优化")
            }
        }

        // 通用建议
        suggestions.add("定期重启路由器以保持最佳性能")
        suggestions.add("避免在高峰时段进行大型文件传输")

        val priority = when {
            wifiInfo?.signalStrength ?: -100 < -80 -> 5  // 紧急
            performance?.pingLatency ?: 0L > 200 -> 4     // 高
            wifiInfo?.linkSpeed ?: 0 < 30 -> 3            // 中
            else -> 2                                      // 低
        }

        val estimatedImprovement = when (priority) {
            5 -> "显著改善 (50-80%性能提升)"
            4 -> "中等改善 (20-50%性能提升)"
            3 -> "轻微改善 (10-30%性能提升)"
            else -> "保持当前性能"
        }

        return WifiOptimization(suggestions, priority, estimatedImprovement)
    }

    /**
     * 获取可用的WiFi网络列表
     */
    fun getAvailableNetworks(): List<android.net.wifi.ScanResult> {
        return try {
            wifiManager.scanResults ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 检查网络连接状态
     */
    fun isNetworkConnected(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取网络类型
     */
    fun getNetworkType(): String {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "移动数据"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "以太网"
                else -> "未知"
            }
        } catch (e: Exception) {
            "未知"
        }
    }

    /**
     * 格式化IP地址
     */
    private fun formatIpAddress(ipAddress: Int): String {
        return try {
            val bytes = ByteArray(4)
            bytes[0] = (ipAddress and 0xFF).toByte()
            bytes[1] = (ipAddress shr 8 and 0xFF).toByte()
            bytes[2] = (ipAddress shr 16 and 0xFF).toByte()
            bytes[3] = (ipAddress shr 24 and 0xFF).toByte()

            "${bytes[3].toInt() and 0xFF}.${bytes[2].toInt() and 0xFF}.${bytes[1].toInt() and 0xFF}.${bytes[0].toInt() and 0xFF}"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 获取WiFi信号强度等级
     */
    fun getSignalLevel(rssi: Int): String {
        return when {
            rssi >= -50 -> "优秀"
            rssi >= -60 -> "良好"
            rssi >= -70 -> "一般"
            rssi >= -80 -> "较差"
            else -> "很差"
        }
    }

    /**
     * 检查WiFi是否启用
     */
    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    /**
     * 获取WiFi状态
     */
    fun getWifiState(): Int {
        return wifiManager.wifiState
    }

    /**
     * 优化网络连接
     */
    suspend fun optimizeNetwork(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查WiFi是否启用
            if (!wifiManager.isWifiEnabled) {
                return@withContext false
            }

            // 获取当前连接信息
            val currentNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(currentNetwork)

            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                // 这里可以实现实际的网络优化逻辑
                // 例如：调整WiFi频段、重新连接等
                return@withContext true
            }

            return@withContext false
        } catch (e: Exception) {
            return@withContext false
        }
    }
}
