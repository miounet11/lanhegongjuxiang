# Shizuku下载安装优化方案

## 实现的功能

已经优化了Shizuku的下载和安装流程，提供了多种便捷的下载方式。

## 改进内容

### 1. 多种下载方式选择

修改了`ShizukuAuthActivity.kt`中的`installShizuku()`方法，现在用户点击"下载安装Shizuku"按钮时，会看到以下三个选项：

```
📦 直接下载最新版本 (推荐)
🌐 在内置浏览器中下载
🔗 在外部浏览器中下载
```

### 2. 方式详解

#### 📦 直接下载最新版本（推荐）
- 使用内置的Chromium浏览器
- 直接访问Shizuku最新版本的下载链接
- 下载链接：`shizuku-v13.5.4.r1038.05cd6fc-release.apk`
- 下载完成后，用户只需点击安装即可

#### 🌐 在内置浏览器中下载
- 使用内置的Chromium浏览器
- 打开Shizuku的GitHub发布页面
- 用户可以查看所有版本，选择需要的版本下载

#### 🔗 在外部浏览器中下载
- 使用系统默认浏览器
- 保留了原有的功能，作为备选方案

## 技术实现

### 修改的文件
- `app/src/main/java/com/lanhe/gongjuxiang/activities/ShizukuAuthActivity.kt`

### 新增的文件
- `app/src/main/java/com/lanhe/gongjuxiang/utils/ApkInstaller.kt` - APK安装工具类

### 关键代码

#### ShizukuAuthActivity.kt
```kotlin
private fun showShizukuDownloadOptions() {
    val options = arrayOf(
        "📦 直接下载最新版本 (推荐)",
        "🌐 在内置浏览器中下载",
        "🔗 在外部浏览器中下载"
    )

    AlertDialog.Builder(this)
        .setTitle("选择下载方式")
        .setItems(options) { dialog, which ->
            when (which) {
                0 -> downloadShizukuDirectly()
                1 -> openInInternalBrowser()
                2 -> openInExternalBrowser()
            }
        }
        .show()
}
```

#### 直接下载方法
```kotlin
private fun downloadShizukuDirectly() {
    val downloadUrl = "https://github.com/RikkaApps/Shizuku/releases/latest/download/shizuku-v13.5.4.r1038.05cd6fc-release.apk"
    
    val intent = Intent(this, ChromiumBrowserActivity::class.java)
    intent.putExtra("url", downloadUrl)
    startActivity(intent)
}
```

## 可选：集成Shizuku APK到应用内

### 方案说明

如果希望将Shizuku APK直接集成到应用中，避免用户需要联网下载，可以按照以下步骤操作：

### 步骤1：下载Shizuku APK

从以下地址下载最新版本的Shizuku APK：
```
https://github.com/RikkaApps/Shizuku/releases/latest
```

推荐下载文件：`shizuku-v13.5.4.r1038.05cd6fc-release.apk`

### 步骤2：放置APK文件

1. 在项目中创建assets文件夹：
   ```
   /Users/lu/Downloads/lanhezhushou/app/src/main/assets/
   ```

2. 将下载的Shizuku APK重命名为：`shizuku.apk`

3. 放置到assets文件夹中：
   ```
   /Users/lu/Downloads/lanhezhushou/app/src/main/assets/shizuku.apk
   ```

### 步骤3：修改ShizukuAuthActivity

在`showShizukuDownloadOptions()`方法中添加第四个选项：

```kotlin
private fun showShizukuDownloadOptions() {
    val options = arrayOf(
        "📱 从应用内直接安装 (最快)",  // 新增
        "📦 直接下载最新版本",
        "🌐 在内置浏览器中下载",
        "🔗 在外部浏览器中下载"
    )

    AlertDialog.Builder(this)
        .setTitle("选择下载方式")
        .setItems(options) { dialog, which ->
            when (which) {
                0 -> installFromAssets()  // 新增
                1 -> downloadShizukuDirectly()
                2 -> openInInternalBrowser()
                3 -> openInExternalBrowser()
            }
        }
        .show()
}

private fun installFromAssets() {
    lifecycleScope.launch {
        showInstallationProgress("正在准备安装Shizuku...")
        delay(500)
        
        try {
            val success = ApkInstaller.installApkFromAssets(
                this@ShizukuAuthActivity,
                "shizuku.apk"
            )
            
            if (success) {
                Toast.makeText(
                    this@ShizukuAuthActivity,
                    "请在弹出的安装界面中点击安装",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@ShizukuAuthActivity,
                    "安装失败，请尝试其他下载方式",
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            hideInstallationProgress()
        } catch (e: Exception) {
            hideInstallationProgress()
            Toast.makeText(
                this@ShizukuAuthActivity,
                "安装失败: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
```

### 步骤4：配置FileProvider

确保在`AndroidManifest.xml`中配置了FileProvider（通常已经配置）：

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### 步骤5：创建file_paths.xml

在`res/xml/file_paths.xml`中添加：

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-cache-path name="external_cache" path="." />
    <cache-path name="cache" path="." />
</paths>
```

## 优缺点分析

### 方案1：在线下载（当前实现）
✅ 优点：
- 应用体积小
- 始终下载最新版本
- 无需维护集成的APK版本

❌ 缺点：
- 需要网络连接
- 某些网络环境下GitHub访问较慢

### 方案2：集成APK（可选）
✅ 优点：
- 无需网络连接
- 安装速度最快
- 用户体验最佳

❌ 缺点：
- 增加应用体积（约8-10MB）
- 需要定期更新集成的APK版本
- 用户可能安装的不是最新版本

## 推荐方案

**推荐采用混合方案：**
1. 提供应用内集成的APK（作为快速安装选项）
2. 同时提供在线下载最新版本的选项
3. 用户可以根据自己的情况选择

这样既保证了便利性，又能让用户选择安装最新版本。

## Shizuku版本信息

- **当前集成版本建议**：v13.5.4.r1038
- **下载地址**：https://github.com/RikkaApps/Shizuku/releases/latest
- **APK大小**：约8-10MB
- **建议更新频率**：每3-6个月检查一次新版本

## 注意事项

1. **版本更新**：如果选择集成APK，需要定期检查Shizuku是否有重要更新
2. **权限申请**：确保应用有安装未知来源应用的权限
3. **用户提示**：在用户首次安装时，系统会要求授予安装权限
4. **测试**：在不同Android版本上测试安装流程

## 使用流程

### 用户操作流程
1. 打开应用，看到Shizuku权限提示
2. 点击"去设置"
3. 点击"下载安装Shizuku"按钮
4. 选择喜欢的下载方式
5. 等待下载完成
6. 点击安装
7. 返回应用，继续授权流程

### 开发者维护流程
1. 定期检查Shizuku新版本
2. 如有重要更新，下载新版APK
3. 替换assets中的shizuku.apk
4. 更新下载链接中的版本号
5. 发布应用更新

## 总结

通过这次优化：
- ✅ 用户可以在内置浏览器中下载Shizuku
- ✅ 提供了直接下载最新版本的快捷方式  
- ✅ 保留了外部浏览器下载的备选方案
- ✅ 创建了APK安装工具类，为集成APK提供了基础
- ✅ 提供了完整的集成APK方案文档

用户体验得到了显著提升，下载安装过程更加便捷！
