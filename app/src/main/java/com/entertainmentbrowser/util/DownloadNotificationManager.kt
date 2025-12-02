package com.entertainmentbrowser.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.entertainmentbrowser.R
import com.entertainmentbrowser.domain.model.DownloadItem
import com.entertainmentbrowser.domain.model.DownloadStatus

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
        val pendingIntent = PendingIntent.getActivity(
            context,
            download.id,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Download complete")
            .setContentText(download.filename)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open",
                pendingIntent
            )
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_BASE + download.id, notification)
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
     * Create intent to open downloaded file
     */
    private fun createOpenIntent(download: DownloadItem): Intent {
        val uri = if (download.filePath != null) {
            Uri.parse(download.filePath)
        } else {
            MediaStoreHelper.getContentUri(context, download.filename)
        }
        
        val mimeType = MediaStoreHelper.getMimeType(download.filename)
        
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }
}
