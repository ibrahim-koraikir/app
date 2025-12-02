package com.entertainmentbrowser.presentation.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entertainmentbrowser.core.error.toAppError
import com.entertainmentbrowser.domain.model.DownloadStatus
import com.entertainmentbrowser.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()
    
    init {
        observeDownloads()
    }
    
    fun onEvent(event: DownloadsEvent) {
        when (event) {
            is DownloadsEvent.PauseDownload -> pauseDownload(event.downloadId)
            is DownloadsEvent.ResumeDownload -> resumeDownload(event.downloadId)
            is DownloadsEvent.CancelDownload -> cancelDownload(event.downloadId)
            is DownloadsEvent.DeleteDownload -> deleteDownload(event.downloadId)
            is DownloadsEvent.OpenDownload -> openDownload(event.downloadId)
            is DownloadsEvent.Refresh -> refreshDownloads()
        }
    }
    
    private fun observeDownloads() {
        viewModelScope.launch {
            downloadRepository.observeDownloads()
                .catch { e ->
                    val appError = e.toAppError()
                    _uiState.update { it.copy(error = appError.message) }
                }
                .collect { downloads ->
                    val active = downloads.filter { 
                        it.status == DownloadStatus.QUEUED || 
                        it.status == DownloadStatus.DOWNLOADING ||
                        it.status == DownloadStatus.PAUSED
                    }
                    val completed = downloads.filter { it.status == DownloadStatus.COMPLETED }
                    val failed = downloads.filter { 
                        it.status == DownloadStatus.FAILED || 
                        it.status == DownloadStatus.CANCELLED 
                    }
                    
                    _uiState.update {
                        it.copy(
                            activeDownloads = active,
                            completedDownloads = completed,
                            failedDownloads = failed,
                            isRefreshing = false,
                            error = null
                        )
                    }
                }
        }
    }
    
    private fun pauseDownload(downloadId: Int) {
        viewModelScope.launch {
            try {
                downloadRepository.pauseDownload(downloadId)
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.update { it.copy(error = appError.message) }
            }
        }
    }
    
    private fun resumeDownload(downloadId: Int) {
        viewModelScope.launch {
            try {
                downloadRepository.resumeDownload(downloadId)
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.update { it.copy(error = appError.message) }
            }
        }
    }
    
    private fun cancelDownload(downloadId: Int) {
        viewModelScope.launch {
            try {
                downloadRepository.cancelDownload(downloadId)
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.update { it.copy(error = appError.message) }
            }
        }
    }
    
    private fun deleteDownload(downloadId: Int) {
        viewModelScope.launch {
            try {
                downloadRepository.deleteDownload(downloadId)
            } catch (e: Exception) {
                val appError = e.toAppError()
                _uiState.update { it.copy(error = appError.message) }
            }
        }
    }
    
    private fun openDownload(downloadId: Int) {
        // Opening is handled by the UI layer with intents
        // This is just a placeholder for future implementation
    }
    
    private fun refreshDownloads() {
        _uiState.update { it.copy(isRefreshing = true) }
        // The flow will automatically update with latest data
    }
}
