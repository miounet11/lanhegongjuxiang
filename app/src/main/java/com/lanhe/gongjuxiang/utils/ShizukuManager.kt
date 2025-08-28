package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import rikka.shizuku.ShizukuProvider
import android.content.pm.IPackageManager
import android.app.IActivityManager
import android.app.usage.IUsageStatsManager
import android.content.IContentProvider
import android.os.Bundle
import android.app.INotificationManager
import android.os.PowerManager
import android.os.IPowerManager
import android.app.IAlarmManager
import android.net.INetworkStatsService
import android.app.IAppOpsService
import android.hardware.ISensorPrivacyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shizuku权限管理器
 * 负责Shizuku服务的连接、权限检查和系统服务获取
 */
object ShizukuManager {

    // Shizuku状态
    private val _shizukuState = MutableStateFlow<ShizukuState>(ShizukuState.Unavailable)
    val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

    // 系统服务缓存
    private var packageManager: IPackageManager? = null
    private var activityManager: IActivityManager? = null
    private var usageStatsManager: IUsageStatsManager? = null
    private var notificationManager: INotificationManager? = null
    private var powerManager: IPowerManager? = null
    private var settingsProvider: IContentProvider? = null

    init {
        // 初始化Shizuku监听器
        Shizuku.addBinderReceivedListenerSticky {
            updateShizukuState()
        }
        Shizuku.addBinderDeadListener {
            _shizukuState.value = ShizukuState.Unavailable
        }
        updateShizukuState()
    }

    /**
     * 更新Shizuku状态
     */
    private fun updateShizukuState() {
        if (!Shizuku.pingBinder()) {
            _shizukuState.value = ShizukuState.Unavailable
            return
        }

        _shizukuState.value = if (Shizuku.checkSelfPermission() == 0) {
            ShizukuState.Granted
        } else {
            ShizukuState.Denied
        }
    }

    /**
     * 请求Shizuku权限
     */
    fun requestPermission(context: Context) {
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            // 显示权限说明
            Toast.makeText(context, "需要Shizuku权限来执行系统级操作", Toast.LENGTH_LONG).show()
        }
        Shizuku.requestPermission(0)
    }

    /**
     * 检查Shizuku是否可用
     */
    fun isShizukuAvailable(): Boolean {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == 0
    }

    /**
     * 获取包管理器
     */
    fun getPackageManager(): IPackageManager? {
        if (!isShizukuAvailable()) return null
        if (packageManager == null) {
            packageManager = IPackageManager.Stub.asInterface(
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
            )
        }
        return packageManager
    }

    /**
     * 获取活动管理器
     */
    fun getActivityManager(): IActivityManager? {
        if (!isShizukuAvailable()) return null
        if (activityManager == null) {
            activityManager = IActivityManager.Stub.asInterface(
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity"))
            )
        }
        return activityManager
    }

    /**
     * 获取使用统计管理器
     */
    fun getUsageStatsManager(): IUsageStatsManager? {
        if (!isShizukuAvailable()) return null
        if (usageStatsManager == null) {
            usageStatsManager = IUsageStatsManager.Stub.asInterface(
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService("usagestats"))
            )
        }
        return usageStatsManager
    }

    /**
     * 获取通知管理器
     */
    fun getNotificationManager(): INotificationManager? {
        if (!isShizukuAvailable()) return null
        if (notificationManager == null) {
            notificationManager = INotificationManager.Stub.asInterface(
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService("notification"))
            )
        }
        return notificationManager
    }

    /**
     * 获取电源管理器
     */
    fun getPowerManager(): IPowerManager? {
        if (!isShizukuAvailable()) return null
        if (powerManager == null) {
            powerManager = IPowerManager.Stub.asInterface(
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService("power"))
            )
        }
        return powerManager
    }

    /**
     * 获取设置内容提供者
     */
    fun getSettingsProvider(): IContentProvider? {
        if (!isShizukuAvailable()) return null
        if (settingsProvider == null) {
            settingsProvider = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("settings"))
        }
        return settingsProvider
    }

    /**
     * 执行系统设置操作
     */
    fun putSystemSetting(key: String, value: String, userId: Int = 0): Boolean {
        return try {
            val provider = getSettingsProvider() ?: return false
            val extras = Bundle().apply {
                putString("android:setting:put", "system")
                putString("android:setting:key", key)
                putString("android:setting:value", value)
                putInt("android:setting:user", userId)
            }
            provider.call(null, "PUT_system", null, extras) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取系统设置值
     */
    fun getSystemSetting(key: String, userId: Int = 0): String? {
        return try {
            val provider = getSettingsProvider() ?: return null
            val extras = Bundle().apply {
                putString("android:setting:get", "system")
                putString("android:setting:key", key)
                putInt("android:setting:user", userId)
            }
            val result = provider.call(null, "GET_system", null, extras)
            result?.getString("value")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 执行全局设置操作
     */
    fun putGlobalSetting(key: String, value: String, userId: Int = 0): Boolean {
        return try {
            val provider = getSettingsProvider() ?: return false
            val extras = Bundle().apply {
                putString("android:setting:put", "global")
                putString("android:setting:key", key)
                putString("android:setting:value", value)
                putInt("android:setting:user", userId)
            }
            provider.call(null, "PUT_global", null, extras) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取全局设置值
     */
    fun getGlobalSetting(key: String, userId: Int = 0): String? {
        return try {
            val provider = getSettingsProvider() ?: return null
            val extras = Bundle().apply {
                putString("android:setting:get", "global")
                putString("android:setting:key", key)
                putInt("android:setting:user", userId)
            }
            val result = provider.call(null, "GET_global", null, extras)
            result?.getString("value")
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Shizuku状态枚举
 */
enum class ShizukuState {
    Unavailable,  // Shizuku不可用
    Denied,       // 权限被拒绝
    Granted       // 权限已授予
}
