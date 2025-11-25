package com.lanhe.gongjuxiang.activities

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityWifiSettingsBinding
import com.lanhe.gongjuxiang.utils.setupFeatureInfo

/**
 * WiFi管理活动
 *
 * 功能:
 * - WiFi开关管理
 * - WiFi网络扫描
 * - WiFi信号强度显示
 * - 快速跳转系统WiFi设置
 * - WiFi连接状态监控
 */
class WifiSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWifiSettingsBinding
    private var wifiManager: WifiManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWifiSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置标题栏
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "WiFi管理"
        }

        wifiManager = getSystemService(Context.WIFI_SERVICE) as? WifiManager

        setupFeatureInfoCard()
        setupUI()
        updateWifiStatus()
    }

    private fun setupFeatureInfoCard() {
        // 使用findViewById直接获取include的根View
        val featureInfoCard = binding.root.findViewById<View>(R.id.featureInfoCard)
        featureInfoCard?.setupFeatureInfo("wifi_manager")
    }

    private fun setupUI() {
        // WiFi开关
        binding.switchWifi.apply {
            isChecked = wifiManager?.isWifiEnabled ?: false
            setOnCheckedChangeListener { _, isChecked ->
                toggleWifi(isChecked)
            }
        }

        // 打开系统WiFi设置
        binding.btnOpenSystemSettings.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开系统WiFi设置", Toast.LENGTH_SHORT).show()
            }
        }

        // 刷新WiFi状态
        binding.btnRefresh.setOnClickListener {
            updateWifiStatus()
            Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
        }

        // WiFi优化建议
        binding.btnOptimize.setOnClickListener {
            showOptimizationTips()
        }
    }

    /**
     * 切换WiFi开关
     */
    @Suppress("DEPRECATION")
    private fun toggleWifi(enable: Boolean) {
        try {
            // Android 10以上不再允许应用直接开关WiFi
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // 跳转到系统WiFi设置
                MaterialAlertDialogBuilder(this)
                    .setTitle("WiFi设置")
                    .setMessage("Android 10及以上版本需要在系统设置中手动开关WiFi")
                    .setPositiveButton("打开设置") { _, _ ->
                        startActivity(Intent(Settings.Panel.ACTION_WIFI))
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else {
                // Android 9及以下可以直接控制
                wifiManager?.isWifiEnabled = enable
                updateWifiStatus()
                Toast.makeText(
                    this,
                    if (enable) "WiFi已开启" else "WiFi已关闭",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "WiFi操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 更新WiFi状态显示
     */
    private fun updateWifiStatus() {
        val wifiInfo = wifiManager?.connectionInfo
        val isEnabled = wifiManager?.isWifiEnabled ?: false

        binding.apply {
            switchWifi.isChecked = isEnabled

            if (isEnabled && wifiInfo != null) {
                val ssid = wifiInfo.ssid?.replace("\"", "") ?: "未连接"
                val rssi = wifiInfo.rssi
                val signalLevel = getWifiSignalLevel(rssi)

                tvWifiStatus.text = "已连接"
                tvCurrentNetwork.text = "当前网络: $ssid"
                tvSignalStrength.text = "信号强度: ${getSignalDescription(signalLevel)}"
                tvIpAddress.text = "IP地址: ${formatIpAddress(wifiInfo.ipAddress)}"

                // 显示信号强度条
                progressSignalStrength.apply {
                    max = 4
                    progress = signalLevel
                }
            } else {
                tvWifiStatus.text = "未连接"
                tvCurrentNetwork.text = "当前网络: 无"
                tvSignalStrength.text = "信号强度: -"
                tvIpAddress.text = "IP地址: -"
                progressSignalStrength.progress = 0
            }
        }
    }

    /**
     * 获取WiFi信号等级 (0-4)
     */
    private fun getWifiSignalLevel(rssi: Int): Int {
        return when {
            rssi >= -50 -> 4  // 优秀
            rssi >= -60 -> 3  // 良好
            rssi >= -70 -> 2  // 一般
            rssi >= -80 -> 1  // 较弱
            else -> 0         // 很弱
        }
    }

    /**
     * 获取信号强度描述
     */
    private fun getSignalDescription(level: Int): String {
        return when (level) {
            4 -> "优秀 ★★★★★"
            3 -> "良好 ★★★★☆"
            2 -> "一般 ★★★☆☆"
            1 -> "较弱 ★★☆☆☆"
            0 -> "很弱 ★☆☆☆☆"
            else -> "未知"
        }
    }

    /**
     * 格式化IP地址
     */
    private fun formatIpAddress(ip: Int): String {
        return String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    }

    /**
     * 显示优化建议
     */
    private fun showOptimizationTips() {
        val tips = buildString {
            appendLine("🔧 WiFi优化建议：")
            appendLine()

            val wifiInfo = wifiManager?.connectionInfo
            val rssi = wifiInfo?.rssi ?: -100
            val signalLevel = getWifiSignalLevel(rssi)

            when (signalLevel) {
                0, 1 -> {
                    appendLine("⚠️ 当前信号较弱")
                    appendLine("• 靠近路由器可改善信号")
                    appendLine("• 减少障碍物遮挡")
                    appendLine("• 考虑使用5GHz频段")
                }
                2 -> {
                    appendLine("📶 信号一般")
                    appendLine("• 可正常使用")
                    appendLine("• 避免高带宽任务时移动")
                }
                3, 4 -> {
                    appendLine("✅ 信号优秀")
                    appendLine("• 当前连接状态良好")
                    appendLine("• 可进行高带宽任务")
                }
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("WiFi优化建议")
            .setMessage(tips)
            .setPositiveButton("知道了", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        updateWifiStatus()
    }
}
