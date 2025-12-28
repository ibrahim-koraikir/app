package com.entertainmentbrowser.presentation.webview

import android.content.Context
import com.entertainmentbrowser.util.adblock.AdvancedAdBlockEngine
import com.entertainmentbrowser.util.adblock.AntiAdblockBypass
import com.entertainmentbrowser.util.adblock.FastAdBlockEngine
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AdBlockWebViewClient.
 * Tests basic initialization.
 * Note: Full integration tests with WebView should be done as instrumented tests.
 */
class AdBlockWebViewClientTest {
    
    private lateinit var client: AdBlockWebViewClient
    private lateinit var context: Context
    private lateinit var fastEngine: FastAdBlockEngine
    private lateinit var advancedEngine: AdvancedAdBlockEngine
    private lateinit var antiAdblockBypass: AntiAdblockBypass
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        fastEngine = mockk(relaxed = true)
        advancedEngine = mockk(relaxed = true)
        antiAdblockBypass = mockk(relaxed = true)
        // Create client with mocked dependencies
        client = AdBlockWebViewClient(
            context = context,
            fastEngine = fastEngine,
            advancedEngine = advancedEngine,
            antiAdblockBypass = antiAdblockBypass
        )
    }
    
    @Test
    fun `client is created successfully`() {
        assertNotNull("Client should be created", client)
    }
    
    @Test
    fun `client can be created with custom callbacks`() {
        var videoDetected = false
        var drmDetected = false
        var loadingChanged = false
        var urlChanged = false
        var navigationChanged = false
        var errorOccurred = false
        var pageFinished = false
        
        val customClient = AdBlockWebViewClient(
            context = context,
            fastEngine = fastEngine,
            advancedEngine = advancedEngine,
            antiAdblockBypass = antiAdblockBypass,
            onVideoDetected = { videoDetected = true },
            onDrmDetected = { drmDetected = true },
            onLoadingChanged = { loadingChanged = true },
            onUrlChanged = { urlChanged = true },
            onNavigationStateChanged = { _, _ -> navigationChanged = true },
            onError = { errorOccurred = true },
            onPageFinished = { pageFinished = true }
        )
        
        assertNotNull("Client with custom callbacks should be created", customClient)
    }
    
    @Test
    fun `client can be created with ad blocking disabled`() {
        val clientWithAdBlockDisabled = AdBlockWebViewClient(
            context = context,
            fastEngine = fastEngine,
            advancedEngine = advancedEngine,
            antiAdblockBypass = antiAdblockBypass,
            isAdBlockingEnabled = false
        )
        
        assertNotNull("Client with ad blocking disabled should be created", clientWithAdBlockDisabled)
    }
    
    @Test
    fun `client can be created with strict ad blocking enabled`() {
        val clientWithStrictMode = AdBlockWebViewClient(
            context = context,
            fastEngine = fastEngine,
            advancedEngine = advancedEngine,
            antiAdblockBypass = antiAdblockBypass,
            strictAdBlockingEnabled = true
        )
        
        assertNotNull("Client with strict mode should be created", clientWithStrictMode)
    }
    
    @Test
    fun `client can be created without antiAdblockBypass`() {
        val clientWithoutBypass = AdBlockWebViewClient(
            context = context,
            fastEngine = fastEngine,
            advancedEngine = advancedEngine,
            antiAdblockBypass = null
        )
        
        assertNotNull("Client without antiAdblockBypass should be created", clientWithoutBypass)
    }
}
