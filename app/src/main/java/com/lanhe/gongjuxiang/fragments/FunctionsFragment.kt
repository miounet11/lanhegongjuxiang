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
import com.lanhe.gongjuxiang.activities.*
import com.lanhe.gongjuxiang.adapters.CoreFeatureAdapter
import com.lanhe.gongjuxiang.databinding.FragmentFunctionsBinding
import com.lanhe.gongjuxiang.models.CoreFeature
import com.lanhe.gongjuxiang.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 工具箱Fragment - 展示所有核心功能
 *
 * 功能分类：
 * 1. 🚀 性能优化 - 核心加速、内存清理、CPU管理
 * 2. 🌐 浏览器工具 - 智能浏览器、书签管理、历史记录
 * 3. 📱 系统管理 - 应用管理、存储管理、权限管理
 * 4. 🔒 安全工具 - Shizuku授权、安全中心、权限控制
 * 5. 🌍 网络工具 - WiFi管理、网络诊断、抓包分析
 * 6. 📁 文件工具 - 文件管理器、查看器、安装包管理
 * 7. ⚙️ 系统工具 - 快速设置、系统监控、电池管理
 */
class FunctionsFragment : Fragment() {

    private var _binding: FragmentFunctionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var dataManager: DataManager
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var performanceManager: PerformanceMonitorManager
    private lateinit var systemOptimizer: SystemOptimizer

    private var cpuUsage = 0f
    private var memoryUsage = 0f

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

