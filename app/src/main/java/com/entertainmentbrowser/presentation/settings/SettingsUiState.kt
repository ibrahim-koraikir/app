package com.entertainmentbrowser.presentation.settings

import com.entertainmentbrowser.data.local.datastore.AppSettings

/**
 * UI state for the Settings screen.
 * 
 * Requirements: 12.1-12.5
 */
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showClearCacheDialog: Boolean = false,
    val showClearHistoryDialog: Boolean = false,
    val cacheCleared: Boolean = false,
    val historyCleared: Boolean = false
)
