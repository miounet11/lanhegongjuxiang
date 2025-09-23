package com.lanhe.gongjuxiang.ui.transitions

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialSharedAxis

/**
 * Helper class for managing shared element transitions
 */
object SharedElementTransitionHelper {

    /**
     * Start activity with shared element transition
     */
    fun startActivityWithSharedElement(
        activity: Activity,
        intent: Intent,
        sharedElement: View,
        transitionName: String
    ) {
        ViewCompat.setTransitionName(sharedElement, transitionName)
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            activity,
            sharedElement,
            transitionName
        )
        activity.startActivity(intent, options.toBundle())
    }

    /**
     * Start activity with multiple shared elements
     */
    fun startActivityWithSharedElements(
        activity: Activity,
        intent: Intent,
        vararg sharedElements: Pair<View, String>
    ) {
        val pairs = sharedElements.map { (view, name) ->
            ViewCompat.setTransitionName(view, name)
            androidx.core.util.Pair.create(view, name)
        }.toTypedArray()

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, *pairs)
        activity.startActivity(intent, options.toBundle())
    }

    /**
     * Setup fragment transition with shared elements
     */
    fun setupFragmentTransition(
        fragment: Fragment,
        sharedElements: Map<View, String> = emptyMap(),
        axis: Int = MaterialSharedAxis.X,
        forward: Boolean = true
    ) {
        // Setup container transform for shared elements
        if (sharedElements.isNotEmpty()) {
            fragment.sharedElementEnterTransition = MaterialContainerTransform().apply {
                duration = 300
                scrimColor = android.graphics.Color.TRANSPARENT
            }
            fragment.sharedElementReturnTransition = MaterialContainerTransform().apply {
                duration = 250
                scrimColor = android.graphics.Color.TRANSPARENT
            }
        }

        // Setup enter/exit transitions
        fragment.enterTransition = MaterialSharedAxis(axis, forward).apply {
            duration = 300
        }
        fragment.returnTransition = MaterialSharedAxis(axis, !forward).apply {
            duration = 250
        }
        fragment.exitTransition = MaterialSharedAxis(axis, forward).apply {
            duration = 250
        }
        fragment.reenterTransition = MaterialSharedAxis(axis, !forward).apply {
            duration = 300
        }
    }

    /**
     * Add shared elements to fragment transaction
     */
    fun addSharedElements(
        transaction: FragmentTransaction,
        sharedElements: Map<View, String>
    ): FragmentTransaction {
        sharedElements.forEach { (view, name) ->
            ViewCompat.setTransitionName(view, name)
            transaction.addSharedElement(view, name)
        }
        return transaction
    }

    /**
     * Setup hero transition for image views
     */
    fun setupHeroTransition(
        fragment: Fragment,
        imageView: View,
        transitionName: String
    ) {
        ViewCompat.setTransitionName(imageView, transitionName)

        fragment.sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = 375
            scrimColor = android.graphics.Color.TRANSPARENT
            setAllContainerColors(android.graphics.Color.TRANSPARENT)
        }
    }

    /**
     * Create fade through transition
     */
    fun createFadeThroughTransition(duration: Long = 300): MaterialSharedAxis {
        return MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            this.duration = duration
        }
    }

    /**
     * Create slide transition
     */
    fun createSlideTransition(
        axis: Int = MaterialSharedAxis.X,
        forward: Boolean = true,
        duration: Long = 300
    ): MaterialSharedAxis {
        return MaterialSharedAxis(axis, forward).apply {
            this.duration = duration
        }
    }

    /**
     * Setup activity transitions
     */
    fun setupActivityTransitions(activity: Activity) {
        activity.window.apply {
            allowEnterTransitionOverlap = true
            allowReturnTransitionOverlap = true

            // Setup enter/exit transitions
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
                duration = 300
            }
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply {
                duration = 250
            }
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                duration = 250
            }
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply {
                duration = 300
            }

            // Setup shared element transition
            sharedElementEnterTransition = MaterialContainerTransform().apply {
                duration = 375
                scrimColor = android.graphics.Color.TRANSPARENT
            }
            sharedElementReturnTransition = MaterialContainerTransform().apply {
                duration = 325
                scrimColor = android.graphics.Color.TRANSPARENT
            }
        }
    }

    /**
     * Common transition names
     */
    object TransitionNames {
        const val CARD_TRANSFORM = "card_transform"
        const val IMAGE_HERO = "image_hero"
        const val TOOLBAR = "toolbar"
        const val FAB = "fab"
        const val BOTTOM_NAV = "bottom_nav"
        const val CONTAINER = "container"
        const val CONTENT = "content"
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val ICON = "icon"
    }

    /**
     * Transition axes
     */
    object Axes {
        const val X = MaterialSharedAxis.X
        const val Y = MaterialSharedAxis.Y
        const val Z = MaterialSharedAxis.Z
    }
}