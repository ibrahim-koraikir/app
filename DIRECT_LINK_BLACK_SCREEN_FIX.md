# Direct Link Ad Black Screen Fix

## Problem
1. Direct link ads were being blocked, showing black/blank screens
2. When clicking videos/links, ad redirects were blocked completely, preventing access to actual content

## Root Cause
1. WebView had BLACK background color set in WebViewPool
2. `shouldOverrideUrlLoading` was blocking ad redirect navigation completely
3. Many sites use ad networks as intermediaries: Click → Ad Redirect → Actual Content
4. Blocking the redirect prevented the chain from completing

## Solution

### 1. Allow Ad Redirect Navigation (IMPORTANT CHANGE!)
**Removed** the blocking of ad redirects in `shouldOverrideUrlLoading`:

```kotlin
// OLD CODE (REMOVED):
if (isAdRedirect) {
    view?.stopLoading()
    return true  // This prevented redirect chains!
}

// NEW CODE:
// Allow all HTTPS navigation
// Ad resources are blocked in shouldInterceptRequest
// Ad containers are hidden by CSS injection
return false
```

**Why this works:**
- User clicks video/link → Goes through ad redirect → Reaches actual content
- Ad resources (scripts, images, iframes) are still blocked in `shouldInterceptRequest`
- Ad containers are hidden by CSS injection
- User gets to their destination without seeing ads

### 2. Transparent WebView Background
Changed WebView background from BLACK to TRANSPARENT in multiple places:

**WebViewPool.kt:**
```kotlin
// Set transparent background to prevent black screen when ads are blocked
setBackgroundColor(android.graphics.Color.TRANSPARENT)
```

**CustomWebView.kt:**
```kotlin
// Set transparent background to prevent black screen
setBackgroundColor(android.graphics.Color.TRANSPARENT)
```

### 2. CSS Injection to Hide Ad Containers
Added CSS injection on page load that hides common ad container elements:

```javascript
// Hides elements with ad-related IDs, classes, and data attributes
[id*="ad-"], [class*="ad-"], [data-ad], iframe[src*="doubleclick"], etc.
```

The CSS uses aggressive hiding:
- `display: none !important`
- `visibility: hidden !important`
- `opacity: 0 !important`
- `height: 0 !important`
- `width: 0 !important`
- `position: absolute !important; left: -9999px !important`

### 3. Targets Multiple Ad Networks
The CSS specifically targets:
- Google Ads (doubleclick, googlesyndication, adsbygoogle)
- Yandex Ads (yandex.ru/ads, yastatic.net/partner)
- Native ad networks (outbrain, taboola, revcontent, mgid)
- Generic ad containers (id/class containing "ad", "ads", "advert", "sponsor", "banner")

## How It Works Now

### Scenario 1: Click on Video/Link with Ad Redirect
1. User clicks video/link
2. Page tries to redirect through ad network (e.g., doubleclick.com)
3. **Navigation is allowed** (redirect happens)
4. Ad resources (scripts, images) are blocked in `shouldInterceptRequest`
5. User reaches actual video/link destination
6. Any ad containers are hidden by CSS

### Scenario 2: Embedded Ads on Page
1. Page loads normally
2. Ad resources (scripts, images, iframes) are blocked in `shouldInterceptRequest`
3. CSS hides empty ad containers
4. User sees clean page without ads

### Before Fix
1. Click on video/link with ad redirect
2. Ad redirect blocked completely
3. Black screen shown
4. Cannot reach destination

### After Fix
1. Click on video/link with ad redirect
2. Redirect allowed to complete
3. Ad resources blocked
4. Reach destination successfully
5. No ads visible

### Test Commands
```bash
# Watch ad blocking logs
watch_adblock_logs.bat

# Test direct link blocking
test_direct_link_blocking.bat
```

## Files Modified

### 1. AdBlockWebViewClient.kt
- Added `view?.stopLoading()` when ad redirect is blocked
- Added `AD_HIDING_CSS` constant with comprehensive CSS rules
- Inject CSS on every page load in `onPageFinished()`

### 2. WebViewPool.kt
- Changed background color from `BLACK` to `TRANSPARENT`
- Prevents black screen when WebView is empty or loading

### 3. CustomWebView.kt
- Added `setBackgroundColor(android.graphics.Color.TRANSPARENT)` in DisposableEffect
- Ensures transparent background even if WebViewPool setting is overridden

## Benefits
1. No more black screens from blocked ads
2. Cleaner user experience
3. No navigation disruption
4. Works across all ad networks
5. Handles both direct link ads and embedded ad containers
