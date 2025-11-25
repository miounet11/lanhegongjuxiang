package com.lanhe.gongjuxiang.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.utils.WifiOptimizer
import kotlinx.coroutines.*

/**
 * WiFi监控服务 - 重构版本
 * 监控WiFi连接状态并提供智能提醒
 * 包含完整的生命周期管理和异常恢复机制
 */
class WifiMonitorService : BaseLifecycleService() {

    companion object {
        private const val TAG = "WifiMonitorService"
        private const val NOTIFICATION_ID_SERVICE = 2001
        private const val MONITORING_INTERVAL = 30000L  // 30秒
        private const val SIGNAL_CHANGE_THRESHOLD = 10   // 信号变化阈值
        private const val CHANNEL_ID = "wifi_monitor_channel"
        private const val CHANNEL_NAME = "WiFi监控"

        fun startService(context: Context) {
            val intent = Intent(context, WifiMonitorService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.i(TAG, "WiFi监控服务启动请求已发送")
            } catch (e: Exception) {
                Log.e(TAG, "启动服务失败", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WifiMonitorService::class.java)
            try {
                context.stopService(intent)
                Log.i(TAG, "WiFi监控服务停止请求已发送")
            } catch (e: Exception) {
                Log.e(TAG, "停止服务失败", e)
            }
        }
    }

    // 系统服务
    private var wifiManager: WifiManager? = null
    private var connectivityManager: ConnectivityManager? = null
    private var wifiOptimizer: WifiOptimizer? = null
    private var notificationHelper: NotificationHelper? = null

    // 监控状态
    @Volatile
    private var isMonitoring = false
    private var monitoringJob: Job? = null

    // WiFi状态缓存
    private data class WifiState(
        val isConnected: Boolean = false,
        val ssid: String = "",
        val signalStrength: Int = -100,
        val linkSpeed: Int = 0,
        val signalQuality: Int = 0
    )

    private var currentWifiState = WifiState()
    private var lastWifiState = WifiState()
    private var lastSignalAlert = 0L
    private val alertCooldownMs = 300000L // 5分钟冷却时间

    // WiFi状态监控广播接收器
    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 确保context不为null
            if (context == null || intent == null) {
                Log.w(TAG, "接收到null context或intent")
                return
            }

            serviceScope.launch {
                try {
                    when (intent.action) {
                        WifiManager.WIFI_STATE_CHANGED_ACTION -> handleWifiStateChanged(intent)
                        WifiManager.NETWORK_STATE_CHANGED_ACTION -> handleNetworkStateChanged(intent)
                        WifiManager.RSSI_CHANGED_ACTION -> handleRssiChanged(intent)
                        WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> handleScanResults()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理WiFi事件异常", e)
                }
            }
        }
    }

    override fun getServiceTag(): String = TAG

