package com.lanhe.gongjuxiang.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.ChromiumBrowserActivity
import com.lanhe.gongjuxiang.databinding.FragmentMyBinding

class MyFragment : Fragment() {

    private var _binding: FragmentMyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 关于我们
        binding.llAboutUs.setOnClickListener {
            ChromiumBrowserActivity.openUrl(
                requireContext(),
                "https://github.com/lanhe/toolbox"
            )
        }

        // 使用帮助
        binding.llHelp.setOnClickListener {
            ChromiumBrowserActivity.openUrl(
                requireContext(),
                "https://github.com/lanhe/toolbox/wiki"
            )
        }

        // 意见反馈
        binding.llFeedback.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:feedback@lanhe.com")
                intent.putExtra(Intent.EXTRA_SUBJECT, "蓝河工具箱反馈")
                intent.putExtra(Intent.EXTRA_TEXT, "请在此处描述您的问题或建议...")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开邮件应用", Toast.LENGTH_SHORT).show()
            }
        }

        // 检查更新
        binding.llCheckUpdate.setOnClickListener {
            try {
                // 这里可以添加检查更新的逻辑
                Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "检查更新失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
