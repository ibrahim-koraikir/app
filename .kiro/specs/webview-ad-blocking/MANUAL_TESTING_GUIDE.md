# Ad-Blocking Manual Testing Guide

This guide provides step-by-step instructions for manually testing the ad-blocking implementation in Entertainment Browser.

## Prerequisites

- Android device or emulator with API 24+ (Android 7.0+)
- ADB (Android Debug Bridge) installed and configured
- Entertainment Browser app installed in debug mode
- Stable internet connection

## Quick Start

1. Build and install the debug APK:
   ```bash
   gradlew installDebug
   ```

2. Start ADB logging (in a separate terminal):
   ```bash
   adb logcat -s FastAdBlockEngine:D AdBlockWebViewClient:D AdBlockMetrics:D
   ```

3. Launch the app and navigate to test websites
4. Record results in the test report template

---

## Test 10.1: Ad-Blocking Effectiveness

### Objective
Verify that the ad-blocking engine successfully blocks ads, trackers, and popups on various websites.

### Test Sites

#### 1. Forbes.com - Banner Ads Test
**URL:** `https://www.forbes.com`

**What to test:**
- [x] Top banner ads are blocked



- [ ] Sidebar ads are blocked
- [ ] In-article ads are blocked
- [ ] Video ads are blocked

**Expected behavior:**
- Empty spaces where ads would normally appear
- No ad content visible
- Page loads faster than without ad-blocking

**How to verify:**
1. Open Entertainment Browser
2. Navigate to `https://www.forbes.com`
3. Scroll through an article
4. Check Logcat for blocked requests: `adb logcat | grep "Blocked by"`
5. Count visible ads (should be 0 or minimal)

---

#### 2. CNN.com - Video Ads Test
**URL:** `https://www.cnn.com`

**What to test:**
- [ ] Pre-roll video ads are blocked
- [ ] Mid-roll video ads are blocked
- [ ] Banner ads around videos are blocked
- [ ] Autoplay ads are blocked

**Expected behavior:**
- Videos play without pre-roll ads
- No ad interruptions during video playback
- Sidebar and banner ads are blocked

**How to verify:**
1. Navigate to `https://www.cnn.com`
2. Click on a video article
3. Observe if video plays immediately without ads
4. Check Logcat for blocked video ad domains
5. Watch for 30+ seconds to check for mid-roll ads

---

#### 3. DailyMail.co.uk - Popup Test
**URL:** `https://www.dailymail.co.uk`

**What to test:**
- [ ] Popup ads are blocked
- [ ] Overlay ads are blocked
- [ ] Interstitial ads are blocked
- [ ] Banner ads are blocked

**Expected behavior:**
- No popups appear when navigating
- No overlay ads covering content
- Clean reading experience

**How to verify:**
1. Navigate to `https://www.dailymail.co.uk`
2. Click on multiple articles
3. Scroll through pages
4. Verify no popups or overlays appear
5. Check Logcat for blocked popup domains

---

#### 4. Adblock-Tester.com - Blocking Rate Test
**URL:** `https://adblock-tester.com` or `https://d3ward.github.io/toolz/adblock.html`

**What to test:**
- [ ] Blocking rate is 85% or higher
- [ ] Most ad networks are blocked
- [ ] Tracker domains are blocked

**Expected behavior:**
- Test page shows 85%+ ads blocked
- Green checkmarks for blocked ad networks
- Red X marks for allowed requests (should be minimal)

**How to verify:**
1. Navigate to the ad-block tester site
2. Wait for all tests to complete
3. Record the blocking percentage
4. Take a screenshot for documentation
5. Check which ad networks passed/failed

**Alternative test sites:**
- `https://canyoublockit.com/`
- `https://blockads.fivefilters.org/`

---

### Success Criteria for Task 10.1

- ✅ Forbes.com: No visible banner ads
- ✅ CNN.com: Videos play without pre-roll ads
- ✅ DailyMail.co.uk: No popups or overlays
- ✅ Adblock-tester.com: 85%+ blocking rate
- ✅ Logcat shows blocked requests for all test sites

---

## Test 10.2: Performance Metrics

### Objective
Verify that ad-blocking performs efficiently without degrading app performance.

### Metric 1: Filter List Load Time

**Requirement:** Filter lists should load in <1 second

