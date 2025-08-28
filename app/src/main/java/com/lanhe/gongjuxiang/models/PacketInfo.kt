package com.lanhe.gongjuxiang.models

import java.text.SimpleDateFormat
import java.util.*

data class PacketInfo(
    val timestamp: String = getCurrentTimestamp(),
    val protocol: String,
    val sourceAddress: String,
    val destinationAddress: String,
    val size: Int,
    val description: String,
    val rawData: String? = null,
    val requestMethod: String? = null,
    val responseCode: Int? = null,
    val contentType: String? = null,
    val userAgent: String? = null,
    val isHttps: Boolean = false
) {
    companion object {
        private fun getCurrentTimestamp(): String {
            val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    // 获取格式化的数据大小
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> String.format("%.1fKB", size / 1024.0)
            else -> String.format("%.1fMB", size / (1024.0 * 1024.0))
        }
    }

    // 获取协议颜色
    fun getProtocolColor(): Int {
        return when (protocol.uppercase()) {
            "HTTP" -> 0xFF2196F3.toInt()  // 蓝色
            "HTTPS" -> 0xFF4CAF50.toInt() // 绿色
            "TCP" -> 0xFFFF9800.toInt()   // 橙色
            "UDP" -> 0xFF9C27B0.toInt()   // 紫色
            "ICMP" -> 0xFFF44336.toInt()  // 红色
            else -> 0xFF757575.toInt()    // 灰色
        }
    }

    // 获取状态图标
    fun getStatusIcon(): String {
        return when {
            protocol.contains("HTTP") && responseCode != null -> {
                when (responseCode) {
                    in 200..299 -> "✅"
                    in 300..399 -> "🔄"
                    in 400..499 -> "⚠️"
                    in 500..599 -> "❌"
                    else -> "🔍"
                }
            }
            protocol == "TCP" -> "🔗"
            protocol == "UDP" -> "📡"
            protocol == "ICMP" -> "🏓"
            else -> "📦"
        }
    }

    // 判断是否为错误包
    fun isError(): Boolean {
        return responseCode != null && responseCode >= 400
    }

    // 判断是否为成功的HTTP请求
    fun isSuccess(): Boolean {
        return responseCode != null && responseCode in 200..299
    }

    // 获取简化的URL显示
    fun getSimplifiedUrl(): String {
        return if (destinationAddress.length > 50) {
            destinationAddress.substring(0, 47) + "..."
        } else {
            destinationAddress
        }
    }

    // 获取数据包类型描述
    fun getTypeDescription(): String {
        return when {
            protocol.contains("HTTP") -> {
                if (requestMethod != null) {
                    "$requestMethod 请求"
                } else {
                    "HTTP 响应"
                }
            }
            protocol == "TCP" -> "TCP 连接"
            protocol == "UDP" -> "UDP 数据包"
            protocol == "ICMP" -> "ICMP 消息"
            else -> "$protocol 数据包"
        }
    }

    // 生成摘要信息
    fun getSummary(): String {
        val method = requestMethod ?: ""
        val url = getSimplifiedUrl()
        val size = getFormattedSize()

        return when {
            protocol.contains("HTTP") -> "$method $url ($size)"
            else -> "$protocol ${sourceAddress.split(":").first()} → ${destinationAddress.split(":").first()} ($size)"
        }
    }

    // 获取详细信息
    fun getDetailedInfo(): String {
        return """
            📅 时间: $timestamp
            🔗 协议: $protocol ${if (isHttps) "(HTTPS)" else ""}
            📍 源地址: $sourceAddress
            🎯 目标地址: $destinationAddress
            📏 数据大小: ${getFormattedSize()}
            📝 描述: $description
            ${if (requestMethod != null) "🔧 请求方法: $requestMethod" else ""}
            ${if (responseCode != null) "📊 响应代码: $responseCode" else ""}
            ${if (contentType != null) "📄 内容类型: $contentType" else ""}
            ${if (userAgent != null && userAgent.length < 100) "🤖 User-Agent: $userAgent" else ""}
        """.trimIndent()
    }
}
