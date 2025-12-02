# 🔧 Blank Screen Fix

## Problem
Clicking on sites shows blank screen.

## Likely Causes

### 1. AdvancedEngine Not Initialized Yet
The engine loads in background and might not be ready when you click a site.

**Fix Applied**: Added graceful degradation - if not initialized, allows all requests.

### 2. Check Logs
Run this to see what's happening:
```bash
debug_blank_screen.bat
```

Look for:
- ✅ "Advanced ad-blocker ready" - means it initialized
- ❌ "Not initialized yet" - means it's still loading
- ❌ "FATAL" or crashes - means there's an error

## Quick Test

1. **Wait 5 seconds after opening app** before clicking a site
   - This gives engines time to initialize

2. **Check if it works after waiting**
   - If YES: Engine just needs more time to load
   - If NO: Check logs with `debug_blank_screen.bat`

## If Still Blank

The issue might be:
1. **Too aggressive blocking** - blocking CDNs or critical resources
2. **Engine crash** - check logcat for errors
3. **WebView not loading** - check CustomWebView logs

Run `debug_blank_screen.bat` and share the output!
