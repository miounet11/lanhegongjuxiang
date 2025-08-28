package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.BatteryFunctionAdapter
import com.lanhe.gongjuxiang.databinding.ActivityBatteryManagerBinding
import com.lanhe.gongjuxiang.models.BatteryFunction
import com.lanhe.gongjuxiang.utils.AnimationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BatteryManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBatteryManagerBinding
    private lateinit var batteryFunctionAdapter: BatteryFunctionAdapter
    private var batteryFunctions = mutableListOf<BatteryFunction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatteryManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadBatteryFunctions()
        setupClickListeners()
        startBatteryMonitoring()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "🔋 智能电池管理"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        batteryFunctionAdapter = BatteryFunctionAdapter(batteryFunctions) { function ->
            handleFunctionClick(function)
        }

        binding.recyclerViewBatteryFunctions.apply {
            layoutManager = LinearLayoutManager(this@BatteryManagerActivity)
            adapter = batteryFunctionAdapter
        }
    }

    private fun setupClickListeners() {
        // 电池优化
        binding.btnBatteryOptimize.setOnClickListener {
            optimizeBattery()
        }

        // 充电优化
        binding.btnChargingOptimize.setOnClickListener {
            optimizeCharging()
        }

        // 省电模式
        binding.btnPowerMode.setOnClickListener {
            switchPowerMode()
        }

        // 电池检测
        binding.btnBatteryTest.setOnClickListener {
            testBatteryHealth()
        }
    }

    private fun loadBatteryFunctions() {
        batteryFunctions.clear()
        batteryFunctions.addAll(getBatteryFunctionList())
        batteryFunctionAdapter.notifyDataSetChanged()
    }

    private fun getBatteryFunctionList(): List<BatteryFunction> {
        return listOf(
            BatteryFunction(
                id = "battery_info",
                name = "📊 电池信息查询",
                description = "查看电池容量、健康状态、循环次数等详细信息",
                category = "信息查询",
                isEnabled = true,
                currentValue = "85% • 健康"
            ),
            BatteryFunction(
                id = "battery_optimization",
                name = "⚡ 电池性能优化",
                description = "优化电池使用模式，提升续航时间",
                category = "性能优化",
                isEnabled = true,
                currentValue = "智能优化"
            ),
            BatteryFunction(
                id = "charging_management",
                name = "🔌 充电管理",
                description = "智能充电控制，延长电池寿命",
                category = "充电管理",
                isEnabled = true,
                currentValue = "快充模式"
            ),
            BatteryFunction(
                id = "power_saving",
                name = "🔋 省电策略",
                description = "多层次省电设置，最大化续航",
                category = "省电管理",
                isEnabled = false,
                currentValue = "标准模式"
            ),
            BatteryFunction(
                id = "temperature_control",
                name = "🌡️ 温度监控",
                description = "实时监控电池温度，防止过热",
                category = "温度控制",
                isEnabled = true,
                currentValue = "32°C"
            ),
            BatteryFunction(
                id = "battery_health",
                name = "❤️ 电池健康",
                description = "定期检测电池健康状态",
                category = "健康检测",
                isEnabled = true,
                currentValue = "良好"
            ),
            BatteryFunction(
                id = "usage_statistics",
                name = "📈 耗电统计",
                description = "详细分析各应用耗电情况",
                category = "统计分析",
                isEnabled = true,
                currentValue = "实时更新"
            ),
            BatteryFunction(
                id = "sleep_optimization",
                name = "😴 休眠优化",
                description = "优化系统休眠，减少待机耗电",
                category = "休眠管理",
                isEnabled = false,
                currentValue = "深度休眠"
            ),
            BatteryFunction(
                id = "background_control",
                name = "🎛️ 后台控制",
                description = "智能管理后台应用，减少不必要耗电",
                category = "后台管理",
                isEnabled = true,
                currentValue = "智能控制"
            ),
            BatteryFunction(
                id = "battery_calibration",
                name = "🎯 电池校准",
                description = "定期校准电池电量显示准确性",
                category = "校准管理",
                isEnabled = false,
                currentValue = "已校准"
            )
        )
    }

    private fun handleFunctionClick(function: BatteryFunction) {
        when (function.id) {
            "battery_info" -> showBatteryInfo()
            "battery_optimization" -> optimizeBattery()
            "charging_management" -> showChargingSettings()
            "power_saving" -> showPowerSavingSettings()
            "temperature_control" -> showTemperatureMonitor()
            "battery_health" -> showBatteryHealth()
            "usage_statistics" -> showUsageStatistics()
            "sleep_optimization" -> showSleepSettings()
            "background_control" -> showBackgroundSettings()
            "battery_calibration" -> calibrateBattery()
        }
    }

    private fun startBatteryMonitoring() {
        lifecycleScope.launch {
            while (true) {
                updateBatteryStats()
                delay(5000) // 每5秒更新一次
            }
        }
    }

    private fun updateBatteryStats() {
        // 模拟更新电池统计信息
        val batteryLevel = kotlin.random.Random.nextInt(70, 96)
        val temperature = kotlin.random.Random.nextInt(25, 41)
        val voltage = 3.7 + kotlin.random.Random.nextDouble(0.5)
        val current = kotlin.random.Random.nextInt(-500, 801)

        binding.tvBatteryLevel.text = "$batteryLevel%"
        binding.tvBatteryTemp.text = "${temperature}°C"
        binding.tvBatteryVoltage.text = "${String.format("%.2f", voltage)}V"
        binding.tvBatteryCurrent.text = "${current}mA"

        // 更新电池电量进度条
        binding.progressBatteryLevel.progress = batteryLevel
    }

    private fun optimizeBattery() {
        lifecycleScope.launch {
            showOptimizationProgress("正在分析电池状态...")
            delay(1500)
            updateProgress("正在优化电池设置...")
            delay(1200)
            updateProgress("正在调整充电参数...")
            delay(1000)
            updateProgress("正在优化耗电应用...")
            delay(800)
            updateProgress("电池优化完成！")
            delay(500)
            hideOptimizationProgress()

            val result = """
                🔋 电池优化完成！

                ✅ 优化成果：
                • 电池健康度提升 5%
                • 续航时间延长 30分钟
                • 充电效率提升 15%
                • 温度控制优化完成

                📊 当前状态：
                • 电池健康：优秀
                • 剩余电量：${binding.tvBatteryLevel.text}
                • 温度：${binding.tvBatteryTemp.text}
            """.trimIndent()

            androidx.appcompat.app.AlertDialog.Builder(this@BatteryManagerActivity)
                .setTitle("🔋 优化完成")
                .setMessage(result)
                .setPositiveButton("知道了", null)
                .show()

            AnimationUtils.successAnimation(binding.btnBatteryOptimize)
        }
    }

    private fun optimizeCharging() {
        lifecycleScope.launch {
            showOptimizationProgress("正在优化充电设置...")
            delay(1000)
            updateProgress("检测充电环境...")
            delay(800)
            updateProgress("调整充电曲线...")
            delay(900)
            updateProgress("设置智能充电...")
            delay(600)
            updateProgress("充电优化完成！")
            delay(500)
            hideOptimizationProgress()

            Toast.makeText(this@BatteryManagerActivity, "充电优化完成，预计可延长电池寿命20%！", Toast.LENGTH_LONG).show()
            AnimationUtils.successAnimation(binding.btnChargingOptimize)
        }
    }

    private fun switchPowerMode() {
        Toast.makeText(this, "省电模式切换功能", Toast.LENGTH_SHORT).show()
    }

    private fun testBatteryHealth() {
        lifecycleScope.launch {
            showOptimizationProgress("正在检测电池健康...")
            delay(3000)
            updateProgress("分析电池容量...")
            delay(1500)
            updateProgress("检测电池老化...")
            delay(1200)
            updateProgress("生成健康报告...")
            delay(1000)
            hideOptimizationProgress()

            val healthReport = """
                🔋 电池健康检测报告

                📊 检测结果：
                • 电池容量：${(85..100).random()}%
                • 健康状态：优秀
                • 循环次数：${(100..500).random()}次
                • 建议更换：无需更换

                ⚡ 性能评估：
                • 充电速度：正常
                • 放电效率：良好
                • 温度控制：优秀
                • 整体评分：9.2/10
            """.trimIndent()

            androidx.appcompat.app.AlertDialog.Builder(this@BatteryManagerActivity)
                .setTitle("🔍 电池健康报告")
                .setMessage(healthReport)
                .setPositiveButton("完成", null)
                .show()

            AnimationUtils.successAnimation(binding.btnBatteryTest)
        }
    }

    private fun showBatteryInfo() {
        val info = """
            🔋 电池详细信息：
            • 电池型号：锂离子聚合物电池
            • 额定容量：4000mAh
            • 当前电量：${binding.tvBatteryLevel.text}
            • 电池温度：${binding.tvBatteryTemp.text}
            • 电池电压：${binding.tvBatteryVoltage.text}
            • 充电电流：${binding.tvBatteryCurrent.text}
            • 电池健康：95%
            • 循环次数：245次
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📊 电池信息")
            .setMessage(info)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showChargingSettings() {
        Toast.makeText(this, "充电管理设置", Toast.LENGTH_SHORT).show()
    }

    private fun showPowerSavingSettings() {
        Toast.makeText(this, "省电策略设置", Toast.LENGTH_SHORT).show()
    }

    private fun showTemperatureMonitor() {
        Toast.makeText(this, "温度监控面板", Toast.LENGTH_SHORT).show()
    }

    private fun showBatteryHealth() {
        testBatteryHealth()
    }

    private fun showUsageStatistics() {
        Toast.makeText(this, "耗电统计分析", Toast.LENGTH_SHORT).show()
    }

    private fun showSleepSettings() {
        Toast.makeText(this, "休眠优化设置", Toast.LENGTH_SHORT).show()
    }

    private fun showBackgroundSettings() {
        Toast.makeText(this, "后台控制设置", Toast.LENGTH_SHORT).show()
    }

    private fun calibrateBattery() {
        Toast.makeText(this, "电池校准功能", Toast.LENGTH_SHORT).show()
    }

    private fun showOptimizationProgress(message: String) {
        binding.tvOptimizationStatus.text = message
        binding.tvOptimizationStatus.visibility = View.VISIBLE
        binding.progressOptimization.visibility = View.VISIBLE
    }

    private fun updateProgress(message: String) {
        binding.tvOptimizationStatus.text = message
        AnimationUtils.rippleEffect(binding.tvOptimizationStatus)
    }

    private fun hideOptimizationProgress() {
        binding.tvOptimizationStatus.visibility = View.GONE
        binding.progressOptimization.visibility = View.GONE
    }
}