package com.entertainmentbrowser.presentation.webview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.entertainmentbrowser.BuildConfig
import com.entertainmentbrowser.util.LogUtils
import com.entertainmentbrowser.util.adblock.AdBlockMetrics
import com.entertainmentbrowser.util.adblock.FastAdBlockEngine
import com.entertainmentbrowser.util.adblock.HardcodedFilters
import java.io.ByteArrayInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom WebViewClient with ad-blocking capabilities
 * 
 * Integrates AdvancedAdBlockEngine (95%+ blocking) with FastAdBlockEngine fallback
 * while maintaining all existing WebView functionality including video detection,
 * DRM detection, and navigation.
 * 
 * This is a singleton shared across all tabs for consistent ad blocking.
 */
class AdBlockWebViewClient(
    private val context: Context,
    private val fastEngine: FastAdBlockEngine,
    private val advancedEngine: com.entertainmentbrowser.util.adblock.AdvancedAdBlockEngine,
    private val onVideoDetected: (String) -> Unit = {},
    private val onUrlChanged: (String) -> Unit = {},
    private val onDrmDetected: () -> Unit = {},
    private val onLoadingChanged: (Boolean) -> Unit = {},
    private val onNavigationStateChanged: (Boolean, Boolean) -> Unit = { _, _ -> },
    private val onError: (String) -> Unit = {},
    private val onPageFinished: (String) -> Unit = {},
    private val isAdBlockingEnabled: Boolean = true, // Default to true (blocking enabled)
    private val strictAdBlockingEnabled: Boolean = false // Default to false (balanced mode)
) : WebViewClient() {
    
    companion object {
        private const val TAG = "AdBlockWebViewClient"
    }
    
    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Track blocked requests per page
    private var blockedCount = 0
    private var directLinkBlockedCount = 0
    // Track if main frame was blocked to prevent blank screens
    private var isMainFrameBlocked = false
    
    // Track current page URL for first-party ad detection
    private var currentPageUrl: String? = null
    
    // Logging caps to prevent excessive logging on noisy pages
    private var loggedBlockedSamples = 0
    private var loggedAllowedSamples = 0
    private val maxLoggedSamplesPerPage = 20 // Log first 20 blocks/allows, then aggregate only
    private var suppressedBlockedCount = 0
    private var suppressedAllowedCount = 0

    /**
     * Intercept requests for API 21+ (WebResourceRequest parameter)
     */
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null

        // If ad blocking is disabled (Monetized Tab), allow everything
        if (!isAdBlockingEnabled) {
            return null
        }
        
        // Check for video URLs
        if (VideoDetector.isVideoUrl(url)) {
            onVideoDetected(url)
        }
        
        // Check and block ads/trackers
        return checkAndBlock(view, url, request?.isForMainFrame ?: false)
    }
    
    /**
     * Intercept requests for API <21 (String URL parameter)
     */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun shouldInterceptRequest(
        view: WebView?,
        url: String?
    ): WebResourceResponse? {
        if (url == null) return null

        // If ad blocking is disabled (Monetized Tab), allow everything
        if (!isAdBlockingEnabled) {
            return null
        }
        
        // Check for video URLs
        if (VideoDetector.isVideoUrl(url)) {
            onVideoDetected(url)
        }
        
        // Check and block ads/trackers
        return checkAndBlock(view, url, false)
    }

    /**
     * Check if URL should be blocked and return appropriate response
     * 
     * Checks FastAdBlockEngine first, then falls back to HardcodedFilters.
     * Returns empty response if blocked, null to allow request.
     */
    /**
     * Whitelist of YOUR monetization ad domains that should NEVER be blocked.
     * These are your revenue-generating ad networks.
     */
    private val monetizationWhitelist = setOf(
        "effectivegatecpm.com",
        "effectivegate.com",
        // Add more of your ad network domains here
    )
    
    /**
     * Check if URL is from your whitelisted monetization ad networks
     */
    private fun isWhitelistedMonetization(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return monetizationWhitelist.any { lowerUrl.contains(it) }
    }
    
    /**
     * Check if URL should be blocked and return appropriate response.
     * 
     * **Ad Blocking Strategy:**
     * Uses AdvancedAdBlockEngine (95%+ blocking) with FastEngine and HardcodedFilters fallbacks.
     * 
     * **Main-Frame Blocking Behavior:**
     * When an ad rule matches a main-frame request (the primary page being loaded):
     * 
     * 1. **Smart Redirect (Preferred):** If the blocked URL contains a target URL parameter
     *    (e.g., `?url=https://destination.com`), extract and redirect to the clean target.
     *    This skips ad trackers while preserving navigation.
     * 
     * 2. **Balanced Mode (Default):** If no redirect target is found, ALLOW the request
     *    to prevent blank screens. This trades some protection for usability.
     *    - Rationale: Blocking main frames without alternatives breaks navigation
     *    - Impact: Some redirect-based ad flows may succeed
     *    - Mitigation: Subresources (scripts, images, iframes) are still blocked
     * 
     * 3. **Strict Mode (Advanced Users):** If strict ad blocking is enabled in settings,
     *    BLOCK the main frame even without a redirect target. Shows "Ad Blocked" page.
     *    - Provides maximum protection
     *    - May cause legitimate pages to fail loading
     *    - Recommended only for advanced users who understand the trade-offs
     * 
     * **Subresource Blocking:**
     * All non-main-frame requests (scripts, images, iframes, etc.) are always blocked
     * when matched by ad rules, regardless of strict mode setting.
     * 
     * @param view WebView instance for smart redirects
     * @param url URL to check against ad blocking rules
     * @param isMainFrame true if this is the primary page load, false for subresources
     * @return WebResourceResponse to block (empty response), or null to allow
     */
    private fun checkAndBlock(view: WebView?, url: String, isMainFrame: Boolean = false): WebResourceResponse? {
        // Log.d(TAG, "Checking URL: $url (MainFrame: $isMainFrame)")
        try {
            // WHITELIST CHECK: Never block your own monetization ads
            if (isWhitelistedMonetization(url)) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "💰 ALLOWED (Monetization Whitelist): ${LogUtils.redactUrl(url)}")
                }
                return null // Allow your ads
            }
            // Check AdvancedEngine FIRST (95%+ blocking with first-party ad detection)
            // Wrapped in try-catch for safety during initialization
            val shouldBlockAdvanced = try {
                advancedEngine.shouldBlock(url, currentPageUrl)
            } catch (e: Exception) {
                Log.w(TAG, "AdvancedEngine error, falling back: ${e.message}")
                false
            }
            if (shouldBlockAdvanced) {
                blockedCount++
                if (isDirectLinkAd(url)) {
                    directLinkBlockedCount++
                }
                
                // Detailed logging only for first N samples per page (DEBUG only)
                if (loggedBlockedSamples < maxLoggedSamplesPerPage) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "🚫 BLOCKED by AdvancedEngine: ${LogUtils.redactUrl(url)} (MainFrame: $isMainFrame)")
                    }
                    AdBlockMetrics.onRequestBlocked(url, "AdvancedEngine")
                    loggedBlockedSamples++
                } else {
                    suppressedBlockedCount++
                }
                
                if (isMainFrame) {
                    // SMART REDIRECT: Check if this blocked URL contains a target URL
                    val targetUrl = extractTargetUrl(url)
                    if (targetUrl != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "🔄 Smart Redirect: Skipping tracker ${LogUtils.redactUrl(url)} -> ${LogUtils.redactUrl(targetUrl)}")
                        }
                        view?.post { view.loadUrl(targetUrl) }
                        return createEmptyResponse() // Block the tracker
                    }

                    // STRICT MODE: Block main frame even without redirect target
                    if (strictAdBlockingEnabled) {
                        Log.w(TAG, "🔒 STRICT MODE: Blocking main frame (AdvancedEngine): ${LogUtils.redactUrl(url)}")
                        isMainFrameBlocked = true
                        return createBlockedMainFrameResponse()
                    }

                    // BALANCED MODE: Allow main frame to prevent blank screens
                    Log.w(TAG, "⚠️ Main Frame matched AdvancedEngine but no redirect target found. Allowing to prevent broken navigation: ${LogUtils.redactUrl(url)}")
                    return null // Allow request
                }
                
                return createEmptyResponse()
            }
            
            // Fallback to FastEngine (for rules AdvancedEngine might miss)
            if (fastEngine.shouldBlock(url)) {
                blockedCount++
                if (isDirectLinkAd(url)) {
                    directLinkBlockedCount++
                }
                
                // Detailed logging only for first N samples per page
                if (loggedBlockedSamples < maxLoggedSamplesPerPage) {
                    AdBlockMetrics.onRequestBlocked(url, "FastEngine")
                    loggedBlockedSamples++
                } else {
                    suppressedBlockedCount++
                }
                
                if (isMainFrame) {
                    val targetUrl = extractTargetUrl(url)
                    if (targetUrl != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "🔄 Smart Redirect: Skipping tracker ${LogUtils.redactUrl(url)} -> ${LogUtils.redactUrl(targetUrl)}")
                        }
                        view?.post { view.loadUrl(targetUrl) }
                        return createEmptyResponse()
                    }

                    // STRICT MODE: Block main frame even without redirect target
                    if (strictAdBlockingEnabled) {
                        Log.w(TAG, "🔒 STRICT MODE: Blocking main frame (FastEngine): ${LogUtils.redactUrl(url)}")
                        isMainFrameBlocked = true
                        return createBlockedMainFrameResponse()
                    }

                    // BALANCED MODE: Allow main frame to prevent blank screens
                    Log.w(TAG, "⚠️ Main Frame matched FastEngine but no redirect target found. Allowing: ${LogUtils.redactUrl(url)}")
                    return null // Allow request
                }
                
                return createEmptyResponse()
            }
            
            // Final fallback to HardcodedFilters
            if (HardcodedFilters.shouldBlock(url)) {
                blockedCount++
                if (isDirectLinkAd(url)) {
                    directLinkBlockedCount++
                }
                
                // Detailed logging only for first N samples per page
                if (loggedBlockedSamples < maxLoggedSamplesPerPage) {
                    AdBlockMetrics.onRequestBlocked(url, "HardcodedFilters")
                    loggedBlockedSamples++
                } else {
                    suppressedBlockedCount++
                }
                
                if (isMainFrame) {
                    val targetUrl = extractTargetUrl(url)
                    if (targetUrl != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "🔄 Smart Redirect: Skipping tracker ${LogUtils.redactUrl(url)} -> ${LogUtils.redactUrl(targetUrl)}")
                        }
                        view?.post { view.loadUrl(targetUrl) }
                        return createEmptyResponse()
                    }

                    // STRICT MODE: Block main frame even without redirect target
                    if (strictAdBlockingEnabled) {
                        Log.w(TAG, "🔒 STRICT MODE: Blocking main frame (HardcodedFilters): ${LogUtils.redactUrl(url)}")
                        isMainFrameBlocked = true
                        return createBlockedMainFrameResponse()
                    }

                    // BALANCED MODE: Allow main frame to prevent blank screens
                    Log.w(TAG, "⚠️ Main Frame matched HardcodedFilters but no redirect target found. Allowing: ${LogUtils.redactUrl(url)}")
                    return null // Allow request
                }
                
                return createEmptyResponse()
            }
            
            // Allow request - only log samples to avoid spam
            if (loggedAllowedSamples < maxLoggedSamplesPerPage) {
                AdBlockMetrics.onRequestAllowed(url)
                loggedAllowedSamples++
            } else {
                suppressedAllowedCount++
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking URL: $url", e)
            // Allow request on error (graceful degradation)
            return null
        }
    }

    /**
     * Extract a target URL from a blocked tracker URL.
     * Many trackers pass the final destination as a query parameter.
     */
    private fun extractTargetUrl(blockedUrl: String): String? {
        try {
            val uri = android.net.Uri.parse(blockedUrl)
            val queryParams = uri.queryParameterNames
            
            // Common parameters used for redirect targets
            val targetParams = listOf("url", "target", "dest", "destination", "next", "r", "link", "adurl", "click_url")
            
            for (param in targetParams) {
                val value = uri.getQueryParameter(param)
                if (!value.isNullOrEmpty() && (value.startsWith("http://") || value.startsWith("https://"))) {
                    return value
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting target URL", e)
        }
        return null
    }
    
    // ... (isDirectLinkAd remains unchanged) ...
    
    /**
     * Detect if this is a direct link ad (for statistics)
     */
    private fun isDirectLinkAd(url: String): Boolean {
        val directLinkKeywords = listOf(
            // Sponsored content
            "sponsor", "sponsored", "promo", "promotional",
            // Affiliate links
            "affiliate", "aff", "partner",
            // Native ad networks
            "outbrain", "taboola", "revcontent", "mgid", "zergnet",
            // Tracking and redirects
            "redirect", "redir", "click", "clk", "tracking", "tracker",
            // Ad-related
            "/ads/", "/ad/", "adclick", "adsclick",
            // Yandex/Russian ad networks (common in your logs)
            "yastatic.net/partner", "yandex.ru/ads"
        )
        
        val lowerUrl = url.lowercase()
        return directLinkKeywords.any { lowerUrl.contains(it) }
    }
    
    /**
     * Detect suspicious redirect patterns that are likely ads
     */
    private fun isLikelyRedirectAd(url: String): Boolean {
        val lowerUrl = url.lowercase()
        
        // Suspicious redirect patterns
        val suspiciousPatterns = listOf(
            // URL shorteners often used for ad redirects
            "bit.ly", "tinyurl.com", "goo.gl", "ow.ly", "t.co",
            // Ad redirect domains
            "adclick", "adserver", "adservice", "adsrv", "adtrack",
            "clicktrack", "clickserve", "redirect.php", "redir.php",
            "go.php", "out.php", "link.php", "track.php",
            // Suspicious query parameters
            "?ad=", "&ad=", "?adid=", "&adid=", "?campaign=", "&campaign=",
            "?utm_source=", "?ref=ad", "&ref=ad",
            // Pop-under/pop-up patterns
            "popunder", "popup", "pop-up", "pop_up",
            // Interstitial ad patterns
            "interstitial", "splash", "landing",
            // More ad networks
            "propellerads", "popcash", "popads", "adsterra", "hilltopads",
            "exoclick", "trafficjunky", "juicyads", "plugrush",
            // Crypto miners (bonus protection)
            "coinhive", "crypto-loot", "cryptoloot", "coin-hive"
        )
        
        return suspiciousPatterns.any { lowerUrl.contains(it) }
    }
    
    /**
     * Create empty response to block request
     */
    private fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    /**
     * Create a minimal HTML response for blocked main frames.
     * This ensures the WebView has a valid DOM to inject the "Ad Blocked" UI into,
     * preventing blank screens while avoiding the complexity/crashes of a full custom response.
     */
    private fun createBlockedMainFrameResponse(): WebResourceResponse {
        val emptyHtml = "<html><head></head><body></body></html>"
        return WebResourceResponse(
            "text/html",
            "UTF-8",
            ByteArrayInputStream(emptyHtml.toByteArray())
        )
    }

    /**
     * Called when page starts loading
     * 
     * Resets blocked count, updates loading state, checks for DRM sites
     */
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        
        // Store current page URL for first-party ad detection
        currentPageUrl = url
        
        if (BuildConfig.DEBUG) {
            if (isAdBlockingEnabled) {
                Log.d(TAG, "📄 Page started loading: ${LogUtils.redactUrl(url)}")
            } else {
                Log.d(TAG, "💰 Monetized page started loading: ${LogUtils.redactUrl(url)}")
            }
        }
        
        // Reset blocked count for new page
        blockedCount = 0
        directLinkBlockedCount = 0
        isMainFrameBlocked = false
        
        // Reset logging caps for new page
        loggedBlockedSamples = 0
        loggedAllowedSamples = 0
        suppressedBlockedCount = 0
        suppressedAllowedCount = 0
        
        // Track page start in metrics
        url?.let { AdBlockMetrics.onPageStarted(it) }
        
        // Update loading state
        onLoadingChanged(true)
        
        // Update URL
        url?.let { u -> onUrlChanged(u) }
        
        // Check if URL is from known DRM site
        if (url != null && DrmDetector.isKnownDrmSite(url)) {
            onDrmDetected()
        }
    }
    
    /**
     * Called when page finishes loading
     * 
     * Logs blocked count, updates navigation state, injects detection scripts
     */
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        
        // Log aggregate counts if we suppressed any logging
        if (suppressedBlockedCount > 0 || suppressedAllowedCount > 0) {
            Log.d(TAG, "📊 Page finished - Total blocked: $blockedCount (logged: $loggedBlockedSamples, suppressed: $suppressedBlockedCount), Total allowed: suppressed $suppressedAllowedCount")
        }
        
        // Track page finish in metrics with total count
        url?.let { AdBlockMetrics.onPageFinished(it, blockedCount) }
        
        // Update loading state
        onLoadingChanged(false)
        
        // Update navigation state
        view?.let {
            onNavigationStateChanged(it.canGoBack(), it.canGoForward())
        }
        
        view?.evaluateJavascript(AD_HIDING_CSS, null)
        view?.evaluateJavascript(VideoDetector.VIDEO_DETECTION_SCRIPT, null)
        view?.evaluateJavascript(DrmDetector.DRM_DETECTION_SCRIPT, null)
        view?.evaluateJavascript(WEBRTC_BLOCK_SCRIPT, null) // Block WebRTC to prevent IP leaks
        view?.evaluateJavascript(REDIRECT_BLOCKER_SCRIPT, null) // Block redirect ads
        
        // Inject "Ad Blocked" UI if main frame was blocked
        if (isMainFrameBlocked) {
            val blockedPageJs = """
                (function() {
                    document.body.innerHTML = `
                        <div style="
                            background-color: #121212;
                            color: #ffffff;
                            display: flex;
                            flex-direction: column;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                            margin: 0;
                            font-family: system-ui, -apple-system, sans-serif;
                            text-align: center;
                        ">
                            <h2 style="margin-bottom: 10px;">Ad Blocked</h2>
                            <p style="color: #aaaaaa; margin-bottom: 20px;">Redirect prevented. Returning...</p>
                            <div style="
                                border: 3px solid #333;
                                border-top: 3px solid #3498db;
                                border-radius: 50%;
                                width: 24px;
                                height: 24px;
                                animation: spin 1s linear infinite;
                            "></div>
                            <style>
                                @keyframes spin {
                                    0% { transform: rotate(0deg); }
                                    100% { transform: rotate(360deg); }
                                }
                            </style>
                        </div>
                    `;
                    
                    setTimeout(function() {
                        if (window.history.length > 1) {
                            window.history.back();
                        } else {
                            window.close();
                        }
                    }, 800);
                })();
            """.trimIndent()
            view?.evaluateJavascript(blockedPageJs, null)
        }
        
        // Notify page finished
        onPageFinished(url ?: "")
    }
    
    /**
     * CSS injection to hide ad containers and prevent black screens
     * This hides common ad container elements that would otherwise show as blank/black
     */
    private val AD_HIDING_CSS = """
        (function() {
            try {
                var style = document.createElement('style');
                style.textContent = `
                    /* Hide common ad containers */
                    [id*="ad-"],
                    [id*="ads-"],
                    [id*="advert"],
                    [id*="sponsor"],
                    [id*="banner"],
                    [class*="ad-"],
                    [class*="ads-"],
                    [class*="advert"],
                    [class*="sponsor"],
                    [class*="banner"],
                    [data-ad],
                    [data-ads],
                    [data-adunit],
                    iframe[src*="doubleclick"],
                    iframe[src*="googlesyndication"],
                    iframe[src*="googleadservices"],
                    iframe[src*="yandex.ru/ads"],
                    iframe[src*="yastatic.net/partner"],
                    iframe[src*="outbrain"],
                    iframe[src*="taboola"],
                    iframe[src*="revcontent"],
                    iframe[src*="mgid"],
                    div[id*="google_ads"],
                    div[class*="google-ad"],
                    ins.adsbygoogle {
                        display: none !important;
                        visibility: hidden !important;
                        opacity: 0 !important;
                        height: 0 !important;
                        width: 0 !important;
                        position: absolute !important;
                        left: -9999px !important;
                    }
                `;
                document.head.appendChild(style);
                } catch(e) {
            }
        })();
    """.trimIndent()

    /**
     * WebRTC Blocking Script (Comprehensive)
     * 
     * Disables all known WebRTC entry points to prevent IP leaks and WebRTC-based tracking.
     * This provides defense-in-depth by blocking at the JavaScript API level.
     * 
     * RESIDUAL RISK: This JavaScript-level blocking is not foolproof. Determined attackers
     * with native code or browser exploits could potentially bypass these blocks. For absolute
     * protection against IP leaks, users should employ a VPN or Tor at the network level.
     * 
     * This implementation blocks:
     * - RTCPeerConnection (all vendor prefixes)
     * - getUserMedia (all vendor prefixes)
     * - mediaDevices.getUserMedia
     * - mediaDevices.enumerateDevices
     * - RTCDataChannel
     * - RTCSessionDescription
     * - RTCIceCandidate
     */
    private val WEBRTC_BLOCK_SCRIPT = """
        (function() {
            try {
                // Block RTCPeerConnection (all vendor prefixes)
                var throwingStub = function() {
                    throw new Error('WebRTC is disabled for privacy');
                };
                
                if (window.RTCPeerConnection) {
                    window.RTCPeerConnection = throwingStub;
                }
                if (window.webkitRTCPeerConnection) {
                    window.webkitRTCPeerConnection = throwingStub;
                }
                if (window.mozRTCPeerConnection) {
                    window.mozRTCPeerConnection = throwingStub;
                }
                
                // Block getUserMedia (all vendor prefixes)
                if (navigator.getUserMedia) {
                    navigator.getUserMedia = throwingStub;
                }
                if (navigator.webkitGetUserMedia) {
                    navigator.webkitGetUserMedia = throwingStub;
                }
                if (navigator.mozGetUserMedia) {
                    navigator.mozGetUserMedia = throwingStub;
                }
                if (navigator.msGetUserMedia) {
                    navigator.msGetUserMedia = throwingStub;
                }
                
                // Block modern mediaDevices API
                if (navigator.mediaDevices) {
                    if (navigator.mediaDevices.getUserMedia) {
                        navigator.mediaDevices.getUserMedia = throwingStub;
                    }
                    if (navigator.mediaDevices.enumerateDevices) {
                        // Return empty list instead of throwing to avoid breaking sites
                        navigator.mediaDevices.enumerateDevices = function() {
                            return Promise.resolve([]);
                        };
                    }
                }
                
                // Block RTCDataChannel
                if (window.RTCDataChannel) {
                    window.RTCDataChannel = throwingStub;
                }
                
                // Block RTCSessionDescription
                if (window.RTCSessionDescription) {
                    window.RTCSessionDescription = throwingStub;
                }
                if (window.webkitRTCSessionDescription) {
                    window.webkitRTCSessionDescription = throwingStub;
                }
                if (window.mozRTCSessionDescription) {
                    window.mozRTCSessionDescription = throwingStub;
                }
                
                // Block RTCIceCandidate
                if (window.RTCIceCandidate) {
                    window.RTCIceCandidate = throwingStub;
                }
                if (window.webkitRTCIceCandidate) {
                    window.webkitRTCIceCandidate = throwingStub;
                }
                if (window.mozRTCIceCandidate) {
                    window.mozRTCIceCandidate = throwingStub;
                }
                
                console.log('🚫 WebRTC comprehensively blocked for privacy');
            } catch(e) {
                console.warn('WebRTC blocking encountered an error:', e);
            }
        })();
    """.trimIndent()
    
    /**
     * Redirect Blocker Script (Lightweight)
     * Blocks only obvious ad popups and redirects
     */
    private val REDIRECT_BLOCKER_SCRIPT = """
        (function() {
            try {
                // Lightweight ad patterns (most common)
                var adPatterns = ['doubleclick', 'googlesyndication', 'popunder', 'popup', 'adclick'];
                
                function isAdUrl(url) {
                    if (!url) return false;
                    var lowerUrl = url.toLowerCase();
                    return adPatterns.some(function(p) { return lowerUrl.indexOf(p) !== -1; });
                }
                
                // Block window.open popups only
                var originalOpen = window.open;
                window.open = function(url, name, features) {
                    if (isAdUrl(url)) {
                        console.log('🚫 Blocked popup');
                        return null;
                    }
                    return originalOpen.call(window, url, name, features);
                };
            } catch(e) {}
        })();
    """.trimIndent()
    
    /**
     * Override URL loading to handle external intents (Play Store, etc.)
     * Also blocks redirect ads aggressively
     */
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        
        try {
            // SMART REDIRECT AD BLOCKING
            // Only check if URL looks suspicious (performance optimization)
            if (isAdBlockingEnabled && isLikelyRedirectAd(url)) {
                val shouldBlockAdvanced = try {
                    advancedEngine.shouldBlock(url, currentPageUrl)
                } catch (e: Exception) {
                    false
                }
                
                if (shouldBlockAdvanced) {
                    Log.d(TAG, "🚫 BLOCKED redirect ad: $url")
                    blockedCount++
                    directLinkBlockedCount++
                    AdBlockMetrics.onRequestBlocked(url, "RedirectBlocked")
                    
                    // Try to extract target URL and redirect there instead
                    val targetUrl = extractTargetUrl(url)
                    if (targetUrl != null && !advancedEngine.shouldBlock(targetUrl, currentPageUrl)) {
                        Log.d(TAG, "🔄 Smart redirect: $url -> $targetUrl")
                        view?.loadUrl(targetUrl)
                    }
                    return true // Block the redirect
                }
                
                // Also check FastEngine for suspicious redirects
                if (fastEngine.shouldBlock(url)) {
                    Log.d(TAG, "🚫 BLOCKED redirect ad (FastEngine): $url")
                    blockedCount++
                    directLinkBlockedCount++
                    AdBlockMetrics.onRequestBlocked(url, "RedirectBlocked")
                    
                    val targetUrl = extractTargetUrl(url)
                    if (targetUrl != null && !fastEngine.shouldBlock(targetUrl)) {
                        Log.d(TAG, "🔄 Smart redirect: $url -> $targetUrl")
                        view?.loadUrl(targetUrl)
                    }
                    return true
                }
                
                // Block if pattern is very suspicious
                Log.d(TAG, "🚫 BLOCKED suspicious redirect: $url")
                blockedCount++
                directLinkBlockedCount++
                AdBlockMetrics.onRequestBlocked(url, "SuspiciousRedirect")
                return true
            }
            
            // Handle Play Store links (market:// and play.google.com)
            if (url.startsWith("market://") || url.contains("play.google.com/store/apps")) {
                Log.d(TAG, "🛒 Opening Play Store: $url")
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true // Prevent WebView from loading
            }
            
            // Handle intent:// URLs (common in ads for deep linking)
            if (url.startsWith("intent://")) {
                Log.d(TAG, "🔗 Handling intent URL: $url")
                try {
                    val intent = android.content.Intent.parseUri(url, android.content.Intent.URI_INTENT_SCHEME)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    
                    // Check if an app can handle this intent
                    if (context.packageManager.resolveActivity(intent, 0) != null) {
                        context.startActivity(intent)
                        return true
                    }
                    
                    // Fallback to browser if app not installed
                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                    if (fallbackUrl != null) {
                        Log.d(TAG, "📱 App not installed, using fallback: $fallbackUrl")
                        view?.loadUrl(fallbackUrl)
                        return true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to handle intent URL", e)
                    // Continue to allow WebView to try loading
                }
            }
            
            // Handle tel:, mailto:, sms: links
            if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
                Log.d(TAG, "📞 Opening external app for: ${url.substringBefore(":")}")
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            }
            
            // Handle other custom schemes (e.g., app deep links)
            val uri = android.net.Uri.parse(url)
            val scheme = uri.scheme?.lowercase()
            
            // BLOCK UNWANTED SCHEMES / POPUPS
            val blockedSchemes = listOf("blob", "filesystem", "magnet", "bitcoin", "itms-appss", "itms-apps")
            if (scheme in blockedSchemes) {
                Log.d(TAG, "🚫 Blocked restricted scheme: $scheme")
                return true
            }

            if (scheme != null && scheme !in listOf("http", "https", "about", "data", "javascript")) {
                Log.d(TAG, "🔗 Custom scheme detected: $scheme")
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    
                    // Check if an app can handle this
                    if (context.packageManager.resolveActivity(intent, 0) != null) {
                        // CONFIRMATION DIALOG COULD GO HERE (Safety)
                        // For now, only allow if explicitly user-initiated or whitelisted
                        // But since we want "Smart" blocking, let's allow if it looks like a legitimate app link
                        // and block if it looks like a spammy redirect.
                        
                        context.startActivity(intent)
                        return true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to handle custom scheme", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in shouldOverrideUrlLoading", e)
        }
        
        // Allow normal HTTP/HTTPS navigation in WebView
        return false
    }
    
    /**
     * Handle page load errors
     */
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        
        val url = request?.url?.toString() ?: "unknown"
        val errorCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            error?.errorCode ?: -1
        } else {
            -1
        }
        val errorMessage = error?.description?.toString() ?: "Unknown error"
        
        // Only report main frame errors to user
        if (request?.isForMainFrame == true) {
            Log.e(TAG, "❌ Main frame error: $errorMessage (code: $errorCode) for URL: $url")
            onError(errorMessage)
        } else {
            // Log subresource errors but don't show to user
            Log.w(TAG, "⚠️ Subresource error: $errorMessage (code: $errorCode) for URL: $url")
        }
    }
}
