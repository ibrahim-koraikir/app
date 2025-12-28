package com.entertainmentbrowser.util.adblock

/**
 * AdBlockDomainRegistry - Centralized domain knowledge for ad-blocking engines.
 * 
 * This singleton provides shared sets of domains and keywords used by both
 * FastAdBlockEngine and AdvancedAdBlockEngine, eliminating duplication and
 * ensuring consistent behavior across engines.
 * 
 * Categories:
 * - Essential domains: Never block (Google services, CDNs, payments, auth)
 * - Ad/tracking keywords: Used for heuristic detection
 * - Sponsored keywords: Used for affiliate/sponsored content detection
 */
object AdBlockDomainRegistry {
    
    // ============================================================================
    // ESSENTIAL GOOGLE DOMAINS - Never block these
    // Used by: FastAdBlockEngine.isGoogleEssential()
    // ============================================================================
    val googleEssentialDomains = setOf(
        "www.google.com",
        "google.com",
        "encrypted-tbn0.gstatic.com",
        "encrypted-tbn1.gstatic.com",
        "encrypted-tbn2.gstatic.com",
        "encrypted-tbn3.gstatic.com",
        "ssl.gstatic.com",
        "www.gstatic.com",
        "fonts.gstatic.com",
        "fonts.googleapis.com",
        "apis.google.com",
        "accounts.google.com",
        "play.google.com",
        "lh3.googleusercontent.com",
        "lh4.googleusercontent.com",
        "lh5.googleusercontent.com",
        "lh6.googleusercontent.com"
    )
    
    // ============================================================================
    // CRITICAL WHITELIST - Domains that must NEVER be blocked
    // Only includes domains strictly required for: login, checkout, media playback, platform stability
    // For site-specific exceptions, use targeted filter rules instead of global whitelist
    // Used by: AdvancedAdBlockEngine.criticalWhitelist
    // ============================================================================
    val criticalWhitelist = hashSetOf(
        // Payment processors (checkout/transactions)
        "paypal.com", "stripe.com", "square.com", "braintreepayments.com",
        "checkout.com", "adyen.com", "worldpay.com", "venmo.com", "klarna.com",
        "affirm.com", "afterpay.com", "pay.google.com",
        
        // CDNs (content delivery - blocking breaks sites)
        "cloudflare.com", "fastly.net", "akamaihd.net", "cloudfront.net",
        "jsdelivr.net", "unpkg.com", "cdnjs.cloudflare.com", "azureedge.net",
        "edgecastcdn.net", "limelight.com", "cdn77.com", "bunnycdn.com",
        
        // Google Services (critical APIs for auth, storage, app functionality)
        "googleapis.com", "gstatic.com", "googleusercontent.com",
        "firebase.google.com", "firebaseio.com", "firestore.googleapis.com",
        "accounts.google.com", "oauth2.googleapis.com", "identitytoolkit.googleapis.com",
        "securetoken.googleapis.com", "play.google.com", "android.com",
        // Google Images & Search essential domains
        "www.google.com", "google.com",
        "encrypted-tbn0.gstatic.com", "encrypted-tbn1.gstatic.com",
        "encrypted-tbn2.gstatic.com", "encrypted-tbn3.gstatic.com",
        "ssl.gstatic.com", "www.gstatic.com", "fonts.gstatic.com",
        "lh3.googleusercontent.com", "lh4.googleusercontent.com",
        "lh5.googleusercontent.com", "lh6.googleusercontent.com",
        
        // CAPTCHA & Security (required for login/verification)
        "recaptcha.net", "google.com/recaptcha", "hcaptcha.com",
        "funcaptcha.com", "arkoselabs.com",
        
        // Streaming CDNs (media playback)
        "netflix.com", "nflxvideo.net", "nflximg.net", "nflxext.com", "nflxso.net",
        "spotify.com", "scdn.co", "spotifycdn.com", "audio-ak-spotify-com.akamaized.net",
        "youtube.com", "ytimg.com", "ggpht.com", "googlevideo.com",
        "twitch.tv", "ttvnw.net", "jtvnw.net", "hls.ttvnw.net",
        "disneyplus.com", "bamgrid.com", "hulu.com", "primevideo.com",
        "pv-cdn.net", "aiv-cdn.net",
        
        // Social Login (auth endpoints only)
        "facebook.com/login", "accounts.google.com", "login.live.com",
        "appleid.apple.com", "twitter.com/oauth", "github.com/login",
        "linkedin.com/oauth", "discord.com/api/oauth2",
        
        // Maps & Location (core functionality)
        "maps.googleapis.com", "maps.gstatic.com", "mapbox.com", "tile.openstreetmap.org",
        
        // Microsoft / Office 365 (auth & core services)
        "microsoft.com", "office.com", "live.com", "azure.com", "windows.net",
        "office365.com", "sharepoint.com", "onenote.com",
        
        // Apple Services (auth & core services)
        "apple.com", "icloud.com", "cdn-apple.com", "mzstatic.com"
    )
    
    // ============================================================================
    // AD HINT KEYWORDS - Fast heuristic check for ad-related URLs
    // Only URLs containing these hints undergo expensive pattern scanning
    // Used by: FastAdBlockEngine.containsAdHints()
    // ============================================================================
    val adHintKeywords = setOf(
        "ad", "ads", "adv", "advert", "banner", "track", "click", "pixel",
        "beacon", "sponsor", "promo", "affiliate", "aff", "partner",
        "impression", "imp", "redirect", "redir", "outbrain", "taboola",
        "mgid", "revcontent", "zergnet", "native", "campaign", "utm_"
    )
    
    // ============================================================================
    // SPONSORED KEYWORDS - Detect affiliate/sponsored content in URL paths
    // Used by: FastAdBlockEngine.containsSponsoredKeywords()
    // ============================================================================
    val sponsoredKeywords = listOf(
        "sponsor", "sponsored", "advertisement", "promo", "promotional",
        "affiliate", "aff", "partner", "campaign", "tracking",
        "redirect", "redir", "click", "clk", "imp", "impression",
        "adlink", "adclick", "adsclick", "outbrain", "taboola",
        "revcontent", "mgid", "zergnet", "bidvertiser"
    )
    
    // ============================================================================
    // AD DOMAIN KEYWORDS - Heuristic domain-based ad detection
    // Used by: AdvancedAdBlockEngine.isHeuristicAdUrl()
    // ============================================================================
    val adDomainKeywords = hashSetOf(
        "adserv", "adserver", "adsystem", "adnetwork", "adtech", "advert",
        "tracking", "tracker", "analytics", "telemetry", "pixel", "beacon",
        "popup", "popunder", "banner", "sponsor", "affiliate", "monetiz"
    )
    
    // ============================================================================
    // AD PATH KEYWORDS - Heuristic path-based ad detection
    // Used by: AdvancedAdBlockEngine.isHeuristicAdUrl()
    // ============================================================================
    val adPathKeywords = hashSetOf(
        "/ads/", "/ad/", "/advert/", "/banner/", "/sponsor/", "/track/",
        "/pixel/", "/beacon/", "/click/", "/redirect/", "/redir/",
        "/affiliate/", "/aff/", "pagead", "doubleclick", "adsense"
    )
}