    override suspend fun onInitialize(): Boolean {
        return try {
            Log.d(TAG, "开始初始化WiFi监控服务")

            // 初始化组件
            initializeComponents()

            // 创建通知通道
            createNotificationChannel()

            // 启动前台服务
            startForegroundServiceSafely()

            // 注册广播接收器
            registerWifiReceivers()

            // 获取当前WiFi状态
            updateCurrentWifiState()

            // 开始监控
            startMonitoring()

            Log.i(TAG, "WiFi监控服务初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
            false
        }
    }

    override fun onCleanup() {
        Log.d(TAG, "开始清理WiFi监控服务")

        // 停止监控
        stopMonitoring()

        // 保存当前状态
        saveServiceState()

        // 清理组件引用
        wifiManager = null
        connectivityManager = null
        wifiOptimizer = null
        notificationHelper = null

        Log.d(TAG, "WiFi监控服务清理完成")
    }

    override fun onTaskRemovedHandle() {
        Log.w(TAG, "应用从最近任务中被移除")

        // 保存状态
        saveServiceState()

        // 如果WiFi连接不稳定，继续监控
        if (shouldContinueMonitoring()) {
            Log.i(TAG, "WiFi需要继续监控，保持服务运行")
            scheduleServiceRestart()
        } else {
            Log.i(TAG, "WiFi状态稳定，停止服务")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand - 确保前台服务运行")

        // 确保前台服务运行
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundServiceSafely()
        }

        // 处理特定命令
        intent?.action?.let { action ->
            when (action) {
                "SCAN_WIFI" -> scanWifiNetworks()
                "OPTIMIZE_WIFI" -> optimizeWifiConnection()
                "RESTORE_STATE" -> restoreServiceState()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 初始化核心组件
     */
    private suspend fun initializeComponents() = withContext(Dispatchers.Main) {
        try {
            wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            wifiOptimizer = WifiOptimizer(this@WifiMonitorService)
            notificationHelper = NotificationHelper(this@WifiMonitorService)

            if (wifiManager == null) {
                Log.e(TAG, "无法获取WifiManager")
                throw IllegalStateException("WifiManager不可用")
            }

            Log.d(TAG, "组件初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "组件初始化失败", e)
            throw e
        }
    }

    /**
     * 创建通知通道
     */
    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "WiFi连接状态监控和提醒"
                    setShowBadge(true)
                    enableVibration(false)
                    setSound(null, null)
                }

                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.createNotificationChannel(channel)
                Log.d(TAG, "通知通道创建成功")
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建通知通道失败", e)
        }
    }

    /**
     * 安全启动前台服务
     */
    private fun startForegroundServiceSafely() {
        try {
            val notification = createServiceNotification()

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Android 12+
                    startForeground(
                        NOTIFICATION_ID_SERVICE,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    // Android 10-11
                    startForeground(
                        NOTIFICATION_ID_SERVICE,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                }
                else -> {
                    // Android 9及以下
                    startForeground(NOTIFICATION_ID_SERVICE, notification)
                }
            }
            Log.d(TAG, "前台服务启动成功")
        } catch (e: Exception) {
            Log.e(TAG, "启动前台服务失败", e)
        }
    }

    /**
     * 创建服务通知
     */
    private fun createServiceNotification(): Notification {
        val intent = Intent(this, WifiMonitorService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (currentWifiState.isConnected) {
            "已连接: ${currentWifiState.ssid} (信号: ${currentWifiState.signalQuality}%)"
        } else {
            "WiFi未连接"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("WiFi监控服务")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .addAction(createScanAction())
            .build()
    }

    /**
     * 创建扫描操作按钮
     */
    private fun createScanAction(): NotificationCompat.Action {
        val scanIntent = Intent(this, WifiMonitorService::class.java).apply {
            action = "SCAN_WIFI"
        }

        val scanPendingIntent = PendingIntent.getService(
            this, 1, scanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_search,
            "扫描网络",
            scanPendingIntent
        ).build()
    }

    /**
     * 注册WiFi相关广播接收器
     */
    private fun registerWifiReceivers() {
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        }
        registerReceiverSafely(wifiReceiver, filter)
        Log.d(TAG, "WiFi广播接收器注册成功")
    }

    /**
     * 更新当前WiFi状态
     */
    private fun updateCurrentWifiState() {
        try {
            val optimizer = wifiOptimizer ?: return
            val isConnected = optimizer.isNetworkConnected() && optimizer.getNetworkType() == "WiFi"

            currentWifiState = if (isConnected) {
                optimizer.getCurrentWifiInfo()?.let { info ->
                    WifiState(
                        isConnected = true,
                        ssid = info.ssid,
                        signalStrength = info.signalStrength,
                        linkSpeed = info.linkSpeed,
                        signalQuality = optimizer.calculateSignalQuality(info.signalStrength)
                    )
                } ?: WifiState()
            } else {
                WifiState()
            }

            Log.d(TAG, "WiFi状态已更新: $currentWifiState")
        } catch (e: Exception) {
            Log.e(TAG, "更新WiFi状态失败", e)
        }
    }

    /**
     * 开始监控
     */
    private fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        monitoringJob = serviceScope.launch {
            Log.d(TAG, "开始WiFi监控")
            while (isMonitoring && isActive) {
                try {
                    // 更新状态
                    updateCurrentWifiState()

                    // 检查WiFi状态
                    checkWifiStatus()

                    // 检查信号质量
                    checkSignalQuality()

                    // 更新通知
                    updateNotification()

                    delay(MONITORING_INTERVAL)
                } catch (e: CancellationException) {
                    Log.d(TAG, "监控协程被取消")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "监控过程中出现异常", e)
                    delay(5000) // 短暂延迟后继续
                }
            }
            Log.d(TAG, "WiFi监控结束")
        }
    }

    /**
     * 停止监控
     */
    private fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        Log.d(TAG, "监控已停止")
    }

