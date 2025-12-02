package com.entertainmentbrowser.presentation.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entertainmentbrowser.core.error.toAppError
import com.entertainmentbrowser.domain.repository.WebsiteRepository
import com.entertainmentbrowser.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val websiteRepository: WebsiteRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    fun onEvent(event: FavoritesEvent) {
        when (event) {
            is FavoritesEvent.ToggleFavorite -> {
                toggleFavorite(event.websiteId)
            }
            
            is FavoritesEvent.WebsiteClicked -> {
                // Navigation handled by the screen composable
            }
            
            is FavoritesEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun loadFavorites() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        websiteRepository.getFavorites()
            .catch { exception ->
                val appError = exception.toAppError()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = appError.message
                    )
                }
            }
            .onEach { favorites ->
                _uiState.update {
                    it.copy(
                        favorites = favorites,
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
}
