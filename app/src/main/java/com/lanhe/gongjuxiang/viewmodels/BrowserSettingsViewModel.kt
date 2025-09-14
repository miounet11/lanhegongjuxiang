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

package com.lanhe.gongjuxiang.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
// TODO: 暂时注释掉mokuai模块导入
// import com.hippo.ehviewer.module.settings.SettingsManager

/**
 * 浏览器设置ViewModel
 * 管理浏览器设置相关的数据和业务逻辑
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
class BrowserSettingsViewModel : ViewModel() {

    private val TAG = BrowserSettingsViewModel::class.java.simpleName

    // 设置项数据
    private val _settingsItems = MutableLiveData<List<SettingsItem>>()
    val settingsItems: LiveData<List<SettingsItem>> = _settingsItems

    init {
        Log.d(TAG, "BrowserSettingsViewModel created")
    }

    /**
     * 加载设置数据
     */
    fun loadSettingsData() {
        val items = listOf(
            // 常规设置
            SettingsItem(
                id = "homepage",
                title = "主页设置",
                subtitle = "设置浏览器默认主页",
                type = SettingsItem.TYPE_CLICKABLE,
                iconRes = android.R.drawable.ic_menu_view
            ),

            SettingsItem(
                id = "adblock",
                title = "广告拦截",
                subtitle = "拦截网页广告，提升浏览体验",
                type = SettingsItem.TYPE_SWITCH,
                isChecked = true, // TODO: 从本地设置管理器获取
                iconRes = android.R.drawable.ic_menu_delete
            ),

            SettingsItem(
                id = "javascript",
                title = "JavaScript",
                subtitle = "启用JavaScript脚本支持",
                type = SettingsItem.TYPE_SWITCH,
                isChecked = true, // TODO: 从本地设置管理器获取
                iconRes = android.R.drawable.ic_menu_set_as
            ),

            SettingsItem(
                id = "images",
                title = "图片加载",
                subtitle = "加载网页中的图片内容",
                type = SettingsItem.TYPE_SWITCH,
                isChecked = true, // TODO: 从本地设置管理器获取
                iconRes = android.R.drawable.ic_menu_gallery
            ),

            // 数据管理
            SettingsItem(
                id = "cache",
                title = "缓存管理",
                subtitle = "管理浏览器缓存和临时文件",
                type = SettingsItem.TYPE_CLICKABLE,
                iconRes = android.R.drawable.ic_menu_save
            ),

            SettingsItem(
                id = "privacy",
                title = "隐私设置",
                subtitle = "管理隐私和安全设置",
                type = SettingsItem.TYPE_CLICKABLE,
                iconRes = android.R.drawable.ic_menu_preferences
            ),

            SettingsItem(
                id = "downloads",
                title = "下载设置",
                subtitle = "配置下载路径和行为",
                type = SettingsItem.TYPE_CLICKABLE,
                iconRes = android.R.drawable.ic_menu_save
            ),

            // 高级设置
            SettingsItem(
                id = "advanced",
                title = "高级设置",
                subtitle = "开发者选项和高级配置",
                type = SettingsItem.TYPE_CLICKABLE,
                iconRes = android.R.drawable.ic_menu_manage
            ),

            SettingsItem(
                id = "about",
                title = "关于浏览器",
                subtitle = "版本信息和帮助",
                type = SettingsItem.TYPE_CLICKABLE,
                iconRes = android.R.drawable.ic_menu_info_details
            ),

            // 操作
            SettingsItem(
                id = "clear_data",
                title = "清除数据",
                subtitle = "清除浏览历史、缓存和Cookie",
                type = SettingsItem.TYPE_CLICKABLE,
                iconRes = android.R.drawable.ic_menu_delete
            ),

            SettingsItem(
                id = "reset_settings",
                title = "重置设置",
                subtitle = "恢复所有设置为默认值",
                type = SettingsItem.TYPE_CLICKABLE,
                iconRes = android.R.drawable.ic_menu_revert
            )
        )

        _settingsItems.value = items
        Log.d(TAG, "Loaded ${items.size} settings items")
    }

    /**
     * 更新设置项的值
     */
    fun updateSettingValue(itemId: String, newValue: Boolean) {
        val currentItems = _settingsItems.value ?: return
        val updatedItems = currentItems.map { item ->
            if (item.id == itemId && item.type == SettingsItem.TYPE_SWITCH) {
                item.copy(isChecked = newValue)
            } else {
                item
            }
        }
        _settingsItems.value = updatedItems
    }

    /**
     * 设置项数据类
     */
    data class SettingsItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val type: Int,
        val isChecked: Boolean = false,
        val iconRes: Int
    ) {
        companion object {
            const val TYPE_CLICKABLE = 0
            const val TYPE_SWITCH = 1
        }
    }

    /**
     * 清理资源
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "BrowserSettingsViewModel cleared")
    }
}
