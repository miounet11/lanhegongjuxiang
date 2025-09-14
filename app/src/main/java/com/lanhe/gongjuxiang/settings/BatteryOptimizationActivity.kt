package com.lanhe.gongjuxiang.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityBatteryOptimizationBinding

/**
 * 电池优化设置页面
 * 针对国内各大厂商ROM的电池优化设置
 */
class BatteryOptimizationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBatteryOptimizationBinding
    private val optimizationItems = mutableListOf<BatteryOptimizationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatteryOptimizationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupOptimizationList()
        detectDeviceManufacturer()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "电池优化设置"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupOptimizationList() {
        // 通用电池优化设置
        optimizationItems.add(BatteryOptimizationItem(
            id = "ignore_battery_optimization",
            title = "忽略电池优化",
            description = "允许应用在后台运行，提升功能稳定性",
            manufacturer = "通用",
            action = "ignore_battery_optimization"
        ))

        optimizationItems.add(BatteryOptimizationItem(
            id = "background_restrictions",
            title = "后台运行限制",
            description = "移除后台运行限制",
            manufacturer = "通用",
            action = "background_restrictions"
        ))

        // 华为设备优化
        if (isHuaweiDevice()) {
            optimizationItems.add(BatteryOptimizationItem(
                id = "huawei_power_genius",
                title = "华为省电精灵",
                description = "关闭华为省电精灵的后台限制",
                manufacturer = "华为",
                action = "huawei_power_genius"
            ))

            optimizationItems.add(BatteryOptimizationItem(
                id = "huawei_protected_apps",
                title = "华为受保护应用",
                description = "将应用添加到受保护列表",
                manufacturer = "华为",
                action = "huawei_protected_apps"
            ))
        }

        // 小米设备优化
        if (isXiaomiDevice()) {
            optimizationItems.add(BatteryOptimizationItem(
                id = "xiaomi_god_mode",
                title = "小米神隐模式",
                description = "关闭小米神隐模式限制",
                manufacturer = "小米",
                action = "xiaomi_god_mode"
            ))

            optimizationItems.add(BatteryOptimizationItem(
                id = "xiaomi_auto_start",
                title = "小米自启动管理",
                description = "开启应用自启动权限",
                manufacturer = "小米",
                action = "xiaomi_auto_start"
            ))

            optimizationItems.add(BatteryOptimizationItem(
                id = "xiaomi_battery_saver",
                title = "小米电池优化",
                description = "关闭小米电池优化限制",
                manufacturer = "小米",
                action = "xiaomi_battery_saver"
            ))
        }

        // OPPO设备优化
        if (isOppoDevice()) {
            optimizationItems.add(BatteryOptimizationItem(
                id = "oppo_power_manager",
                title = "OPPO省电管理",
                description = "关闭OPPO省电管理限制",
                manufacturer = "OPPO",
                action = "oppo_power_manager"
            ))

            optimizationItems.add(BatteryOptimizationItem(
                id = "oppo_auto_start",
                title = "OPPO自启动管理",
                description = "开启应用自启动权限",
                manufacturer = "OPPO",
                action = "oppo_auto_start"
            ))
        }

        // vivo设备优化
        if (isVivoDevice()) {
            optimizationItems.add(BatteryOptimizationItem(
                id = "vivo_power_manager",
                title = "vivo省电管理",
                description = "关闭vivo省电管理限制",
                manufacturer = "vivo",
                action = "vivo_power_manager"
            ))

            optimizationItems.add(BatteryOptimizationItem(
                id = "vivo_high_power_consumption",
                title = "vivo高耗电应用",
                description = "移除高耗电应用限制",
                manufacturer = "vivo",
                action = "vivo_high_power_consumption"
            ))
        }

        // 三星设备优化
        if (isSamsungDevice()) {
            optimizationItems.add(BatteryOptimizationItem(
                id = "samsung_adaptive_battery",
                title = "三星自适应电池",
                description = "关闭三星自适应电池限制",
                manufacturer = "三星",
                action = "samsung_adaptive_battery"
            ))

            optimizationItems.add(BatteryOptimizationItem(
                id = "samsung_device_care",
                title = "三星设备维护",
                description = "三星设备维护设置",
                manufacturer = "三星",
                action = "samsung_device_care"
            ))
        }

        // 魅族设备优化
        if (isMeizuDevice()) {
            optimizationItems.add(BatteryOptimizationItem(
                id = "meizu_power_manager",
                title = "魅族省电管理",
                description = "关闭魅族省电管理限制",
                manufacturer = "魅族",
                action = "meizu_power_manager"
            ))
        }

        // 其他厂商通用设置
        optimizationItems.add(BatteryOptimizationItem(
            id = "app_details",
            title = "应用详情设置",
            description = "查看应用权限和设置",
            manufacturer = "通用",
            action = "app_details"
        ))

        val adapter = BatteryOptimizationAdapter(optimizationItems) { item ->
            handleOptimizationClick(item)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@BatteryOptimizationActivity)
            this.adapter = adapter
        }
    }

    private fun handleOptimizationClick(item: BatteryOptimizationItem) {
        when (item.action) {
            "ignore_battery_optimization" -> openIgnoreBatteryOptimization()
            "background_restrictions" -> openBackgroundRestrictions()
            "huawei_power_genius" -> openHuaweiPowerGenius()
            "huawei_protected_apps" -> openHuaweiProtectedApps()
            "xiaomi_god_mode" -> openXiaomiGodMode()
            "xiaomi_auto_start" -> openXiaomiAutoStart()
            "xiaomi_battery_saver" -> openXiaomiBatterySaver()
            "oppo_power_manager" -> openOppoPowerManager()
            "oppo_auto_start" -> openOppoAutoStart()
            "vivo_power_manager" -> openVivoPowerManager()
            "vivo_high_power_consumption" -> openVivoHighPowerConsumption()
            "samsung_adaptive_battery" -> openSamsungAdaptiveBattery()
            "samsung_device_care" -> openSamsungDeviceCare()
            "meizu_power_manager" -> openMeizuPowerManager()
            "app_details" -> openAppDetails()
        }
    }

    private fun openIgnoreBatteryOptimization() {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开电池优化设置", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBackgroundRestrictions() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开应用详情", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openHuaweiPowerGenius() {
        try {
            val intent = Intent().apply {
                setClassName("com.huawei.systemmanager",
                           "com.huawei.systemmanager.power.ui.HwPowerManagerActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("华为")
        }
    }

    private fun openHuaweiProtectedApps() {
        try {
            val intent = Intent().apply {
                setClassName("com.huawei.systemmanager",
                           "com.huawei.systemmanager.optimize.process.ProtectActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("华为")
        }
    }

    private fun openXiaomiGodMode() {
        try {
            val intent = Intent().apply {
                setClassName("com.miui.powerkeeper",
                           "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("小米")
        }
    }

    private fun openXiaomiAutoStart() {
        try {
            val intent = Intent().apply {
                setClassName("com.miui.securitycenter",
                           "com.miui.permcenter.autostart.AutoStartManagementActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("小米")
        }
    }

    private fun openXiaomiBatterySaver() {
        try {
            val intent = Intent().apply {
                setClassName("com.miui.powerkeeper",
                           "com.miui.powerkeeper.ui.PowerKeeperSettingsActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("小米")
        }
    }

    private fun openOppoPowerManager() {
        try {
            val intent = Intent().apply {
                setClassName("com.coloros.safecenter",
                           "com.coloros.safecenter.privacy.view.password.PrivacyPasswordSetting")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("OPPO")
        }
    }

    private fun openOppoAutoStart() {
        try {
            val intent = Intent().apply {
                setClassName("com.coloros.safecenter",
                           "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("OPPO")
        }
    }

    private fun openVivoPowerManager() {
        try {
            val intent = Intent().apply {
                setClassName("com.vivo.abe",
                           "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("vivo")
        }
    }

    private fun openVivoHighPowerConsumption() {
        try {
            val intent = Intent().apply {
                setClassName("com.vivo.abe",
                           "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("vivo")
        }
    }

    private fun openSamsungAdaptiveBattery() {
        try {
            val intent = Intent().apply {
                setClassName("com.samsung.android.lool",
                           "com.samsung.android.sm.ui.battery.BatteryActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("三星")
        }
    }

    private fun openSamsungDeviceCare() {
        try {
            val intent = Intent().apply {
                setClassName("com.samsung.android.lool",
                           "com.samsung.android.sm.ui.care.SmartManagerActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("三星")
        }
    }

    private fun openMeizuPowerManager() {
        try {
            val intent = Intent().apply {
                setClassName("com.meizu.safe",
                           "com.meizu.safe.powerui.PowerAppPermissionActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openGenericVendorSettings("魅族")
        }
    }

    private fun openAppDetails() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开应用详情", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGenericVendorSettings(vendorName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
        Toast.makeText(this, "请在${vendorName}设置中查找相关电池优化选项", Toast.LENGTH_LONG).show()
    }

    private fun detectDeviceManufacturer() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        val deviceInfo = when {
            manufacturer.contains("huawei") || brand.contains("huawei") ||
            manufacturer.contains("honor") || brand.contains("honor") -> "华为/荣耀设备"
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
            manufacturer.contains("redmi") || brand.contains("redmi") -> "小米/红米设备"
            manufacturer.contains("oppo") || brand.contains("oppo") ||
            manufacturer.contains("realme") || brand.contains("realme") -> "OPPO/Realme设备"
            manufacturer.contains("vivo") || brand.contains("vivo") -> "vivo设备"
            manufacturer.contains("samsung") || brand.contains("samsung") -> "三星设备"
            manufacturer.contains("meizu") || brand.contains("meizu") -> "魅族设备"
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> "一加设备"
            else -> "其他设备"
        }

        binding.tvDeviceInfo.text = "检测到设备类型：$deviceInfo"
    }

    private fun isHuaweiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("huawei") || brand.contains("huawei") ||
               manufacturer.contains("honor") || brand.contains("honor")
    }

    private fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
               manufacturer.contains("redmi") || brand.contains("redmi")
    }

    private fun isOppoDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("oppo") || brand.contains("oppo") ||
               manufacturer.contains("realme") || brand.contains("realme")
    }

    private fun isVivoDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("vivo") || brand.contains("vivo")
    }

    private fun isSamsungDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("samsung") || brand.contains("samsung")
    }

    private fun isMeizuDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("meizu") || brand.contains("meizu")
    }

    companion object {
        fun start(context: android.content.Context) {
            val intent = Intent(context, BatteryOptimizationActivity::class.java)
            context.startActivity(intent)
        }
    }
}

/**
 * 电池优化项数据类
 */
data class BatteryOptimizationItem(
    val id: String,
    val title: String,
    val description: String,
    val manufacturer: String,
    val action: String
)
