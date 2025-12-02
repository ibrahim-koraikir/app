# Testing Monetization Ads - Black Screen Fix

## Quick Test

1. **Build and Install**
   ```bash
   gradlew installDebug
   ```

2. **Start Monitoring**
   ```bash
   watch_monetization_ads.bat
   ```

3. **Trigger Ad**
   - Open the app
   - Perform 10 actions (open 10 different websites)
   - On the 10th action, an ad should appear

4. **What to Look For**
   - ✅ Ad content should be visible (not black screen)
   - ✅ Ad should be interactive/clickable
   - ✅ After closing ad, normal browsing resumes

## Detailed Testing Steps

### Step 1: Clean Install
```bash
# Uninstall old version
adb uninstall com.entertainmentbrowser

# Install new version
gradlew installDebug
```

### Step 2: Start Log Monitoring
Run one of these scripts:
- `watch_monetization_ads.bat` - Live monitoring
- `save_monetization_logs.bat` - Save to file

### Step 3: Trigger Monetization Ad

The app shows ads after every 10 actions. Actions include:
- Opening a website from home screen
- Opening a website from favorites
- Clicking a link in WebView
- Opening a new tab

**Quick way to trigger:**
1. Open app
2. Go to home screen
3. Click on 10 different websites (one after another)
4. On the 10th click, ad should appear

### Step 4: Verify Ad Display

#### ✅ Success Indicators:
- Ad page loads with visible content
- You can see the ad website (effectivegatecpm.com or similar)
- Ad is interactive (can click/scroll)
- No black screen
- After ad, you can continue browsing

#### ❌ Failure Indicators:
- Black screen appears
- White screen with no content
- App crashes
- Ad doesn't load at all

## Log Analysis

### What to Look For in Logs

#### 1. Ad Trigger (Should see this):
```
MonetizationManager: Action tracked: 10/10
MonetizationManager: Should show ad: 10 >= 10
MonetizationManager: Next ad URL: https://...
WebViewViewModel: 💰 Showing monetization ad: https://...
```

#### 2. WebView Creation (Should see this):
```
WebViewPool: Pool empty, creating new WebView
WebViewPool: Created new WebView with performance settings
CustomWebView: Initial load for tab [UUID]: https://...
```

#### 3. Ad Loading (Should see this):
```
AdBlockMetrics: 📄 Page started: https://www.effectivegatecpm.com/...
FastAdBlockEngine: ✅ Allowing monetization domain: www.effectivegatecpm.com
AdBlockMetrics: ✓ Allowed: https://www.effectivegatecpm.com/...
AdBlockMetrics: ✅ Page finished: https://www.effectivegatecpm.com/...
```

#### 4. Rendering Issues (Should NOT see these):
```
❌ GPUAUX: [AUX]GuiExtAuxCheckAuxPath:670: Null anb
❌ MALI DEBUG: BAD ALLOC from gles_texture_egl_image_get_2d_template
❌ chromium: ERR_CACHE_MISS
❌ chromium: net::ERR_FAILED
```

### Common Issues and Solutions

#### Issue 1: Black Screen Still Appears
**Logs to check:**
```
gralloc4: @set_metadata: update dataspace from GM (0x00000000 -> 0x08010000)
GPUAUX: [AUX]GuiExtAuxCheckAuxPath:670: Null anb
```

**Solution:** Hardware acceleration issue. Check if:
- Device supports hardware acceleration
- WebView is properly initialized
- Layer type is set correctly

#### Issue 2: Ad Blocked Instead of Shown
**Logs to check:**
```
AdBlockWebViewClient: Blocked by FastEngine: https://www.effectivegatecpm.com/...
```

**Solution:** Monetization domain not whitelisted. Check:
- `FastAdBlockEngine.kt` has monetization domains
- `AdBlockWebViewClient.kt` checks for monetization URLs

#### Issue 3: Ad Loads But Content Not Visible
**Logs to check:**
```
AdBlockMetrics: ✅ Page finished: https://...
chromium: [INFO:CONSOLE] JavaScript errors
```

**Solution:** JavaScript or rendering issue. Check:
- JavaScript is enabled
- DOM storage is enabled
- No console errors

## Testing Checklist

- [ ] Clean install of app
- [ ] Logs monitoring started
- [ ] Performed 10 actions
- [ ] Ad appeared (not black screen)
- [ ] Ad content is visible
- [ ] Ad is interactive
- [ ] Can close ad and continue browsing
- [ ] No crashes or errors
- [ ] Logs show successful ad loading
- [ ] No GPU/rendering errors in logs

## Expected Behavior

### Before Fix:
1. User performs 10 actions
2. Ad URL loads
3. **Black screen appears** ❌
4. User sees nothing
5. Must close tab to continue

### After Fix:
1. User performs 10 actions
2. Ad URL loads
3. **Ad content displays** ✅
4. User sees ad website
5. Can interact with ad or close it
6. Normal browsing resumes

## Automated Test Commands

### Quick Test Sequence:
```bash
# 1. Clean install
adb uninstall com.entertainmentbrowser
gradlew installDebug

# 2. Start app
adb shell am start -n com.entertainmentbrowser/.MainActivity

# 3. Monitor logs
adb logcat -c
adb logcat | findstr /i "MonetizationManager WebViewViewModel"
```

### Check Current Action Count:
```bash
adb logcat | findstr "Action tracked"
```

### Force Ad Display (for testing):
You can temporarily modify `MonetizationManager.kt` to show ads more frequently:
```kotlin
private const val ACTIONS_BEFORE_AD = 2  // Instead of 10
```

## Troubleshooting

### Problem: Can't trigger ad
**Solution:** Make sure you're performing valid actions:
- Opening websites counts
- Switching tabs does NOT count
- Scrolling does NOT count
- Only navigation actions count

### Problem: Logs not showing
**Solution:** 
```bash
# Check if device is connected
adb devices

# Check if app is running
adb shell ps | findstr entertainmentbrowser

# Clear and restart logcat
adb logcat -c
adb logcat
```

### Problem: App crashes when ad appears
**Solution:** Check crash logs:
```bash
adb logcat | findstr "AndroidRuntime:E"
```

## Success Criteria

The fix is successful if:
1. ✅ Ad appears after 10 actions
2. ✅ Ad content is visible (not black)
3. ✅ Ad is fully functional
4. ✅ No rendering errors in logs
5. ✅ User can continue browsing after ad
6. ✅ No performance degradation
7. ✅ No crashes

## Reporting Issues

If the black screen still appears, provide:
1. Full logcat output (use `save_monetization_logs.bat`)
2. Screenshot of black screen
3. Device model and Android version
4. Steps to reproduce
5. Any error messages

Save logs to file:
```bash
save_monetization_logs.bat
```

Then share the `monetization_ad_logs.txt` file.
