package com.lanhe.gongjuxiang.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lanhe.gongjuxiang.utils.PreferencesManager
import com.lanhe.gongjuxiang.utils.ShizukuManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Hilt集成测试Activity
 * 用于验证依赖注入是否正常工作
 */
@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var shizukuManager: ShizukuManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 验证注入是否成功
        val isInjected = ::preferencesManager.isInitialized && ::shizukuManager.isInitialized

        if (isInjected) {
            println("✅ Hilt注入成功!")
            println("PreferencesManager: $preferencesManager")
            println("ShizukuManager: $shizukuManager")

            // 测试使用注入的依赖
            // 暂时注释掉不存在的方法
            // val theme = preferencesManager.getThemeMode()
            val isShizukuAvailable = shizukuManager.isShizukuAvailable()

            println("PreferencesManager已注入")
            println("Shizuku可用: $isShizukuAvailable")
        } else {
            println("❌ Hilt注入失败!")
        }

        finish() // 测试完成后关闭
    }
}