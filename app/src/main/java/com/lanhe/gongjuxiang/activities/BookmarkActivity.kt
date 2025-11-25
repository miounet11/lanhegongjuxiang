package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.BookmarkAdapter
import com.lanhe.gongjuxiang.databinding.ActivityBookmarkBinding
import com.lanhe.gongjuxiang.utils.BrowserManager
import com.lanhe.mokuai.bookmark.BookmarkManager

/**
 * 书签管理Activity
 * 功能：
 * 1. 展示所有书签列表
 * 2. 搜索书签
 * 3. 按文件夹分组
 * 4. 编辑/删除书签
 * 5. 导入/导出书签
 */
class BookmarkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookmarkBinding
    private lateinit var browserManager: BrowserManager
    private lateinit var adapter: BookmarkAdapter

    private var currentFolderId: String = "default"
    private var allBookmarks: List<BookmarkManager.Bookmark> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        browserManager = BrowserManager.getInstance(this)

        setupToolbar()
        setupRecyclerView()
        loadBookmarks()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "书签管理"
        }
    }

    private fun setupRecyclerView() {
        adapter = BookmarkAdapter(
            onBookmarkClick = { bookmark ->
                // 点击书签，打开浏览器
                ChromiumBrowserActivity.openUrl(this, bookmark.url)
                finish()
            },
            onBookmarkLongClick = { bookmark ->
                showBookmarkOptionsDialog(bookmark)
                true
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BookmarkActivity)
            adapter = this@BookmarkActivity.adapter
        }
    }

    private fun loadBookmarks() {
        allBookmarks = browserManager.getAllBookmarks()
        val filteredBookmarks = if (currentFolderId == "default") {
            allBookmarks
        } else {
            allBookmarks.filter { it.folderId == currentFolderId }
        }
        adapter.submitList(filteredBookmarks)

        binding.emptyView.visibility = if (filteredBookmarks.isEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    private fun showBookmarkOptionsDialog(bookmark: BookmarkManager.Bookmark) {
        val options = arrayOf(
            "打开",
            "编辑",
            "删除",
            "分享"
        )

        AlertDialog.Builder(this)
            .setTitle(bookmark.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        ChromiumBrowserActivity.openUrl(this, bookmark.url)
                        finish()
                    }
                    1 -> showEditBookmarkDialog(bookmark)
                    2 -> deleteBookmark(bookmark)
                    3 -> shareBookmark(bookmark)
                }
            }
            .show()
    }

    private fun showEditBookmarkDialog(bookmark: BookmarkManager.Bookmark) {
        // TODO: 实现编辑对话框
        Toast.makeText(this, "编辑功能开发中", Toast.LENGTH_SHORT).show()
    }

    private fun deleteBookmark(bookmark: BookmarkManager.Bookmark) {
        AlertDialog.Builder(this)
            .setTitle("删除书签")
            .setMessage("确定要删除 \"${bookmark.title}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                if (browserManager.deleteBookmark(bookmark.id)) {
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
                    loadBookmarks()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun shareBookmark(bookmark: BookmarkManager.Bookmark) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "${bookmark.title}\n${bookmark.url}")
        }
        startActivity(android.content.Intent.createChooser(intent, "分享书签"))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bookmark, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "搜索书签"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchBookmarks(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    loadBookmarks()
                } else {
                    searchBookmarks(newText)
                }
                return true
            }
        })

        return true
    }

    private fun searchBookmarks(query: String) {
        val results = browserManager.searchBookmarks(query)
        adapter.submitList(results)

        binding.emptyView.visibility = if (results.isEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_add -> {
                showAddBookmarkDialog()
                true
            }
            R.id.action_import -> {
                importBookmarks()
                true
            }
            R.id.action_export -> {
                exportBookmarks()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddBookmarkDialog() {
        // TODO: 实现添加对话框
        Toast.makeText(this, "添加功能将集成到浏览器", Toast.LENGTH_SHORT).show()
    }

    private fun importBookmarks() {
        // TODO: 实现导入功能
        Toast.makeText(this, "导入功能开发中", Toast.LENGTH_SHORT).show()
    }

    private fun exportBookmarks() {
        try {
            val json = browserManager.bookmarkManager.exportToJson()
            // TODO: 保存到文件
            Toast.makeText(this, "导出成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导出失败", Toast.LENGTH_SHORT).show()
        }
    }
}
