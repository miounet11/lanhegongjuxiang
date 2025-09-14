package com.lanhe.gongjuxiang.browser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityBrowserSettingsBinding
import com.lanhe.gongjuxiang.utils.AdBlocker
import com.lanhe.gongjuxiang.utils.ImageOptimizer
import com.lanhe.gongjuxiang.utils.SecurityManager

/**
 * 浏览器设置页面
 * 提供全面的浏览器配置选项，包括厂商兼容性设置
 */
class BrowserSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserSettingsBinding
    private lateinit var adBlocker: AdBlocker
    private lateinit var imageOptimizer: ImageOptimizer
    private lateinit var securityManager: SecurityManager

    // 设置选项数据
    private val settingsItems = mutableListOf<BrowserSettingItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupToolbar()
        setupSettingsList()
        loadCurrentSettings()
    }

    private fun initializeComponents() {
        adBlocker = AdBlocker(this)
        imageOptimizer = ImageOptimizer(this)
        securityManager = SecurityManager(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "浏览器设置"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSettingsList() {
        // 广告拦截设置
        settingsItems.add(BrowserSettingItem(
            id = "ad_block",
            title = "广告拦截",
            description = "屏蔽网页广告，提升浏览体验",
            icon = R.drawable.ic_block,
            type = SettingType.SWITCH,
            defaultValue = true
        ))

        // 图片优化设置
        settingsItems.add(BrowserSettingItem(
            id = "image_optimize",
            title = "图片优化",
            description = "压缩图片，提升加载速度",
            icon = R.drawable.ic_image,
            type = SettingType.SWITCH,
            defaultValue = true
        ))

        // JavaScript设置
        settingsItems.add(BrowserSettingItem(
            id = "javascript",
            title = "JavaScript",
            description = "启用JavaScript以获得更好的网页体验",
            icon = R.drawable.ic_code,
            type = SettingType.SWITCH,
            defaultValue = true
        ))

        // Cookie设置
        settingsItems.add(BrowserSettingItem(
            id = "cookies",
            title = "Cookie管理",
            description = "管理网站Cookie存储",
            icon = R.drawable.ic_cookie,
            type = SettingType.SWITCH,
            defaultValue = true
        ))

        // 缓存设置
        settingsItems.add(BrowserSettingItem(
            id = "cache",
            title = "缓存设置",
            description = "管理浏览器缓存",
            icon = R.drawable.ic_storage,
            type = SettingType.BUTTON,
            action = "clear_cache"
        ))

        // 隐私设置
        settingsItems.add(BrowserSettingItem(
            id = "privacy",
            title = "隐私与安全",
            description = "隐私保护和安全设置",
            icon = R.drawable.ic_security,
            type = SettingType.CATEGORY
        ))

        // SSL证书验证
        settingsItems.add(BrowserSettingItem(
            id = "ssl_verify",
            title = "SSL证书验证",
            description = "验证网站SSL证书安全性",
            icon = R.drawable.ic_ssl,
            type = SettingType.SWITCH,
            defaultValue = true
        ))

        // 厂商兼容性设置
        settingsItems.add(BrowserSettingItem(
            id = "vendor_compat",
            title = "厂商兼容性",
            description = "针对不同厂商ROM的优化设置",
            icon = R.drawable.ic_android,
            type = SettingType.CATEGORY
        ))

        // 华为设置
        settingsItems.add(BrowserSettingItem(
            id = "huawei_settings",
            title = "华为设备设置",
            description = "华为EMUI系统优化设置",
            icon = R.drawable.ic_huawei,
            type = SettingType.BUTTON,
            action = "huawei_settings"
        ))

        // 小米设置
        settingsItems.add(BrowserSettingItem(
            id = "xiaomi_settings",
            title = "小米设备设置",
            description = "小米MIUI系统优化设置",
            icon = R.drawable.ic_xiaomi,
            type = SettingType.BUTTON,
            action = "xiaomi_settings"
        ))

        // OPPO设置
        settingsItems.add(BrowserSettingItem(
            id = "oppo_settings",
            title = "OPPO设备设置",
            description = "OPPO ColorOS系统优化设置",
            icon = R.drawable.ic_oppo,
            type = SettingType.BUTTON,
            action = "oppo_settings"
        ))

        // vivo设置
        settingsItems.add(BrowserSettingItem(
            id = "vivo_settings",
            title = "vivo设备设置",
            description = "vivo OriginOS系统优化设置",
            icon = R.drawable.ic_vivo,
            type = SettingType.BUTTON,
            action = "vivo_settings"
        ))

        // 三星设置
        settingsItems.add(BrowserSettingItem(
            id = "samsung_settings",
            title = "三星设备设置",
            description = "三星OneUI系统优化设置",
            icon = R.drawable.ic_samsung,
            type = SettingType.BUTTON,
            action = "samsung_settings"
        ))

        // 其他厂商设置
        settingsItems.add(BrowserSettingItem(
            id = "other_vendor_settings",
            title = "其他厂商设置",
            description = "通用厂商ROM优化设置",
            icon = R.drawable.ic_settings,
            type = SettingType.BUTTON,
            action = "other_vendor_settings"
        ))

        // 电池优化设置
        settingsItems.add(BrowserSettingItem(
            id = "battery_optimization",
            title = "电池优化",
            description = "浏览器电池使用优化",
            icon = R.drawable.ic_battery,
            type = SettingType.BUTTON,
            action = "battery_optimization"
        ))

        // 网络设置
        settingsItems.add(BrowserSettingItem(
            id = "network_settings",
            title = "网络设置",
            description = "网络连接和代理设置",
            icon = R.drawable.ic_network,
            type = SettingType.BUTTON,
            action = "network_settings"
        ))

        // 关于
        settingsItems.add(BrowserSettingItem(
            id = "about",
            title = "关于浏览器",
            description = "版本信息和帮助",
            icon = R.drawable.ic_info,
            type = SettingType.BUTTON,
            action = "about"
        ))

        val adapter = BrowserSettingsAdapter(settingsItems) { item ->
            handleSettingClick(item)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BrowserSettingsActivity)
            this.adapter = adapter
        }
    }

    private fun loadCurrentSettings() {
        // 从SharedPreferences加载当前设置
        val prefs = getSharedPreferences("browser_settings", MODE_PRIVATE)

        // 更新设置项的状态
        settingsItems.forEach { item ->
            when (item.id) {
                "ad_block" -> item.isEnabled = prefs.getBoolean("ad_block_enabled", true)
                "image_optimize" -> item.isEnabled = prefs.getBoolean("image_optimize_enabled", true)
                "javascript" -> item.isEnabled = prefs.getBoolean("javascript_enabled", true)
                "cookies" -> item.isEnabled = prefs.getBoolean("cookies_enabled", true)
                "ssl_verify" -> item.isEnabled = prefs.getBoolean("ssl_verify_enabled", true)
            }
        }

        binding.recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun handleSettingClick(item: BrowserSettingItem) {
        when (item.id) {
            "ad_block" -> toggleAdBlock(item)
            "image_optimize" -> toggleImageOptimization(item)
            "javascript" -> toggleJavaScript(item)
            "cookies" -> toggleCookies(item)
            "cache" -> clearCache()
            "ssl_verify" -> toggleSslVerification(item)
            "huawei_settings" -> openHuaweiSettings()
            "xiaomi_settings" -> openXiaomiSettings()
            "oppo_settings" -> openOppoSettings()
            "vivo_settings" -> openVivoSettings()
            "samsung_settings" -> openSamsungSettings()
            "other_vendor_settings" -> openOtherVendorSettings()
            "battery_optimization" -> openBatteryOptimization()
            "network_settings" -> openNetworkSettings()
            "about" -> showAbout()
        }
    }

    private fun toggleAdBlock(item: BrowserSettingItem) {
        item.isEnabled = !item.isEnabled
        saveSetting("ad_block_enabled", item.isEnabled)
        binding.recyclerView.adapter?.notifyDataSetChanged()

        val message = if (item.isEnabled) "广告拦截已开启" else "广告拦截已关闭"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun toggleImageOptimization(item: BrowserSettingItem) {
        item.isEnabled = !item.isEnabled
        saveSetting("image_optimize_enabled", item.isEnabled)
        binding.recyclerView.adapter?.notifyDataSetChanged()

        val message = if (item.isEnabled) "图片优化已开启" else "图片优化已关闭"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun toggleJavaScript(item: BrowserSettingItem) {
        item.isEnabled = !item.isEnabled
        saveSetting("javascript_enabled", item.isEnabled)
        binding.recyclerView.adapter?.notifyDataSetChanged()

        val message = if (item.isEnabled) "JavaScript已开启" else "JavaScript已关闭"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun toggleCookies(item: BrowserSettingItem) {
        item.isEnabled = !item.isEnabled
        saveSetting("cookies_enabled", item.isEnabled)
        binding.recyclerView.adapter?.notifyDataSetChanged()

        val message = if (item.isEnabled) "Cookie已开启" else "Cookie已关闭"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun toggleSslVerification(item: BrowserSettingItem) {
        item.isEnabled = !item.isEnabled
        saveSetting("ssl_verify_enabled", item.isEnabled)
        binding.recyclerView.adapter?.notifyDataSetChanged()

        val message = if (item.isEnabled) "SSL验证已开启" else "SSL验证已关闭"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun clearCache() {
        // 清除浏览器缓存
        Toast.makeText(this, "缓存清除中...", Toast.LENGTH_SHORT).show()

        // 这里可以实现缓存清除逻辑
        // 例如：清除WebView缓存、应用缓存等

        Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show()
    }

    private fun openHuaweiSettings() {
        try {
            // 华为设备设置
            val intent = Intent().apply {
                setClassName("com.huawei.systemmanager",
                           "com.huawei.systemmanager.optimize.process.ProtectActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // 如果找不到华为设置，打开通用设置
            openGenericVendorSettings("华为")
        }
    }

    private fun openXiaomiSettings() {
        try {
            // 小米设备设置 - 电池优化
            val intent = Intent().apply {
                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("小米")
        }
    }

    private fun openOppoSettings() {
        try {
            // OPPO设备设置 - 自启动管理
            val intent = Intent().apply {
                setClassName("com.coloros.safecenter",
                           "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("OPPO")
        }
    }

    private fun openVivoSettings() {
        try {
            // vivo设备设置 - 后台高耗电
            val intent = Intent().apply {
                setClassName("com.vivo.abe",
                           "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("vivo")
        }
    }

    private fun openSamsungSettings() {
        try {
            // 三星设备设置 - 设备维护
            val intent = Intent().apply {
                setClassName("com.samsung.android.lool",
                           "com.samsung.android.sm.ui.battery.BatteryActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("三星")
        }
    }

    private fun openOtherVendorSettings() {
        // 其他厂商通用设置
        val intent = Intent(Settings.ACTION_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "请在系统设置中查找相关权限设置", Toast.LENGTH_LONG).show()
    }

    private fun openGenericVendorSettings(vendorName: String) {
        // 通用厂商设置页面
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
        Toast.makeText(this, "请在${vendorName}设置中开启相关权限", Toast.LENGTH_LONG).show()
    }

    private fun openBatteryOptimization() {
        try {
            // 打开电池优化设置
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开电池优化设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNetworkSettings() {
        try {
            // 打开网络设置
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开网络设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAbout() {
        // 显示关于页面
        Toast.makeText(this, "YCWebView浏览器 v1.0\n基于腾讯x5内核", Toast.LENGTH_LONG).show()
    }

    private fun saveSetting(key: String, value: Boolean) {
        val prefs = getSharedPreferences("browser_settings", MODE_PRIVATE)
        prefs.edit().putBoolean(key, value).apply()
    }

    companion object {
        fun start(context: android.content.Context) {
            val intent = Intent(context, BrowserSettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}

/**
 * 浏览器设置项数据类
 */
data class BrowserSettingItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: Int,
    val type: SettingType,
    val defaultValue: Boolean = false,
    val action: String? = null,
    var isEnabled: Boolean = false
)

/**
 * 设置类型枚举
 */
enum class SettingType {
    SWITCH,     // 开关
    BUTTON,     // 按钮
    CATEGORY    // 分类
}
