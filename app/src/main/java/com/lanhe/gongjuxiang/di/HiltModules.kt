package com.lanhe.gongjuxiang.di

import android.content.Context
import com.lanhe.gongjuxiang.utils.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Hilt模块定义 - 应用级单例组件
 * 提供全局单例依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供应用级CoroutineScope
     */
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    /**
     * 提供数据库实例
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * 提供性能数据DAO
     */
    @Provides
    @Singleton
    fun providePerformanceDataDao(
        database: AppDatabase
    ): PerformanceDataDao {
        return database.performanceDataDao()
    }

    /**
     * 提供优化历史DAO
     */
    @Provides
    @Singleton
    fun provideOptimizationHistoryDao(
        database: AppDatabase
    ): OptimizationHistoryDao {
        return database.optimizationHistoryDao()
    }

    /**
     * 提供电池统计DAO
     */
    @Provides
    @Singleton
    fun provideBatteryStatsDao(
        database: AppDatabase
    ): BatteryStatsDao {
        return database.batteryStatsDao()
    }
}

/**
 * 管理器模块 - 提供核心管理器单例
 */
@Module
@InstallIn(SingletonComponent::class)
object ManagerModule {

    /**
     * 提供ShizukuManager单例
     */
    @Provides
    @Singleton
    fun provideShizukuManager(
        @ApplicationContext context: Context
    ): ShizukuManager {
        ShizukuManager.initWithContext(context)
        return ShizukuManager
    }

    /**
     * 提供性能监控器
     */
    @Provides
    @Singleton
    fun providePerformanceMonitor(
        @ApplicationContext context: Context
    ): PerformanceMonitor {
        return PerformanceMonitor(context)
    }

    /**
     * 提供真实性能监控管理器
     */
    @Provides
    @Singleton
    fun provideRealPerformanceMonitorManager(
        @ApplicationContext context: Context
    ): RealPerformanceMonitorManager {
        return RealPerformanceMonitorManager(context)
    }

    /**
     * 提供数据管理器
     */
    @Provides
    @Singleton
    fun provideDataManager(
        @ApplicationContext context: Context
    ): DataManager {
        return DataManager(context)
    }

    /**
     * 提供系统优化器
     */
    @Provides
    @Singleton
    fun provideSystemOptimizer(
        @ApplicationContext context: Context
    ): SystemOptimizer {
        return SystemOptimizer(context)
    }

    /**
     * 提供电池监控器
     */
    @Provides
    @Singleton
    fun provideBatteryMonitor(
        @ApplicationContext context: Context
    ): BatteryMonitor {
        return BatteryMonitor(context)
    }

    /**
     * 提供网络监控器
     */
    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }

    /**
     * 提供WiFi优化器
     */
    @Provides
    @Singleton
    fun provideWifiOptimizer(
        @ApplicationContext context: Context
    ): WifiOptimizer {
        return WifiOptimizer(context)
    }

    /**
     * 提供网络管理器
     */
    @Provides
    @Singleton
    fun provideNetworkManager(
        @ApplicationContext context: Context
    ): NetworkOptimizer {  // 使用正确的类名
        return NetworkOptimizer() // 使用无参构造函数
    }

    /**
     * 提供智能清理器
     */
    @Provides
    @Singleton
    fun provideSmartCleaner(
        @ApplicationContext context: Context
    ): SmartCleaner {
        return SmartCleaner(context)
    }

    /**
     * 提供安全管理器
     */
    @Provides
    @Singleton
    fun provideSecurityManager(
        @ApplicationContext context: Context
    ): SecurityManager {
        return SecurityManager(context)
    }

    /**
     * 提供通知管理器
     */
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): SmartNotificationManager {  // 使用正确的类名
        return SmartNotificationManager(context)
    }

    /**
     * 提供首选项管理器
     */
    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager {
        return PreferencesManager(context)
    }

    /**
     * 提供存储优化器
     * 暂时注释掉，因为StorageOptimizer类不存在
     */
    /*
    @Provides
    @Singleton
    fun provideStorageOptimizer(
        @ApplicationContext context: Context
    ): StorageOptimizer {
        return StorageOptimizer(context)
    }
    */

    /**
     * 提供内存优化器
     * 暂时注释掉，因为MemoryOptimizer类不存在
     */
    /*
    @Provides
    @Singleton
    fun provideMemoryOptimizer(
        @ApplicationContext context: Context
    ): MemoryOptimizer {
        return MemoryOptimizer(context)
    }
    */

    /**
     * 提供CPU优化器
     * 注意: RealCpuGpuPerformanceTuner已删除，使用基础CpuGpuPerformanceTuner
     */
    @Provides
    @Singleton
    fun provideCpuOptimizer(
        @ApplicationContext context: Context
    ): CpuGpuPerformanceTuner {
        return CpuGpuPerformanceTuner(context)
    }

    /**
     * 提供应用管理器
     */
    @Provides
    @Singleton
    fun provideAppManager(
        @ApplicationContext context: Context
    ): AppManager {
        return AppManager(context)
    }
}

/**
 * Activity模块 - 提供Activity作用域的依赖
 */
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {

    /**
     * 提供Activity作用域的CoroutineScope
     */
    @Provides
    @ActivityScoped
    fun provideActivityScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Main + SupervisorJob())
    }
}

/**
 * ViewModel模块 - 提供ViewModel作用域的依赖
 */
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    /**
     * 提供ViewModel作用域的CoroutineScope
     */
    @Provides
    @ViewModelScoped
    fun provideViewModelScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Default + SupervisorJob())
    }
}

/**
 * Service模块 - 提供Service作用域的依赖
 */
@Module
@InstallIn(ServiceComponent::class)
object ServiceModule {

    /**
     * 提供Service作用域的CoroutineScope
     */
    @Provides
    @ServiceScoped
    fun provideServiceScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}

/**
 * Repository模块 - 提供Repository层的依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /**
     * 提供性能数据仓库
     * 暂时注释掉，因为PerformanceRepository类已删除
     */
    /*
    @Provides
    @Singleton
    fun providePerformanceRepository(
        performanceDataDao: PerformanceDataDao,
        performanceMonitor: RealPerformanceMonitorManager,
        scope: CoroutineScope
    ): PerformanceRepository {
        return PerformanceRepository(
            performanceDataDao,
            performanceMonitor,
            scope
        )
    }
    */

    /**
     * 提供优化历史仓库
     * 暂时注释掉，因为OptimizationRepository类已删除
     */
    /*
    @Provides
    @Singleton
    fun provideOptimizationRepository(
        optimizationHistoryDao: OptimizationHistoryDao,
        systemOptimizer: SystemOptimizer,
        scope: CoroutineScope
    ): OptimizationRepository {
        return OptimizationRepository(
            optimizationHistoryDao,
            systemOptimizer,
            scope
        )
    }
    */

    /**
     * 提供电池统计仓库
     * 暂时注释掉，因为BatteryRepository类已删除
     */
    /*
    @Provides
    @Singleton
    fun provideBatteryRepository(
        batteryStatsDao: BatteryStatsDao,
        batteryMonitor: BatteryMonitor,
        scope: CoroutineScope
    ): BatteryRepository {
        return BatteryRepository(
            batteryStatsDao,
            batteryMonitor,
            scope
        )
    }
    */
}