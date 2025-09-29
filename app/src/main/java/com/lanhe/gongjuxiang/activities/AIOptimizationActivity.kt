package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.ai.AIOptimizationManager
import com.lanhe.gongjuxiang.databinding.ActivityAiOptimizationBinding
import kotlinx.coroutines.launch

/**
 * AI智能优化界面
 */
class AIOptimizationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiOptimizationBinding
    private lateinit var aiManager: AIOptimizationManager
    private lateinit var suggestionAdapter: AIOptimizationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiOptimizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        aiManager = AIOptimizationManager(this)

        setupToolbar()
        setupViews()
        setupChips()
        performInitialAnalysis()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "AI智能优化"
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        // 设置RecyclerView
        suggestionAdapter = AIOptimizationAdapter { suggestion ->
            executeSuggestion(suggestion)
        }

        binding.recyclerViewSuggestions.apply {
            layoutManager = LinearLayoutManager(this@AIOptimizationActivity)
            adapter = suggestionAdapter
        }

        // 设置刷新按钮
        binding.fabRefresh.setOnClickListener {
            performAnalysis()
        }

        // 设置一键优化按钮
        binding.btnOneClickOptimize.setOnClickListener {
            performOneClickOptimization()
        }
    }

    private fun setupChips() {
        val chipData = listOf(
            "系统分析" to AIOptimizationManager.FEATURE_SYSTEM_ANALYSIS,
            "性能优化" to AIOptimizationManager.FEATURE_PERFORMANCE_OPTIMIZE,
            "电池优化" to AIOptimizationManager.FEATURE_BATTERY_OPTIMIZE,
            "内存优化" to AIOptimizationManager.FEATURE_MEMORY_OPTIMIZE,
            "网络优化" to AIOptimizationManager.FEATURE_NETWORK_OPTIMIZE,
            "安全检查" to AIOptimizationManager.FEATURE_SECURITY_CHECK
        )

        chipData.forEach { (label, feature) ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        performAnalysis(feature)
                    }
                }
            }
            binding.chipGroupFeatures.addView(chip)
        }

        // 默认选中第一个
        (binding.chipGroupFeatures.getChildAt(0) as? Chip)?.isChecked = true
    }

    private fun performInitialAnalysis() {
        performAnalysis()
    }

    private fun performAnalysis(feature: String = AIOptimizationManager.FEATURE_SYSTEM_ANALYSIS) {
        lifecycleScope.launch {
            showLoading(true)
            updateStatus("正在进行AI分析...")

            try {
                // 获取系统状态
                val systemStatus = aiManager.getCurrentSystemStatus()
                updateSystemStatus(systemStatus)

                // 进行AI分析
                val suggestions = aiManager.performAIAnalysis(feature)

                if (suggestions.isNotEmpty()) {
                    suggestionAdapter.submitList(suggestions)
                    updateStatus("AI分析完成，发现${suggestions.size}个优化建议")

                    // 更新优化评分
                    calculateAndUpdateScore(systemStatus)
                } else {
                    updateStatus("系统运行良好，暂无优化建议")
                }
            } catch (e: Exception) {
                updateStatus("AI分析失败: ${e.message}")
                Toast.makeText(this@AIOptimizationActivity, "分析失败，请重试", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateSystemStatus(status: AIOptimizationManager.SystemStatus) {
        binding.apply {
            // 更新系统状态卡片
            tvCpuUsage.text = "CPU: ${status.cpuUsage.toInt()}%"
            progressCpu.progress = status.cpuUsage.toInt()

            tvMemoryUsage.text = "内存: ${status.memoryUsage.toInt()}%"
            progressMemory.progress = status.memoryUsage.toInt()

            tvStorageUsage.text = "存储: ${status.storageUsage.toInt()}%"
            progressStorage.progress = status.storageUsage.toInt()

            tvBatteryLevel.text = "电池: ${status.batteryLevel}%"
            progressBattery.progress = status.batteryLevel

            tvNetworkType.text = "网络: ${status.networkType}"
            tvRunningApps.text = "运行中应用: ${status.runningApps}个"
            tvUptime.text = "运行时间: ${formatUptime(status.uptime)}"
            tvBatteryTemp.text = "温度: ${status.batteryTemperature}°C"
        }
    }

    private fun calculateAndUpdateScore(status: AIOptimizationManager.SystemStatus) {
        // 计算系统健康评分
        val cpuScore = (100 - status.cpuUsage) / 100f * 25
        val memoryScore = (100 - status.memoryUsage) / 100f * 25
        val storageScore = (100 - status.storageUsage) / 100f * 25
        val batteryScore = status.batteryLevel / 100f * 25

        val totalScore = (cpuScore + memoryScore + storageScore + batteryScore).toInt()

        binding.tvHealthScore.text = "$totalScore"
        binding.progressHealthScore.progress = totalScore

        // 更新健康状态描述
        val healthStatus = when {
            totalScore >= 90 -> "优秀"
            totalScore >= 75 -> "良好"
            totalScore >= 60 -> "一般"
            else -> "需要优化"
        }
        binding.tvHealthStatus.text = "系统健康状态: $healthStatus"

        // 更新颜色
        val color = when {
            totalScore >= 75 -> getColor(R.color.green)
            totalScore >= 50 -> getColor(R.color.yellow)
            else -> getColor(R.color.red)
        }
        binding.progressHealthScore.setIndicatorColor(color)
    }

    private fun executeSuggestion(suggestion: AIOptimizationManager.OptimizationSuggestion) {
        lifecycleScope.launch {
            showLoading(true)
            updateStatus("正在执行: ${suggestion.title}")

            val success = aiManager.executeOptimization(suggestion)

            if (success) {
                updateStatus("优化完成: ${suggestion.title}")
                Toast.makeText(
                    this@AIOptimizationActivity,
                    "优化成功: ${suggestion.estimatedImprovement}",
                    Toast.LENGTH_LONG
                ).show()

                // 重新分析
                performAnalysis()
            } else {
                updateStatus("优化失败: ${suggestion.title}")
                Toast.makeText(
                    this@AIOptimizationActivity,
                    "优化失败，请手动执行",
                    Toast.LENGTH_SHORT
                ).show()
            }

            showLoading(false)
        }
    }

    private fun performOneClickOptimization() {
        lifecycleScope.launch {
            showLoading(true)
            updateStatus("正在执行一键优化...")

            try {
                // 获取所有高优先级建议
                val suggestions = aiManager.performAIAnalysis()
                val highPrioritySuggestions = suggestions.filter {
                    it.priority == AIOptimizationManager.Priority.HIGH
                }

                if (highPrioritySuggestions.isNotEmpty()) {
                    var successCount = 0
                    highPrioritySuggestions.forEach { suggestion ->
                        updateStatus("优化中: ${suggestion.title}")
                        if (aiManager.executeOptimization(suggestion)) {
                            successCount++
                        }
                    }

                    updateStatus("一键优化完成，成功执行${successCount}/${highPrioritySuggestions.size}项")
                    Toast.makeText(
                        this@AIOptimizationActivity,
                        "优化完成！成功优化${successCount}项",
                        Toast.LENGTH_LONG
                    ).show()

                    // 重新分析显示结果
                    performAnalysis()
                } else {
                    updateStatus("系统状态良好，无需优化")
                    Toast.makeText(
                        this@AIOptimizationActivity,
                        "系统运行良好，暂无需要优化的项目",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                updateStatus("一键优化失败")
                Toast.makeText(
                    this@AIOptimizationActivity,
                    "优化失败: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.fabRefresh.isEnabled = !show
        binding.btnOneClickOptimize.isEnabled = !show
    }

    private fun updateStatus(status: String) {
        binding.tvStatus.text = status
    }

    private fun formatUptime(uptimeMillis: Long): String {
        val hours = uptimeMillis / 1000 / 3600
        val minutes = (uptimeMillis / 1000 % 3600) / 60
        return "${hours}小时${minutes}分钟"
    }
}