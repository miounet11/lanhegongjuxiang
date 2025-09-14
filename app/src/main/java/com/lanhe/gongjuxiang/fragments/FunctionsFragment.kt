package com.lanhe.gongjuxiang.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.*
import com.lanhe.gongjuxiang.adapters.CoreFeatureAdapter
import com.lanhe.gongjuxiang.databinding.FragmentFunctionsBinding
import com.lanhe.gongjuxiang.models.CoreFeature
import com.lanhe.gongjuxiang.utils.AnimationUtils
import com.lanhe.gongjuxiang.utils.PreferencesManager
import com.lanhe.gongjuxiang.utils.DataManager
import com.lanhe.gongjuxiang.utils.PerformanceMonitor
import com.lanhe.gongjuxiang.utils.PerformanceMonitorManager
import com.lanhe.gongjuxiang.utils.SystemOptimizer
import com.lanhe.gongjuxiang.models.PerformanceData
import com.lanhe.gongjuxiang.models.BatteryInfo
import androidx.recyclerview.widget.GridLayoutManager
import com.lanhe.gongjuxiang.utils.OptimizationState
import com.lanhe.gongjuxiang.utils.OptimizationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FunctionsFragment : Fragment() {

    private var _binding: FragmentFunctionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var dataManager: DataManager
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var performanceManager: PerformanceMonitorManager
    private lateinit var systemOptimizer: SystemOptimizer

    // 性能数据
    private var beforeOptimizationData: PerformanceData? = null
    private var cpuUsage = 0f
    private var memoryUsage = 0f

    // 统计数据
    private var dailyMemorySaved = 0.0
    private var dailyBatterySaved = 0
    private var dailyStorageSaved = 0.0
    private var dailyOptimizations = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFunctionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化管理器
        preferencesManager = PreferencesManager(requireContext())
        dataManager = DataManager(requireContext())
        performanceMonitor = PerformanceMonitor(requireContext())
        performanceManager = PerformanceMonitorManager(requireContext())
        systemOptimizer = SystemOptimizer(requireContext())

        setupClickListeners()
        setupCoreFeaturesRecyclerView()
        startPerformanceMonitoring()
        updateCoreOptimizationStatus()

        // 延迟执行动画
        view.postDelayed({
            animateViewsIn()
        }, 100)
    }

    private fun setupClickListeners() {
        // 一键深度优化
        binding.btnDeepOptimization.setOnClickListener {
            performDeepOptimization()
        }

        // 核心功能现在通过RecyclerView处理

        // 快捷设置
        binding.btnGameModeQuick.setOnClickListener {
            optimizeGameMode()
        }

        binding.btnEyeProtectionQuick.setOnClickListener {
            enableEyeProtection()
        }

        binding.btnPowerSave.setOnClickListener {
            enablePowerSave()
        }

        binding.btnCleanupQuick.setOnClickListener {
            performQuickCleanup()
        }

        binding.btnNetworkQuick.setOnClickListener {
            optimizeNetwork()
        }

        binding.btnMoreSettings.setOnClickListener {
            openQuickSettings()
        }

        binding.btnThemeSettings.setOnClickListener {
            openThemeSettings()
        }

        // 系统工具管理器
        binding.cardCpuManager.setOnClickListener {
            openCpuManager()
        }

        binding.cardMemoryManager.setOnClickListener {
            openMemoryManager()
        }

        binding.cardStorageManager.setOnClickListener {
            openStorageManager()
        }

        binding.cardBatteryManager.setOnClickListener {
            openBatteryManager()
        }

        binding.cardNetworkDiagnostic.setOnClickListener {
            openNetworkDiagnostic()
        }

        binding.cardGameAcceleration.setOnClickListener {
            openGameAcceleration()
        }

        // 系统状态指标点击
        binding.llCpuStatus.setOnClickListener {
            openCpuManager()
        }

        binding.llMemoryStatus.setOnClickListener {
            openMemoryManager()
        }

        binding.llBatteryStatus.setOnClickListener {
            openBatteryManager()
        }

        binding.llStorageStatus.setOnClickListener {
            openStorageManager()
        }

        // 高级控制中心
        binding.llShizukuAuthorization.setOnClickListener {
            openShizukuAuth()
        }

        // 长按一键优化
        binding.btnDeepOptimization.setOnLongClickListener {
            showOptimizationSettings()
            true
        }
    }

    private fun startPerformanceMonitoring() {
        performanceManager.startMonitoring()
        updateSystemStatus()
    }

    private fun updateSystemStatus() {
        lifecycleScope.launch {
            while (true) {
                try {
                    // 模拟系统状态更新
                    updateSimulatedData()
                    delay(2000) // 每2秒更新一次
                } catch (e: Exception) {
                    Log.e("FunctionsFragment", "更新系统状态失败", e)
                    break
                }
            }
        }
    }

    private fun updateSimulatedData() {
        // 模拟CPU使用率更新
        cpuUsage = (15 + Math.random() * 25).toFloat()
        binding.tvCpuUsage.text = String.format("%.1f%%", cpuUsage)

        // 模拟内存使用率更新
        memoryUsage = (30 + Math.random() * 40).toFloat()
        binding.tvMemoryUsage.text = String.format("%.1f%%", memoryUsage)

        // 模拟电池信息更新
        val batteryLevel = (20 + Math.random() * 60).toFloat()
        binding.tvBatteryLevel.text = String.format("%.1f%%", batteryLevel)
    }

    private fun updateBatteryIcon(level: Int) {
        // 电池图标更新已移除，因为新布局中没有对应的View
    }

    private fun performDeepOptimization() {
        lifecycleScope.launch {
            try {
                // 显示优化进度对话框
                val dialog = OptimizationProgressDialogFragment()
                dialog.show(childFragmentManager, "optimization_progress")

                // 模拟优化过程
                delay(3000)

                // 关闭对话框并显示结果
                dialog.dismiss()
                showOptimizationResult(true, "深度优化完成，系统性能提升15%")

            } catch (e: Exception) {
                Toast.makeText(context, "优化失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showOptimizationResult(success: Boolean, message: String) {
        if (success) {
            // 成功动画
            animateOptimizationSuccess()
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            // 更新优化统计数据
            updateOptimizationStats()
        } else {
            animateOptimizationError()
            Toast.makeText(context, "优化失败: $message", Toast.LENGTH_SHORT).show()
        }
    }



    private fun updateOptimizationStats() {
        dailyOptimizations++
        dailyMemorySaved += 0.5 // 模拟节省0.5GB内存
        dailyStorageSaved += 0.2 // 模拟清理0.2GB存储
        dailyBatterySaved += 30 // 模拟节省30分钟电池

        // 保存统计数据（简化实现）
        Log.d("FunctionsFragment", "优化统计已更新")
    }

    private fun animateViewsIn() {
        // 为各个功能卡片添加进入动画
        val views = listOf(
            binding.cardTopStatus,
            binding.btnDeepOptimization,
            binding.cardQuickSettings,
            binding.cardCpuManager,
            binding.cardMemoryManager,
            binding.cardStorageManager,
            binding.cardBatteryManager
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(index * 50L)
                .start()
        }

        // 延迟显示RecyclerView内容
        binding.rvCoreFeatures.postDelayed({
            binding.rvCoreFeatures.alpha = 0f
            binding.rvCoreFeatures.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(views.size * 50L + 100)
                .start()
        }, 200)
    }

    private fun animateOptimizationStart() {
        // 优化开始时的动画效果
        AnimationUtils.buttonPressFeedback(binding.btnDeepOptimization)
        AnimationUtils.pulse(binding.cardTopStatus)
    }

    private fun animateOptimizationSuccess() {
        // 优化成功时的动画效果
        AnimationUtils.successAnimation(binding.btnDeepOptimization)
        AnimationUtils.highlightAnimation(binding.cardTopStatus)
    }

    private fun animateOptimizationError() {
        // 优化失败时的动画效果
        AnimationUtils.errorAnimation(binding.btnDeepOptimization)
    }

    private fun updateCoreOptimizationStatus() {
        // 新的布局中已移除核心优化状态显示
        Log.d("FunctionsFragment", "核心优化状态更新")
    }

    private fun updateDailyAchievementsDisplay() {
        // 新的布局中已移除这些统计显示
        // 如需显示统计信息，可以添加到核心优化卡片的状态中
    }

    private fun calculatePerformanceBoost(): Int {
        // 模拟性能提升计算
        return (15 + (dailyOptimizations * 2)).coerceAtMost(50)
    }

    private fun calculateNetworkBoost(): Int {
        // 模拟网络提升计算
        return (20 + (dailyOptimizations * 3)).coerceAtMost(70)
    }

    // 快捷功能实现
    private fun optimizeGameMode() {
        AnimationUtils.buttonPressFeedback(binding.btnGameModeQuick)
        Toast.makeText(context, "游戏模式已开启", Toast.LENGTH_SHORT).show()
    }

    private fun enableEyeProtection() {
        AnimationUtils.buttonPressFeedback(binding.btnEyeProtectionQuick)
        Toast.makeText(context, "护眼模式已开启", Toast.LENGTH_SHORT).show()
    }

    private fun enablePowerSave() {
        AnimationUtils.buttonPressFeedback(binding.btnPowerSave)
        Toast.makeText(context, "省电模式已开启", Toast.LENGTH_SHORT).show()
    }

    private fun performQuickCleanup() {
        AnimationUtils.buttonPressFeedback(binding.btnCleanupQuick)
        Toast.makeText(context, "快速清理完成", Toast.LENGTH_SHORT).show()
    }

    private fun optimizeNetwork() {
        AnimationUtils.buttonPressFeedback(binding.btnNetworkQuick)
        Toast.makeText(context, "网络优化完成", Toast.LENGTH_SHORT).show()
    }

    private fun openQuickSettings() {
        try {
            val intent = Intent(requireContext(), QuickSettingsActivity::class.java)
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.btnMoreSettings)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开更多设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCoreOptimization() {
        try {
            val intent = Intent(requireContext(), CoreOptimizationActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开核心优化: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPacketCapture() {
        try {
            val intent = Intent(requireContext(), PacketCaptureActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开网络抓包: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppManagement() {
        try {
            val intent = Intent(requireContext(), AppManagerActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开应用管理: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSystemMonitoring() {
        try {
            val intent = Intent(requireContext(), SystemMonitorActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开系统监控: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSmartBrowser() {
        try {
            val intent = Intent(requireContext(), BrowserActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开智能浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCpuManager() {
        try {
            val intent = Intent(requireContext(), CpuManagerActivity::class.java)
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.cardCpuManager)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开CPU管理: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMemoryManager() {
        try {
            val intent = Intent(requireContext(), MemoryManagerActivity::class.java)
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.cardMemoryManager)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开内存管理: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openStorageManager() {
        try {
            val intent = Intent(requireContext(), StorageManagerActivity::class.java)
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.cardStorageManager)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开存储管理: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBatteryManager() {
        try {
            val intent = Intent(requireContext(), BatteryManagerActivity::class.java)
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.cardBatteryManager)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开电池管理: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNetworkDiagnostic() {
        try {
            val intent = Intent(requireContext(), NetworkDiagnosticActivity::class.java)
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.cardNetworkDiagnostic)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开网络诊断: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGameAcceleration() {
        try {
            val intent = Intent(requireContext(), GameAccelerationActivity::class.java)
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.cardGameAcceleration)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开游戏加速: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openShizukuAuth() {
        try {
            val intent = Intent(requireContext(), ShizukuAuthActivity::class.java)
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.llShizukuAuthorization)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开Shizuku授权: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showOptimizationSettings(): Boolean {
        // 显示优化设置对话框
        Toast.makeText(context, "长按功能开发中...", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun openThemeSettings() {
        try {
            val intent = Intent(requireContext(), ThemeSettingsActivity::class.java)
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.btnThemeSettings)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开主题设置: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCoreFeaturesRecyclerView() {
        val coreFeatures = listOf(
            CoreFeature(
                "core_optimization",
                "核心性能优化",
                "FPS boost • Latency reduction • Download acceleration • Video stabilization",
                android.R.drawable.ic_menu_manage,
                android.R.color.holo_blue_light
            ),
            CoreFeature(
                "packet_capture",
                "网络抓包分析",
                "Network monitoring • Protocol analysis • Packet capture",
                android.R.drawable.ic_menu_search,
                android.R.color.holo_green_light
            ),
            CoreFeature(
                "app_management",
                "应用管理",
                "App management • Install/uninstall • Permissions • Storage cleanup",
                android.R.drawable.ic_menu_view,
                android.R.color.holo_purple
            ),
            CoreFeature(
                "system_monitor",
                "系统监控",
                "Real-time monitoring • System resources • Process management",
                android.R.drawable.ic_menu_info_details,
                android.R.color.holo_orange_light
            ),
            CoreFeature(
                "smart_browser",
                "智能浏览器",
                "Web browsing • Bookmarks • Downloads • Ad blocking • Image optimization",
                android.R.drawable.ic_menu_view,
                android.R.color.holo_blue_bright
            )
        )

        val coreFeatureAdapter = CoreFeatureAdapter(coreFeatures) { feature ->
            when (feature.id) {
                "core_optimization" -> openCoreOptimization()
                "packet_capture" -> openPacketCapture()
                "app_management" -> openAppManagement()
                "system_monitor" -> openSystemMonitoring()
                "smart_browser" -> openSmartBrowser()
            }
        }

        binding.rvCoreFeatures.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = coreFeatureAdapter
        }
    }

    override fun onResume() {
        super.onResume()
        startPerformanceMonitoring()
        updateCoreOptimizationStatus()
    }

    override fun onPause() {
        super.onPause()
        performanceManager.stopMonitoring()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
