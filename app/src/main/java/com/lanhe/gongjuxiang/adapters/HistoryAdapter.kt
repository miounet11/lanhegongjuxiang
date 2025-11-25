package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.databinding.ItemHistoryBinding
import com.lanhe.gongjuxiang.utils.BrowserHistoryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 浏览历史列表适配器
 */
class HistoryAdapter(
    private val onHistoryClick: (BrowserHistoryEntity) -> Unit,
    private val onHistoryLongClick: (BrowserHistoryEntity) -> Boolean
) : ListAdapter<BrowserHistoryEntity, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(history: BrowserHistoryEntity) {
            binding.apply {
                // 历史标题
                historyTitle.text = history.title.ifEmpty { "未命名网页" }

                // 历史URL
                historyUrl.text = history.url

                // 访问时间
                val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                historyTime.text = dateFormat.format(Date(history.visitTime))

                // 访问次数
                if (history.visitCount > 1) {
                    historyVisitCount.text = "访问${history.visitCount}次"
                    historyVisitCount.visibility = android.view.View.VISIBLE
                } else {
                    historyVisitCount.visibility = android.view.View.GONE
                }

                // 书签标记
                if (history.isBookmarked) {
                    historyBookmarkIcon.visibility = android.view.View.VISIBLE
                } else {
                    historyBookmarkIcon.visibility = android.view.View.GONE
                }

                // 搜索词标记
                if (!history.searchTerm.isNullOrEmpty()) {
                    historySearchIcon.visibility = android.view.View.VISIBLE
                } else {
                    historySearchIcon.visibility = android.view.View.GONE
                }

                // 点击事件
                root.setOnClickListener {
                    onHistoryClick(history)
                }

                root.setOnLongClickListener {
                    onHistoryLongClick(history)
                }
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<BrowserHistoryEntity>() {
        override fun areItemsTheSame(
            oldItem: BrowserHistoryEntity,
            newItem: BrowserHistoryEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: BrowserHistoryEntity,
            newItem: BrowserHistoryEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
