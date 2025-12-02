# Ad-Blocking Testing - Quick Reference Card

## 🚀 Quick Start (5 minutes)

```bash
# 1. Install app
gradlew installDebug

# 2. Start logging (separate terminal)
adb logcat -s FastAdBlockEngine:D AdBlockWebViewClient:D AdBlockMetrics:D

# 3. Launch app and test
# 4. Check logs for results
```

---

## 📊 Essential ADB Commands

### Installation & Setup
```bash
# Install debug APK
gradlew installDebug

# Clear app data (cold start)
adb shell pm clear com.entertainmentbrowser
```

### Logging
```bash
# Real-time ad-blocking logs
adb logcat -s FastAdBlockEngine:D AdBlockWebViewClient:D AdBlockMetrics:D

# Save logs to file
adb logcat -d > adblock_logs.txt

# Filter for specific events
adb logcat | grep "Blocked by"
adb logcat | grep "Page finished"
adb logcat | grep "Loaded in"
```

### Performance Monitoring
```bash
# Check memory usage
adb shell dumpsys meminfo com.entertainmentbrowser | grep "TOTAL PSS"

# Continuous memory monitoring (every 5 seconds)
adb shell "while true; do dumpsys meminfo com.entertainmentbrowser | grep 'TOTAL PSS'; sleep 5; done"

# Enable GPU profiling (visual frame time bars)
adb shell setprop debug.hwui.profile visual_bars

# Disable GPU profiling
adb shell setprop debug.hwui.profile false
```

### Screenshots & Recording
```bash
# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Record screen (Ctrl+C to stop)
adb shell screenrecord /sdcard/recording.mp4
adb pull /sdcard/recording.mp4
```

---

## ✅ Test Checklist

### Task 10.1: Ad-Blocking Effectiveness
- [ ] **Forbes.com** - Banner ads blocked
- [ ] **CNN.com** - Video ads blocked  
- [ ] **DailyMail.co.uk** - Popups blocked
- [ ] **Adblock-tester.com** - 85%+ blocking rate

### Task 10.2: Performance Metrics
- [ ] **Filter load time** - <1 second
- [ ] **Smooth scrolling** - No jank on ad-heavy sites
- [ ] **Memory usage** - <100MB increase
- [ ] **Page load time** - 30%+ improvement

---

## 🎯 Test Sites

| Site | URL | What to Test |
|------|-----|--------------|
| Forbes | `https://www.forbes.com` | Banner ads |
| CNN | `https://www.cnn.com` | Video ads |
| DailyMail | `https://www.dailymail.co.uk` | Popups |
| Ad Tester | `https://d3ward.github.io/toolz/adblock.html` | Blocking rate |

---

## 📈 Expected Results

### Filter Load Time
```
FastAdBlockEngine: 🚀 Loading filter lists...
FastAdBlockEngine: ✅ Loaded in 850ms
FastAdBlockEngine:    Blocked domains: 45000
FastAdBlockEngine:    Blocked patterns: 12000
FastAdBlockEngine:    Allowed domains: 3000
```
✅ **Target:** <1000ms

### Blocked Requests
```
AdBlockWebViewClient: Blocked by FastEngine: https://ads.example.com/banner.js
AdBlockWebViewClient: Page finished: https://www.forbes.com - Blocked 47 requests
```
✅ **Target:** 20+ requests blocked on ad-heavy sites

### Page Metrics
```
AdBlockMetrics: ✅ Page finished: https://www.forbes.com
AdBlockMetrics:    Load time: 4200ms
AdBlockMetrics:    Blocked: 47 requests
AdBlockMetrics:    Allowed: 23 requests
AdBlockMetrics:    Blocking rate: 67%
```
✅ **Target:** 50%+ blocking rate

### Memory Usage
```
TOTAL PSS:   220,000 KB (baseline: 150,000 KB)
```
✅ **Target:** <100MB increase (100,000 KB)

---

## 🔍 What to Look For

