package com.lanhe.gongjuxiang.utils

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 浏览器下载记录数据访问对象
 * 提供对browser_downloads表的数据库操作接口
 * 用于下载历史、进度跟踪和下载管理
 */
@Dao
interface BrowserDownloadDao {

    /**
     * 创建新的下载记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: BrowserDownloadEntity)

    /**
     * 更新下载记录（进度、状态等）
     */
    @Update
    suspend fun updateDownload(download: BrowserDownloadEntity)

    /**
     * 删除下载记录
     */
    @Delete
    suspend fun deleteDownload(download: BrowserDownloadEntity)

    /**
     * 获取所有下载记录（按创建时间降序）
     */
    @Query("SELECT * FROM browser_downloads ORDER BY createTime DESC")
    fun getAllDownloads(): Flow<List<BrowserDownloadEntity>>

    /**
     * 获取进行中的下载
     */
    @Query("""
        SELECT * FROM browser_downloads
        WHERE status = 'DOWNLOADING'
        ORDER BY createTime DESC
    """)
    fun getDownloadingDownloads(): Flow<List<BrowserDownloadEntity>>

    /**
     * 获取已暂停的下载
     */
    @Query("""
        SELECT * FROM browser_downloads
        WHERE status = 'PAUSED'
        ORDER BY createTime DESC
    """)
    fun getPausedDownloads(): Flow<List<BrowserDownloadEntity>>

    /**
     * 获取已完成的下载
     */
    @Query("""
        SELECT * FROM browser_downloads
        WHERE status = 'COMPLETED'
        ORDER BY completeTime DESC
    """)
    fun getCompletedDownloads(): Flow<List<BrowserDownloadEntity>>

    /**
     * 获取失败的下载
     */
    @Query("""
        SELECT * FROM browser_downloads
        WHERE status = 'FAILED'
        ORDER BY createTime DESC
    """)
    fun getFailedDownloads(): Flow<List<BrowserDownloadEntity>>

    /**
     * 通过下载ID查询
     */
    @Query("SELECT * FROM browser_downloads WHERE downloadId = :downloadId")
    suspend fun getDownloadById(downloadId: String): BrowserDownloadEntity?

    /**
     * 通过URL查询
     */
    @Query("SELECT * FROM browser_downloads WHERE url = :url ORDER BY createTime DESC LIMIT 1")
    suspend fun getDownloadByUrl(url: String): BrowserDownloadEntity?

    /**
     * 搜索下载记录（文件名或URL）
     */
    @Query("""
        SELECT * FROM browser_downloads
        WHERE fileName LIKE '%' || :keyword || '%'
           OR url LIKE '%' || :keyword || '%'
        ORDER BY createTime DESC
    """)
    fun searchDownloads(keyword: String): Flow<List<BrowserDownloadEntity>>

    /**
     * 获取最近的N条下载
     */
    @Query("""
        SELECT * FROM browser_downloads
        ORDER BY createTime DESC
        LIMIT :limit
    """)
    suspend fun getRecentDownloads(limit: Int): List<BrowserDownloadEntity>

    /**
     * 更新下载进度
     */
    @Query("""
        UPDATE browser_downloads
        SET downloadedSize = :downloadedSize
        WHERE downloadId = :downloadId
    """)
    suspend fun updateDownloadProgress(downloadId: String, downloadedSize: Long)

    /**
     * 更新下载状态
     */
    @Query("""
        UPDATE browser_downloads
        SET status = :status
        WHERE downloadId = :downloadId
    """)
    suspend fun updateDownloadStatus(downloadId: String, status: String)

    /**
     * 标记下载完成
     */
    @Query("""
        UPDATE browser_downloads
        SET status = 'COMPLETED', completeTime = :completeTime, downloadedSize = :fileSize
        WHERE downloadId = :downloadId
    """)
    suspend fun markDownloadCompleted(downloadId: String, completeTime: Long, fileSize: Long)

    /**
     * 标记下载失败
     */
    @Query("""
        UPDATE browser_downloads
        SET status = 'FAILED', retryCount = retryCount + 1
        WHERE downloadId = :downloadId
    """)
    suspend fun markDownloadFailed(downloadId: String)

    /**
     * 获取下载总数
     */
    @Query("SELECT COUNT(*) FROM browser_downloads")
    suspend fun getDownloadCount(): Int

    /**
     * 获取进行中的下载总数
     */
    @Query("SELECT COUNT(*) FROM browser_downloads WHERE status = 'DOWNLOADING'")
    suspend fun getDownloadingCount(): Int

    /**
     * 获取已完成的下载总数
     */
    @Query("SELECT COUNT(*) FROM browser_downloads WHERE status = 'COMPLETED'")
    suspend fun getCompletedCount(): Int

    /**
     * 清除所有下载记录
     */
    @Query("DELETE FROM browser_downloads")
    suspend fun deleteAllDownloads()

    /**
     * 清除已完成的下载记录
     */
    @Query("DELETE FROM browser_downloads WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedDownloads()

    /**
     * 清除指定时间前的下载记录
     */
    @Query("""
        DELETE FROM browser_downloads
        WHERE createTime < :beforeTime
    """)
    suspend fun deleteDownloadsBefore(beforeTime: Long)

    /**
     * 批量插入下载记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloads(downloads: List<BrowserDownloadEntity>)

    /**
     * 增加重试次数
     */
    @Query("""
        UPDATE browser_downloads
        SET retryCount = retryCount + 1
        WHERE downloadId = :downloadId
    """)
    suspend fun incrementRetryCount(downloadId: String)

    /**
     * 按状态删除下载记录
     */
    @Query("DELETE FROM browser_downloads WHERE status = :status")
    suspend fun deleteByStatus(status: String)

    /**
     * 按状态获取下载记录
     */
    @Query("SELECT * FROM browser_downloads WHERE status = :status")
    suspend fun getDownloadsByStatus(status: String): List<BrowserDownloadEntity>

    /**
     * 获取可以重试的失败下载（重试次数少于3次）
     */
    @Query("""
        SELECT * FROM browser_downloads
        WHERE status = 'FAILED' AND retryCount < 3
        ORDER BY createTime ASC
        LIMIT :limit
    """)
    suspend fun getRetryableFailedDownloads(limit: Int = 10): List<BrowserDownloadEntity>
}
