package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.browser.BrowserSettingsActivity
import com.lanhe.gongjuxiang.browser.YcWebViewBrowser
import com.lanhe.gongjuxiang.databinding.ActivityMainBinding
import com.lanhe.gongjuxiang.services.ChargingReminderService
import com.lanhe.gongjuxiang.settings.BatteryOptimizationActivity
import com.lanhe.gongjuxiang.viewmodels.MainViewModel

/**
 * 主活动 - 统一UI设计
 * 采用现代化的Material Design，统一展示所有功能
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        setupNavigation()
        setupBottomNavigation()
        setupFab()
        setupDrawer()

        // 启动充电提醒服务
        ChargingReminderService.startService(this)

        // 观察ViewModel数据
        observeViewModel()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        // 设置顶部应用栏配置
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_performance,
                R.id.nav_browser,
                R.id.nav_security,
                R.id.nav_settings
            ),
            binding.drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // 设置侧边栏导航
        binding.navView.setupWithNavController(navController)
    }

    private fun setupBottomNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNavView: BottomNavigationView = binding.appBarMain.contentMain.bottomNavView
        bottomNavView.setupWithNavController(navController)

        // 设置底部导航项的徽章
        updateBottomNavigationBadges()
    }

    private fun setupFab() {
        binding.appBarMain.fab.setOnClickListener {
            // 一键优化功能
            performQuickOptimization()
        }
    }

    private fun setupDrawer() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
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
        binding.appBarMain.contentMain.apply {
            tvCpuUsage.text = String.format("%.1f%%", data.cpuUsage)
            tvMemoryUsage.text = String.format("%.1f%%", data.memoryUsage.usagePercent)
            tvStorageUsage.text = String.format("%.1f%%", data.storageUsage)
        }
    }

    private fun updateBatteryStatus(battery: com.lanhe.gongjuxiang.models.BatteryInfo) {
        binding.appBarMain.contentMain.apply {
            tvBatteryLevel.text = "${battery.level}%"
            tvBatteryTemp.text = String.format("%.1f°C", battery.temperature)

            // 更新电池图标
            ivBatteryStatus.setImageResource(
                when {
                    battery.level >= 80 -> R.drawable.ic_battery_full
                    battery.level >= 60 -> R.drawable.ic_battery_high
                    battery.level >= 30 -> R.drawable.ic_battery_medium
                    battery.level >= 15 -> R.drawable.ic_battery_low
                    else -> R.drawable.ic_battery_critical
                }
            )
        }
    }

    private fun updateNetworkStatus(network: com.lanhe.gongjuxiang.models.NetworkStats) {
        binding.appBarMain.contentMain.apply {
            tvNetworkType.text = network.interfaceName
            tvNetworkSpeed.text = formatNetworkSpeed(network)
        }
    }

    private fun updateOptimizationState(state: com.lanhe.gongjuxiang.utils.OptimizationState) {
        binding.appBarMain.fab.apply {
            when (state) {
                com.lanhe.gongjuxiang.utils.OptimizationState.IDLE -> {
                    setImageResource(R.drawable.ic_optimize)
                    isEnabled = true
                }
                com.lanhe.gongjuxiang.utils.OptimizationState.RUNNING -> {
                    setImageResource(R.drawable.ic_optimizing)
                    isEnabled = false
                }
                com.lanhe.gongjuxiang.utils.OptimizationState.COMPLETED -> {
                    setImageResource(R.drawable.ic_optimized)
                    isEnabled = true
                }
                com.lanhe.gongjuxiang.utils.OptimizationState.ERROR -> {
                    setImageResource(R.drawable.ic_error)
                    isEnabled = true
                }
            }
        }
    }

    private fun updateBottomNavigationBadges() {
        val bottomNavView: BottomNavigationView = binding.appBarMain.contentMain.bottomNavView

        // 为性能标签添加徽章（如果有优化建议）
        val performanceMenuItem = bottomNavView.menu.findItem(R.id.nav_performance)
        if (hasOptimizationSuggestions()) {
            performanceMenuItem.setIcon(R.drawable.ic_performance_with_badge)
        }

        // 为安全标签添加徽章（如果有安全警告）
        val securityMenuItem = bottomNavView.menu.findItem(R.id.nav_security)
        if (hasSecurityWarnings()) {
            securityMenuItem.setIcon(R.drawable.ic_security_with_badge)
        }
    }

    private fun performQuickOptimization() {
        // 执行一键优化
        viewModel.performQuickOptimization()
    }

    private fun openBrowser() {
        YcWebViewBrowser.start(this)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun openBatteryOptimization() {
        BatteryOptimizationActivity.start(this)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun openBrowserSettings() {
        BrowserSettingsActivity.start(this)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun showAbout() {
        // 显示关于页面
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
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
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
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
}
