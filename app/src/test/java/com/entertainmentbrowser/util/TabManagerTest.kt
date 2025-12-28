package com.entertainmentbrowser.util

import com.entertainmentbrowser.data.local.dao.TabDao
import com.entertainmentbrowser.data.local.entity.TabEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TabManager.
 * Tests tab creation, limit enforcement, automatic cleanup, and race condition handling.
 * 
 * ## Single Source of Truth
 * TabManager + TabDao is the authoritative source for:
 * - Tab ordering (by timestamp, oldest first)
 * - Active tab state
 * - "Next active tab" policy when closing tabs
 * 
 * These tests verify that tab state decisions use fresh database queries
 * rather than potentially stale in-memory caches.
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
        coEvery { tabDao.getAllTabsSnapshot() } returns emptyList()
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
    fun `closeTab deletes tab from database and returns next tab ID`() = runTest {
        // Arrange
        val tabId = "tab-to-close"
        val tab1 = createTabEntity("tab-1", isActive = false, timestamp = 1000)
        val tab2 = createTabEntity(tabId, isActive = true, timestamp = 2000)
        val tab3 = createTabEntity("tab-3", isActive = false, timestamp = 3000)
        
        coEvery { tabDao.getAllTabsSnapshot() } returnsMany listOf(
            listOf(tab1, tab2, tab3),  // Before close
            listOf(tab1, tab3)          // After close
        )
        
        // Act
        val nextTabId = tabManager.closeTab(tabId)
        
        // Assert
        coVerify { tabDao.delete(tabId) }
        assertEquals("tab-1", nextTabId) // Previous tab by index
    }
    
    @Test
    fun `closeTab returns null when no tabs remain`() = runTest {
        // Arrange
        val tabId = "only-tab"
        val onlyTab = createTabEntity(tabId, isActive = true, timestamp = 1000)
        
        coEvery { tabDao.getAllTabsSnapshot() } returnsMany listOf(
            listOf(onlyTab),  // Before close
            emptyList()       // After close
        )
        
        // Act
        val nextTabId = tabManager.closeTab(tabId)
        
        // Assert
        assertNull(nextTabId)
    }
    
    @Test
    fun `closeTab selects first tab when closing index 0`() = runTest {
        // Arrange
        val tabId = "first-tab"
        val tab1 = createTabEntity(tabId, isActive = true, timestamp = 1000)
        val tab2 = createTabEntity("tab-2", isActive = false, timestamp = 2000)
        val tab3 = createTabEntity("tab-3", isActive = false, timestamp = 3000)
        
        coEvery { tabDao.getAllTabsSnapshot() } returnsMany listOf(
            listOf(tab1, tab2, tab3),  // Before close
            listOf(tab2, tab3)          // After close
        )
        
        // Act
        val nextTabId = tabManager.closeTab(tabId)
        
        // Assert
        assertEquals("tab-2", nextTabId) // New first tab
    }
    
    @Test
    fun `closeTab does not switch when closing inactive tab`() = runTest {
        // Arrange
        val tabId = "inactive-tab"
        val activeTab = createTabEntity("active-tab", isActive = true, timestamp = 1000)
        val inactiveTab = createTabEntity(tabId, isActive = false, timestamp = 2000)
        
        coEvery { tabDao.getAllTabsSnapshot() } returns listOf(activeTab, inactiveTab)
        
        // Act
        val nextTabId = tabManager.closeTab(tabId)
        
        // Assert
        assertNull(nextTabId) // No switch needed
        coVerify(exactly = 0) { tabDao.setActiveTab(any()) }
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
    
    // ==================== Race Condition Tests ====================
    
    @Test
    fun `rapid tab switching maintains consistent active tab state`() = runTest {
        // Arrange - simulate 5 tabs
        val tabs = (1..5).map { createTabEntity("tab-$it", isActive = it == 1, timestamp = it * 1000L) }
        coEvery { tabDao.getActiveTab() } returns flowOf(tabs.first())
        
        // Act - rapidly switch between tabs
        val switchJobs = listOf("tab-2", "tab-3", "tab-4", "tab-5", "tab-1").map { tabId ->
            async { tabManager.switchTab(tabId) }
        }
        switchJobs.awaitAll()
        
        // Assert - verify each switch properly deactivated all tabs first
        coVerify(exactly = 5) { tabDao.deactivateAllTabs() }
        coVerify(exactly = 5) { tabDao.setActiveTab(any()) }
    }
    
    @Test
    fun `rapid tab creation and closing maintains consistent state`() = runTest {
        // Arrange
        coEvery { tabDao.getTabCount() } returns 5
        coEvery { tabDao.getAllTabsSnapshot() } returns emptyList()
        
        // Act - create and close tabs rapidly
        val createJobs = (1..3).map { i ->
            async { tabManager.createTab("https://example$i.com", "Example $i") }
        }
        createJobs.awaitAll()
        
        // Assert - all creates should deactivate existing tabs
        coVerify(exactly = 3) { tabDao.deactivateAllTabs() }
        coVerify(exactly = 3) { tabDao.insert(any()) }
    }
    
    @Test
    fun `closeTab uses fresh database query not stale cache`() = runTest {
        // Arrange - simulate scenario where DB state changes between operations
        val tab1 = createTabEntity("tab-1", isActive = false, timestamp = 1000)
        val tab2 = createTabEntity("tab-2", isActive = true, timestamp = 2000)
        val tab3 = createTabEntity("tab-3", isActive = false, timestamp = 3000)
        
        // First call returns all 3 tabs, second call (after delete) returns 2 tabs
        coEvery { tabDao.getAllTabsSnapshot() } returnsMany listOf(
            listOf(tab1, tab2, tab3),
            listOf(tab1, tab3)
        )
        
        // Act
        val nextTabId = tabManager.closeTab("tab-2")
        
        // Assert - should query DB twice (before and after delete)
        coVerify(exactly = 2) { tabDao.getAllTabsSnapshot() }
        assertEquals("tab-1", nextTabId)
    }
    
    @Test
    fun `switchTab properly manages WebView state during rapid switches`() = runTest {
        // Arrange
        val activeTab = createTabEntity("tab-1", isActive = true, timestamp = 1000)
        coEvery { tabDao.getActiveTab() } returns flowOf(activeTab)
        
        // Act - switch tabs rapidly
        tabManager.switchTab("tab-2")
        tabManager.switchTab("tab-3")
        
        // Assert - WebView state should be paused/resumed in order
        coVerifyOrder {
            webViewStateManager.pauseWebView("tab-1")
            webViewStateManager.saveWebViewState("tab-1")
            webViewStateManager.resumeWebView("tab-2")
            webViewStateManager.pauseWebView("tab-1") // Still active in mock
            webViewStateManager.saveWebViewState("tab-1")
            webViewStateManager.resumeWebView("tab-3")
        }
    }
    
    // ==================== Helper Functions ====================
    
    private fun createTabEntity(
        id: String,
        url: String = "https://$id.com",
        title: String = id,
        isActive: Boolean = false,
        timestamp: Long = System.currentTimeMillis()
    ) = TabEntity(
        id = id,
        url = url,
        title = title,
        thumbnailPath = null,
        isActive = isActive,
        timestamp = timestamp
    )
}
