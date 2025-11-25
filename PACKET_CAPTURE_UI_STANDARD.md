# 📊 网络抓包界面UI风格统一规范

## 问题分析

**用户需求：** "网络抓包的界面优化一下，需要与程序保持风格一体，统一一下字体大小、配色、图标、消息提醒，能设定，全部统一起来"

### 当前问题

1. **字体大小不统一**
   - activity_packet_capture.xml: 18sp标题、14sp内容、12sp统计信息
   - item_packet.xml: 12sp时间戳、14sp摘要、12sp高级信息
   - activity_network_diagnostic.xml: 20sp标题、16sp内容、12sp标签

2. **配色不统一**
   - Packet Capture: 使用primary色
   - Network Diagnostic: 使用多种颜色(signal_excellent, signal_good等)
   - 需要统一为应用主题色

3. **图标emoji不协调**
   - 🎛️ 🎛️ 🔍 - 控制区域
   - 📊 📦 - 统计区域
   - 📶 📍 🌐 🔋 - 诊断区域
   - 缺乏一致性和专业性

4. **消息提醒**
   - 目前没有可配置的提醒功能
   - 警告提示不统一
   - 缺乏通知级别配置

---

## 统一规范设计

### 字体大小标准化

根据应用Material Design 3.0规范，制定统一的字体体系：

| 用途 | 字体大小 | 字体样式 | 颜色 | 应用场景 |
|------|---------|---------|------|---------|
| 主标题 | 20sp | Bold + monospace | text_primary | Activity标题 |
| 卡片标题 | 16sp | Bold | text_primary | 各卡片section标题 |
| 正文内容 | 14sp | Normal | text_primary | 主要数据显示 |
| 标签标注 | 12sp | Normal | text_secondary | 标签、说明 |
| 提示信息 | 11sp | Normal | text_hint | 辅助信息、代码 |
| 统计数值 | 14sp | Bold | text_primary | 大数值、统计 |
| 按钮文本 | 14sp | Bold | white/primary | 操作按钮 |

### 配色统一方案

#### Primary Brand Colors (主色系)
```xml
primary        → #2563EB (Electric Blue)     - 主操作、链接
primary_dark   → #1E40AF (Deep Blue)        - 悬停、选中
primary_light  → #DBEAFE (Soft Blue)        - 背景、禁用
accent         → #06B6D4 (Cyan)             - 强调、次要操作
```

#### Functional Colors (功能色)
```xml
success        → #10B981 (Emerald)          - 正常、成功、优秀
warning        → #F59E0B (Amber)            - 注意、中等
error/danger   → #EF4444 (Red)              - 错误、严重
info           → #3B82F6 (Blue)             - 信息提示
```

#### Text Colors (文本色)
```xml
text_primary   → #0F172A (Slate 900)        - 主要文本
text_secondary → #64748B (Slate 500)        - 次要文本
text_hint      → #94A3B8 (Slate 400)        - 提示文本
```

### 图标设计规范

#### Icon Categories (图标分类)

**网络状态** (Network Status)
- 🌐 连接状态
- 📶 信号强度
- 📊 数据统计
- 📡 网络诊断

**操作控制** (Control)
- ▶️ 开始/播放
- ⏹️ 停止/暂停
- 🔄 刷新
- 🗑️ 清除
- 📤 导出
- 🔍 搜索/过滤

**警告状态** (Alerts)
- ✅ 成功
- ⚠️ 警告
- ❌ 错误
- 💡 建议/信息

**位置导航** (Location)
- 📍 位置/扫描
- 🎯 目标
- 🚀 优化

### 消息提醒系统设计

#### 提醒级别

```kotlin
enum class NotificationLevel {
    INFO,      // 信息性提醒 - 蓝色
    SUCCESS,   // 成功提醒 - 绿色
    WARNING,   // 警告提醒 - 橙色
    ERROR,     // 错误提醒 - 红色
    CRITICAL   // 严重提醒 - 深红色
}
```

#### 提醒配置项

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| 启用提醒 | Boolean | true | 全局启用/禁用 |
| 提醒音量 | Enum | 振动 | 声音、振动、静音 |
| 自动关闭时间 | Int | 3秒 | 提醒自动关闭延迟 |
| 显示详情 | Boolean | true | 是否显示详细信息 |

#### 提醒样式规范

