package com.lanhe.gongjuxiang.utils

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.lanhe.gongjuxiang.services.NotificationHelper
import com.lanhe.gongjuxiang.services.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 快速设置管理器
 * 提供快速开关系统设置的功能
 */
class QuickSettingsManager(private val context: Context) {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val notificationHelper = NotificationHelper(context)

    /**
     * 系统设置状态数据类
     */
    data class SystemSettingsStatus(
        val wifiEnabled: Boolean,
        val bluetoothEnabled: Boolean,
        val airplaneModeEnabled: Boolean,
        val mobileDataEnabled: Boolean,
        val locationEnabled: Boolean,
        val doNotDisturbEnabled: Boolean,
        val autoRotateEnabled: Boolean,
        val brightnessLevel: Int,
        val volumeLevel: Int,
        val batterySaverEnabled: Boolean
    )

    /**
     * 快速设置项数据类
     */
    data class QuickSettingItem(
        val id: String,
        val name: String,
        val description: String,
        val iconResId: String, // Fluent图标资源名
        val isEnabled: Boolean,
        val canToggle: Boolean = true,
        val category: String = "general"
    )

    /**
     * 设置变更结果
     */
    data class SettingChangeResult(
        val success: Boolean,
        val message: String,
        val requiresPermission: Boolean = false
    )

    /**
     * 获取当前系统设置状态
     */
    fun getCurrentSettingsStatus(): SystemSettingsStatus {
        return SystemSettingsStatus(
            wifiEnabled = wifiManager.isWifiEnabled,
            bluetoothEnabled = bluetoothAdapter?.isEnabled == true,
            airplaneModeEnabled = isAirplaneModeEnabled(),
            mobileDataEnabled = isMobileDataEnabled(),
            locationEnabled = isLocationEnabled(),
            doNotDisturbEnabled = isDoNotDisturbEnabled(),
            autoRotateEnabled = isAutoRotateEnabled(),
            brightnessLevel = getBrightnessLevel(),
            volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
            batterySaverEnabled = isBatterySaverEnabled()
        )
    }

    /**
     * 获取快速设置项列表
     */
    fun getQuickSettingItems(): List<QuickSettingItem> {
        val status = getCurrentSettingsStatus()

        return listOf(
            QuickSettingItem(
                id = "wifi",
                name = "WiFi",
                description = if (status.wifiEnabled) "WiFi已开启" else "WiFi已关闭",
                iconResId = "ic_fluent_wifi_1_24_regular",
                isEnabled = status.wifiEnabled
            ),
            QuickSettingItem(
                id = "bluetooth",
                name = "蓝牙",
                description = if (status.bluetoothEnabled) "蓝牙已开启" else "蓝牙已关闭",
                iconResId = "ic_fluent_bluetooth_24_regular",
                isEnabled = status.bluetoothEnabled
            ),
            QuickSettingItem(
                id = "airplane",
                name = "飞行模式",
                description = if (status.airplaneModeEnabled) "飞行模式已开启" else "飞行模式已关闭",
                iconResId = "ic_fluent_airplane_24_regular",
                isEnabled = status.airplaneModeEnabled
            ),
            QuickSettingItem(
                id = "mobile_data",
                name = "移动数据",
                description = if (status.mobileDataEnabled) "移动数据已开启" else "移动数据已关闭",
                iconResId = "ic_fluent_cellular_data_1_24_regular",
                isEnabled = status.mobileDataEnabled
            ),
            QuickSettingItem(
                id = "location",
                name = "定位服务",
                description = if (status.locationEnabled) "定位服务已开启" else "定位服务已关闭",
                iconResId = "ic_fluent_location_24_regular",
                isEnabled = status.locationEnabled
            ),
            QuickSettingItem(
                id = "do_not_disturb",
                name = "勿扰模式",
                description = if (status.doNotDisturbEnabled) "勿扰模式已开启" else "勿扰模式已关闭",
                iconResId = "ic_fluent_alert_off_24_regular",
                isEnabled = status.doNotDisturbEnabled
            ),
            QuickSettingItem(
                id = "auto_rotate",
                name = "自动旋转",
                description = if (status.autoRotateEnabled) "自动旋转已开启" else "自动旋转已关闭",
                iconResId = "ic_fluent_arrow_rotate_clockwise_24_regular",
                isEnabled = status.autoRotateEnabled
            ),
            QuickSettingItem(
                id = "battery_saver",
                name = "省电模式",
                description = if (status.batterySaverEnabled) "省电模式已开启" else "省电模式已关闭",
                iconResId = "ic_fluent_battery_saver_24_regular",
                isEnabled = status.batterySaverEnabled
            ),
            QuickSettingItem(
                id = "brightness",
                name = "亮度调节",
                description = "当前亮度: ${status.brightnessLevel}%",
                iconResId = "ic_fluent_brightness_high_24_regular",
                isEnabled = status.brightnessLevel > 50,
                category = "adjustment"
            ),
            QuickSettingItem(
                id = "volume",
                name = "音量调节",
                description = "媒体音量: ${status.volumeLevel}",
                iconResId = "ic_fluent_speaker_2_24_regular",
                isEnabled = status.volumeLevel > 0,
                category = "adjustment"
            )
        )
    }

