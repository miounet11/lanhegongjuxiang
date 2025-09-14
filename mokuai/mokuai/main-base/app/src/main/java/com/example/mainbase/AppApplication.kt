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

package com.example.mainbase

import android.app.Application
import android.util.Log
import com.hippo.ehviewer.module.database.DatabaseManager
import com.hippo.ehviewer.module.network.NetworkManager
import com.hippo.ehviewer.module.notification.NotificationManager
import com.hippo.ehviewer.module.settings.SettingsManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 应用Application类
 * 负责应用的初始化和全局配置
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@HiltAndroidApp
class AppApplication : Application() {

    private val TAG = AppApplication::class.java.simpleName

    // 注入的依赖
    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "MainBase Application starting...")

        try {
            // 初始化所有模块
            initModules()

            // 配置全局设置
            configureGlobalSettings()

            Log.i(TAG, "MainBase Application started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize application", e)
            // 在生产环境中，这里可能需要重启应用或显示错误界面
            throw RuntimeException("Application initialization failed", e)
        }
    }

    /**
     * 初始化所有模块
     * 按照依赖顺序依次初始化各个模块
     */
    private fun initModules() {
        Log.d(TAG, "Initializing modules...")

        try {
            // 1. 初始化工具模块（通常无依赖）
            Log.d(TAG, "Initializing utils module...")
            // Utils模块通常是静态方法，不需要显式初始化

            // 2. 初始化设置模块（依赖工具模块）
            Log.d(TAG, "Initializing settings module...")
            SettingsManager.getInstance(this)

            // 3. 初始化数据库模块（依赖工具模块）
            Log.d(TAG, "Initializing database module...")
            DatabaseManager.getInstance(this)

            // 4. 初始化网络模块（依赖工具和设置模块）
            Log.d(TAG, "Initializing network module...")
            NetworkManager.getInstance(this)

            // 5. 初始化通知模块（依赖工具模块）
            Log.d(TAG, "Initializing notification module...")
            NotificationManager.getInstance(this)

            // 6. 初始化其他模块...
            // 根据需要初始化其他模块

            Log.d(TAG, "All modules initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize modules", e)
            throw e
        }
    }

    /**
     * 配置全局设置
     */
    private fun configureGlobalSettings() {
        Log.d(TAG, "Configuring global settings...")

        try {
            // 设置应用级别配置
            settingsManager.putBoolean("debug_mode", BuildConfig.DEBUG)
            settingsManager.putString("app_version", BuildConfig.VERSION_NAME)
            settingsManager.putString("api_base_url", BuildConfig.API_BASE_URL)

            // 设置默认配置
            setupDefaultSettings()

            Log.d(TAG, "Global settings configured")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure global settings", e)
            throw e
        }
    }

    /**
     * 设置默认配置
     */
    private fun setupDefaultSettings() {
        // 网络设置
        if (!settingsManager.contains("network_timeout")) {
            settingsManager.putInt("network_timeout", 30000) // 30秒
        }

        // UI设置
        if (!settingsManager.contains("theme_mode")) {
            settingsManager.putString("theme_mode", "system") // 系统主题
        }

        if (!settingsManager.contains("language")) {
            settingsManager.putString("language", "zh-CN") // 默认中文
        }

        // 缓存设置
        if (!settingsManager.contains("cache_size")) {
            settingsManager.putInt("cache_size", 100) // 100MB
        }

        // 其他默认设置...
    }

    /**
     * 获取应用实例
     */
    companion object {
        private lateinit var instance: AppApplication

        fun getInstance(): AppApplication {
            return instance
        }
    }

    init {
        instance = this
    }

    /**
     * 应用终止时的清理工作
     */
    override fun onTerminate() {
        super.onTerminate()

        Log.d(TAG, "Application terminating...")

        try {
            // 清理各个模块
            cleanupModules()

            Log.d(TAG, "Application terminated")

        } catch (e: Exception) {
            Log.e(TAG, "Error during application termination", e)
        }
    }

    /**
     * 清理模块资源
     */
    private fun cleanupModules() {
        try {
            // 清理网络模块
            NetworkManager.getInstance(this).cleanup()

            // 清理数据库模块
            DatabaseManager.getInstance(this).close()

            // 清理通知模块
            NotificationManager.getInstance(this).cleanup()

        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up modules", e)
        }
    }

    /**
     * 内存不足时的处理
     */
    override fun onLowMemory() {
        super.onLowMemory()

        Log.w(TAG, "Low memory detected")

        try {
            // 通知各个模块进行内存清理
            NetworkManager.getInstance(this).cleanup()
            DatabaseManager.getInstance(this).cleanup()

        } catch (e: Exception) {
            Log.e(TAG, "Error handling low memory", e)
        }
    }

    /**
     * 应用进入后台
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        Log.d(TAG, "Trim memory level: $level")

        when (level) {
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // 应用仍在运行，但系统内存不足
                try {
                    // 执行轻量级清理
                    DatabaseManager.getInstance(this).cleanup()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during light cleanup", e)
                }
            }

            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                // 应用进入后台，执行深度清理
                try {
                    cleanupModules()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during deep cleanup", e)
                }
            }
        }
    }
}
