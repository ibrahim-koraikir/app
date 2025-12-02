# Comprehensive Code Index: Entertainment Browser App

## Project Overview

This is an Android entertainment browser application built with Kotlin and Jetpack Compose, featuring advanced ad-blocking, video detection, download capabilities, and tab management. The app follows Clean Architecture principles with Hilt dependency injection.

## Architecture Summary

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: Clean Architecture (Domain/Data/Presentation layers)
- **Dependency Injection**: Hilt
- **Database**: Room
- **Async**: Coroutines + Flow
- **Navigation**: Compose Navigation
- **WebView**: Android WebView with custom client
- **Build System**: Gradle with Kotlin DSL

### Key Features
1. **Advanced Ad-Blocking**: Fast HashSet-based engine with 1000+ hardcoded filters
2. **Video Detection & Download**: Automatic video detection with quality selection
3. **Tab Management**: Multi-tab browsing with thumbnails and session management
4. **Monetization**: Ad injection system every 3-6 URL loads
5. **GPU Memory Management**: Optimized WebView performance
6. **DRM Protection**: Detection and warning for protected content

## Detailed Architecture Breakdown

### 1. Application Layer

#### [`EntertainmentBrowserApp.kt`](app/src/main/java/com/entertainmentbrowser/EntertainmentBrowserApp.kt:1)
- **Purpose**: Application class with Hilt integration
- **Key Responsibilities**:
  - Initialize ad-blocking engine in background
  - Set up monetization manager
  - Schedule periodic tab cleanup (7-day intervals)
  - Preload WebView pool for performance
  - Configure GPU memory management
  - Database prepopulation

#### [`MainActivity.kt`](app/src/main/java/com/entertainmentbrowser/MainActivity.kt:1)
- **Purpose**: Main activity handling splash screen and navigation setup
- **Key Features**:
  - Permission handling for storage access
  - Edge-to-edge display configuration
  - Dark theme setup to prevent visual glitches
  - Navigation to onboarding or home based on first launch

### 2. Domain Layer (Business Logic)

#### Data Models
- **[`Tab.kt`](app/src/main/java/com/entertainmentbrowser/domain/model/Tab.kt:1)**: Tab entity with URL, title, thumbnail, and active state
- **[`Website.kt`](app/src/main/java/com/entertainmentbrowser/domain/model/Website.kt:1)**: Website catalog entry with categories and favorites
- **[`Session.kt`](app/src/main/java/com/entertainmentbrowser/domain/model/Session.kt:1)**: Saved tab collections
- **[`DownloadItem.kt`](app/src/main/java/com/entertainmentbrowser/domain/model/DownloadItem.kt:1)**: Download management with progress tracking
- **[`Category.kt`](app/src/main/java/com/entertainmentbrowser/domain/model/Category.kt:1)**: Content categories (STREAMING, TV_SHOWS, BOOKS, VIDEO_PLATFORMS)

#### Repository Interfaces
- **[`TabRepository.kt`](app/src/main/java/com/entertainmentbrowser/domain/repository/TabRepository.kt:1)**: Tab CRUD operations with Flow-based observation
- **[`WebsiteRepository.kt`](app/src/main/java/com/entertainmentbrowser/domain/repository/WebsiteRepository.kt:1)**: Website catalog management and search
- **[`SessionRepository.kt`](app/src/main/java/com/entertainmentbrowser/domain/repository/SessionRepository.kt:1)**: Session persistence and restoration
- **[`DownloadRepository.kt`](app/src/main/java/com/entertainmentbrowser/domain/repository/DownloadRepository.kt:1)**: Download lifecycle management
- **[`SettingsRepository.kt`](app/src/main/java/com/entertainmentbrowser/domain/repository/SettingsRepository.kt:1)**: App preferences and configuration

