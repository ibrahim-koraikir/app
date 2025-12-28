package com.entertainmentbrowser.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that tracks download-related permission state.
 * Allows ViewModels to check permissions before attempting downloads,
 * avoiding reliance on catching SecurityException for control flow.
 * 
 * This is updated by PermissionHandler in MainActivity when permissions change.
 */
@Singleton
class DownloadPermissionState @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val _storagePermissionGranted = MutableStateFlow(checkStoragePermission())
    private val _notificationPermissionGranted = MutableStateFlow(checkNotificationPermission())
    
    /**
     * Observable storage permission state.
     */
    val storagePermissionGranted: StateFlow<Boolean> = _storagePermissionGranted.asStateFlow()
    
    /**
     * Observable notification permission state.
     */
    val notificationPermissionGranted: StateFlow<Boolean> = _notificationPermissionGranted.asStateFlow()
    
    /**
     * Check if storage permission is currently granted.
     * Returns true for API 29+ (scoped storage doesn't need permission).
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Scoped storage, no permission needed for Downloads folder
        } else {
            _storagePermissionGranted.value
        }
    }
    
    /**
     * Check if notification permission is currently granted.
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
     * Check if all required permissions for downloads are granted.
     * Storage permission is required; notification permission is optional but recommended.
     */
    fun canStartDownload(): Boolean {
        return hasStoragePermission()
    }
    
    /**
     * Update storage permission state.
     * Called by PermissionHandler when permission result is received.
     */
    fun updateStoragePermission(granted: Boolean) {
        _storagePermissionGranted.value = granted
    }
    
    /**
     * Update notification permission state.
     * Called by PermissionHandler when permission result is received.
     */
    fun updateNotificationPermission(granted: Boolean) {
        _notificationPermissionGranted.value = granted
    }
    
    /**
     * Refresh permission state by checking current system state.
     * Useful when returning from app settings.
     */
    fun refreshPermissions() {
        _storagePermissionGranted.value = checkStoragePermission()
        _notificationPermissionGranted.value = checkNotificationPermission()
    }
    
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Scoped storage
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true // No runtime permission needed
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
