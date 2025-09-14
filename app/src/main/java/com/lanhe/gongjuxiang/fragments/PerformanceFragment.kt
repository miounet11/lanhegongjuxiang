package com.lanhe.gongjuxiang.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lanhe.gongjuxiang.databinding.FragmentPerformanceBinding
import com.lanhe.gongjuxiang.utils.PerformanceMonitor
import com.lanhe.gongjuxiang.utils.SmartCleaner
import com.lanhe.gongjuxiang.utils.WifiOptimizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 性能监控面板Fragment
 * 显示实时的系统性能数据和优化建议
 */
class PerformanceFragment : Fragment() {

    private var _binding: FragmentPerformanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var smartCleaner: SmartCleaner
    private lateinit var wifiOptimizer: WifiOptimizer

    private var isMonitoring = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPerformanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化工具类
        performanceMonitor = PerformanceMonitor(requireContext())
        smartCleaner = SmartCleaner(requireContext())
        wifiOptimizer = WifiOptimizer(requireContext())

        // 设置界面
        setupUI()

        // 开始性能监控
        startPerformanceMonitoring()
    }

    private fun setupUI() {
        // 设置一键优化按钮
        binding.btnOneKeyOptimize.setOnClickListener {
            performOneKeyOptimization()
        }

        // 设置清理按钮
        binding.btnCleanJunk.setOnClickListener {
            performJunkClean()
        }

        // 设置WiFi优化按钮
        binding.btnOptimizeWifi.setOnClickListener {
            performWifiOptimization()
        }

        // 设置刷新按钮
        binding.btnRefresh.setOnClickListener {
            refreshAllData()
        }
    }

    private fun startPerformanceMonitoring() {
        isMonitoring = true

        viewLifecycleOwner.lifecycleScope.launch {
            while (isMonitoring) {
                try {
                    updatePerformanceData()
                    delay(2000) // 每2秒更新一次
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(5000) // 出错时等待5秒后重试
                }
            }
        }
    }

    private suspend fun updatePerformanceData() {
        // 更新CPU信息
        val cpuUsage = performanceMonitor.getCpuUsage()
        binding.tvCpuUsage.text = "${cpuUsage.totalUsage.roundToInt()}%"
        binding.progressCpu.progress = cpuUsage.totalUsage.roundToInt()

        // 更新内存信息
        val memoryInfo = performanceMonitor.getMemoryInfo()
        binding.tvMemoryUsage.text = "${memoryInfo.usagePercent.roundToInt()}%"
        binding.progressMemory.progress = memoryInfo.usagePercent.roundToInt()
        binding.tvMemoryDetails.text = "${memoryInfo.availableMemory}MB / ${memoryInfo.totalMemory}MB"

        // 更新存储信息
        val storageInfo = performanceMonitor.getStorageInfo()
        binding.tvStorageUsage.text = "${storageInfo.usagePercent.roundToInt()}%"
        binding.progressStorage.progress = storageInfo.usagePercent.roundToInt()
        binding.tvStorageDetails.text = "${storageInfo.availableSpace}GB / ${storageInfo.totalSpace}GB"

        // 更新WiFi信息
        val wifiInfo = wifiOptimizer.getCurrentWifiInfo()
        if (wifiInfo != null) {
            binding.tvWifiName.text = wifiInfo.ssid
            binding.tvWifiSpeed.text = "${wifiInfo.linkSpeed} Mbps"
            binding.tvWifiSignal.text = wifiOptimizer.getSignalLevel(wifiInfo.signalStrength)
            binding.progressWifi.progress = wifiOptimizer.calculateSignalQuality(wifiInfo.signalStrength)
        } else {
            binding.tvWifiName.text = "未连接"
            binding.tvWifiSpeed.text = "--"
            binding.tvWifiSignal.text = "--"
            binding.progressWifi.progress = 0
        }

        // 更新设备信息
        val deviceInfo = performanceMonitor.getDeviceInfo()
        binding.tvDeviceModel.text = deviceInfo.model
        binding.tvAndroidVersion.text = "Android ${deviceInfo.androidVersion}"
        binding.tvCpuCores.text = "${deviceInfo.cpuCores}核"
    }

    private fun performOneKeyOptimization() {
        binding.btnOneKeyOptimize.isEnabled = false
        binding.btnOneKeyOptimize.text = "优化中..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 执行一键优化
                val results = mutableListOf<String>()

                // 1. 清理垃圾文件
                val junkFiles = smartCleaner.scanJunkFiles()
                if (junkFiles.isNotEmpty()) {
                    val cleanResult = smartCleaner.performClean(junkFiles)
                    results.add("清理了 ${smartCleaner.formatFileSize(cleanResult.totalSize)} 垃圾文件")
                }

                // 2. WiFi优化建议
                val wifiInfo = wifiOptimizer.getCurrentWifiInfo()
                val wifiPerf = wifiOptimizer.performSpeedTest()
                val wifiOpt = wifiOptimizer.getOptimizationSuggestions(wifiInfo, wifiPerf)

                if (wifiOpt.priority >= 3) {
                    results.add("WiFi优化建议：${wifiOpt.suggestions.firstOrNull() ?: "检查网络连接"}")
                }

                // 3. 内存优化（这里可以添加更多优化逻辑）

                // 显示优化结果
                showOptimizationResults(results)

            } catch (e: Exception) {
                showOptimizationResults(listOf("优化过程中出现错误：${e.message}"))
            } finally {
                binding.btnOneKeyOptimize.isEnabled = true
                binding.btnOneKeyOptimize.text = "一键优化"
            }
        }
    }

    private fun performJunkClean() {
        binding.btnCleanJunk.isEnabled = false
        binding.btnCleanJunk.text = "扫描中..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val junkFiles = smartCleaner.scanJunkFiles()

                if (junkFiles.isEmpty()) {
                    showMessage("未发现可清理的垃圾文件")
                } else {
                    val totalSize = junkFiles.sumOf { it.size }
                    val message = "发现 ${junkFiles.size} 个垃圾文件，总计 ${smartCleaner.formatFileSize(totalSize)}"

                    // 这里可以显示详细的清理列表，让用户选择要清理的项目
                    showCleanDialog(junkFiles)
                }

            } catch (e: Exception) {
                showMessage("扫描失败：${e.message}")
            } finally {
                binding.btnCleanJunk.isEnabled = true
                binding.btnCleanJunk.text = "清理垃圾"
            }
        }
    }

    private fun performWifiOptimization() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val wifiInfo = wifiOptimizer.getCurrentWifiInfo()
                val wifiPerf = wifiOptimizer.performSpeedTest()
                val wifiOpt = wifiOptimizer.getOptimizationSuggestions(wifiInfo, wifiPerf)

                showWifiOptimizationResults(wifiOpt)

            } catch (e: Exception) {
                showMessage("WiFi优化失败：${e.message}")
            }
        }
    }

    private fun refreshAllData() {
        binding.btnRefresh.isEnabled = false
        binding.btnRefresh.text = "刷新中..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                updatePerformanceData()
                showMessage("数据已刷新")
            } catch (e: Exception) {
                showMessage("刷新失败：${e.message}")
            } finally {
                binding.btnRefresh.isEnabled = true
                binding.btnRefresh.text = "刷新"
            }
        }
    }

    private fun showOptimizationResults(results: List<String>) {
        val message = if (results.isEmpty()) {
            "未发现需要优化的项目"
        } else {
            "优化完成：\n" + results.joinToString("\n") { "• $it" }
        }

        showMessage(message)
    }

    private fun showWifiOptimizationResults(optimization: WifiOptimizer.WifiOptimization) {
        val message = buildString {
            append("WiFi优化建议 (优先级: ${optimization.priority}/5)\n")
            append("预期改善: ${optimization.estimatedImprovement}\n\n")
            optimization.suggestions.forEachIndexed { index, suggestion ->
                append("${index + 1}. $suggestion\n")
            }
        }

        showMessage(message)
    }

    private fun showCleanDialog(junkFiles: List<SmartCleaner.CleanItem>) {
        // 这里应该显示一个对话框让用户选择要清理的项目
        // 暂时显示总数
        val totalSize = junkFiles.sumOf { it.size }
        val message = "发现 ${junkFiles.size} 个项目，总计 ${smartCleaner.formatFileSize(totalSize)}\n\n是否开始清理？"
        showMessage(message)
    }

    private fun showMessage(message: String) {
        // 这里应该使用Toast或者Snackbar显示消息
        // 暂时使用TextView显示
        binding.tvStatus.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isMonitoring = false
        _binding = null
    }
}
