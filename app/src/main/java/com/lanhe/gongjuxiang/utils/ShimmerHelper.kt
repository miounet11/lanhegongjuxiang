package com.lanhe.gongjuxiang.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.facebook.shimmer.ShimmerFrameLayout
import com.lanhe.gongjuxiang.R

/**
 * Shimmer加载效果辅助类
 * 提供统一的加载动画管理
 */
object ShimmerHelper {

    /**
     * 开始Shimmer动画
     */
    fun startShimmer(shimmerLayout: ShimmerFrameLayout?) {
        shimmerLayout?.startShimmer()
    }

    /**
     * 停止Shimmer动画
     */
    fun stopShimmer(shimmerLayout: ShimmerFrameLayout?) {
        shimmerLayout?.stopShimmer()
    }

    /**
     * 显示加载状态
     */
    fun showLoading(contentView: View, shimmerLayout: ShimmerFrameLayout) {
        contentView.visibility = View.GONE
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.startShimmer()
    }

    /**
     * 显示内容状态
     */
    fun showContent(contentView: View, shimmerLayout: ShimmerFrameLayout) {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        contentView.visibility = View.VISIBLE
    }

    /**
     * 创建网格卡片Shimmer布局
     */
    fun createGridCardShimmer(context: Context, parent: ViewGroup): View {
        return LayoutInflater.from(context)
            .inflate(R.layout.shimmer_grid_card, parent, false)
    }

    /**
     * 创建列表项Shimmer布局
     */
    fun createListItemShimmer(context: Context, parent: ViewGroup): View {
        return LayoutInflater.from(context)
            .inflate(R.layout.shimmer_list_item, parent, false)
    }

    /**
     * 创建系统状态Shimmer布局
     */
    fun createStatusShimmer(context: Context, parent: ViewGroup): View {
        return LayoutInflater.from(context)
            .inflate(R.layout.shimmer_status_card, parent, false)
    }

    /**
     * 设置Shimmer自定义属性
     */
    fun configureShimmer(shimmerLayout: ShimmerFrameLayout) {
        // Shimmer配置通过XML实现，这里保持空实现
    }
}