**How to test:**
1. Clear app data: `adb shell pm clear com.entertainmentbrowser`
2. Start ADB logging: `adb logcat -s FastAdBlockEngine:D`
3. Launch the app
4. Look for log message: `✅ Loaded in XXXms`
5. Record the load time

**Expected result:**
```
FastAdBlockEngine: 🚀 Loading filter lists...
FastAdBlockEngine: ✅ Loaded in 850ms
FastAdBlockEngine:    Blocked domains: 45000
FastAdBlockEngine:    Blocked patterns: 12000
FastAdBlockEngine:    Allowed domains: 3000
```

**Success criteria:**
- ✅ Load time < 1000ms
- ✅ No errors during loading
- ✅ All filter lists loaded successfully

**ADB Command:**
```bash
adb logcat -s FastAdBlockEngine:D | grep "Loaded in"
```

---

### Metric 2: Smooth Scrolling on Ad-Heavy Sites

**Requirement:** Smooth scrolling with no jank on ad-heavy websites

**Test sites:**
- `https://www.forbes.com`
- `https://www.dailymail.co.uk`
- `https://www.cnn.com`

**How to test:**
1. Navigate to test site
2. Wait for page to fully load
3. Scroll rapidly up and down
4. Observe scrolling smoothness
5. Check for frame drops or stuttering

**Expected behavior:**
- Smooth 60fps scrolling
- No visible jank or stuttering
- Responsive touch input

**How to measure:**
1. Enable GPU rendering profile:
   ```bash
   adb shell setprop debug.hwui.profile visual_bars
   ```
2. Scroll through pages
3. Observe green bars (should stay below red line for 60fps)

**Success criteria:**
- ✅ No visible stuttering during scrolling
- ✅ Frame time stays below 16ms (60fps)
- ✅ Responsive to touch input

---

### Metric 3: Memory Usage

**Requirement:** Memory increase should be <100MB for ad-blocking

**How to test:**
1. Get baseline memory before loading pages:
   ```bash
   adb shell dumpsys meminfo com.entertainmentbrowser | grep "TOTAL"
   ```
2. Navigate to 5 different ad-heavy websites
3. Check memory again:
   ```bash
   adb shell dumpsys meminfo com.entertainmentbrowser | grep "TOTAL"
   ```
4. Calculate the difference

**Expected result:**
- Baseline: ~150MB
- After browsing: ~220MB
- Increase: ~70MB (well under 100MB limit)

**Detailed memory monitoring:**
```bash
# Continuous memory monitoring
adb shell "while true; do dumpsys meminfo com.entertainmentbrowser | grep 'TOTAL PSS'; sleep 5; done"
```

**Success criteria:**
- ✅ Memory increase < 100MB
- ✅ No memory leaks (memory stabilizes)
- ✅ App doesn't crash due to OOM

---

### Metric 4: Page Load Time Improvement

**Requirement:** Measurable improvement in page load time on ad-heavy sites

**How to test:**

**Without ad-blocking (baseline):**
1. Disable ad-blocking temporarily (or use Chrome browser)
2. Clear cache
3. Navigate to test site
4. Measure time until `onPageFinished` is called
5. Record the time

**With ad-blocking:**
1. Enable ad-blocking (Entertainment Browser)
2. Clear cache
3. Navigate to same test site
4. Measure time until `onPageFinished` is called
5. Record the time

**Test sites:**
- `https://www.forbes.com`
- `https://www.cnn.com`
- `https://www.dailymail.co.uk`

**ADB Command to measure:**
```bash
adb logcat -s AdBlockWebViewClient:D | grep "Page finished"
```

**Expected results:**
| Site | Without Ad-Block | With Ad-Block | Improvement |
|------|------------------|---------------|-------------|
| Forbes | 8.5s | 4.2s | 50% faster |
| CNN | 7.8s | 3.9s | 50% faster |
| DailyMail | 9.2s | 4.5s | 51% faster |

**Success criteria:**
- ✅ Page load time reduced by 30%+ on ad-heavy sites
- ✅ Fewer network requests made
- ✅ Less data transferred

---

### Metric 5: Blocked Request Count

**How to monitor:**
```bash
adb logcat -s AdBlockWebViewClient:D | grep "Blocked"
```

