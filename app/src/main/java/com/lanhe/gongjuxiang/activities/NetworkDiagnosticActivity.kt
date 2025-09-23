package com.lanhe.gongjuxiang.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.WifiSignalAdapter
import com.lanhe.gongjuxiang.adapters.NetworkUsageAdapter
import com.lanhe.gongjuxiang.adapters.BatteryConsumingAdapter
import com.lanhe.gongjuxiang.databinding.ActivityNetworkDiagnosticBinding
import com.lanhe.gongjuxiang.utils.*
import com.lanhe.gongjuxiang.viewmodels.NetworkDiagnosticViewModel
import kotlinx.coroutines.launch

/**
 * 网络诊断Activity - 精简版主控制器
 * 专注于UI交互和业务流程控制
 */
class NetworkDiagnosticActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNetworkDiagnosticBinding
    private val viewModel: NetworkDiagnosticViewModel by viewModels()

    // 工具类
    private lateinit var networkInfoHelper: NetworkInfoHelper
    private lateinit var latencyTester: LatencyTester
    private lateinit var positionScanner: PositionScanner
    private lateinit var networkOptimizer: NetworkOptimizer
    private lateinit var uiController: NetworkDiagnosticUIController

    // 适配器
    private lateinit var wifiSignalAdapter: WifiSignalAdapter
    private lateinit var networkUsageAdapter: NetworkUsageAdapter
    private lateinit var batteryConsumingAdapter: BatteryConsumingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNetworkDiagnosticBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupUI()
        setupObservers()
        checkPermissions()
    }

    private fun initializeComponents() {
        networkInfoHelper = NetworkInfoHelper(this)
        latencyTester = LatencyTester()
        positionScanner = PositionScanner()
        networkOptimizer = NetworkOptimizer()
        uiController = NetworkDiagnosticUIController(binding, this)
    }

    private fun setupUI() {
        uiController.setupViews()
        setupAdapters()
        setupClickListeners()
    }

    private fun setupAdapters() {
        wifiSignalAdapter = WifiSignalAdapter()
        networkUsageAdapter = NetworkUsageAdapter { app ->
            uiController.showNetworkUsageWarning(app)
        }
        batteryConsumingAdapter = BatteryConsumingAdapter { app ->
            uiController.showBatteryConsumptionWarning(app)
        }

        binding.rvWifiSignals.apply {
            layoutManager = LinearLayoutManager(this@NetworkDiagnosticActivity)
            adapter = wifiSignalAdapter
        }

        binding.rvNetworkUsage.apply {
            layoutManager = LinearLayoutManager(this@NetworkDiagnosticActivity)
            adapter = networkUsageAdapter
        }

        binding.rvBatteryConsuming.apply {
            layoutManager = LinearLayoutManager(this@NetworkDiagnosticActivity)
            adapter = batteryConsumingAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnStartLatencyTest.setOnClickListener { startLatencyTest() }
        binding.btnScanPositions.setOnClickListener { startPositionScan() }
        binding.btnOptimizeNetwork.setOnClickListener { optimizeNetwork() }
        binding.btnShowTips.setOnClickListener { uiController.showNetworkTips() }
    }

    private fun setupObservers() {
        viewModel.networkInfo.observe(this) { networkInfo ->
            uiController.updateNetworkDisplay(networkInfo)
        }

        viewModel.latencyResult.observe(this) { result ->
            uiController.updateLatencyDisplay(result)
        }

        viewModel.positionScanResult.observe(this) { result ->
            uiController.updatePositionScanDisplay(result)
        }

        viewModel.wifiSignals.observe(this) { wifiSignals ->
            wifiSignalAdapter.updateData(wifiSignals.sortedByDescending { it.rssi })
            uiController.updateWifiSignalsSummary(wifiSignals)
        }

        viewModel.networkUsageApps.observe(this) { networkApps ->
            networkUsageAdapter.updateData(networkApps.sortedByDescending { it.usageMB })
            uiController.updateNetworkUsageSummary(networkApps)
            uiController.checkForNetworkWarnings(networkApps)
        }

        viewModel.batteryConsumingApps.observe(this) { batteryApps ->
            batteryConsumingAdapter.updateData(batteryApps.sortedByDescending { it.consumptionPercent })
            uiController.updateBatteryConsumingSummary(batteryApps)
            uiController.checkForBatteryWarnings(batteryApps)
        }

        viewModel.diagnosticStatus.observe(this) { status ->
            binding.tvDiagnosticStatus.text = status
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1001)
        } else {
            initializeNetworkInfo()
        }
    }

    private fun initializeNetworkInfo() {
        lifecycleScope.launch {
            try {
                val networkInfo = networkInfoHelper.getCurrentNetworkInfo()
                // 转换类型为ViewModel期望的格式
                val vmNetworkInfo = com.lanhe.gongjuxiang.viewmodels.NetworkDiagnosticViewModel.NetworkInfo(
                    type = networkInfo.type,
                    ssid = networkInfo.ssid,
                    bssid = networkInfo.bssid,
                    signalStrength = networkInfo.signalStrength,
                    rssi = networkInfo.rssi,
                    estimatedDistance = networkInfo.estimatedDistance,
                    linkSpeed = networkInfo.linkSpeed,
                    frequency = networkInfo.frequency,
                    isConnected = networkInfo.isConnected
                )
                viewModel.updateNetworkInfo(vmNetworkInfo)
            } catch (e: Exception) {
                Toast.makeText(this@NetworkDiagnosticActivity, "获取网络信息失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startLatencyTest() {
        lifecycleScope.launch {
            try {
                uiController.setTestingStatus(true)
                val result = latencyTester.performLatencyTest()
                // 转换类型为ViewModel期望的格式
                val vmLatencyResult = com.lanhe.gongjuxiang.viewmodels.NetworkDiagnosticViewModel.LatencyResult(
                    averageLatency = result.averageLatency,
                    minLatency = result.minLatency,
                    maxLatency = result.maxLatency,
                    packetLoss = result.packetLoss,
                    quality = result.quality
                )
                viewModel.updateLatencyResult(vmLatencyResult)

                uiController.setTestingStatus(false)
                Toast.makeText(
                    this@NetworkDiagnosticActivity,
                    "延迟测试完成：${result.averageLatency}ms",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                uiController.setTestingStatus(false)
                Toast.makeText(this@NetworkDiagnosticActivity, "延迟测试失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPositionScan() {
        lifecycleScope.launch {
            try {
                uiController.setScanningStatus(true)
                val result = positionScanner.performPositionScan()
                // 转换类型为ViewModel期望的格式
                val vmPositionResult = com.lanhe.gongjuxiang.viewmodels.NetworkDiagnosticViewModel.PositionScanResult(
                    bestPosition = result.bestPosition,
                    recommendedAction = result.recommendedAction,
                    positions = emptyList() // 暂时使用空列表
                )
                viewModel.updatePositionScanResult(vmPositionResult)

                uiController.setScanningStatus(false)
                Toast.makeText(
                    this@NetworkDiagnosticActivity,
                    "位置扫描完成，最佳位置：${result.bestPosition}",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                uiController.setScanningStatus(false)
                Toast.makeText(this@NetworkDiagnosticActivity, "位置扫描失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun optimizeNetwork() {
        lifecycleScope.launch {
            try {
                uiController.showOptimizationProgress()
                val result = networkOptimizer.performOptimization()
                uiController.hideOptimizationProgress()
                uiController.showOptimizationResult(result)

            } catch (e: Exception) {
                uiController.hideOptimizationProgress()
                Toast.makeText(this@NetworkDiagnosticActivity, "网络优化失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeNetworkInfo()
            } else {
                Toast.makeText(this, "需要网络权限才能进行诊断", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
