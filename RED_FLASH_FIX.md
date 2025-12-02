# Red Background Flash Fix

## Problem
When opening a site or switching between tabs, a red background flash was visible during navigation transitions.

## Root Cause
The red flash was caused by the app's primary color (RedPrimary = #FD1D1D) being briefly visible during screen transitions. This happened because:
1. The NavHost didn't have an explicit background color
2. Some Scaffold containers were using default colors instead of the dark background
3. During navigation transitions, Compose was showing the primary color as a fallback

## Solution
Added explicit dark background colors to all screens and the navigation host:

### Files Modified

1. **EntertainmentNavHost.kt**
   - Wrapped NavHost in a Surface with `MaterialTheme.colorScheme.background`
   - Ensures consistent dark background during all navigation transitions

2. **WebViewScreen.kt**
   - Added `containerColor = MaterialTheme.colorScheme.background` to Scaffold
   - Prevents red flash when loading websites

3. **HomeScreen.kt**
   - Added `containerColor = MaterialTheme.colorScheme.background` to Scaffold

4. **TabsScreen.kt**
   - Added `.background(MaterialTheme.colorScheme.background)` to root Box

5. **OnboardingScreen.kt**
   - Added `.background(MaterialTheme.colorScheme.background)` to root Box

6. **FavoritesScreen.kt**
   - Added `containerColor = MaterialTheme.colorScheme.background` to Scaffold

7. **SessionsScreen.kt**
   - Added `containerColor = MaterialTheme.colorScheme.background` to Scaffold

8. **DownloadsScreen.kt**
   - Added `containerColor = MaterialTheme.colorScheme.background` to Scaffold

## Result
- No more red background flash during navigation
- Consistent dark background across all screens
- Smooth transitions between screens and tabs
- All animations remain disabled (EnterTransition.None, ExitTransition.None)

## Build Fix
After the initial changes, the autoformatter created duplicate `containerColor` parameters in some Scaffold components. These were fixed by:
- Removing duplicate `containerColor = MaterialTheme.colorScheme.background` 
- Keeping only `containerColor = DarkBackground` for consistency

## Testing
Build and test the app:
```bash
gradlew assembleDebug
```

Build Status: ✅ **BUILD SUCCESSFUL**

The red flash should no longer appear when:
- Opening a website from the home screen
- Switching between tabs
- Navigating between different screens
- Loading new pages in the WebView
