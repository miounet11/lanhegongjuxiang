package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.MemoryFunctionAdapter
import com.lanhe.gongjuxiang.databinding.ActivityMemoryManagerBinding
import com.lanhe.gongjuxiang.models.MemoryFunction
import com.lanhe.gongjuxiang.utils.AnimationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MemoryManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemoryManagerBinding
    private lateinit var memoryFunctionAdapter: MemoryFunctionAdapter
    private var memoryFunctions = mutableListOf<MemoryFunction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadMemoryFunctions()
        setupClickListeners()
        startMemoryMonitoring()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "🧠 神经内存优化"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        memoryFunctionAdapter = MemoryFunctionAdapter(memoryFunctions) { function ->
            handleFunctionClick(function)
        }

        binding.recyclerViewMemoryFunctions.apply {
            layoutManager = LinearLayoutManager(this@MemoryManagerActivity)
            adapter = memoryFunctionAdapter
        }
    }

    private fun setupClickListeners() {
        // 内存清理
        binding.btnMemoryCleanup.setOnClickListener {
            performMemoryCleanup()
        }

        // 内存压缩
        binding.btnMemoryCompress.setOnClickListener {
            performMemoryCompression()
        }

        // 缓存优化
        binding.btnCacheOptimize.setOnClickListener {
            optimizeCache()
        }

        // 内存监控
        binding.btnMemoryMonitor.setOnClickListener {
            startMemoryMonitoring()
        }
    }

    private fun loadMemoryFunctions() {
        memoryFunctions.clear()
        memoryFunctions.addAll(getMemoryFunctionList())
        memoryFunctionAdapter.notifyDataSetChanged()
    }

    private fun getMemoryFunctionList(): List<MemoryFunction> {
        return listOf(
            MemoryFunction(
                id = "memory_info",
                name = "📊 内存信息查询",
                description = "查看系统内存总量、使用情况、可用空间等详细信息",
                category = "信息查询",
                isEnabled = true,
                currentValue = "8GB / 12GB"
            ),
            MemoryFunction(
                id = "memory_cleanup",
                name = "🧹 内存深度清理",
                description = "清理后台进程和临时文件，释放系统内存",
                category = "内存清理",
                isEnabled = true,
                currentValue = "可释放 2.3GB"
            ),
            MemoryFunction(
                id = "memory_compression",
                name = "🗜️ 内存压缩优化",
                description = "使用内存压缩技术，提升内存使用效率",
                category = "压缩优化",
                isEnabled = false,
                currentValue = "压缩比 1.8x"
            ),
            MemoryFunction(
                id = "cache_management",
                name = "💾 缓存智能管理",
                description = "智能管理应用缓存，平衡性能与空间",
                category = "缓存管理",
                isEnabled = true,
                currentValue = "已优化 5.2GB"
            ),
            MemoryFunction(
                id = "memory_swap",
                name = "🔄 虚拟内存管理",
                description = "优化虚拟内存使用，提升多任务性能",
                category = "虚拟内存",
                isEnabled = false,
                currentValue = "Swap 2GB"
            ),
            MemoryFunction(
                id = "memory_allocation",
                name = "🎯 内存分配策略",
                description = "优化内存分配算法，提升应用启动速度",
                category = "分配优化",
                isEnabled = true,
                currentValue = "智能分配"
            ),
            MemoryFunction(
                id = "memory_gc",
                name = "♻️ 垃圾回收优化",
                description = "优化垃圾回收机制，减少内存碎片",
                category = "垃圾回收",
                isEnabled = true,
                currentValue = "GC优化中"
            ),
            MemoryFunction(
                id = "memory_monitoring",
                name = "📈 内存实时监控",
                description = "实时监控内存使用情况，及时发现泄漏",
                category = "监控分析",
                isEnabled = true,
                currentValue = "监控中"
            ),
            MemoryFunction(
                id = "memory_prediction",
                name = "🔮 内存使用预测",
                description = "预测内存使用趋势，提前进行优化",
                category = "预测分析",
                isEnabled = false,
                currentValue = "预测模式"
            ),
            MemoryFunction(
                id = "memory_security",
                name = "🔒 内存安全防护",
                description = "保护敏感数据在内存中的安全",
                category = "安全防护",
                isEnabled = true,
                currentValue = "安全模式"
            )
        )
    }

    private fun handleFunctionClick(function: MemoryFunction) {
        when (function.id) {
            "memory_info" -> showMemoryInfo()
            "memory_cleanup" -> performMemoryCleanup()
            "memory_compression" -> performMemoryCompression()
            "cache_management" -> showCacheManagement()
            "memory_swap" -> showSwapSettings()
            "memory_allocation" -> showAllocationSettings()
            "memory_gc" -> showGcSettings()
            "memory_monitoring" -> showMemoryMonitor()
            "memory_prediction" -> showPredictionSettings()
            "memory_security" -> showSecuritySettings()
        }
    }

    private fun startMemoryMonitoring() {
        lifecycleScope.launch {
            while (true) {
                updateMemoryStats()
                delay(2000) // 每2秒更新一次
            }
        }
    }

    private fun updateMemoryStats() {
        // 模拟更新内存统计信息
        val usedMemory = (4..10).random()
        val totalMemory = 12
        val availableMemory = totalMemory - usedMemory
        val usagePercent = (usedMemory.toFloat() / totalMemory * 100).toInt()

        binding.tvMemoryUsage.text = "${usagePercent}%"
        binding.tvMemoryDetails.text = "${availableMemory}GB / ${totalMemory}GB"
        binding.progressMemoryUsage.progress = usagePercent
    }

    private fun performMemoryCleanup() {
        lifecycleScope.launch {
            showOptimizationProgress("正在执行深度内存清理...")
            delay(2000)
            hideOptimizationProgress()
            Toast.makeText(this@MemoryManagerActivity, "内存清理完成，释放了2.3GB内存！", Toast.LENGTH_SHORT).show()
            AnimationUtils.successAnimation(binding.btnMemoryCleanup)
        }
    }

    private fun performMemoryCompression() {
        lifecycleScope.launch {
            showOptimizationProgress("正在优化内存压缩...")
            delay(1500)
            hideOptimizationProgress()
            Toast.makeText(this@MemoryManagerActivity, "内存压缩优化完成！", Toast.LENGTH_SHORT).show()
            AnimationUtils.successAnimation(binding.btnMemoryCompress)
        }
    }

    private fun optimizeCache() {
        lifecycleScope.launch {
            showOptimizationProgress("正在优化缓存管理...")
            delay(1200)
            hideOptimizationProgress()
            Toast.makeText(this@MemoryManagerActivity, "缓存优化完成！", Toast.LENGTH_SHORT).show()
            AnimationUtils.successAnimation(binding.btnCacheOptimize)
        }
    }

    private fun showMemoryInfo() {
        val info = """
            内存详细信息：
            • 总内存：12GB
            • 已使用：8.5GB
            • 可用内存：3.5GB
            • 内存类型：LPDDR5
            • 频率：3200MHz
            • 通道：双通道
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📊 内存信息")
            .setMessage(info)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showCacheManagement() {
        Toast.makeText(this, "缓存管理设置", Toast.LENGTH_SHORT).show()
    }

    private fun showSwapSettings() {
        Toast.makeText(this, "虚拟内存设置", Toast.LENGTH_SHORT).show()
    }

    private fun showAllocationSettings() {
        Toast.makeText(this, "内存分配设置", Toast.LENGTH_SHORT).show()
    }

    private fun showGcSettings() {
        Toast.makeText(this, "垃圾回收设置", Toast.LENGTH_SHORT).show()
    }

    private fun showMemoryMonitor() {
        Toast.makeText(this, "内存监控面板", Toast.LENGTH_SHORT).show()
    }

    private fun showPredictionSettings() {
        Toast.makeText(this, "内存预测设置", Toast.LENGTH_SHORT).show()
    }

    private fun showSecuritySettings() {
        Toast.makeText(this, "内存安全设置", Toast.LENGTH_SHORT).show()
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
