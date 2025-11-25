package com.lanhe.gongjuxiang

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.lanhe.gongjuxiang.utils.ShizukuManager

/**
 * 蓝河助手应用程序类
 * 负责应用级别的组件初始化和全局状态管理
 *
 * 使用Hilt依赖注入框架管理依赖关系
 */
@HiltAndroidApp
class LanheApplication : Application() {
    
    // 单例实例
    companion object {
        @Volatile
        private var INSTANCE: LanheApplication? = null
        
        fun getInstance(): LanheApplication {
            return INSTANCE ?: throw IllegalStateException("Application not initialized")
        }
        
        fun getContext(): Context {
            return getInstance().applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        Log.i("LanheApplication", "Initializing Lanhe Assistant with Hilt...")

        // 初始化应用级别的组件（Hilt会自动处理依赖注入）
        initializeComponents()

        Log.i("LanheApplication", "Lanhe Assistant initialization completed")
    }

    /**
     * 初始化所有组件
     * Hilt会自动处理依赖注入，这里只处理必要的初始化
     */
    private fun initializeComponents() {
        try {
            // 初始化通知渠道（必须在后台服务之前）
            initializeNotificationChannels()

            // 初始化崩溃处理器
            initializeCrashHandler()

            // 初始化内置Shizuku APK
            initializeBuiltInShizuku()

        } catch (e: Exception) {
            Log.e("LanheApplication", "Failed to initialize components", e)
        }
    }
    /**
     * 初始化通知渠道
     */
    private fun initializeNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                // 主通知渠道
                val mainChannel = NotificationChannel(
                    "main_channel",
                    "主要通知",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "应用主要功能通知"
                    setShowBadge(true)
                }
                
                // 优化结果通知渠道
                val optimizationChannel = NotificationChannel(
                    "optimization_channel",
                    "优化通知",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "系统优化结果通知"
                    setShowBadge(false)
                }
                
                // 监控服务通知渠道
                val monitoringChannel = NotificationChannel(
                    "monitoring_channel",
                    "监控服务",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "后台监控服务通知"
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
                
                // 电池管理通知渠道
                val batteryChannel = NotificationChannel(
                    "battery_channel",
                    "电池管理",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "电池优化和充电提醒"
                    setShowBadge(true)
                }
                
                notificationManager.createNotificationChannels(
                    listOf(mainChannel, optimizationChannel, monitoringChannel, batteryChannel)
                )
                
                Log.d("LanheApplication", "Notification channels created")
            } catch (e: Exception) {
                Log.e("LanheApplication", "Failed to create notification channels", e)
            }
        }
    }

    /**
     * 初始化崩溃处理器
     */
    private fun initializeCrashHandler() {
        try {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

            Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
                Log.e("LanheApplication", "Uncaught exception in thread ${thread.name}", exception)

                // 保存崩溃信息
                // 这里可以添加崩溃日志保存逻辑

                // 调用原始处理器
                defaultHandler?.uncaughtException(thread, exception)
            }

            Log.d("LanheApplication", "Crash handler initialized")
        } catch (e: Exception) {
            Log.e("LanheApplication", "Failed to initialize crash handler", e)
        }
    }

    /**
     * 初始化内置Shizuku APK
     * 在应用启动时自动检查和安装（如需要）
     */
    private fun initializeBuiltInShizuku() {
        try {
            Log.i("LanheApplication", "Starting built-in Shizuku initialization...")
            ShizukuManager.initializeBuiltInShizuku(this)
            Log.i("LanheApplication", "Built-in Shizuku initialization completed")
        } catch (e: Exception) {
            Log.e("LanheApplication", "Failed to initialize built-in Shizuku", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.i("LanheApplication", "Application terminated")
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w("LanheApplication", "System is running low on memory")

        // 执行内存清理
        System.gc()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d("LanheApplication", "Memory trim requested with level: $level")
        
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.w("LanheApplication", "Critical memory situation")
                System.gc()
            }
            TRIM_MEMORY_RUNNING_LOW -> {
                Log.w("LanheApplication", "Low memory situation")
            }
            TRIM_MEMORY_RUNNING_MODERATE -> {
                Log.d("LanheApplication", "Moderate memory situation")
            }
        }
    }
}
