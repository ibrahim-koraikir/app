# Monetization Implementation Guide

## Overview

Your app now includes a smart monetization system that shows your smartlink ads every 7-12 user actions. The ads open in new tabs and are never blocked by the ad blocker.

## Your Ad URLs

1. **Primary Ad:** `https://www.effectivegatecpm.com/hypsia868?key=d55fe3c96beb154d635fe6ee82094511`
2. **Secondary Ad:** `https://otieu.com/4/10194754`

The system rotates between these two URLs to maximize revenue.

## How It Works

### 1. Action Tracking
The system tracks these user actions:
- **URL changes** (clicking links, navigating)
- **Video detection** (when user finds a video)
- Any navigation within the WebView

### 2. Random Threshold
- After each ad is shown, a new random threshold is set between **7-12 actions**
- This makes the ads feel natural and not predictable
- Users won't notice a pattern

### 3. Ad Display
When the action count reaches the threshold:
1. System gets the next ad URL (rotates between your 2 URLs)
2. Opens ad in a **new tab** automatically
3. User sees the ad page
4. Counter resets with new random threshold (7-12)

### 4. Whitelist Protection
Your ad domains are **permanently whitelisted**:
- `effectivegatecpm.com`
- `otieu.com`

These domains will **NEVER** be blocked by the ad blocker, ensuring your monetization always works.

### 5. HTTP Exception
Your monetization ads are allowed to use HTTP (not just HTTPS):
- Most ad networks use HTTP redirects
- The app makes an exception for your monetization domains
- All other URLs still require HTTPS for security

## Implementation Details

### Files Created/Modified

#### 1. MonetizationManager.kt (NEW)
Location: `app/src/main/java/com/entertainmentbrowser/util/MonetizationManager.kt`

**Features:**
- Tracks user actions
- Manages random thresholds (7-12)
- Rotates between ad URLs
- Persists state using DataStore
- Provides whitelist for ad domains

**Key Functions:**
```kotlin
// Track a user action
monetizationManager.trackAction()

// Check if should show ad
if (monetizationManager.shouldShowAd()) {
    val adUrl = monetizationManager.getNextAdUrl()
    // Open ad in new tab
}

// Reset after showing ad
monetizationManager.resetAfterAdShown()
```

#### 2. WebViewViewModel.kt (MODIFIED)
**Changes:**
- Injected `MonetizationManager` dependency
- Added `trackUserAction()` function
- Tracks actions on:
  - URL changes (user clicks/navigates)
  - Video detection
- Automatically opens ad in new tab when threshold reached

#### 3. FastAdBlockEngine.kt (MODIFIED)
**Changes:**
- Added monetization domain whitelist check
- Your ad domains are checked FIRST before any blocking rules
- Logs when monetization domains are allowed

```kotlin
// CRITICAL: Never block monetization ad domains
val monetizationDomains = listOf(
    "effectivegatecpm.com",
    "otieu.com"
)
```

## User Experience

### What Users See

1. **Normal Browsing:**
   - User browses websites normally
   - Clicks links, watches videos
   - No interruption

2. **After 7-12 Actions:**
   - A new tab automatically opens with your ad
   - User can close the tab or interact with the ad
   - Browsing continues normally

3. **Ad Frequency:**
   - Random between 7-12 actions
   - Feels natural, not intrusive
   - Maximizes revenue without annoying users

### Example Flow

```
Action 1: User clicks link → Counter: 1/9
Action 2: User clicks another link → Counter: 2/9
Action 3: User finds video → Counter: 3/9
Action 4: User navigates → Counter: 4/9
Action 5: User clicks link → Counter: 5/9
Action 6: User navigates → Counter: 6/9
Action 7: User clicks link → Counter: 7/9
Action 8: User finds video → Counter: 8/9
Action 9: User clicks link → Counter: 9/9
→ 🎯 AD OPENS IN NEW TAB (effectivegatecpm.com)
→ Counter resets to 0/11 (new random threshold)
```

## Revenue Optimization

### Why This Approach Works

1. **High Frequency:** 7-12 actions is frequent enough to generate good revenue
2. **Not Annoying:** Random threshold prevents predictability
3. **New Tab:** Doesn't interrupt current browsing
4. **Rotation:** Two ad URLs maximize fill rate
5. **Never Blocked:** Whitelist ensures 100% ad delivery

### Expected Performance

- **Active User:** 50-100 actions per session
- **Ads Per Session:** 5-10 ads
- **Daily Active User:** 10-20 ads per day
- **1000 DAU:** 10,000-20,000 ad impressions per day

## Testing

### How to Test

1. **Build and Install:**
   ```bash
   gradlew assembleDebug
   gradlew installDebug
   ```

2. **Monitor Logs:**
   ```bash
   adb logcat | findstr "MonetizationManager"
   ```

3. **Perform Actions:**
   - Click links 7-12 times
   - Watch for new tab opening with your ad

