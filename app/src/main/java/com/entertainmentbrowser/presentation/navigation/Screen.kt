package com.entertainmentbrowser.presentation.navigation

import android.net.Uri
import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Sealed class representing all navigation destinations in the app.
 * Each screen has a route pattern and optional arguments.
 */
sealed class Screen(val route: String) {
    
    /**
     * Onboarding flow screen shown to first-time users
     */
    data object Onboarding : Screen("onboarding")
    
    /**
     * Home screen displaying website catalog organized by categories
     */
    data object Home : Screen("home")
    
    /**
     * WebView screen for browsing websites
     * @param url The website URL to load (URL encoded)
     */
    data object WebView : Screen("webview/{url}") {
        const val ARG_URL = "url"
        const val ACTIVE_TAB_MARKER = "__ACTIVE_TAB__"
        
        fun createRoute(url: String): String {
            val encodedUrl = Uri.encode(url.trim())
            return "webview/$encodedUrl"
        }
        
        /**
         * Creates a route to show the active tab without creating a new one
         */
        fun createActiveTabRoute(): String {
            return "webview/$ACTIVE_TAB_MARKER"
        }
        
        val arguments = listOf(
            navArgument(ARG_URL) {
                type = NavType.StringType
            }
        )
    }
    
    /**
     * Downloads screen for managing video downloads
     */
    data object Downloads : Screen("downloads")
    
    /**
     * Tabs screen for managing browsing tabs
     */
    data object Tabs : Screen("tabs")
    
    /**
     * Sessions screen for managing saved tab collections
     */
    data object Sessions : Screen("sessions")
    
    /**
     * Settings screen for app configuration
     */
    data object Settings : Screen("settings")
    
    /**
     * Bookmarks screen for managing saved web pages
     */
    data object Bookmarks : Screen("bookmarks")
}

/**
 * Deep link URI patterns for the app
 */
object DeepLink {
    const val SCHEME = "entertainmentbrowser"
    const val HOST = "app"
    
    /**
     * Deep link to open a specific website
     * Format: entertainmentbrowser://app/webview?url={url}
     */
    const val WEBVIEW = "$SCHEME://$HOST/webview?url={url}"
    
    /**
     * Creates a deep link URI for opening a website
     */
    fun createWebViewDeepLink(url: String): String {
        val encodedUrl = Uri.encode(url)
        return "$SCHEME://$HOST/webview?url=$encodedUrl"
    }
}
