package com.lanhe.gongjuxiang.services

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.WifiSettingsActivity

/**
 * 网络监控服务
 *
 * 功能:
 * 1. 监听WiFi连接状态变化
 * 2. 检测WiFi信号强度
 * 3. 网络切换时发送通知
 * 4. 提示用户进入WiFi管理
 */
class NetworkMonitorService : BaseLifecycleService() {

    companion object {
        private const val TAG = "NetworkMonitorService"
        const val ACTION_WIFI_CHANGED = "com.lanhe.gongjuxiang.WIFI_CHANGED"
    }

    override fun getServiceTag(): String = TAG

    private var wifiManager: WifiManager? = null
    private var connectivityManager: ConnectivityManager? = null

    // 网络状态接收器
    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiManager.NETWORK_STATE_CHANGED_ACTION,
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    checkNetworkStatus()
                }
                WifiManager.RSSI_CHANGED_ACTION -> {
                    checkWifiSignalStrength()
                }
            }
        }
    }

    override suspend fun onInitialize(): Boolean {
        wifiManager = getSystemService(Context.WIFI_SERVICE) as? WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        // 注册网络监听
        registerNetworkReceiver()

        Log.d(TAG, "Network monitor service created")
        return true
    }

    override fun onCleanup() {
        try {
            unregisterReceiver(networkReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    private fun registerNetworkReceiver() {
        val filter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
        }
        registerReceiverSafely(networkReceiver, filter)
    }

    /**
     * 检查网络状态
     */
    private fun checkNetworkStatus() {
        val isWifiConnected = isWifiConnected()
        val isMobileConnected = isMobileDataConnected()

        when {
            isWifiConnected && !wasWifiConnected() -> {
                // WiFi刚连接
                onWifiConnected()
            }
            !isWifiConnected && wasWifiConnected() -> {
                // WiFi断开
                onWifiDisconnected()
            }
            isMobileConnected && !isWifiConnected -> {
                // 切换到移动网络
                onSwitchToMobileData()
            }
        }

        saveWifiState(isWifiConnected)
    }

    /**
     * WiFi连接时的处理
     */
    private fun onWifiConnected() {
        val wifiInfo = wifiManager?.connectionInfo
        val ssid = wifiInfo?.ssid?.replace("\"", "") ?: "未知网络"
        val signalLevel = getWifiSignalLevel()

        Log.d(TAG, "WiFi connected: $ssid, signal: $signalLevel")

        // 发送通知
        sendWifiNotification(
            title = "已连接WiFi",
            message = "网络: $ssid\n信号强度: ${getSignalDescription(signalLevel)}",
            showManageButton = signalLevel <= 2 // 信号弱时显示管理按钮
        )

        // 广播WiFi变化
        sendBroadcast(Intent(ACTION_WIFI_CHANGED).apply {
            putExtra("connected", true)
            putExtra("ssid", ssid)
            putExtra("signal_level", signalLevel)
        })
    }

    /**
     * WiFi断开时的处理
     */
    private fun onWifiDisconnected() {
        Log.d(TAG, "WiFi disconnected")

        sendWifiNotification(
            title = "WiFi已断开",
            message = "点击进入WiFi管理重新连接",
            showManageButton = true
        )
    }

    /**
     * 切换到移动数据时的处理
     */
    private fun onSwitchToMobileData() {
        Log.d(TAG, "Switched to mobile data")

        sendWifiNotification(
            title = "正在使用移动数据",
            message = "点击连接WiFi节省流量",
            showManageButton = true
        )
    }

    /**
     * 检查WiFi信号强度
     */
    private fun checkWifiSignalStrength() {
        if (!isWifiConnected()) return

        val signalLevel = getWifiSignalLevel()

        // 信号很弱时提醒用户
        if (signalLevel == 0) {
            sendWifiNotification(
                title = "WiFi信号很弱",
                message = "当前信号质量较差,建议切换网络或移动位置",
                showManageButton = true
            )
        }
    }

    /**
     * 获取WiFi信号等级 (0-4)
     */
    private fun getWifiSignalLevel(): Int {
        val wifiInfo = wifiManager?.connectionInfo ?: return 0
        val rssi = wifiInfo.rssi

        return when {
            rssi >= -50 -> 4  // 优秀
            rssi >= -60 -> 3  // 良好
            rssi >= -70 -> 2  // 一般
            rssi >= -80 -> 1  // 较弱
            else -> 0         // 很弱
        }
    }

    /**
     * 获取信号强度描述
     */
    private fun getSignalDescription(level: Int): String {
        return when (level) {
            4 -> "优秀 ★★★★★"
            3 -> "良好 ★★★★☆"
            2 -> "一般 ★★★☆☆"
            1 -> "较弱 ★★☆☆☆"
            0 -> "很弱 ★☆☆☆☆"
            else -> "未知"
        }
    }

    /**
     * 判断WiFi是否连接
     */
    private fun isWifiConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager?.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager?.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }

    /**
     * 判断移动网络是否连接
     */
    private fun isMobileDataConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager?.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager?.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_MOBILE && networkInfo.isConnected
        }
    }

    /**
     * 发送WiFi通知
     */
    private fun sendWifiNotification(title: String, message: String, showManageButton: Boolean) {
        // 创建通知
        val builder = NotificationCompat.Builder(this, "network_monitor")
            .setSmallIcon(R.drawable.ic_wifi)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (showManageButton) {
            // 添加"WiFi管理"操作按钮
            val manageIntent = Intent(this, WifiSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val managePendingIntent = PendingIntent.getActivity(
                this,
                0,
                manageIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(managePendingIntent)
            builder.addAction(R.drawable.ic_wifi, "WiFi管理", managePendingIntent)
        }

        // TODO: 使用NotificationManager发送通知
        // 这里需要先创建通知渠道和NotificationManager实例
    }

    /**
     * 保存WiFi状态
     */
    private fun saveWifiState(connected: Boolean) {
        getSharedPreferences("network_monitor", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("wifi_connected", connected)
            .apply()
    }

    /**
     * 检查上次WiFi状态
     */
    private fun wasWifiConnected(): Boolean {
        return getSharedPreferences("network_monitor", Context.MODE_PRIVATE)
            .getBoolean("wifi_connected", false)
    }

    override fun onBind(intent: Intent?) = null
}
