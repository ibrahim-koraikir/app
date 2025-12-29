package com.entertainmentbrowser.util

import android.os.Bundle
import android.webkit.WebView
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages WebView instances and their states for tabs.
 * Each tab gets its own WebView that persists across screen switches.
 * Implements LRU eviction to prevent memory issues on low-end devices.
 * 
 * ## Memory Leak Prevention
 * 
 * When removing or recycling WebViews, use [releaseWebViewForPooling] to ensure
 * all UI-specific references are cleared before the WebView is returned to the pool.
 * This prevents Activity/Fragment leaks from:
 * - WebViewClient/WebChromeClient holding Activity references
 * - JavaScript interfaces holding UI-scoped references
 * - Listeners capturing Activity scope
 * 
 * @see WebViewPool for pool-level documentation on memory safety
 */
class WebViewStateManager {
    
    // Map of tabId -> WebView
    private val webViewCache = ConcurrentHashMap<String, WebView>()
    
    // Map of tabId -> WebView state bundle
    private val stateCache = ConcurrentHashMap<String, Bundle>()
    
    // Map of tabId -> last access timestamp for LRU tracking
    private val lastAccessTime = ConcurrentHashMap<String, Long>()
    
    /**
     * Check if a WebView is cached for the given tab ID.
     * This can be used to determine if getWebViewForTab will return an existing
     * WebView (with preserved state) or create a new one.
     */
    fun hasWebViewForTab(tabId: String): Boolean {
        return webViewCache.containsKey(tabId)
    }
    
    /**
     * Get or create WebView for a specific tab
     * Updates LRU timestamp and enforces cache size limit
     */
    fun getWebViewForTab(tabId: String, factory: () -> WebView): WebView {
        // Update access time for LRU tracking
        lastAccessTime[tabId] = System.currentTimeMillis()
        
        // Check cache limit before creating new WebView
        if (webViewCache.size >= MAX_CACHED_WEBVIEWS && !webViewCache.containsKey(tabId)) {
            evictLeastRecentlyUsed()
        }
        
        return webViewCache.getOrPut(tabId) {
            factory().also { webView ->
                // Restore state if exists
                stateCache[tabId]?.let { state ->
                    webView.restoreState(state)
                    stateCache.remove(tabId)
                }
            }
        }
    }
    
    /**
     * Save WebView state when tab becomes inactive
     */
    fun saveWebViewState(tabId: String) {
        webViewCache[tabId]?.let { webView ->
            val state = Bundle()
            webView.saveState(state)
            stateCache[tabId] = state
        }
    }
    
    /**
     * Remove WebView when tab is closed.
     * The WebView is destroyed, not recycled to the pool.
     * Also releases the GPU hardware-acceleration slot.
     */
    fun removeWebView(tabId: String) {
        webViewCache.remove(tabId)?.let { webView ->
            // Clear UI references before destroying to prevent any lingering callbacks
            releaseWebViewReferences(webView)
            webView.destroy()
        }
        stateCache.remove(tabId)
        lastAccessTime.remove(tabId)
        
        // Release GPU hardware-acceleration slot
        GpuMemoryManager.releaseTab(tabId)
        android.util.Log.d("WebViewStateManager", "Released GPU slot for tab $tabId")
    }
    
    /**
     * Release a WebView for pooling instead of destroying it.
     * 
     * This method:
     * 1. Removes the WebView from the cache
     * 2. Clears all UI-specific references (clients, listeners, JS interfaces)
     * 3. Returns the WebView to the pool for reuse
     * 
     * Use this instead of [removeWebView] when you want to recycle the WebView
     * for better performance.
     * 
     * @param tabId The tab ID whose WebView should be released
     * @param additionalJsInterfaces Additional JavaScript interface names to remove (optional)
     */
    fun releaseWebViewForPooling(tabId: String, additionalJsInterfaces: List<String> = emptyList()) {
        webViewCache.remove(tabId)?.let { webView ->
            // Clear all UI-specific references before recycling
            releaseWebViewReferences(webView, additionalJsInterfaces)
            
            // Return to pool for reuse
            WebViewPool.recycle(webView)
            
            android.util.Log.d("WebViewStateManager", "Released WebView for tab $tabId to pool")
        }
        stateCache.remove(tabId)
        lastAccessTime.remove(tabId)
        
        // Release GPU hardware-acceleration slot so new tabs can use it
        GpuMemoryManager.releaseTab(tabId)
        android.util.Log.d("WebViewStateManager", "Released GPU slot for tab $tabId")
    }
    
