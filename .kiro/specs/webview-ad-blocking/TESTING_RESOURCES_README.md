# Ad-Blocking Testing Resources

This directory contains comprehensive resources for manually testing the ad-blocking implementation in Entertainment Browser.

## 📁 Files Overview

### 1. MANUAL_TESTING_GUIDE.md
**Comprehensive testing guide with detailed instructions**

- Step-by-step testing procedures for all test cases
- Expected results and success criteria
- Troubleshooting tips
- ADB command reference
- Edge case and compatibility testing

**Use this when:** You need detailed instructions for each test case

---

### 2. TEST_REPORT_TEMPLATE.md
**Structured template for documenting test results**

- Pre-formatted sections for all test cases
- Checkboxes for pass/fail status
- Tables for metrics and results
- Space for screenshots and logs
- Issue tracking sections

**Use this when:** You're ready to document your test results

---

### 3. QUICK_REFERENCE.md
**One-page quick reference card**

- Essential ADB commands
- Test checklist
- Expected results
- Quick troubleshooting
- One-liner commands

**Use this when:** You need quick command reference during testing

---

### 4. adb_commands.sh (Linux/Mac)
**Interactive bash script for testing**

Features:
- Menu-driven interface
- Install APK
- Monitor logs in real-time
- Check performance metrics
- Measure filter load time
- Capture screenshots
- Generate test report data

**Usage:**
```bash
chmod +x adb_commands.sh
./adb_commands.sh
```

---

### 5. adb_commands.bat (Windows)
**Interactive batch script for testing**

Same features as bash script, optimized for Windows:
- Menu-driven interface
- All testing commands
- Performance monitoring
- Report generation

**Usage:**
```cmd
adb_commands.bat
```

---

## 🚀 Quick Start

### For First-Time Testing

1. **Read the guide:**
   ```
   Open: MANUAL_TESTING_GUIDE.md
   ```

2. **Use the script:**
   ```bash
   # Linux/Mac
   ./adb_commands.sh
   
   # Windows
   adb_commands.bat
   ```

3. **Follow the checklist:**
   ```
   Open: QUICK_REFERENCE.md
   ```

4. **Document results:**
   ```
   Fill out: TEST_REPORT_TEMPLATE.md
   ```

### For Quick Testing

1. **Use quick reference:**
   ```
   Open: QUICK_REFERENCE.md
   ```

2. **Run one-liner commands:**
   ```bash
   # See QUICK_REFERENCE.md for commands
   ```

---

## 📊 Testing Workflow

```
┌─────────────────────────────────────┐
│  1. Install & Setup                 │
│     - Install debug APK             │
│     - Clear app data                │
│     - Start ADB logging             │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  2. Test Ad-Blocking (Task 10.1)    │
│     - Forbes.com (banners)          │
│     - CNN.com (video ads)           │
│     - DailyMail.co.uk (popups)      │
│     - Adblock-tester.com (rate)     │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  3. Test Performance (Task 10.2)    │
│     - Filter load time              │
│     - Smooth scrolling              │
│     - Memory usage                  │
│     - Page load improvement         │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  4. Document Results                │
│     - Fill test report              │
│     - Attach screenshots            │
│     - Save logs                     │
│     - Note issues                   │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  5. Review & Sign-off               │
│     - Verify all tests passed       │
│     - Review issues                 │
│     - Get approval                  │
└─────────────────────────────────────┘
```

---

## 🎯 Test Coverage

### Task 10.1: Ad-Blocking Effectiveness
| Test | Site | What's Tested | Requirement |
|------|------|---------------|-------------|
| 10.1.1 | Forbes.com | Banner ads | 1.4 |
| 10.1.2 | CNN.com | Video ads | 1.4 |
| 10.1.3 | DailyMail.co.uk | Popups | 1.4 |
| 10.1.4 | Adblock-tester.com | Blocking rate | 1.4 |

### Task 10.2: Performance Metrics
| Test | Metric | Target | Requirement |
|------|--------|--------|-------------|
| 10.2.1 | Filter load time | <1000ms | 1.1, 2.4 |
| 10.2.2 | Smooth scrolling | No jank | 1.5 |
| 10.2.3 | Memory usage | <100MB | 2.5 |
| 10.2.4 | Page load time | 30%+ faster | 1.1 |

---

## 🛠️ Code Enhancements

### New File: AdBlockMetrics.kt
**Purpose:** Track and log performance metrics

**Features:**
- Session-level metrics tracking
- Per-page metrics tracking
- Blocked/allowed request counting
- Load time measurement
- Blocking rate calculation
- Session summary logging

