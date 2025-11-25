package com.lanhe.gongjuxiang.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 蓝河浏览器独立启动器
 *
 * 功能:
 * - 提供桌面快捷方式入口
 * - 直接启动浏览器主Activity
 * - 显示为"蓝河浏览器"应用
 */
class BrowserLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 直接启动浏览器,不显示任何UI
        val intent = Intent(this, ChromiumBrowserActivity::class.java).apply {
            // 传递启动参数
            action = Intent.ACTION_VIEW
            // 如果有URL数据,传递过去
            data?.let { uri ->
                putExtra("url", uri.toString())
            }
            // 清除启动器Activity从任务栈
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        startActivity(intent)
        finish() // 立即关闭启动器
    }
}
