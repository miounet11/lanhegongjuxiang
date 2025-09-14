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

package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.models.FileInfo

/**
 * 文件列表适配器
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
class FileAdapter(
    private val fileList: List<FileInfo>,
    private val onFileClick: (FileInfo) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileInfo = fileList[position]
        holder.bind(fileInfo)
    }

    override fun getItemCount(): Int = fileList.size

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivFileIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        private val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        private val tvFileInfo: TextView = itemView.findViewById(R.id.tvFileInfo)
        private val tvFileDate: TextView = itemView.findViewById(R.id.tvFileDate)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFileClick(fileList[position])
                }
            }
        }

        fun bind(fileInfo: FileInfo) {
            // 设置文件图标
            ivFileIcon.setImageResource(fileInfo.getIconResId())

            // 设置文件名
            tvFileName.text = fileInfo.name

            // 设置文件信息（大小或类型）
            if (fileInfo.isDirectory) {
                tvFileInfo.text = fileInfo.getFileTypeDescription()
            } else {
                tvFileInfo.text = "${fileInfo.getFileTypeDescription()} • ${fileInfo.getFormattedSize()}"
            }

            // 设置修改时间
            tvFileDate.text = fileInfo.getFormattedLastModified()
        }
    }
}
