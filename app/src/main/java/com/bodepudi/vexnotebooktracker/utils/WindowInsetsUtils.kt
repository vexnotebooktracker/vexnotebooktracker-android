package com.bodepudi.vexnotebooktracker.utils

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

/**
 * Window inset helpers for edge-to-edge display.
 *
 * Android 16 (API 36) enforces edge-to-edge for every app that targets SDK 36: the app window
 * always spans the full screen, `android:statusBarColor` / `android:navigationBarColor` are
 * ignored, and `android:windowOptOutEdgeToEdgeEnforcement` no longer opts out. Each screen has to
 * inset its own content so that it is not drawn underneath the status bar or the gesture
 * navigation bar.
 *
 * Every activity therefore calls [applyTopBarInsets] for its app bar and [applyContentInsets] for
 * the container below it.
 */
object WindowInsetsUtils {

    private fun systemBarInsets(insets: WindowInsetsCompat) = insets.getInsets(
        WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
    )

    /**
     * Pads [topBar] with the status bar height so the bar's own background paints the status bar
     * area. The view keeps whatever padding it already declares - the inset is added on top of it,
     * so repeated inset passes never accumulate.
     */
    fun applyTopBarInsets(topBar: View) {
        val initialTop = topBar.paddingTop
        val initialLeft = topBar.paddingLeft
        val initialRight = topBar.paddingRight

        ViewCompat.setOnApplyWindowInsetsListener(topBar) { view, insets ->
            val bars = systemBarInsets(insets)
            view.updatePadding(
                top = initialTop + bars.top,
                left = initialLeft + bars.left,
                right = initialRight + bars.right
            )
            // Not consumed - sibling views below still need the bottom inset.
            insets
        }
        ViewCompat.requestApplyInsets(topBar)
    }

    /**
     * Keeps [content] clear of the navigation bar and, when [handleIme] is set, of the on-screen
     * keyboard. Edge-to-edge windows are not resized by `adjustResize` any more, so screens with
     * text input have to add the IME inset themselves.
     *
     * [includeTopInset] is for screens that have no app bar of their own and need to sit below the
     * status bar.
     */
    fun applyContentInsets(
        content: View,
        handleIme: Boolean = false,
        includeTopInset: Boolean = false
    ) {
        val initialTop = content.paddingTop
        val initialBottom = content.paddingBottom
        val initialLeft = content.paddingLeft
        val initialRight = content.paddingRight

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = systemBarInsets(insets)
            val imeBottom = if (handleIme) {
                insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            } else {
                0
            }

            view.updatePadding(
                top = if (includeTopInset) initialTop + bars.top else initialTop,
                bottom = initialBottom + maxOf(bars.bottom, imeBottom),
                left = initialLeft + bars.left,
                right = initialRight + bars.right
            )
            insets
        }
        ViewCompat.requestApplyInsets(content)
    }

    /**
     * Keeps a floating view - a FAB, say - clear of the navigation bar by growing its margins.
     *
     * These views sit directly on the window rather than inside the content container, so they do
     * not benefit from [applyContentInsets]. The inset has to be applied as margin rather than
     * padding: padding on a FAB shrinks the button's own drawable area instead of moving it.
     */
    fun applyFloatingInsets(view: View) {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val initialBottom = params.bottomMargin
        val initialLeft = params.leftMargin
        val initialRight = params.rightMargin

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = systemBarInsets(insets)
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = initialBottom + bars.bottom
                leftMargin = initialLeft + bars.left
                rightMargin = initialRight + bars.right
            }
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }

    /**
     * Sizes [scrim] to the height of the status bar so it can paint that area, for screens that
     * have no app bar to extend behind it.
     */
    fun applyStatusBarScrim(scrim: View) {
        ViewCompat.setOnApplyWindowInsetsListener(scrim) { view, insets ->
            val height = systemBarInsets(insets).top
            if (view.layoutParams.height != height) {
                view.updateLayoutParams<ViewGroup.LayoutParams> { this.height = height }
            }
            insets
        }
        ViewCompat.requestApplyInsets(scrim)
    }
}
