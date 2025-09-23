package com.lanhe.gongjuxiang.utils

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lanhe.gongjuxiang.R
import kotlinx.coroutines.*

/**
 * 蓝河助手 - 用户引导管理器
 *
 * 功能特性：
 * - 首次启动引导
 * - 功能介绍教程
 * - 新功能展示
 * - 权限申请引导
 * - 个性化设置
 * - 引导完成状态管理
 */
class OnboardingManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "OnboardingManager"

        @Volatile
        private var INSTANCE: OnboardingManager? = null

        fun getInstance(context: Context): OnboardingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnboardingManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // 偏好设置键
        private const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val PREF_ONBOARDING_VERSION = "onboarding_version"
        private const val PREF_FEATURE_TUTORIAL_SHOWN = "feature_tutorial_shown"
        private const val PREF_PERMISSIONS_TUTORIAL_SHOWN = "permissions_tutorial_shown"

        // 当前引导版本
        private const val CURRENT_ONBOARDING_VERSION = 1
    }

    private val analyticsManager = AnalyticsManager.getInstance(context)
    private val preferencesManager = PreferencesManager.getInstance(context)

    /**
     * 引导步骤数据类
     */
    data class OnboardingStep(
        val title: String,
        val description: String,
        val imageRes: Int,
        val actionText: String? = null,
        val action: (() -> Unit)? = null
    )

    /**
     * 引导类型枚举
     */
    enum class OnboardingType {
        FIRST_TIME,         // 首次使用
        NEW_FEATURES,       // 新功能介绍
        PERMISSIONS,        // 权限申请
        SHIZUKU_SETUP,      // Shizuku设置
        OPTIMIZATION_GUIDE  // 优化功能指南
    }

    /**
     * 检查是否需要显示引导
     */
    fun shouldShowOnboarding(): Boolean {
        val completed = preferencesManager.getBoolean(PREF_ONBOARDING_COMPLETED, false)
        val version = preferencesManager.getInt(PREF_ONBOARDING_VERSION, 0)

        return !completed || version < CURRENT_ONBOARDING_VERSION
    }

    /**
     * 检查是否需要显示特定功能教程
     */
    fun shouldShowFeatureTutorial(featureName: String): Boolean {
        val key = "${PREF_FEATURE_TUTORIAL_SHOWN}_$featureName"
        return !preferencesManager.getBoolean(key, false)
    }

    /**
     * 开始引导流程
     */
    fun startOnboarding(activity: FragmentActivity, type: OnboardingType = OnboardingType.FIRST_TIME) {
        val steps = getOnboardingSteps(type)
        showOnboardingDialog(activity, steps, type)

        analyticsManager.trackEvent("onboarding_started", android.os.Bundle().apply {
            putString("onboarding_type", type.name)
            putInt("steps_count", steps.size)
        })
    }

    /**
     * 获取引导步骤
     */
    private fun getOnboardingSteps(type: OnboardingType): List<OnboardingStep> {
        return when (type) {
            OnboardingType.FIRST_TIME -> getFirstTimeSteps()
            OnboardingType.NEW_FEATURES -> getNewFeatureSteps()
            OnboardingType.PERMISSIONS -> getPermissionSteps()
            OnboardingType.SHIZUKU_SETUP -> getShizukuSetupSteps()
            OnboardingType.OPTIMIZATION_GUIDE -> getOptimizationGuideSteps()
        }
    }

    /**
     * 首次使用引导步骤
     */
    private fun getFirstTimeSteps(): List<OnboardingStep> {
        return listOf(
            OnboardingStep(
                title = "欢迎使用蓝河助手",
                description = "专业的Android系统优化工具，让您的设备运行更流畅",
                imageRes = R.drawable.onboarding_welcome
            ),
            OnboardingStep(
                title = "系统优化",
                description = "一键优化系统性能，包括内存清理、CPU优化、电池管理等功能",
                imageRes = R.drawable.onboarding_optimization
            ),
            OnboardingStep(
                title = "性能监控",
                description = "实时监控设备性能状态，及时发现并解决性能问题",
                imageRes = R.drawable.onboarding_monitoring
            ),
            OnboardingStep(
                title = "高级功能",
                description = "配合Shizuku使用，解锁更多高级系统管理功能",
                imageRes = R.drawable.onboarding_advanced,
                actionText = "了解Shizuku",
                action = { showShizukuInfo() }
            ),
            OnboardingStep(
                title = "开始使用",
                description = "现在您可以开始享受蓝河助手带来的优质体验了",
                imageRes = R.drawable.onboarding_start
            )
        )
    }

    /**
     * 新功能引导步骤
     */
    private fun getNewFeatureSteps(): List<OnboardingStep> {
        return listOf(
            OnboardingStep(
                title = "新功能介绍",
                description = "蓝河助手新增了以下功能，让您的使用体验更加完善",
                imageRes = R.drawable.onboarding_new_features
            ),
            OnboardingStep(
                title = "悬浮气泡",
                description = "全局悬浮气泡助手，随时随地快速执行优化操作",
                imageRes = R.drawable.onboarding_floating_bubble
            ),
            OnboardingStep(
                title = "桌面小部件",
                description = "添加桌面小部件，实时查看系统状态和一键优化",
                imageRes = R.drawable.onboarding_widget
            ),
            OnboardingStep(
                title = "生物识别",
                description = "新增指纹和面部识别功能，保护您的隐私数据",
                imageRes = R.drawable.onboarding_biometric
            )
        )
    }

    /**
     * 权限申请引导步骤
     */
    private fun getPermissionSteps(): List<OnboardingStep> {
        return listOf(
            OnboardingStep(
                title = "权限说明",
                description = "蓝河助手需要一些权限来提供最佳的优化体验",
                imageRes = R.drawable.onboarding_permissions
            ),
            OnboardingStep(
                title = "基础权限",
                description = "访问设备状态、网络信息等基础权限，用于系统监控",
                imageRes = R.drawable.onboarding_basic_permissions,
                actionText = "授予权限",
                action = { requestBasicPermissions() }
            ),
            OnboardingStep(
                title = "悬浮窗权限",
                description = "显示悬浮窗权限，用于全局悬浮气泡功能",
                imageRes = R.drawable.onboarding_overlay_permission,
                actionText = "授予权限",
                action = { requestOverlayPermission() }
            ),
            OnboardingStep(
                title = "Shizuku权限",
                description = "配合Shizuku使用，获得更强大的系统管理能力",
                imageRes = R.drawable.onboarding_shizuku_permission,
                actionText = "了解更多",
                action = { showShizukuGuide() }
            )
        )
    }

    /**
     * Shizuku设置引导步骤
     */
    private fun getShizukuSetupSteps(): List<OnboardingStep> {
        return listOf(
            OnboardingStep(
                title = "什么是Shizuku",
                description = "Shizuku是一个系统API调用框架，可以让应用获得更高级的系统权限",
                imageRes = R.drawable.onboarding_shizuku_intro
            ),
            OnboardingStep(
                title = "安装Shizuku",
                description = "从Google Play或GitHub下载并安装Shizuku应用",
                imageRes = R.drawable.onboarding_shizuku_install,
                actionText = "去下载",
                action = { openShizukuDownload() }
            ),
            OnboardingStep(
                title = "启动Shizuku",
                description = "通过ADB命令或root权限启动Shizuku服务",
                imageRes = R.drawable.onboarding_shizuku_start,
                actionText = "查看教程",
                action = { showShizukuStartGuide() }
            ),
            OnboardingStep(
                title = "授权蓝河助手",
                description = "在Shizuku中为蓝河助手授权，解锁高级功能",
                imageRes = R.drawable.onboarding_shizuku_auth
            )
        )
    }

    /**
     * 优化功能指南步骤
     */
    private fun getOptimizationGuideSteps(): List<OnboardingStep> {
        return listOf(
            OnboardingStep(
                title = "智能优化",
                description = "AI智能分析设备状态，提供个性化优化建议",
                imageRes = R.drawable.onboarding_smart_optimization
            ),
            OnboardingStep(
                title = "内存管理",
                description = "智能清理后台应用，释放内存空间，提升运行速度",
                imageRes = R.drawable.onboarding_memory_management
            ),
            OnboardingStep(
                title = "电池优化",
                description = "优化电池使用策略，延长设备续航时间",
                imageRes = R.drawable.onboarding_battery_optimization
            ),
            OnboardingStep(
                title = "网络加速",
                description = "优化网络连接，提升网络访问速度和稳定性",
                imageRes = R.drawable.onboarding_network_optimization
            )
        )
    }

    /**
     * 显示引导对话框
     */
    private fun showOnboardingDialog(
        activity: FragmentActivity,
        steps: List<OnboardingStep>,
        type: OnboardingType
    ) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_onboarding, null)
        val viewPager = dialogView.findViewById<ViewPager2>(R.id.viewpager_onboarding)
        val tabLayout = dialogView.findViewById<TabLayout>(R.id.tab_layout)
        val btnSkip = dialogView.findViewById<Button>(R.id.btn_skip)
        val btnNext = dialogView.findViewById<Button>(R.id.btn_next)
        val btnPrevious = dialogView.findViewById<Button>(R.id.btn_previous)

        // 设置ViewPager适配器
        val adapter = OnboardingPagerAdapter(activity, steps)
        viewPager.adapter = adapter

        // 关联TabLayout和ViewPager
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // 创建对话框
        val dialog = androidx.appcompat.app.AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // 设置按钮点击事件
        setupOnboardingButtons(
            dialog, viewPager, btnSkip, btnNext, btnPrevious,
            steps, type
        )

        dialog.show()
    }

    /**
     * 设置引导按钮事件
     */
    private fun setupOnboardingButtons(
        dialog: androidx.appcompat.app.AlertDialog,
        viewPager: ViewPager2,
        btnSkip: Button,
        btnNext: Button,
        btnPrevious: Button,
        steps: List<OnboardingStep>,
        type: OnboardingType
    ) {
        var currentPosition = 0

        // 更新按钮状态
        fun updateButtons() {
            btnPrevious.visibility = if (currentPosition > 0) View.VISIBLE else View.GONE

            when (currentPosition) {
                steps.size - 1 -> {
                    btnNext.text = "完成"
                    btnSkip.visibility = View.GONE
                }
                else -> {
                    btnNext.text = "下一步"
                    btnSkip.visibility = View.VISIBLE
                }
            }
        }

        // ViewPager页面改变监听
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
                updateButtons()
            }
        })

        // 跳过按钮
        btnSkip.setOnClickListener {
            completeOnboarding(type)
            dialog.dismiss()

            analyticsManager.trackEvent("onboarding_skipped", android.os.Bundle().apply {
                putString("onboarding_type", type.name)
                putInt("skipped_at_step", currentPosition)
            })
        }

        // 上一步按钮
        btnPrevious.setOnClickListener {
            if (currentPosition > 0) {
                viewPager.currentItem = currentPosition - 1
            }
        }

        // 下一步/完成按钮
        btnNext.setOnClickListener {
            if (currentPosition < steps.size - 1) {
                // 执行当前步骤的动作
                steps[currentPosition].action?.invoke()
                viewPager.currentItem = currentPosition + 1
            } else {
                // 完成引导
                completeOnboarding(type)
                dialog.dismiss()

                analyticsManager.trackEvent("onboarding_completed", android.os.Bundle().apply {
                    putString("onboarding_type", type.name)
                    putInt("total_steps", steps.size)
                })
            }
        }

        updateButtons()
    }

    /**
     * 完成引导
     */
    private fun completeOnboarding(type: OnboardingType) {
        when (type) {
            OnboardingType.FIRST_TIME -> {
                preferencesManager.putBoolean(PREF_ONBOARDING_COMPLETED, true)
                preferencesManager.putInt(PREF_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION)
            }
            OnboardingType.PERMISSIONS -> {
                preferencesManager.putBoolean(PREF_PERMISSIONS_TUTORIAL_SHOWN, true)
            }
            else -> {
                // 其他类型的完成处理
            }
        }
    }

    /**
     * 标记功能教程已显示
     */
    fun markFeatureTutorialShown(featureName: String) {
        val key = "${PREF_FEATURE_TUTORIAL_SHOWN}_$featureName"
        preferencesManager.putBoolean(key, true)

        analyticsManager.trackEvent("feature_tutorial_completed", android.os.Bundle().apply {
            putString("feature_name", featureName)
        })
    }

    /**
     * 重置引导状态（用于测试）
     */
    fun resetOnboarding() {
        preferencesManager.apply {
            remove(PREF_ONBOARDING_COMPLETED)
            remove(PREF_ONBOARDING_VERSION)
            remove(PREF_PERMISSIONS_TUTORIAL_SHOWN)
        }

        analyticsManager.trackFeatureUsed("onboarding_reset")
    }

    // 引导动作方法
    private fun showShizukuInfo() {
        // 显示Shizuku信息
    }

    private fun requestBasicPermissions() {
        // 请求基础权限
    }

    private fun requestOverlayPermission() {
        // 请求悬浮窗权限
    }

    private fun showShizukuGuide() {
        // 显示Shizuku指南
    }

    private fun openShizukuDownload() {
        // 打开Shizuku下载页面
    }

    private fun showShizukuStartGuide() {
        // 显示Shizuku启动教程
    }
}

