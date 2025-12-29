package com.entertainmentbrowser.presentation.webview

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.entertainmentbrowser.util.GpuMemoryManager
import com.entertainmentbrowser.util.WebViewPool
import com.entertainmentbrowser.util.WebViewStateManager
import com.entertainmentbrowser.util.adblock.AntiAdblockBypass
import com.entertainmentbrowser.util.adblock.FastAdBlockEngine
import kotlinx.coroutines.launch

/**
 * Custom WebView composable with video detection and security features
 */
@Composable
fun CustomWebView(
    url: String,
    tabId: String,  // NEW: Tab ID to identify which WebView to use
    isActiveTab: Boolean = true,  // NEW: Track if this is the active tab
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {},
    onVideoDetected: (String) -> Unit = {},
    onDrmDetected: () -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {},
    onProgressChanged: (Int) -> Unit = {},
    onTitleChanged: (String) -> Unit = {},
    onUrlChanged: (String) -> Unit = {},
    onNavigationStateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit = { _, _ -> },
    onError: (String) -> Unit = {},
    onPageLoadError: (PageErrorType, Int) -> Unit = { _, _ -> }, // Enhanced error callback
    onPageFinished: () -> Unit = {},
    onLongPress: (url: String, type: Int) -> Unit = { _, _ -> },
    onScroll: (scrollY: Int, oldScrollY: Int) -> Unit = { _, _ -> },
    onPullOffset: (Float) -> Unit = {},
    onShowDownloadDialog: (url: String, filename: String) -> Unit = { _, _ -> },
    onVideoPlayingStateChanged: (Boolean) -> Unit = {},
    fastAdBlockEngine: FastAdBlockEngine,
    advancedAdBlockEngine: com.entertainmentbrowser.util.adblock.AdvancedAdBlockEngine,
    antiAdblockBypass: AntiAdblockBypass? = null,
    webViewStateManager: WebViewStateManager,
    downloadRepository: com.entertainmentbrowser.domain.repository.DownloadRepository,
    strictAdBlockingEnabled: Boolean = false // Strict ad blocking mode from settings
) {
    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    
    // Check if WebView is already cached for this tab BEFORE getting it
    // This tells us if we're reusing an existing WebView (with preserved state)
    // or creating a new one (which needs URL loaded)
    val wasWebViewCached = remember(tabId) {
        webViewStateManager.hasWebViewForTab(tabId)
    }
    
    // Get or create WebView for this specific tab - REUSES existing WebView!
    // This preserves scroll position, page state, and navigation history
    val webView = remember(tabId) {
        webViewStateManager.getWebViewForTab(tabId) {
            WebViewPool.obtain(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    }
    
    // State to track last long-press coordinates
    var lastTouchX by remember { mutableStateOf<Float>(0f) }
    var lastTouchY by remember { mutableStateOf<Float>(0f) }
    
    /**
     * Validates URLs received from JavaScript interface to prevent abuse.
     * 
     * **Security Intent:**
     * The JavaScript interface exposes native Android functionality to web content.
     * Malicious or compromised web pages could attempt to exploit this by passing
     * crafted URLs to trigger crashes, DoS, or unexpected behavior. This function
     * provides defense-in-depth validation.
     * 
     * **Validation Rules:**
     * - Non-empty and not literal "null" string
     * - Maximum length 2048 characters (prevent DoS)
     * - No internal whitespace (URLs should be properly encoded)
     * - Only http/https schemes allowed
     * - Must have valid host (domain or IP address)
     * - Must parse as valid URI
     * 
     * **Usage:**
     * Called by WebViewJsBridge before processing URLs from JavaScript callbacks
     * (video detection, link long-press, etc.)
     * 
     * @param url The URL string received from JavaScript
     * @return true if URL is safe to process, false otherwise
     */
    fun isValidAndSafeUrl(url: String): Boolean {
        // Non-empty and not "null" string literal
        if (url.isBlank() || url == "null") {
            return false
        }
        
        // Length limit (prevent DoS attacks)
        if (url.length > 2048) {
            return false
        }
        
        // No internal whitespace (URLs should be properly encoded)
        if (url.trim() != url || url.contains(Regex("\\s"))) {
            return false
        }
        
        // Must parse as valid URI
        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return false
        }
        
        // Only http/https schemes allowed
        val scheme = uri.scheme?.lowercase()
        if (scheme !in listOf("http", "https")) {
            return false
        }
        
        // Must have valid host
        val host = uri.host
        if (host.isNullOrBlank()) {
            return false
        }
        
        return true
    }
    
    /**
     * Sanitizes JSON results from our own injected JavaScript before parsing.
     * 
     * **Security Intent:**
     * This function validates JSON returned from `evaluateJavascript()` calls to our
     * own injected scripts (e.g., element detection for long-press). While we control
     * the JavaScript, the web page's DOM content (which our scripts inspect) is untrusted.
     * Malicious pages could craft DOM elements with special characters to exploit JSON parsing.
     * 
     * **Expected Input:**
     * Small, flat JSON objects from our injected scripts, typically:
     * - `{"type": "video", "url": "https://..."}`
     * - `{"type": "link", "url": "https://..."}`
     * 
     * **Validation Rules:**
     * - Non-empty and not literal "null"/"undefined"
     * - Must be JSON object format (starts with `{`, ends with `}`)
     * - Maximum length 4096 characters (prevent DoS)
     * - Only ASCII printable characters (prevents control character injection)
     * 
     * **Note:**
     * This is defense-in-depth. The primary security boundary is `isValidAndSafeUrl()`
     * which validates the extracted URL values. This function prevents malformed JSON
     * from reaching the parser.
     * 
     * @param jsonResult Raw string result from evaluateJavascript()
     * @return Sanitized JSON string ready for parsing, or null if invalid
     */
    fun sanitizeJsonResult(jsonResult: String?): String? {
        if (jsonResult.isNullOrBlank()) return null
        
        // Clean up JavaScript string wrapping
        val cleaned = jsonResult.trim('"').replace("\\\"", "\"").trim()
        
        // Reject empty or null-like results
        if (cleaned.isEmpty() || cleaned == "null" || cleaned == "undefined") {
            return null
        }
        
        // Must be JSON object format
        if (!cleaned.startsWith("{") || !cleaned.endsWith("}")) {
            return null
        }
        
        // Length limit (prevent DoS)
        if (cleaned.length > 4096) {
            return null
        }
        
        // Only ASCII printable characters (prevents control character injection)
        val hasControlChars = cleaned.any { char ->
            val code = char.code
            code < 0x20 || code > 0x7E
        }
        if (hasControlChars) {
            android.util.Log.w("CustomWebView", "JSON contains control characters, rejecting")
            return null
        }
        
        return cleaned
    }
    
    // JavaScript interface for video and DRM detection
    val jsInterface = remember {
        WebViewJsBridge(
            onVideoDetected = onVideoDetected,
            onDrmDetected = onDrmDetected,
            onLongPress = onLongPress,
            urlValidator = ::isValidAndSafeUrl,
            onVideoPlayingStateChanged = onVideoPlayingStateChanged
        )
    }
    
    // Configure WebView ONCE when first created for this tab
    // Note: Basic settings are already configured by WebViewPool
    DisposableEffect(tabId) {
        webView.apply {
            // Set dark background to match app theme
            setBackgroundColor(android.graphics.Color.parseColor("#121212"))
            
            // Configure hardware acceleration based on GPU memory availability
            GpuMemoryManager.configureWebView(this, tabId, isActiveTab)
            
            // Add JavaScript interface for video detection
            // Note: All methods in jsInterface are properly annotated with @JavascriptInterface
            // Lint false positive - annotations are present on lines 120-122, 145-147, 154-156
            @Suppress("JavascriptInterface")
            addJavascriptInterface(jsInterface, "AndroidInterface")
            
            // Set download listener to handle file downloads
            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                try {
                    // Generate filename from URL or content disposition
                    val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
                    
                    android.util.Log.d("CustomWebView", "Download triggered: $filename, mimetype: $mimetype")
                    
                    // Check if this is a video download
                    if (mimetype.startsWith("video/", ignoreCase = true)) {
                        // Show quality selection dialog for videos
                        onShowDownloadDialog(url, filename)
                    } else {
                        // For non-video downloads, use repository with default quality
                        coroutineScope.launch {
                            try {
                                downloadRepository.startDownload(
                                    url = url,
                                    filename = filename,
                                    quality = "default"
                                )
                                
                                android.util.Log.d("CustomWebView", "Download started via repository: $filename")
                                
                                // Show toast notification
                                android.widget.Toast.makeText(
                                    context,
                                    "Downloading $filename",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                android.util.Log.e("CustomWebView", "Failed to start download via repository", e)
                                android.widget.Toast.makeText(
                                    context,
                                    "Download failed: ${e.message}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CustomWebView", "Failed to process download", e)
                    android.widget.Toast.makeText(
                        context,
                        "Download failed: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            
            // Track touch events for long-press and pull-to-refresh
            var initialTouchY = 0f
            var isPulling = false
            var hasMoved = false
            
            setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        lastTouchX = event.x
                        lastTouchY = event.y
                        initialTouchY = event.y
                        isPulling = scrollY == 0 // Only allow pull if at top
                        hasMoved = false
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        hasMoved = true
                        if (isPulling && scrollY == 0) {
                            val pullDistance = event.y - initialTouchY
                            if (pullDistance > 20) { // Only trigger after 20px to avoid interfering with clicks
                                onPullOffset(pullDistance)
                            }
                        }
                    }
                    android.view.MotionEvent.ACTION_UP,
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        isPulling = false
                        onPullOffset(0f)
                    }
                }
                false // Don't consume the event - let WebView handle it
            }
            
            // Handle long-press for context menu
            setOnLongClickListener {
                val result = hitTestResult
                val hitTestUrl = result.extra
                
                android.util.Log.d("CustomWebView", "Long press detected - HitTest type: ${result.type}, URL: $hitTestUrl")
                android.util.Log.d("CustomWebView", "Touch coordinates: x=$lastTouchX, y=$lastTouchY")
                
                // Get WebView scroll position to calculate correct document coordinates
                val scrollX = scrollX
                val scrollY = scrollY
                val scale = scale
                
                // Convert touch coordinates to document coordinates
                // Touch coordinates are relative to WebView, need to account for scroll and scale
                val docX = (lastTouchX / scale).toInt()
                val docY = (lastTouchY / scale).toInt()
                
                android.util.Log.d("CustomWebView", "Document coordinates: x=$docX, y=$docY (scale=$scale)")
                
                // Try to detect if we're on a video or link element using JavaScript
                evaluateJavascript("""
                    (function() {
                        var x = $docX;
                        var y = $docY;
                        
                        console.log('CustomWebView JS: Looking for element at x=' + x + ', y=' + y);
                        
                        var element = document.elementFromPoint(x, y);
                        
                        console.log('CustomWebView JS: Found element:', element ? element.tagName : 'null');
                        
                        if (!element) {
                            console.log('CustomWebView JS: No element found at coordinates');
                            return '';
                        }
                        
                        // FIRST: Always check for parent <a> link - this handles images/thumbnails inside links
                        // This is the most common case for video thumbnails
                        var linkParent = element.closest('a');
                        if (linkParent && linkParent.href) {
                            var href = linkParent.href;
                            // Make sure it's not just a javascript: or # link
                            if (href && !href.startsWith('javascript:') && href !== '#' && !href.endsWith('#')) {
                                console.log('CustomWebView JS: Found parent <a> link:', href);
                                return JSON.stringify({type: 'link', url: href});
                            }
                        }
                        
                        // Check if element itself is a link
                        if (element.tagName === 'A' && element.href) {
                            var href = element.href;
                            if (href && !href.startsWith('javascript:') && href !== '#') {
                                console.log('CustomWebView JS: Found direct link:', href);
                                return JSON.stringify({type: 'link', url: href});
                            }
                        }
                        
                        // Check if it's a video element
                        if (element.tagName === 'VIDEO') {
                            return JSON.stringify({type: 'video', url: element.currentSrc || element.src || ''});
                        }
                        
                        // Check if it's inside a video element (like poster or controls)
                        var videoParent = element.closest('video');
                        if (videoParent) {
                            return JSON.stringify({type: 'video', url: videoParent.currentSrc || videoParent.src || ''});
                        }
                        
                        // Check if it's a source element inside video
                        if (element.tagName === 'SOURCE' && element.parentElement && element.parentElement.tagName === 'VIDEO') {
                            return JSON.stringify({type: 'video', url: element.src || ''});
                        }
                        
                        // Check for onclick handlers that might contain URLs (traverse up to 10 levels)
                        var currentEl = element;
                        for (var i = 0; i < 10 && currentEl; i++) {
                            // Check onclick attribute
                            var onclick = currentEl.getAttribute && currentEl.getAttribute('onclick');
                            if (onclick) {
                                var urlMatch = onclick.match(/https?:\/\/[^\s'")\]]+/);
                                if (urlMatch) {
                                    console.log('CustomWebView JS: Found onclick URL:', urlMatch[0]);
                                    return JSON.stringify({type: 'link', url: urlMatch[0]});
                                }
                            }
                            
                            // Check data attributes that might contain URLs
                            if (currentEl.dataset) {
                                var dataUrl = currentEl.dataset.href || currentEl.dataset.url || 
                                              currentEl.dataset.link || currentEl.dataset.src ||
                                              currentEl.dataset.videoUrl || currentEl.dataset.pageUrl;
                                if (dataUrl && dataUrl.startsWith('http')) {
                                    console.log('CustomWebView JS: Found data attribute URL:', dataUrl);
                                    return JSON.stringify({type: 'link', url: dataUrl});
                                }
                            }
                            
                            // Check for href attribute (some elements use href without being <a>)
                            var href = currentEl.getAttribute && currentEl.getAttribute('href');
                            if (href && href.startsWith('http')) {
                                console.log('CustomWebView JS: Found href attribute:', href);
                                return JSON.stringify({type: 'link', url: href});
                            }
                            
                            currentEl = currentEl.parentElement;
                        }
                        
                        console.log('CustomWebView JS: No link found after checking all options');
                        return '';
                    })();
                """.trimIndent()) { jsonResult ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        var urlToUse: String? = null
                        var typeToUse = android.webkit.WebView.HitTestResult.UNKNOWN_TYPE
                        
                        val hitTestType = result.type
                        val isImageType = hitTestType == android.webkit.WebView.HitTestResult.IMAGE_TYPE
                        val isAnchorType = hitTestType == android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE
                        val isImageAnchorType = hitTestType == android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
                        
                        android.util.Log.d("CustomWebView", "📋 JS Result: $jsonResult")
                        android.util.Log.d("CustomWebView", "📋 HitTest URL: $hitTestUrl, Type: $hitTestType (isImage=$isImageType, isAnchor=$isAnchorType, isImageAnchor=$isImageAnchorType)")
                        
                        // PRIORITY 1: Try JavaScript result FIRST
                        // JavaScript can find parent <a> links when user taps on an image/thumbnail
                        // This is crucial for video thumbnails that are wrapped in links
                        val sanitizedJson = sanitizeJsonResult(jsonResult)
                        if (sanitizedJson != null) {
                            try {
                                val json = org.json.JSONObject(sanitizedJson)
                                val jsUrl = json.optString("url", "")
                                val jsType = json.optString("type", "")
                                
                                if (jsUrl.isNotEmpty() && jsUrl != "null" && isValidAndSafeUrl(jsUrl)) {
                                    urlToUse = jsUrl
                                    typeToUse = if (jsType == "video") {
                                        android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE
                                    } else {
                                        android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE
                                    }
                                    android.util.Log.d("CustomWebView", "✅ Got URL from JavaScript (priority): $jsUrl (type=$jsType)")
                                }
                            } catch (e: org.json.JSONException) {
                                android.util.Log.d("CustomWebView", "Malformed JSON from JS, trying HitTest")
                            } catch (e: Exception) {
                                android.util.Log.d("CustomWebView", "Failed to parse JS result: ${e.javaClass.simpleName}")
                            }
                        }
                        
                        // PRIORITY 2: Use HitTestResult for SRC_IMAGE_ANCHOR_TYPE
                        // This means an image inside a link - HitTest gives us the link URL directly
                        if (urlToUse == null && hitTestUrl != null && hitTestUrl.isNotEmpty() && isImageAnchorType) {
                            if (isValidAndSafeUrl(hitTestUrl)) {
                                urlToUse = hitTestUrl
                                typeToUse = hitTestType
                                android.util.Log.d("CustomWebView", "✅ Using HitTest URL (image-anchor): $hitTestUrl")
                            }
                        }
                        
                        // PRIORITY 3: Use HitTestResult for pure anchor type
                        if (urlToUse == null && hitTestUrl != null && hitTestUrl.isNotEmpty() && isAnchorType) {
                            if (isValidAndSafeUrl(hitTestUrl)) {
                                urlToUse = hitTestUrl
                                typeToUse = hitTestType
                                android.util.Log.d("CustomWebView", "✅ Using HitTest URL (anchor): $hitTestUrl")
                            }
                        }
                        
                        // PRIORITY 4 (LAST RESORT): Use image URL only if nothing else found
                        // This is for standalone images not wrapped in links
                        if (urlToUse == null && hitTestUrl != null && hitTestUrl.isNotEmpty() && isImageType) {
                            if (isValidAndSafeUrl(hitTestUrl)) {
                                urlToUse = hitTestUrl
                                typeToUse = hitTestType
                                android.util.Log.d("CustomWebView", "⚠️ Using HitTest image URL (last resort): $hitTestUrl")
                            }
                        }
                        
                        // Only trigger long-press callback if we have a valid URL
                        if (urlToUse != null) {
                            android.util.Log.d("CustomWebView", "📍 Triggering long-press with URL: $urlToUse")
                            onLongPress(urlToUse, typeToUse)
                        } else {
                            android.util.Log.d("CustomWebView", "❌ No valid URL found for long-press")
                        }
                    }
                }
                
                true // Consume the event
            }
            
            // Add scroll listener for auto-hide tab bar with debouncing
            // Debounce scroll events to improve performance on video-heavy sites (TikTok, Reels, Shorts)
            var lastScrollTime = 0L
            val scrollDebounceMs = 50L // Only process scroll events every 50ms
            
            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastScrollTime >= scrollDebounceMs) {
                    lastScrollTime = currentTime
                    onScroll(scrollY, oldScrollY)
                }
            }
            
            // Set AdBlockWebViewClient with ad-blocking and all existing functionality
            // FastAdBlockEngine is injected as singleton - shared across all tabs!
            webViewClient = AdBlockWebViewClient(
                context = context,
                fastEngine = fastAdBlockEngine,
                advancedEngine = advancedAdBlockEngine,
                antiAdblockBypass = antiAdblockBypass,
                onVideoDetected = onVideoDetected,
                onLoadingChanged = onLoadingChanged,
                onNavigationStateChanged = onNavigationStateChanged,
                onError = onError,
                onPageFinished = { url ->
                    // Call the original onPageFinished
                    onPageFinished()
                },
                onPageLoadError = onPageLoadError, // Pass enhanced error callback
                isAdBlockingEnabled = true, // Ad blocking always enabled
                strictAdBlockingEnabled = strictAdBlockingEnabled // Pass strict mode setting
            )
            
            // Set WebChromeClient for fullscreen video support and progress
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    onProgressChanged(newProgress)
                }
                
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    title?.let { onTitleChanged(it) }
                }
                
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    super.onShowCustomView(view, callback)
                    // Handle fullscreen video
                    // This would require activity-level handling for true fullscreen
                }
                
                override fun onHideCustomView() {
                    super.onHideCustomView()
                    // Restore normal view
                }
                
                // Prevent websites from opening new tabs/windows
                // When a site tries to open a new window (target="_blank" or window.open()),
                // load it in the current tab instead
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message?
                ): Boolean {
                    // Get the URL that wants to open in a new window
                    val result = view?.hitTestResult
                    val data = result?.extra
                    
                    android.util.Log.d("CustomWebView", "onCreateWindow called - URL: $data, isUserGesture: $isUserGesture")
                    
                    // If we have a URL, load it in the current tab instead of creating a new one
                    if (data != null && data.isNotEmpty()) {
                        view?.loadUrl(data)
                        android.util.Log.d("CustomWebView", "Loading URL in current tab instead of new window: $data")
                    }
                    
                    // Return false to prevent creating a new window
                    return false
                }

                /**
                 * Handle permission requests (e.g. WebRTC, Geolocation)
                 * 
                 * SECURITY HARDENING:
                 * Explicitly deny camera and microphone access to prevent WebRTC leaks.
                 * This prevents malicious sites from discovering real IP addresses via STUN/TURN
                 * even when using a VPN.
                 */
                override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                    if (request == null) return
                    
                    val resources = request.resources
                    val safeResources = mutableListOf<String>()
                    
                    for (resource in resources) {
                        // Block camera and microphone (WebRTC leak vectors)
                        if (resource == android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                            resource == android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                            android.util.Log.w("CustomWebView", "🚫 Blocked WebRTC permission request: $resource")
                            continue
                        }
                        
                        // Block protected media identifier (DRM fingerprinting risk)
                        if (resource == android.webkit.PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID) {
                            android.util.Log.w("CustomWebView", "🚫 Blocked Protected Media ID request")
                            continue
                        }
                        
                        // Allow other resources if needed (e.g. MIDI, sensors)
                        // For now, we only add them if explicitly safe
                        // safeResources.add(resource)
                    }
                    
                    if (safeResources.isNotEmpty()) {
                        request.grant(safeResources.toTypedArray())
                    } else {
                        request.deny()
                    }
                }
            }
        }
        
        // Notify that WebView is created
        onWebViewCreated(webView)
        
        // Resume WebView when this tab becomes active
        webViewStateManager.resumeWebView(tabId)
        
        // Clean up when tab is disposed
        onDispose {
            // Pause WebView to save battery when tab is not visible
            webViewStateManager.pauseWebView(tabId)
            webViewStateManager.saveWebViewState(tabId)
            // Release GPU resources
            GpuMemoryManager.releaseTab(tabId)
        }
    }
    
    // Update hardware acceleration when active state changes
    LaunchedEffect(isActiveTab) {
        GpuMemoryManager.configureWebView(webView, tabId, isActiveTab)
    }
    
    // Load URL when it changes or on first creation
    // Track the last loaded URL per tab to avoid unnecessary reloads
    var lastLoadedUrlForTab by remember(tabId) { mutableStateOf<String?>(null) }
    
    LaunchedEffect(url, tabId) {
        // Get the current URL in the WebView
        val currentWebViewUrl = webView.url
        
        // Check if WebView already has content loaded (not blank/empty)
        val webViewHasContent = !currentWebViewUrl.isNullOrBlank() && 
                                currentWebViewUrl != "about:blank"
        
        // Check if this is a "new" tab context (first time seeing this tabId in this composable instance)
        val isNewTabContext = lastLoadedUrlForTab == null
        
        // CRITICAL FIX for tab switching bug:
        // - If WebView was CACHED for this tab -> it has the correct content, preserve it
        // - If WebView was NOT cached (new/recycled) -> need to load the URL
        //
        // When wasWebViewCached is true, the WebView already has this tab's content
        // (user's navigation history, scroll position, etc.) - don't reload!
        //
        // When wasWebViewCached is false, the WebView is new or recycled from pool,
        // so we need to load the correct URL for this tab.
        val shouldPreserveContent = wasWebViewCached && webViewHasContent
        
        // Load URL if:
        // 1. URL is not blank
        // 2. AND we should NOT preserve content (WebView is new/recycled)
        // 3. OR WebView has no URL yet
        // 4. OR WebView is on about:blank
        val shouldLoad = url.isNotBlank() && (
            !shouldPreserveContent ||
            currentWebViewUrl.isNullOrBlank() ||
            currentWebViewUrl == "about:blank"
        )
        
        if (shouldLoad) {
            android.util.Log.d("CustomWebView", "Loading URL for tab $tabId: $url (wasCached=$wasWebViewCached, isNewContext=$isNewTabContext, WebView was: $currentWebViewUrl)")
            webView.loadUrl(url)
            lastLoadedUrlForTab = url
        } else {
            android.util.Log.d("CustomWebView", "Tab $tabId - Preserving cached WebView state, current URL: $currentWebViewUrl (wasCached=$wasWebViewCached)")
            // Update lastLoadedUrlForTab to match what's in the WebView
            lastLoadedUrlForTab = currentWebViewUrl
        }
    }
    
    // Use key() to force recomposition when tabId changes
    // This ensures the correct WebView is displayed for each tab
    androidx.compose.runtime.key(tabId) {
        AndroidView(
            factory = { context ->
                // CRITICAL: Remove WebView from any existing parent before adding
                // This must happen in the factory to prevent "child already has parent" crash
                try {
                    (webView.parent as? ViewGroup)?.removeView(webView)
                } catch (e: Exception) {
                    android.util.Log.e("CustomWebView", "Error removing WebView from parent", e)
                }
                webView
            },
            update = { view ->
                // Update block - called on recomposition
                // Ensure WebView is properly attached
            },
            modifier = modifier
        )
    }
}

/**
 * Extension functions for WebView control
 */
fun WebView.goBackSafe() {
    if (canGoBack()) {
        goBack()
    }
}

fun WebView.goForwardSafe() {
    if (canGoForward()) {
        goForward()
    }
}

fun WebView.refreshPage() {
    reload()
}
