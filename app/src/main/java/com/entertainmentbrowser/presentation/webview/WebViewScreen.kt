package com.entertainmentbrowser.presentation.webview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.zIndex
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.webkit.WebView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

/**
 * WebView screen for browsing websites with video detection, download capabilities, and tab management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    onNavigateBack: () -> Unit,
    viewModel: WebViewViewModel = hiltViewModel(),
    settingsViewModel: com.entertainmentbrowser.presentation.settings.SettingsViewModel = hiltViewModel(),
    fastAdBlockEngine: com.entertainmentbrowser.util.adblock.FastAdBlockEngine,
    advancedAdBlockEngine: com.entertainmentbrowser.util.adblock.AdvancedAdBlockEngine,
    webViewStateManager: com.entertainmentbrowser.util.WebViewStateManager,
    downloadRepository: com.entertainmentbrowser.domain.repository.DownloadRepository
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val showInterstitialAd by viewModel.showInterstitialAd.collectAsState()
    val shouldPreloadAd by viewModel.shouldPreloadAd.collectAsState()
    val hapticFeedback = com.entertainmentbrowser.util.rememberHapticFeedback()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // Keep reference to WebView for navigation controls
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    
    // Get current active tab ID
    val currentTabId = remember(tabs) {
        tabs.find { it.isActive }?.id ?: ""
    }
    
    // Context menu state for long-press
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuUrl by remember { mutableStateOf("") }
    var contextMenuType by remember { mutableStateOf(0) }
    
    // Download dialog state
    var showDownloadDialog by remember { mutableStateOf(false) }
    
    // Tab bar visibility state for auto-hide on scroll
    var isTabBarVisible by remember { mutableStateOf(true) }
    var lastScrollY by remember { mutableStateOf(0) }
    var isAtTop by remember { mutableStateOf(true) }
    
    // Pull-to-refresh state
    var pullOffset by remember { mutableStateOf(0f) }
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshThreshold = 150f // Pull distance needed to trigger refresh
    
    // Handle pull-to-refresh trigger
    LaunchedEffect(pullOffset) {
        if (pullOffset >= refreshThreshold && !isRefreshing && isAtTop) {
            isRefreshing = true
            webViewRef?.reload()
            // Reset after reload starts
            kotlinx.coroutines.delay(800)
            isRefreshing = false
            pullOffset = 0f
        }
    }
    
    // Handle system back button - go back in WebView history if possible
    BackHandler(enabled = uiState.canGoBack) {
        webViewRef?.goBackSafe()
    }
    
    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Preload ad in background when triggered
    LaunchedEffect(shouldPreloadAd) {
        if (shouldPreloadAd) {
            android.util.Log.d("WebViewScreen", "🔄 Starting background ad preload...")
            InterstitialAdPreloader.startPreload(context) {
                android.util.Log.d("WebViewScreen", "✅ Ad preload complete, notifying ViewModel")
                viewModel.onAdPreloaded()
            }
        }
    }
    
    // Show interstitial ad dialog when ready
    if (showInterstitialAd) {
        InterstitialAdDialog(
            onDismiss = {
                viewModel.dismissInterstitialAd()
            }
        )
    }
    
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            // Show download FAB when video is detected
            if (uiState.videoDetected && !uiState.drmDetected) {
                FloatingActionButton(
                    onClick = { showDownloadDialog = true },
                    modifier = Modifier.padding(bottom = 80.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "Download video"
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress Indicator at the very top (z-index high)
            if (uiState.isLoading || uiState.loadingProgress < 100) {
                LinearProgressIndicator(
                    progress = { uiState.loadingProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                        .zIndex(10f), // Ensure it's on top of everything
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            // WebView - Only render if we have a valid tab ID
            if (currentTabId.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CustomWebView(
                        url = uiState.url,
                        tabId = currentTabId,  // Pass tab ID to preserve WebView per tab
                        isActiveTab = true,  // This tab is currently visible
                        modifier = Modifier.fillMaxSize(),
                        onWebViewCreated = { webView ->
                            webViewRef = webView
                        },
                        onVideoDetected = { videoUrl ->
                            viewModel.onEvent(WebViewEvent.VideoDetected(videoUrl))
                        },
                        onDrmDetected = {
                            viewModel.onEvent(WebViewEvent.DrmDetected)
                        },
                        onLoadingChanged = { isLoading ->
                            viewModel.onEvent(WebViewEvent.UpdateLoading(isLoading))
                        },
                        onProgressChanged = { progress ->
                            viewModel.onEvent(WebViewEvent.UpdateProgress(progress))
                        },
                        onTitleChanged = { title ->
                            viewModel.onEvent(WebViewEvent.UpdateTitle(title))
                            viewModel.updateTabTitle(title)
                        },
                        onUrlChanged = { url ->
                            viewModel.onEvent(WebViewEvent.UpdateUrl(url))
                        },
                        onNavigationStateChanged = { canGoBack, canGoForward ->
                            viewModel.onEvent(
                                WebViewEvent.UpdateNavigationState(canGoBack, canGoForward)
                            )
                        },
                        onError = { error ->
                            viewModel.onEvent(WebViewEvent.Error(error))
                        },
                        onPageFinished = {
                            // Capture thumbnail when page finishes loading
                            webViewRef?.let { webView ->
                                viewModel.captureThumbnail(webView)
                            }
                            // Track page load for interstitial ad
                            viewModel.onPageLoaded()
                        },
                        onLongPress = { url, type ->
                            contextMenuUrl = url
                            contextMenuType = type
                            showContextMenu = true
                            if (settingsState.settings.hapticFeedbackEnabled) {
                                hapticFeedback.performLongPress()
                            }
                        },
                        onScroll = { scrollY, oldScrollY ->
                            // Track if at top of page for pull-to-refresh
                            isAtTop = scrollY == 0
                            
                            // Auto-hide/show tab bar based on scroll direction
                            val scrollDelta = scrollY - oldScrollY
                            val scrollThreshold = 10 // Minimum scroll distance to trigger hide/show
                            
                            if (kotlin.math.abs(scrollDelta) > scrollThreshold) {
                                isTabBarVisible = scrollDelta < 0 // Show when scrolling up, hide when scrolling down
                            }
                            
                            lastScrollY = scrollY
                        },
                        onPullOffset = { offset ->
                            pullOffset = offset
                        },
                        onShowDownloadDialog = { url, filename ->
                            viewModel.onEvent(WebViewEvent.VideoDetected(url))
                            showDownloadDialog = true
                        },
                        fastAdBlockEngine = fastAdBlockEngine,
                        advancedAdBlockEngine = advancedAdBlockEngine,
                        webViewStateManager = webViewStateManager,
                        downloadRepository = downloadRepository
                    )
                    
                    // Show loading indicator while page is loading
                    // Removed centered indicator in favor of top linear indicator as requested
                    /*
                    if (uiState.isLoading && uiState.loadingProgress < 30) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    */
                }
            }
            
            // Bottom tab bar (matching tabs.html design) with auto-hide on scroll
            if (tabs.isNotEmpty()) {
                val tabBarOffsetY by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (isTabBarVisible) 0f else 150f, // 150dp is approximate tab bar height
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 200,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    ),
                    label = "tabBarOffset"
                )
                
                WebViewTabBar(
                    tabs = tabs,
                    onTabClick = { tab ->
                        viewModel.switchTab(tab.id)
                    },
                    onCloseTab = { tabId ->
                        if (settingsState.settings.hapticFeedbackEnabled) {
                            hapticFeedback.performContextClick()
                        }
                        viewModel.closeTab(tabId)
                        
                        // If no tabs left, navigate back
                        if (tabs.size == 1) {
                            onNavigateBack()
                        }
                    },
                    onNavigateToHome = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = tabBarOffsetY.dp)
                )
            }
            
            // Pull-to-refresh indicator
            if (isRefreshing || (isAtTop && pullOffset > 0)) {
                val progress = (pullOffset / refreshThreshold).coerceIn(0f, 1f)
                val alpha = progress.coerceIn(0f, 1f)
                
                // Material Design style Pull-to-Refresh indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = (16.dp + (pullOffset * 0.1f).dp).coerceAtMost(80.dp)) // Parallax effect
                        .offset(y = if (isRefreshing) 0.dp else (-16).dp) // Hide when not active/pulling
                        .zIndex(5f)
                ) {
                    Surface(
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .size(40.dp)
                            .scale(if (isRefreshing) 1f else progress)
                            .alpha(if (isRefreshing) 1f else alpha)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Pull to refresh",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(pullOffset * 2f) // Rotate icon as you pull
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // DRM Warning Dialog
        if (uiState.showDrmWarning) {
            DrmWarningDialog(
                onDismiss = {
                    viewModel.onEvent(WebViewEvent.DismissDrmWarning)
                }
            )
        }
        
        // Download Dialog
        if (showDownloadDialog && uiState.detectedVideoUrl != null) {
            val suggestedFilename = remember(uiState.detectedVideoUrl) {
                "video_${System.currentTimeMillis()}.mp4"
            }
            
            DownloadDialog(
                videoUrl = uiState.detectedVideoUrl!!,
                suggestedFilename = suggestedFilename,
                onConfirm = { filename, quality ->
                    viewModel.onEvent(WebViewEvent.DownloadVideoWithFilename(filename, quality))
                    showDownloadDialog = false
                },
                onDismiss = {
                    showDownloadDialog = false
                }
            )
        }
        
        // Context Menu Dialog for long-press
        if (showContextMenu && contextMenuUrl.isNotBlank()) {
            WebViewContextMenuDialog(
                url = contextMenuUrl,
                type = contextMenuType,
                onDismiss = { showContextMenu = false },
                onOpenInNewTab = {
                    if (contextMenuUrl.isNotBlank()) {
                        viewModel.openNewTab(contextMenuUrl)
                    }
                    showContextMenu = false
                },
                onCopyLink = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("URL", contextMenuUrl)
                    clipboard.setPrimaryClip(clip)
                    showContextMenu = false
                },
                onShare = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, contextMenuUrl)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share link"))
                    showContextMenu = false
                },
                onDownload = {
                    viewModel.onEvent(WebViewEvent.VideoDetected(contextMenuUrl))
                    viewModel.onEvent(WebViewEvent.DownloadVideo)
                    showContextMenu = false
                },
                onAddBookmark = {
                    viewModel.addUrlToBookmarks(contextMenuUrl)
                    showContextMenu = false
                }
            )
        }
    }
}

