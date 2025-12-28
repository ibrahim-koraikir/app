package com.entertainmentbrowser.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat

/**
 * Handles runtime permission requests for the app.
 * Centralizes permission logic to keep MainActivity focused on UI setup.
 * 
 * Updates DownloadPermissionState when permissions change so ViewModels
 * can proactively check permissions before starting downloads.
 * 
 * Usage:
 * ```
 * class MainActivity : ComponentActivity() {
 *     @Inject lateinit var downloadPermissionState: DownloadPermissionState
 *     private lateinit var permissionHandler: PermissionHandler
 *     
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         permissionHandler = PermissionHandler(this, downloadPermissionState)
 *         permissionHandler.requestStoragePermissionIfNeeded()
 *     }
 *     
 *     fun hasStoragePermission() = permissionHandler.hasStoragePermission()
 * }
 * ```
 */
class PermissionHandler(
    private val activity: ComponentActivity,
    private val downloadPermissionState: DownloadPermissionState? = null
) {
    
    companion object {
        private const val TAG = "PermissionHandler"
    }
    
    // Storage permission state - default true for API 29+ (scoped storage)
    private val _storagePermissionGranted: MutableState<Boolean> = mutableStateOf(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    )
    
    // Notification permission state - default true for API < 33
    private val _notificationPermissionGranted: MutableState<Boolean> = mutableStateOf(
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    )
    
    // Permission launchers - must be registered before activity is started
    private val storagePermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            _storagePermissionGranted.value = isGranted
            // Update shared permission state for ViewModels
            downloadPermissionState?.updateStoragePermission(isGranted)
            if (isGranted) {
                Log.d(TAG, "Storage permission granted")
            } else {
                Log.w(TAG, "Storage permission denied - downloads will fail on API 24-28")
            }
        }
    
    private val notificationPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            _notificationPermissionGranted.value = isGranted
            // Update shared permission state for ViewModels
            downloadPermissionState?.updateNotificationPermission(isGranted)
            if (isGranted) {
                Log.d(TAG, "Notification permission granted")
            } else {
                Log.w(TAG, "Notification permission denied - download notifications won't show")
            }
        }
    
    /**
     * Check if storage permission is granted for downloads.
     * Returns true for API 29+ (scoped storage doesn't need permission).
     * Returns actual permission state for API 24-28.
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Scoped storage, no permission needed
        } else {
            _storagePermissionGranted.value
        }
    }
    
    /**
     * Check if notification permission is granted.
     * Returns true for API < 33 (no runtime permission needed).
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true // No runtime permission needed
        } else {
            _notificationPermissionGranted.value
        }
    }
    
    /**
     * Request storage permission if needed (API 24-28 only).
     * Android 10+ uses scoped storage and doesn't need this permission for Downloads.
     */
    fun requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.P) {
            val hasPermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            
            _storagePermissionGranted.value = hasPermission
            
            if (!hasPermission) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    
    /**
     * Request notification permission if needed (API 33+ only).
     */
    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            _notificationPermissionGranted.value = hasPermission
            
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
