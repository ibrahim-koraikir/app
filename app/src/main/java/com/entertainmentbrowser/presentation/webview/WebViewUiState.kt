package com.entertainmentbrowser.presentation.webview

/**
 * Types of page load errors for user-friendly display
 */
enum class PageErrorType {
    NONE,
    NO_INTERNET,
    TIMEOUT,
    SERVER_ERROR,
    SSL_ERROR,
    PAGE_NOT_FOUND,
    BLOCKED_BY_ADBLOCK,
    UNKNOWN
}

/**
 * UI state for the WebView screen
 */
data class WebViewUiState(
    val url: String = "",
    val currentUrl: String = "",
    val title: String = "",
    val isLoading: Boolean = false,
    val loadingProgress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val videoDetected: Boolean = false,
    val isVideoPlaying: Boolean = false,
    val detectedVideoUrl: String? = null,
    val drmDetected: Boolean = false,
    val showDrmWarning: Boolean = false,
    val error: String? = null,
    // Enhanced error state for blank screen handling
    val pageErrorType: PageErrorType = PageErrorType.NONE,
    val showErrorOverlay: Boolean = false,
    val lastSuccessfulUrl: String? = null
)
