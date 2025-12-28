package com.entertainmentbrowser.util

import android.content.Context
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * WebView pool for reusing WebView instances to improve performance.
 * 
 * This pool maintains a collection of pre-initialized WebView instances that can be
 * reused instead of creating new ones, which significantly reduces initialization time.
 * 
 * ## Memory Leak Prevention
 * 
 * **WARNING FOR CONTRIBUTORS:**
 * Do NOT store Activity, Fragment, or other UI-scoped references on WebView instances
 * (directly or via JavaScript interfaces) without providing a corresponding cleanup path
 * before recycling. Failure to do so will cause memory leaks.
 * 
 * **Safe patterns:**
 * - Use `applicationContext` for WebView creation (already done in [createWebView])
 * - Clear all clients and JS interfaces before recycling (done in [recycle])
 * - Detach UI-specific listeners before calling [recycle]
 * 
 * **Unsafe patterns (avoid):**
 * - Storing Activity references in WebViewClient/WebChromeClient
 * - Adding JavaScript interfaces that hold Activity/Fragment references
 * - Setting listeners that capture Activity scope without cleanup
 * 
 * **Before recycling a WebView:**
 * 1. Call [prepareForRecycling] to clear all UI-specific references
 * 2. Then call [recycle] to return the WebView to the pool
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
object WebViewPool {
    private const val TAG = "WebViewPool"
    private const val MAX_POOL_SIZE = 3
    
    private val pool = ConcurrentLinkedQueue<WebView>()
    
