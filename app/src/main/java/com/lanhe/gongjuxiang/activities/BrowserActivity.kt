/*
 * Copyright 2024 LanHe Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.BookmarkAdapter
import com.lanhe.gongjuxiang.databinding.ActivityBrowserBinding
import com.lanhe.gongjuxiang.viewmodels.BrowserViewModel
import com.hippo.ehviewer.module.network.NetworkManager
import com.hippo.ehviewer.module.network.interfaces.INetworkCallback
// TODO: 暂时注释掉mokuai模块的导入，使用本地实现
// import com.hippo.ehviewer.module.bookmark.BookmarkManager
// import com.hippo.ehviewer.module.settings.SettingsManager
// import com.hippo.ehviewer.module.adblock.AdBlocker
// import com.hippo.ehviewer.module.image.ImageHelper
// import com.hippo.ehviewer.module.download.DownloadManager

/**
 * 浏览器主页Activity
 * 蓝河工具箱浏览器功能集成
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
class BrowserActivity : AppCompatActivity() {

    private val TAG = BrowserActivity::class.java.simpleName

    // View Binding
    private lateinit var binding: ActivityBrowserBinding

    // ViewModel
    private val viewModel: BrowserViewModel by viewModels()

    // 蓝河工具箱模块管理器（暂时使用本地实现）
    private lateinit var networkManager: NetworkManager
    // TODO: 实现本地书签管理器
    // private lateinit var bookmarkManager: BookmarkManager
    // private lateinit var settingsManager: SettingsManager
    // private lateinit var adBlocker: AdBlocker
    // private lateinit var imageHelper: ImageHelper
    // private lateinit var downloadManager: DownloadManager

    // UI组件
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var urlInput: TextInputEditText
    private lateinit var bookmarkRecyclerView: RecyclerView
    private lateinit var fabRefresh: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "BrowserActivity created")

        // 设置View Binding
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化蓝河工具箱模块
        initModules()

        // 初始化UI组件
        initViews()

        // 设置观察者
        setupObservers()

        // 加载初始数据
        loadInitialData()

        // 设置WebView
        setupWebView()

        // 加载默认页面
        loadDefaultPage()
    }

    /**
     * 初始化蓝河工具箱模块
     */
    private fun initModules() {
        try {
            // 初始化网络模块（使用mokuai模块）
            networkManager = NetworkManager.getInstance(this)

            // TODO: 初始化其他模块的本地实现
            // bookmarkManager = BookmarkManager.getInstance(this)
            // settingsManager = SettingsManager.getInstance(this)
            // adBlocker = AdBlocker.getInstance(this)
            // imageHelper = ImageHelper.getInstance(this)
            // downloadManager = DownloadManager.getInstance(this)

            Log.i(TAG, "Network module initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize modules", e)
            Toast.makeText(this, "模块初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 初始化UI组件
     */
    private fun initViews() {
        // 设置ActionBar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.browser_title)

        // 获取UI组件引用
        webView = binding.webView
        progressBar = binding.progressBar
        urlInput = binding.urlInput
        bookmarkRecyclerView = binding.bookmarkRecyclerView
        fabRefresh = binding.fabRefresh

        // 设置RecyclerView
        bookmarkRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // 设置点击监听器
        setupClickListeners()
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        // 刷新按钮
        fabRefresh.setOnClickListener {
            refreshCurrentPage()
        }

        // URL输入框回车键处理
        urlInput.setOnEditorActionListener { _, _, _ ->
            val url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                loadUrl(url)
            }
            true
        }
    }

    /**
     * 设置观察者
     */
    private fun setupObservers() {
        // 观察书签数据变化
        viewModel.bookmarks.observe(this, Observer { bookmarks ->
            updateBookmarkList(bookmarks)
        })

        // 观察加载进度
        viewModel.loadingProgress.observe(this, Observer { progress ->
            updateLoadingProgress(progress)
        })

        // 观察当前URL
        viewModel.currentUrl.observe(this, Observer { url ->
            updateUrlDisplay(url)
        })
    }

    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        // TODO: 加载书签数据（使用本地实现）
        viewModel.loadBookmarks()

        // 加载浏览器设置
        loadBrowserSettings()
    }

    /**
     * 设置WebView
     */
    private fun setupWebView() {
        val webSettings = webView.settings

        // 启用JavaScript
        webSettings.javaScriptEnabled = true

        // 启用缩放
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false

        // 启用文件访问
        webSettings.allowFileAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true

        // 设置缓存模式
        webSettings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

        // 设置User-Agent
        webSettings.userAgentString = networkManager.getConfig().getUserAgent()

        // 设置WebViewClient
        webView.webViewClient = createWebViewClient()

        // 设置WebChromeClient
        webView.webChromeClient = createWebChromeClient()

        // TODO: 设置广告拦截（需要实现本地AdBlocker）
        // adBlocker.enableForWebView(this)
    }

    /**
     * 创建WebViewClient
     */
    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    // TODO: 检查是否应该拦截URL（广告拦截等）
                    // if (adBlocker.shouldBlockUrl(url)) {
                    //     Log.d(TAG, "Blocked URL: $url")
                    //     return true // 拦截
                    // }

                    // 处理特殊协议
                    if (handleSpecialUrl(url)) {
                        return true
                    }
                }
                return false // 不拦截，由WebView处理
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                viewModel.setCurrentUrl(url ?: "")
                viewModel.setLoadingProgress(0)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                viewModel.setLoadingProgress(100)
            }
        }
    }

    /**
     * 创建WebChromeClient
     */
    private fun createWebChromeClient(): android.webkit.WebChromeClient {
        return object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                viewModel.setLoadingProgress(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                // 更新标题
                if (title != null && title != view?.url) {
                    supportActionBar?.title = title
                }
            }
        }
    }

    /**
     * 处理特殊URL
     */
    private fun handleSpecialUrl(url: String): Boolean {
        return when {
            url.startsWith("intent://") -> {
                // 处理Android Intent URL
                handleIntentUrl(url)
                true
            }
            url.startsWith("market://") -> {
                // 处理Google Play URL
                handleMarketUrl(url)
                true
            }
            url.startsWith("tel:") -> {
                // 处理电话URL
                handleTelUrl(url)
                true
            }
            url.startsWith("mailto:") -> {
                // 处理邮件URL
                handleMailtoUrl(url)
                true
            }
            url.startsWith("file://") -> {
                // 处理本地文件URL
                handleFileUrl(url)
                true
            }
            else -> false
        }
    }

    /**
     * 处理Intent URL
     */
    private fun handleIntentUrl(url: String) {
        try {
            val intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle intent URL", e)
            Toast.makeText(this, "无法处理此链接", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理Google Play URL
     */
    private fun handleMarketUrl(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle market URL", e)
            Toast.makeText(this, "无法打开应用市场", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理电话URL
     */
    private fun handleTelUrl(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle tel URL", e)
            Toast.makeText(this, "无法拨打电话", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理邮件URL
     */
    private fun handleMailtoUrl(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle mailto URL", e)
            Toast.makeText(this, "无法发送邮件", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理本地文件URL
     */
    private fun handleFileUrl(url: String) {
        try {
            val filePath = android.net.Uri.parse(url).path
            if (filePath != null) {
                val file = java.io.File(filePath)
                if (file.exists()) {
                    openLocalFile(file)
                } else {
                    Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle file URL", e)
            Toast.makeText(this, "无法打开文件: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 打开本地文件
     */
    private fun openLocalFile(file: java.io.File) {
        val mimeType = getMimeType(file.absolutePath)

        when {
            // 图片文件
            mimeType?.startsWith("image/") == true -> {
                val intent = android.content.Intent(this, ImageViewerActivity::class.java)
                intent.putExtra("file_path", file.absolutePath)
                startActivity(intent)
            }
            // PDF文件
            mimeType == "application/pdf" -> {
                val intent = android.content.Intent(this, PdfViewerActivity::class.java)
                intent.putExtra("file_path", file.absolutePath)
                startActivity(intent)
            }
            // Office文档
            isOfficeDocument(file.absolutePath) -> {
                val intent = android.content.Intent(this, OfficeViewerActivity::class.java)
                intent.putExtra("file_path", file.absolutePath)
                startActivity(intent)
            }
            // 文本文件
            mimeType?.startsWith("text/") == true || isTextFile(file.absolutePath) -> {
                val intent = android.content.Intent(this, TextViewerActivity::class.java)
                intent.putExtra("file_path", file.absolutePath)
                startActivity(intent)
            }
            else -> {
                // 使用系统默认应用打开
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                    intent.setDataAndType(android.net.Uri.fromFile(file), mimeType)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open file with system app", e)
                    Toast.makeText(this, "无法打开此类型的文件", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 获取文件MIME类型
     */
    private fun getMimeType(filePath: String): String? {
        return android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(java.io.File(filePath).extension)
    }

    /**
     * 判断是否为Office文档
     */
    private fun isOfficeDocument(filePath: String): Boolean {
        val extension = java.io.File(filePath).extension.lowercase()
        return extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx")
    }

    /**
     * 判断是否为文本文件
     */
    private fun isTextFile(filePath: String): Boolean {
        val extension = java.io.File(filePath).extension.lowercase()
        return extension in listOf("txt", "log", "xml", "json", "html", "css", "js", "md")
    }

    /**
     * 加载默认页面
     */
    private fun loadDefaultPage() {
        // TODO: 使用本地设置管理器获取默认URL
        val defaultUrl = "https://www.baidu.com"
        loadUrl(defaultUrl)
    }

    /**
     * 加载URL
     */
    private fun loadUrl(url: String) {
        var processedUrl = url

        // 处理URL格式
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            processedUrl = "https://$url"
        }

        // 更新输入框
        urlInput.setText(processedUrl)

        // 加载URL
        webView.loadUrl(processedUrl)

        // 记录到历史记录
        viewModel.addToHistory(processedUrl)
    }

    /**
     * 刷新当前页面
     */
    private fun refreshCurrentPage() {
        webView.reload()
    }

    /**
     * 更新书签列表
     */
    private fun updateBookmarkList(bookmarks: List<BrowserViewModel.BookmarkInfo>) {
        val adapter = BookmarkAdapter(bookmarks) { bookmark ->
            loadUrl(bookmark.url)
        }
        bookmarkRecyclerView.adapter = adapter
    }

    /**
     * 更新加载进度
     */
    private fun updateLoadingProgress(progress: Int) {
        progressBar.progress = progress
        progressBar.visibility = if (progress < 100) android.view.View.VISIBLE else android.view.View.GONE
    }

    /**
     * 更新URL显示
     */
    private fun updateUrlDisplay(url: String) {
        if (url != urlInput.text.toString()) {
            urlInput.setText(url)
        }
    }

    /**
     * 加载浏览器设置
     */
    private fun loadBrowserSettings() {
        // TODO: 应用广告拦截设置
        // val adBlockEnabled = settingsManager.getBoolean("adblock_enabled", true)
        // adBlocker.setEnabled(adBlockEnabled)

        // TODO: 应用图片加载设置
        // val imageLoadingEnabled = settingsManager.getBoolean("image_loading_enabled", true)
        // webView.settings.blockNetworkImage = !imageLoadingEnabled
    }

    /**
     * 创建选项菜单
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_browser, menu)
        return true
    }

    /**
     * 处理选项菜单选择
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_bookmark -> {
                addCurrentPageToBookmarks()
                true
            }
            R.id.action_file_browser -> {
                openFileBrowser()
                true
            }
            R.id.action_download -> {
                showDownloadDialog()
                true
            }
            R.id.action_settings -> {
                openBrowserSettings()
                true
            }
            R.id.action_history -> {
                openHistoryActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * 添加当前页面到书签
     */
    private fun addCurrentPageToBookmarks() {
        val currentUrl = webView.url
        val currentTitle = webView.title

        if (currentUrl != null && currentTitle != null) {
            // TODO: 使用本地书签管理器添加书签
            viewModel.addBookmark(currentTitle, currentUrl)
            Toast.makeText(this, "已添加到书签", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "无法获取页面信息", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 显示下载对话框
     */
    private fun showDownloadDialog() {
        // TODO: 实现下载对话框
        Toast.makeText(this, "下载功能开发中", Toast.LENGTH_SHORT).show()
    }

    /**
     * 打开浏览器设置
     */
    private fun openBrowserSettings() {
        val intent = Intent(this, BrowserSettingsActivity::class.java)
        startActivity(intent)
    }

    /**
     * 打开历史记录Activity
     */
    private fun openHistoryActivity() {
        // TODO: 实现历史记录Activity
        Toast.makeText(this, "历史记录功能开发中", Toast.LENGTH_SHORT).show()
    }

    /**
     * 打开文件浏览器
     */
    private fun openFileBrowser() {
        try {
            val intent = Intent(this, FileBrowserActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file browser", e)
            Toast.makeText(this, "无法打开文件浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理返回键
     */
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Activity销毁时的清理
     */
    override fun onDestroy() {
        super.onDestroy()

        // 清理WebView
        webView.destroy()

        // 清理模块资源
        try {
            networkManager.cleanup()
            // TODO: 清理其他模块资源
            // bookmarkManager.cleanup()
            // adBlocker.cleanup()
            // imageHelper.cleanup()
            // downloadManager.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up modules", e)
        }
    }

    /**
     * 处理Activity结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 处理文件选择等结果
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && resultCode == RESULT_OK) {
            // 处理文件选择结果
            val result = android.webkit.WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            // TODO: 处理文件选择结果
        }
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST_CODE = 1001
    }
}
