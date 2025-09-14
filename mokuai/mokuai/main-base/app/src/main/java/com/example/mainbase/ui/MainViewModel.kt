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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hippo.ehviewer.module.network.NetworkManager
import com.hippo.ehviewer.module.network.interfaces.INetworkCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主ViewModel
 * 管理MainActivity的数据和业务逻辑
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val networkManager: NetworkManager
) : ViewModel() {

    private val TAG = MainViewModel::class.java.simpleName

    // 网络测试结果
    private val _networkTestResult = MutableLiveData<String>()
    val networkTestResult: LiveData<String> = _networkTestResult

    // 数据库测试结果
    private val _databaseTestResult = MutableLiveData<String>()
    val databaseTestResult: LiveData<String> = _databaseTestResult

    // 设置测试结果
    private val _settingsTestResult = MutableLiveData<String>()
    val settingsTestResult: LiveData<String> = _settingsTestResult

    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        Log.d(TAG, "MainViewModel created")
    }

    /**
     * 设置网络测试结果
     */
    fun setNetworkTestResult(result: String) {
        _networkTestResult.value = result
    }

    /**
     * 设置数据库测试结果
     */
    fun setDatabaseTestResult(result: String) {
        _databaseTestResult.value = result
    }

    /**
     * 设置设置测试结果
     */
    fun setSettingsTestResult(result: String) {
        _settingsTestResult.value = result
    }

    /**
     * 设置加载状态
     */
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    /**
     * 执行网络测试
     */
    fun performNetworkTest(baseUrl: String) {
        viewModelScope.launch {
            setLoading(true)

            try {
                networkManager.get("$baseUrl/test")
                    .enqueue(object : INetworkCallback<String> {
                        override fun onSuccess(result: String) {
                            setNetworkTestResult("网络测试成功: $result")
                            setLoading(false)
                        }

                        override fun onFailure(error: Exception) {
                            setNetworkTestResult("网络测试失败: ${error.message}")
                            setLoading(false)
                        }

                        override fun onCancel() {
                            setNetworkTestResult("网络测试取消")
                            setLoading(false)
                        }
                    })
            } catch (e: Exception) {
                Log.e(TAG, "Network test error", e)
                setNetworkTestResult("网络测试错误: ${e.message}")
                setLoading(false)
            }
        }
    }

    /**
     * 执行数据库测试
     */
    fun performDatabaseTest() {
        viewModelScope.launch {
            setLoading(true)

            try {
                // 这里可以执行数据库操作
                // 例如：增删改查操作

                // 模拟数据库操作
                kotlinx.coroutines.delay(1000)

                setDatabaseTestResult("数据库测试成功")
                setLoading(false)

            } catch (e: Exception) {
                Log.e(TAG, "Database test error", e)
                setDatabaseTestResult("数据库测试失败: ${e.message}")
                setLoading(false)
            }
        }
    }

    /**
     * 执行设置测试
     */
    fun performSettingsTest() {
        viewModelScope.launch {
            setLoading(true)

            try {
                // 这里可以执行设置操作
                // 例如：读写设置项

                // 模拟设置操作
                kotlinx.coroutines.delay(500)

                setSettingsTestResult("设置测试成功")
                setLoading(false)

            } catch (e: Exception) {
                Log.e(TAG, "Settings test error", e)
                setSettingsTestResult("设置测试失败: ${e.message}")
                setLoading(false)
            }
        }
    }

    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "MainViewModel cleared")
    }
}
