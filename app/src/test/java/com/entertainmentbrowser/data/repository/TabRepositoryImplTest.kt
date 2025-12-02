package com.entertainmentbrowser.data.repository

import com.entertainmentbrowser.data.local.dao.TabDao
import com.entertainmentbrowser.data.local.entity.TabEntity
import com.entertainmentbrowser.domain.model.Tab
import com.entertainmentbrowser.util.TabManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TabRepositoryImplTest {
    
    private lateinit var tabDao: TabDao
    private lateinit var tabManager: TabManager
    private lateinit var repository: TabRepositoryImpl
    
    @Before
    fun setup() {
        tabDao = mockk()
        tabManager = mockk()
        repository = TabRepositoryImpl(tabDao, tabManager)
    }
    
    @Test
    fun `getAllTabs returns all tabs`() = runTest {
        // Given
        val entities = listOf(
            TabEntity(
                id = "tab1",
                url = "https://netflix.com",
                title = "Netflix",
                thumbnailPath = null,
                isActive = true,
                timestamp = System.currentTimeMillis()
            ),
            TabEntity(
                id = "tab2",
                url = "https://disney.com",
                title = "Disney+",
                thumbnailPath = null,
                isActive = false,
                timestamp = System.currentTimeMillis()
            )
        )
        every { tabDao.getAllTabs() } returns flowOf(entities)
        
        // When
        val result = repository.getAllTabs().first()
        
        // Then
        assertEquals(2, result.size)
        assertEquals("tab1", result[0].id)
        assertEquals("tab2", result[1].id)
    }
    
    @Test
    fun `getActiveTab returns active tab`() = runTest {
        // Given
        val entity = TabEntity(
            id = "tab1",
            url = "https://netflix.com",
            title = "Netflix",
            thumbnailPath = null,
            isActive = true,
            timestamp = System.currentTimeMillis()
        )
        every { tabDao.getActiveTab() } returns flowOf(entity)
        
        // When
        val result = repository.getActiveTab().first()
        
        // Then
        assertEquals("tab1", result?.id)
        assertEquals(true, result?.isActive)
    }
    
    @Test
    fun `getActiveTab returns null when no active tab`() = runTest {
        // Given
        every { tabDao.getActiveTab() } returns flowOf(null)
        
        // When
        val result = repository.getActiveTab().first()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `createTab creates new tab through manager`() = runTest {
        // Given
        val url = "https://netflix.com"
        val title = "Netflix"
        val tabEntity = TabEntity(
            id = "tab1",
            url = url,
            title = title,
            thumbnailPath = null,
            isActive = true,
            timestamp = System.currentTimeMillis()
        )
        coEvery { tabManager.createTab(url, title) } returns tabEntity
        
        // When
        val result = repository.createTab(url, title)
        
        // Then
        assertEquals("tab1", result.id)
        assertEquals(url, result.url)
        assertEquals(title, result.title)
    }
    
    @Test
    fun `switchTab delegates to manager`() = runTest {
        // Given
        val tabId = "tab1"
        coEvery { tabManager.switchTab(tabId) } returns Unit
        
        // When
        repository.switchTab(tabId)
        
        // Then
        coVerify { tabManager.switchTab(tabId) }
    }
    
    @Test
    fun `closeTab delegates to manager`() = runTest {
        // Given
        val tabId = "tab1"
        coEvery { tabManager.closeTab(tabId) } returns Unit
        
        // When
        repository.closeTab(tabId)
        
        // Then
        coVerify { tabManager.closeTab(tabId) }
    }
    
    @Test
    fun `closeAllTabs deletes all tabs`() = runTest {
        // Given
        coEvery { tabDao.deleteAllTabs() } returns Unit
        
        // When
        repository.closeAllTabs()
        
        // Then
        coVerify { tabDao.deleteAllTabs() }
    }
    
    @Test
    fun `getTabCount returns correct count`() = runTest {
        // Given
        coEvery { tabDao.getTabCount() } returns 5
        
        // When
        val result = repository.getTabCount()
        
        // Then
        assertEquals(5, result)
    }
    
    @Test
    fun `updateTabThumbnail updates thumbnail path`() = runTest {
        // Given
        val tabId = "tab1"
        val thumbnailPath = "/path/to/thumbnail.jpg"
        coEvery { tabDao.updateThumbnail(tabId, thumbnailPath) } returns Unit
        
        // When
        repository.updateTabThumbnail(tabId, thumbnailPath)
        
        // Then
        coVerify { tabDao.updateThumbnail(tabId, thumbnailPath) }
    }
}
