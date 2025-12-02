package com.entertainmentbrowser.presentation.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.domain.model.Website
import com.entertainmentbrowser.domain.usecase.GetAllWebsitesUseCase
import com.entertainmentbrowser.domain.usecase.GetWebsitesByCategoryUseCase
import com.entertainmentbrowser.domain.usecase.SearchWebsitesUseCase
import com.entertainmentbrowser.domain.usecase.ToggleFavoriteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var getWebsitesByCategoryUseCase: GetWebsitesByCategoryUseCase
    private lateinit var getAllWebsitesUseCase: GetAllWebsitesUseCase
    private lateinit var searchWebsitesUseCase: SearchWebsitesUseCase
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase
    private lateinit var viewModel: HomeViewModel
    
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
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        getWebsitesByCategoryUseCase = mockk()
        getAllWebsitesUseCase = mockk()
        searchWebsitesUseCase = mockk()
        toggleFavoriteUseCase = mockk()
        
        every { getAllWebsitesUseCase() } returns flowOf(testWebsites)
        
        viewModel = HomeViewModel(
            getWebsitesByCategoryUseCase,
            getAllWebsitesUseCase,
            searchWebsitesUseCase,
            toggleFavoriteUseCase
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state loads all websites`() = runTest(testDispatcher) {
        // When
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(2, state.websites.size)
        assertFalse(state.isLoading)
    }
    
    @Test
    fun `CategorySelected event filters websites by category`() = runTest(testDispatcher) {
        // Given
        val streamingWebsites = listOf(testWebsites[0])
        every { getWebsitesByCategoryUseCase(Category.STREAMING) } returns flowOf(streamingWebsites)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(HomeEvent.CategorySelected(Category.STREAMING))
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(Category.STREAMING, state.selectedCategory)
        assertEquals(1, state.websites.size)
        assertEquals("Netflix", state.websites[0].name)
    }
    
    @Test
    fun `SearchQueryChanged event triggers debounced search`() = runTest(testDispatcher) {
        // Given
        val searchResults = listOf(testWebsites[0])
        every { searchWebsitesUseCase.searchDirect("Netflix") } returns flowOf(searchResults)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(HomeEvent.SearchQueryChanged("Netflix"))
        advanceTimeBy(300) // Wait for debounce
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Netflix", state.searchQuery)
        assertEquals(1, state.websites.size)
    }
    
    @Test
    fun `search debounce prevents rapid queries`() = runTest(testDispatcher) {
        // Given
        every { searchWebsitesUseCase.searchDirect(any()) } returns flowOf(emptyList())
        advanceUntilIdle()
        
        // When - Type multiple characters rapidly
        viewModel.onEvent(HomeEvent.SearchQueryChanged("N"))
        advanceTimeBy(100)
        viewModel.onEvent(HomeEvent.SearchQueryChanged("Ne"))
        advanceTimeBy(100)
        viewModel.onEvent(HomeEvent.SearchQueryChanged("Net"))
        advanceTimeBy(300) // Wait for debounce
        advanceUntilIdle()
        
        // Then - Only one search should be triggered
        coVerify(exactly = 1) { searchWebsitesUseCase.searchDirect("Net") }
    }
    
    @Test
    fun `ToggleFavorite event calls use case`() = runTest(testDispatcher) {
        // Given
        coEvery { toggleFavoriteUseCase(1) } returns Unit
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(HomeEvent.ToggleFavorite(1))
        advanceUntilIdle()
        
        // Then
        coVerify { toggleFavoriteUseCase(1) }
    }
    
    @Test
    fun `clearing search query reloads all websites`() = runTest(testDispatcher) {
        // Given
        every { searchWebsitesUseCase.searchDirect("test") } returns flowOf(emptyList())
        advanceUntilIdle()
        
        viewModel.onEvent(HomeEvent.SearchQueryChanged("test"))
        advanceTimeBy(300)
        advanceUntilIdle()
        
        // When - Clear search
        viewModel.onEvent(HomeEvent.SearchQueryChanged(""))
        advanceTimeBy(300)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(2, state.websites.size) // All websites restored
    }
    
    @Test
    fun `ClearError event clears error state`() = runTest(testDispatcher) {
        // Given
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(HomeEvent.ClearError)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(null, state.error)
    }
}
