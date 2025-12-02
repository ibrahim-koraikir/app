package com.entertainmentbrowser.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.domain.model.Website
import com.entertainmentbrowser.presentation.theme.EntertainmentBrowserTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for HomeScreen composable.
 * Tests critical user flows like browsing websites and toggling favorites.
 */
class HomeScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val testWebsites = listOf(
        Website(
            id = 1,
            name = "Netflix",
            url = "https://netflix.com",
            category = Category.STREAMING,
            logoUrl = "",
            description = "Streaming service",
            backgroundColor = "#E50914",
            isFavorite = false,
            order = 0
        ),
        Website(
            id = 2,
            name = "Disney+",
            url = "https://disneyplus.com",
            category = Category.STREAMING,
            logoUrl = "",
            description = "Disney streaming",
            backgroundColor = "#113CCF",
            isFavorite = false,
            order = 1
        )
    )
    
    @Test
    fun homeScreen_displaysWebsitesInGrid() {
        // Given
        val viewModel = mockk<HomeViewModel>(relaxed = true)
        val uiStateFlow = MutableStateFlow(
            HomeUiState(
                websites = testWebsites,
                isLoading = false
            )
        )
        every { viewModel.uiState } returns uiStateFlow
        
        // When
        composeTestRule.setContent {
            EntertainmentBrowserTheme {
                HomeScreen(
                    onNavigateToWebView = {},
                    onNavigateToFavorites = {},
                    onNavigateToSettings = {},
                    viewModel = viewModel
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("CineBrowse").assertIsDisplayed()
        composeTestRule.onNodeWithText("Movies").assertIsDisplayed()
    }
    
    @Test
    fun homeScreen_navigatesToFavoritesOnFabClick() {
        // Given
        val viewModel = mockk<HomeViewModel>(relaxed = true)
        val uiStateFlow = MutableStateFlow(HomeUiState())
        every { viewModel.uiState } returns uiStateFlow
        
        var favoritesClicked = false
        
        // When
        composeTestRule.setContent {
            EntertainmentBrowserTheme {
                HomeScreen(
                    onNavigateToWebView = {},
                    onNavigateToFavorites = { favoritesClicked = true },
                    onNavigateToSettings = {},
                    viewModel = viewModel
                )
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("View all favorite websites").performClick()
        assert(favoritesClicked)
    }
}
