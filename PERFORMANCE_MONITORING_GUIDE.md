# 真实性能监控功能实现指南

## 概述

本项目已成功实现了真实的性能监控功能，完全替换了原有的硬编码占位符数据。新的监控系统提供：

- **真实的CPU使用率监控** - 通过读取/proc/stat和Shizuku API
- **详细的内存使用统计** - 从系统API和/proc/meminfo获取
- **完整的电池状态监控** - 包括电量、温度、健康度、充电时间估算
- **精确的网络流量统计** - 支持WiFi和移动网络，包含应用级统计
- **设备温度监控** - 多传感器温度读取
- **数据持久化存储** - Room数据库存储历史数据

## 核心组件

### 1. RealPerformanceMonitorManager
主要性能监控管理器，负责协调所有监控任务。

```kotlin
// 创建实例
val performanceMonitor = RealPerformanceMonitorManager(context)

// 设置回调
performanceMonitor.setCallback(object : RealPerformanceMonitorManager.PerformanceCallback {
    override fun onPerformanceUpdate(data: PerformanceData) {
        // 处理性能数据更新
        updateUI(data)
    }
    
    override fun onError(error: Exception) {
        // 处理错误
        Log.e(TAG, "性能监控错误", error)
    }
    
    // ... 其他回调方法
})

// 启动监控
performanceMonitor.startMonitoring()

// 获取当前性能快照
val currentPerformance = performanceMonitor.getCurrentPerformance()

// 停止监控
performanceMonitor.stopMonitoring()
```

### 2. EnhancedBatteryMonitor
增强的电池监控器，提供详细的电池信息和健康度分析。

```kotlin
val batteryMonitor = EnhancedBatteryMonitor(context)

// 获取当前电池信息
val batteryInfo = batteryMonitor.getCurrentBatteryInfo()

// 获取电池使用摘要
val batterySummary = batteryMonitor.getBatterySummary()

// 启动电池监控（包含异常提醒）
batteryMonitor.startMonitoring()
```

### 3. EnhancedNetworkStatsManager
增强的网络统计管理器，提供详细的网络使用分析。

```kotlin
val networkStatsManager = EnhancedNetworkStatsManager(context)

// 获取详细网络统计
val detailedStats = networkStatsManager.getDetailedNetworkStats()

// 获取网络速度
val networkSpeed = networkStatsManager.getNetworkSpeed()

// 获取网络类型
val networkType = networkStatsManager.getNetworkTypeName()
```

## 数据模型

### PerformanceData
完整的性能数据模型，包含所有监控指标：

```kotlin
data class PerformanceData(
    val timestamp: Long,              // 时间戳
    val cpuUsage: Float,             // CPU使用率 (%)
    val memoryUsage: MemoryInfo,     // 内存使用信息
    val storageUsage: Float,         // 存储使用率 (%)
    val batteryInfo: BatteryInfo,    // 电池信息
    val networkType: String,         // 网络类型
    val deviceTemperature: Float     // 设备温度 (°C)
)
```

### BatteryInfo
详细的电池信息模型：

```kotlin
data class BatteryInfo(
    val level: Int,                  // 电量 (0-100%)
    val temperature: Float,          // 温度 (°C)
    val voltage: Float,              // 电压 (V)
    val current: Float,              // 电流 (mA)
    val status: Int,                 // 充电状态
    val health: Int,                 // 健康状态
    val technology: String,          // 电池技术
    val capacity: Long,              // 设计容量 (mAh)
    val isCharging: Boolean,         // 是否充电中
    val chargeType: String,          // 充电类型
    val timeToFull: Long,            // 预计充满时间 (分钟)
    val timeToEmpty: Long            // 预计使用时间 (分钟)
)
```

## 集成到现有代码

### 替换现有的PerformanceMonitorManager

新的监控系统向后兼容，可以直接替换现有的监控管理器：

```kotlin
// 原来的代码
val performanceMonitor = PerformanceMonitorManager(context)

// 替换为新的实现
val performanceMonitor = RealPerformanceMonitorManager(context)

// API保持兼容，无需修改其他代码
performanceMonitor.setCallback(callback)
performanceMonitor.startMonitoring()
```

