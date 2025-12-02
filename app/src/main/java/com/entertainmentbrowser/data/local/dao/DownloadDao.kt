package com.entertainmentbrowser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.entertainmentbrowser.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE id = :downloadId")
    suspend fun getDownloadById(downloadId: Int): DownloadEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)
    
    @Update
    suspend fun update(download: DownloadEntity)
    
    @Query("DELETE FROM downloads WHERE id = :downloadId")
    suspend fun delete(downloadId: Int)
    
    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    suspend fun deleteCompletedDownloads()
    
    @Query("SELECT id FROM downloads")
    suspend fun getAllDownloadIds(): List<Int>
    
    @Query("SELECT id FROM downloads WHERE status NOT IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    suspend fun getActiveDownloadIds(): List<Int>
}
