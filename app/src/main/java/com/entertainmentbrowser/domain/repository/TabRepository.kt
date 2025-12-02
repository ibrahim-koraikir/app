package com.entertainmentbrowser.domain.repository

import com.entertainmentbrowser.domain.model.Tab
import kotlinx.coroutines.flow.Flow

interface TabRepository {
    fun getAllTabs(): Flow<List<Tab>>
    fun getActiveTab(): Flow<Tab?>
    suspend fun createTab(url: String, title: String): Tab
    suspend fun switchTab(tabId: String)
    suspend fun closeTab(tabId: String)
    suspend fun closeAllTabs()
    suspend fun getTabCount(): Int
    suspend fun updateTabThumbnail(tabId: String, thumbnailPath: String)
    suspend fun updateTabUrl(tabId: String, url: String)
}
