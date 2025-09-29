package com.lanhe.gongjuxiang.activities

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.ai.AIOptimizationManager

class AIOptimizationAdapter(
    private val onExecuteClick: (AIOptimizationManager.OptimizationSuggestion) -> Unit
) : ListAdapter<AIOptimizationManager.OptimizationSuggestion, AIOptimizationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardSuggestion)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvSuggestionTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvSuggestionDescription)
        private val chipPriority: Chip = itemView.findViewById(R.id.chipPriority)
        private val tvImpact: TextView = itemView.findViewById(R.id.tvImpact)
        private val tvImprovement: TextView = itemView.findViewById(R.id.tvEstimatedImprovement)
        private val tvActions: TextView = itemView.findViewById(R.id.tvActions)
        private val btnExecute: Button = itemView.findViewById(R.id.btnExecute)

        fun bind(suggestion: AIOptimizationManager.OptimizationSuggestion) {
            tvTitle.text = suggestion.title
            tvDescription.text = suggestion.description
            tvImpact.text = "影响: ${suggestion.impact}"
            tvImprovement.text = "预期改善: ${suggestion.estimatedImprovement}"

            // 设置优先级
            chipPriority.text = when (suggestion.priority) {
                AIOptimizationManager.Priority.HIGH -> "高优先级"
                AIOptimizationManager.Priority.MEDIUM -> "中优先级"
                AIOptimizationManager.Priority.LOW -> "低优先级"
            }

            // 设置优先级颜色
            val priorityColor = when (suggestion.priority) {
                AIOptimizationManager.Priority.HIGH -> R.color.red
                AIOptimizationManager.Priority.MEDIUM -> R.color.yellow
                AIOptimizationManager.Priority.LOW -> R.color.green
            }
            chipPriority.setChipBackgroundColorResource(priorityColor)

            // 设置卡片边框颜色
            val strokeColor = ContextCompat.getColor(itemView.context, priorityColor)
            cardView.strokeColor = strokeColor

            // 显示操作列表
            val actionsText = suggestion.actions.mapIndexed { index, action ->
                "${index + 1}. $action"
            }.joinToString("\n")
            tvActions.text = actionsText

            // 执行按钮
            btnExecute.setOnClickListener {
                onExecuteClick(suggestion)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AIOptimizationManager.OptimizationSuggestion>() {
        override fun areItemsTheSame(
            oldItem: AIOptimizationManager.OptimizationSuggestion,
            newItem: AIOptimizationManager.OptimizationSuggestion
        ): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(
            oldItem: AIOptimizationManager.OptimizationSuggestion,
            newItem: AIOptimizationManager.OptimizationSuggestion
        ): Boolean {
            return oldItem == newItem
        }
    }
}