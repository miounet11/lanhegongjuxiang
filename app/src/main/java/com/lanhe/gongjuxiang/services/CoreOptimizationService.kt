package com.lanhe.gongjuxiang.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.CoreOptimizationActivity
import com.lanhe.gongjuxiang.utils.CoreOptimizationManager
import com.lanhe.gongjuxiang.utils.PreferencesManager

class CoreOptimizationService : Service() {

    companion object {
        private const val TAG = "CoreOptimizationService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "core_optimization_channel"
        private const val CHANNEL_NAME = "核心优化服务"
    }

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var coreOptimizationManager: CoreOptimizationManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CoreOptimizationService created")

        preferencesManager = PreferencesManager(this)
        coreOptimizationManager = CoreOptimizationManager(this)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "CoreOptimizationService started")

        // 启动前台服务
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, createNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        // 检查并恢复活跃的优化功能
        restoreActiveOptimizations()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "核心性能优化服务"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, CoreOptimizationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val activeFeatures = getActiveFeaturesText()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentTitle("⚡ 核心优化运行中")
            .setContentText(activeFeatures)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun getActiveFeaturesText(): String {
        val activeFeatures = mutableListOf<String>()

        if (preferencesManager.isFpsBoostActive()) {
            activeFeatures.add("帧率提升")
        }
        if (preferencesManager.isLatencyOptimizationActive()) {
            activeFeatures.add("延迟优化")
        }
        if (preferencesManager.isDownloadBoostActive()) {
            activeFeatures.add("下载提速")
        }
        if (preferencesManager.isNetworkVideoBoostActive()) {
            activeFeatures.add("视频优化")
        }

        return when (activeFeatures.size) {
            0 -> "无活跃优化"
            1 -> "${activeFeatures[0]}运行中"
            else -> "${activeFeatures.size}个优化运行中"
        }
    }

    private fun restoreActiveOptimizations() {
        // 恢复FPS提升
        if (preferencesManager.isFpsBoostActive() && !coreOptimizationManager.isFpsOptimizationActive()) {
            coreOptimizationManager.startFpsOptimization()
        }

        // 恢复延迟优化
        if (preferencesManager.isLatencyOptimizationActive() && !coreOptimizationManager.isLatencyOptimizationActive()) {
            coreOptimizationManager.startLatencyOptimization()
        }

        // 恢复下载提速
        if (preferencesManager.isDownloadBoostActive() && !coreOptimizationManager.isDownloadOptimizationActive()) {
            coreOptimizationManager.startDownloadOptimization()
        }

        // 恢复视频优化
        if (preferencesManager.isNetworkVideoBoostActive() && !coreOptimizationManager.isNetworkVideoOptimizationActive()) {
            coreOptimizationManager.startNetworkVideoOptimization()
        }

        // 更新通知
        updateNotification()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CoreOptimizationService destroyed")

        // 停止所有优化
        coreOptimizationManager.stopAllOptimizations()
    }

    // 公共方法，用于从外部更新通知
    fun updateServiceNotification() {
        updateNotification()
    }
}
