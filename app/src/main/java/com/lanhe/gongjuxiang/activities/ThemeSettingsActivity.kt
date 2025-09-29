package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityThemeSettingsBinding
import com.lanhe.gongjuxiang.utils.AnimationUtils

class ThemeSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupThemeOptions()
        updateCurrentTheme()
        setupBackPress()
    }

    private fun setupToolbar() {
        // 使用系统默认的ActionBar而不是自定义Toolbar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "🎨 主题设置"
        }
    }

    private fun setupThemeOptions() {
        // 白天模式
        binding.cardLightTheme.setOnClickListener {
            AnimationUtils.buttonPressFeedback(it)
            setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
            Toast.makeText(this, "已切换到白天模式", Toast.LENGTH_SHORT).show()
        }

        // 黑夜模式
        binding.cardDarkTheme.setOnClickListener {
            AnimationUtils.buttonPressFeedback(it)
            setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
            Toast.makeText(this, "已切换到黑夜模式", Toast.LENGTH_SHORT).show()
        }

        // 跟随系统
        binding.cardSystemTheme.setOnClickListener {
            AnimationUtils.buttonPressFeedback(it)
            setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            Toast.makeText(this, "已设置为跟随系统", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setThemeMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        // 延迟一点时间再更新UI，确保主题切换完成
        binding.root.postDelayed({
            updateCurrentTheme()
        }, 100)
    }

    private fun updateCurrentTheme() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK

        // 清除所有选中状态
        binding.cardLightTheme.strokeWidth = 0
        binding.cardDarkTheme.strokeWidth = 0
        binding.cardSystemTheme.strokeWidth = 0

        // 设置当前选中状态
        when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                binding.cardLightTheme.strokeWidth = 4
                binding.cardLightTheme.strokeColor = getColor(R.color.primary)
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                binding.cardDarkTheme.strokeWidth = 4
                binding.cardDarkTheme.strokeColor = getColor(R.color.primary)
            }
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                binding.cardSystemTheme.strokeWidth = 4
                binding.cardSystemTheme.strokeColor = getColor(R.color.primary)
            }
            else -> {
                // 根据实际的夜间模式状态
                when (currentNightMode) {
                    android.content.res.Configuration.UI_MODE_NIGHT_YES -> {
                        binding.cardDarkTheme.strokeWidth = 4
                        binding.cardDarkTheme.strokeColor = getColor(R.color.primary)
                    }
                    android.content.res.Configuration.UI_MODE_NIGHT_NO -> {
                        binding.cardLightTheme.strokeWidth = 4
                        binding.cardLightTheme.strokeColor = getColor(R.color.primary)
                    }
                }
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
