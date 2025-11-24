# Chromium 浏览器快速参考

## 项目结构

```
app/src/main/
├── java/com/lanhe/gongjuxiang/
│   └── activities/
│       └── ChromiumBrowserActivity.kt (⭐ 主要浏览器实现)
├── res/
│   ├── layout/
│   │   └── activity_chromium_browser.xml (⭐ 浏览器布局)
│   └── menu/
│       └── menu_browser.xml (⭐ 浏览器菜单)
└── AndroidManifest.xml
```

## 快速启动代码

### 从任何地方启动浏览器

```kotlin
// 基础启动
startActivity(Intent(context, ChromiumBrowserActivity::class.java))

// 带 URL 启动（可选）
startActivity(Intent(context, ChromiumBrowserActivity::class.java).apply {
    putExtra("url", "https://www.example.com")
})

// 或者使用便利方法
fun launchBrowser(context: Context, url: String = "https://www.baidu.com") {
    context.startActivity(Intent(context, ChromiumBrowserActivity::class.java).apply {
        putExtra("url", url)
    })
}
```

## 核心方法

### ChromiumBrowserActivity 的主要方法

```kotlin
// 加载 URL（支持搜索和自动补全）
loadUrl(url: String)
// 例如:
// loadUrl("https://example.com")        // 完整 URL
// loadUrl("example.com")                 // 自动补全
// loadUrl("Android 开发")                 // 百度搜索

// WebView 导航
if (binding.webView.canGoBack()) binding.webView.goBack()
if (binding.webView.canGoForward()) binding.webView.goForward()
binding.webView.reload()

// 缓存和历史管理
binding.webView.clearCache(true)
binding.webView.clearHistory()
binding.webView.clearSslPreferences()

// 获取当前状态
val currentUrl = binding.webView.url
val canGoBack = binding.webView.canGoBack()
val canGoForward = binding.webView.canGoForward()
val title = binding.webView.title
```

## WebView 配置

### 当前启用的功能

```kotlin
// JavaScript 执行
javaScriptEnabled = true
javaScriptCanOpenWindowsAutomatically = true

// 数据持久化
domStorageEnabled = true
databaseEnabled = true

// 位置服务
setGeolocationEnabled(true)

// 混合内容
mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

// 用户代理
userAgentString = "$originalUserAgent Chromium/蓝河"
```

### 常见的扩展配置

```kotlin
// 启用调试（仅在 Debug 构建中）
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true)
}

// 缓存配置
settings.cacheMode = WebSettings.LOAD_DEFAULT  // 正常缓存
// 或
settings.cacheMode = WebSettings.LOAD_NO_CACHE // 不使用缓存

// 字体大小
settings.textZoom = 100  // 默认大小
settings.textZoom = 120  // 放大 20%

// Cookies
CookieManager.getInstance().setAcceptCookie(true)

// 本地存储大小
settings.databasePath = context.getDir("webviewdb", Context.MODE_PRIVATE).path
settings.setAppCachePath(context.getDir("webcache", Context.MODE_PRIVATE).path)
settings.setAppCacheEnabled(true)
```

## UI 元素

### 布局中的可用 ID

| ID | 类型 | 用途 |
|----|------|------|
| `toolbar` | Toolbar | 顶部工具栏 |
| `btn_back` | ImageButton | 返回按钮 |
| `btn_forward` | ImageButton | 前进按钮 |
| `btn_refresh` | ImageButton | 刷新按钮 |
| `address_bar` | EditText | 地址栏/搜索框 |
| `btn_account` | ImageButton | 账户按钮 |
| `btn_menu` | ImageButton | 菜单按钮 |
| `progress_bar` | ProgressBar | 加载进度条 |
| `webView` | WebView | 主要内容区域 |
| `status_text` | TextView | 状态文本 |

## 常见任务

### 任务 1: 添加新的菜单项

```xml
<!-- 在 menu_browser.xml 中添加 -->
<item
    android:id="@+id/action_new_feature"
    android:title="新功能"
    android:icon="@drawable/ic_new_feature"
    app:showAsAction="ifRoom" />
```

```kotlin
// 在 onOptionsItemSelected 中处理
R.id.action_new_feature -> {
    // 实现功能
    Toast.makeText(this, "新功能", Toast.LENGTH_SHORT).show()
    true
}
```

### 任务 2: 自定义 WebViewClient

```kotlin
// 扩展内部客户端
private inner class CustomWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        // 自定义 URL 加载逻辑
        if (url?.contains("example.com") == true) {
            // 处理特定域名
            return true
        }
        return super.shouldOverrideUrlLoading(view, url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        // 处理加载错误
        Toast.makeText(
            this@ChromiumBrowserActivity,
            "页面加载失败: ${error?.description}",
            Toast.LENGTH_SHORT
        ).show()
    }
}
```

### 任务 3: 注入 JavaScript

```kotlin
// 页面加载完成后注入 JS
override fun onPageFinished(view: WebView?, url: String?) {
    super.onPageFinished(view, url)
    binding.webView.evaluateJavascript(
        """
        javascript: (function() {
            document.querySelector('body').style.backgroundColor = '#f5f5f5';
        })();
        """.trimIndent(),
        null
    )
}
```

### 任务 4: 拦截网络请求

