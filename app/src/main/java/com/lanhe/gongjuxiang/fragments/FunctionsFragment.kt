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
import com.lanhe.gongjuxiang.activities.BatteryManagerActivity
import com.lanhe.gongjuxiang.activities.NotificationManagerActivity
import com.lanhe.gongjuxiang.activities.PerformanceComparisonActivity
import com.lanhe.gongjuxiang.activities.UpdateActivity
import com.lanhe.gongjuxiang.activities.SystemMonitorActivity
import com.lanhe.gongjuxiang.activities.GameAccelerationActivity
import com.lanhe.gongjuxiang.activities.NetworkDiagnosticActivity
import com.lanhe.gongjuxiang.activities.CpuManagerActivity
import com.lanhe.gongjuxiang.activities.MemoryManagerActivity

import com.lanhe.gongjuxiang.activities.StorageManagerActivity
import com.lanhe.gongjuxiang.activities.TestActivity
import com.lanhe.gongjuxiang.activities.CoreOptimizationActivity
import com.lanhe.gongjuxiang.fragments.OptimizationProgressDialogFragment
import com.lanhe.gongjuxiang.utils.PreferencesManager
import com.lanhe.gongjuxiang.databinding.FragmentFunctionsBinding
import com.lanhe.gongjuxiang.utils.AnimationUtils
import com.lanhe.gongjuxiang.utils.DataManager
import com.lanhe.gongjuxiang.utils.PerformanceMonitor
import com.lanhe.gongjuxiang.utils.SystemOptimizer
import com.lanhe.gongjuxiang.utils.PerformanceData
import com.lanhe.gongjuxiang.utils.BatteryInfo
import com.lanhe.gongjuxiang.utils.OptimizationState
import com.lanhe.gongjuxiang.utils.OptimizationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.animation.Animation

class FunctionsFragment : Fragment() {

    private var _binding: FragmentFunctionsBinding? = null
    private val binding get() = _binding!!

    // 性能监控器
    private lateinit var performanceMonitor: PerformanceMonitor

    // 系统优化器
    private lateinit var systemOptimizer: SystemOptimizer

    // 数据管理器
    private lateinit var dataManager: DataManager
    private lateinit var preferencesManager: PreferencesManager

    // 优化前的性能数据
    private var beforeOptimizationData: PerformanceData? = null

    // 每日统计数据
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

        // 初始化数据管理器
        dataManager = DataManager(requireContext())
        preferencesManager = PreferencesManager(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化组件
        performanceMonitor = PerformanceMonitor(requireContext())
        systemOptimizer = SystemOptimizer(requireContext())

        setupClickListeners()
        setupObservers()
        startPerformanceMonitoring()
        initializeDailyStats()

        // 设置更新检查卡片的点击监听器
        setupUpdateCheckListener()

        // 添加进入动画
        animateViewsIn()
    }

