# 🛠️ Shizuku 内置集成 - 代码实现清单

**优先级顺序：** 按照实现优先级排列
**难度等级：** ⭐ 简单 | ⭐⭐ 中等 | ⭐⭐⭐ 复杂

---

## 任务列表

### Task 1: 依赖版本升级 ⭐

**文件：** `gradle/libs.versions.toml`
**预期时间：** 15 分钟
**影响范围：** 小

**当前状态：**
```toml
[versions]
shizukuApi = "13.1.0"
shizukuProvider = "13.1.0"
```

**需要修改为：**
```toml
[versions]
shizukuApi = "13.6.0"
shizukuProvider = "13.6.0"
```

**验证方法：**
```bash
./gradlew dependencies | grep shizuku
# 应该显示 13.6.0 版本
```

---

### Task 2: 完整实现 ApkInstaller.installApkFromAssets() ⭐⭐

**文件：** `app/src/main/java/com/lanhe/gongjuxiang/utils/ApkInstaller.kt`
**预期时间：** 1 小时
**影响范围：** 中等

**当前状态：** 需要完整实现

**需要实现的核心逻辑：**

```kotlin
object ApkInstaller {

    /**
     * 从 Assets 安装 APK
     *
     * 流程：
     * 1. 检查权限 (REQUEST_INSTALL_PACKAGES)
     * 2. 从 Assets 复制到缓存目录
     * 3. 创建 FileProvider URI
     * 4. 启动系统安装程序
     */
    fun installApkFromAssets(
        context: Context,
        assetFileName: String
    ): Boolean {
        return try {
            // Step 1: 权限检查
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Log.w("ApkInstaller", "缺少 INSTALL_PACKAGES 权限")
                    return false
                }
            }

            // Step 2: 从 Assets 复制 APK
            val tempFile = File(context.cacheDir, assetFileName)
            context.assets.open(assetFileName).use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Step 3: 创建 URI（兼容不同 Android 版本）
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)

            Log.i("ApkInstaller", "APK 安装程序已启动: $assetFileName")
            return true

        } catch (e: Exception) {
            Log.e("ApkInstaller", "APK 安装失败", e)
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
            true // 低版本 Android 默认有权限
        }
    }

    /**
     * 请求安装权限（仅 Android 8.0+）
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

**FileProvider 配置：**

需要在 `app/src/main/res/xml/file_paths.xml` 中添加：

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <!-- APK 安装程序 -->
    <cache-path name="apk_install" path="." />

    <!-- 其他文件提供商配置 -->
    <files-path name="app_files" path="." />
    <external-path name="documents" path="Documents" />
</paths>
```

在 `AndroidManifest.xml` 中声明 FileProvider：

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

**验证方法：**
```kotlin
// 在 ShizukuAuthActivity 中测试
val success = ApkInstaller.installApkFromAssets(this, "shizuku.apk")
// 应该启动安装程序
```

---

### Task 3: 在 ShizukuManager 中添加版本管理 ⭐⭐

**文件：** `app/src/main/java/com/lanhe/gongjuxiang/utils/ShizukuManager.kt`
**预期时间：** 1.5 小时
**影响范围：** 中等（核心文件）

**需要添加的方法：**

