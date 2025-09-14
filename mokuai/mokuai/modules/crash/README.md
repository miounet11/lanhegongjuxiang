# 🚨 崩溃处理模块 (Crash Handler Module)

## 🎯 概述

蓝河工具箱崩溃处理模块提供完整的应用崩溃检测、日志记录和错误报告功能，帮助开发者快速定位和修复应用崩溃问题，提高应用稳定性。

## ✨ 主要特性

- ✅ **自动崩溃检测**：实时监控应用崩溃并自动记录
- ✅ **详细崩溃日志**：收集设备信息、堆栈跟踪、内存状态等
- ✅ **日志文件管理**：自动清理过期日志，控制存储空间
- ✅ **崩溃统计分析**：统计崩溃频率和模式
- ✅ **远程上报支持**：支持将崩溃信息上传到服务器
- ✅ **用户友好提示**：崩溃后显示友好的错误提示界面
- ✅ **恢复机制**：尝试从崩溃状态恢复应用
- ✅ **调试信息**：提供详细的调试和诊断信息

## 🚀 快速开始

### 初始化崩溃处理

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化崩溃处理
        CrashHandler.getInstance(this).initialize();
    }
}
```

### 手动记录异常

```java
try {
    // 可能发生异常的代码
    riskyOperation();
} catch (Exception e) {
    // 记录异常信息
    CrashHandler.getInstance(context)
        .logException(e, "Manual exception logging");

    // 可以选择重新抛出或处理异常
    handleException(e);
}
```

### 自定义崩溃处理

```java
// 自定义崩溃处理器
CrashHandler.getInstance(context)
    .setCustomCrashListener(new CrashListener() {
        @Override
        public void onCrash(Thread thread, Throwable throwable) {
            // 自定义崩溃处理逻辑
            Log.e(TAG, "Custom crash handler", throwable);

            // 保存应用状态
            saveAppState();

            // 显示自定义错误界面
            showCustomErrorScreen();
        }
    });
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `CrashHandler` | 崩溃处理管理器核心类 |
| `CrashConfig` | 崩溃处理配置类 |
| `CrashInfo` | 崩溃信息数据类 |
| `CrashReporter` | 崩溃报告器 |

### 主要方法

#### CrashHandler

```java
// 初始化崩溃处理
void initialize()

// 设置自定义崩溃监听器
void setCustomCrashListener(CrashListener listener)

// 手动记录异常
void logException(Throwable throwable, String message)

// 记录错误信息
void logError(String tag, String message, Throwable throwable)

// 获取崩溃日志列表
List<CrashInfo> getCrashLogs()

// 清除所有崩溃日志
void clearCrashLogs()

// 导出崩溃日志
boolean exportCrashLogs(File exportDir)

// 获取设备信息
DeviceInfo getDeviceInfo()

// 获取内存信息
MemoryInfo getMemoryInfo()
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enableCrashHandler` | `boolean` | `true` | 是否启用崩溃处理 |
| `enableRemoteReporting` | `boolean` | `false` | 是否启用远程上报 |
| `maxLogFiles` | `int` | `10` | 最大日志文件数量 |
| `maxLogFileSize` | `long` | `1048576` | 单个日志文件最大大小(字节) |
| `logRetentionDays` | `int` | `7` | 日志保留天数 |
| `enableDeviceInfo` | `boolean` | `true` | 是否收集设备信息 |

## 📦 依赖项

```gradle
dependencies {
    // 蓝河工具箱崩溃处理模块
    implementation 'com.hippo.ehviewer:crash-handler:1.0.0'
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### 存储位置
崩溃日志默认保存在应用的外部存储目录中，确保应用具有适当的存储权限。

### 性能影响
崩溃处理会轻微影响应用启动性能，但不会影响正常运行时的性能。

## 🧪 测试

### 模拟崩溃测试
```java
@Test
public void testCrashHandler_simulateCrash_shouldHandleGracefully() {
    // Given
    CrashHandler handler = CrashHandler.getInstance(context);

    // When - 模拟崩溃
    try {
        throw new RuntimeException("Test crash");
    } catch (Exception e) {
        handler.logException(e, "Test crash simulation");
    }

    // Then - 验证崩溃被正确处理
    List<CrashInfo> logs = handler.getCrashLogs();
    assertFalse(logs.isEmpty());
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4::class)
public class CrashHandlerIntegrationTest {

    @Test
    public void testFullCrashHandlingFlow() {
        // 测试完整的崩溃处理流程
        // 1. 初始化崩溃处理器
        // 2. 模拟应用崩溃
        // 3. 验证日志记录
        // 4. 验证错误恢复
        // 5. 清理测试数据
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingCrashHandler`)
3. 提交更改 (`git commit -m 'Add some AmazingCrashHandler'`)
4. 推送到分支 (`git push origin feature/AmazingCrashHandler`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整API文档](https://docs.ehviewer.com/crash-handler/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
