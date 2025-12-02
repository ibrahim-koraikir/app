package com.entertainmentbrowser.presentation.webview

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entertainmentbrowser.domain.repository.DownloadRepository
import com.entertainmentbrowser.presentation.navigation.Screen
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
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()
    
    private val _tabs = MutableStateFlow<List<com.entertainmentbrowser.domain.model.Tab>>(emptyList())
    val tabs: StateFlow<List<com.entertainmentbrowser.domain.model.Tab>> = _tabs.asStateFlow()
    
    private var currentTabId: String? = null
    
    // Interstitial ad counter with background preloading
    private var pageLoadCount = 0
    private val preloadThreshold = 2  // Start preloading on page 2
    private val showThreshold = 3     // Show ad on page 3+ (if preloaded)
    
    private val _showInterstitialAd = MutableStateFlow(false)
    val showInterstitialAd: StateFlow<Boolean> = _showInterstitialAd.asStateFlow()
    
    // Signal to start preloading in background
    private val _shouldPreloadAd = MutableStateFlow(false)
    val shouldPreloadAd: StateFlow<Boolean> = _shouldPreloadAd.asStateFlow()
    
    // Track if ad is ready to show
    private var isAdReady = false
    
    init {
        // Get URL from navigation arguments
        val encodedUrl = savedStateHandle.get<String>(Screen.WebView.ARG_URL)
        val decodedUrl = encodedUrl?.let { android.net.Uri.decode(it) } ?: ""
        
        // ALWAYS observe tabs first - needed for both monetized and standard tabs
        observeTabs()
        
        if (decodedUrl.startsWith("monetized:")) {
            // Handle monetized tab request
            val realUrl = decodedUrl.removePrefix("monetized:")
            val sanitizedUrl = sanitizeUrl(realUrl)
            
            // Set initial URL in UI state BEFORE opening tab
            _uiState.update { it.copy(url = sanitizedUrl, currentUrl = sanitizedUrl) }
            
            // Check if URL is from known DRM site
            if (DrmDetector.isKnownDrmSite(sanitizedUrl)) {
                _uiState.update { it.copy(drmDetected = true) }
            }
            
            // Open monetized tab
            openMonetizedTab(sanitizedUrl)
        } else {
            // Standard tab request
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
     * Create a new tab for the given URL
     */
    private fun createTabForUrl(url: String) {
        viewModelScope.launch {
            try {
                val uri = try { android.net.Uri.parse(url) } catch (_: Exception) { null }
                if (uri == null || uri.host.isNullOrBlank()) {
                    _uiState.update { it.copy(error = "Invalid URL") }
                    return@launch
                }
                val title = extractTitleFromUrl(url)
                val tab = tabRepository.createTab(url, title)
                currentTabId = tab.id
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create tab: ${e.message}") }
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
            uri.host?.replace("www.", "") ?: "New Tab"
        } catch (e: Exception) {
            "New Tab"
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
                    error = "Video format not supported for download: ${VideoDetector.getFormatName(videoUrl)}"
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
                // Permission denied - show user-friendly message
                _uiState.update { 
                    it.copy(error = "Storage permission required. Please grant permission in Settings to download videos.")
                }
            } catch (e: IllegalArgumentException) {
                // Invalid URL
                _uiState.update { 
                    it.copy(error = "Invalid download URL")
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to start download: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Start downloading the detected video with custom filename and quality
     */
    private fun startDownloadWithFilename(filename: String, quality: String) {
        val videoUrl = _uiState.value.detectedVideoUrl ?: return
        
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
                        error = "Download started: $filename ($quality)"
                    )
                }
            } catch (e: SecurityException) {
                // Permission denied - show user-friendly message
                _uiState.update { 
                    it.copy(error = "Storage permission required. Please grant permission in Settings to download videos.")
                }
            } catch (e: IllegalArgumentException) {
                // Invalid URL
                _uiState.update { 
                    it.copy(error = "Invalid download URL")
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to start download: ${e.message}")
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
        val title = _uiState.value.title.ifEmpty { "Check out this website" }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_SUBJECT, title)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, "Share via")
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
     * Track page load for interstitial ad with background preloading
     * - Page 2: Start preloading ad in background
     * - Page 3+: Show ad instantly if preloaded
     */
    fun onPageLoaded() {
        pageLoadCount++
        android.util.Log.d("WebViewViewModel", "📊 Page load count: $pageLoadCount (preload@$preloadThreshold, show@$showThreshold, ready=$isAdReady)")
        
        // Start preloading on page 2
        if (pageLoadCount >= preloadThreshold && !_shouldPreloadAd.value && !isAdReady) {
            android.util.Log.d("WebViewViewModel", "🔄 Starting ad preload in background...")
            _shouldPreloadAd.value = true
        }
        
        // Show ad on page 3+ ONLY if ad is ready
        if (pageLoadCount >= showThreshold && isAdReady) {
            android.util.Log.d("WebViewViewModel", "💰 Showing interstitial ad NOW! (preloaded and ready)")
            _showInterstitialAd.value = true
            _shouldPreloadAd.value = false
            isAdReady = false
            pageLoadCount = 0  // Reset counter for next cycle
        } else if (pageLoadCount >= showThreshold && !isAdReady) {
            android.util.Log.d("WebViewViewModel", "⏳ Threshold reached but ad not ready yet, waiting...")
            // Don't reset counter - keep waiting for ad to be ready
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
     * Switch to a different tab
     */
    fun switchTab(tabId: String) {
        viewModelScope.launch {
            try {
                tabRepository.switchTab(tabId)
                currentTabId = tabId
                
                // Get the tab's stored URL from database
                val tab = _tabs.value.find { it.id == tabId }
                
                // Also check if WebView has a different (more current) URL
                val webViewState = webViewStateManager.getWebViewState(tabId)
                val actualUrl = webViewState?.url?.takeIf { it.isNotBlank() } ?: tab?.url ?: ""
                
                android.util.Log.d("WebViewViewModel", "Switching to tab $tabId - DB URL: ${tab?.url}, WebView URL: ${webViewState?.url}, Using: $actualUrl")
                
                tab?.let {
                    _uiState.update { state ->
                        state.copy(
                            url = actualUrl,  // Use the actual WebView URL if available
                            currentUrl = actualUrl,
                            title = webViewState?.title?.takeIf { t -> t.isNotBlank() } ?: it.title,
                            videoDetected = false,
                            detectedVideoUrl = null,
                            drmDetected = DrmDetector.isKnownDrmSite(actualUrl)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to switch tab: ${e.message}") }
            }
        }
    }
    
    /**
     * Close a tab
     */
    fun closeTab(tabId: String) {
        viewModelScope.launch {
            try {
                tabRepository.closeTab(tabId)
                
                // If we closed the current tab, switch to another tab or clear state
                if (tabId == currentTabId) {
                    val remainingTabs = _tabs.value.filter { it.id != tabId }
                    if (remainingTabs.isNotEmpty()) {
                        // Switch to the first remaining tab
                        switchTab(remainingTabs.first().id)
                    } else {
                        // No tabs left, clear state
                        currentTabId = null
                        _uiState.update {
                            WebViewUiState()
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to close tab: ${e.message}") }
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
     * Open a new tab with the given URL
     * Handles monetization ad interception
     */
    fun openNewTab(url: String) {
        viewModelScope.launch {
            try {
                // Validate URL before creating tab
                if (url.isBlank() || url == "null") {
                    android.util.Log.w("WebViewViewModel", "❌ Attempted to open blank tab, ignoring")
                    _uiState.update { it.copy(error = "Invalid URL") }
                    return@launch
                }
                
                // Validate URL format
                val uri = try {
                    android.net.Uri.parse(url)
                } catch (e: Exception) {
                    android.util.Log.w("WebViewViewModel", "❌ Invalid URL format: $url")
                    _uiState.update { it.copy(error = "Invalid URL format") }
                    return@launch
                }
                
                // Check for valid scheme
                val scheme = uri.scheme?.lowercase()
                if (scheme !in listOf("http", "https")) {
                    android.util.Log.w("WebViewViewModel", "❌ Invalid URL scheme: $scheme")
                    _uiState.update { it.copy(error = "Only HTTP/HTTPS URLs are supported") }
                    return@launch
                }
                
                android.util.Log.d("WebViewViewModel", "✅ Opening new tab with URL: $url")
                
                val title = extractTitleFromUrl(url)
                val tab = tabRepository.createTab(url, title)
                // Switch to the new tab
                switchTab(tab.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to open new tab: ${e.message}") }
            }
        }
    }
    
    /**
     * Open a new tab specifically for monetization (AdBlock disabled)
     */
    fun openMonetizedTab(url: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("WebViewViewModel", "💰 Opening Monetized Tab: $url")
                
                val title = "Sponsored"
                val tab = tabRepository.createTab(url, title)
                
                // Mark this tab as monetized in the state manager
                // This will be read by CustomWebView to disable AdBlock
                webViewStateManager.setMonetized(tab.id, true)
                
                // Switch to the new tab
                switchTab(tab.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to open sponsored tab: ${e.message}") }
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
                val message = if (isAdded) "Bookmark added" else "Bookmark removed"
                _uiState.update { it.copy(error = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add bookmark: ${e.message}") }
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
                val message = if (isAdded) "Bookmark added" else "Bookmark removed"
                _uiState.update { it.copy(error = message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add bookmark: ${e.message}") }
            }
        }
    }
}
