package com.entertainmentbrowser.data.repository

import android.app.DownloadManager
import android.app.NotificationManager
import android.content.Context
import com.entertainmentbrowser.data.local.dao.DownloadDao
import com.entertainmentbrowser.data.local.entity.DownloadEntity
import com.entertainmentbrowser.domain.model.DownloadStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DownloadRepositoryImplTest {
    
    private lateinit var context: Context
    private lateinit var downloadManager: DownloadManager
    private lateinit var downloadDao: DownloadDao
    private lateinit var notificationManager: NotificationManager
    private lateinit var repository: DownloadRepositoryImpl
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        downloadManager = mockk(relaxed = true)
        downloadDao = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)
        repository = DownloadRepositoryImpl(context, downloadManager, downloadDao, notificationManager)
    }
    
    @Test
    fun `observeDownloads returns all downloads`() = runTest {
        // Given
        val entities = listOf(
            DownloadEntity(
                id = 1,
                url = "https://example.com/video.mp4",
                filename = "video.mp4",
                filePath = "/storage/downloads/video.mp4",
                status = "COMPLETED",
                progress = 100,
                downloadedBytes = 1024000,
                totalBytes = 1024000,
                createdAt = System.currentTimeMillis()
            )
        )
        every { downloadDao.getAllDownloads() } returns flowOf(entities)
        
        // When
        val result = repository.observeDownloads().first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals("video.mp4", result[0].filename)
        assertEquals(DownloadStatus.COMPLETED, result[0].status)
    }
    
    @Test
    fun `deleteDownload removes download from database`() = runTest {
        // Given
        val downloadId = 1
        coEvery { downloadDao.delete(downloadId) } returns Unit
        
        // When
        repository.deleteDownload(downloadId)
        
        // Then
        coVerify { downloadDao.delete(downloadId) }
    }
    @Test
    fun `cancelDownload removes from manager and database`() = runTest {
        // Given
        val downloadId = 123
        coEvery { downloadDao.delete(downloadId) } returns Unit
        every { downloadManager.remove(any()) } returns 1 // Returns number of removed downloads
        
        // When
        repository.cancelDownload(downloadId)
        
        // Then
        verify { downloadManager.remove(downloadId.toLong()) }
        coVerify { downloadDao.delete(downloadId) }
    }
}