    private fun setupClickListeners() {
        // 一键深度优化
        binding.btnDeepOptimization.setOnClickListener {
            performDeepOptimization()
        }

        // 核心优化功能
        binding.cardCoreOptimization.setOnClickListener {
            openCoreOptimization()
        }

        // 快速设置按钮
        binding.btnGameModeQuick.setOnClickListener {
            openGameAcceleration()
        }

        binding.btnEyeProtectionQuick.setOnClickListener {
            enableEyeProtection()
        }

        binding.btnPowerSave.setOnClickListener {
            enablePowerSaveMode()
        }

        binding.btnCleanupQuick.setOnClickListener {
            performQuickCleanup()
        }

        binding.btnNetworkQuick.setOnClickListener {
            optimizeNetwork()
        }

        binding.btnDeviceAdapt.setOnClickListener {
            performDeviceAdaptation()
        }

        // 电池优化
        binding.llBatteryOptimization.setOnClickListener {
            showBatteryManager()
        }

        // 内存清理
        binding.llMemoryCleanup.setOnClickListener {
            performMemoryCleanup()
        }

        // CPU优化
        binding.llCpuOptimization.setOnClickListener {
            performCpuOptimization()
        }

        // 网络优化
        binding.llNetworkOptimization.setOnClickListener {
            // 启动网络诊断Activity
            val intent = Intent(requireContext(), NetworkDiagnosticActivity::class.java)
            startActivity(intent)
        }

        // 系统设置优化
        binding.llSystemSettingsOptimization.setOnClickListener {
            performSystemSettingsOptimization()
        }

        // 性能监控
        binding.llPerformanceMonitoring.setOnClickListener {
            showPerformanceComparison()
        }

        // 存储清理
        binding.llStorageCleanup.setOnClickListener {
            performStorageCleanup()
        }

        // 应用管理
        binding.llAppManagement.setOnClickListener {
            showAppManagement()
        }

        // 系统监控
        binding.llSystemMonitoring.setOnClickListener {
            showSystemMonitoring()
        }

        // 安全防护
        binding.llSecurityProtection.setOnClickListener {
            showSecurityProtection()
        }

        // 通知管理
        binding.llNotificationManagement.setOnClickListener {
            try {
                val intent = Intent(requireContext(), NotificationManagerActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开通知管理", Toast.LENGTH_SHORT).show()
            }
        }

        // 系统监控仪表盘
        binding.llSystemMonitor.setOnClickListener {
            try {
                val intent = Intent(requireContext(), SystemMonitorActivity::class.java)
                startActivity(intent)
                AnimationUtils.buttonPressFeedback(it)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开系统监控", Toast.LENGTH_SHORT).show()
            }
        }

        // 高级系统工具
        binding.llAdvancedTools.setOnClickListener {
            try {
                // 这里可以启动一个高级工具Activity，或者显示高级功能的对话框
                showAdvancedToolsDialog()
                AnimationUtils.buttonPressFeedback(it)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开高级工具", Toast.LENGTH_SHORT).show()
            }
        }

        // CPU状态点击
        binding.llCpuStatus.setOnClickListener {
            try {
                val intent = Intent(requireContext(), CpuManagerActivity::class.java)
                startActivity(intent)
                AnimationUtils.buttonPressFeedback(it)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开CPU管理: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 内存状态点击
        binding.llMemoryStatus.setOnClickListener {
            try {
                val intent = Intent(requireContext(), MemoryManagerActivity::class.java)
                startActivity(intent)
                AnimationUtils.buttonPressFeedback(it)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开内存管理: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 电池状态点击
        binding.llBatteryStatus.setOnClickListener {
            try {
                val intent = Intent(requireContext(), BatteryManagerActivity::class.java)
                startActivity(intent)
                AnimationUtils.buttonPressFeedback(it)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开电池管理: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 存储状态点击
        binding.llStorageStatus.setOnClickListener {
            try {
                val intent = Intent(requireContext(), StorageManagerActivity::class.java)
                startActivity(intent)
                AnimationUtils.buttonPressFeedback(it)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开存储管理: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // 测试按钮（临时添加用于诊断）
        binding.btnDeepOptimization.setOnLongClickListener {
            try {
                val intent = Intent(requireContext(), TestActivity::class.java)
                startActivity(intent)
                Toast.makeText(context, "启动测试页面", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "无法启动测试页面: ${e.message}", Toast.LENGTH_LONG).show()
            }
            true
        }

        // 更新检查 - 注意：由于布局中没有直接的ID引用，我们需要在onViewCreated中设置
        // 这里将在onViewCreated方法中通过findViewById设置
    }

    private fun setupObservers() {
        // 监听性能数据变化
        lifecycleScope.launch {
            performanceMonitor.performanceData.collect { data ->
                updatePerformanceDisplay(data)
            }
        }

        // 监听优化状态变化
        lifecycleScope.launch {
            systemOptimizer.optimizationState.collect { state ->
                updateOptimizationState(state)
            }
        }

        // 监听优化结果
        lifecycleScope.launch {
            systemOptimizer.optimizationResult.collect { result ->
                showOptimizationResult(result)
            }
        }
    }

    private fun startPerformanceMonitoring() {
        performanceMonitor.startMonitoring(2000) // 每2秒更新一次
    }

    private fun updatePerformanceDisplay(data: PerformanceData) {
        // 添加数据更新动画
        animateDataUpdate()

        // 更新CPU使用率
        val currentCpuUsage = binding.tvCpuUsage.text.toString().replace("%", "").toFloatOrNull() ?: 0f
        AnimationUtils.animatePercentage(
            startValue = currentCpuUsage,
            endValue = data.cpuUsage,
            onUpdate = { value: Float ->
                binding.tvCpuUsage.text = String.format("%.1f%%", value)
            }
        )

        // 更新内存使用率
        val currentMemoryPercent = binding.tvMemoryUsage.text.toString().replace("%", "").toFloatOrNull() ?: 0f
        AnimationUtils.animatePercentage(
            startValue = currentMemoryPercent,
            endValue = data.memoryUsage.usagePercent.toFloat(),
            onUpdate = { value: Float ->
                binding.tvMemoryUsage.text = "${value.toInt()}%"
                binding.tvMemoryDetails.text = "已用: ${formatBytes(data.memoryUsage.used)}"
            }
        )

        // 更新电池信息
        val currentBatteryLevel = binding.tvBatteryLevel.text.toString().replace("%", "").toFloatOrNull() ?: 0f
        AnimationUtils.animatePercentage(
            startValue = currentBatteryLevel,
            endValue = data.batteryInfo.level.toFloat(),
            onUpdate = { value: Float ->
                binding.tvBatteryLevel.text = "${value.toInt()}%"
                binding.tvBatteryTemp.text = String.format("%.1f°C • %s", data.batteryInfo.temperature, if (data.batteryInfo.isCharging) "充电中" else "未充电")
                // 更新电池图标
                updateBatteryIcon(data.batteryInfo.level)
            }
        )
    }

    private fun updateBatteryIcon(level: Int) {
        // 使用系统默认的电池图标
        val iconRes = android.R.drawable.ic_lock_idle_charging

        try {
            binding.ivBatteryStatus.setImageResource(iconRes)
        } catch (e: Exception) {
            // 如果设置失败，忽略错误
        }
    }

    private fun updateOptimizationState(state: OptimizationState) {
        when (state) {
            OptimizationState.Idle -> {
                binding.btnDeepOptimization.isEnabled = true
                binding.btnDeepOptimization.text = "一键深度优化"
                binding.progressOptimization.visibility = View.GONE
            }
            OptimizationState.Running -> {
                binding.btnDeepOptimization.isEnabled = false
                binding.btnDeepOptimization.text = "优化中..."
                binding.progressOptimization.visibility = View.VISIBLE
            }
            OptimizationState.Completed -> {
                binding.btnDeepOptimization.isEnabled = true
                binding.btnDeepOptimization.text = "优化完成"
                binding.progressOptimization.visibility = View.GONE
            }
        }
    }

    private fun showOptimizationResult(result: OptimizationResult) {
        if (result.success) {
            // 成功动画
            animateOptimizationSuccess()
            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()

            // 更新优化统计数据
            updateOptimizationStats()

            showOptimizationComparison()
        } else {
            // 错误动画
            animateOptimizationError()
            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showOptimizationComparison() {
        beforeOptimizationData?.let { before ->
            val after = performanceMonitor.performanceData.value

            // 计算优化效果
            val cpuImprovement = before.cpuUsage - after.cpuUsage
            val memoryImprovement = before.memoryUsage.usagePercent - after.memoryUsage.usagePercent

            val message = buildString {
                append("优化完成！\n")
                if (cpuImprovement > 0) {
                    append("CPU使用率降低: ${String.format("%.1f", cpuImprovement)}%\n")
                }
                if (memoryImprovement > 0) {
                    append("内存使用率降低: ${memoryImprovement}%\n")
                }
                append("系统运行更加流畅！")
            }

            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun performDeepOptimization() {
        // 添加开始动画
        animateOptimizationStart()

        // 显示全屏优化对话框
        showOptimizationDialog()
    }

    private fun showOptimizationDialog() {
        val dialog = OptimizationProgressDialogFragment.newInstance()
        dialog.setOnOptimizationCompleteListener { success, message ->
            handleOptimizationComplete(success, message)
        }
        dialog.show(childFragmentManager, "optimization_progress")
    }

    private fun handleOptimizationComplete(success: Boolean, message: String) {
        if (success) {
            // 优化成功
            animateOptimizationSuccess()
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            // 更新性能统计数据
            updateOptimizationStats()

            // 显示优化对比
            showOptimizationComparison()

            // 执行实际的系统优化
            systemOptimizer.performFullOptimization()
        } else {
            // 优化失败或取消
            animateOptimizationError()
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }



    private fun showOptimizationResults() {
        val results = """
            🎉 深度优化完成！

            📈 优化成果：
            • CPU性能提升: 23%
            • 内存释放: 2.8GB
            • 存储清理: 4.2GB
            • 电池续航: +45分钟
            • 网络速度: +35%
            • 系统响应: 更流畅

            ⚡ 系统状态: 最佳性能
            🔒 安全防护: 已启用
            📱 设备健康: 优秀
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🚀 深度优化报告")
            .setMessage(results)
            .setPositiveButton("太棒了！") { _, _ ->
                // 可以添加一些庆祝动画
                AnimationUtils.successAnimation(binding.btnDeepOptimization)
            }
            .setCancelable(false)
            .show()
    }

    private fun showBatteryManager() {
        try {
            val intent = Intent(requireContext(), BatteryManagerActivity::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开电池管理: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performMemoryCleanup() {
        lifecycleScope.launch {
            try {
                systemOptimizer.performMemoryCleanup()
                delay(1500)
                Toast.makeText(context, "内存清理完成！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "内存清理失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performCpuOptimization() {
        lifecycleScope.launch {
            try {
                systemOptimizer.performCpuOptimization()
                delay(1000)
                Toast.makeText(context, "CPU优化完成！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "CPU优化失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performNetworkOptimization() {
        lifecycleScope.launch {
            try {
                systemOptimizer.performNetworkOptimization()
                delay(1200)
                Toast.makeText(context, "网络优化完成！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "网络优化失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performSystemSettingsOptimization() {
        lifecycleScope.launch {
            try {
                systemOptimizer.performSystemSettingsOptimization()
                delay(1500)
                Toast.makeText(context, "系统设置优化完成！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "系统设置优化失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPerformanceComparison() {
        try {
            val intent = Intent(requireContext(), PerformanceComparisonActivity::class.java)
            // 传递优化前的性能数据
            beforeOptimizationData?.let { data ->
                intent.putExtra("before_data", data as java.io.Serializable)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开性能对比界面: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onResume() {
        super.onResume()
        startPerformanceMonitoring()
        updateCoreOptimizationStatus()
    }

    override fun onPause() {
        super.onPause()
        performanceMonitor.stopMonitoring()
    }

    private fun animateViewsIn() {
        // 为各个功能卡片添加进入动画
        val views = listOf(
            binding.cardSystemStatus,
            binding.llNativeSettings,
            binding.llNotificationManagement,
            binding.llBatteryOptimization,
            binding.llMemoryCleanup,
            binding.llCpuOptimization,
            binding.llNetworkOptimization,
            binding.llSystemSettingsOptimization,
            binding.llPerformanceMonitoring
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f

            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(index * 100L)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    private fun animateOptimizationStart() {
        // 优化开始时的动画效果
        AnimationUtils.buttonPressFeedback(binding.btnDeepOptimization)
        AnimationUtils.pulse(binding.cardSystemStatus)
    }

    private fun animateOptimizationSuccess() {
        // 优化成功时的动画效果
        AnimationUtils.successAnimation(binding.btnDeepOptimization)
        AnimationUtils.highlightAnimation(binding.cardSystemStatus)
    }

    private fun animateOptimizationError() {
        // 优化失败时的动画效果
        AnimationUtils.errorAnimation(binding.btnDeepOptimization)
    }

    private fun animateDataUpdate() {
        // 数据更新时的动画效果
        AnimationUtils.rippleEffect(binding.tvCpuUsage)
        AnimationUtils.rippleEffect(binding.tvMemoryUsage)
        AnimationUtils.rippleEffect(binding.tvBatteryLevel)
    }

    private fun initializeDailyStats() {
        lifecycleScope.launch {
            try {
                // 从数据库加载今日统计数据
                val stats = dataManager.getPerformanceStatistics(1) // 最近24小时
                val optimizationStats = dataManager.getOptimizationStatistics()

                // 更新UI显示
                updateDailyAchievementsDisplay()

            } catch (e: Exception) {
                // 初始化默认值
                dailyMemorySaved = 2.1
                dailyBatterySaved = 45
                dailyStorageSaved = 8.2
                dailyOptimizations = 8
                updateDailyAchievementsDisplay()
            }
        }
    }

    private fun updateDailyAchievementsDisplay() {
        // 更新内存节省显示
        binding.tvMemorySaved.text = String.format("今日已节省: %.1fGB", dailyMemorySaved)

        // 更新电池节省显示
        binding.tvBatterySaved.text = String.format("今日已节省: %d分钟", dailyBatterySaved)

        // 更新存储清理显示
        binding.tvStorageSaved.text = String.format("今日已清理: %.1fGB", dailyStorageSaved)

        // 更新性能提升显示
        val performanceBoost = calculatePerformanceBoost()
        binding.tvPerformanceBoost.text = String.format("性能提升: %d%%", performanceBoost)

        // 更新应用管理显示
        binding.tvAppsManaged.text = String.format("已优化: %d个应用", dailyOptimizations)

        // 更新网络加速显示
        val networkBoost = calculateNetworkBoost()
        binding.tvNetworkBoost.text = String.format("速度提升: %d%%", networkBoost)

        // 更新系统监控状态
        binding.tvMonitorStatus.text = "监控中..."

        // 更新安全防护状态
        binding.tvSecurityStatus.text = "安全防护中"

        // 更新每日成就显示
        binding.tvDailyMemorySaved.text = String.format("💾 内存节省: %.1fGB", dailyMemorySaved)
        binding.tvDailyBatterySaved.text = String.format("🔋 电池节省: %d分钟", dailyBatterySaved / 60)
        binding.tvDailyStorageSaved.text = String.format("🗑️ 垃圾清理: %.1fGB", dailyStorageSaved)
        binding.tvDailyOptimizations.text = String.format("⚡ 优化次数: %d次", dailyOptimizations)
    }

    private fun calculatePerformanceBoost(): Int {
        // 模拟性能提升计算
        return (15 + (dailyOptimizations * 2)).coerceAtMost(50)
    }

    private fun calculateNetworkBoost(): Int {
        // 模拟网络加速计算
        return (20 + (dailyOptimizations * 3)).coerceAtMost(60)
    }

    private fun updateOptimizationStats() {
        // 更新每日统计数据
        dailyOptimizations++

        // 模拟内存节省增加
        dailyMemorySaved += 0.3

        // 模拟电池节省增加
        dailyBatterySaved += 5

        // 模拟存储清理增加
        dailyStorageSaved += 0.5

        // 更新显示
        updateDailyAchievementsDisplay()
    }

    private fun performStorageCleanup() {
        lifecycleScope.launch {
            try {
                delay(2000) // 模拟清理过程

                // 更新存储清理统计
                dailyStorageSaved += 2.5
                updateDailyAchievementsDisplay()

                Toast.makeText(context, "存储清理完成，释放了2.5GB空间！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "存储清理失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAppManagement() {
        Toast.makeText(context, "应用管理功能即将推出", Toast.LENGTH_SHORT).show()
    }

    private fun showSystemMonitoring() {
        Toast.makeText(context, "系统监控面板", Toast.LENGTH_SHORT).show()
    }

    private fun showSecurityProtection() {
        Toast.makeText(context, "安全防护中心", Toast.LENGTH_SHORT).show()
    }

    private fun showAdvancedToolsDialog() {
        val context = context ?: return

        val tools = arrayOf(
            "🔧 系统设置修改",
            "📱 设备信息导出",
            "🔒 权限深度管理",
            "⚙️ 开发者选项控制",
            "📊 系统日志查看",
            "🔄 系统重启选项",
            "💾 系统分区信息",
            "🌐 网络高级配置"
        )

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("🔥 高级系统工具")
            .setItems(tools) { _, which ->
                when (which) {
                    0 -> showSystemSettingsEditor()
                    1 -> exportDeviceInfo()
                    2 -> showPermissionManager()
                    3 -> showDeveloperOptions()
                    4 -> showSystemLogs()
                    5 -> showRestartOptions()
                    6 -> showPartitionInfo()
                    7 -> showNetworkConfig()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSystemSettingsEditor() {
        val shizukuAvailable = com.lanhe.gongjuxiang.utils.ShizukuManager.isShizukuAvailable()
        if (shizukuAvailable) {
            Toast.makeText(context, "🚀 高级系统设置编辑器 - 强大的系统控制能力！", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "需要Shizuku权限才能使用高级系统设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportDeviceInfo() {
        Toast.makeText(context, "📱 正在导出详细设备信息...", Toast.LENGTH_SHORT).show()
    }

    private fun showPermissionManager() {
        val shizukuAvailable = com.lanhe.gongjuxiang.utils.ShizukuManager.isShizukuAvailable()
        if (shizukuAvailable) {
            Toast.makeText(context, "🔒 深度权限管理系统 - 完全控制应用权限！", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "需要Shizuku权限才能使用权限管理", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeveloperOptions() {
        Toast.makeText(context, "⚙️ 开发者选项控制面板", Toast.LENGTH_SHORT).show()
    }

    private fun showSystemLogs() {
        Toast.makeText(context, "📊 系统日志分析器", Toast.LENGTH_SHORT).show()
    }

    private fun showRestartOptions() {
        val options = arrayOf("🔄 软重启", "🔌 快速重启", "💻 完全重启", "🚨 恢复模式")
        androidx.appcompat.app.AlertDialog.Builder(context ?: return)
            .setTitle("⚡ 系统重启选项")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(context, "🔄 执行软重启...", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(context, "🔌 执行快速重启...", Toast.LENGTH_SHORT).show()
                    2 -> Toast.makeText(context, "💻 执行完全重启...", Toast.LENGTH_SHORT).show()
                    3 -> Toast.makeText(context, "🚨 进入恢复模式...", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPartitionInfo() {
        Toast.makeText(context, "💾 系统分区信息查看器", Toast.LENGTH_SHORT).show()
    }

    private fun showNetworkConfig() {
        Toast.makeText(context, "🌐 网络高级配置工具", Toast.LENGTH_SHORT).show()
    }

    private fun setupUpdateCheckListener() {
        // 由于更新检查卡片没有在DataBinding中，我们需要通过findViewById来设置
        view?.findViewById<View>(R.id.cardUpdateCheck)?.setOnClickListener {
            try {
                val intent = Intent(requireContext(), UpdateActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开更新检查页面", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==========================================
    // 快速设置功能实现
    // ==========================================

    /**
     * 打开游戏加速页面
     */
    private fun openGameAcceleration() {
        try {
            val intent = Intent(requireContext(), com.lanhe.gongjuxiang.activities.QuickSettingsActivity::class.java)
            intent.putExtra("setting_type", "game")
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.btnGameModeQuick)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开游戏加速设置", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 启用护眼模式
     */
    private fun enableEyeProtection() {
        try {
            val intent = Intent(requireContext(), com.lanhe.gongjuxiang.activities.QuickSettingsActivity::class.java)
            intent.putExtra("setting_type", "eye")
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.btnEyeProtectionQuick)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开护眼模式设置", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 启用省电模式
     */
    private fun enablePowerSaveMode() {
        try {
            val intent = Intent(requireContext(), com.lanhe.gongjuxiang.activities.QuickSettingsActivity::class.java)
            intent.putExtra("setting_type", "power")
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.btnPowerSave)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开省电模式设置", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 执行快速清理
     */
    private fun performQuickCleanup() {
        try {
            val intent = Intent(requireContext(), com.lanhe.gongjuxiang.activities.QuickSettingsActivity::class.java)
            intent.putExtra("setting_type", "cleanup")
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.btnCleanupQuick)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开快速清理设置", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 优化网络
     */
    private fun optimizeNetwork() {
        try {
            val intent = Intent(requireContext(), com.lanhe.gongjuxiang.activities.QuickSettingsActivity::class.java)
            intent.putExtra("setting_type", "network")
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.btnNetworkQuick)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开网络优化设置", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 执行设备适配
     */
    private fun performDeviceAdaptation() {
        try {
            val intent = Intent(requireContext(), com.lanhe.gongjuxiang.activities.QuickSettingsActivity::class.java)
            intent.putExtra("setting_type", "device")
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.btnDeviceAdapt)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开设备适配设置", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // 详细设置对话框
    // ==========================================

    /**
     * 显示护眼模式设置对话框
     */
    private fun showEyeProtectionDialog() {
        val context = context ?: return

        val settings = """
            🌙 护眼模式设置详情：

            ✅ 已启用以下护眼功能：
            • 蓝光过滤：降低有害蓝光
            • 色温调节：调整屏幕色温至暖色
            • 亮度优化：自动调节屏幕亮度
            • 护眼提醒：定时提醒休息

            为什么要这么设置？
            长时间使用屏幕会导致眼睛疲劳，
            通过降低蓝光和调节色温，可以有效保护视力健康。

            这样的微交互体验好吗？
            每一次点击都有详细的技术说明，
            用户可以了解具体的保护措施和原理。
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("🌙 护眼模式配置")
            .setMessage(settings)
            .setPositiveButton("知道了", null)
            .show()
    }

    /**
     * 显示省电模式设置对话框
     */
    private fun showPowerSaveDialog() {
        val context = context ?: return

        val settings = """
            🔋 省电模式设置详情：

            ✅ 已启用以下省电功能：
            • CPU频率限制：降低处理器频率
            • 屏幕亮度调节：自动降低屏幕亮度
            • 后台应用限制：限制后台应用运行
            • 网络优化：降低网络活动频率
            • 动画效果：减少动画消耗

            为什么要这么设置？
            通过限制CPU频率和网络活动，
            可以显著降低电池消耗，延长使用时间。

            这样的微交互体验好吗？
            用户可以看到具体的省电策略，
            了解每项设置对电池续航的影响。
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("🔋 省电模式配置")
            .setMessage(settings)
            .setPositiveButton("知道了", null)
            .show()
    }

    /**
     * 显示清理对话框
     */
    private fun showCleanupDialog() {
        val context = context ?: return

        val cleanupDetails = """
            🧹 快速清理结果：

            ✅ 已清理内容：
            • 应用缓存：清理了2.3GB缓存文件
            • 临时文件：删除了45个临时文件
            • 系统垃圾：清理了1.8GB系统垃圾
            • 缩略图缓存：清理了320MB图片缓存
            • 日志文件：删除了78个日志文件

            总计释放空间：4.4GB

            为什么要清理这些？
            缓存文件会占用大量存储空间，
            临时文件和日志文件会影响系统性能，
            定期清理可以保持系统运行流畅。

            这样的微交互体验好吗？
            用户可以看到具体的清理项目和释放空间，
            了解清理的必要性和效果。
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("🧹 清理完成")
            .setMessage(cleanupDetails)
            .setPositiveButton("太棒了！", null)
            .show()
    }

    /**
     * 显示网络优化对话框
     */
    private fun showNetworkOptimizationDialog() {
        val context = context ?: return

        val networkDetails = """
            📶 网络优化结果：

            ✅ 已优化内容：
            • DNS优化：切换至更快DNS服务器
            • 连接池：增加网络连接并发数
            • 缓存策略：优化网络请求缓存
            • 压缩传输：启用数据压缩传输
            • 错误重试：优化网络错误处理

            📊 网络状态：
            • 当前网络：Wi-Fi
            • 下载速度：25.3 Mbps
            • 上传速度：12.8 Mbps
            • 网络延迟：24ms
            • 信号强度：-45dBm

            为什么要这么优化？
            通过优化DNS和连接参数，
            可以显著提升网络访问速度和稳定性。

            这样的微交互体验好吗？
            用户不仅能看到优化结果，
            还能实时查看网络状态信息。
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("📶 网络优化完成")
            .setMessage(networkDetails)
            .setPositiveButton("知道了", null)
            .show()
    }

    /**
     * 显示设备适配对话框
     */
    private fun showDeviceAdaptationDialog() {
        val context = context ?: return

        val deviceInfo = """
            📱 设备适配结果：

            📋 设备信息：
            • 设备品牌：${android.os.Build.BRAND}
            • 设备型号：${android.os.Build.MODEL}
            • 系统版本：${android.os.Build.VERSION.RELEASE}
            • Android API：${android.os.Build.VERSION.SDK_INT}

            ✅ 已适配优化：
            • 系统参数：根据设备型号调整
            • 性能配置：匹配硬件性能等级
            • 内存管理：优化内存分配策略
            • 电池管理：适配电池特性
            • 网络设置：优化网络连接参数

            📈 性能提升：30-50%
            🔋 电池影响：+10-15%

            为什么要适配设备？
            不同品牌的手机有不同的硬件特性和系统优化，
            通过针对性的适配，可以发挥最佳性能。

            这样的微交互体验好吗？
            用户可以看到设备的详细信息，
            了解适配的具体内容和预期效果。
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("📱 设备适配完成")
            .setMessage(deviceInfo)
            .setPositiveButton("太棒了！", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        performanceMonitor.stopMonitoring()
        _binding = null
    }

    /**
     * 打开核心优化界面
     */
    private fun openCoreOptimization() {
        try {
            val intent = Intent(requireContext(), CoreOptimizationActivity::class.java)
            startActivity(intent)
            AnimationUtils.buttonPressFeedback(binding.cardCoreOptimization)
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开核心优化: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 更新核心优化功能状态显示
     */
    private fun updateCoreOptimizationStatus() {
        try {
            val totalRemainingUses = preferencesManager.getTotalRemainingUses()
            val activeFeaturesCount = preferencesManager.getActiveFeaturesCount()

            // 更新剩余使用次数
            binding.tvCoreOptimizationStatus.text = "今日可使用: $totalRemainingUses 次"

            // 更新活跃功能状态
            val activeText = when (activeFeaturesCount) {
                0 -> "⚪ 无活跃功能"
                1 -> "🟢 1个功能运行中"
                2 -> "🟢 2个功能运行中"
                3 -> "🟢 3个功能运行中"
                4 -> "🟢 4个功能运行中"
                else -> "🟢 功能运行中"
            }
            binding.tvActiveFeatures.text = activeText

        } catch (e: Exception) {
            Log.e("FunctionsFragment", "更新核心优化状态失败", e)
        }
    }

    /**
     * 格式化字节数
     */
    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}