/**
 * 引导页面适配器
 */
class OnboardingPagerAdapter(
    private val fragmentActivity: FragmentActivity,
    private val steps: List<OnboardingManager.OnboardingStep>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = steps.size

    override fun createFragment(position: Int): Fragment {
        return OnboardingStepFragment.newInstance(steps[position])
    }
}

/**
 * 引导步骤Fragment
 */
class OnboardingStepFragment : Fragment() {

    companion object {
        private const val ARG_STEP = "step"

        fun newInstance(step: OnboardingManager.OnboardingStep): OnboardingStepFragment {
            val fragment = OnboardingStepFragment()
            val args = android.os.Bundle().apply {
                putString("title", step.title)
                putString("description", step.description)
                putInt("image_res", step.imageRes)
                putString("action_text", step.actionText)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_step, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments ?: return

        val imageView = view.findViewById<ImageView>(R.id.iv_step_image)
        val titleText = view.findViewById<TextView>(R.id.tv_step_title)
        val descriptionText = view.findViewById<TextView>(R.id.tv_step_description)
        val actionButton = view.findViewById<Button>(R.id.btn_step_action)

        // 设置内容
        imageView.setImageResource(args.getInt("image_res"))
        titleText.text = args.getString("title")
        descriptionText.text = args.getString("description")

        // 设置动作按钮
        val actionText = args.getString("action_text")
        if (actionText.isNullOrEmpty()) {
            actionButton.visibility = View.GONE
        } else {
            actionButton.text = actionText
            actionButton.visibility = View.VISIBLE
        }
    }
}

/**
 * 扩展函数
 */
fun Context.onboarding(): OnboardingManager = OnboardingManager.getInstance(this)