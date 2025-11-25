package com.lanhe.gongjuxiang.utils

import android.annotation.SuppressLint
import android.content.Context
import android.media.RingtoneManager
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.lanhe.gongjuxiang.R

/**
 * 统一的通知提醒系统
 *
 * 支持5个通知级别，可配置声音、振动、自动关闭时间等参数
 * 用于网络抓包、网络诊断等功能的消息提示
 */
object NotificationHelper {

    /**
     * 通知级别枚举
     */
    enum class NotificationLevel {
        INFO,      // 信息性提醒 - 蓝色
        SUCCESS,   // 成功提醒 - 绿色
        WARNING,   // 警告提醒 - 橙色
        ERROR,     // 错误提醒 - 红色
        CRITICAL   // 严重提醒 - 深红色
    }

    /**
     * 通知模式枚举
     */
    enum class NotificationMode {
        SILENT,    // 无声
        VIBRATION, // 仅振动（默认）
        SOUND,     // 仅声音
        BOTH       // 声音+振动
    }

    /**
     * 通知配置数据类
     */
    data class NotificationConfig(
        val enableNotification: Boolean = true,
        val notificationMode: NotificationMode = NotificationMode.VIBRATION,
        val autoDismissTime: Int = 3000, // 毫秒
        val showDetails: Boolean = true,
        val vibrationDuration: Long = 200, // 毫秒
        val playSound: Boolean = false
    )

    /**
     * 根据通知级别获取对应的颜色资源ID
     */
    @ColorRes
    fun getNotificationColor(level: NotificationLevel): Int = when (level) {
        NotificationLevel.INFO -> R.color.info
        NotificationLevel.SUCCESS -> R.color.success
        NotificationLevel.WARNING -> R.color.warning
        NotificationLevel.ERROR -> R.color.error
        NotificationLevel.CRITICAL -> R.color.error // 使用error的深红色
    }

    /**
     * 根据通知级别获取图标emoji
     */
    fun getNotificationIcon(level: NotificationLevel): String = when (level) {
        NotificationLevel.INFO -> "ℹ️"
        NotificationLevel.SUCCESS -> "✅"
        NotificationLevel.WARNING -> "⚠️"
        NotificationLevel.ERROR -> "❌"
        NotificationLevel.CRITICAL -> "🔴"
    }

    /**
     * 显示Snackbar通知（推荐）
     *
     * @param context 上下文
     * @param view 用于显示Snackbar的锚点视图
     * @param message 通知消息
     * @param level 通知级别
     * @param config 通知配置
     * @param action 可选的操作按钮文本
     * @param actionCallback 操作按钮的回调函数
     */
    fun showSnackbar(
        context: Context,
        view: View,
        message: String,
        level: NotificationLevel = NotificationLevel.INFO,
        config: NotificationConfig = NotificationConfig(),
        action: String? = null,
        actionCallback: (() -> Unit)? = null
    ) {
        if (!config.enableNotification) return

        val icon = getNotificationIcon(level)
        val displayMessage = "$icon $message"
        val duration = if (config.autoDismissTime > 0) {
            Snackbar.LENGTH_LONG
        } else {
            Snackbar.LENGTH_INDEFINITE
        }

        val snackbar = Snackbar.make(view, displayMessage, duration).apply {
            setBackgroundTint(ContextCompat.getColor(context, getNotificationColor(level)))
            setTextColor(ContextCompat.getColor(context, android.R.color.white))

            if (action != null && actionCallback != null) {
                setAction(action) { actionCallback() }
                setActionTextColor(ContextCompat.getColor(context, android.R.color.white))
            }
        }

        snackbar.show()

        // 自动关闭
        if (config.autoDismissTime > 0 && config.autoDismissTime != 3000) {
            snackbar.view.postDelayed({
                snackbar.dismiss()
            }, config.autoDismissTime.toLong())
        }

        // 触发提醒（声音/振动）
        triggerNotificationFeedback(context, level, config)
    }

