package com.entertainmentbrowser.presentation.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entertainmentbrowser.domain.model.Bookmark
import com.entertainmentbrowser.domain.repository.BookmarkRepository
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

data class BookmarksUiState(
    val bookmarks: List<Bookmark> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed interface BookmarksEvent {
    data class DeleteBookmark(val id: Int) : BookmarksEvent
    data class BookmarkClicked(val url: String) : BookmarksEvent
    data object ClearError : BookmarksEvent
}

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()
    
    init {
        loadBookmarks()
    }
    
    fun onEvent(event: BookmarksEvent) {
        when (event) {
            is BookmarksEvent.DeleteBookmark -> deleteBookmark(event.id)
            is BookmarksEvent.BookmarkClicked -> { /* Navigation handled by screen */ }
            is BookmarksEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }
    
    private fun loadBookmarks() {
        bookmarkRepository.getAllBookmarks()
            .onEach { bookmarks ->
                _uiState.update {
                    it.copy(
                        bookmarks = bookmarks,
                        isLoading = false
                    )
                }
            }
            .catch { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load bookmarks"
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    private fun deleteBookmark(id: Int) {
        viewModelScope.launch {
            try {
                bookmarkRepository.deleteBookmark(id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to delete bookmark")
                }
            }
        }
    }
}
