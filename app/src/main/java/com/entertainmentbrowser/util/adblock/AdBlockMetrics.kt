package com.entertainmentbrowser.util.adblock

import android.util.Log
import com.entertainmentbrowser.BuildConfig
import com.entertainmentbrowser.util.LogUtils

/**
 * AdBlockMetrics - Performance metrics tracking for ad-blocking
 * 
 * This class tracks and logs performance metrics for the ad-blocking system,
 * including blocked request counts, load times, and memory usage.
 * 
 * **Privacy Protection:**
 * - URLs are redacted in release builds (domain only)
 * - Full URLs only logged in debug builds for development
 * 
 * Metrics are logged with tag "AdBlockMetrics" for easy filtering in Logcat.
 */
object AdBlockMetrics {
    
    private const val TAG = "AdBlockMetrics"
    
    // Session metrics
    private var sessionStartTime = System.currentTimeMillis()
    private var totalBlockedRequests = 0
    private var totalAllowedRequests = 0
    private var pageLoadCount = 0
    
    // Per-page metrics
    private var currentPageUrl: String? = null
    private var currentPageStartTime = 0L
    private var currentPageBlockedCount = 0
    private var currentPageAllowedCount = 0
    
    /**
     * Called when a new page starts loading
     */
    fun onPageStarted(url: String) {
        currentPageUrl = url
        currentPageStartTime = System.currentTimeMillis()
        currentPageBlockedCount = 0
        currentPageAllowedCount = 0
        
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "📄 Page started: ${LogUtils.redactUrl(url)}")
        }
    }
    
    /**
     * Called when a page finishes loading
     */
    fun onPageFinished(url: String, blockedCount: Int) {
        val loadTime = System.currentTimeMillis() - currentPageStartTime
        pageLoadCount++
        
        // Update session totals
        totalBlockedRequests += blockedCount
        totalAllowedRequests += currentPageAllowedCount
        
        // Log page metrics (only in debug builds)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "✅ Page finished: ${LogUtils.redactUrl(url)}")
            Log.d(TAG, "   Load time: ${loadTime}ms")
            Log.d(TAG, "   Blocked: $blockedCount requests")
            Log.d(TAG, "   Allowed: $currentPageAllowedCount requests")
            Log.d(TAG, "   Blocking rate: ${calculateBlockingRate(blockedCount, currentPageAllowedCount)}%")
        }
        
        // Reset page metrics
        currentPageUrl = null
        currentPageStartTime = 0L
        currentPageBlockedCount = 0
        currentPageAllowedCount = 0
    }
    
    /**
     * Called when a request is blocked
     * Note: Only logs in DEBUG builds to avoid verbose production logs
     */
    fun onRequestBlocked(url: String, engine: String) {
        currentPageBlockedCount++
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "🚫 Blocked by $engine: ${LogUtils.redactUrl(url)}")
        }
    }
    
    /**
     * Called when a request is allowed
     * Note: Only logs in DEBUG builds to avoid verbose production logs
     */
    fun onRequestAllowed(url: String) {
        currentPageAllowedCount++
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "✓ Allowed: ${LogUtils.redactUrl(url)}")
        }
    }
    
    // Main-frame specific metrics
    private var mainFrameBlockedCount = 0
    private var mainFrameAllowlistedCount = 0
    
    /**
     * Called when a main-frame ad request is blocked
     * This tracks direct ad URLs that were blocked at the main-frame level
     */
    fun onMainFrameBlocked(url: String, engine: String) {
        mainFrameBlockedCount++
        currentPageBlockedCount++
        Log.i(TAG, "🚫 Main-frame BLOCKED by $engine: ${LogUtils.redactUrl(url)}")
    }
    
    /**
     * Called when a main-frame request matched ad rules but was allowlisted
     * This tracks cases where we allowed navigation to critical domains despite ad match
     */
    fun onMainFrameAllowlisted(url: String, engine: String) {
        mainFrameAllowlistedCount++
        currentPageAllowedCount++
        Log.i(TAG, "✅ Main-frame ALLOWLISTED (matched $engine but domain is critical): ${LogUtils.redactUrl(url)}")
    }
    
    /**
     * Log session summary
     */
    fun logSessionSummary() {
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        val sessionMinutes = sessionDuration / 60000
        
        Log.i(TAG, "========================================")
        Log.i(TAG, "Ad-Blocking Session Summary")
        Log.i(TAG, "========================================")
        Log.i(TAG, "Session duration: ${sessionMinutes}m")
        Log.i(TAG, "Pages loaded: $pageLoadCount")
        Log.i(TAG, "Total blocked: $totalBlockedRequests requests")
        Log.i(TAG, "Total allowed: $totalAllowedRequests requests")
        Log.i(TAG, "Overall blocking rate: ${calculateBlockingRate(totalBlockedRequests, totalAllowedRequests)}%")
        Log.i(TAG, "Avg blocked per page: ${if (pageLoadCount > 0) totalBlockedRequests / pageLoadCount else 0}")
        Log.i(TAG, "Main-frame ads blocked: $mainFrameBlockedCount")
        Log.i(TAG, "Main-frame allowlisted: $mainFrameAllowlistedCount")
        Log.i(TAG, "========================================")
    }
    
    /**
     * Reset all metrics
     */
    fun reset() {
        sessionStartTime = System.currentTimeMillis()
        totalBlockedRequests = 0
        totalAllowedRequests = 0
        pageLoadCount = 0
        currentPageUrl = null
        currentPageStartTime = 0L
        currentPageBlockedCount = 0
        currentPageAllowedCount = 0
        mainFrameBlockedCount = 0
        mainFrameAllowlistedCount = 0
        
        Log.d(TAG, "📊 Metrics reset")
    }
    
    /**
     * Get current session metrics
     */
    fun getSessionMetrics(): SessionMetrics {
        return SessionMetrics(
            sessionDuration = System.currentTimeMillis() - sessionStartTime,
            pageLoadCount = pageLoadCount,
            totalBlockedRequests = totalBlockedRequests,
            totalAllowedRequests = totalAllowedRequests,
            blockingRate = calculateBlockingRate(totalBlockedRequests, totalAllowedRequests)
        )
    }
    
    /**
     * Calculate blocking rate percentage
     */
    private fun calculateBlockingRate(blocked: Int, allowed: Int): Int {
        val total = blocked + allowed
        return if (total > 0) {
            (blocked * 100) / total
        } else {
            0
        }
    }
    
    /**
     * Data class for session metrics
     */
    data class SessionMetrics(
        val sessionDuration: Long,
        val pageLoadCount: Int,
        val totalBlockedRequests: Int,
        val totalAllowedRequests: Int,
        val blockingRate: Int
    )
}
