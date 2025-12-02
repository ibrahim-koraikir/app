# Privacy & Logging Policy

**Last Updated:** 2025-12-01  
**Status:** ✅ IMPLEMENTED

---

## Overview

Entertainment Browser implements strict privacy protections in logging to ensure user browsing data is never exposed in production builds.

## Privacy Protection Strategy

### URL Redaction

**Implementation:** `LogUtils.redactUrl()`

**Behavior:**
- **DEBUG builds:** Full URLs logged for development/debugging
- **RELEASE builds:** URLs redacted to domain only (e.g., `https://example.com`)

**Example:**
```kotlin
// Input: https://example.com/private/path?token=secret123
// DEBUG:   https://example.com/private/path?token=secret123
// RELEASE: https://example.com
```

### Build-Specific Logging

All verbose logging is guarded by `BuildConfig.DEBUG`:

```kotlin
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Message with sensitive data")
}
```

**Result:**
- DEBUG builds: Full logging for development
- RELEASE builds: No verbose logs, minimal production logging

## Protected Components

### 1. AdBlockWebViewClient
**File:** `app/src/main/java/com/entertainmentbrowser/presentation/webview/AdBlockWebViewClient.kt`

**Protected Logs:**
- Page loading URLs (onPageStarted)
- Blocked request URLs
- Smart redirect URLs
- Main-frame blocking decisions

**Implementation:**
```kotlin
if (BuildConfig.DEBUG) {
    Log.d(TAG, "🚫 BLOCKED: ${LogUtils.redactUrl(url)}")
}
```

### 2. AdBlockMetrics
**File:** `app/src/main/java/com/entertainmentbrowser/util/adblock/AdBlockMetrics.kt`

**Protected Logs:**
- Page start/finish URLs
- Blocked/allowed request URLs
- All metrics logging

**Implementation:**
```kotlin
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Page finished: ${LogUtils.redactUrl(url)}")
}
```

### 3. CustomWebView
**File:** `app/src/main/java/com/entertainmentbrowser/presentation/webview/CustomWebView.kt`

**Protected Logs:**
- Video detection URLs
- Long-press URLs
- Download URLs
- Navigation URLs

### 4. WebViewViewModel
**File:** `app/src/main/java/com/entertainmentbrowser/presentation/webview/WebViewViewModel.kt`

**Protected Logs:**
- Tab switching URLs
- Video detected URLs
- Download URLs

## What Gets Logged in Production

### RELEASE Builds (Production)

**Logged:**
- Error messages (without URLs)
- Warning messages with redacted URLs (domain only)
- Critical failures
- Aggregate metrics (counts, percentages)

**NOT Logged:**
- Full URLs with paths/queries
- User browsing history
- Video URLs
- Download URLs
- Debug information

### DEBUG Builds (Development)

**Logged:**
- Full URLs for debugging
- Verbose request/response details
- Performance metrics
- All debug information

## Privacy Guarantees

### ✅ What We Protect

1. **Browsing History:** URLs are redacted in production logs
2. **Query Parameters:** Sensitive data in URLs (tokens, IDs) never logged in production
3. **Video URLs:** Video detection URLs redacted in production
4. **Download URLs:** Download URLs redacted in production
5. **Personal Data:** No PII logged at any time

### ✅ What We Log (Production)

1. **Domains Only:** `https://example.com` (no paths/queries)
2. **Error Types:** Generic error messages
3. **Aggregate Stats:** Block counts, percentages
4. **App State:** Loading states, navigation states

## Implementation Details

### LogUtils.kt

Central utility for privacy-safe logging:

```kotlin
object LogUtils {
    /**
     * Redacts URL to domain only in release builds
     */
    fun redactUrl(url: String?): String {
        if (BuildConfig.DEBUG) return url ?: "[empty]"
        
        // Release: domain only
        val uri = Uri.parse(url)
        return "${uri.scheme}://${uri.host}"
    }
    
    /**
     * Debug log with URL redaction
     */
    fun d(tag: String, message: String, url: String? = null) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message.format(redactUrl(url)))
        }
    }
}
```

### Usage Pattern

**Before (Privacy Risk):**
```kotlin
Log.d(TAG, "Loading URL: $url")  // ❌ Exposes full URL in production
```

**After (Privacy Safe):**
```kotlin
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Loading URL: ${LogUtils.redactUrl(url)}")  // ✅ Protected
}
```

## Verification

### How to Verify Privacy Protection

1. **Build Release APK:**
   ```bash
   gradlew assembleRelease
   ```

2. **Check Logs:**
   ```bash
   adb logcat | grep -E "AdBlock|WebView|Video"
   ```

3. **Expected Results:**
   - No full URLs in logs
   - Only domain-level information
   - Minimal production logging

### Testing Checklist

- [ ] Release build has no full URLs in logs
- [ ] Debug build has full URLs for debugging
- [ ] AdBlockMetrics doesn't log URLs in release
- [ ] AdBlockWebViewClient redacts URLs in release
- [ ] Video detection doesn't log URLs in release
- [ ] Download URLs are redacted in release

## Compliance

### Privacy Standards

This implementation aligns with:
- **GDPR:** Minimizes personal data processing
- **CCPA:** Protects consumer privacy
- **Google Play:** Meets privacy policy requirements

### Data Minimization

We follow the principle of data minimization:
- Only log what's necessary for debugging
- Redact sensitive data in production
- No persistent storage of browsing history in logs

## Related Files

### Core Implementation
- `app/src/main/java/com/entertainmentbrowser/util/LogUtils.kt` - URL redaction utility
- `app/src/main/java/com/entertainmentbrowser/util/adblock/AdBlockMetrics.kt` - Metrics with privacy
- `app/src/main/java/com/entertainmentbrowser/presentation/webview/AdBlockWebViewClient.kt` - Protected logging

### Documentation
- `COMPREHENSIVE_CODE_REVIEW.md` - Overall code review
- `CODE_REVIEW_SUMMARY.md` - Security review summary
- `.kiro/specs/production-readiness-fixes/` - Production readiness spec

## Future Improvements

1. **Crash Reporting:** Ensure crash reports also redact URLs
2. **Analytics:** If analytics are added, ensure no URL logging
3. **Remote Logging:** If remote logging is added, enforce redaction
4. **Audit Tool:** Create automated tool to scan for unprotected logging

## Conclusion

Entertainment Browser implements comprehensive privacy protections in logging:
- ✅ URLs redacted in production (domain only)
- ✅ Verbose logging disabled in release builds
- ✅ No user browsing data exposed in logs
- ✅ Full debugging capability in development builds

**Privacy is protected by default. User browsing data never leaves the device via logs.**
