package com.entertainmentbrowser.util

import android.os.Bundle
import android.webkit.WebView
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages WebView instances and their states for tabs
 * Each tab gets its own WebView that persists across screen switches
 * Implements LRU eviction to prevent memory issues on low-end devices
 */
class WebViewStateManager {
    
    // Map of tabId -> WebView
    private val webViewCache = ConcurrentHashMap<String, WebView>()
    
    // Map of tabId -> WebView state bundle
    private val stateCache = ConcurrentHashMap<String, Bundle>()
    
    // Map of tabId -> last access timestamp for LRU tracking
    private val lastAccessTime = ConcurrentHashMap<String, Long>()
    
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
     * Remove WebView when tab is closed
     */
    fun removeWebView(tabId: String) {
        webViewCache.remove(tabId)?.let { webView ->
            webView.destroy()
        }
        stateCache.remove(tabId)
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
     */
    private fun evictLeastRecentlyUsed() {
        // Find LRU tab (excluding currently active tabs)
        val lruTabId = lastAccessTime
            .filter { (tabId, _) -> webViewCache.containsKey(tabId) }
            .minByOrNull { (_, timestamp) -> timestamp }
            ?.key
        
        lruTabId?.let { tabId ->
            android.util.Log.d("WebViewStateManager", "Evicting LRU WebView for tab: $tabId")
            removeWebView(tabId)
            lastAccessTime.remove(tabId)
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

    // Track monetized tabs (AdBlock disabled)
    private val monetizedTabs = ConcurrentHashMap.newKeySet<String>()

    /**
     * Set a tab as monetized (AdBlock disabled)
     */
    fun setMonetized(tabId: String, isMonetized: Boolean) {
        if (isMonetized) {
            monetizedTabs.add(tabId)
        } else {
            monetizedTabs.remove(tabId)
        }
    }

    /**
     * Check if a tab is monetized
     */
    fun isMonetized(tabId: String): Boolean {
        return monetizedTabs.contains(tabId)
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