**Usage:**
```kotlin
// Metrics are automatically tracked by AdBlockWebViewClient
// View logs with:
adb logcat -s AdBlockMetrics:D
```

**Log Output:**
```
AdBlockMetrics: 📄 Page started: https://www.forbes.com
AdBlockMetrics: ✅ Page finished: https://www.forbes.com
AdBlockMetrics:    Load time: 4200ms
AdBlockMetrics:    Blocked: 47 requests
AdBlockMetrics:    Allowed: 23 requests
AdBlockMetrics:    Blocking rate: 67%
```

### Updated: AdBlockWebViewClient.kt
**Changes:**
- Integrated AdBlockMetrics tracking
- Enhanced logging for blocked requests
- Page start/finish metrics
- Request-level tracking

**Benefits:**
- Better visibility into ad-blocking performance
- Easier debugging and testing
- Detailed metrics for test reports

---

## 📋 Prerequisites

### Required Tools
- Android SDK with ADB
- Android device or emulator (API 24+)
- Gradle (for building APK)

### Optional Tools
- Android Studio (for profiling)
- Screen recording software
- Image editing software (for screenshots)

### Device Setup
1. Enable USB debugging
2. Connect device via USB or WiFi
3. Verify connection: `adb devices`

---

## 🎓 Testing Tips

### Best Practices
1. **Always test on real device** for accurate performance
2. **Clear app data** before testing filter load time
3. **Use separate terminal** for continuous logging
4. **Take screenshots** of all test results
5. **Save logs** before clearing or restarting
6. **Test multiple times** to get average results
7. **Compare with baseline** (without ad-blocking)

### Common Mistakes
- ❌ Not clearing app data before cold start tests
- ❌ Testing on emulator only (performance differs)
- ❌ Not saving logs before clearing
- ❌ Testing only once (results may vary)
- ❌ Not documenting issues immediately

### Time Estimates
- **Task 10.1** (Ad-blocking effectiveness): 30-45 minutes
- **Task 10.2** (Performance metrics): 30-45 minutes
- **Documentation**: 15-30 minutes
- **Total**: 1.5-2 hours

---

## 🐛 Troubleshooting

### Device Not Connected
```bash
# Check devices
adb devices

# Restart ADB server
adb kill-server
adb start-server
```

### App Not Installing
```bash
# Uninstall old version
adb uninstall com.entertainmentbrowser

# Reinstall
gradlew installDebug
```

### No Logs Appearing
```bash
# Clear logcat buffer
adb logcat -c

# Verify log tags
adb logcat | grep -E "FastAdBlockEngine|AdBlockWebViewClient|AdBlockMetrics"
```

### Filter Lists Not Loading
```bash
# Check for errors
adb logcat | grep -E "ERROR|FATAL"

# Verify assets exist
# Check app/src/main/assets/adblock/
```

---

## 📚 Additional Resources

### Documentation
- [Requirements](requirements.md) - Feature requirements
- [Design](design.md) - Technical design
- [Tasks](tasks.md) - Implementation tasks

### External Links
- [EasyList](https://easylist.to/) - Filter list source
- [Adblock Tester](https://d3ward.github.io/toolz/adblock.html) - Online testing tool
- [ADB Documentation](https://developer.android.com/studio/command-line/adb) - Official ADB docs

---

## ✅ Success Criteria

All tests must pass to complete Task 10:

### Task 10.1: Ad-Blocking Effectiveness
- ✅ Forbes.com: No visible banner ads
- ✅ CNN.com: Videos play without pre-roll ads
- ✅ DailyMail.co.uk: No popups or overlays
- ✅ Adblock-tester.com: 85%+ blocking rate

### Task 10.2: Performance Metrics
- ✅ Filter list load time: <1 second
- ✅ Smooth scrolling: No jank on ad-heavy sites
- ✅ Memory usage: <100MB increase
- ✅ Page load time: 30%+ improvement

---

## 📞 Support

If you encounter issues:

1. Check the troubleshooting section in MANUAL_TESTING_GUIDE.md
2. Review logs for error messages
3. Verify device setup and prerequisites
4. Try on a different device/emulator
5. Document the issue in TEST_REPORT_TEMPLATE.md

---

## 🎉 Next Steps

After completing manual testing:

1. ✅ Fill out TEST_REPORT_TEMPLATE.md
2. ✅ Attach screenshots and logs
3. ✅ Mark tasks 10.1 and 10.2 as complete
4. ✅ Share results with team
5. ✅ Create bug tickets for any issues found
6. ✅ Update documentation if needed

---

*Last updated: $(date)*
*For questions or issues, refer to MANUAL_TESTING_GUIDE.md*
