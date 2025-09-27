package com.lanhe.gongjuxiang.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.lanhe.gongjuxiang.R
import kotlin.math.min

/**
 * 自定义圆形进度条视图
 * 支持动画、渐变颜色和趋势指示
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 画笔
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 属性
    private var progress = 0f
    private var animatedProgress = 0f
    private var maxProgress = 100f
    private var strokeWidth = 12f
    private var title = ""
    private var value = ""
    private var unit = "%"

    // 颜色
    private var backgroundStrokeColor = Color.LTGRAY
    private var progressStartColor = Color.GREEN
    private var progressEndColor = Color.RED
    private var textColor = Color.BLACK
    private var titleColor = Color.GRAY

    // 动画
    private var animator: ValueAnimator? = null

    // 趋势
    private var trend = Trend.NONE
    private var showTrend = false

    // 渐变
    private var progressGradient: SweepGradient? = null

    enum class Trend {
        UP, DOWN, NONE
    }

    init {
        setupDefaultValues()
        setupPaints()

        // 读取自定义属性
        attrs?.let { parseAttributes(it) }
    }

    private fun setupDefaultValues() {
        strokeWidth = context.resources.getDimension(R.dimen.spacing_small)
        backgroundStrokeColor = ContextCompat.getColor(context, R.color.md_theme_light_outline)
        progressStartColor = ContextCompat.getColor(context, R.color.status_success)
        progressEndColor = ContextCompat.getColor(context, R.color.status_danger)
        textColor = ContextCompat.getColor(context, R.color.md_theme_light_onSurface)
        titleColor = ContextCompat.getColor(context, R.color.md_theme_light_onSurfaceVariant)
    }

    private fun setupPaints() {
        backgroundPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = this@CircularProgressView.strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = backgroundStrokeColor
        }

        progressPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = this@CircularProgressView.strokeWidth
            strokeCap = Paint.Cap.ROUND
        }

        textPaint.apply {
            textAlign = Paint.Align.CENTER
            color = textColor
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
        }

        iconPaint.apply {
            textAlign = Paint.Align.CENTER
            color = titleColor
            textSize = 16f
        }
    }

    private fun parseAttributes(attrs: AttributeSet) {
        // 这里可以读取自定义属性
        // 暂时使用默认值
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
    }

    private fun updateGradient() {
        val centerX = width / 2f
        val centerY = height / 2f

        progressGradient = SweepGradient(
            centerX, centerY,
            intArrayOf(progressStartColor, progressEndColor),
            null
        )
        progressPaint.shader = progressGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(centerX, centerY) - strokeWidth / 2

        // 绘制背景圆环
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // 绘制进度圆弧
        val sweepAngle = (animatedProgress / maxProgress) * 360f
        val oval = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)

        // 绘制文本
        drawText(canvas, centerX, centerY)

        // 绘制趋势指示器
        if (showTrend && trend != Trend.NONE) {
            drawTrendIndicator(canvas, centerX, centerY)
        }
    }

    private fun drawText(canvas: Canvas, centerX: Float, centerY: Float) {
        // 绘制主要数值
        val valueText = if (value.isNotEmpty()) value else "${animatedProgress.toInt()}"
        textPaint.textSize = 28f
        textPaint.color = textColor
        canvas.drawText(valueText, centerX, centerY + 8f, textPaint)

        // 绘制单位
        if (unit.isNotEmpty()) {
            textPaint.textSize = 14f
            textPaint.color = titleColor
            canvas.drawText(unit, centerX + textPaint.measureText(valueText) / 2 + 8f, centerY - 4f, textPaint)
        }

        // 绘制标题
        if (title.isNotEmpty()) {
            textPaint.textSize = 12f
            textPaint.color = titleColor
            canvas.drawText(title, centerX, centerY + 32f, textPaint)
        }
    }

    private fun drawTrendIndicator(canvas: Canvas, centerX: Float, centerY: Float) {
        val arrowSize = 16f
        val arrowX = centerX + 36f
        val arrowY = centerY - 16f

        val path = Path()
        when (trend) {
            Trend.UP -> {
                // 向上箭头
                path.moveTo(arrowX, arrowY)
                path.lineTo(arrowX - arrowSize / 2, arrowY + arrowSize)
                path.lineTo(arrowX + arrowSize / 2, arrowY + arrowSize)
                path.close()
                progressPaint.color = progressStartColor
            }
            Trend.DOWN -> {
                // 向下箭头
                path.moveTo(arrowX, arrowY + arrowSize)
                path.lineTo(arrowX - arrowSize / 2, arrowY)
                path.lineTo(arrowX + arrowSize / 2, arrowY)
                path.close()
                progressPaint.color = progressEndColor
            }
            Trend.NONE -> return
        }

        canvas.drawPath(path, progressPaint)
        // 恢复渐变
        progressPaint.shader = progressGradient
    }

    /**
     * 设置进度值（带动画）
     */
    fun setProgress(newProgress: Float, animate: Boolean = true) {
        val clampedProgress = newProgress.coerceIn(0f, maxProgress)

        if (!animate) {
            progress = clampedProgress
            animatedProgress = clampedProgress
            invalidate()
            return
        }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(animatedProgress, clampedProgress).apply {
            duration = 1000
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                animatedProgress = animation.animatedValue as Float
                invalidate()
            }
            start()
        }

        progress = clampedProgress
    }

    /**
     * 设置最大进度值
     */
    fun setMaxProgress(max: Float) {
        maxProgress = max
        invalidate()
    }

    /**
     * 设置标题
     */
    fun setTitle(newTitle: String) {
        title = newTitle
        invalidate()
    }

    /**
     * 设置数值文本
     */
    fun setValue(newValue: String) {
        value = newValue
        invalidate()
    }

    /**
     * 设置单位
     */
    fun setUnit(newUnit: String) {
        unit = newUnit
        invalidate()
    }

    /**
     * 设置颜色
     */
    fun setProgressColors(startColor: Int, endColor: Int) {
        progressStartColor = startColor
        progressEndColor = endColor
        updateGradient()
        invalidate()
    }

    /**
     * 设置趋势指示
     */
    fun setTrend(newTrend: Trend, show: Boolean = true) {
        trend = newTrend
        showTrend = show
        invalidate()
    }

    /**
     * 获取当前进度
     */
    fun getProgress(): Float = progress

    /**
     * 设置进度颜色基于状态
     */
    fun setStatusColors(status: Status) {
        val colors = when (status) {
            Status.GOOD -> Pair(
                ContextCompat.getColor(context, R.color.status_success),
                ContextCompat.getColor(context, R.color.status_success)
            )
            Status.WARNING -> Pair(
                ContextCompat.getColor(context, R.color.status_warning),
                ContextCompat.getColor(context, R.color.status_warning)
            )
            Status.CRITICAL -> Pair(
                ContextCompat.getColor(context, R.color.status_danger),
                ContextCompat.getColor(context, R.color.status_danger)
            )
            Status.UNKNOWN -> Pair(
                ContextCompat.getColor(context, R.color.md_theme_light_outline),
                ContextCompat.getColor(context, R.color.md_theme_light_outline)
            )
        }
        setProgressColors(colors.first, colors.second)
    }

    enum class Status {
        GOOD, WARNING, CRITICAL, UNKNOWN
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}