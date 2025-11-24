# Chromium浏览器系统集成指南

## 快速导航

本指南帮助开发者快速了解和使用蓝河Chromium浏览器系统。

### 📚 文档体系

| 文档 | 用途 | 阅读时间 |
|------|------|----------|
| [QUICK_START_GUIDE.md](QUICK_START_GUIDE.md) | 5分钟快速上手 | 10-15分钟 |
| [CHROMIUM_BROWSER_COMPLETE_GUIDE.md](CHROMIUM_BROWSER_COMPLETE_GUIDE.md) | 完整开发指南 | 1-2小时 |
| [CHROMIUM_BROWSER_ARCHITECTURE.md](CHROMIUM_BROWSER_ARCHITECTURE.md) | 架构设计深入 | 30-45分钟 |
| [CHROMIUM_INTEGRATION_VERIFICATION_REPORT.md](CHROMIUM_INTEGRATION_VERIFICATION_REPORT.md) | **验证报告（本次新增）** | 20-30分钟 |

---

## 🚀 启动浏览器（3种方式）

### 方式1：主菜单启动

```kotlin
// 在MainActivity或任何Fragment中
Intent(this, ChromiumBrowserActivity::class.java).apply {
    startActivity(this)
}
```

### 方式2：按钮启动

```kotlin
// 在任何UI中添加启动按钮
binding.browserButton.setOnClickListener {
    Intent(requireActivity(), ChromiumBrowserActivity::class.java).apply {
        requireActivity().startActivity(this)
    }
}
```

### 方式3：自动启动

```kotlin
// 在Application的初始化中自动启动
Intent(context, ChromiumBrowserActivity::class.java).apply {
    context.startActivity(this)
}
```

---

## 💡 核心模块使用

### 1️⃣ 浏览器引擎 (BrowserEngine)

#### 创建和管理标签

```kotlin
// 获取浏览器引擎实例
val browserEngine = BrowserEngine(context)

// 创建新标签
val newTab = browserEngine.createTab("https://www.google.com")

// 切换标签
browserEngine.switchTab(tabId)

// 关闭标签
browserEngine.closeTab(tabId)

// 获取当前标签
val activeTab = browserEngine.getActiveTab()

// 获取所有标签
val allTabs = browserEngine.getAllTabs()
```

#### 导航控制

```kotlin
val browserEngine = BrowserEngine(context)

// 导航到URL
browserEngine.navigateTo("https://www.github.com")

// 返回上一页
browserEngine.goBack()

// 前进下一页
browserEngine.goForward()

// 刷新当前页
browserEngine.refresh()

// 停止加载
browserEngine.stopLoading()
```

#### 数据管理

```kotlin
// 获取浏览历史
lifecycleScope.launch {
    val history = browserEngine.getHistory()
    history.forEach { entry ->
        println("${entry.title} - ${entry.url}")
    }
}

// 清除缓存
browserEngine.clearCache()

// 清除Cookie
browserEngine.clearCookies()

// 清除历史
browserEngine.clearHistory()

// 清理资源
browserEngine.cleanup()
```

### 2️⃣ 账号系统 (BrowserAccountManager)

#### 账户创建

```kotlin
val accountManager = BrowserAccountManager(context)

lifecycleScope.launch {
    try {
        val result = accountManager.createAccount(
            username = "john_doe",
            password = "SecurePass123!",
            email = "john@example.com"
        )

        if (result.isSuccess) {
            val account = result.getOrNull()
            Toast.makeText(
                context,
                "账户创建成功: ${account?.username}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "创建失败: ${result.exceptionOrNull()?.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (e: Exception) {
        Log.e("AccountManager", "创建账户异常", e)
    }
}
```

#### 用户登录

```kotlin
val accountManager = BrowserAccountManager(context)

lifecycleScope.launch {
    val result = accountManager.login("john_doe", "SecurePass123!")

    if (result.isSuccess) {
        val account = result.getOrNull()
        Log.d("Login", "登录成功: ${account?.username}")
        // 更新UI显示登录状态
        updateLoginUI(account)
    } else {
        Log.e("Login", "登录失败: ${result.exceptionOrNull()?.message}")
    }
}
```

#### 账户管理

```kotlin
val accountManager = BrowserAccountManager(context)

// 修改密码
lifecycleScope.launch {
    accountManager.changePassword(
        username = "john_doe",
        oldPassword = "SecurePass123!",
        newPassword = "NewSecurePass456!"
    )
}

// 登出
accountManager.logout()

// 获取当前用户
val currentAccount = accountManager.getCurrentAccount()
if (currentAccount != null) {
    println("当前用户: ${currentAccount.username}")
}

// 列出所有账户
lifecycleScope.launch {
    val accounts = accountManager.listAccounts()
    accounts.forEach { account ->
        println("${account.username} - ${account.email}")
    }
}

// 删除账户
lifecycleScope.launch {
    accountManager.deleteAccount("john_doe")
}
```

