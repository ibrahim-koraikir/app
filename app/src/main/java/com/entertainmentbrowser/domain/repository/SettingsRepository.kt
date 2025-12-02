package com.entertainmentbrowser.domain.repository

import com.entertainmentbrowser.data.local.datastore.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing app settings.
 * 
 * Requirements: 12.1-12.5
 */
interface SettingsRepository {
    /**
     * Observe all app settings as a Flow.
     */
    fun observeSettings(): Flow<AppSettings>
    
    /**
     * Get the onboarding completed status.
     */
    fun getOnboardingCompleted(): Flow<Boolean>
    
    /**
     * Update the download on Wi-Fi only setting.
     * 
     * Requirements: 12.2
     */
    suspend fun updateDownloadOnWifiOnly(enabled: Boolean)
    
    /**
     * Update the maximum concurrent downloads setting.
     * 
     * Requirements: 12.3
     */
    suspend fun updateMaxConcurrentDownloads(max: Int)
    
    /**
     * Update the haptic feedback enabled setting.
     * 
     * Requirements: 12.1
     */
    suspend fun updateHapticFeedbackEnabled(enabled: Boolean)
    
    /**
     * Update the onboarding completed status.
     */
    suspend fun updateOnboardingCompleted(completed: Boolean)
    
    /**
     * Update the strict ad blocking mode setting.
     * 
     * When enabled, blocks main-frame requests matched by ad rules even without
     * safe redirect targets. Provides maximum protection but may break some sites.
     */
    suspend fun updateStrictAdBlockingEnabled(enabled: Boolean)
    
    /**
     * Clear WebView cache.
     * 
     * Requirements: 12.4
     */
    suspend fun clearCache()
    
    /**
     * Clear download history from database.
     * 
     * Requirements: 12.5
     */
    suspend fun clearDownloadHistory()
}
