package com.entertainmentbrowser.presentation.settings

/**
 * Events that can be triggered from the Settings screen.
 * 
 * Requirements: 12.1-12.5
 */
sealed class SettingsEvent {
    /**
     * Toggle download on Wi-Fi only setting.
     * Requirements: 12.2
     */
    data class ToggleDownloadOnWifiOnly(val enabled: Boolean) : SettingsEvent()
    
    /**
     * Update maximum concurrent downloads.
     * Requirements: 12.3
     */
    data class UpdateMaxConcurrentDownloads(val max: Int) : SettingsEvent()
    
    /**
     * Toggle haptic feedback setting.
     * Requirements: 12.1
     */
    data class ToggleHapticFeedback(val enabled: Boolean) : SettingsEvent()
    
    /**
     * Show clear cache confirmation dialog.
     * Requirements: 12.4
     */
    data object ShowClearCacheDialog : SettingsEvent()
    
    /**
     * Confirm clear cache action.
     * Requirements: 12.4
     */
    data object ConfirmClearCache : SettingsEvent()
    
    /**
     * Show clear download history confirmation dialog.
     * Requirements: 12.5
     */
    data object ShowClearHistoryDialog : SettingsEvent()
    
    /**
     * Confirm clear download history action.
     * Requirements: 12.5
     */
    data object ConfirmClearHistory : SettingsEvent()
    
    /**
     * Dismiss any open dialog.
     */
    data object DismissDialog : SettingsEvent()
    
    /**
     * Clear error message.
     */
    data object ClearError : SettingsEvent()
    
    /**
     * Dismiss success message.
     */
    data object DismissSuccess : SettingsEvent()
}
