package com.lanhe.gongjuxiang.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.*
import com.lanhe.gongjuxiang.adapters.CoreFeatureAdapter
import com.lanhe.gongjuxiang.databinding.FragmentHomeBinding
import com.lanhe.gongjuxiang.models.CoreFeature
import com.lanhe.gongjuxiang.utils.ShimmerHelper
import com.lanhe.gongjuxiang.utils.ItemDecorationHelper
import com.lanhe.gongjuxiang.ui.components.CircularProgressView
import com.lanhe.gongjuxiang.ui.animations.ViewAnimations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.lanhe.gongjuxiang.utils.SystemMonitorHelper

/**
 * 首页Fragment - 展示核心功能入口
 * 整合FunctionsFragment的功能，提供清晰的功能导航
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var coreFeatureAdapter: CoreFeatureAdapter
    private lateinit var systemMonitor: SystemMonitorHelper
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        systemMonitor = SystemMonitorHelper(requireContext())
        setupSwipeRefresh()
        setupRecyclerView()
        setupQuickActions()
        setupStatusCard()
        loadDataWithShimmer()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout?.setOnRefreshListener {
            refreshData()
        }

        binding.swipeRefreshLayout?.setColorSchemeResources(
            R.color.md_theme_light_primary,
            R.color.md_theme_light_secondary,
            R.color.md_theme_light_tertiary
        )
    }

    private fun setupRecyclerView() {
        // 新的浏览器为中心的首页设计不需要特性列表 RecyclerView
        // 保持此代码以备后用，但 RecyclerView 在新布局中设置为 visibility="gone"
        coreFeatureAdapter = CoreFeatureAdapter { feature ->
            handleFeatureClick(feature)
        }

        binding.recyclerViewFeatures.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = coreFeatureAdapter

            // 添加间距装饰器
            val spacing = ItemDecorationHelper.dpToPx(requireContext(), 8f)
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

    private fun setupQuickActions() {
        // ========== 搜索框功能 ==========
        // 处理搜索框文本输入
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            // 显示或隐藏清除按钮
            binding.ivClear.visibility = if (text?.isNotEmpty() == true) View.VISIBLE else View.GONE
        }

        // 清除按钮点击事件
        binding.ivClear.setOnClickListener {
            binding.etSearch.text.clear()
            binding.ivClear.visibility = View.GONE
        }

        // 搜索框输入事件 - 支持URL/搜索词/语音输入
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.etSearch.text.toString())
                true
            } else {
                false
            }
        }

        // 语音搜索按钮
        binding.ivVoice.setOnClickListener {
            // 暂时显示Toast，可后续集成语音识别
            Toast.makeText(context, "语音搜索功能开发中...", Toast.LENGTH_SHORT).show()
        }

        // ========== 快速搜索芯片 ==========
        // 百度搜索
        binding.chipBaidu.setOnClickListener {
            performSearch("https://www.baidu.com")
        }

        // 谷歌搜索
        binding.chipGoogle.setOnClickListener {
            performSearch("https://www.google.com")
        }

        // 视频搜索
        binding.chipVideo.setOnClickListener {
            performSearch("https://www.baidu.com/s?tn=baiduimage&word=")
        }

        // 新闻搜索
        binding.chipNews.setOnClickListener {
            performSearch("https://news.baidu.com")
        }

        // ========== 浏览器快速操作卡片 ==========
        // 打开浏览器
        setupCardWithAnimation(binding.cardOpenBrowser) {
            startActivity(Intent(context, ChromiumBrowserActivity::class.java))
        }

        // 浏览历史
        setupCardWithAnimation(binding.cardHistory) {
            // 启动浏览器并显示历史
            startActivity(Intent(context, ChromiumBrowserActivity::class.java).apply {
                putExtra("action", "show_history")
            })
        }

        // 书签
        setupCardWithAnimation(binding.cardBookmarks) {
            // 启动浏览器并显示书签
            startActivity(Intent(context, ChromiumBrowserActivity::class.java).apply {
                putExtra("action", "show_bookmarks")
            })
        }

        // 浏览器设置
        setupCardWithAnimation(binding.cardBrowserSettings) {
            // 启动浏览器设置
            startActivity(Intent(context, ChromiumBrowserActivity::class.java).apply {
                putExtra("action", "settings")
            })
        }

        // ========== 系统状态卡片点击事件 ==========
        binding.heroStatusCard.setOnClickListener {
            // 点击系统状态卡片进入详细系统监控
            startActivity(Intent(context, SystemMonitorActivity::class.java))
        }
    }

    private fun setupCardWithAnimation(cardView: View, action: () -> Unit) {
        // Setup press animation
        ViewAnimations.setupPressAnimation(cardView)

        // Set click listener with haptic feedback
        cardView.setOnClickListener {
            // Add haptic feedback
            cardView.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            action()
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return

        // 隐藏软键盘
        val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)

        // 清除搜索框
        binding.etSearch.text.clear()

        // 启动浏览器并传递搜索URL
        val intent = Intent(context, ChromiumBrowserActivity::class.java)

        // 判断是否是完整URL
        val urlToLoad = when {
            query.startsWith("http://") || query.startsWith("https://") -> {
                // 完整URL，直接使用
                query
            }
            query.contains(".") && !query.contains(" ") -> {
                // 看起来像域名（包含点，但不包含空格），自动补全为https://
                "https://$query"
            }
            else -> {
                // 搜索词，使用百度搜索
                "https://www.baidu.com/s?wd=$query"
            }
        }

        intent.putExtra("url", urlToLoad)
        startActivity(intent)
    }

    private fun loadCoreFeatures(): List<CoreFeature> {
        return listOf(
            CoreFeature(
                id = "ai_optimization",
                title = "AI智能优化",
                description = "使用AI进行智能系统分析和优化",
                icon = R.drawable.ic_optimize,
                category = "AI"
            ),
            CoreFeature(
                id = "system_monitor",
                title = "系统监控",
                description = "实时监控CPU、内存、存储状态",
                icon = R.drawable.ic_optimize,
                category = "性能"
            ),
            CoreFeature(
                id = "battery_manager",
                title = "电池管理",
                description = "智能电池优化和健康检测",
                icon = R.drawable.ic_battery_full,
                category = "电源"
            ),
            CoreFeature(
                id = "memory_manager",
                title = "内存管理",
                description = "内存清理和应用管理",
                icon = R.drawable.ic_optimize,
                category = "性能"
            ),
            CoreFeature(
                id = "storage_manager",
                title = "存储管理",
                description = "文件清理和存储优化",
                icon = R.drawable.ic_optimize,
                category = "存储"
            ),
            CoreFeature(
                id = "network_diagnostic",
                title = "网络诊断",
                description = "网络速度测试和连接诊断",
                icon = R.drawable.ic_optimize,
                category = "网络"
            ),
            CoreFeature(
                id = "packet_capture",
                title = "网络抓包",
                description = "专业网络数据包分析",
                icon = R.drawable.ic_optimize,
                category = "网络"
            ),
            CoreFeature(
                id = "cpu_manager",
                title = "CPU管理",
                description = "CPU性能调节和优化",
                icon = R.drawable.ic_optimize,
                category = "性能"
            ),
            CoreFeature(
                id = "app_manager",
                title = "应用管理",
                description = "应用权限和运行管理",
                icon = R.drawable.ic_optimize,
                category = "应用"
            ),
            CoreFeature(
                id = "file_browser",
                title = "文件浏览",
                description = "功能强大的文件管理器",
                icon = R.drawable.ic_optimize,
                category = "工具"
            ),
            CoreFeature(
                id = "game_acceleration",
                title = "游戏加速",
                description = "游戏性能优化和加速",
                icon = R.drawable.ic_optimize,
                category = "游戏"
            ),
            CoreFeature(
                id = "quick_settings",
                title = "快捷设置",
                description = "常用系统设置快速访问",
                icon = R.drawable.ic_optimize,
                category = "设置"
            ),
            CoreFeature(
                id = "notification_manager",
                title = "通知管理",
                description = "智能通知过滤和管理",
                icon = R.drawable.ic_optimize,
                category = "通知"
            )
        )

        lifecycleScope.launch {
            val features = loadCoreFeatures()
            coreFeatureAdapter.submitList(features)
        }
    }

    private fun loadDataWithShimmer() {
        if (isLoading) return

        isLoading = true
        showShimmerEffect(true)

        lifecycleScope.launch {
            // 加载真实数据
            delay(500) // 短暂延迟以显示加载效果

            // 不再加载特性列表，直接加载系统状态
            loadSystemStatus()

            showShimmerEffect(false)
            isLoading = false
        }
    }

    private fun refreshData() {
        lifecycleScope.launch {
            // 刷新系统状态数据
            delay(500)

            val cpuUsage = systemMonitor.getCpuUsage()
            val memoryUsage = systemMonitor.getMemoryUsage()
            val batteryInfo = systemMonitor.getBatteryInfo()
            val networkStatus = if (batteryInfo.level > 50) "良好" else "一般"

            updateSystemStatus(
                cpuUsage = cpuUsage,
                memoryUsage = memoryUsage.usedGB + "GB",
                batteryLevel = batteryInfo.level,
                networkStatus = networkStatus
            )

            binding.swipeRefreshLayout?.isRefreshing = false
        }
    }

    private fun loadSystemStatus() {
        // 加载真实系统状态数据
        val cpuUsage = systemMonitor.getCpuUsage()
        val memoryUsage = systemMonitor.getMemoryUsage()
        val batteryInfo = systemMonitor.getBatteryInfo()
        val networkStatus = if (batteryInfo.level > 50) "良好" else "一般"

        updateSystemStatus(
            cpuUsage = cpuUsage,
            memoryUsage = memoryUsage.usedGB + "GB",
            batteryLevel = batteryInfo.level,
            networkStatus = networkStatus
        )
    }

    private fun showShimmerEffect(show: Boolean) {
        if (show) {
            binding.heroStatusCard.visibility = View.GONE
            // RecyclerView 保持隐藏（新布局中不使用）
        } else {
            // 动画显示系统状态卡片
            binding.heroStatusCard.alpha = 0f
            binding.heroStatusCard.visibility = View.VISIBLE

            ViewAnimations.staggeredListAnimation(
                listOf(binding.heroStatusCard),
                100L
            )
        }
    }

    private fun handleFeatureClick(feature: CoreFeature) {
        val intent = when (feature.id) {
            "ai_optimization" -> Intent(context, AIOptimizationActivity::class.java)
            "system_monitor" -> Intent(context, SystemMonitorActivity::class.java)
            "battery_manager" -> Intent(context, BatteryManagerActivity::class.java)
            "memory_manager" -> Intent(context, MemoryManagerActivity::class.java)
            "storage_manager" -> Intent(context, StorageManagerActivity::class.java)
            "network_diagnostic" -> Intent(context, NetworkDiagnosticActivity::class.java)
            "packet_capture" -> Intent(context, PacketCaptureActivity::class.java)
            "cpu_manager" -> Intent(context, CpuManagerActivity::class.java)
            "app_manager" -> Intent(context, AppManagerActivity::class.java)
            "file_browser" -> Intent(context, FileBrowserActivity::class.java)
            "game_acceleration" -> Intent(context, GameAccelerationActivity::class.java)
            "quick_settings" -> Intent(context, QuickSettingsActivity::class.java)
            "notification_manager" -> Intent(context, NotificationManagerActivity::class.java)
            else -> return
        }
        startActivity(intent)
    }

    private fun setupStatusCard() {
        // Initialize circular progress views
        setupCircularProgressViews()

        // 获取真实系统状态数据
        val cpuUsage = systemMonitor.getCpuUsage()
        val memoryUsage = systemMonitor.getMemoryUsage()
        val batteryInfo = systemMonitor.getBatteryInfo()
        val networkStatus = if (batteryInfo.level > 50) "良好" else "一般"

        updateSystemStatus(
            cpuUsage = cpuUsage,
            memoryUsage = memoryUsage.usedGB + "GB",
            batteryLevel = batteryInfo.level,
            networkStatus = networkStatus
        )

        // 添加状态卡片的微动效
        binding.heroStatusCard.setOnClickListener {
            // 可以点击查看详细状态
            startActivity(Intent(context, SystemMonitorActivity::class.java))
        }
    }

    private fun setupCircularProgressViews() {
        // 新的布局使用简单的TextView，不需要特殊设置
        // 系统状态会在updateSystemStatus方法中更新
    }
    
    private fun updateSystemStatus(
        cpuUsage: Float,
        memoryUsage: String,
        batteryLevel: Int,
        networkStatus: String
    ) {
        // Update circular progress views
        binding.progressCpu.apply {
            setProgress(cpuUsage, true)
            val status = when {
                cpuUsage > 80 -> CircularProgressView.Status.CRITICAL
                cpuUsage > 60 -> CircularProgressView.Status.WARNING
                else -> CircularProgressView.Status.GOOD
            }
            setStatusColors(status)
            setTrend(
                when {
                    cpuUsage > 70 -> CircularProgressView.Trend.UP
                    cpuUsage < 30 -> CircularProgressView.Trend.DOWN
                    else -> CircularProgressView.Trend.NONE
                }
            )
        }

        binding.progressMemory.apply {
            val memoryValue = memoryUsage.replace("GB", "").toFloatOrNull() ?: 0f
            setProgress(memoryValue, true)
            setValue(memoryUsage)
            val status = when {
                memoryValue > 7 -> CircularProgressView.Status.CRITICAL
                memoryValue > 5 -> CircularProgressView.Status.WARNING
                else -> CircularProgressView.Status.GOOD
            }
            setStatusColors(status)
        }

        binding.progressBattery.apply {
            setProgress(batteryLevel.toFloat(), true)
            val status = when {
                batteryLevel < 20 -> CircularProgressView.Status.CRITICAL
                batteryLevel < 50 -> CircularProgressView.Status.WARNING
                else -> CircularProgressView.Status.GOOD
            }
            setStatusColors(status)
        }

        binding.progressNetwork.apply {
            val networkValue = when (networkStatus) {
                "良好" -> 80f
                "一般" -> 50f
                "较差" -> 20f
                else -> 60f
            }
            setProgress(networkValue, true)
            setValue(networkStatus)
            val status = when (networkStatus) {
                "良好" -> CircularProgressView.Status.GOOD
                "一般" -> CircularProgressView.Status.WARNING
                else -> CircularProgressView.Status.CRITICAL
            }
            setStatusColors(status)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}