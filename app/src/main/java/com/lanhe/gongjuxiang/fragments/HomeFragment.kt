package com.lanhe.gongjuxiang.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 首页Fragment - 展示核心功能入口
 * 整合FunctionsFragment的功能，提供清晰的功能导航
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var coreFeatureAdapter: CoreFeatureAdapter
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
        // 一键优化按钮
        binding.btnQuickOptimize.setOnClickListener {
            startActivity(Intent(context, CoreOptimizationActivity::class.java))
        }

        // 快捷功能卡片点击事件
        binding.cardSystemMonitor.setOnClickListener {
            startActivity(Intent(context, SystemMonitorActivity::class.java))
        }

        binding.cardSecurityCenter.setOnClickListener {
            // 导航到安全Fragment
            (activity as? MainActivity)?.let { mainActivity ->
                // 这里可以通过Navigation组件切换到安全Fragment
                // 或者启动SecurityActivity
            }
        }

        binding.cardNetworkDiagnostic.setOnClickListener {
            startActivity(Intent(context, NetworkDiagnosticActivity::class.java))
        }

        binding.cardAppManager.setOnClickListener {
            startActivity(Intent(context, AppManagerActivity::class.java))
        }

        binding.cardFileBrowser.setOnClickListener {
            startActivity(Intent(context, FileBrowserActivity::class.java))
        }

        binding.cardSystemSettings.setOnClickListener {
            startActivity(Intent(context, QuickSettingsActivity::class.java))
        }
    }

    private fun loadCoreFeatures() {
        val features = listOf(
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
            coreFeatureAdapter.submitList(features)
        }
    }

    private fun loadDataWithShimmer() {
        if (isLoading) return

        isLoading = true
        showShimmerEffect(true)

        lifecycleScope.launch {
            // 模拟网络延迟
            delay(1500)

            loadCoreFeatures()
            loadSystemStatus()

            showShimmerEffect(false)
            isLoading = false
        }
    }

    private fun refreshData() {
        lifecycleScope.launch {
            // 模拟刷新数据
            delay(1000)

            loadCoreFeatures()
            updateSystemStatus(
                cpuUsage = Random.nextInt(40, 80).toFloat(),
                memoryUsage = "${"%.1f".format(Random.nextDouble(5.0, 8.5))}GB",
                batteryLevel = Random.nextInt(20, 100),
                networkStatus = listOf("良好", "一般", "较差").random()
            )

            binding.swipeRefreshLayout?.isRefreshing = false
        }
    }

    private fun loadSystemStatus() {
        // 模拟系统状态数据加载
        updateSystemStatus(
            cpuUsage = 45f,
            memoryUsage = "6.2GB",
            batteryLevel = 76,
            networkStatus = "良好"
        )
    }

    private fun showShimmerEffect(show: Boolean) {
        if (show) {
            binding.heroStatusCard.visibility = View.GONE
            binding.recyclerViewFeatures.visibility = View.GONE
            // Shimmer views will be shown by default in layout
        } else {
            binding.heroStatusCard.visibility = View.VISIBLE
            binding.recyclerViewFeatures.visibility = View.VISIBLE
            // Hide shimmer views if they exist
        }
    }

    private fun handleFeatureClick(feature: CoreFeature) {
        val intent = when (feature.id) {
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
        // 模拟系统状态数据 - 实际项目中应该从ViewModel获取
        updateSystemStatus(
            cpuUsage = 45f,
            memoryUsage = "6.2GB",
            batteryLevel = 76,
            networkStatus = "良好"
        )
        
        // 添加状态卡片的微动效
        binding.heroStatusCard.setOnClickListener {
            // 可以点击查看详细状态
            startActivity(Intent(context, SystemMonitorActivity::class.java))
        }
    }
    
    private fun updateSystemStatus(
        cpuUsage: Float,
        memoryUsage: String,
        batteryLevel: Int,
        networkStatus: String
    ) {
        binding.tvCpuValue.text = "${cpuUsage.toInt()}%"
        binding.tvMemoryValue.text = memoryUsage
        binding.tvBatteryValue.text = "${batteryLevel}%"
        binding.tvNetworkValue.text = networkStatus
        
        // 根据状态设置颜色
        val cpuColor = when {
            cpuUsage > 80 -> android.R.color.holo_red_light
            cpuUsage > 60 -> android.R.color.holo_orange_light
            else -> android.R.color.holo_green_light
        }
        
        val batteryColor = when {
            batteryLevel < 20 -> android.R.color.holo_red_light
            batteryLevel < 50 -> android.R.color.holo_orange_light
            else -> android.R.color.holo_green_light
        }
        
        // 这里可以设置状态指示器的颜色
        // binding.tvCpuValue.setTextColor(ContextCompat.getColor(requireContext(), cpuColor))
        // binding.tvBatteryValue.setTextColor(ContextCompat.getColor(requireContext(), batteryColor))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}