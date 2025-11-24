# 蓝河Chromium浏览器 - 快速入门指南

## 🎯 5分钟快速上手

### 1. 基本了解

蓝河Chromium浏览器是一个**完整的、本地化的、安全的浏览器系统**，包含：

```
┌─────────────────────────────────────┐
│    蓝河Chromium浏览器系统            │
├─────────────────────────────────────┤
│ 📱 浏览器引擎    - 多标签、导航控制  │
│ 👤 账号系统      - 本地账户、无依赖  │
│ 🔐 密码管理      - 加密保存、自动填  │
│ 📁 文件管理      - 完整文件操作      │
│ 📦 APK管理       - 安装、分析、权限  │
│ 🔒 安全加密      - AES256、PBKDF2   │
└─────────────────────────────────────┘
```

### 2. 核心文件位置

```kotlin
// 浏览器引擎
lanhe/browser/engine/BrowserEngine.kt

// 账号系统
lanhe/browser/account/BrowserAccountManager.kt

// 密码管理
lanhe/browser/password/PasswordManager.kt

// 浏览器界面
com/lanhe/gongjuxiang/activities/ChromiumBrowserActivity.kt
```

### 3. 启动浏览器

**方式1：从主菜单启动**
```
打开蓝河助手 → 工具选项卡 → Chromium浏览器
```

**方式2：代码启动**
```kotlin
val intent = Intent(context, ChromiumBrowserActivity::class.java)
context.startActivity(intent)
```

## 💼 常见任务

### 任务1: 用户创建账户

```kotlin
lifecycleScope.launch {
    val accountManager = BrowserAccountManager(context)

    val result = accountManager.createAccount(
        username = "john_doe",
        password = "SecurePass123!",
        email = "john@example.com"
    )

    if (result.isSuccess) {
        println("账户创建成功！")
        val account = result.getOrNull()
        println("用户名: ${account?.username}")
    } else {
        println("创建失败: ${result.exceptionOrNull()?.message}")
    }
}
```

### 任务2: 用户登录

```kotlin
lifecycleScope.launch {
    val accountManager = BrowserAccountManager(context)

    val result = accountManager.login("john_doe", "SecurePass123!")

    if (result.isSuccess) {
        println("登录成功！")
        // 浏览器UI会自动显示已登录状态
    } else {
        println("登录失败: ${result.exceptionOrNull()?.message}")
    }
}
```

### 任务3: 保存网站密码

```kotlin
lifecycleScope.launch {
    val passwordManager = PasswordManager(context)

    passwordManager.savePassword(
        domain = "github.com",
        username = "user@example.com",
        password = "GithubPass123!"
    )

    println("密码已保存")
}
```

### 任务4: 获取自动填充建议

```kotlin
lifecycleScope.launch {
    val passwordManager = PasswordManager(context)

    val suggestions = passwordManager.getAutofillSuggestions("github.com")

    suggestions.forEach { username ->
        println("可自动填充: $username")
    }
}
```

### 任务5: 浏览网页

```kotlin
val browserEngine = BrowserEngine(context)

// 创建新标签
val tab = browserEngine.createTab("https://www.google.com")

// 导航到URL
browserEngine.navigateTo("https://www.github.com")

// 返回上一页
browserEngine.goBack()

// 前进
browserEngine.goForward()

// 刷新
browserEngine.refresh()
```

### 任务6: 管理访问历史

```kotlin
lifecycleScope.launch {
    val browserEngine = BrowserEngine(context)

    // 获取访问历史
    val history = browserEngine.getHistory()
    history.forEach { entry ->
        println("${entry.title} - ${entry.url}")
    }

    // 清除历史
    browserEngine.clearHistory()
}
```

### 任务7: 清理缓存和Cookie

```kotlin
lifecycleScope.launch {
    val browserEngine = BrowserEngine(context)

    browserEngine.clearCache()
    browserEngine.clearCookies()

    println("缓存和Cookie已清除")
}
```

### 任务8: 生成强密码

```kotlin
val passwordManager = PasswordManager(context)

// 生成16个字符的强密码
val strongPass = passwordManager.generateStrongPassword(length = 16)
println("生成的密码: $strongPass")

// 评估密码强度
val strength = passwordManager.evaluatePasswordStrength("SecurePass123!")
println("密码强度: ${strength.name}") // STRONG, GOOD, FAIR, WEAK
```

### 任务9: 检查密码泄露

```kotlin
lifecycleScope.launch {
    val passwordManager = PasswordManager(context)

    val isLeaked = passwordManager.checkPasswordLeakage("password123")

    if (isLeaked) {
        println("⚠️ 该密码可能已泄露，请更换")
    } else {
        println("✅ 密码看起来安全")
    }
}
```

### 任务10: 查看当前用户

