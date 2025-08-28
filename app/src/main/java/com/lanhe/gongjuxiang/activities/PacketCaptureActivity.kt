package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.PacketAdapter
import com.lanhe.gongjuxiang.databinding.ActivityPacketCaptureBinding
import com.lanhe.gongjuxiang.models.PacketInfo
import com.lanhe.gongjuxiang.utils.AnimationUtils
import com.lanhe.gongjuxiang.utils.NetworkMonitor
import com.lanhe.gongjuxiang.utils.PacketCaptureManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PacketCaptureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPacketCaptureBinding
    private lateinit var packetAdapter: PacketAdapter
    private lateinit var packetCaptureManager: PacketCaptureManager
    private lateinit var networkMonitor: NetworkMonitor

    private var isCapturing = false
    private var totalPackets = 0
    private var totalDataSize = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPacketCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        initializeComponents()
        setupRecyclerView()
        setupClickListeners()
        updateUI()
        startNetworkMonitoring()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "🔍 网络抓包"
            subtitle = "实时监控网络流量"
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun initializeComponents() {
        packetCaptureManager = PacketCaptureManager(this)
        networkMonitor = NetworkMonitor(this)

        packetAdapter = PacketAdapter(mutableListOf()) { packet ->
            showPacketDetails(packet)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerPackets.apply {
            layoutManager = LinearLayoutManager(this@PacketCaptureActivity)
            adapter = packetAdapter
        }
    }

    private fun setupClickListeners() {
        // 开始/停止抓包按钮
        binding.btnStartStopCapture.setOnClickListener {
            toggleCapture()
        }

        // 清除数据按钮
        binding.btnClearData.setOnClickListener {
            clearPacketData()
        }

        // 过滤按钮
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        // 导出数据按钮
        binding.btnExport.setOnClickListener {
            exportPacketData()
        }

        // 刷新按钮
        binding.btnRefresh.setOnClickListener {
            AnimationUtils.buttonPressFeedback(it)
            updateNetworkStats()
            Toast.makeText(this, "网络统计已刷新", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleCapture() {
        if (isCapturing) {
            stopPacketCapture()
        } else {
            startPacketCapture()
        }
    }

    private fun startPacketCapture() {
        try {
            packetCaptureManager.startCapture { packet ->
                runOnUiThread {
                    addPacketToList(packet)
                }
            }

            isCapturing = true
            updateUI()
            Toast.makeText(this, "🔍 开始抓包...", Toast.LENGTH_SHORT).show()

            // 启动实时更新
            startRealtimeUpdate()

        } catch (e: Exception) {
            Toast.makeText(this, "启动抓包失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPacketCapture() {
        packetCaptureManager.stopCapture()
        isCapturing = false
        updateUI()
        Toast.makeText(this, "🛑 停止抓包", Toast.LENGTH_SHORT).show()
    }

    private fun addPacketToList(packet: PacketInfo) {
        packetAdapter.addPacket(packet)
        totalPackets++
        totalDataSize += packet.size
        updatePacketStats()
        binding.recyclerPackets.scrollToPosition(0)
    }

    private fun clearPacketData() {
        packetAdapter.clearPackets()
        totalPackets = 0
        totalDataSize = 0L
        updatePacketStats()
        Toast.makeText(this, "🗑️ 数据已清除", Toast.LENGTH_SHORT).show()
    }

    private fun showFilterDialog() {
        // 创建过滤对话框
        val filterOptions = arrayOf("全部", "HTTP", "HTTPS", "TCP", "UDP", "ICMP")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("过滤数据包")
            .setItems(filterOptions) { _, which ->
                val filter = when (which) {
                    0 -> null
                    1 -> "HTTP"
                    2 -> "HTTPS"
                    3 -> "TCP"
                    4 -> "UDP"
                    5 -> "ICMP"
                    else -> null
                }
                applyPacketFilter(filter)
            }
            .show()
    }

    private fun applyPacketFilter(protocol: String?) {
        packetAdapter.filterByProtocol(protocol)
        val message = if (protocol != null) "已过滤: $protocol" else "显示全部数据包"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun exportPacketData() {
        val packets = packetAdapter.getAllPackets()
        if (packets.isEmpty()) {
            Toast.makeText(this, "没有数据可导出", Toast.LENGTH_SHORT).show()
            return
        }

        // 创建导出数据
        val exportData = buildExportData(packets)

        // 分享数据
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "网络抓包数据")
            putExtra(Intent.EXTRA_TEXT, exportData)
        }

        startActivity(Intent.createChooser(shareIntent, "导出抓包数据"))
        Toast.makeText(this, "📤 数据已准备导出", Toast.LENGTH_SHORT).show()
    }

    private fun buildExportData(packets: List<PacketInfo>): String {
        val sb = StringBuilder()
        sb.append("网络抓包数据导出\n")
        sb.append("导出时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        sb.append("总数据包数: $totalPackets\n")
        sb.append("总数据量: ${formatDataSize(totalDataSize)}\n\n")

        sb.append("数据包详情:\n")
        sb.append("-".repeat(80))
        sb.append("\n")

        packets.forEachIndexed { index, packet ->
            sb.append("${index + 1}. ${packet.timestamp}\n")
            sb.append("   协议: ${packet.protocol}\n")
            sb.append("   源地址: ${packet.sourceAddress}\n")
            sb.append("   目标地址: ${packet.destinationAddress}\n")
            sb.append("   大小: ${formatDataSize(packet.size.toLong())}\n")
            sb.append("   描述: ${packet.description}\n\n")
        }

        return sb.toString()
    }

    private fun formatDataSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun updateUI() {
        binding.btnStartStopCapture.apply {
            text = if (isCapturing) "🛑 停止抓包" else "🔍 开始抓包"
            setBackgroundColor(resources.getColor(
                if (isCapturing) android.R.color.holo_red_dark else android.R.color.holo_green_dark
            ))
        }

        binding.tvCaptureStatus.apply {
            text = if (isCapturing) "🔴 抓包中..." else "⚪ 未抓包"
            setTextColor(resources.getColor(
                if (isCapturing) android.R.color.holo_red_dark else android.R.color.darker_gray
            ))
        }

        updatePacketStats()
    }

    private fun updatePacketStats() {
        binding.tvPacketCount.text = "数据包: $totalPackets"
        binding.tvDataSize.text = "数据量: ${formatDataSize(totalDataSize)}"
    }

    private fun updateNetworkStats() {
        val networkStats = networkMonitor.getNetworkStats()
        binding.tvNetworkType.text = "网络: ${networkStats.type}"
        binding.tvNetworkSpeed.text = "速度: ${networkStats.speed}"
        binding.tvNetworkLatency.text = "延迟: ${networkStats.latency}ms"
    }

    private fun startRealtimeUpdate() {
        lifecycleScope.launch {
            while (isCapturing) {
                updateNetworkStats()
                delay(2000) // 每2秒更新一次
            }
        }
    }

    private fun startNetworkMonitoring() {
        lifecycleScope.launch {
            while (true) {
                updateNetworkStats()
                delay(5000) // 每5秒更新一次
            }
        }
    }

    private fun showPacketDetails(packet: PacketInfo) {
        val details = """
            时间: ${packet.timestamp}
            协议: ${packet.protocol}
            源地址: ${packet.sourceAddress}
            目标地址: ${packet.destinationAddress}
            数据大小: ${formatDataSize(packet.size.toLong())}
            描述: ${packet.description}

            原始数据:
            ${packet.rawData ?: "无原始数据"}
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📦 数据包详情")
            .setMessage(details)
            .setPositiveButton("确定", null)
            .setNeutralButton("复制") { _, _ ->
                copyToClipboard(details)
            }
            .show()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(android.content.ClipboardManager::class.java)
        val clip = android.content.ClipData.newPlainText("packet_details", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(this, "📋 已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_packet_capture, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                showSettingsDialog()
                true
            }
            R.id.action_help -> {
                showHelpDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSettingsDialog() {
        val options = arrayOf("自动滚动到最新", "显示原始数据", "启用高级过滤", "保存到文件")
        val checkedItems = booleanArrayOf(true, false, true, false)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚙️ 抓包设置")
            .setMultiChoiceItems(options, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
                // 处理设置变更
                handleSettingChange(which, isChecked)
            }
            .setPositiveButton("确定", null)
            .show()
    }

    private fun handleSettingChange(settingIndex: Int, enabled: Boolean) {
        when (settingIndex) {
            0 -> packetAdapter.setAutoScroll(enabled)
            1 -> packetAdapter.showRawData(enabled)
            2 -> packetAdapter.enableAdvancedFilter(enabled)
            3 -> packetAdapter.enableFileLogging(enabled)
        }
    }

    private fun showHelpDialog() {
        val helpText = """
            🔍 网络抓包使用指南

            📋 基本功能:
            • 开始/停止抓包: 控制数据包捕获
            • 清除数据: 清空已捕获的数据包
            • 过滤数据: 按协议类型过滤显示
            • 导出数据: 分享或保存抓包结果

            📊 数据说明:
            • 数据包: 已捕获的网络数据包数量
            • 数据量: 总的数据传输量
            • 网络状态: 当前网络连接信息

            ⚠️ 注意事项:
            • 抓包功能仅监控应用内网络请求
            • 不会捕获系统级别的网络数据
            • 请合理使用，避免过度消耗资源

            🆘 故障排除:
            • 如果无法启动，请检查网络权限
            • 数据为空可能是网络问题
            • 如有问题请导出日志反馈
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("📖 帮助说明")
            .setMessage(helpText)
            .setPositiveButton("知道了", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isCapturing) {
            stopPacketCapture()
        }
        networkMonitor.stopMonitoring()
    }
}
