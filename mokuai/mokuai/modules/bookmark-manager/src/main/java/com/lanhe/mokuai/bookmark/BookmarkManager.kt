package com.lanhe.mokuai.bookmark

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * 书签管理器 - 管理浏览器书签和收藏
 */
class BookmarkManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "bookmark_manager",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_BOOKMARKS = "bookmarks"
        private const val KEY_FOLDERS = "folders"
        private const val KEY_TAGS = "tags"
        private const val KEY_HISTORY = "history"
        private const val MAX_HISTORY = 100
    }

    data class Bookmark(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val url: String,
        val description: String = "",
        val folderId: String = "default",
        val tags: List<String> = emptyList(),
        val favicon: String = "",
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis(),
        val visitCount: Int = 0,
        val lastVisited: Long = 0,
        val isFavorite: Boolean = false,
        val isPrivate: Boolean = false,
        val customColor: String = ""
    )

    data class BookmarkFolder(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val parentId: String = "root",
        val icon: String = "📁",
        val color: String = "",
        val createdAt: Long = System.currentTimeMillis(),
        val isExpanded: Boolean = true,
        val sortOrder: Int = 0
    )

    data class VisitHistory(
        val bookmarkId: String,
        val visitTime: Long,
        val duration: Long = 0,
        val referrer: String = ""
    )

    /**
     * 添加书签
     */
    fun addBookmark(bookmark: Bookmark): Boolean {
        return try {
            val bookmarks = getAllBookmarksInternal().toMutableList()

            // 检查URL是否已存在
            if (bookmarks.any { it.url == bookmark.url && it.folderId == bookmark.folderId }) {
                return false
            }

            bookmarks.add(bookmark)
            saveBookmarks(bookmarks)

            // 自动添加标签
            bookmark.tags.forEach { tag ->
                addTag(tag)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 更新书签
     */
    fun updateBookmark(bookmark: Bookmark): Boolean {
        return try {
            val bookmarks = getAllBookmarksInternal().toMutableList()
            val index = bookmarks.indexOfFirst { it.id == bookmark.id }

            if (index == -1) return false

            bookmarks[index] = bookmark.copy(updatedAt = System.currentTimeMillis())
            saveBookmarks(bookmarks)

            // 更新标签
            bookmark.tags.forEach { tag ->
                addTag(tag)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除书签
     */
    fun deleteBookmark(id: String): Boolean {
        return try {
            val bookmarks = getAllBookmarksInternal().toMutableList()
            val removed = bookmarks.removeAll { it.id == id }
            if (removed) {
                saveBookmarks(bookmarks)
            }
            removed
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 批量删除书签
     */
    fun deleteBookmarks(ids: List<String>): Boolean {
        return try {
            val bookmarks = getAllBookmarksInternal().toMutableList()
            val removed = bookmarks.removeAll { it.id in ids }
            if (removed) {
                saveBookmarks(bookmarks)
            }
            removed
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取所有书签
     */
    fun getAllBookmarks(): List<Bookmark> {
        return getAllBookmarksInternal()
    }

    /**
     * 根据文件夹获取书签
     */
    fun getBookmarksByFolder(folderId: String): List<Bookmark> {
        return getAllBookmarksInternal().filter { it.folderId == folderId }
    }

    /**
     * 根据标签获取书签
     */
    fun getBookmarksByTag(tag: String): List<Bookmark> {
        return getAllBookmarksInternal().filter { tag in it.tags }
    }

    /**
     * 获取收藏的书签
     */
    fun getFavoriteBookmarks(): List<Bookmark> {
        return getAllBookmarksInternal().filter { it.isFavorite }
    }

    /**
     * 获取私密书签
     */
    fun getPrivateBookmarks(): List<Bookmark> {
        return getAllBookmarksInternal().filter { it.isPrivate }
    }

    /**
     * 搜索书签
     */
    fun searchBookmarks(query: String): List<Bookmark> {
        val lowercaseQuery = query.lowercase()
        return getAllBookmarksInternal().filter { bookmark ->
            bookmark.title.lowercase().contains(lowercaseQuery) ||
            bookmark.url.lowercase().contains(lowercaseQuery) ||
            bookmark.description.lowercase().contains(lowercaseQuery) ||
            bookmark.tags.any { it.lowercase().contains(lowercaseQuery) }
        }
    }

    /**
     * 获取最近访问的书签
     */
    fun getRecentBookmarks(limit: Int = 10): List<Bookmark> {
        return getAllBookmarksInternal()
            .filter { it.lastVisited > 0 }
            .sortedByDescending { it.lastVisited }
            .take(limit)
    }

    /**
     * 获取最常访问的书签
     */
    fun getFrequentBookmarks(limit: Int = 10): List<Bookmark> {
        return getAllBookmarksInternal()
            .filter { it.visitCount > 0 }
            .sortedByDescending { it.visitCount }
            .take(limit)
    }

    /**
     * 记录访问
     */
    fun recordVisit(bookmarkId: String, duration: Long = 0) {
        val bookmarks = getAllBookmarksInternal().toMutableList()
        val index = bookmarks.indexOfFirst { it.id == bookmarkId }

        if (index != -1) {
            bookmarks[index] = bookmarks[index].copy(
                visitCount = bookmarks[index].visitCount + 1,
                lastVisited = System.currentTimeMillis()
            )
            saveBookmarks(bookmarks)

            // 记录历史
            addHistory(VisitHistory(bookmarkId, System.currentTimeMillis(), duration))
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite(id: String): Boolean {
        val bookmarks = getAllBookmarksInternal().toMutableList()
        val index = bookmarks.indexOfFirst { it.id == id }

        if (index != -1) {
            bookmarks[index] = bookmarks[index].copy(
                isFavorite = !bookmarks[index].isFavorite
            )
            saveBookmarks(bookmarks)
            return bookmarks[index].isFavorite
        }
        return false
    }

    /**
     * 移动书签到文件夹
     */
    fun moveBookmarkToFolder(bookmarkId: String, folderId: String): Boolean {
        val bookmarks = getAllBookmarksInternal().toMutableList()
        val index = bookmarks.indexOfFirst { it.id == bookmarkId }

        if (index != -1) {
            bookmarks[index] = bookmarks[index].copy(
                folderId = folderId,
                updatedAt = System.currentTimeMillis()
            )
            saveBookmarks(bookmarks)
            return true
        }
        return false
    }

    /**
     * 批量移动书签
     */
    fun moveBookmarksToFolder(bookmarkIds: List<String>, folderId: String): Boolean {
        val bookmarks = getAllBookmarksInternal().toMutableList()
        var moved = false

        bookmarkIds.forEach { bookmarkId ->
            val index = bookmarks.indexOfFirst { it.id == bookmarkId }
            if (index != -1) {
                bookmarks[index] = bookmarks[index].copy(
                    folderId = folderId,
                    updatedAt = System.currentTimeMillis()
                )
                moved = true
            }
        }

        if (moved) {
            saveBookmarks(bookmarks)
        }
        return moved
    }

    // 文件夹管理

    /**
     * 添加文件夹
     */
    fun addFolder(folder: BookmarkFolder): Boolean {
        return try {
            val folders = getAllFoldersInternal().toMutableList()

            // 检查名称是否已存在
            if (folders.any { it.name == folder.name && it.parentId == folder.parentId }) {
                return false
            }

            folders.add(folder)
            saveFolders(folders)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 更新文件夹
     */
    fun updateFolder(folder: BookmarkFolder): Boolean {
        return try {
            val folders = getAllFoldersInternal().toMutableList()
            val index = folders.indexOfFirst { it.id == folder.id }

            if (index == -1) return false

            folders[index] = folder
            saveFolders(folders)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除文件夹
     */
    fun deleteFolder(folderId: String, moveBookmarksToFolder: String = "default"): Boolean {
        return try {
            // 移动该文件夹下的书签
            val bookmarks = getAllBookmarksInternal().toMutableList()
            bookmarks.forEach { bookmark ->
                if (bookmark.folderId == folderId) {
                    val index = bookmarks.indexOf(bookmark)
                    bookmarks[index] = bookmark.copy(folderId = moveBookmarksToFolder)
                }
            }
            saveBookmarks(bookmarks)

            // 删除文件夹
            val folders = getAllFoldersInternal().toMutableList()
            val removed = folders.removeAll { it.id == folderId }
            if (removed) {
                saveFolders(folders)
            }
            removed
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取所有文件夹
     */
    fun getAllFolders(): List<BookmarkFolder> {
        return getAllFoldersInternal()
    }

    /**
     * 获取子文件夹
     */
    fun getSubFolders(parentId: String): List<BookmarkFolder> {
        return getAllFoldersInternal().filter { it.parentId == parentId }
    }

    // 标签管理

    /**
     * 获取所有标签
     */
    fun getAllTags(): List<String> {
        val tags = prefs.getStringSet(KEY_TAGS, emptySet()) ?: emptySet()
        return tags.toList().sorted()
    }

    /**
     * 添加标签
     */
    fun addTag(tag: String) {
        val tags = prefs.getStringSet(KEY_TAGS, emptySet())?.toMutableSet() ?: mutableSetOf()
        tags.add(tag)
        prefs.edit().putStringSet(KEY_TAGS, tags).apply()
    }

    /**
     * 删除标签
     */
    fun deleteTag(tag: String) {
        val tags = prefs.getStringSet(KEY_TAGS, emptySet())?.toMutableSet() ?: mutableSetOf()
        tags.remove(tag)
        prefs.edit().putStringSet(KEY_TAGS, tags).apply()

        // 从所有书签中移除该标签
        val bookmarks = getAllBookmarksInternal().toMutableList()
        bookmarks.forEach { bookmark ->
            if (tag in bookmark.tags) {
                val index = bookmarks.indexOf(bookmark)
                bookmarks[index] = bookmark.copy(
                    tags = bookmark.tags.filter { it != tag }
                )
            }
        }
        saveBookmarks(bookmarks)
    }

    // 导入导出

    /**
     * 导出为JSON
     */
    fun exportToJson(): String {
        val root = JSONObject()

        // 导出书签
        val bookmarksArray = JSONArray()
        getAllBookmarksInternal().forEach { bookmark ->
            val json = JSONObject().apply {
                put("id", bookmark.id)
                put("title", bookmark.title)
                put("url", bookmark.url)
                put("description", bookmark.description)
                put("folderId", bookmark.folderId)
                put("tags", JSONArray(bookmark.tags))
                put("favicon", bookmark.favicon)
                put("createdAt", bookmark.createdAt)
                put("updatedAt", bookmark.updatedAt)
                put("visitCount", bookmark.visitCount)
                put("lastVisited", bookmark.lastVisited)
                put("isFavorite", bookmark.isFavorite)
                put("isPrivate", bookmark.isPrivate)
                put("customColor", bookmark.customColor)
            }
            bookmarksArray.put(json)
        }
        root.put("bookmarks", bookmarksArray)

        // 导出文件夹
        val foldersArray = JSONArray()
        getAllFoldersInternal().forEach { folder ->
            val json = JSONObject().apply {
                put("id", folder.id)
                put("name", folder.name)
                put("parentId", folder.parentId)
                put("icon", folder.icon)
                put("color", folder.color)
                put("createdAt", folder.createdAt)
                put("isExpanded", folder.isExpanded)
                put("sortOrder", folder.sortOrder)
            }
            foldersArray.put(json)
        }
        root.put("folders", foldersArray)

        // 导出标签
        root.put("tags", JSONArray(getAllTags()))

        // 元数据
        root.put("version", 1)
        root.put("exportDate", System.currentTimeMillis())

        return root.toString(2)
    }

    /**
     * 从JSON导入
     */
    fun importFromJson(json: String, merge: Boolean = false): Boolean {
        return try {
            val root = JSONObject(json)

            // 导入书签
            val bookmarksArray = root.getJSONArray("bookmarks")
            val bookmarks = mutableListOf<Bookmark>()

            for (i in 0 until bookmarksArray.length()) {
                val item = bookmarksArray.getJSONObject(i)
                val tags = mutableListOf<String>()

                val tagsArray = item.optJSONArray("tags")
                if (tagsArray != null) {
                    for (j in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(j))
                    }
                }

                bookmarks.add(
                    Bookmark(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        url = item.getString("url"),
                        description = item.optString("description", ""),
                        folderId = item.optString("folderId", "default"),
                        tags = tags,
                        favicon = item.optString("favicon", ""),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                        visitCount = item.optInt("visitCount", 0),
                        lastVisited = item.optLong("lastVisited", 0),
                        isFavorite = item.optBoolean("isFavorite", false),
                        isPrivate = item.optBoolean("isPrivate", false),
                        customColor = item.optString("customColor", "")
                    )
                )
            }

            if (merge) {
                val existing = getAllBookmarksInternal().toMutableList()
                bookmarks.forEach { bookmark ->
                    if (existing.none { it.id == bookmark.id }) {
                        existing.add(bookmark)
                    }
                }
                saveBookmarks(existing)
            } else {
                saveBookmarks(bookmarks)
            }

            // 导入文件夹
            val foldersArray = root.optJSONArray("folders")
            if (foldersArray != null) {
                val folders = mutableListOf<BookmarkFolder>()

                for (i in 0 until foldersArray.length()) {
                    val item = foldersArray.getJSONObject(i)
                    folders.add(
                        BookmarkFolder(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            parentId = item.optString("parentId", "root"),
                            icon = item.optString("icon", "📁"),
                            color = item.optString("color", ""),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                            isExpanded = item.optBoolean("isExpanded", true),
                            sortOrder = item.optInt("sortOrder", 0)
                        )
                    )
                }

                if (merge) {
                    val existing = getAllFoldersInternal().toMutableList()
                    folders.forEach { folder ->
                        if (existing.none { it.id == folder.id }) {
                            existing.add(folder)
                        }
                    }
                    saveFolders(existing)
                } else {
                    saveFolders(folders)
                }
            }

            // 导入标签
            val tagsArray = root.optJSONArray("tags")
            if (tagsArray != null) {
                val tags = mutableSetOf<String>()
                for (i in 0 until tagsArray.length()) {
                    tags.add(tagsArray.getString(i))
                }

                if (merge) {
                    val existing = prefs.getStringSet(KEY_TAGS, emptySet())?.toMutableSet() ?: mutableSetOf()
                    existing.addAll(tags)
                    prefs.edit().putStringSet(KEY_TAGS, existing).apply()
                } else {
                    prefs.edit().putStringSet(KEY_TAGS, tags).apply()
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 清除所有数据
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // 私有方法

    private fun getAllBookmarksInternal(): List<Bookmark> {
        val json = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return parseBookmarksFromJson(json)
    }

    private fun saveBookmarks(bookmarks: List<Bookmark>) {
        val jsonArray = JSONArray()
        bookmarks.forEach { bookmark ->
            val json = JSONObject().apply {
                put("id", bookmark.id)
                put("title", bookmark.title)
                put("url", bookmark.url)
                put("description", bookmark.description)
                put("folderId", bookmark.folderId)
                put("tags", JSONArray(bookmark.tags))
                put("favicon", bookmark.favicon)
                put("createdAt", bookmark.createdAt)
                put("updatedAt", bookmark.updatedAt)
                put("visitCount", bookmark.visitCount)
                put("lastVisited", bookmark.lastVisited)
                put("isFavorite", bookmark.isFavorite)
                put("isPrivate", bookmark.isPrivate)
                put("customColor", bookmark.customColor)
            }
            jsonArray.put(json)
        }
        prefs.edit().putString(KEY_BOOKMARKS, jsonArray.toString()).apply()
    }

    private fun parseBookmarksFromJson(json: String): List<Bookmark> {
        val bookmarks = mutableListOf<Bookmark>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val tags = mutableListOf<String>()

                val tagsArray = item.optJSONArray("tags")
                if (tagsArray != null) {
                    for (j in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(j))
                    }
                }

                bookmarks.add(
                    Bookmark(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        url = item.getString("url"),
                        description = item.optString("description", ""),
                        folderId = item.optString("folderId", "default"),
                        tags = tags,
                        favicon = item.optString("favicon", ""),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                        visitCount = item.optInt("visitCount", 0),
                        lastVisited = item.optLong("lastVisited", 0),
                        isFavorite = item.optBoolean("isFavorite", false),
                        isPrivate = item.optBoolean("isPrivate", false),
                        customColor = item.optString("customColor", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bookmarks
    }

    private fun getAllFoldersInternal(): List<BookmarkFolder> {
        val json = prefs.getString(KEY_FOLDERS, null) ?: return getDefaultFolders()
        return parseFoldersFromJson(json)
    }

    private fun saveFolders(folders: List<BookmarkFolder>) {
        val jsonArray = JSONArray()
        folders.forEach { folder ->
            val json = JSONObject().apply {
                put("id", folder.id)
                put("name", folder.name)
                put("parentId", folder.parentId)
                put("icon", folder.icon)
                put("color", folder.color)
                put("createdAt", folder.createdAt)
                put("isExpanded", folder.isExpanded)
                put("sortOrder", folder.sortOrder)
            }
            jsonArray.put(json)
        }
        prefs.edit().putString(KEY_FOLDERS, jsonArray.toString()).apply()
    }

    private fun parseFoldersFromJson(json: String): List<BookmarkFolder> {
        val folders = mutableListOf<BookmarkFolder>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                folders.add(
                    BookmarkFolder(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        parentId = item.optString("parentId", "root"),
                        icon = item.optString("icon", "📁"),
                        color = item.optString("color", ""),
                        createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        isExpanded = item.optBoolean("isExpanded", true),
                        sortOrder = item.optInt("sortOrder", 0)
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return folders.ifEmpty { getDefaultFolders() }
    }

    private fun getDefaultFolders(): List<BookmarkFolder> {
        return listOf(
            BookmarkFolder(id = "default", name = "默认", parentId = "root", icon = "📚"),
            BookmarkFolder(id = "work", name = "工作", parentId = "root", icon = "💼"),
            BookmarkFolder(id = "personal", name = "个人", parentId = "root", icon = "👤"),
            BookmarkFolder(id = "reading", name = "阅读", parentId = "root", icon = "📖"),
            BookmarkFolder(id = "tech", name = "技术", parentId = "root", icon = "💻")
        )
    }

    private fun addHistory(history: VisitHistory) {
        try {
            val historyJson = prefs.getString(KEY_HISTORY, null)
            val historyList = if (historyJson != null) {
                parseHistoryFromJson(historyJson).toMutableList()
            } else {
                mutableListOf()
            }

            historyList.add(0, history)

            // 限制历史记录数量
            if (historyList.size > MAX_HISTORY) {
                historyList.removeAt(historyList.size - 1)
            }

            saveHistory(historyList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveHistory(history: List<VisitHistory>) {
        val jsonArray = JSONArray()
        history.forEach { item ->
            val json = JSONObject().apply {
                put("bookmarkId", item.bookmarkId)
                put("visitTime", item.visitTime)
                put("duration", item.duration)
                put("referrer", item.referrer)
            }
            jsonArray.put(json)
        }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }

    private fun parseHistoryFromJson(json: String): List<VisitHistory> {
        val history = mutableListOf<VisitHistory>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                history.add(
                    VisitHistory(
                        bookmarkId = item.getString("bookmarkId"),
                        visitTime = item.getLong("visitTime"),
                        duration = item.optLong("duration", 0),
                        referrer = item.optString("referrer", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return history
    }
}