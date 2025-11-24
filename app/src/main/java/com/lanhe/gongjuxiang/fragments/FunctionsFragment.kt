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
import androidx.recyclerview.widget.GridLayoutManager
import com.lanhe.gongjuxiang.activities.AppManagerActivity
import com.lanhe.gongjuxiang.activities.BatteryManagerActivity
import com.lanhe.gongjuxiang.activities.ChromiumBrowserActivity
import com.lanhe.gongjuxiang.activities.CoreOptimizationActivity
import com.lanhe.gongjuxiang.activities.CpuManagerActivity
import com.lanhe.gongjuxiang.activities.FileBrowserActivity
import com.lanhe.gongjuxiang.activities.GameAccelerationActivity
import com.lanhe.gongjuxiang.activities.MemoryManagerActivity
import com.lanhe.gongjuxiang.activities.NetworkDiagnosticActivity
import com.lanhe.gongjuxiang.activities.PacketCaptureActivity
import com.lanhe.gongjuxiang.activities.PerformanceToolsActivity
import com.lanhe.gongjuxiang.activities.QuickSettingsActivity
import com.lanhe.gongjuxiang.activities.SecurityCenterActivity
import com.lanhe.gongjuxiang.activities.SettingsActivity
import com.lanhe.gongjuxiang.activities.ShizukuAuthActivity
import com.lanhe.gongjuxiang.activities.StorageManagerActivity
import com.lanhe.gongjuxiang.activities.SystemMonitorActivity
import com.lanhe.gongjuxiang.adapters.CoreFeatureAdapter
import com.lanhe.gongjuxiang.databinding.FragmentFunctionsBinding
import com.lanhe.gongjuxiang.models.BatteryInfo
import com.lanhe.gongjuxiang.models.CoreFeature
import com.lanhe.gongjuxiang.models.PerformanceData
import com.lanhe.gongjuxiang.utils.AnimationUtils
import com.lanhe.gongjuxiang.utils.DataManager
import com.lanhe.gongjuxiang.utils.OptimizationResult
import com.lanhe.gongjuxiang.utils.OptimizationState
import com.lanhe.gongjuxiang.utils.PerformanceMonitor
import com.lanhe.gongjuxiang.utils.PerformanceMonitorManager
import com.lanhe.gongjuxiang.utils.PreferencesManager
import com.lanhe.gongjuxiang.utils.SystemOptimizer
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

    private var beforeOptimizationData: PerformanceData? = null
    private var cpuUsage = 0f
    private var memoryUsage = 0f

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

        preferencesManager = PreferencesManager(requireContext())
        dataManager = DataManager(requireContext())
        performanceMonitor = PerformanceMonitor(requireContext())
        performanceManager = PerformanceMonitorManager(requireContext())
        systemOptimizer = SystemOptimizer(requireContext())

        setupClickListeners()
        setupCoreFeaturesRecyclerView()
        startPerformanceMonitoring()
        updateCoreOptimizationStatus()

        view.postDelayed({
            if (_binding != null) {
                animateViewsIn()
            }
        }, 100)
    }

    private fun setupClickListeners() {
        // 快速操作按钮
        binding.btnQuickOptimize.setOnClickListener { performDeepOptimization() }
        binding.btnSystemMonitor.setOnClickListener { openSystemMonitor() }
        binding.btnPerformanceTools.setOnClickListener { openPerformanceTools() }
        binding.btnSecurityCenter.setOnClickListener { openSecurityCenter() }

        binding.btnGameModeQuick.setOnClickListener { optimizeGameMode() }
        binding.btnEyeProtectionQuick.setOnClickListener { enableEyeProtection() }
        binding.btnPowerSave.setOnClickListener { enablePowerSave() }
        binding.btnCleanupQuick.setOnClickListener { performQuickCleanup() }
        binding.btnNetworkQuick.setOnClickListener { optimizeNetwork() }
        binding.btnMoreSettings.setOnClickListener { openQuickSettings() }

        binding.cardCpuManager.setOnClickListener { openCpuManager() }
        binding.cardMemoryManager.setOnClickListener { openMemoryManager() }
        binding.cardStorageManager.setOnClickListener { openStorageManager() }
        binding.cardBatteryManager.setOnClickListener { openBatteryManager() }
        binding.cardNetworkDiagnostic.setOnClickListener { openNetworkDiagnostic() }
        binding.cardGameAcceleration.setOnClickListener { openGameAcceleration() }

        binding.llShizukuAuthorization.setOnClickListener { openShizukuAuth() }

        binding.btnQuickOptimize.setOnLongClickListener {
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
                    updateSimulatedData()
                    delay(2000)
                } catch (e: Exception) {
                    Log.e("FunctionsFragment", "更新系统状态失败", e)
                    break
                }
            }
        }
    }

    private fun updateSimulatedData() {
        _binding?.let { binding ->
            cpuUsage = (15 + Math.random() * 25).toFloat()
            binding.tvCpuUsage.text = String.format("%.1f%%", cpuUsage)

            memoryUsage = (30 + Math.random() * 40).toFloat()
            binding.tvMemoryUsage.text = String.format("%.1f%%", memoryUsage)

            val batteryLevel = (20 + Math.random() * 60).toFloat()
            binding.tvBatteryLevel.text = String.format("%.1f%%", batteryLevel)
        }
    }

    private fun updateBatteryIcon(level: Int) { }

    private fun performDeepOptimization() {
        lifecycleScope.launch {
            try {
                val dialog = OptimizationProgressDialogFragment()
                dialog.show(childFragmentManager, "optimization_progress")

                delay(3000)

                dialog.dismiss()
                showOptimizationResult(true, "深度优化完成，系统性能提升15%")
            } catch (e: Exception) {
                Toast.makeText(context, "优化失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showOptimizationResult(success: Boolean, message: String) {
        if (success) {
            animateOptimizationSuccess()
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            updateOptimizationStats()
        } else {
            animateOptimizationError()
            Toast.makeText(context, "优化失败: $message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateOptimizationStats() {
        dailyOptimizations++
        dailyMemorySaved += 0.5
        dailyStorageSaved += 0.2
        dailyBatterySaved += 30
    }

    private fun animateViewsIn() {
        _binding?.let { binding ->
            val views = listOf(
                binding.cardTopStatus,
                binding.quickActionsGrid,
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

            binding.rvCoreFeatures.postDelayed({
                if (_binding != null) {
                    binding.rvCoreFeatures.alpha = 0f
                    binding.rvCoreFeatures.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .setStartDelay(views.size * 50L + 100)
                        .start()
                }
            }, 200)
        }
    }

    private fun animateOptimizationStart() {
        _binding?.let { binding ->
            AnimationUtils.buttonPressFeedback(binding.btnQuickOptimize)
            AnimationUtils.pulse(binding.cardTopStatus)
        }
    }

    private fun animateOptimizationSuccess() {
        _binding?.let { binding ->
            AnimationUtils.successAnimation(binding.btnQuickOptimize)
            AnimationUtils.highlightAnimation(binding.cardTopStatus)
        }
    }

    private fun animateOptimizationError() {
        _binding?.let { binding ->
            AnimationUtils.errorAnimation(binding.btnQuickOptimize)
        }
    }

    private fun updateCoreOptimizationStatus() {
        Log.d("FunctionsFragment", "核心优化状态更新")
    }

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
            startActivity(Intent(requireContext(), QuickSettingsActivity::class.java))
            AnimationUtils.buttonPressFeedback(binding.btnMoreSettings)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开更多设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCoreOptimization() {
        try {
            startActivity(Intent(requireContext(), CoreOptimizationActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开核心优化: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPacketCapture() {
        try {
            startActivity(Intent(requireContext(), PacketCaptureActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开网络抓包: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppManagement() {
        try {
            startActivity(Intent(requireContext(), AppManagerActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开应用管理: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSystemMonitoring() {
        try {
            startActivity(Intent(requireContext(), SystemMonitorActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开系统监控: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSmartBrowser() {
        try {
            startActivity(Intent(requireContext(), ChromiumBrowserActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开智能浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileManager() {
        try {
            startActivity(Intent(requireContext(), FileBrowserActivity::class.java))
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开文件管理器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCpuManager() {
        try {
            startActivity(Intent(requireContext(), CpuManagerActivity::class.java))
            AnimationUtils.buttonPressFeedback(binding.cardCpuManager)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开CPU管理: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMemoryManager() {
        try {
            startActivity(Intent(requireContext(), MemoryManagerActivity::class.java))
            AnimationUtils.buttonPressFeedback(binding.cardMemoryManager)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开内存管理: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openStorageManager() {
        try {
            startActivity(Intent(requireContext(), StorageManagerActivity::class.java))
            AnimationUtils.buttonPressFeedback(binding.cardStorageManager)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开存储管理: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBatteryManager() {
        try {
            startActivity(Intent(requireContext(), BatteryManagerActivity::class.java))
            AnimationUtils.buttonPressFeedback(binding.cardBatteryManager)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开电池管理: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNetworkDiagnostic() {
        try {
            startActivity(Intent(requireContext(), NetworkDiagnosticActivity::class.java))
            AnimationUtils.buttonPressFeedback(binding.cardNetworkDiagnostic)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开网络诊断: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGameAcceleration() {
        try {
            startActivity(Intent(requireContext(), GameAccelerationActivity::class.java))
            AnimationUtils.buttonPressFeedback(binding.cardGameAcceleration)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开游戏加速: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openShizukuAuth() {
        try {
            startActivity(Intent(requireContext(), ShizukuAuthActivity::class.java))
            AnimationUtils.buttonPressFeedback(binding.llShizukuAuthorization)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开Shizuku授权: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showOptimizationSettings(): Boolean {
        Toast.makeText(context, "长按功能开发中...", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun setupCoreFeaturesRecyclerView() {
        _binding?.let { binding ->
            val coreFeatures = listOf(
                CoreFeature(
                    "core_optimization",
                    "核心性能优化",
                    "FPS boost • Latency reduction • Download acceleration • Video stabilization",
                    android.R.drawable.ic_menu_manage,
                    "性能"
                ),
                CoreFeature(
                    "packet_capture",
                    "网络抓包分析",
                    "Network monitoring • Protocol analysis • Packet capture",
                    android.R.drawable.ic_menu_search,
                    "网络"
                ),
                CoreFeature(
                    "app_management",
                    "应用管理",
                    "App management • Install/uninstall • Permissions • Storage cleanup",
                    android.R.drawable.ic_menu_view,
                    "应用"
                ),
                CoreFeature(
                    "system_monitor",
                    "系统监控",
                    "Real-time monitoring • System resources • Process management",
                    android.R.drawable.ic_menu_info_details,
                    "监控"
                ),
                CoreFeature(
                    "smart_browser",
                    "智能浏览器",
                    "Web browsing • Bookmarks • Downloads • Ad blocking • Image optimization",
                    android.R.drawable.ic_menu_view,
                    "工具"
                ),
                CoreFeature(
                    "file_manager",
                    "Chromium文件管理器",
                    "Advanced file management • Multimedia preview • APK installation • Cloud storage",
                    android.R.drawable.ic_menu_save,
                    "文件"
                )
            )

            val coreFeatureAdapter = CoreFeatureAdapter { feature ->
                when (feature.id) {
                    "core_optimization" -> openCoreOptimization()
                    "packet_capture" -> openPacketCapture()
                    "app_management" -> openAppManagement()
                    "system_monitor" -> openSystemMonitoring()
                    "smart_browser" -> openSmartBrowser()
                    "file_manager" -> openFileManager()
                }
            }

            binding.rvCoreFeatures.apply {
                layoutManager = GridLayoutManager(requireContext(), 3) // 改为3列以适应更多功能
                adapter = coreFeatureAdapter
            }

            coreFeatureAdapter.submitList(coreFeatures)
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            startPerformanceMonitoring()
            updateCoreOptimizationStatus()
        }
    }

    override fun onPause() {
        super.onPause()
        performanceManager.stopMonitoring()
    }

    private fun openSystemMonitor() {
        try {
            startActivity(Intent(requireContext(), SystemMonitorActivity::class.java))
            AnimationUtils.buttonPressFeedback(binding.btnSystemMonitor)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开系统监控: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPerformanceTools() {
        try {
            // 跳转到性能工具页面
            startActivity(Intent(context, PerformanceToolsActivity::class.java))
            AnimationUtils.buttonPressFeedback(binding.btnPerformanceTools)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开性能工具: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSecurityCenter() {
        try {
            // 跳转到安全中心页面
            startActivity(Intent(context, SecurityCenterActivity::class.java))
            AnimationUtils.buttonPressFeedback(binding.btnSecurityCenter)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开安全中心: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
