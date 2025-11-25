# 📋 Shizuku内置集成 - 修订实施计划 v2.0

**计划版本：** v2.0（修订版）
**更新日期：** 2025-11-24
**基础版本：** Shizuku v13.1.0（可用最稳定版本）
**目标：** 将Shizuku直接集成到蓝河助手项目

---

## 执行摘要

### 关键发现

1. **版本可用性**
   - ❌ Shizuku v13.6.0 **不存在** 于Maven Central仓库
   - ✅ Shizuku v13.1.0-v13.1.5 都可用
   - ✅ 当前项目使用v13.1.0，**可编译通过**

2. **API兼容性**
   - ⚠️ 项目代码中存在访问私有API的调用
   - ✅ 当前编译状态：**BUILD SUCCESSFUL**
   - 📝 这些调用可能在未来版本中失效

3. **集成策略**
   - 坚持使用Shizuku v13.1.0（稳定可靠）
   - 完成内置APK集成（不再需要用户单独安装）
   - 实现自动初始化流程

---

## 任务清单（修订版）

### Task 1: APK资源集成 ⭐ (必须优先)

**文件：** `app/src/main/assets/shizuku.apk`

**步骤：**

1. **创建Assets目录**
   ```bash
   mkdir -p app/src/main/assets
   ```

2. **获取Shizuku APK**

   **选项A：从GitHub Release下载（推荐）**
   - 访问：https://github.com/RikkaApps/Shizuku/releases
   - 下载：Shizuku v13.1.0+ APK
   - 文件名：如 `shizuku.apk` 或 `app-release.apk`

   **选项B：从官方渠道**
   - 使用adb从已安装的设备提取
   - 命令：`adb pull /data/app/moe.shizuku.privileged.api-*/base.apk shizuku.apk`

3. **验证APK**
   ```bash
   # 计算SHA256验证
   sha256sum shizuku.apk

   # 验证APK完整性
   unzip -t shizuku.apk > /dev/null && echo "Valid APK"
   ```

4. **放置文件**
   ```
   app/src/main/assets/
   └── shizuku.apk (约2.5-3 MB)
   ```

5. **编译验证**
   ```bash
   ./gradlew :app:assembleDebug
   # 检查APK中是否包含shizuku.apk资源
   unzip -l app/build/outputs/apk/debug/app-debug.apk | grep shizuku.apk
   ```

**预期时间：** 30分钟
**难度：** ⭐ 简单

---

### Task 2: 权限和FileProvider配置 ⭐

**文件1：** `app/src/main/AndroidManifest.xml`

需要确保以下权限声明存在：

```xml
<!-- APK安装权限 -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

<!-- Shizuku权限 -->
<uses-permission android:name="moe.shizuku.manager.permission.API_V23" />

<!-- 存储权限 -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />

<!-- 应用查询 -->
<queries>
    <package android:name="moe.shizuku.privileged.api" />
    <package android:name="rikka.shizuku" />
</queries>

<!-- FileProvider声明（application内） -->
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

**文件2：** 创建 `app/src/main/res/xml/file_paths.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- APK安装程序 -->
    <cache-path name="apk_install" path="." />

    <!-- 其他文件提供商配置 -->
    <files-path name="app_files" path="." />
    <external-path name="documents" path="Documents" />
</paths>
```

**预期时间：** 15分钟
**难度：** ⭐ 简单

---

### Task 3: 完整实现ApkInstaller ⭐⭐

**文件：** `app/src/main/java/com/lanhe/gongjuxiang/utils/ApkInstaller.kt`

**核心方法：**

```kotlin
object ApkInstaller {

    /**
     * 从Assets目录安装APK
     */
    fun installApkFromAssets(
        context: Context,
        assetFileName: String
    ): Boolean {
        return try {
            // Step 1: 权限检查
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Log.w("ApkInstaller", "缺少INSTALL_PACKAGES权限")
                    return false
                }
            }

            // Step 2: 从Assets复制APK到临时目录
            val tempFile = File(context.cacheDir, assetFileName)
            context.assets.open(assetFileName).use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Step 3: 创建FileProvider URI
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile
                )
            } else {
                @Suppress("DEPRECATION")
                Uri.fromFile(tempFile)
            }

            // Step 4: 启动安装程序
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)

            Log.i("ApkInstaller", "APK安装程序已启动: $assetFileName")
            return true

        } catch (e: Exception) {
            Log.e("ApkInstaller", "APK安装失败", e)
            return false
        }
    }

    /**
     * 检查是否有安装权限
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * 请求安装权限
     */
    fun requestInstallPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            activity.startActivity(intent)
        }
    }
}
```

**预期时间：** 1小时
**难度：** ⭐⭐ 中等

---

### Task 4: 版本管理功能 ⭐⭐

**文件：** `app/src/main/java/com/lanhe/gongjuxiang/utils/ShizukuManager.kt`

**新增方法：**

```kotlin
object ShizukuManager {

    // ... 现有代码 ...

