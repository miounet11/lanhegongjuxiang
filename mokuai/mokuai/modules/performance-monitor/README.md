# 📈 性能监控模块 (Performance Monitor Module)

## 🎯 概述

蓝河工具箱性能监控模块提供全面的应用性能监控功能，包括CPU使用率、内存消耗、网络流量、界面渲染性能等，帮助开发者优化应用性能。

## ✨ 主要特性

- ✅ **CPU监控**：实时监控CPU使用率和线程状态
- ✅ **内存监控**：监控内存使用、GC频率、内存泄漏
- ✅ **网络监控**：监控网络请求、流量消耗、连接状态
- ✅ **渲染监控**：监控界面渲染性能、掉帧情况
- ✅ **电池监控**：监控电池消耗和优化建议
- ✅ **存储监控**：监控存储空间使用和缓存大小
- ✅ **性能报告**：生成详细的性能分析报告
- ✅ **实时告警**：性能异常时的实时告警通知

## 🚀 快速开始

### 初始化性能监控

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化性能监控
        PerformanceMonitor.initialize(this);
    }
}
```

### 监控Activity性能

```java
public class MainActivity extends BaseActivity {

    private PerformanceSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 开始性能监控会话
        session = PerformanceMonitor.getInstance()
            .startSession("MainActivity");

        // 监控关键操作
        session.markEvent("view_inflation_start");
        setContentView(R.layout.activity_main);
        session.markEvent("view_inflation_end");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 结束性能监控会话
        session.end();
    }
}
```

### 自定义性能监控

```java
// 监控自定义操作
PerformanceMonitor.getInstance()
    .monitorOperation("image_processing", () -> {
        // 执行图片处理操作
        processImage(imageData);
    }, result -> {
        Log.d(TAG, "Image processing completed in " + result.getDuration() + "ms");
    });
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `PerformanceMonitor` | 性能监控器核心类 |
| `PerformanceSession` | 性能监控会话 |
| `PerformanceMetrics` | 性能指标数据类 |
| `PerformanceAlert` | 性能告警类 |

### 主要方法

#### PerformanceMonitor

```java
// 初始化性能监控
void initialize(Context context)

// 开始监控会话
PerformanceSession startSession(String name)

// 监控操作
<T> void monitorOperation(String name, Callable<T> operation, Callback<T> callback)

// 获取当前性能指标
PerformanceMetrics getCurrentMetrics()

// 设置性能阈值
void setThreshold(String metric, double value)

// 启用/禁用监控
void setEnabled(boolean enabled)

// 生成性能报告
PerformanceReport generateReport()

// 清理监控数据
void clearData()
```

## 📦 依赖项

```gradle
dependencies {
    // 蓝河工具箱性能监控模块
    implementation 'com.hippo.ehviewer:performance-monitor:1.0.0'
}
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