/**
 * Context menu dialog shown on long-press
 */
@Composable
private fun WebViewContextMenuDialog(
    url: String,
    type: Int,
    onDismiss: () -> Unit,
    onOpenInNewTab: () -> Unit,
    onCopyLink: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit,
    onAddBookmark: () -> Unit = {}
) {
    val isImage = type == android.webkit.WebView.HitTestResult.IMAGE_TYPE || 
                  type == android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
    val isVideo = url.contains(".mp4") || url.contains(".m3u8") || url.contains("video")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    isImage -> "Image Options"
                    isVideo -> "Video Options"
                    else -> "Link Options"
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show URL (truncated)
                Text(
                    text = url.take(50) + if (url.length > 50) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                // Add to Bookmarks
                TextButton(
                    onClick = onAddBookmark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add to Bookmarks")
                }
                
                // Open in new tab
                TextButton(
                    onClick = onOpenInNewTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open in new tab")
                }
                
                // Copy link
                TextButton(
                    onClick = onCopyLink,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy link")
                }
                
                // Share
                TextButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share")
                }
                
                // Download (for videos/images)
                if (isVideo || isImage) {
                    TextButton(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Download")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = Color.Black
    )
}

/**
 * Dialog shown when DRM content is detected
 */
@Composable
private fun DrmWarningDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "DRM-Protected Content",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = DrmDetector.getDrmWarningMessage(),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        containerColor = androidx.compose.ui.graphics.Color.Black
    )
}

