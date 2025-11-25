package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * 应用管理器
 * 提供应用的安装、卸载、查询等功能
 */
class AppManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * 获取所有已安装应用
     */
    fun getInstalledApps(includeSystem: Boolean = false): List<ApplicationInfo> {
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }

        return if (includeSystem) {
            apps
        } else {
            apps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
        }
    }

    /**
     * 获取应用信息
     */
    fun getAppInfo(packageName: String): ApplicationInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * 获取应用包信息
     */
    fun getPackageInfo(packageName: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * 检查应用是否已安装
     */
    fun isAppInstalled(packageName: String): Boolean {
        return getAppInfo(packageName) != null
    }

    /**
     * 启动应用
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 打开应用设置页面
     */
    fun openAppSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * 获取应用大小（需要权限）
     */
    fun getAppSize(packageName: String): Long {
        return try {
            val appInfo = getAppInfo(packageName)
            if (appInfo != null) {
                val sourceDir = appInfo.sourceDir
                if (sourceDir != null) {
                    java.io.File(sourceDir).length()
                } else {
                    0L
                }
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 获取应用版本名称
     */
    fun getAppVersionName(packageName: String): String? {
        return getPackageInfo(packageName)?.versionName
    }

    /**
     * 获取应用版本号
     */
    fun getAppVersionCode(packageName: String): Long {
        val packageInfo = getPackageInfo(packageName) ?: return 0L
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    /**
     * 获取应用名称
     */
    fun getAppName(packageName: String): String? {
        return try {
            val appInfo = getAppInfo(packageName)
            appInfo?.loadLabel(packageManager)?.toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取运行中的应用（需要权限）
     */
    fun getRunningApps(): List<String> {
        // 这个功能在新版本Android中受限
        // 可以通过UsageStatsManager获取部分信息
        return emptyList()
    }
}