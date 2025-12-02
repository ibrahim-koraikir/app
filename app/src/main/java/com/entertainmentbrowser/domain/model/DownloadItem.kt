package com.entertainmentbrowser.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class DownloadItem(
    val id: Int,
    val url: String,
    val filename: String,
    val filePath: String?,
    val status: DownloadStatus,
    val progress: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val createdAt: Long
)

@Immutable
enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