```xml
<!-- INFO提醒: 蓝色 -->
background="@color/info"
textColor="@color/white"
cornerRadius="8dp"
elevation="4dp"
padding="12dp"
iconSize="24dp"

<!-- SUCCESS提醒: 绿色 -->
background="@color/success"
textColor="@color/white"

<!-- WARNING提醒: 橙色 -->
background="@color/warning"
textColor="@color/white"

<!-- ERROR提醒: 红色 -->
background="@color/error"
textColor="@color/white"
```

---

## 具体优化方案

### 方案1: activity_packet_capture.xml

#### 修改内容

**1. 标题栏统一**
```xml
<!-- 修改前 -->
<TextView ... android:textSize="18sp" />  <!-- 不统一 -->

<!-- 修改后 -->
<TextView ...
    android:textSize="20sp"              <!-- 统一为主标题 -->
    android:fontFamily="monospace"       <!-- 添加字体 -->
    android:textColor="@color/text_primary"
    android:textStyle="bold" />
```

**2. 卡片标题统一**
```xml
<!-- 所有卡片标题统一为 -->
<TextView
    android:textSize="16sp"
    android:textStyle="bold"
    android:fontFamily="monospace"
    android:textColor="@color/text_primary"
    android:layout_marginBottom="12dp" />
```

**3. 统计信息统一**
```xml
<!-- 修改前: 14sp -->
<!-- 修改后: 标签12sp, 数值14sp -->
<TextView ... android:textSize="12sp" android:textColor="@color/text_secondary" />  <!-- 标签 -->
<TextView ... android:textSize="14sp" android:textColor="@color/text_primary" />    <!-- 数值 -->
```

**4. 按钮统一**
```xml
<com.google.android.material.button.MaterialButton
    style="?attr/materialButtonOutlinedStyle"
    android:layout_height="40dp"
    android:textSize="14sp"
    android:textStyle="bold"
    app:cornerRadius="8dp"
    android:textColor="@color/primary" />
```

#### 修改位置

| 行号 | 内容 | 修改 |
|------|------|------|
| 51-52 | 标题 | textSize 18→20, 添加fontFamily |
| 69-70 | tvCaptureStatus | 改为14sp bold |
| 77, 96, 107, 117 | 按钮 | 改为14sp bold |
| 162-163, 171-172, 188-199 | 统计标签 | 改为12sp secondary |

### 方案2: item_packet.xml

#### 修改内容

**1. 整体padding统一**
```xml
<!-- 修改前 -->
android:padding="16dp"

<!-- 修改后 -->
android:paddingHorizontal="16dp"
android:paddingVertical="12dp"
```

**2. 时间戳与大小统一**
```xml
<!-- 修改前: 12sp -->
<!-- 修改后: 12sp -->
<TextView
    android:textSize="12sp"
    android:textColor="@color/text_secondary" />
```

**3. 摘要信息统一**
```xml
<!-- 修改前: 14sp -->
<!-- 修改后: 14sp bold primary -->
<TextView
    android:id="@+id/tvSummary"
    android:textSize="14sp"
    android:textStyle="bold"
    android:textColor="@color/text_primary" />
```

**4. 协议标签颜色**
```xml
<!-- 统一使用info色 -->
<TextView
    android:id="@+id/tvProtocol"
    android:textColor="@color/info"
    android:textSize="12sp"
    android:textStyle="bold" />
```

### 方案3: activity_network_diagnostic.xml

#### 修改内容

**1. 主标题统一**
```xml
<!-- 现有: 20sp -->
<!-- 保持: 20sp, 添加fontFamily -->
<TextView
    android:textSize="20sp"
    android:fontFamily="monospace"
    android:textStyle="bold"
    android:textColor="@color/text_primary" />
```

**2. 卡片标题统一**
```xml
<!-- 所有卡片标题改为: 16sp -->
android:textSize="16sp"
android:fontFamily="monospace"
android:textStyle="bold"
android:textColor="@color/text_primary"
```

**3. GridLayout标签统一**
```xml
<!-- 标签: 12sp secondary -->
<TextView ... android:textSize="12sp" android:textColor="@color/text_secondary" />

<!-- 数值: 14sp bold primary -->
<TextView ...
    android:textSize="14sp"
    android:textStyle="bold"
    android:textColor="@color/text_primary" />
```

**4. 按钮文本统一**
```xml
<!-- 所有按钮: 14sp bold -->
android:textSize="14sp"
android:textStyle="bold"
android:textColor="@color/primary"
```

### 方案4: 消息提醒系统实现

#### 新建文件: NotificationHelper.kt

