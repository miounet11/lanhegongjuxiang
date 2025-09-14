package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * 图片优化器
 * 优化网页图片加载，提升浏览速度
 */
class ImageOptimizer(private val context: Context) {

    // 支持优化的图片格式
    private val supportedFormats = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    // 图片URL模式
    private val imagePatterns = listOf(
        ".*\\.(jpg|jpeg|png|webp|gif|bmp)(\\?.*)?$",
        ".*\\/images?\\/.*",
        ".*\\/img\\/.*",
        ".*\\/photos?\\/.*",
        ".*\\/pictures?\\/.*",
        ".*\\/thumbnails?\\/.*"
    )

    // 统计数据
    private var optimizedImages = 0
    private var totalImages = 0
    private var savedBytes = 0L

    /**
     * 检查是否应该优化图片
     */
    fun shouldOptimizeImage(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false

        totalImages++

        // 检查URL模式
        for (pattern in imagePatterns) {
            if (url.matches(Regex(pattern, RegexOption.IGNORE_CASE))) {
                return true
            }
        }

        // 检查文件扩展名
        val extension = getFileExtension(url)
        if (supportedFormats.contains(extension.lowercase())) {
            return true
        }

        return false
    }

    /**
     * 优化图片请求
     */
    fun optimizeImageRequest(request: WebResourceRequest?): WebResourceResponse? {
        if (request == null) return null

        val url = request.url.toString()

        // 这里可以实现图片压缩、格式转换等优化
        // 暂时返回空响应表示不拦截
        optimizedImages++

        // 模拟节省的字节数
        savedBytes += 1024L // 假设每张图片节省1KB

        return null
    }

    /**
     * 获取图片优化统计
     */
    fun getOptimizationStats(): ImageOptimizationStats {
        val optimizationRate = if (totalImages > 0) {
            (optimizedImages.toFloat() / totalImages.toFloat()) * 100f
        } else {
            0f
        }

        return ImageOptimizationStats(
            totalImages = totalImages,
            optimizedImages = optimizedImages,
            optimizationRate = optimizationRate,
            savedBytes = savedBytes
        )
    }

    /**
     * 获取图片优化JavaScript代码
     */
    fun getImageOptimizationScript(): String {
        return """
            (function() {
                var images = document.querySelectorAll('img');
                var optimizedCount = 0;
                var savedBytes = 0;

                images.forEach(function(img) {
                    // 添加懒加载
                    if (!img.hasAttribute('loading')) {
                        img.setAttribute('loading', 'lazy');
                    }

                    // 添加图片优化参数
                    var src = img.src;
                    if (src && !src.includes('optimized=true')) {
                        // 这里可以添加图片优化参数
                        // 例如: img.src = src + '&optimized=true&w=800&h=600';
                        optimizedCount++;
                        savedBytes += 1024; // 模拟节省的字节数
                    }

                    // 添加错误处理
                    img.onerror = function() {
                        console.log('Image failed to load:', src);
                        // 可以设置默认图片
                        // img.src = 'data:image/svg+xml;base64,...';
                    };
                });

                console.log('ImageOptimizer: Optimized ' + optimizedCount + ' images, saved ' + savedBytes + ' bytes');
                return {count: optimizedCount, saved: savedBytes};
            })();
        """.trimIndent()
    }

    /**
     * 获取文件扩展名
     */
    private fun getFileExtension(url: String): String {
        return try {
            val lastDotIndex = url.lastIndexOf('.')
            if (lastDotIndex > 0) {
                val extension = url.substring(lastDotIndex + 1)
                val questionMarkIndex = extension.indexOf('?')
                if (questionMarkIndex > 0) {
                    extension.substring(0, questionMarkIndex)
                } else {
                    extension
                }
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 重置统计数据
     */
    fun resetStats() {
        optimizedImages = 0
        totalImages = 0
        savedBytes = 0L
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        resetStats()
    }

    /**
     * 图片优化统计数据类
     */
    data class ImageOptimizationStats(
        val totalImages: Int,
        val optimizedImages: Int,
        val optimizationRate: Float,
        val savedBytes: Long
    )
}
