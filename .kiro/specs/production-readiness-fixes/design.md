# Design Document

## Overview

This design addresses 8 critical production readiness issues across security, compatibility, data integrity, performance, and maintainability. The fixes are surgical and focused, targeting specific problem areas without requiring architectural changes. Each fix is designed to be independent and can be implemented incrementally.

## Architecture

### Current Architecture Context

The Entertainment Browser follows Clean Architecture with MVVM:
- **Presentation Layer**: Jetpack Compose UI with ViewModels
- **Domain Layer**: Repository interfaces and business models
- **Data Layer**: Room database, DataStore, and repository implementations
- **Utility Layer**: WebView management, ad-blocking, monetization

### Affected Components

```
app/src/main/
├── AndroidManifest.xml                    [MODIFY: Permissions]
├── java/com/entertainmentbrowser/
│   ├── di/
│   │   └── DatabaseModule.kt              [MODIFY: Migration safety]
│   ├── data/repository/
│   │   └── DownloadRepositoryImpl.kt      [REFERENCE: Already correct]
│   ├── presentation/
│   │   ├── webview/
│   │   │   ├── CustomWebView.kt           [MODIFY: JS security + download integration]
│   │   │   ├── AdBlockWebViewClient.kt    [MODIFY: Centralize whitelist]
│   │   │   └── WebViewViewModel.kt        [MODIFY: Fix tracking]
│   │   └── settings/
│   │       └── SettingsScreen.kt          [MODIFY: Update URLs]
│   └── util/
│       ├── WebViewStateManager.kt         [MODIFY: Cache management]
│       ├── MonetizationManager.kt         [MODIFY: Centralize whitelist]
│       └── adblock/
│           ├── FastAdBlockEngine.kt       [MODIFY: Use central whitelist]
│           └── HardcodedFilters.kt        [MODIFY: Add whitelist check]
```

## Components and Interfaces

### 1. Storage Permission Updates (AndroidManifest.xml)

**Design Decision**: Remove deprecated permissions and rely on scoped storage APIs.

**Changes**:
- Remove `WRITE_EXTERNAL_STORAGE` (deprecated on Android 10+)
- Keep `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"` for backward compatibility
- Keep `READ_MEDIA_VIDEO` with `minSdkVersion="33"` for Android 13+

**Rationale**: The app already uses `Environment.DIRECTORY_DOWNLOADS` in `DownloadRepositoryImpl`, which works with scoped storage without requiring `WRITE_EXTERNAL_STORAGE`. This change ensures Play Store compliance and follows Android best practices.

### 2. JavaScript Interface Security (CustomWebView.kt)

**Design Decision**: Add comprehensive input validation layer before processing JavaScript calls.

**New Component**: URL Validation Helper

```kotlin
private fun isValidAndSafeUrl(url: String): Boolean {
    // Check 1: Non-empty and not "null" string
    if (url.isBlank() || url == "null") return false
    
    // Check 2: Length limit (prevent DoS)
    if (url.length > 2048) return false
    
    // Check 3: Valid URL format
    val uri = try { Uri.parse(url) } catch (e: Exception) { return false }
    
    // Check 4: Allowed schemes only
    if (uri.scheme !in listOf("http", "https")) return false
    
    // Check 5: Has valid host
    if (uri.host.isNullOrBlank()) return false
    
    return true
}
```

**Modified Interface Methods**:
```kotlin
@JavascriptInterface
fun onVideoDetected(videoUrl: String) {
    try {
        if (!isValidAndSafeUrl(videoUrl)) {
            Log.w(TAG, "Invalid video URL rejected: $videoUrl")
            return
        }
        // Existing logic...
    } catch (e: Exception) {
        Log.e(TAG, "Error in onVideoDetected", e)
    }
}

@JavascriptInterface
fun onDrmDetected() {
    try {
        // Existing logic...
    } catch (e: Exception) {
        Log.e(TAG, "Error in onDrmDetected", e)
    }
}

@JavascriptInterface
fun onVideoElementLongPress(videoUrl: String) {
    try {
        if (!isValidAndSafeUrl(videoUrl)) {
            Log.w(TAG, "Invalid long press URL rejected: $videoUrl")
            return
        }
        // Existing logic...
    } catch (e: Exception) {
        Log.e(TAG, "Error in onVideoElementLongPress", e)
    }
}
```

**Rationale**: JavaScript interfaces are exposed to potentially malicious web content. Validation prevents crashes, DoS attacks, and unintended behavior from malformed input.

### 3. Download Repository Integration (CustomWebView.kt)

**Design Decision**: Replace direct DownloadManager usage with DownloadRepository to ensure centralized tracking.

