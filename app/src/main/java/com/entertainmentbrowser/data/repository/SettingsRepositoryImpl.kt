package com.entertainmentbrowser.data.repository

import android.content.Context
import android.webkit.WebView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.entertainmentbrowser.data.local.datastore.AppSettings
import com.entertainmentbrowser.data.local.datastore.SettingsKeys
import com.entertainmentbrowser.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SettingsRepository using DataStore for persistence.
 * 
 * Requirements: 12.1-12.5
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : SettingsRepository {
    
    override fun observeSettings(): Flow<AppSettings> {
        return dataStore.data.map { preferences ->
            AppSettings(
                downloadOnWifiOnly = preferences[SettingsKeys.DOWNLOAD_ON_WIFI_ONLY] ?: false,
                maxConcurrentDownloads = preferences[SettingsKeys.MAX_CONCURRENT_DOWNLOADS] ?: 3,
                hapticFeedbackEnabled = preferences[SettingsKeys.HAPTIC_FEEDBACK_ENABLED] ?: true,
                onboardingCompleted = preferences[SettingsKeys.ONBOARDING_COMPLETED] ?: false,
                strictAdBlockingEnabled = preferences[SettingsKeys.STRICT_AD_BLOCKING_ENABLED] ?: false,
                searchEngine = com.entertainmentbrowser.domain.model.SearchEngine.fromOrdinal(
                    preferences[SettingsKeys.SEARCH_ENGINE] ?: 0
                )
            )
        }
    }
    
    override fun getOnboardingCompleted(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[SettingsKeys.ONBOARDING_COMPLETED] ?: false
        }
    }
    
    override suspend fun updateDownloadOnWifiOnly(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.DOWNLOAD_ON_WIFI_ONLY] = enabled
        }
    }
    
    override suspend fun updateMaxConcurrentDownloads(max: Int) {
        // Enforce range: 1-5
        val validMax = max.coerceIn(1, 5)
        dataStore.edit { preferences ->
            preferences[SettingsKeys.MAX_CONCURRENT_DOWNLOADS] = validMax
        }
    }
    
    override suspend fun updateHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }
    
    override suspend fun updateOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.ONBOARDING_COMPLETED] = completed
        }
    }
    
    override suspend fun updateStrictAdBlockingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.STRICT_AD_BLOCKING_ENABLED] = enabled
        }
    }
    
    override suspend fun clearCache() {
        // Clear WebView cache on main thread
        WebView(context).apply {
            clearCache(true)
            clearHistory()
            clearFormData()
        }
    }
    
    override suspend fun clearDownloadHistory() {
        // This will be implemented when DownloadDao is available
        // For now, this is a placeholder that will be connected to the database
        // in the download management implementation
    }
    
    override suspend fun updateSearchEngine(engineOrdinal: Int) {
        dataStore.edit { preferences ->
            preferences[SettingsKeys.SEARCH_ENGINE] = engineOrdinal
        }
    }
}
