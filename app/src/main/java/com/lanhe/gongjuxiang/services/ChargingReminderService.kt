package com.lanhe.gongjuxiang.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.models.BatteryInfo
import com.lanhe.gongjuxiang.utils.PerformanceMonitor
import com.lanhe.gongjuxiang.utils.PerformanceMonitorManager
import kotlinx.coroutines.*

/**
 * 充电提醒服务
 * 监控充电状态并提供智能提醒
 */
class ChargingReminderService : Service() {

    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var performanceManager: PerformanceMonitorManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false

    // 充电状态监控
    private val chargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    handlePowerConnected()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    handlePowerDisconnected()
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    handleBatteryChanged(intent)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        performanceMonitor = PerformanceMonitor(this)
        performanceManager = PerformanceMonitorManager(this)

        // 注册广播接收器
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        registerReceiver(chargingReceiver, filter)

        // 创建通知渠道
        createNotificationChannel()

        // 开始监控
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 创建前台服务通知
        val notification = createServiceNotification()
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
        // 充电器连接
        showNotification(
            "充电器已连接",
            "开始监控充电状态，确保安全充电",
            NotificationType.CHARGING_CONNECTED
        )

        // 检查充电环境
        serviceScope.launch {
            delay(2000) // 等待2秒让系统稳定
            checkChargingEnvironment()
        }
    }

    private fun handlePowerDisconnected() {
        // 充电器断开
        showNotification(
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

        // 检查充电状态
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

        if (isCharging) {
            // 检查温度
            checkTemperature(temperature)

            // 检查电压
            checkVoltage(voltage)

            // 检查充电类型
            checkChargingType(plugged)

            // 检查电池状态
            checkBatteryStatus(level, temperature)
        }
    }

    private fun checkChargingEnvironment() {
        serviceScope.launch {
            try {
                val batteryInfo = performanceManager.getBatteryInfo()

                // 检查设备温度
                if (batteryInfo.temperature > 35.0f) {
                    showNotification(
                        "充电环境温度较高",
                        "当前温度${String.format("%.1f", batteryInfo.temperature)}°C，建议在凉爽环境中充电",
                        NotificationType.TEMPERATURE_WARNING
                    )
                }

                // 检查是否连接电脑
                if (isConnectedToComputer()) {
                    showNotification(
                        "检测到电脑充电",
                        "正在通过电脑USB充电，充电速度较慢",
                        NotificationType.COMPUTER_CHARGING
                    )
                }

                // 检查充电安全
                checkChargingSafety(batteryInfo)

            } catch (e: Exception) {
                // 忽略异常
            }
        }
    }

    private fun checkTemperature(temperature: Float) {
        when {
            temperature >= 45.0f -> {
                // 危险温度
                showNotification(
                    "⚠️ 电池温度过高",
                    "当前温度${String.format("%.1f", temperature)}°C，建议停止充电并让设备冷却",
                    NotificationType.TEMPERATURE_DANGER
                )
            }
            temperature >= 40.0f -> {
                // 高温警告
                showNotification(
                    "🔥 电池温度较高",
                    "当前温度${String.format("%.1f", temperature)}°C，注意设备散热",
                    NotificationType.TEMPERATURE_WARNING
                )
            }
            temperature <= 5.0f -> {
                // 低温警告
                showNotification(
                    "❄️ 电池温度较低",
                    "当前温度${String.format("%.1f", temperature)}°C，建议在温暖环境中充电",
                    NotificationType.TEMPERATURE_LOW
                )
            }
        }
    }

    private fun checkVoltage(voltage: Float) {
        when {
            voltage >= 4.4f -> {
                // 电压过高
                showNotification(
                    "⚡ 充电电压异常",
                    "检测到异常电压${String.format("%.2f", voltage)}V，请检查充电器",
                    NotificationType.VOLTAGE_WARNING
                )
            }
            voltage <= 4.0f -> {
                // 电压过低
                showNotification(
                    "🔋 充电电压偏低",
                    "检测到低电压${String.format("%.2f", voltage)}V，充电效率较低",
                    NotificationType.VOLTAGE_LOW
                )
            }
        }
    }

    private fun checkChargingType(plugged: Int) {
        when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> {
                // 交流电充电
                showNotification(
                    "交流电充电",
                    "正在使用电源适配器充电，充电速度最快",
                    NotificationType.AC_CHARGING
                )
            }
            BatteryManager.BATTERY_PLUGGED_USB -> {
                // USB充电
                showNotification(
                    "USB充电",
                    "正在使用USB充电，充电速度较慢",
                    NotificationType.USB_CHARGING
                )
            }
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> {
                // 无线充电
                showNotification(
                    "无线充电",
                    "正在使用无线充电，充电速度适中",
                    NotificationType.WIRELESS_CHARGING
                )
            }
        }
    }

    private fun checkBatteryStatus(level: Int, temperature: Float) {
        // 检查电池电量
        when {
            level >= 95 -> {
                showNotification(
                    "电池即将充满",
                    "当前电量${level}%，建议及时断开充电器",
                    NotificationType.BATTERY_FULL
                )
            }
            level >= 80 -> {
                showNotification(
                    "电池电量充足",
                    "当前电量${level}%，可考虑断开充电器节省电量",
                    NotificationType.BATTERY_HIGH
                )
            }
        }

        // 检查电池健康状态
        if (temperature > 42.0f && level > 90) {
            showNotification(
                "电池健康提醒",
                "长时间高电量高温度充电可能影响电池寿命",
                NotificationType.BATTERY_HEALTH
            )
        }
    }

    private fun checkChargingSafety(batteryInfo: BatteryInfo) {
        // 检查充电安全
        if (batteryInfo.temperature > 40.0f) {
            showNotification(
                "🔥 充电安全提醒",
                "电池温度较高，请确保设备在通风良好的环境中充电",
                NotificationType.SAFETY_WARNING
            )
        }

        // 检查充电时间
        val chargingTime = getChargingTime()
        if (chargingTime > 8 * 60 * 60 * 1000) { // 8小时
            showNotification(
                "⏰ 长时间充电提醒",
                "已充电${chargingTime / (60 * 60 * 1000)}小时，建议检查电池状态",
                NotificationType.LONG_CHARGING
            )
        }
    }

    private fun checkChargingStatus() {
        // 定期检查充电状态
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)

        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            // 正在充电，检查各项指标
            val temperature = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_TEMPERATURE) / 10.0f
            val current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000.0f

            // 检查充电电流
            if (current > 2000) { // 超过2A
                showNotification(
                    "⚡ 快速充电",
                    "检测到快速充电电流${String.format("%.1f", current)}mA",
                    NotificationType.FAST_CHARGING
                )
            }
        }
    }

    private fun isConnectedToComputer(): Boolean {
        // 检查是否连接到电脑
        // 这里可以根据USB连接状态或其他方式判断
        return false // 暂时返回false，需要具体实现
    }

    private fun getChargingTime(): Long {
        // 获取充电时间
        // 这里需要记录充电开始时间
        return 0L // 暂时返回0，需要具体实现
    }

    private fun showNotification(title: String, message: String, type: NotificationType) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(getNotificationIcon(type))
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(getNotificationPriority(type))
            .setAutoCancel(true)
            .setCategory(getNotificationCategory(type))
            .build()

        NotificationManagerCompat.from(this).notify(type.id, notification)
    }

    private fun createServiceNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_battery)
            .setContentTitle("充电提醒服务")
            .setContentText("正在监控充电状态")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "充电提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "充电状态监控和提醒通知"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotificationIcon(type: NotificationType): Int {
        return when (type) {
            NotificationType.CHARGING_CONNECTED -> R.drawable.ic_battery_charging
            NotificationType.CHARGING_DISCONNECTED -> R.drawable.ic_battery
            NotificationType.TEMPERATURE_WARNING -> R.drawable.ic_temperature
            NotificationType.TEMPERATURE_DANGER -> R.drawable.ic_warning
            NotificationType.TEMPERATURE_LOW -> R.drawable.ic_cold
            NotificationType.VOLTAGE_WARNING -> R.drawable.ic_voltage
            NotificationType.VOLTAGE_LOW -> R.drawable.ic_low_voltage
            NotificationType.AC_CHARGING -> R.drawable.ic_ac_power
            NotificationType.USB_CHARGING -> R.drawable.ic_usb
            NotificationType.WIRELESS_CHARGING -> R.drawable.ic_wireless
            NotificationType.BATTERY_FULL -> R.drawable.ic_battery_full
            NotificationType.BATTERY_HIGH -> R.drawable.ic_battery_high
            NotificationType.BATTERY_HEALTH -> R.drawable.ic_health
            NotificationType.SAFETY_WARNING -> R.drawable.ic_safety
            NotificationType.LONG_CHARGING -> R.drawable.ic_time
            NotificationType.FAST_CHARGING -> R.drawable.ic_fast_charge
            NotificationType.COMPUTER_CHARGING -> R.drawable.ic_computer
        }
    }

    private fun getNotificationPriority(type: NotificationType): Int {
        return when (type) {
            NotificationType.TEMPERATURE_DANGER,
            NotificationType.SAFETY_WARNING -> NotificationCompat.PRIORITY_HIGH
            NotificationType.TEMPERATURE_WARNING,
            NotificationType.VOLTAGE_WARNING,
            NotificationType.BATTERY_FULL -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
    }

    private fun getNotificationCategory(type: NotificationType): String {
        return when (type) {
            NotificationType.TEMPERATURE_DANGER,
            NotificationType.SAFETY_WARNING -> NotificationCompat.CATEGORY_ALARM
            NotificationType.CHARGING_CONNECTED,
            NotificationType.CHARGING_DISCONNECTED -> NotificationCompat.CATEGORY_STATUS
            else -> NotificationCompat.CATEGORY_SERVICE
        }
    }

    companion object {
        private const val CHANNEL_ID = "charging_reminder_channel"
        private const val NOTIFICATION_ID_SERVICE = 1001
        private const val MONITORING_INTERVAL = 30000L // 30秒检查一次

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

/**
 * 通知类型枚举
 */
enum class NotificationType(val id: Int) {
    CHARGING_CONNECTED(2001),
    CHARGING_DISCONNECTED(2002),
    TEMPERATURE_WARNING(2003),
    TEMPERATURE_DANGER(2004),
    TEMPERATURE_LOW(2005),
    VOLTAGE_WARNING(2006),
    VOLTAGE_LOW(2007),
    AC_CHARGING(2008),
    USB_CHARGING(2009),
    WIRELESS_CHARGING(2010),
    BATTERY_FULL(2011),
    BATTERY_HIGH(2012),
    BATTERY_HEALTH(2013),
    SAFETY_WARNING(2014),
    LONG_CHARGING(2015),
    FAST_CHARGING(2016),
    COMPUTER_CHARGING(2017)
}
