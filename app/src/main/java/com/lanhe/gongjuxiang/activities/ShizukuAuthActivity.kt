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
import com.lanhe.gongjuxiang.utils.ApkInstaller
import com.lanhe.gongjuxiang.utils.ShizukuManager
import com.lanhe.gongjuxiang.utils.ShizukuState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import rikka.shizuku.Shizuku
import android.util.Log

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

        // 获取详细状态信息用于诊断
        val isServiceRunning = try {
            rikka.shizuku.Shizuku.pingBinder()
        } catch (e: Exception) {
            Log.w("ShizukuAuthActivity", "Shizuku服务检测失败", e)
            false
        }

        Log.d("ShizukuAuthActivity", "Shizuku检查: 已安装=$isShizukuInstalled, 服务运行=$isServiceRunning")

        if (isShizukuInstalled) {
            when {
                !isServiceRunning -> {
                    // Shizuku已安装但服务未运行
                    binding.tvShizukuStatus.text = "⚠️ Shizuku已安装，但服务未运行"
                    binding.tvShizukuStatus.setTextColor(resources.getColor(R.color.warning, null))
                    binding.btnInstallShizuku.text = "🚀 一键启动Shizuku服务"
                    binding.btnInstallShizuku.visibility = View.VISIBLE
                    binding.btnRequestPermission.visibility = View.VISIBLE
                    binding.btnRequestPermission.text = "请先启动服务"
                    binding.btnRequestPermission.isEnabled = false

                    Log.w("ShizukuAuthActivity", "Shizuku已安装但服务未运行")
                }
                else -> {
                    // Shizuku已安装且服务运行
                    binding.tvShizukuStatus.text = "✅ Shizuku已安装且服务运行中"
                    binding.tvShizukuStatus.setTextColor(resources.getColor(R.color.success, null))
                    binding.btnInstallShizuku.visibility = View.GONE
                    binding.btnRequestPermission.visibility = View.VISIBLE
                    binding.btnRequestPermission.text = "🔑 请求授权"
                    binding.btnRequestPermission.isEnabled = true

                    Log.i("ShizukuAuthActivity", "Shizuku已安装且服务运行")
                }
            }

            // 显示已安装版本信息
            displayInstalledVersionInfo()

            // 检查权限状态
            updatePermissionStatus()
        } else {
            binding.tvShizukuStatus.text = "❌ Shizuku未安装"
            binding.tvShizukuStatus.setTextColor(resources.getColor(R.color.error, null))
            binding.btnInstallShizuku.visibility = View.VISIBLE
            binding.btnInstallShizuku.text = "安装Shizuku"
            binding.btnRequestPermission.visibility = View.GONE

            // 显示内置APK版本信息
            displayAssetVersionInfo()

            Log.w("ShizukuAuthActivity", "Shizuku应用未安装")
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

    /**
     * 显示已安装的Shizuku版本信息
     */
    private fun displayInstalledVersionInfo() {
        try {
            val versionInfo = ShizukuManager.getVersionInfo(this)
            val versionText = "📦 已安装版本: ${versionInfo.installed}"
            Log.d("ShizukuAuthActivity", versionText)
            // 可以在日志中或UI上显示
        } catch (e: Exception) {
            Log.e("ShizukuAuthActivity", "显示已安装版本失败", e)
        }
    }

    /**
     * 显示内置Assets中的Shizuku版本信息
     */
    private fun displayAssetVersionInfo() {
        try {
            val versionInfo = ShizukuManager.getVersionInfo(this)
            val versionText = "📱 可安装版本: ${versionInfo.asset}"
            Log.d("ShizukuAuthActivity", versionText)
            // 可以在日志中或UI上显示
        } catch (e: Exception) {
            Log.e("ShizukuAuthActivity", "显示Asset版本失败", e)
        }
    }

    private fun updatePermissionStatus() {
        val statusMessage = ShizukuManager.getShizukuStatusMessage()
        binding.tvPermissionStatus.text = statusMessage

        when (ShizukuManager.shizukuState.value) {
            ShizukuState.Granted -> {
                binding.tvPermissionStatus.setTextColor(resources.getColor(R.color.success, null))
                binding.btnRequestPermission.text = "权限已授予"
                binding.btnRequestPermission.isEnabled = false
                binding.tvAuthResult.visibility = View.VISIBLE
                binding.tvAuthResult.text = "恭喜！您现在可以享受全部高级功能！"

                // 权限授予成功，显示成功动画并返回
                lifecycleScope.launch {
                    showSuccessAnimation()
                    delay(1500)
                    finish() // 返回前一页面
                }
            }
            ShizukuState.Denied -> {
                binding.tvPermissionStatus.setTextColor(resources.getColor(R.color.warning, null))
                binding.btnRequestPermission.text = "请求权限"
                binding.btnRequestPermission.isEnabled = true
                binding.tvAuthResult.visibility = View.GONE
            }
            ShizukuState.Unavailable -> {
                binding.tvPermissionStatus.setTextColor(resources.getColor(R.color.error, null))
                binding.btnRequestPermission.text = "服务不可用"
                binding.btnRequestPermission.isEnabled = false
                binding.tvAuthResult.visibility = View.GONE
            }
            ShizukuState.Checking -> {
                binding.tvPermissionStatus.setTextColor(resources.getColor(R.color.primary, null))
                binding.btnRequestPermission.text = "检查中..."
                binding.btnRequestPermission.isEnabled = false
                binding.tvAuthResult.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        // 安装/启动Shizuku按钮
        binding.btnInstallShizuku.setOnClickListener {
            if (isShizukuInstalled) {
                // 如果已安装但服务未运行，使用内置启动器启动服务
                startShizukuServiceDirectly()
            } else {
                // 如果未安装，进行安装流程
                installShizuku()
            }
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

    /**
     * 直接启动Shizuku服务（使用内置启动器）
     */
    private fun startShizukuServiceDirectly() {
        lifecycleScope.launch {
            try {
                showPermissionProgress("正在启动Shizuku服务...")

                // 使用内置的Shizuku Starter启动服务
                val result = launchShizukuService()

                hidePermissionProgress()

                if (result) {
                    Toast.makeText(
                        this@ShizukuAuthActivity,
                        "✅ Shizuku服务启动成功！可以继续授权",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.i("ShizukuAuthActivity", "Shizuku服务启动成功")

                    // 延迟一下，让用户看到成功提示
                    delay(500)

                    // 重新检查状态，应该会自动更新为"服务运行"
                    checkShizukuStatus()
                } else {
                    Toast.makeText(
                        this@ShizukuAuthActivity,
                        "❌ Shizuku服务启动失败，请尝试其他方法",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.w("ShizukuAuthActivity", "Shizuku服务启动失败")
                }

            } catch (e: Exception) {
                hidePermissionProgress()
                Toast.makeText(
                    this@ShizukuAuthActivity,
                    "启动服务出错: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("ShizukuAuthActivity", "启动服务异常", e)
            }
        }
    }

    /**
     * 使用内置Shizuku启动器启动服务
     * 返回true表示启动成功，false表示启动失败
     */
    private suspend fun launchShizukuService(): Boolean {
        return try {
            // 尝试直接启动Shizuku服务进程
            // 可以使用内置的Starter或者通过无障碍服务启动

            // 方法1：通过IPC启动（如果有Shizuku权限）
            // 方法2：通过无障碍服务启动
            // 方法3：通过ADB启动

            // 首先检查是否可以通过IPC直接启动
            launchShizukuServiceViaStarter()

        } catch (e: Exception) {
            Log.e("ShizukuAuthActivity", "启动服务异常", e)
            false
        }
    }

    /**
     * 通过Shizuku Starter启动服务
     */
    private suspend fun launchShizukuServiceViaStarter(): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                // 使用Shizuku内置的ServiceStarter启动服务
                val intent = Intent()
                intent.setClassName(
                    "moe.shizuku.privileged.api",
                    "moe.shizuku.manager.home.HomeActivity"
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // 发送启动Shizuku服务的命令
                // 实际的启动需要通过Shell命令或者Manager的API来完成
                try {
                    // 尝试通过ContentProvider获取Shizuku Manager的启动命令
                    val shizukuManager = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (shizukuManager != null) {
                        startActivity(shizukuManager)

                        // 给用户2秒时间启动服务
                        delay(2000)

                        // 检查服务是否已启动
                        val serviceRunning = try {
                            rikka.shizuku.Shizuku.pingBinder()
                        } catch (e: Exception) {
                            false
                        }

                        return@withContext serviceRunning
                    }
                } catch (e: Exception) {
                    Log.e("ShizukuAuthActivity", "无法启动Shizuku Manager", e)
                }

                // 备用方案：通过Shell命令启动
                launchShizukuServiceViaShell()

            } catch (e: Exception) {
                Log.e("ShizukuAuthActivity", "Starter启动失败", e)
                false
            }
        }
    }

    /**
     * 通过Shell命令启动Shizuku服务
     */
    private suspend fun launchShizukuServiceViaShell(): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                // 通过su命令或者脚本启动Shizuku服务
                val process = Runtime.getRuntime().exec(
                    arrayOf(
                        "sh",
                        "/data/adb/shizuku/starter.sh"
                    )
                )

                val exitCode = process.waitFor()
                Log.d("ShizukuAuthActivity", "Shell启动结果: $exitCode")

                // 等待服务启动
                delay(2000)

                // 检查服务是否可用
                val serviceRunning = try {
                    rikka.shizuku.Shizuku.pingBinder()
                } catch (e: Exception) {
                    false
                }

                return@withContext serviceRunning

            } catch (e: Exception) {
                Log.e("ShizukuAuthActivity", "Shell启动失败", e)
                false
            }
        }
    }

    private fun observeShizukuState() {
        // 观察Shizuku状态变化
        lifecycleScope.launch {
            ShizukuManager.shizukuState.collectLatest { state ->
                Log.d("ShizukuAuthActivity", "Shizuku状态变化: $state")
                updatePermissionStatus()

                // 如果权限被授予，显示成功提示
                if (state == ShizukuState.Granted) {
                    Toast.makeText(
                        this@ShizukuAuthActivity,
                        "Shizuku权限授权成功！",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun installShizuku() {
        lifecycleScope.launch {
            // 显示下载选项对话框
            showShizukuDownloadOptions()
        }
    }

    private fun showShizukuDownloadOptions() {
        val options = arrayOf(
            "📱 从应用内直接安装（最快）",
            "📦 直接下载最新版本",
            "🌐 在内置浏览器中下载",
            "🔗 在外部浏览器中下载"
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择安装方式")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> installFromAssets()
                    1 -> downloadShizukuDirectly()
                    2 -> openInInternalBrowser()
                    3 -> openInExternalBrowser()
                }
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun installFromAssets() {
        lifecycleScope.launch {
            showInstallationProgress("⚡ 正在从应用内安装Shizuku...")
            delay(500)

            try {
                // ✅ 使用内置APK直接安装 - 快速且无需离开应用
                val success = ApkInstaller.installApkFromAssets(
                    this@ShizukuAuthActivity,
                    "shizuku.apk"
                )

                if (success) {
                    // 安装成功 - 系统安装程序会接管安装流程
                    Toast.makeText(
                        this@ShizukuAuthActivity,
                        "✅ Shizuku安装程序已启动，请按照提示完成安装",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // 安装失败 - 回退到浏览器下载
                    Toast.makeText(
                        this@ShizukuAuthActivity,
                        "❌ 内置APK安装失败，改为使用浏览器下载最新版本",
                        Toast.LENGTH_LONG
                    ).show()

                    // 改为打开浏览器让用户下载最新版本
                    ChromiumBrowserActivity.openUrl(
                        this@ShizukuAuthActivity,
                        "https://github.com/RikkaApps/Shizuku/releases"
                    )
                }

                hideInstallationProgress()
            } catch (e: Exception) {
                hideInstallationProgress()
                Toast.makeText(
                    this@ShizukuAuthActivity,
                    "❌ 安装出错: ${e.message}，请重试或通过浏览器下载",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun downloadShizukuDirectly() {
        lifecycleScope.launch {
            showInstallationProgress("正在准备下载最新版Shizuku...")
            delay(500)

            try {
                // Shizuku最新版本的直接下载链接
                val downloadUrl = "https://github.com/RikkaApps/Shizuku/releases/latest/download/shizuku-v13.5.4.r1038.05cd6fc-release.apk"
                
                // 使用内置浏览器打开下载链接
                val intent = Intent(this@ShizukuAuthActivity, ChromiumBrowserActivity::class.java)
                intent.putExtra("url", downloadUrl)
                startActivity(intent)

                Toast.makeText(
                    this@ShizukuAuthActivity,
                    "正在浏览器中下载Shizuku，下载完成后请安装",
                    Toast.LENGTH_LONG
                ).show()

                hideInstallationProgress()
            } catch (e: Exception) {
                hideInstallationProgress()
                Toast.makeText(this@ShizukuAuthActivity, "无法启动下载: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openInInternalBrowser() {
        lifecycleScope.launch {
            showInstallationProgress("正在打开内置浏览器...")
            delay(500)

            try {
                // 使用内置浏览器打开Shizuku发布页面
                val intent = Intent(this@ShizukuAuthActivity, ChromiumBrowserActivity::class.java)
                intent.putExtra("url", "https://github.com/RikkaApps/Shizuku/releases")
                startActivity(intent)

                Toast.makeText(
                    this@ShizukuAuthActivity,
                    "请在发布页面下载最新版本的Shizuku APK",
                    Toast.LENGTH_LONG
                ).show()

                hideInstallationProgress()
            } catch (e: Exception) {
                hideInstallationProgress()
                Toast.makeText(this@ShizukuAuthActivity, "无法打开内置浏览器", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openInExternalBrowser() {
        lifecycleScope.launch {
            showInstallationProgress("正在跳转到Shizuku下载页面...")
            delay(500)

            try {
                // 使用内置浏览器打开Shizuku下载页面
                ChromiumBrowserActivity.openUrl(
                    this@ShizukuAuthActivity,
                    "https://github.com/RikkaApps/Shizuku/releases"
                )

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

                // 诊断Shizuku服务状态
                val isServiceRunning = try {
                    Shizuku.pingBinder()
                } catch (e: Exception) {
                    Log.e("ShizukuAuthActivity", "Shizuku服务检测异常", e)
                    false
                }

                if (!isServiceRunning) {
                    hidePermissionProgress()
                    Log.w("ShizukuAuthActivity", "Shizuku服务未运行，显示诊断对话框")
                    showServiceNotRunningDialog()
                    return@launch
                }

                // 检查权限状态
                val hasPermission = try {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) {
                    Log.e("ShizukuAuthActivity", "权限检测异常", e)
                    false
                }

                if (hasPermission) {
                    hidePermissionProgress()
                    Toast.makeText(this@ShizukuAuthActivity, "✅ 权限已授予", Toast.LENGTH_SHORT).show()
                    Log.i("ShizukuAuthActivity", "权限已存在")
                    return@launch
                }

                Log.d("ShizukuAuthActivity", "服务可用，发送权限请求")
                // 请求权限（结果会通过StateFlow自动更新UI）
                ShizukuManager.requestPermission(this@ShizukuAuthActivity)

                // 给用户时间查看权限对话框
                delay(1500)
                hidePermissionProgress()

            } catch (e: Exception) {
                hidePermissionProgress()
                Log.e("ShizukuAuthActivity", "权限请求异常", e)
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
                ChromiumBrowserActivity.openUrl(
                    this@ShizukuAuthActivity,
                    "https://shizuku.rikka.app/"
                )
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
                检测到Shizuku已安装但服务未启动。

                📋 解决步骤：

                1️⃣ 点击下方"打开Shizuku"按钮
                2️⃣ 在Shizuku应用中点击"启动"按钮
                3️⃣ 等待提示"服务已启动"
                4️⃣ 返回本应用继续授权

                💡 如果仍未生效，请尝试：
                • 通过无障碍服务启动（在Shizuku中设置）
                • 通过ADB命令启动（开发者选项）
                • 卸载重装Shizuku应用

                ℹ️ Shizuku官网：https://shizuku.rikka.app/
            """.trimIndent())
            .setPositiveButton("✅ 打开Shizuku") { _, _ ->
                openShizukuApp()
            }
            .setNegativeButton("❌ 取消", null)
            .setCancelable(false)
            .show()
    }

    private fun showPermissionFailedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("权限授权失败")
            .setMessage("""
                Shizuku权限授权失败，可能是以下原因：

                • Shizuku服务未正常启动
                • 权限被拒绝
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
        Log.d("ShizukuAuthActivity", "页面恢复，重新检查Shizuku状态")
        checkShizukuStatus()
        logDiagnosticInfo()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 页面销毁时不需要清理ShizukuManager，因为它是单例对象
        Log.d("ShizukuAuthActivity", "页面销毁")
    }

    /**
     * 记录诊断信息到日志，帮助调试
     */
    private fun logDiagnosticInfo() {
        try {
            val isInstalled = isShizukuPackageInstalled()
            val isServiceRunning = try {
                Shizuku.pingBinder()
            } catch (e: Exception) {
                false
            }
            val hasPermission = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }

            val diagnosticInfo = """
                ========== Shizuku诊断信息 ==========
                应用已安装: $isInstalled
                服务运行中: $isServiceRunning
                权限已授予: $hasPermission
                当前状态: ${ShizukuManager.shizukuState.value}
                状态消息: ${ShizukuManager.getShizukuStatusMessage()}
                ===================================
            """.trimIndent()

            Log.i("ShizukuAuthActivity", diagnosticInfo)
        } catch (e: Exception) {
            Log.e("ShizukuAuthActivity", "诊断信息记录异常", e)
        }
    }
    private fun openShizukuApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "无法打开Shizuku应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开Shizuku应用: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
