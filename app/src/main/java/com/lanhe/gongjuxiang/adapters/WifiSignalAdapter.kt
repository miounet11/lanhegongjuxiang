package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.viewmodels.NetworkDiagnosticViewModel

/**
 * WiFi信号适配器
 * 显示WiFi信号强度列表，按dBm排序
 */
class WifiSignalAdapter : RecyclerView.Adapter<WifiSignalAdapter.WifiSignalViewHolder>() {

    private var wifiSignals = listOf<NetworkDiagnosticViewModel.WifiSignal>()

    fun updateData(signals: List<NetworkDiagnosticViewModel.WifiSignal>) {
        wifiSignals = signals
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiSignalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wifi_signal, parent, false)
        return WifiSignalViewHolder(view)
    }

    override fun onBindViewHolder(holder: WifiSignalViewHolder, position: Int) {
        holder.bind(wifiSignals[position])
    }

    override fun getItemCount(): Int = wifiSignals.size

    class WifiSignalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSsid: TextView = itemView.findViewById(R.id.tvSsid)
        private val tvBssid: TextView = itemView.findViewById(R.id.tvBssid)
        private val tvRssi: TextView = itemView.findViewById(R.id.tvRssi)
        private val tvSignalLevel: TextView = itemView.findViewById(R.id.tvSignalLevel)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(signal: NetworkDiagnosticViewModel.WifiSignal) {
            tvSsid.text = signal.ssid
            tvBssid.text = signal.bssid
            tvRssi.text = "${signal.rssi}dBm"
            tvSignalLevel.text = getSignalLevelText(signal.signalLevel)
            tvStatus.text = if (signal.isConnected) "已连接" else "可用"

            // 设置信号强度颜色
            val colorRes = when (signal.signalLevel) {
                5 -> R.color.signal_excellent
                4 -> R.color.signal_good
                3 -> R.color.signal_fair
                2 -> R.color.signal_poor
                1 -> R.color.signal_weak
                else -> R.color.signal_none
            }
            tvRssi.setTextColor(itemView.context.getColor(colorRes))
        }

        private fun getSignalLevelText(level: Int): String {
            return when (level) {
                5 -> "极强"
                4 -> "强"
                3 -> "良好"
                2 -> "一般"
                1 -> "弱"
                else -> "无信号"
            }
        }
    }
}
