# 🚫 广告拦截模块 (Ad Blocker Module)

## 🎯 概述

蓝河工具箱广告拦截模块提供强大的广告过滤和屏蔽功能，支持多种广告检测方式和自定义规则，帮助用户获得更清爽的浏览体验。基于Chromium广告过滤规范实现。

## ✨ 主要特性

- ✅ **多层过滤**：URL过滤、元素过滤、脚本过滤
- ✅ **自定义规则**：支持自定义广告过滤规则
- ✅ **hosts拦截**：基于hosts文件的广告拦截
- ✅ **元素隐藏**：动态隐藏页面中的广告元素
- ✅ **白名单支持**：为可信网站设置白名单
- ✅ **统计报告**：拦截广告数量和类型的统计
- ✅ **规则更新**：自动更新广告过滤规则
- ✅ **性能优化**：高效的过滤算法，minimal性能影响

## 🚀 快速开始

### 初始化广告拦截器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化广告拦截器
        AdBlocker.initialize(this);
    }
}
```

### WebView集成

```java
// 在WebView中启用广告拦截
public class MyWebView extends WebView {

    public MyWebView(Context context) {
        super(context);
        initAdBlocker();
    }

    private void initAdBlocker() {
        // 设置WebViewClient
        setWebViewClient(new AdBlockWebViewClient());

        // 启用广告拦截
        AdBlocker.getInstance().enableForWebView(this);
    }
}
```

### 自定义过滤规则

```java
// 添加自定义过滤规则
AdBlocker.getInstance()
    .addCustomRule(new AdBlockRule()
        .setDomain("example.com")
        .setUrlPattern("*ads*")
        .setAction(AdBlockRule.ACTION_BLOCK));

// 启用规则
AdBlocker.getInstance().updateRules();
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `AdBlocker` | 广告拦截器核心类 |
| `AdBlockRule` | 广告过滤规则类 |
| `AdBlockWebViewClient` | WebView广告拦截客户端 |
| `AdBlockStats` | 广告拦截统计信息 |

### 主要方法

#### AdBlocker

```java
// 初始化广告拦截器
void initialize(Context context)

// 获取单例实例
AdBlocker getInstance()

// 为WebView启用广告拦截
void enableForWebView(WebView webView)

// 添加自定义规则
void addCustomRule(AdBlockRule rule)

// 移除规则
void removeRule(String ruleId)

// 更新规则
void updateRules()

// 检查URL是否应被拦截
boolean shouldBlockUrl(String url)

// 获取拦截统计
AdBlockStats getStats()

// 清除统计数据
void clearStats()
```

## 📦 依赖项

```gradle
dependencies {
    // 蓝河工具箱广告拦截模块
    implementation 'com.hippo.ehviewer:ad-blocker:1.0.0'
}
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
