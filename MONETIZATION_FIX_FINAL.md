I have the following verification comments after thorough review and exploration of the codebase. Implement the comments by following the instructions in the comments verbatim.

---
## Comment 1: Legacy WRITE_EXTERNAL_STORAGE permission still present despite scoped storage plan.

In `app/src/main/AndroidManifest.xml`, locate the `uses-permission` entries for storage near the top of the file. Remove the `WRITE_EXTERNAL_STORAGE` permission block entirely, including its `maxSdkVersion` and `tools:ignore` attributes. Leave the `READ_EXTERNAL_STORAGE` permission constrained to `maxSdkVersion="32"` and the `READ_MEDIA_VIDEO` permission constrained to `minSdkVersion="33"` as-is. Do not add any new broad storage permissions elsewhere.

### Referred Files
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\AndroidManifest.xml
---
## Comment 2: CustomWebView JavaScript interface lacks planned URL validation and hardening.

In `app/src/main/java/com/entertainmentbrowser/presentation/webview/CustomWebView.kt`, update the anonymous `jsInterface` object. Add a private helper method inside the object, for example `isValidAndSafeUrl(url: String): Boolean`, that checks for non-empty, non-"null" strings, enforces a reasonable maximum length (e.g. 2048 chars), parses the URL via `Uri.parse` or `URL` to ensure it is syntactically valid, and only allows `http` or `https` schemes. Inside `onVideoDetected` and `onVideoElementLongPress`, first validate the `videoUrl` with this helper and return early if it fails; only then call `VideoDetector.isVideoUrl` or `onLongPress`. Wrap each `@JavascriptInterface` method body in a try/catch that logs the exception and returns gracefully without propagating to the app, so malformed JS input cannot crash the process.

### Referred Files
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\presentation\webview\CustomWebView.kt
---
## Comment 3: CustomWebView still uses DownloadManager directly instead of DownloadRepository.

In `app/src/main/java/com/entertainmentbrowser/presentation/webview/CustomWebView.kt`, update the `CustomWebView` composable signature to accept a `DownloadRepository` parameter after the existing `fastAdBlockEngine` and `webViewStateManager` parameters. In the `setDownloadListener` lambda, remove all direct `DownloadManager` usage, including building and enqueuing `DownloadManager.Request`. Instead, derive a filename with `URLUtil.guessFileName` as before, then call the injected `DownloadRepository` (for example `downloadRepository.startDownload(url, filename, null)` or similar depending on your API). Make sure any required context-dependent headers (cookies, user agent) are passed via the repository or its data source layer, not from the WebView directly. Update all call sites of `CustomWebView` to provide the `DownloadRepository` via Hilt or another DI mechanism so the API contract remains consistent.

### Referred Files
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\presentation\webview\CustomWebView.kt
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\domain\repository\DownloadRepository.kt
---
## Comment 4: Room database still uses fallbackToDestructiveMigration, risking silent data loss.

In `app/src/main/java/com/entertainmentbrowser/di/DatabaseModule.kt`, locate the `provideAppDatabase` function where the `Room.databaseBuilder` is configured. Remove the `.fallbackToDestructiveMigration()` call from the builder chain so only `.addMigrations(MIGRATION_4_5)` and the `.addCallback(...)` remain before `.build()`. Optionally add a comment above the builder indicating that destructive migrations are intentionally disabled and that all future schema changes must provide explicit `Migration` objects.

### Referred Files
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\di\DatabaseModule.kt
---
## Comment 5: WebViewStateManager does not enforce MAX_CACHED_WEBVIEWS or implement LRU trimming.

In `app/src/main/java/com/entertainmentbrowser/util/WebViewStateManager.kt`, introduce a `ConcurrentHashMap<String, Long>` or similar to track last access timestamps per `tabId`. Update `getWebViewForTab`, `pauseWebView`, and `resumeWebView` to refresh the timestamp each time a tab is used. Before creating a new WebView in `getWebViewForTab`, check if `webViewCache.size` is greater than or equal to `MAX_CACHED_WEBVIEWS` and, if so, identify the least recently used `tabId` (oldest timestamp) and call `removeWebView` on that id. Add a public `trimCache(maxSize: Int = MAX_CACHED_WEBVIEWS)` method that repeatedly evicts LRU entries until the cache size is below the specified limit. Optionally call `trimCache()` from lifecycle-aware parts of the app (e.g. when sending the app to background) to proactively reduce memory usage.

