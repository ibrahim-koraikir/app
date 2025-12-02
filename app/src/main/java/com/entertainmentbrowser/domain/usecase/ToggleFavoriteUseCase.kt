package com.entertainmentbrowser.domain.usecase

import com.entertainmentbrowser.domain.repository.WebsiteRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val websiteRepository: WebsiteRepository
) {
    suspend operator fun invoke(websiteId: Int) {
        websiteRepository.toggleFavorite(websiteId)
    }
}
