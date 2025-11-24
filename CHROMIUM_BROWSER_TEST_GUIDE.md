# Chromium 浏览器集成测试指南

## 编译和部署

### 1. 编译项目（已完成）✅
```bash
./gradlew clean build -x test -x lint
# 结果: BUILD SUCCESSFUL in 1s
```

### 2. 生成的 APK
```
路径: app/build/outputs/apk/debug/app-debug.apk
大小: 80M
生成时间: 2025-11-24
```

### 3. 安装到设备
```bash
# 连接 Android 设备或启动模拟器
./gradlew installDebug

# 或使用 adb
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 功能测试

### 测试场景 1: 启动 Chromium 浏览器

**测试步骤**:
1. 启动蓝河助手应用
2. 进入"功能"标签页
3. 点击"智能浏览器"或"浏览器"按钮
4. 验证 ChromiumBrowserActivity 启动

**期望结果**:
- ✅ Activity 成功启动
- ✅ 工具栏显示 "Chromium浏览器"
- ✅ 浏览器导航栏加载完成
- ✅ 默认页面为百度首页（https://www.baidu.com）

**实际结果**: [待测试]

---

### 测试场景 2: 导航功能

#### 2.1 返回按钮
```
操作: 访问第一个页面 → 访问第二个页面 → 点击返回按钮
期望: 回到第一个页面
```

#### 2.2 前进按钮
```
操作: 点击返回 → 点击前进按钮
期望: 回到第二个页面
```

#### 2.3 刷新按钮
```
操作: 点击刷新按钮
期望: 当前页面重新加载
```

**实际结果**: [待测试]

---

### 测试场景 3: URL 输入和搜索

#### 3.1 完整 URL
```
输入: https://www.google.com
期望: 加载 Google 主页
```

#### 3.2 不完整 URL（自动补全）
```
输入: baidu.com
期望: 自动补全为 https://baidu.com
```

#### 3.3 搜索关键词
```
输入: Android 开发教程
期望: 使用百度搜索 "Android 开发教程"
```

**实际结果**: [待测试]

---

### 测试场景 4: 页面加载

#### 4.1 进度条显示
```
操作: 加载任何页面
期望:
- 加载开始时显示进度条
- 进度条显示加载百分比
- 加载完成时隐藏进度条
```

#### 4.2 页面标题更新
```
操作: 加载任何页面
期望: 工具栏 subtitle 显示页面标题
```

#### 4.3 地址栏更新
```
操作: 加载任何页面
期望: 地址栏显示最终加载的 URL
```

**实际结果**: [待测试]

---

### 测试场景 5: 菜单功能

#### 5.1 清除缓存
```
操作: 点击菜单 → 选择 "清除缓存"
期望:
- 显示 Toast: "缓存已清除"
- WebView 缓存被清空
```

#### 5.2 清除历史
```
操作: 点击菜单 → 选择 "清除历史记录"
期望:
- 显示 Toast: "历史记录已清除"
- WebView 历史被清空
```

**实际结果**: [待测试]

---

### 测试场景 6: 集成点

#### 6.1 Settings 中启动浏览器
```
操作:
- 进入"设置"标签页
- 点击 "浏览器设置" 卡片
期望: 启动 ChromiumBrowserActivity
```

#### 6.2 FunctionsFragment 中启动浏览器
```
操作:
- 进入"功能"标签页
- 点击智能浏览器功能
期望: 启动 ChromiumBrowserActivity
```

#### 6.3 MainActivity 菜单启动
```
操作:
- 点击菜单
- 选择 "打开浏览器" （如果存在）
期望: 启动 ChromiumBrowserActivity
```

**实际结果**: [待测试]

---

### 测试场景 7: 页面交互

#### 7.1 链接点击
```
操作: 在加载的网页中点击链接
期望: 导航到新页面，返回按钮启用
```

#### 7.2 表单输入
```
操作: 在网页表单中输入数据
期望: 正常输入，软键盘显示
```

#### 7.3 按钮点击
```
操作: 点击网页中的按钮（如搜索按钮）
期望: 执行相应的页面操作
```

**实际结果**: [待测试]

---

### 测试场景 8: 文件下载

#### 8.1 下载对话框
```
操作: 在网页上下载文件
期望: 显示 Toast: "下载已开始: [URL]"
```

**实际结果**: [待测试]

---

### 测试场景 9: 内存和性能

#### 9.1 长时间使用
```
操作: 持续浏览多个网页 5+ 分钟
期望:
- 应用不崩溃
- 导航响应时间 < 200ms
- 内存占用 < 150MB
```

#### 9.2 背景恢复
```
操作:
- 打开浏览器
- 加载网页
- 按 Home 键
- 点击应用重新启动
期望:
- 应用恢复时页面继续加载或显示最后页面
- 不丢失浏览历史
```

**实际结果**: [待测试]

---

### 测试场景 10: 返回和退出

#### 10.1 物理返回键
```
操作:
- 访问多个页面
- 按物理返回键多次
期望:
- 第一次返回导航到上一页面
- 最后一次返回关闭 Activity
```

#### 10.2 返回按钮
```
操作:
- 访问单个页面
- 点击返回按钮
期望: 显示 Toast: "已经是第一页"
```

**实际结果**: [待测试]

---

## 错误处理测试

### 测试场景 11: 无效 URL
```
操作: 输入无效 URL 如 "这不是网址"
期望: 自动转换为百度搜索
```

### 测试场景 12: 网络错误
```
操作:
- 断开网络连接
- 尝试加载页面
期望:
- WebView 显示错误页面（来自浏览器引擎）
- 应用不崩溃
```

### 测试场景 13: 加载超时
```
操作: 加载不可达的域名（如 http://notexistdomain123456.com）
期望:
- 等待超时
- 显示错误信息
- 应用响应正常
```

**实际结果**: [待测试]

---

## 集成验证

### 导入验证
```kotlin
// ✅ 所有必需的导入应该存在
import com.lanhe.gongjuxiang.activities.ChromiumBrowserActivity
import android.webkit.WebView
import android.webkit.WebSettings
```

### 布局验证
```xml
<!-- ✅ 所有 UI 元素应该正确引用 -->
- @+id/toolbar
- @+id/btn_back
- @+id/btn_forward
- @+id/btn_refresh
- @+id/address_bar
- @+id/btn_account
- @+id/btn_menu
- @+id/progress_bar
- @+id/webView
- @+id/status_text
```

### Manifest 验证
```xml
<!-- ✅ Activity 应该在 Manifest 中注册 -->
<activity android:name=".activities.ChromiumBrowserActivity"
    android:label="Chromium浏览器"
    android:configChanges="orientation|screenSize"
    android:exported="true" />
