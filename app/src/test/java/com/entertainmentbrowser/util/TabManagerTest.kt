package com.entertainmentbrowser.util

import com.entertainmentbrowser.data.local.dao.TabDao
import com.entertainmentbrowser.data.local.entity.TabEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TabManager.
 * Tests tab creation, limit enforcement, and automatic cleanup.
 */
class TabManagerTest {
    
    private lateinit var tabDao: TabDao
    private lateinit var thumbnailCapture: ThumbnailCapture
    private lateinit var webViewStateManager: WebViewStateManager
    private lateinit var tabManager: TabManager
    
    @Before
    fun setup() {
        tabDao = mockk(relaxed = true)
        thumbnailCapture = mockk(relaxed = true)
        webViewStateManager = mockk(relaxed = true)
        coEvery { tabDao.getActiveTab() } returns flowOf(null)
        tabManager = TabManager(tabDao, thumbnailCapture, webViewStateManager)
    }
    
    @Test
    fun `createTab creates new tab with unique ID`() = runTest {
        // Arrange
        coEvery { tabDao.getTabCount() } returns 5
        
        // Act
        val tab = tabManager.createTab("https://example.com", "Example")
        
        // Assert
        assertEquals("https://example.com", tab.url)
        assertEquals("Example", tab.title)
        assertEquals(true, tab.isActive)
        coVerify { tabDao.deactivateAllTabs() }
        coVerify { tabDao.insert(any()) }
    }
    
    @Test
    fun `createTab enforces 20 tab limit`() = runTest {
        // Arrange
        val oldestTab = TabEntity(
            id = "old-tab",
            url = "https://old.com",
            title = "Old",
            thumbnailPath = null,
            isActive = false,
            timestamp = System.currentTimeMillis() - 10000
        )
        coEvery { tabDao.getTabCount() } returns 20
        coEvery { tabDao.getOldestInactiveTab() } returns oldestTab
        
        // Act
        tabManager.createTab("https://new.com", "New")
        
        // Assert
        coVerify { tabDao.delete("old-tab") }
        coVerify { tabDao.insert(any()) }
    }
    
    @Test
    fun `switchTab deactivates all tabs and activates selected tab`() = runTest {
        // Arrange
        val tabId = "test-tab-id"
        
        // Act
        tabManager.switchTab(tabId)
        
        // Assert
        coVerify { tabDao.deactivateAllTabs() }
        coVerify { tabDao.setActiveTab(tabId) }
    }
    
    @Test
    fun `closeTab deletes tab from database`() = runTest {
        // Arrange
        val tabId = "test-tab-id"
        
        // Act
        tabManager.closeTab(tabId)
        
        // Assert
        coVerify { tabDao.delete(tabId) }
    }
    
    @Test
    fun `getTabCount returns correct count`() = runTest {
        // Arrange
        coEvery { tabDao.getTabCount() } returns 15
        
        // Act
        val count = tabManager.getTabCount()
        
        // Assert
        assertEquals(15, count)
    }
}
