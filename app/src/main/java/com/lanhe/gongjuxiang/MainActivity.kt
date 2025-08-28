package com.lanhe.gongjuxiang

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.lanhe.gongjuxiang.databinding.ActivityMainBinding
import com.lanhe.gongjuxiang.fragments.AdvancedFragment
import com.lanhe.gongjuxiang.fragments.FunctionsFragment
import com.lanhe.gongjuxiang.fragments.MyFragment
import com.lanhe.gongjuxiang.utils.ShizukuManager
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupBottomNavigation()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> FunctionsFragment()
                    1 -> AdvancedFragment()
                    2 -> MyFragment()
                    else -> FunctionsFragment()
                }
            }
        }

        // 禁用ViewPager2的滑动
        binding.viewPager.isUserInputEnabled = false
    }

    private fun setupBottomNavigation() {
        // 设置初始选中状态
        updateTabSelection(0)

        // 功能标签点击
        binding.tabFunctions.setOnClickListener {
            binding.viewPager.currentItem = 0
            updateTabSelection(0)
        }

        // 高级标签点击
        binding.tabAdvanced.setOnClickListener {
            binding.viewPager.currentItem = 1
            updateTabSelection(1)
        }

        // 我的标签点击
        binding.tabMy.setOnClickListener {
            binding.viewPager.currentItem = 2
            updateTabSelection(2)
        }
    }

    private fun updateTabSelection(selectedPosition: Int) {
        // 重置所有标签状态
        binding.ivFunctions.setColorFilter(resources.getColor(R.color.nav_unselected, null))
        binding.ivAdvanced.setColorFilter(resources.getColor(R.color.nav_unselected, null))
        binding.ivMy.setColorFilter(resources.getColor(R.color.nav_unselected, null))

        // 重置所有文本颜色
        binding.tabFunctions.findViewById<android.widget.TextView>(0)?.setTextColor(
            resources.getColor(R.color.nav_unselected, null)
        )
        binding.tabAdvanced.findViewById<android.widget.TextView>(0)?.setTextColor(
            resources.getColor(R.color.nav_unselected, null)
        )
        binding.tabMy.findViewById<android.widget.TextView>(0)?.setTextColor(
            resources.getColor(R.color.nav_unselected, null)
        )

        // 设置选中标签状态
        when (selectedPosition) {
            0 -> {
                binding.ivFunctions.setColorFilter(resources.getColor(R.color.nav_selected, null))
                binding.tabFunctions.findViewById<android.widget.TextView>(0)?.setTextColor(
                    resources.getColor(R.color.nav_selected, null)
                )
            }
            1 -> {
                binding.ivAdvanced.setColorFilter(resources.getColor(R.color.nav_selected, null))
                binding.tabAdvanced.findViewById<android.widget.TextView>(0)?.setTextColor(
                    resources.getColor(R.color.nav_selected, null)
                )
            }
            2 -> {
                binding.ivMy.setColorFilter(resources.getColor(R.color.nav_selected, null))
                binding.tabMy.findViewById<android.widget.TextView>(0)?.setTextColor(
                    resources.getColor(R.color.nav_selected, null)
                )
            }
        }
    }

    // 处理Shizuku权限请求结果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == ShizukuManager.SHIZUKU_PERMISSION_REQUEST_CODE) {
            // Shizuku权限请求结果会通过Shizuku的回调自动处理
            // 这里不需要额外处理，ShizukuManager会自动更新状态
        }
    }
}