#### Use Cases (Business Logic Operations)
- **[`CreateTabUseCase.kt`](app/src/main/java/com/entertainmentbrowser/domain/usecase/CreateTabUseCase.kt:1)**: Tab creation with 20-tab limit enforcement
- **[`SwitchTabUseCase.kt`](app/src/main/java/com/entertainmentbrowser/domain/usecase/SwitchTabUseCase.kt:1)**: Active tab switching
- **[`CloseTabUseCase.kt`](app/src/main/java/com/entertainmentbrowser/domain/usecase/CloseTabUseCase.kt:1)**: Tab closure and cleanup
- **[`SearchWebsitesUseCase.kt`](app/src/main/java/com/entertainmentbrowser/domain/usecase/SearchWebsitesUseCase.kt:1)**: Debounced website search (300ms)
- **[`ToggleFavoriteUseCase.kt`](app/src/main/java/com/entertainmentbrowser/domain/usecase/ToggleFavoriteUseCase.kt:1)**: Favorite status management
- **[`GetAllWebsitesUseCase.kt`](app/src/main/java/com/entertainmentbrowser/domain/usecase/GetAllWebsitesUseCase.kt:1)**: Combined website data from all categories
- **[`CreateSessionUseCase.kt`](app/src/main/java/com/entertainmentbrowser/domain/usecase/CreateSessionUseCase.kt:1)**: Session creation from current tabs
- **[`RestoreSessionUseCase.kt`](app/src/main/java/com/entertainmentbrowser/domain/usecase/RestoreSessionUseCase.kt:1)**: Session restoration

### 3. Presentation Layer (UI)

#### Navigation
- **[`EntertainmentNavHost.kt`](app/src/main/java/com/entertainmentbrowser/presentation/navigation/EntertainmentNavHost.kt:1)**: Main navigation host with deep link support
- **[`Screen.kt`](app/src/main/java/com/entertainmentbrowser/presentation/navigation/Screen.kt:1)**: Sealed class defining all navigation destinations

#### ViewModels
- **[`HomeViewModel.kt`](app/src/main/java/com/entertainmentbrowser/presentation/home/HomeViewModel.kt:1)**: Home screen state management with search and categorization
- **[`WebViewViewModel.kt`](app/src/main/java/com/entertainmentbrowser/presentation/webview/WebViewViewModel.kt:1)**: WebView state, video detection, downloads, and tab management

#### Screens
- **[`WebViewScreen.kt`](app/src/main/java/com/entertainmentbrowser/presentation/webview/WebViewScreen.kt:1)**: Main browsing interface with:
  - Custom WebView integration
  - Tab bar with thumbnails
  - Pull-to-refresh functionality
  - Download dialog integration
  - Context menu for long-press
  - DRM warning dialogs
  - Auto-hiding tab bar on scroll

#### WebView Components
- **[`CustomWebView.kt`](app/src/main/java/com/entertainmentbrowser/presentation/webview/CustomWebView.kt:1)**: Advanced WebView composable with:
  - Tab-specific WebView state management
  - JavaScript interface for video/DRM detection
  - Download handling with quality selection
  - Long-press context menu
  - Touch event handling for pull-to-refresh
  - Hardware acceleration optimization
- **[`AdBlockWebViewClient.kt`](app/src/main/java/com/entertainmentbrowser/presentation/webview/AdBlockWebViewClient.kt:1)**: WebView client with:
  - Ad-blocking integration
  - Video detection callbacks
  - Monetization ad injection
  - CSS injection for ad hiding
  - Navigation state management

### 4. Utility Layer (Infrastructure)

#### Ad-Blocking System
- **[`FastAdBlockEngine.kt`](app/src/main/java/com/entertainmentbrowser/util/adblock/FastAdBlockEngine.kt:1)**: High-performance ad blocker with:
  - HashSet-based O(1) domain lookups
  - Filter list loading from assets
  - Direct link ad detection
  - Monetization domain whitelisting
  - Customizable filter lists
- **[`HardcodedFilters.kt`](app/src/main/java/com/entertainmentbrowser/util/adblock/HardcodedFilters.kt:1)**: Fallback system with 1000+ ad domains
- **[`AdBlockMetrics.kt`](app/src/main/java/com/entertainmentbrowser/util/adblock/AdBlockMetrics.kt:1)**: Performance tracking and analytics

