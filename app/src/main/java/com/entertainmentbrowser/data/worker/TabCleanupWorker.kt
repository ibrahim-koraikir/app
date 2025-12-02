package com.entertainmentbrowser.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.entertainmentbrowser.data.local.dao.TabDao
import com.entertainmentbrowser.util.ThumbnailCapture
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically cleans up old tabs.
 * Deletes tabs older than 7 days and their associated thumbnails.
 * 
 * Runs with battery-aware and device-idle constraints for efficiency.
 * Uses exponential backoff for failed attempts.
 * 
 * Requirements: 16.1, 16.2, 16.3, 16.4, 16.5
 */
@HiltWorker
class TabCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val tabDao: TabDao,
    private val thumbnailCapture: ThumbnailCapture
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        const val WORK_NAME = "tab_cleanup_work"
        private const val DAYS_TO_KEEP = 7L
    }
    
    override suspend fun doWork(): Result {
        return try {
            // Calculate cutoff time (7 days ago)
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DAYS_TO_KEEP)
            
            // Delete old tabs from database
            tabDao.deleteOldTabs(cutoffTime)
            
            // Delete old thumbnails from storage
            thumbnailCapture.deleteOldThumbnails(cutoffTime)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Retry on failure
            Result.retry()
        }
    }
}
