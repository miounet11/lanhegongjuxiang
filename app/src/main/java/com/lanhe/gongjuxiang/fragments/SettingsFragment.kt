package com.lanhe.gongjuxiang.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.*
import com.lanhe.gongjuxiang.databinding.FragmentSettingsBinding

/**
 * 设置Fragment
 * 提供应用设置和系统设置入口
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 应用设置
        binding.cardAppSettings.setOnClickListener {
            startActivity(Intent(context, SettingsActivity::class.java))
        }

        // 关于应用
        binding.cardAbout.setOnClickListener {
            startActivity(Intent(context, AboutActivity::class.java))
        }

        // 帮助中心
        binding.cardHelp.setOnClickListener {
            startActivity(Intent(context, HelpActivity::class.java))
        }

        // 反馈建议
        binding.cardFeedback.setOnClickListener {
            startActivity(Intent(context, FeedbackActivity::class.java))
        }

        // 主题设置
        binding.cardThemeSettings.setOnClickListener {
            startActivity(Intent(context, ThemeSettingsActivity::class.java))
        }

        // 浏览器设置
        binding.cardBrowserSettings.setOnClickListener {
            // 启动Chromium浏览器
            startActivity(Intent(context, ChromiumBrowserActivity::class.java))
        }

        // 快捷设置
        binding.cardQuickSettings.setOnClickListener {
            startActivity(Intent(context, QuickSettingsActivity::class.java))
        }

        // 更新检查
        binding.cardUpdate.setOnClickListener {
            startActivity(Intent(context, UpdateActivity::class.java))
        }

        // 测试页面
        binding.cardTest.setOnClickListener {
            startActivity(Intent(context, TestActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}