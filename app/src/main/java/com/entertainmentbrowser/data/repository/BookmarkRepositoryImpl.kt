package com.entertainmentbrowser.data.repository

import com.entertainmentbrowser.data.local.dao.BookmarkDao
import com.entertainmentbrowser.data.local.entity.BookmarkEntity
import com.entertainmentbrowser.domain.model.Bookmark
import com.entertainmentbrowser.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {
    
    override fun getAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getBookmarkById(id: Int): Bookmark? {
        return bookmarkDao.getBookmarkById(id)?.toDomain()
    }
    
    override suspend fun getBookmarkByUrl(url: String): Bookmark? {
        return bookmarkDao.getBookmarkByUrl(url)?.toDomain()
    }
    
    override suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.isBookmarked(url)
    }
    
    override suspend fun addBookmark(title: String, url: String, favicon: String?): Long {
        val entity = BookmarkEntity(
            title = title,
            url = url,
            favicon = favicon,
            createdAt = System.currentTimeMillis()
        )
        return bookmarkDao.insertBookmark(entity)
    }
    
    override suspend fun deleteBookmark(id: Int) {
        bookmarkDao.deleteBookmarkById(id)
    }
    
    override suspend fun deleteBookmarkByUrl(url: String) {
        bookmarkDao.deleteBookmarkByUrl(url)
    }
    
    override suspend fun toggleBookmark(title: String, url: String, favicon: String?): Boolean {
        val isCurrentlyBookmarked = bookmarkDao.isBookmarked(url)
        if (isCurrentlyBookmarked) {
            bookmarkDao.deleteBookmarkByUrl(url)
            return false
        } else {
            addBookmark(title, url, favicon)
            return true
        }
    }
    
    private fun BookmarkEntity.toDomain(): Bookmark {
        return Bookmark(
            id = id,
            title = title,
            url = url,
            favicon = favicon,
            createdAt = createdAt
        )
    }
}
