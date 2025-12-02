package com.entertainmentbrowser.util.adblock

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FastAdBlockEngine.
 * Tests URL blocking logic and domain extraction.
 * Note: Tests that require filter list loading are simplified due to singleton pattern.
 */
class FastAdBlockEngineTest {
    
    private lateinit var context: Context
    private lateinit var engine: FastAdBlockEngine
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        
        // Mock assets to return empty streams (engine won't be initialized)
        every { context.assets.open(any()) } throws Exception("No assets in unit test")
        every { context.applicationContext } returns context
        
        engine = FastAdBlockEngine(context)
    }
    
    @Test
    fun `shouldBlock returns false when not initialized`() {
        // Engine is not initialized (no filter lists loaded)
        val result = engine.shouldBlock("https://example.com")
        
        assertFalse("Should return false when not initialized", result)
    }
    
    @Test
    fun `shouldBlock handles malformed URLs gracefully`() {
        // Test various malformed URLs
        val malformedUrls = listOf(
            "",
            "not-a-url",
            "://missing-protocol",
            "http://",
            "https://",
            "ftp://unsupported.com"
        )
        
        malformedUrls.forEach { url ->
            val result = engine.shouldBlock(url)
            assertFalse("Should handle malformed URL gracefully: $url", result)
        }
    }
    
    @Test
    fun `extractDomain returns correct domain from various URL formats`() {
        // Use reflection to access private extractDomain method
        val extractDomainMethod = FastAdBlockEngine::class.java.getDeclaredMethod(
            "extractDomain",
            String::class.java
        )
        extractDomainMethod.isAccessible = true
        
        val testCases = mapOf(
            "https://example.com/path" to "example.com",
            "http://sub.example.com:8080/path" to "sub.example.com",
            "//cdn.example.com/file.js" to "cdn.example.com",
            "https://example.com" to "example.com",
            "http://example.com/" to "example.com",
            "https://example.com?query=value" to "example.com",
            "https://sub.domain.example.com/path/to/file" to "sub.domain.example.com"
        )
        
        testCases.forEach { (url, expectedDomain) ->
            val result = extractDomainMethod.invoke(engine, url) as String?
            assertEquals("Domain extraction failed for: $url", expectedDomain, result)
        }
    }
    
    @Test
    fun `extractDomain handles edge cases`() {
        val extractDomainMethod = FastAdBlockEngine::class.java.getDeclaredMethod(
            "extractDomain",
            String::class.java
        )
        extractDomainMethod.isAccessible = true
        
        // Test edge cases
        val result1 = extractDomainMethod.invoke(engine, "") as String?
        assertEquals("Empty URL should return null", null, result1)
        
        val result2 = extractDomainMethod.invoke(engine, "https://") as String?
        assertEquals("URL with only protocol should return null", null, result2)
    }
    
    @Test
    fun `extractDomain handles URLs with ports`() {
        val extractDomainMethod = FastAdBlockEngine::class.java.getDeclaredMethod(
            "extractDomain",
            String::class.java
        )
        extractDomainMethod.isAccessible = true
        
        val result = extractDomainMethod.invoke(engine, "https://example.com:8080/path") as String?
        assertEquals("Should extract domain without port", "example.com", result)
    }
    
    @Test
    fun `extractDomain handles URLs with query strings`() {
        val extractDomainMethod = FastAdBlockEngine::class.java.getDeclaredMethod(
            "extractDomain",
            String::class.java
        )
        extractDomainMethod.isAccessible = true
        
        val result = extractDomainMethod.invoke(engine, "https://example.com/path?query=value&other=data") as String?
        assertEquals("Should extract domain without query string", "example.com", result)
    }
    
    @Test
    fun `extractDomain handles protocol-relative URLs`() {
        val extractDomainMethod = FastAdBlockEngine::class.java.getDeclaredMethod(
            "extractDomain",
            String::class.java
        )
        extractDomainMethod.isAccessible = true
        
        val result = extractDomainMethod.invoke(engine, "//cdn.example.com/script.js") as String?
        assertEquals("Should extract domain from protocol-relative URL", "cdn.example.com", result)
    }
}
