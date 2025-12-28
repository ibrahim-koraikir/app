package com.entertainmentbrowser.util.adblock

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FilterUpdateManager - Automatically updates ad filter lists from the internet
 * 
 * Features:
 * - Downloads latest EasyList, EasyPrivacy, and Fanboy filters
 * - Caches filters locally for offline use
 * - Checks for updates every 7 days
 * - Falls back to bundled filters if download fails
 * - Minimal battery/data usage
 */
@Singleton
class FilterUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FilterUpdateManager"
        
        // Filter list URLs (official sources) - COMPREHENSIVE LIST FOR FUTURE-PROOFING
        private const val EASYLIST_URL = "https://easylist.to/easylist/easylist.txt"
        private const val EASYPRIVACY_URL = "https://easylist.to/easylist/easyprivacy.txt"
        private const val FANBOY_ANNOYANCE_URL = "https://easylist.to/easylist/fanboy-annoyance.txt"
        
        // Additional filter sources for better coverage
        private const val PETER_LOWE_URL = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=0"
        private const val ADGUARD_BASE_URL = "https://filters.adtidy.org/extension/chromium/filters/2.txt"
        private const val ADGUARD_MOBILE_URL = "https://filters.adtidy.org/extension/chromium/filters/11.txt"
        private const val ADGUARD_TRACKING_URL = "https://filters.adtidy.org/extension/chromium/filters/3.txt"
        private const val ADGUARD_ANNOYANCES_URL = "https://filters.adtidy.org/extension/chromium/filters/14.txt"
        
        // Popup/Redirect specific filters
        private const val FANBOY_SOCIAL_URL = "https://easylist.to/easylist/fanboy-social.txt"
        private const val UBLOCK_FILTERS_URL = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt"
        private const val UBLOCK_BADWARE_URL = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/badware.txt"
        private const val UBLOCK_PRIVACY_URL = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt"
        private const val UBLOCK_ANNOYANCES_URL = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/annoyances.txt"
        
        // HaGeZi's DNS Blocklists - EXCELLENT for popup/redirect ads
        private const val HAGEZI_MULTI_URL = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/multi.txt"
        private const val HAGEZI_PRO_URL = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/pro.txt"
        private const val HAGEZI_POPUP_URL = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/popupads.txt"
        private const val HAGEZI_FAKE_URL = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/adblock/fake.txt"
        
        // 1Hosts - Comprehensive ad blocking
        private const val ONEHOSTS_PRO_URL = "https://raw.githubusercontent.com/badmojr/1Hosts/master/Pro/adblock.txt"
        
        // OISD - One of the best comprehensive blocklists
        private const val OISD_BIG_URL = "https://big.oisd.nl/"
        
        // Update interval (3 days for better protection)
        private const val UPDATE_INTERVAL_MS = 3 * 24 * 60 * 60 * 1000L
        
        // Cache directory
        private const val CACHE_DIR = "adblock_filters"
        
        // Remote domain blocklist (can be updated without app update)
        private const val REMOTE_DOMAINS_URL = "https://raw.githubusercontent.com/nickspaargaren/no-google/master/pihole-google-adservices.txt"
    }
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Check if filters need updating and update if necessary
     * Call this from Application.onCreate() or WorkManager
     */
    suspend fun checkAndUpdateFilters(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (shouldUpdate()) {
                Log.d(TAG, "🔄 Checking for filter updates...")
                updateAllFilters()
            } else {
                Log.d(TAG, "✅ Filters are up to date")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update filters", e)
            false
        }
    }
    
    /**
     * Check if filters should be updated
     */
    private fun shouldUpdate(): Boolean {
        val prefs = context.getSharedPreferences("adblock_prefs", Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong("last_update", 0)
        val now = System.currentTimeMillis()
        
        // Update if never updated or older than 7 days
        return (now - lastUpdate) > UPDATE_INTERVAL_MS
    }
    
    /**
     * Update all filter lists - COMPREHENSIVE for future-proofing
     */
    private suspend fun updateAllFilters(): Boolean = withContext(Dispatchers.IO) {
        try {
            var successCount = 0
            var totalFilters = 0
            
            // Core filters (must-have)
            val coreFilters = listOf(
                "easylist.txt" to EASYLIST_URL,
                "easyprivacy.txt" to EASYPRIVACY_URL,
                "fanboy-annoyance.txt" to FANBOY_ANNOYANCE_URL
            )
            
            // Extended filters (nice-to-have for better coverage)
            val extendedFilters = listOf(
                "peter-lowe.txt" to PETER_LOWE_URL,
                "adguard-base.txt" to ADGUARD_BASE_URL,
                "adguard-mobile.txt" to ADGUARD_MOBILE_URL,
                "adguard-tracking.txt" to ADGUARD_TRACKING_URL,
                "ublock-filters.txt" to UBLOCK_FILTERS_URL,
                "ublock-badware.txt" to UBLOCK_BADWARE_URL,
                "ublock-privacy.txt" to UBLOCK_PRIVACY_URL,
                // HaGeZi's DNS Blocklists - BEST for popup/redirect/direct link ads
                "hagezi-multi.txt" to HAGEZI_MULTI_URL,
                "hagezi-pro.txt" to HAGEZI_PRO_URL,
                "hagezi-popup.txt" to HAGEZI_POPUP_URL,  // Specifically for popup ads!
                "hagezi-fake.txt" to HAGEZI_FAKE_URL,
                // 1Hosts Pro
                "1hosts-pro.txt" to ONEHOSTS_PRO_URL
            )
            
            // Download core filters (required)
            for ((filename, url) in coreFilters) {
                totalFilters++
                if (downloadFilter(filename, url)) {
                    successCount++
                }
            }
            
            // Download extended filters (optional - don't fail if these fail)
            for ((filename, url) in extendedFilters) {
                totalFilters++
                try {
                    if (downloadFilter(filename, url)) {
                        successCount++
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Optional filter $filename failed, continuing...")
                }
            }
            
            // Download remote domain blocklist for future updates
            downloadRemoteDomains()
            
            // Consider success if at least core filters downloaded
            val coreSuccess = successCount >= coreFilters.size
            
            if (coreSuccess) {
                val prefs = context.getSharedPreferences("adblock_prefs", Context.MODE_PRIVATE)
                prefs.edit().putLong("last_update", System.currentTimeMillis()).apply()
                Log.d(TAG, "✅ Filters updated: $successCount/$totalFilters successful")
            }
            
            coreSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update filters", e)
            false
        }
    }
    
    /**
     * Download remote domain blocklist - allows blocking new domains without app update
     */
    private suspend fun downloadRemoteDomains(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📥 Downloading remote domain blocklist...")
            
            val request = Request.Builder()
                .url(REMOTE_DOMAINS_URL)
                .addHeader("User-Agent", "Mozilla/5.0 (Android) AdBlocker/1.0")
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val content = response.body?.string()
                if (content != null && content.isNotEmpty()) {
                    val file = File(cacheDir, "remote-domains.txt")
                    file.writeText(content)
                    Log.d(TAG, "✅ Remote domains updated")
                    return@withContext true
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Remote domains update failed (non-critical)", e)
            false
        }
    }
    
    /**
     * Get remote blocked domains (for runtime updates)
     * Includes HaGeZi popup domains for direct link ad blocking
     */
    fun getRemoteDomains(): Set<String> {
        val domains = mutableSetOf<String>()
        
        // Load from remote-domains.txt
        val remoteFile = File(cacheDir, "remote-domains.txt")
        if (remoteFile.exists()) {
            try {
                remoteFile.readLines()
                    .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("!") }
                    .forEach { line ->
                        // Parse hosts format (0.0.0.0 domain.com) or plain domain
                        val domain = if (line.contains(" ")) {
                            line.split(" ").lastOrNull()?.trim()?.lowercase()
                        } else {
                            line.trim().lowercase()
                        }
                        if (!domain.isNullOrEmpty() && domain.contains(".")) {
                            domains.add(domain)
                        }
                    }
            } catch (e: Exception) { }
        }
        
        // Load HaGeZi popup domains (specifically for direct link ads)
        val haGeZiFile = File(cacheDir, "hagezi-popup.txt")
        if (haGeZiFile.exists()) {
            try {
                haGeZiFile.readLines()
                    .filter { it.isNotBlank() && !it.startsWith("!") && !it.startsWith("[") }
                    .forEach { line ->
                        // Parse AdBlock format: ||domain.com^
                        if (line.startsWith("||") && line.contains("^")) {
                            val domain = line.removePrefix("||").substringBefore("^").trim().lowercase()
                            if (domain.isNotEmpty() && domain.contains(".") && !domain.contains("*")) {
                                domains.add(domain)
                            }
                        }
                    }
            } catch (e: Exception) { }
        }
        
        // Load HaGeZi pro domains
        val haGeZiProFile = File(cacheDir, "hagezi-pro.txt")
        if (haGeZiProFile.exists()) {
            try {
                haGeZiProFile.readLines()
                    .filter { it.isNotBlank() && !it.startsWith("!") && !it.startsWith("[") }
                    .take(5000) // Limit for performance
                    .forEach { line ->
                        if (line.startsWith("||") && line.contains("^")) {
                            val domain = line.removePrefix("||").substringBefore("^").substringBefore("$").trim().lowercase()
                            if (domain.isNotEmpty() && domain.contains(".") && !domain.contains("*")) {
                                domains.add(domain)
                            }
                        }
                    }
            } catch (e: Exception) { }
        }
        
        Log.d(TAG, "📋 Loaded ${domains.size} remote domains for blocking")
        return domains
    }
    
    /**
     * Get HaGeZi popup ad domains specifically
     */
    fun getPopupAdDomains(): Set<String> {
        val domains = mutableSetOf<String>()
        val haGeZiFile = File(cacheDir, "hagezi-popup.txt")
        
        if (haGeZiFile.exists()) {
            try {
                haGeZiFile.readLines()
                    .filter { it.isNotBlank() && !it.startsWith("!") && !it.startsWith("[") }
                    .forEach { line ->
                        if (line.startsWith("||") && line.contains("^")) {
                            val domain = line.removePrefix("||").substringBefore("^").trim().lowercase()
                            if (domain.isNotEmpty() && domain.contains(".") && !domain.contains("*")) {
                                domains.add(domain)
                            }
                        }
                    }
                Log.d(TAG, "📋 Loaded ${domains.size} HaGeZi popup domains")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load HaGeZi popup domains", e)
            }
        }
        
        return domains
    }
    
    /**
     * Original getRemoteDomains implementation for backward compatibility
     */
    private fun getRemoteDomainsLegacy(): Set<String> {
        val file = File(cacheDir, "remote-domains.txt")
        if (!file.exists()) return emptySet()
        
        return try {
            file.readLines()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    /**
     * Download a single filter list
     */
    private suspend fun downloadFilter(filename: String, url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📥 Downloading $filename...")
            
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Android) AdBlocker/1.0")
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val content = response.body?.string()
                if (content != null && content.isNotEmpty()) {
                    // Save to cache
                    val file = File(cacheDir, filename)
                    file.writeText(content)
                    
                    Log.d(TAG, "✅ Downloaded $filename (${content.length} bytes)")
                    return@withContext true
                }
            }
            
            Log.w(TAG, "⚠️ Failed to download $filename: ${response.code}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download $filename", e)
            false
        }
    }
    
    /**
     * Get cached filter file or fall back to bundled asset
     */
    fun getFilterFile(filename: String): File? {
        val cachedFile = File(cacheDir, filename)
        
        // Use cached file if it exists and is recent
        if (cachedFile.exists() && cachedFile.length() > 0) {
            val age = System.currentTimeMillis() - cachedFile.lastModified()
            if (age < UPDATE_INTERVAL_MS * 2) { // Allow 14 days before forcing re-download
                return cachedFile
            }
        }
        
        // Fall back to bundled asset (return null to signal using assets)
        return null
    }
    
    /**
     * Force update filters (for manual refresh)
     */
    suspend fun forceUpdate(): Boolean = withContext(Dispatchers.IO) {
        // Clear last update timestamp to force update
        val prefs = context.getSharedPreferences("adblock_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_update", 0).apply()
        
        checkAndUpdateFilters()
    }
    
    /**
     * Get filter statistics
     */
    fun getFilterStats(): FilterStats {
        val easylist = File(cacheDir, "easylist.txt")
        val easyprivacy = File(cacheDir, "easyprivacy.txt")
        val fanboy = File(cacheDir, "fanboy-annoyance.txt")
        
        val prefs = context.getSharedPreferences("adblock_prefs", Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong("last_update", 0)
        
        return FilterStats(
            easylistSize = if (easylist.exists()) easylist.length() else 0,
            easyprivacySize = if (easyprivacy.exists()) easyprivacy.length() else 0,
            fanboySize = if (fanboy.exists()) fanboy.length() else 0,
            lastUpdate = lastUpdate,
            isUpToDate = !shouldUpdate()
        )
    }
    
    data class FilterStats(
        val easylistSize: Long,
        val easyprivacySize: Long,
        val fanboySize: Long,
        val lastUpdate: Long,
        val isUpToDate: Boolean
    )
}
