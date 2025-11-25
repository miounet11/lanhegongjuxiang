# 📢 NotificationHelper 集成指南

## 概述

`NotificationHelper.kt` 是蓝河助手统一的通知提醒系统，支持：
- ✅ 5个通知级别（INFO、SUCCESS、WARNING、ERROR、CRITICAL）
- ✅ 4种通知模式（SILENT、VIBRATION、SOUND、BOTH）
- ✅ 可配置的自动关闭时间
- ✅ 声音和振动反馈
- ✅ Material Design 3.0设计规范

---

## 快速开始

### 1. 基本使用

```kotlin
// 在Activity或Fragment中使用
NotificationHelper.showSuccess(
    context = this,
    view = binding.root,  // 任何View都可以作为锚点
    message = "抓包已启动"
)

NotificationHelper.showError(
    context = this,
    view = binding.root,
    message = "抓包失败：网络连接错误"
)

NotificationHelper.showWarning(
    context = this,
    view = binding.root,
    message = "当前WiFi信号较弱"
)
```

### 2. 高级配置

```kotlin
// 自定义配置
val config = NotificationHelper.NotificationConfig(
    enableNotification = true,
    notificationMode = NotificationHelper.NotificationMode.BOTH,  // 声音+振动
    autoDismissTime = 4000,  // 4秒后自动关闭
    showDetails = true,
    vibrationDuration = 200  // 200ms振动
)

NotificationHelper.showSnackbar(
    context = this,
    view = binding.root,
    message = "数据包已捕获：125 个包，共 2.5MB",
    level = NotificationHelper.NotificationLevel.SUCCESS,
    config = config,
    action = "查看详情",
    actionCallback = {
        // 用户点击"查看详情"按钮时的回调
        showPacketDetails()
    }
)
```

### 3. 场景快速配置

```kotlin
// 使用预设的场景配置
val quickActionConfig = NotificationHelper.getConfigForScene("quick_action")
val errorConfig = NotificationHelper.getConfigForScene("error")
val criticalConfig = NotificationHelper.getConfigForScene("critical")

NotificationHelper.showError(
    context = this,
    view = binding.root,
    message = "严重错误：无法访问网络",
    config = criticalConfig  // 自动配置为：声音+振动、5秒显示时间
)
```

---

## 在网络抓包功能中集成

### PacketCaptureActivity 集成示例

```kotlin
import com.lanhe.gongjuxiang.utils.NotificationHelper

class PacketCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPacketCaptureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPacketCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置开始/停止抓包按钮
        binding.btnStartStopCapture.setOnClickListener {
            startPacketCapture()
        }

        // 设置清除数据按钮
        binding.btnClearData.setOnClickListener {
            clearCapturedData()
        }
    }

    private fun startPacketCapture() {
        try {
            // 执行抓包逻辑
            val success = capturePackets()

            if (success) {
                // 显示成功通知
                NotificationHelper.showSuccess(
                    context = this,
                    view = binding.root,
                    message = "🎛️ 已开始捕获数据包",
                    config = NotificationHelper.getConfigForScene("quick_action")
                )

                // 更新UI
                binding.btnStartStopCapture.text = "⏹️ 停止抓包"
                binding.tvCaptureStatus.text = "🟢 正在抓包..."
            } else {
                // 显示错误通知
                NotificationHelper.showError(
                    context = this,
                    view = binding.root,
                    message = "❌ 抓包启动失败，请检查权限",
                    config = NotificationHelper.getConfigForScene("error")
                )
            }
        } catch (e: Exception) {
            // 显示严重错误通知
            NotificationHelper.showSnackbar(
                context = this,
                view = binding.root,
                message = "系统错误：${e.message}",
                level = NotificationHelper.NotificationLevel.CRITICAL,
                config = NotificationHelper.getConfigForScene("critical")
            )
        }
    }

    private fun clearCapturedData() {
        // 清除数据逻辑
        val clearedCount = 0  // 假设清除了100个包

        NotificationHelper.showSnackbar(
            context = this,
            view = binding.root,
            message = "已清除 $clearedCount 个数据包",
            level = NotificationHelper.NotificationLevel.INFO,
            config = NotificationHelper.getConfigForScene("quick_action")
        )

        // 更新UI
        binding.tvPacketCount.text = "数据包: 0"
        binding.tvDataSize.text = "数据量: 0B"
    }
}
```

---

## 在网络诊断功能中集成

### NetworkDiagnosticActivity 集成示例