    /**
     * 获取已安装的Shizuku版本
     */
    fun getInstalledShizukuVersion(): String? {
        return try {
            val context = LanheApplication.getContext() ?: return null
            val packageInfo = context.packageManager.getPackageInfo(
                "moe.shizuku.privileged.api",
                0
            )
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w("ShizukuManager", "Shizuku未安装")
            null
        } catch (e: Exception) {
            Log.e("ShizukuManager", "获取版本失败", e)
            null
        }
    }

    /**
     * 版本号比较
     * @return -1 (v1 < v2), 0 (v1 == v2), 1 (v1 > v2)
     */
    private fun compareVersions(v1: String, v2: String): Int {
        try {
            val v1Parts = v1.split(".").map { it.toIntOrNull() ?: 0 }
            val v2Parts = v2.split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(v1Parts.size, v2Parts.size)) {
                val v1Part = v1Parts.getOrNull(i) ?: 0
                val v2Part = v2Parts.getOrNull(i) ?: 0

                when {
                    v1Part < v2Part -> return -1
                    v1Part > v2Part -> return 1
                }
            }
            return 0
        } catch (e: Exception) {
            Log.e("ShizukuManager", "版本比较失败", e)
            return 0
        }
    }

    /**
     * 检查Shizuku版本是否符合最低要求
     */
    fun isShizukuVersionValid(minimumVersion: String = "13.1.0"): Boolean {
        val installed = getInstalledShizukuVersion() ?: return false
        return compareVersions(installed, minimumVersion) >= 0
    }

    /**
     * 获取Shizuku版本信息
     */
    fun getVersionInfo(): String {
        val installed = getInstalledShizukuVersion() ?: "未安装"
        val required = "13.1.0"
        val status = if (isShizukuVersionValid(required)) "✓ 满足" else "✗ 不满足"
        return "已安装: v$installed | 最低要求: v$required | 状态: $status"
    }

    /**
     * 初始化内置Shizuku
     */
    fun initializeBuiltInShizuku(context: Context) {
        try {
            Log.i("ShizukuManager", "开始初始化内置Shizuku...")

            if (!isShizukuInstalled(context)) {
                Log.i("ShizukuManager", "Shizuku未安装，将从Assets安装")
                val success = ApkInstaller.installApkFromAssets(context, "shizuku.apk")
                if (success) {
                    showToastSafely("✅ Shizuku安装程序已启动，请完成安装后重启应用")
                } else {
                    showToastSafely("❌ Shizuku安装失败，请检查权限")
                }
            } else if (!isShizukuVersionValid()) {
                Log.w("ShizukuManager", "Shizuku版本过旧")
                showToastSafely("⚠️ Shizuku版本过旧，建议升级")
                registerShizukuListeners()
            } else {
                Log.i("ShizukuManager", "Shizuku已安装且版本符合")
                registerShizukuListeners()
            }
        } catch (e: Exception) {
            Log.e("ShizukuManager", "初始化失败", e)
            showToastSafely("❌ Shizuku初始化失败: ${e.message}")
        }
    }

    /**
     * 检查Shizuku是否已安装
     */
    private fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(
                "moe.shizuku.privileged.api",
                0
            )
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 注册Shizuku监听器
     */
    private fun registerShizukuListeners() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            Log.i("ShizukuManager", "Shizuku监听器已注册")
        } catch (e: Exception) {
            Log.e("ShizukuManager", "监听器注册失败", e)
        }
    }

    /**
     * 记录初始化状态
     */
    fun logInitializationStatus() {
        val version = getInstalledShizukuVersion() ?: "未安装"
        val state = shizukuState.value
        val pingable = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }

        Log.i("ShizukuManager", buildString {
            appendLine("========== Shizuku状态 ==========")
            appendLine("已安装版本: v$version")
            appendLine("最低要求: v13.1.0")
            appendLine("权限状态: $state")
            appendLine("服务可用: $pingable")
            appendLine("版本信息: ${getVersionInfo()}")
            appendLine("==================================")
        })
    }
}
```

**预期时间：** 1.5小时
**难度：** ⭐⭐ 中等

---

### Task 5: 应用启动集成 ⭐

**文件：** `app/src/main/java/com/lanhe/gongjuxiang/LanheApplication.kt`

在 `onCreate()` 中添加：

```kotlin
override fun onCreate() {
    super.onCreate()

    // ==================== Shizuku初始化 ====================
    try {
        Log.d("LanheApplication", "初始化内置Shizuku...")
        ShizukuManager.initializeBuiltInShizuku(this)
        ShizukuManager.logInitializationStatus()
    } catch (e: Exception) {
        Log.e("LanheApplication", "Shizuku初始化异常", e)
        // 继续执行，Shizuku失败不应影响应用启动
    }

    // ==================== 其他初始化 ====================
    // ... 现有初始化代码 ...
}
```

**预期时间：** 30分钟
**难度：** ⭐ 简单

---

### Task 6: ShizukuAuthActivity优化 ⭐

**文件：** `app/src/main/java/com/lanhe/gongjuxiang/activities/ShizukuAuthActivity.kt`

**修改要点：**

```kotlin
private fun setupUI() {
    // 显示版本信息
    val versionInfo = ShizukuManager.getVersionInfo()
    binding.tvShizukuVersion.text = "Shizuku状态: $versionInfo"

    // 如果已获得权限，自动关闭
    if (ShizukuManager.shizukuState.value == ShizukuState.Granted) {
        lifecycleScope.launch {
            delay(1000)  // 显示1秒成功界面
            finish()
        }
        return
    }

    // 简化UI - 不再显示4种安装方式
    // 直接显示权限请求按钮
    binding.btnRequestPermission.setOnClickListener {
        requestShizukuPermission()
    }
}

