package com.lanhe.gongjuxiang.ui.gestures

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs

/**
 * Advanced gesture detector for premium interactions
 */
class AdvancedGestureDetector(
    context: Context,
    private val listener: AdvancedGestureListener
) {

    interface AdvancedGestureListener {
        fun onSingleTap(view: View, event: MotionEvent): Boolean = false
        fun onDoubleTap(view: View, event: MotionEvent): Boolean = false
        fun onLongPress(view: View, event: MotionEvent) {}
        fun onSwipeLeft(view: View, velocityX: Float): Boolean = false
        fun onSwipeRight(view: View, velocityX: Float): Boolean = false
        fun onSwipeUp(view: View, velocityY: Float): Boolean = false
        fun onSwipeDown(view: View, velocityY: Float): Boolean = false
        fun onPinchToZoom(view: View, scaleFactor: Float, focusX: Float, focusY: Float): Boolean = false
        fun onRotation(view: View, angle: Float): Boolean = false
    }

    private val gestureDetector = GestureDetector(context, GestureListener())
    private val scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    private val rotationGestureDetector = RotationGestureDetector(RotationListener())

    private var currentView: View? = null

    fun onTouchEvent(view: View, event: MotionEvent): Boolean {
        currentView = view

        var handled = false
        handled = scaleGestureDetector.onTouchEvent(event) || handled
        handled = rotationGestureDetector.onTouchEvent(event) || handled
        handled = gestureDetector.onTouchEvent(event) || handled

        return handled
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return currentView?.let { listener.onSingleTap(it, e) } ?: false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            return currentView?.let { listener.onDoubleTap(it, e) } ?: false
        }

        override fun onLongPress(e: MotionEvent) {
            currentView?.let { listener.onLongPress(it, e) }
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val view = currentView ?: return false

            if (e1 == null) return false

            val diffY = e2.y - e1.y
            val diffX = e2.x - e1.x

            return if (abs(diffX) > abs(diffY)) {
                if (abs(diffX) > swipeThreshold && abs(velocityX) > swipeVelocityThreshold) {
                    if (diffX > 0) {
                        listener.onSwipeRight(view, velocityX)
                    } else {
                        listener.onSwipeLeft(view, velocityX)
                    }
                } else false
            } else if (abs(diffY) > swipeThreshold && abs(velocityY) > swipeVelocityThreshold) {
                if (diffY > 0) {
                    listener.onSwipeDown(view, velocityY)
                } else {
                    listener.onSwipeUp(view, velocityY)
                }
            } else false
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val view = currentView ?: return false
            return listener.onPinchToZoom(
                view,
                detector.scaleFactor,
                detector.focusX,
                detector.focusY
            )
        }
    }

    private inner class RotationListener : RotationGestureDetector.OnRotationGestureListener {
        override fun onRotation(rotationDetector: RotationGestureDetector): Boolean {
            val view = currentView ?: return false
            return listener.onRotation(view, rotationDetector.angle)
        }
    }

    /**
     * Custom rotation gesture detector
     */
    private class RotationGestureDetector(private val listener: OnRotationGestureListener) {
        interface OnRotationGestureListener {
            fun onRotation(rotationDetector: RotationGestureDetector): Boolean
        }

        private var fX: Float = 0f
        private var fY: Float = 0f
        private var sX: Float = 0f
        private var sY: Float = 0f
        var angle: Float = 0f
            private set

        fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    fX = event.getX(0)
                    fY = event.getY(0)
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount >= 2) {
                        sX = event.getX(1)
                        sY = event.getY(1)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount >= 2) {
                        val nfX = event.getX(0)
                        val nfY = event.getY(0)
                        val nsX = event.getX(1)
                        val nsY = event.getY(1)

                        angle = angleBetweenLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY)

                        return listener.onRotation(this)
                    }
                }
            }
            return false
        }

        private fun angleBetweenLines(
            fX: Float, fY: Float, sX: Float, sY: Float,
            nfX: Float, nfY: Float, nsX: Float, nsY: Float
        ): Float {
            val angle1 = kotlin.math.atan2((fY - sY).toDouble(), (fX - sX).toDouble()).toFloat()
            val angle2 = kotlin.math.atan2((nfY - nsY).toDouble(), (nfX - nsX).toDouble()).toFloat()

            var angle = Math.toDegrees((angle1 - angle2).toDouble()).toFloat() % 360
            if (angle < -180f) angle += 360f
            if (angle > 180f) angle -= 360f
            return angle
        }
    }
}