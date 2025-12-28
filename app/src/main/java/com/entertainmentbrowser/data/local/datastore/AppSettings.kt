package com.entertainmentbrowser.data.local.datastore

import com.entertainmentbrowser.domain.model.SearchEngine

/**
 * Data class representing app settings stored in DataStore.
 * 
 * Requirements: 12.1
 */
data class AppSettings(
    val downloadOnWifiOnly: Boolean = false,
    val maxConcurrentDownloads: Int = 3,
    val hapticFeedbackEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    /**
     * Strict ad blocking mode (advanced users only).
     * 
     * When enabled, blocks main-frame requests matched by ad rules even if no safe
     * redirect target is found. This provides maximum protection but may cause some
     * legitimate pages to fail loading.
     * 
     * Default: false (balanced mode - allows main-frame requests without redirect targets)
     */
    val strictAdBlockingEnabled: Boolean = false,
    /**
     * Selected search engine for the search bar.
     */
    val searchEngine: SearchEngine = SearchEngine.GOOGLE
)