### 3️⃣ 密码管理 (PasswordManager)

#### 保存和检索密码

```kotlin
val passwordManager = PasswordManager(context)

// 保存密码
lifecycleScope.launch {
    passwordManager.savePassword(
        domain = "github.com",
        username = "user@example.com",
        password = "GithubPass123!"
    )
    Toast.makeText(context, "密码已保存", Toast.LENGTH_SHORT).show()
}

// 检索密码
lifecycleScope.launch {
    val password = passwordManager.getPassword("github.com", "user@example.com")
    password.onSuccess { pwd ->
        Log.d("Password", "密码: $pwd")
    }
}

// 获取域名下的所有密码
lifecycleScope.launch {
    val passwords = passwordManager.getPasswordsForDomain("github.com")
    passwords.forEach { entry ->
        println("${entry.username} - ${entry.password}")
    }
}
```

#### 密码工具

```kotlin
val passwordManager = PasswordManager(context)

// 生成强密码
val strongPassword = passwordManager.generateStrongPassword(length = 16)
println("生成的密码: $strongPassword")

// 评估密码强度
val strength = passwordManager.evaluatePasswordStrength("SecurePass123!")
println("密码强度: ${strength.name}") // WEAK, FAIR, GOOD, STRONG

// 检查密码泄露
lifecycleScope.launch {
    val isLeaked = passwordManager.checkPasswordLeakage("password123")
    if (isLeaked) {
        Toast.makeText(context, "⚠️ 该密码可能已泄露", Toast.LENGTH_LONG).show()
    } else {
        Toast.makeText(context, "✅ 密码看起来安全", Toast.LENGTH_SHORT).show()
    }
}
```

#### 自动填充

```kotlin
val passwordManager = PasswordManager(context)
val webView = WebView(context)

// 启用WebView自动填充
passwordManager.enableAutofill(webView)

// 获取自动填充建议
lifecycleScope.launch {
    val suggestions = passwordManager.getAutofillSuggestions("github.com")
    // 在UI中显示建议
    displaySuggestions(suggestions)
}
```

---

## 🔧 高级用法

### 集成到Activity

```kotlin
@AndroidEntryPoint
class MyBrowserActivity : AppCompatActivity() {

    private lateinit var browserEngine: BrowserEngine
    private lateinit var accountManager: BrowserAccountManager
    private lateinit var passwordManager: PasswordManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化所有系统
        browserEngine = BrowserEngine(this)
        accountManager = BrowserAccountManager(this)
        passwordManager = PasswordManager(this)

        // 检查用户登录
        val currentUser = accountManager.getCurrentAccount()
        if (currentUser != null) {
            // 用户已登录
            startBrowsing()
        } else {
            // 显示登录界面
            showLoginDialog()
        }
    }

    private fun startBrowsing() {
        val tab = browserEngine.createTab("https://www.google.com")
        // 继续浏览...
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        browserEngine.cleanup()
        accountManager.cleanup()
        passwordManager.cleanup()
    }
}
```

### 自定义WebView配置

```kotlin
@SuppressLint("SetJavaScriptEnabled")
private fun setupWebView(webView: WebView) {
    webView.settings.apply {
        // 基础设置
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true

        // 缓存设置
        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

        // 用户界面
        builtInZoomControls = true
        displayZoomControls = false

        // 内容设置
        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALLOW_ALL

        // 用户代理（可选）
        userAgentString = "蓝河浏览器/1.0"
    }

    // 设置事件处理
    webView.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            showLoadingProgress()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            hideLoadingProgress()
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            updateProgressBar(newProgress)
        }
    }
}
```

### 与ViewModel集成

```kotlin
class BrowserViewModel : ViewModel() {

    private lateinit var browserEngine: BrowserEngine
    private lateinit var accountManager: BrowserAccountManager
    private lateinit var passwordManager: PasswordManager

    // 观察当前用户
    private val _currentUser = MutableLiveData<BrowserAccount?>()
    val currentUser: LiveData<BrowserAccount?> = _currentUser

    // 观察浏览历史
    private val _history = MutableLiveData<List<BrowserHistoryEntry>>()
    val history: LiveData<List<BrowserHistoryEntry>> = _history

    fun initializeBrowser(context: Context) {
        browserEngine = BrowserEngine(context)
        accountManager = BrowserAccountManager(context)
        passwordManager = PasswordManager(context)

        _currentUser.postValue(accountManager.getCurrentAccount())
    }

    fun createNewTab(url: String) {
        viewModelScope.launch {
            browserEngine.createTab(url)
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val result = accountManager.login(username, password)
            if (result.isSuccess) {
                _currentUser.postValue(result.getOrNull())
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _history.postValue(browserEngine.getHistory())
        }
    }

    override fun onCleared() {
        super.onCleared()
        browserEngine.cleanup()
        accountManager.cleanup()
        passwordManager.cleanup()
    }
}
```

