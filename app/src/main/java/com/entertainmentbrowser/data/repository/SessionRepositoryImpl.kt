package com.entertainmentbrowser.data.repository

import com.entertainmentbrowser.data.local.dao.SessionDao
import com.entertainmentbrowser.data.local.entity.SessionEntity
import com.entertainmentbrowser.domain.model.Session
import com.entertainmentbrowser.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {
    
    override fun getAllSessions(): Flow<List<Session>> {
        return sessionDao.getAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun createSession(name: String, tabIds: List<String>) {
        val session = SessionEntity(
            name = name,
            tabIdsJson = Json.encodeToString(tabIds),
            createdAt = System.currentTimeMillis()
        )
        sessionDao.insert(session)
    }
    
    override suspend fun restoreSession(sessionId: Int): List<String> {
        val session = sessionDao.getSessionById(sessionId) ?: return emptyList()
        return Json.decodeFromString(session.tabIdsJson)
    }
    
    override suspend fun deleteSession(sessionId: Int) {
        sessionDao.delete(sessionId)
    }
    
    override suspend fun renameSession(sessionId: Int, newName: String) {
        sessionDao.updateName(sessionId, newName)
    }
    
    private fun SessionEntity.toDomain() = Session(
        id = id,
        name = name,
        tabIds = Json.decodeFromString(tabIdsJson),
        createdAt = createdAt
    )
}
