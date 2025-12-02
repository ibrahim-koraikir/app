package com.entertainmentbrowser

import android.app.Application
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.entertainmentbrowser.data.worker.TabCleanupWorker
import com.entertainmentbrowser.domain.repository.WebsiteRepository
import com.entertainmentbrowser.util.CacheManager
import com.entertainmentbrowser.util.GpuMemoryManager
import com.entertainmentbrowser.util.WebViewPool
import com.entertainmentbrowser.util.adblock.FastAdBlockEngine
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class EntertainmentBrowserApp : Application() {
    
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
            enableDebugProfiler()
        }
        
        // Critical setup on main thread - logging only
        setupLogging()
        
        // Move all non-critical initialization to background threads
        if (BuildConfig.DEBUG) {
            // Wrap initialization with performance logging in debug builds (Requirement 17.5)
            logInitializationPerformance()
        } else {
            initializeInBackground()
        }
    }
    
    /**
     * Enables debug profiler with StrictMode.
     * Only available in debug builds via reflection to avoid compile-time dependency.
     */
    private fun enableDebugProfiler() {
        try {
            val profilerClass = Class.forName("com.entertainmentbrowser.debug.ProfilerConfig")
            val enableStrictModeMethod = profilerClass.getMethod("enableStrictMode")
            enableStrictModeMethod.invoke(profilerClass.getField("INSTANCE").get(null))
        } catch (e: Exception) {
            Log.w(TAG, "ProfilerConfig not available (expected in release builds)")
        }
    }
    
    /**
     * Logs initialization performance using ProfilerConfig.
     * Only available in debug builds via reflection.
     */
    private fun logInitializationPerformance() {
        try {
            val profilerClass = Class.forName("com.entertainmentbrowser.debug.ProfilerConfig")
            val logPerformanceMethod = profilerClass.getMethod("logPerformance", String::class.java, Function0::class.java)
            logPerformanceMethod.invoke(
                profilerClass.getField("INSTANCE").get(null),
                "App Initialization",
                { initializeInBackground() }
            )
        } catch (e: Exception) {
            // Fallback if profiler not available
            Log.w(TAG, "Performance logging not available")
            initializeInBackground()
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Cancel application scope to clean up coroutines
        applicationScope.coroutineContext.cancel()
        // Clean up WebView state manager
        webViewStateManager.clearAll()
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
    }
    
    @Inject
    lateinit var fastAdBlockEngine: FastAdBlockEngine
    
    @Inject
    lateinit var advancedAdBlockEngine: com.entertainmentbrowser.util.adblock.AdvancedAdBlockEngine
    
    @Inject
    lateinit var webViewStateManager: com.entertainmentbrowser.util.WebViewStateManager
    
    private fun initializeAdBlocking() {
        try {
            Log.d(TAG, "🚀 Preloading ad-blockers...")
            val startTime = System.currentTimeMillis()
            
            // Initialize both engines in parallel
            val fastThread = Thread { fastAdBlockEngine.preloadFromAssets() }
            val advancedThread = Thread { advancedAdBlockEngine.preloadFromAssets() }
            
            fastThread.start()
            advancedThread.start()
            
            fastThread.join()
            advancedThread.join()
            
            val duration = System.currentTimeMillis() - startTime
            
            // Verify initialization status
            val fastStatus = fastAdBlockEngine.getStatus()
            val advancedStatus = advancedAdBlockEngine.getStatus()
            
            if (fastStatus.isHealthy() && advancedStatus.isHealthy()) {
                Log.d(TAG, "✅ Ad-blockers ready in ${duration}ms (95%+ blocking)")
                Log.d(TAG, "   FastEngine: ${fastStatus.blockedDomainsCount} domains, ${fastStatus.blockedPatternsCount} patterns")
                Log.d(TAG, "   AdvancedEngine: ${advancedStatus.blockedDomainsCount} domains, ${advancedStatus.wildcardPatternsCount} wildcards, ${advancedStatus.regexPatternsCount} regex")
                
                // Log rule truncation if any
                if (advancedStatus.isTruncated()) {
                    val ruleStats = advancedAdBlockEngine.getRuleStats()
                    Log.w(TAG, "⚠️ AdvancedEngine rule truncation detected:")
                    Log.w(TAG, "   Wildcard: ${ruleStats.wildcardPatternsLoaded}/${ruleStats.wildcardPatternsLimit} (dropped: ${ruleStats.wildcardPatternsDropped})")
                    Log.w(TAG, "   Regex: ${ruleStats.regexPatternsLoaded}/${ruleStats.regexPatternsLimit} (dropped: ${ruleStats.regexPatternsDropped})")
                    Log.w(TAG, "   Total truncation: ${ruleStats.getTruncationPercentage()}%")
                }
            } else {
                Log.w(TAG, "⚠️ Ad-blocker initialization incomplete:")
                if (!fastStatus.isHealthy()) {
                    Log.w(TAG, "   FastEngine: initialized=${fastStatus.isInitialized}, failed=${fastStatus.initializationFailed}, rules=${fastStatus.blockedDomainsCount}")
                }
                if (!advancedStatus.isHealthy()) {
                    Log.w(TAG, "   AdvancedEngine: initialized=${advancedStatus.isInitialized}, failed=${advancedStatus.initializationFailed}, rules=${advancedStatus.blockedDomainsCount}")
                }
                
                // If both engines failed, consider triggering filter update
                if (fastStatus.initializationFailed && advancedStatus.initializationFailed) {
                    Log.e(TAG, "❌ Both ad-blocking engines failed to initialize - ad blocking will be degraded")
                    // Note: FilterUpdateManager.forceUpdate() could be triggered here if needed
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start ad-blocker", e)
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
