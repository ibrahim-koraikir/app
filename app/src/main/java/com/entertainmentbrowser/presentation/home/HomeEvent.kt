package com.entertainmentbrowser.presentation.home

import com.entertainmentbrowser.domain.model.Category

/**
 * Events that can be triggered from the Home screen
 */
sealed class HomeEvent {
    /**
     * User selected a different category tab
     */
    data class CategorySelected(val category: Category) : HomeEvent()
    
    /**
     * User entered search query
     */
    data class SearchQueryChanged(val query: String) : HomeEvent()
    
    /**
     * User toggled favorite status of a website
     */
    data class ToggleFavorite(val websiteId: Int) : HomeEvent()
    
    /**
     * User clicked on a website card
     */
    data class WebsiteClicked(val url: String) : HomeEvent()
    
    /**
     * User submitted search bar input (URL or search query)
     */
    data class SearchBarSubmit(val input: String) : HomeEvent()
    
    /**
     * Navigation event consumed
     */
    data object NavigationConsumed : HomeEvent()
    
    /**
     * User pulled to refresh
     */
    data object Refresh : HomeEvent()
    
    /**
     * User cleared error message
     */
    data object ClearError : HomeEvent()
    
    /**
     * User long-pressed a website card to open in new tab
     */
    data class OpenInNewTab(val url: String, val title: String) : HomeEvent()
    
    /**
     * Snackbar message consumed
     */
    data object SnackbarConsumed : HomeEvent()
}
