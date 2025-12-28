package com.entertainmentbrowser.presentation.webview

import android.webkit.JavascriptInterface

/**
 * JavaScript bridge for WebView communication.
 * Provides methods for video detection, DRM detection, and video element interactions.
 * 
 * This class is kept by ProGuard to ensure JavaScript interface methods remain accessible.
 */
class WebViewJsBridge(
    private val onVideoDetected: (String) -> Unit,
    private val onDrmDetected: () -> Unit,
    private val onLongPress: (String, Int) -> Unit,
    private val urlValidator: (String) -> Boolean,
    private val onVideoPlayingStateChanged: (Boolean) -> Unit = {}
) {
    
    /**
     * Called from JavaScript when a video URL is detected.
     * Validates the URL before processing to prevent security issues.
     */
    @JavascriptInterface
    fun onVideoDetected(videoUrl: String) {
        try {
            android.util.Log.d("WebViewJsBridge", "JS Interface called with URL: $videoUrl")
            
            // Validate URL before processing
            if (!urlValidator(videoUrl)) {
                android.util.Log.w("WebViewJsBridge", "Invalid video URL rejected: $videoUrl")
                return
            }
            
            if (VideoDetector.isVideoUrl(videoUrl)) {
                android.util.Log.d("WebViewJsBridge", "✅ Video URL detected: $videoUrl")
                onVideoDetected(videoUrl)
            } else {
                android.util.Log.d("WebViewJsBridge", "❌ URL not recognized as video: $videoUrl")
            }
        } catch (e: Exception) {
            android.util.Log.e("WebViewJsBridge", "Error in onVideoDetected", e)
        }
    }
    
    /**
     * Called from JavaScript when DRM-protected content is detected.
     */
    @JavascriptInterface
    fun onDrmDetected() {
        try {
            onDrmDetected()
        } catch (e: Exception) {
            android.util.Log.e("WebViewJsBridge", "Error in onDrmDetected", e)
        }
    }
    
    /**
     * Called from JavaScript when a video element is long-pressed.
     * Validates the URL before processing.
     */
    @JavascriptInterface
    fun onVideoElementLongPress(videoUrl: String) {
        try {
            // Validate URL before processing
            if (!urlValidator(videoUrl)) {
                android.util.Log.w("WebViewJsBridge", "Invalid long press URL rejected: $videoUrl")
                return
            }
            
            // Called from JavaScript when a video element is long-pressed
            onLongPress(videoUrl, android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE)
        } catch (e: Exception) {
            android.util.Log.e("WebViewJsBridge", "Error in onVideoElementLongPress", e)
        }
    }
    
    /**
     * Called from JavaScript when video playing state changes.
     */
    @JavascriptInterface
    fun onVideoPlayingStateChanged(isPlaying: Boolean) {
        try {
            android.util.Log.d("WebViewJsBridge", "Video playing state changed: $isPlaying")
            onVideoPlayingStateChanged(isPlaying)
        } catch (e: Exception) {
            android.util.Log.e("WebViewJsBridge", "Error in onVideoPlayingStateChanged", e)
        }
    }
}
