package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * 广告拦截器
 * 拦截和屏蔽网页广告，提升浏览体验
 */
class AdBlocker(private val context: Context) {

    // 广告域名黑名单
    private val adDomains = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "googletagmanager.com",
        "googletagservices.com",
        "amazon-adsystem.com",
        "facebook.com",
        "facebook.net",
        "instagram.com",
        "twitter.com",
        "linkedin.com",
        "pinterest.com",
        "tiktok.com",
        "snapchat.com",
        "adsystem.amazon",
        "adsystem.facebook",
        "adsystem.twitter",
        "adsystem.linkedin",
        "adsystem.pinterest",
        "adsystem.tiktok",
        "adsystem.snapchat",
        "baidu.com",
        "sina.com.cn",
        "sohu.com",
        "qq.com",
        "163.com",
        "ifeng.com",
        "xinhuanet.com",
        "people.com.cn",
        "cctv.com"
    )

    // 广告URL模式
    private val adPatterns = listOf(
        ".*googlesyndication\\.com.*",
        ".*doubleclick\\.net.*",
        ".*googleadservices\\.com.*",
        ".*googletagmanager\\.com.*",
        ".*googletagservices\\.com.*",
        ".*amazon-adsystem\\.com.*",
        ".*facebook\\.com.*",
        ".*facebook\\.net.*",
        ".*instagram\\.com.*",
        ".*twitter\\.com.*",
        ".*linkedin\\.com.*",
        ".*pinterest\\.com.*",
        ".*tiktok\\.com.*",
        ".*snapchat\\.com.*",
        ".*baidu\\.com.*",
        ".*sina\\.com\\.cn.*",
        ".*sohu\\.com.*",
        ".*qq\\.com.*",
        ".*163\\.com.*",
        ".*ifeng\\.com.*",
        ".*xinhuanet\\.com.*",
        ".*people\\.com\\.cn.*",
        ".*cctv\\.com.*"
    )

    // 广告元素选择器
    private val adSelectors = listOf(
        "[class*='ad']",
        "[id*='ad']",
        "[class*='banner']",
        "[id*='banner']",
        "[class*='popup']",
        "[id*='popup']",
        "[class*='modal']",
        "[id*='modal']",
        "[class*='overlay']",
        "[id*='overlay']",
        "[class*='sponsor']",
        "[id*='sponsor']",
        "[class*='promo']",
        "[id*='promo']",
        "[class*='advertisement']",
        "[id*='advertisement']"
    )

    // 统计数据
    private var blockedRequests = 0
    private var totalRequests = 0

    /**
     * 检查是否应该拦截请求
     */
    fun shouldBlock(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false

        totalRequests++

        // 检查域名黑名单
        val domain = extractDomain(url)
        if (adDomains.contains(domain)) {
            blockedRequests++
            return true
        }

        // 检查URL模式
        for (pattern in adPatterns) {
            if (url.matches(Regex(pattern))) {
                blockedRequests++
                return true
            }
        }

        return false
    }

    /**
     * 创建拦截响应
     */
    fun createBlockedResponse(): WebResourceResponse {
        val emptyContent = ByteArrayInputStream(ByteArray(0))
        return WebResourceResponse("text/plain", "utf-8", emptyContent)
    }

    /**
     * 获取广告拦截统计
     */
    fun getBlockStats(): AdBlockStats {
        val blockRate = if (totalRequests > 0) {
            (blockedRequests.toFloat() / totalRequests.toFloat()) * 100f
        } else {
            0f
        }

        return AdBlockStats(
            totalRequests = totalRequests,
            blockedRequests = blockedRequests,
            blockRate = blockRate
        )
    }

    /**
     * 重置统计数据
     */
    fun resetStats() {
        blockedRequests = 0
        totalRequests = 0
    }

    /**
     * 获取广告移除JavaScript代码
     */
    fun getAdRemovalScript(): String {
        val selectors = adSelectors.joinToString(",") { "'$it'" }
        return """
            (function() {
                var adSelectors = [$selectors];
                var removedCount = 0;

                adSelectors.forEach(function(selector) {
                    var elements = document.querySelectorAll(selector);
                    elements.forEach(function(element) {
                        element.style.display = 'none';
                        removedCount++;
                    });
                });

                // 移除常见的广告脚本
                var scripts = document.querySelectorAll('script');
                scripts.forEach(function(script) {
                    var src = script.src || '';
                    if (src.includes('googlesyndication') ||
                        src.includes('doubleclick') ||
                        src.includes('googleadservices') ||
                        src.includes('googletagmanager') ||
                        src.includes('googletagservices') ||
                        src.includes('amazon-adsystem') ||
                        src.includes('facebook') ||
                        src.includes('twitter') ||
                        src.includes('linkedin') ||
                        src.includes('pinterest') ||
                        src.includes('tiktok') ||
                        src.includes('snapchat')) {
                        script.remove();
                        removedCount++;
                    }
                });

                console.log('AdBlocker: Removed ' + removedCount + ' ad elements');
                return removedCount;
            })();
        """.trimIndent()
    }

    /**
     * 提取域名
     */
    private fun extractDomain(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        resetStats()
    }

    /**
     * 广告拦截统计数据类
     */
    data class AdBlockStats(
        val totalRequests: Int,
        val blockedRequests: Int,
        val blockRate: Float
    )
}
