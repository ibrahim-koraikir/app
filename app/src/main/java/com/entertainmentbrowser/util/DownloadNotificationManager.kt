package com.entertainmentbrowser.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.entertainmentbrowser.R
import com.entertainmentbrowser.domain.model.DownloadItem
import com.entertainmentbrowser.domain.model.DownloadStatus
import java.io.File

class DownloadNotificationManager(
    private val context: Context,
    private val notificationManager: NotificationManager
) {
    
    companion object {
        private const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID_BASE = 1000
    }
    
    /**
     * Show progress notification for active download
     */
    fun showProgressNotification(download: DownloadItem) {
        val notification = NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Downloading ${download.filename}")
            .setContentText("${download.progress}% complete")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, download.progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_BASE + download.id, notification)
    }
    
    /**
     * Show completion notification with "Open" action
     */
    fun showCompletionNotification(download: DownloadItem) {
        val openIntent = createOpenIntent(download)
        
        // Only create pending intent if we have a valid open intent
        val pendingIntent = if (openIntent != null) {
            PendingIntent.getActivity(
                context,
                download.id,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else null
        
        val builder = NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download complete")
            .setContentText(download.filename)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
                .addAction(
                    android.R.drawable.ic_menu_view,
                    "Open",
                    pendingIntent
                )
        }
        
        notificationManager.notify(NOTIFICATION_ID_BASE + download.id, builder.build())
    }
    
    /**
     * Show failure notification
     */
    fun showFailureNotification(download: DownloadItem) {
        val notification = NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download failed")
            .setContentText(download.filename)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_BASE + download.id, notification)
    }
    
    /**
     * Cancel notification for a download
     */
    fun cancelNotification(downloadId: Int) {
        notificationManager.cancel(NOTIFICATION_ID_BASE + downloadId)
    }
    
    /**
     * Update notification based on download status
     */
    fun updateNotification(download: DownloadItem) {
        when (download.status) {
            DownloadStatus.DOWNLOADING -> showProgressNotification(download)
            DownloadStatus.COMPLETED -> showCompletionNotification(download)
            DownloadStatus.FAILED -> showFailureNotification(download)
            DownloadStatus.CANCELLED -> cancelNotification(download.id)
            else -> {
                // No notification for QUEUED or PAUSED
            }
        }
    }
    
    /**
     * Create intent to open downloaded file.
     * Handles both content:// URIs (API 29+) and file paths (legacy) with FileProvider.
     */
    private fun createOpenIntent(download: DownloadItem): Intent? {
        val uri = getFileUri(download) ?: return null
        val mimeType = MediaStoreHelper.getMimeType(download.filename)
        
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }
    
    /**
     * Get the appropriate URI for opening a downloaded file.
     * - For API 29+: Uses content:// URI from MediaStore (stored in filePath after completion)
     * - For legacy API <29: Wraps file path with FileProvider
     */
    private fun getFileUri(download: DownloadItem): Uri? {
        val filePath = download.filePath ?: return null
        
        // Check if it's already a content:// URI (API 29+ after MediaStore copy)
        if (filePath.startsWith("content://")) {
            return Uri.parse(filePath)
        }
        
        // For API 29+, try to look up content URI from MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentUri = MediaStoreHelper.getContentUri(context, download.filename)
            if (contentUri != null) {
                return contentUri
            }
        }
        
        // Legacy path: wrap with FileProvider for secure sharing
        return try {
            val file = File(filePath)
            if (file.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                android.util.Log.w("DownloadNotification", "File not found: $filePath")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadNotification", "Failed to get FileProvider URI", e)
            null
        }
    }
}
