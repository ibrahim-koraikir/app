package com.entertainmentbrowser.presentation.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entertainmentbrowser.domain.model.Session
import com.entertainmentbrowser.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Sessions screen.
 * Manages session state, creation, restoration, deletion, and renaming operations.
 */
@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<SessionsUiState>(SessionsUiState.Loading)
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()
    
    private val _events = MutableStateFlow<SessionEvent?>(null)
    val events: StateFlow<SessionEvent?> = _events.asStateFlow()
    
    init {
        loadSessions()
    }
    
    /**
     * Loads all sessions from the repository.
     * Sessions are sorted by creation date (newest first).
     */
    private fun loadSessions() {
        viewModelScope.launch {
            try {
                sessionRepository.getAllSessions().collect { sessions ->
                    _uiState.value = if (sessions.isEmpty()) {
                        SessionsUiState.Empty
                    } else {
                        SessionsUiState.Success(sessions)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = SessionsUiState.Error("Failed to load sessions: ${e.message}")
            }
        }
    }
    
    /**
     * Creates a new session with the given name and tab IDs.
     * 
     * @param name The name for the session
     * @param tabIds List of tab IDs to save in the session
     */
    fun createSession(name: String, tabIds: List<String>) {
        if (name.isBlank()) {
            _events.value = SessionEvent.ShowError("Session name cannot be empty")
            return
        }
        
        viewModelScope.launch {
            try {
                sessionRepository.createSession(name.trim(), tabIds)
                _events.value = SessionEvent.SessionCreated
            } catch (e: Exception) {
                e.printStackTrace()
                _events.value = SessionEvent.ShowError("Failed to create session: ${e.message}")
            }
        }
    }
    
    /**
     * Restores a session by its ID.
     * Returns the list of tab IDs that should be opened.
     * 
     * @param sessionId The ID of the session to restore
     */
    fun restoreSession(sessionId: Int) {
        viewModelScope.launch {
            try {
                val tabIds = sessionRepository.restoreSession(sessionId)
                if (tabIds.isEmpty()) {
                    _events.value = SessionEvent.ShowError("Session is empty or not found")
                } else {
                    _events.value = SessionEvent.SessionRestored(tabIds)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _events.value = SessionEvent.ShowError("Failed to restore session: ${e.message}")
            }
        }
    }
    
    /**
     * Deletes a session after confirmation.
     * 
     * @param sessionId The ID of the session to delete
     */
    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            try {
                sessionRepository.deleteSession(sessionId)
                _events.value = SessionEvent.SessionDeleted
            } catch (e: Exception) {
                e.printStackTrace()
                _events.value = SessionEvent.ShowError("Failed to delete session: ${e.message}")
            }
        }
    }
    
    /**
     * Renames a session.
     * 
     * @param sessionId The ID of the session to rename
     * @param newName The new name for the session
     */
    fun renameSession(sessionId: Int, newName: String) {
        if (newName.isBlank()) {
            _events.value = SessionEvent.ShowError("Session name cannot be empty")
            return
        }
        
        viewModelScope.launch {
            try {
                sessionRepository.renameSession(sessionId, newName.trim())
                _events.value = SessionEvent.SessionRenamed
            } catch (e: Exception) {
                e.printStackTrace()
                _events.value = SessionEvent.ShowError("Failed to rename session: ${e.message}")
            }
        }
    }
    
    /**
     * Clears the current event after it has been handled.
     */
    fun clearEvent() {
        _events.value = null
    }
}

/**
 * UI state for the Sessions screen.
 */
sealed interface SessionsUiState {
    /**
     * Loading state while sessions are being fetched.
     */
    data object Loading : SessionsUiState
    
    /**
     * Success state with list of sessions sorted by date (newest first).
     */
    data class Success(val sessions: List<Session>) : SessionsUiState
    
    /**
     * Empty state when there are no sessions.
     */
    data object Empty : SessionsUiState
    
    /**
     * Error state with error message.
     */
    data class Error(val message: String) : SessionsUiState
}

/**
 * Events emitted by the SessionsViewModel.
 */
sealed interface SessionEvent {
    /**
     * Session was successfully created.
     */
    data object SessionCreated : SessionEvent
    
    /**
     * Session was successfully restored with the given tab IDs.
     */
    data class SessionRestored(val tabIds: List<String>) : SessionEvent
    
    /**
     * Session was successfully deleted.
     */
    data object SessionDeleted : SessionEvent
    
    /**
     * Session was successfully renamed.
     */
    data object SessionRenamed : SessionEvent
    
    /**
     * An error occurred with the given message.
     */
    data class ShowError(val message: String) : SessionEvent
}
