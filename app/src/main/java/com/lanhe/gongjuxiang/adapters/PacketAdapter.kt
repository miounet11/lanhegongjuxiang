package com.lanhe.gongjuxiang.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.databinding.ItemPacketBinding
import com.lanhe.gongjuxiang.models.PacketInfo

class PacketAdapter(
    private var packets: MutableList<PacketInfo>,
    private val onItemClick: (PacketInfo) -> Unit
) : RecyclerView.Adapter<PacketAdapter.PacketViewHolder>() {

    private var filteredPackets: MutableList<PacketInfo> = packets.toMutableList()
    private var currentFilter: String? = null
    private var autoScroll = true
    private var showRawData = false
    private var advancedFilter = true
    private var fileLogging = false

    class PacketViewHolder(val binding: ItemPacketBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacketViewHolder {
        val binding = ItemPacketBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PacketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PacketViewHolder, position: Int) {
        val packet = filteredPackets[position]
        holder.binding.apply {
            // 设置基本信息
            tvTimestamp.text = packet.timestamp
            tvProtocol.text = packet.protocol
            tvSummary.text = packet.getSummary()
            tvSize.text = packet.getFormattedSize()

            // 设置协议颜色
            tvProtocol.setTextColor(packet.getProtocolColor())

            // 设置状态图标
            tvStatusIcon.text = packet.getStatusIcon()

            // 设置背景颜色（根据是否为错误包）
            val backgroundColor = if (packet.isError()) {
                Color.parseColor("#FFF8E1") // 浅橙色背景
            } else if (packet.isSuccess()) {
                Color.parseColor("#E8F5E8") // 浅绿色背景
            } else {
                Color.WHITE
            }
            root.setBackgroundColor(backgroundColor)

            // 设置点击监听器
            root.setOnClickListener {
                onItemClick(packet)
            }

            // 显示原始数据（如果启用）
            if (showRawData && packet.rawData != null) {
                tvRawData.text = packet.rawData
                tvRawData.visibility = android.view.View.VISIBLE
            } else {
                tvRawData.visibility = android.view.View.GONE
            }

            // 显示高级信息
            if (advancedFilter) {
                tvAdvancedInfo.text = packet.getTypeDescription()
                tvAdvancedInfo.visibility = android.view.View.VISIBLE
            } else {
                tvAdvancedInfo.visibility = android.view.View.GONE
            }
        }
    }

    override fun getItemCount(): Int = filteredPackets.size

    // 添加新的数据包
    fun addPacket(packet: PacketInfo) {
        packets.add(0, packet) // 添加到开头
        applyCurrentFilter()
        notifyItemInserted(0)

        // 如果启用了自动滚动，滚动到顶部
        if (autoScroll) {
            // 这里需要外部调用者处理滚动
        }
    }

    // 清除所有数据包
    fun clearPackets() {
        packets.clear()
        filteredPackets.clear()
        notifyDataSetChanged()
    }

    // 获取所有数据包
    fun getAllPackets(): List<PacketInfo> = packets.toList()

    // 根据协议过滤
    fun filterByProtocol(protocol: String?) {
        currentFilter = protocol
        applyCurrentFilter()
    }

    // 应用当前过滤器
    private fun applyCurrentFilter() {
        filteredPackets = if (currentFilter == null) {
            packets.toMutableList()
        } else {
            packets.filter {
                it.protocol.contains(currentFilter!!, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    // 设置自动滚动
    fun setAutoScroll(enabled: Boolean) {
        autoScroll = enabled
    }

    // 设置显示原始数据
    fun showRawData(enabled: Boolean) {
        showRawData = enabled
        notifyDataSetChanged()
    }

    // 设置高级过滤
    fun enableAdvancedFilter(enabled: Boolean) {
        advancedFilter = enabled
        notifyDataSetChanged()
    }

    // 设置文件日志
    fun enableFileLogging(enabled: Boolean) {
        fileLogging = enabled
        // 这里可以实现文件日志功能
    }

    // 搜索功能
    fun search(query: String) {
        filteredPackets = if (query.isEmpty()) {
            packets.toMutableList()
        } else {
            packets.filter { packet ->
                packet.destinationAddress.contains(query, ignoreCase = true) ||
                packet.description.contains(query, ignoreCase = true) ||
                packet.protocol.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    // 获取统计信息
    fun getStats(): PacketStats {
        val totalPackets = packets.size
        val totalSize = packets.sumOf { it.size }
        val errorPackets = packets.count { it.isError() }
        val httpPackets = packets.count { it.protocol.contains("HTTP", ignoreCase = true) }

        return PacketStats(totalPackets, totalSize.toLong(), errorPackets, httpPackets)
    }

    // 统计数据类
    data class PacketStats(
        val totalPackets: Int,
        val totalSize: Long,
        val errorPackets: Int,
        val httpPackets: Int
    ) {
        fun getFormattedSize(): String {
            return when {
                totalSize < 1024 -> "${totalSize}B"
                totalSize < 1024 * 1024 -> String.format("%.1fKB", totalSize / 1024.0)
                else -> String.format("%.1fMB", totalSize / (1024.0 * 1024.0))
            }
        }

        fun getErrorRate(): String {
            if (totalPackets == 0) return "0%"
            return String.format("%.1f%%", (errorPackets.toFloat() / totalPackets) * 100)
        }
    }
}
