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

    /**
     * 3D翻转动画
     */
    fun flip3D(view: View, duration: Long = DEFAULT_DURATION) {
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
     * 弹性缩放动画
     */
    fun bounceScale(view: View, duration: Long = DEFAULT_DURATION) {
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f, 0.8f, 1.1f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f, 0.8f, 1.1f, 1f)

        AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator)
            this.duration = duration
            interpolator = BounceInterpolator()
            start()
        }
    }

    /**
     * 波浪动画
     */
    fun waveAnimation(view: View, duration: Long = DEFAULT_DURATION) {
        val waveAnimator = ObjectAnimator.ofFloat(
            view,
            "translationY",
            0f, -20f, 0f, -10f, 0f
        )

        waveAnimator.apply {
            this.duration = duration
            interpolator = defaultInterpolator
            start()
        }
    }

    /**
     * 发光效果动画
     */
    fun glowEffect(view: View, duration: Long = DEFAULT_DURATION) {
        val glowAnimator = ObjectAnimator.ofFloat(view, "alpha", 0.5f, 1f, 0.5f)
        glowAnimator.duration = duration
        glowAnimator.repeatCount = 3
        glowAnimator.interpolator = defaultInterpolator
        glowAnimator.start()
    }

    /**
     * 粒子爆炸动画
     */
    fun particleExplosion(view: View, duration: Long = DEFAULT_DURATION) {
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 2f, 0f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 1f, 2f, 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f, 0f)

        AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
            this.duration = duration
            interpolator = AccelerateInterpolator()
            start()
        }
    }

    /**
     * 数字滚动动画
     */
    fun numberRolling(
        startValue: Int,
        endValue: Int,
        duration: Long = LONG_DURATION,
        onUpdate: (Int) -> Unit
    ) {
        val animator = ValueAnimator.ofInt(startValue, endValue)
        animator.duration = duration
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            onUpdate(value)
        }
        animator.start()
    }

    /**
     * 进度条填充动画
     */
    fun progressFill(
        progressBar: android.widget.ProgressBar,
        targetProgress: Int,
        duration: Long = DEFAULT_DURATION,
        onComplete: (() -> Unit)? = null
    ) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress)
        animator.duration = duration
        animator.interpolator = defaultInterpolator
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                onComplete?.invoke()
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        animator.start()
    }

    /**
     * 卡片堆叠动画
     */
    fun cardStackAnimation(views: List<View>, duration: Long = DEFAULT_DURATION) {
        views.forEachIndexed { index, view ->
            view.translationY = (index * 20).toFloat()
            view.alpha = 1f - (index * 0.1f)
            view.scaleX = 1f - (index * 0.05f)
            view.scaleY = 1f - (index * 0.05f)
            
            view.animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(duration)
                .setStartDelay(index * 100L)
                .setInterpolator(defaultInterpolator)
                .start()
        }
    }

    /**
     * 打字机效果动画
     */
    fun typewriterEffect(
        textView: android.widget.TextView,
        text: String,
        duration: Long = 1000L,
        onComplete: (() -> Unit)? = null
    ) {
        textView.text = ""
        val charDuration = duration / text.length
        
        text.forEachIndexed { index, char ->
            textView.postDelayed({
                textView.text = text.substring(0, index + 1)
                if (index == text.length - 1) {
                    onComplete?.invoke()
                }
            }, index * charDuration)
        }
    }

    /**
     * 心跳动画
     */
    fun heartbeat(view: View, duration: Long = DEFAULT_DURATION) {
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f, 1.1f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f, 1.1f, 1f)

        AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator)
            this.duration = duration
            interpolator = defaultInterpolator
            start()
        }
    }

    /**
     * 彩虹色彩动画
     */
    fun rainbowEffect(view: View, duration: Long = DEFAULT_DURATION) {
        val colors = intArrayOf(
            android.graphics.Color.RED,
            android.graphics.Color.parseColor("#FFA500"), // Orange
            android.graphics.Color.YELLOW,
            android.graphics.Color.GREEN,
            android.graphics.Color.BLUE,
            android.graphics.Color.MAGENTA
        )
        
        val colorAnimator = ValueAnimator.ofInt(*colors)
        colorAnimator.duration = duration
        colorAnimator.repeatCount = ValueAnimator.INFINITE
        colorAnimator.addUpdateListener { animation ->
            val color = animation.animatedValue as Int
            view.setBackgroundColor(color)
        }
        colorAnimator.start()
        
        // 存储动画以便后续停止
        view.tag = colorAnimator
    }

    /**
     * 停止彩虹动画
     */
    fun stopRainbowEffect(view: View) {
        (view.tag as? ValueAnimator)?.cancel()
        view.tag = null
    }

    /**
     * 磁铁吸附动画
     */
    fun magneticAttraction(view: View, targetX: Float, targetY: Float, duration: Long = DEFAULT_DURATION) {
        val translateXAnimator = ObjectAnimator.ofFloat(view, "translationX", view.translationX, targetX)
        val translateYAnimator = ObjectAnimator.ofFloat(view, "translationY", view.translationY, targetY)
        val scaleAnimator = ObjectAnimator.ofFloat(view, "scaleX", view.scaleX, 1.2f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", view.scaleY, 1.2f)

        AnimatorSet().apply {
            playTogether(translateXAnimator, translateYAnimator, scaleAnimator, scaleYAnimator)
            this.duration = duration
            interpolator = AccelerateInterpolator()
            start()
        }
    }

    /**
     * 液体流动动画
     */
    fun liquidFlow(view: View, duration: Long = DEFAULT_DURATION) {
        val flowAnimator = ObjectAnimator.ofFloat(
            view,
            "translationX",
            0f, 50f, -30f, 20f, -10f, 0f
        )

        flowAnimator.apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * 全屏过渡动画
     */
    fun fullScreenTransition(
        fromView: View,
        toView: View,
        duration: Long = DEFAULT_DURATION,
        onComplete: (() -> Unit)? = null
    ) {
        // 淡出当前视图
        fadeOut(fromView, duration / 2)
        
        // 延迟显示新视图
        toView.postDelayed({
            fadeIn(toView, duration / 2)
            onComplete?.invoke()
        }, duration / 2)
    }

    /**
     * 创建自定义插值器
     */
    fun createCustomInterpolator(): Interpolator {
        return object : Interpolator {
            override fun getInterpolation(input: Float): Float {
                return if (input < 0.5f) {
                    2 * input * input
                } else {
                    1 - 2 * (1 - input) * (1 - input)
                }
            }
        }
    }
}
