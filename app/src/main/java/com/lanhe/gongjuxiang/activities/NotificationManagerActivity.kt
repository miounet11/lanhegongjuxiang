package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.databinding.ActivityNotificationManagerBinding

class NotificationManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationManagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationManagerBinding.inflate(layoutInflater)
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
            title = "通知管理"
        }
    }

    private fun setupClickListeners() {
        // 通知访问设置
        binding.btnNotificationAccess.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开通知访问设置", Toast.LENGTH_SHORT).show()
            }
        }

        // 通知权限设置
        binding.btnNotificationPermission.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开通知权限设置", Toast.LENGTH_SHORT).show()
            }
        }

        // 勿扰模式设置
        binding.btnDoNotDisturb.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开勿扰模式设置", Toast.LENGTH_SHORT).show()
            }
        }

        // 应用通知设置
        binding.btnAppNotifications.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开应用通知设置", Toast.LENGTH_SHORT).show()
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
