package com.entertainmentbrowser.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.webkit.WebView

/**
 * Manages GPU memory usage to prevent Mali GPU allocation errors
 */
object GpuMemoryManager {
    private const val TAG = "GpuMemoryManager"
    
    // Track active hardware-accelerated WebViews
    private val hardwareAcceleratedViews = mutableSetOf<String>()
    
    // Maximum hardware-accelerated WebViews based on device memory
    private var maxHardwareAccelerated = 3
    
    fun initialize(context: Context) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // Calculate max based on available memory
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024)
        maxHardwareAccelerated = when {
            totalMemoryMB < 2048 -> 2  // Low memory devices
            totalMemoryMB < 4096 -> 3  // Mid-range devices
            else -> 5                   // High-end devices
        }
        
        Log.d(TAG, "Device memory: ${totalMemoryMB}MB, max hardware accelerated: $maxHardwareAccelerated")
    }
    
    /**
     * Configure WebView layer type based on GPU memory availability
     */
    fun configureWebView(webView: WebView, tabId: String, isActive: Boolean) {
        if (isActive) {
            // Active tab gets hardware acceleration if available
            if (hardwareAcceleratedViews.size < maxHardwareAccelerated) {
                enableHardwareAcceleration(webView, tabId)
            } else {
                disableHardwareAcceleration(webView, tabId)
            }
        } else {
            // Background tabs use software rendering
            disableHardwareAcceleration(webView, tabId)
        }
    }
    
    private fun enableHardwareAcceleration(webView: WebView, tabId: String) {
        if (!hardwareAcceleratedViews.contains(tabId)) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            hardwareAcceleratedViews.add(tabId)
            Log.d(TAG, "Enabled hardware acceleration for tab $tabId (${hardwareAcceleratedViews.size}/$maxHardwareAccelerated)")
        }
    }
    
    private fun disableHardwareAcceleration(webView: WebView, tabId: String) {
        if (hardwareAcceleratedViews.contains(tabId)) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            hardwareAcceleratedViews.remove(tabId)
            Log.d(TAG, "Disabled hardware acceleration for tab $tabId (${hardwareAcceleratedViews.size}/$maxHardwareAccelerated)")
        }
    }
    
    /**
     * Release GPU resources for a tab
     */
    fun releaseTab(tabId: String) {
        hardwareAcceleratedViews.remove(tabId)
    }
    
    /**
     * Get current GPU memory pressure
     */
    fun getMemoryPressure(): MemoryPressure {
        return when {
            hardwareAcceleratedViews.size >= maxHardwareAccelerated -> MemoryPressure.HIGH
            hardwareAcceleratedViews.size >= maxHardwareAccelerated * 0.7 -> MemoryPressure.MEDIUM
            else -> MemoryPressure.LOW
        }
    }
    
    enum class MemoryPressure {
        LOW, MEDIUM, HIGH
    }
}
