package com.entertainmentbrowser.data.repository

import com.entertainmentbrowser.data.local.PrepopulateData
import com.entertainmentbrowser.data.local.dao.WebsiteDao
import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.domain.model.Website
import com.entertainmentbrowser.domain.repository.WebsiteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton

class WebsiteRepositoryImpl @Inject constructor(
    private val websiteDao: WebsiteDao
) : WebsiteRepository {
    
    override fun getByCategory(category: Category): Flow<List<Website>> {
        return websiteDao.getByCategory(category.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getFavorites(): Flow<List<Website>> {
        return websiteDao.getFavorites().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun search(query: String): Flow<List<Website>> {
        return websiteDao.search(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun toggleFavorite(id: Int) {
        val website = websiteDao.getById(id) ?: return
        websiteDao.update(website.copy(isFavorite = !website.isFavorite))
    }
    
    override suspend fun prepopulateWebsites() {
        val websites = PrepopulateData.getWebsites()
        val count = websiteDao.getCount()
        
        android.util.Log.d("WebsiteRepo", "Prepopulate: count=$count, total=${websites.size}")
        
        if (count == 0) {
            // First time - insert all websites
            websiteDao.insertAll(websites)
            android.util.Log.d("WebsiteRepo", "Inserted all ${websites.size} websites")
        } else {
            // Check for new websites and add them
            var added = 0
            websites.forEach { website ->
                val existing = websiteDao.getById(website.id)
                if (existing == null) {
                    websiteDao.insert(website)
                    added++
                    android.util.Log.d("WebsiteRepo", "Added new website: ${website.name} (id=${website.id})")
                }
            }
            android.util.Log.d("WebsiteRepo", "Added $added new websites")
        }
    }
    
    private fun com.entertainmentbrowser.data.local.entity.WebsiteEntity.toDomain() = Website(
        id = id,
        name = name,
        url = url,
        category = Category.valueOf(category),
        logoUrl = logoUrl,
        description = description,
        backgroundColor = backgroundColor,
        isFavorite = isFavorite,
        order = order
    )
}
