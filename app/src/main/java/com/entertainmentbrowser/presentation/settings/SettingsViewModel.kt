package com.entertainmentbrowser.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entertainmentbrowser.domain.repository.SettingsRepository
import com.entertainmentbrowser.util.adblock.AdBlockStatus
import com.entertainmentbrowser.util.adblock.FilterUpdateManager
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
import com.entertainmentbrowser.util.WebViewPool

/**
 * ViewModel for the Settings screen.
 * Manages app settings and handles user actions.
 * 
 * Requirements: 12.1-12.5
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val filterUpdateManager: FilterUpdateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        observeAdBlockStatus()
    }

    /**
     * Observe settings changes from the repository.
     */
    private fun observeSettings() {
        settingsRepository.observeSettings()
            .catch { exception ->
                _uiState.update {
                    it.copy(
                        error = exception.message ?: "Failed to load settings",
                        isLoading = false
                    )
                }
            }
            .onEach { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Observe ad-blocking status changes.
     */
    private fun observeAdBlockStatus() {
        AdBlockStatus.status
            .onEach { status ->
                _uiState.update {
                    it.copy(adBlockStatus = status)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Handle settings events from the UI.
     */
    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleDownloadOnWifiOnly -> {
                updateDownloadOnWifiOnly(event.enabled)
            }
            
            is SettingsEvent.UpdateMaxConcurrentDownloads -> {
                updateMaxConcurrentDownloads(event.max)
            }
            
            is SettingsEvent.ToggleHapticFeedback -> {
                updateHapticFeedback(event.enabled)
            }
            
            is SettingsEvent.ShowClearCacheDialog -> {
                _uiState.update { it.copy(showClearCacheDialog = true) }
            }
            
            is SettingsEvent.ConfirmClearCache -> {
                clearCache()
            }
            
            is SettingsEvent.ShowClearHistoryDialog -> {
                _uiState.update { it.copy(showClearHistoryDialog = true) }
            }
            
            is SettingsEvent.ConfirmClearHistory -> {
                clearDownloadHistory()
            }
            
            is SettingsEvent.DismissDialog -> {
                _uiState.update {
                    it.copy(
                        showClearCacheDialog = false,
                        showClearHistoryDialog = false,
                        showClearBrowsingDataDialog = false
                    )
                }
            }
            
            is SettingsEvent.ShowClearBrowsingDataDialog -> {
                _uiState.update { it.copy(showClearBrowsingDataDialog = true) }
            }
            
            is SettingsEvent.ConfirmClearBrowsingData -> {
                clearBrowsingData()
            }
            
            is SettingsEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
            
            is SettingsEvent.DismissSuccess -> {
                _uiState.update {
                    it.copy(
                        cacheCleared = false,
                        historyCleared = false,
                        browsingDataCleared = false
                    )
                }
            }
            
            is SettingsEvent.UpdateSearchEngine -> {
                updateSearchEngine(event.engineOrdinal)
            }
            
            is SettingsEvent.RefreshAdBlockFilters -> {
                refreshAdBlockFilters()
            }
            
            is SettingsEvent.DismissFiltersRefreshed -> {
                _uiState.update { it.copy(filtersRefreshed = false) }
            }
        }
    }
    
    /**
     * Update search engine setting.
     */
    private fun updateSearchEngine(engineOrdinal: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.updateSearchEngine(engineOrdinal)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update search engine")
                }
            }
        }
    }

    /**
     * Update download on Wi-Fi only setting.
     * Requirements: 12.2
     */
    private fun updateDownloadOnWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateDownloadOnWifiOnly(enabled)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update setting")
                }
            }
        }
    }

    /**
     * Update maximum concurrent downloads setting.
     * Requirements: 12.3
     */
    private fun updateMaxConcurrentDownloads(max: Int) {
        viewModelScope.launch {
            try {
                settingsRepository.updateMaxConcurrentDownloads(max)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update setting")
                }
            }
        }
    }

    /**
     * Update haptic feedback setting.
     * Requirements: 12.1
     */
    private fun updateHapticFeedback(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.updateHapticFeedbackEnabled(enabled)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to update setting")
                }
            }
        }
    }

    /**
     * Clear WebView cache.
     * Requirements: 12.4
     */
    private fun clearCache() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, showClearCacheDialog = false) }
                settingsRepository.clearCache()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cacheCleared = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to clear cache"
                    )
                }
            }
        }
    }

    /**
     * Clear download history from database.
     * Requirements: 12.5
     */
    private fun clearDownloadHistory() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, showClearHistoryDialog = false) }
                settingsRepository.clearDownloadHistory()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        historyCleared = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to clear download history"
                    )
                }
            }
        }
    }
    
    /**
     * Clear all browsing data including cookies and WebStorage.
     */
    private fun clearBrowsingData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, showClearBrowsingDataDialog = false) }
                WebViewPool.clearAllBrowsingData()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        browsingDataCleared = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to clear browsing data"
                    )
                }
            }
        }
    }
    
    /**
     * Manually refresh ad-blocking filters.
     * Triggers a filter update and reloads the engines.
     */
    private fun refreshAdBlockFilters() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                AdBlockStatus.setRefreshing(true)
                
                // Trigger filter update
                val success = filterUpdateManager.forceUpdate()
                
                AdBlockStatus.setRefreshing(false)
                AdBlockStatus.resetWarningFlag()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        filtersRefreshed = success,
                        error = if (!success) "Failed to update filters. Check your internet connection." else null
                    )
                }
            } catch (e: Exception) {
                AdBlockStatus.setRefreshing(false)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to refresh ad-block filters"
                    )
                }
            }
        }
    }
}