```kotlin
override fun shouldInterceptRequest(
    view: WebView?,
    request: WebResourceRequest?
): WebResourceResponse? {
    val url = request?.url?.toString() ?: return null

    // 拦截广告
    if (url.contains("ads.") || url.contains("advertisement")) {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream("".toByteArray())
        )
    }

    return super.shouldInterceptRequest(view, request)
}
```

## 常见问题

### Q1: 如何实现账户登录保存？

```kotlin
// 使用 EncryptedSharedPreferences
private fun saveLoginInfo(username: String, password: String) {
    val masterKey = MasterKey.Builder(this)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        this,
        "browser_login",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    encryptedSharedPreferences.edit().apply {
        putString("username", username)
        putString("password", password)
        apply()
    }
}
```

### Q2: 如何处理文件下载？

```kotlin
// 当前实现
private fun handleDownload(url: String) {
    Toast.makeText(this, "下载已开始: $url", Toast.LENGTH_SHORT).show()
}

// 完整实现（可选）
private fun downloadFile(url: String) {
    val request = DownloadManager.Request(Uri.parse(url))
    request.setTitle("下载文件")
    request.setDescription(url)
    request.allowScanningByMediaScanner()
    request.setNotificationVisibility(
        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
    )

    val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    downloadManager.enqueue(request)
}
```

### Q3: 如何实现书签功能？

```kotlin
// 使用 Room 数据库
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

// DAO
@Dao
interface BookmarkDao {
    @Insert
    suspend fun insert(bookmark: Bookmark)

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Delete
    suspend fun delete(bookmark: Bookmark)
}

// 在浏览器中添加书签
fun addBookmark() {
    viewModelScope.launch {
        val bookmark = Bookmark(
            title = binding.webView.title ?: "未命名",
            url = binding.webView.url ?: ""
        )
        bookmarkDao.insert(bookmark)
        Toast.makeText(this@ChromiumBrowserActivity, "已添加书签", Toast.LENGTH_SHORT).show()
    }
}
```

### Q4: 如何实现搜索历史？

```kotlin
// 监听地址栏输入
binding.addressBar.doOnTextChanged { text, _, _, _ ->
    // 自动建议
    if (text?.length ?: 0 > 0) {
        // 显示历史和建议
        showSearchSuggestions(text.toString())
    }
}

// 保存搜索历史到 SharedPreferences
private fun saveSearchHistory(query: String) {
    val sharedPref = getSharedPreferences("search_history", Context.MODE_PRIVATE)
    val history = sharedPref.getStringSet("queries", mutableSetOf()) ?: mutableSetOf()
    history.add(query)
    sharedPref.edit().putStringSet("queries", history).apply()
}
```

## 性能优化技巧

### 1. 内存管理

```kotlin
override fun onDestroy() {
    // 清理 WebView
    binding.webView.apply {
        stopLoading()
        clearHistory()
        clearCache(true)
        clearSslPreferences()
        destroy()
    }
    // 清理作用域
    scope.cancel()
    super.onDestroy()
}
```

### 2. 线程管理

```kotlin
// 在后台线程加载 JavaScript
private fun executeJSInBackground(script: String) {
    scope.launch(Dispatchers.Default) {
        val result = binding.webView.evaluateJavascript(script) { result ->
            // 处理结果
        }
    }
}
```

### 3. 缓存优化

```kotlin
// 使用 HTTP 缓存
settings.cacheMode = when {
    isNetworkAvailable() -> WebSettings.LOAD_DEFAULT
    else -> WebSettings.LOAD_CACHE_ONLY
}
```

## 测试指南

### 单元测试

```kotlin
@RunWith(AndroidJUnit4::class)
class ChromiumBrowserActivityTest {

    @get:Rule
    val activityScenarioRule = activityScenarioRule<ChromiumBrowserActivity>()

    @Test
    fun testActivityLaunches() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity { activity ->
            assertNotNull(activity.binding.webView)
        }
    }
}
```

### 集成测试

```kotlin
@RunWith(AndroidJUnit4::class)
class ChromiumBrowserIntegrationTest {

    @Test
    fun testUrlLoading() {
        onView(withId(R.id.address_bar))
            .perform(typeText("https://www.baidu.com"), closeSoftKeyboard())
        onView(withId(R.id.address_bar))
            .perform(pressKey(KeyEvent.KEYCODE_ENTER))

        // 验证页面加载
        onView(withId(R.id.webView))
            .check(matches(isDisplayed()))
    }
}
```

## 调试命令

```bash
# 查看 WebView 日志
adb logcat | grep chromium

# 转发 WebView 调试端口
adb forward tcp:9222 localabstract:webview_devtools_remote

# 重启应用
adb shell am force-stop com.lanhe.gongjuxiang
adb shell am start -n com.lanhe.gongjuxiang/.activities.MainActivity

# 清除应用数据
adb shell pm clear com.lanhe.gongjuxiang
```

## 资源链接

- [Android WebView 官方文档](https://developer.android.com/guide/webapps/webview)
- [Material Design 3](https://m3.material.io/)
- [Kotlin 协程指南](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android 架构组件](https://developer.android.com/topic/architecture)

## 版本信息

- **Activity 版本**: 1.0.0
- **最后更新**: 2025-11-24
- **兼容 API**: 24+ (Android 7.0+)
- **目标 API**: 36 (Android 15)

---

**快速参考生成时间**: 2025-11-24
**文档版本**: 1.0
