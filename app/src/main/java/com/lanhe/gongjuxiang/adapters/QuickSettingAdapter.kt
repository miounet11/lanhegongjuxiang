package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.switchmaterial.SwitchMaterial
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.models.QuickSetting

class QuickSettingAdapter(
    private val settings: List<QuickSetting>,
    private val onSettingClick: (QuickSetting) -> Unit
) : RecyclerView.Adapter<QuickSettingAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardSetting)
        val tvSettingName: TextView = itemView.findViewById(R.id.tvSettingName)
        val tvSettingDescription: TextView = itemView.findViewById(R.id.tvSettingDescription)
        val tvCurrentValue: TextView = itemView.findViewById(R.id.tvCurrentValue)
        val switchEnabled: SwitchMaterial = itemView.findViewById(R.id.switchEnabled)
        val chipOptions: Chip = itemView.findViewById(R.id.chipOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quick_setting, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val setting = settings[position]

        holder.tvSettingName.text = setting.name
        holder.tvSettingDescription.text = setting.description
        holder.tvCurrentValue.text = setting.currentValue
        holder.switchEnabled.isChecked = setting.isEnabled

        // 显示选项数量
        if (setting.options.isNotEmpty()) {
            holder.chipOptions.text = "${setting.options.size} 个选项"
            holder.chipOptions.visibility = View.VISIBLE
        } else {
            holder.chipOptions.visibility = View.GONE
        }

        holder.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            // 处理开关状态变化
        }

        holder.cardView.setOnClickListener {
            onSettingClick(setting)
        }
    }

    override fun getItemCount(): Int = settings.size
}
