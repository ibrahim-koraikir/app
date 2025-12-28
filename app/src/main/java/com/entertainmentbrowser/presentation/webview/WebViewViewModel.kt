package com.entertainmentbrowser.presentation.webview

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entertainmentbrowser.domain.repository.DownloadRepository
import com.entertainmentbrowser.presentation.navigation.Screen
import com.entertainmentbrowser.util.DownloadPermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

/**
 * ViewModel for the WebView screen
 * Handles video detection, download triggering, tab management, and toolbar state management
 */
@HiltViewModel
class WebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val downloadRepository: DownloadRepository,
    private val tabRepository: com.entertainmentbrowser.domain.repository.TabRepository,
    private val bookmarkRepository: com.entertainmentbrowser.domain.repository.BookmarkRepository,
    private val thumbnailCapture: com.entertainmentbrowser.util.ThumbnailCapture,
    private val webViewStateManager: com.entertainmentbrowser.util.WebViewStateManager,
    private val downloadPermissionState: DownloadPermissionState,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()
    
    private val _tabs = MutableStateFlow<List<com.entertainmentbrowser.domain.model.Tab>>(emptyList())
    val tabs: StateFlow<List<com.entertainmentbrowser.domain.model.Tab>> = _tabs.asStateFlow()
    
    private var currentTabId: String? = null
    
    // Interstitial ad counter with background preloading
    private var pageLoadCount = 0
    private var showThreshold = (7..10).random() // Random threshold between 7 and 10
    private val preloadBeforePages = 2           // Start preloading 2 pages before threshold
    
    private val _showInterstitialAd = MutableStateFlow(false)
    val showInterstitialAd: StateFlow<Boolean> = _showInterstitialAd.asStateFlow()
    
    // Signal to start preloading in background
    private val _shouldPreloadAd = MutableStateFlow(false)
    val shouldPreloadAd: StateFlow<Boolean> = _shouldPreloadAd.asStateFlow()
    
    // Top toast message for background tab notifications
    private val _topToastMessage = MutableStateFlow<String?>(null)
    val topToastMessage: StateFlow<String?> = _topToastMessage.asStateFlow()
    
    // Track if ad is ready to show
    private var isAdReady = false
    
    init {
        // Get URL from navigation arguments
        val encodedUrl = savedStateHandle.get<String>(Screen.WebView.ARG_URL)
        val decodedUrl = encodedUrl?.let { android.net.Uri.decode(it) } ?: ""
        
        // Observe tabs first
        observeTabs()
        
        // Check if this is a request to show the active tab (edge swipe from home)
        val isActiveTabRequest = decodedUrl == Screen.WebView.ACTIVE_TAB_MARKER
        
        if (isActiveTabRequest) {
            // Just show the active tab, don't create a new one
            loadActiveTab()
        } else {
            // Standard tab request - create new tab
            val initialUrl = sanitizeUrl(decodedUrl)
            
            // Set initial URL in UI state immediately
            _uiState.update { it.copy(url = initialUrl, currentUrl = initialUrl) }
            
            // Check if URL is from known DRM site
            if (DrmDetector.isKnownDrmSite(initialUrl)) {
                _uiState.update { it.copy(drmDetected = true) }
            }
            
            // Create a new tab for this URL
            createTabForUrl(initialUrl)
        }
    }
    
    /**
     * Load the active tab without creating a new one (used for edge swipe navigation)
     */
    private fun loadActiveTab() {
        viewModelScope.launch {
            try {
                tabRepository.getActiveTab().collect { activeTab ->
                    activeTab?.let { tab ->
                        currentTabId = tab.id
                        _uiState.update { 
                            it.copy(
                                url = tab.url, 
                                currentUrl = tab.url,
                                drmDetected = DrmDetector.isKnownDrmSite(tab.url)
                            ) 
                        }
                    }
                    return@collect // Only need first emission
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load active tab") }
            }
        }
    }
    
    /**
     * Create a new tab for the given URL
     */
    private fun createTabForUrl(url: String) {
        viewModelScope.launch {
            try {
                val uri = try { android.net.Uri.parse(url) } catch (_: Exception) { null }
                if (uri == null || uri.host.isNullOrBlank()) {
                    _uiState.update { it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_invalid_url)) }
                    return@launch
                }
                val title = extractTitleFromUrl(url)
                val tab = tabRepository.createTab(url, title)
                currentTabId = tab.id
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_failed_create_tab, e.message ?: "")) }
            }
        }
    }
    
    /**
     * Observe all tabs from repository
     */
    private fun observeTabs() {
        viewModelScope.launch {
            tabRepository.getAllTabs().collect { tabList ->
                _tabs.value = tabList
            }
        }
    }
    
    /**
     * Extract a title from URL (domain name)
     */
    private fun extractTitleFromUrl(url: String): String {
        return try {
            val uri = android.net.Uri.parse(url)
            uri.host?.replace("www.", "") ?: context.getString(com.entertainmentbrowser.R.string.new_tab)
        } catch (e: Exception) {
            context.getString(com.entertainmentbrowser.R.string.new_tab)
        }
    }

    private fun sanitizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val lower = trimmed.lowercase()
        return when {
            lower.startsWith("http://") || lower.startsWith("https://") -> trimmed
            lower.startsWith("www.") -> "https://$trimmed"
            else -> "https://$trimmed"
        }
    }
    
    /**
     * Handle events from the UI
     */
    fun onEvent(event: WebViewEvent) {
        when (event) {
            is WebViewEvent.UpdateUrl -> {
                _uiState.update { it.copy(currentUrl = event.url) }
                // Update the tab's URL in the database so it persists when switching tabs
                updateCurrentTabUrl(event.url)
            }
            
            is WebViewEvent.UpdateTitle -> {
                _uiState.update { it.copy(title = event.title) }
            }
            
            is WebViewEvent.UpdateLoading -> {
                _uiState.update { it.copy(isLoading = event.isLoading) }
            }
            
            is WebViewEvent.UpdateProgress -> {
                _uiState.update { it.copy(loadingProgress = event.progress) }
            }
            
            is WebViewEvent.UpdateNavigationState -> {
                _uiState.update { 
                    it.copy(
                        canGoBack = event.canGoBack,
                        canGoForward = event.canGoForward
                    )
                }
            }
            
            is WebViewEvent.VideoDetected -> {
                android.util.Log.d("WebViewViewModel", "📹 VideoDetected event received: ${event.videoUrl}")
                handleVideoDetected(event.videoUrl)
            }
            
            is WebViewEvent.VideoPlayingStateChanged -> {
                android.util.Log.d("WebViewViewModel", "▶️ Video playing state changed: ${event.isPlaying}")
                _uiState.update { it.copy(isVideoPlaying = event.isPlaying) }
            }
            
            is WebViewEvent.DrmDetected -> {
                _uiState.update { 
                    it.copy(
                        drmDetected = true,
                        showDrmWarning = true,
                        videoDetected = false,
                        detectedVideoUrl = null
                    )
                }
            }
            
            is WebViewEvent.DownloadVideo -> {
                startDownload()
            }
            
            is WebViewEvent.DownloadVideoWithFilename -> {
                startDownloadWithFilename(event.filename, event.quality)
            }
            
            is WebViewEvent.Share -> {
                shareCurrentUrl()
            }
            
            is WebViewEvent.DismissDrmWarning -> {
                _uiState.update { it.copy(showDrmWarning = false) }
            }
            
            is WebViewEvent.Error -> {
                _uiState.update { it.copy(error = event.message) }
            }
            
            is WebViewEvent.PageLoadError -> {
                handlePageLoadError(event.errorType, event.errorCode)
            }
            
            is WebViewEvent.RetryPageLoad -> {
                retryPageLoad()
            }
            
            is WebViewEvent.DismissErrorOverlay -> {
                _uiState.update { it.copy(showErrorOverlay = false, pageErrorType = PageErrorType.NONE) }
            }
            
            // Navigation events are handled by the screen directly
            WebViewEvent.NavigateBack,
            WebViewEvent.NavigateForward,
            WebViewEvent.Refresh -> {
                // These are handled by the WebView directly in the screen
            }
        }
    }
    
    /**
     * Handle video detection
     */
    private fun handleVideoDetected(videoUrl: String) {
        android.util.Log.d("WebViewViewModel", "handleVideoDetected called with: $videoUrl")
        
        // Don't show download button if DRM is detected
        if (_uiState.value.drmDetected) {
            android.util.Log.d("WebViewViewModel", "❌ DRM detected, not showing download button")
            return
        }
        
        // Check if video format is supported
        if (!VideoDetector.isSupportedFormat(videoUrl)) {
            android.util.Log.d("WebViewViewModel", "❌ Unsupported format: ${VideoDetector.getFormatName(videoUrl)}")
            _uiState.update { 
                it.copy(
                    error = context.getString(com.entertainmentbrowser.R.string.error_video_format_not_supported, VideoDetector.getFormatName(videoUrl))
                )
            }
            return
        }
        
        android.util.Log.d("WebViewViewModel", "✅ Setting videoDetected = true")
        _uiState.update { 
            it.copy(
                videoDetected = true,
                detectedVideoUrl = videoUrl
            )
        }
    }
    
    /**
     * Start downloading the detected video
     */
    private fun startDownload() {
        val videoUrl = _uiState.value.detectedVideoUrl ?: return
        
        // Check permissions BEFORE attempting download
        if (!downloadPermissionState.canStartDownload()) {
            _uiState.update { 
                it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_storage_permission_required))
            }
            return
        }
        
        viewModelScope.launch {
            try {
                // Generate filename from URL or use timestamp
                val filename = generateFilename(videoUrl)
                
                downloadRepository.startDownload(videoUrl, filename)
                
                // Hide download button after starting download
                _uiState.update { 
                    it.copy(
                        videoDetected = false,
                        detectedVideoUrl = null
                    )
                }
            } catch (e: SecurityException) {
                // Permission denied - show user-friendly message (fallback if permission check missed edge case)
                _uiState.update { 
                    it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_storage_permission_required))
                }
            } catch (e: IllegalArgumentException) {
                // Invalid URL
                _uiState.update { 
                    it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_invalid_download_url))
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_failed_start_download, e.message ?: ""))
                }
            }
        }
    }
    
    /**
     * Start downloading the detected video with custom filename and quality
     */
    private fun startDownloadWithFilename(filename: String, quality: String) {
        val videoUrl = _uiState.value.detectedVideoUrl ?: return
        
        // Check permissions BEFORE attempting download
        if (!downloadPermissionState.canStartDownload()) {
            _uiState.update { 
                it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_storage_permission_required))
            }
            return
        }
        
        viewModelScope.launch {
            try {
                android.util.Log.d("WebViewViewModel", "Starting download: $filename with quality: $quality")
                
                // For now, we pass the quality as metadata
                // The actual quality selection would need to be implemented in the download system
                // This could involve parsing m3u8 playlists or selecting different video sources
                downloadRepository.startDownload(videoUrl, filename, quality)
                
                // Show success message
                _uiState.update { 
                    it.copy(
                        videoDetected = false,
                        detectedVideoUrl = null,
                        error = context.getString(com.entertainmentbrowser.R.string.message_download_started, filename, quality)
                    )
                }
            } catch (e: SecurityException) {
                // Permission denied - show user-friendly message (fallback if permission check missed edge case)
                _uiState.update { 
                    it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_storage_permission_required))
                }
            } catch (e: IllegalArgumentException) {
                // Invalid URL
                _uiState.update { 
                    it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_invalid_download_url))
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_failed_start_download, e.message ?: ""))
                }
            }
        }
    }
    
    /**
     * Generate a filename for the download
     */
    private fun generateFilename(url: String): String {
        // Try to extract filename from URL
        val urlPath = url.substringBefore('?').substringAfterLast('/')
        
        return if (urlPath.isNotEmpty() && urlPath.contains('.')) {
            urlPath
        } else {
            // Generate filename with timestamp
            val timestamp = System.currentTimeMillis()
            val extension = when {
                url.contains(".mp4", ignoreCase = true) -> "mp4"
                url.contains(".webm", ignoreCase = true) -> "webm"
                url.contains(".m3u8", ignoreCase = true) -> "m3u8"
                url.contains(".mpd", ignoreCase = true) -> "mpd"
                else -> "mp4"
            }
            "video_$timestamp.$extension"
        }
    }
    
    /**
     * Share the current URL
     */
    private fun shareCurrentUrl() {
        val url = _uiState.value.currentUrl
        val title = _uiState.value.title.ifEmpty { context.getString(com.entertainmentbrowser.R.string.share_default_title) }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, context.getString(com.entertainmentbrowser.R.string.share_via))
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        context.startActivity(chooserIntent)
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Handle page load errors with user-friendly classification
     */
    private fun handlePageLoadError(errorType: PageErrorType, errorCode: Int) {
        android.util.Log.e("WebViewViewModel", "❌ Page load error: $errorType (code: $errorCode)")
        
        // Store the last successful URL for retry
        val lastUrl = _uiState.value.currentUrl.takeIf { it.isNotBlank() }
        
        _uiState.update { 
            it.copy(
                pageErrorType = errorType,
                showErrorOverlay = true,
                lastSuccessfulUrl = lastUrl ?: it.lastSuccessfulUrl,
                isLoading = false
            )
        }
    }
    
    /**
     * Retry loading the page after an error
     */
    private fun retryPageLoad() {
        val urlToRetry = _uiState.value.lastSuccessfulUrl ?: _uiState.value.currentUrl
        
        if (urlToRetry.isNotBlank()) {
            _uiState.update { 
                it.copy(
                    url = urlToRetry,
                    showErrorOverlay = false,
                    pageErrorType = PageErrorType.NONE,
                    isLoading = true
                )
            }
        }
    }
    
    /**
     * Clear top toast message
     */
    fun clearTopToast() {
        _topToastMessage.value = null
    }
    
    /**
     * Track page load for interstitial ad with background preloading
     * - Random threshold between 8-14: Show ad
     * - Preload starts 2 pages before threshold
     */
    fun onPageLoaded() {
        pageLoadCount++
        val preloadAt = showThreshold - preloadBeforePages
        android.util.Log.d("WebViewViewModel", "📊 Page load count: $pageLoadCount (preload@$preloadAt, show@$showThreshold, ready=$isAdReady)")
        
        // Start preloading 2 pages before the threshold
        if (pageLoadCount >= preloadAt && pageLoadCount < showThreshold && !_shouldPreloadAd.value && !isAdReady) {
            android.util.Log.d("WebViewViewModel", "🔄 Starting ad preload in background...")
            _shouldPreloadAd.value = true
        }
        
        // Show ad at threshold ONLY if ad is ready
        if (pageLoadCount >= showThreshold && isAdReady) {
            android.util.Log.d("WebViewViewModel", "💰 Showing interstitial ad NOW! (page $pageLoadCount)")
            _showInterstitialAd.value = true
            _shouldPreloadAd.value = false
            isAdReady = false
            pageLoadCount = 0
            showThreshold = (8..14).random() // Pick new random threshold for next cycle
        } else if (pageLoadCount >= showThreshold && !isAdReady) {
            android.util.Log.d("WebViewViewModel", "⏳ Threshold reached but ad not ready yet, waiting...")
        }
    }
    
    /**
     * Called when preloaded ad finishes loading in background
     */
    fun onAdPreloaded() {
        android.util.Log.d("WebViewViewModel", "✅ Ad preloaded and ready! Count=$pageLoadCount, threshold=$showThreshold")
        isAdReady = true
        
        // If we already passed the show threshold, show ad immediately
        if (pageLoadCount >= showThreshold) {
            android.util.Log.d("WebViewViewModel", "💰 Showing interstitial ad NOW! (was waiting for preload)")
            _showInterstitialAd.value = true
            _shouldPreloadAd.value = false
            isAdReady = false
            pageLoadCount = 0
            showThreshold = (8..14).random() // Pick new random threshold for next cycle
        }
    }
    
    /**
     * Dismiss interstitial ad
     */
    fun dismissInterstitialAd() {
        android.util.Log.d("WebViewViewModel", "✅ Interstitial ad dismissed")
        _showInterstitialAd.value = false
        _shouldPreloadAd.value = false
        isAdReady = false
    }
    
    /**
     * Switch to a different tab.
     * 
     * Uses one-shot repository query to validate tab existence before switching,
     * avoiding race conditions from stale Flow emissions in _tabs.value.
     * 
     * @param tabId The ID of the tab to switch to
     */
    fun switchTab(tabId: String) {
        viewModelScope.launch {
            try {
                // Fetch tab by ID from repository (one-shot query) to validate it exists
                // This avoids race conditions from relying on potentially stale _tabs.value
                val tab = tabRepository.getTabById(tabId)
                
                if (tab == null) {
                    android.util.Log.w("WebViewViewModel", "⚠️ Tab not found in database: $tabId")
                    _uiState.update { it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_tab_not_found)) }
                    return@launch
                }
                
                // Perform the switch in repository (updates database and WebView state)
                tabRepository.switchTab(tabId)
                currentTabId = tabId
                
                // Get actual URL from WebViewStateManager (may be more current than DB)
                val webViewState = webViewStateManager.getWebViewState(tabId)
                val actualUrl = webViewState?.url?.takeIf { it.isNotBlank() } ?: tab.url
                
                android.util.Log.d("WebViewViewModel", "Switching to tab $tabId - DB URL: ${tab.url}, WebView URL: ${webViewState?.url}, Using: $actualUrl")
                
                _uiState.update { state ->
                    state.copy(
                        url = actualUrl,
                        currentUrl = actualUrl,
                        title = webViewState?.title?.takeIf { t -> t.isNotBlank() } ?: tab.title,
                        videoDetected = false,
                        detectedVideoUrl = null,
                        drmDetected = DrmDetector.isKnownDrmSite(actualUrl)
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("WebViewViewModel", "❌ Failed to switch tab: $tabId", e)
                _uiState.update { it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_failed_switch_tab, e.message ?: "")) }
            }
        }
    }
    
    /**
     * Close a tab.
     * 
     * Delegates "next active tab" policy to TabManager/TabRepository to avoid
     * split-brain state from computing next tab using stale _tabs.value.
     * The repository returns the ID of the next tab to activate after closing.
     * 
     * @param tabId The ID of the tab to close
     */
    fun closeTab(tabId: String) {
        viewModelScope.launch {
            try {
                val wasCurrentTab = tabId == currentTabId
                
                // Close tab and get next tab ID from repository
                // The repository handles "next active tab" policy using fresh DB queries
                val nextTabId = tabRepository.closeTab(tabId)
                
                // Release the WebView for the closed tab to free GPU/memory resources
                // This recycles the WebView to the pool instead of leaving it paused in cache
                webViewStateManager.releaseWebViewForPooling(tabId, listOf("AndroidInterface"))
                
                if (wasCurrentTab) {
                    if (nextTabId != null) {
                        // Repository already activated the next tab, update UI state
                        currentTabId = nextTabId
                        
                        // Fetch the activated tab's data from repository
                        val nextTab = tabRepository.getTabById(nextTabId)
                        if (nextTab != null) {
                            val webViewState = webViewStateManager.getWebViewState(nextTabId)
                            val actualUrl = webViewState?.url?.takeIf { it.isNotBlank() } ?: nextTab.url
                            
                            _uiState.update { state ->
                                state.copy(
                                    url = actualUrl,
                                    currentUrl = actualUrl,
                                    title = webViewState?.title?.takeIf { t -> t.isNotBlank() } ?: nextTab.title,
                                    videoDetected = false,
                                    detectedVideoUrl = null,
                                    drmDetected = DrmDetector.isKnownDrmSite(actualUrl)
                                )
                            }
                        }
                    } else {
                        // No tabs left, clear state
                        currentTabId = null
                        _uiState.update { WebViewUiState() }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_failed_close_tab, e.message ?: "")) }
            }
        }
    }
    
    /**
     * Update the thumbnail for the current tab
     */
    fun updateTabThumbnail(thumbnailPath: String) {
        viewModelScope.launch {
            try {
                currentTabId?.let { tabId ->
                    tabRepository.updateTabThumbnail(tabId, thumbnailPath)
                }
            } catch (e: Exception) {
                // Silently fail - thumbnail update is not critical
            }
        }
    }
    
    /**
     * Update the title of the current tab
     */
    fun updateTabTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }
    
    /**
     * Update the URL of the current tab in the database
     * This ensures the tab remembers its current page when switching tabs
     */
    private fun updateCurrentTabUrl(url: String) {
        viewModelScope.launch {
            try {
                currentTabId?.let { tabId ->
                    // Only update if URL is valid and different from original
                    if (url.isNotBlank() && url.startsWith("http")) {
                        tabRepository.updateTabUrl(tabId, url)
                    }
                }
            } catch (e: Exception) {
                // Silently fail - URL update is not critical
                android.util.Log.e("WebViewViewModel", "Failed to update tab URL: ${e.message}")
            }
        }
    }
    
    /**
     * Capture thumbnail for the current tab
     */
    fun captureThumbnail(webView: android.webkit.WebView) {
        viewModelScope.launch {
            try {
                currentTabId?.let { tabId ->
                    val thumbnailPath = thumbnailCapture.captureWebViewThumbnail(webView, tabId)
                    thumbnailPath?.let {
                        tabRepository.updateTabThumbnail(tabId, it)
                    }
                }
            } catch (e: Exception) {
                // Silently fail - thumbnail capture is not critical
            }
        }
    }
    
    /**
     * Open a new tab with the given URL in the background.
     * The user stays on the current tab and the new tab is created in the background.
     */
    fun openNewTab(url: String) {
        viewModelScope.launch {
            try {
                // Validate URL before creating tab
                if (url.isBlank() || url == "null") {
                    android.util.Log.w("WebViewViewModel", "❌ Attempted to open blank tab, ignoring")
                    _uiState.update { it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_invalid_url)) }
                    return@launch
                }
                
                // Validate URL format
                val uri = try {
                    android.net.Uri.parse(url)
                } catch (e: Exception) {
                    android.util.Log.w("WebViewViewModel", "❌ Invalid URL format: $url")
                    _uiState.update { it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_invalid_url_format)) }
                    return@launch
                }
                
                // Check for valid scheme
                val scheme = uri.scheme?.lowercase()
                if (scheme !in listOf("http", "https")) {
                    android.util.Log.w("WebViewViewModel", "❌ Invalid URL scheme: $scheme")
                    _uiState.update { it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_unsupported_url_scheme)) }
                    return@launch
                }
                
                android.util.Log.d("WebViewViewModel", "🆕 Opening new tab in background with URL: $url")
                
                val title = extractTitleFromUrl(url)
                
                // Create tab in background - stays on current tab
                val tab = tabRepository.createTabInBackground(url, title)
                
                android.util.Log.d("WebViewViewModel", "✅ New background tab created with ID: ${tab.id}")
                
                // Show top toast confirmation to user
                _topToastMessage.value = context.getString(com.entertainmentbrowser.R.string.message_tab_opened_background)
                
            } catch (e: Exception) {
                android.util.Log.e("WebViewViewModel", "❌ Failed to open new tab", e)
                _uiState.update { it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_failed_open_new_tab, e.message ?: "")) }
            }
        }
    }
    
    /**
     * Add current page to bookmarks
     */
    fun addBookmark(url: String, title: String) {
        viewModelScope.launch {
            try {
                val bookmarkTitle = title.ifBlank { extractTitleFromUrl(url) }
                val isAdded = bookmarkRepository.toggleBookmark(bookmarkTitle, url)
                val message = if (isAdded) {
                    context.getString(com.entertainmentbrowser.R.string.message_bookmark_added)
                } else {
                    context.getString(com.entertainmentbrowser.R.string.message_bookmark_removed)
                }
                _uiState.update { it.copy(error = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_failed_add_bookmark, e.message ?: "")) }
            }
        }
    }
    
    /**
     * Add a specific URL to bookmarks (from context menu)
     */
    fun addUrlToBookmarks(url: String) {
        viewModelScope.launch {
            try {
                val title = extractTitleFromUrl(url)
                val isAdded = bookmarkRepository.toggleBookmark(title, url)
                val message = if (isAdded) {
                    context.getString(com.entertainmentbrowser.R.string.message_bookmark_added)
                } else {
                    context.getString(com.entertainmentbrowser.R.string.message_bookmark_removed)
                }
                _uiState.update { it.copy(error = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(com.entertainmentbrowser.R.string.error_failed_add_bookmark, e.message ?: "")) }
            }
        }
    }
}
