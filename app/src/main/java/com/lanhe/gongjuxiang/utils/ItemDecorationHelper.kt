package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView装饰器辅助类
 * 提供统一的间距和分隔线管理
 */
object ItemDecorationHelper {

    /**
     * 创建网格间距装饰器
     */
    fun createGridSpacingDecoration(
        spacing: Int,
        includeEdge: Boolean = true
    ): RecyclerView.ItemDecoration {
        return GridSpacingItemDecoration(spacing, includeEdge)
    }

    /**
     * 创建线性间距装饰器
     */
    fun createLinearSpacingDecoration(
        spacing: Int,
        orientation: Int = RecyclerView.VERTICAL
    ): RecyclerView.ItemDecoration {
        return LinearSpacingItemDecoration(spacing, orientation)
    }

    /**
     * 从dp转换为px
     */
    fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * 网格间距装饰器
     */
    class GridSpacingItemDecoration(
        private val spacing: Int,
        private val includeEdge: Boolean
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val layoutManager = parent.layoutManager as? GridLayoutManager ?: return
            val spanCount = layoutManager.spanCount
            val column = position % spanCount

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount

                if (position < spanCount) {
                    outRect.top = spacing
                }
                outRect.bottom = spacing
            } else {
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount
                if (position >= spanCount) {
                    outRect.top = spacing
                }
            }
        }
    }

    /**
     * 线性间距装饰器
     */
    class LinearSpacingItemDecoration(
        private val spacing: Int,
        private val orientation: Int
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val itemCount = state.itemCount

            when (orientation) {
                RecyclerView.VERTICAL -> {
                    if (position != itemCount - 1) {
                        outRect.bottom = spacing
                    }
                }
                RecyclerView.HORIZONTAL -> {
                    if (position != itemCount - 1) {
                        outRect.right = spacing
                    }
                }
            }
        }
    }
}