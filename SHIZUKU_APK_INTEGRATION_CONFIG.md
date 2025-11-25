# 集成Shizuku APK的配置步骤

## 如果您想要集成Shizuku APK到应用内，需要完成以下配置：

## 1. 添加必要的权限到AndroidManifest.xml

在`<application>`标签之前添加：

```xml
<!-- 安装未知来源应用权限 -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

## 2. 配置FileProvider

在`<application>`标签内添加FileProvider配置（在最后一个service或activity之后）：

```xml
<!-- FileProvider用于共享APK文件 -->
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

## 3. 创建FileProvider路径配置文件

创建文件：`app/src/main/res/xml/file_paths.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 外部缓存目录 -->
    <external-cache-path 
        name="external_cache" 
        path="." />
    
    <!-- 内部缓存目录 -->
    <cache-path 
        name="cache" 
        path="." />
    
    <!-- 外部文件目录 -->
    <external-files-path 
        name="external_files" 
        path="." />
    
    <!-- 内部文件目录 -->
    <files-path 
        name="files" 
        path="." />
</paths>
```

## 4. 下载并集成Shizuku APK

### 4.1 下载Shizuku
从GitHub下载最新版本：
```
https://github.com/RikkaApps/Shizuku/releases/latest
```

### 4.2 创建assets目录
如果不存在，创建目录：
```bash
mkdir -p app/src/main/assets
```

### 4.3 放置APK文件
将下载的Shizuku APK重命名为`shizuku.apk`，放到：
```
app/src/main/assets/shizuku.apk
```

## 5. 修改ShizukuAuthActivity

在`showShizukuDownloadOptions()`方法中添加第四个选项：

```kotlin
private fun showShizukuDownloadOptions() {
    val options = arrayOf(
        "📱 从应用内直接安装（最快）",  // 新增
        "📦 直接下载最新版本",
        "🌐 在内置浏览器中下载",
        "🔗 在外部浏览器中下载"
    )

    androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle("选择下载方式")
        .setItems(options) { dialog, which ->
            when (which) {
                0 -> installFromAssets()  // 新增
                1 -> downloadShizukuDirectly()
                2 -> openInInternalBrowser()
                3 -> openInExternalBrowser()
            }
            dialog.dismiss()
        }
        .setNegativeButton("取消", null)
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

## 6. 确认ApkInstaller工具类已存在

确认以下文件已经创建：
```
app/src/main/java/com/lanhe/gongjuxiang/utils/ApkInstaller.kt
```

该文件已经在前面的步骤中创建了。

## 完整的AndroidManifest.xml修改示例

在`<application>`标签之前添加权限：
```xml
<!-- 在第72行左右，振动权限之后添加 -->
<!-- 安装未知来源应用权限 -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

在`<application>`标签内的末尾（AIOptimizationActivity之后）添加：
```xml
<!-- FileProvider用于共享APK文件 -->
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

## 注意事项

1. **权限请求**：Android 8.0+需要用户手动授予"安装未知应用"权限
2. **APK大小**：Shizuku APK约8-10MB，会增加应用体积
3. **版本更新**：建议每3-6个月检查并更新集成的Shizuku版本
4. **测试**：在不同Android版本上测试安装流程

## 检查清单

- [ ] 添加REQUEST_INSTALL_PACKAGES权限
- [ ] 配置FileProvider
- [ ] 创建file_paths.xml
- [ ] 下载Shizuku APK
- [ ] 将APK放到assets目录
- [ ] 修改ShizukuAuthActivity添加安装方法
- [ ] 确认ApkInstaller.kt存在
- [ ] 在不同设备上测试

## 可选但推荐

如果不想集成完整的APK，当前的实现（使用内置浏览器下载）已经提供了很好的用户体验。

集成APK的主要优势是：
- ✅ 无需网络即可安装
- ✅ 安装速度最快
- ✅ 最佳用户体验

如果应用体积增加不是问题，强烈建议集成APK。
