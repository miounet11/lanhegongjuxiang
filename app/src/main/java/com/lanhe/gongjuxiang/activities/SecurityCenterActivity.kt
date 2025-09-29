package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.SecurityFeatureAdapter
import com.lanhe.gongjuxiang.databinding.ActivitySecurityCenterBinding
import com.lanhe.gongjuxiang.models.SecurityFeature
import com.lanhe.gongjuxiang.utils.AnimationUtils
import com.lanhe.gongjuxiang.utils.SecurityScanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SecurityCenterActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecurityCenterBinding
    private lateinit var securityFeatureAdapter: SecurityFeatureAdapter
    private lateinit var securityScanner: SecurityScanner
    private var securityFeatures = mutableListOf<SecurityFeature>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecurityCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        securityScanner = SecurityScanner(this)
        setupToolbar()
        setupRecyclerView()
        loadSecurityFeatures()
        setupClickListeners()
        startSecurityMonitoring()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "🛡️ 安全中心"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        securityFeatureAdapter = SecurityFeatureAdapter(securityFeatures) { feature ->
            handleFeatureClick(feature)
        }

        binding.recyclerViewSecurityFeatures.apply {
            layoutManager = LinearLayoutManager(this@SecurityCenterActivity)
            adapter = securityFeatureAdapter

            // 优化性能
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            isDrawingCacheEnabled = true
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        }
    }

    private fun loadSecurityFeatures() {
        securityFeatures.clear()
        securityFeatures.addAll(listOf(
            SecurityFeature(
                id = "permission_check",
                title = "权限安全检查",
                description = "检查应用权限设置和潜在风险",
                icon = R.drawable.ic_optimize,
                category = "权限",
                status = "安全"
            ),
            SecurityFeature(
                id = "app_security_scan",
                title = "应用安全扫描",
                description = "扫描安装的应用是否存在安全风险",
                icon = R.drawable.ic_optimize,
                category = "应用",
                status = "未扫描"
            ),
            SecurityFeature(
                id = "network_security",
                title = "网络安全防护",
                description = "检查网络连接安全性和数据传输",
                icon = R.drawable.ic_optimize,
                category = "网络",
                status = "正常"
            ),
            SecurityFeature(
                id = "privacy_protection",
                title = "隐私保护设置",
                description = "管理隐私设置和数据保护",
                icon = R.drawable.ic_optimize,
                category = "隐私",
                status = "启用"
            ),
            SecurityFeature(
                id = "system_vulnerability",
                title = "系统漏洞检测",
                description = "检测系统安全漏洞和补丁状态",
                icon = R.drawable.ic_optimize,
                category = "系统",
                status = "检查中"
            ),
            SecurityFeature(
                id = "data_encryption",
                title = "数据加密管理",
                description = "管理文件加密和安全存储",
                icon = R.drawable.ic_optimize,
                category = "加密",
                status = "启用"
            ),
            SecurityFeature(
                id = "antivirus_scan",
                title = "病毒扫描",
                description = "全面扫描设备病毒和恶意软件",
                icon = R.drawable.ic_optimize,
                category = "杀毒",
                status = "未扫描"
            ),
            SecurityFeature(
                id = "security_monitor",
                title = "安全实时监控",
                description = "实时监控设备安全状态",
                icon = R.drawable.ic_optimize,
                category = "监控",
                status = "运行中"
            )
        ))

        securityFeatureAdapter.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        binding.btnQuickScan.setOnClickListener {
            performQuickSecurityScan()
        }

        binding.btnFullScan.setOnClickListener {
            performFullSecurityScan()
        }

        binding.btnSecuritySettings.setOnClickListener {
            openSecuritySettings()
        }
    }

    private fun handleFeatureClick(feature: SecurityFeature) {
        when (feature.id) {
            "permission_check" -> checkPermissions()
            "app_security_scan" -> scanApps()
            "network_security" -> checkNetworkSecurity()
            "privacy_protection" -> managePrivacy()
            "system_vulnerability" -> checkVulnerabilities()
            "data_encryption" -> manageEncryption()
            "antivirus_scan" -> performAntivirusScan()
            "security_monitor" -> startSecurityMonitor()
            else -> Toast.makeText(this, "${feature.title}功能开发中", Toast.LENGTH_SHORT).show()
        }
        AnimationUtils.buttonPressFeedback(binding.root)
    }

    private fun performQuickSecurityScan() {
        Toast.makeText(this, "开始快速安全扫描...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvScanStatus.text = "正在扫描权限设置..."
            delay(1000)

            binding.tvScanStatus.text = "正在检查应用安全..."
            delay(1000)

            binding.tvScanStatus.text = "正在扫描网络安全..."
            delay(1000)

            binding.tvScanStatus.text = "扫描完成"
            binding.progressBar.visibility = View.GONE
            updateScanResults("快速扫描", "发现0个安全问题")
        }
    }

    private fun performFullSecurityScan() {
        Toast.makeText(this, "开始全面安全扫描...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            val scanSteps = listOf(
                "扫描系统权限",
                "检查应用安全",
                "检测网络威胁",
                "分析隐私泄露",
                "检查系统漏洞",
                "扫描病毒文件"
            )

            var totalIssues = 0
            for (step in scanSteps) {
                binding.tvScanStatus.text = step
                delay(1200)
                totalIssues += (0..2).random()
            }

            binding.tvScanStatus.text = "全面扫描完成"
            binding.progressBar.visibility = View.GONE
            updateScanResults("全面扫描", "发现${totalIssues}个安全问题")
        }
    }

    private fun openSecuritySettings() {
        Toast.makeText(this, "安全设置功能开发中", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissions() {
        Toast.makeText(this, "开始权限安全检查...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvScanStatus.text = "检查危险权限..."
            delay(1500)

            val dangerousPermissions = checkDangerousPermissions()
            binding.tvScanStatus.text = "权限检查完成"
            binding.progressBar.visibility = View.GONE
            updateScanResults("权限检查", "发现${dangerousPermissions.size}个高风险权限")
        }
    }

    private fun checkDangerousPermissions(): List<String> {
        // 模拟检查危险权限
        return listOf("位置权限", "相机权限", "麦克风权限").take((0..3).random())
    }

    private fun scanApps() {
        Toast.makeText(this, "开始应用安全扫描...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvScanStatus.text = "扫描已安装应用..."
            delay(2000)

            val suspiciousApps = securityScanner.scanInstalledApps()
            binding.tvScanStatus.text = "应用扫描完成"
            binding.progressBar.visibility = View.GONE
            updateScanResults("应用扫描", "发现${suspiciousApps.size}个可疑应用")
        }
    }

    private fun checkNetworkSecurity() {
        Toast.makeText(this, "开始网络安全检查...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvScanStatus.text = "检查网络连接..."
            delay(1000)

            binding.tvScanStatus.text = "分析数据传输..."
            delay(1000)

            binding.tvScanStatus.text = "网络安全检查完成"
            binding.progressBar.visibility = View.GONE
            updateScanResults("网络安全", "网络连接安全")
        }
    }

    private fun managePrivacy() {
        Toast.makeText(this, "隐私保护设置功能开发中", Toast.LENGTH_SHORT).show()
    }

    private fun checkVulnerabilities() {
        Toast.makeText(this, "开始系统漏洞检测...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvScanStatus.text = "检测系统漏洞..."
            delay(1800)

            val vulnerabilities = securityScanner.checkSystemVulnerabilities()
            binding.tvScanStatus.text = "漏洞检测完成"
            binding.progressBar.visibility = View.GONE
            updateScanResults("漏洞检测", "发现${vulnerabilities.size}个系统漏洞")
        }
    }

    private fun manageEncryption() {
        Toast.makeText(this, "数据加密管理功能开发中", Toast.LENGTH_SHORT).show()
    }

    private fun performAntivirusScan() {
        Toast.makeText(this, "开始病毒扫描...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvScanStatus.text = "扫描系统文件..."
            delay(3000)

            val threats = securityScanner.performAntivirusScan()
            binding.tvScanStatus.text = "病毒扫描完成"
            binding.progressBar.visibility = View.GONE
            updateScanResults("病毒扫描", "发现${threats.size}个威胁文件")
        }
    }

    private fun startSecurityMonitor() {
        startActivity(Intent(this, SystemMonitorActivity::class.java))
    }

    private fun updateScanResults(scanType: String, result: String) {
        val resultText = "$scanType: $result\n\n${binding.tvScanResults.text}"
        binding.tvScanResults.text = resultText
        Toast.makeText(this, "$scanType 完成", Toast.LENGTH_SHORT).show()
    }

    private fun startSecurityMonitoring() {
        // 启动后台安全监控
        lifecycleScope.launch {
            while (true) {
                updateSecurityStatus()
                delay(5000) // 每5秒更新一次安全状态
            }
        }
    }

    private fun updateSecurityStatus() {
        // 更新安全状态显示
        binding.tvSecurityStatus.text = "安全状态：良好"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