**Current Problem**: 
```kotlin
// Lines 123-177: Direct DownloadManager usage bypasses tracking
setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
    val request = DownloadManager.Request(Uri.parse(url))
    // ... direct DownloadManager calls
}
```

**New Design**:
```kotlin
@Composable
fun CustomWebView(
    // ... existing parameters
    fastAdBlockEngine: FastAdBlockEngine,
    downloadRepository: DownloadRepository, // NEW PARAMETER
    // ... rest
) {
    // Replace download listener implementation
    setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
        val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
        
        // Show quality selection dialog if video
        if (mimetype.startsWith("video/")) {
            onShowDownloadDialog(url, filename)
        } else {
            // Use repository for tracking
            coroutineScope.launch {
                downloadRepository.startDownload(
                    url = url,
                    filename = filename,
                    quality = "default"
                )
            }
        }
    }
}
```

**Dependency Injection**: Add `DownloadRepository` to CustomWebView call sites (WebViewScreen.kt).

**Rationale**: Centralized download tracking enables pause/resume, history, and consistent UI updates. The repository already handles DownloadManager internally with proper tracking.

### 4. Database Migration Safety (DatabaseModule.kt)

**Design Decision**: Remove destructive migration fallback to force explicit migration paths.

**Current Problem**:
```kotlin
.addMigrations(MIGRATION_4_5)
.fallbackToDestructiveMigration() // DANGEROUS: Silently wipes data on error
```

**New Design**:
```kotlin
.addMigrations(MIGRATION_4_5)
// Destructive migration intentionally disabled to prevent data loss.
// All schema changes must provide explicit migration paths.
// If migration fails, the app will crash with a clear error instead of
// silently deleting user data (favorites, tabs, download history).
```

**Error Handling**: Migration failures will throw `IllegalStateException`, which should be caught in `EntertainmentBrowserApp` and logged with clear error messages for debugging.

**Rationale**: Silent data loss is catastrophic for user trust. Explicit failures during development ensure migrations are properly tested before release.

### 5. WebView Cache Management (WebViewStateManager.kt)

**Design Decision**: Implement LRU eviction strategy with proactive memory management.

**New Data Structure**:
```kotlin
private val lastAccessTime = ConcurrentHashMap<String, Long>()
```

**Cache Enforcement Logic**:
```kotlin
fun getWebViewForTab(tabId: String, context: Context): WebView {
    // Update access time
    lastAccessTime[tabId] = System.currentTimeMillis()
    
    // Check cache limit before creating new WebView
    if (webViewCache.size >= MAX_CACHED_WEBVIEWS && !webViewCache.containsKey(tabId)) {
        evictLeastRecentlyUsed()
    }
    
    return webViewCache.getOrPut(tabId) {
        createWebView(context)
    }
}

private fun evictLeastRecentlyUsed() {
    // Find LRU tab (excluding currently active tabs)
    val lruTabId = lastAccessTime
        .filter { (tabId, _) -> webViewCache.containsKey(tabId) }
        .minByOrNull { (_, timestamp) -> timestamp }
        ?.key
    
    lruTabId?.let { removeWebView(it) }
}

fun trimCache(maxSize: Int = MAX_CACHED_WEBVIEWS) {
    while (webViewCache.size > maxSize) {
        evictLeastRecentlyUsed()
    }
}
```

**Proactive Cleanup**:
```kotlin
fun pauseWebView(tabId: String) {
    webViewCache[tabId]?.onPause()
    // Trim cache when app goes to background
    trimCache(MAX_CACHED_WEBVIEWS / 2) // Keep only half when backgrounded
}
```

**Rationale**: Unbounded cache growth causes OOM crashes on low-end devices. LRU eviction maintains performance while respecting memory constraints.

### 6. Monetization Tracking Accuracy (WebViewViewModel.kt)

**Design Decision**: Remove automatic tracking, keep only explicit user actions.

**Removals**:
```kotlin
// REMOVE from createTabForUrl (line 78)
monetizationManager.trackUserAction() // ❌ Initial load is not user action

// REMOVE from onEvent(WebViewEvent.UpdateUrl) (line 116)
monetizationManager.trackUserAction() // ❌ Auto-navigation is not user action

// REMOVE from onEvent(WebViewEvent.VideoDetected) (line 144)
monetizationManager.trackUserAction() // ❌ Passive detection is not user action
```

**Keep**:
```kotlin
// KEEP in openNewTab (line 439)
if (!url.contains("monetization-domain")) {
    monetizationManager.trackUserAction() // ✅ Explicit user action
}
```

