package com.entertainmentbrowser.presentation.downloads

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.entertainmentbrowser.domain.model.DownloadItem
import com.entertainmentbrowser.domain.model.DownloadStatus
import com.entertainmentbrowser.domain.repository.DownloadRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var downloadRepository: DownloadRepository
    private lateinit var viewModel: DownloadsViewModel
    
    private val testDownloads = listOf(
        DownloadItem(
            id = 1,
            url = "https://example.com/video1.mp4",
            filename = "video1.mp4",
            filePath = "/storage/downloads/video1.mp4",
            status = DownloadStatus.DOWNLOADING,
            progress = 50,
            downloadedBytes = 512000,
            totalBytes = 1024000,
            createdAt = System.currentTimeMillis()
        ),
        DownloadItem(
            id = 2,
            url = "https://example.com/video2.mp4",
            filename = "video2.mp4",
            filePath = "/storage/downloads/video2.mp4",
            status = DownloadStatus.COMPLETED,
            progress = 100,
            downloadedBytes = 2048000,
            totalBytes = 2048000,
            createdAt = System.currentTimeMillis()
        ),
        DownloadItem(
            id = 3,
            url = "https://example.com/video3.mp4",
            filename = "video3.mp4",
            filePath = null,
            status = DownloadStatus.FAILED,
            progress = 25,
            downloadedBytes = 256000,
            totalBytes = 1024000,
            createdAt = System.currentTimeMillis()
        )
    )
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        downloadRepository = mockk()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state groups downloads by status`() = runTest(testDispatcher) {
        // Given
        every { downloadRepository.observeDownloads() } returns flowOf(testDownloads)
        
        // When
        viewModel = DownloadsViewModel(downloadRepository)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(1, state.activeDownloads.size)
        assertEquals(1, state.completedDownloads.size)
        assertEquals(1, state.failedDownloads.size)
        assertEquals(DownloadStatus.DOWNLOADING, state.activeDownloads[0].status)
        assertEquals(DownloadStatus.COMPLETED, state.completedDownloads[0].status)
        assertEquals(DownloadStatus.FAILED, state.failedDownloads[0].status)
    }
    
    @Test
    fun `PauseDownload event calls repository`() = runTest(testDispatcher) {
        // Given
        every { downloadRepository.observeDownloads() } returns flowOf(testDownloads)
        coEvery { downloadRepository.pauseDownload(1) } returns Unit
        viewModel = DownloadsViewModel(downloadRepository)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(DownloadsEvent.PauseDownload(1))
        advanceUntilIdle()
        
        // Then
        coVerify { downloadRepository.pauseDownload(1) }
    }
    
    @Test
    fun `ResumeDownload event calls repository`() = runTest(testDispatcher) {
        // Given
        every { downloadRepository.observeDownloads() } returns flowOf(testDownloads)
        coEvery { downloadRepository.resumeDownload(1) } returns Unit
        viewModel = DownloadsViewModel(downloadRepository)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(DownloadsEvent.ResumeDownload(1))
        advanceUntilIdle()
        
        // Then
        coVerify { downloadRepository.resumeDownload(1) }
    }
    
    @Test
    fun `CancelDownload event calls repository`() = runTest(testDispatcher) {
        // Given
        every { downloadRepository.observeDownloads() } returns flowOf(testDownloads)
        coEvery { downloadRepository.cancelDownload(1) } returns Unit
        viewModel = DownloadsViewModel(downloadRepository)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(DownloadsEvent.CancelDownload(1))
        advanceUntilIdle()
        
        // Then
        coVerify { downloadRepository.cancelDownload(1) }
    }
    
    @Test
    fun `DeleteDownload event calls repository`() = runTest(testDispatcher) {
        // Given
        every { downloadRepository.observeDownloads() } returns flowOf(testDownloads)
        coEvery { downloadRepository.deleteDownload(2) } returns Unit
        viewModel = DownloadsViewModel(downloadRepository)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(DownloadsEvent.DeleteDownload(2))
        advanceUntilIdle()
        
        // Then
        coVerify { downloadRepository.deleteDownload(2) }
    }
    
    @Test
    fun `Refresh event sets refreshing state`() = runTest(testDispatcher) {
        // Given
        every { downloadRepository.observeDownloads() } returns flowOf(testDownloads)
        viewModel = DownloadsViewModel(downloadRepository)
        advanceUntilIdle()
        
        // When
        viewModel.onEvent(DownloadsEvent.Refresh)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals(true, state.isRefreshing)
    }
}
