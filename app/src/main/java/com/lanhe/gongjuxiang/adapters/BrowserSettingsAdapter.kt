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

package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.viewmodels.BrowserSettingsViewModel

/**
 * 浏览器设置适配器
 * 用于显示浏览器设置列表
 *
 * @author LanHe Team
 * @version 1.0.0
 * @since 2024-01-01
 */
class BrowserSettingsAdapter(
    private val settingsItems: List<BrowserSettingsViewModel.SettingsItem>,
    private val onItemClick: (BrowserSettingsViewModel.SettingsItem) -> Unit
) : RecyclerView.Adapter<BrowserSettingsAdapter.SettingsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_browser_setting, parent, false)
        return SettingsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val item = settingsItems[position]
        holder.bind(item, onItemClick)
    }

    override fun getItemCount(): Int = settingsItems.size

    /**
     * 设置项ViewHolder
     */
    class SettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivIcon: ImageView = itemView.findViewById(R.id.ivSettingIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSettingTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSettingSubtitle)
        private val switchControl: Switch = itemView.findViewById(R.id.switchSetting)
        private val ivChevron: ImageView = itemView.findViewById(R.id.ivChevron)

        fun bind(item: BrowserSettingsViewModel.SettingsItem, onItemClick: (BrowserSettingsViewModel.SettingsItem) -> Unit) {
            // 设置基本信息
            ivIcon.setImageResource(item.iconRes)
            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle

            // 根据类型设置UI
            when (item.type) {
                BrowserSettingsViewModel.SettingsItem.TYPE_SWITCH -> {
                    // 开关类型
                    switchControl.visibility = View.VISIBLE
                    ivChevron.visibility = View.GONE
                    switchControl.isChecked = item.isChecked

                    // 开关点击事件
                    switchControl.setOnClickListener {
                        onItemClick(item)
                    }

                    // 整行点击事件
                    itemView.setOnClickListener {
                        switchControl.isChecked = !switchControl.isChecked
                        onItemClick(item)
                    }
                }

                BrowserSettingsViewModel.SettingsItem.TYPE_CLICKABLE -> {
                    // 可点击类型
                    switchControl.visibility = View.GONE
                    ivChevron.visibility = View.VISIBLE

                    // 点击事件
                    itemView.setOnClickListener {
                        onItemClick(item)
                    }
                }
            }
        }
    }
}
