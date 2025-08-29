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
        // 设置BottomNavigationView的选中监听器
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_functions -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.navigation_advanced -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                R.id.navigation_my -> {
                    binding.viewPager.currentItem = 2
                    true
                }
                else -> false
            }
        }

        // 设置ViewPager2的页面变化监听器来同步BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomNavigation.menu.getItem(position).isChecked = true
            }
        })

        // 设置初始选中状态
        binding.bottomNavigation.selectedItemId = R.id.navigation_functions
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