```kotlin
import com.lanhe.gongjuxiang.utils.NotificationHelper

class NetworkDiagnosticActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNetworkDiagnosticBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkDiagnosticBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartDiagnostic.setOnClickListener {
            performNetworkDiagnostic()
        }
    }

    private fun performNetworkDiagnostic() {
        lifecycleScope.launch {
            try {
                // 显示诊断开始通知
                NotificationHelper.showInfo(
                    context = this@NetworkDiagnosticActivity,
                    view = binding.root,
                    message = "📡 开始进行网络诊断...",
                    config = NotificationHelper.getConfigForScene("quick_action")
                )

                // 执行诊断
                val result = runNetworkDiagnostics()

                if (result.isHealthy) {
                    // 网络状态良好
                    NotificationHelper.showSuccess(
                        context = this@NetworkDiagnosticActivity,
                        view = binding.root,
                        message = "✅ 网络状态良好，延迟：${result.latency}ms",
                        config = NotificationHelper.getConfigForScene("success")
                    )
                } else if (result.isWarning) {
                    // 网络状态一般
                    NotificationHelper.showWarning(
                        context = this@NetworkDiagnosticActivity,
                        view = binding.root,
                        message = "⚠️ 网络状态一般，建议检查WiFi信号",
                        config = NotificationHelper.getConfigForScene("warning")
                    )
                } else {
                    // 网络状态差
                    NotificationHelper.showError(
                        context = this@NetworkDiagnosticActivity,
                        view = binding.root,
                        message = "❌ 网络连接有问题，请重试",
                        config = NotificationHelper.getConfigForScene("error")
                    )
                }

                // 更新诊断结果UI
                updateDiagnosticResults(result)

            } catch (e: Exception) {
                // 诊断过程发生错误
                NotificationHelper.showSnackbar(
                    context = this@NetworkDiagnosticActivity,
                    view = binding.root,
                    message = "诊断异常：${e.message}",
                    level = NotificationHelper.NotificationLevel.ERROR,
                    config = NotificationHelper.getConfigForScene("error")
                )
            }
        }
    }
}
```

---

## 通知级别参考

### NotificationLevel 枚举

| 级别 | 颜色 | 图标 | 用途 | 示例 |
|------|------|------|------|------|
| **INFO** | 蓝色 | ℹ️ | 信息性提醒 | "已开始捕获数据包" |
| **SUCCESS** | 绿色 | ✅ | 成功提醒 | "网络诊断完成" |
| **WARNING** | 橙色 | ⚠️ | 警告提醒 | "WiFi信号较弱" |
| **ERROR** | 红色 | ❌ | 错误提醒 | "网络连接失败" |
| **CRITICAL** | 深红 | 🔴 | 严重提醒 | "系统错误" |

### NotificationMode 枚举

| 模式 | 描述 | 场景 |
|------|------|------|
| **SILENT** | 无声反馈 | 静音场景（会议、库等） |
| **VIBRATION** | 仅振动 | 默认行为，安静但有感知 |
| **SOUND** | 仅声音 | 特殊情况需要音频提示 |
| **BOTH** | 声音+振动 | 严重错误或关键操作 |

---

## 场景快速配置

### getConfigForScene() 预设

```kotlin
// 快速操作（清除、过滤等）
"quick_action" → {
    自动关闭: 2秒
    振动: 100ms
    模式: VIBRATION
}

// 成功操作（诊断完成、导出成功等）
"success" → {
    自动关闭: 2.5秒
    振动: 150ms
    模式: VIBRATION
}

// 警告情况（信号弱、内存低等）
"warning" → {
    自动关闭: 3.5秒
    振动: 200ms
    模式: VIBRATION
}

// 错误情况（连接失败、权限缺失等）
"error" → {
    自动关闭: 4秒
    振动: 300ms
    模式: VIBRATION
}

// 严重错误（系统崩溃、数据丢失等）
"critical" → {
    自动关闭: 5秒
    振动: 400ms
    模式: BOTH (声音+振动)
}
```

---

## 高级用法

### 带操作按钮的通知

```kotlin
NotificationHelper.showSnackbar(
    context = this,
    view = binding.root,
    message = "检测到新的应用更新",
    level = NotificationHelper.NotificationLevel.INFO,
    config = NotificationHelper.getConfigForScene("info"),
    action = "立即更新",
    actionCallback = {
        // 点击按钮时的处理逻辑
        startUpdateProcess()
    }
)
```

### 无自动关闭的持久通知

