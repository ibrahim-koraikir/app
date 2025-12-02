# Prevent Unwanted New Tabs Fix

## Problem

When browsing websites and clicking on videos or links, the same site was opening in a new tab instead of navigating in the current tab. This was annoying and created many duplicate tabs.

## Root Cause

Websites use two methods to open new windows/tabs:

1. **HTML target="_blank"** - Links with `<a href="..." target="_blank">`
2. **JavaScript window.open()** - JavaScript code that opens new windows

By default, WebView allows these behaviors, which creates new tabs in your app.

## Solution

### 1. Disabled Multiple Windows Support

In `WebViewPool.kt`, added:
```kotlin
setSupportMultipleWindows(false)
```

This tells WebView to NOT support multiple windows, so `target="_blank"` links will open in the current tab.

### 2. Handled onCreateWindow()

In `CustomWebView.kt`, added `onCreateWindow()` handler in WebChromeClient:
```kotlin
override fun onCreateWindow(...): Boolean {
    // Get the URL that wants to open
    val data = result?.extra
    
    // Load it in current tab instead
    if (data != null) {
        view?.loadUrl(data)
    }
    
    // Return false to prevent new window
    return false
}
```

This intercepts any attempts to create new windows and loads the URL in the current tab instead.

## How It Works

**Before Fix:**
```
User clicks video link
  ↓
Website tries: window.open("video.html")
  ↓
WebView creates new tab
  ↓
Result: Same site in 2 tabs ❌
```

**After Fix:**
```
User clicks video link
  ↓
Website tries: window.open("video.html")
  ↓
onCreateWindow() intercepts
  ↓
Loads URL in current tab
  ↓
Result: Video opens in same tab ✅
```

## Testing

### Test Scenario 1: Video Links
1. Open a video streaming site
2. Click on 2-3 video thumbnails
3. Expected: Videos open in the same tab, no new tabs created

### Test Scenario 2: External Links
1. Browse any website
2. Click on links that normally open in new tabs
3. Expected: Links open in current tab

### Test Scenario 3: JavaScript Popups
1. Visit sites with popup ads or windows
2. Expected: Popups are blocked or open in current tab

## Monitoring

Watch logcat for these messages:

```bash
# Windows CMD
adb logcat | findstr "onCreateWindow"

# Linux/Mac
adb logcat | grep "onCreateWindow"
```

You should see:
```
onCreateWindow called - URL: https://example.com/video, isUserGesture: true
Loading URL in current tab instead of new window: https://example.com/video
```

## Edge Cases Handled

1. **User-initiated clicks** - Opens in current tab
2. **JavaScript redirects** - Opens in current tab
3. **Popup attempts** - Blocked or opens in current tab
4. **target="_blank" links** - Opens in current tab

## Benefits

✅ **No duplicate tabs** - Same site doesn't open multiple times
✅ **Better UX** - Users stay in the same tab
✅ **Less memory usage** - Fewer tabs = less memory
✅ **Cleaner navigation** - Back button works as expected

## User Experience

**Before:**
- Click video → New tab opens
- Click another video → Another new tab
- Result: 5 tabs of the same site 😤

**After:**
- Click video → Opens in current tab
- Click another video → Replaces current content
- Result: 1 tab, smooth navigation 😊

## Files Modified

1. ✅ `CustomWebView.kt` - Added `onCreateWindow()` handler
2. ✅ `WebViewPool.kt` - Disabled `setSupportMultipleWindows()`

## Build & Test

```bash
# Clean build
.\gradlew clean

# Build and install
.\gradlew assembleDebug installDebug

# Watch for window creation attempts
adb logcat | findstr "onCreateWindow"
```

## Related Issues

- Duplicate tabs from same website
- target="_blank" links opening new tabs
- JavaScript window.open() creating tabs
- Popup windows

## Future Enhancements

If you want to allow new tabs in specific cases:

1. Check `isUserGesture` - Only allow if user clicked
2. Check URL domain - Allow new tabs for external domains
3. Add user preference - "Open links in new tab" setting

Example:
```kotlin
override fun onCreateWindow(...): Boolean {
    // Allow new tab for external domains
    if (isUserGesture && isDifferentDomain(data)) {
        // Create new tab via ViewModel
        return true
    }
    // Otherwise load in current tab
    view?.loadUrl(data)
    return false
}
```

---

**Status**: ✅ FIXED - Ready for testing
**Priority**: MEDIUM - Improves user experience
**Impact**: All users benefit from cleaner navigation
