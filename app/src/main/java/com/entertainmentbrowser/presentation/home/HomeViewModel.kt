package com.entertainmentbrowser.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entertainmentbrowser.core.error.AppError
import com.entertainmentbrowser.core.error.toAppError
import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.domain.usecase.GetWebsitesByCategoryUseCase
import com.entertainmentbrowser.domain.usecase.SearchWebsitesUseCase
import com.entertainmentbrowser.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getWebsitesByCategoryUseCase: GetWebsitesByCategoryUseCase,
    private val getAllWebsitesUseCase: com.entertainmentbrowser.domain.usecase.GetAllWebsitesUseCase,
    private val searchWebsitesUseCase: SearchWebsitesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        // Load all websites
        loadAllWebsites()
        
        // Set up debounced search
        _searchQuery
            .debounce(300) // 300ms debounce as per requirements
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isBlank()) {
                    // If search is cleared, reload all websites
                    loadAllWebsites()
                } else {
                    // Perform search
                    searchWebsites(query)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.CategorySelected -> {
                _uiState.update { it.copy(selectedCategory = event.category, searchQuery = "") }
                _searchQuery.value = ""
                loadWebsitesByCategory(event.category)
            }
            
            is HomeEvent.SearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                _searchQuery.value = event.query
            }
            
            is HomeEvent.ToggleFavorite -> {
                toggleFavorite(event.websiteId)
            }
            
            is HomeEvent.WebsiteClicked -> {
                // Navigation handled by the screen composable
            }
            
            is HomeEvent.Refresh -> {
                refresh()
            }
            
            is HomeEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun loadWebsitesByCategory(category: Category) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        getWebsitesByCategoryUseCase(category)
            .catch { exception ->
                val appError = exception.toAppError()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = appError.message
                    )
                }
            }
            .onEach { websites ->
                _uiState.update {
                    it.copy(
                        websites = websites,
                        isLoading = false,
                        error = null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun searchWebsites(query: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        searchWebsitesUseCase.searchDirect(query)
            .catch { exception ->
                val appError = exception.toAppError()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = appError.message
                    )
                }
            }
            .onEach { websites ->
                _uiState.update {
                    it.copy(
                        websites = websites,
                        isLoading = false,
                        error = null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun toggleFavorite(websiteId: Int) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(websiteId)
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.update {
                    it.copy(error = appError.message)
                }
            }
        }
    }

    private fun loadAllWebsites() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        getAllWebsitesUseCase()
            .catch { exception ->
                val appError = exception.toAppError()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = appError.message
                    )
                }
            }
            .onEach { websites ->
                _uiState.update {
                    it.copy(
                        websites = websites,
                        isLoading = false,
                        error = null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        
        val currentQuery = _uiState.value.searchQuery
        if (currentQuery.isBlank()) {
            loadAllWebsites()
        } else {
            searchWebsites(currentQuery)
        }
        
        _uiState.update { it.copy(isRefreshing = false) }
    }
}
