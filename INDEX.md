I have created the following plan after thorough exploration and analysis of the codebase. Follow the below plan verbatim. Trust the files and references. Do not re-verify what's written in the plan. Explore only when absolutely necessary. First implement all the proposed file changes and then I'll review all the changes together at the end.

### Observations

The review identified 8 critical issues across security, compatibility, data integrity, performance, and maintainability:

**Security & Compatibility:** Legacy storage permissions conflict with Android 10+ scoped storage requirements, and JavaScript interfaces expose powerful methods without input validation.

**Data Integrity:** WebView downloads bypass the central DownloadRepository, and Room database uses destructive migration fallback that could silently wipe user data.

**Performance:** WebViewStateManager lacks cache size enforcement, risking memory issues on low-end devices.

**Maintainability:** Monetization tracking has overlapping calls causing potential double-counting, ad-block whitelisting is duplicated across components, and settings contain placeholder URLs.

### Approach

Fix all critical security, compatibility, and data integrity issues identified in the code review. Address storage permissions for Android 10+ scoped storage, secure JavaScript interfaces, integrate download tracking, protect user data during migrations, implement memory management, eliminate monetization double-counting, centralize ad-block whitelisting, and update placeholder URLs.

### Reasoning

Listed the project structure to understand the codebase organization. Read all files mentioned in the review comments including AndroidManifest.xml, CustomWebView.kt, AdBlockWebViewClient.kt, AppDatabase.kt, DatabaseModule.kt, WebViewStateManager.kt, MonetizationManager.kt, FastAdBlockEngine.kt, HardcodedFilters.kt, SettingsScreen.kt, DownloadRepositoryImpl.kt, WebViewViewModel.kt, and build.gradle.kts to understand the current implementation and identify the exact locations requiring fixes.

## Proposed File Changes

### app\src\main\AndroidManifest.xml(MODIFY)

References: 

- app\src\main\java\com\entertainmentbrowser\data\repository\DownloadRepositoryImpl.kt

**Update storage permissions for Android 10+ scoped storage compatibility:**

1. Remove the legacy `WRITE_EXTERNAL_STORAGE` permission (lines 12-14) as it's deprecated and conflicts with scoped storage
2. Keep `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"` (lines 10-11) for backward compatibility
3. Keep `READ_MEDIA_VIDEO` with `minSdkVersion="33"` (lines 15-16) for Android 13+
4. The app should rely on MediaStore API and scoped storage for downloads on Android 10+ instead of requesting broad external storage access

**Rationale:** Android 10+ enforces scoped storage, making `WRITE_EXTERNAL_STORAGE` ineffective and potentially causing Play Store rejections. The app's download functionality in `DownloadRepositoryImpl.kt` already uses `Environment.DIRECTORY_DOWNLOADS` which works with scoped storage without requiring this permission.

### app\src\main\java\com\entertainmentbrowser\presentation\webview\CustomWebView.kt(MODIFY)

References: 

- app\src\main\java\com\entertainmentbrowser\domain\repository\DownloadRepository.kt
- app\src\main\java\com\entertainmentbrowser\data\repository\DownloadRepositoryImpl.kt

**Add input validation and security hardening to JavaScript interface methods:**

1. In the `jsInterface` object (lines 80-106), add comprehensive validation to all three `@JavascriptInterface` methods:
   - `onVideoDetected(videoUrl: String)`: Validate URL format, check for null/empty/"null" strings, verify URL scheme (http/https only), limit URL length (e.g., max 2048 chars), and sanitize against JavaScript injection attempts
   - `onDrmDetected()`: Add try-catch wrapper to prevent crashes from malformed calls
   - `onVideoElementLongPress(videoUrl: String)`: Apply same validation as onVideoDetected before passing to onLongPress callback

2. Add a private helper function `isValidAndSafeUrl(url: String): Boolean` to centralize URL validation logic checking for: non-empty strings, valid URL format, allowed schemes (http/https), reasonable length limits, and absence of suspicious patterns

3. Wrap all interface method bodies in try-catch blocks to prevent JavaScript-triggered crashes, logging errors appropriately

**Replace direct DownloadManager usage with DownloadRepository integration:**

4. Remove the direct DownloadManager implementation in `setDownloadListener` (lines 123-177)

5. Inject `DownloadRepository` as a parameter to the CustomWebView composable function (add after `fastAdBlockEngine` parameter)

6. Reimplement the download listener to call `downloadRepository.startDownload(url, filename, quality)` instead of using DownloadManager directly

7. This ensures all downloads are tracked in the Room database and appear in the downloads UI, enabling features like pause/resume and proper history tracking

**Rationale:** JavaScript interfaces are attack vectors for malicious web pages. Without validation, malformed input can crash the app or trigger unintended behavior. The download bypass prevents proper tracking and breaks the app's download management features.

