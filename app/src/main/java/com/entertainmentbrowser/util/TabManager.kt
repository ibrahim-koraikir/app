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
     * Switches to an existing tab by ID.
     * Deactivates all other tabs and activates the specified tab.
     * Also manages WebView state (pause old, resume new).
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
        
        // Resume new tab
        webViewStateManager.resumeWebView(tabId)
    }
    
    /**
     * Closes a tab by ID.
     * Also cleans up the WebView for this tab.
     * 
     * @param tabId The ID of the tab to close
     */
    suspend fun closeTab(tabId: String) {
        // Clean up WebView for this tab
        webViewStateManager.removeWebView(tabId)
        
        // Delete tab from database
        tabDao.delete(tabId)
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
            // Clean up WebView for this tab
            webViewStateManager.removeWebView(tab.id)
            
            // Delete tab from database
            tabDao.delete(tab.id)
        }
    }
}