**Optional Additions** (if desired):
```kotlin
// In startDownloadWithFilename
monetizationManager.trackUserAction() // User-initiated download

// In shareCurrentUrl
monetizationManager.trackUserAction() // User-initiated share
```

**Updated Documentation**:
```kotlin
/**
 * Tracks explicit user actions for monetization purposes.
 * Only counts deliberate user interactions:
 * - Manually opening a new tab
 * - Initiating downloads (optional)
 * - Sharing content (optional)
 * 
 * Does NOT count:
 * - Automatic page loads
 * - URL changes during navigation
 * - Passive video detection
 */
private fun trackUserAction() {
    monetizationManager.trackUserAction()
}
```

**Rationale**: Current implementation over-counts by 10-20x, causing excessive ad interruptions. Only genuine user interactions should trigger monetization.

### 7. Centralized Monetization Whitelist (MonetizationManager.kt)

**Design Decision**: Make MonetizationManager the single source of truth for monetization domains.

**Public API**:
```kotlin
object MonetizationManager {
    // Renamed for clarity
    val MONETIZATION_DOMAINS = listOf(
        "monetization-domain1.com",
        "monetization-domain2.com",
        "ad-network.com"
    )
    
    /**
     * Checks if a URL belongs to a monetization domain.
     * Used by ad-blocking components to whitelist monetization ads.
     */
    fun isMonetizationDomain(url: String): Boolean {
        val domain = extractDomain(url)
        return MONETIZATION_DOMAINS.any { domain.contains(it) }
    }
    
    private fun extractDomain(url: String): String {
        return try {
            Uri.parse(url).host?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
```

**Consumer Updates**:

**FastAdBlockEngine.kt**:
```kotlin
// Remove hardcoded list (lines 299-302)
// Replace check (lines 303-308) with:
if (MonetizationManager.isMonetizationDomain(url)) {
    Log.d(TAG, "Allowing monetization domain: $domain")
    return false
}
```

**AdBlockWebViewClient.kt**:
```kotlin
// Remove hardcoded list (lines 298-301)
// Replace check (lines 303-305) with:
if (MonetizationManager.isMonetizationDomain(requestUrl)) {
    return false // Allow monetization navigation
}
```

**HardcodedFilters.kt**:
```kotlin
fun shouldBlock(url: String): Boolean {
    // Add at beginning of method
    if (MonetizationManager.isMonetizationDomain(url)) {
        return false // Never block monetization domains
    }
    
    // ... existing blocking logic
}
```

**Rationale**: Three separate hardcoded lists create maintenance burden and risk inconsistency. Single source of truth ensures all components stay synchronized.

### 8. Production URL Configuration (SettingsScreen.kt)

**Design Decision**: Replace placeholder URLs with production values or TODO comments.

**Current**:
```kotlin
// Line 173
uriHandler.openUri("https://example.com/privacy")

// Line 184
uriHandler.openUri("https://example.com/terms")
```

**Option A - Production URLs** (if available):
```kotlin
// Line 173
uriHandler.openUri("https://entertainmentbrowser.app/privacy-policy")

// Line 184
uriHandler.openUri("https://entertainmentbrowser.app/terms-of-service")
```

**Option B - TODO Comments** (if not ready):
```kotlin
// Line 173
// TODO: Replace with production privacy policy URL before Play Store submission
uriHandler.openUri("https://example.com/privacy")

// Line 184
// TODO: Replace with production terms of service URL before Play Store submission
uriHandler.openUri("https://example.com/terms")
```

**Recommendation**: Use a privacy policy generator (e.g., TermsFeed, FreePrivacyPolicy) and host on GitHub Pages or similar free hosting.

**Rationale**: Play Store requires valid privacy policy URLs for apps that collect data or use third-party services. Placeholder URLs will cause rejection.

## Data Models

No new data models required. All fixes work with existing models:
- `Download` entity (already exists in Room)
- `Tab` entity (already exists in Room)
- WebView cache uses existing `ConcurrentHashMap<String, WebView>`

## Error Handling

