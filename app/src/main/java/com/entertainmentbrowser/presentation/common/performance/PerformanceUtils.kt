package com.entertainmentbrowser.presentation.common.performance

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import com.entertainmentbrowser.BuildConfig

/**
 * Remembers a stable value based on a key and calculation.
 * Useful for expensive computations that should only recalculate when the key changes.
 *
 * @param key The key to determine when to recalculate
 * @param calculation The calculation to perform
 * @return The calculated value
 */
@Composable
inline fun <T> rememberStable(key: Any?, crossinline calculation: () -> T): T {
    return remember(key) { calculation() }
}

/**
 * Logs composition counts for debugging recomposition issues.
 * Only active in debug builds to avoid performance overhead in production.
 *
 * @param tag The tag to use for logging
 */
@Composable
fun LogCompositions(tag: String) {
    if (BuildConfig.DEBUG) {
        val recompositionCount = remember { mutableListOf(0) }
        
        SideEffect {
            recompositionCount[0]++
            Log.d("Recomposition", "[$tag] Recomposition #${recompositionCount[0]}")
        }
    }
}
