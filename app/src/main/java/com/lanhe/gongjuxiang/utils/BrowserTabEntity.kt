package com.lanhe.gongjuxiang.utils

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 浏览器标签页实体
 * 记录多标签页的状态信息，用于标签页持久化和恢复
 *
 * 字段说明：
 * - tabId: 标签页唯一ID (UUID格式)
 * - url: 当前加载的URL
 * - title: 页面标题
 * - favicon: 网站图标
 * - scrollY: 页面滚动位置 (Y轴像素)
 * - createTime: 标签创建时间戳
 * - lastAccessTime: 最后访问时间戳
 * - isActive: 是否为当前活跃标签页
 * - isIncognito: 是否为隐私/无痕模式标签页
 * - thumbnailPath: 标签页缩略图文件路径
 * - webViewState: WebView状态Bundle (用于恢复页面状态)
 */
@Entity(
    tableName = "browser_tabs",
    indices = [
        Index("createTime"),        // 按创建时间索引，用于标签页排序
        Index("lastAccessTime"),    // 按访问时间索引，用于最近标签排序
        Index("isActive"),          // 活跃标签索引，快速查找当前标签
        Index("isIncognito")        // 隐私模式标签索引
    ]
)
data class BrowserTabEntity(
    @PrimaryKey
    val tabId: String,              // 唯一标签ID (UUID)

    val url: String = "about:blank", // 当前加载的URL
    val title: String = "",         // 页面标题
    val favicon: ByteArray? = null, // 网站图标 (Base64)

    val scrollY: Int = 0,           // 页面滚动位置
    val createTime: Long = 0,       // 创建时间戳
    val lastAccessTime: Long = 0,   // 最后访问时间戳

    val isActive: Boolean = false,  // 当前活跃标签页
    val isIncognito: Boolean = false, // 隐私模式标签页

    val thumbnailPath: String? = null, // 缩略图路径
    val webViewState: String? = null   // WebView状态序列化数据
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BrowserTabEntity

        if (tabId != other.tabId) return false
        if (url != other.url) return false
        if (title != other.title) return false
        if (favicon != null) {
            if (other.favicon == null) return false
            if (!favicon.contentEquals(other.favicon)) return false
        } else if (other.favicon != null) return false
        if (scrollY != other.scrollY) return false
        if (createTime != other.createTime) return false
        if (lastAccessTime != other.lastAccessTime) return false
        if (isActive != other.isActive) return false
        if (isIncognito != other.isIncognito) return false
        if (thumbnailPath != other.thumbnailPath) return false
        if (webViewState != other.webViewState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tabId.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (favicon?.contentHashCode() ?: 0)
        result = 31 * result + scrollY
        result = 31 * result + createTime.hashCode()
        result = 31 * result + lastAccessTime.hashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + isIncognito.hashCode()
        result = 31 * result + (thumbnailPath?.hashCode() ?: 0)
        result = 31 * result + (webViewState?.hashCode() ?: 0)
        return result
    }
}
