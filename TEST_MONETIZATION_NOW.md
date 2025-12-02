# Test Monetization System - Step by Step

## Quick Test

1. **Install the app:**
   ```
   gradlew installDebug
   ```

2. **Start watching logs:**
   ```
   test_monetization_interception.bat
   ```

3. **Test in the app:**
   - Open the app
   - Go to any website (e.g., Netflix, YouTube)
   - Click on 3-6 different videos/links **within the WebView**
   - Watch the logs

## What You Should See in Logs

### On App Start:
```
EntertainmentBrowserApp: 💰 Initializing monetization...
MonetizationManager: Initialized: loadCount=0, nextThreshold=4
EntertainmentBrowserApp: ✅ Monetization ready - will show ads every 3-6 URL loads
```

### When Clicking Links:
```
AdBlockWebViewClient: 🔗 shouldOverrideUrlLoading called for: https://example.com/video1
AdBlockWebViewClient: 📊 Monetization status: 0/4 loads
AdBlockWebViewClient: 📈 URL load tracked: 1/4

AdBlockWebViewClient: 🔗 shouldOverrideUrlLoading called for: https://example.com/video2
AdBlockWebViewClient: 📊 Monetization status: 1/4 loads
AdBlockWebViewClient: 📈 URL load tracked: 2/4

AdBlockWebViewClient: 🔗 shouldOverrideUrlLoading called for: https://example.com/video3
AdBlockWebViewClient: 📊 Monetization status: 2/4 loads
AdBlockWebViewClient: 📈 URL load tracked: 3/4

AdBlockWebViewClient: 🔗 shouldOverrideUrlLoading called for: https://example.com/video4
AdBlockWebViewClient: 📊 Monetization status: 3/4 loads
AdBlockWebViewClient: 📈 URL load tracked: 4/4
```

### When Ad Shows (4th click):
```
AdBlockWebViewClient: 🔗 shouldOverrideUrlLoading called for: https://example.com/video4
AdBlockWebViewClient: 📊 Monetization status: 4/4 loads
MonetizationManager: Should show ad: 4 >= 4
AdBlockWebViewClient: 💰💰💰 INTERCEPTING URL TO SHOW AD!
AdBlockWebViewClient:    Requested: https://example.com/video4
AdBlockWebViewClient:    Showing: https://www.effectivegatecpm.com/hypsia868?key=d55fe3c96beb154d635fe6ee82094511
MonetizationManager: Reset after ad: new threshold=5
AdBlockWebViewClient: ✅ Monetization counter reset
```

## Troubleshooting

### If No Logs Appear:
1. Make sure device is connected: `adb devices`
2. Clear logcat: `adb logcat -c`
3. Try again: `test_monetization_interception.bat`

### If Ads Don't Show:
1. Check if monetization initialized:
   ```
   adb logcat | findstr "Monetization"
   ```
   Should see: "✅ Monetization ready"

2. Check if URL loading is tracked:
   ```
   adb logcat | findstr "shouldOverrideUrlLoading"
   ```
   Should see logs for each link click

3. Make sure you're clicking **links within the WebView**, not:
   - Opening new tabs from home screen
   - Switching between existing tabs
   - Using back/forward buttons

### If shouldOverrideUrlLoading Not Called:
This means links aren't triggering navigation. Try:
- Clicking actual links on websites (not just scrolling)
- Clicking video thumbnails
- Clicking "Watch Now" buttons
- Navigating to different pages

## Manual Reset (for testing)

If you want to reset the counter manually:

1. Add this to your debug menu or run via adb:
   ```kotlin
   monetizationManager.manualReset()
   ```

2. Or clear app data:
   ```
   adb shell pm clear com.entertainmentbrowser
   ```

## Expected Behavior

✅ **Correct:**
- User clicks link → Counter increments
- After 3-6 clicks → Ad shows instead of requested page
- User sees ad page
- User can browse ad or go back

❌ **Not Happening:**
- Ad doesn't auto-navigate to intended page (this is by design now)
- User must manually navigate after viewing ad

## Configuration

Current settings in `MonetizationManager.kt`:
- **MIN_LOADS = 3** (minimum clicks before ad)
- **MAX_LOADS = 6** (maximum clicks before ad)
- **AD_URL** = Your Adsterra smartlink

To change frequency, edit these values and rebuild.
