package com.lanhe.gongjuxiang.utils

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 浏览器标签页数据访问对象
 * 提供对browser_tabs表的数据库操作接口
 * 用于多标签页的持久化和恢复
 */
@Dao
interface BrowserTabDao {

    /**
     * 创建新的标签页记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: BrowserTabEntity)

    /**
     * 更新标签页信息
     */
    @Update
    suspend fun updateTab(tab: BrowserTabEntity)

    /**
     * 删除标签页记录
     */
    @Delete
    suspend fun deleteTab(tab: BrowserTabEntity)

    /**
     * 获取所有标签页（按创建时间升序）
     */
    @Query("SELECT * FROM browser_tabs ORDER BY createTime ASC")
    fun getAllTabs(): Flow<List<BrowserTabEntity>>

    /**
     * 获取所有非隐私标签页
     */
    @Query("""
        SELECT * FROM browser_tabs
        WHERE isIncognito = 0
        ORDER BY createTime ASC
    """)
    suspend fun getNormalTabs(): List<BrowserTabEntity>

    /**
     * 获取所有隐私标签页
     */
    @Query("""
        SELECT * FROM browser_tabs
        WHERE isIncognito = 1
        ORDER BY createTime ASC
    """)
    suspend fun getIncognitoTabs(): List<BrowserTabEntity>

    /**
     * 获取当前活跃的标签页
     */
    @Query("SELECT * FROM browser_tabs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTab(): BrowserTabEntity?

    /**
     * 通过ID查询标签页
     */
    @Query("SELECT * FROM browser_tabs WHERE tabId = :tabId")
    suspend fun getTabById(tabId: String): BrowserTabEntity?

    /**
     * 获取最近访问的标签页
     * @param limit 数量限制
     */
    @Query("""
        SELECT * FROM browser_tabs
        ORDER BY lastAccessTime DESC
        LIMIT :limit
    """)
    suspend fun getRecentTabs(limit: Int): List<BrowserTabEntity>

    /**
     * 设置活跃标签页（先取消所有活跃状态，再设置新的）
     */
    @Query("UPDATE browser_tabs SET isActive = 0 WHERE isActive = 1")
    suspend fun clearActiveStatus()

    /**
     * 设置指定标签页为活跃
     */
    @Query("""
        UPDATE browser_tabs
        SET isActive = 1, lastAccessTime = :currentTime
        WHERE tabId = :tabId
    """)
    suspend fun setTabActive(tabId: String, currentTime: Long)

    /**
     * 更新标签页的访问时间
     */
    @Query("""
        UPDATE browser_tabs
        SET lastAccessTime = :currentTime
        WHERE tabId = :tabId
    """)
    suspend fun updateTabAccessTime(tabId: String, currentTime: Long)

    /**
     * 更新标签页的滚动位置
     */
    @Query("""
        UPDATE browser_tabs
        SET scrollY = :scrollY
        WHERE tabId = :tabId
    """)
    suspend fun updateTabScrollPosition(tabId: String, scrollY: Int)

    /**
     * 更新标签页的URL和标题
     */
    @Query("""
        UPDATE browser_tabs
        SET url = :url, title = :title, lastAccessTime = :currentTime
        WHERE tabId = :tabId
    """)
    suspend fun updateTabContent(tabId: String, url: String, title: String, currentTime: Long)

    /**
     * 更新标签页缩略图路径
     */
    @Query("""
        UPDATE browser_tabs
        SET thumbnailPath = :thumbnailPath
        WHERE tabId = :tabId
    """)
    suspend fun updateTabThumbnail(tabId: String, thumbnailPath: String)

    /**
     * 获取标签页总数
     */
    @Query("SELECT COUNT(*) FROM browser_tabs")
    suspend fun getTabCount(): Int

    /**
     * 获取非隐私标签页数量
     */
    @Query("SELECT COUNT(*) FROM browser_tabs WHERE isIncognito = 0")
    suspend fun getNormalTabCount(): Int

    /**
     * 获取隐私标签页数量
     */
    @Query("SELECT COUNT(*) FROM browser_tabs WHERE isIncognito = 1")
    suspend fun getIncognitoTabCount(): Int

    /**
     * 删除所有隐私标签页（退出隐私模式时清理）
     */
    @Query("DELETE FROM browser_tabs WHERE isIncognito = 1")
    suspend fun deleteAllIncognitoTabs()

    /**
     * 删除所有标签页
     */
    @Query("DELETE FROM browser_tabs")
    suspend fun deleteAllTabs()

    /**
     * 删除超过指定时间未访问的标签页
     * @param beforeTime 时间戳（毫秒）
     */
    @Query("""
        DELETE FROM browser_tabs
        WHERE lastAccessTime < :beforeTime AND isActive = 0
    """)
    suspend fun deleteUnusedTabs(beforeTime: Long)

    /**
     * 批量插入标签页（用于恢复保存的标签页）
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabs(tabs: List<BrowserTabEntity>)
}
