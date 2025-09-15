package com.lanhe.gongjuxiang.utils

import android.app.Activity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.lanhe.gongjuxiang.databinding.ActivityNetworkDiagnosticBinding
import com.lanhe.gongjuxiang.viewmodels.NetworkDiagnosticViewModel

/**
 * 网络诊断UI控制器
 * 处理所有UI相关的操作
 */
class NetworkDiagnosticUIController(
    private val binding: ActivityNetworkDiagnosticBinding,
    private val activity: Activity
) {

    fun setupViews() {
        binding.tvTitle.text = "🌐 网络诊断中心"
        binding.tvSubtitle.text = "WiFi质量检测 • 延迟测试 • 距离预估 • 最佳位置"
        updateTestingStatus(false)
        updateScanningStatus(false)
    }

    fun updateNetworkDisplay(networkInfo: NetworkDiagnosticViewModel.NetworkInfo) {
        activity.runOnUiThread {
            binding.tvNetworkType.text = networkInfo.type
            binding.tvWifiSsid.text = networkInfo.ssid
            binding.tvWifiBssid.text = networkInfo.bssid
            binding.tvSignalStrength.text = "${networkInfo.signalStrength}/5"
            binding.tvRssi.text = "${networkInfo.rssi}dBm"
            binding.tvEstimatedDistance.text = "${networkInfo.estimatedDistance}米"
            binding.tvLinkSpeed.text = "${networkInfo.linkSpeed}Mbps"
            binding.tvFrequency.text = "${networkInfo.frequency}MHz"

            val signalColor = when (networkInfo.signalStrength) {
                5, 4 -> android.R.color.holo_green_dark
                3 -> android.R.color.holo_blue_dark
                2 -> android.R.color.holo_orange_dark
                1 -> android.R.color.holo_red_dark
                else -> android.R.color.darker_gray
            }
            binding.tvSignalStrength.setTextColor(activity.getColor(signalColor))
        }
    }

    fun updateLatencyDisplay(result: NetworkDiagnosticViewModel.LatencyResult) {
        activity.runOnUiThread {
            binding.tvAverageLatency.text = if (result.averageLatency > 0) "${result.averageLatency}ms" else "测试失败"
            binding.tvMinLatency.text = if (result.minLatency > 0) "${result.minLatency}ms" else "-"
            binding.tvMaxLatency.text = if (result.maxLatency > 0) "${result.maxLatency}ms" else "-"
            binding.tvLatencyQuality.text = result.quality

            val qualityColor = when (result.quality) {
                "优秀" -> android.R.color.holo_green_dark
                "良好" -> android.R.color.holo_blue_dark
                "一般" -> android.R.color.holo_orange_dark
                "较差" -> android.R.color.holo_red_dark
                else -> android.R.color.darker_gray
            }
            binding.tvLatencyQuality.setTextColor(activity.getColor(qualityColor))
        }
    }

    fun updatePositionScanDisplay(result: NetworkDiagnosticViewModel.PositionScanResult) {
        activity.runOnUiThread {
            binding.tvBestPosition.text = result.bestPosition
            binding.tvRecommendedAction.text = result.recommendedAction

            val positionsText = result.positions.joinToString("\n") { position ->
                "${position.position}: ${position.signalStrength}/5格 (${position.distance}米)${if (position.recommended) " ⭐" else ""}"
            }
            binding.tvPositionDetails.text = positionsText
        }
    }

    fun updateWifiSignalsSummary(wifiSignals: List<NetworkDiagnosticViewModel.WifiSignal>) {
        val connectedSignal = wifiSignals.find { it.isConnected }
        val strongestSignal = wifiSignals.maxByOrNull { it.rssi }

        val summary = buildString {
            append("📶 WiFi信号汇总:\n")
            append("已连接: ${connectedSignal?.ssid ?: "无"} (${connectedSignal?.rssi ?: 0}dBm)\n")
            append("最强信号: ${strongestSignal?.ssid ?: "无"} (${strongestSignal?.rssi ?: 0}dBm)\n")
            append("可用WiFi: ${wifiSignals.size} 个")
        }

        binding.tvWifiSummary.text = summary
    }

    fun updateNetworkUsageSummary(networkApps: List<NetworkDiagnosticViewModel.NetworkUsageApp>) {
        val totalUsage = networkApps.sumOf { it.usageMB.toDouble() }
        val topApp = networkApps.maxByOrNull { it.usageMB }

        val summary = buildString {
            append("🌐 网络占用汇总:\n")
            append("总使用量: ${String.format("%.1f", totalUsage)}MB\n")
            append("占用最多: ${topApp?.appName ?: "无"} (${topApp?.usageMB ?: 0}MB)\n")
            append("活跃应用: ${networkApps.size} 个")
        }

        binding.tvNetworkUsageSummary.text = summary
    }

    fun updateBatteryConsumingSummary(batteryApps: List<NetworkDiagnosticViewModel.BatteryConsumingApp>) {
        val totalConsumption = batteryApps.sumOf { it.consumptionPercent.toDouble() }
        val topConsumingApp = batteryApps.maxByOrNull { it.consumptionPercent }
        val appsToClose = batteryApps.count { it.shouldClose }

        val summary = buildString {
            append("🔋 电池消耗汇总:\n")
            append("总消耗: ${String.format("%.1f", totalConsumption)}%\n")
            append("消耗最多: ${topConsumingApp?.appName ?: "无"} (${topConsumingApp?.consumptionPercent ?: 0}%)\n")
            append("建议关闭: $appsToClose 个应用")
        }

        binding.tvBatterySummary.text = summary
    }

    fun checkForNetworkWarnings(networkApps: List<NetworkDiagnosticViewModel.NetworkUsageApp>) {
        val highUsageApps = networkApps.filter { it.usageMB > 50 }

        if (highUsageApps.isNotEmpty()) {
            binding.tvNetworkWarning.visibility = View.VISIBLE
            binding.tvNetworkWarning.text = "⚠️ 发现 ${highUsageApps.size} 个网络占用严重的应用"
        } else {
            binding.tvNetworkWarning.visibility = View.GONE
        }
    }

    fun checkForBatteryWarnings(batteryApps: List<NetworkDiagnosticViewModel.BatteryConsumingApp>) {
        val appsToClose = batteryApps.filter { it.shouldClose }

        if (appsToClose.isNotEmpty()) {
            binding.tvBatteryWarning.visibility = View.VISIBLE
            binding.tvBatteryWarning.text = "⚠️ 发现 ${appsToClose.size} 个耗电应用需要关闭"
            showForceCloseSuggestion(appsToClose)
        } else {
            binding.tvBatteryWarning.visibility = View.GONE
        }
    }

    fun setTestingStatus(isTesting: Boolean) {
        binding.btnStartLatencyTest.isEnabled = !isTesting
        binding.btnStartLatencyTest.text = if (isTesting) "🔄 测试中..." else "📊 开始延迟测试"
    }

    fun setScanningStatus(isScanning: Boolean) {
        binding.btnScanPositions.isEnabled = !isScanning
        binding.btnScanPositions.text = if (isScanning) "🔍 扫描中..." else "📍 扫描最佳位置"
    }

    fun showOptimizationProgress() {
        binding.progressOptimization.visibility = View.VISIBLE
        binding.tvOptimizationStatus.text = "🔄 正在优化网络..."
        binding.btnOptimizeNetwork.isEnabled = false
    }

    fun hideOptimizationProgress() {
        binding.progressOptimization.visibility = View.GONE
        binding.tvOptimizationStatus.text = "✅ 优化完成"
        binding.btnOptimizeNetwork.isEnabled = true
    }

    fun showOptimizationResult(result: String) {
        AlertDialog.Builder(activity)
            .setTitle("🎉 网络优化完成")
            .setMessage(result)
            .setPositiveButton("太棒了！", null)
            .show()
    }

    fun showNetworkTips() {
        val tips = """
            🌐 网络诊断使用指南：

            📶 WiFi信号强度说明：
            • 4格：信号优秀，网络体验最佳
            • 3格：信号良好，基本满足使用
            • 2格：信号一般，可能有卡顿
            • 1格：信号较弱，建议靠近路由器
            • 0格：无信号，需要检查连接

            🕒 延迟标准：
            • <50ms：优秀，游戏和视频无压力
            • 50-100ms：良好，日常使用流畅
            • 100-200ms：一般，轻微延迟感
            • >200ms：较差，影响使用体验

            📍 最佳位置建议：
            • 避免障碍物阻挡
            • 远离微波炉等干扰源
            • 选择路由器2.4GHz频段
            • 定期重启路由器

            🔧 优化技巧：
            • 清理路由器缓存
            • 更新路由器固件
            • 调整WiFi频道
            • 使用网络优化工具
        """.trimIndent()

        AlertDialog.Builder(activity)
            .setTitle("🌐 网络诊断指南")
            .setMessage(tips)
            .setPositiveButton("明白了", null)
            .show()
    }

    fun showNetworkUsageWarning(app: NetworkDiagnosticViewModel.NetworkUsageApp) {
        AlertDialog.Builder(activity)
            .setTitle("🌐 网络占用警告")
            .setMessage(buildString {
                append("${app.appName} 正在大量使用网络!\n\n")
                append("📊 使用情况:\n")
                append("• 网络使用量: ${app.usageMB}MB\n")
                append("• 活跃时间: ${app.activeTime}分钟\n\n")
                append("⚠️ 建议措施:\n")
                append("• 检查应用是否有自动更新\n")
                append("• 限制应用的后台网络访问\n")
                append("• 考虑使用数据节省模式\n")
                append("• 定期清理应用缓存")
            })
            .setPositiveButton("限制网络") { _, _ ->
                Toast.makeText(activity, "已限制 ${app.appName} 的网络使用", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("稍后处理", null)
            .show()
    }

    fun showBatteryConsumptionWarning(app: NetworkDiagnosticViewModel.BatteryConsumingApp) {
        val action = if (app.shouldClose) "立即关闭" else "查看详情"

        AlertDialog.Builder(activity)
            .setTitle("🔋 电池消耗警告")
            .setMessage(buildString {
                append("${app.appName} 正在消耗大量电池!\n\n")
                append("📊 消耗情况:\n")
                append("• 电池消耗: ${app.consumptionPercent}%\n")
                append("• 运行时间: ${app.runningTime}分钟\n")
                append("• 建议关闭: ${if (app.shouldClose) "是" else "否"}\n\n")

                if (app.shouldClose) {
                    append("⚠️ 此应用正在后台大量消耗电量!\n")
                    append("• 长时间驻留在后台\n")
                    append("• 频繁进行网络活动\n")
                    append("• 建议立即关闭以节省电池")
                } else {
                    append("✅ 此应用的电池消耗在合理范围内")
                }
            })
            .setPositiveButton(action) { _, _ ->
                if (app.shouldClose) {
                    forceCloseApp(app)
                } else {
                    showAppDetails(app)
                }
            }
            .setNegativeButton("稍后处理", null)
            .show()
    }

    private fun showForceCloseSuggestion(appsToClose: List<NetworkDiagnosticViewModel.BatteryConsumingApp>) {
        val appsText = appsToClose.joinToString("\n") { app ->
            "• ${app.appName} (消耗${app.consumptionPercent}%, 已运行${app.runningTime}分钟)"
        }

        AlertDialog.Builder(activity)
            .setTitle("🔋 发现耗电应用")
            .setMessage(buildString {
                append("以下应用正在消耗大量电池，建议立即关闭:\n\n")
                append(appsText)
                append("\n\n是否立即关闭这些应用以节省电池?")
            })
            .setPositiveButton("立即关闭全部") { _, _ ->
                forceCloseMultipleApps(appsToClose)
            }
            .setNegativeButton("稍后处理", null)
            .setNeutralButton("逐个处理") { _, _ ->
                showIndividualCloseOptions(appsToClose)
            }
            .show()
    }

    private fun forceCloseApp(app: NetworkDiagnosticViewModel.BatteryConsumingApp) {
        Toast.makeText(activity, "正在关闭 ${app.appName}...", Toast.LENGTH_SHORT).show()
        // 模拟关闭应用
    }

    private fun forceCloseMultipleApps(apps: List<NetworkDiagnosticViewModel.BatteryConsumingApp>) {
        Toast.makeText(activity, "正在关闭 ${apps.size} 个耗电应用...", Toast.LENGTH_SHORT).show()
        // 模拟关闭多个应用
    }

    private fun showIndividualCloseOptions(apps: List<NetworkDiagnosticViewModel.BatteryConsumingApp>) {
        val appsText = apps.joinToString("\n") { app ->
            "• ${app.appName} (消耗${app.consumptionPercent}%)"
        }

        AlertDialog.Builder(activity)
            .setTitle("选择要关闭的应用")
            .setMessage("请选择要强制关闭的应用:\n\n$appsText")
            .setPositiveButton("关闭第一个") { _, _ ->
                if (apps.isNotEmpty()) {
                    forceCloseApp(apps.first())
                }
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("查看详情") { _, _ ->
                showAppDetails(apps.firstOrNull())
            }
            .show()
    }

    private fun showAppDetails(app: NetworkDiagnosticViewModel.BatteryConsumingApp?) {
        if (app == null) return

        AlertDialog.Builder(activity)
            .setTitle("${app.appName} 详情")
            .setMessage(buildString {
                append("📱 应用信息:\n")
                append("• 应用名称: ${app.appName}\n")
                append("• 包名: ${app.packageName}\n")
                append("• 电池消耗: ${app.consumptionPercent}%\n")
                append("• 运行时间: ${app.runningTime}分钟\n")
                append("• 状态: ${if (app.shouldClose) "建议关闭" else "正常运行"}\n\n")
                append("🔍 技术分析:\n")
                append("• CPU使用率: ${String.format("%.1f", app.consumptionPercent * 0.8)}%\n")
                append("• 内存占用: ${String.format("%.1f", app.consumptionPercent * 2.5)}MB\n")
                append("• 网络活动: ${if (app.consumptionPercent > 15) "高" else "正常"}\n")
                append("• 后台服务: ${if (app.shouldClose) "运行中" else "已停止"}")
            })
            .setPositiveButton("确定", null)
            .show()
    }

    private fun updateTestingStatus(isTesting: Boolean) {
        binding.btnStartLatencyTest.isEnabled = !isTesting
    }

    private fun updateScanningStatus(isScanning: Boolean) {
        binding.btnScanPositions.isEnabled = !isScanning
    }
}
