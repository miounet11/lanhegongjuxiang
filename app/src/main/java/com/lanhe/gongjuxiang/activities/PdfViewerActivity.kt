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

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lanhe.gongjuxiang.R
import java.io.File

/**
 * PDF查看器Activity
 * 使用WebView显示PDF文件
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
class PdfViewerActivity : AppCompatActivity() {

    private val TAG = PdfViewerActivity::class.java.simpleName

    // UI组件
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    // 数据
    private var pdfFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        Log.i(TAG, "PdfViewerActivity created")

        // 获取PDF文件路径
        val filePath = intent.getStringExtra("file_path")
        if (filePath != null) {
            pdfFile = File(filePath)
        }

        // 初始化UI组件
        initViews()

        // 加载PDF文件
        loadPdfFile()
    }

    /**
     * 初始化UI组件
     */
    private fun initViews() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)

        // 设置Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = pdfFile?.name ?: "PDF查看器"

        // 配置WebView
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        // 设置WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = android.view.View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = android.view.View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@PdfViewerActivity, "加载PDF失败", Toast.LENGTH_SHORT).show()
            }
        }

        // 设置WebChromeClient
        webView.webChromeClient = android.webkit.WebChromeClient()
    }

    /**
     * 加载PDF文件
     */
    private fun loadPdfFile() {
        try {
            if (pdfFile != null && pdfFile!!.exists()) {
                // 使用Google Docs查看器或直接加载
                val pdfUrl = pdfFile!!.absolutePath
                val googleDocsUrl = "https://docs.google.com/viewer?url=file://$pdfUrl"

                Log.d(TAG, "Loading PDF: $pdfUrl")
                webView.loadUrl(googleDocsUrl)
            } else {
                showError("PDF文件不存在")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load PDF file", e)
            showError("加载PDF文件失败: ${e.message}")
        }
    }

    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, message)
        progressBar.visibility = android.view.View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理WebView
        webView.destroy()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
