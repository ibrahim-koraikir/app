package com.entertainmentbrowser.data.local.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

/**
 * Object containing all DataStore preference keys for app settings.
 * 
 * Requirements: 12.1
 */
object SettingsKeys {
    val DOWNLOAD_ON_WIFI_ONLY = booleanPreferencesKey("download_on_wifi_only")
    val MAX_CONCURRENT_DOWNLOADS = intPreferencesKey("max_concurrent_downloads")
    val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val STRICT_AD_BLOCKING_ENABLED = booleanPreferencesKey("strict_ad_blocking_enabled")
    val SEARCH_ENGINE = intPreferencesKey("search_engine")
}
