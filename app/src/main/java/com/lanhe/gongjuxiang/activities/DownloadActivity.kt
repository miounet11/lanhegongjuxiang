package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.DownloadAdapter
import com.lanhe.gongjuxiang.databinding.ActivityDownloadBinding
import com.lanhe.gongjuxiang.utils.BrowserDownloadEntity
import com.lanhe.gongjuxiang.utils.BrowserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 下载管理Activity
 * 功能：
 * 1. 展示所有下载任务列表
 * 2. 下载进度实时更新
 * 3. 暂停/继续/取消下载
 * 4. 打开已完成的文件
 * 5. 清除下载记录
 */
class DownloadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadBinding
    private lateinit var browserManager: BrowserManager
    private lateinit var adapter: DownloadAdapter

    private var allDownloads: List<BrowserDownloadEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        browserManager = BrowserManager.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        observeDownloads()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "下载管理"
        }
    }

    private fun setupRecyclerView() {
        adapter = DownloadAdapter(
            onDownloadClick = { download ->
                handleDownloadClick(download)
            },
            onDownloadLongClick = { download ->
                showDownloadOptionsDialog(download)
                true
            },
            onPauseClick = { download ->
                pauseDownload(download)
            },
            onResumeClick = { download ->
                resumeDownload(download)
            },
            onCancelClick = { download ->
                cancelDownload(download)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@DownloadActivity)
            adapter = this@DownloadActivity.adapter
        }
    }

    private fun observeDownloads() {
        lifecycleScope.launch {
            browserManager.getAllDownloads().collectLatest { downloadList ->
                withContext(Dispatchers.Main) {
                    allDownloads = downloadList
                    updateDownloadList(downloadList)
                }
            }
        }
    }

    private fun updateDownloadList(downloadList: List<BrowserDownloadEntity>) {
        adapter.submitList(downloadList)

        binding.emptyView.visibility = if (downloadList.isEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    private fun handleDownloadClick(download: BrowserDownloadEntity) {
        when (download.status) {
            "completed" -> openDownloadedFile(download)
            "failed" -> retryDownload(download)
            "downloading", "paused" -> showDownloadDetails(download)
            else -> Toast.makeText(this, "下载状态：${download.status}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDownloadOptionsDialog(download: BrowserDownloadEntity) {
        val options = when (download.status) {
            "completed" -> arrayOf("打开文件", "分享", "删除记录")
            "downloading" -> arrayOf("暂停下载", "取消下载", "查看详情")
            "paused" -> arrayOf("继续下载", "取消下载", "查看详情")
            "failed" -> arrayOf("重试下载", "删除记录", "查看错误")
            else -> arrayOf("删除记录")
        }

        AlertDialog.Builder(this)
            .setTitle(download.fileName)
            .setItems(options) { _, which ->
                when (download.status) {
                    "completed" -> {
                        when (which) {
                            0 -> openDownloadedFile(download)
                            1 -> shareDownload(download)
                            2 -> deleteDownload(download)
                        }
                    }
                    "downloading" -> {
                        when (which) {
                            0 -> pauseDownload(download)
                            1 -> cancelDownload(download)
                            2 -> showDownloadDetails(download)
                        }
                    }
                    "paused" -> {
                        when (which) {
                            0 -> resumeDownload(download)
                            1 -> cancelDownload(download)
                            2 -> showDownloadDetails(download)
                        }
                    }
                    "failed" -> {
                        when (which) {
                            0 -> retryDownload(download)
                            1 -> deleteDownload(download)
                            2 -> showDownloadError(download)
                        }
                    }
                    else -> {
                        if (which == 0) deleteDownload(download)
                    }
                }
            }
            .show()
    }

    private fun openDownloadedFile(download: BrowserDownloadEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = browserManager.openDownloadedFile(download.downloadId)
            withContext(Dispatchers.Main) {
                if (!success) {
                    Toast.makeText(this@DownloadActivity, "无法打开文件", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun pauseDownload(download: BrowserDownloadEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            browserManager.pauseDownload(download.downloadId)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DownloadActivity, "已暂停下载", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resumeDownload(download: BrowserDownloadEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            browserManager.startDownload(download.downloadId)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DownloadActivity, "继续下载", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelDownload(download: BrowserDownloadEntity) {
        AlertDialog.Builder(this)
            .setTitle("取消下载")
            .setMessage("确定要取消下载 \"${download.fileName}\" 吗？")
            .setPositiveButton("取消下载") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    browserManager.cancelDownload(download.downloadId)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DownloadActivity, "已取消下载", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("返回", null)
            .show()
    }

    private fun retryDownload(download: BrowserDownloadEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            browserManager.retryDownload(download.downloadId)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DownloadActivity, "重新开始下载", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteDownload(download: BrowserDownloadEntity) {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定要删除 \"${download.fileName}\" 的下载记录吗？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val deleted = browserManager.deleteDownload(download.downloadId)
                    withContext(Dispatchers.Main) {
                        if (deleted) {
                            Toast.makeText(this@DownloadActivity, "已删除", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun shareDownload(download: BrowserDownloadEntity) {
        // TODO: 实现文件分享功能
        Toast.makeText(this, "文件分享功能开发中", Toast.LENGTH_SHORT).show()
    }

    private fun showDownloadDetails(download: BrowserDownloadEntity) {
        val details = """
            文件名：${download.fileName}
            URL：${download.url}
            保存路径：${download.filePath}
            文件大小：${formatFileSize(download.fileSize)}
            已下载：${formatFileSize(download.downloadedSize)}
            进度：${download.getProgress().toInt()}%
            状态：${download.status}
            创建时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(download.createTime))}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("下载详情")
            .setMessage(details)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showDownloadError(download: BrowserDownloadEntity) {
        AlertDialog.Builder(this)
            .setTitle("下载错误")
            .setMessage("下载失败\n\n重试次数：${download.retryCount}\n\n建议：检查网络连接或存储空间")
            .setPositiveButton("重试", { _, _ ->
                retryDownload(download)
            })
            .setNegativeButton("取消", null)
            .show()
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.2f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024))
            else -> String.format("%.2f GB", size / (1024.0 * 1024 * 1024))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_download, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "搜索下载"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchDownloads(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    updateDownloadList(allDownloads)
                } else {
                    searchDownloads(newText)
                }
                return true
            }
        })

        return true
    }

    private fun searchDownloads(query: String) {
        lifecycleScope.launch {
            browserManager.searchDownloads(query).collectLatest { results ->
                withContext(Dispatchers.Main) {
                    updateDownloadList(results)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_clear_completed -> {
                clearCompletedDownloads()
                true
            }
            R.id.action_clear_all -> {
                clearAllDownloads()
                true
            }
            R.id.action_pause_all -> {
                pauseAllDownloads()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearCompletedDownloads() {
        AlertDialog.Builder(this)
            .setTitle("清除已完成")
            .setMessage("确定要清除所有已完成的下载记录吗？")
            .setPositiveButton("清除") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    browserManager.clearCompletedDownloads()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DownloadActivity, "已清除完成的下载", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearAllDownloads() {
        AlertDialog.Builder(this)
            .setTitle("清空所有下载")
            .setMessage("确定要清空所有下载记录吗？此操作不可恢复。")
            .setPositiveButton("清空") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    browserManager.clearAllDownloads()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DownloadActivity, "已清空所有下载", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun pauseAllDownloads() {
        lifecycleScope.launch(Dispatchers.IO) {
            browserManager.pauseAllDownloads()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DownloadActivity, "已暂停所有下载", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 刷新下载列表
        observeDownloads()
    }
}
