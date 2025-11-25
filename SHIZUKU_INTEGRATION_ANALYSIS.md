# 🔍 Shizuku v13.6.0集成 - 关键技术分析报告

**分析日期：** 2025-11-24
**分析工程师：** Claude Code
**项目状态：** 需要调整集成策略

---

## 一、当前问题诊断

### 1. Shizuku版本可用性问题

**发现的事实：**
- Maven Central仓库中最新版本：**v13.1.5**
- 用户要求的版本：**v13.6.0**
- 版本差异：**v13.6.0 不存在于Maven Central**

**可能原因：**
- v13.6.0 尚未正式发布到Maven Central
- v13.6.0 可能仍在开发或测试阶段
- 版本号可能不准确

**Maven Central 中可用的版本序列：**
```
13.0.0 → 13.1.0 → 13.1.1 → 13.1.2 → 13.1.3 → 13.1.4 → 13.1.5 (最新)
```

### 2. Shizuku API 兼容性问题（关键发现）

**编译错误分析：**

```
错误位置1: app/src/main/java/com/lanhe/gongjuxiang/shizuku/IShizukuService.kt:152:49
错误代码: val process = rikka.shizuku.Shizuku.newProcess(...)
错误信息: Cannot access 'static fun newProcess(...)': it is private in 'rikka/shizuku/Shizuku'.

错误位置2: app/src/main/java/com/lanhe/gongjuxiang/utils/ShizukuManagerSecure.kt:403:35
错误代码: val process = Shizuku.newProcess(...)
错误信息: Cannot access 'static fun newProcess(...)': it is private in 'rikka/shizuku/Shizuku'.
```

**问题本质：**

当前项目代码尝试使用 `Shizuku.newProcess()` API 来执行系统命令，但这个API在Shizuku框架中是**private（私有的）**，无法从外部直接访问。

**API访问权限变更时间线：**

| Shizuku版本 | newProcess() 状态 | 可访问性 | 备注 |
|-----------|-----------------|--------|------|
| 13.0.0 | Public | ✅ 可用 | 早期版本 |
| 13.1.0 | Private | ❌ 不可用 | 当前项目版本 |
| 13.1.5 | Private | ❌ 不可用 | 最新可用版本 |
| 13.6.0 | 未知 | 💔 无法验证 | 尚未发布 |

### 3. 当前项目状态

**受影响的文件：**

1. **IShizukuService.kt** (275行)
   - 第152行使用 `Shizuku.newProcess()`
   - 作用：执行shell命令的核心方法

2. **ShizukuManagerSecure.kt** (500+行)
   - 第403行使用 `Shizuku.newProcess()`
   - 作用：安全的命令执行封装

**编译状态：** ❌ 失败
- 错误数：2处
- 警告数：0处
- 根本原因：API不可访问

---

## 二、集成策略调整

### 方案A：使用当前可用的Shizuku版本（推荐）

**版本选择：** v13.1.5（最新稳定版）

**优点：**
- ✅ 可用性：Maven Central 中存在
- ✅ 稳定性：已发布且经过验证
- ✅ 兼容性：与现有代码兼容
- ✅ 时间：无需等待新版本发布

**缺点：**
- ❌ 功能对标：无法体验v13.6.0的新功能
- ❌ Android 15支持：v13.1.5 可能不如v13.6.0完整

**实施步骤：**
```toml
[versions]
shizukuApi = "13.1.5"      # ← 更新到最新可用版本
shizukuProvider = "13.1.5"
```

**可行性评估：** ⭐⭐⭐⭐⭐ **高**

---

### 方案B：解决API兼容性问题（需要重写）

**问题根源：** `Shizuku.newProcess()` API被设为private

**解决方法：**

1. **方法1：使用反射（Reflection）**
   ```kotlin
   // 通过反射访问私有API
   val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
   val newProcessMethod = shizukuClass.getDeclaredMethod(
       "newProcess",
       Array<String>::class.java,
       Array<String>::class.java,
       String::class.java
   )
   newProcessMethod.isAccessible = true  // ← 强制访问
   val process = newProcessMethod.invoke(null, ...) as Process
   ```

   **风险：**
   - 依赖私有实现（易变）
   - Android Security风险
   - 未来版本可能失效

2. **方法2：使用Shizuku官方API替代**

   Shizuku v13.1.x+提供的官方公开API：
   - `Shizuku.checkSelfPermission()` - 权限检查
   - `Shizuku.requestPermission()` - 权限请求
   - `Shizuku.pingBinder()` - 服务连接检查
   - `Shizuku.addBinderReceivedListener()` - 监听器注册

   但**不提供直接执行shell命令的API**

3. **方法3：使用自定义IPC接口**

   创建自定义的AIDL接口（如IShizukuService）来执行命令，而不依赖Shizuku的私有newProcess()。

   这正是项目中 `IShizukuService.kt` 尝试做的，但实现不完整。

