package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayoutMediator
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.ProcessListAdapter
import com.lanhe.gongjuxiang.databinding.ActivitySystemMonitorBinding
import com.lanhe.gongjuxiang.utils.AnimationUtils
import com.lanhe.gongjuxiang.utils.ShizukuManager
import com.lanhe.gongjuxiang.utils.SystemInfo
import com.lanhe.gongjuxiang.utils.PerformanceMetrics
import com.lanhe.gongjuxiang.utils.NetworkInfo
import com.lanhe.gongjuxiang.viewmodels.SystemMonitorViewModel
import com.lanhe.gongjuxiang.models.ProcessInfo  // 添加正确的ProcessInfo import
import kotlinx.coroutines.launch

/**
 * 系统监控仪表盘 - 类似macOS活动监视器
 * 展现强大的系统监控能力
 */
class SystemMonitorActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySystemMonitorBinding
    private val viewModel: SystemMonitorViewModel by viewModels()
    private lateinit var processAdapter: ProcessListAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateSystemStats()
            updateNetworkStats()
            updatePerformanceMetrics()
            handler.postDelayed(this, 1000) // 每1秒更新一次，更加实时
        }
    }

    // 性能指标数据
    private var baselineCpuUsage = 0f
    private var baselineMemoryUsage = 0L
    private var baselineNetworkLatency = 0L
    private var startTime = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySystemMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeViews()
        setupObservers()
        checkShizukuStatus()
        startMonitoring()
    }

    /**
     * 初始化视图
     */
    private fun initializeViews() {
        // 设置标题
        binding.tvTitle.text = "🔥 系统监控仪表盘"
        binding.tvSubtitle.text = "实时监控 • 深度分析 • 专业控制"

        // 设置进程列表
        processAdapter = ProcessListAdapter { process ->
            // 点击进程时的处理
            showProcessDetails(process)
        }
        binding.rvProcesses.layoutManager = LinearLayoutManager(this)
        binding.rvProcesses.adapter = processAdapter

        // 设置按钮点击事件
        binding.btnRefresh.setOnClickListener {
            updateSystemStats()
            AnimationUtils.buttonPressFeedback(it)
        }

        binding.btnKillProcess.setOnClickListener {
            killSelectedProcesses()
            AnimationUtils.buttonPressFeedback(it)
        }

        binding.btnSystemInfo.setOnClickListener {
            showSystemInfo()
            AnimationUtils.buttonPressFeedback(it)
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    /**
     * 设置观察者
     */
    private fun setupObservers() {
        // 观察Shizuku状态
        lifecycleScope.launch {
            ShizukuManager.shizukuState.collect { state ->
                updateShizukuStatus(state)
            }
        }

        // 观察系统统计数据
        viewModel.systemStats.observe(this) { stats ->
            updateSystemStatsDisplay(stats)
        }

        // 观察进程列表
        viewModel.processList.observe(this) { processes ->
            processAdapter.updateProcesses(processes)
            binding.tvProcessCount.text = "进程数量: ${processes.size}"
        }

        // 观察系统信息
        viewModel.systemInfo.observe(this) { info ->
            updateSystemInfoDisplay(info ?: SystemInfo())
        }
    }

    /**
     * 检查Shizuku状态
     */
    private fun checkShizukuStatus() {
        if (!ShizukuManager.isShizukuAvailable()) {
            showShizukuRequiredDialog()
        } else {
            binding.tvShizukuStatus.text = "✅ Shizuku已连接 - 高级功能已启用"
            binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        }
    }

    /**
     * 开始监控
     */
    private fun startMonitoring() {
        updateSystemStats()
        handler.post(updateRunnable)
    }

    /**
     * 停止监控
     */
    private fun stopMonitoring() {
        handler.removeCallbacks(updateRunnable)
    }

    /**
     * 更新系统统计
     */
    private fun updateSystemStats() {
        if (!ShizukuManager.isShizukuAvailable()) {
            // 如果Shizuku不可用，使用基本统计
            updateBasicStats()
            return
        }

        lifecycleScope.launch {
            try {
                // 获取CPU使用率
                val cpuUsage = ShizukuManager.getCpuUsage()

                // 获取内存信息
                val memoryInfo = ShizukuManager.getMemoryInfo()

                // 获取网络统计
                val networkStats = ShizukuManager.getNetworkStats()

                // 获取进程列表
                val processes = ShizukuManager.getRunningProcesses()

                // 获取系统信息
                val systemInfo = ShizukuManager.getSystemInfo()

                // 更新ViewModel
                viewModel.updateSystemStats(cpuUsage, memoryInfo, networkStats)
                viewModel.updateProcessList(processes)
                viewModel.updateSystemInfo(systemInfo)

                // 更新UI显示
                updateSystemStatsDisplay(viewModel.systemStats.value ?: SystemMonitorViewModel.SystemStats())

            } catch (e: Exception) {
                Toast.makeText(this@SystemMonitorActivity, "获取系统信息失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 更新网络状态
     */
    private fun updateNetworkStats() {
        lifecycleScope.launch {
            try {
                val networkInfo = ShizukuManager.getNetworkInfo()
                updateNetworkDisplay(networkInfo)
            } catch (e: Exception) {
                // 静默处理网络信息获取失败
            }
        }
    }

    /**
     * 更新性能指标
     */
    private fun updatePerformanceMetrics() {
        lifecycleScope.launch {
            try {
                val metrics = ShizukuManager.getPerformanceMetrics()
                updatePerformanceMetricsDisplay(metrics)

                // 计算性能提升
                calculatePerformanceImprovement(metrics)
            } catch (e: Exception) {
                // 静默处理性能指标获取失败
            }
        }
    }

    /**
     * 更新基本统计（当Shizuku不可用时）
     */
    private fun updateBasicStats() {
        lifecycleScope.launch {
            try {
                // 使用基本的系统信息获取方式
                val systemInfo = ShizukuManager.getSystemInfo()
                viewModel.updateSystemInfo(systemInfo)

                // 显示提示信息
                binding.tvShizukuStatus.text = "⚠️ Shizuku未连接 - 功能受限"
                binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_orange_dark))

            } catch (e: Exception) {
                Toast.makeText(this@SystemMonitorActivity, "获取基本信息失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 更新系统统计显示
     */
    private fun updateSystemStatsDisplay(stats: SystemMonitorViewModel.SystemStats) {
        // CPU使用率
        binding.tvCpuUsage.text = String.format("%.1f%%", stats.cpuUsage)
        binding.progressCpu.progress = stats.cpuUsage.toInt()

        // 内存使用率
        val memoryUsagePercent = if (stats.totalMemory > 0) {
            ((stats.totalMemory - stats.availableMemory).toFloat() / stats.totalMemory.toFloat()) * 100f
        } else 0f
        binding.tvMemoryUsage.text = String.format("%.1f%%", memoryUsagePercent)
        binding.progressMemory.progress = memoryUsagePercent.toInt()

        // 内存详情
        binding.tvMemoryTotal.text = formatBytes(stats.totalMemory)
        binding.tvMemoryUsed.text = formatBytes(stats.totalMemory - stats.availableMemory)
        binding.tvMemoryFree.text = formatBytes(stats.availableMemory)

        // 网络统计
        binding.tvNetworkRx.text = formatBytes(stats.networkRx)
        binding.tvNetworkTx.text = formatBytes(stats.networkTx)
    }

    /**
     * 更新网络显示
     */
    private fun updateNetworkDisplay(networkInfo: NetworkInfo) {
        // 这里需要添加网络信息的UI元素，或者更新现有的网络显示
        // 暂时使用Toast显示网络信息
        runOnUiThread {
            // 更新网络状态显示（如果有相关UI元素）
            // binding.tvNetworkType.text = networkInfo.type
            // binding.tvNetworkSpeed.text = "${networkInfo.downloadSpeed}/${networkInfo.uploadSpeed} Mbps"
            // binding.tvNetworkLatency.text = "${networkInfo.latency}ms"
        }
    }

    /**
     * 更新性能指标显示
     */
    private fun updatePerformanceMetricsDisplay(metrics: PerformanceMetrics) {
        runOnUiThread {
            // 这里可以更新性能指标的UI显示
            // 例如：图片加载时间、网络效率、内存节省等
        }
    }

    /**
     * 计算性能提升
     */
    private fun calculatePerformanceImprovement(currentMetrics: PerformanceMetrics) {
        // 计算相对于基准线的性能提升
        val cpuImprovement = if (baselineCpuUsage > 0f) {
            ((baselineCpuUsage - currentMetrics.cpuUsage) / baselineCpuUsage) * 100f
        } else 0f

        val memoryEfficiency = if (baselineMemoryUsage > 0L) {
            ((baselineMemoryUsage - currentMetrics.memoryUsed).toFloat() / baselineMemoryUsage.toFloat()) * 100f
        } else 0f

        // 更新性能提升显示
        runOnUiThread {
            // 这里可以更新性能提升的UI显示
            // 例如：显示"性能提升: ${cpuImprovement.toInt()}%"等
        }
    }

    /**
     * 更新系统信息显示
     */
    private fun updateSystemInfoDisplay(info: SystemInfo) {
        binding.tvKernelVersion.text = info.kernelVersion.take(30) + if (info.kernelVersion.length > 30) "..." else ""
        binding.tvUptime.text = formatUptime(info.uptime)
        binding.tvCpuCores.text = "${info.cpuCores} 核心"
        binding.tvBatteryLevel.text = "${info.batteryLevel}%"
    }

    /**
     * 更新Shizuku状态显示
     */
    private fun updateShizukuStatus(state: com.lanhe.gongjuxiang.utils.ShizukuState) {
        when (state) {
            com.lanhe.gongjuxiang.utils.ShizukuState.Granted -> {
                binding.tvShizukuStatus.text = "✅ Shizuku已连接 - 高级功能已启用"
                binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                binding.btnKillProcess.isEnabled = true
                binding.btnSystemInfo.isEnabled = true
            }
            com.lanhe.gongjuxiang.utils.ShizukuState.Denied -> {
                binding.tvShizukuStatus.text = "❌ Shizuku权限被拒绝"
                binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                binding.btnKillProcess.isEnabled = false
                binding.btnSystemInfo.isEnabled = false
            }
            com.lanhe.gongjuxiang.utils.ShizukuState.Unavailable -> {
                binding.tvShizukuStatus.text = "⚠️ Shizuku服务不可用"
                binding.tvShizukuStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                binding.btnKillProcess.isEnabled = false
                binding.btnSystemInfo.isEnabled = false
            }
            com.lanhe.gongjuxiang.utils.ShizukuState.Checking -> {
                binding.tvShizukuStatus.text = "⏳ 正在检查Shizuku状态..."
                binding.tvShizukuStatus.setTextColor(getColor(android.R.color.darker_gray))
                binding.btnKillProcess.isEnabled = false
                binding.btnSystemInfo.isEnabled = false
            }
        }
    }

    /**
     * 显示进程详情
     */
    private fun showProcessDetails(process: com.lanhe.gongjuxiang.models.ProcessInfo) {
        val memoryMB = process.memoryUsage / (1024 * 1024)
        val message = """
            进程名称: ${process.processName}
            包名: ${process.packageName}
            PID: ${process.pid}
            UID: ${process.uid}
            内存使用: ${memoryMB}MB
        """.trimIndent()

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * 杀死选中的进程
     */
    private fun killSelectedProcesses() {
        if (!ShizukuManager.isShizukuAvailable()) {
            Toast.makeText(this, "需要Shizuku权限才能杀死进程", Toast.LENGTH_SHORT).show()
            return
        }

        // 这里应该实现进程选择和杀死逻辑
        Toast.makeText(this, "进程管理功能开发中...", Toast.LENGTH_SHORT).show()
    }

    /**
     * 显示系统信息
     */
    private fun showSystemInfo() {
        val systemInfo = viewModel.systemInfo.value
        if (systemInfo != null) {
            val message = """
                内核版本: ${systemInfo.kernelVersion}
                运行时间: ${formatUptime(systemInfo.uptime)}
                CPU核心: ${systemInfo.cpuCores}
                总内存: ${formatBytes(systemInfo.totalMemory)}
                电池电量: ${systemInfo.batteryLevel}%
            """.trimIndent()

            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 显示Shizuku必需对话框
     */
    private fun showShizukuRequiredDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("需要Shizuku权限")
            .setMessage("系统监控功能需要Shizuku权限来访问系统级信息。请安装并启动Shizuku服务，然后授予权限。")
            .setPositiveButton("去设置") { _, _ ->
                ShizukuManager.requestPermission(this)
            }
            .setNegativeButton("稍后", null)
            .show()
    }

    /**
     * 格式化字节数
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes.toFloat() / (1024 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toFloat() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes.toFloat() / 1024)
            else -> "$bytes B"
        }
    }

    /**
     * 格式化运行时间
     */
    private fun formatUptime(seconds: Long): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60

        return when {
            days > 0 -> "${days}天 ${hours}小时 ${minutes}分钟"
            hours > 0 -> "${hours}小时 ${minutes}分钟"
            else -> "${minutes}分钟"
        }
    }

    override fun onResume() {
        super.onResume()
        startMonitoring()
    }

    override fun onPause() {
        super.onPause()
        stopMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }
}