    /**
     * Obtain a WebView from the pool or create a new one if pool is empty.
     * 
     * @param context Context for creating WebView
     * @return WebView instance ready for use
     * 
     * Requirements: 4.1, 4.2, 4.4, 4.5
     */
    fun obtain(context: Context): WebView {
        return try {
            val webView = pool.poll()
            if (webView != null) {
                Log.d(TAG, "Obtained WebView from pool. Pool size: ${pool.size}")
                webView
            } else {
                Log.d(TAG, "Pool empty, creating new WebView")
                createWebView(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to obtain from pool, creating new WebView", e)
            createWebView(context)
        }
    }
    
    /**
     * Prepare a WebView for recycling by clearing all UI-specific references.
     * 
     * **MUST be called before [recycle]** to prevent memory leaks from:
     * - WebViewClient holding Activity references
     * - WebChromeClient holding Activity references
     * - JavaScript interfaces holding Activity/Fragment references
     * - Download listeners, touch listeners, etc.
     * 
     * This method clears:
     * - WebViewClient (replaced with default)
     * - WebChromeClient (replaced with null)
     * - Known JavaScript interfaces (AndroidInterface)
     * - All listeners (touch, scroll, long-click, download)
     * 
     * @param webView WebView to prepare for recycling
     * @param jsInterfaceNames Additional JavaScript interface names to remove (optional)
     */
    fun prepareForRecycling(webView: WebView, jsInterfaceNames: List<String> = emptyList()) {
        try {
            webView.apply {
                // Clear WebViewClient to prevent Activity leaks
                // Use default WebViewClient instead of null to avoid crashes
                webViewClient = WebViewClient()
                
                // Clear WebChromeClient to prevent Activity leaks
                webChromeClient = null
                
                // Remove known JavaScript interfaces that may hold Activity references
                // "AndroidInterface" is used by CustomWebView for video detection
                removeJavascriptInterface("AndroidInterface")
                
                // Remove any additional JS interfaces specified by caller
                jsInterfaceNames.forEach { name ->
                    removeJavascriptInterface(name)
                }
                
                // Clear all listeners that may hold Activity/Fragment references
                setOnTouchListener(null)
                setOnLongClickListener(null)
                setOnScrollChangeListener(null)
                setDownloadListener(null)
                
                Log.d(TAG, "Prepared WebView for recycling - cleared clients and interfaces")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare WebView for recycling", e)
        }
    }
    
    /**
     * Recycle a WebView back to the pool for reuse.
     * 
     * **IMPORTANT:** Call [prepareForRecycling] before this method to clear
     * UI-specific references and prevent memory leaks.
     * 
     * The WebView is cleared of all data and returned to the pool if there's space.
     * If the pool is full, the WebView is destroyed.
     * 
     * @param webView WebView to recycle
     * 
     * Requirements: 4.1, 4.4
     */
    fun recycle(webView: WebView) {
        try {
            // STEP 1: Clear UI-specific references to prevent Activity/Fragment leaks
            // This MUST happen before clearing data to ensure no callbacks fire during cleanup
            webView.apply {
                // Clear WebViewClient to prevent Activity leaks
                webViewClient = WebViewClient()
                
                // Clear WebChromeClient to prevent Activity leaks
                webChromeClient = null
                
                // Remove known JavaScript interfaces
                removeJavascriptInterface("AndroidInterface")
                
                // Clear all listeners
                setOnTouchListener(null)
                setOnLongClickListener(null)
                setOnScrollChangeListener(null)
                setDownloadListener(null)
            }
            
            // STEP 2: Clear per-WebView data only (not global cookies/storage)
            // This preserves user logins across tab closures
            // NOTE: clearCache(false) clears only RAM cache, preserving disk cache for performance
            // Use CacheManager.clearAllBrowsingData() for explicit global cache management
            webView.apply {
                stopLoading()
                
                // CRITICAL: Load about:blank to clear the current URL
                // This ensures recycled WebViews don't have stale URLs that prevent
                // new URLs from loading in CustomWebView's LaunchedEffect
                loadUrl("about:blank")
                
                clearHistory()
                clearCache(false)
                clearFormData()
                clearMatches()
                clearSslPreferences()
                
                // Remove all views and callbacks
                removeAllViews()
                
                // NOTE: Cookies and WebStorage are NOT cleared here to preserve logins
                // Use clearAllBrowsingData() for full privacy cleanup (e.g., from Settings)
            }
            
            // Return to pool if under max size
            if (pool.size < MAX_POOL_SIZE) {
                pool.offer(webView)
                Log.d(TAG, "Recycled WebView to pool. Pool size: ${pool.size}")
            } else {
                // Pool is full - destroy the WebView
                // Note: GPU slot should already be released by caller (WebViewStateManager)
                // before calling recycle(), so no need to release here
                Log.d(TAG, "Pool full, destroying WebView")
                webView.destroy()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recycle WebView", e)
            try {
                webView.destroy()
            } catch (destroyException: Exception) {
                Log.e(TAG, "Failed to destroy WebView", destroyException)
            }
        }
    }
    
    /**
     * Clear all browsing data including cookies and WebStorage.
     * Call this from Settings or for privacy cleanup, not during normal recycling.
     */
    fun clearAllBrowsingData() {
        try {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.WebStorage.getInstance().deleteAllData()
            Log.d(TAG, "Cleared all browsing data (cookies + storage)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear browsing data", e)
        }
    }
    
    /**
     * Clear the pool and destroy all WebView instances.
     * 
     * This should be called when the app is being terminated or when memory needs to be freed.
     * 
     * Note: GPU slots are released by WebViewStateManager before WebViews are recycled to the pool,
     * so pooled WebViews don't have associated GPU slots. This method only destroys the WebView
     * instances themselves.
     * 
     * Requirements: 4.1, 4.5
     */
    fun clear() {
        try {
            var webView = pool.poll()
            var count = 0
            while (webView != null) {
                try {
                    webView.destroy()
                    count++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to destroy WebView during clear", e)
                }
                webView = pool.poll()
            }
            Log.d(TAG, "Cleared pool, destroyed $count WebView instances")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear pool", e)
        }
    }
    
    /**
     * Create a new WebView with optimized performance settings.
     * 
     * @param context Context for creating WebView
     * @return Configured WebView instance
     * 
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
     */
    private fun createWebView(context: Context): WebView {
        return WebView(context.applicationContext).apply {
            // Set dark background to match app theme
            setBackgroundColor(android.graphics.Color.parseColor("#121212"))
            
            // Enable hardware acceleration for better rendering performance
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            
            settings.apply {
                // Enable JavaScript (Requirement 4.3)
                javaScriptEnabled = true
                
                // Enable DOM storage (Requirement 4.3)
                domStorageEnabled = true
                
                // CRITICAL: Enable image loading
                loadsImagesAutomatically = true
                blockNetworkImage = false
                
                // CRITICAL: Set Chrome-like user agent for Google OAuth compatibility
                // Google blocks sign-in from WebViews with non-Chrome user agents
                // We need to use a FULL Chrome user agent string, not just append Chrome
                // This mimics a real Chrome browser on Android
                val chromeUserAgent = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36"
                userAgentString = chromeUserAgent
                Log.d(TAG, "Set Chrome user agent for OAuth support")
                
                // Viewport settings - DISABLED to prevent breaking CSS Grid layouts
                // useWideViewPort and loadWithOverviewMode cause Google Images masonry to break
                useWideViewPort = false
                loadWithOverviewMode = false
                
                // Use NORMAL layout algorithm - TEXT_AUTOSIZING can break some layouts
                @Suppress("DEPRECATION")
                layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                
                // Set default text encoding
                defaultTextEncodingName = "UTF-8"
                
                // DISABLE force dark mode completely to prevent layout issues
                // Sites like Google Images have their own responsive layouts that break
                // when WebView applies dark mode transformations
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    forceDark = WebSettings.FORCE_DARK_OFF
                }
                
                // Disable algorithmic darkening to prevent layout issues
                // Sites like Google have their own dark mode that works better
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    isAlgorithmicDarkeningAllowed = false
                }
                
                // Set cache mode to default (Requirement 4.3)
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Set render priority to high (Requirement 4.3)
                @Suppress("DEPRECATION")
                setRenderPriority(WebSettings.RenderPriority.HIGH)
                
                // Enable smooth transition (Requirement 4.3)
                @Suppress("DEPRECATION")
                setEnableSmoothTransition(true)
                
                // Additional performance settings
                @Suppress("DEPRECATION")
                databaseEnabled = true
                
                // Security settings
                allowFileAccess = false
                allowContentAccess = false
                
                // Safe Browsing (API 26+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
                
                // Media settings - optimized for video-heavy scroll feeds (TikTok, Reels, Shorts)
                mediaPlaybackRequiresUserGesture = false // Allow autoplay for better UX
                
                // Offscreen precomposite - improves scroll performance on video-heavy pages
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    offscreenPreRaster = true
                }
                
                // Disable geolocation
                setGeolocationEnabled(false)
                
                // Enable zoom controls
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                
                // Disable multiple windows to prevent sites from opening new tabs
                // This prevents target="_blank" and window.open() from creating new windows
                setSupportMultipleWindows(false)
                
                // Allow JavaScript to open windows (needed for some search functionality)
                // Combined with setSupportMultipleWindows(false), this allows JS navigation
                // but prevents actual new window creation
                javaScriptCanOpenWindowsAutomatically = true
                
                // Enable form data saving for better UX
                saveFormData = true
                
                // Mixed content mode - use compatibility mode for security while maintaining functionality
                // COMPATIBILITY_MODE blocks mixed content on secure origins but allows it for legacy content
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }
            }
            
            // Privacy & Security Enhancements
            // Disable third-party cookies to prevent cross-site tracking
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            
            Log.d(TAG, "Created new WebView with performance settings")
        }
    }
    
    /**
     * Get the current size of the pool.
     * 
     * @return Number of WebView instances in the pool
     */
    fun getPoolSize(): Int = pool.size
}
