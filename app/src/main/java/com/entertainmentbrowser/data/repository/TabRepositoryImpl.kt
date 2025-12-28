package com.entertainmentbrowser.data.repository

import com.entertainmentbrowser.data.local.dao.TabDao
import com.entertainmentbrowser.data.local.entity.TabEntity
import com.entertainmentbrowser.domain.model.Tab
import com.entertainmentbrowser.domain.repository.TabRepository
import com.entertainmentbrowser.util.TabManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of TabRepository.
 * 
 * ## Single Source of Truth
 * This repository delegates to TabDao for all tab state queries.
 * The database is the authoritative source - avoid caching tab lists
 * in memory as they may become stale during rapid operations.
 */
class TabRepositoryImpl @Inject constructor(
    private val tabDao: TabDao,
    private val tabManager: TabManager
) : TabRepository {
    
    override fun getAllTabs(): Flow<List<Tab>> {
        return tabDao.getAllTabs().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getActiveTab(): Flow<Tab?> {
        return tabDao.getActiveTab().map { it?.toDomain() }
    }
    
    override suspend fun getTabById(tabId: String): Tab? {
        return tabDao.getTabById(tabId)?.toDomain()
    }
    
    override suspend fun getAllTabsSnapshot(): List<Tab> {
        return tabDao.getAllTabsSnapshot().map { it.toDomain() }
    }
    
    override suspend fun createTab(url: String, title: String): Tab {
        val tab = tabManager.createTab(url, title)
        return tab.toDomain()
    }
    
    override suspend fun createTabInBackground(url: String, title: String): Tab {
        val tab = tabManager.createTabInBackground(url, title)
        return tab.toDomain()
    }
    
    override suspend fun switchTab(tabId: String) {
        tabManager.switchTab(tabId)
    }
    
    override suspend fun closeTab(tabId: String): String? {
        return tabManager.closeTab(tabId)
    }
    
    override suspend fun closeAllTabs() {
        tabDao.deleteAllTabs()
    }
    
    override suspend fun getTabCount(): Int {
        return tabDao.getTabCount()
    }
    
    override suspend fun updateTabThumbnail(tabId: String, thumbnailPath: String) {
        tabDao.updateThumbnail(tabId, thumbnailPath)
    }
    
    override suspend fun updateTabUrl(tabId: String, url: String) {
        tabDao.updateUrl(tabId, url)
    }
    
    private fun TabEntity.toDomain() = Tab(
        id = id,
        url = url,
        title = title,
        thumbnailPath = thumbnailPath,
        isActive = isActive,
        timestamp = timestamp
    )
}
