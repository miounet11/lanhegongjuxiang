package com.lanhe.gongjuxiang.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.lanhe.gongjuxiang.R

/**
 * 面包屑导航组件
 * 用于显示当前页面在应用中的位置层级
 */
class BreadcrumbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val breadcrumbItems = mutableListOf<BreadcrumbItem>()
    private var onBreadcrumbClickListener: OnBreadcrumbClickListener? = null

    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(16, 8, 16, 8)
    }

    /**
     * 面包屑项目数据类
     */
    data class BreadcrumbItem(
        val title: String,
        val isClickable: Boolean = true,
        val data: Any? = null
    )

    /**
     * 面包屑点击监听器
     */
    interface OnBreadcrumbClickListener {
        fun onBreadcrumbClick(item: BreadcrumbItem, position: Int)
    }

    /**
     * 设置面包屑路径
     */
    fun setBreadcrumbs(items: List<BreadcrumbItem>) {
        breadcrumbItems.clear()
        breadcrumbItems.addAll(items)
        buildBreadcrumbViews()
    }

    /**
     * 添加面包屑项目
     */
    fun addBreadcrumb(item: BreadcrumbItem) {
        breadcrumbItems.add(item)
        buildBreadcrumbViews()
    }

    /**
     * 移除最后一个面包屑项目
     */
    fun removeLastBreadcrumb() {
        if (breadcrumbItems.isNotEmpty()) {
            breadcrumbItems.removeAt(breadcrumbItems.size - 1)
            buildBreadcrumbViews()
        }
    }

    /**
     * 设置点击监听器
     */
    fun setOnBreadcrumbClickListener(listener: OnBreadcrumbClickListener) {
        this.onBreadcrumbClickListener = listener
    }

    /**
     * 构建面包屑视图
     */
    private fun buildBreadcrumbViews() {
        removeAllViews()

        breadcrumbItems.forEachIndexed { index, item ->
            // 添加文本视图
            val textView = createBreadcrumbTextView(item, index)
            addView(textView)

            // 添加分隔符（除了最后一个）
            if (index < breadcrumbItems.size - 1) {
                val separator = createSeparatorView()
                addView(separator)
            }
        }
    }

    /**
     * 创建面包屑文本视图
     */
    private fun createBreadcrumbTextView(item: BreadcrumbItem, position: Int): TextView {
        return TextView(context).apply {
            text = item.title

            // 样式设置
            textSize = 14f
            setPadding(8, 4, 8, 4)

            if (position == breadcrumbItems.size - 1) {
                // 当前页面（最后一个）
                setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_primary))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            } else {
                // 可点击的上级页面
                setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_onSurfaceVariant))
                if (item.isClickable) {
                    background = ContextCompat.getDrawable(context, android.R.drawable.btn_default)
                    isClickable = true
                    setOnClickListener {
                        onBreadcrumbClickListener?.onBreadcrumbClick(item, position)
                    }
                }
            }

            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
    }

    /**
     * 创建分隔符视图
     */
    private fun createSeparatorView(): TextView {
        return TextView(context).apply {
            text = ">"
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_onSurfaceVariant))

            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(8, 0, 8, 0)
            }
        }
    }

    /**
     * 便捷方法：设置简单的字符串面包屑
     */
    fun setBreadcrumbs(vararg titles: String) {
        val items = titles.map { BreadcrumbItem(it) }
        setBreadcrumbs(items)
    }

    /**
     * 便捷方法：从当前面包屑导航到指定层级
     */
    fun navigateToLevel(level: Int) {
        if (level < breadcrumbItems.size) {
            // 移除指定层级之后的所有项目
            val itemsToKeep = breadcrumbItems.take(level + 1)
            breadcrumbItems.clear()
            breadcrumbItems.addAll(itemsToKeep)
            buildBreadcrumbViews()
        }
    }

    /**
     * 获取当前面包屑路径
     */
    fun getCurrentPath(): List<String> {
        return breadcrumbItems.map { it.title }
    }

    /**
     * 清空面包屑
     */
    fun clear() {
        breadcrumbItems.clear()
        removeAllViews()
    }
}