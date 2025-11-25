package com.lanhe.gongjuxiang.utils

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.lanhe.gongjuxiang.R

/**
 * 功能介绍卡片扩展函数
 *
 * 简化在Activity中展示功能介绍的过程
 */
object FeatureInfoCardHelper {

    /**
     * 为功能介绍卡片设置数据并配置展开/折叠交互
     *
     * @param cardView 卡片根View
     * @param featureId 功能ID
     */
    fun setupFeatureInfoCard(cardView: View, featureId: String) {
        val featureInfo = FeatureInfoHelper.getFeatureInfo(featureId)

        if (featureInfo == null) {
            // 如果没有找到功能信息，隐藏卡片
            cardView.visibility = View.GONE
            return
        }

        // 获取所有视图元素
        val header = cardView.findViewById<LinearLayout>(R.id.layoutFeatureInfoHeader)
        val expandIcon = cardView.findViewById<ImageView>(R.id.ivFeatureInfoExpand)
        val scrollView = cardView.findViewById<ScrollView>(R.id.scrollViewFeatureInfo)

        val tvBrief = cardView.findViewById<TextView>(R.id.tvFeatureBrief)
        val tvDescription = cardView.findViewById<TextView>(R.id.tvFeatureDescription)
        val tvLogic = cardView.findViewById<TextView>(R.id.tvFeatureLogic)
        val tvPrinciple = cardView.findViewById<TextView>(R.id.tvFeaturePrinciple)
        val tvImplementation = cardView.findViewById<TextView>(R.id.tvFeatureImplementation)
        val tvResults = cardView.findViewById<TextView>(R.id.tvFeatureResults)
        val tvWarnings = cardView.findViewById<TextView>(R.id.tvFeatureWarnings)
        val layoutWarnings = cardView.findViewById<LinearLayout>(R.id.layoutWarnings)

        // 设置数据
        tvBrief.text = featureInfo.brief
        tvDescription.text = featureInfo.description
        tvLogic.text = featureInfo.optimizationLogic
        tvPrinciple.text = featureInfo.technicalPrinciple
        tvImplementation.text = featureInfo.implementationDetails
        tvResults.text = featureInfo.expectedResults

        // 设置注意事项（如果有）
        if (featureInfo.warnings.isNotBlank()) {
            tvWarnings.text = featureInfo.warnings
            layoutWarnings.visibility = View.VISIBLE
        } else {
            layoutWarnings.visibility = View.GONE
        }

        // 配置展开/折叠交互
        var isExpanded = false
        header.setOnClickListener {
            isExpanded = !isExpanded
            if (isExpanded) {
                // 展开
                scrollView.visibility = View.VISIBLE
                expandIcon.animate().rotation(180f).setDuration(200).start()
                AnimationUtils.fadeIn(scrollView, 200)
            } else {
                // 折叠
                AnimationUtils.fadeOut(scrollView, 200)
                expandIcon.animate().rotation(0f).setDuration(200).start()
            }
        }
    }

    /**
     * 展开功能介绍卡片
     */
    fun expandFeatureInfoCard(cardView: View) {
        val scrollView = cardView.findViewById<ScrollView>(R.id.scrollViewFeatureInfo)
        val expandIcon = cardView.findViewById<ImageView>(R.id.ivFeatureInfoExpand)

        scrollView.visibility = View.VISIBLE
        expandIcon.animate().rotation(180f).setDuration(200).start()
        AnimationUtils.fadeIn(scrollView, 200)
    }

    /**
     * 折叠功能介绍卡片
     */
    fun collapseFeatureInfoCard(cardView: View) {
        val scrollView = cardView.findViewById<ScrollView>(R.id.scrollViewFeatureInfo)
        val expandIcon = cardView.findViewById<ImageView>(R.id.ivFeatureInfoExpand)

        AnimationUtils.fadeOut(scrollView, 200)
        expandIcon.animate().rotation(0f).setDuration(200).start()
    }
}

// 扩展函数：简化调用
fun View.setupFeatureInfo(featureId: String) {
    FeatureInfoCardHelper.setupFeatureInfoCard(this, featureId)
}

fun View.expandFeatureInfo() {
    FeatureInfoCardHelper.expandFeatureInfoCard(this)
}

fun View.collapseFeatureInfo() {
    FeatureInfoCardHelper.collapseFeatureInfoCard(this)
}
