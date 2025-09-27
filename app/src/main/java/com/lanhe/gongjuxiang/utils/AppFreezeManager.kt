package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 应用冻结管理器
 * 负责应用的冻结和解冻操作
 */
class AppFreezeManager(private val context: Context) {

    private val packageManager = context.packageManager
    private val shizukuManager = ShizukuManager

    /**
     * 冻结应用
     */
    suspend fun freezeApp(packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Android 7.0+ 使用隐藏API
                    if (shizukuManager.isShizukuAvailable()) {
                        return@withContext freezeAppWithShizuku(packageName)
                    }
                }
                
                // 备用方案：禁用应用
                return@withContext disableApp(packageName)
            } catch (e: Exception) {
                Log.e("AppFreezeManager", "冻结应用失败: $packageName", e)
                false
            }
        }
    }

    /**
     * 解冻应用
     */
    suspend fun unfreezeApp(packageName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (shizukuManager.isShizukuAvailable()) {
                        return@withContext unfreezeAppWithShizuku(packageName)
                    }
                }
                
                // 备用方案：启用应用
                return@withContext enableApp(packageName)
            } catch (e: Exception) {
                Log.e("AppFreezeManager", "解冻应用失败: $packageName", e)
                false
            }
        }
    }

    /**
     * 批量冻结应用
     */
    suspend fun freezeApps(packageNames: List<String>): Map<String, Boolean> {
        return withContext(Dispatchers.IO) {
            val results = mutableMapOf<String, Boolean>()
            
            packageNames.forEach { packageName ->
                results[packageName] = freezeApp(packageName)
            }
            
            results
        }
    }

    /**
     * 获取可冻结的应用列表
     */
    fun getFreezableApps(): List<AppInfo> {
        return try {
            val packages = packageManager.getInstalledPackages(0)
            val freezableApps = mutableListOf<AppInfo>()
            
            packages.forEach { packageInfo ->
                val appInfo = packageInfo.applicationInfo ?: return@forEach

                // 跳过系统应用和当前应用
                if (isSystemApp(appInfo) || appInfo.packageName == context.packageName) {
                    return@forEach
                }

                // 检查应用是否已冻结
                val isFrozen = isAppFrozen(appInfo.packageName)

                freezableApps.add(
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        isFrozen = isFrozen,
                        isSystemApp = isSystemApp(appInfo)
                    )
                )
            }
            
            freezableApps.sortedBy { it.appName }
        } catch (e: Exception) {
            Log.e("AppFreezeManager", "获取可冻结应用列表失败", e)
            emptyList()
        }
    }

    /**
     * 使用Shizuku冻结应用
     */
    private suspend fun freezeAppWithShizuku(packageName: String): Boolean {
        return try {
            // 这里需要实现Shizuku的隐藏API调用
            // 由于隐藏API的限制，暂时返回false
            Log.i("AppFreezeManager", "Shizuku冻结功能需要进一步实现")
            false
        } catch (e: Exception) {
            Log.e("AppFreezeManager", "Shizuku冻结失败", e)
            false
        }
    }

    /**
     * 使用Shizuku解冻应用
     */
    private suspend fun unfreezeAppWithShizuku(packageName: String): Boolean {
        return try {
            // 这里需要实现Shizuku的隐藏API调用
            Log.i("AppFreezeManager", "Shizuku解冻功能需要进一步实现")
            false
        } catch (e: Exception) {
            Log.e("AppFreezeManager", "Shizuku解冻失败", e)
            false
        }
    }

    /**
     * 禁用应用（备用方案）
     */
    private fun disableApp(packageName: String): Boolean {
        return try {
            packageManager.setApplicationEnabledSetting(
                packageName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            true
        } catch (e: Exception) {
            Log.e("AppFreezeManager", "禁用应用失败", e)
            false
        }
    }

    /**
     * 启用应用（备用方案）
     */
    private fun enableApp(packageName: String): Boolean {
        return try {
            packageManager.setApplicationEnabledSetting(
                packageName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            true
        } catch (e: Exception) {
            Log.e("AppFreezeManager", "启用应用失败", e)
            false
        }
    }

    /**
     * 检查应用是否已冻结
     */
    private fun isAppFrozen(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            !appInfo.enabled
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 判断是否为系统应用
     */
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
               (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    /**
     * 获取应用使用统计
     */
    fun getAppUsageStats(): List<AppUsageInfo> {
        return try {
            val packages = packageManager.getInstalledPackages(0)
            val usageStats = mutableListOf<AppUsageInfo>()
            
            packages.forEach { packageInfo ->
                val appInfo = packageInfo.applicationInfo ?: return@forEach

                if (isSystemApp(appInfo) || appInfo.packageName == context.packageName) {
                    return@forEach
                }

                usageStats.add(
                    AppUsageInfo(
                        packageName = appInfo.packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        installTime = packageInfo.firstInstallTime,
                        lastUpdateTime = packageInfo.lastUpdateTime,
                        versionName = packageInfo.versionName,
                        versionCode = packageInfo.longVersionCode
                    )
                )
            }
            
            usageStats.sortedByDescending { it.lastUpdateTime }
        } catch (e: Exception) {
            Log.e("AppFreezeManager", "获取应用使用统计失败", e)
            emptyList()
        }
    }
}

/**
 * 应用信息类
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isFrozen: Boolean = false,
    val isSystemApp: Boolean = false
)

/**
 * 应用使用信息类
 */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val installTime: Long,
    val lastUpdateTime: Long,
    val versionName: String?,
    val versionCode: Long
)