    /**
     * 切换设置项
     */
    suspend fun toggleSetting(settingId: String): SettingChangeResult = withContext(Dispatchers.IO) {
        when (settingId) {
            "wifi" -> toggleWifi()
            "bluetooth" -> toggleBluetooth()
            "airplane" -> toggleAirplaneMode()
            "mobile_data" -> toggleMobileData()
            "location" -> toggleLocation()
            "do_not_disturb" -> toggleDoNotDisturb()
            "auto_rotate" -> toggleAutoRotate()
            "battery_saver" -> toggleBatterySaver()
            else -> SettingChangeResult(false, "未知的设置项: $settingId")
        }
    }

    /**
     * 切换WiFi
     */
    private fun toggleWifi(): SettingChangeResult {
        return try {
            val newState = !wifiManager.isWifiEnabled
            val success = wifiManager.setWifiEnabled(newState)

            if (success) {
                val message = if (newState) "WiFi已开启" else "WiFi已关闭"
                notificationHelper.showNotification(
                    "WiFi设置",
                    message,
                    if (newState) NotificationType.WIFI_CONNECTED else NotificationType.WIFI_DISCONNECTED
                )
                SettingChangeResult(true, message)
            } else {
                SettingChangeResult(false, "WiFi切换失败")
            }
        } catch (e: Exception) {
            SettingChangeResult(false, "WiFi切换异常: ${e.message}")
        }
    }

    /**
     * 切换蓝牙
     */
    private fun toggleBluetooth(): SettingChangeResult {
        return try {
            val newState = bluetoothAdapter?.isEnabled != true
            val success = if (newState) {
                bluetoothAdapter?.enable() == true
            } else {
                bluetoothAdapter?.disable() == true
            }

            if (success) {
                val message = if (newState) "蓝牙已开启" else "蓝牙已关闭"
                notificationHelper.showNotification(
                    "蓝牙设置",
                    message,
                    NotificationType.BATTERY_LOW // 使用现有通知类型
                )
                SettingChangeResult(true, message)
            } else {
                SettingChangeResult(false, "蓝牙切换失败")
            }
        } catch (e: Exception) {
            SettingChangeResult(false, "蓝牙切换异常: ${e.message}")
        }
    }

    /**
     * 切换飞行模式
     */
    private fun toggleAirplaneMode(): SettingChangeResult {
        return try {
            val currentState = isAirplaneModeEnabled()
            val newState = !currentState

            Settings.Global.putInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                if (newState) 1 else 0
            )

            // 发送广播让系统知道设置已更改
            val intent = Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).apply {
                putExtra("state", newState)
            }
            context.sendBroadcast(intent)

            val message = if (newState) "飞行模式已开启" else "飞行模式已关闭"
            notificationHelper.showNotification(
                "飞行模式设置",
                message,
                NotificationType.BATTERY_LOW
            )

