package com.entertainmentbrowser.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tutorialDataStore by preferencesDataStore(name = "tutorial_tips")

/**
 * Manages tutorial tips/hints shown to users.
 * Tips are shown until the user performs the action OR until they've seen it MAX_SHOW_COUNT times.
 */
@Singleton
class TutorialTipsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MAX_SHOW_COUNT = 7 // Show tip up to 7 times before giving up
        
        // Completed flags (user performed the action)
        private val TIP_EDGE_SWIPE_COMPLETED = booleanPreferencesKey("tip_edge_swipe_completed")
        private val TIP_LONG_PRESS_COMPLETED = booleanPreferencesKey("tip_long_press_completed")
        private val TIP_PULL_REFRESH_COMPLETED = booleanPreferencesKey("tip_pull_refresh_completed")
        private val TIP_VIDEO_DOWNLOAD_COMPLETED = booleanPreferencesKey("tip_video_download_completed")
        
        // Show count (how many times tip was shown)
        private val TIP_EDGE_SWIPE_COUNT = intPreferencesKey("tip_edge_swipe_count")
        private val TIP_LONG_PRESS_COUNT = intPreferencesKey("tip_long_press_count")
        private val TIP_PULL_REFRESH_COUNT = intPreferencesKey("tip_pull_refresh_count")
        private val TIP_VIDEO_DOWNLOAD_COUNT = intPreferencesKey("tip_video_download_count")
    }

    // ==================== EDGE SWIPE TIP ====================
    
    /**
     * Check if edge swipe tip should be shown
     * Shows until user does it OR seen 7 times
     */
    fun shouldShowEdgeSwipeTip(): Flow<Boolean> {
        return context.tutorialDataStore.data.map { prefs ->
            val completed = prefs[TIP_EDGE_SWIPE_COMPLETED] ?: false
            val count = prefs[TIP_EDGE_SWIPE_COUNT] ?: 0
            !completed && count < MAX_SHOW_COUNT
        }
    }

    /**
     * Increment edge swipe tip show count
     */
    suspend fun incrementEdgeSwipeTipCount() {
        context.tutorialDataStore.edit { prefs ->
            val current = prefs[TIP_EDGE_SWIPE_COUNT] ?: 0
            prefs[TIP_EDGE_SWIPE_COUNT] = current + 1
        }
    }

    /**
     * Mark edge swipe as completed (user did the action)
     */
    suspend fun markEdgeSwipeCompleted() {
        context.tutorialDataStore.edit { prefs ->
            prefs[TIP_EDGE_SWIPE_COMPLETED] = true
        }
    }

    // ==================== LONG PRESS TIP ====================
    
    /**
     * Check if long press tip should be shown
     */
    fun shouldShowLongPressTip(): Flow<Boolean> {
        return context.tutorialDataStore.data.map { prefs ->
            val completed = prefs[TIP_LONG_PRESS_COMPLETED] ?: false
            val count = prefs[TIP_LONG_PRESS_COUNT] ?: 0
            !completed && count < MAX_SHOW_COUNT
        }
    }

    /**
     * Increment long press tip show count
     */
    suspend fun incrementLongPressTipCount() {
        context.tutorialDataStore.edit { prefs ->
            val current = prefs[TIP_LONG_PRESS_COUNT] ?: 0
            prefs[TIP_LONG_PRESS_COUNT] = current + 1
        }
    }

    /**
     * Mark long press as completed (user did the action)
     */
    suspend fun markLongPressCompleted() {
        context.tutorialDataStore.edit { prefs ->
            prefs[TIP_LONG_PRESS_COMPLETED] = true
        }
    }

    // ==================== PULL REFRESH TIP ====================
    
    /**
     * Check if pull to refresh tip should be shown
     */
    fun shouldShowPullRefreshTip(): Flow<Boolean> {
        return context.tutorialDataStore.data.map { prefs ->
            val completed = prefs[TIP_PULL_REFRESH_COMPLETED] ?: false
            val count = prefs[TIP_PULL_REFRESH_COUNT] ?: 0
            !completed && count < MAX_SHOW_COUNT
        }
    }

    /**
     * Increment pull refresh tip show count
     */
    suspend fun incrementPullRefreshTipCount() {
        context.tutorialDataStore.edit { prefs ->
            val current = prefs[TIP_PULL_REFRESH_COUNT] ?: 0
            prefs[TIP_PULL_REFRESH_COUNT] = current + 1
        }
    }

    /**
     * Mark pull refresh as completed (user did the action)
     */
    suspend fun markPullRefreshCompleted() {
        context.tutorialDataStore.edit { prefs ->
            prefs[TIP_PULL_REFRESH_COMPLETED] = true
        }
    }

    // ==================== VIDEO DOWNLOAD TIP ====================
    
    /**
     * Check if video download tip should be shown
     */
    fun shouldShowVideoDownloadTip(): Flow<Boolean> {
        return context.tutorialDataStore.data.map { prefs ->
            val completed = prefs[TIP_VIDEO_DOWNLOAD_COMPLETED] ?: false
            val count = prefs[TIP_VIDEO_DOWNLOAD_COUNT] ?: 0
            !completed && count < MAX_SHOW_COUNT
        }
    }

    /**
     * Increment video download tip show count
     */
    suspend fun incrementVideoDownloadTipCount() {
        context.tutorialDataStore.edit { prefs ->
            val current = prefs[TIP_VIDEO_DOWNLOAD_COUNT] ?: 0
            prefs[TIP_VIDEO_DOWNLOAD_COUNT] = current + 1
        }
    }

    /**
     * Mark video download as completed (user did the action)
     */
    suspend fun markVideoDownloadCompleted() {
        context.tutorialDataStore.edit { prefs ->
            prefs[TIP_VIDEO_DOWNLOAD_COMPLETED] = true
        }
    }

    /**
     * Reset all tips (for testing or settings)
     */
    suspend fun resetAllTips() {
        context.tutorialDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
