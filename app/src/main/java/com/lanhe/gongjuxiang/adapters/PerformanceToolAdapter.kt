package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.models.PerformanceTool

class PerformanceToolAdapter(
    private val tools: List<PerformanceTool>,
    private val onToolClick: (PerformanceTool) -> Unit
) : RecyclerView.Adapter<PerformanceToolAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardTool)
        val ivToolIcon: ImageView = itemView.findViewById(R.id.ivToolIcon)
        val tvToolTitle: TextView = itemView.findViewById(R.id.tvToolTitle)
        val tvToolDescription: TextView = itemView.findViewById(R.id.tvToolDescription)
        val tvToolCategory: TextView = itemView.findViewById(R.id.tvToolCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_performance_tool, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tool = tools[position]

        holder.ivToolIcon.setImageResource(tool.icon)
        holder.tvToolTitle.text = tool.title
        holder.tvToolDescription.text = tool.description
        holder.tvToolCategory.text = tool.category

        holder.cardView.setOnClickListener {
            onToolClick(tool)
        }
    }

    override fun getItemCount(): Int = tools.size
}