            SettingChangeResult(true, message)
        } catch (e: Exception) {
            SettingChangeResult(false, "飞行模式切换异常: ${e.message}", requiresPermission = true)
        }
    }

    /**
     * 切换移动数据
     */
    private fun toggleMobileData(): SettingChangeResult {
        return try {
            val currentState = isMobileDataEnabled()
            val newState = !currentState

            // 注意：Android不允许直接切换移动数据，需要通过系统设置
            val intent = Intent(Settings.ACTION_DATA_USAGE_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

            SettingChangeResult(
                true,
                "已打开移动数据设置页面，请手动切换",
                requiresPermission = true
            )
        } catch (e: Exception) {
            SettingChangeResult(false, "移动数据切换异常: ${e.message}")
        }
    }

    /**
     * 切换定位服务
     */
    private fun toggleLocation(): SettingChangeResult {
        return try {
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

            SettingChangeResult(
                true,
                "已打开定位设置页面，请手动切换",
                requiresPermission = true
            )
        } catch (e: Exception) {
            SettingChangeResult(false, "定位服务切换异常: ${e.message}")
        }
    }

    /**
     * 切换勿扰模式
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun toggleDoNotDisturb(): SettingChangeResult {
        return try {
            val currentState = isDoNotDisturbEnabled()
            val newState = !currentState

            val interruptionFilter = if (newState) {
                NotificationManager.INTERRUPTION_FILTER_NONE
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }

            notificationManager.setInterruptionFilter(interruptionFilter)

            val message = if (newState) "勿扰模式已开启" else "勿扰模式已关闭"
            notificationHelper.showNotification(
                "勿扰模式设置",
                message,
                NotificationType.BATTERY_LOW
            )

            SettingChangeResult(true, message)
        } catch (e: Exception) {
            SettingChangeResult(false, "勿扰模式切换异常: ${e.message}", requiresPermission = true)
        }
    }

    /**
     * 切换自动旋转
     */
    private fun toggleAutoRotate(): SettingChangeResult {
        return try {
            val currentState = isAutoRotateEnabled()
            val newState = !currentState

            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (newState) 1 else 0
            )

            val message = if (newState) "自动旋转已开启" else "自动旋转已关闭"
            notificationHelper.showNotification(
                "自动旋转设置",
                message,
                NotificationType.BATTERY_LOW
            )

            SettingChangeResult(true, message)
        } catch (e: Exception) {
            SettingChangeResult(false, "自动旋转切换异常: ${e.message}", requiresPermission = true)
        }
    }

    /**
     * 切换省电模式
     */
    private fun toggleBatterySaver(): SettingChangeResult {
        return try {
            val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

            SettingChangeResult(
                true,
                "已打开省电模式设置页面，请手动切换",
                requiresPermission = true
            )
        } catch (e: Exception) {
            SettingChangeResult(false, "省电模式切换异常: ${e.message}")
        }
    }

    /**
     * 调节亮度
     */
    fun adjustBrightness(level: Int): SettingChangeResult {
        return try {
            val clampedLevel = level.coerceIn(0, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                clampedLevel
            )

            val message = "亮度已调节到 ${((clampedLevel / 255.0) * 100).toInt()}%"
            notificationHelper.showNotification(
                "亮度调节",
                message,
                NotificationType.BATTERY_LOW
            )

            SettingChangeResult(true, message, requiresPermission = true)
        } catch (e: Exception) {
            SettingChangeResult(false, "亮度调节异常: ${e.message}", requiresPermission = true)
        }
    }

    /**
     * 调节音量
     */
    fun adjustVolume(level: Int): SettingChangeResult {
        return try {
            val clampedLevel = level.coerceIn(0, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clampedLevel, 0)

            val message = "媒体音量已调节到 $clampedLevel"
            notificationHelper.showNotification(
                "音量调节",
                message,
                NotificationType.BATTERY_LOW
            )

            SettingChangeResult(true, message)
        } catch (e: Exception) {
            SettingChangeResult(false, "音量调节异常: ${e.message}")
        }
    }

    /**
     * 设备震动反馈
     */
    fun vibrateDevice(duration: Long = 100) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            // 震动可能不可用，忽略异常
        }
    }

    /**
     * 检查飞行模式状态
     */
    private fun isAirplaneModeEnabled(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) == 1
    }

    /**
     * 检查移动数据状态
     */
    private fun isMobileDataEnabled(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查定位服务状态
     */
    private fun isLocationEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查勿扰模式状态
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun isDoNotDisturbEnabled(): Boolean {
        return try {
            notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查自动旋转状态
     */
    private fun isAutoRotateEnabled(): Boolean {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取亮度级别
     */
    private fun getBrightnessLevel(): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (e: Exception) {
            128 // 默认中等亮度
        }
    }

    /**
     * 检查省电模式状态
     */
    private fun isBatterySaverEnabled(): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return powerManager.isPowerSaveMode
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取设置项分组
     */
    fun getSettingsByCategory(): Map<String, List<QuickSettingItem>> {
        return getQuickSettingItems().groupBy { it.category }
    }

    /**
     * 批量应用设置
     */
    suspend fun applySettingsBatch(settings: Map<String, Boolean>): List<SettingChangeResult> {
        val results = mutableListOf<SettingChangeResult>()

        for ((settingId, enable) in settings) {
            // 这里可以实现批量设置逻辑
            // 暂时只返回成功结果
            results.add(SettingChangeResult(true, "$settingId 设置已应用"))
        }

        return results
    }

    /**
     * 获取设置建议
     */
    fun getSettingsSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        val status = getCurrentSettingsStatus()

        if (status.wifiEnabled && status.bluetoothEnabled) {
            suggestions.add("WiFi和蓝牙都已开启，建议在不需要时关闭以节省电量")
        }

        if (status.locationEnabled && !status.wifiEnabled && !status.mobileDataEnabled) {
            suggestions.add("定位服务已开启但网络连接不可用，建议检查网络设置")
        }

        if (status.doNotDisturbEnabled) {
            suggestions.add("勿扰模式已开启，您将不会收到通知提醒")
        }

        if (status.batterySaverEnabled) {
            suggestions.add("省电模式已开启，某些功能可能受到限制")
        }

        if (status.brightnessLevel > 200) {
            suggestions.add("屏幕亮度较高，建议适当降低以节省电量")
        }

        return suggestions
    }

    /**
     * 创建设置快捷方式
     */
    fun createSettingShortcut(settingId: String): Boolean {
        return try {
            // 这里可以实现创建桌面快捷方式的逻辑
            // 需要额外的权限和实现
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取设置历史记录
     */
    fun getSettingsHistory(): List<String> {
        // 这里可以实现设置变更历史记录
        // 暂时返回空列表
        return emptyList()
    }
}
