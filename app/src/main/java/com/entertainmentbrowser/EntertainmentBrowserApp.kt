package com.entertainmentbrowser

import android.app.Application
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.entertainmentbrowser.data.worker.TabCleanupWorker
import com.entertainmentbrowser.domain.repository.WebsiteRepository
import com.entertainmentbrowser.util.CacheManager
import com.entertainmentbrowser.util.GpuMemoryManager
import com.entertainmentbrowser.util.WebViewPool
import com.entertainmentbrowser.util.adblock.FastAdBlockEngine
import com.entertainmentbrowser.data.remote.RemoteAdConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class EntertainmentBrowserApp : Application(), ImageLoaderFactory {
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(com.entertainmentbrowser.di.Base64Keyer())
                add(com.entertainmentbrowser.di.Base64Fetcher.Factory())
            }
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .crossfade(150)
            .respectCacheHeaders(false)
            .build()
    }
    
    @Inject
    lateinit var websiteRepository: WebsiteRepository
    
    @Inject
    lateinit var cacheManager: CacheManager
    
    // Application scope with SupervisorJob and Default dispatcher for background work
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    companion object {
        private const val TAG = "EntertainmentBrowserApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Enable StrictMode in debug builds for performance monitoring (Requirements 17.1, 17.2)
        if (BuildConfig.DEBUG) {
            enableDebugStrictMode()
        }
        
        // Critical setup on main thread - logging only
        setupLogging()
        
        // Move all non-critical initialization to background threads
        if (BuildConfig.DEBUG) {
            // Wrap initialization with performance logging in debug builds (Requirement 17.5)
            logDebugPerformance("App Initialization") {
                initializeInBackground()
            }
        } else {
            initializeInBackground()
        }
    }
    
    /**
     * Enable StrictMode via reflection to avoid compile-time dependency on debug source set
     */
    private fun enableDebugStrictMode() {
        try {
            val profilerClass = Class.forName("com.entertainmentbrowser.debug.ProfilerConfig")
            val method = profilerClass.getMethod("enableStrictMode")
            method.invoke(null)
        } catch (e: Exception) {
            // ProfilerConfig not available in release builds
        }
    }
    
    /**
     * Log performance via reflection to avoid compile-time dependency on debug source set
     */
    private fun logDebugPerformance(tag: String, block: () -> Unit) {
        try {
            val profilerClass = Class.forName("com.entertainmentbrowser.debug.ProfilerConfig")
            val companion = profilerClass.getDeclaredField("Companion").get(null)
            val method = companion.javaClass.getMethod("logPerformance", String::class.java, Function0::class.java)
            method.invoke(companion, tag, block)
        } catch (e: Exception) {
            // ProfilerConfig not available, just run the block
            block()
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Cancel application scope to clean up coroutines
        applicationScope.coroutineContext.cancel()
        // Clean up WebView state manager
        webViewStateManager.clearAll()
        // Clear WebView pool to destroy any pooled WebView instances
        WebViewPool.clear()
    }
    
    private fun setupLogging() {
        // Critical logging setup happens here
        Log.d(TAG, "🚀 App starting...")
    }
    
    private fun initializeInBackground() {
        // Initialize GPU memory manager immediately (critical for preventing crashes)
        GpuMemoryManager.initialize(this)
        
        // Launch all non-critical initialization tasks in parallel
        
        // Ad blocker initialization on IO dispatcher
        applicationScope.launch(Dispatchers.IO) {
            initializeAdBlocking()
        }
        
        // Database prepopulation on IO dispatcher
        applicationScope.launch(Dispatchers.IO) {
            prepopulateDatabase()
        }
        
        // Tab cleanup scheduling on Default dispatcher
        applicationScope.launch(Dispatchers.Default) {
            scheduleTabCleanup()
            scheduleFilterUpdates()
        }
        
        // WebView preloading on Main dispatcher after delay (Requirement 1.5)
        applicationScope.launch(Dispatchers.Main) {
            kotlinx.coroutines.delay(1000) // Wait 1 second after app start
            preloadWebView()
        }
        
        // Cache cleanup on IO dispatcher (Requirement 5.4)
        applicationScope.launch(Dispatchers.IO) {
            clearOldCache()
        }
        
        // Initialize remote ad config on IO dispatcher
        applicationScope.launch(Dispatchers.IO) {
            initializeRemoteAdConfig()
        }
    }
    
    private suspend fun initializeRemoteAdConfig() {
        try {
            Log.d(TAG, "📢 Initializing remote ad config...")
            RemoteAdConfig.refreshConfig(this)
            Log.d(TAG, "✅ Remote ad config initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize remote ad config", e)
        }
    }
    
    @Inject
    lateinit var fastAdBlockEngine: FastAdBlockEngine
    
    @Inject
    lateinit var advancedAdBlockEngine: com.entertainmentbrowser.util.adblock.AdvancedAdBlockEngine
    
    @Inject
    lateinit var webViewStateManager: com.entertainmentbrowser.util.WebViewStateManager
    
    @Inject
    lateinit var antiAdblockBypass: com.entertainmentbrowser.util.adblock.AntiAdblockBypass
    
    private suspend fun initializeAdBlocking() {
        try {
            Log.d(TAG, "🚀 Preloading ad-blockers...")
            val startTime = System.currentTimeMillis()
            
            // Initialize engines in parallel using coroutines
            kotlinx.coroutines.coroutineScope {
                // Fast engine (HashSet-based, O(1) lookups)
                launch(Dispatchers.IO) {
                    fastAdBlockEngine.preloadFromAssets()
                }
                // Advanced engine (Bloom filter + Trie, full filter support)
                launch(Dispatchers.IO) {
                    advancedAdBlockEngine.preloadFromAssets()
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            // Verify initialization status
            val fastStatus = fastAdBlockEngine.getStatus()
            val advancedStatus = advancedAdBlockEngine.getStatus()
            
            // Calculate total blocked domains for status
            val totalBlockedDomains = fastStatus.blockedDomainsCount + advancedStatus.blockedDomainsCount
            
            // Get rule stats from AdvancedAdBlockEngine (now with no limits)
            val engineRuleStats = advancedAdBlockEngine.getRuleStats()
            val truncationPercentage = engineRuleStats.getTruncationPercentage()
            
            // Convert to AdBlockStatus.RuleStats for UI observation
            val statusRuleStats = com.entertainmentbrowser.util.adblock.AdBlockStatus.RuleStats(
                blockedDomains = engineRuleStats.blockedDomains,
                blockedPaths = engineRuleStats.blockedPaths,
                wildcardPatternsLoaded = engineRuleStats.wildcardPatternsLoaded,
                wildcardPatternsDropped = engineRuleStats.wildcardPatternsDropped,
                wildcardPatternsLimit = engineRuleStats.wildcardPatternsLimit,
                regexPatternsLoaded = engineRuleStats.regexPatternsLoaded,
                regexPatternsDropped = engineRuleStats.regexPatternsDropped,
                regexPatternsLimit = engineRuleStats.regexPatternsLimit,
                totalRulesLoaded = engineRuleStats.totalRulesLoaded,
                totalRulesDropped = engineRuleStats.totalRulesDropped
            )
            
            if (fastStatus.isHealthy() && advancedStatus.isHealthy()) {
                Log.d(TAG, "✅ Ad-blockers ready in ${duration}ms (FULL filter support - no limits)")
                Log.d(TAG, "   FastEngine: ${fastStatus.blockedDomainsCount} domains, ${fastStatus.blockedPatternsCount} patterns")
                Log.d(TAG, "   AdvancedEngine: ${advancedStatus.blockedDomainsCount} domains, ${advancedStatus.wildcardPatternsCount} wildcards, ${advancedStatus.regexPatternsCount} regex")
                
                // Update shared status for UI observation with detailed rule stats
                com.entertainmentbrowser.util.adblock.AdBlockStatus.updateStatus(
                    isInitialized = true,
                    fastEngineHealthy = true,
                    advancedEngineHealthy = true,
                    rustEngineHealthy = false, // No Rust engine
                    isTruncated = false, // No limits = no truncation
                    truncationPercentage = 0f,
                    blockedDomainsCount = totalBlockedDomains,
                    ruleStats = statusRuleStats
                )
            } else {
                Log.w(TAG, "⚠️ Ad-blocker initialization incomplete:")
                
                val errorMessages = mutableListOf<String>()
                if (!fastStatus.isHealthy()) {
                    Log.w(TAG, "   FastEngine: initialized=${fastStatus.isInitialized}, failed=${fastStatus.initializationFailed}, rules=${fastStatus.blockedDomainsCount}")
                    errorMessages.add("FastEngine failed")
                }
                if (!advancedStatus.isHealthy()) {
                    Log.w(TAG, "   AdvancedEngine: initialized=${advancedStatus.isInitialized}, failed=${advancedStatus.initializationFailed}, rules=${advancedStatus.blockedDomainsCount}")
                    errorMessages.add("AdvancedEngine failed")
                }
                
                // Update shared status with degraded state and detailed rule stats
                com.entertainmentbrowser.util.adblock.AdBlockStatus.updateStatus(
                    isInitialized = true,
                    fastEngineHealthy = fastStatus.isHealthy(),
                    advancedEngineHealthy = advancedStatus.isHealthy(),
                    rustEngineHealthy = false,
                    isTruncated = false,
                    truncationPercentage = 0f,
                    blockedDomainsCount = totalBlockedDomains,
                    errorMessage = errorMessages.joinToString(", "),
                    ruleStats = statusRuleStats
                )
                
                // If all engines failed, log critical error
                if (fastStatus.initializationFailed && advancedStatus.initializationFailed) {
                    Log.e(TAG, "❌ All ad-blocking engines failed to initialize - ad blocking will be degraded")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start ad-blocker", e)
            // Update status with failure
            com.entertainmentbrowser.util.adblock.AdBlockStatus.setInitializationFailed(
                e.message ?: "Unknown error during initialization"
            )
            // Continue without ad-blocking (graceful degradation)
        }
    }
    

    private suspend fun prepopulateDatabase() {
        try {
            Log.d(TAG, "📊 Prepopulating database...")
            websiteRepository.prepopulateWebsites()
            Log.d(TAG, "✅ Database ready")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to prepopulate database", e)
        }
    }
    
    private fun scheduleTabCleanup() {
        try {
            Log.d(TAG, "⏰ Scheduling tab cleanup...")
            
            // Build constraints for efficient background work (Requirements 16.1, 16.2)
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)  // Requirement 16.2
                .setRequiresDeviceIdle(true)     // Requirement 16.1
                .build()
            
            // Create periodic work request with 7-day interval (Requirements 16.3, 16.4)
            val cleanupRequest = PeriodicWorkRequestBuilder<TabCleanupWorker>(
                repeatInterval = 7,  // Requirement 16.4: 7 day interval
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(  // Requirement 16.5: exponential backoff
                    BackoffPolicy.EXPONENTIAL,
                    10000L,  // 10 seconds minimum backoff
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                TabCleanupWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
            Log.d(TAG, "✅ Tab cleanup scheduled (7-day interval, battery-aware)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to schedule tab cleanup", e)
        }
    }
    
    private fun scheduleFilterUpdates() {
        try {
            Log.d(TAG, "⏰ Scheduling filter updates...")
            
            // Build constraints for filter updates (WiFi + battery)
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(androidx.work.NetworkType.UNMETERED) // WiFi only
                .build()
            
            // Create periodic work request with 7-day interval
            val updateRequest = PeriodicWorkRequestBuilder<com.entertainmentbrowser.data.worker.FilterUpdateWorker>(
                repeatInterval = 7,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10000L,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                com.entertainmentbrowser.data.worker.FilterUpdateWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                updateRequest
            )
            Log.d(TAG, "✅ Filter updates scheduled (7-day interval, WiFi only)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to schedule filter updates", e)
        }
    }
    
    private fun preloadWebView() {
        try {
            Log.d(TAG, "🌐 Preloading WebView...")
            // Warm up WebView by obtaining an instance from the pool (Requirement 1.5, 4.2)
            WebViewPool.obtain(this)
            Log.d(TAG, "✅ WebView preloaded and warmed up")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to preload WebView", e)
        }
    }
    
    private suspend fun clearOldCache() {
        try {
            Log.d(TAG, "🧹 Clearing old cache files...")
            cacheManager.clearOldCache() // Default: 7 days
            Log.d(TAG, "✅ Old cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to clear old cache", e)
        }
    }
}
