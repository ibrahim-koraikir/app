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
        
        // Filter list URLs (official sources)
        private const val EASYLIST_URL = "https://easylist.to/easylist/easylist.txt"
        private const val EASYPRIVACY_URL = "https://easylist.to/easylist/easyprivacy.txt"
        private const val FANBOY_ANNOYANCE_URL = "https://easylist.to/easylist/fanboy-annoyance.txt"
        
        // Update interval (7 days)
        private const val UPDATE_INTERVAL_MS = 7 * 24 * 60 * 60 * 1000L
        
        // Cache directory
        private const val CACHE_DIR = "adblock_filters"
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
     * Update all filter lists
     */
    private suspend fun updateAllFilters(): Boolean = withContext(Dispatchers.IO) {
        try {
            var success = true
            
            // Download each filter list
            success = downloadFilter("easylist.txt", EASYLIST_URL) && success
            success = downloadFilter("easyprivacy.txt", EASYPRIVACY_URL) && success
            success = downloadFilter("fanboy-annoyance.txt", FANBOY_ANNOYANCE_URL) && success
            
            if (success) {
                // Save last update timestamp
                val prefs = context.getSharedPreferences("adblock_prefs", Context.MODE_PRIVATE)
                prefs.edit().putLong("last_update", System.currentTimeMillis()).apply()
                
                Log.d(TAG, "✅ All filters updated successfully")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update filters", e)
            false
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
