package com.lanhe.gongjuxiang.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.BrowserActivity
import com.lanhe.gongjuxiang.activities.BrowserSettingsActivity
import com.lanhe.gongjuxiang.browser.YcWebViewBrowser
import com.lanhe.gongjuxiang.databinding.FragmentBrowserBinding

/**
 * 浏览器Fragment
 * 提供浏览器功能入口和设置
 */
class BrowserFragment : Fragment() {

    private var _binding: FragmentBrowserBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        loadQuickAccess()
    }

    private fun setupClickListeners() {
        // 启动浏览器
        binding.btnLaunchBrowser.setOnClickListener {
            YcWebViewBrowser.start(requireContext())
        }

        // 浏览器设置
        binding.btnBrowserSettings.setOnClickListener {
            startActivity(Intent(context, BrowserSettingsActivity::class.java))
        }

        // 新标签页功能已集成到浏览器设置中
        // binding.cardNewTab 已移除，功能整合到启动浏览器按钮

        // 快速搜索
        binding.btnQuickSearch.setOnClickListener {
            val query = binding.etQuickSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                val searchUrl = "https://www.google.com/search?q=${query}"
                val intent = Intent(context, BrowserActivity::class.java)
                intent.putExtra("url", searchUrl)
                startActivity(intent)
            }
        }

        // 快速访问网站
        binding.cardBaidu.setOnClickListener {
            openUrl("https://www.baidu.com")
        }

        binding.cardTaobao.setOnClickListener {
            openUrl("https://www.taobao.com")
        }

        binding.cardWeibo.setOnClickListener {
            openUrl("https://weibo.com")
        }

        binding.cardBilibili.setOnClickListener {
            openUrl("https://www.bilibili.com")
        }

        binding.cardGithub.setOnClickListener {
            openUrl("https://github.com")
        }

        binding.cardYoutube.setOnClickListener {
            openUrl("https://www.youtube.com")
        }
    }

    private fun loadQuickAccess() {
        // 这里可以加载用户的快速访问网站
        // 当前显示默认的常用网站
    }

    private fun openUrl(url: String) {
        val intent = Intent(context, BrowserActivity::class.java)
        intent.putExtra("url", url)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}