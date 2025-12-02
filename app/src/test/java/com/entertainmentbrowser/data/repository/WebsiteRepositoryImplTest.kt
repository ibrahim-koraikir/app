package com.entertainmentbrowser.data.repository

import com.entertainmentbrowser.data.local.dao.WebsiteDao
import com.entertainmentbrowser.data.local.entity.WebsiteEntity
import com.entertainmentbrowser.domain.model.Category
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WebsiteRepositoryImplTest {
    
    private lateinit var websiteDao: WebsiteDao
    private lateinit var repository: WebsiteRepositoryImpl
    
    @Before
    fun setup() {
        websiteDao = mockk()
        repository = WebsiteRepositoryImpl(websiteDao)
    }
    
    @Test
    fun `getByCategory returns correct websites`() = runTest {
        // Given
        val entities = listOf(
            WebsiteEntity(
                id = 1,
                name = "Netflix",
                url = "https://netflix.com",
                category = "STREAMING",
                logoUrl = "",
                description = "Streaming service",
                backgroundColor = "#E50914",
                isFavorite = false,
                order = 0
            )
        )
        every { websiteDao.getByCategory("STREAMING") } returns flowOf(entities)
        
        // When
        val result = repository.getByCategory(Category.STREAMING).first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals("Netflix", result[0].name)
        assertEquals(Category.STREAMING, result[0].category)
    }
    
    @Test
    fun `getFavorites returns only favorited websites`() = runTest {
        // Given
        val entities = listOf(
            WebsiteEntity(
                id = 1,
                name = "Netflix",
                url = "https://netflix.com",
                category = "STREAMING",
                logoUrl = "",
                description = "Streaming service",
                backgroundColor = "#E50914",
                isFavorite = true,
                order = 0
            )
        )
        every { websiteDao.getFavorites() } returns flowOf(entities)
        
        // When
        val result = repository.getFavorites().first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(true, result[0].isFavorite)
    }
    
    @Test
    fun `search returns matching websites`() = runTest {
        // Given
        val query = "Netflix"
        val entities = listOf(
            WebsiteEntity(
                id = 1,
                name = "Netflix",
                url = "https://netflix.com",
                category = "STREAMING",
                logoUrl = "",
                description = "Streaming service",
                backgroundColor = "#E50914",
                isFavorite = false,
                order = 0
            )
        )
        every { websiteDao.search(query) } returns flowOf(entities)
        
        // When
        val result = repository.search(query).first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals("Netflix", result[0].name)
    }
    
    @Test
    fun `toggleFavorite updates favorite status`() = runTest {
        // Given
        val websiteId = 1
        val entity = WebsiteEntity(
            id = websiteId,
            name = "Netflix",
            url = "https://netflix.com",
            category = "STREAMING",
            logoUrl = "",
            description = "Streaming service",
            backgroundColor = "#E50914",
            isFavorite = false,
            order = 0
        )
        coEvery { websiteDao.getById(websiteId) } returns entity
        coEvery { websiteDao.update(any()) } returns Unit
        
        // When
        repository.toggleFavorite(websiteId)
        
        // Then
        coVerify { websiteDao.update(entity.copy(isFavorite = true)) }
    }
    
    @Test
    fun `prepopulateWebsites inserts data when database is empty`() = runTest {
        // Given
        coEvery { websiteDao.getCount() } returns 0
        coEvery { websiteDao.insertAll(any()) } returns Unit
        
        // When
        repository.prepopulateWebsites()
        
        // Then
        coVerify { websiteDao.insertAll(any()) }
    }
    
    @Test
    fun `prepopulateWebsites does not insert when database has data`() = runTest {
        // Given
        coEvery { websiteDao.getCount() } returns 10
        
        // When
        repository.prepopulateWebsites()
        
        // Then
        coVerify(exactly = 0) { websiteDao.insertAll(any()) }
    }
}
