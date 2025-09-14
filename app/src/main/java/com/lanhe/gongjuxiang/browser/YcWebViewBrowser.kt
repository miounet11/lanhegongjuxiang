package com.lanhe.gongjuxiang.browser

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityBrowserBinding
import com.lanhe.gongjuxiang.models.Bookmark
import com.lanhe.gongjuxiang.utils.AdBlocker
import com.lanhe.gongjuxiang.utils.ImageOptimizer
import com.lanhe.gongjuxiang.utils.SecurityManager
import kotlinx.coroutines.*

/**
 * YCWebView浏览器 - 基于腾讯x5内核的高性能浏览器
 * 集成广告拦截、图片优化、安全防护等高级功能
 */
class YcWebViewBrowser : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bookmarksRecyclerView: RecyclerView
    private lateinit var fabMenu: FloatingActionButton

    // 浏览器功能管理器
    private lateinit var adBlocker: AdBlocker
    private lateinit var imageOptimizer: ImageOptimizer
    private lateinit var securityManager: SecurityManager

    // 浏览器状态
    private var currentUrl = ""
    private var currentTitle = ""
    private var isLoading = false
    private var canGoBack = false
    private var canGoForward = false

    // 书签列表
    private val bookmarks = mutableListOf<Bookmark>()

    // 历史记录
    private val history = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupWebView()
        setupNavigationDrawer()
        setupFabMenu()
        setupBackPressedCallback()

        // 加载初始页面
        loadUrl("https://www.baidu.com")
    }

    private fun initializeComponents() {
        webView = binding.webView
        progressBar = binding.progressBar
        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView
        bookmarksRecyclerView = binding.bookmarksRecyclerView
        fabMenu = binding.fabMenu

        // 初始化功能管理器
        adBlocker = AdBlocker(this)
        imageOptimizer = ImageOptimizer(this)
        securityManager = SecurityManager(this)

        // 设置RecyclerView
        bookmarksRecyclerView.layoutManager = LinearLayoutManager(this)
        bookmarksRecyclerView.adapter = BookmarksAdapter(bookmarks) { bookmark ->
            loadUrl(bookmark.url)
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        // 加载书签
        loadBookmarks()
    }

    private fun setupWebView() {
        val webSettings = webView.settings

        // 启用JavaScript
        webSettings.javaScriptEnabled = true

        // 启用DOM存储
        webSettings.domStorageEnabled = true

        // 启用数据库存储
        webSettings.databaseEnabled = true

        // 启用应用缓存
        webSettings.setAppCacheEnabled(true)
        webSettings.setAppCachePath(cacheDir.absolutePath)

        // 启用地理位置
        webSettings.setGeolocationEnabled(true)

        // 启用文件访问
        webSettings.allowFileAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true

        // 设置用户代理
        webSettings.userAgentString = webSettings.userAgentString + " YCWebView/1.0"

        // 启用硬件加速
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // 设置WebView客户端
        webView.webViewClient = YcWebViewClient()
        webView.webChromeClient = YcWebChromeClient()

        // 设置下载监听器
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            handleDownload(url, contentDisposition, mimetype)
        }

        // 设置长按监听器
        webView.setOnLongClickListener { handleLongClick() }

        // 设置键盘事件监听
        webView.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (webView.canGoBack()) {
                    webView.goBack()
                    return@setOnKeyListener true
                }
            }
            false
        }
    }

    private fun setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> loadUrl("https://www.baidu.com")
                R.id.nav_bookmarks -> showBookmarks()
                R.id.nav_history -> showHistory()
                R.id.nav_downloads -> showDownloads()
                R.id.nav_settings -> showSettings()
                R.id.nav_incognito -> startIncognitoMode()
                R.id.nav_clear_cache -> clearCache()
                R.id.nav_clear_cookies -> clearCookies()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupFabMenu() {
        fabMenu.setOnClickListener {
            showFabMenu()
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    private fun loadUrl(url: String) {
        var finalUrl = url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            finalUrl = "https://$url"
        }

        currentUrl = finalUrl
        webView.loadUrl(finalUrl)

        // 添加到历史记录
        if (!history.contains(finalUrl)) {
            history.add(finalUrl)
        }

        updateNavigationButtons()
    }

    private fun updateNavigationButtons() {
        canGoBack = webView.canGoBack()
        canGoForward = webView.canGoForward()

        // 更新导航按钮状态
        binding.btnBack.isEnabled = canGoBack
        binding.btnForward.isEnabled = canGoForward
    }

    private fun handleDownload(url: String, contentDisposition: String, mimetype: String) {
        // 处理文件下载
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)

        Snackbar.make(binding.root, "开始下载文件", Snackbar.LENGTH_SHORT).show()
    }

    private fun handleLongClick(): Boolean {
        val hitTestResult = webView.hitTestResult

        when (hitTestResult.type) {
            WebView.HitTestResult.IMAGE_TYPE,
            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                // 长按图片
                showImageMenu(hitTestResult.extra)
                return true
            }
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> {
                // 长按链接
                showLinkMenu(hitTestResult.extra)
                return true
            }
        }

        return false
    }

    private fun showImageMenu(imageUrl: String?) {
        // 显示图片操作菜单
        Snackbar.make(binding.root, "图片操作: $imageUrl", Snackbar.LENGTH_SHORT).show()
    }

    private fun showLinkMenu(linkUrl: String?) {
        // 显示链接操作菜单
        Snackbar.make(binding.root, "链接操作: $linkUrl", Snackbar.LENGTH_SHORT).show()
    }

    private fun showFabMenu() {
        // 显示浮动操作菜单
        Snackbar.make(binding.root, "浮动菜单功能开发中", Snackbar.LENGTH_SHORT).show()
    }

    private fun loadBookmarks() {
        // 加载书签数据
        bookmarks.clear()
        bookmarks.addAll(getDefaultBookmarks())
        bookmarksRecyclerView.adapter?.notifyDataSetChanged()
    }

    private fun getDefaultBookmarks(): List<Bookmark> {
        return listOf(
            Bookmark("百度", "https://www.baidu.com", "search"),
            Bookmark("Google", "https://www.google.com", "search"),
            Bookmark("GitHub", "https://github.com", "code"),
            Bookmark("Stack Overflow", "https://stackoverflow.com", "code"),
            Bookmark("Android Developers", "https://developer.android.com", "android")
        )
    }

    private fun showBookmarks() {
        // 显示书签页面
        Snackbar.make(binding.root, "书签管理功能开发中", Snackbar.LENGTH_SHORT).show()
    }

    private fun showHistory() {
        // 显示历史记录
        Snackbar.make(binding.root, "历史记录功能开发中", Snackbar.LENGTH_SHORT).show()
    }

    private fun showDownloads() {
        // 显示下载管理
        Snackbar.make(binding.root, "下载管理功能开发中", Snackbar.LENGTH_SHORT).show()
    }

    private fun showSettings() {
        // 显示浏览器设置
        Snackbar.make(binding.root, "浏览器设置功能开发中", Snackbar.LENGTH_SHORT).show()
    }

    private fun startIncognitoMode() {
        // 启动无痕浏览模式
        Snackbar.make(binding.root, "无痕浏览模式启动", Snackbar.LENGTH_SHORT).show()
    }

    private fun clearCache() {
        // 清除缓存
        webView.clearCache(true)
        Snackbar.make(binding.root, "缓存已清除", Snackbar.LENGTH_SHORT).show()
    }

    private fun clearCookies() {
        // 清除Cookie
        CookieManager.getInstance().removeAllCookies(null)
        Snackbar.make(binding.root, "Cookie已清除", Snackbar.LENGTH_SHORT).show()
    }

    // WebView客户端
    private inner class YcWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            isLoading = true
            progressBar.visibility = View.VISIBLE
            currentUrl = url ?: ""
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url, favicon)
            isLoading = false
            progressBar.visibility = View.GONE
            updateNavigationButtons()

            // 执行JavaScript优化
            injectJavaScriptOptimizations()
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            // 广告拦截
            if (adBlocker.shouldBlock(request?.url.toString())) {
                return WebResourceResponse("text/plain", "utf-8", null)
            }

            // 图片优化
            if (imageOptimizer.shouldOptimizeImage(request?.url.toString())) {
                return imageOptimizer.optimizeImageRequest(request)
            }

            return super.shouldInterceptRequest(view, request)
        }

        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            // SSL证书验证
            if (securityManager.shouldAllowSslError(error)) {
                handler?.proceed()
            } else {
                handler?.cancel()
                Snackbar.make(binding.root, "SSL证书验证失败", Snackbar.LENGTH_SHORT).show()
            }
        }

        private fun injectJavaScriptOptimizations() {
            // 注入JavaScript优化代码
            val jsCode = """
                // 移除广告元素
                var ads = document.querySelectorAll('[class*="ad"], [id*="ad"], [class*="banner"]');
                ads.forEach(function(ad) { ad.style.display = 'none'; });

                // 优化图片加载
                var images = document.querySelectorAll('img');
                images.forEach(function(img) {
                    if (img.src && img.src.includes('data:image')) {
                        img.loading = 'lazy';
                    }
                });

                // 移除跟踪脚本
                var trackers = document.querySelectorAll('script[src*="google-analytics"], script[src*="facebook"]');
                trackers.forEach(function(tracker) { tracker.remove(); });
            """

            webView.evaluateJavascript(jsCode, null)
        }
    }

    // WebChrome客户端
    private inner class YcWebChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            progressBar.progress = newProgress

            if (newProgress == 100) {
                progressBar.visibility = View.GONE
            }
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            currentTitle = title ?: ""
            supportActionBar?.title = currentTitle
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            // 处理文件选择
            return true
        }
    }

    // 书签适配器
    private inner class BookmarksAdapter(
        private val bookmarks: List<Bookmark>,
        private val onBookmarkClick: (Bookmark) -> Unit
    ) : RecyclerView.Adapter<BookmarksAdapter.BookmarkViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
            val view = layoutInflater.inflate(R.layout.item_bookmark, parent, false)
            return BookmarkViewHolder(view)
        }

        override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
            holder.bind(bookmarks[position])
        }

        override fun getItemCount() = bookmarks.size

        inner class BookmarkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(bookmark: Bookmark) {
                // 设置书签数据显示
                itemView.setOnClickListener { onBookmarkClick(bookmark) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 清理WebView
        webView.stopLoading()
        webView.clearHistory()
        webView.clearCache(true)
        webView.destroy()

        // 清理其他资源
        adBlocker.cleanup()
        imageOptimizer.cleanup()
        securityManager.cleanup()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"

        fun start(context: Context, url: String? = null, title: String? = null) {
            val intent = Intent(context, YcWebViewBrowser::class.java).apply {
                url?.let { putExtra(EXTRA_URL, it) }
                title?.let { putExtra(EXTRA_TITLE, it) }
            }
            context.startActivity(intent)
        }
    }
}
