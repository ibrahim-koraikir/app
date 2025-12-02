package com.entertainmentbrowser.domain.usecase

import com.entertainmentbrowser.domain.model.Website
import com.entertainmentbrowser.domain.repository.WebsiteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class SearchWebsitesUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository
) {
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    operator fun invoke(query: Flow<String>): Flow<List<Website>> {
        return query
            .debounce(300) // 300ms debounce as per requirements
            .flatMapLatest { searchQuery ->
                websiteRepository.search(searchQuery)
            }
    }
    
    // Alternative method for direct search without debouncing
    fun searchDirect(query: String): Flow<List<Website>> {
        return websiteRepository.search(query)
    }
}
