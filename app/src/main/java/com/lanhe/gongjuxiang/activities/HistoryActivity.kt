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
import com.lanhe.gongjuxiang.adapters.HistoryAdapter
import com.lanhe.gongjuxiang.databinding.ActivityHistoryBinding
import com.lanhe.gongjuxiang.utils.BrowserHistoryEntity
import com.lanhe.gongjuxiang.utils.BrowserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 浏览历史Activity
 * 功能：
 * 1. 展示所有浏览历史列表
 * 2. 搜索历史记录
 * 3. 按时间段筛选
 * 4. 删除历史记录
 * 5. 清空所有历史
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var browserManager: BrowserManager
    private lateinit var adapter: HistoryAdapter

    private var allHistory: List<BrowserHistoryEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        browserManager = BrowserManager.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        observeHistory()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "浏览历史"
        }
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onHistoryClick = { history ->
                // 点击历史记录，打开浏览器
                ChromiumBrowserActivity.openUrl(this, history.url)
                finish()
            },
            onHistoryLongClick = { history ->
                showHistoryOptionsDialog(history)
                true
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = this@HistoryActivity.adapter
        }
    }

    private fun observeHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            val historyList = browserManager.getRecentHistory(1000)
            withContext(Dispatchers.Main) {
                allHistory = historyList
                updateHistoryList(historyList)
            }
        }
    }

    private fun updateHistoryList(historyList: List<BrowserHistoryEntity>) {
        adapter.submitList(historyList)

        binding.emptyView.visibility = if (historyList.isEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    private fun showHistoryOptionsDialog(history: BrowserHistoryEntity) {
        val options = arrayOf(
            "打开",
            "添加到书签",
            "从历史中删除",
            "分享"
        )

        AlertDialog.Builder(this)
            .setTitle(history.title.ifEmpty { "未命名" })
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        ChromiumBrowserActivity.openUrl(this, history.url)
                        finish()
                    }
                    1 -> addToBookmark(history)
                    2 -> deleteHistory(history)
                    3 -> shareHistory(history)
                }
            }
            .show()
    }

    private fun addToBookmark(history: BrowserHistoryEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = browserManager.markAsBookmark(history.url)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@HistoryActivity, "已添加到书签", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@HistoryActivity, "添加失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteHistory(history: BrowserHistoryEntity) {
        AlertDialog.Builder(this)
            .setTitle("删除历史")
            .setMessage("确定要删除 \"${history.title.ifEmpty { history.url }}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val deleted = browserManager.deleteHistory(history.url)
                    withContext(Dispatchers.Main) {
                        if (deleted) {
                            Toast.makeText(this@HistoryActivity, "已删除", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun shareHistory(history: BrowserHistoryEntity) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                android.content.Intent.EXTRA_TEXT,
                "${history.title.ifEmpty { "网页链接" }}\n${history.url}"
            )
        }
        startActivity(android.content.Intent.createChooser(intent, "分享网页"))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "搜索历史记录"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchHistory(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    updateHistoryList(allHistory)
                } else {
                    searchHistory(newText)
                }
                return true
            }
        })

        return true
    }

    private fun searchHistory(query: String) {
        lifecycleScope.launch {
            browserManager.searchHistory(query).collectLatest { results ->
                withContext(Dispatchers.Main) {
                    updateHistoryList(results)
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
            R.id.action_clear_today -> {
                clearTodayHistory()
                true
            }
            R.id.action_clear_week -> {
                clearWeekHistory()
                true
            }
            R.id.action_clear_all -> {
                clearAllHistory()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearTodayHistory() {
        val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        confirmAndClear("今天的历史", todayStart)
    }

    private fun clearWeekHistory() {
        val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        confirmAndClear("最近7天的历史", weekStart)
    }

    private fun clearAllHistory() {
        AlertDialog.Builder(this)
            .setTitle("清空所有历史")
            .setMessage("确定要清空所有浏览历史吗？此操作不可恢复。")
            .setPositiveButton("清空") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    browserManager.clearAllHistory()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@HistoryActivity, "已清空所有历史", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmAndClear(description: String, beforeTimestamp: Long) {
        AlertDialog.Builder(this)
            .setTitle("清除历史")
            .setMessage("确定要清除${description}吗？")
            .setPositiveButton("清除") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    browserManager.clearHistoryBefore(beforeTimestamp)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@HistoryActivity, "已清除${description}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