---

## 🔐 安全最佳实践

### ✅ DO - 应该做

1. **使用强密码**
   ```kotlin
   // ✅ 正确做法
   val password = passwordManager.generateStrongPassword(length = 16)
   accountManager.createAccount("user", password)
   ```

2. **定期检查泄露**
   ```kotlin
   // ✅ 正确做法
   lifecycleScope.launch {
       val isLeaked = passwordManager.checkPasswordLeakage(userPassword)
       if (isLeaked) {
           alertUserToChangePassword()
       }
   }
   ```

3. **清理敏感数据**
   ```kotlin
   // ✅ 正确做法
   fun logout() {
       accountManager.logout()
       browserEngine.clearCache()
       browserEngine.clearCookies()
   }
   ```

4. **使用HTTPS**
   ```kotlin
   // ✅ 正确做法
   browserEngine.navigateTo("https://secure-site.com")  // 推荐
   // ❌ 避免
   // browserEngine.navigateTo("http://insecure-site.com")  // 不安全
   ```

### ❌ DON'T - 不应该做

1. **不要硬编码密码**
   ```kotlin
   // ❌ 错误做法
   val password = "MyPassword123"

   // ✅ 正确做法
   val password = passwordManager.generateStrongPassword()
   ```

2. **不要重复使用密码**
   ```kotlin
   // ❌ 错误做法
   accountManager.createAccount("user1", "samePassword")
   accountManager.createAccount("user2", "samePassword")

   // ✅ 正确做法
   val pwd1 = passwordManager.generateStrongPassword()
   val pwd2 = passwordManager.generateStrongPassword()
   accountManager.createAccount("user1", pwd1)
   accountManager.createAccount("user2", pwd2)
   ```

3. **不要在代码中存储密钥**
   ```kotlin
   // ❌ 错误做法
   val apiKey = "sk-1234567890abcdef"

   // ✅ 正确做法
   // 使用EncryptedSharedPreferences或系统密钥存储
   ```

4. **不要忽视错误处理**
   ```kotlin
   // ❌ 错误做法
   val account = accountManager.login("user", "pass").getOrThrow()

   // ✅ 正确做法
   val result = accountManager.login("user", "pass")
   if (result.isSuccess) {
       handleSuccess(result.getOrNull())
   } else {
       handleError(result.exceptionOrNull())
   }
   ```

---

## 📱 UI集成示例

### 完整的登录流程

```kotlin
class LoginFragment : Fragment() {

    private lateinit var accountManager: BrowserAccountManager
    private lateinit var binding: FragmentLoginBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accountManager = BrowserAccountManager(requireContext())

        binding.loginButton.setOnClickListener {
            handleLogin()
        }

        binding.registerButton.setOnClickListener {
            handleRegister()
        }
    }

    private fun handleLogin() {
        val username = binding.usernameInput.text.toString()
        val password = binding.passwordInput.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            binding.loginButton.isEnabled = false
            binding.loadingProgress.visibility = View.VISIBLE

            val result = accountManager.login(username, password)

            binding.loginButton.isEnabled = true
            binding.loadingProgress.visibility = View.GONE

            if (result.isSuccess) {
                Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                navigateToBrowser()
            } else {
                Toast.makeText(
                    context,
                    "登录失败: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleRegister() {
        val username = binding.usernameInput.text.toString()
        val password = binding.passwordInput.text.toString()
        val email = binding.emailInput.text.toString()

        // 验证输入
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            Toast.makeText(context, "请填写所有字段", Toast.LENGTH_SHORT).show()
            return
        }

        // 检查密码强度
        val strength = PasswordManager(requireContext()).evaluatePasswordStrength(password)
        if (strength.ordinal < 2) { // FAIR等级以上
            Toast.makeText(context, "密码强度不足, 请使用更复杂的密码", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val result = accountManager.createAccount(username, password, email)

            if (result.isSuccess) {
                Toast.makeText(context, "注册成功", Toast.LENGTH_SHORT).show()
                // 自动登录
                val loginResult = accountManager.login(username, password)
                if (loginResult.isSuccess) {
                    navigateToBrowser()
                }
            } else {
                Toast.makeText(
                    context,
                    "注册失败: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun navigateToBrowser() {
        Intent(requireContext(), ChromiumBrowserActivity::class.java).apply {
            startActivity(this)
            requireActivity().finish()
        }
    }
}
```

