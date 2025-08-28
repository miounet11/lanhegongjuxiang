package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.util.Log
import com.lanhe.gongjuxiang.models.PacketInfo
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class PacketCaptureManager(private val context: Context) {

    companion object {
        private const val TAG = "PacketCaptureManager"
        private const val MAX_PACKETS = 1000 // 最大保存的数据包数量
    }

    private var isCapturing = false
    private var captureJob: Job? = null
    private var packetCallback: ((PacketInfo) -> Unit)? = null
    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastTxBytes = TrafficStats.getTotalTxBytes()

    // HTTP客户端用于模拟请求
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // 开始抓包
    fun startCapture(callback: (PacketInfo) -> Unit) {
        if (isCapturing) {
            Log.w(TAG, "Packet capture is already running")
            return
        }

        packetCallback = callback
        isCapturing = true

        captureJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting packet capture")

                // 启动流量监控
                startTrafficMonitoring()

                // 启动网络连接监控
                startConnectionMonitoring()

                // 启动模拟HTTP请求（用于演示）
                simulateHttpRequests()

            } catch (e: Exception) {
                Log.e(TAG, "Error during packet capture", e)
            }
        }
    }

    // 停止抓包
    fun stopCapture() {
        if (!isCapturing) {
            Log.w(TAG, "Packet capture is not running")
            return
        }

        isCapturing = false
        captureJob?.cancel()
        captureJob = null
        packetCallback = null

        Log.d(TAG, "Packet capture stopped")
    }

    // 启动流量监控
    private suspend fun startTrafficMonitoring() {
        while (isCapturing) {
            try {
                val currentRxBytes = TrafficStats.getTotalRxBytes()
                val currentTxBytes = TrafficStats.getTotalTxBytes()

                if (currentRxBytes != TrafficStats.UNSUPPORTED.toLong() &&
                    currentTxBytes != TrafficStats.UNSUPPORTED.toLong()) {

                    val rxDiff = currentRxBytes - lastRxBytes
                    val txDiff = currentTxBytes - lastTxBytes

                    if (rxDiff > 0 || txDiff > 0) {
                        // 创建流量统计包
                        val packet = createTrafficPacket(rxDiff, txDiff)
                        withContext(Dispatchers.Main) {
                            packetCallback?.invoke(packet)
                        }
                    }

                    lastRxBytes = currentRxBytes
                    lastTxBytes = currentTxBytes
                }

                delay(1000) // 每秒检查一次
            } catch (e: Exception) {
                Log.e(TAG, "Error in traffic monitoring", e)
                delay(1000)
            }
        }
    }

    // 启动网络连接监控
    private suspend fun startConnectionMonitoring() {
        while (isCapturing) {
            try {
                // 监控网络连接状态
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)

                if (capabilities != null) {
                    val packet = createConnectionPacket(capabilities)
                    withContext(Dispatchers.Main) {
                        packetCallback?.invoke(packet)
                    }
                }

                delay(5000) // 每5秒检查一次
            } catch (e: Exception) {
                Log.e(TAG, "Error in connection monitoring", e)
                delay(5000)
            }
        }
    }

    // 模拟HTTP请求（用于演示抓包功能）
    private suspend fun simulateHttpRequests() {
        val demoUrls = listOf(
            "https://httpbin.org/get",
            "https://httpbin.org/post",
            "https://httpbin.org/put",
            "https://httpbin.org/delete",
            "https://api.github.com/zen",
            "https://jsonplaceholder.typicode.com/posts/1"
        )

        while (isCapturing) {
            try {
                // 随机选择一个URL进行测试
                val randomUrl = demoUrls.random()

                val packet = createHttpPacket("GET", randomUrl, "模拟HTTP请求")
                withContext(Dispatchers.Main) {
                    packetCallback?.invoke(packet)
                }

                // 实际发送HTTP请求
                makeHttpRequest(randomUrl)

                // 等待一段时间再发送下一个请求
                delay((2000..8000).random().toLong())

            } catch (e: Exception) {
                Log.e(TAG, "Error in HTTP simulation", e)
                delay(3000)
            }
        }
    }

    // 发送实际的HTTP请求
    private suspend fun makeHttpRequest(url: String) {
        try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = withContext(Dispatchers.IO) {
                httpClient.newCall(request).execute()
            }

            response.use { resp ->
                val packet = createHttpResponsePacket(
                    url,
                    resp.code,
                    resp.body?.contentLength() ?: 0,
                    resp.headers["content-type"] ?: "unknown"
                )

                withContext(Dispatchers.Main) {
                    packetCallback?.invoke(packet)
                }
            }

        } catch (e: IOException) {
            Log.e(TAG, "HTTP request failed: $url", e)

            val errorPacket = createHttpErrorPacket(url, e.message ?: "Unknown error")
            withContext(Dispatchers.Main) {
                packetCallback?.invoke(errorPacket)
            }
        }
    }

    // 创建流量统计包
    private fun createTrafficPacket(rxBytes: Long, txBytes: Long): PacketInfo {
        val totalBytes = rxBytes + txBytes
        val direction = when {
            rxBytes > 0 && txBytes > 0 -> "双向"
            rxBytes > 0 -> "下载"
            txBytes > 0 -> "上传"
            else -> "无流量"
        }

        return PacketInfo(
            protocol = "TRAFFIC",
            sourceAddress = "系统",
            destinationAddress = "网络接口",
            size = totalBytes.toInt(),
            description = "网络流量统计 - $direction ${formatBytes(totalBytes)}"
        )
    }

    // 创建连接状态包
    private fun createConnectionPacket(capabilities: NetworkCapabilities): PacketInfo {
        val networkType = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动网络"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
            else -> "未知网络"
        }

        val speedInfo = if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            "有互联网连接"
        } else {
            "无互联网连接"
        }

        return PacketInfo(
            protocol = "CONNECT",
            sourceAddress = "设备",
            destinationAddress = networkType,
            size = 0,
            description = "网络连接状态 - $networkType, $speedInfo"
        )
    }

    // 创建HTTP请求包
    private fun createHttpPacket(method: String, url: String, description: String): PacketInfo {
        val domain = extractDomain(url)
        val isHttps = url.startsWith("https://")

        return PacketInfo(
            protocol = if (isHttps) "HTTPS" else "HTTP",
            sourceAddress = "本机:随机端口",
            destinationAddress = "$domain:80",
            size = (100..2000).random(),
            description = "$method $url - $description",
            requestMethod = method,
            isHttps = isHttps
        )
    }

    // 创建HTTP响应包
    private fun createHttpResponsePacket(
        url: String,
        responseCode: Int,
        contentLength: Long,
        contentType: String
    ): PacketInfo {
        val domain = extractDomain(url)

        return PacketInfo(
            protocol = "HTTP",
            sourceAddress = "$domain:80",
            destinationAddress = "本机:随机端口",
            size = contentLength.toInt(),
            description = "HTTP响应 - 状态码: $responseCode",
            responseCode = responseCode,
            contentType = contentType
        )
    }

    // 创建HTTP错误包
    private fun createHttpErrorPacket(url: String, errorMessage: String): PacketInfo {
        val domain = extractDomain(url)

        return PacketInfo(
            protocol = "HTTP",
            sourceAddress = "本机:随机端口",
            destinationAddress = "$domain:80",
            size = 0,
            description = "HTTP请求失败 - $errorMessage",
            responseCode = 0
        )
    }

    // 创建TCP连接包
    private fun createTcpPacket(source: String, destination: String, size: Int): PacketInfo {
        return PacketInfo(
            protocol = "TCP",
            sourceAddress = source,
            destinationAddress = destination,
            size = size,
            description = "TCP数据传输"
        )
    }

    // 创建UDP数据包
    private fun createUdpPacket(source: String, destination: String, size: Int): PacketInfo {
        return PacketInfo(
            protocol = "UDP",
            sourceAddress = source,
            destinationAddress = destination,
            size = size,
            description = "UDP数据传输"
        )
    }

    // 创建ICMP包
    private fun createIcmpPacket(source: String, destination: String): PacketInfo {
        return PacketInfo(
            protocol = "ICMP",
            sourceAddress = source,
            destinationAddress = destination,
            size = 64, // ICMP包的典型大小
            description = "ICMP消息"
        )
    }

    // 从URL中提取域名
    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    // 格式化字节数
    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f%s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    // 获取抓包状态
    fun isCapturing(): Boolean = isCapturing

    // 获取当前活跃的网络接口
    fun getActiveNetworkInterface(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        return when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "移动网络"
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "以太网"
            else -> "未知"
        }
    }

    // 测试网络连通性
    fun testConnectivity(host: String = "8.8.8.8"): Boolean {
        return try {
            val address = InetAddress.getByName(host)
            address.isReachable(3000)
        } catch (e: Exception) {
            false
        }
    }

    // 获取网络延迟
    fun getNetworkLatency(host: String = "8.8.8.8"): Long {
        return try {
            val startTime = System.currentTimeMillis()
            val address = InetAddress.getByName(host)
            if (address.isReachable(5000)) {
                System.currentTimeMillis() - startTime
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }
}
