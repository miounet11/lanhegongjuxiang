package com.lanhe.gongjuxiang.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * 延迟测试器
 * 处理网络延迟测试功能
 */
class LatencyTester {

    data class LatencyResult(
        val averageLatency: Long,
        val minLatency: Long,
        val maxLatency: Long,
        val packetLoss: Float,
        val quality: String
    )

    /**
     * 开始延迟测试
     */
    suspend fun performLatencyTest(): LatencyResult {
        return withContext(Dispatchers.IO) {
            // 测试多个目标的延迟
            val targets = listOf(
                "https://www.baidu.com",
                "https://www.qq.com",
                "https://www.taobao.com"
            )

            val results = mutableListOf<Long>()
            for (target in targets) {
                val latency = measureLatency(target)
                if (latency > 0) {
                    results.add(latency)
                }
            }

            val averageLatency = if (results.isNotEmpty()) {
                results.average().toLong()
            } else {
                -1L
            }

            LatencyResult(
                averageLatency = averageLatency,
                minLatency = results.minOrNull() ?: -1L,
                maxLatency = results.maxOrNull() ?: -1L,
                packetLoss = 0f,
                quality = getLatencyQuality(averageLatency)
            )
        }
    }

    /**
     * 测量单个目标的延迟
     */
    private fun measureLatency(urlString: String): Long {
        try {
            val startTime = System.currentTimeMillis()
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "HEAD"
            connection.connect()
            connection.disconnect()
            return System.currentTimeMillis() - startTime
        } catch (e: IOException) {
            return -1L
        }
    }

    /**
     * 获取延迟质量
     */
    private fun getLatencyQuality(latency: Long): String {
        return when {
            latency <= 0 -> "无连接"
            latency < 50 -> "优秀"
            latency < 100 -> "良好"
            latency < 200 -> "一般"
            else -> "较差"
        }
    }

    /**
     * 获取延迟质量颜色资源ID
     */
    fun getLatencyQualityColor(quality: String): Int {
        return when (quality) {
            "优秀" -> android.R.color.holo_green_dark
            "良好" -> android.R.color.holo_blue_dark
            "一般" -> android.R.color.holo_orange_dark
            "较差" -> android.R.color.holo_red_dark
            else -> android.R.color.darker_gray
        }
    }

    /**
     * 获取延迟建议
     */
    fun getLatencySuggestions(latency: Long): List<String> {
        val suggestions = mutableListOf<String>()

        when {
            latency <= 0 -> {
                suggestions.add("检查网络连接")
                suggestions.add("确认WiFi信号强度")
                suggestions.add("尝试重启路由器")
            }
            latency < 50 -> {
                suggestions.add("网络延迟优秀，无需优化")
            }
            latency < 100 -> {
                suggestions.add("网络延迟良好")
                suggestions.add("可考虑使用有线连接获得更好体验")
            }
            latency < 200 -> {
                suggestions.add("网络延迟一般")
                suggestions.add("建议检查WiFi信号强度")
                suggestions.add("考虑更换DNS服务器")
            }
            else -> {
                suggestions.add("网络延迟较差")
                suggestions.add("建议靠近路由器")
                suggestions.add("检查是否有网络拥堵")
                suggestions.add("考虑更换网络提供商")
            }
        }

        return suggestions
    }
}
