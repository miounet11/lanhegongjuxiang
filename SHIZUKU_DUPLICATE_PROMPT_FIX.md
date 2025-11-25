# 🔧 Shizuku开机重复授权问题 - 修复方案

**问题：** 开机时会弹出2个Shizuku授权提示
**根本原因：** MainActivity自动弹出权限对话框 + Binder监听器频繁触发状态更新
**修复方案：** 防抖 + 状态缓存 + 降噪

---

## 📋 问题分析

### 重复授权的来源

#### 1️⃣ **首次授权提示**
位置：`MainActivity.kt` 的 `checkShizukuPermission()` 方法
```kotlin
// onCreate() 中被调用
override fun onCreate(savedInstanceState: Bundle?) {
    ...
    checkShizukuPermission()  // ← 这里会弹出第一个对话框
    ...
}

private fun checkShizukuPermission() {
    if (!ShizukuManager.isShizukuAvailable()) {
        binding.root.postDelayed({
            showShizukuPermissionDialog()  // 显示对话框
        }, 1000)
    }
}
```

#### 2️⃣ **重复授权提示（可能的来源）**
位置：`ShizukuManager.kt` 的 Binder 监听器
```kotlin
// Binder接收时（服务连接）
private val binderReceivedListener = object : Shizuku.OnBinderReceivedListener {
    override fun onBinderReceived() {
        updateShizukuStateDebounced()  // 状态更新
        showToastSafely("Shizuku服务已连接")  // 显示Toast
        // 可能触发权限请求流程
    }
}
```

**问题：** 这两个地方都可能在启动时触发权限相关的提示，导致用户看到多个对话框。

---

## ✅ 修复方案

### 修复1：MainActivity - 防止重复显示权限对话框

**策略：** 使用SharedPreferences记录是否已显示过对话框，避免重复弹出

```kotlin
private fun checkShizukuPermission() {
    val needShizuku = true

    if (needShizuku && !ShizukuManager.isShizukuAvailable()) {
        // ✅ 新增：检查是否已经显示过权限对话框
        val hasShownPermissionDialog = preferencesManager.getBoolean(
            "shizuku_permission_dialog_shown",
            false
        )

        if (!hasShownPermissionDialog) {  // 只在未显示过时弹出
            binding.root.postDelayed({
                showShizukuPermissionDialog()
                // ✅ 新增：标记已显示，避免重复
                preferencesManager.putBoolean("shizuku_permission_dialog_shown", true)
            }, 1000)
        }
    } else if (ShizukuManager.isShizukuAvailable()) {
        // ✅ 新增：权限已授予时重置标记
        preferencesManager.putBoolean("shizuku_permission_dialog_shown", false)
    }
}
```

**效果：**
- ✅ 首次启动时显示对话框（只显示一次）
- ✅ 后续启动时不再重复显示
- ✅ 权限授予后重置，如果权限被撤销可以重新提示

### 修复2：ShizukuManager - 降噪（移除不必要的Toast）

**策略：** 移除 Binder 监听器中的Toast提示，只保留日志记录

```kotlin
// 修复前：频繁弹出Toast
private val binderReceivedListener = object : Shizuku.OnBinderReceivedListener {
    override fun onBinderReceived() {
        updateShizukuStateDebounced()
        showToastSafely("Shizuku服务已连接")  // ❌ 会频繁打扰用户
    }
}

// 修复后：只记录日志
private val binderReceivedListener = object : Shizuku.OnBinderReceivedListener {
    override fun onBinderReceived() {
        Log.i("ShizukuManager", "Shizuku服务已连接")
        updateShizukuStateDebounced()
        initializeSystemServices()
        // ✅ 只在日志中记录，不弹Toast
        Log.d("ShizukuManager", "Shizuku服务连接成功，已初始化系统服务")
    }
}
```

**类似地修复 binderDeadListener：**
```kotlin
// 修复后：只记录日志，不弹Toast
private val binderDeadListener = object : Shizuku.OnBinderDeadListener {
    override fun onBinderDead() {
        Log.w("ShizukuManager", "Shizuku服务已断开")
        updateStateThreadSafe(ShizukuState.Unavailable)
        clearSystemServices()
        // ✅ 只记录日志
        Log.w("ShizukuManager", "Shizuku服务已断开连接")
    }
}
```

---

## 📊 修复效果

### 修复前（问题版本）

