package com.lanhe.gongjuxiang.activities

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
// 暂时禁用图表功能，避免依赖问题
// import com.github.mikephil.charting.components.XAxis
// import com.github.mikephil.charting.data.*
// import com.github.mikephil.charting.formatter.ValueFormatter
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityPerformanceComparisonBinding
import com.lanhe.gongjuxiang.models.PerformanceData
import com.lanhe.gongjuxiang.models.BatteryInfo
import com.lanhe.gongjuxiang.models.MemoryInfo
import com.lanhe.gongjuxiang.utils.PerformanceMonitor
import com.lanhe.gongjuxiang.utils.PerformanceMonitorManager
import com.lanhe.gongjuxiang.utils.ShizukuManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 性能对比展示Activity
 * 显示优化前后的性能对比数据
 */
class PerformanceComparisonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerformanceComparisonBinding
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var performanceManager: PerformanceMonitorManager

    // 优化前后的数据
    private var beforeData: PerformanceData? = null
    private var afterData: PerformanceData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerformanceComparisonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        performanceMonitor = PerformanceMonitor(this)
        performanceManager = PerformanceMonitorManager(this)

        setupToolbar()
        setupClickListeners()
        loadComparisonData()
        updateShizukuStatus()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "性能对比分析"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupClickListeners() {
        binding.btnRefreshData.setOnClickListener {
            refreshCurrentData()
        }

        binding.btnStartRealtimeMonitor.setOnClickListener {
            startRealtimeMonitoring()
        }

        binding.btnStopMonitor.setOnClickListener {
            stopRealtimeMonitoring()
        }
    }

    private fun loadComparisonData() {
        // 从Intent中获取优化前的数据
        beforeData = intent.getSerializableExtra("before_data") as? PerformanceData

        // 如果没有传入数据，则模拟一些数据用于演示
        if (beforeData == null) {
            beforeData = PerformanceData(
                timestamp = System.currentTimeMillis(),
                cpuUsage = 45.2f,
                memoryUsage = MemoryInfo(
                    total = 8L * 1024 * 1024 * 1024, // 8GB
                    available = 2L * 1024 * 1024 * 1024, // 2GB
                    used = 6L * 1024 * 1024 * 1024, // 6GB
                    usagePercent = 75f
                ),
                totalMemory = 8L * 1024 * 1024 * 1024, // 8GB
                availableMemory = 2L * 1024 * 1024 * 1024, // 2GB
                storageUsage = 60f,
                totalStorage = 128L * 1024 * 1024 * 1024, // 128GB
                availableStorage = 50L * 1024 * 1024 * 1024, // 50GB
                batteryInfo = BatteryInfo(
                    level = 65,
                    temperature = 38.5f,
                    voltage = 4.2f,
                    current = 0.0f,
                    status = 0,
                    health = 0,
                    technology = "Li-ion",
                    capacity = 4000L,
                    isCharging = false,
                    chargeType = "None",
                    timeToFull = 0L,
                    timeToEmpty = 0L
                ),
                batteryLevel = 65,
                batteryTemperature = 38.5f,
                isCharging = false,
                networkType = "WiFi",
                wifiSignalStrength = -45,
                mobileSignalStrength = 0,
                deviceTemperature = 35.0f
            )
        }

        // 获取当前数据作为优化后的数据
        lifecycleScope.launch {
            // 等待一段时间让性能监控器收集数据
            delay(1000)
            afterData = performanceManager.getCurrentPerformance()

            updateComparisonDisplay()
            setupCharts()
            animateComparisonCards()
        }
    }

    private fun updateComparisonDisplay() {
        beforeData?.let { before ->
            afterData?.let { after ->
                // CPU对比
                val cpuImprovement = before.cpuUsage - after.cpuUsage
                updateComparisonCard(
                    binding.cardCpu,
                    "CPU使用率",
                    String.format("%.1f%%", before.cpuUsage),
                    String.format("%.1f%%", after.cpuUsage),
                    cpuImprovement,
                    "%"
                )

                // 内存对比
                val memoryImprovement = before.memoryUsage.usagePercent - after.memoryUsage.usagePercent
                updateComparisonCard(
                    binding.cardMemory,
                    "内存使用率",
                    "${before.memoryUsage.usagePercent}%",
                    "${after.memoryUsage.usagePercent}%",
                    memoryImprovement.toFloat(),
                    "%"
                )

                // 电池对比
                val batteryImprovement = before.batteryInfo.level - after.batteryInfo.level
                updateComparisonCard(
                    binding.cardBattery,
                    "电池电量",
                    "${before.batteryInfo.level}%",
                    "${after.batteryInfo.level}%",
                    batteryImprovement.toFloat(),
                    "%"
                )

                // 温度对比
                val tempImprovement = before.batteryInfo.temperature - after.batteryInfo.temperature
                updateComparisonCard(
                    binding.cardTemperature,
                    "电池温度",
                    String.format("%.1f°C", before.batteryInfo.temperature),
                    String.format("%.1f°C", after.batteryInfo.temperature),
                    tempImprovement,
                    "°C"
                )

                // 计算总体优化评分
                val overallScore = calculateOverallScore(before, after)
                binding.tvOverallScore.text = "${overallScore.roundToInt()}"

                // 显示优化建议
                updateOptimizationSuggestions(before, after)
            }
        }
    }

    private fun updateComparisonCard(
        card: View,
        title: String,
        beforeValue: String,
        afterValue: String,
        improvement: Float,
        unit: String
    ) {
        // 这里应该更新卡片的内容
        // 由于使用了ViewBinding，这里需要根据实际的布局来调整
    }

    private fun setupCharts() {
        // 暂时禁用图表功能
        // setupCpuChart()
        // setupMemoryChart()
        // setupBatteryChart()
    }

    private fun setupCpuChart() {
        // 暂时禁用图表功能
        /*
        val entries = ArrayList<BarEntry>()
        beforeData?.let { before ->
            afterData?.let { after ->
                entries.add(BarEntry(0f, before.cpuUsage))
                entries.add(BarEntry(1f, after.cpuUsage))
            }
        }

        val dataSet = BarDataSet(entries, "CPU使用率")
        dataSet.colors = listOf(Color.RED, Color.GREEN)
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        binding.cpuChart.data = barData

        // 配置图表
        binding.cpuChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return when (value.toInt()) {
                        0 -> "优化前"
                        1 -> "优化后"
                        else -> ""
                    }
                }
            }
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            invalidate()
        }
        */
    }

    private fun setupMemoryChart() {
        // 暂时禁用图表功能
        /*
        val entries = ArrayList<PieEntry>()
        afterData?.let { data ->
            val usedPercent = data.memoryUsage.usagePercent.toFloat()
            val freePercent = (100 - data.memoryUsage.usagePercent).toFloat()

            entries.add(PieEntry(usedPercent, "已使用"))
            entries.add(PieEntry(freePercent, "可用"))
        }

        val dataSet = PieDataSet(entries, "内存使用情况")
        dataSet.colors = listOf(Color.RED, Color.GREEN)
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)
        binding.memoryChart.data = pieData

        binding.memoryChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            centerText = "内存使用"
            setCenterTextSize(14f)
            invalidate()
        }
        */
    }

    private fun setupBatteryChart() {
        // 暂时禁用图表功能
        /*
        val entries = ArrayList<Entry>()
        beforeData?.let { before ->
            afterData?.let { after ->
                entries.add(Entry(0f, before.batteryInfo.level.toFloat()))
                entries.add(Entry(1f, after.batteryInfo.level.toFloat()))
                entries.add(Entry(2f, after.batteryInfo.temperature))
            }
        }

        val lineDataSet = LineDataSet(entries, "电池状态")
        lineDataSet.color = Color.BLUE
        lineDataSet.valueTextSize = 12f
        lineDataSet.circleColors = listOf(Color.RED, Color.GREEN, Color.YELLOW)

        val lineData = LineData(lineDataSet)
        binding.batteryChart.data = lineData

        binding.batteryChart.apply {
            description.isEnabled = false
            legend.isEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return when (value.toInt()) {
                        0 -> "优化前电量"
                        1 -> "优化后电量"
                        2 -> "当前温度"
                        else -> ""
                    }
                }
            }
            axisRight.isEnabled = false
            invalidate()
        }
        */
    }

    private fun animateComparisonCards() {
        // 为对比卡片添加动画效果
        val cards = listOf(
            binding.cardCpu,
            binding.cardMemory,
            binding.cardBattery,
            binding.cardTemperature
        )

        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.animate()
                .alpha(1f)
                .setStartDelay(index * 200L)
                .setDuration(500)
                .start()
        }
    }

    private fun calculateOverallScore(before: PerformanceData, after: PerformanceData): Float {
        val cpuScore = (before.cpuUsage - after.cpuUsage) / before.cpuUsage * 100
        val memoryScore = (before.memoryUsage.usagePercent - after.memoryUsage.usagePercent).toFloat() / before.memoryUsage.usagePercent * 100
        val batteryScore = (before.batteryInfo.level - after.batteryInfo.level).toFloat() / before.batteryInfo.level * 100
        val tempScore = (before.batteryInfo.temperature - after.batteryInfo.temperature) / before.batteryInfo.temperature * 100

        return (cpuScore + memoryScore + batteryScore + tempScore) / 4
    }

    private fun updateOptimizationSuggestions(before: PerformanceData, after: PerformanceData) {
        val suggestions = mutableListOf<String>()

        if (after.cpuUsage > before.cpuUsage) {
            suggestions.add("• CPU使用率有所上升，建议检查后台应用")
        } else if (after.cpuUsage < before.cpuUsage * 0.8) {
            suggestions.add("• CPU优化效果显著，系统运行更加流畅")
        }

        if (after.memoryUsage.usagePercent > before.memoryUsage.usagePercent) {
            suggestions.add("• 内存使用率上升，建议清理后台应用")
        } else if (after.memoryUsage.usagePercent < before.memoryUsage.usagePercent * 0.9) {
            suggestions.add("• 内存优化效果良好，可用内存增加")
        }

        if (after.batteryInfo.temperature > before.batteryInfo.temperature) {
            suggestions.add("• 电池温度上升，注意设备散热")
        } else if (after.batteryInfo.temperature < before.batteryInfo.temperature * 0.95) {
            suggestions.add("• 电池温度降低，散热效果良好")
        }

        binding.tvOptimizationSuggestions.text = suggestions.joinToString("\n")
    }

    private fun refreshCurrentData() {
        lifecycleScope.launch {
            binding.btnRefreshData.isEnabled = false
            binding.btnRefreshData.text = "刷新中..."

            // 重新获取当前性能数据
            delay(1000) // 模拟刷新时间
            afterData = performanceManager.getCurrentPerformance()

            updateComparisonDisplay()
            setupCharts()

            binding.btnRefreshData.isEnabled = true
            binding.btnRefreshData.text = "刷新数据"
        }
    }

    private fun startRealtimeMonitoring() {
        performanceManager.startMonitoring()
        binding.btnStartRealtimeMonitor.visibility = View.GONE
        binding.btnStopMonitor.visibility = View.VISIBLE

        // 简化实时监控实现
        lifecycleScope.launch {
            while (performanceManager.isMonitoring()) {
                delay(2000) // 每2秒更新一次
                val currentData = performanceManager.getCurrentPerformance()
                currentData?.let { updateRealtimeData(it) }
            }
        }
    }

    private fun stopRealtimeMonitoring() {
        performanceManager.stopMonitoring()
        binding.btnStartRealtimeMonitor.visibility = View.VISIBLE
        binding.btnStopMonitor.visibility = View.GONE
    }

    private fun updateRealtimeData(data: PerformanceData) {
        // 更新实时数据显示
        binding.tvRealtimeCpu.text = String.format("%.1f%%", data.cpuUsage)
        binding.tvRealtimeMemory.text = "${data.memoryUsage.usagePercent}%"
        binding.tvRealtimeBattery.text = "${data.batteryInfo.level}%"
        binding.tvRealtimeTemp.text = String.format("%.1f°C", data.batteryInfo.temperature)
    }

    private fun updateShizukuStatus() {
        val isAvailable = ShizukuManager.isShizukuAvailable()
        binding.tvShizukuStatus.text = if (isAvailable) "已连接" else "未连接"
        binding.tvShizukuStatus.setTextColor(
            if (isAvailable) Color.GREEN else Color.RED
        )

        if (!isAvailable) {
            binding.btnRequestShizuku.visibility = View.VISIBLE
            binding.btnRequestShizuku.setOnClickListener {
                ShizukuManager.requestPermission(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateShizukuStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        performanceManager.stopMonitoring()
        performanceManager.cleanup()
    }
}
