package com.entertainmentbrowser.presentation.webview

import com.entertainmentbrowser.util.adblock.FastAdBlockEngine
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AdBlockWebViewClient.
 * Tests basic initialization and blocked count tracking.
 * Note: Full integration tests with WebView should be done as instrumented tests.
 */
class AdBlockWebViewClientTest {
    
    private lateinit var client: AdBlockWebViewClient
    private lateinit var engine: FastAdBlockEngine
    
    @Before
    fun setup() {
        engine = mockk(relaxed = true)
        // Create client with default callbacks
        client = AdBlockWebViewClient(engine)
    }
    
    @Test
    fun `client initializes with zero blocked count`() {
        assertEquals("Initial blocked count should be 0", 0, client.getBlockedCount())
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
            engine,
            onVideoDetected = { videoDetected = true },
            onDrmDetected = { drmDetected = true },
            onLoadingChanged = { loadingChanged = true },
            onUrlChanged = { urlChanged = true },
            onNavigationStateChanged = { _, _ -> navigationChanged = true },
            onError = { errorOccurred = true },
            onPageFinished = { pageFinished = true }
        )
        
        assertNotNull("Client with custom callbacks should be created", customClient)
        assertEquals("Custom client should have zero blocked count", 0, customClient.getBlockedCount())
    }
    
    @Test
    fun `getBlockedCount returns integer value`() {
        val count = client.getBlockedCount()
        assertEquals("Blocked count should be an integer", 0, count)
    }
}