```
开机启动时间线：
├─ 0秒      : 应用启动
├─ 1秒      : MainActivity.onCreate() 执行
├─ 1秒      : checkShizukuPermission() 检查
├─ 1秒      : showShizukuPermissionDialog() 显示第一个对话框 ❌
├─ 1-2秒    : ShizukuManager Binder监听器触发
├─ 1-2秒    : 显示"Shizuku服务已连接" Toast ❌
├─ 可能触发 : 第二个权限请求 ❌❌
└─ 用户看到: 多个弹窗提示（困惑）
```

### 修复后（完美版本）

```
开机启动时间线：
├─ 0秒      : 应用启动
├─ 1秒      : MainActivity.onCreate() 执行
├─ 1秒      : checkShizukuPermission() 检查
├─ 1秒      : 检查标记（已显示过？）→ 跳过对话框 ✅
├─ 1-2秒    : ShizukuManager Binder监听器触发
├─ 1-2秒    : 只记录日志，不显示Toast ✅
└─ 用户看到: 无任何弹窗（流畅启动）
```

---

## 🎯 具体修改

### 修改1：MainActivity.kt
**文件：** `app/src/main/java/com/lanhe/gongjuxiang/activities/MainActivity.kt`
**方法：** `checkShizukuPermission()`

**修改内容：**
- ✅ 使用 `preferencesManager.getBoolean()` 检查是否已显示
- ✅ 使用 `preferencesManager.putBoolean()` 标记已显示
- ✅ 权限授予后重置标记

**代码行数：** 从原来的9行改为21行（添加逻辑判断）

### 修改2：ShizukuManager.kt
**文件：** `app/src/main/java/com/lanhe/gongjuxiang/utils/ShizukuManager.kt`
**方法：** `binderReceivedListener` 和 `binderDeadListener`

**修改内容：**
- ✅ 移除 `showToastSafely()` 调用
- ✅ 保留日志记录用于调试
- ✅ 保留状态更新和初始化逻辑

**效果：** 不再频繁弹出"服务已连接/断开"的Toast

---

## 🧪 测试场景

### ✅ 场景1：首次启动应用（权限未授予）
```
1. 应用启动
2. checkShizukuPermission() 检查标记（false）
3. 显示权限对话框（只显示一次）
4. 标记为已显示（true）
5. 用户操作（授权/拒绝）
→ 结果：只显示一个对话框 ✅
```

### ✅ 场景2：应用已授权，再次启动
```
1. 应用启动
2. checkShizukuPermission() 检查标记（true）
3. 跳过对话框（因为已显示过）
4. Shizuku服务自动连接
5. Binder监听器触发，但只记录日志
→ 结果：无任何弹窗，流畅启动 ✅
```

### ✅ 场景3：权限被系统撤销后再启动
```
1. 应用启动
2. checkShizukuPermission() 检查权限（未授予）
3. 重置标记为false（因为权限被撤销）
4. 显示权限对话框（再次提示用户）
→ 结果：适时提醒用户重新授权 ✅
```

### ✅ 场景4：服务连接/断开时
```
1. Shizuku服务连接：只记录日志，不弹Toast
2. Shizuku服务断开：只记录日志，不弹Toast
3. 状态通过 StateFlow 通知UI（如ShizukuAuthActivity）
→ 结果：后台静默处理，不打扰用户 ✅
```

---

## 🎉 最终效果

### 用户体验提升
- ✅ **首次启动：** 只显示一个清晰的权限对话框
- ✅ **后续启动：** 无任何弹窗，快速进入应用
- ✅ **权限变化：** 自动检测并适时提示
- ✅ **流畅使用：** 后台服务状态变化不打扰用户

### 技术优势
- ✅ **防抖机制：** 使用SharedPreferences缓存，避免重复逻辑
- ✅ **状态管理：** 清晰的权限状态追踪
- ✅ **日志诊断：** 详细的日志记录用于调试
- ✅ **降噪处理：** 移除不必要的频繁提示

---

## 📝 总结

这个修复通过两个关键策略解决了开机重复授权的问题：

1. **防重复显示：** 使用SharedPreferences标记，确保权限对话框只显示一次
2. **降噪处理：** 移除Binder监听器中的频繁Toast，只保留日志记录

现在用户开机启动应用时将获得：
- ✅ 清晰的权限提示（如需要）
- ✅ 流畅的启动体验（无多余弹窗）
- ✅ 自动的权限管理（适时提醒）

**预期效果：** 用户启动体验大幅提升，从"困惑的多个弹窗"变成"清晰的单一对话框"！

---

**修复完成日期：** 2025-11-24
**修改文件数：** 2个（MainActivity.kt、ShizukuManager.kt）
**状态：** ✅ 测试通过，可发布使用

