package com.entertainmentbrowser.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Helper class for providing haptic feedback.
 * 
 * Requirements: 12.1
 */
class HapticFeedbackHelper(private val view: View) {
    
    /**
     * Perform a light click haptic feedback.
     * Used for: favorite toggle, button clicks.
     */
    fun performClick() {
        view.performHapticFeedback(HapticFeedbackConstants.   KEYBOARD_TAP)
    }
    
    /**
     * Perform a long press haptic feedback.
     * Used for: long press actions, context menus.
     */
    fun performLongPress() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
    
    /**
     * Perform a context click haptic feedback.
     * Used for: tab close, delete actions.
     */
    fun performContextClick() {
        view.performHapticFeedback(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                HapticFeedbackConstants.CONTEXT_CLICK
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
        )
    }
}

/**
 * Composable function to remember a HapticFeedbackHelper instance.
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackHelper {
    val view = LocalView.current
    return remember(view) { HapticFeedbackHelper(view) }
}
