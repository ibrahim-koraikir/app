# Implementation Plan

- [x] 1. Download and set up filter list assets






  - Create `app/src/main/assets/adblock` directory
  - Download EasyList filter list to `app/src/main/assets/adblock/easylist.txt`
  - Download EasyPrivacy filter list to `app/src/main/assets/adblock/easyprivacy.txt`
  - Verify filter list files are properly placed and accessible
  - _Requirements: 1.1, 4.1, 4.2_

- [x] 2. Implement FastAdBlockEngine core functionality






  - [x] 2.1 Create FastAdBlockEngine class with singleton pattern


    - Create `app/src/main/java/com/entertainmentbrowser/util/adblock/FastAdBlockEngine.kt`
    - Implement singleton getInstance method with context parameter
    - Define HashSet data structures for blockedDomains, blockedPatterns, and allowedDomains
    - Add isInitialized flag to track loading state
    - _Requirements: 2.1, 2.3, 3.3_

  - [x] 2.2 Implement filter list loading and parsing

    - Implement preloadFromAssets method to load filter lists in background thread
    - Implement parseFastRule method to extract domain-based rules (||domain.com^)
    - Implement parseFastRule to extract simple pattern rules (no wildcards or regex)
    - Implement parseFastRule to handle exception rules (@@||domain.com^)
    - Skip element hiding rules (##, #@#) and complex patterns during parsing
    - Add logging for load time, blocked domains count, and blocked patterns count
    - _Requirements: 1.1, 2.2, 4.3, 4.4, 5.1, 5.2, 5.5_

  - [x] 2.3 Implement URL checking logic

    - Implement shouldBlock method with URL parameter
    - Implement extractDomain helper method to parse domain from URL
    - Check allowedDomains HashSet first (return false if found)
    - Check blockedDomains HashSet for O(1) domain lookup
    - Check blockedPatterns HashSet for pattern matching
    - Return false if not initialized (graceful degradation)
    - Add try-catch to return false on any errors
    - _Requirements: 1.2, 1.5, 2.3, 5.3, 5.4, 9.1, 9.3_

  - [x] 2.4 Add whitelist support

    - Add whitelistedDomains HashSet for custom domain whitelist
    - Check whitelistedDomains before applying blocking rules in shouldBlock
    - Document how to add domains to whitelist
    - _Requirements: 8.1, 8.2, 8.3, 8.5_

- [x] 3. Implement HardcodedFilters fallback system






  - [x] 3.1 Create HardcodedFilters object with domain lists

    - Create `app/src/main/java/com/entertainmentbrowser/util/adblock/HardcodedFilters.kt`
    - Define adDomains Set with 1000+ common ad/tracking domains
    - Include Google Ads domains (doubleclick.net, googleadservices.com, etc.)
    - Include Facebook/Meta tracking domains (facebook.com/tr, pixel.facebook.com, etc.)
    - Include Amazon ad domains (amazon-adsystem.com, etc.)
    - Include major ad networks (adnxs.com, criteo.com, outbrain.com, taboola.com, etc.)
    - Include analytics domains (google-analytics.com, scorecardresearch.com, etc.)
    - _Requirements: 4.5, 6.1_

  - [x] 3.2 Add keyword-based filtering

    - Define adKeywords List with common ad URL patterns
    - Include patterns like "/ads/", "/ad/", "/advert", "/banner", "/sponsor"
    - Include keywords like "doubleclick", "adsystem", "analytics", "tracking", "pixel"
    - _Requirements: 4.5_


  - [x] 3.3 Implement shouldBlock method

    - Implement shouldBlock method that checks URL against adDomains
    - Check URL against adKeywords patterns
    - Return true if any domain or keyword matches
    - Return false if no matches found
    - _Requirements: 4.5, 9.2_

- [x] 4. Implement AdBlockWebViewClient





  - [x] 4.1 Create AdBlockWebViewClient class extending WebViewClient


    - Create `app/src/main/java/com/entertainmentbrowser/presentation/webview/AdBlockWebViewClient.kt`
    - Extend WebViewClient class
    - Add callback parameters for existing CustomWebView functionality (onVideoDetected, onDrmDetected, onLoadingChanged, etc.)
    - Add private fastEngine property for lazy initialization
    - Add blockedCount property to track blocked requests per page
    - _Requirements: 10.1, 10.4_

  - [x] 4.2 Implement request interception

    - Override shouldInterceptRequest for API 21+ (WebResourceRequest parameter)
    - Override shouldInterceptRequest for API <21 (String URL parameter)
    - Implement lazy initialization of fastEngine using getInstance
    - Call checkAndBlock method for both overrides
    - _Requirements: 1.2, 3.3, 10.2_

  - [x] 4.3 Implement blocking logic

    - Implement checkAndBlock private method
    - Check fastEngine.shouldBlock first (return empty response if true)
    - Check HardcodedFilters.shouldBlock as fallback (return empty response if true)
    - Return null to allow request if not blocked
    - Increment blockedCount when blocking
    - Wrap in try-catch to return null on errors
    - _Requirements: 1.3, 9.4_

  - [x] 4.4 Implement empty response creation

    - Implement createEmptyResponse private method
    - Return WebResourceResponse with "text/plain" mime type
    - Use "UTF-8" encoding
    - Use ByteArrayInputStream with empty ByteArray as data
    - _Requirements: 1.3_

  - [x] 4.5 Implement blocked count tracking

    - Override onPageStarted to reset blockedCount to zero
    - Override onPageFinished to log blocked count
    - Implement getBlockedCount method to return current count
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [x] 4.6 Integrate existing CustomWebView functionality

    - Override onPageStarted to call onLoadingChanged(true) and onUrlChanged
    - Override onPageStarted to check for DRM sites and call onDrmDetected
    - Override onPageFinished to call onLoadingChanged(false) and onNavigationStateChanged
    - Override onPageFinished to inject video detection and DRM detection scripts
    - Override onPageFinished to call onPageFinished callback
    - Override shouldOverrideUrlLoading to enforce HTTPS
    - Override shouldInterceptRequest to detect video URLs and call onVideoDetected
    - Override onReceivedError to call onError callback
    - _Requirements: 10.3, 10.5_

- [x] 5. Integrate ad-blocking into Application class




  - [x] 5.1 Add ad-blocking initialization to EntertainmentBrowserApp



    - Open `app/src/main/java/com/entertainmentbrowser/EntertainmentBrowserApp.kt`
    - Create initializeAdBlocking private method
    - Call FastAdBlockEngine.getInstance(this).preloadFromAssets() in try-catch
    - Add logging for initialization start, success, and failure
    - Call initializeAdBlocking from onCreate method
    - _Requirements: 3.1, 3.2, 3.4_

- [x] 6. Update CustomWebView to use AdBlockWebViewClient




  - [x] 6.1 Modify CustomWebView composable


    - Open `app/src/main/java/com/entertainmentbrowser/presentation/webview/CustomWebView.kt`
    - Replace anonymous WebViewClient with AdBlockWebViewClient instance
    - Pass all callback parameters to AdBlockWebViewClient constructor
    - Remove WebViewClient logic from CustomWebView (moved to AdBlockWebViewClient)
    - Verify WebChromeClient remains unchanged
    - _Requirements: 10.3, 10.5_

- [x] 7. Add configuration and customization support




  - [x] 7.1 Document customization options



    - Add comments in FastAdBlockEngine for adding custom whitelisted domains
    - Add comments in HardcodedFilters for adding custom ad domains
    - Add comments in FastAdBlockEngine for adding additional filter lists
    - _Requirements: 6.2, 6.3, 6.4, 8.4_

- [x] 8. Write unit tests for ad-blocking components






  - [x] 8.1 Write FastAdBlockEngine unit tests



    - Create `app/src/test/java/com/entertainmentbrowser/util/adblock/FastAdBlockEngineTest.kt`
    - Test shouldBlock returns true for known ad domains
    - Test shouldBlock returns false for allowed domains (exception rules)
    - Test shouldBlock returns true for pattern matches
    - Test shouldBlock returns false for whitelisted domains
    - Test shouldBlock returns false when not initialized
    - Test extractDomain returns correct domain from various URL formats
    - Test parseFastRule correctly parses domain rules (||domain.com^)
    - Test parseFastRule correctly parses exception rules (@@||domain.com^)
    - Test shouldBlock handles malformed URLs gracefully
    - _Requirements: 1.5, 2.1, 5.3, 5.4, 8.2, 9.1, 9.3_

  - [x] 8.2 Write HardcodedFilters unit tests



    - Create `app/src/test/java/com/entertainmentbrowser/util/adblock/HardcodedFiltersTest.kt`
    - Test shouldBlock returns true for Google Ads domains
    - Test shouldBlock returns true for Facebook tracking domains
    - Test shouldBlock returns true for URLs with ad keywords
    - Test shouldBlock returns false for legitimate domains
    - Test adDomains contains at least 1000 entries
    - _Requirements: 4.5_

  - [x] 8.3 Write AdBlockWebViewClient unit tests




    - Create `app/src/test/java/com/entertainmentbrowser/presentation/webview/AdBlockWebViewClientTest.kt`
    - Test shouldInterceptRequest blocks known ad URLs
    - Test shouldInterceptRequest allows legitimate URLs
    - Test createEmptyResponse returns valid empty WebResourceResponse
    - Test getBlockedCount returns correct count
    - Test blockedCount resets to zero on page start
    - Test shouldInterceptRequest handles exceptions gracefully
    - _Requirements: 1.2, 1.3, 7.1, 7.2, 7.3, 9.4_

- [x] 9. Write integration tests





  - [x] 9.1 Write ad-blocking integration tests



    - Create `app/src/androidTest/java/com/entertainmentbrowser/util/adblock/AdBlockingIntegrationTest.kt`
    - Test WebView with AdBlockWebViewClient blocks ads on real page
    - Test FastAdBlockEngine loads filter lists successfully from assets
    - Test ad-blocking works alongside video detection functionality
    - Test ad-blocking preserves existing WebView navigation functionality
    - Test blocked count updates correctly during page load
    - _Requirements: 1.4, 2.2, 2.4, 10.5_

- [x] 10. Perform manual testing and verification








  - [x] 10.1 Test ad-blocking effectiveness


    - Visit forbes.com and verify banner ads are blocked
    - Visit cnn.com and verify video ads are blocked
    - Visit dailymail.co.uk and verify popups are blocked
    - Visit adblock-tester.com and verify 85%+ blocking rate
    - _Requirements: 1.4_


  - [ ] 10.2 Test performance metrics

    - Check Logcat for filter list load time (should be <1 second)
    - Verify smooth scrolling on ad-heavy websites
    - Monitor memory usage (should be <100MB increase)
    - Measure page load time improvement on ad-heavy sites
    - _Requirements: 1.1, 1.5, 2.4, 2.5_

  - [ ]* 10.3 Test existing functionality preservation
    - Verify video detection still works on video sites
    - Verify download functionality still works
    - Verify navigation (back/forward) works correctly
    - Verify tab management works correctly
    - Verify DRM detection still works
    - _Requirements: 10.5_

  - [ ]* 10.4 Test error handling
    - Delete filter list files and verify fallback to HardcodedFilters
    - Test with malformed URLs and verify no crashes
    - Test with HTTPS enforcement and verify blocking still works
    - Verify graceful degradation when engine fails to initialize
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_
