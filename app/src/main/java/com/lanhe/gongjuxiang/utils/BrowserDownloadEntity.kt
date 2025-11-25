package com.lanhe.gongjuxiang.utils

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 浏览器下载记录实体
 * 记录用户的所有下载任务信息
 *
 * 字段说明：
 * - downloadId: 下载任务唯一ID (UUID)
 * - url: 下载链接地址
 * - fileName: 保存的文件名
 * - filePath: 完整的文件保存路径
 * - fileSize: 文件总大小 (字节)
 * - downloadedSize: 已下载大小 (字节)
 * - status: 下载状态 (PENDING/DOWNLOADING/PAUSED/COMPLETED/FAILED)
 * - mimeType: 文件MIME类型 (application/pdf等)
 * - createTime: 下载创建时间
 * - completeTime: 下载完成时间 (null表示未完成)
 * - retryCount: 重试次数
 */
@Entity(
    tableName = "browser_downloads",
    indices = [
        Index("status"),            // 状态索引，快速查找进行中的下载
        Index("createTime"),        // 创建时间索引，用于历史排序
        Index("url", unique = true) // URL唯一索引，防止重复下载
    ]
)
data class BrowserDownloadEntity(
    @PrimaryKey
    val downloadId: String,         // 下载ID (UUID)

    val url: String,                // 下载链接
    val fileName: String,           // 文件名
    val filePath: String,           // 保存路径

    val fileSize: Long = 0L,        // 文件总大小
    val downloadedSize: Long = 0L,  // 已下载大小

    val status: String = "PENDING", // 下载状态
    val mimeType: String? = null,   // MIME类型

    val createTime: Long = 0,       // 创建时间戳
    val completeTime: Long? = null, // 完成时间戳
    val retryCount: Int = 0         // 重试计数
) {
    // 计算下载进度 (百分比)
    fun getProgress(): Float = if (fileSize > 0) {
        (downloadedSize.toFloat() / fileSize) * 100
    } else {
        0f
    }

    // 判断是否在下载中
    fun isDownloading(): Boolean = status == "DOWNLOADING"

    // 判断是否已完成
    fun isCompleted(): Boolean = status == "COMPLETED"

    // 判断是否失败
    fun isFailed(): Boolean = status == "FAILED"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BrowserDownloadEntity

        if (downloadId != other.downloadId) return false
        if (url != other.url) return false
        if (fileName != other.fileName) return false
        if (filePath != other.filePath) return false
        if (fileSize != other.fileSize) return false
        if (downloadedSize != other.downloadedSize) return false
        if (status != other.status) return false
        if (mimeType != other.mimeType) return false
        if (createTime != other.createTime) return false
        if (completeTime != other.completeTime) return false
        if (retryCount != other.retryCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = downloadId.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + fileName.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + downloadedSize.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        result = 31 * result + createTime.hashCode()
        result = 31 * result + (completeTime?.hashCode() ?: 0)
        result = 31 * result + retryCount
        return result
    }
}
