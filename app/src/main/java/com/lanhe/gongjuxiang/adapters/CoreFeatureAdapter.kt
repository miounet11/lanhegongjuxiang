package com.lanhe.gongjuxiang.adapters

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lanhe.gongjuxiang.databinding.ItemCoreFeatureModernBinding
import com.lanhe.gongjuxiang.models.CoreFeature

class CoreFeatureAdapter(
    private val onFeatureClick: (CoreFeature) -> Unit
) : ListAdapter<CoreFeature, CoreFeatureAdapter.CoreFeatureViewHolder>(DiffCallback()) {

    inner class CoreFeatureViewHolder(private val binding: ItemCoreFeatureModernBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(feature: CoreFeature) {
            binding.apply {
                ivFeatureIcon.setImageResource(feature.icon)
                tvFeatureTitle.text = feature.title
                tvFeatureDesc.text = feature.description

                // Add entrance animation
                root.alpha = 0f
                root.scaleX = 0.8f
                root.scaleY = 0.8f

                ObjectAnimator.ofFloat(root, "alpha", 0f, 1f).apply {
                    duration = 300
                    interpolator = DecelerateInterpolator()
                    start()
                }

                ObjectAnimator.ofFloat(root, "scaleX", 0.8f, 1f).apply {
                    duration = 300
                    interpolator = DecelerateInterpolator()
                    start()
                }

                ObjectAnimator.ofFloat(root, "scaleY", 0.8f, 1f).apply {
                    duration = 300
                    interpolator = DecelerateInterpolator()
                    start()
                }

                root.setOnClickListener {
                    // Add click animation
                    ObjectAnimator.ofFloat(root, "scaleX", 1f, 0.95f, 1f).apply {
                        duration = 150
                        start()
                    }
                    ObjectAnimator.ofFloat(root, "scaleY", 1f, 0.95f, 1f).apply {
                        duration = 150
                        start()
                    }

                    // Delayed click to allow animation
                    root.postDelayed({
                        onFeatureClick(feature)
                    }, 150)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CoreFeature>() {
        override fun areItemsTheSame(oldItem: CoreFeature, newItem: CoreFeature): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CoreFeature, newItem: CoreFeature): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoreFeatureViewHolder {
        val binding = ItemCoreFeatureModernBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CoreFeatureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CoreFeatureViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
