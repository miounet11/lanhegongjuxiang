package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.animation.ObjectAnimator
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.lanhe.gongjuxiang.ui.transformers.AdvancedPageTransformers
import com.lanhe.gongjuxiang.ui.haptic.HapticFeedbackManager
import com.lanhe.gongjuxiang.ui.transitions.SharedElementTransitionHelper
import com.lanhe.gongjuxiang.ui.animations.RecyclerViewAnimations
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityMainBinding
import com.lanhe.gongjuxiang.fragments.*
import com.lanhe.gongjuxiang.services.ChargingReminderService
import com.lanhe.gongjuxiang.settings.BatteryOptimizationActivity
import com.lanhe.gongjuxiang.utils.PreferencesManager
import com.lanhe.gongjuxiang.utils.ShizukuManager
import com.lanhe.gongjuxiang.utils.PermissionHelper
import com.lanhe.gongjuxiang.utils.PermissionConstants
import com.lanhe.gongjuxiang.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 主活动 - 统一UI设计
 * 采用现代化的Material Design，统一展示所有功能
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var viewPager: ViewPager2

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var shizukuManager: ShizukuManager

    private lateinit var hapticFeedbackManager: HapticFeedbackManager
    private lateinit var permissionHelper: PermissionHelper
    private var isTablet = false
    private var isLandscape = false

    // Fragment cache for ViewPager2 - Simplified to 4 tabs
    private val fragments = listOf(
        HomeFragment(),
        FunctionsFragment(), // Tools fragment
        SecurityFragment(), // Monitor fragment
        SettingsFragment()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // 初始化Shizuku (通过注入的方式)
        initializeShizuku()

        // Detect device type and orientation
        detectDeviceConfiguration()

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize managers (non-injected ones)
        hapticFeedbackManager = HapticFeedbackManager.getInstance(this)
        permissionHelper = PermissionHelper.getInstance(this)

        // Apply theme based on user preference
        applyTheme()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check and request critical permissions first
        checkAndRequestPermissions()

        // Setup shared element transitions
        SharedElementTransitionHelper.setupActivityTransitions(this)

        // Setup edge-to-edge window insets
        setupEdgeToEdge()

        // Setup UI based on device configuration
        setupViewPager()
        if (isTablet) {
            setupNavigationRail()
        } else if (isLandscape) {
            setupLandscapeNavigation()
        } else {
            setupBottomNavigation()
        }

        // 检查Shizuku权限
        checkShizukuPermission()


        setupTransitions()

        // 启动充电提醒服务
        ChargingReminderService.startService(this)

        // 观察ViewModel数据
        observeViewModel()

        // Keep splash screen on screen until ready
        splashScreen.setKeepOnScreenCondition {
            viewModel.isLoading.value == true
        }
    }

    private fun setupViewPager() {
        viewPager = binding.viewPager

        // Setup ViewPager2 adapter
        val adapter = MainPagerAdapter(this, fragments)
        viewPager.adapter = adapter

        // Disable user swiping (optional - can be enabled for better UX)
        viewPager.isUserInputEnabled = true

        // Setup advanced page transformer based on user preference
        val transformerType = preferencesManager.getPageTransformerType()
        val transformer = when (transformerType) {
            "depth" -> AdvancedPageTransformers.DepthPageTransformer()
            "zoom_out" -> AdvancedPageTransformers.ZoomOutPageTransformer()
            "cube" -> AdvancedPageTransformers.CubeInRotationTransformer()
            "parallax" -> AdvancedPageTransformers.ParallaxTransformer()
            "stack" -> AdvancedPageTransformers.StackTransformer()
            "flip" -> AdvancedPageTransformers.FlipHorizontalTransformer()
            "accordion" -> AdvancedPageTransformers.AccordionTransformer()
            else -> AdvancedPageTransformers.ZoomOutPageTransformer() // Default
        }
        viewPager.setPageTransformer(transformer)
    }

    private fun setupBottomNavigation() {
        val bottomNavView = binding.bottomNavView

        // Sync ViewPager2 with BottomNavigationView
        bottomNavView.setOnItemSelectedListener { item ->
            val position = when (item.itemId) {
                R.id.nav_home -> 0
                R.id.nav_tools -> 1
                R.id.nav_monitor -> 2
                R.id.nav_settings -> 3
                else -> 0
            }

            // Animate page change
            viewPager.setCurrentItem(position, true)

            // Add haptic feedback and selection animation
            hapticFeedbackManager.lightClick()
            animateBottomNavSelection(item.itemId)
            true
        }

        // Listen to ViewPager2 page changes and update BottomNavigationView
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val itemId = when (position) {
                    0 -> R.id.nav_home
                    1 -> R.id.nav_tools
                    2 -> R.id.nav_monitor
                    3 -> R.id.nav_settings
                    else -> R.id.nav_home
                }
                bottomNavView.selectedItemId = itemId
            }
        })

        // Setup badges with animations
        setupBottomNavigationBadges()
    }




    private fun observeViewModel() {
        // 观察性能数据
        viewModel.performanceData.observe(this) { data ->
            updatePerformanceIndicators(data)
        }

        // 观察电池状态
        viewModel.batteryInfo.observe(this) { battery ->
            updateBatteryStatus(battery)
        }

        // 观察网络状态
        viewModel.networkInfo.observe(this) { network ->
            updateNetworkStatus(network)
        }


    }

    private fun updatePerformanceIndicators(data: com.lanhe.gongjuxiang.models.PerformanceData) {
        // 更新顶部状态栏的性能指标
        findViewById<android.widget.TextView>(R.id.tv_cpu_usage)?.text = String.format("%.1f%%", data.cpuUsage)
        findViewById<android.widget.TextView>(R.id.tv_memory_usage)?.text = String.format("%.1f%%", data.memoryUsage.usagePercent)
        findViewById<android.widget.TextView>(R.id.tv_storage_usage)?.text = String.format("%.1f%%", data.storageUsage)
    }

    private fun updateBatteryStatus(battery: com.lanhe.gongjuxiang.models.BatteryInfo) {
        findViewById<android.widget.TextView>(R.id.tv_battery_level)?.text = "${battery.level}%"
        findViewById<android.widget.TextView>(R.id.tv_battery_temp)?.text = String.format("%.1f°C", battery.temperature)

        // 更新电池图标
        val batteryIconView = findViewById<android.widget.ImageView>(R.id.iv_battery_status)
        batteryIconView?.setImageResource(
            when {
                battery.level >= 80 -> R.drawable.ic_battery_full
                battery.level >= 60 -> R.drawable.ic_battery_high
                battery.level >= 30 -> R.drawable.ic_battery_medium
                battery.level >= 15 -> R.drawable.ic_battery_low
                else -> R.drawable.ic_battery_critical
            }
        )
    }

    private fun updateNetworkStatus(network: com.lanhe.gongjuxiang.models.NetworkStats) {
        findViewById<android.widget.TextView>(R.id.tv_network_type)?.text = network.interfaceName
        findViewById<android.widget.TextView>(R.id.tv_network_speed)?.text = formatNetworkSpeed(network)
    }



    private fun setupBottomNavigationBadges() {
        val bottomNavView = binding.bottomNavView

        // Setup tools badge with custom drawable
        if (hasOptimizationSuggestions()) {
            val badge = bottomNavView.getOrCreateBadge(R.id.nav_tools)
            badge.number = getOptimizationSuggestionsCount()
            badge.backgroundColor = getColor(R.color.status_warning)
            animateBadgeEntry(badge)
        }

        // Setup monitor badge with custom drawable
        if (hasSecurityWarnings()) {
            val badge = bottomNavView.getOrCreateBadge(R.id.nav_monitor)
            badge.number = getSecurityWarningsCount()
            badge.backgroundColor = getColor(R.color.status_danger)
            animateBadgeEntry(badge)
        }
    }

    private fun updateBottomNavigationBadges() {
        setupBottomNavigationBadges()
    }



    private fun openBrowser() {
        Intent(this, ChromiumBrowserActivity::class.java).apply {
            startActivity(this)
        }
    }

    private fun openBatteryOptimization() {
        BatteryOptimizationActivity.start(this)
    }

    private fun openBrowserSettings() {
        // Chromium浏览器设置已整合到浏览器内部
        Toast.makeText(this, "浏览器设置功能开发中...", Toast.LENGTH_SHORT).show()
    }

    private fun showAbout() {
        // 显示关于页面
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }

    private fun hasOptimizationSuggestions(): Boolean {
        // 检查是否有优化建议
        return viewModel.hasOptimizationSuggestions()
    }

    private fun hasSecurityWarnings(): Boolean {
        // 检查是否有安全警告
        return viewModel.hasSecurityWarnings()
    }

    /**
     * 初始化Shizuku
     */
    private fun initializeShizuku() {
        // ShizukuManager已通过Hilt注入，无需手动初始化
        // 检查权限状态
        if (shizukuManager.isShizukuAvailable()) {
            Toast.makeText(this, "Shizuku服务已就绪", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 检查Shizuku权限
     * 只在首次启动或权限状态变化时显示提示
     */
    private fun checkShizukuPermission() {
        // 检查是否需要Shizuku权限
        val needShizuku = true // 默认启用Shizuku功能

        if (needShizuku && !ShizukuManager.isShizukuAvailable()) {
            // 检查是否已经显示过权限对话框（避免重复显示）
            val hasShownPermissionDialog = preferencesManager.isShizukuPermissionDialogShown()

            if (!hasShownPermissionDialog) {
                // 延迟显示权限请求对话框，避免影响启动体验
                binding.root.postDelayed({
                    showShizukuPermissionDialog()
                    // 标记已显示，避免重复
                    preferencesManager.setShizukuPermissionDialogShown(true)
                }, 1000)
            }
        } else if (ShizukuManager.isShizukuAvailable()) {
            // 权限已授予，重置标记以便下次需要时重新显示
            preferencesManager.setShizukuPermissionDialogShown(false)
        }
    }

    /**
     * 显示Shizuku权限对话框
     */
    private fun showShizukuPermissionDialog() {
        // 使用完整的AlertDialog，提供"去设置"选项
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🔑 需要Shizuku权限")
            .setMessage(
                """
                为了使用以下高级功能，需要安装并授权Shizuku：
                
                ⚡ 深度系统优化
                🎯 进程管理与控制  
                🔧 应用权限管理
                🛡️ 系统设置修改
                
                点击"去设置"进行配置，或稍后在高级设置中启用。
                """.trimIndent()
            )
            .setPositiveButton("去设置") { _, _ ->
                // 跳转到Shizuku授权页面
                try {
                    val intent = Intent(this, ShizukuAuthActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "无法打开Shizuku设置", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("稍后设置") { dialog, _ ->
                dialog.dismiss()
                viewModel.onShizukuPermissionDenied()
            }
            .setCancelable(false)
            .show()
    }

    private fun formatNetworkSpeed(network: com.lanhe.gongjuxiang.models.NetworkStats): String {
        // 格式化网络速度显示
        val downloadSpeed = network.rxBytes / 1024.0 / 1024.0 // MB/s
        val uploadSpeed = network.txBytes / 1024.0 / 1024.0   // MB/s

        return String.format("%.1f↓ %.1f↑ MB/s", downloadSpeed, uploadSpeed)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                openSettings()
                true
            }
            R.id.action_help -> {
                showHelp()
                true
            }
            R.id.action_feedback -> {
                showFeedback()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun showHelp() {
        val intent = Intent(this, HelpActivity::class.java)
        startActivity(intent)
    }

    private fun showFeedback() {
        val intent = Intent(this, FeedbackActivity::class.java)
        startActivity(intent)
    }


    override fun onResume() {
        super.onResume()
        // 刷新数据
        viewModel.refreshData()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止充电提醒服务
        ChargingReminderService.stopService(this)
        // 清理权限助手
        permissionHelper.clear()
    }

    /**
     * 检查并请求关键权限
     */
    private fun checkAndRequestPermissions() {
        // 请求关键权限
        permissionHelper.requestCriticalPermissions(this, object : PermissionHelper.PermissionCallback {
            override fun onPermissionsGranted() {
                // 所有关键权限已授予
                initializeAfterPermissions()
            }

            override fun onPermissionsDenied(deniedPermissions: List<String>) {
                // 权限被拒绝，降级模式运行
                handlePermissionDenied(deniedPermissions)
                initializeAfterPermissions()
            }

            override fun onPermissionsPermanentlyDenied(permanentlyDeniedPermissions: List<String>) {
                // 权限被永久拒绝，降级模式运行
                handlePermissionPermanentlyDenied(permanentlyDeniedPermissions)
                initializeAfterPermissions()
            }
        })
    }

    /**
     * 权限检查后的初始化
     */
    private fun initializeAfterPermissions() {
        // 继续其他初始化操作
        viewModel.refreshData()
    }

    /**
     * 处理权限被拒绝的情况
     */
    private fun handlePermissionDenied(deniedPermissions: List<String>) {
        // 根据被拒绝的权限禁用相关功能
        deniedPermissions.forEach { permission ->
            when {
                permission.contains("STORAGE") -> {
                    viewModel.disableStorageFeatures()
                }
                permission.contains("POST_NOTIFICATIONS") -> {
                    viewModel.disableNotificationFeatures()
                }
            }
        }
    }

    /**
     * 处理权限被永久拒绝的情况
     */
    private fun handlePermissionPermanentlyDenied(permanentlyDeniedPermissions: List<String>) {
        // 根据被永久拒绝的权限禁用相关功能
        permanentlyDeniedPermissions.forEach { permission ->
            when {
                permission.contains("STORAGE") -> {
                    viewModel.disableStorageFeatures()
                    showFeatureDisabledToast("文件管理功能已禁用，需要存储权限")
                }
                permission.contains("POST_NOTIFICATIONS") -> {
                    viewModel.disableNotificationFeatures()
                    showFeatureDisabledToast("通知功能已禁用")
                }
            }
        }
    }

    /**
     * 显示功能禁用提示
     */
    private fun showFeatureDisabledToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // 处理权限请求结果
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 处理从设置页面返回的结果
        if (requestCode == PermissionHelper.REQUEST_CODE_SETTINGS) {
            // 重新检查权限
            checkAndRequestPermissions()
        }
    }

    // New methods for enhanced UI functionality

    private fun setupEdgeToEdge() {
        // Handle system window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Apply top padding for status bar
            binding.appBarLayout.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                view.paddingBottom
            )

            // Apply bottom padding for navigation bar to bottom navigation
            binding.bottomNavView.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                navigationBars.bottom
            )

            // Apply gesture navigation hints
            ViewCompat.setOnApplyWindowInsetsListener(binding.viewPager) { viewPager, insets ->
                val gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures())
                viewPager.setPadding(
                    gestureInsets.left,
                    0,
                    gestureInsets.right,
                    0
                )
                insets
            }

            windowInsets
        }
    }

    private fun applyTheme() {
        val isDarkMode = preferencesManager.isDarkModeEnabled()
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun setupTransitions() {
        // Setup shared element transitions
        window.allowEnterTransitionOverlap = true
        window.allowReturnTransitionOverlap = true

        // Setup enter/exit transitions
        val sharedAxis = MaterialSharedAxis(MaterialSharedAxis.X, true)
        window.enterTransition = sharedAxis
        window.exitTransition = sharedAxis
    }

    private fun animateBottomNavSelection(itemId: Int) {
        val bottomNavView = binding.bottomNavView
        val menuItem = bottomNavView.menu.findItem(itemId)

        // Create ripple effect animation
        ObjectAnimator.ofFloat(bottomNavView, "scaleX", 0.95f, 1.0f).apply {
            duration = 150
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun animateBadgeEntry(badge: BadgeDrawable) {
        // Animate badge appearance
        ObjectAnimator.ofFloat(badge, "alpha", 0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun getOptimizationSuggestionsCount(): Int {
        // Return actual count from ViewModel
        return viewModel.getOptimizationSuggestionsCount()
    }

    private fun getSecurityWarningsCount(): Int {
        // Return actual count from ViewModel
        return viewModel.getSecurityWarningsCount()
    }

    // ViewPager2 Adapter
    private class MainPagerAdapter(
        fragmentActivity: FragmentActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int = fragments.size

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }
    }

    // New advanced UI methods

    private fun detectDeviceConfiguration() {
        val configuration = resources.configuration
        isTablet = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
        isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun setupNavigationRail() {
        // Navigation rail is optional - only exists in tablet layouts
        // TODO: Implement when navigation_rail layout is available
        // Currently no navigation_rail resource exists in layouts
    }

    private fun setupLandscapeNavigation() {
        // Tab layout is optional - only exists in landscape layouts
        // TODO: Implement when tab_layout_landscape layout is available
        // Currently no tab_layout_landscape resource exists in layouts
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Handle configuration changes
        val wasTablet = isTablet
        val wasLandscape = isLandscape

        detectDeviceConfiguration()

        // If configuration changed significantly, recreate the activity
        if (wasTablet != isTablet || (wasLandscape != isLandscape && !isTablet)) {
            recreate()
        }
    }
}
