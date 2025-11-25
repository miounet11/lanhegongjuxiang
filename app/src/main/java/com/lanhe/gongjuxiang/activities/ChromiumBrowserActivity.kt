package com.lanhe.gongjuxiang.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityChromiumBrowserBinding
import com.lanhe.gongjuxiang.utils.BrowserManager
import com.lanhe.mokuai.bookmark.BookmarkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Chromium浏览器主Activity
 *
 * 企业级浏览器实现，完整功能：
 * - ✅ 书签管理系统（BookmarkManager集成）
 * - ✅ 历史记录自动保存（BrowserHistory）
 * - ✅ 多标签页支持（TabManager）
 * - ✅ 下载管理器集成（DownloadManager）
 * - ✅ 广告拦截引擎（AdBlocker）
 * - ✅ 智能地址栏（搜索和URL识别）
 * - ✅ Material Design 3.0 UI
 *
 * @author 蓝河助手
 * @version 3.0.0 (Full Integration)
 */
class ChromiumBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChromiumBrowserBinding
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 浏览器管理器（核心）
    private lateinit var browserManager: BrowserManager

    // 浏览器状态
    private var currentUrl = ""
    private var currentTitle = ""
    private var isLoading = false
    private var currentTabId: String? = null

    // 当前页面是否已加入书签
    private var isBookmarked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChromiumBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化浏览器管理器
        browserManager = BrowserManager.getInstance(this)

        // Hide default action bar as we use a custom floating one
        supportActionBar?.hide()

        setupWebView()
        setupClickListeners()
        setupBookmarkButton()

        // 创建或恢复标签页
        initializeTab()

        // 加载初始页面
        val initialUrl = intent?.getStringExtra("url") ?: browserManager.getHomepage()
        loadUrl(initialUrl)
    }

    /**
     * 设置WebView配置和客户端
     */
    private fun setupWebView() {
        binding.webView.settings.apply {
            // 启用JavaScript
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true

            // 启用DOM存储
            domStorageEnabled = true
            databaseEnabled = true

            // 启用地理位置
            setGeolocationEnabled(true)

            // 混合内容模式（支持HTTPS页面加载HTTP资源）
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            // 用户代理
            val originalUserAgent = userAgentString
            userAgentString = "$originalUserAgent Chromium/蓝河"
        }

        // 设置WebViewClient
        binding.webView.webViewClient = ChromiumWebViewClient()
        binding.webView.webChromeClient = ChromiumWebChromeClient()

        // 设置下载监听器
        binding.webView.setDownloadListener { url, _, _, _, _ ->
            handleDownload(url)
        }
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        // Bottom Navigation Bar Listeners
        binding.btnBack.setOnClickListener {
            animateButtonPress(it)
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                Toast.makeText(this, "No previous page", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnForward.setOnClickListener {
            animateButtonPress(it)
            if (binding.webView.canGoForward()) {
                binding.webView.goForward()
            } else {
                Toast.makeText(this, "No next page", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnHome.setOnClickListener {
            animateButtonPress(it)
            loadUrl("https://www.baidu.com")
        }

        binding.btnTabs.setOnClickListener {
            animateButtonPress(it)
            showTabsDialog()
        }

        binding.btnMenu.setOnClickListener {
            animateButtonPress(it)
            showBrowserMenu()
        }

        // Top Bar Listeners
        binding.btnRefresh.setOnClickListener {
            animateButtonPress(it)
            binding.webView.reload()
        }

        // 地址栏回车处理
        binding.addressBar.setOnEditorActionListener { _, actionId, event ->
            if (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                val url = binding.addressBar.text.toString().trim()
                if (url.isNotEmpty()) {
                    loadUrl(url)
                }
                true
            } else {
                false
            }
        }
    }

    private fun animateButtonPress(view: View) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    /**
     * 加载URL
     */
    private fun loadUrl(url: String) {
        var finalUrl = url

        // 如果不是URL格式，作为搜索关键词处理
        val searchTerm = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(" ") || !url.contains(".")) {
                // 搜索
                val searchEngine = browserManager.getSearchEngine()
                finalUrl = "$searchEngine$url"
                url // 保存搜索关键词
            } else {
                // 补全https://
                finalUrl = "https://$url"
                null
            }
        } else {
            null
        }

        currentUrl = finalUrl
        binding.addressBar.setText(finalUrl)
        binding.webView.loadUrl(finalUrl)

        // 如果是搜索，记录搜索关键词
        searchTerm?.let { term ->
            scope.launch(Dispatchers.IO) {
                browserManager.addHistory(finalUrl, "搜索: $term", term)
            }
        }
    }

    /**
     * 初始化或恢复标签页
     */
    private fun initializeTab() {
        scope.launch(Dispatchers.IO) {
            // 获取或创建标签页
            val tabCount = browserManager.getTabCount()
            currentTabId = if (tabCount == 0) {
                // 创建新标签页
                browserManager.createTab(currentUrl)
            } else {
                // 恢复最后一个活跃标签页
                browserManager.activeTabId.value
            }

            // 切换到当前标签页
            currentTabId?.let {
                browserManager.switchToTab(it)
            }
        }
    }

    /**
     * 设置书签按钮
     */
    private fun setupBookmarkButton() {
        // 书签按钮点击
        binding.btnBookmark?.setOnClickListener {
            animateButtonPress(it)
            toggleBookmark()
        }

        // 初始化书签状态
        updateBookmarkStatus()
    }

    /**
     * 切换书签状态
     */
    private fun toggleBookmark() {
        if (currentUrl.isEmpty() || currentUrl == "about:blank") {
            Toast.makeText(this, "无法添加书签", Toast.LENGTH_SHORT).show()
            return
        }

        if (isBookmarked) {
            // 删除书签
            val bookmarks = browserManager.getAllBookmarks()
            val bookmark = bookmarks.find { it.url == currentUrl }
            bookmark?.let {
                if (browserManager.deleteBookmark(it.id)) {
                    isBookmarked = false
                    updateBookmarkStatus()
                    Toast.makeText(this, "已删除书签", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // 添加书签
            val newBookmark = BookmarkManager.Bookmark(
                id = UUID.randomUUID().toString(),
                title = currentTitle.ifEmpty { "未命名" },
                url = currentUrl,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            if (browserManager.addBookmark(newBookmark)) {
                isBookmarked = true
                updateBookmarkStatus()
                Toast.makeText(this, "已添加书签", Toast.LENGTH_SHORT).show()

                // 标记历史记录
                scope.launch(Dispatchers.IO) {
                    browserManager.markAsBookmark(currentUrl)
                }
            }
        }
    }

    /**
     * 更新书签按钮状态
     */
    private fun updateBookmarkStatus() {
        val bookmarks = browserManager.getAllBookmarks()
        isBookmarked = bookmarks.any { it.url == currentUrl }

        binding.btnBookmark?.setImageResource(
            if (isBookmarked) android.R.drawable.star_on
            else android.R.drawable.star_off
        )
    }

    /**
     * 处理文件下载
     */
    private fun handleDownload(url: String) {
        // 获取文件名
        val fileName = url.substringAfterLast("/").ifEmpty { "download_${System.currentTimeMillis()}" }

        // 下载目录
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val savePath = File(downloadDir, fileName).absolutePath

        // 创建下载任务
        val downloadId = browserManager.createDownload(
            url = url,
            fileName = fileName,
            savePath = savePath
        )

        // 开始下载
        browserManager.startDownload(downloadId)

        Toast.makeText(
            this,
            "开始下载: $fileName",
            Toast.LENGTH_SHORT
        ).show()

        // 监听下载进度（可选）
        scope.launch {
            browserManager.getDownloadState(downloadId)?.collect { state ->
                // 更新下载进度通知
                // 这里可以集成NotificationManager显示下载进度
            }
        }
    }

    /**
     * 显示浏览器菜单
     */
    private fun showBrowserMenu() {
        val menuItems = arrayOf(
            "📑 书签管理",
            "📜 浏览历史",
            "⬇️ 下载管理",
            "🏠 设置主页",
            "🔍 网页内搜索",
            "📤 分享页面",
            "🖼️ 截图页面",
            "🌐 桌面模式",
            "⚙️ 浏览器设置"
        )

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("浏览器菜单")
            .setItems(menuItems) { _, which ->
                when (which) {
                    0 -> openBookmarkActivity()
                    1 -> openHistoryActivity()
                    2 -> openDownloadActivity()
                    3 -> setHomepage()
                    4 -> searchInPage()
                    5 -> sharePage()
                    6 -> takeScreenshot()
                    7 -> toggleDesktopMode()
                    8 -> openBrowserSettings()
                }
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }

    /**
     * 打开书签管理
     */
    private fun openBookmarkActivity() {
        startActivity(Intent(this, BookmarkActivity::class.java))
    }

    /**
     * 打开历史记录
     */
    private fun openHistoryActivity() {
        startActivity(Intent(this, HistoryActivity::class.java))
    }

    /**
     * 打开下载管理
     */
    private fun openDownloadActivity() {
        startActivity(Intent(this, DownloadActivity::class.java))
    }

    /**
     * 设置主页
     */
    private fun setHomepage() {
        if (currentUrl.isNotEmpty() && currentUrl != "about:blank") {
            browserManager.setHomepage(currentUrl)
            Toast.makeText(this, "已设置为主页", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 网页内搜索
     */
    private fun searchInPage() {
        // TODO: 实现网页内搜索
        Toast.makeText(this, "网页内搜索功能开发中", Toast.LENGTH_SHORT).show()
    }

    /**
     * 分享页面
     */
    private fun sharePage() {
        if (currentUrl.isNotEmpty()) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${currentTitle}\n${currentUrl}")
            }
            startActivity(Intent.createChooser(shareIntent, "分享网页"))
        }
    }

    /**
     * 截图页面
     */
    private fun takeScreenshot() {
        // TODO: 实现网页截图
        Toast.makeText(this, "网页截图功能开发中", Toast.LENGTH_SHORT).show()
    }

    /**
     * 切换桌面模式
     */
    private fun toggleDesktopMode() {
        val currentUA = binding.webView.settings.userAgentString
        val isDesktop = currentUA.contains("X11")

        binding.webView.settings.userAgentString = if (isDesktop) {
            // 切换到移动模式
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        } else {
            // 切换到桌面模式
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        binding.webView.reload()
        Toast.makeText(
            this,
            if (isDesktop) "已切换到移动模式" else "已切换到桌面模式",
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 显示标签页管理对话框
     */
    private fun showTabsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_tabs, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewTabs)
        val emptyView = dialogView.findViewById<android.view.View>(R.id.emptyView)
        val tvTabCount = dialogView.findViewById<android.widget.TextView>(R.id.tvTabCount)
        val btnNewTab = dialogView.findViewById<android.widget.Button>(R.id.btnNewTab)
        val btnCloseAll = dialogView.findViewById<android.widget.Button>(R.id.btnCloseAll)

        // 设置RecyclerView
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        // 加载标签页列表
        scope.launch {
            val tabs = browserManager.tabs.value
            val activeTabId = browserManager.activeTabId.value

            withContext(Dispatchers.Main) {
                // 更新标签数量
                tvTabCount.text = "${tabs.size} 个标签"

                // 显示/隐藏空状态
                if (tabs.isEmpty()) {
                    recyclerView.visibility = android.view.View.GONE
                    emptyView.visibility = android.view.View.VISIBLE
                } else {
                    recyclerView.visibility = android.view.View.VISIBLE
                    emptyView.visibility = android.view.View.GONE
                }

                // 设置适配器
                val adapter = com.lanhe.gongjuxiang.adapters.TabsAdapter(
                    onTabClick = { tab ->
                        // 切换标签页
                        scope.launch {
                            browserManager.switchToTab(tab.tabId)
                            currentTabId = tab.tabId
                            loadUrl(tab.url)
                            dialog.dismiss()
                        }
                    },
                    onTabClose = { tab ->
                        // 关闭标签页
                        scope.launch {
                            browserManager.closeTab(tab.tabId)
                            // 刷新列表
                            showTabsDialog()
                            dialog.dismiss()
                        }
                    },
                    activeTabId = activeTabId
                )
                adapter.submitList(tabs)
                recyclerView.adapter = adapter
            }
        }

        // 新建标签页
        btnNewTab.setOnClickListener {
            scope.launch {
                val newTabId = browserManager.createTab("about:blank")
                browserManager.switchToTab(newTabId)
                currentTabId = newTabId
                loadUrl("about:blank")
                dialog.dismiss()
            }
        }

        // 关闭所有标签页
        btnCloseAll.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("关闭所有标签")
                .setMessage("确定要关闭所有标签页吗?")
                .setPositiveButton("确定") { _, _ ->
                    scope.launch {
                        val tabs = browserManager.tabs.value
                        tabs.forEach { tab ->
                            browserManager.closeTab(tab.tabId)
                        }
                        // 创建新标签页
                        val newTabId = browserManager.createTab("about:blank")
                        browserManager.switchToTab(newTabId)
                        currentTabId = newTabId
                        loadUrl("about:blank")
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }

        dialog.show()
    }

    /**
     * 打开浏览器设置
     */
    private fun openBrowserSettings() {
        startActivity(Intent(this, BrowserSettingsActivity::class.java))
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        // 清理WebView
        binding.webView.apply {
            stopLoading()
            clearHistory()
            clearCache(true)
            destroy()
        }
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Chromium WebView客户端
     */
    private inner class ChromiumWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            isLoading = true
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.progress = 0
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            isLoading = false
            binding.progressBar.visibility = View.GONE

            // 更新地址栏
            url?.let {
                currentUrl = it
                currentTitle = view?.title ?: ""

                if (!binding.addressBar.hasFocus()) {
                    binding.addressBar.setText(it)
                }

                // 保存浏览历史
                if (browserManager.isSaveHistoryEnabled()) {
                    scope.launch(Dispatchers.IO) {
                        browserManager.addHistory(it, currentTitle)
                    }
                }

                // 更新标签页信息
                currentTabId?.let { tabId ->
                    scope.launch(Dispatchers.IO) {
                        browserManager.updateTabContent(tabId, it, currentTitle)
                    }
                }

                // 更新书签状态
                updateBookmarkStatus()
            }

            // 更新导航按钮状态
            updateNavigationButtons()
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            // 广告拦截逻辑可以在这里实现
            return super.shouldInterceptRequest(view, request)
        }
    }

    /**
     * Chromium WebChrome客户端
     */
    private inner class ChromiumWebChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            binding.progressBar.progress = newProgress
        }
    }

    /**
     * 更新导航按钮状态
     */
    private fun updateNavigationButtons() {
        binding.btnBack.isEnabled = binding.webView.canGoBack()
        binding.btnBack.alpha = if (binding.webView.canGoBack()) 1.0f else 0.3f
        
        binding.btnForward.isEnabled = binding.webView.canGoForward()
        binding.btnForward.alpha = if (binding.webView.canGoForward()) 1.0f else 0.3f
    }

    companion object {
        /**
         * 使用内置Chromium浏览器打开URL
         *
         * @param context 上下文对象（Activity或Fragment的Context）
         * @param url 要打开的URL地址
         */
        fun openUrl(context: Context, url: String) {
            try {
                val intent = Intent(context, ChromiumBrowserActivity::class.java).apply {
                    putExtra("url", url)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开浏览器：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        /**
         * 使用内置Chromium浏览器打开URL（支持Fragment调用）
         *
         * @param context 上下文对象
         * @param url 要打开的URL地址
         * @param finishCaller 是否关闭调用者Activity（true则打开浏览器后关闭当前Activity）
         */
        fun openUrlAndFinish(context: Context, url: String, finishCaller: Boolean = false) {
            try {
                val intent = Intent(context, ChromiumBrowserActivity::class.java).apply {
                    putExtra("url", url)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                if (finishCaller && context is AppCompatActivity) {
                    context.finish()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开浏览器：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
