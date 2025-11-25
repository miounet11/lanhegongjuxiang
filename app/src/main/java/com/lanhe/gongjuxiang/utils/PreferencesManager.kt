package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import java.util.*

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("core_optimization", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = prefs.edit()

    companion object {
        private const val MAX_DAILY_USES = 5
        private const val SESSION_DURATION_MS = 30 * 60 * 1000L // 30分钟

        // FPS提升相关
        private const val FPS_BOOST_COUNT = "fps_boost_count"
        private const val FPS_BOOST_ACTIVE = "fps_boost_active"
        private const val FPS_BOOST_START_TIME = "fps_boost_start_time"
        private const val FPS_BOOST_LAST_RESET = "fps_boost_last_reset"

        // 延迟优化相关
        private const val LATENCY_OPTIMIZATION_COUNT = "latency_optimization_count"
        private const val LATENCY_OPTIMIZATION_ACTIVE = "latency_optimization_active"
        private const val LATENCY_OPTIMIZATION_START_TIME = "latency_optimization_start_time"
        private const val LATENCY_OPTIMIZATION_LAST_RESET = "latency_optimization_last_reset"

        // 下载提速相关
        private const val DOWNLOAD_BOOST_COUNT = "download_boost_count"
        private const val DOWNLOAD_BOOST_ACTIVE = "download_boost_active"
        private const val DOWNLOAD_BOOST_START_TIME = "download_boost_start_time"
        private const val DOWNLOAD_BOOST_LAST_RESET = "download_boost_last_reset"

        // 弱网络视频优化相关
        private const val NETWORK_VIDEO_BOOST_COUNT = "network_video_boost_count"
        private const val NETWORK_VIDEO_BOOST_ACTIVE = "network_video_boost_active"
        private const val NETWORK_VIDEO_BOOST_START_TIME = "network_video_boost_start_time"
        private const val NETWORK_VIDEO_BOOST_LAST_RESET = "network_video_boost_last_reset"

        // UI & Theme preferences
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_ANIMATIONS_ENABLED = "animations_enabled"
        private const val KEY_AUTO_OPTIMIZATION = "auto_optimization"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PAGE_TRANSFORMER = "page_transformer_type"
        private const val KEY_SHIZUKU_PERMISSION_DIALOG_SHOWN = "shizuku_permission_dialog_shown"
    }

    // Shizuku Permission Dialog
    fun isShizukuPermissionDialogShown(): Boolean {
        return prefs.getBoolean(KEY_SHIZUKU_PERMISSION_DIALOG_SHOWN, false)
    }

    fun setShizukuPermissionDialogShown(shown: Boolean) {
        editor.putBoolean(KEY_SHIZUKU_PERMISSION_DIALOG_SHOWN, shown)
        editor.apply()
    }

    // FPS提升相关方法
    fun getFpsBoostRemainingCount(): Int {
        checkAndResetIfNeeded(FPS_BOOST_LAST_RESET, FPS_BOOST_COUNT)
        return prefs.getInt(FPS_BOOST_COUNT, MAX_DAILY_USES)
    }

    fun decrementFpsBoostCount() {
        val currentCount = getFpsBoostRemainingCount()
        if (currentCount > 0) {
            editor.putInt(FPS_BOOST_COUNT, currentCount - 1)
            editor.putLong(FPS_BOOST_LAST_RESET, System.currentTimeMillis())
            editor.apply()
        }
    }

    fun isFpsBoostActive(): Boolean {
        return prefs.getBoolean(FPS_BOOST_ACTIVE, false)
    }

    fun setFpsBoostActive(active: Boolean) {
        editor.putBoolean(FPS_BOOST_ACTIVE, active)
        if (active) {
            editor.putLong(FPS_BOOST_START_TIME, System.currentTimeMillis())
        }
        editor.apply()
    }

    fun getFpsBoostStartTime(): Long {
        return prefs.getLong(FPS_BOOST_START_TIME, 0)
    }

    fun setFpsBoostStartTime(time: Long) {
        editor.putLong(FPS_BOOST_START_TIME, time)
        editor.apply()
    }

    fun getFpsBoostRemainingTime(): Long {
        val startTime = getFpsBoostStartTime()
        if (startTime == 0L) return SESSION_DURATION_MS

        val elapsed = System.currentTimeMillis() - startTime
        return maxOf(0, SESSION_DURATION_MS - elapsed)
    }

    // 延迟优化相关方法
    fun getLatencyOptimizationRemainingCount(): Int {
        checkAndResetIfNeeded(LATENCY_OPTIMIZATION_LAST_RESET, LATENCY_OPTIMIZATION_COUNT)
        return prefs.getInt(LATENCY_OPTIMIZATION_COUNT, MAX_DAILY_USES)
    }

    fun decrementLatencyOptimizationCount() {
        val currentCount = getLatencyOptimizationRemainingCount()
        if (currentCount > 0) {
            editor.putInt(LATENCY_OPTIMIZATION_COUNT, currentCount - 1)
            editor.putLong(LATENCY_OPTIMIZATION_LAST_RESET, System.currentTimeMillis())
            editor.apply()
        }
    }

    fun isLatencyOptimizationActive(): Boolean {
        return prefs.getBoolean(LATENCY_OPTIMIZATION_ACTIVE, false)
    }

    fun setLatencyOptimizationActive(active: Boolean) {
        editor.putBoolean(LATENCY_OPTIMIZATION_ACTIVE, active)
        if (active) {
            editor.putLong(LATENCY_OPTIMIZATION_START_TIME, System.currentTimeMillis())
        }
        editor.apply()
    }

    fun getLatencyOptimizationStartTime(): Long {
        return prefs.getLong(LATENCY_OPTIMIZATION_START_TIME, 0)
    }

    fun setLatencyOptimizationStartTime(time: Long) {
        editor.putLong(LATENCY_OPTIMIZATION_START_TIME, time)
        editor.apply()
    }

    fun getLatencyOptimizationRemainingTime(): Long {
        val startTime = getLatencyOptimizationStartTime()
        if (startTime == 0L) return SESSION_DURATION_MS

        val elapsed = System.currentTimeMillis() - startTime
        return maxOf(0, SESSION_DURATION_MS - elapsed)
    }

    // 下载提速相关方法
    fun getDownloadBoostRemainingCount(): Int {
        checkAndResetIfNeeded(DOWNLOAD_BOOST_LAST_RESET, DOWNLOAD_BOOST_COUNT)
        return prefs.getInt(DOWNLOAD_BOOST_COUNT, MAX_DAILY_USES)
    }

    fun decrementDownloadBoostCount() {
        val currentCount = getDownloadBoostRemainingCount()
        if (currentCount > 0) {
            editor.putInt(DOWNLOAD_BOOST_COUNT, currentCount - 1)
            editor.putLong(DOWNLOAD_BOOST_LAST_RESET, System.currentTimeMillis())
            editor.apply()
        }
    }

    fun isDownloadBoostActive(): Boolean {
        return prefs.getBoolean(DOWNLOAD_BOOST_ACTIVE, false)
    }

    fun setDownloadBoostActive(active: Boolean) {
        editor.putBoolean(DOWNLOAD_BOOST_ACTIVE, active)
        if (active) {
            editor.putLong(DOWNLOAD_BOOST_START_TIME, System.currentTimeMillis())
        }
        editor.apply()
    }

    fun getDownloadBoostStartTime(): Long {
        return prefs.getLong(DOWNLOAD_BOOST_START_TIME, 0)
    }

    fun setDownloadBoostStartTime(time: Long) {
        editor.putLong(DOWNLOAD_BOOST_START_TIME, time)
        editor.apply()
    }

    fun getDownloadBoostRemainingTime(): Long {
        val startTime = getDownloadBoostStartTime()
        if (startTime == 0L) return SESSION_DURATION_MS

        val elapsed = System.currentTimeMillis() - startTime
        return maxOf(0, SESSION_DURATION_MS - elapsed)
    }

    // 弱网络视频优化相关方法
    fun getNetworkVideoBoostRemainingCount(): Int {
        checkAndResetIfNeeded(NETWORK_VIDEO_BOOST_LAST_RESET, NETWORK_VIDEO_BOOST_COUNT)
        return prefs.getInt(NETWORK_VIDEO_BOOST_COUNT, MAX_DAILY_USES)
    }

    fun decrementNetworkVideoBoostCount() {
        val currentCount = getNetworkVideoBoostRemainingCount()
        if (currentCount > 0) {
            editor.putInt(NETWORK_VIDEO_BOOST_COUNT, currentCount - 1)
            editor.putLong(NETWORK_VIDEO_BOOST_LAST_RESET, System.currentTimeMillis())
            editor.apply()
        }
    }

    fun isNetworkVideoBoostActive(): Boolean {
        return prefs.getBoolean(NETWORK_VIDEO_BOOST_ACTIVE, false)
    }

    fun setNetworkVideoBoostActive(active: Boolean) {
        editor.putBoolean(NETWORK_VIDEO_BOOST_ACTIVE, active)
        if (active) {
            editor.putLong(NETWORK_VIDEO_BOOST_START_TIME, System.currentTimeMillis())
        }
        editor.apply()
    }

    fun getNetworkVideoBoostStartTime(): Long {
        return prefs.getLong(NETWORK_VIDEO_BOOST_START_TIME, 0)
    }

    fun setNetworkVideoBoostStartTime(time: Long) {
        editor.putLong(NETWORK_VIDEO_BOOST_START_TIME, time)
        editor.apply()
    }

    fun getNetworkVideoBoostRemainingTime(): Long {
        val startTime = getNetworkVideoBoostStartTime()
        if (startTime == 0L) return SESSION_DURATION_MS

        val elapsed = System.currentTimeMillis() - startTime
        return maxOf(0, SESSION_DURATION_MS - elapsed)
    }

    // 通用方法
    private fun checkAndResetIfNeeded(lastResetKey: String, countKey: String) {
        val lastReset = prefs.getLong(lastResetKey, 0)
        val currentTime = System.currentTimeMillis()

        // 检查是否已经过了一天
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastReset
        val lastResetDay = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = currentTime
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

        // 如果是新的一天，重置计数
        if (currentDay != lastResetDay || lastReset == 0L) {
            editor.putInt(countKey, MAX_DAILY_USES)
            editor.putLong(lastResetKey, currentTime)
            editor.apply()
        }
    }

    // 重置所有功能的状态（用于测试或清理）
    fun resetAllFeatures() {
        editor.clear()
        editor.apply()
    }

    // 获取所有功能的总剩余次数
    fun getTotalRemainingUses(): Int {
        return getFpsBoostRemainingCount() +
               getLatencyOptimizationRemainingCount() +
               getDownloadBoostRemainingCount() +
               getNetworkVideoBoostRemainingCount()
    }

    // 获取当前活跃的功能数量
    fun getActiveFeaturesCount(): Int {
        var count = 0
        if (isFpsBoostActive()) count++
        if (isLatencyOptimizationActive()) count++
        if (isDownloadBoostActive()) count++
        if (isNetworkVideoBoostActive()) count++
        return count
    }

    // UI & Theme related methods

    /**
     * 深色模式设置
     */
    fun isDarkModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DARK_MODE, false)
    }

    fun setDarkModeEnabled(enabled: Boolean) {
        editor.putBoolean(KEY_DARK_MODE, enabled)
        editor.apply()
    }

    /**
     * 动画效果设置
     */
    fun isAnimationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_ANIMATIONS_ENABLED, true)
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        editor.putBoolean(KEY_ANIMATIONS_ENABLED, enabled)
        editor.apply()
    }

    /**
     * 自动优化设置
     */
    fun isAutoOptimizationEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_OPTIMIZATION, false)
    }

    fun setAutoOptimizationEnabled(enabled: Boolean) {
        editor.putBoolean(KEY_AUTO_OPTIMIZATION, enabled)
        editor.apply()
    }

    /**
     * 通知设置
     */
    fun isNotificationEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
    }

    fun setNotificationEnabled(enabled: Boolean) {
        editor.putBoolean(KEY_NOTIFICATION_ENABLED, enabled)
        editor.apply()
    }

    /**
     * 首次启动标识
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted() {
        editor.putBoolean(KEY_FIRST_LAUNCH, false)
        editor.apply()
    }

    /**
     * 应用主题
     */
    fun applyTheme() {
        val mode = if (isDarkModeEnabled()) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * 页面转换效果类型
     */
    fun getPageTransformerType(): String {
        return prefs.getString(KEY_PAGE_TRANSFORMER, "zoom_out") ?: "zoom_out"
    }

    fun setPageTransformerType(type: String) {
        editor.putString(KEY_PAGE_TRANSFORMER, type)
        editor.apply()
    }

    /**
     * 通用getString方法
     */
    fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * 通用putString方法
     */
    fun putString(key: String, value: String) {
        editor.putString(key, value)
        editor.apply()
    }

    /**
     * 通用getBoolean方法
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    /**
     * 通用putBoolean方法
     */
    fun putBoolean(key: String, value: Boolean) {
        editor.putBoolean(key, value)
        editor.apply()
    }

    /**
     * 获取Int值
     */
    fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    /**
     * 保存Int值
     */
    fun putInt(key: String, value: Int) {
        editor.putInt(key, value)
        editor.apply()
    }

    /**
     * 获取Long值
     */
    fun getLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }

    /**
     * 保存Long值
     */
    fun putLong(key: String, value: Long) {
        editor.putLong(key, value)
        editor.apply()
    }

    /**
     * 获取Float值
     */
    fun getFloat(key: String, defaultValue: Float): Float {
        return prefs.getFloat(key, defaultValue)
    }

    /**
     * 保存Float值
     */
    fun putFloat(key: String, value: Float) {
        editor.putFloat(key, value)
        editor.apply()
    }

    /**
     * 删除指定key的值
     */
    fun remove(key: String) {
        editor.remove(key)
        editor.apply()
    }
}
