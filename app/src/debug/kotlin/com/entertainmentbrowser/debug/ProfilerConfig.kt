package com.entertainmentbrowser.debug

import android.os.StrictMode
import android.util.Log

/**
 * Performance profiling configuration for debug builds.
 * Enables StrictMode policies and provides performance logging utilities.
 * 
 * Requirements: 17.1, 17.2, 17.3
 */
object ProfilerConfig {
    
    private const val TAG = "ProfilerConfig"
    
    /**
     * Enables StrictMode with thread and VM policies to detect performance issues.
     * 
     * Thread Policy detects:
     * - Disk reads on main thread
     * - Disk writes on main thread
     * - Network operations on main thread
     * 
     * VM Policy detects:
     * - Leaked SQLite objects
     * - Leaked closeable objects
     * - Leaked activities
     * 
     * Requirements: 17.1, 17.2
     */
    fun enableStrictMode() {
        // Thread policy to detect main thread violations
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                // .penaltyFlashScreen() // Disabled - causes red border flash during tab switching
                .build()
        )
        
        // VM policy to detect memory leaks
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build()
        )
        
        Log.d(TAG, "✅ StrictMode enabled for performance monitoring")
    }
    
    /**
     * Measures and logs the execution time of a code block.
     * Useful for identifying performance bottlenecks during development.
     * 
     * @param tag The tag to use for logging
     * @param block The code block to measure
     * 
     * Requirement: 17.3
     */
    fun logPerformance(tag: String, block: () -> Unit) {
        val startTime = System.currentTimeMillis()
        
        try {
            block()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            
            // Log warning if operation takes longer than 100ms (Requirement 17.2)
            if (duration > 100) {
                Log.w(TAG, "⚠️ [$tag] took ${duration}ms (>100ms threshold)")
            } else {
                Log.d(TAG, "⏱️ [$tag] took ${duration}ms")
            }
        }
    }
}
