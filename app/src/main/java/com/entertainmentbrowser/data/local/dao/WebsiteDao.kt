package com.entertainmentbrowser.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.entertainmentbrowser.data.local.entity.WebsiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebsiteDao {
    
    @Query("SELECT * FROM websites WHERE category = :category ORDER BY `order` ASC")
    fun getByCategory(category: String): Flow<List<WebsiteEntity>>
    
    @Query("SELECT * FROM websites WHERE isFavorite = 1 ORDER BY `order` ASC")
    fun getFavorites(): Flow<List<WebsiteEntity>>
    
    @Query("SELECT * FROM websites WHERE name LIKE '%' || :query || '%' ORDER BY `order` ASC")
    fun search(query: String): Flow<List<WebsiteEntity>>
    
    @Query("SELECT * FROM websites WHERE id = :id")
    suspend fun getById(id: Int): WebsiteEntity?
    
    @Update
    suspend fun update(website: WebsiteEntity)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(website: WebsiteEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(websites: List<WebsiteEntity>)
    
    @Query("SELECT COUNT(*) FROM websites")
    suspend fun getCount(): Int
    
    @Query("SELECT id FROM websites")
    suspend fun getAllIds(): List<Int>
}