#### WebView Management
- **[`WebViewPool.kt`](app/src/main/java/com/entertainmentbrowser/util/WebViewPool.kt:1)**: WebView instance pooling for performance
- **[`WebViewStateManager.kt`](app/src/main/java/com/entertainmentbrowser/util/WebViewStateManager.kt:1)**: Per-tab WebView state management with LRU caching
- **[`GpuMemoryManager.kt`](app/src/main/java/com/entertainmentbrowser/util/GpuMemoryManager.kt:1)**: GPU memory optimization for WebView performance

#### Media & Download
- **[`VideoDetector.kt`](app/src/main/java/com/entertainmentbrowser/presentation/webview/VideoDetector.kt:1)**: JavaScript-based video URL detection
- **[`DrmDetector.kt`](app/src/main/java/com/entertainmentbrowser/presentation/webview/DrmDetector.kt:1)**: DRM content detection
- **[`DownloadDialog.kt`](app/src/main/java/com/entertainmentbrowser/presentation/webview/DownloadDialog.kt:1)**: Quality selection UI for downloads
- **[`DownloadNotificationManager.kt`](app/src/main/java/com/entertainmentbrowser/util/DownloadNotificationManager.kt:1)**: Download progress notifications

#### Performance & Storage
- **[`CacheManager.kt`](app/src/main/java/com/entertainmentbrowser/util/CacheManager.kt:1)**: Automatic cache cleanup (7-day retention)
- **[`ThumbnailCapture.kt`](app/src/main/java/com/entertainmentbrowser/util/ThumbnailCapture.kt:1)**: WebView thumbnail generation for tab previews
- **[`BitmapManager.kt`](app/src/main/java/com/entertainmentbrowser/util/BitmapManager.kt:1)**: Bitmap compression and memory optimization

#### Monetization
- **[`MonetizationManager.kt`](app/src/main/java/com/entertainmentbrowser/util/MonetizationManager.kt:1)**: Ad injection system with:
  - URL load tracking
  - 3-6 load interval ads
  - Pending URL management
  - Ad domain whitelisting

#### Tab & Session Management
- **[`TabManager.kt`](app/src/main/java/com/entertainmentbrowser/util/TabManager.kt:1)**: Tab lifecycle management with database operations
- **[`SessionSerializer.kt`](app/src/main/java/com/entertainmentbrowser/util/SessionSerializer.kt:1)**: JSON serialization for session persistence

#### Accessibility & UX
- **[`AccessibilityHelper.kt`](app/src/main/java/com/entertainmentbrowser/util/AccessibilityHelper.kt:1)**: Accessibility support and content descriptions
- **[`HapticFeedbackHelper.kt`](app/src/main/java/com/entertainmentbrowser/util/HapticFeedbackHelper.kt:1)**: Haptic feedback for user interactions

## Configuration & Build

### Build Configuration
- **[`build.gradle.kts (app)`](app/build.gradle.kts:1)**: App-level build configuration with:
  - SDK versions: minSdk 24, targetSdk 36, compileSdk 36
  - ProGuard optimization for release builds
  - APK splitting for different architectures
  - Compose and Hilt plugins
- **[`build.gradle.kts (root)`](build.gradle.kts:1)**: Project-level build configuration
- **[`gradle/libs.versions.toml`](gradle/libs.versions.toml:1)**: Centralized dependency version management
- **[`gradle.properties`](gradle.properties:1)**: Build optimization settings

### Manifest Configuration
- **[`AndroidManifest.xml`](app/src/main/AndroidManifest.xml:1)**: App permissions and configuration:
  - Internet and network state permissions
  - Storage permissions for downloads (API-dependent)
  - Notification permissions
  - Deep link support for custom URL schemes

## Key Technical Features

### 1. Advanced Ad-Blocking
- **Dual-layer system**: FastAdBlockEngine (HashSet lookups) + HardcodedFilters (fallback)
- **Filter lists**: EasyList, EasyPrivacy, Fanboy Annoyance
- **Direct link ad detection**: Sponsored content and affiliate link blocking
- **Performance metrics**: Real-time blocking statistics
- **Custom whitelisting**: Monetization domains never blocked

