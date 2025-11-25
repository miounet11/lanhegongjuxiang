package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.util.Log
import com.lanhe.mokuai.bookmark.BookmarkManager
import com.lanhe.mokuai.download.DownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 浏览器核心管理器
 * 统一管理书签、历史、标签页、下载等浏览器核心功能
 *
 * 采用单例模式，确保全局唯一实例
 * 使用协程进行异步数据库操作
 *
 * 主要职责：
 * 1. 标签页生命周期管理（创建、切换、关闭）
 * 2. 历史记录管理（记录、查询、清除）
 * 3. 书签管理集成（BookmarkManager代理）
 * 4. 下载管理集成（DownloadManager代理）
 * 5. 浏览器设置管理
 */
class BrowserManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BrowserManager"

        @Volatile
        private var INSTANCE: BrowserManager? = null

        /**
         * 获取单例实例
         */
        fun getInstance(context: Context): BrowserManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BrowserManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // 协程作用域
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 数据库DAO
    private val database = AppDatabase.getDatabase(context)
    private val historyDao = database.browserHistoryDao()
    private val tabDao = database.browserTabDao()
    private val downloadDao = database.browserDownloadDao()

    // 模块集成
    val bookmarkManager = BookmarkManager(context)
    val downloadManager = DownloadManager(context)

    // 当前标签页列表（StateFlow，支持响应式订阅）
    private val _tabs = MutableStateFlow<List<BrowserTabEntity>>(emptyList())
    val tabs: StateFlow<List<BrowserTabEntity>> = _tabs.asStateFlow()

    // 当前活跃标签页ID
    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    init {
        // 初始化时加载标签页
        loadTabsFromDatabase()
    }

    // ================== 标签页管理 ==================

    /**
     * 从数据库加载标签页
     */
    private fun loadTabsFromDatabase() {
        scope.launch {
            try {
                val normalTabs = tabDao.getNormalTabs()
                _tabs.value = normalTabs

                // 恢复活跃标签页
                val activeTab = tabDao.getActiveTab()
                _activeTabId.value = activeTab?.tabId
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load tabs from database", e)
            }
        }
    }

    /**
     * 创建新标签页
     * @param url 初始URL
     * @param isIncognito 是否为隐私模式
     * @return 新标签页的ID
     */
    suspend fun createTab(url: String = "about:blank", isIncognito: Boolean = false): String {
        val tabId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()

        val newTab = BrowserTabEntity(
            tabId = tabId,
            url = url,
            title = "",
            createTime = currentTime,
            lastAccessTime = currentTime,
            isActive = false,
            isIncognito = isIncognito
        )

        tabDao.insertTab(newTab)

        // 更新内存中的标签页列表
        if (!isIncognito) {
            _tabs.value = _tabs.value + newTab
        }

        Log.d(TAG, "Created new tab: $tabId, url: $url, incognito: $isIncognito")
        return tabId
    }

    /**
     * 切换到指定标签页
     */
    suspend fun switchToTab(tabId: String) {
        try {
            // 清除旧的活跃状态
            tabDao.clearActiveStatus()

            // 设置新的活跃标签页
            val currentTime = System.currentTimeMillis()
            tabDao.setTabActive(tabId, currentTime)

            _activeTabId.value = tabId

            Log.d(TAG, "Switched to tab: $tabId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch tab", e)
        }
    }

    /**
     * 关闭标签页
     */
    suspend fun closeTab(tabId: String) {
        try {
            val tab = tabDao.getTabById(tabId)
            if (tab != null) {
                tabDao.deleteTab(tab)

                // 更新内存列表
                _tabs.value = _tabs.value.filter { it.tabId != tabId }

                // 如果关闭的是活跃标签页，切换到下一个
                if (_activeTabId.value == tabId) {
                    val remainingTabs = _tabs.value
                    if (remainingTabs.isNotEmpty()) {
                        switchToTab(remainingTabs.first().tabId)
                    } else {
                        _activeTabId.value = null
                    }
                }

                Log.d(TAG, "Closed tab: $tabId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close tab", e)
        }
    }

    /**
     * 更新标签页内容（URL和标题）
     */
    suspend fun updateTabContent(tabId: String, url: String, title: String) {
        try {
            val currentTime = System.currentTimeMillis()
            tabDao.updateTabContent(tabId, url, title, currentTime)

            // 同时记录到历史
            addHistory(url, title)

            Log.d(TAG, "Updated tab content: $tabId, url: $url, title: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update tab content", e)
        }
    }

    /**
     * 更新标签页滚动位置
     */
    suspend fun updateTabScrollPosition(tabId: String, scrollY: Int) {
        try {
            tabDao.updateTabScrollPosition(tabId, scrollY)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update tab scroll position", e)
        }
    }

    /**
     * 获取标签页总数
     */
    suspend fun getTabCount(): Int {
        return try {
            tabDao.getTabCount()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tab count", e)
            0
        }
    }

    /**
     * 关闭所有隐私标签页
     */
    suspend fun closeAllIncognitoTabs() {
        try {
            tabDao.deleteAllIncognitoTabs()
            Log.d(TAG, "Closed all incognito tabs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close incognito tabs", e)
        }
    }

    // ================== 历史记录管理 ==================

    /**
     * 添加浏览历史记录
     */
    suspend fun addHistory(url: String, title: String, searchTerm: String? = null) {
        try {
            val currentTime = System.currentTimeMillis()

            // 检查是否已存在
            val existingHistory = historyDao.getHistoryByUrl(url)
            if (existingHistory != null) {
                // 更新访问计数
                historyDao.incrementVisitCount(url, currentTime)
            } else {
                // 创建新记录
                val history = BrowserHistoryEntity(
                    url = url,
                    title = title,
                    visitTime = currentTime,
                    visitCount = 1,
                    searchTerm = searchTerm,
                    lastUpdated = currentTime
                )
                historyDao.addHistory(history)
            }

            Log.d(TAG, "Added history: $url")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add history", e)
        }
    }

    /**
     * 获取最近的历史记录
     */
    suspend fun getRecentHistory(limit: Int = 50): List<BrowserHistoryEntity> {
        return try {
            historyDao.getRecentHistory(limit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recent history", e)
            emptyList()
        }
    }

    /**
     * 搜索历史记录
     */
    fun searchHistory(keyword: String) = historyDao.searchHistory(keyword)

    /**
     * 清除所有历史记录
     */
    suspend fun clearAllHistory() {
        try {
            historyDao.clearAllHistory()
            Log.d(TAG, "Cleared all history")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear history", e)
        }
    }

    /**
     * 清除指定时间前的历史记录
     */
    suspend fun clearHistoryBefore(beforeTime: Long) {
        try {
            historyDao.clearHistoryBefore(beforeTime)
            Log.d(TAG, "Cleared history before: $beforeTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear history", e)
        }
    }

    /**
     * 删除指定URL的历史记录
     */
    suspend fun deleteHistory(url: String): Boolean {
        return try {
            val history = historyDao.getHistoryByUrl(url)
            if (history != null) {
                historyDao.deleteHistory(history)
                Log.d(TAG, "Deleted history: $url")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete history", e)
            false
        }
    }

    /**
     * 标记为书签
     */
    suspend fun markAsBookmark(url: String): Boolean {
        return try {
            historyDao.markAsBookmark(url)
            Log.d(TAG, "Marked as bookmark: $url")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark as bookmark", e)
            false
        }
    }

    // ================== 书签管理（代理） ==================

    /**
     * 添加书签
     */
    fun addBookmark(bookmark: BookmarkManager.Bookmark): Boolean {
        return bookmarkManager.addBookmark(bookmark)
    }

    /**
     * 获取所有书签
     */
    fun getAllBookmarks(): List<BookmarkManager.Bookmark> {
        return bookmarkManager.getAllBookmarks()
    }

    /**
     * 搜索书签
     */
    fun searchBookmarks(query: String): List<BookmarkManager.Bookmark> {
        return bookmarkManager.searchBookmarks(query)
    }

    /**
     * 删除书签
     */
    fun deleteBookmark(id: String): Boolean {
        return bookmarkManager.deleteBookmark(id)
    }

    /**
     * 获取收藏的书签
     */
    fun getFavoriteBookmarks(): List<BookmarkManager.Bookmark> {
        return bookmarkManager.getFavoriteBookmarks()
    }

    // ================== 下载管理（代理） ==================

    /**
     * 创建下载任务
     */
    fun createDownload(
        url: String,
        fileName: String,
        savePath: String,
        headers: Map<String, String> = emptyMap(),
        threadCount: Int = 4
    ): String {
        return downloadManager.createDownload(url, fileName, savePath, headers, threadCount)
    }

    /**
     * 开始下载
     */
    fun startDownload(downloadId: String) {
        downloadManager.startDownload(downloadId)
    }

    /**
     * 暂停下载
     */
    fun pauseDownload(downloadId: String) {
        downloadManager.pauseDownload(downloadId)
    }

    /**
     * 获取下载状态
     */
    fun getDownloadState(downloadId: String) = downloadManager.getDownloadState(downloadId)

    /**
     * 获取所有下载任务
     */
    fun getAllDownloads() = downloadDao.getAllDownloads()

    /**
     * 搜索下载
     */
    fun searchDownloads(keyword: String) = downloadDao.searchDownloads(keyword)

    /**
     * 取消下载
     */
    suspend fun cancelDownload(downloadId: String) {
        try {
            downloadDao.updateDownloadStatus(downloadId, "cancelled")
            downloadManager.pauseDownload(downloadId) // 停止下载线程
            Log.d(TAG, "Cancelled download: $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel download", e)
        }
    }

    /**
     * 重试下载
     */
    suspend fun retryDownload(downloadId: String) {
        try {
            downloadDao.updateDownloadStatus(downloadId, "pending")
            downloadManager.startDownload(downloadId)
            Log.d(TAG, "Retrying download: $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retry download", e)
        }
    }

    /**
     * 删除下载记录
     */
    suspend fun deleteDownload(downloadId: String): Boolean {
        return try {
            val download = downloadDao.getDownloadById(downloadId)
            if (download != null) {
                downloadDao.deleteDownload(download)
                Log.d(TAG, "Deleted download: $downloadId")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete download", e)
            false
        }
    }

    /**
     * 打开已下载的文件
     */
    suspend fun openDownloadedFile(downloadId: String): Boolean {
        // TODO: 实现文件打开逻辑
        return try {
            val download = downloadDao.getDownloadById(downloadId)
            if (download != null && download.status == "completed") {
                // 这里需要使用Intent打开文件
                // 需要传入Activity context
                Log.d(TAG, "Opening file: ${download.filePath}")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file", e)
            false
        }
    }

    /**
     * 清除已完成的下载
     */
    suspend fun clearCompletedDownloads() {
        try {
            downloadDao.deleteByStatus("completed")
            Log.d(TAG, "Cleared completed downloads")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear completed downloads", e)
        }
    }

    /**
     * 清空所有下载
     */
    suspend fun clearAllDownloads() {
        try {
            downloadDao.deleteAllDownloads()
            Log.d(TAG, "Cleared all downloads")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all downloads", e)
        }
    }

    /**
     * 暂停所有下载
     */
    suspend fun pauseAllDownloads() {
        try {
            val activeDownloads = downloadDao.getDownloadsByStatus("downloading")
            activeDownloads.forEach { download ->
                pauseDownload(download.downloadId)
            }
            Log.d(TAG, "Paused all downloads")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause all downloads", e)
        }
    }

    // ================== 设置管理 ==================

    private val preferences = PreferencesManager(context)

    /**
     * 获取搜索引擎
     */
    fun getSearchEngine(): String {
        return preferences.getString("search_engine", "https://www.baidu.com/s?wd=")
    }

    /**
     * 设置搜索引擎
     */
    fun setSearchEngine(searchEngineUrl: String) {
        preferences.putString("search_engine", searchEngineUrl)
    }

    /**
     * 是否启用广告拦截
     */
    fun isAdBlockEnabled(): Boolean {
        return preferences.getBoolean("ad_block_enabled", true)
    }

    /**
     * 设置广告拦截
     */
    fun setAdBlockEnabled(enabled: Boolean) {
        preferences.putBoolean("ad_block_enabled", enabled)
    }

    /**
     * 获取主页URL
     */
    fun getHomepage(): String {
        return preferences.getString("homepage", "https://www.baidu.com")
    }

    /**
     * 设置主页URL
     */
    fun setHomepage(url: String) {
        preferences.putString("homepage", url)
    }

    /**
     * 是否保存历史记录
     */
    fun isSaveHistoryEnabled(): Boolean {
        return preferences.getBoolean("save_history", true)
    }

    /**
     * 设置是否保存历史记录
     */
    fun setSaveHistoryEnabled(enabled: Boolean) {
        preferences.putBoolean("save_history", enabled)
    }
}