    /**
     * Clear all UI-specific references from a WebView to prevent memory leaks.
     * 
     * This is called before destroying or recycling a WebView to ensure:
     * - WebViewClient/WebChromeClient don't hold Activity references
     * - JavaScript interfaces don't hold UI-scoped references
     * - Listeners don't capture Activity scope
     * 
     * @param webView The WebView to clean up
     * @param additionalJsInterfaces Additional JavaScript interface names to remove
     */
    private fun releaseWebViewReferences(
        webView: WebView,
        additionalJsInterfaces: List<String> = emptyList()
    ) {
        try {
            webView.apply {
                // Clear WebViewClient to prevent Activity leaks
                webViewClient = android.webkit.WebViewClient()
                
                // Clear WebChromeClient to prevent Activity leaks
                webChromeClient = null
                
                // Remove known JavaScript interfaces
                // "AndroidInterface" is used by CustomWebView for video detection
                removeJavascriptInterface("AndroidInterface")
                
                // Remove any additional JS interfaces specified by caller
                additionalJsInterfaces.forEach { name ->
                    removeJavascriptInterface(name)
                }
                
                // Clear all listeners that may hold Activity/Fragment references
                setOnTouchListener(null)
                setOnLongClickListener(null)
                setOnScrollChangeListener(null)
                setDownloadListener(null)
            }
        } catch (e: Exception) {
            android.util.Log.e("WebViewStateManager", "Failed to release WebView references", e)
        }
    }
    
    /**
     * Pause WebView when not visible (saves battery)
     * Does NOT update LRU timestamp - pausing is a lifecycle event, not user interaction
     */
    fun pauseWebView(tabId: String) {
        webViewCache[tabId]?.let { webView ->
            webView.onPause()
            webView.pauseTimers()
        }
    }
    
    /**
     * Resume WebView when visible
     * Updates LRU timestamp as this indicates user is viewing the tab
     */
    fun resumeWebView(tabId: String) {
        // Update access time - user is actively viewing this tab
        lastAccessTime[tabId] = System.currentTimeMillis()
        
        webViewCache[tabId]?.let { webView ->
            webView.onResume()
            webView.resumeTimers()
        }
    }
    
    /**
     * Called when app goes to background
     * Performs aggressive cache trimming to free memory
     */
    fun onAppBackgrounded() {
        val targetSize = MAX_CACHED_WEBVIEWS / 2
        android.util.Log.d("WebViewStateManager", "App backgrounded - trimming cache from ${webViewCache.size} to $targetSize")
        trimCache(targetSize)
    }
    
    /**
     * Get current state of a WebView
     */
    fun getWebViewState(tabId: String): WebViewState? {
        return webViewCache[tabId]?.let { webView ->
            WebViewState(
                url = webView.url ?: "",
                title = webView.title ?: "",
                canGoBack = webView.canGoBack(),
                canGoForward = webView.canGoForward(),
                scrollX = webView.scrollX,
                scrollY = webView.scrollY
            )
        }
    }
    
    /**
     * Evict the least recently used WebView from cache
     * Excludes currently active tabs to prevent disruption
     * 
     * Uses [releaseWebViewForPooling] to recycle the WebView instead of destroying it,
     * which frees GPU/heap memory while allowing the WebView to be reused.
     */
    private fun evictLeastRecentlyUsed() {
        // Find LRU tab (excluding currently active tabs)
        val lruTabId = lastAccessTime
            .filter { (tabId, _) -> webViewCache.containsKey(tabId) }
            .minByOrNull { (_, timestamp) -> timestamp }
            ?.key
        
        lruTabId?.let { tabId ->
            android.util.Log.d("WebViewStateManager", "Evicting LRU WebView for tab: $tabId")
            // Use releaseWebViewForPooling to recycle instead of destroy
            // This frees GPU/heap memory while allowing WebView reuse
            releaseWebViewForPooling(tabId, listOf("AndroidInterface"))
        }
    }
    
    /**
     * Trim cache to specified size by removing least recently used WebViews
     * Used for proactive memory management when app backgrounds
     * 
     * @param maxSize Target cache size (default: MAX_CACHED_WEBVIEWS)
     */
    fun trimCache(maxSize: Int = MAX_CACHED_WEBVIEWS) {
        while (webViewCache.size > maxSize) {
            evictLeastRecentlyUsed()
        }
        
        if (webViewCache.size > maxSize) {
            android.util.Log.d("WebViewStateManager", "Cache trimmed to ${webViewCache.size} WebViews (target: $maxSize)")
        }
    }
    
    /**
     * Clear all WebViews (call when app closes)
     */
    fun clearAll() {
        webViewCache.values.forEach { it.destroy() }
        webViewCache.clear()
        stateCache.clear()
        lastAccessTime.clear()
    }
    
    /**
     * Get number of cached WebViews
     */
    fun getCacheSize(): Int = webViewCache.size
    
    companion object {
        private const val MAX_CACHED_WEBVIEWS = 20  // Match your max tabs
    }
}

data class WebViewState(
    val url: String,
    val title: String,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val scrollX: Int,
    val scrollY: Int
)
