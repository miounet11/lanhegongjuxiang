package com.lanhe.gongjuxiang.utils

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import kotlin.math.min

class CoreOptimizationManager(private val context: Context) {

    companion object {
        private const val TAG = "CoreOptimizationManager"

        // CPU Governors
        private const val GOVERNOR_PERFORMANCE = "performance"
        private const val GOVERNOR_INTERACTIVE = "interactive"
        private const val GOVERNOR_POWERSAVE = "powersave"
        private const val GOVERNOR_ONDEMAND = "ondemand"

        // Network optimization values
        private const val TCP_BUFFER_SIZE_WIFI = "524288,1048576,4194304,524288,1048576,4194304"
        private const val TCP_BUFFER_SIZE_LTE = "524288,1048576,2097152,262144,524288,1048576"

        // FPS target values
        private const val TARGET_FPS_GAMING = 60
        private const val TARGET_FPS_VIDEO = 30
        private const val TARGET_FPS_NORMAL = 30
    }

    private var fpsOptimizationJob: kotlinx.coroutines.Job? = null
    private var latencyOptimizationJob: kotlinx.coroutines.Job? = null
    private var downloadOptimizationJob: kotlinx.coroutines.Job? = null
    private var networkVideoOptimizationJob: kotlinx.coroutines.Job? = null

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Optimization state tracking
    private var lastCpuOptimizationTime = 0L
    private var lastMemoryOptimizationTime = 0L
    private var lastNetworkOptimizationTime = 0L
    private var currentFpsTarget = TARGET_FPS_NORMAL

