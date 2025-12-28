package com.entertainmentbrowser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.entertainmentbrowser.data.local.entity.TabEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Tab entities.
 * 
 * ## Single Source of Truth
 * This DAO is the authoritative source for tab ordering and state.
 * All tab-related decisions (active tab, next tab after close, tab ordering)
 * should be derived from database queries, not from in-memory caches.
 * 
 * ## Tab Ordering
 * Tabs are ordered by timestamp (ASC) - oldest tabs first.
 * 
 * ## Active Tab Policy
 * Only one tab can be active at a time. Use [deactivateAllTabs] before
 * [setActiveTab] to ensure consistency.
 * 
 * ## Next Active Tab Policy (after close)
 * When closing the active tab, the next active tab should be determined by:
 * 1. Re-querying the database for remaining tabs
 * 2. Selecting the previous tab by index, or the first tab if closing index 0
 * This logic is delegated to [TabManager] to avoid split-brain state.
 */
@Dao
interface TabDao {
    
    @Query("SELECT * FROM tabs ORDER BY timestamp ASC")
    fun getAllTabs(): Flow<List<TabEntity>>
    
    @Query("SELECT * FROM tabs WHERE isActive = 1 LIMIT 1")
    fun getActiveTab(): Flow<TabEntity?>
    
    /**
     * One-shot query to get a tab by ID.
     * Use this for validation before switching tabs to avoid race conditions.
     * @param tabId The ID of the tab to retrieve
     * @return The tab entity or null if not found
     */
    @Query("SELECT * FROM tabs WHERE id = :tabId LIMIT 1")
    suspend fun getTabById(tabId: String): TabEntity?
    
    /**
     * One-shot query to get all tabs (non-Flow).
     * Use this when you need a consistent snapshot of tabs for decision-making.
     * @return List of all tabs ordered by timestamp
     */
    @Query("SELECT * FROM tabs ORDER BY timestamp ASC")
    suspend fun getAllTabsSnapshot(): List<TabEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tab: TabEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tabs: List<TabEntity>)
    
    @Query("DELETE FROM tabs WHERE id = :tabId")
    suspend fun delete(tabId: String)
    
    /**
     * Delete old inactive tabs based on lastAccessedAt.
     * Excludes active tabs to prevent unexpected tab loss.
     * @param cutoffTime Tabs with lastAccessedAt before this time will be deleted
     * @return List of tab IDs that were deleted (for thumbnail cleanup)
     */
    @Query("DELETE FROM tabs WHERE lastAccessedAt < :cutoffTime AND isActive = 0")
    suspend fun deleteOldInactiveTabs(cutoffTime: Long)
    
    /**
     * Get IDs of old inactive tabs that will be deleted.
     * Call this before deleteOldInactiveTabs to get thumbnail paths for cleanup.
     * @param cutoffTime Tabs with lastAccessedAt before this time
     * @return List of tabs that match the criteria
     */
    @Query("SELECT * FROM tabs WHERE lastAccessedAt < :cutoffTime AND isActive = 0")
    suspend fun getOldInactiveTabs(cutoffTime: Long): List<TabEntity>
    
    @Deprecated("Use deleteOldInactiveTabs instead to exclude active tabs")
    @Query("DELETE FROM tabs WHERE timestamp < :cutoffTime")
    suspend fun deleteOldTabs(cutoffTime: Long)
    
    @Query("UPDATE tabs SET lastAccessedAt = :accessTime WHERE id = :tabId")
    suspend fun updateLastAccessedAt(tabId: String, accessTime: Long)
    
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
