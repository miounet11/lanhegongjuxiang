package com.lanhe.gongjuxiang.ui.animations

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.RecyclerView
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import kotlin.math.max

/**
 * Advanced animations for RecyclerView items
 */
object RecyclerViewAnimations {

    /**
     * Stagger animation for RecyclerView items
     */
    fun animateStagger(
        recyclerView: RecyclerView,
        delay: Long = 50L,
        duration: Long = 300L
    ) {
        val childCount = recyclerView.childCount
        for (i in 0 until childCount) {
            val child = recyclerView.getChildAt(i)
            child.alpha = 0f
            child.translationY = 100f

            ObjectAnimator.ofFloat(child, "alpha", 0f, 1f).apply {
                startDelay = i * delay
                this.duration = duration
                interpolator = DecelerateInterpolator()
                start()
            }

            ObjectAnimator.ofFloat(child, "translationY", 100f, 0f).apply {
                startDelay = i * delay
                this.duration = duration
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    /**
     * Slide in from right animation
     */
    fun slideInFromRight(view: View, duration: Long = 300L, delay: Long = 0L) {
        view.translationX = view.width.toFloat()
        view.alpha = 0f

        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "translationX", view.width.toFloat(), 0f),
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            )
            this.duration = duration
            startDelay = delay
            interpolator = DecelerateInterpolator()
        }
        animatorSet.start()
    }