### 在MainActivity中集成

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var performanceMonitor: RealPerformanceMonitorManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化性能监控
        performanceMonitor = RealPerformanceMonitorManager(this)
        performanceMonitor.setCallback(performanceCallback)
        
        // 如果需要，可以自动启动监控
        if (shouldStartMonitoring()) {
            performanceMonitor.startMonitoring()
        }
    }
    
    private val performanceCallback = object : RealPerformanceMonitorManager.PerformanceCallback {
        override fun onPerformanceUpdate(data: PerformanceData) {
            // 更新UI显示
            runOnUiThread {
                updatePerformanceUI(data)
            }
        }
        
        override fun onDataSaved(recordCount: Long) {
            Log.i(TAG, "已保存 $recordCount 条性能记录")
        }
        
        override fun onError(error: Exception) {
            Log.e(TAG, "性能监控错误", error)
            // 可以显示错误提示或重试
        }
        
        override fun onMonitoringStarted() {
            Log.i(TAG, "性能监控已启动")
        }
        
        override fun onMonitoringStopped() {
            Log.i(TAG, "性能监控已停止")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        performanceMonitor.cleanup()
    }
}
```

## 权限要求

确保在AndroidManifest.xml中声明必要的权限：

```xml
<!-- 基础权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.BATTERY_STATS" />

<!-- 网络统计权限（Android M+） -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" 
    xmlns:tools="http://schemas.android.com/tools"
    tools:ignore="ProtectedPermissions" />

<!-- Shizuku权限（可选，用于更深层系统监控） -->
<uses-permission android:name="moe.shizuku.manager.permission.API_V23" />
```

## 测试

项目包含完整的测试套件：

### 单元测试
- `RealPerformanceMonitorManagerTest` - 核心监控功能测试
- `EnhancedBatteryMonitorTest` - 电池监控测试
- `EnhancedNetworkStatsManagerTest` - 网络统计测试

### 集成测试
- `PerformanceMonitorIntegrationTest` - 完整功能集成测试

### 性能测试
- `PerformanceMonitorPerformanceTest` - 监控性能影响测试

运行测试：
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## 性能优化

### 1. 监控频率配置
可以根据需求调整监控间隔：

```kotlin
// 在RealPerformanceMonitorManager中
private val MONITORING_INTERVAL = 2000L // 2秒，可以根据需要调整
```

### 2. 数据缓存
系统实现了智能缓存，减少重复计算：
- 网络统计数据缓存
- CPU使用率增量计算
- 电池历史数据管理

### 3. 内存管理
- 自动限制历史数据大小（100条记录）
- 定期清理过期数据库记录（30天）
- 使用原子操作避免内存泄漏

## 故障排除

### 常见问题

1. **权限不足导致数据不准确**
   - 确保已获取BATTERY_STATS权限
   - 对于网络统计，需要PACKAGE_USAGE_STATS权限

2. **Shizuku功能不可用**
   - 系统会自动回退到标准API
   - 可以检查ShizukuManager.isShizukuAvailable()

3. **某些设备无法获取温度数据**
   - 不同设备的温度传感器路径不同
   - 系统会尝试多个路径并回退到电池温度

### 调试日志

启用详细日志：

```kotlin
// 在Application或Activity中
if (BuildConfig.DEBUG) {
    Log.setLevel(Log.DEBUG)
}
```

## 最佳实践

1. **合理使用监控**
   - 只在需要时启动监控
   - 长时间后台运行时注意电池消耗

2. **错误处理**
   - 实现合适的错误回调
   - 提供用户友好的错误提示

3. **UI更新**
   - 使用合理的更新频率
   - 考虑使用图表展示趋势数据

4. **数据隐私**
   - 说明数据收集目的
   - 提供数据清除选项

## 扩展功能

### 添加自定义监控指标

```kotlin
// 扩展PerformanceData模型
data class PerformanceData(
    // ... 现有字段
    val customMetric: Float = 0f  // 自定义指标
)

// 在监控管理器中添加收集逻辑
private fun collectCustomMetric(): Float {
    // 实现自定义指标收集
    return calculateCustomValue()
}
```

### 设置监控策略

```kotlin
// 根据应用状态调整监控策略
fun adjustMonitoringStrategy(appState: AppState) {
    when (appState) {
        AppState.FOREGROUND -> {
            // 前台：高频率监控
            performanceMonitor.setMonitoringInterval(1000L)
        }
        AppState.BACKGROUND -> {
            // 后台：低频率监控
            performanceMonitor.setMonitoringInterval(10000L)
        }
    }
}
```

## 总结

新的真实性能监控系统提供了：

✅ **完全替换硬编码数据** - 所有数据都来自真实系统API  
✅ **全面的性能指标** - CPU、内存、电池、网络、温度  
✅ **智能数据缓存** - 优化性能和电池使用  
✅ **完整的测试覆盖** - 单元测试、集成测试、性能测试  
✅ **向后兼容** - 可以无缝替换现有实现  
✅ **可扩展架构** - 易于添加新的监控指标  
✅ **详细的文档和示例** - 便于集成和维护  

通过这个系统，用户可以获得准确、实时的设备性能信息，为系统优化提供可靠的数据基础。
