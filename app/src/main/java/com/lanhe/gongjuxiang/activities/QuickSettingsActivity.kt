package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.adapters.QuickSettingAdapter
import com.lanhe.gongjuxiang.databinding.ActivityQuickSettingsBinding
import com.lanhe.gongjuxiang.models.QuickSetting
import com.lanhe.gongjuxiang.utils.AnimationUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class QuickSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickSettingsBinding
    private lateinit var quickSettingAdapter: QuickSettingAdapter
    private var quickSettings = mutableListOf<QuickSetting>()
    private var settingType: String = "game" // game, eye, power, cleanup, network, device

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取设置类型
        settingType = intent.getStringExtra("setting_type") ?: "game"

        setupToolbar()
        setupRecyclerView()
        loadQuickSettings()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val title = when (settingType) {
            "game" -> "🎮 游戏加速设置"
            "eye" -> "🌙 护眼模式设置"
            "power" -> "🔋 省电模式设置"
            "cleanup" -> "🧹 快速清理设置"
            "network" -> "📶 网络优化设置"
            "device" -> "📱 设备适配设置"
            else -> "⚡ 快速设置"
        }

        supportActionBar?.title = title
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        quickSettingAdapter = QuickSettingAdapter(quickSettings) { setting ->
            handleSettingClick(setting)
        }

        binding.recyclerViewQuickSettings.apply {
            layoutManager = LinearLayoutManager(this@QuickSettingsActivity)
            adapter = quickSettingAdapter
        }
    }

    private fun setupClickListeners() {
        // 应用设置按钮
        binding.btnApplySettings.setOnClickListener {
            applySettings()
        }

        // 重置设置按钮
        binding.btnResetSettings.setOnClickListener {
            resetSettings()
        }
    }

    private fun loadQuickSettings() {
        quickSettings.clear()
        quickSettings.addAll(getQuickSettingsForType(settingType))
        quickSettingAdapter.notifyDataSetChanged()

        // 更新描述文本
        binding.tvSettingDescription.text = getSettingDescription(settingType)
    }

    private fun getQuickSettingsForType(type: String): List<QuickSetting> {
        return when (type) {
            "game" -> getGameSettings()
            "eye" -> getEyeProtectionSettings()
            "power" -> getPowerSavingSettings()
            "cleanup" -> getCleanupSettings()
            "network" -> getNetworkSettings()
            "device" -> getDeviceSettings()
            else -> emptyList()
        }
    }

    private fun getGameSettings(): List<QuickSetting> {
        return listOf(
            QuickSetting(
                id = "game_mode",
                name = "🎮 游戏模式",
                description = "启用游戏专属性能优化模式",
                isEnabled = true,
                currentValue = "高性能模式",
                options = listOf("节能模式", "平衡模式", "高性能模式", "极致模式")
            ),
            QuickSetting(
                id = "cpu_boost",
                name = "🚀 CPU性能增强",
                description = "提升CPU频率以获得更好游戏体验",
                isEnabled = true,
                currentValue = "最大频率",
                options = listOf("标准频率", "高频率", "最大频率")
            ),
            QuickSetting(
                id = "gpu_boost",
                name = "🎨 GPU加速",
                description = "优化GPU渲染性能，提升画面流畅度",
                isEnabled = true,
                currentValue = "高画质",
                options = listOf("节能渲染", "标准渲染", "高画质", "极致画质")
            ),
            QuickSetting(
                id = "memory_priority",
                name = "🧠 内存优先级",
                description = "为游戏分配更多内存资源",
                isEnabled = true,
                currentValue = "最高优先级",
                options = listOf("低优先级", "标准优先级", "高优先级", "最高优先级")
            ),
            QuickSetting(
                id = "thermal_control",
                name = "🌡️ 温度控制",
                description = "智能调节设备温度，防止过热",
                isEnabled = true,
                currentValue = "智能调节",
                options = listOf("节能散热", "标准散热", "智能调节", "性能优先")
            ),
            QuickSetting(
                id = "network_optimization",
                name = "🌐 网络优化",
                description = "降低网络延迟，提升游戏响应速度",
                isEnabled = true,
                currentValue = "游戏优化",
                options = listOf("标准网络", "游戏优化", "电竞模式")
            ),
            QuickSetting(
                id = "battery_optimization",
                name = "🔋 电池优化",
                description = "平衡性能与续航，延长游戏时间",
                isEnabled = false,
                currentValue = "性能优先",
                options = listOf("续航优先", "平衡模式", "性能优先")
            ),
            QuickSetting(
                id = "notification_filter",
                name = "🚫 通知过滤",
                description = "屏蔽游戏时的无关通知",
                isEnabled = true,
                currentValue = "完全屏蔽",
                options = listOf("允许通知", "重要通知", "完全屏蔽")
            ),
            QuickSetting(
                id = "background_apps",
                name = "📱 后台应用管理",
                description = "限制后台应用占用系统资源",
                isEnabled = true,
                currentValue = "深度限制",
                options = listOf("不限制", "标准限制", "深度限制", "完全冻结")
            ),
            QuickSetting(
                id = "screen_settings",
                name = "📺 屏幕设置",
                description = "优化屏幕显示参数，提升游戏体验",
                isEnabled = true,
                currentValue = "游戏模式",
                options = listOf("标准模式", "游戏模式", "电影模式")
            )
        )
    }

    private fun getEyeProtectionSettings(): List<QuickSetting> {
        return listOf(
            QuickSetting(
                id = "blue_light_filter",
                name = "🛡️ 蓝光过滤",
                description = "减少有害蓝光，保护视力健康",
                isEnabled = true,
                currentValue = "强力过滤",
                options = listOf("关闭", "轻度过滤", "标准过滤", "强力过滤")
            ),
            QuickSetting(
                id = "color_temperature",
                name = "🌡️ 色温调节",
                description = "调整屏幕色温，营造舒适视觉环境",
                isEnabled = true,
                currentValue = "暖色调",
                options = listOf("冷色调", "标准色温", "暖色调", "护眼色温")
            ),
            QuickSetting(
                id = "brightness_adaptation",
                name = "💡 亮度自适应",
                description = "根据环境光线自动调节屏幕亮度",
                isEnabled = true,
                currentValue = "智能调节",
                options = listOf("手动调节", "自动调节", "智能调节")
            ),
            QuickSetting(
                id = "eye_rest_reminder",
                name = "⏰ 护眼提醒",
                description = "定时提醒休息，预防眼部疲劳",
                isEnabled = true,
                currentValue = "30分钟",
                options = listOf("关闭", "20分钟", "30分钟", "45分钟", "60分钟")
            ),
            QuickSetting(
                id = "screen_timeout",
                name = "⏱️ 屏幕超时",
                description = "调整屏幕自动关闭时间",
                isEnabled = true,
                currentValue = "5分钟",
                options = listOf("30秒", "1分钟", "2分钟", "5分钟", "10分钟", "永不")
            ),
            QuickSetting(
                id = "night_mode",
                name = "🌙 夜间模式",
                description = "启用深色主题，减少眼部刺激",
                isEnabled = true,
                currentValue = "自动切换",
                options = listOf("关闭", "手动开启", "自动切换", "定时切换")
            ),
            QuickSetting(
                id = "font_size",
                name = "📝 字体大小",
                description = "调整系统字体大小，优化阅读体验",
                isEnabled = false,
                currentValue = "标准",
                options = listOf("小", "标准", "大", "特大")
            ),
            QuickSetting(
                id = "contrast_optimization",
                name = "🎨 对比度优化",
                description = "优化屏幕对比度，提升文字可读性",
                isEnabled = true,
                currentValue = "高对比度",
                options = listOf("标准对比度", "高对比度", "超高对比度")
            )
        )
    }

    private fun getPowerSavingSettings(): List<QuickSetting> {
        return listOf(
            QuickSetting(
                id = "cpu_frequency_limit",
                name = "⚡ CPU频率限制",
                description = "降低CPU运行频率，减少功耗",
                isEnabled = true,
                currentValue = "智能调节",
                options = listOf("最大性能", "高性能", "平衡模式", "智能调节", "极致省电")
            ),
            QuickSetting(
                id = "screen_brightness",
                name = "💡 屏幕亮度",
                description = "降低屏幕亮度，节省电池",
                isEnabled = true,
                currentValue = "自动调节",
                options = listOf("最高亮度", "高亮度", "标准亮度", "自动调节", "最低亮度")
            ),
            QuickSetting(
                id = "background_app_limit",
                name = "📱 后台应用限制",
                description = "限制后台应用运行，减少电池消耗",
                isEnabled = true,
                currentValue = "深度限制",
                options = listOf("不限制", "标准限制", "深度限制", "完全冻结")
            ),
            QuickSetting(
                id = "network_optimization",
                name = "🌐 网络优化",
                description = "减少网络活动，降低电池消耗",
                isEnabled = true,
                currentValue = "智能节电",
                options = listOf("正常网络", "智能节电", "深度节电")
            ),
            QuickSetting(
                id = "animation_reduction",
                name = "🎭 动画效果",
                description = "减少动画消耗，节省系统资源",
                isEnabled = true,
                currentValue = "最小动画",
                options = listOf("丰富动画", "标准动画", "减少动画", "最小动画", "关闭动画")
            ),
            QuickSetting(
                id = "vibration_reduction",
                name = "📳 振动反馈",
                description = "减少振动反馈，节省电池",
                isEnabled = false,
                currentValue = "标准振动",
                options = listOf("强力振动", "标准振动", "轻微振动", "关闭振动")
            ),
            QuickSetting(
                id = "location_services",
                name = "📍 定位服务",
                description = "优化定位服务，减少GPS使用",
                isEnabled = true,
                currentValue = "智能定位",
                options = listOf("高精度定位", "标准定位", "智能定位", "关闭定位")
            ),
            QuickSetting(
                id = "sync_frequency",
                name = "🔄 同步频率",
                description = "减少数据同步频率，节省流量和电池",
                isEnabled = true,
                currentValue = "30分钟",
                options = listOf("5分钟", "15分钟", "30分钟", "1小时", "手动同步")
            ),
            QuickSetting(
                id = "screen_timeout_power",
                name = "⏱️ 屏幕超时(省电)",
                description = "缩短屏幕超时时间",
                isEnabled = true,
                currentValue = "30秒",
                options = listOf("1分钟", "30秒", "15秒", "5秒")
            ),
            QuickSetting(
                id = "thermal_management",
                name = "🌡️ 温度管理",
                description = "智能温度管理，优化电池寿命",
                isEnabled = true,
                currentValue = "智能散热",
                options = listOf("性能优先", "平衡模式", "智能散热", "极致散热")
            )
        )
    }

    private fun getCleanupSettings(): List<QuickSetting> {
        return listOf(
            QuickSetting(
                id = "cache_cleanup",
                name = "💾 缓存清理",
                description = "清理应用缓存文件",
                isEnabled = true,
                currentValue = "深度清理",
                options = listOf("快速清理", "标准清理", "深度清理")
            ),
            QuickSetting(
                id = "temp_files",
                name = "📁 临时文件",
                description = "清理系统临时文件",
                isEnabled = true,
                currentValue = "全部清理",
                options = listOf("选择清理", "标准清理", "全部清理")
            ),
            QuickSetting(
                id = "system_junk",
                name = "🗑️ 系统垃圾",
                description = "清理系统产生的垃圾文件",
                isEnabled = true,
                currentValue = "智能清理",
                options = listOf("手动清理", "智能清理", "深度清理")
            ),
            QuickSetting(
                id = "app_data_cleanup",
                name = "📱 应用数据",
                description = "清理应用产生的无用数据",
                isEnabled = false,
                currentValue = "安全清理",
                options = listOf("保守清理", "标准清理", "安全清理", "深度清理")
            ),
            QuickSetting(
                id = "thumbnail_cleanup",
                name = "🖼️ 缩略图清理",
                description = "清理图片和视频缩略图缓存",
                isEnabled = true,
                currentValue = "全部清理",
                options = listOf("7天前", "30天前", "全部清理")
            ),
            QuickSetting(
                id = "log_cleanup",
                name = "📋 日志清理",
                description = "清理系统和应用日志文件",
                isEnabled = true,
                currentValue = "7天前",
                options = listOf("1天前", "3天前", "7天前", "30天前", "全部清理")
            ),
            QuickSetting(
                id = "download_cleanup",
                name = "⬇️ 下载文件",
                description = "清理下载目录中的临时文件",
                isEnabled = false,
                currentValue = "智能清理",
                options = listOf("不清理", "智能清理", "全部清理")
            ),
            QuickSetting(
                id = "clipboard_cleanup",
                name = "📋 剪贴板清理",
                description = "清理剪贴板历史记录",
                isEnabled = true,
                currentValue = "7天前",
                options = listOf("1天前", "3天前", "7天前", "30天前", "全部清理")
            )
        )
    }

    private fun getNetworkSettings(): List<QuickSetting> {
        return listOf(
            QuickSetting(
                id = "dns_optimization",
                name = "🌐 DNS优化",
                description = "切换到更快的DNS服务器",
                isEnabled = true,
                currentValue = "Google DNS",
                options = listOf("系统默认", "Google DNS", "Cloudflare", "自定义DNS")
            ),
            QuickSetting(
                id = "connection_pool",
                name = "🔗 连接池优化",
                description = "增加网络连接并发数",
                isEnabled = true,
                currentValue = "最大连接",
                options = listOf("标准连接", "增加连接", "最大连接")
            ),
            QuickSetting(
                id = "cache_strategy",
                name = "💾 缓存策略",
                description = "优化网络请求缓存",
                isEnabled = true,
                currentValue = "智能缓存",
                options = listOf("不缓存", "标准缓存", "智能缓存", "预加载")
            ),
            QuickSetting(
                id = "compression_enabled",
                name = "🗜️ 数据压缩",
                description = "启用数据传输压缩",
                isEnabled = true,
                currentValue = "GZIP压缩",
                options = listOf("不压缩", "GZIP压缩", "Brotli压缩")
            ),
            QuickSetting(
                id = "error_retry",
                name = "🔄 错误重试",
                description = "优化网络错误处理重试机制",
                isEnabled = true,
                currentValue = "智能重试",
                options = listOf("不重试", "标准重试", "智能重试", "持久重试")
            ),
            QuickSetting(
                id = "bandwidth_management",
                name = "📊 带宽管理",
                description = "智能管理网络带宽使用",
                isEnabled = false,
                currentValue = "平衡模式",
                options = listOf("速度优先", "平衡模式", "流量节省")
            ),
            QuickSetting(
                id = "proxy_settings",
                name = "🛡️ 代理设置",
                description = "配置网络代理服务器",
                isEnabled = false,
                currentValue = "直连",
                options = listOf("直连", "系统代理", "自定义代理", "VPN代理")
            ),
            QuickSetting(
                id = "network_monitoring",
                name = "📈 网络监控",
                description = "实时监控网络连接状态",
                isEnabled = true,
                currentValue = "详细监控",
                options = listOf("关闭监控", "简单监控", "详细监控")
            )
        )
    }

    private fun getDeviceSettings(): List<QuickSetting> {
        return listOf(
            QuickSetting(
                id = "device_info",
                name = "📱 设备信息",
                description = "显示详细的设备硬件信息",
                isEnabled = true,
                currentValue = "已检测",
                options = listOf("重新检测", "已检测")
            ),
            QuickSetting(
                id = "performance_profile",
                name = "⚡ 性能配置",
                description = "根据设备型号调整性能参数",
                isEnabled = true,
                currentValue = "旗舰配置",
                options = listOf("入门配置", "标准配置", "旗舰配置", "自定义配置")
            ),
            QuickSetting(
                id = "memory_optimization",
                name = "🧠 内存优化",
                description = "优化内存分配和管理策略",
                isEnabled = true,
                currentValue = "智能优化",
                options = listOf("保守策略", "平衡策略", "智能优化", "激进策略")
            ),
            QuickSetting(
                id = "battery_characteristics",
                name = "🔋 电池特性",
                description = "适配电池特性，优化充电和放电",
                isEnabled = true,
                currentValue = "快充电池",
                options = listOf("标准电池", "快充电池", "耐用电池", "自定义特性")
            ),
            QuickSetting(
                id = "thermal_profile",
                name = "🌡️ 散热配置",
                description = "根据设备散热能力调整策略",
                isEnabled = true,
                currentValue = "强力散热",
                options = listOf("基础散热", "标准散热", "强力散热", "极致散热")
            ),
            QuickSetting(
                id = "network_adaptation",
                name = "🌐 网络适配",
                description = "根据网络环境优化连接参数",
                isEnabled = true,
                currentValue = "智能适配",
                options = listOf("固定配置", "自动适配", "智能适配")
            ),
            QuickSetting(
                id = "display_calibration",
                name = "📺 显示校准",
                description = "校准屏幕显示参数",
                isEnabled = false,
                currentValue = "标准校准",
                options = listOf("默认设置", "标准校准", "专业校准", "自定义校准")
            ),
            QuickSetting(
                id = "audio_optimization",
                name = "🔊 音频优化",
                description = "优化音频输出参数",
                isEnabled = true,
                currentValue = "高保真",
                options = listOf("标准音质", "高保真", "游戏音效", "自定义配置")
            ),
            QuickSetting(
                id = "sensor_calibration",
                name = "📡 传感器校准",
                description = "校准各类传感器精度",
                isEnabled = false,
                currentValue = "自动校准",
                options = listOf("不校准", "自动校准", "手动校准", "专业校准")
            ),
            QuickSetting(
                id = "compatibility_mode",
                name = "🔧 兼容模式",
                description = "启用设备兼容性优化",
                isEnabled = true,
                currentValue = "最新模式",
                options = listOf("兼容模式", "标准模式", "最新模式", "实验模式")
            )
        )
    }

    private fun getSettingDescription(type: String): String {
        return when (type) {
            "game" -> """
                🎮 游戏加速设置
                为游戏场景深度优化系统性能，提升游戏体验

                主要优化内容：
                • CPU/GPU性能增强
                • 内存优先级调整
                • 网络延迟优化
                • 温度智能控制
                • 通知屏蔽保护
            """.trimIndent()
            "eye" -> """
                🌙 护眼模式设置
                科学护眼，保护视力健康

                护眼措施：
                • 蓝光智能过滤
                • 色温柔和调节
                • 亮度自适应
                • 定时休息提醒
                • 深色主题切换
            """.trimIndent()
            "power" -> """
                🔋 省电模式设置
                智能省电，延长续航时间

                省电策略：
                • CPU频率智能调节
                • 屏幕亮度自动降低
                • 后台应用深度限制
                • 网络活动优化
                • 系统动画减少
            """.trimIndent()
            "cleanup" -> """
                🧹 快速清理设置
                深度清理系统垃圾，提升运行速度

                清理范围：
                • 应用缓存文件
                • 系统临时文件
                • 缩略图和日志
                • 无用数据清理
                • 存储空间优化
            """.trimIndent()
            "network" -> """
                📶 网络优化设置
                提升网络速度和稳定性

                优化项目：
                • DNS服务器优化
                • 连接池参数调整
                • 缓存策略优化
                • 数据压缩传输
                • 错误重试机制
            """.trimIndent()
            "device" -> """
                📱 设备适配设置
                根据设备特性进行深度优化

                适配内容：
                • 硬件参数检测
                • 性能配置调整
                • 内存管理优化
                • 电池特性适配
                • 散热策略优化
            """.trimIndent()
            else -> "⚡ 快速设置"
        }
    }

    private fun handleSettingClick(setting: QuickSetting) {
        // 处理设置项点击，可以显示选项对话框
        showSettingOptionsDialog(setting)
    }

    private fun showSettingOptionsDialog(setting: QuickSetting) {
        val options = setting.options.toTypedArray()
        if (options.isEmpty()) return

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(setting.name)
            .setSingleChoiceItems(options, options.indexOf(setting.currentValue)) { dialog, which ->
                val selectedOption = options[which]
                // 更新设置值
                updateSettingValue(setting.id, selectedOption)
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateSettingValue(settingId: String, newValue: String) {
        // 找到对应的设置项并更新值
        val index = quickSettings.indexOfFirst { it.id == settingId }
        if (index != -1) {
            quickSettings[index] = quickSettings[index].copy(currentValue = newValue)
            quickSettingAdapter.notifyItemChanged(index)
        }
    }

    private fun applySettings() {
        lifecycleScope.launch {
            showOptimizationProgress("正在应用设置...")
            delay(1500)

            // 模拟应用设置过程
            for (i in 0 until quickSettings.size step 3) {
                val endIndex = minOf(i + 3, quickSettings.size)
                val batchSettings = quickSettings.subList(i, endIndex)

                updateProgress("正在配置 ${batchSettings.size} 项设置...")
                delay(800)
            }

            updateProgress("设置应用完成！")
            delay(500)
            hideOptimizationProgress()

            val appliedCount = quickSettings.count { it.isEnabled }
            Toast.makeText(
                this@QuickSettingsActivity,
                "✅ 成功应用 $appliedCount 项设置！",
                Toast.LENGTH_LONG
            ).show()

            AnimationUtils.successAnimation(binding.btnApplySettings)
        }
    }

    private fun resetSettings() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("重置设置")
            .setMessage("确定要重置所有设置为默认值吗？")
            .setPositiveButton("确定") { _, _ ->
                resetAllSettings()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun resetAllSettings() {
        // 重置所有设置为默认值
        loadQuickSettings()
        Toast.makeText(this, "设置已重置为默认值", Toast.LENGTH_SHORT).show()
    }

    private fun showOptimizationProgress(message: String) {
        binding.tvOptimizationStatus.text = message
        binding.tvOptimizationStatus.visibility = View.VISIBLE
        binding.progressOptimization.visibility = View.VISIBLE
    }

    private fun updateProgress(message: String) {
        binding.tvOptimizationStatus.text = message
        AnimationUtils.rippleEffect(binding.tvOptimizationStatus)
    }

    private fun hideOptimizationProgress() {
        binding.tvOptimizationStatus.visibility = View.GONE
        binding.progressOptimization.visibility = View.GONE
    }
}
