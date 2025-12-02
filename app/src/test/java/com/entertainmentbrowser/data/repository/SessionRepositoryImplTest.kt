package com.entertainmentbrowser.data.repository

import com.entertainmentbrowser.data.local.dao.SessionDao
import com.entertainmentbrowser.data.local.entity.SessionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionRepositoryImplTest {
    
    private lateinit var sessionDao: SessionDao
    private lateinit var repository: SessionRepositoryImpl
    
    @Before
    fun setup() {
        sessionDao = mockk()
        repository = SessionRepositoryImpl(sessionDao)
    }
    
    @Test
    fun `getAllSessions returns all sessions`() = runTest {
        // Given
        val entities = listOf(
            SessionEntity(
                id = 1,
                name = "Work Session",
                tabIdsJson = """["tab1","tab2"]""",
                createdAt = System.currentTimeMillis()
            ),
            SessionEntity(
                id = 2,
                name = "Entertainment",
                tabIdsJson = """["tab3","tab4","tab5"]""",
                createdAt = System.currentTimeMillis()
            )
        )
        every { sessionDao.getAllSessions() } returns flowOf(entities)
        
        // When
        val result = repository.getAllSessions().first()
        
        // Then
        assertEquals(2, result.size)
        assertEquals("Work Session", result[0].name)
        assertEquals(2, result[0].tabIds.size)
        assertEquals("Entertainment", result[1].name)
        assertEquals(3, result[1].tabIds.size)
    }
    
    @Test
    fun `createSession saves session with serialized tab IDs`() = runTest {
        // Given
        val name = "My Session"
        val tabIds = listOf("tab1", "tab2", "tab3")
        val sessionSlot = slot<SessionEntity>()
        coEvery { sessionDao.insert(capture(sessionSlot)) } returns Unit
        
        // When
        repository.createSession(name, tabIds)
        
        // Then
        coVerify { sessionDao.insert(any()) }
        assertEquals(name, sessionSlot.captured.name)
        assertTrue(sessionSlot.captured.tabIdsJson.contains("tab1"))
        assertTrue(sessionSlot.captured.tabIdsJson.contains("tab2"))
        assertTrue(sessionSlot.captured.tabIdsJson.contains("tab3"))
    }
    
    @Test
    fun `restoreSession returns deserialized tab IDs`() = runTest {
        // Given
        val sessionId = 1
        val entity = SessionEntity(
            id = sessionId,
            name = "Test Session",
            tabIdsJson = """["tab1","tab2","tab3"]""",
            createdAt = System.currentTimeMillis()
        )
        coEvery { sessionDao.getSessionById(sessionId) } returns entity
        
        // When
        val result = repository.restoreSession(sessionId)
        
        // Then
        assertEquals(3, result.size)
        assertEquals("tab1", result[0])
        assertEquals("tab2", result[1])
        assertEquals("tab3", result[2])
    }
    
    @Test
    fun `restoreSession returns empty list when session not found`() = runTest {
        // Given
        val sessionId = 999
        coEvery { sessionDao.getSessionById(sessionId) } returns null
        
        // When
        val result = repository.restoreSession(sessionId)
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `deleteSession deletes session by ID`() = runTest {
        // Given
        val sessionId = 1
        coEvery { sessionDao.delete(sessionId) } returns Unit
        
        // When
        repository.deleteSession(sessionId)
        
        // Then
        coVerify { sessionDao.delete(sessionId) }
    }
    
    @Test
    fun `renameSession updates session name`() = runTest {
        // Given
        val sessionId = 1
        val newName = "Updated Session Name"
        coEvery { sessionDao.updateName(sessionId, newName) } returns Unit
        
        // When
        repository.renameSession(sessionId, newName)
        
        // Then
        coVerify { sessionDao.updateName(sessionId, newName) }
    }
}
