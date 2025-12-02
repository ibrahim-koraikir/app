package com.entertainmentbrowser.domain.repository

import com.entertainmentbrowser.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessions(): Flow<List<Session>>
    suspend fun createSession(name: String, tabIds: List<String>)
    suspend fun restoreSession(sessionId: Int): List<String>
    suspend fun deleteSession(sessionId: Int)
    suspend fun renameSession(sessionId: Int, newName: String)
}
