package com.entertainmentbrowser.data.repository

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.URLUtil
import com.entertainmentbrowser.data.local.dao.DownloadDao
import com.entertainmentbrowser.data.local.entity.DownloadEntity
import com.entertainmentbrowser.domain.model.DownloadItem
import com.entertainmentbrowser.domain.model.DownloadStatus
import com.entertainmentbrowser.domain.repository.DownloadRepository
import com.entertainmentbrowser.util.MediaStoreHelper
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
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DownloadRepository using Android's DownloadManager.
 * Handles scoped storage for API 29+ and legacy storage for older versions.
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
        val trackedIds = downloadDao.getActiveDownloadIds()
        
        if (trackedIds.isEmpty()) {
            return false
        }
        
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
                        
                        if (newStatus in listOf("QUEUED", "DOWNLOADING", "PAUSED")) {
                            hasActiveDownloads = true
                        }
                        
                        val progress = if (totalBytes > 0) (bytesDownloaded * 100 / totalBytes).toInt() else 0
                        
                        // Handle completion: copy to MediaStore for API 29+
                        var updatedEntity = entity
                        if (newStatus == "COMPLETED" && entity.status != "COMPLETED") {
                            updatedEntity = handleDownloadCompletion(entity)
                        }
                        
                        // Check if status or progress changed
                        val statusChanged = updatedEntity.status != newStatus
                        val progressChanged = updatedEntity.downloadedBytes != bytesDownloaded || updatedEntity.progress != progress
                        
                        if (statusChanged || progressChanged) {
                            val finalEntity = updatedEntity.copy(
                                status = newStatus,
                                downloadedBytes = bytesDownloaded,
                                totalBytes = totalBytes,
                                progress = progress
                            )
                            downloadDao.update(finalEntity)
                            
                            // Update notification based on status change
                            val downloadItem = finalEntity.toDomainModel()
                            downloadNotificationManager.updateNotification(downloadItem)
                        }
                    }
                } while (it.moveToNext())
            }
        }
        
        return hasActiveDownloads
    }
    
    /**
     * Handle download completion: for API 29+, copy from app-specific dir to MediaStore
     * and store the content URI for later access.
     */
    private suspend fun handleDownloadCompletion(entity: DownloadEntity): DownloadEntity {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For API 29+, the file is in app-specific external files dir
            // Copy it to MediaStore and get content URI
            val appDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val sourceFile = File(appDownloadsDir, entity.filename)
            
            if (sourceFile.exists()) {
                val mimeType = MediaStoreHelper.getMimeType(entity.filename)
                val contentUri = MediaStoreHelper.saveToMediaStore(context, sourceFile, entity.filename, mimeType)
                
                if (contentUri != null) {
                    // Delete the temp file from app-specific storage
                    sourceFile.delete()
                    
                    return entity.copy(
                        contentUri = contentUri.toString(),
                        displayPath = "${Environment.DIRECTORY_DOWNLOADS}/${entity.filename}"
                    )
                }
            }
        }
        return entity
    }
    
    override fun observeDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getAllDownloads().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun startDownload(url: String, filename: String, quality: String) {
        try {
            if (!URLUtil.isValidUrl(url)) {
                android.util.Log.e("DownloadRepository", "Invalid URL: $url")
                throw IllegalArgumentException("Invalid download URL")
            }
            
            // Check storage permission for API 24-28 only
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (!hasPermission) {
                    val errorMsg = "Storage permission required for downloads on Android ${Build.VERSION.SDK_INT}"
                    android.util.Log.e("DownloadRepository", errorMsg)
                    throw SecurityException(errorMsg)
                }
            }
            
            android.util.Log.d("DownloadRepository", "Starting download with quality: $quality")
            
            val cookies = CookieManager.getInstance().getCookie(url)
            
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(filename)
                setDescription("Downloading video ($quality)")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                
                // Use different storage strategies based on API level
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // API 29+: Use app-specific external files directory (scoped storage)
                    // File will be copied to MediaStore on completion
                    setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, filename)
                } else {
                    // API <29: Use public Downloads directory directly
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                }
                
                if (!cookies.isNullOrEmpty()) {
                    addRequestHeader("Cookie", cookies)
                }
                
                addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                setAllowedOverRoaming(true)
            }
            
            val downloadId = downloadManager.enqueue(request)
            
            // Build entity with appropriate path/URI based on API level
            val (filePath, displayPath) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For API 29+, filePath is temp location, contentUri will be set on completion
                val tempPath = "${context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)}/$filename"
                Pair(tempPath, "${Environment.DIRECTORY_DOWNLOADS}/$filename")
            } else {
                // For legacy, use public Downloads path
                val publicPath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$filename"
                Pair(publicPath, "${Environment.DIRECTORY_DOWNLOADS}/$filename")
            }
            
            val downloadEntity = DownloadEntity(
                id = downloadId.toInt(),
                url = url,
                filename = filename,
                contentUri = null, // Will be set on completion for API 29+
                displayPath = displayPath,
                filePath = filePath,
                status = "DOWNLOADING",
                progress = 0,
                downloadedBytes = 0,
                totalBytes = 0,
                createdAt = System.currentTimeMillis()
            )
            downloadDao.insert(downloadEntity)
            
            android.util.Log.d("DownloadRepository", "Download started: $filename (ID: $downloadId) with quality: $quality")
            
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("DownloadRepository", "Invalid URL for download", e)
            throw e
        } catch (e: SecurityException) {
            android.util.Log.e("DownloadRepository", "Permission denied for download", e)
            throw e
        } catch (e: Exception) {
            android.util.Log.e("DownloadRepository", "Failed to start download", e)
            throw e
        }
    }
    
    override suspend fun pauseDownload(downloadId: Int) {
        // Android DownloadManager doesn't support pausing via public API
    }
    
    override suspend fun resumeDownload(downloadId: Int) {
        // See pauseDownload
    }
    
    override suspend fun cancelDownload(downloadId: Int) {
        try {
            downloadManager.remove(downloadId.toLong())
            downloadDao.delete(downloadId)
            // Cancel the notification for this download
            downloadNotificationManager.cancelNotification(downloadId)
        } catch (e: Exception) {
            android.util.Log.e("DownloadRepository", "Failed to cancel download: $downloadId", e)
        }
    }
    
    override suspend fun deleteDownload(downloadId: Int) {
        downloadDao.delete(downloadId)
        // Cancel the notification for this download
        downloadNotificationManager.cancelNotification(downloadId)
    }
}

/**
 * Extension function to convert DownloadEntity to DownloadItem domain model.
 */
internal fun DownloadEntity.toDomainModel(): DownloadItem {
    return DownloadItem(
        id = id,
        url = url,
        filename = filename,
        filePath = contentUri ?: filePath, // Prefer contentUri for API 29+
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
