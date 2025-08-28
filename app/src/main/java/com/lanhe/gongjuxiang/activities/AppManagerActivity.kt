package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityAppManagerBinding

class AppManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppManagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        setupBackPress()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "应用管理"
        }
    }

    private fun setupClickListeners() {
        // 应用信息
        binding.btnAppInfo.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.fromParts("package", packageName, null)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开应用信息", Toast.LENGTH_SHORT).show()
            }
        }

        // 应用权限
        binding.btnAppPermissions.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.fromParts("package", packageName, null)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开应用权限", Toast.LENGTH_SHORT).show()
            }
        }

        // 默认应用设置
        binding.btnDefaultApps.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开默认应用设置", Toast.LENGTH_SHORT).show()
            }
        }

        // 应用列表
        binding.btnAppList.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开应用列表", Toast.LENGTH_SHORT).show()
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