```kotlin
object ShizukuManager {

    // ... 现有代码 ...

    // ==================== 版本管理 ====================

    /**
     * 获取已安装的 Shizuku 版本
     *
     * @return 版本字符串 (如 "13.6.0") 或 null
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
            Log.w("ShizukuManager", "Shizuku 未安装")
            null
        } catch (e: Exception) {
            Log.e("ShizukuManager", "获取版本失败", e)
            null
        }
    }

    /**
     * 版本号比较
     *
     * @return -1 (v1 < v2), 0 (v1 == v2), 1 (v1 > v2)
     */
    private fun compareVersions(v1: String, v2: String): Int {
        try {
            val v1Parts = v1.split(".").map { it.toIntOrNull() ?: 0 }
            val v2Parts = v2.split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0..maxOf(v1Parts.size, v2Parts.size) - 1) {
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
     * 检查 Shizuku 版本是否符合最低要求
     *
     * @param minimumVersion 最低版本 (如 "13.6.0")
     * @return true 如果已安装版本 >= minimumVersion
     */
    fun isShizukuVersionValid(minimumVersion: String = "13.6.0"): Boolean {
        val installed = getInstalledShizukuVersion() ?: return false
        return compareVersions(installed, minimumVersion) >= 0
    }

    /**
     * 检查是否需要升级 Shizuku
     */
    fun shouldUpgradeShizuku(): Boolean {
        return !isShizukuVersionValid("13.6.0")
    }

    /**
     * 获取 Shizuku 版本信息
     *
     * @return 格式化的版本信息字符串
     */
    fun getVersionInfo(): String {
        val installed = getInstalledShizukuVersion() ?: "未安装"
        val required = "13.6.0"
        val status = if (isShizukuVersionValid(required)) "✓ 满足" else "✗ 不满足"
        return "已安装: v$installed | 最低要求: v$required | 状态: $status"
    }

    // ==================== 自动初始化 ====================

    /**
     * 初始化内置 Shizuku
     * 在应用启动时调用
     *
     * 流程：
     * 1. 检查 Shizuku 是否已安装
     * 2. 如果未安装，从 Assets 安装
     * 3. 如果已安装，初始化状态和监听器
     */
    fun initializeBuiltInShizuku(context: Context) {
        try {
            Log.i("ShizukuManager", "开始初始化内置 Shizuku...")

            when {
                !isShizukuInstalled(context) -> {
                    Log.i("ShizukuManager", "Shizuku 未安装，将从 Assets 安装")
                    val success = ApkInstaller.installApkFromAssets(context, "shizuku.apk")
                    if (success) {
                        showToastSafely("✅ Shizuku 安装程序已启动，请完成安装后重启应用")
                    } else {
                        showToastSafely("❌ Shizuku 安装失败，请检查权限")
                    }
                }
                !isShizukuVersionValid("13.6.0") -> {
                    Log.w("ShizukuManager", "Shizuku 版本过旧，建议升级")
                    showToastSafely("⚠️ Shizuku 版本过旧，建议从应用中升级")
                    // 继续初始化，但提示用户
                    registerShizukuListeners()
                }
                else -> {
                    Log.i("ShizukuManager", "Shizuku 已安装且版本符合要求")
                    registerShizukuListeners()
                }
            }
        } catch (e: Exception) {
            Log.e("ShizukuManager", "初始化失败", e)
            showToastSafely("❌ Shizuku 初始化失败: ${e.message}")
        }
    }

    /**
     * 检查 Shizuku 是否已安装
     */
    private fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(
                "moe.shizuku.privileged.api",
                PackageManager.GET_ACTIVITIES
            )
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 注册 Shizuku 监听器
     * （调用现有的注册逻辑）
     */
    private fun registerShizukuListeners() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            Log.i("ShizukuManager", "Shizuku 监听器已注册")
        } catch (e: Exception) {
            Log.e("ShizukuManager", "监听器注册失败", e)
        }
    }

    // ==================== 日志 ====================

    /**
     * 记录 Shizuku 初始化状态
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
            appendLine("========== Shizuku 状态 ==========")
            appendLine("已安装版本: v$version")
            appendLine("最低要求: v13.6.0")
            appendLine("权限状态: $state")
            appendLine("服务可用: $pingable")
            appendLine("版本信息: ${getVersionInfo()}")
            appendLine("==================================")
        })
    }
}
```

**验证方法：**
```kotlin
// 在应用启动时调用
val version = ShizukuManager.getInstalledShizukuVersion()
Log.i("Shizuku", "版本: $version")

// 检查版本有效性
if (ShizukuManager.isShizukuVersionValid("13.6.0")) {
    // 版本符合要求
}

// 记录状态
ShizukuManager.logInitializationStatus()
```

