package com.lanhe.gongjuxiang.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.R

/**
 * 电池优化设置适配器
 */
class BatteryOptimizationAdapter(
    private val optimizationItems: List<BatteryOptimizationItem>,
    private val onItemClick: (BatteryOptimizationItem) -> Unit
) : RecyclerView.Adapter<BatteryOptimizationAdapter.OptimizationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptimizationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_battery_optimization, parent, false)
        return OptimizationViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptimizationViewHolder, position: Int) {
        holder.bind(optimizationItems[position])
    }

    override fun getItemCount(): Int = optimizationItems.size

    inner class OptimizationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.iv_icon)
        private val titleTextView: TextView = itemView.findViewById(R.id.tv_title)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.tv_description)
        private val manufacturerTextView: TextView = itemView.findViewById(R.id.tv_manufacturer)

        fun bind(item: BatteryOptimizationItem) {
            // 设置图标
            val iconRes = when (item.manufacturer) {
                "华为" -> R.drawable.ic_huawei
                "小米" -> R.drawable.ic_xiaomi
                "OPPO" -> R.drawable.ic_oppo
                "vivo" -> R.drawable.ic_vivo
                "三星" -> R.drawable.ic_samsung
                "魅族" -> R.drawable.ic_meizu
                else -> R.drawable.ic_battery
            }
            iconImageView.setImageResource(iconRes)

            titleTextView.text = item.title
            descriptionTextView.text = item.description
            manufacturerTextView.text = item.manufacturer

            // 设置制造商标签的背景色
            val backgroundRes = when (item.manufacturer) {
                "华为" -> R.drawable.bg_manufacturer_huawei
                "小米" -> R.drawable.bg_manufacturer_xiaomi
                "OPPO" -> R.drawable.bg_manufacturer_oppo
                "vivo" -> R.drawable.bg_manufacturer_vivo
                "三星" -> R.drawable.bg_manufacturer_samsung
                "魅族" -> R.drawable.bg_manufacturer_meizu
                else -> R.drawable.bg_manufacturer_generic
            }
            manufacturerTextView.setBackgroundResource(backgroundRes)

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
