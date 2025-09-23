package com.lanhe.gongjuxiang.utils

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.lanhe.gongjuxiang.R
import kotlinx.coroutines.*

/**
 * 蓝河助手 - 悬浮气泡管理器
 *
 * 功能特性：
 * - 全局悬浮气泡
 * - 快速功能访问
 * - 拖拽移动支持
 * - 自动吸附边缘
 * - 智能隐藏机制
 * - 一键优化功能
 */
class FloatingBubbleManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "FloatingBubbleManager"

        @Volatile
        private var INSTANCE: FloatingBubbleManager? = null

        fun getInstance(context: Context): FloatingBubbleManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FloatingBubbleManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // 悬浮窗权限请求码
        const val OVERLAY_PERMISSION_REQUEST_CODE = 1001

        // 气泡状态
        const val STATE_COLLAPSED = 0
        const val STATE_EXPANDED = 1
        const val STATE_HIDDEN = 2
    }

    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val analyticsManager = AnalyticsManager.getInstance(context)

    // 悬浮视图
    private var bubbleView: View? = null
    private var expandedView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // 状态管理
    private var currentState = STATE_COLLAPSED
    private var isShowing = false

    // 协程作用域
    private val bubbleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 屏幕尺寸
    private var screenWidth = 0
    private var screenHeight = 0

    // 自动隐藏
    private var autoHideJob: Job? = null
    private val autoHideDelay = 3000L // 3秒后自动收起

    init {
        getScreenSize()
    }

    /**
     * 检查悬浮窗权限
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestOverlayPermission() {
        if (!hasOverlayPermission()) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * 显示悬浮气泡
     */
    @SuppressLint("InflateParams")
    fun showBubble() {
        if (isShowing || !hasOverlayPermission()) {
            Log.w(TAG, "Cannot show bubble: permission denied or already showing")
            return
        }

        try {
            // 创建气泡视图
            bubbleView = LayoutInflater.from(context).inflate(R.layout.floating_bubble, null)
            setupBubbleView()

            // 创建布局参数
            layoutParams = createLayoutParams()

            // 添加到窗口
            windowManager.addView(bubbleView, layoutParams)
            isShowing = true

            // 记录分析事件
            analyticsManager.trackFeatureUsed("floating_bubble_shown")

            Log.d(TAG, "Floating bubble shown successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show floating bubble", e)
            analyticsManager.trackError("floating_bubble_show_failed", e.message ?: "Unknown error", e)
        }
    }

    /**
     * 隐藏悬浮气泡
     */
    fun hideBubble() {
        if (!isShowing) return

        try {
            bubbleView?.let { view ->
                windowManager.removeView(view)
                bubbleView = null
            }

            expandedView?.let { view ->
                windowManager.removeView(view)
                expandedView = null
            }

            isShowing = false
            currentState = STATE_COLLAPSED

            // 取消自动隐藏任务
            autoHideJob?.cancel()

            analyticsManager.trackFeatureUsed("floating_bubble_hidden")
            Log.d(TAG, "Floating bubble hidden")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide floating bubble", e)
        }
    }

    /**
     * 设置气泡视图
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubbleView() {
        val bubbleIcon = bubbleView?.findViewById<ImageView>(R.id.bubble_icon)
        val bubbleText = bubbleView?.findViewById<TextView>(R.id.bubble_text)

        // 设置图标和文本
        bubbleIcon?.setImageResource(R.drawable.ic_optimize)
        bubbleText?.text = "蓝河"

        // 设置点击监听器
        bubbleView?.setOnClickListener {
            handleBubbleClick()
        }

        // 设置触摸监听器（拖拽功能）
        bubbleView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams?.x ?: 0
                        initialY = layoutParams?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY

                        if (!isDragging && (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10)) {
                            isDragging = true
                        }

                        if (isDragging) {
                            layoutParams?.let { params ->
                                params.x = (initialX + deltaX).toInt()
                                params.y = (initialY + deltaY).toInt()

                                // 边界检查
                                params.x = params.x.coerceIn(0, screenWidth - (bubbleView?.width ?: 0))
                                params.y = params.y.coerceIn(0, screenHeight - (bubbleView?.height ?: 0))

                                windowManager.updateViewLayout(bubbleView, params)
                            }
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            // 自动吸附到边缘
                            snapToEdge()
                            isDragging = false
                            return true
                        }
                        return false
                    }
                }
                return false
            }
        })
    }

    /**
     * 处理气泡点击
     */
    private fun handleBubbleClick() {
        when (currentState) {
            STATE_COLLAPSED -> expandBubble()
            STATE_EXPANDED -> collapseBubble()
            STATE_HIDDEN -> showBubble()
        }

        analyticsManager.trackUserBehavior("bubble_clicked", "state_$currentState")
    }

    /**
     * 展开气泡
     */
    @SuppressLint("InflateParams")
    private fun expandBubble() {
        if (currentState == STATE_EXPANDED) return

        try {
            // 创建展开视图
            expandedView = LayoutInflater.from(context).inflate(R.layout.floating_bubble_expanded, null)
            setupExpandedView()

            // 创建展开视图的布局参数
            val expandedParams = createExpandedLayoutParams()

            // 添加展开视图
            windowManager.addView(expandedView, expandedParams)
            currentState = STATE_EXPANDED

            // 启动自动隐藏定时器
            startAutoHideTimer()

            analyticsManager.trackFeatureUsed("floating_bubble_expanded")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to expand bubble", e)
        }
    }

    /**
     * 收起气泡
     */
    private fun collapseBubble() {
        if (currentState != STATE_EXPANDED) return

        try {
            expandedView?.let { view ->
                windowManager.removeView(view)
                expandedView = null
            }

            currentState = STATE_COLLAPSED

            // 取消自动隐藏定时器
            autoHideJob?.cancel()

            analyticsManager.trackFeatureUsed("floating_bubble_collapsed")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to collapse bubble", e)
        }
    }

    /**
     * 设置展开视图
     */
    private fun setupExpandedView() {
        val quickOptimizeBtn = expandedView?.findViewById<LinearLayout>(R.id.btn_quick_optimize)
        val memoryCleanBtn = expandedView?.findViewById<LinearLayout>(R.id.btn_memory_clean)
        val batteryOptimizeBtn = expandedView?.findViewById<LinearLayout>(R.id.btn_battery_optimize)
        val closeBtn = expandedView?.findViewById<ImageView>(R.id.btn_close)

        // 快速优化
        quickOptimizeBtn?.setOnClickListener {
            performQuickOptimization()
            collapseBubble()
        }

        // 内存清理
        memoryCleanBtn?.setOnClickListener {
            performMemoryCleanup()
            collapseBubble()
        }

        // 电池优化
        batteryOptimizeBtn?.setOnClickListener {
            performBatteryOptimization()
            collapseBubble()
        }

        // 关闭按钮
        closeBtn?.setOnClickListener {
            hideBubble()
        }
    }

    /**
     * 执行快速优化
     */
    private fun performQuickOptimization() {
        bubbleScope.launch {
            try {
                val systemOptimizer = SystemOptimizer(context)
                val result = systemOptimizer.performQuickOptimization()

                analyticsManager.trackOptimization(
                    "quick_optimization_from_bubble",
                    result.success,
                    result.message
                )

                // 显示结果提示
                showOptimizationResult("快速优化", result.success)

            } catch (e: Exception) {
                Log.e(TAG, "Quick optimization failed", e)
                analyticsManager.trackError("bubble_quick_optimization_failed", e.message ?: "Unknown error", e)
            }
        }
    }

    /**
     * 执行内存清理
     */
    private fun performMemoryCleanup() {
        bubbleScope.launch {
            try {
                val smartCleaner = SmartCleaner(context)
                val result = smartCleaner.performMemoryCleanup()

                analyticsManager.trackOptimization(
                    "memory_cleanup_from_bubble",
                    result.success,
                    "清理了 ${result.cleanedMemory}MB 内存"
                )

                showOptimizationResult("内存清理", result.success)

            } catch (e: Exception) {
                Log.e(TAG, "Memory cleanup failed", e)
                analyticsManager.trackError("bubble_memory_cleanup_failed", e.message ?: "Unknown error", e)
            }
        }
    }

    /**
     * 执行电池优化
     */
    private fun performBatteryOptimization() {
        bubbleScope.launch {
            try {
                val systemOptimizer = SystemOptimizer(context)
                val result = systemOptimizer.performBatteryOptimization()

                analyticsManager.trackOptimization(
                    "battery_optimization_from_bubble",
                    result.success,
                    result.message
                )

                showOptimizationResult("电池优化", result.success)

            } catch (e: Exception) {
                Log.e(TAG, "Battery optimization failed", e)
                analyticsManager.trackError("bubble_battery_optimization_failed", e.message ?: "Unknown error", e)
            }
        }
    }

    /**
     * 显示优化结果
     */
    private fun showOptimizationResult(optimizationType: String, success: Boolean) {
        // 这里可以显示一个简单的Toast或者更新气泡图标
        // 实际实现可以根据需要添加视觉反馈
    }

    /**
     * 自动吸附到边缘
     */
    private fun snapToEdge() {
        layoutParams?.let { params ->
            val centerX = params.x + (bubbleView?.width ?: 0) / 2
            val targetX = if (centerX < screenWidth / 2) {
                0 // 吸附到左边
            } else {
                screenWidth - (bubbleView?.width ?: 0) // 吸附到右边
            }

            // 平滑移动到目标位置
            animateToPosition(targetX, params.y)
        }
    }

    /**
     * 动画移动到指定位置
     */
    private fun animateToPosition(targetX: Int, targetY: Int) {
        // 这里可以实现平滑的动画效果
        layoutParams?.let { params ->
            params.x = targetX
            params.y = targetY
            windowManager.updateViewLayout(bubbleView, params)
        }
    }

    /**
     * 启动自动隐藏定时器
     */
    private fun startAutoHideTimer() {
        autoHideJob?.cancel()
        autoHideJob = bubbleScope.launch {
            delay(autoHideDelay)
            if (currentState == STATE_EXPANDED) {
                collapseBubble()
            }
        }
    }

    /**
     * 创建布局参数
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - 200 // 初始位置在右侧
            y = screenHeight / 2  // 垂直居中
        }
    }

    /**
     * 创建展开视图布局参数
     */
    private fun createExpandedLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    /**
     * 获取屏幕尺寸
     */
    private fun getScreenSize() {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        screenWidth = size.x
        screenHeight = size.y
    }

    /**
     * 更新气泡状态
     */
    fun updateBubbleStatus(status: String) {
        bubbleView?.findViewById<TextView>(R.id.bubble_text)?.text = status
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        hideBubble()
        bubbleScope.coroutineContext.cancelChildren()
    }
}

/**
 * 悬浮气泡服务
 */
class FloatingBubbleService : Service() {

    private lateinit var bubbleManager: FloatingBubbleManager

    override fun onCreate() {
        super.onCreate()
        bubbleManager = FloatingBubbleManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_BUBBLE -> bubbleManager.showBubble()
            ACTION_HIDE_BUBBLE -> bubbleManager.hideBubble()
            ACTION_STOP_SERVICE -> {
                bubbleManager.hideBubble()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        bubbleManager.cleanup()
    }

    companion object {
        const val ACTION_SHOW_BUBBLE = "com.lanhe.gongjuxiang.SHOW_BUBBLE"
        const val ACTION_HIDE_BUBBLE = "com.lanhe.gongjuxiang.HIDE_BUBBLE"
        const val ACTION_STOP_SERVICE = "com.lanhe.gongjuxiang.STOP_BUBBLE_SERVICE"
    }
}