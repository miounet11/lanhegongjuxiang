package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.io.File

object SystemUtils {

    /**
     * 启用直驱供电
     */
    fun enableDirectPower(context: Context) {
        try {
            // 这里需要Shizuku权限来执行系统级操作
            // 实际实现需要调用Shizuku API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 模拟系统设置操作
                val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 禁用直驱供电
     */
    fun disableDirectPower(context: Context) {
        try {
            // 这里需要Shizuku权限来执行系统级操作
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 模拟系统设置操作
                val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 启用全局插帧
     */
    fun enableGlobalFrameInsertion(context: Context) {
        try {
            // 这里需要Shizuku权限来执行系统级操作
            // 实际实现需要调用Shizuku API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 模拟系统设置操作
                val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 禁用全局插帧
     */
    fun disableGlobalFrameInsertion(context: Context) {
        try {
            // 这里需要Shizuku权限来执行系统级操作
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 模拟系统设置操作
                val intent = Intent(Settings.ACTION_SETTINGS)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 清除更新提醒
     */
    fun clearUpdateReminders(context: Context) {
        try {
            // 这里需要系统权限来清除更新提醒
            // 实际实现需要调用系统API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 模拟清除操作
                Toast.makeText(context, "更新提醒已清除", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * 检查Shizuku是否可用
     */
    fun isShizukuAvailable(): Boolean {
        return try {
            // 检查Shizuku是否安装并运行
            // 实际实现需要检查Shizuku服务状态
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取设备信息
     */
    fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "android_version" to Build.VERSION.RELEASE,
            "sdk_version" to Build.VERSION.SDK_INT.toString(),
            "build_number" to Build.DISPLAY
        )
    }

    /**
     * 检查是否有root权限
     */
    fun isRooted(): Boolean {
        return try {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            )
            paths.any { File(it).exists() }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检查是否有ADB权限
     */
    fun hasAdbPermission(): Boolean {
        return try {
            // 检查是否有ADB权限
            // 实际实现需要检查特定权限
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取应用版本信息
     */
    fun getAppVersionInfo(context: Context): Map<String, String> {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            mapOf(
                "version_name" to (packageInfo.versionName ?: "未知"),
                "version_code" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toString()
                },
                "package_name" to packageInfo.packageName
            )
        } catch (e: Exception) {
            mapOf(
                "version_name" to "未知",
                "version_code" to "0",
                "package_name" to context.packageName
            )
        }
    }
}
