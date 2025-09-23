[根目录](../CLAUDE.md) > **app**

# 蓝河助手主应用模块文档

## 变更记录 (Changelog)

**最新扫描时间：** 2025-09-15 13:45:51
- 深度补捞：工具类完整实现分析
- 新增Room数据库架构详情
- 完善Shizuku权限管理系统
- 补充Material Design界面布局
- 更新MainActivity完整实现

## 模块职责

蓝河助手主应用模块，采用MVVM架构模式，集成了20+系统优化和管理功能。核心职责包括：

- **系统优化引擎：** 电池、内存、CPU、网络全面优化
- **性能监控：** 实时监控系统运行状态
- **Shizuku权限管理：** 高级系统权限操作
- **智能浏览器：** 内置WebView浏览器
- **文件管理：** 完整的文件浏览和管理功能

## 入口与启动

### 主要入口点
- **MainActivity.kt** - 主界面，采用Navigation + BottomNavigation架构
- **LanheApplication.kt** - 应用程序入口，全局初始化
- **AndroidManifest.xml** - 应用配置，权限声明

### 启动流程
```kotlin
LanheApplication.onCreate() 
    → ShizukuManager.init()
    → DataManager.init()
    → MainActivity.onCreate()
    → Fragment导航初始化
```

## 对外接口

### 核心功能接口

#### 1. 系统优化API
```kotlin
// SystemOptimizer - 系统优化器
class SystemOptimizer(context: Context) {
    suspend fun performFullOptimization(): OptimizationResult
    suspend fun performBatteryOptimization(): OptimizationItem
    suspend fun performMemoryCleanup(): OptimizationItem
    suspend fun performCpuOptimization(): OptimizationItem
    suspend fun performNetworkOptimization(): OptimizationItem
}
```

#### 2. Shizuku权限管理API
```kotlin
// ShizukuManager - 权限管理器
object ShizukuManager {
    fun isShizukuAvailable(): Boolean
    fun requestPermission(context: Context)
    fun getRunningProcesses(): List<ProcessInfo>
    fun getCpuUsage(): Float
    fun getMemoryInfo(): MemoryInfo
    fun boostSystemPerformance(): PerformanceBoostResult
}
```

#### 3. 性能监控API
```kotlin
// PerformanceMonitor - 性能监控
class PerformanceMonitor {
    fun startMonitoring()
    fun getCpuUsage(): Float
    fun getMemoryUsage(): MemoryUsage
    fun getBatteryInfo(): BatteryInfo
    fun getNetworkStats(): NetworkStats
}
```

### 布局架构

#### Material Design 3.0布局系统
- **CoordinatorLayout + AppBarLayout** - 主界面协调布局
- **DrawerLayout + NavigationView** - 侧边抽屉导航
- **BottomNavigationView** - 底部导航栏
- **Fragment + Navigation Component** - 页面导航管理

#### 关键布局文件
- `activity_main.xml` - 主界面布局（Toolbar + Drawer + Bottom Nav）
- `fragment_dashboard.xml` - 仪表盘界面（系统状态 + 功能网格）
- `content_main.xml` - 主内容区域布局
- `nav_header_main.xml` - 导航抽屉头部

## 关键依赖与配置

### 主要依赖
```kotlin
dependencies {
    // Android核心
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7"
    implementation "androidx.room:room-runtime:2.7.0"
    implementation "androidx.navigation:navigation-fragment-ktx:2.7.6"
    
    // Shizuku权限框架
    implementation "dev.rikka.shizuku:api:13.1.0"
    implementation "dev.rikka.shizuku:provider:13.1.0"
    
    // 网络框架
    implementation "com.squareup.retrofit2:retrofit:2.9.0"
    implementation "com.squareup.okhttp3:okhttp:4.12.0"
    
    // 隐藏API访问
    implementation "org.lsposed.hiddenapibypass:hiddenapibypass:4.3"
}
```

### 权限配置
```xml
<!-- 基础权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.BATTERY_STATS" />

<!-- Shizuku权限 -->
<uses-permission android:name="moe.shizuku.manager.permission.API_V23" />

<!-- 系统管理权限 -->
<uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />
<uses-permission android:name="android.permission.GET_TASKS" />
```

## 数据模型

### Room数据库架构

