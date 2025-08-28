package com.lanhe.gongjuxiang.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * 动画工具类
 * 提供各种动画效果，用于提升用户体验
 */
object AnimationUtils {

    // 默认动画时长
    private const val DEFAULT_DURATION = 300L
    private const val LONG_DURATION = 500L

    // 默认插值器
    private val defaultInterpolator = FastOutSlowInInterpolator()

    /**
     * 淡入动画
     */
    fun fadeIn(view: View, duration: Long = DEFAULT_DURATION) {
        view.alpha = 0f
        view.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            this.duration = duration
            interpolator = defaultInterpolator
            start()
        }
    }

    /**
     * 淡出动画
     */
    fun fadeOut(view: View, duration: Long = DEFAULT_DURATION) {
        ObjectAnimator.ofFloat(view, "alpha", view.alpha, 0f).apply {
            this.duration = duration
            interpolator = defaultInterpolator
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    /**
     * 缩放进入动画
     */
    fun scaleIn(view: View, duration: Long = DEFAULT_DURATION) {
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.alpha = 0f
        view.visibility = View.VISIBLE

        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
            this.duration = duration
            interpolator = defaultInterpolator
            start()
        }
    }

    /**
     * 缩放退出动画
     */
    fun scaleOut(view: View, duration: Long = DEFAULT_DURATION) {
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", view.scaleX, 0.8f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", view.scaleY, 0.8f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", view.alpha, 0f)

        AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
            this.duration = duration
            interpolator = defaultInterpolator
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    /**
     * 滑动进入动画（从底部）
     */
    fun slideInFromBottom(view: View, duration: Long = DEFAULT_DURATION) {
        val parent = view.parent as? ViewGroup ?: return

        val slideAnimator = ObjectAnimator.ofFloat(
            view,
            "translationY",
            parent.height.toFloat(),
            0f
        )
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        AnimatorSet().apply {
            playTogether(slideAnimator, alphaAnimator)
            this.duration = duration
            interpolator = defaultInterpolator
            start()
        }
    }

    /**
     * 滑动退出动画（到底部）
     */
    fun slideOutToBottom(view: View, duration: Long = DEFAULT_DURATION) {
        val parent = view.parent as? ViewGroup ?: return

        val slideAnimator = ObjectAnimator.ofFloat(
            view,
            "translationY",
            view.translationY,
            parent.height.toFloat()
        )
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", view.alpha, 0f)

        AnimatorSet().apply {
            playTogether(slideAnimator, alphaAnimator)
            this.duration = duration
            interpolator = defaultInterpolator
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    /**
     * 脉冲动画
     */
    fun pulse(view: View, duration: Long = DEFAULT_DURATION) {
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)

        AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator)
            this.duration = duration
            interpolator = defaultInterpolator
            start()
        }
    }

    /**
     * 摇晃动画
     */
    fun shake(view: View, duration: Long = DEFAULT_DURATION) {
        val shakeAnimator = ObjectAnimator.ofFloat(
            view,
            "translationX",
            0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f
        )

        shakeAnimator.apply {
            this.duration = duration
            interpolator = defaultInterpolator
            start()
        }
    }

    /**
     * 旋转动画
     */
    fun rotate(view: View, degrees: Float = 360f, duration: Long = DEFAULT_DURATION) {
        ObjectAnimator.ofFloat(view, "rotation", 0f, degrees).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            start()
        }
    }

    /**
     * 数字变化动画
     */
    fun animateNumber(
        startValue: Int,
        endValue: Int,
        duration: Long = LONG_DURATION,
        onUpdate: (Int) -> Unit
    ) {
        ValueAnimator.ofInt(startValue, endValue).apply {
            this.duration = duration
            interpolator = defaultInterpolator
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                onUpdate(value)
            }
            start()
        }
    }

    /**
     * 百分比变化动画
     */
    fun animatePercentage(
        startValue: Float,
        endValue: Float,
        duration: Long = LONG_DURATION,
        onUpdate: (Float) -> Unit
    ) {
        ValueAnimator.ofFloat(startValue, endValue).apply {
            this.duration = duration
            interpolator = defaultInterpolator
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                onUpdate(value)
            }
            start()
        }
    }

    /**
     * 进度条动画
     */
    fun animateProgress(
        progressBar: android.widget.ProgressBar,
        targetProgress: Int,
        duration: Long = DEFAULT_DURATION
    ) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, targetProgress)
        animator.duration = duration
        animator.interpolator = defaultInterpolator
        animator.start()
    }

    /**
     * 卡片翻转动画
     */
    fun flipCard(view: View, duration: Long = DEFAULT_DURATION) {
        val flipOutAnimator = ObjectAnimator.ofFloat(view, "rotationY", 0f, 90f)
        val flipInAnimator = ObjectAnimator.ofFloat(view, "rotationY", -90f, 0f)

        AnimatorSet().apply {
            playSequentially(flipOutAnimator, flipInAnimator)
            this.duration = duration / 2
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * 波纹扩散动画
     */
    fun rippleEffect(view: View, duration: Long = LONG_DURATION) {
        val rippleAnimator = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 1f, 0.3f)
        rippleAnimator.duration = duration
        rippleAnimator.repeatCount = 2
        rippleAnimator.interpolator = defaultInterpolator
        rippleAnimator.start()
    }

    /**
     * 列表项动画
     */
    fun animateListItem(view: View, position: Int, duration: Long = DEFAULT_DURATION) {
        view.translationX = view.width.toFloat()
        view.alpha = 0f

        view.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(defaultInterpolator)
            .setStartDelay(position * 100L)
            .start()
    }

    /**
     * 按钮点击反馈动画
     */
    fun buttonPressFeedback(view: View, duration: Long = 150L) {
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f)
        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)

        AnimatorSet().apply {
            play(scaleDownX).with(scaleDownY)
            play(scaleUpX).with(scaleUpY).after(scaleDownX)
            this.duration = duration / 2
            interpolator = defaultInterpolator
            start()
        }
    }

    /**
     * 加载动画
     */
    fun startLoadingAnimation(view: View) {
        val rotateAnimator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
        rotateAnimator.duration = 1000L
        rotateAnimator.repeatCount = ValueAnimator.INFINITE
        rotateAnimator.interpolator = LinearInterpolator()
        rotateAnimator.start()

        // 存储动画以便后续停止
        view.tag = rotateAnimator
    }

    /**
     * 停止加载动画
     */
    fun stopLoadingAnimation(view: View) {
        (view.tag as? Animator)?.cancel()
        view.tag = null
    }

    /**
     * 成功动画
     */
    fun successAnimation(view: View, duration: Long = DEFAULT_DURATION) {
        // 先放大
        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f)

        // 再缩小回原大小
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1.2f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.2f, 1f)

        AnimatorSet().apply {
            play(scaleUpX).with(scaleUpY)
            play(scaleDownX).with(scaleDownY).after(scaleUpX)
            this.duration = duration / 2
            interpolator = defaultInterpolator
            start()
        }
    }

    /**
     * 错误动画
     */
    fun errorAnimation(view: View, duration: Long = DEFAULT_DURATION) {
        shake(view, duration)
        rippleEffect(view, duration * 2)
    }

    /**
     * 强调动画
     */
    fun highlightAnimation(view: View, duration: Long = DEFAULT_DURATION) {
        val originalColor = view.background

        // 改变背景色
        view.setBackgroundColor(android.graphics.Color.parseColor("#FFF3CD"))

        // 延迟恢复
        view.postDelayed({
            view.background = originalColor
        }, duration)
    }
}
