package com.entertainmentbrowser.data.remote

import android.content.Context
import android.util.Log
import com.entertainmentbrowser.core.constants.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Remote Ad Configuration Manager
 * Fetches ad network URLs from GitHub so you can update them without rebuilding the app
 * 
 * GitHub JSON format:
 * {
 *   "networks": [
 *     {"name": "Adsterra", "url": "https://..."},
 *     {"name": "Ad-Maven", "url": "https://..."}
 *   ]
 * }
 */
object RemoteAdConfig {
    private const val TAG = "RemoteAdConfig"
    
    // GitHub raw URL for remote ad config
    private const val REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/ibrahim-koraikir/AhmedHytworker-AdsConfig/main/ad_networks.json"
    
    // SharedPreferences for caching - always uses MODE_PRIVATE for secure app-only storage
    private const val PREFS_NAME = "remote_ad_config"
    private const val KEY_CACHED_CONFIG = "cached_ad_networks"
    private const val KEY_LAST_FETCH = "last_fetch_time"
    
    // Fetch interval: 1 hour (in milliseconds)
    private const val FETCH_INTERVAL_MS = 60 * 60 * 1000L
    
    @Volatile
    private var cachedNetworks: List<Constants.AdNetwork>? = null
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    @Serializable
    data class RemoteAdNetwork(
        val name: String,
        val url: String,
        val useAppKey: Boolean = false // If true, URL is a template that needs app-side key injection
    )
    
    @Serializable
    data class RemoteConfig(
        val networks: List<RemoteAdNetwork>
    )
    
    /**
     * Processes a remote ad network URL, injecting the app-side Adsterra key if needed.
     * Remote config can use "{ADSTERRA_KEY}" placeholder or set useAppKey=true for Adsterra entries.
     */
    private fun processAdNetworkUrl(network: RemoteAdNetwork): String {
        var url = network.url
        
        // Replace {ADSTERRA_KEY} placeholder with actual key from BuildConfig
        if (url.contains("{ADSTERRA_KEY}")) {
            url = url.replace("{ADSTERRA_KEY}", com.entertainmentbrowser.BuildConfig.ADSTERRA_KEY)
        }
        
        // If useAppKey is true and this is an Adsterra URL, append the key
        if (network.useAppKey && network.name.equals("Adsterra", ignoreCase = true)) {
            val key = com.entertainmentbrowser.BuildConfig.ADSTERRA_KEY
            if (key.isNotEmpty() && !url.contains("key=")) {
                url = if (url.contains("?")) "$url&key=$key" else "$url?key=$key"
            }
        }
        
        return url
    }
    
    /**
     * Get ad networks - tries remote first, falls back to local cache, then hardcoded
     */
    suspend fun getAdNetworks(context: Context): List<Constants.AdNetwork> {
        // Return cached if available
        cachedNetworks?.let { return it }
        
        // Try to load from local cache first (fast)
        loadFromCache(context)?.let { cached ->
            cachedNetworks = cached
            Log.d(TAG, "📦 Loaded ${cached.size} networks from cache")
        }
        
        // Check if we should fetch fresh config
        if (shouldFetchRemote(context)) {
            fetchRemoteConfig(context)?.let { remote ->
                cachedNetworks = remote
                saveToCache(context, remote)
                Log.d(TAG, "🌐 Fetched ${remote.size} networks from remote")
                return remote
            }
        }
        
        // Return cached or fallback to hardcoded
        return cachedNetworks ?: Constants.AD_NETWORKS.also {
            Log.d(TAG, "⚠️ Using hardcoded ${it.size} networks as fallback")
        }
    }
    
    /**
     * Force refresh from remote (call this on app start or pull-to-refresh)
     */
    suspend fun refreshConfig(context: Context): Boolean {
        return fetchRemoteConfig(context)?.let { remote ->
            cachedNetworks = remote
            saveToCache(context, remote)
            Log.d(TAG, "🔄 Refreshed config: ${remote.size} networks")
            true
        } ?: false
    }
    
    private fun shouldFetchRemote(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastFetch = prefs.getLong(KEY_LAST_FETCH, 0)
        val now = System.currentTimeMillis()
        return (now - lastFetch) > FETCH_INTERVAL_MS
    }
    
    private suspend fun fetchRemoteConfig(context: Context): List<Constants.AdNetwork>? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🌐 Fetching remote config from: $REMOTE_CONFIG_URL")
                
                val request = Request.Builder()
                    .url(REMOTE_CONFIG_URL)
                    .header("Cache-Control", "no-cache")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "❌ Remote fetch failed: ${response.code}")
                        return@withContext null
                    }
                    
                    val body = response.body?.string() ?: return@withContext null
                    Log.d(TAG, "📥 Received config: $body")
                    
                    val config = json.decodeFromString<RemoteConfig>(body)
                    val networks = config.networks.map { 
                        Constants.AdNetwork(it.name, processAdNetworkUrl(it)) 
                    }
                    
                    // Update last fetch time
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
                        .apply()
                    
                    networks
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to fetch remote config", e)
                null
            }
        }
    }
    
    private fun loadFromCache(context: Context): List<Constants.AdNetwork>? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cached = prefs.getString(KEY_CACHED_CONFIG, null) ?: return null
            
            val config = json.decodeFromString<RemoteConfig>(cached)
            config.networks.map { Constants.AdNetwork(it.name, processAdNetworkUrl(it)) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load cache", e)
            null
        }
    }
    
    private fun saveToCache(context: Context, networks: List<Constants.AdNetwork>) {
        try {
            val config = RemoteConfig(
                networks = networks.map { RemoteAdNetwork(it.name, it.url) }
            )
            val jsonStr = json.encodeToString(RemoteConfig.serializer(), config)
            
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHED_CONFIG, jsonStr)
                .apply()
                
            Log.d(TAG, "💾 Saved ${networks.size} networks to cache")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save cache", e)
        }
    }
}
