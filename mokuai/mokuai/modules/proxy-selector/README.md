# 🌐 代理选择器模块 (Proxy Selector Module)

## 🎯 概述

蓝河工具箱代理选择器模块提供智能的代理服务器选择和管理功能，支持多种代理协议，自动选择最优代理服务器。基于Chromium代理规范实现。

## ✨ 主要特性

- ✅ **多种协议**：支持HTTP、HTTPS、SOCKS4、SOCKS5
- ✅ **智能选择**：基于延迟、稳定性自动选择最优代理
- ✅ **代理测试**：自动测试代理可用性和性能
- ✅ **负载均衡**：在多个代理间分配请求
- ✅ **故障转移**：代理失败时自动切换备用代理
- ✅ **地理位置**：支持按地理位置选择代理
- ✅ **认证支持**：支持代理认证
- ✅ **配置管理**：灵活的代理配置管理

## 🚀 快速开始

### 初始化代理选择器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化代理选择器
        ProxySelector.initialize(this);
    }
}
```

### 配置代理

```java
// 添加代理服务器
ProxyConfig proxy = new ProxyConfig();
proxy.setHost("proxy.example.com");
proxy.setPort(8080);
proxy.setType(ProxyConfig.TYPE_HTTP);
proxy.setUsername("user");
proxy.setPassword("pass");

ProxySelector.getInstance().addProxy(proxy);
```

### 使用代理

```java
// 自动选择最优代理
ProxyConfig bestProxy = ProxySelector.getInstance()
    .selectBestProxy();

// 创建HTTP客户端时使用代理
OkHttpClient client = new OkHttpClient.Builder()
    .proxy(bestProxy.toJavaProxy())
    .build();
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `ProxySelector` | 代理选择器核心类 |
| `ProxyConfig` | 代理配置类 |
| `ProxyTester` | 代理测试器 |
| `ProxyStats` | 代理统计信息 |

### 主要方法

#### ProxySelector

```java
// 初始化代理选择器
void initialize(Context context)

// 获取单例实例
ProxySelector getInstance()

// 添加代理
void addProxy(ProxyConfig proxy)

// 移除代理
void removeProxy(String proxyId)

// 选择最优代理
ProxyConfig selectBestProxy()

// 选择地理位置代理
ProxyConfig selectProxyByLocation(String country)

// 测试所有代理
void testAllProxies(TestCallback callback)

// 获取代理列表
List<ProxyConfig> getProxyList()

// 获取代理统计
ProxyStats getStats()
```

## 📦 依赖项

```gradle
dependencies {
    // OkHttp代理支持
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'

    // 蓝河工具箱代理选择器模块
    implementation 'com.hippo.ehviewer:proxy-selector:1.0.0'
}
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
