package com.lanhe.gongjuxiang.activities

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
// 暂时禁用图表功能，避免依赖问题
// import com.github.mikephil.charting.components.XAxis
// import com.github.mikephil.charting.data.Entry
// import com.github.mikephil.charting.data.LineData
// import com.github.mikephil.charting.data.LineDataSet
// import com.github.mikephil.charting.formatter.ValueFormatter
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityBatteryManagerBinding
import com.lanhe.gongjuxiang.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 电池管理Activity
 * 提供电池监控、耗电分析和优化功能
 */
class BatteryManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBatteryManagerBinding
    private lateinit var batteryMonitor: BatteryMonitor

    private var isMonitoring = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatteryManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        batteryMonitor = BatteryMonitor(this)

        setupToolbar()
        setupClickListeners()
        setupCharts()
        updateBatteryInfo()
        showBatteryTips()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "电池管理"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupClickListeners() {
        // 开始/停止监控
        binding.btnToggleMonitor.setOnClickListener {
            toggleMonitoring()
        }

        // 电池优化
        binding.btnOptimizeBattery.setOnClickListener {
            performBatteryOptimization()
        }

        // 刷新数据
        binding.btnRefreshData.setOnClickListener {
            refreshBatteryData()
        }

        // 查看详细报告
        binding.btnViewReport.setOnClickListener {
            showDetailedReport()
        }
    }

    private fun setupCharts() {
        // 暂时禁用图表功能
        // setupBatteryLevelChart()
        // setupTemperatureChart()
    }

    private fun setupBatteryLevelChart() {
        // 暂时禁用图表功能
        /*
        val entries = ArrayList<Entry>()
        val history = batteryMonitor.getBatteryHistory(6) // 最近6小时

        history.forEachIndexed { index, dataPoint ->
            entries.add(Entry(index.toFloat(), dataPoint.level.toFloat()))
        }

        val dataSet = LineDataSet(entries, "电池电量")
        dataSet.color = Color.BLUE
        dataSet.setCircleColor(Color.BLUE)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.valueTextSize = 10f

        val lineData = LineData(dataSet)
        binding.batteryLevelChart.data = lineData

        binding.batteryLevelChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    if (index < history.size) {
                        val timestamp = history[index].timestamp
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        return sdf.format(Date(timestamp))
                    }
                    return ""
                }
            }
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 100f
            axisRight.isEnabled = false
            invalidate()
        }
        */
    }

    private fun setupTemperatureChart() {
        // 暂时禁用图表功能
        /*
        val entries = ArrayList<Entry>()
        val history = batteryMonitor.getBatteryHistory(6)

        history.forEachIndexed { index, dataPoint ->
            entries.add(Entry(index.toFloat(), dataPoint.temperature))
        }

        val dataSet = LineDataSet(entries, "电池温度")
        dataSet.color = Color.RED
        dataSet.setCircleColor(Color.RED)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.valueTextSize = 10f

        val lineData = LineData(dataSet)
        binding.temperatureChart.data = lineData

        binding.temperatureChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    if (index < history.size) {
                        val timestamp = history[index].timestamp
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        return sdf.format(Date(timestamp))
                    }
                    return ""
                }
            }
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            invalidate()
        }
        */
    }

    private fun updateBatteryInfo() {
        val batteryStats = batteryMonitor.getCurrentBatteryStats()
        val usageStats = batteryMonitor.getBatteryUsageStats()
        val lifeEstimate = batteryMonitor.estimateBatteryLife()

        // 更新电池基本信息
        binding.tvBatteryLevel.text = "${batteryStats.level}%"
        binding.tvBatteryTemp.text = String.format("%.1f°C", batteryStats.temperature)
        binding.tvBatteryVoltage.text = String.format("%.2fV", batteryStats.voltage)
        binding.tvBatteryHealth.text = batteryStats.healthStatus

        // 更新充电状态
        binding.tvChargingStatus.text = if (batteryStats.isCharging) {
            "充电中 (${batteryStats.chargingMethod})"
        } else {
            "未充电"
        }

        // 更新电池容量信息
        binding.tvDesignCapacity.text = "${batteryStats.designCapacity}mAh"
        binding.tvCurrentCapacity.text = "${batteryStats.currentCapacity}mAh"

        // 更新使用统计
        binding.tvScreenOnTime.text = formatTime(usageStats.screenOnTime)
        binding.tvScreenOnRatio.text = "${usageStats.screenOnRatio}%"

        // 更新电池寿命估算
        binding.tvRemainingTime.text = "${lifeEstimate.remainingHours}小时${lifeEstimate.remainingMinutes % 60}分钟"
        binding.tvBatteryStatus.text = lifeEstimate.status

        // 更新进度条
        updateProgressBars(batteryStats, usageStats)
    }

    private fun updateProgressBars(batteryStats: BatteryStats, usageStats: BatteryUsageStats) {
        // 电池电量进度条
        animateProgressBar(binding.progressBatteryLevel, batteryStats.level)

        // 屏幕使用率进度条
        animateProgressBar(binding.progressScreenOnRatio, usageStats.screenOnRatio)

        // 电池温度指示器
        val tempPercent = (batteryStats.temperature / 50f * 100f).toInt().coerceIn(0, 100)
        animateProgressBar(binding.progressTemperature, tempPercent)
    }

    private fun animateProgressBar(progressBar: android.widget.ProgressBar, targetProgress: Int) {
        val animator = ValueAnimator.ofInt(progressBar.progress, targetProgress)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            progressBar.progress = animation.animatedValue as Int
        }
        animator.start()
    }

    private fun showBatteryTips() {
        val tips = batteryMonitor.getBatteryOptimizationTips()

        val tipsText = if (tips.isEmpty()) {
            "✓ 电池状态良好，无需特别注意"
        } else {
            tips.joinToString("\n\n") { tip ->
                val icon = when (tip.severity) {
                    TipSeverity.HIGH -> "🔴"
                    TipSeverity.MEDIUM -> "🟡"
                    TipSeverity.LOW -> "🟢"
                }
                "$icon ${tip.title}\n${tip.description}\n💡 ${tip.action}"
            }
        }

        binding.tvBatteryTips.text = tipsText
    }

    private fun toggleMonitoring() {
        if (isMonitoring) {
            batteryMonitor.stopBatteryMonitoring()
            binding.btnToggleMonitor.text = "开始监控"
            isMonitoring = false
        } else {
            batteryMonitor.startBatteryMonitoring()
            binding.btnToggleMonitor.text = "停止监控"
            isMonitoring = true

            // 开始实时更新
            startRealtimeUpdate()
        }
    }

    private fun startRealtimeUpdate() {
        lifecycleScope.launch {
            while (isMonitoring) {
                updateBatteryInfo()
                delay(5000) // 每5秒更新一次
            }
        }
    }

    private fun performBatteryOptimization() {
        lifecycleScope.launch {
            binding.btnOptimizeBattery.isEnabled = false
            binding.btnOptimizeBattery.text = "优化中..."

            try {
                val optimizations = batteryMonitor.performBatteryOptimization()

                if (optimizations.isNotEmpty()) {
                    val message = "电池优化完成！\n\n" + optimizations.joinToString("\n• ") { "• $it" }
                    Toast.makeText(this@BatteryManagerActivity, message, Toast.LENGTH_LONG).show()

                    // 重新加载数据
                    delay(1000)
                    updateBatteryInfo()
                    setupCharts()
                    showBatteryTips()
                } else {
                    Toast.makeText(this@BatteryManagerActivity, "电池优化失败，请检查权限", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@BatteryManagerActivity, "优化过程中出现错误: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnOptimizeBattery.isEnabled = true
                binding.btnOptimizeBattery.text = "一键电池优化"
            }
        }
    }

    private fun refreshBatteryData() {
        lifecycleScope.launch {
            binding.btnRefreshData.isEnabled = false
            binding.btnRefreshData.text = "刷新中..."

            // 刷新数据
            updateBatteryInfo()
            setupCharts()
            showBatteryTips()

            delay(1000)

            binding.btnRefreshData.isEnabled = true
            binding.btnRefreshData.text = "刷新数据"
        }
    }

    private fun showDetailedReport() {
        // 这里可以启动详细报告Activity
        Toast.makeText(this, "详细电池报告功能即将推出", Toast.LENGTH_SHORT).show()
    }

    private fun formatTime(timeMillis: Long): String {
        val seconds = timeMillis / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return String.format("%d小时%d分钟", hours, minutes)
    }

    override fun onResume() {
        super.onResume()
        updateBatteryInfo()
    }

    override fun onPause() {
        super.onPause()
        if (isMonitoring) {
            batteryMonitor.stopBatteryMonitoring()
            isMonitoring = false
            binding.btnToggleMonitor.text = "开始监控"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryMonitor.stopBatteryMonitoring()
    }
}
