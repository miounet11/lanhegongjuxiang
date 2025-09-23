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
                "华为" -> android.R.drawable.ic_menu_manage
                "小米" -> android.R.drawable.ic_menu_manage
                "OPPO" -> android.R.drawable.ic_menu_manage
                "vivo" -> android.R.drawable.ic_menu_manage
                "三星" -> android.R.drawable.ic_menu_manage
                "魅族" -> android.R.drawable.ic_menu_manage
                else -> android.R.drawable.ic_lock_idle_lock
            }
            iconImageView.setImageResource(iconRes)

            titleTextView.text = item.title
            descriptionTextView.text = item.description
            manufacturerTextView.text = item.manufacturer

            // 设置制造商标签的背景色
            val backgroundRes = when (item.manufacturer) {
                "华为" -> android.R.drawable.editbox_background
                "小米" -> android.R.drawable.editbox_background
                "OPPO" -> android.R.drawable.editbox_background
                "vivo" -> android.R.drawable.editbox_background
                "三星" -> android.R.drawable.editbox_background
                "魅族" -> android.R.drawable.editbox_background
                else -> android.R.drawable.editbox_background
            }
            manufacturerTextView.setBackgroundResource(backgroundRes)

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
