package com.entertainmentbrowser.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.entertainmentbrowser.util.adblock.FilterUpdateManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker to update ad filter lists
 * Runs periodically (every 7 days) to keep filters up to date
 */
@HiltWorker
class FilterUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val filterUpdateManager: FilterUpdateManager
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "FilterUpdateWorker"
        const val WORK_NAME = "filter_update_work"
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "🔄 Starting filter update...")
            
            val success = filterUpdateManager.checkAndUpdateFilters()
            
            if (success) {
                Log.d(TAG, "✅ Filter update completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "⚠️ Filter update failed, will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Filter update error", e)
            Result.retry()
        }
    }
}