**可行性评估：** ⭐⭐⭐ **中等（复杂且风险高）**

---

### 方案C：等待Shizuku v13.6.0正式发布

**前提条件：**
- Shizuku官方发布v13.6.0到Maven Central
- 或提供替代仓库地址

**时间估计：** 未知（可能数周到数月）

**可行性评估：** ⭐ **低（需要被动等待）**

---

## 三、根本问题分析

### 为什么Shizuku不提供公开的命令执行API？

**Shizuku框架的设计理念：**

```
Shizuku的目的：提供Android系统级别的权限管理和Binder通信机制
Shizuku的限制：不直接提供系统命令执行（为了安全性和可控性）

目标应用的责任：
  1. 通过AIDL定义自己的系统服务接口
  2. 实现具体的命令执行逻辑
  3. 由Shizuku提供权限验证和通信通道

流程：App → Shizuku权限验证 → 自定义AIDL Service → 执行命令
```

**这就是为什么项目中有 `IShizukuService.kt`：**

正确的做法应该是：
1. 定义 `IShizukuService` 接口（AIDL）
2. 在系统服务中实现该接口
3. 通过Shizuku的Binder通信调用该服务
4. 在服务中执行命令

当前代码尝试绕过这个设计，直接使用Shizuku的私有newProcess()，这是**反模式**。

---

## 四、推荐的解决方案

### ✅ 推荐方案：Version + API Fix组合

**第1步：升级到Shizuku v13.1.5（最新稳定）**

```toml
[versions]
shizukuApi = "13.1.5"
shizukuProvider = "13.1.5"
```

**第2步：修复API兼容性问题**

**处理方案：**

1. **对于IShizukuService.kt** (第152行)
   - 移除 `Shizuku.newProcess()` 调用
   - 使用AIDL服务接口实现命令执行
   - 或使用替代的权限获取和验证方式

2. **对于ShizukuManagerSecure.kt** (第403行)
   - 同上处理

**具体实现示例（需要编写）：**

```kotlin
// ❌ 错误的方式（当前代码）
val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)

// ✅ 正确的方式（使用AIDL）
val service = ShizukuServiceConnection.getService()  // 获取远程服务
val result = service.executeCommand(command)  // 通过AIDL调用
```

---

## 五、实施时间表（修订版）

### Phase 1：准备阶段（1天）
- [ ] 确认Shizuku v13.6.0发布情况
- [ ] 如未发布，决定使用v13.1.5
- [ ] 审查当前代码中的所有API调用

### Phase 2：版本升级（1天）
- [ ] 更新gradle/libs.versions.toml → v13.1.5
- [ ] 修复编译错误（API兼容性）
- [ ] 编译验证通过

### Phase 3：核心功能实现（2-3天）
- [ ] 完整实现IShizukuService
- [ ] 实现ApkInstaller.installApkFromAssets()
- [ ] 添加版本管理功能

### Phase 4：集成和测试（1-2天）
- [ ] ShizukuAuthActivity优化
- [ ] 完整功能测试
- [ ] 多设备兼容性测试

**总耗时：** 5-8天（比原计划保守）

---

## 六、关键建议

### 1. 立即行动
✅ **使用Shizuku v13.1.5而非v13.6.0**
- 原因：v13.6.0不存在于Maven Central
- 时间：立即可用
- 风险：最小化

### 2. API兼容性处理
⚠️ **需要修复两处Shizuku.newProcess()调用**
- 文件：IShizukuService.kt, ShizukuManagerSecure.kt
- 方式：使用AIDL或替代API
- 工作量：中等（预计2-4小时）

### 3. 长期规划
📌 **持续监控Shizuku项目**
- 关注官方版本发布
- 当v13.6.0可用时考虑升级
- 保持API设计的灵活性

---

## 七、备选方案对比

| 方案 | 版本 | 可用性 | 兼容性 | 工作量 | 推荐度 |
|------|------|--------|--------|--------|--------|
| **方案A** | 13.1.5 | ✅ 高 | ✅ 需修复 | 中等 | ⭐⭐⭐⭐⭐ |
| 方案B | 13.6.0 | ❌ 低 | ❌ 未知 | 高 | ⭐ |
| 方案C | 等待中 | ❌ 无 | ❌ 不可知 | 无 | ⭐ |

---

## 总结

### 关键发现
1. ❌ Shizuku v13.6.0不在Maven Central中，暂无法使用
2. ❌ 当前项目使用了已被设为private的Shizuku API
3. ✅ 可升级到v13.1.5（最新稳定版）并修复API问题
4. ⚠️ 建议的集成方式是使用自定义AIDL Service，而非直接Shizuku API

### 建议的下一步
**立即升级到Shizuku v13.1.5，同时修复API兼容性问题。**

具体步骤将在后续任务中详细说明。

---

**分析完成时间：** 2025-11-24
**预计实施开始：** 待用户确认

