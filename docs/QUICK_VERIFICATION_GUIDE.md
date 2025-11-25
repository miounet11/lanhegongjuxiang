# 蓝河助手项目快速验证指南
Version: 1.0.0 | 生成日期: 2025-11-24

## 一、快速编译验证（5分钟）

### Step 1: 清理并编译项目
```bash
# 执行完整编译
./gradlew clean build

# 预期输出
BUILD SUCCESSFUL in XXs
```

✅ **成功标志**：看到 "BUILD SUCCESSFUL"
❌ **失败处理**：查看错误信息，通常是依赖或语法问题

---

## 二、核心单元测试（10分钟）

### Step 2: 运行所有单元测试
```bash
# 运行测试套件
./gradlew test

# 查看测试报告
open app/build/reports/tests/testDebugUnitTest/index.html
```

### 预期测试结果
| 测试类 | 测试数量 | 状态 |
|-------|---------|------|
| ShizukuManagerTest | 15 | ✅ PASS |
| PermissionHelperTest | 14 | ✅ PASS |
| PerformanceMonitorTest | 15 | ✅ PASS |
| CommandValidatorTest | 15 | ✅ PASS |
| BatteryOptimizerTest | 15 | ✅ PASS |
| MemoryManagerTest | 14 | ✅ PASS |
| ServiceLifecycleTest | 15 | ✅ PASS |

✅ **成功标志**：所有测试通过，覆盖率 > 60%
❌ **失败处理**：查看具体失败的测试用例

---

## 三、APK构建验证（5分钟）

### Step 3: 构建Debug APK
```bash
# 构建Debug版本
./gradlew assembleDebug

# 检查APK
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

### Step 4: 安装测试
```bash
# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n com.lanhe.gongjuxiang.debug/.activities.MainActivity
```

✅ **成功标志**：应用成功启动，显示主界面
❌ **失败处理**：检查设备连接和权限配置

---

## 四、权限流程快速验证（5分钟）

### Step 5: 权限请求测试
1. **清除应用数据**
```bash
adb shell pm clear com.lanhe.gongjuxiang.debug
```

2. **重新启动应用**
```bash
adb shell am start -n com.lanhe.gongjuxiang.debug/.activities.MainActivity
```

3. **验证点**
- [ ] 显示权限请求对话框
- [ ] 可以同意或拒绝权限
- [ ] 拒绝后应用不崩溃
- [ ] 显示权限提示信息

✅ **成功标志**：权限流程完整，无崩溃
❌ **失败处理**：检查PermissionHelper实现

---

## 五、Shizuku集成验证（10分钟）

### Step 6: Shizuku状态检查

#### 场景1：Shizuku未安装
```bash
# 确保Shizuku未安装
adb uninstall moe.shizuku.privileged.api 2>/dev/null

# 启动应用并进入设置
# UI操作：点击"安全中心"或"设置"
```
**预期**：显示"Shizuku未安装"提示

#### 场景2：Shizuku已安装未授权
```bash
# 安装Shizuku（需要下载Shizuku APK）
# 不启动Shizuku服务
```
**预期**：显示"Shizuku服务未运行"

#### 场景3：Shizuku正常工作
```bash
# 启动Shizuku服务（通过Shizuku应用）
# 在蓝河助手中请求权限
```
**预期**：可以使用系统级功能

✅ **成功标志**：三种状态都正确处理
❌ **失败处理**：检查ShizukuManager状态管理

---

## 六、性能数据验证（5分钟）

### Step 7: 真实数据检查
1. **打开性能监控页面**
2. **观察数据变化**
   - CPU使用率：应该在0-100%之间变化
   - 内存使用：应该显示实际使用量
   - 电池电量：应该匹配系统显示
   - 温度：应该在20-45°C之间

```bash
# 验证CPU数据
adb shell dumpsys cpuinfo | grep com.lanhe.gongjuxiang

# 验证内存数据
adb shell dumpsys meminfo com.lanhe.gongjuxiang.debug | grep TOTAL
```

✅ **成功标志**：数据实时变化，非占位符
❌ **失败处理**：检查RealPerformanceMonitorManager

---

## 七、稳定性快速测试（10分钟）

### Step 8: Monkey测试
```bash
# 运行Monkey测试（1000个事件）
adb shell monkey -p com.lanhe.gongjuxiang.debug -v 1000

# 检查是否崩溃
adb logcat -d | grep "FATAL EXCEPTION"
```

✅ **成功标志**：无崩溃，无ANR
❌ **失败处理**：分析崩溃日志

---

## 八、性能基准检查（5分钟）

### Step 9: 启动时间测试
```bash
# 冷启动时间
adb shell am force-stop com.lanhe.gongjuxiang.debug
adb shell am start -W -n com.lanhe.gongjuxiang.debug/.activities.MainActivity