```kotlin
val persistentConfig = NotificationHelper.NotificationConfig(
    enableNotification = true,
    notificationMode = NotificationHelper.NotificationMode.VIBRATION,
    autoDismissTime = 0,  // 0 表示不自动关闭
    showDetails = true
)

NotificationHelper.showSnackbar(
    context = this,
    view = binding.root,
    message = "数据同步进行中...",
    level = NotificationHelper.NotificationLevel.INFO,
    config = persistentConfig
)
```

### 自定义完全配置

```kotlin
val customConfig = NotificationHelper.NotificationConfig(
    enableNotification = true,
    notificationMode = NotificationHelper.NotificationMode.BOTH,
    autoDismissTime = 5000,
    showDetails = true,
    vibrationDuration = 250,
    playSound = true
)

NotificationHelper.showSnackbar(
    context = this,
    view = binding.root,
    message = "重要操作提醒",
    level = NotificationHelper.NotificationLevel.WARNING,
    config = customConfig
)
```

---

## 权限要求

NotificationHelper 需要以下权限（在 AndroidManifest.xml 中已声明）：

```xml
<!-- 振动权限 -->
<uses-permission android:name="android.permission.VIBRATE" />

<!-- 音频权限（可选，仅在需要播放声音时） -->
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

---

## 最佳实践

### ✅ 推荐做法

1. **使用预设场景配置**
   ```kotlin
   // 好 ✅
   NotificationHelper.showSuccess(
       context = this,
       view = binding.root,
       message = "操作成功",
       config = NotificationHelper.getConfigForScene("success")
   )
   ```

2. **为不同操作选择适当的级别**
   ```kotlin
   // 好 ✅
   if (isSuccess) {
       NotificationHelper.showSuccess(...)
   } else if (isWarning) {
       NotificationHelper.showWarning(...)
   } else {
       NotificationHelper.showError(...)
   }
   ```

3. **提供有意义的错误消息**
   ```kotlin
   // 好 ✅
   NotificationHelper.showError(
       context = this,
       view = binding.root,
       message = "网络连接失败：请检查WiFi连接"
   )

   // 不好 ❌
   NotificationHelper.showError(
       context = this,
       view = binding.root,
       message = "错误"
   )
   ```

### ❌ 避免做法

1. **过度显示通知**
   ```kotlin
   // 不好 ❌ - 每次循环都显示通知
   for (item in items) {
       NotificationHelper.showInfo(...)
   }

   // 好 ✅ - 仅显示最终结果
   NotificationHelper.showSuccess(context, binding.root, "已处理 ${items.size} 个项目")
   ```

2. **忽视用户配置**
   ```kotlin
   // 不好 ❌ - 忽视用户的禁用设置
   config.enableNotification = true

   // 好 ✅ - 遵循用户的设置
   val userConfig = loadUserNotificationPreferences()
   NotificationHelper.showInfo(context, binding.root, message, config = userConfig)
   ```

---

## 故障排查

### 问题：通知不显示
**解决方案：**
1. 确保 View（锚点）已添加到窗口
2. 检查 `enableNotification` 是否为 true
3. 确保 import 了正确的 NotificationHelper 类

### 问题：振动不工作
**解决方案：**
1. 检查是否有 `android.permission.VIBRATE` 权限
2. 检查通知模式是否为 VIBRATION 或 BOTH
3. 检查设备是否支持振动（可能关闭了振动）

### 问题：声音不工作
**解决方案：**
1. 检查设备音量设置
2. 检查系统声音是否已启用
3. 某些设备可能需要额外的权限配置

---

## 文件清单

### 新增文件

| 文件 | 位置 | 功能 |
|------|------|------|
| **NotificationHelper.kt** | `app/src/main/java/com/lanhe/gongjuxiang/utils/` | 核心通知系统实现 |
| **notification_view.xml** | `app/src/main/res/layout/` | 通知卡片布局（参考） |
| **NOTIFICATION_INTEGRATION_GUIDE.md** | 项目根目录 | 本集成指南 |

### 已有依赖

- ✅ Material Design Components (已集成)
- ✅ AndroidX (已集成)
- ✅ Kotlin Coroutines (可选，用于异步操作)

---

## 总结

NotificationHelper 为蓝河助手提供了一套**统一的、可配置的、符合Material Design 3.0的通知系统**，
它能够：

✅ 提升用户体验 - 清晰的视觉反馈
✅ 提高应用质感 - 专业的设计规范
✅ 增强交互感知 - 声音和振动反馈
✅ 灵活配置 - 适应各种场景需求

**立即开始使用 NotificationHelper 为网络抓包和网络诊断功能添加专业的通知提醒！**