---

### Task 4: 修改 LanheApplication 启动流程 ⭐

**文件：** `app/src/main/java/com/lanhe/gongjuxiang/LanheApplication.kt`
**预期时间：** 30 分钟
**影响范围：** 小（应用启动）

**需要在 onCreate() 中添加：**

```kotlin
class LanheApplication : Application() {

    companion object {
        // ... 现有代码 ...
    }

    override fun onCreate() {
        super.onCreate()

        // ==================== Shizuku 初始化 ====================
        // 这应该在其他初始化之前进行
        try {
            Log.d("LanheApplication", "初始化内置 Shizuku...")
            ShizukuManager.initializeBuiltInShizuku(this)
            ShizukuManager.logInitializationStatus()
        } catch (e: Exception) {
            Log.e("LanheApplication", "Shizuku 初始化异常", e)
            // 继续执行，Shizuku 失败不应该影响应用启动
        }

        // ==================== 其他初始化 ====================
        // ... 现有的其他初始化代码 ...

        // Hilt 初始化（如果使用）
        // DataManager 初始化
        // 其他组件初始化
    }
}
```

**验证方法：**
```bash
# 安装应用并检查日志
adb logcat | grep "ShizukuManager"
# 应该看到初始化日志和版本信息
```

---

### Task 5: 优化 ShizukuAuthActivity ⭐

**文件：** `app/src/main/java/com/lanhe/gongjuxiang/activities/ShizukuAuthActivity.kt`
**预期时间：** 1 小时
**影响范围：** 中等（用户交互）

**需要修改的部分：**

```kotlin
class ShizukuAuthActivity : AppCompatActivity() {

    // ... 现有代码 ...

    private fun setupUI() {
        // ... 现有的 UI 代码 ...

        // 添加版本信息显示
        val versionInfo = ShizukuManager.getVersionInfo()
        binding.tvShizukuVersion.text = "Shizuku 状态: $versionInfo"

        // 如果已有权限，自动关闭
        if (ShizukuManager.shizukuState.value == ShizukuState.Granted) {
            Log.i("ShizukuAuthActivity", "已获得权限，自动关闭")
            lifecycleScope.launch {
                delay(1000) // 显示 1 秒成功界面
                finish()
            }
        }

        // 设置权限请求按钮
        binding.btnRequestPermission.setOnClickListener {
            requestShizukuPermission()
        }
    }

    private fun requestShizukuPermission() {
        try {
            // 检查 Shizuku 服务可用性
            if (!Shizuku.pingBinder()) {
                Log.w("ShizukuAuthActivity", "Shizuku 服务不可用")

                // 提示用户完成 Shizuku 安装
                AlertDialog.Builder(this)
                    .setTitle("Shizuku 未就绪")
                    .setMessage("请完成 Shizuku 安装并在 Shizuku 应用中授予权限")
                    .setPositiveButton("打开 Shizuku 应用") { _, _ ->
                        openShizukuApp()
                    }
                    .setNegativeButton("取消", null)
                    .show()

                return
            }

            // Shizuku 服务可用，请求权限
            Log.i("ShizukuAuthActivity", "开始请求 Shizuku 权限...")
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)

        } catch (e: Exception) {
            Log.e("ShizukuAuthActivity", "权限请求失败", e)
            Toast.makeText(this, "权限请求失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开 Shizuku 应用，用户可在其中授予权限
     */
    private fun openShizukuApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                startActivity(intent)
                Log.i("ShizukuAuthActivity", "打开 Shizuku 应用")
            } else {
                Toast.makeText(this, "无法打开 Shizuku 应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ShizukuAuthActivity", "打开应用失败", e)
        }
    }

    /**
     * 简化 APK 安装选项
     * 由于 Shizuku 已内置，不再需要 4 种安装方式
     */
    private fun showInstallOptions() {
        // 已弃用 - Shizuku 应该已通过 Assets 自动安装
        Log.d("ShizukuAuthActivity", "APK 安装选项已简化（使用内置方式）")
    }
}
```

