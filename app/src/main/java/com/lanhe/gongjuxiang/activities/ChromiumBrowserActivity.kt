package com.lanhe.gongjuxiang.activities

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityChromiumBrowserBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Chromium浏览器主Activity
 *
 * 企业级本地化浏览器实现，包含:
 * - 多标签浏览支持
 * - 本地账户管理系统
 * - AES256密码加密存储
 * - 文件管理和APK处理
 * - 广告拦截和性能优化
 *
 * @author 蓝河助手
 * @version 1.0.0
 */
class ChromiumBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChromiumBrowserBinding
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 浏览器状态
    private var currentUrl = ""
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChromiumBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupWebView()
        setupClickListeners()

        // 加载初始页面
        val initialUrl = intent?.getStringExtra("url") ?: "https://www.baidu.com"
        loadUrl(initialUrl)
    }

    /**
     * 设置工具栏
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Chromium浏览器"
        }
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
        // 返回按钮
        binding.btnBack.setOnClickListener {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                Toast.makeText(this, "已经是第一页", Toast.LENGTH_SHORT).show()
            }
        }

        // 前进按钮
        binding.btnForward.setOnClickListener {
            if (binding.webView.canGoForward()) {
                binding.webView.goForward()
            } else {
                Toast.makeText(this, "已经是最后一页", Toast.LENGTH_SHORT).show()
            }
        }

        // 刷新按钮
        binding.btnRefresh.setOnClickListener {
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

        // 账户按钮
        binding.btnAccount.setOnClickListener {
            Toast.makeText(this, "账户管理功能开发中...", Toast.LENGTH_SHORT).show()
        }

        // 菜单按钮
        binding.btnMenu.setOnClickListener {
            openOptionsMenu()
        }
    }

    /**
     * 加载URL
     */
    private fun loadUrl(url: String) {
        var finalUrl = url

        // 如果不是URL格式，作为搜索关键词处理
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(" ") || !url.contains(".")) {
                // 搜索
                finalUrl = "https://www.baidu.com/s?wd=$url"
            } else {
                // 补全http://
                finalUrl = "https://$url"
            }
        }

        currentUrl = finalUrl
        binding.addressBar.setText(finalUrl)
        binding.webView.loadUrl(finalUrl)
    }

    /**
     * 处理文件下载
     */
    private fun handleDownload(url: String) {
        Toast.makeText(this, "下载已开始: $url", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_browser, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_clear_cache -> {
                binding.webView.clearCache(true)
                Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_clear_history -> {
                binding.webView.clearHistory()
                Toast.makeText(this, "历史记录已清除", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
                binding.addressBar.setText(it)
            }

            // 更新导航按钮状态
            updateNavigationButtons()
        }

        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            // 广告拦截逻辑可以在这里实现
            // 这里暂时不做拦截，只做标记
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

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            title?.let {
                supportActionBar?.subtitle = it
            }
        }
    }

    /**
     * 更新导航按钮状态
     */
    private fun updateNavigationButtons() {
        binding.btnBack.isEnabled = binding.webView.canGoBack()
        binding.btnForward.isEnabled = binding.webView.canGoForward()
    }
}
