package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.databinding.ItemCoreFeatureBinding
import com.lanhe.gongjuxiang.models.CoreFeature

class CoreFeatureAdapter(
    private val features: List<CoreFeature>,
    private val onFeatureClick: (CoreFeature) -> Unit
) : RecyclerView.Adapter<CoreFeatureAdapter.CoreFeatureViewHolder>() {

    inner class CoreFeatureViewHolder(private val binding: ItemCoreFeatureBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(feature: CoreFeature) {
            binding.apply {
                ivFeatureIcon.setImageResource(feature.iconRes)
                tvFeatureTitle.text = feature.title
                tvFeatureDesc.text = feature.description

                root.setOnClickListener {
                    onFeatureClick(feature)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoreFeatureViewHolder {
        val binding = ItemCoreFeatureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CoreFeatureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CoreFeatureViewHolder, position: Int) {
        holder.bind(features[position])
    }

    override fun getItemCount(): Int = features.size
}
