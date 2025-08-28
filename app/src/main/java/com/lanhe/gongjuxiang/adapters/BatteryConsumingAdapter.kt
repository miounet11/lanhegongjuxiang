package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.viewmodels.NetworkDiagnosticViewModel

/**
 * 电池消耗应用适配器
 * 显示消耗电池最多的应用列表，并对长时间驻留的应用进行预警
 */
class BatteryConsumingAdapter(
    private val onAppClick: (NetworkDiagnosticViewModel.BatteryConsumingApp) -> Unit
) : RecyclerView.Adapter<BatteryConsumingAdapter.BatteryConsumingViewHolder>() {

    private var batteryApps = listOf<NetworkDiagnosticViewModel.BatteryConsumingApp>()

    fun updateData(apps: List<NetworkDiagnosticViewModel.BatteryConsumingApp>) {
        batteryApps = apps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatteryConsumingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_battery_consuming, parent, false)
        return BatteryConsumingViewHolder(view, onAppClick)
    }

    override fun onBindViewHolder(holder: BatteryConsumingViewHolder, position: Int) {
        holder.bind(batteryApps[position])
    }

    override fun getItemCount(): Int = batteryApps.size

    class BatteryConsumingViewHolder(
        itemView: View,
        private val onAppClick: (NetworkDiagnosticViewModel.BatteryConsumingApp) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        private val tvPackageName: TextView = itemView.findViewById(R.id.tvPackageName)
        private val tvConsumption: TextView = itemView.findViewById(R.id.tvConsumption)
        private val tvRunningTime: TextView = itemView.findViewById(R.id.tvRunningTime)
        private val tvWarning: TextView = itemView.findViewById(R.id.tvWarning)
        private val tvForceClose: TextView = itemView.findViewById(R.id.tvForceClose)

        fun bind(app: NetworkDiagnosticViewModel.BatteryConsumingApp) {
            tvAppName.text = app.appName
            tvPackageName.text = app.packageName
            tvConsumption.text = "${String.format("%.1f", app.consumptionPercent)}%"
            tvRunningTime.text = "${app.runningTime}分钟"

            // 检查是否需要强制关闭
            if (app.shouldClose) {
                tvWarning.visibility = View.VISIBLE
                tvWarning.text = "🔴 建议关闭"
                tvForceClose.visibility = View.VISIBLE
                tvForceClose.text = "⚡ 强制关闭"

                // 设置高危颜色
                tvConsumption.setTextColor(itemView.context.getColor(R.color.danger_red))
                tvRunningTime.setTextColor(itemView.context.getColor(R.color.danger_red))

                // 设置背景颜色表示危险
                itemView.setBackgroundColor(itemView.context.getColor(R.color.background_warning))
            } else if (app.consumptionPercent > 20) {
                tvWarning.visibility = View.VISIBLE
                tvWarning.text = "🟡 高消耗"
                tvForceClose.visibility = View.GONE

                // 设置警告颜色
                tvConsumption.setTextColor(itemView.context.getColor(R.color.warning_orange))
                tvRunningTime.setTextColor(itemView.context.getColor(R.color.warning_orange))

                itemView.setBackgroundColor(itemView.context.getColor(R.color.background_light))
            } else if (app.consumptionPercent > 10) {
                tvWarning.visibility = View.VISIBLE
                tvWarning.text = "🟢 正常"
                tvForceClose.visibility = View.GONE

                // 设置正常颜色
                tvConsumption.setTextColor(itemView.context.getColor(R.color.text_primary))
                tvRunningTime.setTextColor(itemView.context.getColor(R.color.text_secondary))

                itemView.setBackgroundColor(itemView.context.getColor(R.color.background_normal))
            } else {
                tvWarning.visibility = View.GONE
                tvForceClose.visibility = View.GONE

                // 设置正常颜色
                tvConsumption.setTextColor(itemView.context.getColor(R.color.text_primary))
                tvRunningTime.setTextColor(itemView.context.getColor(R.color.text_secondary))

                itemView.setBackgroundColor(itemView.context.getColor(R.color.background_normal))
            }

            // 设置点击事件
            itemView.setOnClickListener {
                onAppClick(app)
            }

            // 强制关闭按钮点击事件
            tvForceClose.setOnClickListener {
                onAppClick(app)
            }
        }
    }
}
