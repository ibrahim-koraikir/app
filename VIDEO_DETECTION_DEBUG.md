# Video Detection Debugging Guide

## What I Added

Added comprehensive logging to track video detection through the entire pipeline:

### 1. CustomWebView (JavaScript Interface)
- Logs when JS interface is called: `"JS Interface called with URL: ..."`
- Logs if URL matches video pattern: `"✅ Video URL detected: ..."` or `"❌ URL not recognized as video: ..."`

### 2. AdBlockWebViewClient (Request Interception)
- Logs when video URL is detected in network requests: `"🎥 Video URL detected in request: ..."`

### 3. WebViewViewModel (Event Handling)
- Logs when VideoDetected event is received: `"📹 VideoDetected event received: ..."`
- Logs in handleVideoDetected: `"handleVideoDetected called with: ..."`
- Logs if DRM blocks download: `"❌ DRM detected, not showing download button"`
- Logs if format unsupported: `"❌ Unsupported format: ..."`
- Logs when setting videoDetected state: `"✅ Setting videoDetected = true"`

## How to Debug

### Option 1: Using Android Studio Logcat
1. Open Android Studio
2. Go to View → Tool Windows → Logcat
3. Filter by: `VideoDetector|CustomWebView|WebViewViewModel|AdBlockWebViewClient`
4. Play a video on a website
5. Watch for the log messages

### Option 2: Using ADB Command Line
Run this batch file:
```
test_video_detection.bat
```

Or manually:
```
adb logcat -v time | findstr /i "VideoDetector CustomWebView AndroidInterface video WebViewViewModel"
```

## What to Look For

### If you see these logs, video detection is working:
1. `"🎥 Video URL detected in request: ..."` - URL interceptor found video
2. `"📹 VideoDetected event received: ..."` - ViewModel received the event
3. `"✅ Setting videoDetected = true"` - UI state updated, button should show

### If button doesn't show, check for:
1. `"❌ DRM detected, not showing download button"` - Site has DRM protection
2. `"❌ Unsupported format: ..."` - Video format not supported (blob, HLS, DASH)
3. `"❌ URL not recognized as video: ..."` - URL doesn't match video patterns

## Supported Video Formats

The app detects these formats:
- `.mp4` - Standard MP4 videos ✅
- `.webm` - WebM videos ✅
- `.m3u8` - HLS streaming ✅
- `.mpd` - DASH streaming ✅
- `blob:` URLs - Blob videos ✅

## Common Issues

### 1. No logs at all
- JavaScript interface not added properly
- WebView not loading the page
- Ad blocker blocking the video request

### 2. URL detected but button doesn't show
- Check if DRM is detected
- Check if format is supported
- Check UI state in logs

### 3. Button shows but download fails
- Check download logs in DownloadRepositoryImpl
- Check permissions
- Check network connectivity

## Test Sites

Try these sites to test video detection:
- YouTube (has DRM, won't show button)
- Vimeo (may have DRM)
- Direct MP4 links (should work)
- HTML5 video test pages

## Next Steps

1. Install the updated APK
2. Navigate to a video site
3. Play a video
4. Check logs using one of the methods above
5. Report what you see in the logs
