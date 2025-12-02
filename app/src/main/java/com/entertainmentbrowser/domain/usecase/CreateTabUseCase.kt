package com.entertainmentbrowser.domain.usecase

import com.entertainmentbrowser.domain.model.Tab
import com.entertainmentbrowser.domain.repository.TabRepository
import javax.inject.Inject

class CreateTabUseCase @Inject constructor(
    private val tabRepository: TabRepository
) {
    suspend operator fun invoke(url: String, title: String): Tab {
        // Check tab count and enforce 20 tab limit
        val currentCount = tabRepository.getTabCount()
        if (currentCount >= 20) {
            // Tab limit will be enforced in TabManager (task 11)
            // For now, just create the tab
        }
        return tabRepository.createTab(url, title)
    }
}
