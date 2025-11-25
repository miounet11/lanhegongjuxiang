package com.lanhe.gongjuxiang.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.activities.*
import com.lanhe.gongjuxiang.databinding.FragmentSecurityBinding
import com.lanhe.gongjuxiang.utils.SecurityManager
import com.lanhe.gongjuxiang.utils.ShimmerHelper
import com.lanhe.gongjuxiang.utils.ShizukuManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 安全中心Fragment
 * 提供系统安全检测和权限管理功能
 */
class SecurityFragment : Fragment() {

    private var _binding: FragmentSecurityBinding? = null
    private val binding get() = _binding!!
    private lateinit var securityManager: SecurityManager
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        securityManager = SecurityManager(requireContext())
        setupSwipeRefresh()
        setupClickListeners()
        loadDataWithShimmer()
    }

    private fun setupClickListeners() {
        // Shizuku权限管理 - 通过状态卡片点击
        binding.shizukuStatusItem.setOnClickListener {
            startActivity(Intent(context, ShizukuAuthActivity::class.java))
        }

        // 应用权限管理
        binding.cardAppManager.setOnClickListener {
            startActivity(Intent(context, AppManagerActivity::class.java))
        }

        // 系统优化
        binding.cardCoreOptimization.setOnClickListener {
            startActivity(Intent(context, CoreOptimizationActivity::class.java))
        }

        // 电磁管理
        binding.cardElectromagneticManager.setOnClickListener {
            startActivity(Intent(context, ElectromagneticManagerActivity::class.java))
        }

        // 通知管理
        binding.cardNotificationManager.setOnClickListener {
            startActivity(Intent(context, NotificationManagerActivity::class.java))
        }

        // 主题设置
        binding.cardThemeSettings.setOnClickListener {
            startActivity(Intent(context, ThemeSettingsActivity::class.java))
        }

        // 一键安全检查
        binding.btnSecurityCheck.setOnClickListener {
            performSecurityCheck()
        }

        // Shizuku状态检查 - 整合到权限认证项目
        binding.permissionAuthItem.setOnClickListener {
            checkShizukuStatus()
        }
    }

    private fun updateSecurityStatus() {
        lifecycleScope.launch {
            // 更新Shizuku状态
            updateShizukuStatus()
            
            // 更新系统安全状态
            updateSystemSecurityStatus()
            
            // 更新权限状态
            updatePermissionStatus()
        }
    }

    private fun updateShizukuStatus() {
        val isShizukuAvailable = ShizukuManager.isShizukuAvailable()
        
        binding.tvShizukuStatus.text = if (isShizukuAvailable) {
            "Shizuku已连接"
        } else {
            "Shizuku未连接"
        }
        
        binding.tvShizukuStatus.setTextColor(
            if (isShizukuAvailable) {
                resources.getColor(android.R.color.holo_green_dark, null)
            } else {
                resources.getColor(android.R.color.holo_red_dark, null)
            }
        )
    }

    private fun updateSystemSecurityStatus() {
        lifecycleScope.launch {
            val securityScore = securityManager.calculateSecurityScore()
            
            binding.tvSecurityScore.text = "${securityScore}"

            // 更新状态芯片颜色
            val statusColor = when {
                securityScore >= 90 -> R.color.status_success
                securityScore >= 70 -> android.R.color.holo_blue_dark
                securityScore >= 50 -> R.color.status_warning
                else -> android.R.color.holo_red_dark
            }

            // 更新Shizuku状态芯片
            binding.chipShizukuStatus.setChipBackgroundColorResource(statusColor)
        }
    }

    private fun updatePermissionStatus() {
        val riskAppsCount = securityManager.getRiskApplicationsCount()
        val suspiciousPermissionsCount = securityManager.getSuspiciousPermissionsCount()
        
        binding.tvRiskApps.text = "${riskAppsCount}"
        binding.tvPermissionCount.text = "${suspiciousPermissionsCount}"

        // 根据风险状态更新UI颜色
        if (riskAppsCount > 0) {
            binding.tvRiskApps.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        } else {
            binding.tvRiskApps.setTextColor(resources.getColor(R.color.status_success, null))
        }
    }

    private fun performSecurityCheck() {
        binding.btnSecurityCheck.isEnabled = false
        binding.btnSecurityCheck.text = "检查中..."
        
        lifecycleScope.launch {
            try {
                securityManager.performFullSecurityScan()
                updateSecurityStatus()
                
                binding.btnSecurityCheck.text = "检查完成"
                
                // 2秒后恢复按钮状态
                kotlinx.coroutines.delay(2000)
                binding.btnSecurityCheck.text = "一键安全检查"
                binding.btnSecurityCheck.isEnabled = true
                
            } catch (e: Exception) {
                binding.btnSecurityCheck.text = "检查失败"
                binding.btnSecurityCheck.isEnabled = true
            }
        }
    }

    private fun checkShizukuStatus() {
        updateShizukuStatus()
        
        if (!ShizukuManager.isShizukuAvailable()) {
            // 引导用户设置Shizuku
            startActivity(Intent(context, ShizukuAuthActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateSecurityStatus()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout?.setOnRefreshListener {
            refreshSecurityData()
        }

        binding.swipeRefreshLayout?.setColorSchemeResources(
            R.color.md_theme_light_primary,
            R.color.md_theme_light_secondary,
            R.color.md_theme_light_tertiary
        )
    }

    private fun loadDataWithShimmer() {
        if (isLoading) return

        isLoading = true
        showShimmerEffect(true)

        lifecycleScope.launch {
            // 模拟加载延迟
            delay(1200)

            updateSecurityStatus()
            showShimmerEffect(false)
            isLoading = false
        }
    }

    private fun refreshSecurityData() {
        lifecycleScope.launch {
            // 刷新安全数据
            delay(800)
            updateSecurityStatus()
            binding.swipeRefreshLayout?.isRefreshing = false
        }
    }

    private fun showShimmerEffect(show: Boolean) {
        // Shimmer views were removed during layout refactoring
        // The hero card is now always visible
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}