### app\src\main\java\com\entertainmentbrowser\di\DatabaseModule.kt(MODIFY)

References: 

- app\src\main\java\com\entertainmentbrowser\data\local\database\AppDatabase.kt

**Remove destructive migration fallback to protect user data:**

1. Remove the `.fallbackToDestructiveMigration()` call on line 47 from the Room database builder

2. Keep only the `.addMigrations(MIGRATION_4_5)` call to ensure proper schema evolution

3. Add a comment explaining that destructive migration is intentionally disabled to prevent data loss, and that all future schema changes must provide explicit migration paths

4. Consider adding additional migrations if there are any missing migration paths between versions (though the current setup appears to only need 4→5)

**Rationale:** Combining manual migrations with `fallbackToDestructiveMigration()` creates a dangerous situation where migration errors silently wipe all user data (favorites, tabs, download history) instead of failing visibly. This masks migration bugs and causes catastrophic data loss. Removing the fallback forces proper migration testing and prevents silent data destruction.

### app\src\main\java\com\entertainmentbrowser\util\WebViewStateManager.kt(MODIFY)

**Implement cache size limits and proactive cleanup:**

1. Add active enforcement of the `MAX_CACHED_WEBVIEWS` constant (line 106) which is currently defined but not used

2. In `getWebViewForTab` method (lines 22-32), before creating a new WebView, check if `webViewCache.size >= MAX_CACHED_WEBVIEWS`

3. If the limit is reached, implement an LRU (Least Recently Used) eviction strategy:
   - Track last access time for each WebView using a `ConcurrentHashMap<String, Long>` for tabId to timestamp mapping
   - Update the timestamp whenever `getWebViewForTab`, `pauseWebView`, or `resumeWebView` is called
   - When eviction is needed, find the least recently used tab (oldest timestamp) that is not currently active
   - Call `removeWebView(tabId)` on the LRU tab to free memory before creating the new WebView

4. Add a new method `trimCache(maxSize: Int = MAX_CACHED_WEBVIEWS)` that can be called proactively to reduce memory pressure, removing the least recently used WebViews until the cache size is below the threshold

5. Consider calling `trimCache()` from `pauseWebView` when the app goes to background to free memory

**Rationale:** Without enforcement, the cache can grow unbounded as users open tabs, leading to excessive memory usage and potential OOM crashes on low-end devices. The MAX_CACHED_WEBVIEWS constant suggests this was intended but never implemented.

### app\src\main\java\com\entertainmentbrowser\presentation\webview\WebViewViewModel.kt(MODIFY)

References: 

- app\src\main\java\com\entertainmentbrowser\util\MonetizationManager.kt(MODIFY)

**Fix monetization tracking to prevent double-counting:**

1. Remove the `trackUserAction()` call from `createTabForUrl` method (line 78) - opening a website should not count as an action since it's the initial load

2. Remove the `trackUserAction()` call from `onEvent(WebViewEvent.UpdateUrl)` (line 116) - URL updates happen automatically during navigation and would cause excessive counting

3. Remove the `trackUserAction()` call from `onEvent(WebViewEvent.VideoDetected)` (line 144) - video detection is passive and shouldn't count as user action

4. Keep only the `trackUserAction()` call in `openNewTab` method (line 439) but ensure it's not called for monetization ad URLs (already has the check on line 438)

5. Consider adding explicit tracking only for genuine user interactions like:
   - User manually clicking to open a new tab (already tracked correctly in openNewTab)
   - User initiating a download (add to startDownloadWithFilename if desired)
   - User sharing content (add to shareCurrentUrl if desired)

6. Update the `trackUserAction` method documentation (line 448) to clarify which specific user actions are tracked

**Rationale:** The current implementation tracks actions on automatic events (URL changes, video detection, initial page loads), causing the action counter to increment far more frequently than intended. This triggers monetization ads too often and counts non-user-initiated events. Only deliberate user interactions should increment the counter.

### app\src\main\java\com\entertainmentbrowser\util\MonetizationManager.kt(MODIFY)

References: 

- app\src\main\java\com\entertainmentbrowser\util\adblock\FastAdBlockEngine.kt(MODIFY)
- app\src\main\java\com\entertainmentbrowser\util\adblock\HardcodedFilters.kt(MODIFY)
- app\src\main\java\com\entertainmentbrowser\presentation\webview\AdBlockWebViewClient.kt(MODIFY)

**Centralize monetization domain whitelisting:**

1. Make the `WHITELISTED_DOMAINS` list (lines 52-55) public and document it as the single source of truth for monetization domains

2. Update the documentation to indicate this list is used by both ad-blocking components (`FastAdBlockEngine` and `HardcodedFilters`) and navigation logic (`AdBlockWebViewClient`)

