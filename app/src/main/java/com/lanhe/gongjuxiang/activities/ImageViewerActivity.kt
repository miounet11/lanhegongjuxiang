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
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.lanhe.gongjuxiang.R
import java.io.File

/**
 * 图片查看器Activity
 * 支持图片缩放、拖拽等操作
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
class ImageViewerActivity : AppCompatActivity() {

    private val TAG = ImageViewerActivity::class.java.simpleName

    // UI组件
    private lateinit var imageView: ImageView

    // 数据
    private var imageFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        Log.i(TAG, "ImageViewerActivity created")

        // 获取图片文件路径
        val filePath = intent.getStringExtra("file_path")
        if (filePath != null) {
            imageFile = File(filePath)
        }

        // 初始化UI组件
        initViews()

        // 加载图片
        loadImage()
    }

    /**
     * 初始化UI组件
     */
    private fun initViews() {
        imageView = findViewById(R.id.imageView)

        // 设置Toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = imageFile?.name ?: "图片查看器"

        // 配置ImageView
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
    }

    /**
     * 加载图片
     */
    private fun loadImage() {
        try {
            if (imageFile != null && imageFile!!.exists()) {
                // 使用Glide加载图片
                Glide.with(this)
                    .load(imageFile)
                    .fitCenter()
                    .into(imageView)

                Log.d(TAG, "Image loaded successfully: ${imageFile!!.name}")
            } else {
                showError("图片文件不存在")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image", e)
            showError("加载图片失败: ${e.message}")
        }
    }

    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.e(TAG, message)
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
        // 清理Glide资源
        Glide.with(this).clear(imageView)
    }
}
