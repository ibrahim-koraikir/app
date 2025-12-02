# Fanboy's Annoyance List - Added to Ad Blocking

## What It Blocks

Fanboy's Annoyance List blocks annoying web elements that aren't necessarily ads but degrade the browsing experience:

### 1. Cookie Consent Banners
- GDPR cookie notices
- Cookie consent popups
- "Accept cookies" banners
- Cookie preference dialogs

### 2. Social Media Widgets
- Facebook Like buttons
- Twitter/X share buttons
- LinkedIn share widgets
- Pinterest pins
- Social media follow buttons
- Share bars and floating social buttons

### 3. Newsletter Popups
- Email subscription popups
- Newsletter signup overlays
- "Subscribe to our newsletter" modals
- Exit-intent popups

### 4. Chat Widgets
- Live chat popups
- Customer support chat bubbles
- Chatbot widgets
- "Need help?" floating buttons

### 5. Push Notification Requests
- Browser push notification prompts
- "Allow notifications" popups
- Notification permission requests

### 6. App Install Banners
- "Download our app" banners
- Mobile app install prompts
- "Open in app" overlays
- App store redirect banners

### 7. Survey & Feedback Widgets
- User survey popups
- Feedback forms
- Rating request dialogs
- "How are we doing?" widgets

### 8. Other Annoyances
- Age verification popups
- Location permission requests
- Floating video players
- Sticky headers that take up screen space
- "Scroll to top" buttons
- Anti-adblock messages

## Benefits

1. **Cleaner browsing experience** - Less clutter on web pages
2. **Faster page loads** - Fewer scripts and widgets to load
3. **More privacy** - Blocks social media tracking widgets
4. **Better mobile experience** - Removes intrusive mobile-specific popups
5. **Reduced distractions** - Focus on actual content

## Filter List Details

- **Source**: https://secure.fanboy.co.nz/fanboy-annoyance.txt
- **File size**: ~2MB
- **Rules**: ~50,000+ blocking rules
- **Update frequency**: Updated regularly by Fanboy
- **Compatibility**: Works with EasyList and EasyPrivacy

## Performance Impact

- **Memory**: Adds ~50-100MB to app memory usage
- **Load time**: Adds ~500-1000ms to initial filter loading
- **Runtime**: Minimal impact (O(1) HashSet lookups)

## Testing

To test if Fanboy's Annoyance List is working:

1. Visit websites with cookie banners (most EU websites)
2. Check if social media share buttons are blocked
3. Look for missing newsletter popups
4. Verify chat widgets are removed

### Test Sites
- https://www.bbc.com/ (cookie banner)
- https://www.theguardian.com/ (cookie banner + social widgets)
- https://www.cnn.com/ (newsletter popup + social buttons)
- https://www.forbes.com/ (multiple annoyances)

## Logcat Monitoring

Watch for these log messages:
```bash
# Filter loading
adb logcat | findstr "FastAdBlockEngine"

# Look for:
# "Loaded in XXXXms"
# "Blocked domains: XXXXXX" (should be higher now)
```

## Notes

- Some websites may break if they require cookie consent
- Social login buttons (Login with Facebook) may be affected
- You can whitelist specific domains if needed in `FastAdBlockEngine.kt`
- The filter list is loaded once at app startup and cached in memory

## Disabling

To disable Fanboy's Annoyance List, remove this line from `FastAdBlockEngine.kt`:
```kotlin
"adblock/fanboy-annoyance.txt"
```

And rebuild the app.