# 查看TotalTime
```

### Step 10: 内存使用检查
```bash
# 内存占用
adb shell dumpsys meminfo com.lanhe.gongjuxiang.debug | grep "TOTAL"
```

### 性能指标参考
| 指标 | 目标值 | 可接受范围 |
|------|--------|-----------|
| 冷启动时间 | < 500ms | < 2000ms |
| 内存占用 | < 100MB | < 150MB |
| CPU占用(待机) | < 1% | < 3% |

✅ **成功标志**：性能指标在可接受范围内
❌ **失败处理**：进行性能优化

---

## 九、安全验证（5分钟）

### Step 11: 命令注入测试
在任何可输入命令的地方尝试：
- `; rm -rf /`
- `&& cat /etc/passwd`
- `| ls -la`

✅ **成功标志**：所有恶意命令被拒绝
❌ **失败处理**：加强CommandValidator

---

## 十、最终检查清单

### 必须通过项（P0级）
- [ ] ✅ 项目成功编译
- [ ] ✅ 单元测试全部通过
- [ ] ✅ APK可以安装运行
- [ ] ✅ 权限请求不崩溃
- [ ] ✅ Shizuku状态正确显示
- [ ] ✅ 无命令注入漏洞

### 建议通过项（P1级）
- [ ] ✅ 性能数据真实
- [ ] ✅ Monkey测试无崩溃
- [ ] ✅ 启动时间 < 2秒
- [ ] ✅ 内存占用 < 150MB

### 优化建议项（P2级）
- [ ] 启动时间 < 500ms
- [ ] 内存占用 < 100MB
- [ ] 测试覆盖率 > 80%
- [ ] 0个Lint Error

---

## 快速问题诊断

### 问题1：编译失败
```bash
# 清理缓存重试
./gradlew clean
rm -rf ~/.gradle/caches/
./gradlew build --refresh-dependencies
```

### 问题2：测试失败
```bash
# 单独运行失败的测试
./gradlew :app:testDebugUnitTest --tests "失败的测试类名"
```

### 问题3：APK安装失败
```bash
# 检查设备连接
adb devices

# 卸载旧版本
adb uninstall com.lanhe.gongjuxiang.debug

# 重新安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 问题4：权限相关崩溃
```bash
# 查看崩溃日志
adb logcat -d | grep -E "SecurityException|Permission"
```

### 问题5：Shizuku不工作
```bash
# 检查Shizuku服务状态
adb shell dumpsys activity services | grep Shizuku
```

---

## 一键验证脚本

创建 `verify.sh` 文件：
```bash
#!/bin/bash
echo "=== 蓝河助手快速验证脚本 ==="

# 1. 编译
echo "Step 1: 编译项目..."
./gradlew clean build
if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi
echo "✅ 编译成功"

# 2. 测试
echo "Step 2: 运行单元测试..."
./gradlew test
if [ $? -ne 0 ]; then
    echo "❌ 测试失败"
    exit 1
fi
echo "✅ 测试通过"

# 3. 构建APK
echo "Step 3: 构建APK..."
./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo "❌ APK构建失败"
    exit 1
fi
echo "✅ APK构建成功"

# 4. 安装测试
echo "Step 4: 安装APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk
if [ $? -ne 0 ]; then
    echo "❌ APK安装失败"
    exit 1
fi
echo "✅ APK安装成功"

# 5. 启动应用
echo "Step 5: 启动应用..."
adb shell am start -n com.lanhe.gongjuxiang.debug/.activities.MainActivity
if [ $? -ne 0 ]; then
    echo "❌ 应用启动失败"
    exit 1
fi
echo "✅ 应用启动成功"

echo "=== 验证完成 ==="
echo "✅ 所有基础测试通过"
```

运行脚本：
```bash
chmod +x verify.sh
./verify.sh
```

---

## 验证结果记录

| 项目 | 状态 | 时间 | 备注 |
|------|------|------|------|
| 编译验证 | ⏳ 待测 | - | - |
| 单元测试 | ⏳ 待测 | - | - |
| APK构建 | ⏳ 待测 | - | - |
| 权限流程 | ⏳ 待测 | - | - |
| Shizuku集成 | ⏳ 待测 | - | - |
| 性能数据 | ⏳ 待测 | - | - |
| 稳定性测试 | ⏳ 待测 | - | - |
| 性能基准 | ⏳ 待测 | - | - |
| 安全验证 | ⏳ 待测 | - | - |

**总体评估**：⏳ 待评估

---

**说明**：
- ✅ = 通过
- ❌ = 失败
- ⏳ = 待测试
- ⚠️ = 有问题但可接受

本指南提供快速验证路径，完整测试请参考 [VERIFICATION_TEST_CHECKLIST.md](./VERIFICATION_TEST_CHECKLIST.md)