package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.lanhe.gongjuxiang.R
import com.lanhe.gongjuxiang.models.SecurityFeature

class SecurityFeatureAdapter(
    private val features: List<SecurityFeature>,
    private val onFeatureClick: (SecurityFeature) -> Unit
) : RecyclerView.Adapter<SecurityFeatureAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardFeature)
        val ivFeatureIcon: ImageView = itemView.findViewById(R.id.ivFeatureIcon)
        val tvFeatureTitle: TextView = itemView.findViewById(R.id.tvFeatureTitle)
        val tvFeatureDescription: TextView = itemView.findViewById(R.id.tvFeatureDescription)
        val tvFeatureCategory: TextView = itemView.findViewById(R.id.tvFeatureCategory)
        val tvFeatureStatus: TextView = itemView.findViewById(R.id.tvFeatureStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_security_feature, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feature = features[position]

        holder.ivFeatureIcon.setImageResource(feature.icon)
        holder.tvFeatureTitle.text = feature.title
        holder.tvFeatureDescription.text = feature.description
        holder.tvFeatureCategory.text = feature.category
        holder.tvFeatureStatus.text = feature.status

        // 根据状态设置不同的颜色
        val statusColor = when (feature.status) {
            "安全", "正常", "启用", "运行中" -> R.color.success
            "警告", "检查中" -> R.color.warning
            "危险", "未扫描" -> R.color.error
            else -> R.color.text_secondary
        }
        holder.tvFeatureStatus.setTextColor(holder.itemView.context.getColor(statusColor))

        holder.cardView.setOnClickListener {
            onFeatureClick(feature)
        }
    }

    override fun getItemCount(): Int = features.size
}
