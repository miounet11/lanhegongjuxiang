package com.lanhe.gongjuxiang.ui.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi

/**
 * Haptic feedback manager for premium tactile interactions
 */
class HapticFeedbackManager(private val context: Context) {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Light haptic feedback for button clicks
     */
    fun lightClick(view: View? = null) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(25)
        }
    }

    /**
     * Medium haptic feedback for selections
     */
    fun mediumClick(view: View? = null) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    /**
     * Heavy haptic feedback for important actions
     */
    fun heavyClick(view: View? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        } else {
            view?.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(75)
        }
    }

    /**
     * Success haptic pattern
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun success() {
        val pattern = longArrayOf(0, 50, 50, 75)
        val amplitudes = intArrayOf(0, 100, 0, 150)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
    }

    /**
     * Error/warning haptic pattern
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun error() {
        val pattern = longArrayOf(0, 100, 100, 100, 100, 100)
        val amplitudes = intArrayOf(0, 200, 0, 200, 0, 200)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
    }

    /**
     * Long press haptic feedback
     */
    fun longPress(view: View) {
        view.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    /**
     * Context click (right-click equivalent)
     */
    fun contextClick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.performHapticFeedback(
                HapticFeedbackConstants.CONTEXT_CLICK,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    /**
     * Selection feedback
     */
    fun selection(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(
                HapticFeedbackConstants.TEXT_HANDLE_MOVE,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    /**
     * Custom haptic pattern
     */
    fun customPattern(pattern: LongArray, amplitudes: IntArray = intArrayOf()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (amplitudes.isNotEmpty()) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    /**
     * Gentle notification feedback
     */
    fun gentleNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, 50))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    /**
     * Quick tick feedback
     */
    fun tick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: HapticFeedbackManager? = null

        fun getInstance(context: Context): HapticFeedbackManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HapticFeedbackManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}