package com.entertainmentbrowser.domain.repository

import com.entertainmentbrowser.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    
    fun getAllBookmarks(): Flow<List<Bookmark>>
    
    suspend fun getBookmarkById(id: Int): Bookmark?
    
    suspend fun getBookmarkByUrl(url: String): Bookmark?
    
    suspend fun isBookmarked(url: String): Boolean
    
    suspend fun addBookmark(title: String, url: String, favicon: String? = null): Long
    
    suspend fun deleteBookmark(id: Int)
    
    suspend fun deleteBookmarkByUrl(url: String)
    
    suspend fun toggleBookmark(title: String, url: String, favicon: String? = null): Boolean
}
