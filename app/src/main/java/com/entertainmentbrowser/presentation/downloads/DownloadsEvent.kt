package com.entertainmentbrowser.presentation.downloads

sealed interface DownloadsEvent {
    data class PauseDownload(val downloadId: Int) : DownloadsEvent
    data class ResumeDownload(val downloadId: Int) : DownloadsEvent
    data class CancelDownload(val downloadId: Int) : DownloadsEvent
    data class DeleteDownload(val downloadId: Int) : DownloadsEvent
    data class OpenDownload(val downloadId: Int) : DownloadsEvent
    data object Refresh : DownloadsEvent
}
