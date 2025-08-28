package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.models.MemoryFunction

class MemoryFunctionAdapter(
    private val functions: List<MemoryFunction>,
    private val onFunctionClick: (MemoryFunction) -> Unit
) : RecyclerView.Adapter<MemoryFunctionAdapter.ViewHolder>() {

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
            .inflate(R.layout.item_memory_function, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val function = functions[position]

        holder.tvFunctionName.text = function.name
        holder.tvFunctionDescription.text = function.description
        holder.tvCurrentValue.text = function.currentValue
        holder.tvCategory.text = function.category
        holder.switchEnabled.isChecked = function.isEnabled

        holder.switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            // 处理开关状态变化
        }

        holder.cardView.setOnClickListener {
            onFunctionClick(function)
        }
    }

    override fun getItemCount(): Int = functions.size
}