#### 1. 数据库定义
```kotlin
@Database(
    entities = [
        PerformanceDataEntity::class,
        OptimizationHistoryEntity::class, 
        BatteryStatsEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun performanceDataDao(): PerformanceDataDao
    abstract fun optimizationHistoryDao(): OptimizationHistoryDao
    abstract fun batteryStatsDao(): BatteryStatsDao
}
```

#### 2. 核心实体类
```kotlin
// 性能数据实体
@Entity(tableName = "performance_data")
data class PerformanceDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val cpuUsage: Float,
    val memoryUsagePercent: Int,
    val batteryLevel: Int,
    val batteryTemperature: Float,
    val deviceTemperature: Float,
    val dataType: String = "performance"
)

// 优化历史实体
@Entity(tableName = "optimization_history")
data class OptimizationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val optimizationType: String,
    val success: Boolean,
    val message: String,
    val improvements: String,
    val duration: Long
)
```

### 业务数据模型
```kotlin
// 性能指标
data class PerformanceData(
    val cpuUsage: Float,
    val memoryUsage: MemoryInfo,
    val batteryInfo: BatteryInfo,
    val networkStats: NetworkStats,
    val storageUsage: Float
)

// 优化结果
data class OptimizationResult(
    val success: Boolean,
    val message: String,
    val batteryOptimization: OptimizationItem,
    val memoryCleanup: OptimizationItem,
    val cpuOptimization: OptimizationItem,
    val networkOptimization: OptimizationItem
)
```

## 测试与质量

### 单元测试覆盖
- `ExampleUnitTest.kt` - 基础单元测试示例
- utils包核心工具类测试

### 仪器测试覆盖  
- `ExampleInstrumentedTest.kt` - 基础仪器测试示例
- Activity UI交互测试

### 代码质量工具
- **ProGuard** - 代码混淆配置
- **Lint** - 静态代码分析
- **Android Studio分析工具** - 内存泄漏检测

## 常见问题 (FAQ)

### Q: Shizuku权限如何获取？
A: 需要用户手动安装Shizuku应用并通过ADB或root方式启动服务，然后在应用中授权。

### Q: 系统优化功能的实现原理？
A: 基于Android隐藏API和Shizuku权限，通过系统服务调用实现进程管理、性能调优等功能。

### Q: 为什么某些优化功能暂时禁用？
A: 部分功能需要更高级的Shizuku API支持，当前版本采用保守实现策略。

### Q: 如何添加新的功能模块？
A: 在utils包中创建对应的Manager类，在MainActivity中添加导航入口，在数据库中添加相关实体。

## 相关文件清单

### 核心工具类（38个）
```
utils/
├── AppDatabase.kt                 # Room数据库主类
├── ShizukuManager.kt             # Shizuku权限管理器（710行）
├── SystemOptimizer.kt            # 系统优化器（578行）
├── PerformanceMonitor.kt         # 性能监控器
├── NetworkManager.kt             # 网络管理器
├── BatteryMonitor.kt             # 电池监控器
├── SmartCleaner.kt               # 智能清理器
├── SecurityManager.kt            # 安全管理器
├── DataManager.kt                # 数据管理器
├── PreferencesManager.kt         # 配置管理器
└── ... （29个其他工具类）
```

### 主要Activity（26个）
```
activities/
├── MainActivity.kt               # 主界面（321行）
├── SystemMonitorActivity.kt     # 系统监控
├── PerformanceComparisonActivity.kt # 性能对比
├── NetworkDiagnosticActivity.kt # 网络诊断
├── BatteryManagerActivity.kt    # 电池管理
├── MemoryManagerActivity.kt     # 内存管理
├── CpuManagerActivity.kt        # CPU管理
├── StorageManagerActivity.kt    # 存储管理
└── ... （18个其他Activity）
```

### 布局文件（53个）
```
res/layout/
├── activity_main.xml             # 主界面布局
├── fragment_dashboard.xml       # 仪表盘界面（675行）
├── content_main.xml              # 主内容区域
├── app_bar_main.xml              # 应用栏布局
├── nav_header_main.xml           # 导航头部
└── ... （48个其他布局文件）
```

## 变更记录 (Changelog)

**2025-09-15 13:45:51**
- 完成深度补捞分析
- 新增数据库架构详情
- 完善Shizuku权限系统文档
- 补充Material Design布局分析