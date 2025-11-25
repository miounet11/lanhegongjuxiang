package com.lanhe.gongjuxiang.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.CoreOptimizationActivity
import com.lanhe.gongjuxiang.utils.CoreOptimizationManager
import com.lanhe.gongjuxiang.utils.PreferencesManager
import kotlinx.coroutines.*

/**
 * 核心优化服务 - 重构版本
 * 管理游戏加速、网络优化等核心功能
 * 包含完整的生命周期管理和异常恢复机制
 */
class CoreOptimizationService : BaseLifecycleService() {

    companion object {
        private const val TAG = "CoreOptimizationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "core_optimization_channel"
        private const val CHANNEL_NAME = "核心优化服务"
        private const val CHECK_INTERVAL = 60000L // 1分钟检查一次状态

        fun startService(context: Context) {
            val intent = Intent(context, CoreOptimizationService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.i(TAG, "服务启动请求已发送")
            } catch (e: Exception) {
                Log.e(TAG, "启动服务失败", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CoreOptimizationService::class.java)
            try {
                context.stopService(intent)
                Log.i(TAG, "服务停止请求已发送")
            } catch (e: Exception) {
                Log.e(TAG, "停止服务失败", e)
            }
        }
    }

    // 核心组件
    private var preferencesManager: PreferencesManager? = null
    private var coreOptimizationManager: CoreOptimizationManager? = null

    // 监控任务
    private var monitoringJob: Job? = null

    @Volatile
    private var isMonitoring = false

    // 优化状态缓存
    private data class OptimizationState(
        val fpsBoostActive: Boolean = false,
        val latencyOptimizationActive: Boolean = false,
        val downloadBoostActive: Boolean = false,
        val networkVideoBoostActive: Boolean = false
    )

    private var currentState = OptimizationState()
    private var lastStateCheckTime = 0L

    override fun getServiceTag(): String = TAG

    override suspend fun onInitialize(): Boolean {
        return try {
            Log.d(TAG, "开始初始化核心优化服务")

            // 初始化组件
            initializeComponents()

            // 创建通知通道
            createNotificationChannel()

            // 启动前台服务
            startForegroundServiceSafely()

            // 恢复活跃的优化功能
            restoreActiveOptimizations()

            // 启动状态监控
            startStatusMonitoring()

            Log.i(TAG, "核心优化服务初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "初始化失败", e)
            false
        }
    }

    override fun onCleanup() {
        Log.d(TAG, "开始清理核心优化服务")

        // 停止监控
        stopMonitoring()

        // 保存当前状态
        saveCurrentState()

        // 停止所有优化（如果配置要求）
        if (shouldStopOptimizationsOnDestroy()) {
            stopAllOptimizations()
        }

        // 清理组件引用
        preferencesManager = null
        coreOptimizationManager = null

        Log.d(TAG, "核心优化服务清理完成")
    }

    override fun onTaskRemovedHandle() {
        Log.w(TAG, "应用从最近任务中被移除")

        // 保存状态
        saveCurrentState()

        // 根据配置决定是否继续运行
        if (shouldContinueOnTaskRemoved()) {
            Log.i(TAG, "配置为继续运行，保持服务活跃")
        } else {
            Log.i(TAG, "配置为停止运行，准备关闭服务")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")

        // 处理特定命令
        intent?.action?.let { action ->
            when (action) {
                "UPDATE_NOTIFICATION" -> updateNotification()
                "RESTORE_STATE" -> restoreActiveOptimizations()
                "STOP_ALL" -> {
                    stopAllOptimizations()
                    stopSelf()
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 初始化核心组件
     */
    private suspend fun initializeComponents() = withContext(Dispatchers.Main) {
        try {
            preferencesManager = PreferencesManager(this@CoreOptimizationService)
            coreOptimizationManager = CoreOptimizationManager(this@CoreOptimizationService)
            Log.d(TAG, "组件初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "组件初始化失败", e)
            throw e
        }
    }

    /**
     * 创建通知通道
     */
    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "核心性能优化服务通知"
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "通知通道创建成功")
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建通知通道失败", e)
        }
    }

    /**
     * 安全启动前台服务
     */
    private fun startForegroundServiceSafely() {
        try {
            val notification = createNotification()

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    // Android 12+
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    // Android 10-11
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                }
                else -> {
                    // Android 9及以下
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            Log.d(TAG, "前台服务启动成功")
        } catch (e: Exception) {
            Log.e(TAG, "启动前台服务失败", e)
        }
    }

    /**
     * 创建服务通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, CoreOptimizationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activeFeatures = getActiveFeaturesText()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("核心优化运行中")
            .setContentText(activeFeatures)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(createStopAction())
            .build()
    }

    /**
     * 创建停止操作按钮
     */
    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent = Intent(this, CoreOptimizationService::class.java).apply {
            action = "STOP_ALL"
        }

        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "停止优化",
            stopPendingIntent
        ).build()
    }

    /**
     * 获取活跃功能文本
     */
    private fun getActiveFeaturesText(): String {
        val activeFeatures = mutableListOf<String>()

        try {
            preferencesManager?.let { prefs ->
                if (prefs.isFpsBoostActive()) {
                    activeFeatures.add("帧率提升")
                }
                if (prefs.isLatencyOptimizationActive()) {
                    activeFeatures.add("延迟优化")
                }
                if (prefs.isDownloadBoostActive()) {
                    activeFeatures.add("下载提速")
                }
                if (prefs.isNetworkVideoBoostActive()) {
                    activeFeatures.add("视频优化")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取活跃功能状态失败", e)
        }

        return when (activeFeatures.size) {
            0 -> "待机中"
            1 -> "${activeFeatures[0]}运行中"
            else -> "${activeFeatures.size}个优化运行中"
        }
    }

    /**
     * 恢复活跃的优化功能
     */
    private fun restoreActiveOptimizations() {
        serviceScope.launch {
            try {
                Log.d(TAG, "开始恢复活跃的优化功能")

                val prefs = preferencesManager ?: return@launch
                val manager = coreOptimizationManager ?: return@launch

                // 恢复FPS提升
                if (prefs.isFpsBoostActive() && !manager.isFpsOptimizationActive()) {
                    Log.i(TAG, "恢复FPS提升功能")
                    manager.startFpsOptimization()
                }

                // 恢复延迟优化
                if (prefs.isLatencyOptimizationActive() && !manager.isLatencyOptimizationActive()) {
                    Log.i(TAG, "恢复延迟优化功能")
                    manager.startLatencyOptimization()
                }

                // 恢复下载提速
                if (prefs.isDownloadBoostActive() && !manager.isDownloadOptimizationActive()) {
                    Log.i(TAG, "恢复下载提速功能")
                    manager.startDownloadOptimization()
                }

                // 恢复视频优化
                if (prefs.isNetworkVideoBoostActive() && !manager.isNetworkVideoOptimizationActive()) {
                    Log.i(TAG, "恢复视频优化功能")
                    manager.startNetworkVideoOptimization()
                }

                // 更新状态缓存
                updateStateCache()

                // 更新通知
                updateNotification()

                Log.d(TAG, "优化功能恢复完成")
            } catch (e: Exception) {
                Log.e(TAG, "恢复优化功能失败", e)
            }
        }
    }

    /**
     * 启动状态监控
     */
    private fun startStatusMonitoring() {
        if (isMonitoring) return

        isMonitoring = true
        monitoringJob = serviceScope.launch {
            Log.d(TAG, "启动状态监控")
            while (isMonitoring && isActive) {
                try {
                    // 检查优化状态
                    checkOptimizationStatus()

                    // 更新通知（如果需要）
                    if (hasStateChanged()) {
                        updateNotification()
                    }

                    delay(CHECK_INTERVAL)
                } catch (e: CancellationException) {
                    Log.d(TAG, "监控协程被取消")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "状态监控异常", e)
                    delay(5000) // 错误后短暂延迟
                }
            }
            Log.d(TAG, "状态监控结束")
        }
    }

    /**
     * 停止监控
     */
    private fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        monitoringJob = null
        Log.d(TAG, "监控已停止")
    }

    /**
     * 检查优化状态
     */
    private suspend fun checkOptimizationStatus() = withContext(Dispatchers.IO) {
        try {
            val manager = coreOptimizationManager ?: return@withContext

            // 验证各项优化是否仍在运行
            val prefs = preferencesManager ?: return@withContext

            // 检查并重启失败的优化
            if (prefs.isFpsBoostActive() && !manager.isFpsOptimizationActive()) {
                Log.w(TAG, "检测到FPS优化异常停止，尝试重启")
                manager.startFpsOptimization()
            }

            if (prefs.isLatencyOptimizationActive() && !manager.isLatencyOptimizationActive()) {
                Log.w(TAG, "检测到延迟优化异常停止，尝试重启")
                manager.startLatencyOptimization()
            }

            lastStateCheckTime = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "检查优化状态失败", e)
        }
    }

    /**
     * 更新状态缓存
     */
    private fun updateStateCache() {
        try {
            preferencesManager?.let { prefs ->
                currentState = OptimizationState(
                    fpsBoostActive = prefs.isFpsBoostActive(),
                    latencyOptimizationActive = prefs.isLatencyOptimizationActive(),
                    downloadBoostActive = prefs.isDownloadBoostActive(),
                    networkVideoBoostActive = prefs.isNetworkVideoBoostActive()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新状态缓存失败", e)
        }
    }

    /**
     * 检查状态是否变化
     */
    private fun hasStateChanged(): Boolean {
        return try {
            preferencesManager?.let { prefs ->
                currentState.fpsBoostActive != prefs.isFpsBoostActive() ||
                currentState.latencyOptimizationActive != prefs.isLatencyOptimizationActive() ||
                currentState.downloadBoostActive != prefs.isDownloadBoostActive() ||
                currentState.networkVideoBoostActive != prefs.isNetworkVideoBoostActive()
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "检查状态变化失败", e)
            false
        }
    }

    /**
     * 更新通知
     */
    fun updateNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, createNotification())
            updateStateCache()
            Log.d(TAG, "通知已更新")
        } catch (e: Exception) {
            Log.e(TAG, "更新通知失败", e)
        }
    }

    /**
     * 停止所有优化
     */
    private fun stopAllOptimizations() {
        try {
            Log.i(TAG, "停止所有优化功能")
            coreOptimizationManager?.stopAllOptimizations()
        } catch (e: Exception) {
            Log.e(TAG, "停止优化功能失败", e)
        }
    }

    /**
     * 保存当前状态
     */
    private fun saveCurrentState() {
        try {
            val prefs = getSharedPreferences("core_optimization_state", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("service_was_running", true)
                putLong("last_run_time", System.currentTimeMillis())
                putString("last_state", currentState.toString())
                apply()
            }
            Log.d(TAG, "当前状态已保存")
        } catch (e: Exception) {
            Log.e(TAG, "保存状态失败", e)
        }
    }

    /**
     * 检查是否应该在销毁时停止优化
     */
    private fun shouldStopOptimizationsOnDestroy(): Boolean {
        return try {
            preferencesManager?.let {
                // 可以添加用户配置选项
                false // 默认不停止，保持优化运行
            } ?: true
        } catch (e: Exception) {
            Log.e(TAG, "检查配置失败", e)
            true
        }
    }

    /**
     * 检查任务移除时是否继续运行
     */
    private fun shouldContinueOnTaskRemoved(): Boolean {
        return try {
            // 如果有活跃的优化，继续运行
            currentState.fpsBoostActive ||
                    currentState.latencyOptimizationActive ||
                    currentState.downloadBoostActive ||
                    currentState.networkVideoBoostActive
        } catch (e: Exception) {
            Log.e(TAG, "检查运行状态失败", e)
            false
        }
    }

    /**
     * 公共方法：更新服务通知（供外部调用）
     */
    fun updateServiceNotification() {
        updateNotification()
    }
}