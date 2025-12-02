package com.entertainmentbrowser.presentation.webview

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.entertainmentbrowser.core.constants.Constants
import kotlinx.coroutines.delay

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
        android.util.Log.d("InterstitialAd", "🔄 Starting preload...")
        
        // Create a temporary WebView just to preload/cache the URL
        try {
            tempWebView = WebView(context.applicationContext).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                }
                
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        android.util.Log.d("InterstitialAd", "✅ Ad preloaded/cached successfully")
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
                            android.util.Log.e("InterstitialAd", "❌ Preload failed, but marking complete")
                            preloadComplete = true
                            isPreloading = false
                            onComplete()
                            tempWebView = null
                        }
                    }
                }
                
                android.util.Log.d("InterstitialAd", "📡 Loading ad URL: ${Constants.ADSTERRA_DIRECT_LINK}")
                loadUrl(Constants.ADSTERRA_DIRECT_LINK)
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
 * Full-screen interstitial ad dialog
 * Shows Adsterra smartlink with countdown timer before allowing close
 */
@Composable
fun InterstitialAdDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var secondsRemaining by remember { mutableStateOf(5) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    android.util.Log.d("InterstitialAd", "🎬 InterstitialAdDialog composing...")
    
    // Countdown timer - starts after ad loads
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            while (secondsRemaining > 0) {
                delay(1000)
                secondsRemaining--
            }
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
            if (secondsRemaining == 0) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = secondsRemaining == 0,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // WebView for ad - loads fresh but should be cached from preload
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT // Use cache
                            setSupportMultipleWindows(false)
                        }
                        
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                android.util.Log.d("InterstitialAd", "📄 Dialog ad loaded")
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
                        
                        // Load the ad URL (should be cached from preload)
                        loadUrl(Constants.ADSTERRA_DIRECT_LINK)
                        
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Bottom controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    isLoading -> {
                        Text(
                            text = "Loading ad...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    secondsRemaining > 0 -> {
                        Text(
                            text = "Close in $secondsRemaining seconds...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    else -> {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}
