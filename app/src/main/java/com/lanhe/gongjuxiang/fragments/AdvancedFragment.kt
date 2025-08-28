package com.lanhe.gongjuxiang.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.AppManagerActivity
import com.lanhe.gongjuxiang.databinding.FragmentAdvancedBinding
import com.lanhe.gongjuxiang.utils.SystemUtils

class AdvancedFragment : Fragment() {

    private var _binding: FragmentAdvancedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdvancedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupSwitches()
    }

    private fun setupClickListeners() {
        // Shizuku授权
        binding.llShizukuAuthorization.setOnClickListener {
            try {
                // 检查Shizuku是否安装
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://github.com/RikkaApps/Shizuku")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开Shizuku下载页面", Toast.LENGTH_SHORT).show()
            }
        }

        // 使用指南
        binding.llUsageGuide.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://github.com/lanhe/toolbox")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开使用指南", Toast.LENGTH_SHORT).show()
            }
        }

        // 手机改名
        binding.llChangePhoneName.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开系统设置", Toast.LENGTH_SHORT).show()
            }
        }

        // 彩色充电动画
        binding.llColorfulChargingAnimation.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开系统设置", Toast.LENGTH_SHORT).show()
            }
        }

        // 清除更新提醒
        binding.llClearUpdateReminders.setOnClickListener {
            try {
                SystemUtils.clearUpdateReminders(requireContext())
                Toast.makeText(context, "更新提醒已清除", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "清除失败，需要系统权限", Toast.LENGTH_SHORT).show()
            }
        }

        // 暂停(冻结)应用
        binding.llFreezeApplications.setOnClickListener {
            try {
                val intent = Intent(requireContext(), AppManagerActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开应用管理", Toast.LENGTH_SHORT).show()
            }
        }

        // 内存管理
        binding.llMemoryManagement.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开系统设置", Toast.LENGTH_SHORT).show()
            }
        }

        // 开发者选项
        binding.llDeveloperOptions.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开开发者选项", Toast.LENGTH_SHORT).show()
            }
        }

        // 隐藏系统通知
        binding.llHideSystemNotifications.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开通知访问设置", Toast.LENGTH_SHORT).show()
            }
        }

        // 设备控制器
        binding.llDeviceController.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "无法打开系统设置", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSwitches() {
        // 强开直驱供电开关
        binding.switchForceDirectPower.setOnCheckedChangeListener { _, isChecked ->
            try {
                if (isChecked) {
                    SystemUtils.enableDirectPower(requireContext())
                    Toast.makeText(context, "直驱供电已开启", Toast.LENGTH_SHORT).show()
                } else {
                    SystemUtils.disableDirectPower(requireContext())
                    Toast.makeText(context, "直驱供电已关闭", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.switchForceDirectPower.isChecked = !isChecked
                Toast.makeText(context, "操作失败，需要Shizuku权限", Toast.LENGTH_SHORT).show()
            }
        }

        // 开启全局插帧开关
        binding.switchGlobalFrameInsertion.setOnCheckedChangeListener { _, isChecked ->
            try {
                if (isChecked) {
                    SystemUtils.enableGlobalFrameInsertion(requireContext())
                    Toast.makeText(context, "全局插帧已开启", Toast.LENGTH_SHORT).show()
                } else {
                    SystemUtils.disableGlobalFrameInsertion(requireContext())
                    Toast.makeText(context, "全局插帧已关闭", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.switchGlobalFrameInsertion.isChecked = !isChecked
                Toast.makeText(context, "操作失败，需要Shizuku权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
