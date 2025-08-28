package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.ElectromagneticFunctionAdapter
import com.lanhe.gongjuxiang.databinding.ActivityElectromagneticManagerBinding
import com.lanhe.gongjuxiang.models.ElectromagneticFunction
import com.lanhe.gongjuxiang.utils.AnimationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ElectromagneticManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityElectromagneticManagerBinding
    private lateinit var electromagneticFunctionAdapter: ElectromagneticFunctionAdapter
    private var electromagneticFunctions = mutableListOf<ElectromagneticFunction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityElectromagneticManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadElectromagneticFunctions()
        setupClickListeners()
        startElectromagneticMonitoring()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "📶 电磁辐射管理"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        electromagneticFunctionAdapter = ElectromagneticFunctionAdapter(electromagneticFunctions) { function ->
            handleFunctionClick(function)
        }

        binding.recyclerViewElectromagneticFunctions.apply {
            layoutManager = LinearLayoutManager(this@ElectromagneticManagerActivity)
            adapter = electromagneticFunctionAdapter
        }
    }

    private fun setupClickListeners() {
        // WiFi信号优化
        binding.btnWifiOptimize.setOnClickListener {
            optimizeWifiSignal()
        }

        // 蓝牙辐射控制
        binding.btnBluetoothControl.setOnClickListener {
            controlBluetoothRadiation()
        }

        // 移动网络优化
        binding.btnMobileNetworkOptimize.setOnClickListener {
            optimizeMobileNetwork()
        }

        // 辐射检测
        binding.btnRadiationDetection.setOnClickListener {
            performRadiationDetection()
        }
    }

    private fun loadElectromagneticFunctions() {
        electromagneticFunctions.clear()
        electromagneticFunctions.addAll(getElectromagneticFunctionList())
        electromagneticFunctionAdapter.notifyDataSetChanged()
    }

    private fun getElectromagneticFunctionList(): List<ElectromagneticFunction> {
        return listOf(
            ElectromagneticFunction(
                id = "wifi_signal",
                name = "📶 WiFi信号优化",
                description = "优化WiFi信号强度和稳定性，减少辐射",
                category = "WiFi管理",
                isEnabled = true,
                currentValue = "-45dBm"
            ),
            ElectromagneticFunction(
                id = "bluetooth_radiation",
                name = "📱 蓝牙辐射控制",
                description = "智能控制蓝牙设备辐射强度",
                category = "蓝牙管理",
                isEnabled = true,
                currentValue = "低辐射模式"
            ),
            ElectromagneticFunction(
                id = "mobile_network",
                name = "📡 移动网络优化",
                description = "优化移动网络信号，降低电磁辐射",
                category = "移动网络",
                isEnabled = true,
                currentValue = "4G网络"
            ),
            ElectromagneticFunction(
                id = "radiation_detection",
                name = "🔍 辐射检测分析",
                description = "实时检测设备电磁辐射水平",
                category = "辐射检测",
                isEnabled = true,
                currentValue = "辐射正常"
            ),
            ElectromagneticFunction(
                id = "antenna_management",
                name = "📻 天线功率管理",
                description = "智能调节天线发射功率",
                category = "天线管理",
                isEnabled = false,
                currentValue = "自动调节"
            ),
            ElectromagneticFunction(
                id = "frequency_band",
                name = "📊 频段智能选择",
                description = "选择最佳频段，优化信号质量",
                category = "频段管理",
                isEnabled = true,
                currentValue = "2.4GHz"
            ),
            ElectromagneticFunction(
                id = "radiation_shielding",
                name = "🛡️ 辐射屏蔽保护",
                description = "启用辐射屏蔽技术，保护用户健康",
                category = "辐射防护",
                isEnabled = false,
                currentValue = "屏蔽模式"
            ),
            ElectromagneticFunction(
                id = "signal_modulation",
                name = "🎵 信号调制优化",
                description = "优化信号调制方式，提升传输效率",
                category = "信号调制",
                isEnabled = true,
                currentValue = "QAM256"
            ),
            ElectromagneticFunction(
                id = "power_amplifier",
                name = "🔊 功率放大器控制",
                description = "精确控制功率放大器，节约能源",
                category = "功率控制",
                isEnabled = true,
                currentValue = "智能调节"
            ),
            ElectromagneticFunction(
                id = "electromagnetic_security",
                name = "🔒 电磁安全监控",
                description = "监控电磁信号安全，防止信息泄露",
                category = "安全监控",
                isEnabled = true,
                currentValue = "安全模式"
            )
        )
    }

    private fun handleFunctionClick(function: ElectromagneticFunction) {
        when (function.id) {
            "wifi_signal" -> showWifiSettings()
            "bluetooth_radiation" -> showBluetoothSettings()
            "mobile_network" -> showMobileNetworkSettings()
            "radiation_detection" -> showRadiationDetection()
            "antenna_management" -> showAntennaSettings()
            "frequency_band" -> showFrequencyBandSettings()
            "radiation_shielding" -> showShieldingSettings()
            "signal_modulation" -> showModulationSettings()
            "power_amplifier" -> showPowerAmplifierSettings()
            "electromagnetic_security" -> showSecuritySettings()
        }
    }

    private fun startElectromagneticMonitoring() {
        lifecycleScope.launch {
            while (true) {
                updateElectromagneticStats()
                delay(3000) // 每3秒更新一次
            }
        }
    }

    private fun updateElectromagneticStats() {
        // 模拟更新电磁统计信息
        val wifiSignal = (-30..-80).random()
        val radiationLevel = (10..50).random()
        val networkType = listOf("4G", "5G", "WiFi").random()

        binding.tvWifiSignal.text = "${wifiSignal}dBm"
        binding.tvRadiationLevel.text = "${radiationLevel}μW/cm²"
        binding.tvNetworkType.text = networkType

        // 更新信号强度指示器
        updateSignalIndicator(wifiSignal)
    }

    private fun updateSignalIndicator(signalStrength: Int) {
        val signalLevel = when {
            signalStrength >= -30 -> 4 // 极好
            signalStrength >= -50 -> 3 // 良好
            signalStrength >= -70 -> 2 // 一般
            else -> 1 // 较差
        }

        val signalText = when (signalLevel) {
            4 -> "📶📶📶📶"
            3 -> "📶📶📶"
            2 -> "📶📶"
            else -> "📶"
        }

        binding.tvSignalIndicator.text = signalText
    }

    private fun optimizeWifiSignal() {
        lifecycleScope.launch {
            showOptimizationProgress("正在优化WiFi信号...")
            delay(2000)
            hideOptimizationProgress()
            Toast.makeText(this@ElectromagneticManagerActivity, "WiFi信号优化完成！", Toast.LENGTH_SHORT).show()
            AnimationUtils.successAnimation(binding.btnWifiOptimize)
        }
    }

    private fun controlBluetoothRadiation() {
        lifecycleScope.launch {
            showOptimizationProgress("正在控制蓝牙辐射...")
            delay(1500)
            hideOptimizationProgress()
            Toast.makeText(this@ElectromagneticManagerActivity, "蓝牙辐射控制完成！", Toast.LENGTH_SHORT).show()
            AnimationUtils.successAnimation(binding.btnBluetoothControl)
        }
    }

    private fun optimizeMobileNetwork() {
        lifecycleScope.launch {
            showOptimizationProgress("正在优化移动网络...")
            delay(1800)
            hideOptimizationProgress()
            Toast.makeText(this@ElectromagneticManagerActivity, "移动网络优化完成！", Toast.LENGTH_SHORT).show()
            AnimationUtils.successAnimation(binding.btnMobileNetworkOptimize)
        }
    }

    private fun performRadiationDetection() {
        lifecycleScope.launch {
            showOptimizationProgress("正在检测电磁辐射...")
            delay(2500)
            hideOptimizationProgress()

            val radiationInfo = """
                辐射检测结果：
                • WiFi辐射：25μW/cm² (正常)
                • 蓝牙辐射：8μW/cm² (低辐射)
                • 移动网络：35μW/cm² (可接受)
                • 总辐射水平：68μW/cm² (安全范围)
            """.trimIndent()

            androidx.appcompat.app.AlertDialog.Builder(this@ElectromagneticManagerActivity)
                .setTitle("🔍 辐射检测报告")
                .setMessage(radiationInfo)
                .setPositiveButton("确定", null)
                .show()

            AnimationUtils.successAnimation(binding.btnRadiationDetection)
        }
    }

    private fun showWifiSettings() {
        Toast.makeText(this, "WiFi信号设置", Toast.LENGTH_SHORT).show()
    }

    private fun showBluetoothSettings() {
        Toast.makeText(this, "蓝牙辐射设置", Toast.LENGTH_SHORT).show()
    }

    private fun showMobileNetworkSettings() {
        Toast.makeText(this, "移动网络设置", Toast.LENGTH_SHORT).show()
    }

    private fun showRadiationDetection() {
        performRadiationDetection()
    }

    private fun showAntennaSettings() {
        Toast.makeText(this, "天线管理设置", Toast.LENGTH_SHORT).show()
    }

    private fun showFrequencyBandSettings() {
        Toast.makeText(this, "频段选择设置", Toast.LENGTH_SHORT).show()
    }

    private fun showShieldingSettings() {
        Toast.makeText(this, "辐射屏蔽设置", Toast.LENGTH_SHORT).show()
    }

    private fun showModulationSettings() {
        Toast.makeText(this, "信号调制设置", Toast.LENGTH_SHORT).show()
    }

    private fun showPowerAmplifierSettings() {
        Toast.makeText(this, "功率放大器设置", Toast.LENGTH_SHORT).show()
    }

    private fun showSecuritySettings() {
        Toast.makeText(this, "电磁安全设置", Toast.LENGTH_SHORT).show()
    }

    private fun showOptimizationProgress(message: String) {
        binding.tvOptimizationStatus.text = message
        binding.tvOptimizationStatus.visibility = View.VISIBLE
        binding.progressOptimization.visibility = View.VISIBLE
    }

    private fun hideOptimizationProgress() {
        binding.tvOptimizationStatus.visibility = View.GONE
        binding.progressOptimization.visibility = View.GONE
    }
}
