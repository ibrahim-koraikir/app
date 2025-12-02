package com.entertainmentbrowser.domain.usecase

import com.entertainmentbrowser.domain.model.Website
import com.entertainmentbrowser.domain.repository.WebsiteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.entertainmentbrowser.domain.model.Category
import javax.inject.Inject

class GetAllWebsitesUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository
) {
    operator fun invoke(): Flow<List<Website>> {
        // Combine all categories into a single flow
        return combine(
            websiteRepository.getByCategory(Category.STREAMING),
            websiteRepository.getByCategory(Category.TV_SHOWS),
            websiteRepository.getByCategory(Category.BOOKS),
            websiteRepository.getByCategory(Category.VIDEO_PLATFORMS),
            websiteRepository.getByCategory(Category.SOCIAL_MEDIA),
            websiteRepository.getByCategory(Category.GAMES),
            websiteRepository.getByCategory(Category.VIDEO_CALL),
            websiteRepository.getByCategory(Category.ARABIC)
        ) { arrays ->
            arrays.flatMap { it.toList() }
        }
    }
}
