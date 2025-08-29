package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.AppAdapter
import com.lanhe.gongjuxiang.databinding.ActivityAppManagerBinding
import com.lanhe.gongjuxiang.models.AppInfo
import com.lanhe.gongjuxiang.utils.AnimationUtils
import kotlinx.coroutines.*

class AppManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppManagerBinding
    private lateinit var appAdapter: AppAdapter
    private val appList = mutableListOf<AppInfo>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadInstalledApps()
        setupBackPress()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "📱 应用市场"
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupRecyclerView() {
        appAdapter = AppAdapter(
            context = this,
            appList = appList,
            onAppClick = { appInfo -> onAppClick(appInfo) },
            onUninstallClick = { appInfo -> onUninstallClick(appInfo) },
            onInstallClick = { appInfo -> onInstallClick(appInfo) }
        )

        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@AppManagerActivity)
            adapter = appAdapter
        }
    }

    private fun loadInstalledApps() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val packageManager = packageManager
                val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)

                val apps = packages.mapNotNull { packageInfo ->
                    try {
                        val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                        val appName = appInfo.loadLabel(packageManager).toString()
                        val icon = appInfo.loadIcon(packageManager)
                        val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                        // 获取应用大小（简化实现）
                        val size = getAppSize(packageInfo.packageName)

                        AppInfo(
                            packageName = packageInfo.packageName,
                            appName = appName,
                            versionName = packageInfo.versionName ?: "未知",
                            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                packageInfo.longVersionCode
                            } else {
                                packageInfo.versionCode.toLong()
                            },
                            icon = icon,
                            isSystemApp = isSystemApp,
                            size = size,
                            installTime = packageInfo.firstInstallTime,
                            lastUpdateTime = packageInfo.lastUpdateTime
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.appName }

                withContext(Dispatchers.Main) {
                    appList.clear()
                    appList.addAll(apps)
                    appAdapter.notifyDataSetChanged()

                    // 更新应用数量显示
                    supportActionBar?.title = "📱 应用市场 (${appList.size})"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AppManagerActivity, "加载应用列表失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getAppSize(packageName: String): Long {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val sourceDir = packageInfo.applicationInfo?.sourceDir
            sourceDir?.let {
                val file = java.io.File(it)
                if (file.exists()) file.length() else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun onAppClick(appInfo: AppInfo) {
        // 打开应用详情页面
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${appInfo.packageName}")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开应用详情", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onUninstallClick(appInfo: AppInfo) {
        if (appInfo.isSystemApp) {
            Toast.makeText(this, "系统应用无法卸载", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:${appInfo.packageName}")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法卸载应用: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onInstallClick(appInfo: AppInfo) {
        // 这里可以实现应用的重新安装或下载功能
        Toast.makeText(this, "安装功能开发中...", Toast.LENGTH_SHORT).show()

        // 示例：可以通过Intent.ACTION_VIEW打开应用市场
        // val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${appInfo.packageName}"))
        // startActivity(intent)
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