3. Add a static helper method `isWhitelistedDomain(url: String): Boolean` that checks if a URL contains any whitelisted domain, to be used by other components

4. Consider renaming `WHITELISTED_DOMAINS` to `MONETIZATION_DOMAINS` for clarity since these are specifically monetization-related, not general whitelisting

**Rationale:** This establishes MonetizationManager as the authoritative source for monetization domains, which will be referenced by other components to eliminate duplication.

### app\src\main\java\com\entertainmentbrowser\util\adblock\FastAdBlockEngine.kt(MODIFY)

References: 

- app\src\main\java\com\entertainmentbrowser\util\MonetizationManager.kt(MODIFY)

**Replace hardcoded monetization domains with centralized reference:**

1. Remove the hardcoded `monetizationDomains` list (lines 299-302) from the `shouldBlock` method

2. Inject `MonetizationManager` into the FastAdBlockEngine constructor (it's already a singleton, so this is safe)

3. Replace the hardcoded domain check loop (lines 303-308) with a call to `MonetizationManager.isWhitelistedDomain(domain)` or access `MonetizationManager.WHITELISTED_DOMAINS` directly

4. Update the log message on line 305 to reference that it's using the centralized monetization whitelist

**Rationale:** Eliminates duplication and ensures monetization domains are managed in one place. If domains need to be added/removed, only MonetizationManager needs updating.

### app\src\main\java\com\entertainmentbrowser\presentation\webview\AdBlockWebViewClient.kt(MODIFY)

References: 

- app\src\main\java\com\entertainmentbrowser\util\MonetizationManager.kt(MODIFY)
- app\src\main\java\com\entertainmentbrowser\presentation\webview\CustomWebView.kt(MODIFY)

**Replace hardcoded monetization domains with centralized reference:**

1. Remove the hardcoded `monetizationDomains` list (lines 298-301) from the `shouldOverrideUrlLoading` method

2. Add `MonetizationManager` as a constructor parameter to AdBlockWebViewClient

3. Replace the hardcoded domain check (lines 303-305) with a call to `MonetizationManager.isWhitelistedDomain(requestUrl)` or access `MonetizationManager.WHITELISTED_DOMAINS` directly

4. Update the comment on lines 297-301 to reference that monetization domains are managed centrally in MonetizationManager

**Note:** AdBlockWebViewClient is instantiated in CustomWebView.kt (line 289), so MonetizationManager will need to be passed through as a parameter to the CustomWebView composable and then to AdBlockWebViewClient.

**Rationale:** Completes the centralization of monetization domain whitelisting, ensuring all three components (FastAdBlockEngine, HardcodedFilters, AdBlockWebViewClient) reference the same source of truth.

### app\src\main\java\com\entertainmentbrowser\util\adblock\HardcodedFilters.kt(MODIFY)

References: 

- app\src\main\java\com\entertainmentbrowser\util\MonetizationManager.kt(MODIFY)

**Add monetization domain whitelist check:**

1. In the `shouldBlock(url: String)` method (around line 1227 based on the summary), add an early return check at the beginning of the method before any blocking logic

2. Extract the domain from the URL using the existing domain extraction logic

3. Check if the domain matches any domain in `MonetizationManager.WHITELISTED_DOMAINS`

4. If it matches, return `false` immediately to allow the request

5. Add a comment explaining this is a whitelist for monetization domains that should never be blocked

**Note:** Since HardcodedFilters is an object (singleton), you can access MonetizationManager.WHITELISTED_DOMAINS directly as a static reference, or consider passing MonetizationManager as a parameter if dependency injection is preferred.

**Rationale:** HardcodedFilters currently doesn't check monetization domains at all, which could cause it to block monetization ads. Adding this check ensures consistency with FastAdBlockEngine and AdBlockWebViewClient.

### app\src\main\java\com\entertainmentbrowser\presentation\settings\SettingsScreen.kt(MODIFY)

**Update placeholder URLs to production values:**

1. Replace the placeholder privacy policy URL on line 173 (`"https://example.com/privacy"`) with your actual privacy policy URL

2. Replace the placeholder terms of service URL on line 184 (`"https://example.com/terms"`) with your actual terms of service URL

3. If you don't have these documents yet, consider:
   - Using a privacy policy generator service (many free options available)
   - Hosting the documents on GitHub Pages or a simple static hosting service
   - Or temporarily removing these menu items until the documents are ready (though this may affect Play Store submission)

4. Add a TODO comment if these URLs are temporary and need to be updated before production release

**Rationale:** Placeholder URLs will fail app store reviews and mislead users. Privacy policies are required for Play Store submission, especially for apps that collect any user data or use third-party services. Terms of service are also recommended for legal protection.