    /**
     * 显示Toast通知（备选）
     *
     * @param context 上下文
     * @param message 通知消息
     * @param level 通知级别
     * @param config 通知配置
     */
    fun showToast(
        context: Context,
        message: String,
        level: NotificationLevel = NotificationLevel.INFO,
        config: NotificationConfig = NotificationConfig()
    ) {
        if (!config.enableNotification) return

        val icon = getNotificationIcon(level)
        val displayMessage = "$icon $message"

        android.widget.Toast.makeText(
            context,
            displayMessage,
            android.widget.Toast.LENGTH_SHORT
        ).show()

        // 触发提醒（声音/振动）
        triggerNotificationFeedback(context, level, config)
    }

    /**
     * 触发通知反馈（声音/振动）
     */
    @SuppressLint("MissingPermission")
    private fun triggerNotificationFeedback(
        context: Context,
        level: NotificationLevel,
        config: NotificationConfig
    ) {
        if (!config.enableNotification) return

        when (config.notificationMode) {
            NotificationMode.SILENT -> {
                // 不进行任何反馈
            }
            NotificationMode.VIBRATION -> {
                performVibration(context, config.vibrationDuration)
            }
            NotificationMode.SOUND -> {
                playNotificationSound(context)
            }
            NotificationMode.BOTH -> {
                playNotificationSound(context)
                performVibration(context, config.vibrationDuration)
            }
        }
    }

    /**
     * 执行振动反馈
     */
    @SuppressLint("MissingPermission")
    private fun performVibration(context: Context, duration: Long) {
        try {
            // Android 12+ 使用 VibratorManager
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            vibrator?.vibrate(duration)
        } catch (e: Exception) {
            // 如果振动失败，静默处理
            e.printStackTrace()
        }
    }

    /**
     * 播放通知音效
     */
    private fun playNotificationSound(context: Context) {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
            ringtone?.play()
        } catch (e: Exception) {
            // 如果播放失败，静默处理
            e.printStackTrace()
        }
    }

    /**
     * 快速显示成功提醒
     */
    fun showSuccess(
        context: Context,
        view: View,
        message: String,
        config: NotificationConfig = NotificationConfig()
    ) {
        showSnackbar(
            context,
            view,
            message,
            NotificationLevel.SUCCESS,
            config
        )
    }

    /**
     * 快速显示警告提醒
     */
    fun showWarning(
        context: Context,
        view: View,
        message: String,
        config: NotificationConfig = NotificationConfig()
    ) {
        showSnackbar(
            context,
            view,
            message,
            NotificationLevel.WARNING,
            config
        )
    }

    /**
     * 快速显示错误提醒
     */
    fun showError(
        context: Context,
        view: View,
        message: String,
        config: NotificationConfig = NotificationConfig()
    ) {
        showSnackbar(
            context,
            view,
            message,
            NotificationLevel.ERROR,
            config
        )
    }

    /**
     * 快速显示信息提醒
     */
    fun showInfo(
        context: Context,
        view: View,
        message: String,
        config: NotificationConfig = NotificationConfig()
    ) {
        showSnackbar(
            context,
            view,
            message,
            NotificationLevel.INFO,
            config
        )
    }

    /**
     * 获取推荐配置（根据场景）
     */
    fun getConfigForScene(scene: String): NotificationConfig = when (scene) {
        "quick_action" -> NotificationConfig(
            enableNotification = true,
            notificationMode = NotificationMode.VIBRATION,
            autoDismissTime = 2000,
            vibrationDuration = 100
        )
        "error" -> NotificationConfig(
            enableNotification = true,
            notificationMode = NotificationMode.VIBRATION,
            autoDismissTime = 4000,
            vibrationDuration = 300
        )
        "success" -> NotificationConfig(
            enableNotification = true,
            notificationMode = NotificationMode.VIBRATION,
            autoDismissTime = 2500,
            vibrationDuration = 150
        )
        "warning" -> NotificationConfig(
            enableNotification = true,
            notificationMode = NotificationMode.VIBRATION,
            autoDismissTime = 3500,
            vibrationDuration = 200
        )
        "critical" -> NotificationConfig(
            enableNotification = true,
            notificationMode = NotificationMode.BOTH,
            autoDismissTime = 5000,
            vibrationDuration = 400,
            playSound = true
        )
        else -> NotificationConfig()
    }
}
