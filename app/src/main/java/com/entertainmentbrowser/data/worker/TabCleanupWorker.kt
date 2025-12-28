package com.entertainmentbrowser.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.entertainmentbrowser.data.local.dao.TabDao
import com.entertainmentbrowser.util.ThumbnailCapture
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically cleans up old inactive tabs.
 * Deletes inactive tabs whose lastAccessedAt is older than 7 days.
 * 
 * IMPORTANT: Active tabs are never deleted, even if they exceed the retention window.
 * Only thumbnails for actually deleted tabs are removed.
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
        private const val TAG = "TabCleanupWorker"
    }
    
    override suspend fun doWork(): Result {
        return try {
            // Calculate cutoff time (7 days ago based on lastAccessedAt)
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DAYS_TO_KEEP)
            
            // Get tabs that will be deleted BEFORE deleting them (for thumbnail cleanup)
            val tabsToDelete = tabDao.getOldInactiveTabs(cutoffTime)
            
            if (tabsToDelete.isEmpty()) {
                Log.d(TAG, "No old inactive tabs to clean up")
                return Result.success()
            }
            
            Log.d(TAG, "Cleaning up ${tabsToDelete.size} old inactive tabs")
            
            // Delete thumbnails only for tabs that will actually be deleted
            tabsToDelete.forEach { tab ->
                tab.thumbnailPath?.let { path ->
                    thumbnailCapture.deleteThumbnail(path)
                }
            }
            
            // Delete old inactive tabs from database (excludes active tabs)
            tabDao.deleteOldInactiveTabs(cutoffTime)
            
            Log.d(TAG, "Tab cleanup completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Tab cleanup failed", e)
            // Retry on failure
            Result.retry()
        }
    }
}
