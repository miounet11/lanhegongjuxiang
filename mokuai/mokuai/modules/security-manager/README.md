# 🔒 安全管理模块 (Security Manager Module)

## 🎯 概述

蓝河工具箱安全管理模块提供全面的应用安全功能，包括数据加密、网络安全、权限管理、反调试等，帮助保护应用和用户数据安全。

## ✨ 主要特性

- ✅ **数据加密**：AES、RSA等加密算法保护敏感数据
- ✅ **网络安全**：HTTPS证书验证、SSL pinning
- ✅ **权限管理**：运行时权限请求和安全检查
- ✅ **反调试检测**：检测调试器连接和逆向工程
- ✅ **Root检测**：检测设备是否已Root
- ✅ **篡改检测**：检测应用是否被篡改
- ✅ **安全存储**：安全的SharedPreferences和文件存储
- ✅ **日志安全**：安全的日志记录和传输

## 🚀 快速开始

### 初始化安全管理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化安全管理器
        SecurityManager.initialize(this);
    }
}
```

### 数据加密

```java
// 加密敏感数据
String sensitiveData = "user_password";
String encrypted = SecurityManager.getInstance()
    .encryptData(sensitiveData, "encryption_key");

// 解密数据
String decrypted = SecurityManager.getInstance()
    .decryptData(encrypted, "encryption_key");
```

### 安全网络请求

```java
// 创建安全的HTTP客户端
OkHttpClient secureClient = SecurityManager.getInstance()
    .createSecureHttpClient();

// 使用SSL pinning
SecurityManager.getInstance()
    .enableSSLPinning("example.com", certificate);
```

### 权限安全检查

```java
// 请求权限时进行安全检查
SecurityManager.getInstance()
    .requestPermissionWithSecurityCheck(this, Manifest.permission.CAMERA,
        new PermissionCallback() {
            @Override
            public void onGranted() {
                openCamera();
            }

            @Override
            public void onDenied() {
                showPermissionDenied();
            }
        });
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `SecurityManager` | 安全管理器核心类 |
| `EncryptionManager` | 加密管理器 |
| `NetworkSecurity` | 网络安全管理器 |
| `PermissionManager` | 权限管理器 |

### 主要方法

#### SecurityManager

```java
// 初始化安全管理器
void initialize(Context context)

// 获取单例实例
SecurityManager getInstance()

// 加密数据
String encryptData(String data, String key)

// 解密数据
String decryptData(String encryptedData, String key)

// 创建安全的HTTP客户端
OkHttpClient createSecureHttpClient()

// 启用SSL pinning
void enableSSLPinning(String domain, String certificate)

// 检查设备安全性
SecurityCheckResult checkDeviceSecurity()

// 检测Root
boolean isDeviceRooted()

// 检测调试器
boolean isDebuggerAttached()

// 检测应用篡改
boolean isAppTampered()
```

## 📦 依赖项

```gradle
dependencies {
    // 加密库
    implementation 'org.bouncycastle:bcprov-jdk15on:1.70'

    // 蓝河工具箱安全管理模块
    implementation 'com.hippo.ehviewer:security-manager:1.0.0'
}
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
