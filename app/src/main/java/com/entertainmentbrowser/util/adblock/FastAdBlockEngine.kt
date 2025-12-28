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
    
    // Pre-lowercased patterns for fast matching (avoid lowercase() in hot path)
    private val blockedPatternsLower = HashSet<String>()
    private val directLinkPatternsLower = HashSet<String>()
    
    // Heuristic keywords from centralized registry
    // Only scan patterns if URL contains these hints (reduces unnecessary iterations)
    private val adHintKeywords = AdBlockDomainRegistry.adHintKeywords
    private val sponsoredKeywords = AdBlockDomainRegistry.sponsoredKeywords
    
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
     * Preload filter lists from assets.
     * This is a suspend function that should be called from a coroutine scope
     * (typically from Application.onCreate() via applicationScope.launch(Dispatchers.IO)).
     * 
     * Uses structured concurrency instead of raw threads for proper lifecycle management.
     */
    suspend fun preloadFromAssets() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized, skipping")
            return
        }
        
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
            Log.d(TAG, "   Blocked patterns: ${blockedPatterns.size} (pre-lowercased: ${blockedPatternsLower.size})")
            Log.d(TAG, "   Allowed domains: ${allowedDomains.size}")
            Log.d(TAG, "   Direct link patterns: ${directLinkPatterns.size} (pre-lowercased: ${directLinkPatternsLower.size})")
            Log.d(TAG, "   Ad hint keywords: ${adHintKeywords.size}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load filter lists", e)
            isInitialized = false
            initializationFailed = true
        }
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
     * Pre-lowercases patterns for fast matching in hot path.
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
        // Pre-lowercase for fast matching (patterns are already lowercase, but ensure consistency)
        directLinkPatternsLower.addAll(patterns.map { it.lowercase() })
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
                // Pre-lowercase for fast matching in hot path
                blockedPatternsLower.add(pattern.lowercase())
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
    // Essential Google domains from centralized registry - should NEVER be blocked
    private val googleEssentialDomains = AdBlockDomainRegistry.googleEssentialDomains
    
    private fun isGoogleEssential(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return googleEssentialDomains.any { lowerUrl.contains(it) }
    }
    
    fun shouldBlock(url: String): Boolean {
        try {
            // Graceful degradation if not initialized
            if (!isInitialized) {
                return false
            }
            
            // WHITELIST CHECK: Never block essential Google domains
            if (isGoogleEssential(url)) {
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
            
            // Check blocked domains with efficient subdomain matching
            // Instead of O(n) linear scan, we progressively strip subdomains and check HashSet
            // e.g., for "a.b.ads.example.com" we check: full domain, then "b.ads.example.com", 
            // then "ads.example.com", then "example.com" - all O(1) HashSet lookups
            if (isBlockedDomainOrParent(domain)) {
                incrementBlockedCount()
                return true
            }
            
            // Pre-lowercase URL once for all pattern checks
            val lowerUrl = url.lowercase()
            
            // OPTIMIZATION: Only scan patterns if URL contains ad-related hints
            // This avoids expensive O(n) pattern iteration for most legitimate URLs
            val looksLikeAd = containsAdHints(lowerUrl)
            
            if (looksLikeAd) {
                // Check blocked patterns using pre-lowercased set (no lowercase() in loop)
                for (pattern in blockedPatternsLower) {
                    if (lowerUrl.contains(pattern)) {
                        incrementBlockedCount()
                        return true
                    }
                }
                
                // Check direct link ad patterns using pre-lowercased set
                for (pattern in directLinkPatternsLower) {
                    if (lowerUrl.contains(pattern)) {
                        incrementBlockedCount()
                        return true
                    }
                }
                
                // Check for sponsored keywords in URL path
                if (containsSponsoredKeywords(lowerUrl)) {
                    incrementBlockedCount()
                    return true
                }
            }
            
            return false
            
        } catch (e: Exception) {
            // Return false on any error (allow request)
            Log.e(TAG, "Error checking URL: $url", e)
            return false
        }
    }
    
    /**
     * Fast heuristic check to determine if URL might be ad-related.
     * Only URLs containing these hints will undergo expensive pattern scanning.
     * This significantly reduces CPU usage for legitimate content URLs.
     * 
     * @param lowerUrl Pre-lowercased URL
     * @return true if URL contains ad-related hints
     */
    private fun containsAdHints(lowerUrl: String): Boolean {
        for (hint in adHintKeywords) {
            if (lowerUrl.contains(hint)) {
                return true
            }
        }
        return false
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
     * Efficiently check if domain or any of its parent domains are blocked.
     * Uses O(k) HashSet lookups where k = number of subdomain levels (typically 2-4),
     * instead of O(n) linear scan through all blocked domains.
     * 
     * For "tracker.ads.example.com", checks:
     * 1. "tracker.ads.example.com" (exact match)
     * 2. "ads.example.com" (parent)
     * 3. "example.com" (parent)
     * 
     * @param domain The full domain to check
     * @return true if domain or any parent is blocked
     */
    private fun isBlockedDomainOrParent(domain: String): Boolean {
        // Check exact domain match first
        if (blockedDomains.contains(domain)) {
            return true
        }
        
        // Progressively strip subdomains and check each parent
        // Limit iterations to prevent excessive checks on malformed domains
        var currentDomain = domain
        var iterations = 0
        val maxIterations = 10 // Safety limit for deeply nested subdomains
        
        while (iterations < maxIterations) {
            val dotIndex = currentDomain.indexOf('.')
            if (dotIndex == -1 || dotIndex == currentDomain.length - 1) {
                // No more subdomains to strip
                break
            }
            
            // Strip the leftmost subdomain: "a.b.c.com" -> "b.c.com"
            currentDomain = currentDomain.substring(dotIndex + 1)
            
            // Check if this parent domain is blocked
            if (blockedDomains.contains(currentDomain)) {
                return true
            }
            
            iterations++
        }
        
        return false
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
