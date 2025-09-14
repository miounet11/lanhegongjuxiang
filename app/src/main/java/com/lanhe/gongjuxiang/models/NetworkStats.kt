package com.lanhe.gongjuxiang.models

/**
 * 网络统计数据类
 */
data class NetworkStats(
    val interfaceName: String,         // 网络接口名称
    val rxBytes: Long,                // 接收字节数
    val txBytes: Long,                // 发送字节数
    val rxPackets: Long,              // 接收数据包数
    val txPackets: Long,              // 发送数据包数
    val rxErrors: Long,               // 接收错误数
    val txErrors: Long,               // 发送错误数
    val rxDropped: Long,              // 接收丢弃数
    val txDropped: Long,              // 发送丢弃数
    val timestamp: Long,              // 时间戳

    // 计算属性
    val totalBytes: Long = rxBytes + txBytes,  // 总流量
    val rxSpeed: Double = 0.0,       // 接收速度 (bytes/s)
    val txSpeed: Double = 0.0,       // 发送速度 (bytes/s)
    val totalSpeed: Double = 0.0     // 总速度 (bytes/s)
) {
    /**
     * 计算网络使用率
     */
    fun getUsagePercent(maxSpeed: Long = 100 * 1024 * 1024): Double { // 默认100Mbps
        return if (maxSpeed > 0) {
            (totalSpeed / maxSpeed.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    /**
     * 格式化流量显示
     */
    fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0

        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }

        return String.format("%.2f %s", value, units[unitIndex])
    }

    /**
     * 格式化速度显示
     */
    fun formatSpeed(bytesPerSecond: Double): String {
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s")
        var value = bytesPerSecond
        var unitIndex = 0

        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }

        return String.format("%.2f %s", value, units[unitIndex])
    }
}
