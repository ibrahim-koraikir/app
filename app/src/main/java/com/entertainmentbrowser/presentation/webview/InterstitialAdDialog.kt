package com.entertainmentbrowser.presentation.webview

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.entertainmentbrowser.core.constants.Constants
import com.entertainmentbrowser.data.remote.RemoteAdConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ad Network Rotator - cycles through available ad networks (supports remote config)
 */
object AdNetworkRotator {
    @Volatile
    private var currentIndex = 0
    
    @Volatile
    private var networks: List<Constants.AdNetwork> = Constants.AD_NETWORKS
    
    /**
     * Initialize with remote config - call this on app start
     */
    fun initialize(context: android.content.Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val remoteNetworks = RemoteAdConfig.getAdNetworks(context)
                if (remoteNetworks.isNotEmpty()) {
                    networks = remoteNetworks
                    android.util.Log.d("AdNetworkRotator", "✅ Loaded ${networks.size} networks from remote config")
                    networks.forEach { 
                        android.util.Log.d("AdNetworkRotator", "  📢 ${it.name}: ${it.url}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AdNetworkRotator", "❌ Failed to load remote config, using hardcoded", e)
            }
        }
    }
    
    /**
     * Force refresh from remote
     */
    fun refresh(context: android.content.Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (RemoteAdConfig.refreshConfig(context)) {
                    networks = RemoteAdConfig.getAdNetworks(context)
                    android.util.Log.d("AdNetworkRotator", "🔄 Refreshed: ${networks.size} networks")
                }
            } catch (e: Exception) {
                android.util.Log.e("AdNetworkRotator", "❌ Refresh failed", e)
            }
        }
    }
    
    fun getNextAdNetwork(): Constants.AdNetwork {
        if (networks.isEmpty()) {
            return Constants.AD_NETWORKS.first()
        }
        val network = networks[currentIndex]
        currentIndex = (currentIndex + 1) % networks.size
        android.util.Log.d("AdNetworkRotator", "📢 Selected ad network: ${network.name} (index: ${currentIndex - 1})")
        return network
    }
    
    fun getCurrentNetwork(): Constants.AdNetwork {
        return if (networks.isNotEmpty()) networks[currentIndex] else Constants.AD_NETWORKS.first()
    }
    
    fun getNetworkCount(): Int = networks.size
    
    fun reset() {
        currentIndex = 0
    }
}

/**
 * Singleton to track if ad is preloaded (just a flag, not the WebView itself)
 */
object InterstitialAdPreloader {
    @Volatile
    private var isPreloading = false
    
    @Volatile
    private var preloadComplete = false
    
    @Volatile
    private var tempWebView: WebView? = null
    
    @Volatile
    var currentAdNetwork: Constants.AdNetwork? = null
        private set
    
    fun startPreload(context: android.content.Context, onComplete: () -> Unit) {
        android.util.Log.d("InterstitialAd", "🔄 startPreload called - isPreloading: $isPreloading, preloadComplete: $preloadComplete")
        
        // If already preloaded, call complete immediately
        if (preloadComplete) {
            android.util.Log.d("InterstitialAd", "✅ Already preloaded, calling onComplete")
            onComplete()
            return
        }
        
        // If already preloading, don't start again
        if (isPreloading) {
            android.util.Log.d("InterstitialAd", "⏳ Already preloading, waiting...")
            return
        }
        
        isPreloading = true
        
        // Get next ad network in rotation
        currentAdNetwork = AdNetworkRotator.getNextAdNetwork()
        android.util.Log.d("InterstitialAd", "🔄 Starting preload for ${currentAdNetwork?.name}...")
        
        // Create a temporary WebView just to preload/cache the URL
        // Security: Restricted WebView for ad content only - no file/content access
        try {
            tempWebView = WebView(context.applicationContext).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                    // Security hardening - restrict file and content access for ad WebViews
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    // Enable safe browsing on API 26+
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        safeBrowsingEnabled = true
                    }
                }
                
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        android.util.Log.d("InterstitialAd", "✅ Ad preloaded/cached successfully for ${currentAdNetwork?.name}")
                        preloadComplete = true
                        isPreloading = false
                        onComplete()
                        // Destroy temp WebView after preload
                        view?.post {
                            view.stopLoading()
                            view.destroy()
                        }
                        tempWebView = null
                    }
                    
                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        // Still mark as complete so we don't block forever
                        if (request?.isForMainFrame == true) {
                            android.util.Log.e("InterstitialAd", "❌ Preload failed for ${currentAdNetwork?.name}, but marking complete")
                            preloadComplete = true
                            isPreloading = false
                            onComplete()
                            tempWebView = null
                        }
                    }
                }
                
                val adUrl = currentAdNetwork?.url ?: Constants.ADSTERRA_DIRECT_LINK
                android.util.Log.d("InterstitialAd", "📡 Loading ad URL: $adUrl (${currentAdNetwork?.name})")
                loadUrl(adUrl)
            }
        } catch (e: Exception) {
            android.util.Log.e("InterstitialAd", "❌ Failed to create preload WebView", e)
            isPreloading = false
            // Still call complete to not block forever
            preloadComplete = true
            onComplete()
        }
    }
    
    fun isReady(): Boolean = preloadComplete
    
    fun reset() {
        android.util.Log.d("InterstitialAd", "🔄 Resetting preloader state")
        preloadComplete = false
        isPreloading = false
        currentAdNetwork = null
        tempWebView?.let {
            try {
                it.stopLoading()
                it.destroy()
            } catch (e: Exception) {
                android.util.Log.e("InterstitialAd", "Error destroying temp WebView", e)
            }
        }
        tempWebView = null
    }
}

