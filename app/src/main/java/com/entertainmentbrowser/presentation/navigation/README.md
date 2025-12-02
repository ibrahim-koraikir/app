# Navigation Setup

This package contains the navigation infrastructure for the Entertainment Browser app.

## Files

### Screen.kt
Defines all navigation routes as a sealed class hierarchy:
- `Onboarding` - First-time user onboarding flow
- `Home` - Main screen with website catalog
- `Favorites` - User's favorited websites
- `WebView` - In-app browser with URL parameter
- `Downloads` - Download management screen
- `Tabs` - Tab management screen
- `Sessions` - Session management screen
- `Settings` - App settings screen

Also includes `DeepLink` object for deep link URI patterns.

### EntertainmentNavHost.kt
Main navigation graph composable that:
- Defines all navigation destinations
- Configures navigation arguments (e.g., URL for WebView)
- Sets up deep link handling
- Manages back stack behavior
- Includes placeholder screens for screens not yet implemented

### MainActivity.kt
Entry point that:
- Determines start destination based on onboarding completion status
- Sets up the navigation controller
- Applies the app theme

## Deep Linking

The app supports deep links in the following format:

```
entertainmentbrowser://app/webview?url={encoded_url}
```

Example:
```
entertainmentbrowser://app/webview?url=https%3A%2F%2Fnetflix.com
```

This allows external apps or web pages to open specific websites directly in the Entertainment Browser.

## Usage

### Navigate to a screen
```kotlin
navController.navigate(Screen.Home.route)
```

### Navigate with arguments
```kotlin
val url = "https://netflix.com"
navController.navigate(Screen.WebView.createRoute(url))
```

### Navigate and clear back stack
```kotlin
navController.navigate(Screen.Home.route) {
    popUpTo(Screen.Onboarding.route) { inclusive = true }
}
```

### Pop back stack
```kotlin
navController.popBackStack()
```

## Requirements Satisfied

- **1.1**: Onboarding flow navigation
- **1.3**: Navigation between screens
- **3.1**: Deep linking support for opening websites
- **3.5**: Back navigation handling