    // FPS提升优化
    fun startFpsOptimization() {
        Log.d(TAG, "Starting FPS optimization")

        fpsOptimizationJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                // Set target FPS for gaming
                currentFpsTarget = TARGET_FPS_GAMING

                // Initial optimization
                performInitialFpsOptimization()

                // Continuous optimization loop
                while (fpsOptimizationJob?.isActive == true) {
                    val cpuUsage = getCpuUsage()
                    val memoryPressure = getMemoryPressure()

                    // Dynamic CPU frequency adjustment based on load
                    if (cpuUsage > 80) {
                        setCpuGovernor(GOVERNOR_PERFORMANCE)
                        boostCpuFrequency()
                    } else if (cpuUsage > 50) {
                        setCpuGovernor(GOVERNOR_INTERACTIVE)
                    }

                    // Memory optimization if pressure is high
                    if (memoryPressure > 70) {
                        performMemoryOptimization()
                    }

                    // Kill background processes that might affect FPS
                    killLowPriorityProcesses()

                    delay(3000) // Check every 3 seconds
                }
            } catch (e: Exception) {
                Log.e(TAG, "FPS optimization error", e)
            }
        }

        showNotification("帧率提升", "帧率优化已启动，游戏体验更流畅！")
    }

    fun stopFpsOptimization() {
        Log.d(TAG, "Stopping FPS optimization")
        fpsOptimizationJob?.cancel()
        fpsOptimizationJob = null

        // 恢复默认CPU模式
        executeShellCommand("echo 'interactive' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")

        showNotification("帧率提升", "帧率优化已停止")
    }

    // 延迟优化
    fun startLatencyOptimization() {
        Log.d(TAG, "Starting latency optimization")

        latencyOptimizationJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initial network optimization
                optimizeNetworkStack()

                // Continuous latency optimization
                while (latencyOptimizationJob?.isActive == true) {
                    val networkType = getActiveNetworkType()

                    when (networkType) {
                        NetworkType.WIFI -> {
                            optimizeWifiLatency()
                            setTcpBufferSize(TCP_BUFFER_SIZE_WIFI)
                        }
                        NetworkType.CELLULAR -> {
                            optimizeCellularLatency()
                            setTcpBufferSize(TCP_BUFFER_SIZE_LTE)
                        }
                        else -> {
                            // General optimization
                            optimizeGeneralNetworkLatency()
                        }
                    }

                    // DNS optimization
                    optimizeDnsSettings()

                    // Clear network cache if needed
                    if (shouldClearNetworkCache()) {
                        clearNetworkCache()
                    }

                    delay(5000) // Check every 5 seconds
                }
            } catch (e: Exception) {
                Log.e(TAG, "Latency optimization error", e)
            }
        }

        showNotification("延迟优化", "网络延迟优化已启动，响应更快！")
    }

    fun stopLatencyOptimization() {
        Log.d(TAG, "Stopping latency optimization")
        latencyOptimizationJob?.cancel()
        latencyOptimizationJob = null

        showNotification("延迟优化", "网络延迟优化已停止")
    }

    // 下载提速
    fun startDownloadOptimization() {
        Log.d(TAG, "Starting download optimization")

        downloadOptimizationJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initial download optimization
                optimizeDownloadSettings()
                increaseTcpBufferSizes()

                // Monitor and optimize downloads continuously
                while (downloadOptimizationJob?.isActive == true) {
                    val currentDownloadSpeed = getCurrentDownloadSpeed()
                    val networkQuality = assessNetworkQuality()

                    // Dynamic optimization based on network quality
                    when (networkQuality) {
                        NetworkQuality.EXCELLENT -> {
                            // Maximum performance settings
                            setMaximumDownloadThreads()
                            enableParallelDownloads()
                        }
                        NetworkQuality.GOOD -> {
                            // Balanced settings
                            setBalancedDownloadThreads()
                        }
                        NetworkQuality.POOR -> {
                            // Conservative settings to prevent timeouts
                            setConservativeDownloadThreads()
                            enableDownloadResume()
                        }
                    }

                    // Optimize based on current speed
                    if (currentDownloadSpeed < 100_000) { // Less than 100KB/s
                        boostDownloadPriority()
                        clearDownloadCache()
                    }

                    delay(2000) // Check every 2 seconds
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download optimization error", e)
            }
        }

        showNotification("下载提速", "下载加速已启动，智能优化中！")
    }

    fun stopDownloadOptimization() {
        Log.d(TAG, "Stopping download optimization")
        downloadOptimizationJob?.cancel()
        downloadOptimizationJob = null

        showNotification("下载提速", "下载加速已停止")
    }

    // 弱网络视频优化
    fun startNetworkVideoOptimization() {
        Log.d(TAG, "Starting network video optimization")

        networkVideoOptimizationJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initial video optimization
                currentFpsTarget = TARGET_FPS_VIDEO
                optimizeVideoPlaybackSettings()

                // Continuous video optimization
                while (networkVideoOptimizationJob?.isActive == true) {
                    val networkSpeed = getCurrentNetworkSpeed()
                    val bufferHealth = getVideoBufferHealth()

                    // Adaptive streaming quality
                    when {
                        networkSpeed < 500_000 -> { // Less than 500KB/s
                            // Low quality mode
                            setVideoQuality(VideoQuality.LOW)
                            increaseBufferSize(3)
                            enableAggressiveCaching()
                        }
                        networkSpeed < 1_000_000 -> { // Less than 1MB/s
                            // Medium quality mode
                            setVideoQuality(VideoQuality.MEDIUM)
                            increaseBufferSize(2)
                        }
                        else -> {
                            // High quality mode
                            setVideoQuality(VideoQuality.HIGH)
                            setNormalBufferSize()
                        }
                    }

                    // Prevent rebuffering
                    if (bufferHealth < 30) {
                        pauseBackgroundDownloads()
                        prioritizeVideoTraffic()
                    }

                    delay(1000) // Check every second
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network video optimization error", e)
            }
        }

        showNotification("弱网络视频优化", "视频优化已启动，智能防卡顿！")
    }

    fun stopNetworkVideoOptimization() {
        Log.d(TAG, "Stopping network video optimization")
        networkVideoOptimizationJob?.cancel()
        networkVideoOptimizationJob = null

        showNotification("弱网络视频优化", "视频优化已停止")
    }

    private suspend fun performInitialFpsOptimization() {
        withContext(Dispatchers.IO) {
            try {
                // Fallback to standard Android APIs
                activityManager.isLowRamDevice().let { isLowRam ->
                    if (!isLowRam) {
                        // Can be more aggressive with optimization
                        executeShellCommand("settings put global animator_duration_scale 0.5")
                        executeShellCommand("settings put global transition_animation_scale 0.5")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Initial FPS optimization failed", e)
            }
        }
    }

    private suspend fun getCpuUsage(): Float {
        return try {
            // Fallback: read from /proc/stat
            val statFile = File("/proc/stat")
            if (statFile.exists()) {
                val lines = statFile.readLines()
                val cpuLine = lines.firstOrNull { it.startsWith("cpu ") }
                cpuLine?.let { parseCpuUsage(it) } ?: 0f
            } else {
                0f
            }
        } catch (e: Exception) {
            // 仅在非权限错误时记录日志，避免刷屏
            val msg = e.message ?: ""
            if (!msg.contains("EACCES") && !msg.contains("Permission denied")) {
                Log.e(TAG, "Failed to get CPU usage", e)
            }
            0f
        }
    }

    private fun parseCpuUsage(cpuLine: String): Float {
        val values = cpuLine.split(" ").filter { it.isNotEmpty() }
        if (values.size >= 5) {
            val user = values[1].toLongOrNull() ?: 0
            val nice = values[2].toLongOrNull() ?: 0
            val system = values[3].toLongOrNull() ?: 0
            val idle = values[4].toLongOrNull() ?: 0

            val total = user + nice + system + idle
            val active = user + nice + system

            return if (total > 0) (active.toFloat() / total * 100) else 0f
        }
        return 0f
    }

    private fun getMemoryPressure(): Int {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val usedMemory = memInfo.totalMem - memInfo.availMem
        return ((usedMemory.toFloat() / memInfo.totalMem) * 100).toInt()
    }

    private suspend fun setCpuGovernor(governor: String) {
        withContext(Dispatchers.IO) {
            try {
                // Try via shell (may not work without root)
                executeShellCommand("echo '$governor' > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set CPU governor", e)
            }
        }
    }

    private suspend fun boostCpuFrequency() {
        withContext(Dispatchers.IO) {
            try {
                // Try to set minimum CPU frequency to a higher value
                val cpuCount = Runtime.getRuntime().availableProcessors()
                for (i in 0 until cpuCount) {
                    executeShellCommand("echo '1200000' > /sys/devices/system/cpu/cpu$i/cpufreq/scaling_min_freq")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to boost CPU frequency", e)
            }
        }
    }

    private suspend fun performMemoryOptimization() {
        withContext(Dispatchers.IO) {
            try {
                // Use standard Android API
                activityManager.killBackgroundProcesses("com.example.app")

                // Trigger garbage collection
                System.gc()
                Runtime.getRuntime().gc()
            } catch (e: Exception) {
                Log.e(TAG, "Memory optimization failed", e)
            }
        }
    }

    private suspend fun killLowPriorityProcesses() {
        withContext(Dispatchers.IO) {
            try {
                val runningApps = activityManager.runningAppProcesses
                runningApps?.filter {
                    it.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND
                }?.forEach { process ->
                    activityManager.killBackgroundProcesses(process.processName)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to kill low priority processes", e)
            }
        }
    }

    // Network optimization functions
    private enum class NetworkType {
        WIFI, CELLULAR, ETHERNET, NONE
    }

    private enum class NetworkQuality {
        EXCELLENT, GOOD, POOR
    }

    private enum class VideoQuality {
        LOW, MEDIUM, HIGH
    }

    private fun getActiveNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.NONE
        }
    }

    private suspend fun optimizeNetworkStack() {
        withContext(Dispatchers.IO) {
            try {
                // TCP optimization
                executeShellCommand("echo '1' > /proc/sys/net/ipv4/tcp_tw_reuse")
                executeShellCommand("echo '1' > /proc/sys/net/ipv4/tcp_tw_recycle")
                executeShellCommand("echo '0' > /proc/sys/net/ipv4/tcp_slow_start_after_idle")

                // Buffer sizes
                executeShellCommand("echo '4096' > /proc/sys/net/core/rmem_default")
                executeShellCommand("echo '4096' > /proc/sys/net/core/wmem_default")
                executeShellCommand("echo '4194304' > /proc/sys/net/core/rmem_max")
                executeShellCommand("echo '4194304' > /proc/sys/net/core/wmem_max")
            } catch (e: Exception) {
                Log.e(TAG, "Network stack optimization failed", e)
            }
        }
    }

    private suspend fun optimizeWifiLatency() {
        withContext(Dispatchers.IO) {
            try {
                // WiFi specific optimizations
                executeShellCommand("settings put global wifi_scan_interval_when_p2p_connected 120000")
                executeShellCommand("settings put global wifi_framework_scan_interval_ms 300000")
            } catch (e: Exception) {
                Log.e(TAG, "WiFi latency optimization failed", e)
            }
        }
    }

    private suspend fun optimizeCellularLatency() {
        withContext(Dispatchers.IO) {
            try {
                // Cellular specific optimizations
                executeShellCommand("settings put global preferred_network_mode 9") // LTE/GSM auto
            } catch (e: Exception) {
                Log.e(TAG, "Cellular latency optimization failed", e)
            }
        }
    }

    private fun optimizeGeneralNetworkLatency() {
        try {
            // General network optimizations
            executeShellCommand("echo '4096 87380 4194304' > /proc/sys/net/ipv4/tcp_rmem")
            executeShellCommand("echo '4096 16384 4194304' > /proc/sys/net/ipv4/tcp_wmem")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to optimize network latency", e)
        }
    }

    private fun setTcpBufferSize(bufferSize: String) {
        try {
            executeShellCommand("setprop net.tcp.buffersize.default $bufferSize")
            executeShellCommand("setprop net.tcp.buffersize.wifi $bufferSize")
            executeShellCommand("setprop net.tcp.buffersize.lte $bufferSize")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set TCP buffer size", e)
        }
    }

    private fun optimizeDnsSettings() {
        try {
            // Use fast DNS servers
            executeShellCommand("setprop net.dns1 8.8.8.8")
            executeShellCommand("setprop net.dns2 8.8.4.4")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to optimize DNS", e)
        }
    }

    private fun shouldClearNetworkCache(): Boolean {
        val currentTime = SystemClock.elapsedRealtime()
        return (currentTime - lastNetworkOptimizationTime) > 300000 // 5 minutes
    }

    private fun clearNetworkCache() {
        try {
            executeShellCommand("ip -s -s neigh flush all")
            lastNetworkOptimizationTime = SystemClock.elapsedRealtime()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear network cache", e)
        }
    }

    // Download optimization functions
    private fun optimizeDownloadSettings() {
        try {
            // Increase download limits
            executeShellCommand("settings put global download_manager_max_bytes_over_mobile 524288000") // 500MB
            executeShellCommand("settings put global download_manager_recommended_max_bytes_over_mobile 262144000") // 250MB

            // Parallel download settings
            executeShellCommand("settings put global max_downloads 10")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to optimize download settings", e)
        }
    }

    private fun increaseTcpBufferSizes() {
        try {
            // Increase buffer sizes for better throughput
            executeShellCommand("echo '6144 87380 4194304' > /proc/sys/net/ipv4/tcp_rmem")
            executeShellCommand("echo '6144 65536 4194304' > /proc/sys/net/ipv4/tcp_wmem")
            executeShellCommand("echo '4194304' > /proc/sys/net/core/rmem_max")
            executeShellCommand("echo '4194304' > /proc/sys/net/core/wmem_max")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increase TCP buffer sizes", e)
        }
    }

    private fun getCurrentDownloadSpeed(): Long {
        // This would integrate with actual download monitoring
        // For now, return a placeholder
        return 0L
    }

    private fun assessNetworkQuality(): NetworkQuality {
        val network = connectivityManager.activeNetwork ?: return NetworkQuality.POOR
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkQuality.POOR

        val linkSpeed = capabilities.linkDownstreamBandwidthKbps
        return when {
            linkSpeed > 10000 -> NetworkQuality.EXCELLENT // > 10 Mbps
            linkSpeed > 2000 -> NetworkQuality.GOOD // > 2 Mbps
            else -> NetworkQuality.POOR
        }
    }

    private fun setMaximumDownloadThreads() {
        executeShellCommand("settings put global max_downloads 10")
    }

    private fun enableParallelDownloads() {
        executeShellCommand("settings put global download_manager_enable_parallel true")
    }

    private fun setBalancedDownloadThreads() {
        executeShellCommand("settings put global max_downloads 5")
    }

    private fun setConservativeDownloadThreads() {
        executeShellCommand("settings put global max_downloads 2")
    }

    private fun enableDownloadResume() {
        executeShellCommand("settings put global download_manager_enable_resume true")
    }

    private fun boostDownloadPriority() {
        try {
            // Increase network priority for downloads
            executeShellCommand("settings put global download_manager_priority high")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to boost download priority", e)
        }
    }

    private fun clearDownloadCache() {
        try {
            // Clear download manager cache
            executeShellCommand("pm clear com.android.providers.downloads")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear download cache", e)
        }
    }

    // Video optimization functions
    private fun optimizeVideoPlaybackSettings() {
        try {
            // Optimize video buffer settings
            executeShellCommand("settings put global media_video_buffering_size_kb 204800") // 200MB
            executeShellCommand("settings put global media_video_max_buffering_time_ms 60000") // 60 seconds
            executeShellCommand("settings put global media_video_min_buffering_time_ms 5000") // 5 seconds
        } catch (e: Exception) {
            Log.w(TAG, "Failed to optimize video playback", e)
        }
    }

    private fun getCurrentNetworkSpeed(): Long {
        val network = connectivityManager.activeNetwork ?: return 0L
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 0L
        return capabilities.linkDownstreamBandwidthKbps * 1000L / 8 // Convert to bytes per second
    }

    private fun getVideoBufferHealth(): Int {
        // This would integrate with actual video player monitoring
        // For now, return a placeholder
        return 50
    }

    private fun setVideoQuality(quality: VideoQuality) {
        val qualityString = when (quality) {
            VideoQuality.LOW -> "360p"
            VideoQuality.MEDIUM -> "720p"
            VideoQuality.HIGH -> "1080p"
        }
        executeShellCommand("settings put global preferred_video_quality $qualityString")
    }

    private fun increaseBufferSize(multiplier: Int) {
        val baseSize = 102400 // 100MB
        val newSize = baseSize * multiplier
        executeShellCommand("settings put global media_video_buffering_size_kb $newSize")
    }

    private fun setNormalBufferSize() {
        executeShellCommand("settings put global media_video_buffering_size_kb 102400")
    }

    private fun enableAggressiveCaching() {
        executeShellCommand("settings put global media_video_aggressive_caching 1")
    }

    private fun pauseBackgroundDownloads() {
        try {
            // Pause all background downloads temporarily
            executeShellCommand("am broadcast -a android.intent.action.DOWNLOAD_PAUSE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause background downloads", e)
        }
    }

    private fun prioritizeVideoTraffic() {
        try {
            // Set QoS for video streaming
            executeShellCommand("tc qdisc add dev wlan0 root handle 1: htb default 30")
            executeShellCommand("tc class add dev wlan0 parent 1: classid 1:1 htb rate 100mbit")
            executeShellCommand("tc class add dev wlan0 parent 1: classid 1:10 htb rate 80mbit ceil 100mbit prio 1")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prioritize video traffic", e)
        }
    }

    // 执行Shell命令
    private fun executeShellCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command: $command", e)
            ""
        }
    }

    // 显示通知
    private fun showNotification(title: String, message: String) {
        try {
            // 这里可以集成通知系统
            Log.i(TAG, "Notification: $title - $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification", e)
        }
    }

    // 检查功能状态
    fun isFpsOptimizationActive(): Boolean {
        return fpsOptimizationJob?.isActive == true
    }

    fun isLatencyOptimizationActive(): Boolean {
        return latencyOptimizationJob?.isActive == true
    }

    fun isDownloadOptimizationActive(): Boolean {
        return downloadOptimizationJob?.isActive == true
    }

    fun isNetworkVideoOptimizationActive(): Boolean {
        return networkVideoOptimizationJob?.isActive == true
    }

    // 停止所有优化
    fun stopAllOptimizations() {
        stopFpsOptimization()
        stopLatencyOptimization()
        stopDownloadOptimization()
        stopNetworkVideoOptimization()
    }
}
