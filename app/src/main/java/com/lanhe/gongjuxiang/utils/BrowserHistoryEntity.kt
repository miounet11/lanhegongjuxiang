package com.lanhe.gongjuxiang.utils

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 浏览历史记录实体
 * 记录用户访问过的网页信息
 *
 * 字段说明：
 * - id: 唯一标识符 (自动递增)
 * - url: 网页URL (唯一约束，防止重复记录)
 * - title: 网页标题
 * - visitTime: 访问时间戳 (Long, 毫秒)
 * - visitCount: 访问次数计数器
 * - favicon: 网站图标 (BLOB二进制数据)
 * - isBookmarked: 是否已书签标记
 * - searchTerm: 搜索关键词 (用户搜索而非直接访问时记录)
 * - lastUpdated: 最后更新时间
 */
@Entity(
    tableName = "browser_history",
    indices = [
        Index("url", unique = true),  // URL唯一索引，加速查询和去重
        Index("visitTime"),            // 时间戳索引，加速按时间排序
        Index("isBookmarked")          // 书签标记索引，加速书签查询
    ]
)
data class BrowserHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val url: String,                  // 网页链接
    val title: String = "",           // 网页标题
    val visitTime: Long,              // 访问时间戳
    val visitCount: Int = 1,          // 访问计数
    val favicon: ByteArray? = null,   // 网站图标（Base64编码存储）

    val isBookmarked: Boolean = false, // 是否已加入书签
    val searchTerm: String? = null,   // 如果是搜索结果，记录搜索词
    val lastUpdated: Long = 0         // 最后更新时间
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BrowserHistoryEntity

        if (id != other.id) return false
        if (url != other.url) return false
        if (title != other.title) return false
        if (visitTime != other.visitTime) return false
        if (visitCount != other.visitCount) return false
        if (favicon != null) {
            if (other.favicon == null) return false
            if (!favicon.contentEquals(other.favicon)) return false
        } else if (other.favicon != null) return false
        if (isBookmarked != other.isBookmarked) return false
        if (searchTerm != other.searchTerm) return false
        if (lastUpdated != other.lastUpdated) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + visitTime.hashCode()
        result = 31 * result + visitCount
        result = 31 * result + (favicon?.contentHashCode() ?: 0)
        result = 31 * result + isBookmarked.hashCode()
        result = 31 * result + (searchTerm?.hashCode() ?: 0)
        result = 31 * result + lastUpdated.hashCode()
        return result
    }
}