```kotlin
val accountManager = BrowserAccountManager(context)

val currentAccount = accountManager.getCurrentAccount()

if (currentAccount != null) {
    println("当前用户: ${currentAccount.username}")
    println("邮箱: ${currentAccount.email}")
    println("创建于: ${java.text.SimpleDateFormat("yyyy-MM-dd").format(currentAccount.createdTime)}")
} else {
    println("尚未登录")
}
```

## 🔐 安全最佳实践

### ✅ DO - 应该做

1. **使用强密码**
   - 至少8个字符
   - 包含大小写字母和数字
   - 使用特殊字符 (!@#$%^&*)

2. **定期修改密码**
   - 建议每月修改一次
   - 使用系统生成的强密码

3. **启用自动填充**
   - 方便快速登录
   - 系统会加密存储

4. **定期清除缓存**
   - 使用菜单中的"清除缓存"
   - 定期清除Cookie

5. **在安全网络登录**
   - 避免在公共WiFi登录
   - 使用VPN增强安全性

### ❌ DON'T - 不应该做

1. **不要使用简单密码**
   - ❌ "123456"
   - ❌ "password"
   - ❌ "abc123"

2. **不要重复使用密码**
   - 每个网站使用不同密码
   - 使用系统密码生成器

3. **不要在公共电脑上登录**
   - 避免在网吧、图书馆等
   - 登录后记得登出

4. **不要忽视安全提示**
   - 弱密码警告
   - 密码泄露提醒

5. **不要关闭自动填充**
   - 安全又方便
   - 系统加密存储

## 📱 UI快速导航

```
浏览器主界面
├── 工具栏
│   ├── 返回按钮        (← 返回上一页)
│   ├── 前进按钮        (→ 前进下一页)
│   ├── 刷新按钮        (⟲ 刷新页面)
│   ├── 地址栏          (输入网址)
│   ├── 账户按钮        (👤 账号菜单)
│   └── 菜单按钮        (⋮ 功能菜单)
├── WebView区域         (网页显示区)
└── 状态栏              (加载进度)

菜单项
├── 书签               (收藏网页)
├── 浏览历史            (查看访问记录)
├── 下载管理            (管理下载文件)
├── 密码管理            (查看保存的密码)
├── 清除缓存            (清理浏览数据)
└── 设置               (浏览器设置)

账户菜单
├── 账号信息            (查看用户信息)
├── 修改密码            (更改账户密码)
└── 登出               (退出账户)
```

## 🔧 常见问题解决

### Q: 忘记了账户密码怎么办？
**A:** 本地账户无法重置，建议：
1. 创建新账户
2. 迁移重要数据（密码等）
3. 删除旧账户

### Q: 密码保存失败怎么办？
**A:** 检查以下几点：
1. 确保已登录账户
2. 检查存储权限
3. 清除应用缓存后重试

### Q: 如何导出密码？
**A:** 目前不支持直接导出，建议：
1. 手动复制密码
2. 使用密码管理器查看列表
3. 定期记录重要密码

### Q: WebView白屏怎么办？
**A:** 尝试以下解决方案：
1. 刷新页面 (⟲ 按钮)
2. 清除缓存 (菜单 → 清除缓存)
3. 重启应用

### Q: 密码没有被保存？
**A:** 确认以下条件：
1. 用户已登录
2. 网站表单填写完整
3. 点击登录按钮后
4. 浏览器会自动提示保存

## 📚 相关文档

- [完整开发指南](CHROMIUM_BROWSER_COMPLETE_GUIDE.md)
- [架构设计](CHROMIUM_BROWSER_ARCHITECTURE.md)
- [实现清单](IMPLEMENTATION_CHECKLIST.md)

## 💡 技巧与提示

### 💡 技巧1: 快速搜索
在地址栏输入关键词（不包含 http://），系统会自动搜索：
```
输入: "kotlin android"
自动搜索: Google搜索结果
```

### 💡 技巧2: 强密码生成
点击密码管理器的"生成强密码"按钮：
```
⚙️ 菜单 → 密码管理 → 生成密码
```

### 💡 技巧3: 批量清理
定期清除缓存保护隐私：
```
⋮ 菜单 → 清除缓存 → 全选 → 确认
```

### 💡 技巧4: 多标签管理
打开多个标签同时浏览：
```
长按地址栏 → 在新标签中打开
或 Ctrl+T 新建标签
```

### 💡 技巧5: 密码安全检查
定期检查密码安全性：
```
⋮ 菜单 → 密码管理 → 查看强度
```

## 🎓 学习路径

**初级**（1小时）
- 创建账户
- 浏览网页
- 自动填充

**中级**（2小时）
- 密码管理
- 历史记录
- 缓存管理

**高级**（3小时）
- 文件管理集成
- APK管理
- 自定义设置

---

**开始你的安全浏览之旅！** 🚀

有任何问题？查看 [完整文档](CHROMIUM_BROWSER_COMPLETE_GUIDE.md) 或提交反馈