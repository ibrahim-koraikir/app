package com.entertainmentbrowser.presentation.downloads

import com.entertainmentbrowser.domain.model.DownloadItem

data class DownloadsUiState(
    val activeDownloads: List<DownloadItem> = emptyList(),
    val completedDownloads: List<DownloadItem> = emptyList(),
    val failedDownloads: List<DownloadItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null
)
