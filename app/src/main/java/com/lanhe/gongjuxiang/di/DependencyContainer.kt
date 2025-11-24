package com.lanhe.gongjuxiang.di

import android.content.Context
import com.lanhe.gongjuxiang.LanheApplication
import com.lanhe.gongjuxiang.utils.AppDatabase
import com.lanhe.gongjuxiang.utils.BatteryMonitor
import com.lanhe.gongjuxiang.utils.DataManager
import com.lanhe.gongjuxiang.utils.NetworkMonitor
import com.lanhe.gongjuxiang.utils.PerformanceMonitor
import com.lanhe.gongjuxiang.utils.PreferencesManager
import com.lanhe.gongjuxiang.utils.ShizukuManager
import com.lanhe.gongjuxiang.utils.SystemOptimizer

/**
 * 依赖注入容器
 * 手动管理依赖，避免使用 Dagger/Hilt
 */
object DependencyContainer {

    private var _appContext: Context? = null
    private val appContext: Context get() = _appContext ?: LanheApplication.getContext()

    // 缓存的实例
    private var _database: AppDatabase? = null
    private var _preferencesManager: PreferencesManager? = null
    private var _dataManager: DataManager? = null
    private var _performanceMonitor: PerformanceMonitor? = null
    private var _batteryMonitor: BatteryMonitor? = null
    private var _networkMonitor: NetworkMonitor? = null

    /**
     * 初始化容器
     */
    fun init(context: Context) {
        _appContext = context.applicationContext
    }

    /**
     * 获取数据库实例
     */
    fun getDatabase(): AppDatabase {
        return _database ?: synchronized(this) {
            _database ?: AppDatabase.getDatabase(appContext).also { _database = it }
        }
    }

    /**
     * 获取首选项管理器
     */
    fun getPreferencesManager(): PreferencesManager {
        return _preferencesManager ?: synchronized(this) {
            _preferencesManager ?: PreferencesManager(appContext).also { _preferencesManager = it }
        }
    }

    /**
     * 获取数据管理器
     */
    fun getDataManager(): DataManager {
        return _dataManager ?: synchronized(this) {
            _dataManager ?: DataManager(appContext).also { _dataManager = it }
        }
    }

    /**
     * 获取性能监控器
     */
    fun getPerformanceMonitor(): PerformanceMonitor {
        return _performanceMonitor ?: synchronized(this) {
            _performanceMonitor ?: PerformanceMonitor(appContext).also { _performanceMonitor = it }
        }
    }

    /**
     * 获取电池监控器
     */
    fun getBatteryMonitor(): BatteryMonitor {
        return _batteryMonitor ?: synchronized(this) {
            _batteryMonitor ?: BatteryMonitor(appContext).also { _batteryMonitor = it }
        }
    }

    /**
     * 获取网络监控器
     */
    fun getNetworkMonitor(): NetworkMonitor {
        return _networkMonitor ?: synchronized(this) {
            _networkMonitor ?: NetworkMonitor(appContext).also { _networkMonitor = it }
        }
    }

    /**
     * 创建系统优化器实例
     */
    fun createSystemOptimizer(): SystemOptimizer {
        return SystemOptimizer(appContext)
    }

    /**
     * 清理资源
     */
    fun clear() {
        _performanceMonitor = null
        _batteryMonitor = null
        _networkMonitor = null
        _dataManager = null
        _preferencesManager = null
        _database = null
        _appContext = null
    }
}