---

## 🧪 测试指南

### 单元测试

```kotlin
class BrowserEngineTest {

    private lateinit var browserEngine: BrowserEngine
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        browserEngine = BrowserEngine(context)
    }

    @Test
    fun testCreateTab() {
        val tab = browserEngine.createTab("https://www.google.com")
        assertNotNull(tab)
        assertEquals("https://www.google.com", tab.url)
    }

    @Test
    fun testNavigation() {
        val tab = browserEngine.createTab("https://www.google.com")
        browserEngine.navigateTo("https://www.github.com")
        assertEquals("https://www.github.com", tab.url)
    }

    @After
    fun cleanup() {
        browserEngine.cleanup()
    }
}
```

### 集成测试

```kotlin
class ChromiumBrowserActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ChromiumBrowserActivity::class.java)

    @Test
    fun testActivityLaunch() {
        activityRule.scenario.onActivity { activity ->
            assertNotNull(activity.findViewById<WebView>(R.id.webView))
        }
    }

    @Test
    fun testLogin() {
        onView(withId(R.id.accountButton)).perform(click())
        onView(withId(R.id.loginInput)).perform(typeText("testuser"))
        onView(withId(R.id.passwordInput)).perform(typeText("TestPass123!"))
        onView(withId(R.id.loginButton)).perform(click())

        // 验证登录成功
        onView(withText("登录成功")).check(matches(isDisplayed()))
    }
}
```

---

## 📊 性能调优建议

### 内存优化

```kotlin
// 限制标签数量
private val MAX_TABS = 20

fun createTab(url: String): BrowserTab? {
    if (tabs.size >= MAX_TABS) {
        // 关闭最早打开的标签
        val oldestTab = tabs.values.minByOrNull { it.createdTime }
        oldestTab?.let { closeTab(it.id) }
    }
    return createNewTab(url)
}

// 及时清理资源
override fun onDestroy() {
    browserEngine.cleanup()
    accountManager.cleanup()
    passwordManager.cleanup()
}
```

### 缓存优化

```kotlin
// 定期清理缓存
fun scheduleCacheCleaning() {
    lifecycleScope.launch {
        delay(24 * 60 * 60 * 1000) // 24小时
        browserEngine.clearCache()
        scheduleCacheCleaning()
    }
}

// 设置缓存模式
webView.settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
```

### 电池优化

```kotlin
// 减少后台刷新
webView.settings.setEnableSmoothTransition(false)

// 禁用不必要的功能
webView.settings.apply {
    blockNetworkLoads = false
    blockNetworkImage = false  // 根据需要设置
}

// 使用WorkManager而不是频繁轮询
val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
    24, TimeUnit.HOURS
).build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "browser_backup",
    ExistingPeriodicWorkPolicy.KEEP,
    backupRequest
)
```

---

## 💬 FAQ - 常见问题

### Q: 如何自定义浏览器外观？
A: 修改`activity_chromium_browser.xml`布局文件，调整工具栏颜色、按钮样式等。

### Q: 密码是否真的安全？
A: 是的。系统使用PBKDF2(10,000次迭代) + 随机盐值 + AES256加密，符合行业标准。

### Q: 能否同步到其他设备？
A: 目前不支持。本系统完全本地化，不依赖云端。可在未来版本中添加可选的云备份功能。

### Q: 如何导出密码？
A: 目前不支持导出。可通过`getAllPasswords()`方法编程访问。

### Q: 支持哪些最低Android版本？
A: 最低Android 7.0 (API 24)。建议使用Android 10+获得最佳体验。

---

## 📞 获取帮助

### 文档资源
- 查看[QUICK_START_GUIDE.md](QUICK_START_GUIDE.md)快速上手
- 查看[CHROMIUM_BROWSER_COMPLETE_GUIDE.md](CHROMIUM_BROWSER_COMPLETE_GUIDE.md)深入学习
- 查看[CHROMIUM_INTEGRATION_VERIFICATION_REPORT.md](CHROMIUM_INTEGRATION_VERIFICATION_REPORT.md)验证状态

### 代码注释
所有核心文件都包含详细的Kotlin文档注释(KDoc)，使用IDE的代码完成功能可以看到详细说明。

### 技术支持
- 检查AndroidStudio中的Logcat输出
- 查看我们的Git历史提交记录
- 查看测试代码中的使用示例

---

**更新时间**: 2025-01-11
**版本**: 1.0
**状态**: ✅ 生产就绪
