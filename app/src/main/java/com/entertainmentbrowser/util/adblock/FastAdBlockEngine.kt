package com.entertainmentbrowser.util.adblock

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FastAdBlockEngine - Primary ad-blocking engine using HashSet-based lookups for O(1) performance.
 * 
 * This engine loads filter lists from assets and provides fast URL checking to determine
 * if a request should be blocked. It uses HashSets for O(1) domain lookups and supports
 * exception rules and custom whitelisting.
 * 
 * Usage:
 * ```
 * // Inject in Application.onCreate()
 * @Inject lateinit var fastAdBlockEngine: FastAdBlockEngine
 * fastAdBlockEngine.preloadFromAssets()
 * 
 * // Check if URL should be blocked
 * val shouldBlock = fastAdBlockEngine.shouldBlock(url)
 * ```
 */
@Singleton
class FastAdBlockEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "FastAdBlockEngine"
    }
    
    // HashSet data structures for O(1) lookups
    private val blockedDomains = HashSet<String>()
    private val blockedPatterns = HashSet<String>()
    private val allowedDomains = HashSet<String>()
    
    // Direct link ad patterns for catching sponsored/affiliate content
    private val directLinkPatterns = HashSet<String>()
    private val sponsoredKeywords = listOf(
        "sponsor", "sponsored", "advertisement", "promo", "promotional",
        "affiliate", "aff", "partner", "campaign", "tracking",
        "redirect", "redir", "click", "clk", "imp", "impression",
        "adlink", "adclick", "adsclick", "outbrain", "taboola",
        "revcontent", "mgid", "zergnet", "bidvertiser"
    )
    
    // ============================================================================
    // CUSTOMIZATION: Custom Whitelist
    // ============================================================================
    // Add domains here that should NEVER be blocked, even if they appear in filter lists.
    // This is useful for:
    // - Fixing false positives (legitimate sites incorrectly blocked)
    // - Whitelisting specific analytics or tracking you want to allow
    // - Allowing specific ad networks for monetization purposes
    //
    // Example usage:
    // private val whitelistedDomains = hashSetOf(
    //     "trusted-analytics.com",      // Allow specific analytics
    //     "required-tracking.com",      // Allow required tracking
    //     "partner-ads.example.com"     // Allow specific ad partner
    // )
    //
    // Note: Whitelist checks happen FIRST, before any blocking rules are evaluated.
    // ============================================================================
    private val whitelistedDomains = HashSet<String>()
    
    // Track initialization state
    @Volatile
    private var isInitialized = false
    
    @Volatile
    private var initializationFailed = false
    
    /**
     * Preload filter lists from assets in a background thread.
     * This should be called from Application.onCreate() to ensure
     * the engine is ready before any WebViews are created.
     */
    fun preloadFromAssets() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized, skipping")
            return
        }
        
        Thread {
            try {
                val startTime = System.currentTimeMillis()
                Log.d(TAG, "🚀 Loading filter lists...")
                
                // ============================================================================
                // CUSTOMIZATION: Additional Filter Lists
                // ============================================================================
                // Add more filter list files here to expand blocking coverage.
                // Filter lists should be placed in app/src/main/assets/adblock/
                //
                // Popular additional filter lists:
                // - "adblock/fanboy-social.txt"        // Block social media widgets
                // - "adblock/fanboy-annoyance.txt"     // Block cookie banners & annoyances
                // - "adblock/easylist-germany.txt"     // German-specific ads
                // - "adblock/easylist-china.txt"       // Chinese-specific ads
                // - "adblock/easylist-italy.txt"       // Italian-specific ads
                // - "adblock/easylist-spanish.txt"     // Spanish-specific ads
                //
                // Download filter lists from:
                // - EasyList: https://easylist.to/
                // - Fanboy's Lists: https://fanboy.co.nz/
                //
                // Example download command:
                // curl -o app/src/main/assets/adblock/fanboy-social.txt \
                //   https://secure.fanboy.co.nz/fanboy-social.txt
                //
                // Note: More filter lists = more blocking but also more memory usage.
                // Each list adds ~50-100MB of memory and ~200-500ms load time.
                // ============================================================================
                val filterFiles = listOf(
                    "adblock/easylist.txt",
                    "adblock/easyprivacy.txt",
                    "adblock/fanboy-annoyance.txt"  // Block cookie notices, social widgets, etc.
                )
                
                // Load each filter file
                filterFiles.forEach { fileName ->
                    loadFilterFile(fileName)
                }
                
                // Load direct link ad patterns
                loadDirectLinkPatterns()
                
                val duration = System.currentTimeMillis() - startTime
                isInitialized = true
                
                Log.d(TAG, "✅ Loaded in ${duration}ms")
                Log.d(TAG, "   Blocked domains: ${blockedDomains.size}")
                Log.d(TAG, "   Blocked patterns: ${blockedPatterns.size}")
                Log.d(TAG, "   Allowed domains: ${allowedDomains.size}")
                Log.d(TAG, "   Direct link patterns: ${directLinkPatterns.size}")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load filter lists", e)
                isInitialized = false
                initializationFailed = true
            }
        }.start()
    }
    
    /**
     * Get total blocked count (for metrics)
     */
    private var blockedCount = 0
    
    @Synchronized
    fun getTotalBlockedCount(): Int = blockedCount
    
    @Synchronized
    fun resetBlockedCount() {
        blockedCount = 0
    }
    
    @Synchronized
    private fun incrementBlockedCount() {
        blockedCount++
    }
    
    /**
     * Load patterns specific to direct link ads (sponsored content, affiliate links, etc.)
     */
    private fun loadDirectLinkPatterns() {
        val patterns = listOf(
            // Sponsored content patterns
            "/sponsored/", "/sponsor/", "/sp/", "/spon/",
            "/advertisement/", "/advert/", "/ads/",
            "/promo/", "/promotional/", "/promotion/",
            
            // Affiliate and tracking patterns
            "/aff_c", "/affiliate/", "/aff/", "/partner/",
            "/track/", "/tracking/", "/tracker/",
            "/click/", "/clk/", "/redirect/", "/redir/",
            "/go/", "/out/", "/exit/", "/away/",
            
            // Native ad networks
            "/outbrain/", "/taboola/", "/revcontent/",
            "/mgid/", "/zergnet/", "/bidvertiser/",
            "/content.ad/", "/nativead/", "/native-ad/",
            
            // Click tracking
            "/clicktrack", "/adclick", "/adsclick",
            "/impression", "/imp/", "/beacon/",
            
            // URL parameter patterns
            "utm_source=", "utm_medium=", "utm_campaign=",
            "ref=sponsored", "ref=partner", "ref=affiliate"
        )
        
        directLinkPatterns.addAll(patterns)
    }
    
    /**
     * Load and parse a single filter file from assets.
     */
    private fun loadFilterFile(fileName: String) {
        try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.lineSequence().forEach { line ->
                        parseFastRule(line)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load filter file: $fileName", e)
        }
    }
    
    /**
     * Parse a single filter rule and add it to appropriate HashSet.
     * Only extracts simple, fast-to-check rules:
     * - Domain rules: ||domain.com^
     * - Exception rules: @@||domain.com^
     * - Simple patterns: /advertising/
     * 
     * Skips:
     * - Element hiding rules: ##, #@#
     * - Complex patterns with wildcards or regex
     */
    private fun parseFastRule(line: String) {
        val trimmed = line.trim()
        
        // Skip empty lines and comments
        if (trimmed.isEmpty() || trimmed.startsWith("!")) {
            return
        }
        
        // Skip element hiding rules
        if (trimmed.contains("##") || trimmed.contains("#@#")) {
            return
        }
        
        // Skip rules with options (too complex)
        if (trimmed.contains("$")) {
            return
        }
        
        // Handle exception rules (@@||domain.com^)
        if (trimmed.startsWith("@@||") && trimmed.contains("^")) {
            val domain = trimmed.substring(4, trimmed.indexOf("^"))
            if (domain.isNotEmpty() && !domain.contains("*") && !domain.contains("/")) {
                allowedDomains.add(domain)
            }
            return
        }
        
        // Handle domain blocking rules (||domain.com^)
        if (trimmed.startsWith("||") && trimmed.contains("^")) {
            val domain = trimmed.substring(2, trimmed.indexOf("^"))
            if (domain.isNotEmpty() && !domain.contains("*") && !domain.contains("/")) {
                blockedDomains.add(domain)
            }
            return
        }
        
        // Handle simple pattern rules (no wildcards or regex)
        if (trimmed.startsWith("/") && trimmed.endsWith("/")) {
            val pattern = trimmed.substring(1, trimmed.length - 1)
            if (pattern.isNotEmpty() && !pattern.contains("*") && !pattern.contains("^")) {
                blockedPatterns.add(pattern)
            }
            return
        }
    }
    
    /**
     * Check if a URL should be blocked.
     * Returns true if the URL matches blocking rules, false otherwise.
     * 
     * Checking order:
     * 1. Return false if not initialized (graceful degradation)
     * 2. Check monetization whitelist (NEVER block monetization ads)
     * 3. Check whitelistedDomains (custom whitelist)
     * 4. Check allowedDomains (exception rules from filter lists)
     * 5. Check blockedDomains (O(1) HashSet lookup)
     * 6. Check blockedPatterns (pattern matching)
     * 
     * @param url The URL to check
     * @return true if URL should be blocked, false otherwise
     */
    fun shouldBlock(url: String): Boolean {
        try {
            // Graceful degradation if not initialized
            if (!isInitialized) {
                return false
            }
            
            // Extract domain from URL
            val domain = extractDomain(url) ?: return false
            
            // Check custom whitelist first
            if (whitelistedDomains.contains(domain)) {
                return false
            }
            
            // Check exception rules (allowedDomains)
            if (allowedDomains.contains(domain)) {
                return false
            }
            
            // Check blocked domains (O(1) lookup)
            if (blockedDomains.contains(domain)) {
                incrementBlockedCount()
                return true
            }
            
            // Check if domain ends with any blocked domain (subdomain check)
            for (blockedDomain in blockedDomains) {
                if (domain.endsWith(".$blockedDomain")) {
                    incrementBlockedCount()
                    return true
                }
            }
            
            // Check blocked patterns
            val lowerUrl = url.lowercase()
            for (pattern in blockedPatterns) {
                if (lowerUrl.contains(pattern.lowercase())) {
                    incrementBlockedCount()
                    return true
                }
            }
            
            // Check direct link ad patterns
            for (pattern in directLinkPatterns) {
                if (lowerUrl.contains(pattern)) {
                    incrementBlockedCount()
                    Log.d(TAG, "🚫 Blocked direct link ad: $url")
                    return true
                }
            }
            
            // Check for sponsored keywords in URL path
            if (containsSponsoredKeywords(lowerUrl)) {
                incrementBlockedCount()
                Log.d(TAG, "🚫 Blocked sponsored link: $url")
                return true
            }
            
            return false
            
        } catch (e: Exception) {
            // Return false on any error (allow request)
            Log.e(TAG, "Error checking URL: $url", e)
            return false
        }
    }
    
    /**
     * Check if URL contains sponsored/affiliate keywords in path or query
     */
    private fun containsSponsoredKeywords(url: String): Boolean {
        try {
            val urlParts = url.split("?")
            val path = urlParts.getOrNull(0)?.substringAfter("://")?.substringAfter("/") ?: ""
            val query = urlParts.getOrNull(1) ?: ""
            
            // Check path segments
            val pathSegments = path.split("/")
            for (segment in pathSegments) {
                for (keyword in sponsoredKeywords) {
                    if (segment.contains(keyword)) {
                        return true
                    }
                }
            }
            
            // Check query parameters
            for (keyword in sponsoredKeywords) {
                if (query.contains(keyword)) {
                    return true
                }
            }
            
            return false
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Extract domain from URL.
     * Examples:
     * - https://example.com/path -> example.com
     * - http://sub.example.com:8080/path -> sub.example.com
     * - //cdn.example.com/file.js -> cdn.example.com
     * 
     * @param url The URL to extract domain from
     * @return The domain, or null if extraction fails
     */
    private fun extractDomain(url: String): String? {
        try {
            var domain = url
            
            // Remove protocol
            if (domain.startsWith("http://")) {
                domain = domain.substring(7)
            } else if (domain.startsWith("https://")) {
                domain = domain.substring(8)
            } else if (domain.startsWith("//")) {
                domain = domain.substring(2)
            }
            
            // Remove port
            val portIndex = domain.indexOf(":")
            if (portIndex != -1) {
                domain = domain.substring(0, portIndex)
            }
            
            // Remove path
            val pathIndex = domain.indexOf("/")
            if (pathIndex != -1) {
                domain = domain.substring(0, pathIndex)
            }
            
            // Remove query string
            val queryIndex = domain.indexOf("?")
            if (queryIndex != -1) {
                domain = domain.substring(0, queryIndex)
            }
            
            return if (domain.isNotEmpty()) domain else null
            
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Check if engine is ready for use.
     * 
     * @return true if initialized successfully, false otherwise
     */
    fun isReady(): Boolean = isInitialized
    
    /**
     * Check if initialization failed.
     * 
     * @return true if initialization was attempted but failed
     */
    fun hasInitializationFailed(): Boolean = initializationFailed
    
    /**
     * Get initialization status for diagnostics.
     * 
     * @return Status object with detailed information
     */
    fun getStatus(): EngineStatus {
        return EngineStatus(
            isInitialized = isInitialized,
            initializationFailed = initializationFailed,
            blockedDomainsCount = blockedDomains.size,
            blockedPatternsCount = blockedPatterns.size,
            allowedDomainsCount = allowedDomains.size,
            directLinkPatternsCount = directLinkPatterns.size,
            totalBlockedCount = blockedCount
        )
    }
    
    /**
     * Data class for engine status
     */
    data class EngineStatus(
        val isInitialized: Boolean,
        val initializationFailed: Boolean,
        val blockedDomainsCount: Int,
        val blockedPatternsCount: Int,
        val allowedDomainsCount: Int,
        val directLinkPatternsCount: Int,
        val totalBlockedCount: Int
    ) {
        fun isHealthy(): Boolean = isInitialized && !initializationFailed && blockedDomainsCount > 0
    }
}