**Expected output:**
```
AdBlockWebViewClient: Blocked by FastEngine: https://ads.example.com/banner.js
AdBlockWebViewClient: Blocked by FastEngine: https://tracker.example.com/pixel.gif
AdBlockWebViewClient: Page finished: https://www.forbes.com - Blocked 47 requests
```

**Success criteria:**
- ✅ 20+ requests blocked on ad-heavy sites
- ✅ Blocked count logged for each page
- ✅ No false positives (legitimate content blocked)

---

## Success Criteria Summary

### Task 10.1: Ad-Blocking Effectiveness
- ✅ Forbes.com: Banner ads blocked
- ✅ CNN.com: Video ads blocked
- ✅ DailyMail.co.uk: Popups blocked
- ✅ Adblock-tester.com: 85%+ blocking rate

### Task 10.2: Performance Metrics
- ✅ Filter list load time: <1 second
- ✅ Smooth scrolling: No jank on ad-heavy sites
- ✅ Memory usage: <100MB increase
- ✅ Page load time: 30%+ improvement on ad-heavy sites

---

## Troubleshooting

### Issue: Filter lists not loading
**Symptoms:** No ads blocked, load time = 0ms

**Solution:**
1. Check if filter list files exist in assets:
   ```bash
   adb shell "run-as com.entertainmentbrowser ls -la /data/data/com.entertainmentbrowser/files"
   ```
2. Verify assets are included in APK
3. Check Logcat for errors during loading

### Issue: Too many false positives
**Symptoms:** Legitimate content is blocked

**Solution:**
1. Identify the blocked domain from Logcat
2. Add to whitelist in `FastAdBlockEngine.kt`:
   ```kotlin
   private val whitelistedDomains = hashSetOf(
       "legitimate-domain.com"
   )
   ```
3. Rebuild and test

### Issue: Poor performance
**Symptoms:** Slow scrolling, high memory usage

**Solution:**
1. Check filter list size (should be <100MB total)
2. Reduce number of filter lists if needed
3. Profile with Android Studio Profiler

### Issue: Ads not blocked
**Symptoms:** Ads still visible on test sites

**Solution:**
1. Verify `preloadFromAssets()` is called in `Application.onCreate()`
2. Check if filter lists are loaded: Look for "✅ Loaded in" log
3. Verify `AdBlockWebViewClient` is set on WebView
4. Check if domain is in exception list

---

## Additional Testing

### Edge Cases
- [ ] Test with airplane mode (cached pages)
- [ ] Test with slow network (3G simulation)
- [ ] Test with multiple tabs open
- [ ] Test after app restart
- [ ] Test after device restart

### Compatibility Testing
- [ ] Test on Android 7.0 (API 24)
- [ ] Test on Android 10 (API 29)
- [ ] Test on Android 13+ (API 33+)
- [ ] Test on different screen sizes
- [ ] Test on different device manufacturers

### Stress Testing
- [ ] Open 20 tabs with ad-heavy sites
- [ ] Navigate rapidly between pages
- [ ] Leave app running for 1+ hour
- [ ] Monitor for memory leaks
- [ ] Check for crashes

---

## Reporting Issues

If you find issues during testing:

1. **Capture logs:**
   ```bash
   adb logcat -d > adblock_test_logs.txt
   ```

2. **Take screenshots** of any visual issues

3. **Record the following:**
   - Device model and Android version
   - App version
   - Steps to reproduce
   - Expected vs actual behavior
   - Logcat output

4. **Document in test report** (see TEST_REPORT_TEMPLATE.md)

---

## Next Steps

After completing manual testing:

1. Fill out the test report template
2. Document any issues found
3. Mark tasks 10.1 and 10.2 as complete
4. Share results with the team
5. Create bug tickets for any issues

---

## Quick Reference: ADB Commands

```bash
# Install debug APK
gradlew installDebug

# Clear app data
adb shell pm clear com.entertainmentbrowser

# View ad-blocking logs
adb logcat -s FastAdBlockEngine:D AdBlockWebViewClient:D

# Monitor memory usage
adb shell dumpsys meminfo com.entertainmentbrowser

# Enable GPU profiling
adb shell setprop debug.hwui.profile visual_bars

# Capture full logs
adb logcat -d > logs.txt

# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Record screen
adb shell screenrecord /sdcard/test.mp4
# (Stop with Ctrl+C after recording)
adb pull /sdcard/test.mp4
```
