package com.lanhe.gongjuxiang.utils

import kotlinx.coroutines.delay

/**
 * 位置扫描器
 * 处理WiFi位置扫描和最佳位置推荐
 */
class PositionScanner {

    data class PositionResult(
        val position: String,
        val signalStrength: Int,
        val rssi: Int,
        val distance: Double,
        val recommended: Boolean
    )

    data class PositionScanResult(
        val positions: List<PositionResult>,
        val bestPosition: String,
        val recommendedAction: String
    )

    /**
     * 执行位置扫描
     */
    suspend fun performPositionScan(): PositionScanResult {
        // 模拟位置扫描过程
        val scanResults = mutableListOf<PositionResult>()

        // 模拟不同位置的信号强度
        for (i in 1..5) {
            val mockRssi = -30 - (i * 5) // 模拟不同的信号强度
            val mockDistance = calculateDistanceFromSignal(mockRssi)

            val positionResult = PositionResult(
                position = "位置$i",
                signalStrength = android.net.wifi.WifiManager.calculateSignalLevel(mockRssi, 5),
                rssi = mockRssi,
                distance = mockDistance,
                recommended = i == 1 // 第一位置最佳
            )

            scanResults.add(positionResult)

            // 模拟扫描延迟
            delay(500)
        }

        val bestPosition = scanResults.maxByOrNull { it.signalStrength }

        return PositionScanResult(
            positions = scanResults,
            bestPosition = bestPosition?.position ?: "未知",
            recommendedAction = getPositionRecommendation(bestPosition)
        )
    }

    /**
     * 获取位置建议
     */
    private fun getPositionRecommendation(bestPosition: PositionResult?): String {
        return when {
            bestPosition == null -> "无法确定最佳位置，请重试"
            bestPosition.distance < 5 -> "当前位置信号良好，可保持"
            bestPosition.distance < 10 -> "建议靠近路由器5-10米"
            else -> "建议移动到更近的距离，目标距离小于${bestPosition.distance - 5}米"
        }
    }

    /**
     * 根据信号强度计算距离
     */
    private fun calculateDistanceFromSignal(rssi: Int): Double {
        if (rssi == -1) return 0.0

        // 使用信号传播模型估算距离
        val rssiAtOneMeter = -40.0
        val pathLossExponent = 3.0

        val distance = Math.pow(10.0, (rssiAtOneMeter - rssi) / (10.0 * pathLossExponent))
        return String.format("%.1f", distance).toDouble()
    }

    /**
     * 获取位置扫描建议
     */
    fun getPositionScanTips(): List<String> {
        return listOf(
            "📍 位置扫描最佳实践：",
            "• 选择路由器正前方位置",
            "• 避免墙壁和障碍物阻挡",
            "• 远离微波炉等干扰源",
            "• 选择2.4GHz频段获得更好覆盖",
            "• 定期重启路由器优化性能",
            "• 使用WiFi分析仪精确测量信号",
            "• 考虑升级到WiFi 6路由器"
        )
    }

    /**
     * 获取位置优化建议
     */
    fun getPositionOptimizationSuggestions(currentPosition: PositionResult): List<String> {
        val suggestions = mutableListOf<String>()

        when {
            currentPosition.distance < 5 -> {
                suggestions.add("✅ 当前位置信号优秀")
                suggestions.add("📍 可保持当前位置")
            }
            currentPosition.distance < 10 -> {
                suggestions.add("📍 建议靠近路由器")
                suggestions.add("🏠 移动到路由器5米范围内")
                suggestions.add("🔄 调整路由器天线方向")
            }
            currentPosition.distance < 20 -> {
                suggestions.add("📍 建议显著改善位置")
                suggestions.add("🏠 移动到路由器10米范围内")
                suggestions.add("🏢 考虑使用WiFi中继器")
                suggestions.add("📡 检查路由器固件更新")
            }
            else -> {
                suggestions.add("⚠️ 信号距离过远")
                suggestions.add("🏠 建议移动到路由器附近")
                suggestions.add("🏢 考虑使用网线连接")
                suggestions.add("📡 检查路由器功率设置")
                suggestions.add("🔄 考虑更换高功率路由器")
            }
        }

        return suggestions
    }
}
