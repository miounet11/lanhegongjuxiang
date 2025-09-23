package com.lanhe.gongjuxiang.ui.transformers

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.max

/**
 * Advanced page transformers for premium ViewPager2 animations
 */
object AdvancedPageTransformers {

    /**
     * Depth page transformer with parallax effect
     */
    class DepthPageTransformer : ViewPager2.PageTransformer {
        private val minScale = 0.75f

        override fun transformPage(view: View, position: Float) {
            val pageWidth = view.width

            when {
                position < -1 -> { // [-Infinity,-1)
                    view.alpha = 0f
                }
                position <= 0 -> { // [-1,0]
                    view.alpha = 1f
                    view.translationX = 0f
                    view.scaleX = 1f
                    view.scaleY = 1f
                }
                position <= 1 -> { // (0,1]
                    view.alpha = 1 - position
                    view.translationX = pageWidth * -position
                    val scaleFactor = minScale + (1 - minScale) * (1 - abs(position))
                    view.scaleX = scaleFactor
                    view.scaleY = scaleFactor
                }
                else -> { // (1,+Infinity]
                    view.alpha = 0f
                }
            }
        }
    }

    /**
     * Zoom out page transformer with smooth scaling
     */
    class ZoomOutPageTransformer : ViewPager2.PageTransformer {
        private val minScale = 0.85f
        private val minAlpha = 0.5f

        override fun transformPage(view: View, position: Float) {
            val pageWidth = view.width
            val pageHeight = view.height

            when {
                position < -1 -> { // [-Infinity,-1)
                    view.alpha = 0f
                }
                position <= 1 -> { // [-1,1]
                    val scaleFactor = max(minScale, 1 - abs(position))
                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
                    val horzMargin = pageWidth * (1 - scaleFactor) / 2

                    if (position < 0) {
                        view.translationX = horzMargin - vertMargin / 2
                    } else {
                        view.translationX = -horzMargin + vertMargin / 2
                    }

                    view.scaleX = scaleFactor
                    view.scaleY = scaleFactor
                    view.alpha = minAlpha + (scaleFactor - minScale) / (1 - minScale) * (1 - minAlpha)
                }
                else -> { // (1,+Infinity]
                    view.alpha = 0f
                }
            }
        }
    }

    /**
     * Cube rotation transformer
     */
    class CubeInRotationTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            view.cameraDistance = 20000f

            when {
                position < -1 -> {
                    view.alpha = 0f
                }
                position <= 0 -> {
                    view.alpha = 1f
                    view.pivotX = view.width.toFloat()
                    view.rotationY = 90 * abs(position)
                }
                position <= 1 -> {
                    view.alpha = 1f
                    view.pivotX = 0f
                    view.rotationY = -90 * abs(position)
                }
                else -> {
                    view.alpha = 0f
                }
            }
        }
    }

    /**
     * Parallax transformer with layered effect
     */
    class ParallaxTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            val pageWidth = view.width

            when {
                position < -1 -> { // [-Infinity,-1)
                    view.alpha = 0.1f
                }
                position <= 1 -> { // [-1,1]
                    // Background moves slower than foreground
                    view.findViewById<View>(android.R.id.background)?.let { background ->
                        background.translationX = pageWidth * position * 0.5f
                    }

                    // Foreground content
                    view.findViewById<View>(android.R.id.content)?.let { content ->
                        content.translationX = pageWidth * position * 0.8f
                        content.alpha = 1 - abs(position) * 0.3f
                    }

                    // Default transformation for the entire view
                    view.translationX = pageWidth * position * 0.75f
                    view.alpha = max(0.5f, 1 - abs(position))
                }
                else -> { // (1,+Infinity]
                    view.alpha = 0.1f
                }
            }
        }
    }

    /**
     * Stack transformer for card-like stacking effect
     */
    class StackTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            when {
                position <= 0 -> {
                    view.alpha = 1f
                    view.translationX = 0f
                    view.scaleX = 1f
                    view.scaleY = 1f
                }
                position <= 1 -> {
                    view.alpha = 1 - position
                    view.translationX = view.width * -position
                    view.scaleX = 1 - position * 0.1f
                    view.scaleY = 1 - position * 0.1f
                }
                else -> {
                    view.alpha = 0f
                }
            }
        }
    }

    /**
     * Flip horizontal transformer
     */
    class FlipHorizontalTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            val rotation = 180f * position
            view.cameraDistance = 20000f

            when {
                position < -0.5f -> {
                    view.visibility = View.INVISIBLE
                }
                position < 0f -> {
                    view.visibility = View.VISIBLE
                    view.translationX = view.width * position
                    view.rotationY = rotation
                }
                position < 0.5f -> {
                    view.visibility = View.VISIBLE
                    view.translationX = view.width * position
                    view.rotationY = rotation
                }
                else -> {
                    view.visibility = View.INVISIBLE
                }
            }
        }
    }

    /**
     * Accordion transformer
     */
    class AccordionTransformer : ViewPager2.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            when {
                position < 0 -> {
                    view.scaleX = 1f + position
                    view.scaleY = 1f
                    view.pivotX = view.width.toFloat()
                    view.pivotY = view.height * 0.5f
                }
                position > 0 -> {
                    view.scaleX = 1f - position
                    view.scaleY = 1f
                    view.pivotX = 0f
                    view.pivotY = view.height * 0.5f
                }
                else -> {
                    view.scaleX = 1f
                    view.scaleY = 1f
                }
            }
        }
    }
}