    /**
     * 处理WiFi状态变化
     */
    private suspend fun handleWifiStateChanged(intent: Intent) = withContext(Dispatchers.IO) {
        try {
            val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)

            when (wifiState) {
                WifiManager.WIFI_STATE_ENABLED -> {
                    Log.i(TAG, "WiFi已启用")
                    // 延迟后扫描可用网络
                    delay(1000)
                    scanWifiNetworks()
                }
                WifiManager.WIFI_STATE_DISABLED -> {
                    Log.i(TAG, "WiFi已禁用")
                    if (lastWifiState.isConnected) {
                        notificationHelper?.showNotification(
                            "WiFi已关闭",
                            "网络连接已断开",
                            NotificationType.WIFI_DISCONNECTED
                        )
                    }
                    currentWifiState = WifiState()
                }
                WifiManager.WIFI_STATE_ENABLING -> {
                    Log.d(TAG, "WiFi正在启用")
                }
                WifiManager.WIFI_STATE_DISABLING -> {
                    Log.d(TAG, "WiFi正在关闭")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理WiFi状态变化异常", e)
        }
    }

    /**
     * 处理网络状态变化
     */
    private suspend fun handleNetworkStateChanged(intent: Intent?) = withContext(Dispatchers.IO) {
        try {
            updateCurrentWifiState()

            if (currentWifiState.isConnected != lastWifiState.isConnected) {
                if (currentWifiState.isConnected) {
                    // WiFi已连接
                    notificationHelper?.showNotification(
                        "WiFi已连接",
                        "已连接到 ${currentWifiState.ssid}，信号强度：${currentWifiState.signalQuality}%",
                        NotificationType.WIFI_CONNECTED
                    )
                    Log.i(TAG, "WiFi连接成功: ${currentWifiState.ssid}")
                } else {
                    // WiFi已断开
                    notificationHelper?.showNotification(
                        "WiFi连接断开",
                        "网络连接已丢失",
                        NotificationType.WIFI_DISCONNECTED
                    )
                    Log.i(TAG, "WiFi连接断开")
                }
                lastWifiState = currentWifiState.copy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理网络状态变化异常", e)
        }
    }

    /**
     * 处理信号强度变化
     */
    private suspend fun handleRssiChanged(intent: Intent) = withContext(Dispatchers.IO) {
        try {
            val newRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -100)

            // 信号强度变化显著时更新
            if (Math.abs(newRssi - currentWifiState.signalStrength) >= SIGNAL_CHANGE_THRESHOLD) {
                val optimizer = wifiOptimizer ?: return@withContext
                val newQuality = optimizer.calculateSignalQuality(newRssi)
                val oldQuality = currentWifiState.signalQuality

                currentWifiState = currentWifiState.copy(
                    signalStrength = newRssi,
                    signalQuality = newQuality
                )

                // 信号质量警告
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastSignalAlert > alertCooldownMs) {
                    when {
                        newQuality < 30 && oldQuality >= 30 -> {
                            notificationHelper?.showNotification(
                                "WiFi信号变弱",
                                "当前信号质量：${newQuality}%，建议靠近路由器",
                                NotificationType.WIFI_SIGNAL_WEAK
                            )
                            lastSignalAlert = currentTime
                        }
                        newQuality >= 75 && oldQuality < 75 -> {
                            notificationHelper?.showNotification(
                                "WiFi信号改善",
                                "当前信号质量：${newQuality}%",
                                NotificationType.WIFI_SIGNAL_STRONG
                            )
                            lastSignalAlert = currentTime
                        }
                    }
                }

                Log.d(TAG, "信号强度变化: $newRssi dBm (质量: $newQuality%)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理信号强度变化异常", e)
        }
    }

    /**
     * 处理扫描结果
     */
    private suspend fun handleScanResults() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "WiFi扫描完成，处理结果")
            // 可以在这里分析扫描结果，推荐更好的网络
        } catch (e: Exception) {
            Log.e(TAG, "处理扫描结果异常", e)
        }
    }

    /**
     * 检查WiFi状态
     */
    private suspend fun checkWifiStatus() = withContext(Dispatchers.IO) {
        try {
            if (!currentWifiState.isConnected) return@withContext

            // 检查连接速度
            if (currentWifiState.linkSpeed in 1..9) {
                Log.w(TAG, "WiFi连接速度较慢: ${currentWifiState.linkSpeed}Mbps")
            }

            // 检查信号质量
            if (currentWifiState.signalQuality < 30) {
                Log.w(TAG, "WiFi信号质量差: ${currentWifiState.signalQuality}%")
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查WiFi状态异常", e)
        }
    }

    /**
     * 检查信号质量
     */
    private fun checkSignalQuality() {
        try {
            if (!currentWifiState.isConnected) return

            val quality = currentWifiState.signalQuality
            when {
                quality < 20 -> Log.e(TAG, "信号质量极差")
                quality < 40 -> Log.w(TAG, "信号质量较差")
                quality < 60 -> Log.i(TAG, "信号质量一般")
                quality < 80 -> Log.d(TAG, "信号质量良好")
                else -> Log.d(TAG, "信号质量优秀")
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查信号质量异常", e)
        }
    }

    /**
     * 扫描WiFi网络
     */
    private fun scanWifiNetworks() {
        serviceScope.launch {
            try {
                Log.i(TAG, "开始扫描WiFi网络")
                wifiManager?.startScan()
            } catch (e: Exception) {
                Log.e(TAG, "扫描WiFi网络失败", e)
            }
        }
    }

    /**
     * 优化WiFi连接
     */
    private fun optimizeWifiConnection() {
        serviceScope.launch {
            try {
                Log.i(TAG, "开始优化WiFi连接")
                // 暂时注释掉不存在的方法调用
                // wifiOptimizer?.optimizeCurrentConnection()
                Log.i(TAG, "WiFi连接优化功能暂时禁用")
            } catch (e: Exception) {
                Log.e(TAG, "优化WiFi连接失败", e)
            }
        }
    }

    /**
     * 更新通知
     */
    private fun updateNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID_SERVICE, createServiceNotification())
        } catch (e: Exception) {
            Log.e(TAG, "更新通知失败", e)
        }
    }

    /**
     * 保存服务状态
     */
    private fun saveServiceState() {
        try {
            val prefs = getSharedPreferences("wifi_monitor_state", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("was_monitoring", isMonitoring)
                putString("last_ssid", currentWifiState.ssid)
                putInt("last_signal_quality", currentWifiState.signalQuality)
                putLong("last_save_time", System.currentTimeMillis())
                apply()
            }
            Log.d(TAG, "服务状态已保存")
        } catch (e: Exception) {
            Log.e(TAG, "保存服务状态失败", e)
        }
    }

    /**
     * 恢复服务状态
     */
    private fun restoreServiceState() {
        try {
            val prefs = getSharedPreferences("wifi_monitor_state", Context.MODE_PRIVATE)
            val wasMonitoring = prefs.getBoolean("was_monitoring", false)

            if (wasMonitoring && !isMonitoring) {
                startMonitoring()
            }

            Log.d(TAG, "服务状态已恢复")
        } catch (e: Exception) {
            Log.e(TAG, "恢复服务状态失败", e)
        }
    }

    /**
     * 检查是否需要继续监控
     */
    private fun shouldContinueMonitoring(): Boolean {
        return try {
            // 如果WiFi信号质量差或不稳定，继续监控
            currentWifiState.isConnected && currentWifiState.signalQuality < 50
        } catch (e: Exception) {
            Log.e(TAG, "检查监控状态失败", e)
            false
        }
    }

    /**
     * 调度服务重启
     */
    private fun scheduleServiceRestart() {
        try {
            val restartIntent = Intent(this, WifiMonitorService::class.java).apply {
                action = "RESTORE_STATE"
            }

            val pendingIntent = PendingIntent.getService(
                this,
                2,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 5000,
                pendingIntent
            )
            Log.d(TAG, "已调度服务重启")
        } catch (e: Exception) {
            Log.e(TAG, "调度服务重启失败", e)
        }
    }
}