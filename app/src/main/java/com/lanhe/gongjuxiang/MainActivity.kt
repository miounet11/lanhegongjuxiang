package com.lanhe.gongjuxiang

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lanhe.gongjuxiang.databinding.ActivityMainBinding
import com.lanhe.gongjuxiang.utils.ShizukuManager
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupBottomNavigation()
    }

    private fun setupNavigation() {
        // 设置Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "蓝河助手"
    }

    private fun setupBottomNavigation() {
        // 设置底部导航点击监听
        binding.bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // 切换到首页
                    true
                }
                R.id.nav_performance -> {
                    // 切换到性能页
                    true
                }
                R.id.nav_browser -> {
                    // 切换到浏览器页
                    true
                }
                R.id.nav_security -> {
                    // 切换到安全页
                    true
                }
                R.id.nav_settings -> {
                    // 切换到设置页
                    true
                }
                else -> false
            }
        }

        // 设置初始选中状态
        binding.bottomNavView.selectedItemId = R.id.nav_home
    }

    override fun onSupportNavigateUp(): Boolean {
        return super.onSupportNavigateUp()
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
