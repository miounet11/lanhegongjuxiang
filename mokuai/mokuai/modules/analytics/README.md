# 📊 数据分析模块 (Analytics Module)

## 🎯 概述

蓝河工具箱数据分析模块提供完整的数据统计和用户行为分析功能，支持Firebase Analytics集成，帮助开发者了解用户使用习惯、应用性能和功能使用情况。

## ✨ 主要特性

- ✅ **Firebase Analytics集成**：完整的Firebase Analytics支持
- ✅ **用户行为跟踪**：页面访问、按钮点击、用户操作跟踪
- ✅ **事件统计**：自定义事件统计和分析
- ✅ **性能监控**：应用性能指标收集
- ✅ **崩溃报告**：自动崩溃信息收集和上报
- ✅ **用户属性**：用户特征和偏好分析
- ✅ **转化跟踪**：用户转化路径分析
- ✅ **隐私保护**：符合隐私保护规范的数据收集

## 🚀 快速开始

### 初始化分析模块

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化数据分析
        AnalyticsManager.getInstance(this).initialize();
    }
}
```

### 跟踪页面访问

```java
// Activity中跟踪页面访问
public class MainActivity extends BaseActivity {

    @Override
    protected void onResume() {
        super.onResume();

        // 跟踪页面访问
        AnalyticsManager.getInstance(this)
            .trackScreenView("main_screen", "MainActivity");
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 跟踪页面离开
        AnalyticsManager.getInstance(this)
            .trackScreenLeave("main_screen", getSessionDuration());
    }
}
```

### 跟踪用户事件

```java
// 跟踪按钮点击
AnalyticsManager.getInstance(context)
    .trackEvent("button_click", new Bundle().apply {
        putString("button_name", "download_button");
        putString("screen_name", "gallery_detail");
    });

// 跟踪用户操作
AnalyticsManager.getInstance(context)
    .trackUserAction("search", "image_search", "Search executed");

// 跟踪功能使用
AnalyticsManager.getInstance(context)
    .trackFeatureUsage("download_manager", "bulk_download", 5);
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `AnalyticsManager` | 数据分析管理器核心类 |
| `AnalyticsConfig` | 分析配置类 |
| `EventTracker` | 事件跟踪器 |
| `UserPropertyManager` | 用户属性管理器 |

### 主要方法

#### AnalyticsManager

```java
// 初始化分析模块
void initialize()

// 跟踪页面访问
void trackScreenView(String screenName, String screenClass)

// 跟踪页面离开
void trackScreenLeave(String screenName, long duration)

// 跟踪事件
void trackEvent(String eventName, Bundle parameters)

// 跟踪用户操作
void trackUserAction(String action, String category)

// 跟踪功能使用
void trackFeatureUsage(String featureName, String action, int count)

// 设置用户属性
void setUserProperty(String key, String value)

// 设置用户ID
void setUserId(String userId)

// 记录异常
void logException(Throwable exception)

// 获取会话ID
String getSessionId()

// 清理资源
void cleanup()
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enableAnalytics` | `boolean` | `true` | 是否启用数据分析 |
| `enableCrashReporting` | `boolean` | `true` | 是否启用崩溃报告 |
| `enablePerformanceMonitoring` | `boolean` | `true` | 是否启用性能监控 |
| `sessionTimeout` | `long` | `1800000` | 会话超时时间(毫秒) |
| `batchSize` | `int` | `20` | 批量发送事件数量 |
| `dispatchInterval` | `long` | `15000` | 发送间隔(毫秒) |

## 📦 依赖项

```gradle
dependencies {
    // Firebase Analytics
    implementation 'com.google.firebase:firebase-analytics:21.2.0'
    implementation 'com.google.firebase:firebase-analytics-ktx:21.2.0'

    // 蓝河工具箱分析模块
    implementation 'com.hippo.ehviewer:analytics:1.0.0'
}
```

## ⚠️ 注意事项

### 隐私保护
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 配置Firebase
在项目中添加 `google-services.json` 文件并配置Firebase。

### 数据合规
- 遵循GDPR等隐私保护法规
- 提供用户数据收集同意机制
- 支持数据删除和导出功能

## 🧪 测试

### 单元测试
```java
@Test
public void testAnalyticsManager_trackEvent_shouldNotThrowException() {
    // Given
    AnalyticsManager manager = AnalyticsManager.getInstance(context);

    // When
    manager.trackEvent("test_event", new Bundle());

    // Then
    // 验证事件被正确记录
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4::class)
public class AnalyticsIntegrationTest {

    @Test
    public void testFullAnalyticsFlow() {
        // 测试完整的分析流程
        // 1. 初始化分析模块
        // 2. 跟踪各种事件
        // 3. 验证数据收集
        // 4. 清理测试数据
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingAnalytics`)
3. 提交更改 (`git commit -m 'Add some AmazingAnalytics'`)
4. 推送到分支 (`git push origin feature/AmazingAnalytics`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整API文档](https://docs.ehviewer.com/analytics/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
