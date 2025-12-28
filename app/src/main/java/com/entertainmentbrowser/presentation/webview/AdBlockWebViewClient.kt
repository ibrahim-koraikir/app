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
import com.entertainmentbrowser.util.adblock.AntiAdblockBypass
import com.entertainmentbrowser.util.adblock.FastAdBlockEngine
import com.entertainmentbrowser.util.adblock.HardcodedFilters
import java.io.ByteArrayInputStream
import java.util.LinkedHashMap
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
    private val antiAdblockBypass: AntiAdblockBypass? = null,
    private val onVideoDetected: (String) -> Unit = {},
    private val onUrlChanged: (String) -> Unit = {},
    private val onDrmDetected: () -> Unit = {},
    private val onLoadingChanged: (Boolean) -> Unit = {},
    private val onNavigationStateChanged: (Boolean, Boolean) -> Unit = { _, _ -> },
    private val onError: (String) -> Unit = {},
    private val onPageFinished: (String) -> Unit = {},
    private val onPageLoadError: (PageErrorType, Int) -> Unit = { _, _ -> }, // Enhanced error callback
    private val isAdBlockingEnabled: Boolean = true, // Default to true (blocking enabled)
    private val strictAdBlockingEnabled: Boolean = false // Default to false (balanced mode)
) : WebViewClient() {
    
    companion object {
        private const val TAG = "AdBlockWebViewClient"
        
        // ===== PERFORMANCE: LRU Cache for URL blocking decisions =====
        // Caches block/allow decisions to avoid re-checking the same URLs
        // Max 500 entries, auto-evicts oldest when full
        private const val URL_CACHE_SIZE = 500
        private val urlBlockCache = object : LinkedHashMap<String, Boolean>(URL_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                return size > URL_CACHE_SIZE
            }
        }
        
        // Pre-computed HashSets for O(1) lookups instead of O(n) list iteration
        private val essentialCdnSet = hashSetOf(
            "i.ytimg.com", "i1.ytimg.com", "i2.ytimg.com", "i3.ytimg.com", "i4.ytimg.com", "i9.ytimg.com",
            "s.ytimg.com", "yt3.ggpht.com", "yt4.ggpht.com",
            "lh3.googleusercontent.com", "lh4.googleusercontent.com", "lh5.googleusercontent.com", "lh6.googleusercontent.com",
            "fonts.googleapis.com", "fonts.gstatic.com", "www.gstatic.com",
            "encrypted-tbn0.gstatic.com", "encrypted-tbn1.gstatic.com", "encrypted-tbn2.gstatic.com", "encrypted-tbn3.gstatic.com",
            "ssl.gstatic.com", "www.google.com", "apis.google.com", "play.google.com", "accounts.google.com",
            "static.xx.fbcdn.net", "scontent.xx.fbcdn.net", "pbs.twimg.com", "abs.twimg.com",
            "cdn.jsdelivr.net", "cdnjs.cloudflare.com", "unpkg.com"
        )
        
        private val googleLayoutCriticalSet = hashSetOf(
            "www.google.com", "apis.google.com", "accounts.google.com",
            "fonts.googleapis.com", "fonts.gstatic.com", "www.gstatic.com", "ssl.gstatic.com",
            "encrypted-tbn0.gstatic.com", "encrypted-tbn1.gstatic.com", "encrypted-tbn2.gstatic.com", "encrypted-tbn3.gstatic.com",
            "lh3.googleusercontent.com", "lh4.googleusercontent.com", "lh5.googleusercontent.com", "lh6.googleusercontent.com",
            "i.ytimg.com", "yt3.ggpht.com"
        )
        
        private val mainFrameAllowSet = hashSetOf(
            "netflix.com", "hulu.com", "disneyplus.com", "primevideo.com",
            "hbomax.com", "max.com", "peacocktv.com", "paramountplus.com",
            "crunchyroll.com", "funimation.com", "spotify.com",
            "youtube.com", "youtu.be", "google.com", "facebook.com", "twitter.com",
            "instagram.com", "reddit.com", "wikipedia.org", "amazon.com",
            "apple.com", "microsoft.com", "github.com", "stackoverflow.com",
            "imdb.com", "rottentomatoes.com", "twitch.tv", "tiktok.com",
            "cnn.com", "bbc.com", "nytimes.com", "theguardian.com"
        )
        
        private val monetizationSet = hashSetOf("effectivegatecpm.com", "effectivegate.com")
        
        // Clear cache when page changes (called from onPageStarted)
        fun clearUrlCache() {
            synchronized(urlBlockCache) {
                urlBlockCache.clear()
            }
        }
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
    
    // PERFORMANCE: Cache extracted host to avoid repeated Uri.parse() calls
    private var cachedUrlHost: String? = null
    private var cachedUrlForHost: String? = null
    
    private fun getHostFast(url: String): String? {
        if (url == cachedUrlForHost) return cachedUrlHost
        cachedUrlForHost = url
        cachedUrlHost = try {
            android.net.Uri.parse(url).host?.lowercase()
        } catch (e: Exception) {
            null
        }
        return cachedUrlHost
    }

    /**
     * Intercept requests for API 21+ (WebResourceRequest parameter)
     * PERFORMANCE: Uses cached lookups and early returns
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
        
        // PERFORMANCE: Check cache first for instant decision
        synchronized(urlBlockCache) {
            urlBlockCache[url]?.let { shouldBlock ->
                return if (shouldBlock) createEmptyResponse() else null
            }
        }
        
        // PERFORMANCE: Fast host-based whitelist check using HashSet O(1)
        val host = getHostFast(url)
        if (host != null && essentialCdnSet.contains(host)) {
            cacheResult(url, false)
            return null
        }
        
        // GOOGLE LAYOUT-CRITICAL BYPASS
        if (host != null && googleLayoutCriticalSet.contains(host)) {
            cacheResult(url, false)
            return null
        }
        
        // Check for video URLs (moved after whitelist checks for performance)
        if (VideoDetector.isVideoUrl(url)) {
            onVideoDetected(url)
        }
        
        // Check and block ads/trackers
        return checkAndBlock(view, url, request?.isForMainFrame ?: false)
    }
    
    // PERFORMANCE: Cache the blocking decision
    private fun cacheResult(url: String, shouldBlock: Boolean) {
        synchronized(urlBlockCache) {
            urlBlockCache[url] = shouldBlock
        }
    }
    
    /**
     * Check if URL is a layout-critical Google resource that should never be blocked.
     * PERFORMANCE: Uses HashSet for O(1) lookup
     */
    private fun isGoogleResource(url: String): Boolean {
        val host = getHostFast(url) ?: return false
        return googleLayoutCriticalSet.contains(host)
    }
    
    /**
     * Intercept requests for API <21 (String URL parameter)
     * PERFORMANCE: Uses cached lookups
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
        
        // PERFORMANCE: Check cache first
        synchronized(urlBlockCache) {
            urlBlockCache[url]?.let { shouldBlock ->
                return if (shouldBlock) createEmptyResponse() else null
            }
        }
        
        // PERFORMANCE: Fast host-based whitelist check
        val host = getHostFast(url)
        if (host != null && (essentialCdnSet.contains(host) || googleLayoutCriticalSet.contains(host))) {
            cacheResult(url, false)
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
     * Whitelist of YOUR monetization ad domains that should NEVER be blocked.
     * PERFORMANCE: Now uses companion object HashSet
     */
    
    /**
     * Check if URL is from your whitelisted monetization ad networks
     * PERFORMANCE: Uses HashSet for O(1) lookup
     */
    private fun isWhitelistedMonetization(url: String): Boolean {
        val host = getHostFast(url) ?: return false
        return monetizationSet.any { host.contains(it) }
    }
    
    /**
     * Check if URL is from essential CDN domains that should never be blocked
     * PERFORMANCE: Uses HashSet for O(1) lookup
     */
    private fun isEssentialCdn(url: String): Boolean {
        val host = getHostFast(url) ?: return false
        return essentialCdnSet.contains(host)
    }
    
    /**
     * Check if URL's domain is in the main-frame allowlist.
     * PERFORMANCE: Uses HashSet for O(1) lookup
     */
    private fun isMainFrameAllowlisted(url: String): Boolean {
        val host = getHostFast(url) ?: return false
        return mainFrameAllowSet.any { host.contains(it) }
    }
    
    /**
     * Check if URL should be blocked and return appropriate response.
     * PERFORMANCE: Uses caching to avoid re-checking same URLs
     * 
     * **Ad Blocking Strategy:**
     * Uses AdvancedAdBlockEngine (95%+ blocking) with FastEngine and HardcodedFilters fallbacks.
     */
    private fun checkAndBlock(view: WebView?, url: String, isMainFrame: Boolean = false): WebResourceResponse? {
        try {
            // PERFORMANCE: Essential CDN check using fast host lookup
            if (isEssentialCdn(url)) {
                cacheResult(url, false)
                return null
            }
            
            // WHITELIST CHECK: Never block your own monetization ads
            if (isWhitelistedMonetization(url)) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "💰 ALLOWED (Monetization Whitelist): ${LogUtils.redactUrl(url)}")
                }
                cacheResult(url, false)
                return null
            }
            
            // Check AdvancedEngine FIRST (95%+ blocking with first-party ad detection)
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
                    return handleMainFrameBlock(view, url, "AdvancedEngine")
                }
                
                cacheResult(url, true)
                return createEmptyResponse()
            }
            
            // Fallback to FastEngine (for rules AdvancedEngine might miss)
            if (fastEngine.shouldBlock(url)) {
                blockedCount++
                if (isDirectLinkAd(url)) {
                    directLinkBlockedCount++
                }
                
                if (loggedBlockedSamples < maxLoggedSamplesPerPage) {
                    AdBlockMetrics.onRequestBlocked(url, "FastEngine")
                    loggedBlockedSamples++
                } else {
                    suppressedBlockedCount++
                }
                
                if (isMainFrame) {
                    return handleMainFrameBlock(view, url, "FastEngine")
                }
                
                cacheResult(url, true)
                return createEmptyResponse()
            }
            
            // Final fallback to HardcodedFilters
            if (HardcodedFilters.shouldBlock(url)) {
                blockedCount++
                if (isDirectLinkAd(url)) {
                    directLinkBlockedCount++
                }
                
                if (loggedBlockedSamples < maxLoggedSamplesPerPage) {
                    AdBlockMetrics.onRequestBlocked(url, "HardcodedFilters")
                    loggedBlockedSamples++
                } else {
                    suppressedBlockedCount++
                }
                
                if (isMainFrame) {
                    return handleMainFrameBlock(view, url, "HardcodedFilters")
                }
                
                cacheResult(url, true)
                return createEmptyResponse()
            }
            
            // Allow request - cache and log
            cacheResult(url, false)
            if (loggedAllowedSamples < maxLoggedSamplesPerPage) {
                AdBlockMetrics.onRequestAllowed(url)
                loggedAllowedSamples++
            } else {
                suppressedAllowedCount++
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking URL: $url", e)
            return null
        }
    }
    
    /**
     * Handle main frame blocking with smart redirect
     * PERFORMANCE: Extracted to reduce code duplication
     */
    private fun handleMainFrameBlock(view: WebView?, url: String, engine: String): WebResourceResponse? {
        // SMART REDIRECT: Check if this blocked URL contains a target URL
        val targetUrl = extractTargetUrl(url)
        if (targetUrl != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "🔄 Smart Redirect: Skipping tracker ${LogUtils.redactUrl(url)} -> ${LogUtils.redactUrl(targetUrl)}")
            }
            view?.post { view.loadUrl(targetUrl) }
            return createEmptyResponse()
        }

        // ALLOWLIST CHECK: Allow main-frame navigation to critical/essential domains
        if (isMainFrameAllowlisted(url)) {
            Log.w(TAG, "✅ Main Frame matched $engine but domain is allowlisted. Allowing: ${LogUtils.redactUrl(url)}")
            AdBlockMetrics.onMainFrameAllowlisted(url, engine)
            return null
        }

        // BALANCED MODE: Block main frame ad
        Log.w(TAG, "🚫 BLOCKED main frame ad ($engine): ${LogUtils.redactUrl(url)}")
        AdBlockMetrics.onMainFrameBlocked(url, engine)
        isMainFrameBlocked = true
        return createBlockedMainFrameResponse()
    }

    /**
     * Extract a target URL from a blocked tracker URL.
     * Many trackers pass the final destination as a query parameter.
     */
    private fun extractTargetUrl(blockedUrl: String): String? {
        try {
            val uri = android.net.Uri.parse(blockedUrl)
            
            // Comprehensive list of parameters used for redirect targets
            val targetParams = listOf(
                "url", "target", "dest", "destination", "next", "r", "link", 
                "adurl", "click_url", "clickurl", "redirect", "redir", "goto",
                "continue", "return", "returnurl", "return_url", "returnUrl",
                "callback", "fallback", "fallback_url", "browser_fallback_url",
                "landing", "landingurl", "landing_url", "landingUrl",
                "out", "outurl", "out_url", "outUrl",
                "exit", "exiturl", "exit_url", "exitUrl",
                "forward", "forwardurl", "forward_url", "forwardUrl",
                "to", "tourl", "to_url", "toUrl",
                "u", "l", "q", "ref", "src", "source"
            )
            
            for (param in targetParams) {
                val value = uri.getQueryParameter(param)
                if (!value.isNullOrEmpty()) {
                    // Check if it's a valid URL
                    if (value.startsWith("http://") || value.startsWith("https://")) {
                        return value
                    }
                    // Try URL decoding in case it's encoded
                    try {
                        val decoded = java.net.URLDecoder.decode(value, "UTF-8")
                        if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
                            return decoded
                        }
                    } catch (e: Exception) {
                        // Ignore decoding errors
                    }
                }
            }
            
            // Also check for base64 encoded URLs (some ad networks use this)
            for (param in listOf("data", "payload", "encoded", "b64")) {
                val value = uri.getQueryParameter(param)
                if (!value.isNullOrEmpty()) {
                    try {
                        val decoded = String(android.util.Base64.decode(value, android.util.Base64.DEFAULT))
                        if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
                            return decoded
                        }
                    } catch (e: Exception) {
                        // Not base64 encoded, ignore
                    }
                }
            }
            
            // Check URL path for embedded URLs (e.g., /redirect/https://example.com)
            val path = uri.path
            if (path != null) {
                val httpIndex = path.indexOf("http://")
                val httpsIndex = path.indexOf("https://")
                val startIndex = when {
                    httpIndex >= 0 && httpsIndex >= 0 -> minOf(httpIndex, httpsIndex)
                    httpIndex >= 0 -> httpIndex
                    httpsIndex >= 0 -> httpsIndex
                    else -> -1
                }
                if (startIndex >= 0) {
                    return path.substring(startIndex)
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
     * Check if two URLs are from the same domain
     */
    private fun isSameDomain(url: String, referrer: String?): Boolean {
        if (referrer == null) return true
        try {
            val urlHost = android.net.Uri.parse(url).host?.lowercase()?.removePrefix("www.") ?: return true
            val refHost = android.net.Uri.parse(referrer).host?.lowercase()?.removePrefix("www.") ?: return true
            return urlHost == refHost || urlHost.endsWith(".$refHost") || refHost.endsWith(".$urlHost")
        } catch (e: Exception) {
            return true
        }
    }
    
    /**
     * Check if URL has redirect parameters (indicates click hijacking)
     */
    private fun hasRedirectParameter(url: String): Boolean {
        val lowerUrl = url.lowercase()
        val redirectParams = listOf(
            "?url=http", "&url=http", "?link=http", "&link=http",
            "?dest=http", "&dest=http", "?target=http", "&target=http",
            "?redirect=http", "&redirect=http", "?r=http", "&r=http",
            "?goto=http", "&goto=http", "?next=http", "&next=http",
            "?continue=http", "&continue=http", "?return=http", "&return=http",
            "?adurl=", "&adurl=", "?click_url=", "&click_url="
        )
        return redirectParams.any { lowerUrl.contains(it) }
    }
    
    /**
     * Check if domain is whitelisted (legitimate sites)
     */
    private fun isWhitelistedDomain(url: String): Boolean {
        val whitelist = listOf(
            "youtube.com", "youtu.be", "google.com", "facebook.com", "twitter.com",
            "instagram.com", "reddit.com", "wikipedia.org", "amazon.com",
            "netflix.com", "hulu.com", "disneyplus.com", "primevideo.com",
            "hbomax.com", "peacocktv.com", "paramountplus.com", "crunchyroll.com",
            "funimation.com", "spotify.com", "apple.com", "microsoft.com",
            "github.com", "stackoverflow.com", "imdb.com", "rottentomatoes.com"
        )
        val lowerUrl = url.lowercase()
        return whitelist.any { lowerUrl.contains(it) }
    }
    
    /**
     * Check if URL is a Google OAuth/Sign-in URL that should be opened in system browser.
     * Google blocks OAuth from WebViews for security (Error 403: disallowed_useragent).
     */
    private fun isGoogleOAuthUrl(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return lowerUrl.contains("accounts.google.com/signin") ||
               lowerUrl.contains("accounts.google.com/o/oauth") ||
               lowerUrl.contains("accounts.google.com/servicelogin") ||
               lowerUrl.contains("accounts.google.com/v3/signin") ||
               lowerUrl.contains("accounts.google.com/accountchooser") ||
               (lowerUrl.contains("accounts.google.com") && lowerUrl.contains("oauth"))
    }
    
    /**
     * Sites that need relaxed ad blocking for video playback to work.
     * These sites use internal redirects that get falsely flagged as ads.
     */
    private val videoPlayerWhitelist = setOf(
        "eljooker.com"
    )
    
    /**
     * Check if current page is a video player site that needs relaxed blocking
     */
    private fun isVideoPlayerSite(pageUrl: String?): Boolean {
        if (pageUrl == null) return false
        val lowerUrl = pageUrl.lowercase()
        return videoPlayerWhitelist.any { lowerUrl.contains(it) }
    }
    
    /**
     * Detect suspicious redirect patterns that are likely ads
     */
    private fun isLikelyRedirectAd(url: String): Boolean {
        val lowerUrl = url.lowercase()
        
        // Suspicious redirect patterns - comprehensive list
        val suspiciousPatterns = listOf(
            // URL shorteners often used for ad redirects
            "bit.ly", "tinyurl.com", "goo.gl", "ow.ly", "t.co", "is.gd", "v.gd",
            "adf.ly", "linkvertise", "link-to.net", "lootlinks", "loot-link",
            "ouo.io", "ouo.press", "bc.vc", "exe.io", "fc.lc", "sh.st", "shorte.st",
            
            // Ad redirect domains
            "adclick", "adserver", "adservice", "adsrv", "adtrack", "adsystem",
            "clicktrack", "clickserve", "redirect.php", "redir.php",
            "go.php", "out.php", "link.php", "track.php", "click.php",
            
            // Direct link ad networks
            "propellerads", "propeller-tracking", "propellerclick",
            "popcash", "popads", "popunder", "popup", "pop-up", "pop_up",
            "adsterra", "adsterratools", "hilltopads", "exoclick", "exosrv",
            "trafficjunky", "juicyads", "plugrush", "clickadu", "clickadilla",
            "richpush", "evadav", "adcash", "monetag", "a-ads",
            
            // Streaming site ad redirects
            "streamtape", "dood.to", "dood.watch", "dood.so", "dood.pm", "dood.wf",
            "mixdrop", "voe.sx", "voeunblock", "filemoon", "streamwish",
            "vidhide", "lulustream", "vtube", "vadbam", "vidsrc",
            "2embed", "autoembed", "moviesapi", "moviesjoy", "fmovies",
            
            // Suspicious query parameters
            "?ad=", "&ad=", "?adid=", "&adid=", "?campaign=", "&campaign=",
            "?utm_source=", "?ref=ad", "&ref=ad", "?aff=", "&aff=",
            "?click_id=", "&click_id=", "?clickid=", "&clickid=",
            "?subid=", "&subid=", "?zoneid=", "&zoneid=",
            
            // Redirect URL parameters (indicates click hijacking)
            "?url=http", "&url=http", "?link=http", "&link=http",
            "?dest=http", "&dest=http", "?target=http", "&target=http",
            "?redirect=http", "&redirect=http", "?r=http", "&r=http",
            "?goto=http", "&goto=http", "?next=http", "&next=http",
            "?continue=http", "&continue=http", "?return=http", "&return=http",
            
            // Interstitial ad patterns
            "interstitial", "splash", "landing", "offer", "promo",
            "sponsor", "sponsored", "partner",
            
            // Crypto miners (bonus protection)
            "coinhive", "crypto-loot", "cryptoloot", "coin-hive", "coinzilla",
            
            // Additional suspicious patterns
            "/aff/", "/affiliate/", "/track/", "/click/", "/clk/",
            "/redirect/", "/redir/", "/go/", "/out/", "/away/", "/exit/",
            "/cpa/", "/cpc/", "/cpm/", "/ppc/", "/ppl/"
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
     * IMPORTANT: Injects ad blocking scripts EARLY to catch click hijacking
     * PERFORMANCE: Clears URL cache for fresh page
     */
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        
        // Store current page URL for first-party ad detection
        currentPageUrl = url
        
        // PERFORMANCE: Clear URL cache for new page to avoid stale decisions
        clearUrlCache()
        
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
        
        // CRITICAL: Inject ad blocking scripts EARLY to catch click hijacking
        // This runs before page scripts load, blocking direct link ads
        if (isAdBlockingEnabled) {
            view?.evaluateJavascript(EARLY_AD_BLOCKER_SCRIPT, null)
            
            // ANTI-ADBLOCK BYPASS: Inject early bypass script (object spoofing + bait preservation)
            // This must run BEFORE page scripts to fool anti-adblock detection
            antiAdblockBypass?.let { bypass ->
                if (bypass.shouldApplyBypass(url)) {
                    val earlyScript = bypass.getEarlyBypassScript(url)
                    if (earlyScript.isNotEmpty()) {
                        view?.evaluateJavascript(earlyScript, null)
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "🛡️ Anti-adblock early bypass injected for: ${LogUtils.redactUrl(url)}")
                        }
                    }
                }
            }
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
        
        // Inject ad blocking scripts
        if (isAdBlockingEnabled) {
            view?.evaluateJavascript(EARLY_AD_BLOCKER_SCRIPT, null) // Reinforce click blocking
            
            // ANTI-ADBLOCK BYPASS: Inject late bypass script (element hiding)
            // This hides anti-adblock warning elements after page loads
            antiAdblockBypass?.let { bypass ->
                if (bypass.shouldApplyBypass(url)) {
                    val lateScript = bypass.getLateBypassScript(url)
                    if (lateScript.isNotEmpty()) {
                        view?.evaluateJavascript(lateScript, null)
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "🛡️ Anti-adblock late bypass injected for: ${LogUtils.redactUrl(url)}")
                        }
                    }
                }
            }
        }
        view?.evaluateJavascript(AD_HIDING_CSS, null)
        view?.evaluateJavascript(VideoDetector.VIDEO_DETECTION_SCRIPT, null)
        view?.evaluateJavascript(DrmDetector.DRM_DETECTION_SCRIPT, null)
        
        // Inject bottom padding CSS to prevent tab bar from covering website bottom elements
        view?.evaluateJavascript(TAB_BAR_PADDING_CSS, null)
        
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
     * EARLY Ad Blocker Script - Balanced: Fast + Effective
     * Blocks direct link ads, popups, and redirect clicks
     */
    private val EARLY_AD_BLOCKER_SCRIPT = """
        (function(){
            if(window.__ab)return;window.__ab=1;
            var ads=['popup','popunder','adclick','doubleclick','googlesyndication','propeller','adsterra','hilltopads','exoclick','trafficjunky','juicyads','clickadu','richpush','evadav','adcash','monetag','linkvertise','lootlinks','streamtape','dood.to','dood.watch','dood.so','dood.pm','dood.wf','mixdrop','voe.sx','voeunblock','filemoon','streamwish','vidhide','lulustream','revcontent','mgid','outbrain','taboola','adserver','adsystem','adtrack','/redirect/','/redir/','/go/','/out/','/away/','/click/','/track/','/aff/','?url=http','&url=http','?redirect=http','?dest=http','?link=http','?goto=http','?r=http'];
            var wl=['youtube.com','youtu.be','google.com','facebook.com','twitter.com','instagram.com','reddit.com','amazon.com','netflix.com','hulu.com','disneyplus.com'];
            function w(u){if(!u)return 0;var l=u.toLowerCase();for(var i=0;i<wl.length;i++)if(l.indexOf(wl[i])!==-1)return 1;return 0}
            function a(u){if(!u||w(u))return 0;var l=u.toLowerCase();for(var i=0;i<ads.length;i++)if(l.indexOf(ads[i])!==-1)return 1;return 0}
            function s(u){try{var c=location.hostname.replace('www.','');var t=new URL(u,location.href).hostname.replace('www.','');return c===t}catch(e){return 1}}
            var o=window.open;window.open=function(u){if(a(u)){console.log('[AB]popup:'+u);return null}return o.apply(window,arguments)};
            document.addEventListener('click',function(e){
                var t=e.target;while(t&&t.tagName!=='A')t=t.parentElement;
                if(t&&t.href){
                    var h=t.href;
                    if(a(h)){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();console.log('[AB]click:'+h);return false}
                    if(!s(h)&&!w(h)){
                        var l=h.toLowerCase();
                        if(l.indexOf('?')!==-1&&(l.indexOf('url=')!==-1||l.indexOf('redirect=')!==-1||l.indexOf('dest=')!==-1||l.indexOf('link=')!==-1||l.indexOf('goto=')!==-1)){
                            e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();
                            try{var p=new URL(h);var ps=['url','link','dest','redirect','goto','r','target'];for(var i=0;i<ps.length;i++){var v=p.searchParams.get(ps[i]);if(v&&(v.startsWith('http://')||v.startsWith('https://'))&&!a(v)){location.href=v;return false}}}catch(x){}
                            console.log('[AB]redir:'+h);return false
                        }
                    }
                }
            },true);
            ['mousedown','touchstart'].forEach(function(ev){document.addEventListener(ev,function(e){var t=e.target;while(t&&t.tagName!=='A')t=t.parentElement;if(t&&t.href&&a(t.href)){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();return false}},true)});
        })();
    """.trimIndent()
    
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
     * CSS injection to add bottom padding so website fixed elements aren't covered by tab bar.
     * Adds padding to body and adjusts fixed/sticky positioned elements at the bottom.
     * Tab bar height is approximately 72dp (40dp thumbnails + 32dp padding).
     */
    private val TAB_BAR_PADDING_CSS = """
        (function() {
            try {
                if (window.__tabBarPaddingApplied) return;
                window.__tabBarPaddingApplied = true;
                
                var style = document.createElement('style');
                style.id = 'entertainment-browser-tab-padding';
                style.textContent = `
                    /* Add padding to body for tab bar */
                    body {
                        padding-bottom: 80px !important;
                    }
                    
                    /* Adjust fixed positioned elements at bottom */
                    [style*="position: fixed"][style*="bottom: 0"],
                    [style*="position:fixed"][style*="bottom:0"],
                    .fixed-bottom,
                    .sticky-bottom,
                    [class*="fixed-bottom"],
                    [class*="sticky-bottom"],
                    footer[style*="position: fixed"],
                    nav[style*="position: fixed"][style*="bottom"],
                    div[style*="position: fixed"][style*="bottom: 0"] {
                        bottom: 80px !important;
                    }
                `;
                document.head.appendChild(style);
                
                // Also adjust any elements with inline fixed bottom styles
                var fixedElements = document.querySelectorAll('*');
                fixedElements.forEach(function(el) {
                    var style = window.getComputedStyle(el);
                    if (style.position === 'fixed' && parseInt(style.bottom) <= 20) {
                        el.style.bottom = '80px';
                    }
                });
            } catch(e) {
                console.log('Tab padding CSS error:', e);
            }
        })();
    """.trimIndent()

    /**
     * WebRTC Blocking Script - OPTIMIZED minimal version
     */
    private val WEBRTC_BLOCK_SCRIPT = """
        (function(){try{var s=function(){throw new Error('WebRTC disabled')};if(window.RTCPeerConnection)window.RTCPeerConnection=s;if(window.webkitRTCPeerConnection)window.webkitRTCPeerConnection=s}catch(e){}})();
    """.trimIndent()
    
    /**
     * Redirect Blocker Script - OPTIMIZED minimal version
     * Most blocking is done by EARLY_AD_BLOCKER_SCRIPT, this is just a no-op backup
     */
    private val REDIRECT_BLOCKER_SCRIPT = """(function(){})();""".trimIndent()
    
    /**
     * Override URL loading to handle external intents (Play Store, etc.)
     * 
     * PERFORMANCE NOTE: This method runs on the UI thread, so we only use lightweight
     * checks here (allowlists, pattern matching). Heavy ad-block engine evaluation is
     * delegated to shouldInterceptRequest() (IO thread) and injected JS scripts.
     * 
     * IMPORTANT: Same-domain navigation is ALWAYS allowed to prevent breaking
     * internal site functionality (video players, pagination, etc.)
     */
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        
        try {
            // ALWAYS allow whitelisted domains (Google, YouTube, etc.) for normal navigation
            // This ensures search results, Images/Videos tabs, and form submissions work
            if (isWhitelistedDomain(url)) {
                Log.d(TAG, "✅ Allowing whitelisted domain navigation: ${LogUtils.redactUrl(url)}")
                return false // Let WebView handle it normally
            }
            
            // Check if this is a cross-domain navigation
            val isCrossDomain = !isSameDomain(url, currentPageUrl)
            
            // CRITICAL FIX: ALWAYS allow same-domain navigation
            // This prevents breaking internal site functionality like video players,
            // pagination, form submissions, and internal redirects
            if (!isCrossDomain) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "✅ Allowing same-domain navigation: ${LogUtils.redactUrl(url)}")
                }
                return false // Let WebView handle it normally
            }
            
            // LIGHTWEIGHT AD BLOCKING (only for cross-domain, UI-thread safe)
            // Heavy engine checks are done in shouldInterceptRequest() on IO thread
            if (isAdBlockingEnabled) {
                // SKIP blocking for video player sites (e.g., eljooker.com)
                // These sites use external video hosts that get falsely flagged
                if (isVideoPlayerSite(currentPageUrl)) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "✅ Allowing cross-domain for video player site: ${LogUtils.redactUrl(url)}")
                    }
                    return false // Let WebView handle it normally
                }
                
                // LIGHTWEIGHT CHECK: Block obvious redirect ad patterns using simple string matching
                // This is fast enough for UI thread - just pattern matching, no regex or rule evaluation
                if (isLikelyRedirectAd(url)) {
                    Log.d(TAG, "🚫 BLOCKED cross-domain redirect ad pattern: $url")
                    blockedCount++
                    directLinkBlockedCount++
                    AdBlockMetrics.onRequestBlocked(url, "RedirectPattern")
                    
                    // Try to extract target URL and redirect there instead
                    val targetUrl = extractTargetUrl(url)
                    if (targetUrl != null && !isLikelyRedirectAd(targetUrl) && isWhitelistedDomain(targetUrl)) {
                        Log.d(TAG, "🔄 Smart redirect: $url -> $targetUrl")
                        view?.loadUrl(targetUrl)
                    } else {
                        // Go back if possible
                        view?.post { if (view.canGoBack()) view.goBack() }
                    }
                    return true
                }
                
                // LIGHTWEIGHT CHECK: Block cross-domain navigations with redirect parameters
                // Full ad-blocking is handled by shouldInterceptRequest() and injected JS
                if (hasRedirectParameter(url)) {
                    Log.d(TAG, "🚫 BLOCKED cross-domain redirect: $url")
                    blockedCount++
                    directLinkBlockedCount++
                    AdBlockMetrics.onRequestBlocked(url, "CrossDomainRedirect")
                    
                    val targetUrl = extractTargetUrl(url)
                    if (targetUrl != null && isWhitelistedDomain(targetUrl)) {
                        Log.d(TAG, "🔄 Smart redirect: $url -> $targetUrl")
                        view?.loadUrl(targetUrl)
                    } else {
                        view?.post { if (view.canGoBack()) view.goBack() }
                    }
                    return true
                }
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
     * Handle page load errors with classification for user-friendly display
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
        
        // Only report main frame errors to user with enhanced error overlay
        if (request?.isForMainFrame == true) {
            Log.e(TAG, "❌ Main frame error: $errorMessage (code: $errorCode) for URL: $url")
            
            // Classify error type for user-friendly display
            val errorType = classifyError(errorCode, errorMessage)
            
            // Trigger enhanced error overlay for main frame errors
            onPageLoadError(errorType, errorCode)
            onError(errorMessage)
        } else {
            // Log subresource errors but don't show to user
            Log.w(TAG, "⚠️ Subresource error: $errorMessage (code: $errorCode) for URL: $url")
        }
    }
    
    /**
     * Classify WebView error codes into user-friendly error types
     */
    private fun classifyError(errorCode: Int, errorMessage: String): PageErrorType {
        return when (errorCode) {
            // Network errors
            ERROR_HOST_LOOKUP, ERROR_CONNECT -> PageErrorType.NO_INTERNET
            ERROR_TIMEOUT -> PageErrorType.TIMEOUT
            
            // SSL/Security errors
            ERROR_FAILED_SSL_HANDSHAKE -> PageErrorType.SSL_ERROR
            
            // Server errors
            ERROR_BAD_URL, ERROR_UNSUPPORTED_SCHEME -> PageErrorType.PAGE_NOT_FOUND
            ERROR_FILE_NOT_FOUND -> PageErrorType.PAGE_NOT_FOUND
            
            // Check if it's an ad-block related issue
            else -> {
                val lowerMessage = errorMessage.lowercase()
                when {
                    lowerMessage.contains("net::err_internet") || 
                    lowerMessage.contains("net::err_network") ||
                    lowerMessage.contains("no internet") -> PageErrorType.NO_INTERNET
                    
                    lowerMessage.contains("timeout") -> PageErrorType.TIMEOUT
                    
                    lowerMessage.contains("ssl") || 
                    lowerMessage.contains("certificate") -> PageErrorType.SSL_ERROR
                    
                    lowerMessage.contains("not found") || 
                    lowerMessage.contains("404") -> PageErrorType.PAGE_NOT_FOUND
                    
                    lowerMessage.contains("500") || 
                    lowerMessage.contains("server") -> PageErrorType.SERVER_ERROR
                    
                    else -> PageErrorType.UNKNOWN
                }
            }
        }
    }
}
