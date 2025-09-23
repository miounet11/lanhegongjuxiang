package com.lanhe.gongjuxiang.utils

import android.animation.*
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.*
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 蓝河助手 - 高级动画管理器
 *
 * 功能特性：
 * - Lottie动画支持
 * - 物理弹簧动画
 * - 自定义转场动画
 * - 揭露动画效果
 * - 加载动画管理
 * - 手势动画响应
 * - 性能优化动画
 */
class AdvancedAnimationManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AdvancedAnimationManager"

        @Volatile
        private var INSTANCE: AdvancedAnimationManager? = null

        fun getInstance(context: Context): AdvancedAnimationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdvancedAnimationManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // 默认动画参数
        private const val DEFAULT_DURATION = 300L
        private const val DEFAULT_SPRING_DAMPINESS = 0.8f
        private const val DEFAULT_SPRING_STIFFNESS = 200f
        private const val DEFAULT_FLING_FRICTION = 1.1f
    }

    private val analyticsManager = AnalyticsManager.getInstance(context)
    private val animationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 动画缓存
    private val animatorCache = mutableMapOf<String, Animator>()
    private val lottieCache = mutableMapOf<String, LottieComposition>()

    /**
     * 动画配置类
     */
    data class AnimationConfig(
        val duration: Long = DEFAULT_DURATION,
        val delay: Long = 0,
        val interpolator: Interpolator = FastOutSlowInInterpolator(),
        val repeatCount: Int = 0,
        val repeatMode: Int = ValueAnimator.RESTART,
        val autoStart: Boolean = true
    )

    /**
     * 弹簧动画配置
     */
    data class SpringConfig(
        val dampingRatio: Float = DEFAULT_SPRING_DAMPINESS,
        val stiffness: Float = DEFAULT_SPRING_STIFFNESS,
        val finalPosition: Float = 0f,
        val minimumVisibleChange: Float = DynamicAnimation.MIN_VISIBLE_CHANGE_PIXELS
    )

    /**
     * Lottie动画配置
     */
    data class LottieConfig(
        val fileName: String,
        val loop: Boolean = false,
        val autoPlay: Boolean = true,
        val speed: Float = 1f,
        val minFrame: Int? = null,
        val maxFrame: Int? = null
    )

    /**
     * 创建淡入动画
     */
    fun createFadeInAnimation(
        view: View,
        config: AnimationConfig = AnimationConfig()
    ): Animator {
        return ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = config.duration
            startDelay = config.delay
            interpolator = config.interpolator
            repeatCount = config.repeatCount
            repeatMode = config.repeatMode

            if (config.autoStart) start()
        }
    }

    /**
     * 创建淡出动画
     */
    fun createFadeOutAnimation(
        view: View,
        config: AnimationConfig = AnimationConfig()
    ): Animator {
        return ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
            duration = config.duration
            startDelay = config.delay
            interpolator = config.interpolator
            repeatCount = config.repeatCount
            repeatMode = config.repeatMode

            if (config.autoStart) start()
        }
    }

    /**
     * 创建缩放动画
     */
    fun createScaleAnimation(
        view: View,
        fromScale: Float = 0f,
        toScale: Float = 1f,
        config: AnimationConfig = AnimationConfig()
    ): Animator {
        val animatorSet = AnimatorSet()
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", fromScale, toScale)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", fromScale, toScale)

        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = config.duration
        animatorSet.startDelay = config.delay
        animatorSet.interpolator = config.interpolator

        if (config.autoStart) animatorSet.start()
        return animatorSet
    }

    /**
     * 创建滑入动画
     */
    fun createSlideInAnimation(
        view: View,
        direction: SlideDirection,
        distance: Float? = null,
        config: AnimationConfig = AnimationConfig()
    ): Animator {
        val actualDistance = distance ?: when (direction) {
            SlideDirection.LEFT, SlideDirection.RIGHT -> view.width.toFloat()
            SlideDirection.TOP, SlideDirection.BOTTOM -> view.height.toFloat()
        }

        val (property, startValue, endValue) = when (direction) {
            SlideDirection.LEFT -> Triple("translationX", actualDistance, 0f)
            SlideDirection.RIGHT -> Triple("translationX", -actualDistance, 0f)
            SlideDirection.TOP -> Triple("translationY", actualDistance, 0f)
            SlideDirection.BOTTOM -> Triple("translationY", -actualDistance, 0f)
        }

        return ObjectAnimator.ofFloat(view, property, startValue, endValue).apply {
            duration = config.duration
            startDelay = config.delay
            interpolator = config.interpolator
            repeatCount = config.repeatCount
            repeatMode = config.repeatMode

            if (config.autoStart) start()
        }
    }

    /**
     * 滑动方向枚举
     */
    enum class SlideDirection {
        LEFT, RIGHT, TOP, BOTTOM
    }

    /**
     * 创建弹簧动画
     */
    fun createSpringAnimation(
        view: View,
        property: DynamicAnimation.ViewProperty,
        springConfig: SpringConfig = SpringConfig()
    ): SpringAnimation {
        return SpringAnimation(view, property, springConfig.finalPosition).apply {
            spring = SpringForce(springConfig.finalPosition).apply {
                dampingRatio = springConfig.dampingRatio
                stiffness = springConfig.stiffness
            }
            minimumVisibleChange = springConfig.minimumVisibleChange
            start()
        }
    }

    /**
     * 创建投掷动画
     */
    fun createFlingAnimation(
        view: View,
        property: DynamicAnimation.ViewProperty,
        startVelocity: Float,
        friction: Float = DEFAULT_FLING_FRICTION
    ): FlingAnimation {
        return FlingAnimation(view, property).apply {
            setStartVelocity(startVelocity)
            setFriction(friction)
            start()
        }
    }

    /**
     * 创建揭露动画
     */
    fun createRevealAnimation(
        view: View,
        centerX: Int? = null,
        centerY: Int? = null,
        startRadius: Float = 0f,
        endRadius: Float? = null,
        config: AnimationConfig = AnimationConfig()
    ): Animator? {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val cx = centerX ?: view.width / 2
            val cy = centerY ?: view.height / 2
            val finalRadius = endRadius ?: Math.hypot(view.width.toDouble(), view.height.toDouble()).toFloat()

            return ViewAnimationUtils.createCircularReveal(view, cx, cy, startRadius, finalRadius).apply {
                duration = config.duration
                startDelay = config.delay
                interpolator = config.interpolator

                if (config.autoStart) start()
            }
        }
        return null
    }

    /**
     * 加载Lottie动画
     */
    suspend fun loadLottieAnimation(fileName: String): LottieComposition? = suspendCoroutine { continuation ->
        // 检查缓存
        lottieCache[fileName]?.let {
            continuation.resume(it)
            return@suspendCoroutine
        }

        LottieCompositionFactory.fromAsset(context, fileName)
            .addListener { composition ->
                lottieCache[fileName] = composition
                continuation.resume(composition)
            }
            .addFailureListener { exception ->
                analyticsManager.trackError("lottie_load_failed", exception.message ?: "Unknown error", exception)
                continuation.resume(null)
            }
    }

    /**
     * 设置Lottie动画
     */
    suspend fun setupLottieAnimation(
        animationView: LottieAnimationView,
        config: LottieConfig
    ): Boolean {
        return try {
            val composition = loadLottieAnimation(config.fileName)
            if (composition != null) {
                animationView.apply {
                    setComposition(composition)
                    repeatCount = if (config.loop) ValueAnimator.INFINITE else 0
                    speed = config.speed

                    config.minFrame?.let { min ->
                        config.maxFrame?.let { max ->
                            setMinAndMaxFrame(min, max)
                        }
                    }

                    if (config.autoPlay) {
                        playAnimation()
                    }
                }

                analyticsManager.trackEvent("lottie_animation_setup", android.os.Bundle().apply {
                    putString("animation_file", config.fileName)
                    putBoolean("auto_play", config.autoPlay)
                    putBoolean("loop", config.loop)
                })

                true
            } else {
                false
            }
        } catch (e: Exception) {
            analyticsManager.trackError("lottie_setup_failed", e.message ?: "Unknown error", e)
            false
        }
    }

    /**
     * 创建加载动画
     */
    fun createLoadingAnimation(view: View): Animator {
        val rotateAnimation = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        val scaleUpAnimation = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
        }

        val scaleUpAnimationY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
        }

        return AnimatorSet().apply {
            playTogether(rotateAnimation, scaleUpAnimation, scaleUpAnimationY)
            start()
        }
    }

    /**
     * 创建脉冲动画
     */
    fun createPulseAnimation(view: View, config: AnimationConfig = AnimationConfig()): Animator {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f)

        return AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = config.duration
            startDelay = config.delay
            interpolator = config.interpolator
            repeatCount = config.repeatCount
            repeatMode = config.repeatMode

            if (config.autoStart) start()
        }
    }

    /**
     * 创建摇摆动画
     */
    fun createShakeAnimation(view: View, intensity: Float = 10f): Animator {
        return ObjectAnimator.ofFloat(
            view, "translationX",
            0f, intensity, -intensity, intensity, -intensity, intensity/2, -intensity/2, 0f
        ).apply {
            duration = 500
            interpolator = LinearInterpolator()
            start()
        }
    }

    /**
     * 创建弹跳动画
     */
    fun createBounceAnimation(view: View, config: AnimationConfig = AnimationConfig()): Animator {
        val bounceInterpolator = BounceInterpolator()

        return ObjectAnimator.ofFloat(view, "translationY", 0f, -100f, 0f).apply {
            duration = config.duration
            startDelay = config.delay
            interpolator = bounceInterpolator
            repeatCount = config.repeatCount
            repeatMode = config.repeatMode

            if (config.autoStart) start()
        }
    }

    /**
     * 创建翻转动画
     */
    fun createFlipAnimation(view: View, axis: FlipAxis = FlipAxis.Y): Animator {
        val property = when (axis) {
            FlipAxis.X -> "rotationX"
            FlipAxis.Y -> "rotationY"
        }

        return ObjectAnimator.ofFloat(view, property, 0f, 180f).apply {
            duration = 600
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    enum class FlipAxis { X, Y }

    /**
     * 创建共享元素转场动画
     */
    fun createSharedElementTransition(
        container: ViewGroup,
        sharedElements: Map<View, String>
    ): TransitionSet {
        val transitionSet = TransitionSet().apply {
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
            addTransition(ChangeImageTransform())
            duration = DEFAULT_DURATION
        }

        sharedElements.forEach { (view, transitionName) ->
            view.transitionName = transitionName
        }

        TransitionManager.beginDelayedTransition(container, transitionSet)
        return transitionSet
    }

    /**
     * 创建列表项动画
     */
    fun createListItemAnimation(view: View, position: Int): Animator {
        view.alpha = 0f
        view.translationY = 100f

        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val slideUp = ObjectAnimator.ofFloat(view, "translationY", 100f, 0f)

        return AnimatorSet().apply {
            playTogether(fadeIn, slideUp)
            duration = 300
            startDelay = (position * 50).toLong() // 错开动画时间
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    /**
     * 创建物理基础的拖拽动画
     */
    fun createDragAnimation(
        view: View,
        velocityX: Float,
        velocityY: Float,
        bounds: android.graphics.RectF? = null
    ): AnimatorSet {
        val flingX = createFlingAnimation(view, DynamicAnimation.TRANSLATION_X, velocityX)
        val flingY = createFlingAnimation(view, DynamicAnimation.TRANSLATION_Y, velocityY)

        // 添加边界检查
        bounds?.let { rect ->
            flingX.setMaxValue(rect.right - view.width)
            flingX.setMinValue(rect.left)
            flingY.setMaxValue(rect.bottom - view.height)
            flingY.setMinValue(rect.top)
        }

        return AnimatorSet().apply {
            // 注意：FlingAnimation不能直接添加到AnimatorSet中
            // 这里只是为了管理生命周期
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    flingX.start()
                    flingY.start()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    flingX.cancel()
                    flingY.cancel()
                }
            })
        }
    }

    /**
     * 预加载动画资源
     */
    fun preloadAnimations(animations: List<String>) {
        animationScope.launch {
            animations.forEach { fileName ->
                try {
                    loadLottieAnimation(fileName)
                } catch (e: Exception) {
                    analyticsManager.trackError("animation_preload_failed", e.message ?: "Unknown error", e)
                }
            }
        }
    }

    /**
     * 清除动画缓存
     */
    fun clearAnimationCache() {
        animatorCache.clear()
        lottieCache.clear()
        analyticsManager.trackFeatureUsed("animation_cache_cleared")
    }

    /**
     * 获取缓存统计
     */
    fun getCacheStats(): Map<String, Int> {
        return mapOf(
            "animator_cache_size" to animatorCache.size,
            "lottie_cache_size" to lottieCache.size
        )
    }

    /**
     * 停止所有动画
     */
    fun stopAllAnimations() {
        animatorCache.values.forEach { animator ->
            if (animator.isRunning) {
                animator.cancel()
            }
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        stopAllAnimations()
        clearAnimationCache()
        animationScope.coroutineContext.cancelChildren()
    }
}

/**
 * 扩展函数
 */
fun View.fadeIn(duration: Long = 300) {
    AdvancedAnimationManager.getInstance(context).createFadeInAnimation(this,
        AdvancedAnimationManager.AnimationConfig(duration = duration))
}

fun View.fadeOut(duration: Long = 300) {
    AdvancedAnimationManager.getInstance(context).createFadeOutAnimation(this,
        AdvancedAnimationManager.AnimationConfig(duration = duration))
}

fun View.scaleIn(duration: Long = 300) {
    AdvancedAnimationManager.getInstance(context).createScaleAnimation(this,
        fromScale = 0f, toScale = 1f,
        AdvancedAnimationManager.AnimationConfig(duration = duration))
}

fun View.pulse(duration: Long = 1000) {
    AdvancedAnimationManager.getInstance(context).createPulseAnimation(this,
        AdvancedAnimationManager.AnimationConfig(duration = duration, repeatCount = ValueAnimator.INFINITE))
}

fun View.shake() {
    AdvancedAnimationManager.getInstance(context).createShakeAnimation(this)
}

fun View.bounce() {
    AdvancedAnimationManager.getInstance(context).createBounceAnimation(this)
}