/*
 * Copyright 2024 LanHe Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.mainbase.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.example.mainbase.R
import com.example.mainbase.databinding.ActivityMainBinding
import com.hippo.ehviewer.module.network.NetworkManager
import com.hippo.ehviewer.module.network.interfaces.INetworkCallback
import com.hippo.ehviewer.module.settings.SettingsManager
import com.hippo.ehviewer.module.ui.BaseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 主Activity
 * 应用的入口Activity，演示各个模块的使用
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private val TAG = MainActivity::class.java.simpleName

    // View Binding
    private lateinit var binding: ActivityMainBinding

    // ViewModel
    private val viewModel: MainViewModel by viewModels()

    // 注入的依赖
    @Inject
    lateinit var networkManager: NetworkManager

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "MainActivity created")

        // 设置View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化视图
        initViews()

        // 设置观察者
        setupObservers()

        // 加载初始数据
        loadInitialData()
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        Log.d(TAG, "Initializing views")

        // 设置标题
        setToolbarTitle(getString(R.string.app_name))

        // 设置按钮点击事件
        binding.btnNetworkTest.setOnClickListener {
            testNetworkModule()
        }

        binding.btnDatabaseTest.setOnClickListener {
            testDatabaseModule()
        }

        binding.btnSettingsTest.setOnClickListener {
            testSettingsModule()
        }

        binding.btnUiTest.setOnClickListener {
            testUIModule()
        }

        // 显示欢迎信息
        showWelcomeMessage()
    }

    /**
     * 设置观察者
     */
    private fun setupObservers() {
        // 观察ViewModel的数据变化
        viewModel.networkTestResult.observe(this, Observer { result ->
            binding.tvNetworkResult.text = result
        })

        viewModel.databaseTestResult.observe(this, Observer { result ->
            binding.tvDatabaseResult.text = result
        })

        viewModel.settingsTestResult.observe(this, Observer { result ->
            binding.tvSettingsResult.text = result
        })
    }

    /**
     * 加载初始数据
     */
    private fun loadInitialData() {
        // 显示应用信息
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        binding.tvAppInfo.text = getString(R.string.app_info_format, versionName)

        // 检查网络状态
        checkNetworkStatus()
    }

    /**
     * 显示欢迎信息
     */
    private fun showWelcomeMessage() {
        showMessage(getString(R.string.welcome_message))
    }

    /**
     * 检查网络状态
     */
    private fun checkNetworkStatus() {
        val networkManager = NetworkManager.getInstance(this)
        val isNetworkAvailable = networkManager.isNetworkAvailable(this)

        binding.tvNetworkStatus.text = if (isNetworkAvailable) {
            getString(R.string.network_available)
        } else {
            getString(R.string.network_unavailable)
        }
    }

    /**
     * 测试网络模块
     */
    private fun testNetworkModule() {
        Log.d(TAG, "Testing network module")

        showLoadingDialog(getString(R.string.testing_network))

        // 发送测试请求
        networkManager.get("${BuildConfig.API_BASE_URL}/test")
            .enqueue(object : INetworkCallback<String> {
                override fun onSuccess(result: String) {
                    runOnUiThread {
                        hideLoadingDialog()
                        viewModel.setNetworkTestResult("网络测试成功: $result")
                        showMessage(getString(R.string.network_test_success))
                    }
                }

                override fun onFailure(error: Exception) {
                    runOnUiThread {
                        hideLoadingDialog()
                        viewModel.setNetworkTestResult("网络测试失败: ${error.message}")
                        showError(getString(R.string.network_test_failed))
                    }
                }

                override fun onCancel() {
                    runOnUiThread {
                        hideLoadingDialog()
                        viewModel.setNetworkTestResult("网络测试取消")
                    }
                }

                override fun onProgress(progress: Int, message: String) {
                    runOnUiThread {
                        // 更新进度（如果需要）
                    }
                }
            })
    }

    /**
     * 测试数据库模块
     */
    private fun testDatabaseModule() {
        Log.d(TAG, "Testing database module")

        try {
            // 这里可以测试数据库操作
            // 例如：插入、查询、更新、删除数据

            viewModel.setDatabaseTestResult("数据库测试成功")
            showMessage(getString(R.string.database_test_success))

        } catch (e: Exception) {
            Log.e(TAG, "Database test failed", e)
            viewModel.setDatabaseTestResult("数据库测试失败: ${e.message}")
            showError(getString(R.string.database_test_failed))
        }
    }

    /**
     * 测试设置模块
     */
    private fun testSettingsModule() {
        Log.d(TAG, "Testing settings module")

        try {
            // 测试设置的读写
            val testKey = "test_key"
            val testValue = "test_value_${System.currentTimeMillis()}"

            // 写入设置
            settingsManager.putString(testKey, testValue)

            // 读取设置
            val readValue = settingsManager.getString(testKey, "")

            if (testValue == readValue) {
                viewModel.setSettingsTestResult("设置测试成功: $readValue")
                showMessage(getString(R.string.settings_test_success))
            } else {
                viewModel.setSettingsTestResult("设置测试失败: 值不匹配")
                showError(getString(R.string.settings_test_failed))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Settings test failed", e)
            viewModel.setSettingsTestResult("设置测试失败: ${e.message}")
            showError(getString(R.string.settings_test_failed))
        }
    }

    /**
     * 测试UI模块
     */
    private fun testUIModule() {
        Log.d(TAG, "Testing UI module")

        try {
            // 测试UI组件功能
            // 例如：显示对话框、切换主题等

            showMessage(getString(R.string.ui_test_success))

        } catch (e: Exception) {
            Log.e(TAG, "UI test failed", e)
            showError(getString(R.string.ui_test_failed))
        }
    }

    /**
     * 处理菜单项点击
     */
    override fun onMenuItemSelected(itemId: Int): Boolean {
        return when (itemId) {
            R.id.menu_settings -> {
                // 打开设置页面
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.menu_about -> {
                // 显示关于对话框
                showAboutDialog()
                true
            }
            else -> super.onMenuItemSelected(itemId)
        }
    }

    /**
     * 显示关于对话框
     */
    private fun showAboutDialog() {
        // 这里可以显示应用的关于信息
        showMessage("MainBase v${BuildConfig.VERSION_NAME}")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity destroyed")
    }
}
