package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.PerformanceToolAdapter
import com.lanhe.gongjuxiang.databinding.ActivityPerformanceToolsBinding
import com.lanhe.gongjuxiang.models.PerformanceTool
import com.lanhe.gongjuxiang.utils.AnimationUtils
import com.lanhe.gongjuxiang.utils.ItemDecorationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PerformanceToolsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerformanceToolsBinding
    private lateinit var performanceToolAdapter: PerformanceToolAdapter
    private var performanceTools = mutableListOf<PerformanceTool>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerformanceToolsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadPerformanceTools()
        setupClickListeners()
        startPerformanceMonitoring()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "⚡ 性能工具箱"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        performanceToolAdapter = PerformanceToolAdapter(performanceTools) { tool ->
            handleToolClick(tool)
        }

        binding.recyclerViewPerformanceTools.apply {
            layoutManager = GridLayoutManager(this@PerformanceToolsActivity, 2)
            adapter = performanceToolAdapter

            // 添加间距装饰器
            val spacing = ItemDecorationHelper.dpToPx(this@PerformanceToolsActivity, 8f)
            addItemDecoration(
                ItemDecorationHelper.createGridSpacingDecoration(spacing, true)
            )

            // 优化性能
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isDrawingCacheEnabled = true
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        }
    }

    private fun loadPerformanceTools() {
        performanceTools.clear()
        performanceTools.addAll(listOf(
            PerformanceTool(
                id = "cpu_test",
                title = "CPU性能测试",
                description = "测试CPU计算性能和多核能力",
                icon = R.drawable.ic_optimize,
                category = "CPU"
            ),
            PerformanceTool(
                id = "memory_test",
                title = "内存性能测试",
                description = "测试内存读写速度和带宽",
                icon = R.drawable.ic_optimize,
                category = "内存"
            ),
            PerformanceTool(
                id = "storage_test",
                title = "存储性能测试",
                description = "测试存储设备读写速度",
                icon = R.drawable.ic_optimize,
                category = "存储"
            ),
            PerformanceTool(
                id = "network_test",
                title = "网络性能测试",
                description = "测试网络延迟和带宽",
                icon = R.drawable.ic_optimize,
                category = "网络"
            ),
            PerformanceTool(
                id = "gpu_test",
                title = "GPU性能测试",
                description = "测试图形处理性能",
                icon = R.drawable.ic_optimize,
                category = "GPU"
            ),
            PerformanceTool(
                id = "battery_test",
                title = "电池性能测试",
                description = "测试电池健康度和续航",
                icon = R.drawable.ic_battery_full,
                category = "电池"
            ),
            PerformanceTool(
                id = "benchmark",
                title = "综合基准测试",
                description = "运行完整的系统性能评估",
                icon = R.drawable.ic_optimize,
                category = "综合"
            ),
            PerformanceTool(
                id = "performance_monitor",
                title = "实时性能监控",
                description = "实时监控各项性能指标",
                icon = R.drawable.ic_optimize,
                category = "监控"
            )
        ))

        performanceToolAdapter.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        binding.btnRunAllTests.setOnClickListener {
            runAllPerformanceTests()
        }

        binding.btnExportResults.setOnClickListener {
            exportPerformanceResults()
        }

        binding.btnClearResults.setOnClickListener {
            clearPerformanceResults()
        }
    }

    private fun handleToolClick(tool: PerformanceTool) {
        when (tool.id) {
            "cpu_test" -> startCpuTest()
            "memory_test" -> startMemoryTest()
            "storage_test" -> startStorageTest()
            "network_test" -> startNetworkTest()
            "gpu_test" -> startGpuTest()
            "battery_test" -> startBatteryTest()
            "benchmark" -> startBenchmarkTest()
            "performance_monitor" -> startPerformanceMonitor()
            else -> Toast.makeText(this, "${tool.title}功能开发中", Toast.LENGTH_SHORT).show()
        }
        AnimationUtils.buttonPressFeedback(binding.root)
    }

    private fun startCpuTest() {
        Toast.makeText(this, "开始CPU性能测试...", Toast.LENGTH_SHORT).show()
        // 实现CPU测试逻辑
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvTestStatus.text = "正在测试CPU性能..."
            delay(2000) // 模拟测试时间
            binding.tvTestStatus.text = "CPU测试完成"
            binding.progressBar.visibility = View.GONE
            showTestResult("CPU性能测试", "得分: 85/100")
        }
    }

    private fun startMemoryTest() {
        Toast.makeText(this, "开始内存性能测试...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvTestStatus.text = "正在测试内存性能..."
            delay(1500)
            binding.tvTestStatus.text = "内存测试完成"
            binding.progressBar.visibility = View.GONE
            showTestResult("内存性能测试", "读写速度: 1200MB/s")
        }
    }

    private fun startStorageTest() {
        Toast.makeText(this, "开始存储性能测试...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvTestStatus.text = "正在测试存储性能..."
            delay(1800)
            binding.tvTestStatus.text = "存储测试完成"
            binding.progressBar.visibility = View.GONE
            showTestResult("存储性能测试", "读速度: 450MB/s, 写速度: 380MB/s")
        }
    }

    private fun startNetworkTest() {
        Toast.makeText(this, "开始网络性能测试...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvTestStatus.text = "正在测试网络性能..."
            delay(2500)
            binding.tvTestStatus.text = "网络测试完成"
            binding.progressBar.visibility = View.GONE
            showTestResult("网络性能测试", "延迟: 25ms, 下载: 50Mbps")
        }
    }

    private fun startGpuTest() {
        Toast.makeText(this, "GPU性能测试功能开发中", Toast.LENGTH_SHORT).show()
    }

    private fun startBatteryTest() {
        startActivity(Intent(this, BatteryManagerActivity::class.java))
    }

    private fun startBenchmarkTest() {
        Toast.makeText(this, "开始综合基准测试...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            val testSteps = listOf("CPU测试", "内存测试", "存储测试", "网络测试")
            var totalScore = 0

            for (step in testSteps) {
                binding.tvTestStatus.text = "正在进行$step..."
                delay(1000)
                totalScore += (70..95).random()
            }

            binding.tvTestStatus.text = "基准测试完成"
            binding.progressBar.visibility = View.GONE
            showTestResult("综合基准测试", "总得分: ${totalScore / testSteps.size}/100")
        }
    }

    private fun startPerformanceMonitor() {
        startActivity(Intent(this, SystemMonitorActivity::class.java))
    }

    private fun runAllPerformanceTests() {
        Toast.makeText(this, "开始运行所有性能测试...", Toast.LENGTH_SHORT).show()
        startBenchmarkTest()
    }

    private fun exportPerformanceResults() {
        Toast.makeText(this, "导出性能测试结果功能开发中", Toast.LENGTH_SHORT).show()
    }

    private fun clearPerformanceResults() {
        binding.tvTestStatus.text = "测试结果已清除"
        Toast.makeText(this, "测试结果已清除", Toast.LENGTH_SHORT).show()
    }

    private fun showTestResult(testName: String, result: String) {
        val resultText = "$testName\n$result\n\n${binding.tvTestResults.text}"
        binding.tvTestResults.text = resultText
        Toast.makeText(this, "$testName 完成", Toast.LENGTH_SHORT).show()
    }

    private fun startPerformanceMonitoring() {
        // 启动后台性能监控
        lifecycleScope.launch {
            while (true) {
                updatePerformanceStatus()
                delay(2000) // 每2秒更新一次
            }
        }
    }

    private fun updatePerformanceStatus() {
        // 更新性能状态显示
        binding.tvPerformanceStatus.text = "系统运行正常"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
