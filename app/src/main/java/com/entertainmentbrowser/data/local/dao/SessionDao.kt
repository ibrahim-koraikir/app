package com.entertainmentbrowser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.entertainmentbrowser.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    
    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>
    
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Int): SessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)
    
    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun delete(sessionId: Int)
    
    @Query("UPDATE sessions SET name = :name WHERE id = :sessionId")
    suspend fun updateName(sessionId: Int, name: String)
}