### 2. Video Detection & Download
- **JavaScript injection**: Client-side video URL detection
- **Format support**: MP4, WebM, M3U8, MPD
- **Quality selection**: User-configurable download quality
- **DRM protection**: Automatic detection and user warnings
- **Download management**: Repository-based download tracking

### 3. Tab Management
- **State preservation**: WebView state per tab with LRU caching
- **Thumbnail generation**: Automatic tab previews
- **20-tab limit**: Enforced at domain layer
- **Session management**: Save/restore tab collections
- **Memory optimization**: Paused WebViews for inactive tabs

### 4. Performance Optimizations
- **WebView pooling**: Reuse WebView instances
- **GPU memory management**: Hardware acceleration control
- **Cache cleanup**: Automatic 7-day cache expiration
- **Background initialization**: Non-blocking app startup
- **Coroutines**: Efficient async operations

### 5. Monetization System
- **Ad injection**: Every 3-6 URL loads
- **URL interception**: Seamless ad integration
- **Domain whitelisting**: Ads never blocked
- **Counter management**: Persistent load tracking

## Development & Testing

### Test Structure
- **Unit Tests**: Located in `app/src/test/java/`
  - Repository implementations
  - ViewModel logic
  - Ad-blocking engine
  - Use cases
- **Integration Tests**: Located in `app/src/androidTest/java/`
  - Ad-blocking integration
  - Tab management
  - WebView functionality

### Build Commands
```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Lint check
./gradlew lint
```

## Documentation & Resources

### Existing Documentation
- **[`INDEX.md`](INDEX.md:1)**: Original project index
- **[`COMPLETE_APP_DOCUMENTATION.md`](COMPLETE_APP_DOCUMENTATION.md:1)**: Full app documentation
- **[`QUICK_REFERENCE_CHEATSHEET.md`](QUICK_REFERENCE_CHEATSHEET.md:1)**: Development quick reference
- **Feature-specific docs**: Individual markdown files for each major feature

### Debug Scripts (Windows)
- **`watch_adblock_logs.bat`**: Monitor ad-blocking performance
- **`test_monetization_working.bat`**: Test monetization system
- **`check_gpu_status.bat`**: Monitor GPU memory usage
- **`check_crash.bat`**: Crash detection and analysis

## Key Insights & Architecture Decisions

### 1. Clean Architecture Implementation
- **Separation of concerns**: Clear distinction between domain, data, and presentation layers
- **Dependency inversion**: Repository interfaces abstract data sources
- **Use case pattern**: Business logic encapsulated in reusable use cases
- **Flow-based reactive programming**: Real-time UI updates

### 2. Performance-First Design
- **HashSet-based ad blocking**: O(1) lookup performance
- **WebView state management**: Efficient tab switching without reloads
- **GPU memory optimization**: Prevents crashes on memory-constrained devices
- **Background initialization**: Fast app startup

### 3. Monetization Integration
- **Seamless ad injection**: URL interception for native ad experience
- **Whitelisting system**: Ads never blocked by ad-blocker
- **Configurable frequency**: Adjustable ad display intervals
- **Graceful degradation**: App functions even if monetization fails

### 4. User Experience Focus
- **Pull-to-refresh**: Natural mobile interaction pattern
- **Auto-hiding UI**: Tab bar hides on scroll for content focus
- **Context menus**: Long-press actions for links and videos
- **Accessibility**: Full accessibility support throughout

## Conclusion

This Entertainment Browser app demonstrates advanced Android development practices with:
- **Modern architecture**: Clean Architecture + MVVM + Compose
- **Performance optimization**: GPU memory management and WebView pooling
- **Advanced features**: Ad-blocking, video detection, and monetization
- **User experience**: Smooth tab management and intuitive UI
- **Maintainability**: Well-structured code with comprehensive testing

The codebase shows expertise in Android development, particularly in WebView management, performance optimization, and implementing complex features like ad-blocking while maintaining a clean, maintainable architecture.