private fun requestShizukuPermission() {
    try {
        if (!Shizuku.pingBinder()) {
            // Shizuku服务不可用，提示用户
            AlertDialog.Builder(this)
                .setTitle("Shizuku未就绪")
                .setMessage("请完成Shizuku安装并在Shizuku应用中授予权限")
                .setPositiveButton("打开Shizuku应用") { _, _ -> openShizukuApp() }
                .setNegativeButton("取消", null)
                .show()
            return
        }

        // Shizuku服务可用，请求权限
        Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
    } catch (e: Exception) {
        Toast.makeText(this, "权限请求失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun openShizukuApp() {
    try {
        val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "无法打开Shizuku应用", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("ShizukuAuthActivity", "打开应用失败", e)
    }
}
```

**预期时间：** 1小时
**难度：** ⭐ 简单

---

### Task 7: 系统功能完整实现 ⭐⭐⭐（可选）

**文件：** `app/src/main/java/com/lanhe/gongjuxiang/shizuku/ShizukuServiceImpl.kt`

完整实现以下占位符方法：
- `installPackage(packagePath)`
- `uninstallPackage(packageName)`
- `getNetworkStats()`
- `getProcessInfo()`
- `getSystemProperties()`
- `setProperty(key, value)`

**预期时间：** 2小时
**难度：** ⭐⭐⭐ 复杂

---

## 分阶段实施计划

### Phase 1：准备阶段（1天）
- [ ] 下载Shizuku v13.1.0+ APK
- [ ] 创建 `app/src/main/assets/` 目录
- [ ] 验证APK完整性

**预期时间：** 30分钟

### Phase 2：配置和权限（1天）
- [ ] Task 1: APK资源集成 ✅
- [ ] Task 2: 权限和FileProvider配置 ✅
- [ ] 编译验证：`./gradlew clean build`

**预期时间：** 45分钟

### Phase 3：核心功能实现（2天）
- [ ] Task 3: ApkInstaller实现
- [ ] Task 4: 版本管理功能
- [ ] Task 5: 应用启动集成
- [ ] 编译验证通过

**预期时间：** 3小时

### Phase 4：UI优化和测试（1-2天）
- [ ] Task 6: ShizukuAuthActivity优化
- [ ] 真机测试：APK安装流程
- [ ] 权限流程测试
- [ ] 编译验证通过

**预期时间：** 2小时

### Phase 5：高级功能（可选，1-2天）
- [ ] Task 7: 系统功能完整实现
- [ ] 命令执行测试
- [ ] 性能优化

**预期时间：** 2-3小时

**总耗时：** 8-11小时（约1-1.5个工作日）

---

## 验证步骤

### 编译验证
```bash
./gradlew clean build
# 预期：BUILD SUCCESSFUL
```

### 功能验证
1. 安装APK到设备
2. 启动应用
3. 观察Shizuku初始化日志
4. 进入ShizukuAuthActivity
5. 点击"请求权限"
6. 在Shizuku应用中授予权限
7. 验证权限状态更新

### 日志验证
```bash
adb logcat | grep "ShizukuManager"
# 应显示类似：
# I ShizukuManager: 开始初始化内置Shizuku...
# I ShizukuManager: Shizuku已安装且版本符合
# I ShizukuManager: Shizuku监听器已注册
```

---

## 关键差异（与原计划对比）

| 项目 | 原计划 | 修订计划 | 原因 |
|------|--------|----------|------|
| 目标版本 | v13.6.0 | v13.1.0+ | v13.6.0不存在于Maven Central |
| 版本升级 | 首要任务 | 可选任务 | 无需强制升级 |
| APK来源 | 从官方下载 | Assets内置 | 用户无需手动安装 |
| 实施周期 | 6天 | 1-2天 | 工作量显著减少 |
| 技术风险 | 高（新版本） | 低（稳定版本） | 使用经过验证的API |

---

## 最终建议

### ✅ 推荐方案
使用Shizuku v13.1.0（或13.1.5最新），集成内置APK安装，无需强制升级到不存在的v13.6.0。

### ⏭️ 后续计划
当官方正式发布Shizuku v13.6.0到Maven Central后，可考虑升级以获得以下改进：
- 更好的Android 15支持
- 改进的Binder连接管理
- 性能优化

### 🚀 立即行动
准备开始Task 1（APK资源集成）。需要用户：
1. 从GitHub Release下载Shizuku v13.1.0+ APK
2. 确认APK完整性
3. 放置到 `app/src/main/assets/` 目录

---

**计划版本：** 2.0 (修订版)
**最后更新：** 2025-11-24
**状态：** 准备就绪，可开始实施

