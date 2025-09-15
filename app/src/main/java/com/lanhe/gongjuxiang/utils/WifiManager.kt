package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.lanhe.gongjuxiang.utils.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 增强WiFi管理器
 * 提供WiFi密码查看、连接管理和历史记录功能
 */
class WifiManager(private val context: Context) {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val shizukuManager = ShizukuManager(context)

    /**
     * WiFi网络信息数据类
     */
    data class WifiNetworkInfo(
        val ssid: String,
        val bssid: String,
        val capabilities: String,
        val frequency: Int,
        val level: Int,
        val password: String? = null,
        val isConfigured: Boolean = false,
        val lastConnected: Long = 0L
    )

    /**
     * WiFi连接历史记录
     */
    data class WifiConnectionHistory(
        val ssid: String,
        val bssid: String,
        val connectedTime: Long,
        val disconnectedTime: Long,
        val duration: Long,
        val dataUsage: Long = 0L
    )

    /**
     * 获取已保存的WiFi网络列表
     */
    fun getConfiguredNetworks(): List<WifiNetworkInfo> {
        return try {
            val configuredNetworks = wifiManager.configuredNetworks ?: emptyList()
            configuredNetworks.map { config ->
                WifiNetworkInfo(
                    ssid = config.SSID?.replace("\"", "") ?: "Unknown",
                    bssid = "Configured",
                    capabilities = config.allowedKeyManagement.toString(),
                    frequency = 0,
                    level = 0,
                    isConfigured = true
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 获取WiFi密码（需要Shizuku权限）
     */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getWifiPassword(ssid: String): String? = withContext(Dispatchers.IO) {
        try {
            if (!shizukuManager.isShizukuAvailable()) {
                return@withContext null
            }

            // 使用Shizuku执行系统命令获取WiFi密码
            val command = "cat /data/misc/wifi/WifiConfigStore.xml | grep -A 5 '$ssid' | grep '<string name=\"PreSharedKey\">' | sed -n 's/.*<string name=\"PreSharedKey\">\\(.*\\)<\\/string>.*/\\1/p'"
            val result = shizukuManager.executeCommand(command)

            if (result.isSuccess) {
                result.output?.trim()?.removeSurrounding("\"")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 智能WiFi连接器
     */
    fun connectToWifi(ssid: String, password: String? = null): Boolean {
        return try {
            val config = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                if (password != null) {
                    preSharedKey = "\"$password\""
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                    allowedProtocols.set(WifiConfiguration.Protocol.RSN)
                    allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                    allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
                    allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                    allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                    allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                    allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
                    allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
                } else {
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                }
            }

            val networkId = wifiManager.addNetwork(config)
            if (networkId != -1) {
                wifiManager.disconnect()
                wifiManager.enableNetwork(networkId, true)
                wifiManager.reconnect()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取WiFi连接历史记录
     */
    fun getConnectionHistory(): List<WifiConnectionHistory> {
        // 这里可以从数据库或SharedPreferences中读取历史记录
        // 暂时返回空列表，实际实现需要数据库存储
        return emptyList()
    }

    /**
     * 保存WiFi连接历史
     */
    fun saveConnectionHistory(ssid: String, bssid: String, connectedTime: Long, disconnectedTime: Long) {
        // 保存到数据库或SharedPreferences
        // 实际实现需要数据库存储
    }

    /**
     * 获取WiFi信号强度详情
     */
    fun getSignalStrengthDetails(): Map<String, Any> {
        return try {
            val wifiInfo = wifiManager.connectionInfo
            mapOf(
                "ssid" to (wifiInfo.ssid?.replace("\"", "") ?: "Unknown"),
                "bssid" to (wifiInfo.bssid ?: "Unknown"),
                "rssi" to wifiInfo.rssi,
                "linkSpeed" to wifiInfo.linkSpeed,
                "frequency" to wifiInfo.frequency,
                "signalLevel" to calculateSignalLevel(wifiInfo.rssi),
                "quality" to calculateSignalQuality(wifiInfo.rssi)
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 计算信号等级
     */
    private fun calculateSignalLevel(rssi: Int): String {
        return when {
            rssi >= -50 -> "优秀"
            rssi >= -60 -> "良好"
            rssi >= -70 -> "一般"
            rssi >= -80 -> "较差"
            else -> "很差"
        }
    }

    /**
     * 计算信号质量百分比
     */
    private fun calculateSignalQuality(rssi: Int): Int {
        return when {
            rssi >= -50 -> 100
            rssi >= -60 -> 75
            rssi >= -70 -> 50
            rssi >= -80 -> 25
            else -> 0
        }
    }

    /**
     * 扫描可用的WiFi网络
     */
    fun scanAvailableNetworks(): List<ScanResult> {
        return try {
            wifiManager.startScan()
            wifiManager.scanResults ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 忘记WiFi网络
     */
    fun forgetNetwork(ssid: String): Boolean {
        return try {
            val configuredNetworks = wifiManager.configuredNetworks
            val network = configuredNetworks?.find { it.SSID?.replace("\"", "") == ssid }
            if (network != null) {
                wifiManager.removeNetwork(network.networkId)
                wifiManager.saveConfiguration()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取WiFi安全类型
     */
    fun getSecurityType(capabilities: String): String {
        return when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA") -> "WPA"
            capabilities.contains("WEP") -> "WEP"
            else -> "开放网络"
        }
    }

    /**
     * 检查WiFi是否可用
     */
    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    /**
     * 启用/禁用WiFi
     */
    fun setWifiEnabled(enabled: Boolean): Boolean {
        return wifiManager.setWifiEnabled(enabled)
    }

    /**
     * 获取当前连接的WiFi信息
     */
    fun getCurrentConnectionInfo(): WifiNetworkInfo? {
        return try {
            val wifiInfo = wifiManager.connectionInfo
            if (wifiInfo.networkId == -1) return null

            WifiNetworkInfo(
                ssid = wifiInfo.ssid?.replace("\"", "") ?: "Unknown",
                bssid = wifiInfo.bssid ?: "Unknown",
                capabilities = "Connected",
                frequency = wifiInfo.frequency,
                level = wifiInfo.rssi,
                isConfigured = true,
                lastConnected = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}
