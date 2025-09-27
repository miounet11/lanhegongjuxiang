package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat

/**
 * 主题管理器
 * 负责应用主题的切换和管理
 */
class ThemeManager(private val context: Context) {

    private val preferences: SharedPreferences = 
        context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE)

    companion object {
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_AUTO = "auto"
        const val THEME_AMOLED = "amoled"
        const val THEME_BLUE = "blue"
        const val THEME_GREEN = "green"
        const val THEME_PURPLE = "purple"
        const val THEME_ORANGE = "orange"
        
        private const val KEY_THEME = "current_theme"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_PRIMARY_COLOR = "primary_color"
        private const val KEY_CUSTOM_COLORS = "custom_colors"
    }

    /**
     * 获取当前主题
     */
    fun getCurrentTheme(): String {
        return preferences.getString(KEY_THEME, THEME_AUTO) ?: THEME_AUTO
    }

    /**
     * 设置主题
     */
    fun setTheme(theme: String) {
        preferences.edit().putString(KEY_THEME, theme).apply()
        applyTheme(theme)
    }

    /**
     * 应用主题
     */
    private fun applyTheme(theme: String) {
        when (theme) {
            THEME_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            THEME_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            THEME_AMOLED -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                // AMOLED主题使用纯黑色背景
                setCustomColors(
                    primaryColor = Color.BLACK,
                    accentColor = Color.WHITE,
                    backgroundColor = Color.BLACK
                )
            }
            THEME_AUTO -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                }
            }
            THEME_BLUE -> {
                setCustomColors(
                    primaryColor = Color.parseColor("#2196F3"),
                    accentColor = Color.parseColor("#FF4081"),
                    backgroundColor = Color.parseColor("#F5F5F5")
                )
            }
            THEME_GREEN -> {
                setCustomColors(
                    primaryColor = Color.parseColor("#4CAF50"),
                    accentColor = Color.parseColor("#FF9800"),
                    backgroundColor = Color.parseColor("#F1F8E9")
                )
            }
            THEME_PURPLE -> {
                setCustomColors(
                    primaryColor = Color.parseColor("#9C27B0"),
                    accentColor = Color.parseColor("#FF5722"),
                    backgroundColor = Color.parseColor("#F3E5F5")
                )
            }
            THEME_ORANGE -> {
                setCustomColors(
                    primaryColor = Color.parseColor("#FF9800"),
                    accentColor = Color.parseColor("#E91E63"),
                    backgroundColor = Color.parseColor("#FFF3E0")
                )
            }
        }
    }

    /**
     * 设置自定义颜色
     */
    fun setCustomColors(
        primaryColor: Int,
        accentColor: Int,
        backgroundColor: Int
    ) {
        preferences.edit()
            .putInt(KEY_PRIMARY_COLOR, primaryColor)
            .putInt(KEY_ACCENT_COLOR, accentColor)
            .putInt("background_color", backgroundColor)
            .apply()
    }

    /**
     * 获取主色调
     */
    fun getPrimaryColor(): Int {
        return preferences.getInt(KEY_PRIMARY_COLOR, ContextCompat.getColor(context, android.R.color.holo_blue_bright))
    }

    /**
     * 获取强调色
     */
    fun getAccentColor(): Int {
        return preferences.getInt(KEY_ACCENT_COLOR, ContextCompat.getColor(context, android.R.color.holo_orange_light))
    }

    /**
     * 获取背景色
     */
    fun getBackgroundColor(): Int {
        return preferences.getInt("background_color", ContextCompat.getColor(context, android.R.color.white))
    }

    /**
     * 获取所有可用主题
     */
    fun getAvailableThemes(): List<ThemeInfo> {
        return listOf(
            ThemeInfo(THEME_LIGHT, "浅色主题", "经典浅色界面，适合白天使用", Color.parseColor("#FFFFFF")),
            ThemeInfo(THEME_DARK, "深色主题", "护眼深色界面，适合夜间使用", Color.parseColor("#121212")),
            ThemeInfo(THEME_AMOLED, "AMOLED主题", "纯黑背景，节省电量", Color.parseColor("#000000")),
            ThemeInfo(THEME_AUTO, "跟随系统", "自动跟随系统主题设置", Color.parseColor("#2196F3")),
            ThemeInfo(THEME_BLUE, "蓝色主题", "清新蓝色配色方案", Color.parseColor("#2196F3")),
            ThemeInfo(THEME_GREEN, "绿色主题", "自然绿色配色方案", Color.parseColor("#4CAF50")),
            ThemeInfo(THEME_PURPLE, "紫色主题", "神秘紫色配色方案", Color.parseColor("#9C27B0")),
            ThemeInfo(THEME_ORANGE, "橙色主题", "活力橙色配色方案", Color.parseColor("#FF9800"))
        )
    }

    /**
     * 获取主题信息
     */
    fun getThemeInfo(theme: String): ThemeInfo? {
        return getAvailableThemes().find { it.id == theme }
    }

    /**
     * 检查是否为深色主题
     */
    fun isDarkTheme(): Boolean {
        val currentTheme = getCurrentTheme()
        return when (currentTheme) {
            THEME_DARK, THEME_AMOLED -> true
            THEME_AUTO -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // 检查系统是否处于深色模式
                    val nightModeFlags = context.resources.configuration.uiMode and 
                                       android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
                } else {
                    false
                }
            }
            else -> false
        }
    }

    /**
     * 获取状态栏颜色
     */
    fun getStatusBarColor(): Int {
        return if (isDarkTheme()) {
            Color.parseColor("#000000")
        } else {
            getPrimaryColor()
        }
    }

    /**
     * 获取导航栏颜色
     */
    fun getNavigationBarColor(): Int {
        return if (isDarkTheme()) {
            Color.parseColor("#000000")
        } else {
            Color.parseColor("#FFFFFF")
        }
    }

    /**
     * 获取文字颜色
     */
    fun getTextColor(): Int {
        return if (isDarkTheme()) {
            Color.parseColor("#FFFFFF")
        } else {
            Color.parseColor("#000000")
        }
    }

    /**
     * 获取次要文字颜色
     */
    fun getSecondaryTextColor(): Int {
        return if (isDarkTheme()) {
            Color.parseColor("#B3FFFFFF")
        } else {
            Color.parseColor("#80000000")
        }
    }

    /**
     * 获取卡片背景色
     */
    fun getCardBackgroundColor(): Int {
        return if (isDarkTheme()) {
            Color.parseColor("#1E1E1E")
        } else {
            Color.parseColor("#FFFFFF")
        }
    }

    /**
     * 获取分割线颜色
     */
    fun getDividerColor(): Int {
        return if (isDarkTheme()) {
            Color.parseColor("#333333")
        } else {
            Color.parseColor("#E0E0E0")
        }
    }

    /**
     * 获取阴影颜色
     */
    fun getShadowColor(): Int {
        return if (isDarkTheme()) {
            Color.parseColor("#40000000")
        } else {
            Color.parseColor("#1A000000")
        }
    }

    /**
     * 应用主题到Activity
     */
    fun applyToActivity(activity: android.app.Activity) {
        val currentTheme = getCurrentTheme()
        applyTheme(currentTheme)
        
        // 设置状态栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.statusBarColor = getStatusBarColor()
            activity.window.navigationBarColor = getNavigationBarColor()
        }
        
        // 设置状态栏文字颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val flags = activity.window.decorView.systemUiVisibility
            activity.window.decorView.systemUiVisibility = if (isDarkTheme()) {
                flags and android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                flags or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    /**
     * 创建主题相关的样式资源
     */
    fun createThemeStyles(): Map<String, Int> {
        return mapOf(
            "primaryColor" to getPrimaryColor(),
            "accentColor" to getAccentColor(),
            "backgroundColor" to getBackgroundColor(),
            "textColor" to getTextColor(),
            "secondaryTextColor" to getSecondaryTextColor(),
            "cardBackgroundColor" to getCardBackgroundColor(),
            "dividerColor" to getDividerColor(),
            "shadowColor" to getShadowColor(),
            "statusBarColor" to getStatusBarColor(),
            "navigationBarColor" to getNavigationBarColor()
        )
    }

    /**
     * 重置为默认主题
     */
    fun resetToDefault() {
        preferences.edit().clear().apply()
        setTheme(THEME_AUTO)
    }

    /**
     * 导出主题配置
     */
    fun exportThemeConfig(): String {
        val config = mapOf(
            "theme" to getCurrentTheme(),
            "primaryColor" to getPrimaryColor(),
            "accentColor" to getAccentColor(),
            "backgroundColor" to getBackgroundColor()
        )
        // Convert map to JSON string
        return config.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "\"$key\":\"$value\""
        }
    }

    /**
     * 导入主题配置
     */
    fun importThemeConfig(config: String): Boolean {
        return try {
            // 这里可以添加JSON反序列化逻辑
            // 暂时返回true表示成功
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 主题信息类
 */
data class ThemeInfo(
    val id: String,
    val name: String,
    val description: String,
    val previewColor: Int
)