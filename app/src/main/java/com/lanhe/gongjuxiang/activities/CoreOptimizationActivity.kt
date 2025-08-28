package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityCoreOptimizationBinding
import com.lanhe.gongjuxiang.services.CoreOptimizationService
import com.lanhe.gongjuxiang.utils.AnimationUtils
import com.lanhe.gongjuxiang.utils.CoreOptimizationManager
import com.lanhe.gongjuxiang.utils.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CoreOptimizationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoreOptimizationBinding
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var coreOptimizationManager: CoreOptimizationManager

    // 倒计时器
    private var fpsTimer: CountDownTimer? = null
    private var latencyTimer: CountDownTimer? = null
    private var downloadTimer: CountDownTimer? = null
    private var networkTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoreOptimizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化管理器
        preferencesManager = PreferencesManager(this)
        coreOptimizationManager = CoreOptimizationManager(this)

        setupToolbar()
        setupSwitches()
        updateUI()
        startBackgroundService()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "⚡ 核心性能优化"
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupSwitches() {
        // FPS提升开关
        binding.switchFpsBoost.setOnCheckedChangeListener { _, isChecked ->
            handleFpsBoostToggle(isChecked)
        }

        // 延迟优化开关
        binding.switchLatencyOptimization.setOnCheckedChangeListener { _, isChecked ->
            handleLatencyOptimizationToggle(isChecked)
        }

        // 下载提速开关
        binding.switchDownloadBoost.setOnCheckedChangeListener { _, isChecked ->
            handleDownloadBoostToggle(isChecked)
        }

        // 弱网络视频优化开关
        binding.switchNetworkVideoBoost.setOnCheckedChangeListener { _, isChecked ->
            handleNetworkVideoBoostToggle(isChecked)
        }

        // 刷新按钮
        binding.btnRefreshStatus.setOnClickListener {
            AnimationUtils.buttonPressFeedback(it)
            updateUI()
            Toast.makeText(this, "状态已刷新", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleFpsBoostToggle(isChecked: Boolean) {
        val remainingCount = preferencesManager.getFpsBoostRemainingCount()
        val isActive = preferencesManager.isFpsBoostActive()

        if (isChecked && !isActive) {
            // 开启FPS提升
            if (remainingCount <= 0) {
                binding.switchFpsBoost.isChecked = false
                showNoUsageLeftDialog("帧率提升")
                return
            }

            // 扣除使用次数
            preferencesManager.decrementFpsBoostCount()
            preferencesManager.setFpsBoostActive(true)
            preferencesManager.setFpsBoostStartTime(System.currentTimeMillis())

            // 启动FPS优化
            coreOptimizationManager.startFpsOptimization()

            // 启动后台服务
            startCoreOptimizationService()

            // 开始倒计时
            startFpsCountdown()

            showActivationSuccess("帧率提升", remainingCount - 1)
            AnimationUtils.successAnimation(binding.cardFpsBoost)

        } else if (!isChecked && isActive) {
            // 关闭FPS提升
            preferencesManager.setFpsBoostActive(false)
            coreOptimizationManager.stopFpsOptimization()
            stopFpsCountdown()
            showDeactivationMessage("帧率提升")
        }

        updateUI()
    }

    private fun handleLatencyOptimizationToggle(isChecked: Boolean) {
        val remainingCount = preferencesManager.getLatencyOptimizationRemainingCount()
        val isActive = preferencesManager.isLatencyOptimizationActive()

        if (isChecked && !isActive) {
            if (remainingCount <= 0) {
                binding.switchLatencyOptimization.isChecked = false
                showNoUsageLeftDialog("延迟优化")
                return
            }

            preferencesManager.decrementLatencyOptimizationCount()
            preferencesManager.setLatencyOptimizationActive(true)
            preferencesManager.setLatencyOptimizationStartTime(System.currentTimeMillis())

            coreOptimizationManager.startLatencyOptimization()
            startCoreOptimizationService()
            startLatencyCountdown()

            showActivationSuccess("延迟优化", remainingCount - 1)
            AnimationUtils.successAnimation(binding.cardLatencyOptimization)

        } else if (!isChecked && isActive) {
            preferencesManager.setLatencyOptimizationActive(false)
            coreOptimizationManager.stopLatencyOptimization()
            stopLatencyCountdown()
            showDeactivationMessage("延迟优化")
        }

        updateUI()
    }

    private fun handleDownloadBoostToggle(isChecked: Boolean) {
        val remainingCount = preferencesManager.getDownloadBoostRemainingCount()
        val isActive = preferencesManager.isDownloadBoostActive()

        if (isChecked && !isActive) {
            if (remainingCount <= 0) {
                binding.switchDownloadBoost.isChecked = false
                showNoUsageLeftDialog("下载提速")
                return
            }

            preferencesManager.decrementDownloadBoostCount()
            preferencesManager.setDownloadBoostActive(true)
            preferencesManager.setDownloadBoostStartTime(System.currentTimeMillis())

            coreOptimizationManager.startDownloadOptimization()
            startCoreOptimizationService()
            startDownloadCountdown()

            showActivationSuccess("下载提速", remainingCount - 1)
            AnimationUtils.successAnimation(binding.cardDownloadBoost)

        } else if (!isChecked && isActive) {
            preferencesManager.setDownloadBoostActive(false)
            coreOptimizationManager.stopDownloadOptimization()
            stopDownloadCountdown()
            showDeactivationMessage("下载提速")
        }

        updateUI()
    }

    private fun handleNetworkVideoBoostToggle(isChecked: Boolean) {
        val remainingCount = preferencesManager.getNetworkVideoBoostRemainingCount()
        val isActive = preferencesManager.isNetworkVideoBoostActive()

        if (isChecked && !isActive) {
            if (remainingCount <= 0) {
                binding.switchNetworkVideoBoost.isChecked = false
                showNoUsageLeftDialog("弱网络视频优化")
                return
            }

            preferencesManager.decrementNetworkVideoBoostCount()
            preferencesManager.setNetworkVideoBoostActive(true)
            preferencesManager.setNetworkVideoBoostStartTime(System.currentTimeMillis())

            coreOptimizationManager.startNetworkVideoOptimization()
            startCoreOptimizationService()
            startNetworkCountdown()

            showActivationSuccess("弱网络视频优化", remainingCount - 1)
            AnimationUtils.successAnimation(binding.cardNetworkVideoBoost)

        } else if (!isChecked && isActive) {
            preferencesManager.setNetworkVideoBoostActive(false)
            coreOptimizationManager.stopNetworkVideoOptimization()
            stopNetworkCountdown()
            showDeactivationMessage("弱网络视频优化")
        }

        updateUI()
    }

    private fun startFpsCountdown() {
        val remainingTime = preferencesManager.getFpsBoostRemainingTime()
        fpsTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateFpsCountdownText(millisUntilFinished)
            }

            override fun onFinish() {
                handleFpsTimeout()
            }
        }.start()
    }

    private fun startLatencyCountdown() {
        val remainingTime = preferencesManager.getLatencyOptimizationRemainingTime()
        latencyTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateLatencyCountdownText(millisUntilFinished)
            }

            override fun onFinish() {
                handleLatencyTimeout()
            }
        }.start()
    }

    private fun startDownloadCountdown() {
        val remainingTime = preferencesManager.getDownloadBoostRemainingTime()
        downloadTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateDownloadCountdownText(millisUntilFinished)
            }

            override fun onFinish() {
                handleDownloadTimeout()
            }
        }.start()
    }

    private fun startNetworkCountdown() {
        val remainingTime = preferencesManager.getNetworkVideoBoostRemainingTime()
        networkTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateNetworkCountdownText(millisUntilFinished)
            }

            override fun onFinish() {
                handleNetworkTimeout()
            }
        }.start()
    }

    private fun stopFpsCountdown() {
        fpsTimer?.cancel()
        fpsTimer = null
    }

    private fun stopLatencyCountdown() {
        latencyTimer?.cancel()
        latencyTimer = null
    }

    private fun stopDownloadCountdown() {
        downloadTimer?.cancel()
        downloadTimer = null
    }

    private fun stopNetworkCountdown() {
        networkTimer?.cancel()
        networkTimer = null
    }

    private fun updateFpsCountdownText(millisUntilFinished: Long) {
        val minutes = millisUntilFinished / 60000
        val seconds = (millisUntilFinished % 60000) / 1000
        binding.tvFpsCountdown.text = String.format("剩余时间: %02d:%02d", minutes, seconds)
    }

    private fun updateLatencyCountdownText(millisUntilFinished: Long) {
        val minutes = millisUntilFinished / 60000
        val seconds = (millisUntilFinished % 60000) / 1000
        binding.tvLatencyCountdown.text = String.format("剩余时间: %02d:%02d", minutes, seconds)
    }

    private fun updateDownloadCountdownText(millisUntilFinished: Long) {
        val minutes = millisUntilFinished / 60000
        val seconds = (millisUntilFinished % 60000) / 1000
        binding.tvDownloadCountdown.text = String.format("剩余时间: %02d:%02d", minutes, seconds)
    }

    private fun updateNetworkCountdownText(millisUntilFinished: Long) {
        val minutes = millisUntilFinished / 60000
        val seconds = (millisUntilFinished % 60000) / 1000
        binding.tvNetworkCountdown.text = String.format("剩余时间: %02d:%02d", minutes, seconds)
    }

    private fun handleFpsTimeout() {
        preferencesManager.setFpsBoostActive(false)
        coreOptimizationManager.stopFpsOptimization()
        binding.switchFpsBoost.isChecked = false
        showTimeoutMessage("帧率提升")
        updateUI()
    }

    private fun handleLatencyTimeout() {
        preferencesManager.setLatencyOptimizationActive(false)
        coreOptimizationManager.stopLatencyOptimization()
        binding.switchLatencyOptimization.isChecked = false
        showTimeoutMessage("延迟优化")
        updateUI()
    }

    private fun handleDownloadTimeout() {
        preferencesManager.setDownloadBoostActive(false)
        coreOptimizationManager.stopDownloadOptimization()
        binding.switchDownloadBoost.isChecked = false
        showTimeoutMessage("下载提速")
        updateUI()
    }

    private fun handleNetworkTimeout() {
        preferencesManager.setNetworkVideoBoostActive(false)
        coreOptimizationManager.stopNetworkVideoOptimization()
        binding.switchNetworkVideoBoost.isChecked = false
        showTimeoutMessage("弱网络视频优化")
        updateUI()
    }

    private fun updateUI() {
        // 更新FPS提升状态
        val fpsRemainingCount = preferencesManager.getFpsBoostRemainingCount()
        val fpsIsActive = preferencesManager.isFpsBoostActive()
        binding.tvFpsCount.text = "今日剩余: $fpsRemainingCount 次"
        binding.switchFpsBoost.isChecked = fpsIsActive
        binding.tvFpsCountdown.visibility = if (fpsIsActive) android.view.View.VISIBLE else android.view.View.GONE

        // 更新延迟优化状态
        val latencyRemainingCount = preferencesManager.getLatencyOptimizationRemainingCount()
        val latencyIsActive = preferencesManager.isLatencyOptimizationActive()
        binding.tvLatencyCount.text = "今日剩余: $latencyRemainingCount 次"
        binding.switchLatencyOptimization.isChecked = latencyIsActive
        binding.tvLatencyCountdown.visibility = if (latencyIsActive) android.view.View.VISIBLE else android.view.View.GONE

        // 更新下载提速状态
        val downloadRemainingCount = preferencesManager.getDownloadBoostRemainingCount()
        val downloadIsActive = preferencesManager.isDownloadBoostActive()
        binding.tvDownloadCount.text = "今日剩余: $downloadRemainingCount 次"
        binding.switchDownloadBoost.isChecked = downloadIsActive
        binding.tvDownloadCountdown.visibility = if (downloadIsActive) android.view.View.VISIBLE else android.view.View.GONE

        // 更新弱网络视频优化状态
        val networkRemainingCount = preferencesManager.getNetworkVideoBoostRemainingCount()
        val networkIsActive = preferencesManager.isNetworkVideoBoostActive()
        binding.tvNetworkCount.text = "今日剩余: $networkRemainingCount 次"
        binding.switchNetworkVideoBoost.isChecked = networkIsActive
        binding.tvNetworkCountdown.visibility = if (networkIsActive) android.view.View.VISIBLE else android.view.View.GONE

        // 更新总体状态
        updateOverallStatus()
    }

    private fun updateOverallStatus() {
        val activeCount = listOf(
            preferencesManager.isFpsBoostActive(),
            preferencesManager.isLatencyOptimizationActive(),
            preferencesManager.isDownloadBoostActive(),
            preferencesManager.isNetworkVideoBoostActive()
        ).count { it }

        binding.tvOverallStatus.text = when (activeCount) {
            0 -> "⚪ 无优化功能运行中"
            1 -> "🟢 1个优化功能运行中"
            2 -> "🟢 2个优化功能运行中"
            3 -> "🟢 3个优化功能运行中"
            4 -> "🟢 4个优化功能运行中"
            else -> "🟢 优化功能运行中"
        }
    }

    private fun showActivationSuccess(featureName: String, remainingCount: Int) {
        val message = "✅ $featureName 已开启！\n剩余使用次数: $remainingCount 次"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showDeactivationMessage(featureName: String) {
        Toast.makeText(this, "❌ $featureName 已关闭", Toast.LENGTH_SHORT).show()
    }

    private fun showTimeoutMessage(featureName: String) {
        Toast.makeText(this, "⏰ $featureName 时间已到，已自动关闭", Toast.LENGTH_SHORT).show()
    }

    private fun showNoUsageLeftDialog(featureName: String) {
        val nextResetTime = getNextResetTime()
        val message = "❌ $featureName 使用次数已用完！\n\n下次重置时间:\n$nextResetTime\n\n每天可使用5次，每次30分钟"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun getNextResetTime(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val sdf = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    private fun startBackgroundService() {
        val intent = Intent(this, CoreOptimizationService::class.java)
        startService(intent)
    }

    private fun startCoreOptimizationService() {
        val intent = Intent(this, CoreOptimizationService::class.java)
        startService(intent)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理所有倒计时器
        stopFpsCountdown()
        stopLatencyCountdown()
        stopDownloadCountdown()
        stopNetworkCountdown()
    }
}
