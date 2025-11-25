package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityBrowserSettingsBinding
import com.lanhe.gongjuxiang.utils.BrowserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 浏览器设置Activity
 */
class BrowserSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserSettingsBinding
    private lateinit var browserManager: BrowserManager

    // 搜索引擎列表
    private val searchEngines = mapOf(
        "百度" to "https://www.baidu.com/s?wd=",
        "Google" to "https://www.google.com/search?q=",
        "必应" to "https://www.bing.com/search?q=",
        "搜狗" to "https://www.sogou.com/web?query=",
        "360搜索" to "https://www.so.com/s?q="
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        browserManager = BrowserManager.getInstance(this)

        setupToolbar()
        setupSearchEngine()
        setupHomepage()
        setupPrivacySettings()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSearchEngine() {
        val engineNames = searchEngines.keys.toList()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            engineNames
        )
        binding.spinnerSearchEngine.adapter = adapter

        // 恢复当前设置
        val currentEngine = browserManager.getSearchEngine()
        val currentIndex = searchEngines.values.indexOf(currentEngine)
        if (currentIndex >= 0) {
            binding.spinnerSearchEngine.setSelection(currentIndex)
        }

        // 监听变更
        binding.spinnerSearchEngine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedEngine = searchEngines.values.toList()[position]
                browserManager.setSearchEngine(selectedEngine)
                Toast.makeText(this@BrowserSettingsActivity, "已切换搜索引擎", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupHomepage() {
        // 恢复当前主页
        binding.etHomepage.setText(browserManager.getHomepage())

        // 失去焦点时保存
        binding.etHomepage.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val homepage = binding.etHomepage.text.toString().trim()
                if (homepage.isNotEmpty()) {
                    browserManager.setHomepage(homepage)
                    Toast.makeText(this, "已保存主页设置", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupPrivacySettings() {
        // 保存历史记录开关
        binding.switchSaveHistory.isChecked = browserManager.isSaveHistoryEnabled()
        binding.switchSaveHistory.setOnCheckedChangeListener { _, isChecked ->
            browserManager.setSaveHistoryEnabled(isChecked)
            val message = if (isChecked) "已启用历史记录" else "已禁用历史记录"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // 广告拦截开关
        binding.switchAdBlock.isChecked = browserManager.isAdBlockEnabled()
        binding.switchAdBlock.setOnCheckedChangeListener { _, isChecked ->
            browserManager.setAdBlockEnabled(isChecked)
            val message = if (isChecked) "已启用广告拦截" else "已禁用广告拦截"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // 清除历史记录
        binding.btnClearHistory.setOnClickListener {
            showClearHistoryDialog()
        }

        // 清除缓存
        binding.btnClearCache.setOnClickListener {
            showClearCacheDialog()
        }
    }

    private fun showClearHistoryDialog() {
        AlertDialog.Builder(this)
            .setTitle("清除浏览历史")
            .setMessage("确定要清除所有浏览历史记录吗?此操作不可恢复。")
            .setPositiveButton("清除") { _, _ ->
                clearHistory()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            browserManager.clearAllHistory()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@BrowserSettingsActivity, "已清除浏览历史", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showClearCacheDialog() {
        AlertDialog.Builder(this)
            .setTitle("清除缓存数据")
            .setMessage("确定要清除所有缓存数据吗?这将清除网页缓存、Cookie等数据。")
            .setPositiveButton("清除") { _, _ ->
                clearCache()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearCache() {
        // 清除WebView缓存
        cacheDir.deleteRecursively()

        Toast.makeText(this, "已清除缓存数据", Toast.LENGTH_SHORT).show()
    }
}