### JavaScript Interface Validation
- **Invalid URLs**: Log warning and reject silently (don't crash)
- **Exceptions**: Catch all exceptions, log with stack trace, continue execution

### Download Integration
- **Repository errors**: Already handled by DownloadRepositoryImpl
- **Coroutine failures**: Caught by existing error handling in WebViewViewModel

### Database Migration
- **Migration failures**: Throw `IllegalStateException` with clear message
- **App-level handling**: Log error and show user-friendly message in EntertainmentBrowserApp

### Cache Eviction
- **Eviction failures**: Log error but continue (don't crash)
- **Memory pressure**: Proactively trim cache on background

### Monetization Tracking
- **No error handling needed**: Simple counter increment, no failure modes

## Testing Strategy

### Unit Tests

**JavaScript Interface Validation**:
- Test valid URLs (http, https)
- Test invalid URLs (null, empty, "null", javascript:, file:, data:)
- Test length limits (2048 chars)
- Test malformed URLs
- Test exception handling

**WebView Cache Management**:
- Test LRU eviction logic
- Test cache size enforcement
- Test access time tracking
- Test proactive trimming

**Monetization Whitelist**:
- Test `isMonetizationDomain()` with various URLs
- Test domain extraction logic
- Test case insensitivity

### Integration Tests

**Download Repository Integration**:
- Test downloads appear in Room database
- Test downloads appear in UI
- Test quality selection flow

**Database Migration**:
- Test migration 4→5 succeeds
- Test missing migration fails (not silently)

### Manual Testing

**Storage Permissions**:
- Test on Android 9 (should request READ_EXTERNAL_STORAGE)
- Test on Android 10-12 (should not request WRITE_EXTERNAL_STORAGE)
- Test on Android 13+ (should request READ_MEDIA_VIDEO)
- Verify downloads work on all versions

**Monetization Tracking**:
- Count actions during normal browsing
- Verify ads appear at appropriate intervals (not too frequently)
- Test explicit actions (new tab, download, share)

**Cache Management**:
- Open 20+ tabs on low-end device
- Monitor memory usage
- Verify no OOM crashes

**Production URLs**:
- Click privacy policy link
- Click terms of service link
- Verify correct documents open

## Performance Considerations

### Memory Impact
- **WebView Cache**: Reduces memory by enforcing limits (positive impact)
- **JavaScript Validation**: Negligible overhead (simple string checks)
- **Download Integration**: No change (same underlying DownloadManager)

### CPU Impact
- **Monetization Tracking**: Reduces CPU by removing unnecessary tracking (positive impact)
- **Whitelist Centralization**: Negligible (same checks, different location)

### Storage Impact
- **Download Tracking**: Minimal (adds Room entries, already happening for some downloads)
- **Database Migration**: No change (same schema)

## Security Considerations

### JavaScript Interface Hardening
- **Threat**: Malicious web pages exploiting interface methods
- **Mitigation**: Comprehensive input validation, exception handling
- **Impact**: Prevents crashes, DoS, and unintended behavior

### Storage Permissions
- **Threat**: Overly broad permissions enabling data exfiltration
- **Mitigation**: Remove deprecated permissions, use scoped storage
- **Impact**: Reduces attack surface, improves privacy

### Database Migration
- **Threat**: Data loss from migration errors
- **Mitigation**: Remove silent fallback, force explicit migrations
- **Impact**: Prevents catastrophic data loss

## Rollback Plan

All changes are backward compatible and can be rolled back independently:

1. **Storage Permissions**: Revert manifest changes (no code impact)
2. **JavaScript Security**: Remove validation (restore original interface)
3. **Download Integration**: Revert to direct DownloadManager (restore lines 123-177)
4. **Database Migration**: Re-add `.fallbackToDestructiveMigration()` (not recommended)
5. **Cache Management**: Remove eviction logic (restore original)
6. **Monetization Tracking**: Re-add removed tracking calls
7. **Whitelist Centralization**: Restore hardcoded lists in each component
8. **Production URLs**: Revert to placeholder URLs

Each fix is isolated and can be reverted without affecting others.

## Implementation Order

Recommended order (least to most risky):

1. **Production URLs** (trivial, no risk)
2. **Monetization Whitelist** (refactoring, low risk)
3. **Storage Permissions** (manifest only, low risk)
4. **JavaScript Security** (additive, low risk)
5. **Monetization Tracking** (behavioral change, medium risk)
6. **Cache Management** (new logic, medium risk)
7. **Download Integration** (requires testing, medium risk)
8. **Database Migration** (requires careful testing, higher risk)

## Dependencies

- **Existing**: Room, Hilt, Coroutines, Compose
- **New**: None (all fixes use existing dependencies)

## Monitoring and Metrics

Post-deployment monitoring:

1. **Crash Rate**: Monitor for JavaScript interface crashes (should decrease)
2. **OOM Crashes**: Monitor for out-of-memory errors (should decrease)
3. **Download Success Rate**: Monitor download completion (should remain stable)
4. **Migration Failures**: Monitor database migration errors (should be zero)
5. **Monetization Revenue**: Monitor ad impressions (should remain stable or increase due to better UX)

## Documentation Updates

Update the following documentation:
- README: Note Android 10+ scoped storage compliance
- CHANGELOG: Document all 8 fixes
- Developer docs: Document MonetizationManager as whitelist source
- Testing docs: Add validation test cases
