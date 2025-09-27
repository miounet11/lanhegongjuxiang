package com.lanhe.gongjuxiang.ui.animations

import android.animation.*
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * 视图动画工具类
 * 提供一系列预定义的微动画效果
 */
object ViewAnimations {

    private const val DURATION_SHORT = 150L
    private const val DURATION_MEDIUM = 300L
    private const val DURATION_LONG = 500L

    /**
     * 按压缩放动画
     */
    fun scaleOnPress(view: View, scale: Float = 0.95f) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, scale)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, scale)

        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY).apply {
            duration = DURATION_SHORT
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    /**
     * 释放缩放动画
     */
    fun scaleOnRelease(view: View) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f)

        ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY).apply {
            duration = DURATION_SHORT
            interpolator = OvershootInterpolator(1.2f)
            start()
        }
    }

    /**
     * 淡入动画
     */
    fun fadeIn(view: View, duration: Long = DURATION_MEDIUM, onComplete: (() -> Unit)? = null) {
        view.alpha = 0f
        view.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            doOnEnd { onComplete?.invoke() }
            start()
        }
    }

    /**
     * 淡出动画
     */
    fun fadeOut(view: View, duration: Long = DURATION_MEDIUM, onComplete: (() -> Unit)? = null) {
        ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 0f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            doOnEnd {
                view.visibility = View.GONE
                onComplete?.invoke()
            }
            start()
        }
    }

    /**
     * 从底部滑入
     */
    fun slideInFromBottom(view: View, duration: Long = DURATION_MEDIUM) {
        view.alpha = 0f
        view.translationY = view.height.toFloat()
        view.visibility = View.VISIBLE

        val fadeIn = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
        val slideIn = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.height.toFloat(), 0f)

        AnimatorSet().apply {
            playTogether(fadeIn, slideIn)
            this.duration = duration
            interpolator = FastOutSlowInInterpolator()
            start()
        }
    }

    /**
     * 向顶部滑出
     */
    fun slideOutToTop(view: View, duration: Long = DURATION_MEDIUM, onComplete: (() -> Unit)? = null) {
        val fadeOut = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 0f)
        val slideOut = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 0f, -view.height.toFloat())

        AnimatorSet().apply {
            playTogether(fadeOut, slideOut)
            this.duration = duration
            interpolator = FastOutSlowInInterpolator()
            doOnEnd {
                view.visibility = View.GONE
                view.translationY = 0f
                onComplete?.invoke()
            }
            start()
        }
    }

    /**
     * 弹性缩放进入
     */
    fun bounceIn(view: View, duration: Long = DURATION_LONG) {
        view.scaleX = 0.3f
        view.scaleY = 0.3f
        view.alpha = 0f
        view.visibility = View.VISIBLE

        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.3f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.3f, 1.0f)
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            this.duration = duration
            interpolator = OvershootInterpolator(1.5f)
            start()
        }
    }

    /**
     * 弹性缩放退出
     */
    fun bounceOut(view: View, duration: Long = DURATION_MEDIUM, onComplete: (() -> Unit)? = null) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, 0.3f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, 0.3f)
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 0f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            this.duration = duration
            interpolator = DecelerateInterpolator()
            doOnEnd {
                view.visibility = View.GONE
                view.scaleX = 1.0f
                view.scaleY = 1.0f
                onComplete?.invoke()
            }
            start()
        }
    }

    /**
     * 摇摆动画（用于提醒或错误）
     */
    fun shake(view: View, amplitude: Float = 16f, cycles: Int = 3) {
        val translateX = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, amplitude, -amplitude, 0f)
        translateX.apply {
            duration = DURATION_MEDIUM
            repeatCount = cycles
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    /**
     * 脉冲动画（用于吸引注意）
     */
    fun pulse(view: View, scale: Float = 1.2f, cycles: Int = 2) {
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, scale, 1.0f).apply {
            repeatCount = cycles
        }
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, scale, 1.0f).apply {
            repeatCount = cycles
        }

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = DURATION_MEDIUM
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    /**
     * 呼吸动画（用于加载状态）
     */
    fun breathe(view: View): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.5f, 1.0f).apply {
            duration = DURATION_LONG * 2
            repeatCount = ObjectAnimator.INFINITE
            interpolator = DecelerateInterpolator()
        }
    }

    /**
     * 旋转动画
     */
    fun rotate(view: View, degrees: Float = 360f, duration: Long = DURATION_MEDIUM) {
        ObjectAnimator.ofFloat(view, View.ROTATION, 0f, degrees).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    /**
     * 错位列表动画（用于RecyclerView）
     */
    fun staggeredListAnimation(views: List<View>, delay: Long = 50L) {
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 100f

            ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
                duration = DURATION_MEDIUM
                startDelay = index * delay
                interpolator = DecelerateInterpolator()
                start()
            }

            ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 100f, 0f).apply {
                duration = DURATION_MEDIUM
                startDelay = index * delay
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    /**
     * 卡片翻转动画
     */
    fun flipCard(frontView: View, backView: View, duration: Long = DURATION_MEDIUM) {
        val flipOut = ObjectAnimator.ofFloat(frontView, View.ROTATION_Y, 0f, 90f).apply {
            this.duration = duration / 2
            interpolator = DecelerateInterpolator()
        }

        val flipIn = ObjectAnimator.ofFloat(backView, View.ROTATION_Y, -90f, 0f).apply {
            this.duration = duration / 2
            interpolator = DecelerateInterpolator()
        }

        flipOut.doOnEnd {
            frontView.visibility = View.GONE
            backView.visibility = View.VISIBLE
            flipIn.start()
        }

        flipOut.start()
    }

    /**
     * 涟漪展开动画
     */
    fun rippleExpand(view: View, duration: Long = DURATION_LONG) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0.8f
        view.visibility = View.VISIBLE

        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1.5f)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1.5f)
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.8f, 0f)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            this.duration = duration
            interpolator = DecelerateInterpolator()
            doOnEnd {
                view.visibility = View.GONE
                view.scaleX = 1f
                view.scaleY = 1f
                view.alpha = 1f
            }
            start()
        }
    }

    /**
     * 视图组合：按压和释放动画
     */
    fun setupPressAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    scaleOnPress(v)
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    scaleOnRelease(v)
                }
            }
            false
        }
    }

    /**
     * 创建自定义动画集合
     */
    fun createCustomAnimatorSet(vararg animators: Animator): AnimatorSet {
        return AnimatorSet().apply {
            playTogether(*animators)
            interpolator = FastOutSlowInInterpolator()
        }
    }
}