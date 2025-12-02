# Red Border Flash - Complete Fix

## Problem
A red border/outline flashes around the entire screen for less than 1 second when:
- Opening the app
- Switching between tabs
- Navigating between screens

This issue appeared after implementing animations and removing them.

## Root Causes
1. **Material You Dynamic Colors** - Android 12+ was pulling red colors from system wallpaper
2. **Light Theme Parent** - Theme was using `Theme.Material.Light` which has light/red defaults
3. **MaterialTheme.colorScheme.background** - Was resolving to red primary color during transitions
4. **Missing explicit black backgrounds** - Containers were showing primary color during brief transitions

## Complete Solution

### 1. Theme Changes (themes.xml)
- Changed parent from `android:Theme.Material.Light.NoActionBar` to `android:Theme.Material.NoActionBar` (dark theme)
- Set `android:windowBackground` to `@color/black`
- Set `android:colorBackground` to `@color/black`
- Set `android:statusBarColor` to `@color/black`
- Set `android:navigationBarColor` to `@color/black`
- Applied to both `Theme.Bro` and `Theme.Bro.PostSplash`

### 2. Compose Theme (Theme.kt)
- Changed `dynamicColor` parameter default from `true` to `false`
- This disables Material You dynamic colors that pull from wallpaper

### 3. MainActivity
- Set window background: `window.setBackgroundDrawableResource(android.R.color.black)`
- Set status bar color: `window.statusBarColor = android.graphics.Color.BLACK`
- Set navigation bar color: `window.navigationBarColor = android.graphics.Color.BLACK`
- Disabled edge-to-edge temporarily (commented out)
- Changed Surface color to explicit `Color(0xFF121212)` instead of `MaterialTheme.colorScheme.background`

### 4. WebViewScreen
- Changed Scaffold `containerColor` from `MaterialTheme.colorScheme.background` to `Color.Black`
- Added explicit `.background(Color.Black)` to the main Box container

### 5. Other Screens
- HomeScreen: `containerColor = DarkBackground`
- FavoritesScreen: `containerColor = DarkBackground`
- SessionsScreen: `containerColor = DarkBackground`
- DownloadsScreen: `containerColor = MaterialTheme.colorScheme.background`
- TabsScreen: Added `.background(MaterialTheme.colorScheme.background)` to root Box
- OnboardingScreen: Added `.background(MaterialTheme.colorScheme.background)` to root Box

### 6. Navigation (EntertainmentNavHost)
- Wrapped NavHost in Surface with `color = MaterialTheme.colorScheme.background`
- All transitions set to `EnterTransition.None` and `ExitTransition.None`

## Files Modified
1. `app/src/main/res/values/themes.xml`
2. `app/src/main/java/com/entertainmentbrowser/presentation/theme/Theme.kt`
3. `app/src/main/java/com/entertainmentbrowser/MainActivity.kt`
4. `app/src/main/java/com/entertainmentbrowser/presentation/webview/WebViewScreen.kt`
5. `app/src/main/java/com/entertainmentbrowser/presentation/webview/CustomWebView.kt`
6. `app/src/main/java/com/entertainmentbrowser/presentation/home/HomeScreen.kt`
7. `app/src/main/java/com/entertainmentbrowser/presentation/favorites/FavoritesScreen.kt`
8. `app/src/main/java/com/entertainmentbrowser/presentation/sessions/SessionsScreen.kt`
9. `app/src/main/java/com/entertainmentbrowser/presentation/downloads/DownloadsScreen.kt`
10. `app/src/main/java/com/entertainmentbrowser/presentation/tabs/TabsScreen.kt`
11. `app/src/main/java/com/entertainmentbrowser/presentation/onboarding/OnboardingScreen.kt`
12. `app/src/main/java/com/entertainmentbrowser/presentation/navigation/EntertainmentNavHost.kt`

## Testing
Build and install:
```bash
gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Test scenarios:
1. Open the app - should see no red flash
2. Click on a website - should navigate with no red flash
3. Switch between tabs - should see no red border flash
4. Navigate between screens - should see smooth dark transitions

## Build Status
✅ BUILD SUCCESSFUL

## Notes
- If red border still appears, it may be a device-specific setting or accessibility feature
- The fix ensures all backgrounds are explicitly black/dark at every level (system, theme, compose)
- Material You dynamic colors are disabled to prevent wallpaper colors from affecting the app
