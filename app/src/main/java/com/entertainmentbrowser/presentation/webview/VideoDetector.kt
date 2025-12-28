package com.entertainmentbrowser.presentation.webview

/**
 * Utility class for detecting video content on web pages
 */
object VideoDetector {
    
    /**
     * JavaScript code to inject into web pages for video element detection and play state monitoring
     */
    const val VIDEO_DETECTION_SCRIPT = """
        (function() {
            // Track which videos we've already set up listeners for
            const trackedVideos = new WeakSet();
            
            // Setup play/pause listeners for a video element
            function setupVideoListeners(video) {
                if (trackedVideos.has(video)) return;
                trackedVideos.add(video);
                
                // Detect video source
                const videoSrc = video.src || video.currentSrc;
                if (videoSrc) {
                    window.AndroidInterface.onVideoDetected(videoSrc);
                }
                
                // Check source elements
                const sources = video.querySelectorAll('source');
                sources.forEach(source => {
                    if (source.src) {
                        window.AndroidInterface.onVideoDetected(source.src);
                    }
                });
                
                // Listen for play event
                video.addEventListener('play', function() {
                    window.AndroidInterface.onVideoPlayingStateChanged(true);
                });
                
                // Listen for playing event (fires when actually playing after buffering)
                video.addEventListener('playing', function() {
                    window.AndroidInterface.onVideoPlayingStateChanged(true);
                });
                
                // Listen for pause event
                video.addEventListener('pause', function() {
                    window.AndroidInterface.onVideoPlayingStateChanged(false);
                });
                
                // Listen for ended event
                video.addEventListener('ended', function() {
                    window.AndroidInterface.onVideoPlayingStateChanged(false);
                });
                
                // Check if video is already playing
                if (!video.paused && !video.ended && video.readyState > 2) {
                    window.AndroidInterface.onVideoPlayingStateChanged(true);
                }
            }
            
            // Detect existing video elements
            const videos = document.querySelectorAll('video');
            videos.forEach(video => setupVideoListeners(video));
            
            // Monitor for dynamically added videos
            const observer = new MutationObserver(mutations => {
                mutations.forEach(mutation => {
                    mutation.addedNodes.forEach(node => {
                        if (node.nodeType === 1) {
                            if (node.tagName === 'VIDEO') {
                                setupVideoListeners(node);
                            }
                            // Also check for videos inside added containers
                            const nestedVideos = node.querySelectorAll ? node.querySelectorAll('video') : [];
                            nestedVideos.forEach(video => setupVideoListeners(video));
                        }
                    });
                });
            });
            
            if (document.body) {
                observer.observe(document.body, {
                    childList: true,
                    subtree: true
                });
            }
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
     * PERFORMANCE: Removed debug logging that ran on every URL check
     */
    fun isVideoUrl(url: String): Boolean {
        // Fast path: check common patterns first
        if (VIDEO_URL_PATTERN.matches(url)) {
            return true
        }
        return STREAMING_PATTERNS.any { it.matches(url) }
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
