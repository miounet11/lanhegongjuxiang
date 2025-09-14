# 🔐 密码管理模块 (Password Manager Module)

## 🎯 概述

蓝河工具箱密码管理模块提供安全的密码存储、自动填充和生物识别认证功能，支持AES加密和Android KeyStore保护，确保用户密码数据的安全性。

## ✨ 主要特性

- ✅ **AES加密存储**：使用AES-GCM加密算法保护密码数据
- ✅ **生物识别认证**：支持指纹和人脸识别解锁
- ✅ **自动填充**：自动填充登录表单
- ✅ **密码生成器**：生成强密码并评估密码强度
- ✅ **安全备份**：支持密码数据的安全备份和恢复
- ✅ **多域名支持**：为不同网站存储独立的密码
- ✅ **使用统计**：跟踪密码使用频率和时间
- ✅ **过期提醒**：提醒用户定期更换密码

## 🚀 快速开始

### 初始化密码管理器

```java
// 在Application中初始化
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化密码管理器
        PasswordManager.initialize(this);
    }
}
```

### 保存密码

```java
// 创建密码条目
PasswordEntry entry = new PasswordEntry();
entry.setDomain("example.com");
entry.setUsername("user@example.com");
entry.setPassword("securePassword123");

// 保存密码
boolean success = PasswordManager.getInstance()
    .savePassword(entry);

// 检查保存结果
if (success) {
    Toast.makeText(context, "密码保存成功", Toast.LENGTH_SHORT).show();
} else {
    Toast.makeText(context, "密码保存失败", Toast.LENGTH_SHORT).show();
}
```

### 自动填充密码

```java
// 自动填充登录表单
PasswordManager.getInstance()
    .autoFill("example.com", "user@example.com",
        new AutoFillCallback() {
            @Override
            public void onSuccess(String username, String password) {
                // 填充用户名和密码字段
                usernameField.setText(username);
                passwordField.setText(password);
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            }
        });
```

## 📋 API 参考

### 核心类

| 类名 | 说明 |
|------|------|
| `PasswordManager` | 密码管理器核心类 |
| `PasswordEntry` | 密码条目数据类 |
| `AutoFillCallback` | 自动填充回调接口 |
| `BiometricCallback` | 生物识别回调接口 |

### 主要方法

#### PasswordManager

```java
// 保存密码
boolean savePassword(PasswordEntry entry)

// 获取密码
PasswordEntry getPassword(String domain, String username)

// 删除密码
boolean deletePassword(String domain, String username)

// 获取域名密码列表
List<PasswordEntry> getPasswordsForDomain(String domain)

// 自动填充密码
boolean autoFill(String domain, String username, AutoFillCallback callback)

// 生物识别解锁
void unlockWithBiometric(FragmentActivity activity, BiometricCallback callback)

// 锁定密码管理器
void lock()

// 检查是否已解锁
boolean isUnlocked()

// 生成强密码
String generateStrongPassword(int length)

// 检查密码强度
PasswordStrength checkPasswordStrength(String password)

// 导出密码数据
String exportPasswords()

// 导入密码数据
boolean importPasswords(String data)
```

## 🔧 配置选项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enableBiometric` | `boolean` | `true` | 是否启用生物识别 |
| `autoLockTimeout` | `long` | `300000` | 自动锁定超时(毫秒) |
| `passwordLength` | `int` | `16` | 生成密码默认长度 |
| `enableBackup` | `boolean` | `true` | 是否启用备份功能 |
| `backupFrequency` | `long` | `86400000` | 备份频率(毫秒) |

## 📦 依赖项

```gradle
dependencies {
    // Android生物识别
    implementation 'androidx.biometric:biometric:1.1.0'

    // 蓝河工具箱密码管理模块
    implementation 'com.hippo.ehviewer:password-manager:1.0.0'
}
```

## ⚠️ 注意事项

### 权限要求
```xml
<!-- 在AndroidManifest.xml中添加 -->
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
```

### 安全注意事项
- 密码数据使用AES-GCM加密存储在Android KeyStore中
- 需要用户认证才能访问密码数据
- 支持数据导出但需要额外验证

### 兼容性
- Android 6.0+ 支持指纹识别
- Android 9.0+ 支持生物识别
- Android 10.0+ 支持更强的生物识别

## 🧪 测试

### 密码管理测试
```java
@Test
public void testPasswordManager_saveAndRetrieve_shouldWorkCorrectly() {
    // Given
    PasswordManager manager = PasswordManager.getInstance();
    PasswordEntry entry = createTestPasswordEntry();

    // When
    boolean saved = manager.savePassword(entry);
    PasswordEntry retrieved = manager.getPassword(entry.getDomain(), entry.getUsername());

    // Then
    assertTrue(saved);
    assertNotNull(retrieved);
    assertEquals(entry.getPassword(), retrieved.getPassword());
}
```

## 📄 许可证

本项目采用 Apache License 2.0 许可证 - 查看 [LICENSE](../LICENSE) 文件了解详情
