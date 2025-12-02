package com.entertainmentbrowser.domain.usecase

import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.domain.model.Website
import com.entertainmentbrowser.domain.repository.WebsiteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetWebsitesByCategoryUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository
) {
    operator fun invoke(category: Category): Flow<List<Website>> {
        return websiteRepository.getByCategory(category)
    }
}
