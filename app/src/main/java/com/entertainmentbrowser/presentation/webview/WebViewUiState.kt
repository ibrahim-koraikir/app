package com.entertainmentbrowser.presentation.webview

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
    val detectedVideoUrl: String? = null,
    val drmDetected: Boolean = false,
    val showDrmWarning: Boolean = false,
    val error: String? = null
)