### ✅ Good Signs
- Filter lists load in <1 second
- 20+ requests blocked per ad-heavy page
- No visible ads on test sites
- Smooth 60fps scrolling
- Memory increase <100MB
- Page load time reduced by 30%+

### ❌ Bad Signs
- Filter load time >1 second
- Few or no requests blocked
- Ads still visible
- Stuttering or jank during scrolling
- Memory increase >100MB
- No improvement in page load time
- App crashes or errors

---

## 🐛 Troubleshooting

### No ads blocked
```bash
# Check if filter lists loaded
adb logcat -d | grep "Loaded in"

# Check if AdBlockWebViewClient is active
adb logcat -d | grep "AdBlockWebViewClient"
```

### Poor performance
```bash
# Check memory usage
adb shell dumpsys meminfo com.entertainmentbrowser

# Check for errors
adb logcat | grep -E "ERROR|FATAL"
```

### False positives (legitimate content blocked)
```bash
# Find blocked URLs
adb logcat -d | grep "Blocked by" > blocked_urls.txt

# Review and add to whitelist in FastAdBlockEngine.kt
```

---

## 📝 Quick Test Script

### Windows (PowerShell)
```powershell
# Run from project root
.\.kiro\specs\webview-ad-blocking\adb_commands.bat
```

### Linux/Mac (Bash)
```bash
# Run from project root
chmod +x .kiro/specs/webview-ad-blocking/adb_commands.sh
./.kiro/specs/webview-ad-blocking/adb_commands.sh
```

---

## 📊 Metrics Summary Template

```
=== Ad-Blocking Test Results ===

Filter Load Time: _____ ms (target: <1000ms)
Memory Increase: _____ MB (target: <100MB)

Forbes.com:
  - Blocked: _____ requests
  - Ads visible: Yes / No
  
CNN.com:
  - Blocked: _____ requests
  - Video ads: Yes / No
  
DailyMail.co.uk:
  - Blocked: _____ requests
  - Popups: Yes / No
  
Adblock-Tester:
  - Blocking rate: _____% (target: >85%)

Overall: PASS / FAIL
```

---

## 🎓 Tips

1. **Always clear app data** before testing filter load time
2. **Use separate terminal** for continuous logging
3. **Test on real device** for accurate performance metrics
4. **Take screenshots** of test results for documentation
5. **Save logs** before clearing or restarting
6. **Test multiple times** to get average results
7. **Compare with/without** ad-blocking for baseline

---

## 📚 Full Documentation

- **Detailed Guide:** `MANUAL_TESTING_GUIDE.md`
- **Test Report:** `TEST_REPORT_TEMPLATE.md`
- **ADB Scripts:** `adb_commands.sh` / `adb_commands.bat`

---

## ⚡ One-Liner Tests

```bash
# Measure filter load time
adb shell pm clear com.entertainmentbrowser && adb logcat -c && echo "Launch app now..." && adb logcat -s FastAdBlockEngine:D | grep -m 1 "Loaded in"

# Check blocking rate on current page
adb logcat -d | grep "Page finished" | tail -1

# Get memory usage
adb shell dumpsys meminfo com.entertainmentbrowser | grep "TOTAL PSS"

# Count blocked requests in session
adb logcat -d | grep "Blocked by" | wc -l

# Generate quick report
adb logcat -d -s AdBlockMetrics:I
```

---

## 🎯 Success Criteria Summary

| Metric | Target | Status |
|--------|--------|--------|
| Filter load time | <1000ms | ⬜ |
| Forbes ads blocked | Yes | ⬜ |
| CNN video ads blocked | Yes | ⬜ |
| DailyMail popups blocked | Yes | ⬜ |
| Blocking rate | >85% | ⬜ |
| Smooth scrolling | Yes | ⬜ |
| Memory increase | <100MB | ⬜ |
| Load time improvement | >30% | ⬜ |

**Overall:** ⬜ PASS | ⬜ FAIL

---

*For detailed instructions, see MANUAL_TESTING_GUIDE.md*
