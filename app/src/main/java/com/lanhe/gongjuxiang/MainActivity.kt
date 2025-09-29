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
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == ShizukuManager.SHIZUKU_PERMISSION_REQUEST_CODE) {
            // Shizuku权限请求结果会通过Shizuku的回调自动处理
            // 这里不需要额外处理，ShizukuManager会自动更新状态
        }
    }
}
