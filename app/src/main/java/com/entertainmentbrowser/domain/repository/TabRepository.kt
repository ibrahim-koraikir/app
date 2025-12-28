package com.entertainmentbrowser.domain.repository

import com.entertainmentbrowser.domain.model.Tab
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for tab management.
 * 
 * ## Single Source of Truth
 * The database (via TabDao) is the authoritative source for tab state.
 * All tab-related decisions should be derived from repository queries,
 * not from in-memory StateFlow caches which may be stale.
 * 
 * ## Race Condition Prevention
 * When switching or closing tabs:
 * 1. Use [getTabById] to validate tab existence before operations
 * 2. Use [getAllTabsSnapshot] for consistent decision-making
 * 3. Avoid relying on Flow emissions which may be delayed
 */
interface TabRepository {
    fun getAllTabs(): Flow<List<Tab>>
    fun getActiveTab(): Flow<Tab?>
    
    /**
     * One-shot query to get a tab by ID.
     * Use this for validation before switching tabs to avoid race conditions.
     * @param tabId The ID of the tab to retrieve
     * @return The tab or null if not found
     */
    suspend fun getTabById(tabId: String): Tab?
    
    /**
     * One-shot query to get all tabs.
     * Use this when you need a consistent snapshot for decision-making.
     * @return List of all tabs ordered by timestamp
     */
    suspend fun getAllTabsSnapshot(): List<Tab>
    
    suspend fun createTab(url: String, title: String): Tab
    suspend fun createTabInBackground(url: String, title: String): Tab
    suspend fun switchTab(tabId: String)
    
    /**
     * Closes a tab and returns the ID of the next tab to activate.
     * The "next active tab" policy is:
     * 1. If closing the active tab, select the previous tab by index
     * 2. If closing index 0, select the new first tab
     * 3. If no tabs remain, return null
     * 
     * @param tabId The ID of the tab to close
     * @return The ID of the next tab to activate, or null if no tabs remain
     */
    suspend fun closeTab(tabId: String): String?
    
    suspend fun closeAllTabs()
    suspend fun getTabCount(): Int
    suspend fun updateTabThumbnail(tabId: String, thumbnailPath: String)
    suspend fun updateTabUrl(tabId: String, url: String)
}