### Referred Files
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\util\WebViewStateManager.kt
---
## Comment 6: WebViewViewModel still over-counts monetization actions on non-explicit user events.

In `app/src/main/java/com/entertainmentbrowser/presentation/webview/WebViewViewModel.kt`, remove the `trackUserAction()` invocation from `createTabForUrl`, so creating the initial tab for the incoming URL does not increment the monetization counter. In the `onEvent` `when` branch for `WebViewEvent.UpdateUrl`, delete the call to `trackUserAction()` so automatic navigation and redirects do not count as actions. In the branch for `WebViewEvent.VideoDetected`, remove the `trackUserAction()` call so passive detection does not contribute. Keep the call in `openNewTab` but ensure it remains guarded by `if (!monetizationManager.isMonetizationAd(url))`. Optionally, add new `trackUserAction()` calls only in handlers for explicit user-initiated actions such as “start download” or “share”, if you want those to influence ad frequency.

### Referred Files
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\presentation\webview\WebViewViewModel.kt
---
## Comment 7: Monetization whitelist is added but not integrated in ad-blocking components.

In `app/src/main/java/com/entertainmentbrowser/util/adblock/FastAdBlockEngine.kt`, inject `MonetizationManager` via the constructor alongside the `Context`. Inside `shouldBlock`, remove the local `monetizationDomains` list and instead extract the domain from the URL, then call `monetizationManager.isWhitelistedDomain(domain)`. If it returns true, immediately return `false` to allow the request. In `app/src/main/java/com/entertainmentbrowser/presentation/webview/AdBlockWebViewClient.kt`, add a `MonetizationManager` parameter to the constructor, and replace the local `monetizationDomains` list in `shouldOverrideUrlLoading` with a check against `monetizationManager.isMonetizationAd(requestUrl)` or a domain-based whitelist using `WHITELISTED_DOMAINS`. Update instantiation sites (e.g. in `CustomWebView`) to pass the `MonetizationManager` from DI. Finally, in `app/src/main/java/com/entertainmentbrowser/util/adblock/HardcodedFilters.kt`, at the top of `shouldBlock(url: String)`, extract the domain and return `false` early if it matches any domain in `MonetizationManager.WHITELISTED_DOMAINS` to ensure monetization traffic is never blocked by the hardcoded fallback.

### Referred Files
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\util\MonetizationManager.kt
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\util\adblock\FastAdBlockEngine.kt
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\presentation\webview\AdBlockWebViewClient.kt
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\util\adblock\HardcodedFilters.kt
---
## Comment 8: HardcodedFilters.shouldBlock does not exempt monetization domains as planned.

In `app/src/main/java/com/entertainmentbrowser/util/adblock/HardcodedFilters.kt`, modify the `shouldBlock(url: String)` function. At the beginning of the method, before converting to lowercase and running block checks, extract the domain from the URL (either by reusing existing logic or adding a small helper similar to `FastAdBlockEngine.extractDomain`). Compare this domain against `MonetizationManager.WHITELISTED_DOMAINS`, and if it matches any entry, immediately return `false` to allow the request. Then proceed with the existing checks (`adDomains`, `adKeywords`, `hasSuspiciousPath`, `hasExcessiveTrackingParams`) only for non-whitelisted domains.

### Referred Files
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\util\adblock\HardcodedFilters.kt
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\util\MonetizationManager.kt
---
## Comment 9: SettingsScreen still uses placeholder privacy/terms URLs instead of production values.

In `app/src/main/java/com/entertainmentbrowser/presentation/settings/SettingsScreen.kt`, update the `onClick` handlers for the "Privacy Policy" and "Terms of Service" items. Replace `"https://example.com/privacy"` with the real URL where your privacy policy is hosted, and similarly replace `"https://example.com/terms"` with your live terms of service URL. If these documents are not yet available, either point them to temporary hosted documents (e.g. on your domain or GitHub Pages) and add a TODO comment above indicating they must be updated before release, or temporarily hide/disable these menu items until proper documents exist.

### Referred Files
- c:\Users\w\Desktop\AndroidStudioProjects\Bro2\app\src\main\java\com\entertainmentbrowser\presentation\settings\SettingsScreen.kt
---