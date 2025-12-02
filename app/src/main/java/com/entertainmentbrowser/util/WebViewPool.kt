package com.entertainmentbrowser.util

import android.content.Context
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * WebView pool for reusing WebView instances to improve performance.
 * 
 * This pool maintains a collection of pre-initialized WebView instances that can be
 * reused instead of creating new ones, which significantly reduces initialization time.
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
     * Recycle a WebView back to the pool for reuse.
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
            // Clear WebView data
            webView.apply {
                stopLoading()
                clearHistory()
                clearCache(true)
                clearFormData()
                clearMatches()
                clearSslPreferences()
                
                // Remove all views and callbacks
                removeAllViews()
                
                // Clear cookies and storage
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                android.webkit.WebStorage.getInstance().deleteAllData()
            }
            
            // Return to pool if under max size
            if (pool.size < MAX_POOL_SIZE) {
                pool.offer(webView)
                Log.d(TAG, "Recycled WebView to pool. Pool size: ${pool.size}")
            } else {
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
     * Clear the pool and destroy all WebView instances.
     * 
     * This should be called when the app is being terminated or when memory needs to be freed.
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
                
                // Enable force dark mode for better appearance (Android 10+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    forceDark = WebSettings.FORCE_DARK_ON
                }
                
                // Enable algorithmic darkening (Android 13+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    isAlgorithmicDarkeningAllowed = true
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
                
                // Media settings
                mediaPlaybackRequiresUserGesture = false // Allow autoplay for better UX
                
                // Disable geolocation
                setGeolocationEnabled(false)
                
                // Enable zoom controls
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                
                // Disable multiple windows to prevent sites from opening new tabs
                // This prevents target="_blank" and window.open() from creating new windows
                setSupportMultipleWindows(false)
                
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
