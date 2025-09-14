# 🧠 内存管理模块 (Memory Manager Module)

## 🎯 概述

蓝河工具箱内存管理模块提供智能的内存监控和优化功能，帮助应用有效管理内存使用，防止内存泄漏，提高应用稳定性。

## ✨ 主要特性

- ✅ **内存监控**：实时监控应用内存使用情况
- ✅ **泄漏检测**：自动检测内存泄漏并告警
- ✅ **缓存管理**：智能管理各种缓存的大小
- ✅ **GC优化**：优化垃圾回收时机和频率
- ✅ **Bitmap优化**：防止Bitmap内存溢出
- ✅ **对象池**：复用常用对象减少GC压力
- ✅ **内存报告**：生成详细的内存使用报告
- ✅ **自动清理**：低内存时自动清理缓存

## 🚀 快速开始

### 初始化内存管理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化内存管理器
        MemoryManager.initialize(this);
    }
}
```

### 监控内存使用

```java
// 获取内存信息
MemoryInfo memoryInfo = MemoryManager.getInstance().getMemoryInfo();
Log.d(TAG, "Used memory: " + memoryInfo.getUsedMemory() + "MB");
Log.d(TAG, "Available memory: " + memoryInfo.getAvailableMemory() + "MB");

// 监听内存变化
MemoryManager.getInstance().setMemoryListener(new MemoryListener() {
    @Override
    public void onMemoryLow(MemoryInfo info) {
        // 处理低内存情况
        clearCaches();
    }

    @Override
    public void onMemoryCritical(MemoryInfo info) {
        // 处理内存严重不足
        releaseResources();
    }
});
```

### 智能缓存管理

```java
// 配置缓存大小
MemoryManager.getInstance()
    .setCacheLimit("image_cache", 50 * 1024 * 1024) // 50MB
    .setCacheLimit("network_cache", 10 * 1024 * 1024) // 10MB
    .setCacheLimit("database_cache", 5 * 1024 * 1024); // 5MB

// 自动清理缓存
MemoryManager.getInstance().trimMemory(ComponentCallbacks2.TRIM_MEMORY_MODERATE);
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `MemoryManager` | 内存管理器核心类 |
| `MemoryInfo` | 内存信息数据类 |
| `MemoryListener` | 内存监听器接口 |
| `CacheManager` | 缓存管理器 |

### 主要方法

#### MemoryManager

```java
// 初始化内存管理器
void initialize(Context context)

// 获取单例实例
MemoryManager getInstance()

// 获取内存信息
MemoryInfo getMemoryInfo()

// 设置内存监听器
void setMemoryListener(MemoryListener listener)

// 设置缓存大小限制
MemoryManager setCacheLimit(String cacheName, long limit)

// 清理内存
void trimMemory(int level)

// 强制GC
void forceGC()

// 获取内存报告
MemoryReport generateReport()

// 检测内存泄漏
void detectMemoryLeaks()
```

## 📦 依赖项

```gradle
dependencies {
    // LeakCanary内存泄漏检测
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'

    // 蓝河工具箱内存管理模块
    implementation 'com.hippo.ehviewer:memory-manager:1.0.0'
}
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
