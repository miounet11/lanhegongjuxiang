package com.lanhe.gongjuxiang.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.models.BatteryInfo
import com.lanhe.gongjuxiang.utils.PerformanceMonitor
import com.lanhe.gongjuxiang.utils.PerformanceMonitorManager
import kotlinx.coroutines.*
import android.util.Log

/**
 * 充电提醒服务 - 重构版本
 * 监控充电状态并提供智能提醒
 * 包含完整的生命周期管理和异常恢复机制
 */
class ChargingReminderService : BaseLifecycleService() {

    companion object {
        private const val TAG = "ChargingReminderService"
        private const val NOTIFICATION_ID_SERVICE = 1001
        private const val MONITORING_INTERVAL = 30000L
        private const val CHANNEL_ID = "charging_reminder_channel"
        private const val CHANNEL_NAME = "充电提醒"

        fun startService(context: Context) {
            val intent = Intent(context, ChargingReminderService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "启动服务失败", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ChargingReminderService::class.java)
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "停止服务失败", e)
            }
        }
    }

    // 核心组件
    private var performanceMonitor: PerformanceMonitor? = null
    private var performanceManager: PerformanceMonitorManager? = null
    private var notificationHelper: NotificationHelper? = null

    // 监控状态
    @Volatile
    private var isMonitoring = false
    private var monitoringJob: Job? = null

    // 充电状态记录
    private var lastChargingTime = 0L
    private var lastTemperatureAlert = 0L
    private val alertCooldownMs = 300000L // 5分钟冷却时间

    // 充电状态监控广播接收器
    private val chargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // 确保context不为null
            if (context == null || intent == null) {
                Log.w(TAG, "接收到null context或intent")
                return
            }

            serviceScope.launch {
                try {
                    when (intent.action) {
                        Intent.ACTION_POWER_CONNECTED -> handlePowerConnected()
                        Intent.ACTION_POWER_DISCONNECTED -> handlePowerDisconnected()
                        Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理充电事件异常", e)
                }
            }
        }
    }

    override fun getServiceTag(): String = TAG

    override suspend fun onInitialize(): Boolean {
        return try {
            // 初始化组件
            initializeComponents()

            // 创建通知通道
            createNotificationChannel()

            // 启动前台服务
            startForegroundServiceSafely()

            // 注册广播接收器
            registerChargingReceivers()

            // 开始监控
            startMonitoring()

            true
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
            false
        }
    }

    override fun onCleanup() {
        // 停止监控
        stopMonitoring()

        // 清理组件
        performanceMonitor = null
        performanceManager = null
        notificationHelper = null

        Log.d(TAG, "清理完成")
    }

    override fun onTaskRemovedHandle() {
        // 用户从最近任务移除应用时的处理
        Log.w(TAG, "应用被用户移除，保存状态并准备重启")

        // 保存当前状态
        saveServiceState()

        // 如果是重要的充电监控，可以选择重启
        if (isMonitoring) {
            scheduleServiceRestart()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand - 确保前台服务运行")

        // 确保前台服务运行
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundServiceSafely()
        }

        // 恢复状态（如果需要）
        intent?.let {
            if (it.getBooleanExtra("restore_state", false)) {
                restoreServiceState()
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
            performanceMonitor = PerformanceMonitor(this@ChargingReminderService)
            performanceManager = PerformanceMonitorManager(this@ChargingReminderService)
            notificationHelper = NotificationHelper(this@ChargingReminderService)
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
                    description = "充电状态监控和提醒通知"
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+
                startForeground(
                    NOTIFICATION_ID_SERVICE,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10-11
                startForeground(
                    NOTIFICATION_ID_SERVICE,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                // Android 9及以下
                startForeground(NOTIFICATION_ID_SERVICE, notification)
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
        val intent = Intent(this, ChargingReminderService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("充电监控服务运行中")
            .setContentText("正在监控充电状态")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * 注册充电相关广播接收器
     */
    private fun registerChargingReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiverSafely(chargingReceiver, filter)
        Log.d(TAG, "充电广播接收器注册成功")
    }

    /**
     * 开始监控
     */
    private fun startMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        monitoringJob = serviceScope.launch {
            Log.d(TAG, "开始充电监控")
            while (isMonitoring && isActive) {
                try {
                    checkChargingStatus()
                    delay(MONITORING_INTERVAL)
                } catch (e: CancellationException) {
                    Log.d(TAG, "监控协程被取消")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "监控过程中出现异常", e)
                    // 继续监控，不中断
                    delay(5000) // 短暂延迟后继续
                }
            }
            Log.d(TAG, "充电监控结束")
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
     * 处理充电器连接
     */
    private suspend fun handlePowerConnected() = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "充电器已连接")
            lastChargingTime = System.currentTimeMillis()

            notificationHelper?.showNotification(
                "充电器已连接",
                "开始监控充电状态，确保安全充电",
                NotificationType.CHARGING_CONNECTED
            )

            delay(2000)
            checkChargingEnvironment()
        } catch (e: Exception) {
            Log.e(TAG, "处理充电器连接异常", e)
        }
    }

    /**
     * 处理充电器断开
     */
    private suspend fun handlePowerDisconnected() = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "充电器已断开")

            val chargingDuration = if (lastChargingTime > 0) {
                System.currentTimeMillis() - lastChargingTime
            } else 0L

            val message = if (chargingDuration > 0) {
                val hours = chargingDuration / (1000 * 60 * 60)
                val minutes = (chargingDuration / (1000 * 60)) % 60
                "充电已停止，充电时长：${hours}小时${minutes}分钟"
            } else {
                "充电已停止，请注意电池状态"
            }

            notificationHelper?.showNotification(
                "充电器已断开",
                message,
                NotificationType.CHARGING_DISCONNECTED
            )

            lastChargingTime = 0
        } catch (e: Exception) {
            Log.e(TAG, "处理充电器断开异常", e)
        }
    }

    /**
     * 处理电池状态变化
     */
    private suspend fun handleBatteryChanged(intent: Intent) = withContext(Dispatchers.IO) {
        try {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0f
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000.0f
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            if (isCharging) {
                checkTemperatureSafety(temperature)
                checkVoltageSafety(voltage)
                checkChargingType(plugged)
                checkBatteryLevel(level, temperature)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理电池状态变化异常", e)
        }
    }

    /**
     * 检查充电环境
     */
    private suspend fun checkChargingEnvironment() = withContext(Dispatchers.IO) {
        try {
            val batteryInfo = performanceManager?.getBatteryInfo()
            batteryInfo?.let {
                if (it.temperature > 35.0f) {
                    notificationHelper?.showNotification(
                        "充电环境温度较高",
                        "当前温度${String.format("%.1f", it.temperature)}°C，建议在凉爽环境中充电",
                        NotificationType.TEMPERATURE_WARNING
                    )
                }
                checkChargingSafety(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查充电环境异常", e)
        }
    }

    /**
     * 检查温度安全
     */
    private fun checkTemperatureSafety(temperature: Float) {
        val currentTime = System.currentTimeMillis()

        // 实施冷却时间，避免频繁提醒
        if (currentTime - lastTemperatureAlert < alertCooldownMs) {
            return
        }

        when {
            temperature >= 45.0f -> {
                notificationHelper?.showNotification(
                    "电池温度过高",
                    "当前温度${String.format("%.1f", temperature)}°C，建议停止充电并让设备冷却",
                    NotificationType.TEMPERATURE_DANGER
                )
                lastTemperatureAlert = currentTime
            }
            temperature >= 40.0f -> {
                notificationHelper?.showNotification(
                    "电池温度较高",
                    "当前温度${String.format("%.1f", temperature)}°C，注意设备散热",
                    NotificationType.TEMPERATURE_WARNING
                )
                lastTemperatureAlert = currentTime
            }
            temperature <= 5.0f -> {
                notificationHelper?.showNotification(
                    "电池温度较低",
                    "当前温度${String.format("%.1f", temperature)}°C，建议在温暖环境中充电",
                    NotificationType.TEMPERATURE_LOW
                )
                lastTemperatureAlert = currentTime
            }
        }
    }

    /**
     * 检查电压安全
     */
    private fun checkVoltageSafety(voltage: Float) {
        when {
            voltage >= 4.4f -> {
                notificationHelper?.showNotification(
                    "充电电压异常",
                    "检测到异常电压${String.format("%.2f", voltage)}V，请检查充电器",
                    NotificationType.VOLTAGE_WARNING
                )
            }
            voltage in 3.0f..3.5f -> {
                notificationHelper?.showNotification(
                    "充电电压偏低",
                    "检测到低电压${String.format("%.2f", voltage)}V，充电效率较低",
                    NotificationType.VOLTAGE_LOW
                )
            }
        }
    }

    /**
     * 检查充电类型
     */
    private fun checkChargingType(plugged: Int) {
        when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> {
                Log.d(TAG, "交流电充电")
            }
            BatteryManager.BATTERY_PLUGGED_USB -> {
                Log.d(TAG, "USB充电")
            }
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> {
                Log.d(TAG, "无线充电")
            }
        }
    }

    /**
     * 检查电池电量
     */
    private fun checkBatteryLevel(level: Int, temperature: Float) {
        when {
            level >= 95 -> {
                notificationHelper?.showNotification(
                    "电池即将充满",
                    "当前电量${level}%，建议及时断开充电器",
                    NotificationType.BATTERY_FULL
                )
            }
            level >= 80 && temperature > 35.0f -> {
                notificationHelper?.showNotification(
                    "电池健康提醒",
                    "高电量高温充电可能影响电池寿命",
                    NotificationType.BATTERY_HEALTH
                )
            }
        }
    }

    /**
     * 检查充电状态
     */
    private suspend fun checkChargingStatus() = withContext(Dispatchers.IO) {
        try {
            val batteryManager = getSystemService(BATTERY_SERVICE) as? BatteryManager
            batteryManager?.let {
                val status = it.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    val current = it.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000.0f
                    Log.d(TAG, "当前充电电流: ${current}mA")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查充电状态异常", e)
        }
    }

    /**
     * 检查充电安全
     */
    private fun checkChargingSafety(batteryInfo: BatteryInfo) {
        if (batteryInfo.temperature > 40.0f) {
            Log.w(TAG, "电池温度较高: ${batteryInfo.temperature}°C")
        }

        val chargingTime = System.currentTimeMillis() - lastChargingTime
        if (lastChargingTime > 0 && chargingTime > 8 * 60 * 60 * 1000) {
            notificationHelper?.showNotification(
                "长时间充电提醒",
                "已充电${chargingTime / (60 * 60 * 1000)}小时，建议检查电池状态",
                NotificationType.LONG_CHARGING
            )
        }
    }

    /**
     * 保存服务状态
     */
    private fun saveServiceState() {
        try {
            val prefs = getSharedPreferences("charging_service_state", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("was_monitoring", isMonitoring)
                putLong("last_charging_time", lastChargingTime)
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
            val prefs = getSharedPreferences("charging_service_state", Context.MODE_PRIVATE)
            val wasMonitoring = prefs.getBoolean("was_monitoring", false)
            lastChargingTime = prefs.getLong("last_charging_time", 0)

            if (wasMonitoring && !isMonitoring) {
                startMonitoring()
            }
            Log.d(TAG, "服务状态已恢复")
        } catch (e: Exception) {
            Log.e(TAG, "恢复服务状态失败", e)
        }
    }

    /**
     * 调度服务重启
     */
    private fun scheduleServiceRestart() {
        try {
            val restartIntent = Intent(this, ChargingReminderService::class.java).apply {
                putExtra("restore_state", true)
            }

            val pendingIntent = PendingIntent.getService(
                this,
                1,
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