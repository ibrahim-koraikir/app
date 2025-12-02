package com.entertainmentbrowser.presentation.favorites

import com.entertainmentbrowser.domain.model.Website

/**
 * UI state for the Favorites screen
 */
data class FavoritesUiState(
    val favorites: List<Website> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