```kotlin
object NotificationHelper {
    enum class NotificationLevel {
        INFO, SUCCESS, WARNING, ERROR, CRITICAL
    }

    data class NotificationConfig(
        val enableNotification: Boolean = true,
        val notificationMode: String = "vibration",  // sound/vibration/silent
        val autoDismissTime: Int = 3000,  // 毫秒
        val showDetails: Boolean = true
    )

    fun getNotificationColor(level: NotificationLevel): Int = when(level) {
        NotificationLevel.INFO -> R.color.info
        NotificationLevel.SUCCESS -> R.color.success
        NotificationLevel.WARNING -> R.color.warning
        NotificationLevel.ERROR -> R.color.error
        NotificationLevel.CRITICAL -> R.color.danger_red
    }

    fun showNotification(
        context: Context,
        message: String,
        level: NotificationLevel,
        config: NotificationConfig
    ) {
        // 实现提醒逻辑
    }
}
```

#### 新建布局: notification_view.xml

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="16dp"
    app:cardCornerRadius="8dp"
    android:background="@color/info">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="12dp"
        android:gravity="center_vertical">

        <TextView
            android:id="@+id/tvNotificationIcon"
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:textSize="18sp"
            android:gravity="center"
            android:layout_marginEnd="12dp" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tvNotificationTitle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="@android:color/white" />

            <TextView
                android:id="@+id/tvNotificationMessage"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:textColor="@android:color/white"
                android:alpha="0.9"
                android:layout_marginTop="4dp" />
        </LinearLayout>

        <ImageButton
            android:id="@+id/btnDismiss"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_close"
            android:tint="@android:color/white"
            android:layout_marginStart="12dp" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

---

## 优化时间表

| 阶段 | 任务 | 预计时间 |
|------|------|---------|
| 1 | 创建规范文档 | 30分钟 |
| 2 | 优化packet capture布局 | 20分钟 |
| 3 | 优化packet item布局 | 15分钟 |
| 4 | 优化network diagnostic布局 | 30分钟 |
| 5 | 实现消息提醒系统 | 40分钟 |
| 6 | 编译验证 | 10分钟 |
| 7 | 生成报告 | 20分钟 |
| **总计** | | **165分钟** |

---

## 预期效果

### Before & After

```
❌ 修改前：
- 字体大小混乱: 12sp, 14sp, 16sp, 18sp, 20sp混用
- 配色不统一: 使用多种颜色，缺乏主题感
- 图标emoji随意: 没有设计系统
- 提醒无法配置: 硬编码警告信息

✅ 修改后：
- 统一的字体体系: 明确的大小级别(20/16/14/12/11)
- 一致的配色方案: 遵循Material Design 3.0
- 专业的图标系统: 科学分类，和谐搭配
- 灵活的提醒系统: 可配置的通知级别和样式
```

### 用户体验改进

| 维度 | 改进 |
|------|------|
| 视觉层次 | 更清晰，便于快速定位信息 |
| 品牌一致性 | 与整体应用风格完全统一 |
| 专业度 | 提升整体产品质感 |
| 可用性 | 提醒系统使用户不会错过重要信息 |

---

## 实施步骤

### 步骤1: 修改fonts (可选)
如果需要添加monospace字体，在dimens.xml中定义：

```xml
<dimen name="text_title">20sp</dimen>
<dimen name="text_headline">16sp</dimen>
<dimen name="text_body">14sp</dimen>
<dimen name="text_label">12sp</dimen>
<dimen name="text_caption">11sp</dimen>
```

### 步骤2: 批量更新布局文件
按照上述规范，统一修改：
- activity_packet_capture.xml
- item_packet.xml
- activity_network_diagnostic.xml
- 相关item布局文件

### 步骤3: 实现NotificationHelper
- 新建NotificationHelper.kt
- 新建notification_view.xml
- 集成到PacketCaptureActivity和NetworkDiagnosticActivity

### 步骤4: 编译和验证
- 执行编译测试
- 验证所有界面显示效果
- 检查font family应用

---

## 注意事项

1. **向后兼容性**
   - 所有修改仅涉及UI层，不影响业务逻辑
   - 现有功能完全保留

2. **性能考虑**
   - monospace字体会略增加渲染时间，但可接受
   - 提醒系统采用对象池模式，避免频繁创建

3. **测试覆盖**
   - 需要在各屏幕尺寸上验证
   - 需要验证横屏显示

4. **可维护性**
   - 所有颜色引用colors.xml
   - 所有字体大小遵循统一规范
   - 提醒系统高度可配置

---

**目标：** 让网络抓包功能的界面与整个蓝河助手应用保持完全一致的视觉风格，提升用户体验！
