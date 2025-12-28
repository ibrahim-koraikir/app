package com.entertainmentbrowser.util

import android.webkit.WebView
import com.entertainmentbrowser.data.local.dao.TabDao
import com.entertainmentbrowser.data.local.entity.TabEntity
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages tab lifecycle including creation, limits, and automatic cleanup.
 * Enforces a maximum of 20 tabs and automatically closes oldest tabs when limit is reached.
 * 
 * ## Single Source of Truth
 * This class, together with [TabDao], is the authoritative source for:
 * - Tab ordering (by timestamp, oldest first)
 * - Active tab state (only one tab active at a time)
 * - "Next active tab" policy when closing tabs
 * 
 * ## Next Active Tab Policy
 * When closing the active tab:
 * 1. Get a fresh snapshot of remaining tabs from the database
 * 2. Select the previous tab by index (closedIndex - 1)
 * 3. If closing index 0, select the new first tab (index 0)
 * 4. If no tabs remain, return null
 * 
 * ## Race Condition Prevention
 * All tab state decisions are made using one-shot database queries
 * ([TabDao.getTabById], [TabDao.getAllTabsSnapshot]) rather than
 * Flow emissions which may be delayed or stale.
 */
@Singleton
class TabManager @Inject constructor(
    private val tabDao: TabDao,
    private val thumbnailCapture: ThumbnailCapture,
    private val webViewStateManager: WebViewStateManager
) {
    companion object {
        const val MAX_TAB_COUNT = 20
    }
    
    /**
     * Creates a new tab with a unique ID.
     * If the tab limit (20) is reached, automatically closes the oldest inactive tab.
     * 
     * @param url The URL to load in the tab
     * @param title The title of the tab
     * @param thumbnailPath Optional path to the tab's thumbnail image
     * @return The created TabEntity
     */
    suspend fun createTab(
        url: String,
        title: String,
        thumbnailPath: String? = null
    ): TabEntity {
        // Check if we've reached the tab limit
        val currentCount = tabDao.getTabCount()
        if (currentCount >= MAX_TAB_COUNT) {
            // Close the oldest inactive tab
            closeOldestInactiveTab()
        }
        
        // Deactivate all existing tabs
        tabDao.deactivateAllTabs()
        
        // Create new tab with unique ID
        val newTab = TabEntity(
            id = UUID.randomUUID().toString(),
            url = url,
            title = title,
            thumbnailPath = thumbnailPath,
            isActive = true,
            timestamp = System.currentTimeMillis()
        )
        
        tabDao.insert(newTab)
        return newTab
    }
    
    /**
     * Creates a new tab in the background without switching to it.
     * The current active tab remains active.
     * If the tab limit (20) is reached, automatically closes the oldest inactive tab.
     * 
     * @param url The URL to load in the tab
     * @param title The title of the tab
     * @param thumbnailPath Optional path to the tab's thumbnail image
     * @return The created TabEntity
     */
    suspend fun createTabInBackground(
        url: String,
        title: String,
        thumbnailPath: String? = null
    ): TabEntity {
        // Check if we've reached the tab limit
        val currentCount = tabDao.getTabCount()
        if (currentCount >= MAX_TAB_COUNT) {
            // Close the oldest inactive tab
            closeOldestInactiveTab()
        }
        
        // Create new tab with unique ID - NOT active (stays in background)
        val newTab = TabEntity(
            id = UUID.randomUUID().toString(),
            url = url,
            title = title,
            thumbnailPath = thumbnailPath,
            isActive = false,
            timestamp = System.currentTimeMillis()
        )
        
        tabDao.insert(newTab)
        return newTab
    }
    
    /**
     * Switches to an existing tab by ID.
     * Deactivates all other tabs and activates the specified tab.
     * Also manages WebView state (pause old, resume new) and updates lastAccessedAt.
     * 
     * @param tabId The ID of the tab to switch to
     */
    suspend fun switchTab(tabId: String) {
        // Get current active tab before switching
        // Note: We collect the first value from the Flow
        val currentActiveTab = tabDao.getActiveTab().first()
        
        // Pause and save state of current tab
        currentActiveTab?.let { tab ->
            webViewStateManager.pauseWebView(tab.id)
            webViewStateManager.saveWebViewState(tab.id)
        }
        
        // Switch tabs in database
        tabDao.deactivateAllTabs()
        tabDao.setActiveTab(tabId)
        
        // Update lastAccessedAt for the newly active tab
        tabDao.updateLastAccessedAt(tabId, System.currentTimeMillis())
        
        // Resume new tab
        webViewStateManager.resumeWebView(tabId)
    }
    
    /**
     * Closes a tab by ID and determines the next tab to activate.
     * Also cleans up the WebView for this tab.
     * 
     * ## Next Active Tab Policy
     * 1. Get a fresh snapshot of all tabs before deletion
     * 2. Find the index of the tab being closed
     * 3. Delete the tab from database
     * 4. Get fresh list of remaining tabs
     * 5. Determine next tab: previous index, or first tab if closing index 0
     * 6. Activate the next tab if one exists
     * 
     * @param tabId The ID of the tab to close
     * @return The ID of the next tab to activate, or null if no tabs remain
     */
    suspend fun closeTab(tabId: String): String? {
        // Get current tabs snapshot BEFORE deletion to find the closed tab's index
        val tabsBeforeClose = tabDao.getAllTabsSnapshot()
        val closedTabIndex = tabsBeforeClose.indexOfFirst { it.id == tabId }
        val wasActiveTab = tabsBeforeClose.find { it.id == tabId }?.isActive == true
        
        // Release WebView for pooling instead of destroying it
        // This allows the WebView to be reused and frees the GPU slot
        webViewStateManager.releaseWebViewForPooling(tabId, listOf("AndroidInterface"))
        
        // Delete tab from database
        tabDao.delete(tabId)
        
        // If the closed tab wasn't active, no need to switch
        if (!wasActiveTab) {
            return null
        }
        
        // Get fresh list of remaining tabs AFTER deletion
        val remainingTabs = tabDao.getAllTabsSnapshot()
        
        if (remainingTabs.isEmpty()) {
            return null
        }
        
        // Determine next tab to activate based on policy:
        // - Select previous tab by index
        // - If closing index 0, select new first tab
        val nextIndex = when {
            closedTabIndex > 0 -> (closedTabIndex - 1).coerceIn(0, remainingTabs.lastIndex)
            else -> 0
        }
        
        val nextTab = remainingTabs[nextIndex]
        
        // Activate the next tab
        tabDao.deactivateAllTabs()
        tabDao.setActiveTab(nextTab.id)
        webViewStateManager.resumeWebView(nextTab.id)
        
        return nextTab.id
    }
    
    /**
     * Updates the thumbnail for a tab.
     * 
     * @param tabId The ID of the tab
     * @param webView The WebView to capture
     * @return The path to the saved thumbnail, or null if capture failed
     */
    suspend fun updateTabThumbnail(tabId: String, webView: WebView): String? {
        return thumbnailCapture.captureWebViewThumbnail(webView, tabId)
    }
    
    /**
     * Gets the current tab count.
     * 
     * @return The number of tabs currently open
     */
    suspend fun getTabCount(): Int {
        return tabDao.getTabCount()
    }
    
    /**
     * Closes the oldest inactive tab.
     * This is called automatically when the tab limit is reached.
     */
    private suspend fun closeOldestInactiveTab() {
        val oldestInactiveTab = tabDao.getOldestInactiveTab()
        oldestInactiveTab?.let { tab ->
            // Release WebView for pooling instead of destroying it
            // This allows the WebView to be reused and frees the GPU slot
            webViewStateManager.releaseWebViewForPooling(tab.id, listOf("AndroidInterface"))
            
            // Delete tab from database
            tabDao.delete(tab.id)
        }
    }
}
