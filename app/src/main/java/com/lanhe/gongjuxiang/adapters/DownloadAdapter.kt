package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.databinding.ItemDownloadBinding
import com.lanhe.gongjuxiang.utils.BrowserDownloadEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 下载列表适配器
 */
class DownloadAdapter(
    private val onDownloadClick: (BrowserDownloadEntity) -> Unit,
    private val onDownloadLongClick: (BrowserDownloadEntity) -> Boolean,
    private val onPauseClick: (BrowserDownloadEntity) -> Unit,
    private val onResumeClick: (BrowserDownloadEntity) -> Unit,
    private val onCancelClick: (BrowserDownloadEntity) -> Unit
) : ListAdapter<BrowserDownloadEntity, DownloadAdapter.DownloadViewHolder>(DownloadDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadViewHolder {
        val binding = ItemDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DownloadViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DownloadViewHolder(
        private val binding: ItemDownloadBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(download: BrowserDownloadEntity) {
            binding.apply {
                // 文件名
                downloadFileName.text = download.fileName

                // URL
                downloadUrl.text = download.url

                // 文件大小
                downloadFileSize.text = formatFileSize(download.fileSize)

                // 进度
                val progress = download.getProgress()
                downloadProgressBar.progress = progress.toInt()
                downloadProgressText.text = "${progress.toInt()}%"

                // 下载速度和剩余时间（仅下载中显示）
                if (download.status == "downloading") {
                    downloadSpeed.visibility = View.VISIBLE
                    downloadSpeed.text = "下载中..."
                } else {
                    downloadSpeed.visibility = View.GONE
                }

                // 状态
                downloadStatus.text = when (download.status) {
                    "pending" -> "等待中"
                    "downloading" -> "下载中"
                    "paused" -> "已暂停"
                    "completed" -> "已完成"
                    "failed" -> "失败"
                    "cancelled" -> "已取消"
                    else -> download.status
                }

                // 状态颜色
                val statusColor = when (download.status) {
                    "downloading" -> android.graphics.Color.parseColor("#4CAF50")
                    "completed" -> android.graphics.Color.parseColor("#2196F3")
                    "failed" -> android.graphics.Color.parseColor("#F44336")
                    "paused" -> android.graphics.Color.parseColor("#FF9800")
                    else -> android.graphics.Color.parseColor("#9E9E9E")
                }
                downloadStatus.setTextColor(statusColor)

                // 时间
                val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                downloadTime.text = dateFormat.format(Date(download.createTime))

                // 按钮显示逻辑
                when (download.status) {
                    "downloading" -> {
                        btnPause.visibility = View.VISIBLE
                        btnResume.visibility = View.GONE
                        btnCancel.visibility = View.VISIBLE
                        downloadProgressBar.visibility = View.VISIBLE
                        downloadProgressText.visibility = View.VISIBLE
                    }
                    "paused" -> {
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.VISIBLE
                        btnCancel.visibility = View.VISIBLE
                        downloadProgressBar.visibility = View.VISIBLE
                        downloadProgressText.visibility = View.VISIBLE
                    }
                    "completed" -> {
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.GONE
                        btnCancel.visibility = View.GONE
                        downloadProgressBar.visibility = View.GONE
                        downloadProgressText.visibility = View.GONE
                    }
                    "failed" -> {
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.VISIBLE // 显示为重试按钮
                        btnCancel.visibility = View.VISIBLE
                        downloadProgressBar.visibility = View.VISIBLE
                        downloadProgressText.visibility = View.VISIBLE
                    }
                    else -> {
                        btnPause.visibility = View.GONE
                        btnResume.visibility = View.GONE
                        btnCancel.visibility = View.GONE
                        downloadProgressBar.visibility = View.GONE
                        downloadProgressText.visibility = View.GONE
                    }
                }

                // 按钮点击事件
                btnPause.setOnClickListener {
                    onPauseClick(download)
                }

                btnResume.setOnClickListener {
                    if (download.status == "failed") {
                        // 失败状态下显示为重试
                        onResumeClick(download)
                    } else {
                        onResumeClick(download)
                    }
                }

                btnCancel.setOnClickListener {
                    onCancelClick(download)
                }

                // 整体点击事件
                root.setOnClickListener {
                    onDownloadClick(download)
                }

                root.setOnLongClickListener {
                    onDownloadLongClick(download)
                }
            }
        }

        private fun formatFileSize(size: Long): String {
            return when {
                size == 0L -> "未知大小"
                size < 1024 -> "$size B"
                size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
                size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024))
                else -> String.format("%.2f GB", size / (1024.0 * 1024 * 1024))
            }
        }
    }

    class DownloadDiffCallback : DiffUtil.ItemCallback<BrowserDownloadEntity>() {
        override fun areItemsTheSame(
            oldItem: BrowserDownloadEntity,
            newItem: BrowserDownloadEntity
        ): Boolean {
            return oldItem.downloadId == newItem.downloadId
        }

        override fun areContentsTheSame(
            oldItem: BrowserDownloadEntity,
            newItem: BrowserDownloadEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
