package com.entertainmentbrowser.domain.usecase

import com.entertainmentbrowser.domain.repository.TabRepository
import javax.inject.Inject

class CloseTabUseCase @Inject constructor(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(tabId: String) {
        tabRepository.closeTab(tabId)
    }
}
