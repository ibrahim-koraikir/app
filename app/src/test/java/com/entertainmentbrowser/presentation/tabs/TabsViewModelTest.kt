package com.entertainmentbrowser.presentation.tabs

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.entertainmentbrowser.domain.model.Tab
import com.entertainmentbrowser.domain.repository.TabRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TabsViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var tabRepository: TabRepository
    private lateinit var viewModel: TabsViewModel
    
    private val testTabs = listOf(
        Tab(
            id = "tab1",
            url = "https://netflix.com",
            title = "Netflix",
            thumbnailPath = null,
            isActive = true,
            timestamp = System.currentTimeMillis()
        ),
        Tab(
            id = "tab2",
            url = "https://disney.com",
            title = "Disney+",
            thumbnailPath = null,
            isActive = false,
            timestamp = System.currentTimeMillis()
        )
    )
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tabRepository = mockk()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state loads tabs successfully`() = runTest(testDispatcher) {
        // Given
        every { tabRepository.getAllTabs() } returns flowOf(testTabs)
        
        // When
        viewModel = TabsViewModel(tabRepository)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is TabsUiState.Success)
        assertEquals(2, (state as TabsUiState.Success).tabs.size)
    }
    
    @Test
    fun `initial state shows empty when no tabs`() = runTest(testDispatcher) {
        // Given
        every { tabRepository.getAllTabs() } returns flowOf(emptyList())
        
        // When
        viewModel = TabsViewModel(tabRepository)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is TabsUiState.Empty)
    }
    
    @Test
    fun `switchTab calls repository`() = runTest(testDispatcher) {
        // Given
        every { tabRepository.getAllTabs() } returns flowOf(testTabs)
        coEvery { tabRepository.switchTab("tab2") } returns Unit
        viewModel = TabsViewModel(tabRepository)
        advanceUntilIdle()
        
        // When
        viewModel.switchTab("tab2")
        advanceUntilIdle()
        
        // Then
        coVerify { tabRepository.switchTab("tab2") }
    }
    
    @Test
    fun `closeTab calls repository`() = runTest(testDispatcher) {
        // Given
        every { tabRepository.getAllTabs() } returns flowOf(testTabs)
        coEvery { tabRepository.closeTab("tab1") } returns "tab2" // Returns next tab ID
        viewModel = TabsViewModel(tabRepository)
        advanceUntilIdle()
        
        // When
        viewModel.closeTab("tab1")
        advanceUntilIdle()
        
        // Then
        coVerify { tabRepository.closeTab("tab1") }
    }
    
    @Test
    fun `closeAllTabs calls repository`() = runTest(testDispatcher) {
        // Given
        every { tabRepository.getAllTabs() } returns flowOf(testTabs)
        coEvery { tabRepository.closeAllTabs() } returns Unit
        viewModel = TabsViewModel(tabRepository)
        advanceUntilIdle()
        
        // When
        viewModel.closeAllTabs()
        advanceUntilIdle()
        
        // Then
        coVerify { tabRepository.closeAllTabs() }
    }
    
    @Test
    fun `error during load shows error state`() = runTest(testDispatcher) {
        // Given
        every { tabRepository.getAllTabs() } throws RuntimeException("Database error")
        
        // When
        viewModel = TabsViewModel(tabRepository)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is TabsUiState.Error)
        assertTrue((state as TabsUiState.Error).message.contains("Failed to restore tabs"))
    }
}
