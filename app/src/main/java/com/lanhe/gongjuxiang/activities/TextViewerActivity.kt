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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lanhe.gongjuxiang.R
import java.io.File

/**
 * 文本文件查看器Activity
 * 支持查看各种文本文件内容
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
class TextViewerActivity : AppCompatActivity() {

    private val TAG = TextViewerActivity::class.java.simpleName

    // UI组件
    private lateinit var scrollView: ScrollView
    private lateinit var textView: TextView

    // 数据
    private var textFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_viewer)

        Log.i(TAG, "TextViewerActivity created")

        // 获取文本文件路径
        val filePath = intent.getStringExtra("file_path")
        if (filePath != null) {
            textFile = File(filePath)
        }

        // 初始化UI组件
        initViews()

        // 加载文本内容
        loadTextContent()
    }

    /**
     * 初始化UI组件
     */
    private fun initViews() {
        scrollView = findViewById(R.id.scrollView)
        textView = findViewById(R.id.textView)

        // 设置Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = textFile?.name ?: "文本查看器"

        // 配置TextView
        textView.setTextIsSelectable(true)
        textView.setHorizontallyScrolling(true)
    }

    /**
     * 加载文本内容
     */
    private fun loadTextContent() {
        try {
            if (textFile != null && textFile!!.exists()) {
                val content = textFile!!.readText(Charsets.UTF_8)
                textView.text = content
                Log.d(TAG, "Text file loaded successfully: ${textFile!!.name}")
            } else {
                showError("文本文件不存在")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load text file", e)
            // 尝试其他编码
            try {
                if (textFile != null && textFile!!.exists()) {
                    val content = textFile!!.readText(Charsets.ISO_8859_1)
                    textView.text = content
                    Log.d(TAG, "Text file loaded with ISO-8859-1 encoding: ${textFile!!.name}")
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to load text file with alternative encoding", e2)
                showError("加载文本文件失败: ${e.message}")
            }
        }
    }

    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, message)
        textView.text = "错误: $message"
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
}
