package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.animation.ObjectAnimator
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
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
import com.lanhe.gongjuxiang.browser.BrowserSettingsActivity
import com.lanhe.gongjuxiang.browser.YcWebViewBrowser
import com.lanhe.gongjuxiang.databinding.ActivityMainBinding
import com.lanhe.gongjuxiang.fragments.*
import com.lanhe.gongjuxiang.services.ChargingReminderService
import com.lanhe.gongjuxiang.settings.BatteryOptimizationActivity
import com.lanhe.gongjuxiang.utils.PreferencesManager
import com.lanhe.gongjuxiang.viewmodels.MainViewModel

/**
 * 主活动 - 统一UI设计
 * 采用现代化的Material Design，统一展示所有功能
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val viewModel: MainViewModel by viewModels()
    private lateinit var viewPager: ViewPager2
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var hapticFeedbackManager: HapticFeedbackManager
    private var isTablet = false
    private var isLandscape = false

    // Fragment cache for ViewPager2
    private val fragments = listOf(
        HomeFragment(),
        FunctionsFragment(), // Performance fragment placeholder
        BrowserFragment(),
        SecurityFragment(),
        SettingsFragment()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Detect device type and orientation
        detectDeviceConfiguration()

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize managers
        preferencesManager = PreferencesManager(this)
        hapticFeedbackManager = HapticFeedbackManager.getInstance(this)

        // Apply theme based on user preference
        applyTheme()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

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

        setupFab()
        setupDrawer()
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
                R.id.nav_performance -> 1
                R.id.nav_browser -> 2
                R.id.nav_security -> 3
                R.id.nav_settings -> 4
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
                    1 -> R.id.nav_performance
                    2 -> R.id.nav_browser
                    3 -> R.id.nav_security
                    4 -> R.id.nav_settings
                    else -> R.id.nav_home
                }
                bottomNavView.selectedItemId = itemId
            }
        })

        // Setup badges with animations
        setupBottomNavigationBadges()
    }

    private fun setupFab() {
        binding.fab?.setOnClickListener {
            // 一键优化功能
            performQuickOptimization()
        }
    }

    private fun setupDrawer() {
        val navView = findViewById<com.google.android.material.navigation.NavigationView>(R.id.nav_view)
        navView?.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_drawer_browser -> {
                    openBrowser()
                    true
                }
                R.id.nav_drawer_battery -> {
                    openBatteryOptimization()
                    true
                }
                R.id.nav_drawer_browser_settings -> {
                    openBrowserSettings()
                    true
                }
                R.id.nav_drawer_about -> {
                    showAbout()
                    true
                }
                else -> false
            }
        }
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

        // 观察优化状态
        viewModel.optimizationState.observe(this) { state ->
            updateOptimizationState(state)
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

    private fun updateOptimizationState(state: com.lanhe.gongjuxiang.utils.OptimizationState) {
        val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab)
        fab?.apply {
            when (state) {
                com.lanhe.gongjuxiang.utils.OptimizationState.IDLE -> {
                    setImageResource(android.R.drawable.ic_menu_manage)
                    isEnabled = true
                }
                com.lanhe.gongjuxiang.utils.OptimizationState.RUNNING -> {
                    setImageResource(android.R.drawable.ic_popup_sync)
                    isEnabled = false
                }
                com.lanhe.gongjuxiang.utils.OptimizationState.COMPLETED -> {
                    setImageResource(android.R.drawable.checkbox_on_background)
                    isEnabled = true
                }
                com.lanhe.gongjuxiang.utils.OptimizationState.ERROR -> {
                    setImageResource(android.R.drawable.ic_delete)
                    isEnabled = true
                }
            }
        }
    }

    private fun setupBottomNavigationBadges() {
        val bottomNavView = binding.bottomNavView

        // Setup performance badge with custom drawable
        if (hasOptimizationSuggestions()) {
            val badge = bottomNavView.getOrCreateBadge(R.id.nav_performance)
            badge.number = getOptimizationSuggestionsCount()
            badge.backgroundColor = getColor(R.color.status_warning)
            animateBadgeEntry(badge)
        }

        // Setup security badge with custom drawable
        if (hasSecurityWarnings()) {
            val badge = bottomNavView.getOrCreateBadge(R.id.nav_security)
            badge.number = getSecurityWarningsCount()
            badge.backgroundColor = getColor(R.color.status_danger)
            animateBadgeEntry(badge)
        }
    }

    private fun updateBottomNavigationBadges() {
        setupBottomNavigationBadges()
    }

    private fun performQuickOptimization() {
        // 执行一键优化
        viewModel.performQuickOptimization()
    }

    private fun openBrowser() {
        YcWebViewBrowser.start(this)
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        drawerLayout?.closeDrawer(androidx.core.view.GravityCompat.START)
    }

    private fun openBatteryOptimization() {
        BatteryOptimizationActivity.start(this)
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        drawerLayout?.closeDrawer(androidx.core.view.GravityCompat.START)
    }

    private fun openBrowserSettings() {
        BrowserSettingsActivity.start(this)
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        drawerLayout?.closeDrawer(androidx.core.view.GravityCompat.START)
    }

    private fun showAbout() {
        // 显示关于页面
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        drawerLayout?.closeDrawer(androidx.core.view.GravityCompat.START)
    }

    private fun hasOptimizationSuggestions(): Boolean {
        // 检查是否有优化建议
        return viewModel.hasOptimizationSuggestions()
    }

    private fun hasSecurityWarnings(): Boolean {
        // 检查是否有安全警告
        return viewModel.hasSecurityWarnings()
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

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
        return if (navHostFragment != null) {
            val navController = navHostFragment.navController
            navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
        } else {
            super.onSupportNavigateUp()
        }
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
        val navigationRail = findViewById<NavigationRailView>(R.id.navigation_rail)
        navigationRail?.let { rail ->
            // Setup navigation rail for tablets
            rail.setOnItemSelectedListener { item ->
                val position = when (item.itemId) {
                    R.id.nav_home -> 0
                    R.id.nav_performance -> 1
                    R.id.nav_browser -> 2
                    R.id.nav_security -> 3
                    R.id.nav_settings -> 4
                    else -> 0
                }

                viewPager.setCurrentItem(position, true)
                hapticFeedbackManager.lightClick()
                true
            }

            // Sync ViewPager2 with NavigationRail
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    val itemId = when (position) {
                        0 -> R.id.nav_home
                        1 -> R.id.nav_performance
                        2 -> R.id.nav_browser
                        3 -> R.id.nav_security
                        4 -> R.id.nav_settings
                        else -> R.id.nav_home
                    }
                    rail.selectedItemId = itemId
                }
            })
        }
    }

    private fun setupLandscapeNavigation() {
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout_landscape)
        tabLayout?.let { tabs ->
            TabLayoutMediator(tabs, viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.nav_home)
                    1 -> getString(R.string.nav_performance)
                    2 -> getString(R.string.nav_browser)
                    3 -> getString(R.string.nav_security)
                    4 -> getString(R.string.nav_settings)
                    else -> ""
                }

                tab.setIcon(when (position) {
                    0 -> R.drawable.ic_home_24
                    1 -> R.drawable.ic_performance_24
                    2 -> R.drawable.ic_browser_24
                    3 -> R.drawable.ic_security_24
                    4 -> R.drawable.ic_settings_24
                    else -> R.drawable.ic_home_24
                })
            }.attach()

            // Add haptic feedback to tabs
            tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    hapticFeedbackManager.lightClick()
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    hapticFeedbackManager.mediumClick()
                }
            })
        }
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
