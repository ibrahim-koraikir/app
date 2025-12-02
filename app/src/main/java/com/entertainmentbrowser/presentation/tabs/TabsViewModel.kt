package com.entertainmentbrowser.presentation.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entertainmentbrowser.domain.model.Tab
import com.entertainmentbrowser.domain.repository.TabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Tabs screen.
 * Manages tab state, switching, and closing operations.
 */
@HiltViewModel
class TabsViewModel @Inject constructor(
    private val tabRepository: TabRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<TabsUiState>(TabsUiState.Loading)
    val uiState: StateFlow<TabsUiState> = _uiState.asStateFlow()
    
    init {
        loadTabs()
    }
    
    /**
     * Loads all tabs from the repository.
     * Handles restoration failures by showing error state.
     */
    private fun loadTabs() {
        viewModelScope.launch {
            try {
                tabRepository.getAllTabs().collect { tabs ->
                    _uiState.value = if (tabs.isEmpty()) {
                        TabsUiState.Empty
                    } else {
                        TabsUiState.Success(tabs)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = TabsUiState.Error("Failed to restore tabs: ${e.message}")
            }
        }
    }
    
    /**
     * Switches to a different tab.
     * 
     * @param tabId The ID of the tab to switch to
     */
    fun switchTab(tabId: String) {
        viewModelScope.launch {
            try {
                tabRepository.switchTab(tabId)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = TabsUiState.Error(e.message ?: "Failed to switch tab")
            }
        }
    }
    
    /**
     * Closes a specific tab.
     * 
     * @param tabId The ID of the tab to close
     */
    fun closeTab(tabId: String) {
        viewModelScope.launch {
            try {
                tabRepository.closeTab(tabId)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = TabsUiState.Error(e.message ?: "Failed to close tab")
            }
        }
    }
    
    /**
     * Closes all tabs.
     */
    fun closeAllTabs() {
        viewModelScope.launch {
            try {
                tabRepository.closeAllTabs()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = TabsUiState.Error(e.message ?: "Failed to close all tabs")
            }
        }
    }
}

/**
 * UI state for the Tabs screen.
 */
sealed interface TabsUiState {
    /**
     * Loading state while tabs are being fetched.
     */
    data object Loading : TabsUiState
    
    /**
     * Success state with list of tabs.
     */
    data class Success(val tabs: List<Tab>) : TabsUiState
    
    /**
     * Empty state when there are no tabs.
     */
    data object Empty : TabsUiState
    
    /**
     * Error state with error message.
     */
    data class Error(val message: String) : TabsUiState
}
