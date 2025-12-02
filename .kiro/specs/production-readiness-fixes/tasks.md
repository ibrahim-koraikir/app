# Implementation Plan

- [x] 1. Update production URLs in settings





  - Replace placeholder privacy policy and terms of service URLs with production values or add TODO comments
  - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 2. Centralize monetization domain whitelist




- [x] 2.1 Update MonetizationManager to expose public whitelist API


  - Rename `WHITELISTED_DOMAINS` to `MONETIZATION_DOMAINS` and make it public
  - Add `isMonetizationDomain(url: String): Boolean` helper method
  - Add `extractDomain(url: String): String` private helper method
  - Update documentation to indicate this is the single source of truth
  - _Requirements: 7.1_

- [x] 2.2 Update FastAdBlockEngine to use centralized whitelist


  - Remove hardcoded `monetizationDomains` list from `shouldBlock` method
  - Replace domain check loop with call to `MonetizationManager.isMonetizationDomain()`
  - Update log message to reference centralized whitelist
  - _Requirements: 7.2_

- [x] 2.3 Update AdBlockWebViewClient to use centralized whitelist


  - Remove hardcoded `monetizationDomains` list from `shouldOverrideUrlLoading` method
  - Add `MonetizationManager` as constructor parameter
  - Replace domain check with call to `MonetizationManager.isMonetizationDomain()`
  - Update CustomWebView.kt to pass MonetizationManager to AdBlockWebViewClient
  - Update comments to reference centralized management
  - _Requirements: 7.3_

- [x] 2.4 Add monetization whitelist check to HardcodedFilters


  - Add early return check at beginning of `shouldBlock` method
  - Extract domain from URL and check against `MonetizationManager.MONETIZATION_DOMAINS`
  - Return false immediately if domain matches (allow request)
  - Add comment explaining monetization whitelist
  - _Requirements: 7.4_

- [x] 3. Update storage permissions for Android 10+ compatibility





  - Remove `WRITE_EXTERNAL_STORAGE` permission from AndroidManifest.xml
  - Keep `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"`
  - Keep `READ_MEDIA_VIDEO` with `minSdkVersion="33"`
  - Add comment explaining scoped storage compliance
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 4. Add JavaScript interface security validation





- [x] 4.1 Implement URL validation helper in CustomWebView


  - Create `isValidAndSafeUrl(url: String): Boolean` private function
  - Check for non-empty strings and reject "null" string literal
  - Validate URL length (max 2048 characters)
  - Validate URL format using Uri.parse with exception handling
  - Validate allowed schemes (http, https only)
  - Validate presence of valid host
  - _Requirements: 2.1, 2.4_


- [x] 4.2 Add validation and error handling to JavaScript interface methods

  - Wrap `onVideoDetected` with try-catch and URL validation
  - Wrap `onDrmDetected` with try-catch for exception safety
  - Wrap `onVideoElementLongPress` with try-catch and URL validation
  - Add appropriate logging for validation failures and exceptions
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 5. Fix monetization tracking to prevent double-counting






- [x] 5.1 Remove automatic tracking calls from WebViewViewModel

  - Remove `trackUserAction()` call from `createTabForUrl` method (line 78)
  - Remove `trackUserAction()` call from `onEvent(WebViewEvent.UpdateUrl)` (line 116)
  - Remove `trackUserAction()` call from `onEvent(WebViewEvent.VideoDetected)` (line 144)
  - Verify `trackUserAction()` in `openNewTab` is kept with monetization URL check
  - _Requirements: 6.1, 6.2, 6.3, 6.4_


- [x] 5.2 Update monetization tracking documentation

  - Update `trackUserAction` method documentation to clarify which actions are tracked
  - Document that only explicit user interactions increment the counter
  - List specific tracked actions (manual new tab, optional: download, share)
  - List non-tracked events (auto page loads, URL changes, video detection)
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 6. Implement WebView cache memory management






- [x] 6.1 Add LRU tracking data structure to WebViewStateManager

  - Add `lastAccessTime` ConcurrentHashMap to track access timestamps
  - Update timestamp in `getWebViewForTab` when WebView is accessed
  - Update timestamp in `pauseWebView` when WebView is paused
  - Update timestamp in `resumeWebView` when WebView is resumed
  - _Requirements: 5.2_


- [x] 6.2 Implement cache size enforcement with LRU eviction








  - Add cache size check in `getWebViewForTab` before creating new WebView
  - Implement `evictLeastRecentlyUsed()` private method to find and remove LRU WebView
  - Ensure eviction excludes currently active tabs
  - Call eviction when cache reaches `MAX_CACHED_WEBVIEWS` limit
  - _Requirements: 5.1, 5.4_


- [x] 6.3 Add proactive cache trimming





  - Implement `trimCache(maxSize: Int)` public method for manual cache reduction
  - Remove least recently used WebViews until cache size is below threshold
  - Call `trimCache()` from `pauseWebView` to free memory when app backgrounds
  - Set trim target to half of MAX_CACHED_WEBVIEWS when backgrounding
  - _Requirements: 5.3, 5.4_

- [x] 7. Integrate download tracking with DownloadRepository




- [x] 7.1 Add DownloadRepository parameter to CustomWebView


  - Add `downloadRepository: DownloadRepository` parameter to CustomWebView composable
  - Update WebViewScreen.kt to inject and pass DownloadRepository
  - Ensure proper dependency injection through Hilt
  - _Requirements: 3.1, 3.2_


- [x] 7.2 Replace direct DownloadManager with repository calls

  - Remove direct DownloadManager implementation from `setDownloadListener`
  - Implement new download listener using `downloadRepository.startDownload()`
  - Extract filename using `URLUtil.guessFileName()`
  - Handle video downloads with quality selection dialog
  - Handle non-video downloads with default quality
  - Use coroutine scope for async repository calls
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 8. Remove destructive database migration fallback





  - Remove `.fallbackToDestructiveMigration()` call from Room database builder in DatabaseModule
  - Keep `.addMigrations(MIGRATION_4_5)` for proper schema evolution
  - Add detailed comment explaining why destructive migration is disabled
  - Document that all future schema changes require explicit migration paths
  - _Requirements: 4.1, 4.2, 4.3, 4.4_
