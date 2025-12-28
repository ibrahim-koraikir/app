package com.entertainmentbrowser.presentation.webview

/**
 * Events that can be triggered from the WebView screen
 */
sealed class WebViewEvent {
    /**
     * Navigate back in browsing history
     */
    data object NavigateBack : WebViewEvent()
    
    /**
     * Navigate forward in browsing history
     */
    data object NavigateForward : WebViewEvent()
    
    /**
     * Refresh the current page
     */
    data object Refresh : WebViewEvent()
    
    /**
     * Share the current URL
     */
    data object Share : WebViewEvent()
    
    /**
     * Start downloading the detected video
     */
    data object DownloadVideo : WebViewEvent()
    
    /**
     * Start downloading the detected video with custom filename and quality
     */
    data class DownloadVideoWithFilename(val filename: String, val quality: String = "auto") : WebViewEvent()
    
    /**
     * Dismiss the DRM warning dialog
     */
    data object DismissDrmWarning : WebViewEvent()
    
    /**
     * Update the current URL (called by WebView)
     */
    data class UpdateUrl(val url: String) : WebViewEvent()
    
    /**
     * Update the page title (called by WebView)
     */
    data class UpdateTitle(val title: String) : WebViewEvent()
    
    /**
     * Update loading state (called by WebView)
     */
    data class UpdateLoading(val isLoading: Boolean) : WebViewEvent()
    
    /**
     * Update loading progress (called by WebView)
     */
    data class UpdateProgress(val progress: Int) : WebViewEvent()
    
    /**
     * Update navigation state (called by WebView)
     */
    data class UpdateNavigationState(val canGoBack: Boolean, val canGoForward: Boolean) : WebViewEvent()
    
    /**
     * Video detected on the page
     */
    data class VideoDetected(val videoUrl: String) : WebViewEvent()
    
    /**
     * Video playing state changed
     */
    data class VideoPlayingStateChanged(val isPlaying: Boolean) : WebViewEvent()
    
    /**
     * DRM content detected on the page
     */
    data object DrmDetected : WebViewEvent()
    
    /**
     * Error occurred while loading page
     */
    data class Error(val message: String) : WebViewEvent()
    
    /**
     * Page load error with type classification for better UX
     */
    data class PageLoadError(val errorType: PageErrorType, val errorCode: Int = -1) : WebViewEvent()
    
    /**
     * Dismiss the error overlay and retry loading
     */
    data object RetryPageLoad : WebViewEvent()
    
    /**
     * Dismiss error overlay without retrying
     */
    data object DismissErrorOverlay : WebViewEvent()
}
