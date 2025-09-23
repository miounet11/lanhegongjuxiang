package com.lanhe.gongjuxiang.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.models.BatteryInfo
import com.lanhe.gongjuxiang.utils.PerformanceMonitor
import com.lanhe.gongjuxiang.utils.PerformanceMonitorManager
import kotlinx.coroutines.*

/**
 * 充电提醒服务 - 核心服务类
 * 监控充电状态并提供智能提醒
 */
class ChargingReminderService : Service() {

    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var performanceManager: PerformanceMonitorManager
    private lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false

    // 充电状态监控
    private val chargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> handlePowerConnected()
                Intent.ACTION_POWER_DISCONNECTED -> handlePowerDisconnected()
                Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
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
        unregisterReceiver(chargingReceiver)
        serviceScope.cancel()
        stopMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun initializeComponents() {
        performanceMonitor = PerformanceMonitor(this)
        performanceManager = PerformanceMonitorManager(this)
        notificationHelper = NotificationHelper(this)
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(chargingReceiver, filter)
    }

    private fun createNotificationChannel() {
        notificationHelper.createNotificationChannel()
    }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        serviceScope.launch {
            while (isMonitoring && isActive) {
                try {
                    checkChargingStatus()
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

    private fun handlePowerConnected() {
        notificationHelper.showNotification(
            "充电器已连接",
            "开始监控充电状态，确保安全充电",
            NotificationType.CHARGING_CONNECTED
        )

        serviceScope.launch {
            delay(2000)
            checkChargingEnvironment()
        }
    }

    private fun handlePowerDisconnected() {
        notificationHelper.showNotification(
            "充电器已断开",
            "充电已停止，请注意电池状态",
            NotificationType.CHARGING_DISCONNECTED
        )
    }

    private fun handleBatteryChanged(intent: Intent?) {
        if (intent == null) return

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0f
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000.0f
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

        if (isCharging) {
            checkTemperature(temperature)
            checkVoltage(voltage)
            checkChargingType(plugged)
            checkBatteryStatus(level, temperature)
        }
    }

    private fun checkChargingEnvironment() {
        serviceScope.launch {
            try {
                val batteryInfo = performanceManager.getBatteryInfo()
                if (batteryInfo.temperature > 35.0f) {
                    notificationHelper.showNotification(
                        "充电环境温度较高",
                        "当前温度${String.format("%.1f", batteryInfo.temperature)}°C，建议在凉爽环境中充电",
                        NotificationType.TEMPERATURE_WARNING
                    )
                }

                if (isConnectedToComputer()) {
                    notificationHelper.showNotification(
                        "检测到电脑充电",
                        "正在通过电脑USB充电，充电速度较慢",
                        NotificationType.COMPUTER_CHARGING
                    )
                }

                checkChargingSafety(batteryInfo)
            } catch (e: Exception) {
                // 忽略异常
            }
        }
    }

    private fun checkTemperature(temperature: Float) {
        when {
            temperature >= 45.0f -> notificationHelper.showNotification(
                "⚠️ 电池温度过高",
                "当前温度${String.format("%.1f", temperature)}°C，建议停止充电并让设备冷却",
                NotificationType.TEMPERATURE_DANGER
            )
            temperature >= 40.0f -> notificationHelper.showNotification(
                "🔥 电池温度较高",
                "当前温度${String.format("%.1f", temperature)}°C，注意设备散热",
                NotificationType.TEMPERATURE_WARNING
            )
            temperature <= 5.0f -> notificationHelper.showNotification(
                "❄️ 电池温度较低",
                "当前温度${String.format("%.1f", temperature)}°C，建议在温暖环境中充电",
                NotificationType.TEMPERATURE_LOW
            )
        }
    }

    private fun checkVoltage(voltage: Float) {
        when {
            voltage >= 4.4f -> notificationHelper.showNotification(
                "⚡ 充电电压异常",
                "检测到异常电压${String.format("%.2f", voltage)}V，请检查充电器",
                NotificationType.VOLTAGE_WARNING
            )
            voltage <= 4.0f -> notificationHelper.showNotification(
                "🔋 充电电压偏低",
                "检测到低电压${String.format("%.2f", voltage)}V，充电效率较低",
                NotificationType.VOLTAGE_LOW
            )
        }
    }

    private fun checkChargingType(plugged: Int) {
        when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> notificationHelper.showNotification(
                "交流电充电",
                "正在使用电源适配器充电，充电速度最快",
                NotificationType.AC_CHARGING
            )
            BatteryManager.BATTERY_PLUGGED_USB -> notificationHelper.showNotification(
                "USB充电",
                "正在使用USB充电，充电速度较慢",
                NotificationType.USB_CHARGING
            )
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> notificationHelper.showNotification(
                "无线充电",
                "正在使用无线充电，充电速度适中",
                NotificationType.WIRELESS_CHARGING
            )
        }
    }

    private fun checkBatteryStatus(level: Int, temperature: Float) {
        when {
            level >= 95 -> notificationHelper.showNotification(
                "电池即将充满",
                "当前电量${level}%，建议及时断开充电器",
                NotificationType.BATTERY_FULL
            )
            level >= 80 -> {
                // 80%提醒
                notificationHelper.showNotification(
                    "电池电量充足",
                    "当前电量${level}%，已达到80%智能提醒阈值",
                    NotificationType.BATTERY_HIGH
                )
            }
            level <= 20 -> {
                // 20%低电量提醒
                notificationHelper.showNotification(
                    "电池电量不足",
                    "当前电量仅${level}%，请及时充电",
                    NotificationType.BATTERY_LOW
                )
            }
            level <= 5 -> {
                // 5%严重低电量提醒
                notificationHelper.showNotification(
                    "⚠️ 电池电量严重不足",
                    "当前电量仅${level}%，设备可能即将自动关机",
                    NotificationType.BATTERY_CRITICAL
                )
            }
        }

        if (temperature > 42.0f && level > 90) {
            notificationHelper.showNotification(
                "电池健康提醒",
                "长时间高电量高温度充电可能影响电池寿命",
                NotificationType.BATTERY_HEALTH
            )
        }
    }

    private fun checkChargingSafety(batteryInfo: BatteryInfo) {
        if (batteryInfo.temperature > 40.0f) {
            notificationHelper.showNotification(
                "🔥 充电安全提醒",
                "电池温度较高，请确保设备在通风良好的环境中充电",
                NotificationType.SAFETY_WARNING
            )
        }

        val chargingTime = getChargingTime()
        if (chargingTime > 8 * 60 * 60 * 1000) {
            notificationHelper.showNotification(
                "⏰ 长时间充电提醒",
                "已充电${chargingTime / (60 * 60 * 1000)}小时，建议检查电池状态",
                NotificationType.LONG_CHARGING
            )
        }
    }

    private fun checkChargingStatus() {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)

        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            val temperature = try {
                val tempProperty = BatteryManager::class.java.getField("BATTERY_PROPERTY_TEMPERATURE")
                    .getInt(null)
                batteryManager.getIntProperty(tempProperty) / 10.0f
            } catch (e: Exception) {
                25.0f // 默认温度
            }
            val current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000.0f

            if (current > 2000) {
                notificationHelper.showNotification(
                    "⚡ 快速充电",
                    "检测到快速充电电流${String.format("%.1f", current)}mA",
                    NotificationType.FAST_CHARGING
                )
            }
        }
    }

    private fun isConnectedToComputer(): Boolean = false
    private fun getChargingTime(): Long = 0L

    companion object {
        private const val NOTIFICATION_ID_SERVICE = 1001
        private const val MONITORING_INTERVAL = 30000L

        fun startService(context: Context) {
            val intent = Intent(context, ChargingReminderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ChargingReminderService::class.java)
            context.stopService(intent)
        }
    }
}
