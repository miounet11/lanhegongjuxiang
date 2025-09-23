package com.lanhe.gongjuxiang.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lanhe.gongjuxiang.databinding.ItemCoreFeatureModernBinding
import com.lanhe.gongjuxiang.models.CoreFeature
import com.lanhe.gongjuxiang.ui.haptic.HapticFeedbackManager
import com.lanhe.gongjuxiang.ui.animations.RecyclerViewAnimations
import com.lanhe.gongjuxiang.ui.gestures.AdvancedGestureDetector

/**
 * Enhanced adapter with premium animations and haptic feedback
 */
class EnhancedCoreFeatureAdapter(
    private val hapticFeedbackManager: HapticFeedbackManager,
    private val onItemClick: (CoreFeature) -> Unit,
    private val onItemLongClick: (CoreFeature) -> Boolean = { false }
) : ListAdapter<CoreFeature, EnhancedCoreFeatureAdapter.ViewHolder>(CoreFeatureDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCoreFeatureModernBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, hapticFeedbackManager, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feature = getItem(position)
        holder.bind(feature)

        // Add staggered animation for items
        RecyclerViewAnimations.slideInFromRight(
            holder.itemView,
            duration = 300L,
            delay = position * 50L
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty()) {
            // Handle partial updates for better performance
            val feature = getItem(position)
            for (payload in payloads) {
                when (payload) {
                    "status" -> holder.updateStatus(feature.status)
                    "badge" -> holder.updateBadge(feature.badgeCount)
                    "enabled" -> holder.updateEnabledState(feature.enabled)
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class ViewHolder(
        private val binding: ItemCoreFeatureModernBinding,
        private val hapticFeedbackManager: HapticFeedbackManager,
        private val onItemClick: (CoreFeature) -> Unit,
        private val onItemLongClick: (CoreFeature) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        private val gestureDetector = AdvancedGestureDetector(
            binding.root.context,
            object : AdvancedGestureDetector.AdvancedGestureListener {
                override fun onSingleTap(view: View, event: android.view.MotionEvent): Boolean {
                    currentFeature?.let { feature ->
                        if (feature.enabled) {
                            hapticFeedbackManager.lightClick(view)
                            onItemClick(feature)

                            // Add click animation
                            RecyclerViewAnimations.scaleAndFadeIn(view, duration = 150L)
                        } else {
                            hapticFeedbackManager.error()
                            RecyclerViewAnimations.shake(view)
                        }
                    }
                    return true
                }

                override fun onDoubleTap(view: View, event: android.view.MotionEvent): Boolean {
                    currentFeature?.let { feature ->
                        hapticFeedbackManager.heavyClick(view)
                        // Add double-tap specific action (e.g., quick toggle)
                        toggleFeatureQuickly(feature)
                    }
                    return true
                }

                override fun onLongPress(view: View, event: android.view.MotionEvent) {
                    currentFeature?.let { feature ->
                        hapticFeedbackManager.longPress(view)
                        if (onItemLongClick(feature)) {
                            // Add long press animation
                            RecyclerViewAnimations.pulse(view)
                        }
                    }
                }

                override fun onSwipeLeft(view: View, velocityX: Float): Boolean {
                    currentFeature?.let { feature ->
                        hapticFeedbackManager.mediumClick(view)
                        // Handle swipe left (e.g., disable feature)
                        handleSwipeAction(feature, "disable")
                    }
                    return true
                }

                override fun onSwipeRight(view: View, velocityX: Float): Boolean {
                    currentFeature?.let { feature ->
                        hapticFeedbackManager.mediumClick(view)
                        // Handle swipe right (e.g., enable feature)
                        handleSwipeAction(feature, "enable")
                    }
                    return true
                }
            }
        )

        private var currentFeature: CoreFeature? = null

        init {
            binding.root.setOnTouchListener { view, event ->
                gestureDetector.onTouchEvent(view, event)
            }

            // Set up interactive card
            binding.cardContainer.apply {
                isClickable = true
                isFocusable = true
                stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(
                    context, com.lanhe.gongjuxiang.R.animator.card_state_list_animator
                )
            }
        }

        fun bind(feature: CoreFeature) {
            currentFeature = feature

            with(binding) {
                // Basic info
                tvTitle.text = feature.title
                tvDescription.text = feature.description

                // Load icon with Glide
                Glide.with(ivIcon.context)
                    .load(feature.iconRes)
                    .placeholder(com.lanhe.gongjuxiang.R.drawable.ic_optimize)
                    .error(com.lanhe.gongjuxiang.R.drawable.ic_error)
                    .into(ivIcon)

                // Update all states
                updateStatus(feature.status)
                updateBadge(feature.badgeCount)
                updateEnabledState(feature.enabled)

                // Animation on bind
                RecyclerViewAnimations.scaleAndFadeIn(
                    root,
                    duration = 200L,
                    delay = adapterPosition * 25L
                )
            }
        }

        fun updateStatus(status: String) {
            binding.tvStatus.apply {
                text = status
                // Update status color based on content
                when {
                    status.contains("优化") -> setTextColor(
                        context.getColor(android.R.color.holo_green_dark)
                    )
                    status.contains("警告") -> setTextColor(
                        context.getColor(android.R.color.holo_orange_dark)
                    )
                    status.contains("错误") -> setTextColor(
                        context.getColor(android.R.color.holo_red_dark)
                    )
                    else -> setTextColor(
                        context.getColor(com.lanhe.gongjuxiang.R.color.colorOnSurface)
                    )
                }
            }
        }

        fun updateBadge(badgeCount: Int) {
            binding.tvBadge.apply {
                if (badgeCount > 0) {
                    visibility = View.VISIBLE
                    text = if (badgeCount > 99) "99+" else badgeCount.toString()

                    // Animate badge appearance
                    RecyclerViewAnimations.bounceIn(this, duration = 400L)
                } else {
                    visibility = View.GONE
                }
            }
        }

        fun updateEnabledState(enabled: Boolean) {
            binding.apply {
                cardContainer.alpha = if (enabled) 1.0f else 0.5f
                cardContainer.isEnabled = enabled

                if (!enabled) {
                    tvStatus.text = "已禁用"
                    tvStatus.setTextColor(
                        root.context.getColor(android.R.color.darker_gray)
                    )
                }
            }
        }

        private fun toggleFeatureQuickly(feature: CoreFeature) {
            // Implementation for quick toggle
            val newState = !feature.enabled
            updateEnabledState(newState)

            // Add visual feedback
            if (newState) {
                RecyclerViewAnimations.scaleAndFadeIn(binding.root)
            } else {
                binding.root.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .alpha(0.5f)
                    .setDuration(150)
                    .start()
            }
        }

        private fun handleSwipeAction(feature: CoreFeature, action: String) {
            when (action) {
                "enable" -> {
                    updateEnabledState(true)
                    RecyclerViewAnimations.slideInFromLeft(binding.root)
                }
                "disable" -> {
                    updateEnabledState(false)
                    RecyclerViewAnimations.slideInFromRight(binding.root)
                }
            }
        }
    }

    class CoreFeatureDiffCallback : DiffUtil.ItemCallback<CoreFeature>() {
        override fun areItemsTheSame(oldItem: CoreFeature, newItem: CoreFeature): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CoreFeature, newItem: CoreFeature): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: CoreFeature, newItem: CoreFeature): Any? {
            val payloads = mutableListOf<String>()

            if (oldItem.status != newItem.status) payloads.add("status")
            if (oldItem.badgeCount != newItem.badgeCount) payloads.add("badge")
            if (oldItem.enabled != newItem.enabled) payloads.add("enabled")

            return if (payloads.isNotEmpty()) payloads else null
        }
    }

    /**
     * Public methods for external animation control
     */

    fun animateItemInsertion(position: Int) {
        notifyItemInserted(position)
        // Animate items below
        for (i in position + 1 until itemCount) {
            notifyItemChanged(i, "animate_shift")
        }
    }

    fun animateItemRemoval(position: Int) {
        notifyItemRemoved(position)
        // Animate items below
        for (i in position until itemCount) {
            notifyItemChanged(i, "animate_shift")
        }
    }

    fun animateAllItems() {
        for (i in 0 until itemCount) {
            notifyItemChanged(i, "animate_all")
        }
    }
}