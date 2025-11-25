# ProGuard 混淆配置使用指南

## 配置概述

蓝河助手已配置完整的生产级ProGuard混淆规则，文件位于 `/app/proguard-rules.pro`。

## 配置状态

✅ **已完成配置：**
- 基础优化配置（5次优化通过）
- Android核心类保护（四大组件、Fragment、View）
- Shizuku框架完整保护
- Room数据库保护
- Kotlin协程保护
- JSON序列化保护（Gson）
- Native方法和反射保护
- 第三方库配置（30+库）
- 项目模块保护
- 生产优化（移除调试日志）

## 构建配置

### Release构建已启用混淆

```kotlin
// app/build.gradle.kts
release {
    isMinifyEnabled = true      // 代码混淆和优化
    isShrinkResources = true    // 资源压缩
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

### Debug构建（不混淆）

```kotlin
debug {
    isMinifyEnabled = false     // 便于调试
    isDebuggable = true
}
```

## 构建命令

```bash
# 构建混淆的Release APK
./gradlew assembleRelease

# 构建未混淆的Debug APK
./gradlew assembleDebug

# 清理并构建Release
./gradlew clean assembleRelease
```

## 生成的文件

构建Release版本后，会在以下位置生成重要文件：

```
app/build/outputs/
├── apk/release/
│   └── app-release.apk          # 混淆后的APK
├── mapping/release/
│   ├── mapping.txt               # 混淆映射文件（重要！）
│   ├── seeds.txt                 # 被keep的类和成员
│   └── usage.txt                 # 被移除的代码
└── logs/
    └── manifest-merger-release-report.txt
```

## 重要文件说明

### 1. mapping.txt（混淆映射文件）

**极其重要！** 必须保存此文件用于：
- 解析崩溃日志的堆栈跟踪
- 还原混淆后的类名和方法名
- Firebase Crashlytics需要此文件

保存位置：`app/build/outputs/mapping/release/mapping.txt`

### 2. seeds.txt

列出所有被`-keep`规则保留的类和成员，用于验证混淆规则是否正确。

### 3. usage.txt

列出所有被移除的代码，可用于检查是否误删了必要代码。

## 崩溃日志解析

使用`retrace`工具解析混淆后的堆栈跟踪：

```bash
# 使用Android SDK的retrace工具
$ANDROID_HOME/cmdline-tools/latest/bin/retrace mapping.txt stacktrace.txt

# 或使用ProGuard的retrace
java -jar retrace.jar mapping.txt stacktrace.txt
```

## 测试流程

### 1. 构建前检查

```bash
# 检查ProGuard配置语法
./gradlew assembleRelease --dry-run
```

### 2. 构建Release版本

```bash
./gradlew clean assembleRelease
```

### 3. 功能测试清单

混淆后必须测试的核心功能：

- [ ] **Shizuku权限**：请求和授权流程
- [ ] **系统优化**：所有优化功能正常工作
- [ ] **数据库操作**：Room增删改查
- [ ] **网络请求**：Retrofit API调用
- [ ] **WebView**：浏览器功能和JavaScript交互
- [ ] **文件管理**：文件浏览和操作
- [ ] **性能监控**：实时性能数据显示
- [ ] **模块通信**：18个模块间的调用

### 4. 性能对比

比较混淆前后的APK：

```bash
# 查看APK大小
ls -lh app/build/outputs/apk/*/app-*.apk

# 使用APK Analyzer分析
# Android Studio: Build > Analyze APK
```

## 常见问题

### Q1: ClassNotFoundException

**原因**：某些类被错误混淆或移除
**解决**：在proguard-rules.pro中添加keep规则

```proguard
-keep class com.example.MyClass { *; }
```

### Q2: NoSuchMethodError

**原因**：反射调用的方法被混淆
**解决**：保留相关方法

```proguard
-keepclassmembers class com.example.MyClass {
    public void myMethod(...);
}
```

### Q3: Shizuku功能失效

**原因**：Shizuku回调接口被混淆
**解决**：已在配置中完整保护Shizuku框架

### Q4: Room数据库崩溃

**原因**：Entity或DAO被混淆
**解决**：已配置Room注解保护规则

### Q5: 构建速度慢

**原因**：5次优化通过需要时间
**解决**：开发时使用Debug版本，仅发布时使用Release

## 优化建议

### 1. APK大小优化

当前配置已启用：
- 代码混淆（减少30-40%）
- 资源压缩（减少10-20%）
- 优化通过5次

### 2. 进一步优化

可考虑：
- 启用R8（已默认使用）
- 移除未使用的备用资源
- 使用WebP替代PNG/JPG
- 分割APK（按ABI）

### 3. 安全性增强

- 定期更新混淆规则
- 使用字符串加密
- 敏感逻辑使用Native代码
- 启用证书固定

## 维护建议

1. **版本控制**：将mapping.txt纳入版本控制或备份系统
2. **CI/CD集成**：自动上传mapping.txt到崩溃报告服务
3. **定期审查**：检查usage.txt确保没有误删代码
4. **规则更新**：第三方库升级时更新混淆规则
5. **测试覆盖**：每次发布前完整测试混淆版本

## 混淆效果预期

- **APK大小**：减少30-50%
- **启动速度**：略有提升（优化后的代码）
- **逆向难度**：大幅提升
- **崩溃报告**：需要mapping.txt才能解析

## 联系支持

如遇到混淆相关问题：
1. 检查`usage.txt`和`seeds.txt`
2. 查看构建日志中的警告信息
3. 使用`-printconfiguration`输出完整配置
4. 临时禁用混淆定位问题

---

**最后更新：2025-11-24**
**版本：1.0.0**