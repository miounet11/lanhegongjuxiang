package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import kotlin.math.pow

/**
 * 网络信息助手类
 * 处理网络信息的获取和计算
 */
class NetworkInfoHelper(private val context: Context) {

    data class NetworkInfo(
        val type: String,
        val ssid: String,
        val bssid: String,
        val signalStrength: Int,
        val rssi: Int,
        val estimatedDistance: Double,
        val linkSpeed: Int,
        val frequency: Int,
        val isConnected: Boolean
    )

    /**
     * 获取当前网络信息
     */
    fun getCurrentNetworkInfo(): NetworkInfoHelper.NetworkInfo {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo

            val isWifi = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isMobile = networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

            val networkType = when {
                isWifi -> "Wi-Fi"
                isMobile -> "移动网络"
                else -> "未知"
            }

            val signalStrength = if (isWifi) {
                WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
            } else {
                -1
            }

            val estimatedDistance = if (isWifi && wifiInfo.rssi != -1) {
                calculateDistanceFromSignal(wifiInfo.rssi)
            } else {
                0.0
            }

            return NetworkInfo(
                type = networkType,
                ssid = wifiInfo.ssid?.replace("\"", "") ?: "未知",
                bssid = wifiInfo.bssid ?: "未知",
                signalStrength = signalStrength,
                rssi = wifiInfo.rssi,
                estimatedDistance = estimatedDistance,
                linkSpeed = wifiInfo.linkSpeed,
                frequency = wifiInfo.frequency,
                isConnected = wifiInfo.networkId != -1
            )
        } catch (e: Exception) {
            return NetworkInfoHelper.NetworkInfo(type = "未知", ssid = "", bssid = "", signalStrength = 0, rssi = 0, estimatedDistance = 0.0, linkSpeed = 0, frequency = 0, isConnected = false)
        }
    }

    /**
     * 根据信号强度计算距离
     */
    private fun calculateDistanceFromSignal(rssi: Int): Double {
        if (rssi == -1) return 0.0

        // 使用信号传播模型估算距离
        val rssiAtOneMeter = -40.0
        val pathLossExponent = 3.0

        val distance = 10.0.pow((rssiAtOneMeter - rssi) / (10.0 * pathLossExponent))
        return String.format("%.1f", distance).toDouble()
    }

    /**
     * 获取网络质量评分
     */
    fun getNetworkQualityScore(networkInfo: NetworkInfo): Int {
        var score = 0

        // 信号强度评分 (0-40分)
        score += when (networkInfo.signalStrength) {
            5 -> 40
            4 -> 35
            3 -> 25
            2 -> 15
            1 -> 5
            else -> 0
        }

        // 连接状态评分 (0-30分)
        if (networkInfo.isConnected) {
            score += 30
        }

        // 距离评分 (0-30分)
        score += when {
            networkInfo.estimatedDistance <= 5 -> 30
            networkInfo.estimatedDistance <= 10 -> 20
            networkInfo.estimatedDistance <= 20 -> 10
            else -> 0
        }

        return score
    }

    /**
     * 获取网络质量描述
     */
    fun getNetworkQualityDescription(score: Int): String {
        return when {
            score >= 80 -> "优秀"
            score >= 60 -> "良好"
            score >= 40 -> "一般"
            score >= 20 -> "较差"
            else -> "很差"
        }
    }
}
