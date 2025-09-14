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

package com.lanhe.gongjuxiang.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 文件信息模型类
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
data class FileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
) {

    /**
     * 获取格式化的文件大小
     */
    fun getFormattedSize(): String {
        if (isDirectory) return ""

        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * 获取格式化的修改时间
     */
    fun getFormattedLastModified(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(lastModified))
    }

    /**
     * 获取文件图标资源ID
     */
    fun getIconResId(): Int {
        return if (isDirectory) {
            android.R.drawable.ic_menu_more // 文件夹图标
        } else {
            getFileIconResId()
        }
    }

    /**
     * 根据文件类型获取图标资源ID
     */
    private fun getFileIconResId(): Int {
        val extension = name.substringAfterLast(".", "").lowercase()

        return when (extension) {
            // 图片文件
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> android.R.drawable.ic_menu_gallery
            // 视频文件
            "mp4", "avi", "mkv", "mov", "wmv", "flv" -> android.R.drawable.ic_media_play
            // 音频文件
            "mp3", "wav", "flac", "aac", "ogg" -> android.R.drawable.ic_lock_silent_mode
            // 文档文件
            "pdf" -> android.R.drawable.ic_menu_agenda
            "doc", "docx" -> android.R.drawable.ic_menu_edit
            "xls", "xlsx" -> android.R.drawable.ic_menu_sort_alphabetically
            "ppt", "pptx" -> android.R.drawable.ic_menu_slideshow
            "txt" -> android.R.drawable.ic_menu_info_details
            "html", "htm" -> android.R.drawable.ic_menu_view
            "xml" -> android.R.drawable.ic_menu_preferences
            "json" -> android.R.drawable.ic_menu_manage
            "css" -> android.R.drawable.ic_menu_set_as
            "js" -> android.R.drawable.ic_menu_call
            "java", "kt" -> android.R.drawable.ic_menu_compass
            "zip", "rar", "7z", "tar", "gz" -> android.R.drawable.ic_menu_save
            "apk" -> android.R.drawable.ic_menu_add
            else -> android.R.drawable.ic_menu_help
        }
    }

    /**
     * 获取文件类型描述
     */
    fun getFileTypeDescription(): String {
        if (isDirectory) return "文件夹"

        val extension = name.substringAfterLast(".", "").lowercase()

        return when (extension) {
            // 图片文件
            "jpg", "jpeg" -> "JPEG图片"
            "png" -> "PNG图片"
            "gif" -> "GIF图片"
            "bmp" -> "BMP图片"
            "webp" -> "WebP图片"
            // 视频文件
            "mp4" -> "MP4视频"
            "avi" -> "AVI视频"
            "mkv" -> "MKV视频"
            "mov" -> "MOV视频"
            "wmv" -> "WMV视频"
            "flv" -> "FLV视频"
            // 音频文件
            "mp3" -> "MP3音频"
            "wav" -> "WAV音频"
            "flac" -> "FLAC音频"
            "aac" -> "AAC音频"
            "ogg" -> "OGG音频"
            // 文档文件
            "pdf" -> "PDF文档"
            "doc", "docx" -> "Word文档"
            "xls", "xlsx" -> "Excel表格"
            "ppt", "pptx" -> "PowerPoint演示文稿"
            "txt" -> "文本文件"
            "html", "htm" -> "HTML网页"
            "xml" -> "XML文件"
            "json" -> "JSON文件"
            "css" -> "CSS样式表"
            "js" -> "JavaScript脚本"
            "java" -> "Java源文件"
            "kt" -> "Kotlin源文件"
            // 压缩文件
            "zip" -> "ZIP压缩包"
            "rar" -> "RAR压缩包"
            "7z" -> "7Z压缩包"
            "tar" -> "TAR压缩包"
            "gz" -> "GZ压缩包"
            "apk" -> "Android应用包"
            else -> "文件"
        }
    }
}
