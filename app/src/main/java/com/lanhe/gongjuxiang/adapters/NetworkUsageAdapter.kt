package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.viewmodels.NetworkDiagnosticViewModel

/**
 * 网络占用应用适配器
 * 显示占用网络最多的应用列表
 */
class NetworkUsageAdapter(
    private val onAppClick: (NetworkDiagnosticViewModel.NetworkUsageApp) -> Unit
) : RecyclerView.Adapter<NetworkUsageAdapter.NetworkUsageViewHolder>() {

    private var networkApps = listOf<NetworkDiagnosticViewModel.NetworkUsageApp>()

    fun updateData(apps: List<NetworkDiagnosticViewModel.NetworkUsageApp>) {
        networkApps = apps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkUsageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_network_usage, parent, false)
        return NetworkUsageViewHolder(view, onAppClick)
    }

    override fun onBindViewHolder(holder: NetworkUsageViewHolder, position: Int) {
        holder.bind(networkApps[position])
    }

    override fun getItemCount(): Int = networkApps.size

    class NetworkUsageViewHolder(
        itemView: View,
        private val onAppClick: (NetworkDiagnosticViewModel.NetworkUsageApp) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvPackageName: TextView = itemView.findViewById(R.id.tvPackageName)
        private val tvUsage: TextView = itemView.findViewById(R.id.tvUsage)
        private val tvActiveTime: TextView = itemView.findViewById(R.id.tvActiveTime)
        private val tvWarning: TextView = itemView.findViewById(R.id.tvWarning)

        fun bind(app: NetworkDiagnosticViewModel.NetworkUsageApp) {
            tvAppName.text = app.appName
            tvPackageName.text = app.packageName
            tvUsage.text = "${String.format("%.1f", app.usageMB)}MB"
            tvActiveTime.text = "${app.activeTime}分钟"

            // 检查是否需要显示警告
            if (app.usageMB > 50) {
                tvWarning.visibility = View.VISIBLE
                tvWarning.text = "⚠️ 高占用"
                // 设置警告颜色
                tvUsage.setTextColor(itemView.context.getColor(R.color.warning_red))
            } else if (app.usageMB > 30) {
                tvWarning.visibility = View.VISIBLE
                tvWarning.text = "⚡ 较高"
                // 设置中等警告颜色
                tvUsage.setTextColor(itemView.context.getColor(R.color.warning_orange))
            } else {
                tvWarning.visibility = View.GONE
                tvUsage.setTextColor(itemView.context.getColor(R.color.text_primary))
            }

            // 设置点击事件
            itemView.setOnClickListener {
                onAppClick(app)
            }
        }
    }
}
