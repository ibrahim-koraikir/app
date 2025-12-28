package com.entertainmentbrowser.presentation.home

import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.domain.model.SearchEngine
import com.entertainmentbrowser.domain.model.Website

/**
 * UI state for the Home screen
 */
data class HomeUiState(
    val websites: List<Website> = emptyList(),
    val selectedCategory: Category = Category.STREAMING,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    /** One-time navigation URL from search bar submission. Null when consumed. */
    val navigationUrl: String? = null,
    /** One-time snackbar message. Null when consumed. */
    val snackbarMessage: String? = null,
    /** Active tab URL for edge swipe navigation. Null when no active tab. */
    val activeTabUrl: String? = null
)
