package com.entertainmentbrowser.presentation.webview

/**
 * Utility class for detecting video content on web pages
 */
object VideoDetector {
    
    /**
     * JavaScript code to inject into web pages for video element detection
     */
    const val VIDEO_DETECTION_SCRIPT = """
        (function() {
            // Detect video elements
            const videos = document.querySelectorAll('video');
            if (videos.length > 0) {
                videos.forEach(video => {
                    if (video.src) {
                        window.AndroidInterface.onVideoDetected(video.src);
                    }
                    // Check source elements
                    const sources = video.querySelectorAll('source');
                    sources.forEach(source => {
                        if (source.src) {
                            window.AndroidInterface.onVideoDetected(source.src);
                        }
                    });
                });
            }
            
            // Monitor for dynamically added videos
            const observer = new MutationObserver(mutations => {
                mutations.forEach(mutation => {
                    mutation.addedNodes.forEach(node => {
                        if (node.tagName === 'VIDEO') {
                            if (node.src) {
                                window.AndroidInterface.onVideoDetected(node.src);
                            }
                        }
                    });
                });
            });
            
            observer.observe(document.body, {
                childList: true,
                subtree: true
            });
        })();
    """
    
    /**
     * Regex pattern for matching video URLs
     * Matches common video file extensions with or without query parameters
     */
    private val VIDEO_URL_PATTERN = Regex(
        pattern = ".*\\.(mp4|webm|m3u8|mpd)(\\?.*)?$",
        option = RegexOption.IGNORE_CASE
    )
    
    /**
     * Additional patterns for streaming protocols and video URLs
     */
    private val STREAMING_PATTERNS = listOf(
        Regex(".*\\.m3u8.*", RegexOption.IGNORE_CASE),  // HLS
        Regex(".*\\.mpd.*", RegexOption.IGNORE_CASE),   // DASH
        Regex(".*blob:.*", RegexOption.IGNORE_CASE),    // Blob URLs
        Regex(".*\\.mp4.*", RegexOption.IGNORE_CASE),   // MP4 with any query params
        Regex(".*\\.webm.*", RegexOption.IGNORE_CASE)   // WebM with any query params
    )
    
    /**
     * Check if a URL matches video patterns
     */
    fun isVideoUrl(url: String): Boolean {
        android.util.Log.d("VideoDetector", "Checking URL: $url")
        
        if (VIDEO_URL_PATTERN.matches(url)) {
            android.util.Log.d("VideoDetector", "✅ Matched VIDEO_URL_PATTERN")
            return true
        }
        
        val matchedPattern = STREAMING_PATTERNS.any { it.matches(url) }
        if (matchedPattern) {
            android.util.Log.d("VideoDetector", "✅ Matched STREAMING_PATTERNS")
            return true
        }
        
        android.util.Log.d("VideoDetector", "❌ No pattern matched")
        return false
    }
    
    /**
     * Extract video URL from various formats
     */
    fun extractVideoUrl(url: String): String? {
        return if (isVideoUrl(url)) url else null
    }
    
    /**
     * Check if video format is supported for download
     */
    fun isSupportedFormat(url: String): Boolean {
        val supportedFormats = listOf(".mp4", ".webm", ".m3u8", ".mpd")
        return supportedFormats.any { url.contains(it, ignoreCase = true) }
    }
    
    /**
     * Get user-friendly format name
     */
    fun getFormatName(url: String): String {
        return when {
            url.contains(".mp4", ignoreCase = true) -> "MP4"
            url.contains(".webm", ignoreCase = true) -> "WebM"
            url.contains(".m3u8", ignoreCase = true) -> "HLS"
            url.contains(".mpd", ignoreCase = true) -> "DASH"
            else -> "Unknown"
        }
    }
}
