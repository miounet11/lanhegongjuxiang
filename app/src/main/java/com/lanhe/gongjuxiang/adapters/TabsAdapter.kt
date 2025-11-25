package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.databinding.ItemTabBinding
import com.lanhe.gongjuxiang.utils.BrowserTabEntity

/**
 * 标签页列表适配器
 */
class TabsAdapter(
    private val onTabClick: (BrowserTabEntity) -> Unit,
    private val onTabClose: (BrowserTabEntity) -> Unit,
    private val activeTabId: String?
) : ListAdapter<BrowserTabEntity, TabsAdapter.TabViewHolder>(TabDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val binding = ItemTabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TabViewHolder(
        private val binding: ItemTabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tab: BrowserTabEntity) {
            binding.apply {
                // 标签标题
                tvTabTitle.text = tab.title.ifEmpty { "新标签页" }

                // 标签URL
                tvTabUrl.text = if (tab.url == "about:blank") {
                    "空白页"
                } else {
                    tab.url
                }

                // 活跃标记
                if (tab.tabId == activeTabId) {
                    activeIndicator.visibility = View.VISIBLE
                    root.strokeWidth = 2
                } else {
                    activeIndicator.visibility = View.GONE
                    root.strokeWidth = 1
                }

                // 点击切换标签
                root.setOnClickListener {
                    onTabClick(tab)
                }

                // 关闭标签
                btnCloseTab.setOnClickListener {
                    onTabClose(tab)
                }
            }
        }
    }

    class TabDiffCallback : DiffUtil.ItemCallback<BrowserTabEntity>() {
        override fun areItemsTheSame(
            oldItem: BrowserTabEntity,
            newItem: BrowserTabEntity
        ): Boolean {
            return oldItem.tabId == newItem.tabId
        }

        override fun areContentsTheSame(
            oldItem: BrowserTabEntity,
            newItem: BrowserTabEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
