package com.entertainmentbrowser.domain.usecase

import com.entertainmentbrowser.domain.repository.TabRepository
import javax.inject.Inject

class SwitchTabUseCase @Inject constructor(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(tabId: String) {
        tabRepository.switchTab(tabId)
    }
}
