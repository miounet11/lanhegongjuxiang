package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.databinding.ItemProcessBinding
import com.lanhe.gongjuxiang.utils.ProcessInfo

/**
 * 进程列表适配器
 * 显示系统进程信息
 */
class ProcessListAdapter(
    private val onProcessClick: (ProcessInfo) -> Unit
) : ListAdapter<ProcessInfo, ProcessListAdapter.ProcessViewHolder>(ProcessDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessViewHolder {
        val binding = ItemProcessBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProcessViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProcessViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProcessViewHolder(
        private val binding: ItemProcessBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onProcessClick(getItem(position))
                }
            }
        }

        fun bind(process: ProcessInfo) {
            binding.apply {
                // 进程名称
                tvProcessName.text = process.processName.take(20) +
                    if (process.processName.length > 20) "..." else ""

                // 包名
                tvPackageName.text = process.packageName.take(25) +
                    if (process.packageName.length > 25) "..." else ""

                // PID
                tvPid.text = "PID: ${process.pid}"

                // 内存使用
                val memoryMB = process.memoryUsage / (1024 * 1024)
                tvMemoryUsage.text = "${memoryMB}MB"

                // UID
                tvUid.text = "UID: ${process.uid}"

                // 根据内存使用设置颜色
                val colorRes = when {
                    memoryMB > 100 -> android.R.color.holo_red_dark
                    memoryMB > 50 -> android.R.color.holo_orange_dark
                    else -> android.R.color.holo_green_dark
                }
                tvMemoryUsage.setTextColor(root.context.getColor(colorRes))
            }
        }
    }

    /**
     * DiffUtil回调
     */
    class ProcessDiffCallback : DiffUtil.ItemCallback<ProcessInfo>() {
        override fun areItemsTheSame(oldItem: ProcessInfo, newItem: ProcessInfo): Boolean {
            return oldItem.pid == newItem.pid
        }

        override fun areContentsTheSame(oldItem: ProcessInfo, newItem: ProcessInfo): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * 更新进程列表
     */
    fun updateProcesses(newProcesses: List<ProcessInfo>) {
        submitList(newProcesses)
    }
}
