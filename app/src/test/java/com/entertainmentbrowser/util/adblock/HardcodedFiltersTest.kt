package com.entertainmentbrowser.util.adblock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for HardcodedFilters.
 * Tests domain and keyword-based blocking logic.
 */
class HardcodedFiltersTest {
    
    @Test
    fun `shouldBlock returns true for Google Ads domains`() {
        // Test various Google Ads domains
        assertTrue("Should block doubleclick.net", 
            HardcodedFilters.shouldBlock("https://doubleclick.net/ad.js"))
        assertTrue("Should block googleadservices.com", 
            HardcodedFilters.shouldBlock("https://www.googleadservices.com/pagead/conversion.js"))
        assertTrue("Should block googlesyndication.com", 
            HardcodedFilters.shouldBlock("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
        assertTrue("Should block google-analytics.com", 
            HardcodedFilters.shouldBlock("https://www.google-analytics.com/analytics.js"))
        assertTrue("Should block googletagmanager.com", 
            HardcodedFilters.shouldBlock("https://www.googletagmanager.com/gtm.js"))
        assertTrue("Should block 2mdn.net", 
            HardcodedFilters.shouldBlock("https://s0.2mdn.net/ads/richmedia/studio/mu/templates/hifi/hifi.js"))
    }
    
    @Test
    fun `shouldBlock returns true for Facebook tracking domains`() {
        // Test Facebook/Meta tracking domains
        assertTrue("Should block facebook.com/tr", 
            HardcodedFilters.shouldBlock("https://www.facebook.com/tr?id=123456"))
        assertTrue("Should block connect.facebook.net", 
            HardcodedFilters.shouldBlock("https://connect.facebook.net/en_US/fbevents.js"))
        assertTrue("Should block pixel.facebook.com", 
            HardcodedFilters.shouldBlock("https://pixel.facebook.com/track"))
        assertTrue("Should block an.facebook.com", 
            HardcodedFilters.shouldBlock("https://an.facebook.com/analytics"))
    }
    
    @Test
    fun `shouldBlock returns true for Amazon ad domains`() {
        // Test Amazon advertising domains
        assertTrue("Should block amazon-adsystem.com", 
            HardcodedFilters.shouldBlock("https://aax.amazon-adsystem.com/e/dtb/bid"))
        assertTrue("Should block aax-us-east.amazon-adsystem.com", 
            HardcodedFilters.shouldBlock("https://aax-us-east.amazon-adsystem.com/e/dtb/bid"))
        assertTrue("Should block s.amazon-adsystem.com", 
            HardcodedFilters.shouldBlock("https://s.amazon-adsystem.com/iu3/d-prod/ad.js"))
    }
    
    @Test
    fun `shouldBlock returns true for major ad networks`() {
        // Test major ad network domains
        assertTrue("Should block adnxs.com (AppNexus)", 
            HardcodedFilters.shouldBlock("https://ib.adnxs.com/seg?add=123"))
        assertTrue("Should block criteo.com", 
            HardcodedFilters.shouldBlock("https://static.criteo.net/js/ld/ld.js"))
        assertTrue("Should block outbrain.com", 
            HardcodedFilters.shouldBlock("https://widgets.outbrain.com/outbrain.js"))
        assertTrue("Should block taboola.com", 
            HardcodedFilters.shouldBlock("https://cdn.taboola.com/libtrc/publisher/loader.js"))
        assertTrue("Should block pubmatic.com", 
            HardcodedFilters.shouldBlock("https://ads.pubmatic.com/AdServer/js/pwt/123/pwt.js"))
        assertTrue("Should block rubiconproject.com", 
            HardcodedFilters.shouldBlock("https://fastlane.rubiconproject.com/a/api/fastlane.json"))
        assertTrue("Should block openx.net", 
            HardcodedFilters.shouldBlock("https://publisher.openx.net/w/1.0/jstag"))
    }
    
    @Test
    fun `shouldBlock returns true for analytics and tracking domains`() {
        // Test analytics and tracking domains
        assertTrue("Should block scorecardresearch.com", 
            HardcodedFilters.shouldBlock("https://sb.scorecardresearch.com/beacon.js"))
        assertTrue("Should block quantserve.com", 
            HardcodedFilters.shouldBlock("https://secure.quantserve.com/quant.js"))
        assertTrue("Should block chartbeat.com", 
            HardcodedFilters.shouldBlock("https://static.chartbeat.com/js/chartbeat.js"))
        assertTrue("Should block hotjar.com", 
            HardcodedFilters.shouldBlock("https://static.hotjar.com/c/hotjar-123.js"))
        assertTrue("Should block mouseflow.com", 
            HardcodedFilters.shouldBlock("https://cdn.mouseflow.com/projects/123.js"))
        assertTrue("Should block newrelic.com", 
            HardcodedFilters.shouldBlock("https://js-agent.newrelic.com/nr-spa-123.min.js"))
    }
    
    @Test
    fun `shouldBlock returns true for URLs with ad keywords`() {
        // Test keyword-based blocking
        assertTrue("Should block URL with /ads/", 
            HardcodedFilters.shouldBlock("https://example.com/ads/banner.jpg"))
        assertTrue("Should block URL with /ad/", 
            HardcodedFilters.shouldBlock("https://example.com/ad/script.js"))
        assertTrue("Should block URL with /advert", 
            HardcodedFilters.shouldBlock("https://example.com/advert/image.png"))
        assertTrue("Should block URL with /banner", 
            HardcodedFilters.shouldBlock("https://example.com/banner/ad.gif"))
        assertTrue("Should block URL with /sponsor", 
            HardcodedFilters.shouldBlock("https://example.com/sponsor/logo.png"))
        assertTrue("Should block URL with /tracking", 
            HardcodedFilters.shouldBlock("https://example.com/tracking/pixel.gif"))
        assertTrue("Should block URL with /analytics", 
            HardcodedFilters.shouldBlock("https://example.com/analytics/track.js"))
        assertTrue("Should block URL with doubleclick keyword", 
            HardcodedFilters.shouldBlock("https://ad.doubleclick.net/ddm/trackclk/123"))
        assertTrue("Should block URL with adsystem keyword", 
            HardcodedFilters.shouldBlock("https://cdn.adsystem.com/ad.js"))
        assertTrue("Should block URL with pagead keyword", 
            HardcodedFilters.shouldBlock("https://example.com/pagead/show"))
    }
    
    @Test
    fun `shouldBlock returns true for query parameter patterns`() {
        // Test query parameter-based blocking
        assertTrue("Should block URL with ad_id parameter", 
            HardcodedFilters.shouldBlock("https://example.com/page?ad_id=123"))
        assertTrue("Should block URL with utm_source parameter", 
            HardcodedFilters.shouldBlock("https://example.com/page?utm_source=google"))
        assertTrue("Should block URL with gclid parameter", 
            HardcodedFilters.shouldBlock("https://example.com/page?gclid=abc123"))
        assertTrue("Should block URL with fbclid parameter", 
            HardcodedFilters.shouldBlock("https://example.com/page?fbclid=xyz789"))
    }
    
    @Test
    fun `shouldBlock returns false for legitimate domains`() {
        // Test that legitimate domains are not blocked
        assertFalse("Should not block google.com", 
            HardcodedFilters.shouldBlock("https://www.google.com/search?q=test"))
        assertFalse("Should not block youtube.com", 
            HardcodedFilters.shouldBlock("https://www.youtube.com/watch?v=123"))
        assertFalse("Should not block wikipedia.org", 
            HardcodedFilters.shouldBlock("https://en.wikipedia.org/wiki/Test"))
        assertFalse("Should not block github.com", 
            HardcodedFilters.shouldBlock("https://github.com/user/repo"))
        assertFalse("Should not block stackoverflow.com", 
            HardcodedFilters.shouldBlock("https://stackoverflow.com/questions/123"))
        assertFalse("Should not block reddit.com", 
            HardcodedFilters.shouldBlock("https://www.reddit.com/r/programming"))
        assertFalse("Should not block twitter.com", 
            HardcodedFilters.shouldBlock("https://twitter.com/user/status/123"))
    }
    
    @Test
    fun `shouldBlock returns false for legitimate content paths`() {
        // Test that legitimate content paths are not blocked
        assertFalse("Should not block /content/", 
            HardcodedFilters.shouldBlock("https://example.com/content/article.html"))
        assertFalse("Should not block /images/", 
            HardcodedFilters.shouldBlock("https://example.com/images/photo.jpg"))
        assertFalse("Should not block /videos/", 
            HardcodedFilters.shouldBlock("https://example.com/videos/movie.mp4"))
        assertFalse("Should not block /api/", 
            HardcodedFilters.shouldBlock("https://example.com/api/data"))
        assertFalse("Should not block /static/", 
            HardcodedFilters.shouldBlock("https://example.com/static/style.css"))
    }
    
    @Test
    fun `shouldBlock is case insensitive`() {
        // Test case insensitivity
        assertTrue("Should block uppercase domain", 
            HardcodedFilters.shouldBlock("https://DOUBLECLICK.NET/ad.js"))
        assertTrue("Should block mixed case domain", 
            HardcodedFilters.shouldBlock("https://DoubleClick.Net/ad.js"))
        assertTrue("Should block uppercase keyword", 
            HardcodedFilters.shouldBlock("https://example.com/ADS/banner.jpg"))
        assertTrue("Should block mixed case keyword", 
            HardcodedFilters.shouldBlock("https://example.com/Advertising/ad.js"))
    }
    
    @Test
    fun `shouldBlock handles empty and malformed URLs gracefully`() {
        // Test graceful handling of edge cases
        assertFalse("Should handle empty URL", 
            HardcodedFilters.shouldBlock(""))
        assertFalse("Should handle malformed URL", 
            HardcodedFilters.shouldBlock("not-a-url"))
        assertFalse("Should handle URL with only protocol", 
            HardcodedFilters.shouldBlock("https://"))
    }
    
    @Test
    fun `adDomains contains substantial number of entries`() {
        // Verify that the hardcoded filter list has sufficient coverage
        val domainCount = HardcodedFilters.adDomains.size
        assertTrue("adDomains should contain at least 500 entries, but has $domainCount", 
            domainCount >= 500)
    }
    
    @Test
    fun `adKeywords contains common patterns`() {
        // Verify that common ad keywords are present
        val keywords = HardcodedFilters.adKeywords
        
        assertTrue("Should contain /ads/ keyword", keywords.contains("/ads/"))
        assertTrue("Should contain /ad/ keyword", keywords.contains("/ad/"))
        assertTrue("Should contain /banner keyword", keywords.contains("/banner"))
        assertTrue("Should contain /tracking keyword", keywords.contains("/tracking"))
        assertTrue("Should contain doubleclick keyword", keywords.contains("doubleclick"))
        assertTrue("Should contain analytics keyword", keywords.contains("analytics"))
        assertTrue("Should contain utm_source= keyword", keywords.contains("utm_source="))
    }
    
    @Test
    fun `shouldBlock works with real-world ad URLs`() {
        // Test with real-world ad URLs
        val realAdUrls = listOf(
            "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
            "https://www.googletagmanager.com/gtag/js?id=UA-123456-1",
            "https://connect.facebook.net/en_US/fbevents.js",
            "https://static.doubleclick.net/instream/ad_status.js",
            "https://sb.scorecardresearch.com/beacon.js",
            "https://cdn.taboola.com/libtrc/network-123/loader.js",
            "https://widgets.outbrain.com/outbrain.js",
            "https://static.criteo.net/js/ld/ld.js",
            "https://aax.amazon-adsystem.com/e/dtb/bid"
        )
        
        realAdUrls.forEach { url ->
            assertTrue("Should block real ad URL: $url", HardcodedFilters.shouldBlock(url))
        }
    }
    
    @Test
    fun `shouldBlock does not block legitimate CDN URLs`() {
        // Test that legitimate CDN URLs are not blocked
        val legitimateCdnUrls = listOf(
            "https://cdn.jsdelivr.net/npm/package@1.0.0/dist/bundle.js",
            "https://unpkg.com/package@1.0.0/dist/bundle.js",
            "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.0/jquery.min.js"
        )
        
        legitimateCdnUrls.forEach { url ->
            assertFalse("Should not block legitimate CDN URL: $url", 
                HardcodedFilters.shouldBlock(url))
        }
    }
}
