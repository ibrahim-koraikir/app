# Monetization Interception System

## Overview
Your Entertainment Browser app now includes a smart monetization system that intercepts URL navigation every 3-6 loads and shows your Adsterra smartlink ad in the same tab before allowing the user to continue to their intended destination.

## How It Works

### User Experience Flow
1. User clicks on a video or link (e.g., wants to watch a video)
2. **Every 3-6 clicks**, instead of going directly to the video:
   - The app loads your ad URL first
   - User sees the ad page
   - After the ad page loads, the app automatically navigates to the intended video
3. User continues browsing normally

### Technical Implementation

#### 1. MonetizationManager (`MonetizationManager.kt`)
- Tracks URL load count (3-6 threshold, randomized)
- Stores pending URL when ad is shown
- Provides whitelist check for ad-blocker
- Persists state using DataStore

**Key Methods:**
- `shouldShowAd()` - Check if it's time to show ad
- `setPendingUrl(url)` - Save user's intended destination
- `getPendingUrl()` - Retrieve and clear pending URL
- `isMonetizationDomain(url)` - Whitelist check for ad-blocker

#### 2. WebViewViewModel Integration
- Intercepts `openNewTab()` calls
- Checks if ad should be shown
- Saves intended URL and loads ad instead
- After ad loads, automatically navigates to intended URL

**Flow:**
```kotlin
User clicks video → shouldShowAd() → Yes → 
  setPendingUrl(videoUrl) → 
  Load ad URL → 
  Ad finishes loading → 
  checkPendingUrlAfterAd() → 
  Navigate to videoUrl
```

#### 3. Ad-Blocker Whitelist
Your ad domain is whitelisted in:
- `FastAdBlockEngine.kt` - Primary ad-blocking engine
- `HardcodedFilters.kt` - Fallback ad-blocking system

This ensures your monetization ads are NEVER blocked.

## Configuration

### Your Ad URL
```kotlin
private const val AD_URL = "https://www.effectivegatecpm.com/hypsia868?key=d55fe3c96beb154d635fe6ee82094511"
```

### Whitelisted Domain
```kotlin
const val MONETIZATION_DOMAIN = "effectivegatecpm.com"
```

### Frequency Settings
```kotlin
private const val MIN_LOADS = 3  // Minimum URL loads before showing ad
private const val MAX_LOADS = 6  // Maximum URL loads before showing ad
```

## Customization

### Change Ad Frequency
Edit `MonetizationManager.kt`:
```kotlin
private const val MIN_LOADS = 5  // Show ad every 5-8 loads instead
private const val MAX_LOADS = 8
```

### Add More Ad URLs (Rotation)
Currently uses single ad URL. To add rotation:
```kotlin
private val AD_URLS = listOf(
    "https://www.effectivegatecpm.com/hypsia868?key=d55fe3c96beb154d635fe6ee82094511",
    "https://your-second-ad-url.com",
    "https://your-third-ad-url.com"
)

fun getAdUrl(): String {
    return AD_URLS.random() // Or implement rotation logic
}
```

### Whitelist Additional Domains
If you add more ad networks, update:

**MonetizationManager.kt:**
```kotlin
const val MONETIZATION_DOMAIN = "effectivegatecpm.com"
const val MONETIZATION_DOMAIN_2 = "another-ad-network.com"

fun isMonetizationDomain(url: String): Boolean {
    val lowerUrl = url.lowercase()
    return lowerUrl.contains(MONETIZATION_DOMAIN) || 
           lowerUrl.contains(MONETIZATION_DOMAIN_2)
}
```

## Testing

### Test Ad Display
1. Build and install app: `gradlew installDebug`
2. Open app and browse to any website
3. Click on 3-6 different links/videos
4. On the 3rd-6th click, you should see your ad first
5. After ad loads, app should automatically navigate to intended destination

### Check Logs
```bash
adb logcat | findstr "MonetizationManager"
```

Look for:
- `URL load tracked: X/Y` - Shows progress toward ad
- `Should show ad: X >= Y` - Ad will be shown
- `Intercepting navigation to show ad` - Ad is being displayed
- `Ad viewed, continuing to pending URL` - Navigating to intended destination

### Manual Reset (for testing)
Add this to your debug menu:
```kotlin
monetizationManager.manualReset()
```

## Files Modified

### New Files
- `app/src/main/java/com/entertainmentbrowser/util/MonetizationManager.kt`

### Modified Files
- `app/src/main/java/com/entertainmentbrowser/presentation/webview/WebViewViewModel.kt`
- `app/src/main/java/com/entertainmentbrowser/presentation/webview/WebViewScreen.kt`
- `app/src/main/java/com/entertainmentbrowser/util/adblock/FastAdBlockEngine.kt`
- `app/src/main/java/com/entertainmentbrowser/util/adblock/HardcodedFilters.kt`
- `app/src/main/java/com/entertainmentbrowser/domain/repository/TabRepository.kt`
- `app/src/main/java/com/entertainmentbrowser/data/repository/TabRepositoryImpl.kt`
- `app/src/main/java/com/entertainmentbrowser/data/local/dao/TabDao.kt`

## Advantages of This Approach

✅ **Better User Experience**
- Ads load in same tab (no confusing new tabs)
- User automatically continues to intended destination
- Feels more natural and less intrusive

✅ **Higher Ad Views**
- User must see ad to reach content
- No easy way to close/ignore ad
- More engagement with ad content

✅ **Reliable Tracking**
- Counts actual URL navigations
- Randomized frequency prevents predictability
- Persists across app restarts

✅ **Ad-Blocker Proof**
- Your ad domain is whitelisted
- Ads will NEVER be blocked
- Guaranteed monetization

## Build Status

✅ **Build Successful** - All code compiles without errors

Your monetization system is ready to use!