```

---

## 性能基准

### 期望的性能指标
| 指标 | 目标 | 实际 |
|-----|------|------|
| 启动时间 | < 2 秒 | [待测试] |
| 首页加载 | < 3 秒 | [待测试] |
| 导航响应 | < 200ms | [待测试] |
| 内存占用 | < 150MB | [待测试] |
| 帧率 | 60 FPS | [待测试] |

---

## 已知限制

1. **账户管理**: 当前显示 Toast，计划中实现
2. **浏览器设置**: 当前显示 Toast，计划中实现
3. **书签功能**: 已删除，可通过历史记录替代
4. **扩展功能**: 广告拦截、阅读模式等尚未实现

---

## 调试技巧

### 启用 WebView 调试
```kotlin
// 在 setupWebView() 中添加
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

### 查看 WebView 日志
```bash
# 使用 Chrome DevTools 连接
adb forward tcp:9222 localabstract:webview_devtools_remote

# 在浏览器中打开
chrome://inspect
```

### 捕获网络流量
```bash
# 使用 Wireshark 或 Charles Proxy
# 或在 WebViewClient 中拦截请求
override fun shouldInterceptRequest(
    view: WebView?,
    request: WebResourceRequest?
): WebResourceResponse?
```

---

## 清单

- [ ] 编译成功（BUILD SUCCESSFUL）
- [ ] APK 生成成功（80M）
- [ ] 安装到设备成功
- [ ] 启动浏览器成功
- [ ] 导航功能正常
- [ ] URL 输入正常
- [ ] 菜单功能正常
- [ ] 集成点正常工作
- [ ] 没有崩溃或异常
- [ ] 性能满足预期

---

## 问题报告模板

如果在测试中发现问题，请使用以下模板报告：

```
**问题标题**: [简短描述]

**重现步骤**:
1. 步骤 1
2. 步骤 2
3. 步骤 3

**期望结果**: [应该发生什么]

**实际结果**: [实际发生了什么]

**日志**: [任何相关的日志或错误信息]

**设备/版本**: [测试设备型号和 Android 版本]

**严重程度**: [关键/高/中/低]
```

---

## 完成检查表

测试完成后，请检查以下项目：

- [ ] 所有 10+ 个测试场景已执行
- [ ] 没有发现关键问题
- [ ] 性能符合预期
- [ ] 所有导航入口正常工作
- [ ] 应用可以稳定运行 > 5 分钟
- [ ] 内存占用合理
- [ ] 所有 UI 元素正确显示
- [ ] 文档已更新

---

## 总结

Chromium 浏览器集成已完成并成功编译。现在需要在实际设备上进行功能测试，验证所有用户交互和性能指标。

**预期结果**: ✅ 所有测试通过，应用可以上线

---

**生成时间**: 2025-11-24
**文档版本**: 1.0
**测试状态**: 待执行
