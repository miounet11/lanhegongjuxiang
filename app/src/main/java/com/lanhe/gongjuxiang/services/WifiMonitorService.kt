package com.lanhe.gongjuxiang.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.utils.WifiOptimizer
import kotlinx.coroutines.*

/**
 * WiFi监控服务
 * 监控WiFi连接状态并提供智能提醒
 */
class WifiMonitorService : Service() {

    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiOptimizer: WifiOptimizer
    private lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false
    private var lastWifiState = false
    private var lastSignalStrength = -100

    // WiFi状态监控
    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> handleWifiStateChanged(intent)
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> handleNetworkStateChanged(intent)
                WifiManager.RSSI_CHANGED_ACTION -> handleRssiChanged(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initializeComponents()
        registerReceivers()
        createNotificationChannel()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationHelper.createServiceNotification()
        startForeground(NOTIFICATION_ID_SERVICE, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiReceiver)
        serviceScope.cancel()
        stopMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeComponents() {
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiOptimizer = WifiOptimizer(this)
        notificationHelper = NotificationHelper(this)
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
        }
        registerReceiver(wifiReceiver, filter)
    }

    private fun createNotificationChannel() {
        notificationHelper.createNotificationChannel()
    }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        // 初始化当前状态
        lastWifiState = wifiOptimizer.isNetworkConnected() && wifiOptimizer.getNetworkType() == "WiFi"
        wifiOptimizer.getCurrentWifiInfo()?.let { info ->
            lastSignalStrength = info.signalStrength
        }

        serviceScope.launch {
            while (isMonitoring && isActive) {
                try {
                    checkWifiStatus()
                    delay(MONITORING_INTERVAL)
                } catch (e: Exception) {
                    // 忽略异常，继续监控
                }
            }
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
    }

    private fun handleWifiStateChanged(intent: Intent?) {
        val wifiState = intent?.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
        when (wifiState) {
            WifiManager.WIFI_STATE_ENABLED -> {
                // WiFi已启用
            }
            WifiManager.WIFI_STATE_DISABLED -> {
                // WiFi已禁用
                if (lastWifiState) {
                    notificationHelper.showNotification(
                        "WiFi已断开",
                        "网络连接已断开，请检查WiFi设置",
                        NotificationType.WIFI_DISCONNECTED
                    )
                    lastWifiState = false
                }
            }
        }
    }

    private fun handleNetworkStateChanged(intent: Intent?) {
        val networkInfo = intent?.getParcelableExtra<android.net.NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
        val isConnected = networkInfo?.isConnected == true

        if (isConnected != lastWifiState) {
            if (isConnected) {
                // WiFi已连接
                wifiOptimizer.getCurrentWifiInfo()?.let { info ->
                    notificationHelper.showNotification(
                        "WiFi已连接",
                        "已连接到 ${info.ssid}，信号强度：${wifiOptimizer.getSignalLevel(info.signalStrength)}",
                        NotificationType.WIFI_CONNECTED
                    )
                    lastWifiState = true
                    lastSignalStrength = info.signalStrength
                }
            } else {
                // WiFi已断开
                notificationHelper.showNotification(
                    "WiFi连接断开",
                    "网络连接已丢失",
                    NotificationType.WIFI_DISCONNECTED
                )
                lastWifiState = false
            }
        }
    }

    private fun handleRssiChanged(intent: Intent?) {
        val newRssi = intent?.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -100) ?: -100

        // 信号强度变化显著时提醒
        if (Math.abs(newRssi - lastSignalStrength) >= SIGNAL_CHANGE_THRESHOLD) {
            val currentLevel = wifiOptimizer.calculateSignalQuality(newRssi)
            val lastLevel = wifiOptimizer.calculateSignalQuality(lastSignalStrength)

            if (currentLevel < lastLevel && currentLevel < 50) {
                // 信号变弱
                notificationHelper.showNotification(
                    "WiFi信号变弱",
                    "当前信号质量：${currentLevel}%，建议靠近路由器",
                    NotificationType.WIFI_SIGNAL_WEAK
                )
            } else if (currentLevel > lastLevel && currentLevel >= 75) {
                // 信号变强
                notificationHelper.showNotification(
                    "WiFi信号改善",
                    "当前信号质量：${currentLevel}%",
                    NotificationType.WIFI_SIGNAL_STRONG
                )
            }

            lastSignalStrength = newRssi
        }
    }

    private fun checkWifiStatus() {
        serviceScope.launch {
            try {
                val wifiInfo = wifiOptimizer.getCurrentWifiInfo()
                wifiInfo?.let { info ->
                    // 检查信号质量
                    val signalQuality = wifiOptimizer.calculateSignalQuality(info.signalStrength)
                    if (signalQuality < 30 && lastSignalStrength >= 30) {
                        notificationHelper.showNotification(
                            "WiFi信号很弱",
                            "信号质量仅${signalQuality}%，网络连接可能不稳定",
                            NotificationType.WIFI_SIGNAL_WEAK
                        )
                    }

                    // 检查连接速度
                    if (info.linkSpeed < 10 && info.isConnected) {
                        notificationHelper.showNotification(
                            "WiFi连接速度慢",
                            "当前速度仅${info.linkSpeed}Mbps，建议检查网络设置",
                            NotificationType.WIFI_SIGNAL_WEAK
                        )
                    }
                }
            } catch (e: Exception) {
                // 忽略异常
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID_SERVICE = 2001
        private const val MONITORING_INTERVAL = 30000L  // 30秒
        private const val SIGNAL_CHANGE_THRESHOLD = 10   // 信号变化阈值

        fun startService(context: Context) {
            val intent = Intent(context, WifiMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WifiMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