---

### Task 6: 完整实现系统功能 ⭐⭐⭐

**文件：** `app/src/main/java/com/lanhe/gongjuxiang/shizuku/ShizukuServiceImpl.kt`
**预期时间：** 2 小时
**影响范围：** 大（核心功能）

**需要完整实现的方法：**

```kotlin
// ... 在 ShizukuServiceImpl 中实现 ...

/**
 * 完整实现 APK 安装
 */
override fun installPackage(packagePath: String): Boolean {
    return try {
        if (!CommandValidator.isValidPackagePath(packagePath)) {
            Log.w("ShizukuServiceImpl", "无效的包路径: $packagePath")
            return false
        }

        val command = "pm install -r \"$packagePath\""
        val result = executeCommand(command)

        if (result.success && !result.output.contains("Failure")) {
            Log.i("ShizukuServiceImpl", "APK 安装成功: $packagePath")
            true
        } else {
            Log.e("ShizukuServiceImpl", "APK 安装失败: ${result.output}")
            false
        }
    } catch (e: Exception) {
        Log.e("ShizukuServiceImpl", "APK 安装异常", e)
        false
    }
}

/**
 * 完整实现 APK 卸载
 */
override fun uninstallPackage(packageName: String): Boolean {
    return try {
        if (!CommandValidator.isValidPackageName(packageName)) {
            Log.w("ShizukuServiceImpl", "无效的包名: $packageName")
            return false
        }

        val command = "pm uninstall $packageName"
        val result = executeCommand(command)

        if (result.success && result.output.contains("Success")) {
            Log.i("ShizukuServiceImpl", "APK 卸载成功: $packageName")
            true
        } else {
            Log.e("ShizukuServiceImpl", "APK 卸载失败: ${result.output}")
            false
        }
    } catch (e: Exception) {
        Log.e("ShizukuServiceImpl", "APK 卸载异常", e)
        false
    }
}

/**
 * 完整实现网络统计
 */
override fun getNetworkStats(): NetworkStats {
    return try {
        val result = executeCommand("cat /proc/net/dev")
        if (!result.success) {
            Log.w("ShizukuServiceImpl", "获取网络统计失败")
            return NetworkStats()
        }

        // 解析 /proc/net/dev 格式
        // Inter-|   Receive                                                |  Transmit
        //  face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
        //    lo: 1234567  5678   0    0    0     0          0         0  1234567  5678   0    0    0     0    0          0
        //  eth0: 9876543  2345   1    2    0     0          0         0  9876543  2345   0    0    0     0    0          0

        val lines = result.output.lines()
        var totalReceived = 0L
        var totalTransmitted = 0L
        var totalPacketsReceived = 0L
        var totalPacketsTransmitted = 0L

        for (line in lines) {
            if (!line.contains(":")) continue

            val parts = line.split(":")[1].trim().split("\\s+".toRegex())
            if (parts.size < 10) continue

            try {
                val received = parts[0].toLong()
                val packetsReceived = parts[1].toLong()
                val transmitted = parts[8].toLong()
                val packetsTransmitted = parts[9].toLong()

                totalReceived += received
                totalTransmitted += transmitted
                totalPacketsReceived += packetsReceived
                totalPacketsTransmitted += packetsTransmitted
            } catch (e: NumberFormatException) {
                continue
            }
        }

        NetworkStats(
            receivedBytes = totalReceived,
            transmittedBytes = totalTransmitted,
            receivedPackets = totalPacketsReceived,
            transmittedPackets = totalPacketsTransmitted
        )
    } catch (e: Exception) {
        Log.e("ShizukuServiceImpl", "网络统计异常", e)
        NetworkStats()
    }
}

/**
 * 完整实现进程信息获取
 */
override fun getProcessInfo(): List<ProcessInfo> {
    return try {
        // 使用 ps 命令获取进程列表
        val result = executeCommand("ps -e")
        if (!result.success) {
            Log.w("ShizukuServiceImpl", "获取进程列表失败")
            return emptyList()
        }

        val processes = mutableListOf<ProcessInfo>()
        val lines = result.output.lines()

        for (line in lines.drop(1)) { // 跳过 header
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size < 9) continue

            try {
                val pid = parts[1].toInt()
                val ppid = parts[2].toInt()
                val name = parts.drop(8).joinToString(" ")

                processes.add(
                    ProcessInfo(
                        pid = pid,
                        name = name,
                        memoryMB = 0f, // 需要额外解析 /proc/$pid/status
                        uid = parts[0].toIntOrNull() ?: 0
                    )
                )
            } catch (e: Exception) {
                continue
            }
        }

        Log.i("ShizukuServiceImpl", "获取进程信息成功: ${processes.size} 个进程")
        processes
    } catch (e: Exception) {
        Log.e("ShizukuServiceImpl", "进程信息获取异常", e)
        emptyList()
    }
}

/**
 * 获取系统属性
 */
override fun getSystemProperties(): Map<String, String> {
    return try {
        val result = executeCommand("getprop")
        if (!result.success) {
            return emptyMap()
        }

        val properties = mutableMapOf<String, String>()
        val regex = Regex("""\[(.*?)\]: \[(.*?)\]""")

        for (line in result.output.lines()) {
            val matchResult = regex.find(line)
            if (matchResult != null) {
                val key = matchResult.groupValues[1]
                val value = matchResult.groupValues[2]
                properties[key] = value
            }
        }

        Log.i("ShizukuServiceImpl", "获取系统属性成功: ${properties.size} 个")
        properties
    } catch (e: Exception) {
        Log.e("ShizukuServiceImpl", "系统属性获取异常", e)
        emptyMap()
    }
}

/**
 * 设置系统属性（需要高权限）
 */
override fun setProperty(key: String, value: String): Boolean {
    return try {
        if (!CommandValidator.isValidPropertyKey(key)) {
            Log.w("ShizukuServiceImpl", "无效的属性键: $key")
            return false
        }

        val command = "setprop \"$key\" \"$value\""
        val result = executeCommand(command)

        if (result.success) {
            Log.i("ShizukuServiceImpl", "属性设置成功: $key=$value")
            true
        } else {
            Log.w("ShizukuServiceImpl", "属性设置失败: $key")
            false
        }
    } catch (e: Exception) {
        Log.e("ShizukuServiceImpl", "属性设置异常", e)
        false
    }
}
```

---

## 优先级顺序

### 必须完成（Phase 1-2）
1. ✅ Task 1 - 依赖版本升级
2. ✅ Task 2 - APK 安装器实现
3. ✅ Task 3 - 版本管理
4. ✅ Task 4 - 应用启动修改

### 应该完成（Phase 3）
5. ⚠️ Task 5 - ShizukuAuthActivity 优化
6. ⚠️ Task 6 - 系统功能完整实现

### 可选完成（Phase 4+）
- 性能优化
- 更多系统功能
- 高级特性

---

## 验证步骤

```bash
# Step 1: 编译验证
./gradlew clean build

# Step 2: 安装应用
./gradlew installDebug

# Step 3: 检查日志
adb logcat | grep "ShizukuManager"

# Step 4: 测试权限流程
# 打开应用 → 进入 ShizukuAuthActivity → 点击请求权限

# Step 5: 验证功能
# 在 ShizukuAuthActivity 中测试各种系统操作
```

---

**现在准备好开始编码了吗？我可以为你逐个实现这些任务。**