    /**
     * Slide in from left animation
     */
    fun slideInFromLeft(view: View, duration: Long = 300L, delay: Long = 0L) {
        view.translationX = -view.width.toFloat()
        view.alpha = 0f

        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "translationX", -view.width.toFloat(), 0f),
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            )
            this.duration = duration
            startDelay = delay
            interpolator = DecelerateInterpolator()
        }
        animatorSet.start()
    }

    /**
     * Scale and fade in animation
     */
    fun scaleAndFadeIn(view: View, duration: Long = 300L, delay: Long = 0L) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f

        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0f, 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 0f, 1f),
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            )
            this.duration = duration
            startDelay = delay
            interpolator = OvershootInterpolator()
        }
        animatorSet.start()
    }

    /**
     * Bounce in animation
     */
    fun bounceIn(view: View, duration: Long = 600L, delay: Long = 0L) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f

        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1.2f, 0.9f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1.2f, 0.9f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            this.duration = duration
            startDelay = delay
            interpolator = OvershootInterpolator()
        }
        animatorSet.start()
    }

    /**
     * Flip in animation
     */
    fun flipIn(view: View, duration: Long = 600L, delay: Long = 0L) {
        view.rotationY = -90f
        view.alpha = 0f

        val animatorSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "rotationY", -90f, 0f),
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            )
            this.duration = duration
            startDelay = delay
            interpolator = DecelerateInterpolator()
        }
        animatorSet.start()
    }

    /**
     * Wave animation for multiple views
     */
    fun waveAnimation(views: List<View>, duration: Long = 300L, delay: Long = 100L) {
        views.forEachIndexed { index, view ->
            view.translationY = 50f
            view.alpha = 0.5f

            val animatorSet = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "translationY", 50f, -10f, 0f),
                    ObjectAnimator.ofFloat(view, "alpha", 0.5f, 1f)
                )
                this.duration = duration
                startDelay = index * delay
                interpolator = DecelerateInterpolator()
            }
            animatorSet.start()
        }
    }

    /**
     * Card flip animation
     */
    fun cardFlip(view: View, frontView: View, backView: View, duration: Long = 600L) {
        val firstHalf = ObjectAnimator.ofFloat(view, "rotationY", 0f, 90f).apply {
            this.duration = duration / 2
            interpolator = DecelerateInterpolator()
            doOnEnd {
                frontView.visibility = View.GONE
                backView.visibility = View.VISIBLE
            }
        }

        val secondHalf = ObjectAnimator.ofFloat(view, "rotationY", -90f, 0f).apply {
            this.duration = duration / 2
            interpolator = DecelerateInterpolator()
        }

        val animatorSet = AnimatorSet().apply {
            playSequentially(firstHalf, secondHalf)
        }
        animatorSet.start()
    }

    /**
     * Swipe to dismiss animation
     */
    fun swipeToDismiss(
        view: View,
        direction: Float, // -1 for left, 1 for right
        onDismiss: () -> Unit
    ) {
        val targetX = view.width * direction
        val animator = ObjectAnimator.ofFloat(view, "translationX", 0f, targetX).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
        }

        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
            duration = 300
        }

        AnimatorSet().apply {
            playTogether(animator, alphaAnimator)
            doOnEnd { onDismiss() }
            start()
        }
    }

    /**
     * Expand animation for collapsible items
     */
    fun expand(view: View, targetHeight: Int, duration: Long = 300L) {
        view.layoutParams.height = 0
        view.visibility = View.VISIBLE

        val animator = ValueAnimator.ofInt(0, targetHeight).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                view.layoutParams.height = value
                view.requestLayout()
            }
        }
        animator.start()
    }

    /**
     * Collapse animation for collapsible items
     */
    fun collapse(view: View, duration: Long = 300L, onComplete: (() -> Unit)? = null) {
        val initialHeight = view.measuredHeight

        val animator = ValueAnimator.ofInt(initialHeight, 0).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                view.layoutParams.height = value
                view.requestLayout()
            }
            doOnEnd {
                view.visibility = View.GONE
                onComplete?.invoke()
            }
        }
        animator.start()
    }

    /**
     * Shake animation for error states
     */
    fun shake(view: View, intensity: Float = 10f, duration: Long = 500L) {
        val animator = ObjectAnimator.ofFloat(
            view, "translationX",
            0f, intensity, -intensity, intensity, -intensity, 0f
        ).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
        }
        animator.start()
    }

    /**
     * Pulse animation for attention
     */
    fun pulse(view: View, scale: Float = 1.1f, duration: Long = 600L) {
        val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 1f, scale).apply {
            this.duration = duration / 2
        }
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f, scale).apply {
            this.duration = duration / 2
        }
        val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", scale, 1f).apply {
            this.duration = duration / 2
        }
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", scale, 1f).apply {
            this.duration = duration / 2
        }

        AnimatorSet().apply {
            play(scaleUp).with(scaleUpY)
            play(scaleDown).with(scaleDownY).after(scaleUp)
            start()
        }
    }

    /**
     * Custom RecyclerView ItemAnimator for advanced animations
     */
    class AdvancedItemAnimator : RecyclerView.ItemAnimator() {
        private val pendingRemovals = mutableListOf<RecyclerView.ViewHolder>()
        private val pendingAdditions = mutableListOf<RecyclerView.ViewHolder>()
        private val runningAnimations = mutableListOf<RecyclerView.ViewHolder>()

        override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
            pendingRemovals.add(holder)
            return true
        }

        override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
            holder.itemView.alpha = 0f
            holder.itemView.translationY = holder.itemView.height.toFloat()
            pendingAdditions.add(holder)
            return true
        }

        override fun animateMove(
            holder: RecyclerView.ViewHolder,
            fromX: Int, fromY: Int, toX: Int, toY: Int
        ): Boolean = false

        override fun animateChange(
            oldHolder: RecyclerView.ViewHolder,
            newHolder: RecyclerView.ViewHolder?,
            fromLeft: Int, fromTop: Int,
            toLeft: Int, toTop: Int
        ): Boolean = false

        override fun runPendingAnimations() {
            val removes = pendingRemovals.toList()
            val additions = pendingAdditions.toList()

            pendingRemovals.clear()
            pendingAdditions.clear()

            // Run remove animations
            removes.forEach { holder ->
                animateRemoveImpl(holder)
            }

            // Run add animations with delay
            if (additions.isNotEmpty()) {
                additions.forEachIndexed { index, holder ->
                    holder.itemView.postDelayed({
                        animateAddImpl(holder)
                    }, index * 50L)
                }
            }
        }

        private fun animateRemoveImpl(holder: RecyclerView.ViewHolder) {
            val view = holder.itemView
            runningAnimations.add(holder)

            ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
                duration = 300
                doOnEnd {
                    runningAnimations.remove(holder)
                    dispatchRemoveFinished(holder)
                    if (runningAnimations.isEmpty()) {
                        dispatchAnimationsFinished()
                    }
                }
                start()
            }
        }

        private fun animateAddImpl(holder: RecyclerView.ViewHolder) {
            val view = holder.itemView
            runningAnimations.add(holder)

            val animatorSet = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(view, "translationY", view.height.toFloat(), 0f)
                )
                duration = 300
                interpolator = DecelerateInterpolator()
                doOnEnd {
                    runningAnimations.remove(holder)
                    dispatchAddFinished(holder)
                    if (runningAnimations.isEmpty()) {
                        dispatchAnimationsFinished()
                    }
                }
            }
            animatorSet.start()
        }

        override fun endAnimation(item: RecyclerView.ViewHolder) {
            item.itemView.animate().cancel()
            if (runningAnimations.remove(item)) {
                dispatchAnimationFinished(item)
            }
        }

        override fun endAnimations() {
            val items = runningAnimations.toList()
            runningAnimations.clear()
            items.forEach { item ->
                item.itemView.animate().cancel()
                dispatchAnimationFinished(item)
            }
        }

        override fun isRunning(): Boolean = runningAnimations.isNotEmpty()
    }
}