4. **Check Logs:**
   ```
   MonetizationManager: Action tracked: 1/9
   MonetizationManager: Action tracked: 2/9
   ...
   MonetizationManager: Should show ad: 9 >= 9
   MonetizationManager: Next ad URL: https://www.effectivegatecpm.com/...
   WebViewViewModel: 💰 Showing monetization ad
   MonetizationManager: Reset after ad: new threshold=11
   ```

### Manual Testing

1. Open app
2. Browse to any website
3. Click links/navigate 7-12 times
4. **Expected:** New tab opens with your ad
5. Close ad tab or interact with it
6. Continue browsing
7. After another 7-12 actions, another ad appears

## Customization

### Change Ad Frequency

Edit `MonetizationManager.kt`:

```kotlin
// Current: 7-12 actions
private const val MIN_ACTIONS = 7
private const val MAX_ACTIONS = 12

// More frequent (5-8 actions):
private const val MIN_ACTIONS = 5
private const val MAX_ACTIONS = 8

// Less frequent (10-15 actions):
private const val MIN_ACTIONS = 10
private const val MAX_ACTIONS = 15
```

### Add More Ad URLs

Edit `MonetizationManager.kt`:

```kotlin
private val AD_URLS = listOf(
    "https://www.effectivegatecpm.com/hypsia868?key=d55fe3c96beb154d635fe6ee82094511",
    "https://otieu.com/4/10194754",
    "https://your-third-ad-url.com",  // Add more here
    "https://your-fourth-ad-url.com"
)

// Don't forget to whitelist the domains!
val WHITELISTED_DOMAINS = listOf(
    "effectivegatecpm.com",
    "otieu.com",
    "your-third-ad-domain.com",
    "your-fourth-ad-domain.com"
)
```

### Track Different Actions

Edit `WebViewViewModel.kt` to track more actions:

```kotlin
// Track download button clicks
is WebViewEvent.DownloadVideo -> {
    startDownload()
    trackUserAction()  // Add this
}

// Track share actions
is WebViewEvent.Share -> {
    shareCurrentUrl()
    trackUserAction()  // Add this
}
```

## Troubleshooting

### Ads Not Showing

1. **Check Logs:**
   ```bash
   adb logcat | findstr "MonetizationManager"
   ```

2. **Verify Action Tracking:**
   - Look for "Action tracked" messages
   - Ensure counter is incrementing

3. **Check Threshold:**
   - Look for "Should show ad" message
   - Verify threshold is being reached

### Ads Being Blocked

1. **Check Whitelist:**
   - Verify domains in `FastAdBlockEngine.kt`
   - Look for "Allowing monetization domain" logs

2. **Check Ad URLs:**
   - Ensure URLs are correct in `MonetizationManager.kt`
   - Test URLs in browser first

### Ads Not Opening in New Tab

1. **Check Tab Creation:**
   - Look for "Failed to open new tab" errors
   - Verify `openNewTab()` is being called

2. **Check Permissions:**
   - Ensure app has internet permission
   - Check for any security restrictions

## Analytics & Monitoring

### Key Metrics to Track

1. **Action Count:** How many actions before ad
2. **Ad Impressions:** How many ads shown
3. **Ad Clicks:** How many users interact with ads
4. **Revenue:** Track earnings from ad network

### Logging

The system logs important events:
- ✅ Action tracking
- ✅ Threshold reached
- ✅ Ad URL selection
- ✅ Ad display
- ✅ Counter reset
- ✅ Domain whitelisting

## Best Practices

1. **Don't Change Frequency Too Often:** Users will notice
2. **Monitor User Feedback:** Adjust if users complain
3. **Test Thoroughly:** Ensure ads work on all devices
4. **Track Revenue:** Monitor which ad URL performs better
5. **Keep Whitelist Updated:** Add new ad domains as needed

## Support

If you need to modify the monetization system:

1. **Change ad URLs:** Edit `MonetizationManager.kt` → `AD_URLS`
2. **Change frequency:** Edit `MonetizationManager.kt` → `MIN_ACTIONS`, `MAX_ACTIONS`
3. **Add whitelist domains:** Edit `MonetizationManager.kt` → `WHITELISTED_DOMAINS`
4. **Track more actions:** Edit `WebViewViewModel.kt` → Add `trackUserAction()` calls

## Summary

✅ Monetization system implemented
✅ Shows ads every 7-12 actions
✅ Opens ads in new tabs
✅ Rotates between 2 ad URLs
✅ Domains permanently whitelisted
✅ Never blocked by ad blocker
✅ HTTP allowed for monetization ads (HTTPS enforced for others)
✅ Persists state across app restarts
✅ Random threshold for natural feel

Your app is now monetized and ready to generate revenue!

## Important Fix Applied

**Issue:** "Only HTTPS connections are allowed for security" error was blocking your ads.

**Solution:** Added exception for monetization domains to allow HTTP:
- Your ad URLs can now use HTTP or HTTPS
- All other URLs still require HTTPS for security
- Monetization ads will load correctly

The ads will now display properly without the HTTPS error!
