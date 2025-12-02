package com.entertainmentbrowser.domain.repository

import com.entertainmentbrowser.domain.model.DownloadItem
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeDownloads(): Flow<List<DownloadItem>>
    suspend fun startDownload(url: String, filename: String, quality: String = "auto")
    suspend fun pauseDownload(downloadId: Int)
    suspend fun resumeDownload(downloadId: Int)
    suspend fun cancelDownload(downloadId: Int)
    suspend fun deleteDownload(downloadId: Int)
}
