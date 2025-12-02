# Tab Bar Auto-Hide on Scroll Feature

## Overview
Implemented auto-hide/show functionality for the bottom tab bar based on scroll direction in the WebView. This provides a cleaner, more immersive browsing experience by maximizing screen real estate when scrolling down.

## User Experience

### Behavior
- **Scroll Down**: Tab bar smoothly slides down and hides
- **Scroll Up**: Tab bar smoothly slides up and shows
- **Smooth Animation**: 200ms transition with FastOutSlowInEasing for natural feel
- **Scroll Threshold**: 10px minimum scroll distance to prevent jittery behavior

### Benefits
- More screen space for content when reading/scrolling
- Tab bar is easily accessible by scrolling up slightly
- Smooth, polished animation that feels native
- Reduces visual clutter during active browsing

## Technical Implementation

### 1. CustomWebView.kt
**Added scroll listener parameter:**
```kotlin
onScroll: (scrollY: Int, oldScrollY: Int) -> Unit = { _, _ -> }
```

**Added scroll change listener:**
```kotlin
setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
    onScroll(scrollY, oldScrollY)
}
```

### 2. WebViewScreen.kt
**Added state management:**
- `isTabBarVisible`: Boolean state to track visibility
- `lastScrollY`: Tracks last scroll position for delta calculation

**Scroll handler logic:**
```kotlin
onScroll = { scrollY, oldScrollY ->
    val scrollDelta = scrollY - oldScrollY
    val scrollThreshold = 10
    
    if (abs(scrollDelta) > scrollThreshold) {
        isTabBarVisible = scrollDelta < 0 // Show on scroll up, hide on scroll down
    }
    
    lastScrollY = scrollY
}
```

**Animated tab bar offset:**
```kotlin
val tabBarOffsetY by animateFloatAsState(
    targetValue = if (isTabBarVisible) 0f else 150f,
    animationSpec = tween(
        durationMillis = 200,
        easing = FastOutSlowInEasing
    )
)
```

## Animation Details

### Parameters
- **Duration**: 200ms (fast enough to feel responsive, slow enough to be smooth)
- **Easing**: FastOutSlowInEasing (natural acceleration/deceleration)
- **Offset**: 150dp (approximate tab bar height including padding)
- **Threshold**: 10px scroll delta (prevents accidental triggers)

### States
- **Visible (0dp offset)**: Tab bar at normal position
- **Hidden (150dp offset)**: Tab bar pushed below screen bottom

## Edge Cases Handled

1. **Small Scrolls**: 10px threshold prevents hiding on tiny scroll movements
2. **Direction Changes**: Immediately responds to scroll direction changes
3. **Tab Switching**: State is preserved when switching between tabs
4. **Initial State**: Tab bar starts visible for easy access

## Performance Considerations

- Scroll listener is lightweight and doesn't impact WebView performance
- Animation uses Compose's optimized `animateFloatAsState`
- No recomposition of WebView content during animation
- Offset transformation is GPU-accelerated

## Future Enhancements

Potential improvements:
1. **Velocity-based hiding**: Hide faster on quick scrolls
2. **Partial reveal**: Show a hint of the tab bar when hidden
3. **User preference**: Setting to disable auto-hide
4. **Smart show**: Auto-show when reaching page bottom
5. **Gesture override**: Swipe up from bottom to force show

## Files Modified
- `app/src/main/java/com/entertainmentbrowser/presentation/webview/CustomWebView.kt`
  - Added `onScroll` parameter
  - Added `setOnScrollChangeListener`
  
- `app/src/main/java/com/entertainmentbrowser/presentation/webview/WebViewScreen.kt`
  - Added visibility state management
  - Added scroll handler logic
  - Added animated offset to tab bar

## Testing
- Build successful with no compilation errors
- All diagnostics passed
- Animation parameters tuned for smooth, natural feel

## Compatibility
- Works with existing tab management system
- Compatible with all WebView features (video detection, ad blocking, etc.)
- No impact on tab switching or WebView state preservation
