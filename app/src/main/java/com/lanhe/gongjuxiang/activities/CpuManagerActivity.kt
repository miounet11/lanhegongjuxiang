package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.CpuFunctionAdapter
import com.lanhe.gongjuxiang.databinding.ActivityCpuManagerBinding
import com.lanhe.gongjuxiang.models.CpuFunction
import com.lanhe.gongjuxiang.utils.AnimationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CpuManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCpuManagerBinding
    private lateinit var cpuFunctionAdapter: CpuFunctionAdapter
    private var cpuFunctions = mutableListOf<CpuFunction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCpuManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadCpuFunctions()
        setupClickListeners()
        startCpuMonitoring()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "🚀 CPU性能管理器"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        cpuFunctionAdapter = CpuFunctionAdapter(cpuFunctions) { function ->
            handleFunctionClick(function)
        }

        binding.recyclerViewCpuFunctions.apply {
            layoutManager = LinearLayoutManager(this@CpuManagerActivity)
            adapter = cpuFunctionAdapter
        }
    }

    private fun setupClickListeners() {
        // CPU频率优化
        binding.btnCpuFrequencyOptimize.setOnClickListener {
            optimizeCpuFrequency()
        }

        // CPU核心管理
        binding.btnCpuCoreManage.setOnClickListener {
            manageCpuCores()
        }

        // CPU调度优化
        binding.btnCpuSchedulerOptimize.setOnClickListener {
            optimizeCpuScheduler()
        }

        // 性能模式切换
        binding.btnPerformanceMode.setOnClickListener {
            switchPerformanceMode()
        }
    }

    private fun loadCpuFunctions() {
        // 模拟加载CPU功能列表
        cpuFunctions.clear()
        cpuFunctions.addAll(getCpuFunctionList())
        cpuFunctionAdapter.notifyDataSetChanged()
    }

    private fun getCpuFunctionList(): List<CpuFunction> {
        return listOf(
            CpuFunction(
                id = "cpu_info",
                name = "📊 CPU信息查询",
                description = "查看CPU型号、核心数、频率等详细信息",
                category = "信息查询",
                isEnabled = true,
                currentValue = "Qualcomm Snapdragon 8 Gen 2"
            ),
            CpuFunction(
                id = "cpu_frequency",
                name = "⚡ CPU频率调节",
                description = "调整CPU运行频率，平衡性能与功耗",
                category = "性能调节",
                isEnabled = true,
                currentValue = "1.8GHz - 3.2GHz"
            ),
            CpuFunction(
                id = "cpu_cores",
                name = "🔥 CPU核心管理",
                description = "控制CPU核心数量，优化多任务性能",
                category = "核心管理",
                isEnabled = true,
                currentValue = "8核心全开"
            ),
            CpuFunction(
                id = "cpu_scheduler",
                name = "🎯 CPU调度器优化",
                description = "优化进程调度策略，提升响应速度",
                category = "调度优化",
                isEnabled = false,
                currentValue = "CFS调度器"
            ),
            CpuFunction(
                id = "cpu_governor",
                name = "🏃 CPU调控器",
                description = "选择CPU频率调控策略",
                category = "频率控制",
                isEnabled = true,
                currentValue = "Performance"
            ),
            CpuFunction(
                id = "cpu_temperature",
                name = "🌡️ CPU温度监控",
                description = "实时监控CPU温度，预防过热",
                category = "温度管理",
                isEnabled = true,
                currentValue = "42°C"
            ),
            CpuFunction(
                id = "cpu_load_balance",
                name = "⚖️ 负载均衡",
                description = "智能分配任务到不同CPU核心",
                category = "负载管理",
                isEnabled = false,
                currentValue = "自动均衡"
            ),
            CpuFunction(
                id = "cpu_power_management",
                name = "🔋 CPU电源管理",
                description = "优化CPU电源使用，延长电池续航",
                category = "电源管理",
                isEnabled = true,
                currentValue = "智能节电"
            ),
            CpuFunction(
                id = "cpu_performance_boost",
                name = "🚀 性能增强",
                description = "临时提升CPU性能，应对重负载",
                category = "性能增强",
                isEnabled = false,
                currentValue = "标准模式"
            ),
            CpuFunction(
                id = "cpu_cache_optimization",
                name = "💾 缓存优化",
                description = "优化CPU缓存使用，提升数据访问速度",
                category = "缓存管理",
                isEnabled = true,
                currentValue = "智能缓存"
            )
        )
    }

    private fun handleFunctionClick(function: CpuFunction) {
        when (function.id) {
            "cpu_info" -> showCpuInfo()
            "cpu_frequency" -> showCpuFrequencySettings()
            "cpu_cores" -> showCpuCoreSettings()
            "cpu_scheduler" -> showCpuSchedulerSettings()
            "cpu_governor" -> showCpuGovernorSettings()
            "cpu_temperature" -> showCpuTemperatureMonitor()
            "cpu_load_balance" -> showLoadBalanceSettings()
            "cpu_power_management" -> showPowerManagementSettings()
            "cpu_performance_boost" -> showPerformanceBoostSettings()
            "cpu_cache_optimization" -> showCacheOptimizationSettings()
        }
    }

    private fun startCpuMonitoring() {
        lifecycleScope.launch {
            while (true) {
                updateCpuStats()
                delay(2000) // 每2秒更新一次
            }
        }
    }

    private fun updateCpuStats() {
        // 模拟更新CPU统计信息
        val cpuUsage = (20..80).random()
        val temperature = (35..55).random()
        val frequency = (1800..3200).random()

        binding.tvCpuUsage.text = "$cpuUsage%"
        binding.tvCpuTemperature.text = "${temperature}°C"
        binding.tvCpuFrequency.text = "${frequency}MHz"

        // 更新CPU使用率进度条
        binding.progressCpuUsage.progress = cpuUsage
    }

    private fun optimizeCpuFrequency() {
        lifecycleScope.launch {
            showOptimizationProgress("正在优化CPU频率...")
            delay(1500)
            hideOptimizationProgress()
            Toast.makeText(this@CpuManagerActivity, "CPU频率优化完成！", Toast.LENGTH_SHORT).show()
            AnimationUtils.successAnimation(binding.btnCpuFrequencyOptimize)
        }
    }

    private fun manageCpuCores() {
        Toast.makeText(this, "CPU核心管理功能", Toast.LENGTH_SHORT).show()
    }

    private fun optimizeCpuScheduler() {
        lifecycleScope.launch {
            showOptimizationProgress("正在优化CPU调度器...")
            delay(1200)
            hideOptimizationProgress()
            Toast.makeText(this@CpuManagerActivity, "CPU调度器优化完成！", Toast.LENGTH_SHORT).show()
            AnimationUtils.successAnimation(binding.btnCpuSchedulerOptimize)
        }
    }

    private fun switchPerformanceMode() {
        Toast.makeText(this, "性能模式切换功能", Toast.LENGTH_SHORT).show()
    }

    private fun showCpuInfo() {
        val info = """
            CPU详细信息：
            • 型号：Qualcomm Snapdragon 8 Gen 2
            • 架构：ARMv8.2-A
            • 核心数：8核心
            • 工艺：4nm
            • 缓存：8MB L3缓存
            • GPU：Adreno 740
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📊 CPU信息")
            .setMessage(info)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showCpuFrequencySettings() {
        Toast.makeText(this, "CPU频率设置", Toast.LENGTH_SHORT).show()
    }

    private fun showCpuCoreSettings() {
        Toast.makeText(this, "CPU核心设置", Toast.LENGTH_SHORT).show()
    }

    private fun showCpuSchedulerSettings() {
        Toast.makeText(this, "CPU调度器设置", Toast.LENGTH_SHORT).show()
    }

    private fun showCpuGovernorSettings() {
        Toast.makeText(this, "CPU调控器设置", Toast.LENGTH_SHORT).show()
    }

    private fun showCpuTemperatureMonitor() {
        Toast.makeText(this, "CPU温度监控", Toast.LENGTH_SHORT).show()
    }

    private fun showLoadBalanceSettings() {
        Toast.makeText(this, "负载均衡设置", Toast.LENGTH_SHORT).show()
    }

    private fun showPowerManagementSettings() {
        Toast.makeText(this, "电源管理设置", Toast.LENGTH_SHORT).show()
    }

    private fun showPerformanceBoostSettings() {
        Toast.makeText(this, "性能增强设置", Toast.LENGTH_SHORT).show()
    }

    private fun showCacheOptimizationSettings() {
        Toast.makeText(this, "缓存优化设置", Toast.LENGTH_SHORT).show()
    }

    private fun showOptimizationProgress(message: String) {
        binding.tvOptimizationStatus.text = message
        binding.tvOptimizationStatus.visibility = View.VISIBLE
        binding.progressOptimization.visibility = View.VISIBLE
    }

    private fun hideOptimizationProgress() {
        binding.tvOptimizationStatus.visibility = View.GONE
        binding.progressOptimization.visibility = View.GONE
    }
}
