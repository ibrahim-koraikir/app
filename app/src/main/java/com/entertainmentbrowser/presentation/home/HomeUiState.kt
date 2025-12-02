package com.entertainmentbrowser.presentation.home

import com.entertainmentbrowser.domain.model.Category
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
    val isRefreshing: Boolean = false
)
