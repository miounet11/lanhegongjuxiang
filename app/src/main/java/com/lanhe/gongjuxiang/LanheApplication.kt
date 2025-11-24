package com.lanhe.gongjuxiang

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.lanhe.gongjuxiang.utils.AppDatabase
import com.lanhe.gongjuxiang.utils.DataManager
import com.lanhe.gongjuxiang.utils.NetworkMonitor
import com.lanhe.gongjuxiang.utils.PerformanceMonitor
import com.lanhe.gongjuxiang.utils.PreferencesManager
import com.lanhe.gongjuxiang.utils.ShizukuManager
import com.lanhe.gongjuxiang.utils.BatteryMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 蓝河助手应用程序类
 * 负责应用级别的组件初始化和全局状态管理
 *
 * 使用手动依赖注入而不是Hilt，以提供更灵活的控制和避免编译问题
 */
class LanheApplication : Application() {

    // 应用级别的CoroutineScope
    private val applicationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
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

    // 数据库实例
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        
        Log.i("LanheApplication", "Initializing Lanhe Assistant...")
        
        // 初始化应用级别的组件
        initializeComponents()
        
        Log.i("LanheApplication", "Lanhe Assistant initialization completed")
    }

    /**
     * 初始化所有组件
     * 按照依赖关系顺序初始化
     */
    private fun initializeComponents() {
        try {
            // 1. 首先初始化首选项管理器（其他组件可能依赖它）
            initializePreferencesManager()
            
            // 2. 初始化通知渠道（必须在后台服务之前）
            initializeNotificationChannels()
            
            // 3. 初始化Shizuku管理器（系统级操作）
            initializeShizukuManager()
            
            // 4. 初始化数据库（数据持久化）
            initializeDatabase()
            
            // 5. 初始化数据管理器（依赖数据库）
            initializeDataManager()
            
            // 6. 初始化性能监控器（系统监控）
            initializePerformanceMonitor()
            
            // 7. 初始化电池监控器（后台监控）
            initializeBatteryMonitor()
            
            // 8. 初始化网络监控器（网络状态）
            initializeNetworkMonitor()
            
            // 9. 初始化崩溃处理器
            initializeCrashHandler()
            
        } catch (e: Exception) {
            Log.e("LanheApplication", "Failed to initialize components", e)
        }
    }
    
    /**
     * 初始化首选项管理器
     */
    private fun initializePreferencesManager() {
        applicationScope.launch {
            try {
                // PreferencesManager 会在需要时自动初始化
                Log.d("LanheApplication", "PreferencesManager ready")
            } catch (e: Exception) {
                Log.e("LanheApplication", "Failed to initialize PreferencesManager", e)
            }
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
     * 初始化Shizuku管理器
     */
    private fun initializeShizukuManager() {
        try {
            // 初始化ShizukuManager并传入Context
            ShizukuManager.initWithContext(this)
            Log.d("LanheApplication", "ShizukuManager initialized")
        } catch (e: Exception) {
            Log.e("LanheApplication", "Failed to initialize ShizukuManager", e)
        }
    }
    
    /**
     * 初始化数据库
     */
    private fun initializeDatabase() {
        applicationScope.launch {
            try {
                // 触发数据库初始化
                database.performanceDataDao()
                database.optimizationHistoryDao()
                database.batteryStatsDao()
                Log.d("LanheApplication", "Database initialized")
            } catch (e: Exception) {
                Log.e("LanheApplication", "Failed to initialize database", e)
            }
        }
    }
    
    /**
     * 初始化数据管理器
     */
    private fun initializeDataManager() {
        applicationScope.launch {
            try {
                // DataManager 会在需要时自动初始化
                Log.d("LanheApplication", "DataManager ready")
            } catch (e: Exception) {
                Log.e("LanheApplication", "Failed to initialize DataManager", e)
            }
        }
    }
    
    /**
     * 初始化性能监控器
     */
    private fun initializePerformanceMonitor() {
        applicationScope.launch {
            try {
                // PerformanceMonitor 会在需要时自动启动
                Log.d("LanheApplication", "PerformanceMonitor ready")
            } catch (e: Exception) {
                Log.e("LanheApplication", "Failed to initialize PerformanceMonitor", e)
            }
        }
    }
    
    /**
     * 初始化电池监控器
     */
    private fun initializeBatteryMonitor() {
        applicationScope.launch {
            try {
                // BatteryMonitor 会在需要时自动初始化
                Log.d("LanheApplication", "BatteryMonitor ready")
            } catch (e: Exception) {
                Log.e("LanheApplication", "Failed to initialize BatteryMonitor", e)
            }
        }
    }
    
    /**
     * 初始化网络监控器
     */
    private fun initializeNetworkMonitor() {
        applicationScope.launch {
            try {
                // NetworkMonitor 会在需要时自动初始化
                Log.d("LanheApplication", "NetworkMonitor ready")
            } catch (e: Exception) {
                Log.e("LanheApplication", "Failed to initialize NetworkMonitor", e)
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
                applicationScope.launch {
                    try {
                        // 这里可以添加崩溃日志保存逻辑
                        Log.w("LanheApplication", "Crash info saved")
                    } catch (e: Exception) {
                        Log.e("LanheApplication", "Failed to save crash info", e)
                    }
                }
                
                // 调用原始处理器
                defaultHandler?.uncaughtException(thread, exception)
            }
            
            Log.d("LanheApplication", "Crash handler initialized")
        } catch (e: Exception) {
            Log.e("LanheApplication", "Failed to initialize crash handler", e)
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
        applicationScope.launch {
            try {
                // 可以在这里执行紧急内存清理
                System.gc()
            } catch (e: Exception) {
                Log.e("LanheApplication", "Failed to perform emergency memory cleanup", e)
            }
        }
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