/**
 * Skip button state: Countdown -> Skip >> -> X close
 */
private enum class SkipState {
    COUNTDOWN,  // Shows "Skip in 5s", "Skip in 4s", etc.
    SKIP_READY, // Shows "Skip >>"
    CLOSE_READY // Shows "X" button
}

/**
 * Full-screen interstitial ad dialog
 * Shows ads from rotating ad networks with skip flow:
 * 1. "Skip in 5s" countdown
 * 2. "Skip >>" button appears
 * 3. User taps >> to reveal X close button
 */
@Composable
fun InterstitialAdDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var secondsRemaining by remember { mutableIntStateOf(5) }
    var skipState by remember { mutableStateOf(SkipState.COUNTDOWN) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    // Get the current ad network (use preloaded one or get next)
    val currentAdNetwork = remember {
        InterstitialAdPreloader.currentAdNetwork ?: AdNetworkRotator.getNextAdNetwork()
    }
    
    android.util.Log.d("InterstitialAd", "🎬 InterstitialAdDialog composing with ${currentAdNetwork.name}...")
    
    // Countdown timer - starts after ad loads
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            while (secondsRemaining > 0) {
                delay(1000)
                secondsRemaining--
            }
            // Countdown finished, show Skip >> button
            skipState = SkipState.SKIP_READY
        }
    }
    
    // Cleanup on dismiss
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                destroy()
            }
            InterstitialAdPreloader.reset()
        }
    }
    
    Dialog(
        onDismissRequest = { 
            if (skipState == SkipState.CLOSE_READY) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = skipState == SkipState.CLOSE_READY,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Network name banner at top (no background)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentAdNetwork.name,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // WebView for ad - loads fresh but should be cached from preload
            // Security: Restricted WebView for ad content only - no file/content access
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT // Use cache
                            setSupportMultipleWindows(false)
                            // Security hardening - restrict file and content access for ad WebViews
                            allowFileAccess = false
                            allowContentAccess = false
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            // Enable safe browsing on API 26+
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = true
                            }
                        }
                        
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                android.util.Log.d("InterstitialAd", "📄 Dialog ad loaded from ${currentAdNetwork.name}")
                                isLoading = false
                            }
                            
                            // Handle Play Store links
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: android.webkit.WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false
                                
                                // Handle Play Store links
                                if (url.startsWith("market://") || url.contains("play.google.com/store/apps")) {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(url)
                                        )
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        ctx.startActivity(intent)
                                        return true
                                    } catch (e: Exception) {
                                        android.util.Log.e("InterstitialAd", "Failed to open Play Store", e)
                                    }
                                }
                                
                                // Handle intent:// URLs
                                if (url.startsWith("intent://")) {
                                    try {
                                        val intent = android.content.Intent.parseUri(
                                            url,
                                            android.content.Intent.URI_INTENT_SCHEME
                                        )
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        if (ctx.packageManager.resolveActivity(intent, 0) != null) {
                                            ctx.startActivity(intent)
                                            return true
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("InterstitialAd", "Failed to handle intent", e)
                                    }
                                }
                                
                                return false
                            }
                        }
                        
                        webChromeClient = android.webkit.WebChromeClient()
                        
                        // Load the ad URL from current network
                        android.util.Log.d("InterstitialAd", "📡 Loading ad from ${currentAdNetwork.name}: ${currentAdNetwork.url}")
                        loadUrl(currentAdNetwork.url)
                        
                        webViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp) // Leave space for network name banner
            )
            
            // Loading indicator with network name
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Loading ${currentAdNetwork.name}...",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
            
            // Top-right skip/close button with 3 states
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        when (skipState) {
                            SkipState.COUNTDOWN -> Color.Gray.copy(alpha = 0.7f)
                            SkipState.SKIP_READY -> Color(0xFF1976D2).copy(alpha = 0.9f) // Blue for skip
                            SkipState.CLOSE_READY -> Color.Black.copy(alpha = 0.8f)
                        }
                    )
                    .clickable(enabled = skipState != SkipState.COUNTDOWN) {
                        when (skipState) {
                            SkipState.SKIP_READY -> {
                                // Tap >> to reveal X button
                                skipState = SkipState.CLOSE_READY
                            }
                            SkipState.CLOSE_READY -> {
                                // Tap X to close
                                onDismiss()
                            }
                            else -> { /* Do nothing during countdown */ }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (skipState) {
                    SkipState.COUNTDOWN -> {
                        // Show "Skip in Xs" countdown
                        Text(
                            text = "Skip in ${secondsRemaining}s",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    SkipState.SKIP_READY -> {
                        // Show "Skip >>" button
                        Text(
                            text = "Skip >>",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    SkipState.CLOSE_READY -> {
                        // Show X icon to close
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close ad",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
