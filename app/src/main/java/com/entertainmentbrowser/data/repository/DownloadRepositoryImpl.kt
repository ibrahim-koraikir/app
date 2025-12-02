package com.entertainmentbrowser.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import com.entertainmentbrowser.data.local.dao.DownloadDao
import com.entertainmentbrowser.data.local.entity.DownloadEntity
import com.entertainmentbrowser.domain.model.DownloadItem
import com.entertainmentbrowser.domain.model.DownloadStatus
import com.entertainmentbrowser.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DownloadRepository using Android's DownloadManager.
 * TODO: Implement full download functionality when Fetch library dependency is resolved.
 */
@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: DownloadManager,
    private val downloadDao: DownloadDao,
    private val notificationManager: android.app.NotificationManager
) : DownloadRepository {
    
    private val downloadNotificationManager = com.entertainmentbrowser.util.DownloadNotificationManager(
        context,
        notificationManager
    )
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null
    
    // Adaptive polling interval
    private var currentPollingInterval = 1000L
    private val fastPollingInterval = 1000L  // 1 second when downloads are active
    private val slowPollingInterval = 2000L  // 2 seconds when no active downloads
    
    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            while (isActive) {
                try {
                    val hasActiveDownloads = updateDownloadProgress()
                    
                    // Adaptive polling: faster when downloads are active, slower when idle
                    currentPollingInterval = if (hasActiveDownloads) {
                        fastPollingInterval
                    } else {
                        slowPollingInterval
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DownloadRepository", "Error updating download progress", e)
                }
                delay(currentPollingInterval)
            }
        }
    }

    /**
     * Update download progress for tracked downloads only.
     * Returns true if there are active downloads, false otherwise.
     */
    private suspend fun updateDownloadProgress(): Boolean {
        // Get only the download IDs we're tracking
        val trackedIds = downloadDao.getActiveDownloadIds()
        
        // Early return if no downloads to track
        if (trackedIds.isEmpty()) {
            return false
        }
        
        // Query only our tracked downloads
        val query = DownloadManager.Query().setFilterById(*trackedIds.map { it.toLong() }.toLongArray())
        val cursor = downloadManager.query(query)
        
        var hasActiveDownloads = false
        
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                    val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytesDownloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalBytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    
                    val entity = downloadDao.getDownloadById(id)
                    if (entity != null) {
                        val newStatus = when (status) {
                            DownloadManager.STATUS_PENDING -> "QUEUED"
                            DownloadManager.STATUS_RUNNING -> "DOWNLOADING"
                            DownloadManager.STATUS_PAUSED -> "PAUSED"
                            DownloadManager.STATUS_SUCCESSFUL -> "COMPLETED"
                            DownloadManager.STATUS_FAILED -> "FAILED"
                            else -> "QUEUED"
                        }
                        
                        // Track if we have any active downloads
                        if (newStatus in listOf("QUEUED", "DOWNLOADING", "PAUSED")) {
                            hasActiveDownloads = true
                        }
                        
                        val progress = if (totalBytes > 0) (bytesDownloaded * 100 / totalBytes).toInt() else 0
                        
                        // Only update if changed to avoid database churn
                        if (entity.status != newStatus || entity.downloadedBytes != bytesDownloaded || entity.progress != progress) {
                            downloadDao.update(entity.copy(
                                status = newStatus,
                                downloadedBytes = bytesDownloaded,
                                totalBytes = totalBytes,
                                progress = progress
                            ))
                        }
                    }
                } while (it.moveToNext())
            }
        }
        
        return hasActiveDownloads
    }
    
    override fun observeDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getAllDownloads().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun startDownload(url: String, filename: String, quality: String) {
        try {
            // Validate URL
            if (!URLUtil.isValidUrl(url)) {
                android.util.Log.e("DownloadRepository", "Invalid URL: $url")
                throw IllegalArgumentException("Invalid download URL")
            }
            
            // Check storage permission for API 24-28
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (!hasPermission) {
                    val errorMsg = "Storage permission required for downloads on Android ${android.os.Build.VERSION.SDK_INT}"
                    android.util.Log.e("DownloadRepository", errorMsg)
                    throw SecurityException(errorMsg)
                }
            }
            
            android.util.Log.d("DownloadRepository", "Starting download with quality: $quality")
            
            // Get cookies for the request
            val cookies = CookieManager.getInstance().getCookie(url)
            
            // Create download request
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                // Set title and description for notification
                setTitle(filename)
                setDescription("Downloading video ($quality)")
                
                // Set notification visibility
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                
                // Set destination in public Downloads directory
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                
                // Add cookies if available
                if (!cookies.isNullOrEmpty()) {
                    addRequestHeader("Cookie", cookies)
                }
                
                // Add user agent
                addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                
                // Allow download over mobile and WiFi
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                setAllowedOverRoaming(true)
            }
            
            // Enqueue the download
            val downloadId = downloadManager.enqueue(request)
            
            // Save to database
            val downloadEntity = DownloadEntity(
                id = downloadId.toInt(),
                url = url,
                filename = filename,
                filePath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$filename",
                status = "DOWNLOADING",
                progress = 0,
                downloadedBytes = 0,
                totalBytes = 0,
                createdAt = System.currentTimeMillis()
            )
            downloadDao.insert(downloadEntity)
            
            android.util.Log.d("DownloadRepository", "Download started: $filename (ID: $downloadId) with quality: $quality")
            
        } catch (e: Exception) {
            android.util.Log.e("DownloadRepository", "Failed to start download", e)
        }
    }
    
    override suspend fun pauseDownload(downloadId: Int) {
        // Android DownloadManager doesn't support pausing arbitrary downloads easily via public API
        // without removing and restarting with Range header, which is complex.
        // For now, we'll leave this as no-op or user can cancel and restart.
    }
    
    override suspend fun resumeDownload(downloadId: Int) {
        // See pauseDownload
    }
    
    override suspend fun cancelDownload(downloadId: Int) {
        try {
            downloadManager.remove(downloadId.toLong())
            downloadDao.delete(downloadId)
        } catch (e: Exception) {
            android.util.Log.e("DownloadRepository", "Failed to cancel download: $downloadId", e)
        }
    }
    
    override suspend fun deleteDownload(downloadId: Int) {
        downloadDao.delete(downloadId)
    }
}

/**
 * Extension function to convert DownloadEntity to DownloadItem domain model.
 */
private fun DownloadEntity.toDomainModel(): DownloadItem {
    return DownloadItem(
        id = id,
        url = url,
        filename = filename,
        filePath = filePath,
        status = when (status) {
            "QUEUED" -> DownloadStatus.QUEUED
            "DOWNLOADING" -> DownloadStatus.DOWNLOADING
            "PAUSED" -> DownloadStatus.PAUSED
            "COMPLETED" -> DownloadStatus.COMPLETED
            "FAILED" -> DownloadStatus.FAILED
            "CANCELLED" -> DownloadStatus.CANCELLED
            else -> DownloadStatus.QUEUED
        },
        progress = progress,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        createdAt = createdAt
    )
}
