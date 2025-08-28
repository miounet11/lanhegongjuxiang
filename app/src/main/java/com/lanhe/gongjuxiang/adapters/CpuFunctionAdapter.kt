package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.models.CpuFunction

class CpuFunctionAdapter(
    private val functions: List<CpuFunction>,
    private val onFunctionClick: (CpuFunction) -> Unit
) : RecyclerView.Adapter<CpuFunctionAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardFunction)
        val tvFunctionName: TextView = itemView.findViewById(R.id.tvFunctionName)
        val tvFunctionDescription: TextView = itemView.findViewById(R.id.tvFunctionDescription)
        val tvCurrentValue: TextView = itemView.findViewById(R.id.tvCurrentValue)
        val switchEnabled: SwitchMaterial = itemView.findViewById(R.id.switchEnabled)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cpu_function, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val function = functions[position]

        holder.tvFunctionName.text = function.name
        holder.tvFunctionDescription.text = function.description
        holder.tvCurrentValue.text = function.currentValue
        holder.tvCategory.text = function.category
        holder.switchEnabled.isChecked = function.isEnabled

        // 设置开关状态变化监听
        holder.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            // 这里可以处理开关状态变化
        }

        // 设置卡片点击监听
        holder.cardView.setOnClickListener {
            onFunctionClick(function)
        }
    }

    override fun getItemCount(): Int = functions.size
}
