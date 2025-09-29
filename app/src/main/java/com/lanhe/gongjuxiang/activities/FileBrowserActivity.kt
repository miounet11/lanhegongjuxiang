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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.FileAdapter
import com.lanhe.gongjuxiang.models.FileInfo
import java.io.File

/**
 * 文件浏览器Activity
 * 用于浏览和选择本地文件
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
class FileBrowserActivity : AppCompatActivity() {

    private val TAG = FileBrowserActivity::class.java.simpleName

    // UI组件
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabSelectFile: FloatingActionButton

    // 数据
    private lateinit var fileAdapter: FileAdapter
    private val fileList = mutableListOf<FileInfo>()
    private var currentPath = Environment.getExternalStorageDirectory().absolutePath

    // 文件选择回调
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val uri = data?.data
            if (uri != null) {
                // 处理选择的文件
                handleSelectedFile(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_browser)

        Log.i(TAG, "FileBrowserActivity created")

        // 初始化UI组件
        initViews()

        // 请求权限
        requestStoragePermission()

        // 设置事件监听
        setupListeners()

        // 加载文件列表
        loadFileList(currentPath)
    }

    /**
     * 初始化UI组件
     */
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        fabSelectFile = findViewById(R.id.fabSelectFile)

        // 设置Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "文件浏览器"

        // 设置RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        fileAdapter = FileAdapter(fileList) { fileInfo ->
            handleFileClick(fileInfo)
        }
        recyclerView.adapter = fileAdapter
    }

    /**
     * 设置事件监听
     */
    private fun setupListeners() {
        fabSelectFile.setOnClickListener {
            // 打开系统文件选择器
            openSystemFilePicker()
        }
    }

    /**
     * 请求存储权限
     */
    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * 加载文件列表
     */
    private fun loadFileList(path: String) {
        try {
            val directory = File(path)
            if (directory.exists() && directory.isDirectory) {
                fileList.clear()

                // 添加父目录选项
                if (directory.parent != null) {
                    fileList.add(FileInfo(
                        name = "..",
                        path = directory.parent!!,
                        isDirectory = true,
                        size = 0,
                        lastModified = 0
                    ))
                }

                // 添加文件和文件夹
                directory.listFiles()?.forEach { file ->
                    fileList.add(FileInfo(
                        name = file.name,
                        path = file.absolutePath,
                        isDirectory = file.isDirectory,
                        size = if (file.isFile) file.length() else 0,
                        lastModified = file.lastModified()
                    ))
                }

                // 按名称排序
                fileList.sortWith(compareBy({ !it.isDirectory }, { it.name }))

                fileAdapter.notifyDataSetChanged()
                currentPath = path
                supportActionBar?.subtitle = path
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load file list", e)
            Toast.makeText(this, "无法访问目录: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理文件点击
     */
    private fun handleFileClick(fileInfo: FileInfo) {
        if (fileInfo.isDirectory) {
            // 如果是目录，进入该目录
            loadFileList(fileInfo.path)
        } else {
            // 如果是文件，打开文件
            openFile(fileInfo)
        }
    }

    /**
     * 打开文件
     */
    private fun openFile(fileInfo: FileInfo) {
        val file = File(fileInfo.path)
        val mimeType = getMimeType(fileInfo.path)

        when {
            // 图片文件
            mimeType?.startsWith("image/") == true -> {
                openImageViewer(file)
            }
            // PDF文件
            mimeType == "application/pdf" -> {
                openPdfViewer(file)
            }
            // Office文档
            isOfficeDocument(fileInfo.path) -> {
                openOfficeViewer(file)
            }
            // 文本文件
            mimeType?.startsWith("text/") == true || isTextFile(fileInfo.path) -> {
                openTextViewer(file)
            }
            else -> {
                Toast.makeText(this, "不支持的文件类型", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 打开系统文件选择器
     */
    private fun openSystemFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    /**
     * 处理选择的文件
     */
    private fun handleSelectedFile(uri: android.net.Uri) {
        // 这里可以处理用户选择的文件
        // 例如：复制到应用目录，或直接打开
        Log.d(TAG, "Selected file: $uri")
    }

    /**
     * 打开图片查看器
     */
    private fun openImageViewer(file: File) {
        val intent = Intent(this, ImageViewerActivity::class.java).apply {
            putExtra("file_path", file.absolutePath)
        }
        startActivity(intent)
    }

    /**
     * 打开PDF查看器
     */
    private fun openPdfViewer(file: File) {
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra("file_path", file.absolutePath)
        }
        startActivity(intent)
    }

    /**
     * 打开Office文档查看器
     */
    private fun openOfficeViewer(file: File) {
        val intent = Intent(this, OfficeViewerActivity::class.java).apply {
            putExtra("file_path", file.absolutePath)
        }
        startActivity(intent)
    }

    /**
     * 打开文本文件查看器
     */
    private fun openTextViewer(file: File) {
        val intent = Intent(this, TextViewerActivity::class.java).apply {
            putExtra("file_path", file.absolutePath)
        }
        startActivity(intent)
    }

    /**
     * 获取文件MIME类型
     */
    private fun getMimeType(filePath: String): String? {
        return android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(File(filePath).extension)
    }

    /**
     * 判断是否为Office文档
     */
    private fun isOfficeDocument(filePath: String): Boolean {
        val extension = File(filePath).extension.lowercase()
        return extension in listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx")
    }

    /**
     * 判断是否为文本文件
     */
    private fun isTextFile(filePath: String): Boolean {
        val extension = File(filePath).extension.lowercase()
        return extension in listOf("txt", "log", "xml", "json", "html", "css", "js", "md")
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadFileList(currentPath)
            } else {
                Toast.makeText(this, "需要存储权限才能浏览文件", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}