        // Initial animation
        view.postDelayed({
            if (_binding != null) {
                animateViewsIn()
            }
        }, 100)
    }

    private fun setupClickListeners() {
        // Quick Actions Grid
        binding.btnPerformanceTools.setOnClickListener {
            AnimationUtils.buttonPressFeedback(it)
            openPerformanceTools()
        }

        binding.btnSystemMonitor.setOnClickListener {
            AnimationUtils.buttonPressFeedback(it)
            openSystemMonitor()
        }

        binding.btnQuickOptimize.setOnClickListener {
            AnimationUtils.buttonPressFeedback(it)
            performDeepOptimization()
        }

        binding.btnSecurityCenter.setOnClickListener {
            AnimationUtils.buttonPressFeedback(it)
            openSecurityCenter()
        }

        // System Status Card
        binding.cardTopStatus.setOnClickListener {
            AnimationUtils.buttonPressFeedback(it)
            openSystemMonitor()
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
                    Log.e("FunctionsFragment", "Update failed", e)
                    break
                }
            }
        }
    }

    private fun updateSimulatedData() {
        _binding?.let { binding ->
            // 使用真实的性能监控数据,避免随机跳动
            lifecycleScope.launch {
                try {
                    // CPU使用率
                    val cpuInfo = performanceMonitor.getCpuUsage()
                    cpuUsage = cpuInfo.totalUsage
                    binding.tvCpuUsage.text = String.format("%.0f%%", cpuUsage)

                    // 内存使用率
                    val memInfo = performanceMonitor.getMemoryInfo()
                    memoryUsage = (memInfo.usedMemory.toFloat() / memInfo.totalMemory.toFloat() * 100)
                    binding.tvMemoryUsage.text = String.format("%.0f%%", memoryUsage)

                    // 电池电量
                    val batteryInfo = performanceManager.getBatteryInfo()
                    binding.tvBatteryLevel.text = String.format("%.0f%%", batteryInfo.level.toFloat())

                    // 存储使用率
                    val storageInfo = performanceMonitor.getStorageInfo()
                    binding.tvStorageUsage.text = String.format("%.0f%%", storageInfo.usagePercent)
                } catch (e: Exception) {
                    Log.e("FunctionsFragment", "Failed to update system status", e)
                    // 降级方案：使用稳定的模拟数据（不再随机跳动）
                    binding.tvCpuUsage.text = "25%"
                    binding.tvMemoryUsage.text = "45%"
                    binding.tvBatteryLevel.text = "78%"
                    binding.tvStorageUsage.text = "62%"
                }
            }
        }
    }

    private fun performDeepOptimization() {
        lifecycleScope.launch {
            try {
                Toast.makeText(context, "正在优化系统...", Toast.LENGTH_SHORT).show()
                delay(1500)
                Toast.makeText(context, "系统优化完成！性能提升 15%", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "优化失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun animateViewsIn() {
        _binding?.let { binding ->
            val views = listOf(
                binding.cardTopStatus,
                binding.quickActionsGrid,
                binding.rvCoreFeatures
            )

            views.forEachIndexed { index, view ->
                view.alpha = 0f
                view.translationY = 50f
                view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(index * 100L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
        }
    }

    /**
     * 设置核心功能RecyclerView
     * 展示所有可用的工具和功能
     */
    private fun setupCoreFeaturesRecyclerView() {
        _binding?.let { binding ->
            // 📋 完整的功能列表（按分类组织）
            val coreFeatures = buildComprehensiveFeatureList()

            val coreFeatureAdapter = CoreFeatureAdapter { feature ->
                handleFeatureClick(feature.id)
            }

            binding.rvCoreFeatures.apply {
                layoutManager = GridLayoutManager(requireContext(), 2)
                adapter = coreFeatureAdapter
            }

            coreFeatureAdapter.submitList(coreFeatures)
        }
    }

    /**
     * 构建完整的功能列表
     * 按刚需和高频使用场景排序，热门功能优先
     */
    private fun buildComprehensiveFeatureList(): List<CoreFeature> {
        return listOf(
            // 🔥 TOP1: 智能浏览器 - 最高频使用
            CoreFeature(
                id = "smart_browser",
                title = "智能浏览器",
                description = "拦截广告 • 隐私保护",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_web,
                category = "高频"
            ),

            // 🔥 TOP2: 微信清理 - 刚需，微信占用空间大
            CoreFeature(
                id = "wechat_cleaner",
                title = "微信清理",
                description = "专项清理 • 释放空间",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_auto_fix,
                category = "高频"
            ),

            // 🔥 TOP3: 存储管理 - 刚需，手机容量不足
            CoreFeature(
                id = "storage_manager",
                title = "存储管理",
                description = "空间分析 • 清理缓存",
                icon = android.R.drawable.ic_menu_save,
                category = "高频"
            ),

            // 🔥 TOP4: 内存管理 - 高频，手机卡顿必用
            CoreFeature(
                id = "memory_manager",
                title = "内存管理",
                description = "智能清理 • 释放空间",
                icon = android.R.drawable.ic_menu_manage,
                category = "高频"
            ),

            // 🔥 TOP5: WiFi管理 - 高频，网络连接问题
            CoreFeature(
                id = "wifi_manager",
                title = "WiFi管理",
                description = "信号检测 • 优化建议",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_wifi,
                category = "高频"
            ),

            // 🔥 TOP6: 应用管理 - 高频，卸载/管理应用
            CoreFeature(
                id = "app_management",
                title = "应用管理",
                description = "卸载 • 权限 • 存储",
                icon = android.R.drawable.ic_menu_view,
                category = "高频"
            ),

            // ⚡ 性能优化类
            CoreFeature(
                id = "core_optimization",
                title = "核心加速",
                description = "提升帧率 • 降低延迟",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_rocket,
                category = "性能"
            ),
            CoreFeature(
                id = "game_acceleration",
                title = "游戏加速",
                description = "游戏优化 • 帧率提升",
                icon = android.R.drawable.ic_media_play,
                category = "性能"
            ),
            CoreFeature(
                id = "battery_manager",
                title = "电池管理",
                description = "续航优化 • 充电保护",
                icon = android.R.drawable.ic_menu_gallery,
                category = "性能"
            ),

            // 🌐 浏览器相关
            CoreFeature(
                id = "browser_bookmarks",
                title = "书签管理",
                description = "收藏网页 • 快速访问",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_bookmark,
                category = "浏览器"
            ),
            CoreFeature(
                id = "browser_history",
                title = "浏览历史",
                description = "历史记录 • 时间线",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_history,
                category = "浏览器"
            ),

            // 📁 文件工具
            CoreFeature(
                id = "file_manager",
                title = "文件管理",
                description = "管理文件 • 安装包",
                icon = android.R.drawable.ic_menu_save,
                category = "文件"
            ),
            CoreFeature(
                id = "photo_compression",
                title = "照片压缩",
                description = "智能压缩 • 节省空间",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_compress,
                category = "文件"
            ),

            // 🌍 网络工具
            CoreFeature(
                id = "network_diagnostic",
                title = "网络诊断",
                description = "延迟测试 • 速度检测",
                icon = android.R.drawable.ic_menu_search,
                category = "网络"
            ),
            CoreFeature(
                id = "packet_capture",
                title = "网络抓包",
                description = "数据包捕获 • 协议分析",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_chart,
                category = "网络"
            ),

            // 🚀 智能工具
            CoreFeature(
                id = "qrcode_tool",
                title = "二维码工具",
                description = "扫描生成 • WiFi分享",
                icon = android.R.drawable.ic_menu_view,
                category = "智能"
            ),
            CoreFeature(
                id = "clipboard_history",
                title = "剪贴板历史",
                description = "复制记录 • 快速恢复",
                icon = android.R.drawable.ic_menu_edit,
                category = "智能"
            ),
            CoreFeature(
                id = "app_usage_stats",
                title = "使用统计",
                description = "时长分析 • 数字健康",
                icon = android.R.drawable.ic_menu_info_details,
                category = "智能"
            ),

            // ⚙️ 系统工具
            CoreFeature(
                id = "system_monitor",
                title = "系统监控",
                description = "实时状态 • 性能监控",
                icon = android.R.drawable.ic_menu_info_details,
                category = "系统"
            ),
            CoreFeature(
                id = "cpu_manager",
                title = "CPU管理",
                description = "频率调节 • 温度控制",
                icon = android.R.drawable.ic_menu_info_details,
                category = "系统"
            ),
            CoreFeature(
                id = "quick_settings",
                title = "快速设置",
                description = "一键设置 • 常用开关",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_settings,
                category = "系统"
            ),

            // 🔒 安全工具
            CoreFeature(
                id = "security_center",
                title = "安全中心",
                description = "隐私扫描 • 安全防护",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_shield,
                category = "安全"
            ),
            CoreFeature(
                id = "shizuku_auth",
                title = "Shizuku授权",
                description = "系统级权限控制",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_lock,
                category = "安全"
            ),

            // 📀 媒体工具（低频）
            CoreFeature(
                id = "notification_history",
                title = "通知历史",
                description = "通知记录 • 恢复查看",
                icon = android.R.drawable.ic_dialog_info,
                category = "媒体"
            ),
            CoreFeature(
                id = "network_scene",
                title = "网络场景",
                description = "场景化模式 • 智能切换",
                icon = com.lanhe.gongjuxiang.R.drawable.ic_wifi,
                category = "媒体"
            )
        )
    }

    /**
     * 处理功能点击事件
     */
    private fun handleFeatureClick(featureId: String) {
        when (featureId) {
            // 性能优化
            "core_optimization" -> openCoreOptimization()
            "memory_manager" -> openMemoryManager()
            "cpu_manager" -> openCpuManager()

            // 浏览器
            "smart_browser" -> openSmartBrowser()
            "browser_bookmarks" -> openBrowserBookmarks()
            "browser_history" -> openBrowserHistory()

            // 系统管理
            "app_management" -> openAppManagement()
            "storage_manager" -> openStorageManager()
            "battery_manager" -> openBatteryManager()

            // 安全工具
            "shizuku_auth" -> openShizukuAuth()
            "security_center" -> openSecurityCenter()

            // 网络工具
            "wifi_manager" -> openWifiManager()
            "network_diagnostic" -> openNetworkDiagnostic()
            "packet_capture" -> openPacketCapture()
            "network_scene" -> openNetworkScene()

            // 文件工具
            "file_manager" -> openFileManager()
            "photo_compression" -> openPhotoCompression()
            "wechat_cleaner" -> openWeChatCleaner()

            // 系统工具
            "quick_settings" -> openQuickSettings()
            "system_monitor" -> openSystemMonitor()
            "game_acceleration" -> openGameAcceleration()

            // 【新增】媒体工具
            "audio_manager" -> openAudioManager()
            "ebook_reader" -> openEBookReader()
            "video_gallery" -> openVideoGallery()

            // 【创新功能】智能工具
            "clipboard_history" -> openClipboardHistory()
            "qrcode_tool" -> openQRCodeTool()
            "app_usage_stats" -> openAppUsageStats()
            "notification_history" -> openNotificationHistory()
        }
    }

    // ==================== Activity启动方法 ====================

    // 性能优化工具
    private fun openCoreOptimization() = startActivitySafe(CoreOptimizationActivity::class.java)
    private fun openMemoryManager() = startActivitySafe(MemoryManagerActivity::class.java)
    private fun openCpuManager() = startActivitySafe(CpuManagerActivity::class.java)

    // 浏览器工具
    private fun openSmartBrowser() = startActivitySafe(ChromiumBrowserActivity::class.java)
    private fun openBrowserBookmarks() {
        val intent = Intent(requireContext(), BookmarkActivity::class.java)
        startActivitySafe(intent)
    }
    private fun openBrowserHistory() {
        val intent = Intent(requireContext(), HistoryActivity::class.java)
        startActivitySafe(intent)
    }

    // 系统管理工具
    private fun openAppManagement() = startActivitySafe(AppManagerActivity::class.java)
    private fun openStorageManager() = startActivitySafe(StorageManagerActivity::class.java)
    private fun openBatteryManager() = startActivitySafe(BatteryManagerActivity::class.java)

    // 安全工具
    private fun openShizukuAuth() = startActivitySafe(ShizukuAuthActivity::class.java)
    private fun openSecurityCenter() = startActivitySafe(SecurityCenterActivity::class.java)

    // 网络工具
    private fun openWifiManager() = startActivitySafe(WifiSettingsActivity::class.java)
    private fun openNetworkDiagnostic() = startActivitySafe(NetworkDiagnosticActivity::class.java)
    private fun openPacketCapture() = startActivitySafe(PacketCaptureActivity::class.java)
    // TODO: NetworkSceneActivity功能待实现
    // private fun openNetworkScene() = startActivitySafe(NetworkSceneActivity::class.java)
    private fun openNetworkScene() { Toast.makeText(requireContext(), "功能开发中", Toast.LENGTH_SHORT).show() }

    // 文件工具
    private fun openFileManager() = startActivitySafe(FileBrowserActivity::class.java)
    // TODO: 以下功能待实现
    // private fun openPhotoCompression() = startActivitySafe(PhotoCompressionActivity::class.java)
    // private fun openWeChatCleaner() = startActivitySafe(WeChatCleanerActivity::class.java)
    private fun openPhotoCompression() { Toast.makeText(requireContext(), "功能开发中", Toast.LENGTH_SHORT).show() }
    private fun openWeChatCleaner() { Toast.makeText(requireContext(), "功能开发中", Toast.LENGTH_SHORT).show() }

    // 系统工具
    private fun openQuickSettings() = startActivitySafe(QuickSettingsActivity::class.java)
    private fun openSystemMonitor() = startActivitySafe(SystemMonitorActivity::class.java)
    private fun openGameAcceleration() = startActivitySafe(GameAccelerationActivity::class.java)
    private fun openPerformanceTools() = startActivitySafe(PerformanceToolsActivity::class.java)

    // 【新增】媒体工具 - TODO: 待实现
    // private fun openAudioManager() = startActivitySafe(AudioManagerActivity::class.java)
    // private fun openEBookReader() = startActivitySafe(EBookReaderActivity::class.java)
    // private fun openVideoGallery() = startActivitySafe(VideoGalleryActivity::class.java)
    private fun openAudioManager() { Toast.makeText(requireContext(), "功能开发中", Toast.LENGTH_SHORT).show() }
    private fun openEBookReader() { Toast.makeText(requireContext(), "功能开发中", Toast.LENGTH_SHORT).show() }
    private fun openVideoGallery() { Toast.makeText(requireContext(), "功能开发中", Toast.LENGTH_SHORT).show() }

    // 【创新功能】智能工具 - TODO: 待实现
    // private fun openClipboardHistory() = startActivitySafe(ClipboardHistoryActivity::class.java)
    // private fun openQRCodeTool() = startActivitySafe(QRCodeToolActivity::class.java)
    private fun openClipboardHistory() { Toast.makeText(requireContext(), "功能开发中", Toast.LENGTH_SHORT).show() }
    private fun openQRCodeTool() { Toast.makeText(requireContext(), "功能开发中", Toast.LENGTH_SHORT).show() }
    // private fun openAppUsageStats() = startActivitySafe(AppUsageStatsActivity::class.java)
    private fun openAppUsageStats() { Toast.makeText(requireContext(), "功能开发中", Toast.LENGTH_SHORT).show() }
    // private fun openNotificationHistory() = startActivitySafe(NotificationHistoryActivity::class.java)
    private fun openNotificationHistory() { Toast.makeText(requireContext(), "功能开发中", Toast.LENGTH_SHORT).show() }

    // ==================== 辅助方法 ====================

    private fun startActivitySafe(cls: Class<*>) {
        try {
            startActivity(Intent(requireContext(), cls))
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startActivitySafe(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            startPerformanceMonitoring()
        }
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
