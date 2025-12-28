package com.entertainmentbrowser.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entertainmentbrowser.core.error.AppError
import com.entertainmentbrowser.core.error.toAppError
import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.domain.repository.SettingsRepository
import com.entertainmentbrowser.domain.repository.TabRepository
import com.entertainmentbrowser.domain.usecase.GetWebsitesByCategoryUseCase
import com.entertainmentbrowser.domain.usecase.SearchWebsitesUseCase
import com.entertainmentbrowser.domain.usecase.ToggleFavoriteUseCase
import com.entertainmentbrowser.util.TutorialTipsManager
import com.entertainmentbrowser.util.UrlUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val settingsRepository: SettingsRepository,
    private val tabRepository: TabRepository,
    private val tutorialTipsManager: TutorialTipsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    
    // Tutorial tips state
    private val _showEdgeSwipeTip = MutableStateFlow(false)
    val showEdgeSwipeTip: StateFlow<Boolean> = _showEdgeSwipeTip.asStateFlow()
    
    private val _showLongPressTip = MutableStateFlow(false)
    val showLongPressTip: StateFlow<Boolean> = _showLongPressTip.asStateFlow()

    init {
        // Load all websites
        loadAllWebsites()
        
        // Observe search engine setting
        observeSearchEngine()
        
        // Check tutorial tips
        checkTutorialTips()
        
        // Observe active tab for edge swipe navigation
        observeActiveTab()
        
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
            
            is HomeEvent.SearchBarSubmit -> {
                resolveSearchInput(event.input)
            }
            
            is HomeEvent.NavigationConsumed -> {
                _uiState.update { it.copy(navigationUrl = null) }
            }
            
            is HomeEvent.Refresh -> {
                refresh()
            }
            
            is HomeEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
            
            is HomeEvent.OpenInNewTab -> {
                openInNewTab(event.url, event.title)
            }
            
            is HomeEvent.SnackbarConsumed -> {
                _uiState.update { it.copy(snackbarMessage = null) }
            }
        }
    }
    
    /**
     * Opens a URL in a new background tab without navigating away.
     * Sanitizes the URL before creating the tab to prevent malformed entries.
     */
    private fun openInNewTab(url: String, title: String) {
        viewModelScope.launch {
            try {
                // Sanitize URL: trim whitespace and validate
                val sanitizedUrl = sanitizeUrlForTab(url)
                
                // Bail out on blank/invalid URLs
                if (sanitizedUrl.isBlank()) {
                    _uiState.update { it.copy(error = "Invalid URL") }
                    return@launch
                }
                
                tabRepository.createTabInBackground(sanitizedUrl, title.trim())
                _uiState.update { it.copy(snackbarMessage = "Opened in new tab") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to open in new tab") }
            }
        }
    }
    
    /**
     * Sanitizes a URL for tab creation by trimming whitespace and ensuring proper format.
     * Returns empty string if URL is invalid.
     */
    private fun sanitizeUrlForTab(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        
        // Ensure URL has a scheme
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("www.") -> "https://$trimmed"
            else -> "https://$trimmed"
        }
    }
    
    /**
     * Resolves search bar input (URL or search query) and emits navigation URL.
     * Centralizes URL resolution logic for potential caching/analytics.
     */
    private fun resolveSearchInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        
        val resolvedUrl = UrlUtils.resolveSearchInput(trimmed, _uiState.value.searchEngine)
        if (resolvedUrl.isNotEmpty()) {
            _uiState.update { it.copy(navigationUrl = resolvedUrl) }
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
    
    private fun observeSearchEngine() {
        settingsRepository.observeSettings()
            .onEach { settings ->
                _uiState.update { it.copy(searchEngine = settings.searchEngine) }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Observes the active tab to enable edge swipe navigation back to WebView.
     */
    private fun observeActiveTab() {
        tabRepository.getActiveTab()
            .onEach { tab ->
                _uiState.update { it.copy(activeTabUrl = tab?.url) }
            }
            .launchIn(viewModelScope)
    }
    
    // ==================== TUTORIAL TIPS ====================
    
    /**
     * Check which tutorial tips should be shown
     */
    private fun checkTutorialTips() {
        viewModelScope.launch {
            // Check edge swipe tip - show when there's an active tab
            tutorialTipsManager.shouldShowEdgeSwipeTip()
                .onEach { shouldShow ->
                    _showEdgeSwipeTip.value = shouldShow
                }
                .launchIn(viewModelScope)
            
            // Check long press tip
            tutorialTipsManager.shouldShowLongPressTip()
                .onEach { shouldShow ->
                    _showLongPressTip.value = shouldShow
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Called when edge swipe tip is shown to user
     */
    fun onEdgeSwipeTipShown() {
        viewModelScope.launch {
            tutorialTipsManager.incrementEdgeSwipeTipCount()
        }
    }
    
    /**
     * Called when user dismisses edge swipe tip
     */
    fun dismissEdgeSwipeTip() {
        _showEdgeSwipeTip.value = false
    }
    
    /**
     * Called when user performs edge swipe (completed the action)
     */
    fun onEdgeSwipeCompleted() {
        viewModelScope.launch {
            tutorialTipsManager.markEdgeSwipeCompleted()
            _showEdgeSwipeTip.value = false
        }
    }
    
    /**
     * Called when long press tip is shown to user
     */
    fun onLongPressTipShown() {
        viewModelScope.launch {
            tutorialTipsManager.incrementLongPressTipCount()
        }
    }
    
    /**
     * Called when user dismisses long press tip
     */
    fun dismissLongPressTip() {
        _showLongPressTip.value = false
    }
    
    /**
     * Called when user performs long press (completed the action)
     */
    fun onLongPressCompleted() {
        viewModelScope.launch {
            tutorialTipsManager.markLongPressCompleted()
            _showLongPressTip.value = false
        }
    }
}
