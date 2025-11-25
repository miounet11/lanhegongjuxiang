package com.lanhe.gongjuxiang.utils

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 浏览历史记录数据访问对象 (Data Access Object)
 * 提供对browser_history表的数据库操作接口
 *
 * 使用Flow进行响应式查询，当数据变更时自动通知观察者
 */
@Dao
interface BrowserHistoryDao {

    /**
     * 插入新的历史记录
     * 如果URL已存在则忽略，由updateHistory负责增加访问计数
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addHistory(history: BrowserHistoryEntity)

    /**
     * 更新现有历史记录
     * 用于更新访问计数和最后更新时间
     */
    @Update
    suspend fun updateHistory(history: BrowserHistoryEntity)

    /**
     * 删除单条历史记录
     */
    @Delete
    suspend fun deleteHistory(history: BrowserHistoryEntity)

    /**
     * 获取所有历史记录（按访问时间降序）
     * @return 历史记录Flow，实时更新
     */
    @Query("SELECT * FROM browser_history ORDER BY visitTime DESC")
    fun getAllHistory(): Flow<List<BrowserHistoryEntity>>

    /**
     * 获取最近的N条历史记录
     * @param limit 数量限制
     */
    @Query("SELECT * FROM browser_history ORDER BY visitTime DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<BrowserHistoryEntity>

    /**
     * 按URL查询单条历史记录
     */
    @Query("SELECT * FROM browser_history WHERE url = :url")
    suspend fun getHistoryByUrl(url: String): BrowserHistoryEntity?

    /**
     * 搜索历史记录（标题或URL包含关键词）
     * @param keyword 搜索关键词
     */
    @Query("""
        SELECT * FROM browser_history
        WHERE title LIKE '%' || :keyword || '%'
           OR url LIKE '%' || :keyword || '%'
        ORDER BY visitTime DESC
    """)
    fun searchHistory(keyword: String): Flow<List<BrowserHistoryEntity>>

    /**
     * 获取访问次数最多的历史记录（热门网站）
     * @param limit 数量限制
     */
    @Query("""
        SELECT * FROM browser_history
        ORDER BY visitCount DESC
        LIMIT :limit
    """)
    suspend fun getFrequentHistory(limit: Int): List<BrowserHistoryEntity>

    /**
     * 获取已标记为书签的历史记录
     */
    @Query("""
        SELECT * FROM browser_history
        WHERE isBookmarked = 1
        ORDER BY visitTime DESC
    """)
    fun getBookmarkedHistory(): Flow<List<BrowserHistoryEntity>>

    /**
     * 获取指定时间范围内的历史记录
     * @param startTime 开始时间戳（毫秒）
     * @param endTime 结束时间戳（毫秒）
     */
    @Query("""
        SELECT * FROM browser_history
        WHERE visitTime BETWEEN :startTime AND :endTime
        ORDER BY visitTime DESC
    """)
    suspend fun getHistoryByTimeRange(startTime: Long, endTime: Long): List<BrowserHistoryEntity>

    /**
     * 获取历史记录数量
     */
    @Query("SELECT COUNT(*) FROM browser_history")
    suspend fun getHistoryCount(): Int

    /**
     * 清除所有历史记录
     */
    @Query("DELETE FROM browser_history")
    suspend fun clearAllHistory()

    /**
     * 清除指定时间前的历史记录
     * @param beforeTime 时间戳（毫秒）
     */
    @Query("DELETE FROM browser_history WHERE visitTime < :beforeTime")
    suspend fun clearHistoryBefore(beforeTime: Long)

    /**
     * 更新访问计数和最后更新时间
     */
    @Query("""
        UPDATE browser_history
        SET visitCount = visitCount + 1, lastUpdated = :currentTime
        WHERE url = :url
    """)
    suspend fun incrementVisitCount(url: String, currentTime: Long)

    /**
     * 标记为书签
     */
    @Query("""
        UPDATE browser_history
        SET isBookmarked = 1
        WHERE url = :url
    """)
    suspend fun markAsBookmark(url: String)

    /**
     * 取消书签标记
     */
    @Query("""
        UPDATE browser_history
        SET isBookmarked = 0
        WHERE url = :url
    """)
    suspend fun unmarkAsBookmark(url: String)
}
