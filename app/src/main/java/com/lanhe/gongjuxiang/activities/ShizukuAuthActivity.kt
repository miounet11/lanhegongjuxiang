package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityShizukuAuthBinding
import com.lanhe.gongjuxiang.utils.AnimationUtils
import com.lanhe.gongjuxiang.utils.ShizukuManager
import com.lanhe.gongjuxiang.utils.ShizukuState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class ShizukuAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShizukuAuthBinding
    private var isShizukuInstalled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShizukuAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        checkShizukuStatus()
        setupClickListeners()
        observeShizukuState()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "🔑 Shizuku系统授权"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun checkShizukuStatus() {
        // 检查Shizuku是否已安装
        isShizukuInstalled = isShizukuPackageInstalled()

        if (isShizukuInstalled) {
            binding.tvShizukuStatus.text = "✅ Shizuku已安装"
            binding.tvShizukuStatus.setTextColor(resources.getColor(R.color.success, null))
            binding.btnInstallShizuku.visibility = View.GONE
            binding.btnRequestPermission.visibility = View.VISIBLE

            // 检查权限状态
            updatePermissionStatus()
        } else {
            binding.tvShizukuStatus.text = "❌ Shizuku未安装"
            binding.tvShizukuStatus.setTextColor(resources.getColor(R.color.error, null))
            binding.btnInstallShizuku.visibility = View.VISIBLE
            binding.btnRequestPermission.visibility = View.GONE
        }

        // 显示功能说明
        showFeatureDescription()
    }

    private fun isShizukuPackageInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun updatePermissionStatus() {
        val statusMessage = ShizukuManager.getShizukuStatusMessage()
        binding.tvPermissionStatus.text = statusMessage

        when (ShizukuManager.shizukuState.value) {
            ShizukuState.Granted -> {
                binding.tvPermissionStatus.setTextColor(resources.getColor(R.color.success, null))
                binding.btnRequestPermission.text = "✅ 权限已授予"
                binding.btnRequestPermission.isEnabled = false
                binding.tvAuthResult.visibility = View.VISIBLE
                binding.tvAuthResult.text = "🎉 恭喜！您现在可以享受全部高级功能！"
            }
            ShizukuState.Denied -> {
                binding.tvPermissionStatus.setTextColor(resources.getColor(R.color.warning, null))
                binding.btnRequestPermission.text = "🔑 请求权限"
                binding.btnRequestPermission.isEnabled = true
                binding.tvAuthResult.visibility = View.GONE
            }
            ShizukuState.Unavailable -> {
                binding.tvPermissionStatus.setTextColor(resources.getColor(R.color.error, null))
                binding.btnRequestPermission.text = "❌ 服务不可用"
                binding.btnRequestPermission.isEnabled = false
                binding.tvAuthResult.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        // 安装Shizuku按钮
        binding.btnInstallShizuku.setOnClickListener {
            installShizuku()
        }

        // 请求权限按钮
        binding.btnRequestPermission.setOnClickListener {
            requestShizukuPermission()
        }

        // 了解更多按钮
        binding.btnLearnMore.setOnClickListener {
            showShizukuDetails()
        }

        // 跳过按钮
        binding.btnSkip.setOnClickListener {
            Toast.makeText(this, "您可以稍后在高级设置中启用Shizuku", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun observeShizukuState() {
        lifecycleScope.launch {
            ShizukuManager.shizukuState.collectLatest { state ->
                updatePermissionStatus()
            }
        }
    }

    private fun installShizuku() {
        lifecycleScope.launch {
            showInstallationProgress("正在跳转到Shizuku下载页面...")
            delay(1000)

            try {
                // 跳转到Shizuku下载页面
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://github.com/RikkaApps/Shizuku/releases")
                startActivity(intent)

                // 显示提示
                Toast.makeText(
                    this@ShizukuAuthActivity,
                    "请下载并安装Shizuku，然后返回此页面继续授权",
                    Toast.LENGTH_LONG
                ).show()

                hideInstallationProgress()
            } catch (e: Exception) {
                hideInstallationProgress()
                Toast.makeText(this@ShizukuAuthActivity, "无法打开下载页面，请手动搜索Shizuku", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestShizukuPermission() {
        lifecycleScope.launch {
            try {
                showPermissionProgress("正在请求Shizuku权限...")

                // 检查Shizuku服务状态
                if (!Shizuku.pingBinder()) {
                    hidePermissionProgress()
                    showServiceNotRunningDialog()
                    return@launch
                }

                // 请求权限
                ShizukuManager.requestPermission(this@ShizukuAuthActivity)

                // 等待权限结果
                delay(2000)
                updatePermissionStatus()

                if (ShizukuManager.isShizukuAvailable()) {
                    hidePermissionProgress()
                    showSuccessAnimation()
                    Toast.makeText(this@ShizukuAuthActivity, "🎉 Shizuku权限授权成功！", Toast.LENGTH_LONG).show()

                    // 延迟关闭页面，让用户看到成功状态
                    delay(2000)
                    finish()
                } else {
                    hidePermissionProgress()
                    showPermissionFailedDialog()
                }

            } catch (e: Exception) {
                hidePermissionProgress()
                Toast.makeText(this@ShizukuAuthActivity, "权限请求失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showShizukuDetails() {
        val details = """
            🔑 Shizuku系统授权

            📋 什么是Shizuku？
            Shizuku是一个Android系统框架，它允许普通应用获得系统级权限，无需Root。

            🚀 授权后您可以享受：

            ⚡ 系统级性能优化
            • CPU频率深度调节
            • 内存管理高级控制
            • GPU性能动态调整

            🔧 高级系统功能
            • 进程管理与优化
            • 系统设置深度修改
            • 应用权限完全控制

            📊 专业监控面板
            • 实时系统状态监控
            • 网络流量深度分析
            • 电池健康智能检测

            🛡️ 安全与稳定
            • 官方框架，安全可靠
            • 无Root权限，无风险
            • 完全开源，透明可信

            💡 如何使用：
            1. 安装Shizuku应用
            2. 启动Shizuku服务
            3. 返回此页面授权
            4. 享受全部高级功能
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🔑 Shizuku详细说明")
            .setMessage(details)
            .setPositiveButton("我知道了", null)
            .setNeutralButton("查看官网") { _, _ ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://shizuku.rikka.app/")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "无法打开官网", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showFeatureDescription() {
        val features = when {
            !isShizukuInstalled -> """
                🔧 当前状态：Shizuku未安装

                📱 安装Shizuku后，您将获得：

                ⚡ 高级性能控制
                🎯 深度系统优化
                🔍 专业监控工具
                🛡️ 安全权限管理
                🚀 无Root神级体验
            """.trimIndent()
            ShizukuManager.isShizukuAvailable() -> """
                🎉 当前状态：Shizuku已授权

                ✨ 您现在可以使用的功能：

                🔥 完整系统控制权限
                ⚡ CPU/GPU深度调节
                🧠 内存智能管理
                🔋 电池高级优化
                🌐 网络深度配置
                📊 实时系统监控
            """.trimIndent()
            else -> """
                ⚠️ 当前状态：Shizuku需要授权

                🚀 授权后立即解锁：

                💪 超级性能提升
                🎛️ 专业系统调节
                📈 深度性能监控
                🔒 安全权限控制
                🏆 极客级体验
            """.trimIndent()
        }

        binding.tvFeatureDescription.text = features
    }

    private fun showServiceNotRunningDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Shizuku服务未运行")
            .setMessage("""
                Shizuku服务未启动，请按以下步骤操作：

                1. 打开Shizuku应用
                2. 点击"启动"按钮
                3. 等待服务启动完成
                4. 返回此页面重新授权

                或者使用以下方法之一启动：
                • 通过无障碍服务启动
                • 通过ADB命令启动
                • 通过Root权限启动
            """.trimIndent())
            .setPositiveButton("打开Shizuku") { _, _ ->
                try {
                    val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (intent != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "无法打开Shizuku应用", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "无法打开Shizuku应用", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPermissionFailedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("❌ 权限授权失败")
            .setMessage("""
                Shizuku权限授权失败，可能是以下原因：

                • Shizuku服务未正常启动
                • 权限已被拒绝
                • 应用需要重启

                建议步骤：
                1. 重启Shizuku服务
                2. 重新启动本应用
                3. 再次尝试授权
            """.trimIndent())
            .setPositiveButton("重试") { _, _ ->
                requestShizukuPermission()
            }
            .setNegativeButton("稍后", null)
            .show()
    }

    private fun showInstallationProgress(message: String) {
        binding.tvInstallationStatus.text = message
        binding.tvInstallationStatus.visibility = View.VISIBLE
        binding.progressInstallation.visibility = View.VISIBLE
        binding.btnInstallShizuku.isEnabled = false
    }

    private fun hideInstallationProgress() {
        binding.tvInstallationStatus.visibility = View.GONE
        binding.progressInstallation.visibility = View.GONE
        binding.btnInstallShizuku.isEnabled = true
    }

    private fun showPermissionProgress(message: String) {
        binding.tvPermissionProgress.text = message
        binding.tvPermissionProgress.visibility = View.VISIBLE
        binding.progressPermission.visibility = View.VISIBLE
        binding.btnRequestPermission.isEnabled = false
    }

    private fun hidePermissionProgress() {
        binding.tvPermissionProgress.visibility = View.GONE
        binding.progressPermission.visibility = View.GONE
        binding.btnRequestPermission.isEnabled = true
    }

    private fun showSuccessAnimation() {
        AnimationUtils.successAnimation(binding.tvPermissionStatus)
        AnimationUtils.successAnimation(binding.btnRequestPermission)
    }

    override fun onResume() {
        super.onResume()
        // 每次返回页面时重新检查状态
        checkShizukuStatus()
    }

    // 处理Shizuku权限请求结果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == ShizukuManager.SHIZUKU_PERMISSION_REQUEST_CODE) {
            // 检查权限是否已授予
            if (ShizukuManager.isShizukuAvailable()) {
                // 权限已授予，更新UI状态
                updatePermissionStatus()
                showSuccessAnimation()
                Toast.makeText(this, "🎉 Shizuku权限授权成功！", Toast.LENGTH_LONG).show()
            } else {
                // 权限被拒绝
                showPermissionFailedDialog()
            }
        }
    }
}
