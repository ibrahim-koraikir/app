package com.entertainmentbrowser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.entertainmentbrowser.data.local.entity.TabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    
    @Query("SELECT * FROM tabs ORDER BY timestamp ASC")
    fun getAllTabs(): Flow<List<TabEntity>>
    
    @Query("SELECT * FROM tabs WHERE isActive = 1 LIMIT 1")
    fun getActiveTab(): Flow<TabEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tab: TabEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tabs: List<TabEntity>)
    
    @Query("DELETE FROM tabs WHERE id = :tabId")
    suspend fun delete(tabId: String)
    
    @Query("DELETE FROM tabs WHERE timestamp < :cutoffTime")
    suspend fun deleteOldTabs(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM tabs")
    suspend fun getTabCount(): Int
    
    @Query("UPDATE tabs SET isActive = 0")
    suspend fun deactivateAllTabs()
    
    @Query("UPDATE tabs SET isActive = 1 WHERE id = :tabId")
    suspend fun setActiveTab(tabId: String)
    
    @Query("SELECT * FROM tabs WHERE isActive = 0 ORDER BY timestamp ASC LIMIT 1")
    suspend fun getOldestInactiveTab(): TabEntity?
    
    @Query("DELETE FROM tabs")
    suspend fun deleteAllTabs()
    
    @Query("UPDATE tabs SET thumbnailPath = :thumbnailPath WHERE id = :tabId")
    suspend fun updateThumbnail(tabId: String, thumbnailPath: String)
    
    @Query("UPDATE tabs SET url = :url WHERE id = :tabId")
    suspend fun updateUrl(tabId: String, url: String)
}
