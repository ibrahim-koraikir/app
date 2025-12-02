package com.entertainmentbrowser.presentation.webview

/**
 * Utility class for detecting DRM-protected content
 */
object DrmDetector {
    
    /**
     * Keywords that indicate DRM protection in page source
     */
    private val DRM_KEYWORDS = listOf(
        "widevine",
        "playready",
        "fairplay",
        "eme",
        "encrypted-media",
        "drm",
        "contentprotection",
        "protectioninfo"
    )
    
    /**
     * Known DRM-protected streaming sites
     */
    private val KNOWN_DRM_SITES = setOf(
        "netflix.com",
        "disneyplus.com",
        "hulu.com",
        "hbomax.com",
        "max.com",
        "primevideo.com",
        "amazon.com/gp/video",
        "peacocktv.com",
        "paramountplus.com",
        "apple.com/tv",
        "youtube.com/premium",
        "spotify.com"
    )
    
    /**
     * JavaScript code to detect DRM usage
     */
    const val DRM_DETECTION_SCRIPT = """
        (function() {
            // Check for EME (Encrypted Media Extensions) API usage
            if (navigator.requestMediaKeySystemAccess) {
                const drmSystems = [
                    'com.widevine.alpha',
                    'com.microsoft.playready',
                    'com.apple.fps'
                ];
                
                drmSystems.forEach(system => {
                    navigator.requestMediaKeySystemAccess(system, [{}])
                        .then(() => {
                            window.AndroidInterface.onDrmDetected();
                        })
                        .catch(() => {});
                });
            }
            
            // Check for video elements with encrypted content
            const videos = document.querySelectorAll('video');
            videos.forEach(video => {
                if (video.mediaKeys || video.setMediaKeys) {
                    window.AndroidInterface.onDrmDetected();
                }
            });
        })();
    """
    
    /**
     * Check if a URL belongs to a known DRM site
     */
    fun isKnownDrmSite(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return KNOWN_DRM_SITES.any { lowerUrl.contains(it) }
    }
    
    /**
     * Check if page source contains DRM keywords
     */
    fun containsDrmKeywords(pageSource: String): Boolean {
        val lowerSource = pageSource.lowercase()
        return DRM_KEYWORDS.any { lowerSource.contains(it) }
    }
    
    /**
     * Get DRM warning message
     */
    fun getDrmWarningMessage(): String {
        return "This content appears to be DRM-protected and cannot be downloaded. " +
                "DRM (Digital Rights Management) prevents unauthorized copying of copyrighted content."
    }
    
    /**
     * Check if DRM is likely present based on URL and content
     */
    fun isDrmLikely(url: String, pageSource: String? = null): Boolean {
        if (isKnownDrmSite(url)) {
            return true
        }
        
        if (pageSource != null && containsDrmKeywords(pageSource)) {
            return true
        }
        
        return false
    }
}