/**
 * Bottom tab bar for WebView screen (matching tabs.html design)
 */
@Composable
private fun WebViewTabBar(
    tabs: List<com.entertainmentbrowser.domain.model.Tab>,
    onTabClick: (com.entertainmentbrowser.domain.model.Tab) -> Unit,
    onCloseTab: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Tab bar content
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home button
            IconButton(
                onClick = onNavigateToHome,
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
            
            // Scrollable tab list
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tabs.size) { index ->
                    val tab = tabs[index]
                    WebViewTabThumbnail(
                        tab = tab,
                        onClick = { onTabClick(tab) },
                        onClose = { onCloseTab(tab.id) }
                    )
                }
            }
        }
    }
}

/**
 * Individual tab thumbnail in the tab bar
 */
@Composable
private fun WebViewTabThumbnail(
    tab: com.entertainmentbrowser.domain.model.Tab,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.size(48.dp) // Increased size to accommodate shadow
    ) {
        // Thumbnail with shadow and border for active tab
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.Center)
                .shadow(
                    elevation = if (tab.isActive) 8.dp else 4.dp,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    clip = false
                )
                .clip(androidx.compose.foundation.shape.CircleShape)
                .then(
                    if (tab.isActive) {
                        Modifier.border(
                            2.dp,
                            androidx.compose.ui.graphics.Color.Red,
                            androidx.compose.foundation.shape.CircleShape
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            if (tab.thumbnailPath != null && java.io.File(tab.thumbnailPath).exists()) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(java.io.File(tab.thumbnailPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = tab.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(androidx.compose.foundation.shape.CircleShape)
                )
            } else {
                // Placeholder - no background, just text
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title.take(1).uppercase(),
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Close button on active tab - bigger and with shadow
        if (tab.isActive) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        clip = false
                    )
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(androidx.compose.ui.graphics.Color.Red)
                    .clickable(
                        onClick = onClose,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close tab",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
