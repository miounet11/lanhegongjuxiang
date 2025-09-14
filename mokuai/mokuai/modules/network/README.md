# 📦 网络模块 (Network Module)

## 🎯 概述

蓝河工具箱网络模块提供完整的网络通信功能，包括HTTP请求、Cookie管理、SSL证书验证、URL构建等。该模块基于OkHttp和Chromium网络栈实现，支持HTTPS、代理、Cookie持久化等高级功能。

## ✨ 主要特性

- ✅ **完整的HTTP客户端**：支持GET、POST、PUT、DELETE等HTTP方法
- ✅ **Cookie管理**：自动Cookie存储和恢复
- ✅ **SSL证书验证**：支持自定义SSL证书和信任管理
- ✅ **代理支持**：支持HTTP/HTTPS/SOCKS代理
- ✅ **连接池管理**：自动连接复用和生命周期管理
- ✅ **超时控制**：可配置连接、读写超时
- ✅ **重试机制**：网络失败自动重试
- ✅ **请求拦截**：支持请求和响应的拦截处理

## 🚀 快速开始

### 基本使用

```java
// 初始化网络管理器
NetworkManager manager = NetworkManager.getInstance(context);

// 基本GET请求
manager.get("https://api.example.com/data")
    .enqueue(new NetworkCallback<String>() {
        @Override
        public void onSuccess(String result) {
            // 处理成功结果
            Log.d(TAG, "Response: " + result);
        }

        @Override
        public void onFailure(NetworkException error) {
            // 处理错误
            Log.e(TAG, "Network error", error);
        }
    });

// POST请求
JSONObject jsonData = new JSONObject();
jsonData.put("key", "value");

manager.post("https://api.example.com/submit", jsonData.toString())
    .enqueue(callback);
```

### 高级配置

```java
// 自定义配置
NetworkConfig config = new NetworkConfig.Builder()
    .setConnectTimeout(30000)      // 30秒连接超时
    .setReadTimeout(60000)         // 60秒读取超时
    .setWriteTimeout(60000)        // 60秒写入超时
    .setRetryCount(3)              // 重试3次
    .enableCookie(true)            // 启用Cookie
    .setUserAgent("LanHe Browser/1.0 (Chromium)")  // 设置User-Agent
    .build();

// 应用配置
manager.setConfig(config);
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `NetworkManager` | 网络管理器核心类 |
| `NetworkConfig` | 网络配置类 |
| `NetworkCallback` | 网络请求回调接口 |
| `CookieManager` | Cookie管理器 |
| `SSLManager` | SSL证书管理器 |

### 主要方法

#### NetworkManager

```java
// GET请求
Call get(String url)

// POST请求
Call post(String url, String body)

// PUT请求
Call put(String url, String body)

// DELETE请求
Call delete(String url)

// 文件上传
Call upload(String url, File file)

// 文件下载
Call download(String url, File destination)

// 设置配置
void setConfig(NetworkConfig config)

// 获取配置
NetworkConfig getConfig()

// 清理资源
void cleanup()
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `connectTimeout` | `long` | `30000` | 连接超时时间(毫秒) |
| `readTimeout` | `long` | `60000` | 读取超时时间(毫秒) |
| `writeTimeout` | `long` | `60000` | 写入超时时间(毫秒) |
| `retryCount` | `int` | `3` | 最大重试次数 |
| `cookieEnabled` | `boolean` | `true` | 是否启用Cookie |
| `userAgent` | `String` | `null` | User-Agent字符串 |

## 📦 依赖项

```gradle
dependencies {
    // 核心依赖
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

    // JSON处理
    implementation 'com.squareup.moshi:moshi:1.15.0'
    implementation 'com.squareup.moshi:moshi-kotlin:1.15.0'

    // 蓝河工具箱网络模块
    implementation 'com.hippo.ehviewer:network:1.0.0'
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 兼容性
- **最低版本**: Android API 21 (Android 5.0)
- **目标版本**: Android API 34 (Android 14)
- **编译版本**: Android API 34

### 已知问题
- 在Android 4.x设备上可能需要额外的SSL配置
- 某些网络环境下可能需要配置代理

## 🧪 测试

### 单元测试
```java
@Test
public void testNetworkManager_GetRequest_Success() {
    // Given
    NetworkManager manager = NetworkManager.getInstance(context);
    String testUrl = "https://httpbin.org/get";

    // When
    Call call = manager.get(testUrl);
    Response response = call.execute();

    // Then
    assertTrue(response.isSuccessful());
    assertNotNull(response.body());
}
```

### 集成测试
```java
@RunWith(AndroidJUnit4::class)
public class NetworkIntegrationTest {

    @Test
    public void testFullNetworkFlow() {
        // 测试完整的网络请求流程
        // 1. 初始化网络管理器
        // 2. 发送请求
        // 3. 验证响应
        // 4. 清理资源
    }
}
```

## 🤝 贡献指南

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingNetworkFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingNetworkFeature'`)
4. 推送到分支 (`git push origin feature/AmazingNetworkFeature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情

## 📞 支持

- 📧 邮箱: support@ehviewer.com
- 📖 文档: [完整API文档](https://docs.ehviewer.com/network/)
- 🐛 问题跟踪: [GitHub Issues](https://github.com/ehviewer/ehviewer/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/ehviewer/ehviewer/discussions)
