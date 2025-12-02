package com.entertainmentbrowser.domain.repository

import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.domain.model.Website
import kotlinx.coroutines.flow.Flow

interface WebsiteRepository {
    fun getByCategory(category: Category): Flow<List<Website>>
    fun getFavorites(): Flow<List<Website>>
    fun search(query: String): Flow<List<Website>>
    suspend fun toggleFavorite(id: Int)
    suspend fun prepopulateWebsites()
}
