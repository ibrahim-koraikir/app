# How to Check Logcat for Ad Blocking Messages

## Quick Start (Choose One Method)

### 🚀 Method 1: Use the Batch Scripts (Easiest)

I've created 3 batch files for you:

#### **watch_adblock_logs.bat** - Live monitoring
```cmd
watch_adblock_logs.bat
```
Shows ad blocking messages in real-time as you browse.

#### **watch_adblock_detailed.bat** - Detailed view
```cmd
watch_adblock_detailed.bat
```
Shows more detailed information including metrics.

#### **save_adblock_logs.bat** - Save to file
```cmd
save_adblock_logs.bat
```
Saves all ad blocking logs to `logs/adblock_log_TIMESTAMP.txt` for later analysis.

**Steps:**
1. Connect your Android device via USB
2. Enable USB debugging on your device
3. Double-click any of the batch files above
4. Open your app and browse websites
5. Watch the logs appear in real-time!

---

### 📱 Method 2: Android Studio Logcat (Visual)

**Steps:**
1. Open Android Studio
2. Connect your device (USB or wireless)
3. Click **Run** button or press `Shift+F10`
4. Click **Logcat** tab at the bottom
5. In the filter box, type: `AdBlock|FastAdBlockEngine`
6. Browse websites in your app
7. Watch the logs appear!

**Logcat Filter Options:**
- `AdBlock` - Shows all ad blocking messages
- `FastAdBlockEngine` - Shows engine-specific messages
- `tag:AdBlockWebViewClient` - Shows only WebView client messages
- `package:com.entertainmentbrowser` - Shows all app logs

---

### 💻 Method 3: Manual ADB Commands

#### Basic Command (Real-time)
```cmd
adb logcat | findstr /i "AdBlock FastAdBlockEngine"
```

#### Clear logs first, then watch
```cmd
adb logcat -c
adb logcat -v time | findstr /i "AdBlock"
```

#### Save to file
```cmd
adb logcat -v time | findstr /i "AdBlock" > adblock_logs.txt
```

#### Filter by priority (Debug and above)
```cmd
adb logcat *:D | findstr /i "AdBlock"
```

---

## What You'll See

### When App Starts
```
FastAdBlockEngine: 🚀 Loading filter lists...
FastAdBlockEngine: ✅ Loaded in 523ms
FastAdBlockEngine:    Blocked domains: 3,847
FastAdBlockEngine:    Blocked patterns: 1,203
FastAdBlockEngine:    Allowed domains: 45
FastAdBlockEngine:    Direct link patterns: 45
```

### When Browsing (Blocking Ads)
```
AdBlockWebViewClient: Blocked by FastEngine: https://doubleclick.net/ad.js
FastAdBlockEngine: 🚫 Blocked direct link ad: https://example.com/sponsored/article
AdBlockWebViewClient: Blocked by HardcodedFilters: https://taboola.com/widget
```

### When Page Finishes Loading
```
AdBlockWebViewClient: 🛡️ Total blocked: 67 requests
AdBlockWebViewClient: 🎯 Direct link ads blocked: 12
```

---

## Troubleshooting

### "adb is not recognized"
**Solution:** Add ADB to your PATH or use full path:
```cmd
C:\Users\w\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat
```

### No device connected
**Check connection:**
```cmd
adb devices
```
Should show:
```
List of devices attached
ABC123XYZ    device
```

If empty:
1. Enable USB debugging on your phone
2. Reconnect USB cable
3. Accept "Allow USB debugging" prompt on phone

### Too many logs
**Filter more specifically:**
```cmd
adb logcat -v time AdBlockWebViewClient:D FastAdBlockEngine:D *:S
```
The `*:S` silences all other logs.

### Logs not showing
**Increase log level:**
```cmd
adb logcat -v time *:V | findstr /i "AdBlock"
```

---

## Advanced Usage

### Watch specific patterns
```cmd
REM Only direct link ads
adb logcat | findstr "direct link"

REM Only blocked requests
adb logcat | findstr "Blocked by"

REM Only statistics
adb logcat | findstr "Total blocked"
```

### Multiple devices
```cmd
REM List devices
adb devices

REM Target specific device
adb -s ABC123XYZ logcat | findstr "AdBlock"
```

### Color-coded output (PowerShell)
```powershell
adb logcat -v time | Select-String "AdBlock|FastAdBlockEngine"
```

---

## Testing Workflow

### Recommended Testing Process:

1. **Start logging:**
   ```cmd
   watch_adblock_detailed.bat
   ```

2. **Install and run app:**
   ```cmd
   gradlew installDebug
   ```

3. **Test on these sites:**
   - forbes.com (sponsored content)
   - businessinsider.com (Taboola widgets)
   - dailymail.co.uk (native ads)
   - cnet.com (affiliate links)

4. **Watch for:**
   - ✅ "🚫 Blocked direct link ad" messages
   - ✅ "🎯 Direct link ads blocked: X" counts
   - ✅ High blocking percentages

5. **Save results:**
   ```cmd
   save_adblock_logs.bat
   ```

---

## Quick Reference

| Command | Purpose |
|---------|---------|
| `watch_adblock_logs.bat` | Live monitoring (simple) |
| `watch_adblock_detailed.bat` | Live monitoring (detailed) |
| `save_adblock_logs.bat` | Save to file |
| `adb logcat -c` | Clear logs |
| `adb devices` | Check connected devices |
| `Ctrl+C` | Stop logging |

---

## Example Session

```cmd
C:\Users\w\Desktop\AndroidStudioProjects\Bro2> watch_adblock_logs.bat

========================================
  Ad Blocking Logcat Monitor
========================================

Watching for ad blocking messages...
Press Ctrl+C to stop

12-15 10:23:45.123 D/FastAdBlockEngine: 🚀 Loading filter lists...
12-15 10:23:45.646 D/FastAdBlockEngine: ✅ Loaded in 523ms
12-15 10:23:45.647 D/FastAdBlockEngine:    Blocked domains: 3,847
12-15 10:23:45.647 D/FastAdBlockEngine:    Blocked patterns: 1,203
12-15 10:23:45.647 D/FastAdBlockEngine:    Direct link patterns: 45
12-15 10:24:12.456 D/AdBlockWebViewClient: Blocked by FastEngine: https://doubleclick.net/ad.js
12-15 10:24:12.789 D/FastAdBlockEngine: 🚫 Blocked direct link ad: https://forbes.com/sponsored/article
12-15 10:24:15.234 D/AdBlockWebViewClient: 🛡️ Total blocked: 67 requests
12-15 10:24:15.234 D/AdBlockWebViewClient: 🎯 Direct link ads blocked: 12
```

---

## Tips

- **Use detailed mode** when testing new patterns
- **Save logs** when reporting issues
- **Clear logs** (`adb logcat -c`) before each test for clean results
- **Filter by time** to focus on specific browsing sessions
- **Compare before/after** by saving logs from different app versions

---

## Need Help?

If logs aren't showing:
1. Check device is connected: `adb devices`
2. Check app is running
3. Try clearing logs: `adb logcat -c`
4. Increase verbosity: `adb logcat *:V`
5. Check USB debugging is enabled on phone
