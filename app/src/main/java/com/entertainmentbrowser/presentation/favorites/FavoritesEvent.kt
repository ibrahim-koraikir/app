package com.entertainmentbrowser.presentation.favorites

/**
 * Events that can be triggered from the Favorites screen
 */
sealed class FavoritesEvent {
    /**
     * User toggled favorite status of a website
     */
    data class ToggleFavorite(val websiteId: Int) : FavoritesEvent()
    
    /**
     * User clicked on a website card
     */
    data class WebsiteClicked(val url: String) : FavoritesEvent()
    
    /**
     * User cleared error message
     */
    data object ClearError : FavoritesEvent()
}
