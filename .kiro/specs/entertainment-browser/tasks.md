# Implementation Plan

## Overview

This implementation plan breaks down the Entertainment Browser app development into discrete, actionable coding tasks. Each task builds incrementally on previous work, ensuring no orphaned code. Tasks are organized by feature area and reference specific requirements from the requirements document.

## Task List

- [x] 1. Project setup and core infrastructure





- [x] 1.1 Configure project dependencies and build.gradle


  - Add Compose BOM, Hilt, Room, OkHttp, Coil, Fetch, DataStore dependencies
  - Configure KSP for Room and Hilt annotation processing
  - Set up ProGuard rules for release builds
  - _Requirements: 13.1, 14.3_


- [x] 1.2 Create base package structure

  - Create packages: core, data, domain, presentation, di, util
  - Create Result sealed class for error handling
  - Create AppError sealed class for error taxonomy
  - _Requirements: 15.1-15.5_

- [x] 1.3 Set up Hilt dependency injection


  - Create Application class with @HiltAndroidApp
  - Create AppModule, DatabaseModule, NetworkModule, DownloadModule, DataStoreModule
  - Create RepositoryModule with @Binds for repository interfaces
  - _Requirements: 2.1, 7.6_

- [x] 1.4 Implement Material 3 theme


  - Create Color.kt with primary color #FD1D1D and dark theme colors
  - Create Theme.kt with dark theme and gradient backgrounds
  - Create Typography.kt with Roboto font family
  - _Requirements: 13.1-13.5_

- [x] 1.5 Translate HTML/Tailwind designs to Compose


  - Analyze existing HTML screens in #screens folder (welcome.html, features.html, permissions.html, home.html, tabs.html, settings.html)
  - Map Tailwind CSS classes to Material 3 Compose equivalents
  - Extract color values (#FF0000, #0D1117, #0D172E, #121212, etc.)
  - Document gradient patterns (linear-gradient(180deg, ...))
  - Create reusable Compose modifiers for common styles (rounded corners, shadows, gradients)
  - _Requirements: 13.1-13.3_

- [x] 2. Database layer implementation




- [x] 2.1 Create Room entities


  - Implement WebsiteEntity with indices on category and isFavorite
  - Implement TabEntity with index on isActive
  - Implement SessionEntity
  - _Requirements: 2.1, 9.1, 11.1_

- [x] 2.2 Create DAO interfaces


  - Implement WebsiteDao with getByCategory, getFavorites, search, update, insertAll
  - Implement TabDao with getAllTabs, getActiveTab, insert, delete, deleteOldTabs, getTabCount
  - Implement SessionDao with getAllSessions, insert, delete, updateName
  - _Requirements: 2.2, 5.2, 9.2, 10.1, 11.2_

- [x] 2.3 Create AppDatabase


  - Define Room database with all entities
  - Add database callback for prepopulation trigger
  - Configure database builder in DatabaseModule
  - _Requirements: 2.1_

- [x] 2.4 Create website prepopulation data


  - Create data class with 45+ websites (15 Streaming, 10 TV Shows, 10 Books, 10 Video Platforms)
  - Implement prepopulation logic in repository on first launch
  - _Requirements: 2.1, 2.2_

- [x] 3. Domain layer implementation






- [x] 3.1 Create domain models

  - Create Website, Tab, Session, DownloadItem domain models
  - Create Category enum
  - Create DownloadStatus enum
  - _Requirements: 2.4, 9.1, 11.1_



- [x] 3.2 Create repository interfaces
  - Define WebsiteRepository interface with Flow-based methods
  - Define TabRepository interface
  - Define SessionRepository interface
  - Define DownloadRepository interface
  - _Requirements: 2.2, 5.1, 9.2, 11.2_



- [x] 3.3 Implement repository implementations
  - Implement WebsiteRepositoryImpl with DAO and domain model mapping
  - Implement TabRepositoryImpl with thumbnail handling
  - Implement SessionRepositoryImpl with JSON serialization
  - Implement DownloadRepositoryImpl with Fetch integration

  - _Requirements: 2.1-2.5, 9.1-9.5, 11.1-11.5_

- [x] 3.4 Create use cases

  - Create GetWebsitesByCategoryUseCase
  - Create SearchWebsitesUseCase with debouncing
  - Create ToggleFavoriteUseCase
  - Create CreateTabUseCase, SwitchTabUseCase, CloseTabUseCase
  - Create CreateSessionUseCase, RestoreSessionUseCase
  - _Requirements: 2.5, 5.1-5.5, 9.1-9.6, 11.1-11.5_

- [x] 4. DataStore settings implementation






- [x] 4.1 Create settings data classes and keys

  - Create AppSettings data class
  - Define SettingsKeys object with preference keys
  - _Requirements: 12.1_



- [x] 4.2 Implement SettingsRepository


  - Create SettingsRepository with DataStore
  - Implement Flow-based settings observation
  - Implement update methods for each setting
  - _Requirements: 12.1-12.5_

- [x] 5. Navigation setup




- [x] 5.1 Create navigation routes


  - Define Screen sealed class with all routes
  - Add route creation functions with URL encoding
  - _Requirements: 1.1, 3.1_

- [x] 5.2 Implement navigation graph


  - Create EntertainmentNavHost composable
  - Define all navigation destinations with arguments
  - Configure back stack behavior
  - _Requirements: 1.3, 3.1, 3.5_

- [x] 5.3 Configure deep linking


  - Add intent filters in AndroidManifest.xml
  - Implement deep link handling in navigation graph
  - _Requirements: 3.1_

- [x] 6. Onboarding flow implementation





- [x] 6.1 Create onboarding data models

  - Create OnboardingPage data class
  - Create OnboardingState data class
  - _Requirements: 1.1, 1.2_

- [x] 6.2 Implement onboarding screens


  - Create WelcomeScreen composable matching welcome.html design (hero image, gradient overlay, feature list with icons)
  - Create FeaturesScreen composable matching features.html design (3 feature cards with icons, skip button)
  - Create PermissionsScreen composable matching permissions.html design (2 permission cards with icons and descriptions)
  - Create FinalScreen with summary and "Start Exploring" CTA button
  - Translate Tailwind styles: bg-gradient-deep-blue, rounded-2xl, shadow effects to Compose
  - _Requirements: 1.2, 1.4, 1.5_

- [x] 6.3 Create OnboardingViewModel


  - Implement page state management
  - Handle permission request results
  - Save completion status to DataStore
  - _Requirements: 1.3, 1.4, 1.5_

- [x] 6.4 Implement OnboardingScreen container


  - Use HorizontalPager for screen navigation
  - Add page indicators
  - Handle completion and navigation to home
  - _Requirements: 1.2, 1.3_

- [x] 6.5 Configure splash screen


  - Use Android 12+ Splash Screen API
  - Check onboarding completion in MainActivity
  - Navigate to appropriate start destination
  - _Requirements: 1.1_

- [x] 7. Home screen implementation





- [x] 7.1 Create HomeUiState and HomeEvent

  - Define UI state with websites, category, search query, loading, error
  - Define event sealed class for user actions
  - _Requirements: 2.3, 2.5, 5.1_


- [x] 7.2 Implement HomeViewModel

  - Inject use cases via Hilt
  - Implement category selection logic
  - Implement search with 300ms debouncing
  - Handle favorite toggle
  - _Requirements: 2.5, 5.1-5.5, 4.4_

- [x] 7.3 Create reusable composables


  - Create SearchBar composable with debounced input
  - Create CategoryTabs composable
  - Create WebsiteCard composable matching home.html design (background image with gradient overlay, logo, name, description, category badge, bookmark icon)
  - Translate card styles: rounded-2xl, border colors (netflix-red, disney-blue, imdb-yellow, goodreads-brown), shadow-deep-dark, hover effects
  - Create ShimmerWebsiteCard for loading state
  - _Requirements: 2.3, 2.4, 5.1, 14.5_

- [x] 7.4 Implement HomeScreen composable


  - Create Scaffold with TopAppBar (title: "CineBrowse") and BottomNavigationBar
  - Implement LazyVerticalGrid with 2 columns matching home.html layout
  - Apply dark background (#121212) and section headers styling
  - Add floating FAB for favorites (bottom-right, red accent)
  - Add pull-to-refresh functionality
  - Handle loading, error, and empty states
  - _Requirements: 2.3, 2.4, 2.5, 5.4, 14.2_



- [x] 7.5 Implement bottom navigation
  - Create BottomNavigationBar with Home, Favorites, Downloads, Tabs, Settings
  - Handle navigation between screens
  - _Requirements: 3.1_

- [x] 8. Favorites screen implementation



















- [x] 8.1 Create FavoritesViewModel


  - Observe favorites from repository
  - Handle favorite toggle
  - _Requirements: 4.2, 4.4_

- [x] 8.2 Implement FavoritesScreen composable


  - Display favorites in LazyVerticalGrid
  - Show empty state when no favorites exist
  - Reuse WebsiteCard composable
  - _Requirements: 4.3, 4.5_
 
- [x] 9. WebView implementation






- [x] 9.1 Create WebViewUiState and WebViewEvent

  - Define state with URL, loading, video detected, DRM detected
  - Define events for toolbar actions and download
  - _Requirements: 3.1, 3.2, 6.1-6.6_

- [x] 9.2 Implement video detection logic


  - Create JavaScript injection for video element detection
  - Implement URL interception in WebViewClient
  - Add video URL pattern matching (.mp4, .webm, .m3u8, .mpd)
  - _Requirements: 6.1, 6.2, 6.3_

- [x] 9.3 Implement DRM detection


  - Check page source for DRM keywords (widevine, playready, eme)
  - Maintain list of known DRM sites
  - Show warning dialog for DRM content
  - _Requirements: 6.4_

- [x] 9.4 Create CustomWebView composable


  - Use AndroidView with WebView
  - Configure WebView settings (JavaScript, DOM storage, security)
  - Implement WebChromeClient for fullscreen video support
  - Implement WebViewClient for URL interception and HTTPS enforcement
  - _Requirements: 3.2, 6.1-6.6_

- [x] 9.5 Implement WebViewViewModel


  - Handle video detection callbacks
  - Trigger download when FAB clicked
  - Manage toolbar state
  - _Requirements: 6.2, 7.1_

- [x] 9.6 Create WebViewScreen composable



  - Add Scaffold with WebViewToolbar
  - Show floating download FAB when video detected
  - Display DRM warning dialog when needed
  - _Requirements: 3.1-3.5, 6.2, 6.4_

- [x] 9.7 Implement WebViewToolbar


  - Add back, forward, refresh, share buttons
  - Show loading progress indicator
  - Handle navigation actions
  - _Requirements: 3.3, 3.4, 3.5_

- [x] 10. Download management implementation




- [x] 10.1 Configure Fetch library


  - Create Fetch instance in DownloadModule
  - Set concurrent download limit to 3
  - Configure OkHttpDownloader
  - Set up notification manager
  - _Requirements: 7.3, 7.6_

- [x] 10.2 Implement MediaStore integration


  - Create function to save files using MediaStore API
  - Handle scoped storage for Android 10+
  - Generate content URIs for downloads
  - _Requirements: 7.2_

- [x] 10.3 Create DownloadRepositoryImpl


  - Implement startDownload with Fetch enqueue
  - Implement pauseDownload, resumeDownload, cancelDownload
  - Observe download progress with Fetch listeners
  - Map Fetch Download to DownloadItem domain model
  - _Requirements: 7.1-7.7, 8.1-8.5_

- [x] 10.4 Implement download notifications


  - Create notification channel for downloads
  - Show progress notifications with percentage
  - Show completion notification with "Open" action
  - Handle notification clicks
  - _Requirements: 7.4, 7.5_

- [x] 10.5 Create DownloadsUiState and DownloadsEvent


  - Define state with active, completed, failed downloads
  - Define events for pause, resume, cancel, delete, open
  - _Requirements: 8.1-8.5_

- [x] 10.6 Implement DownloadsViewModel


  - Observe downloads from repository
  - Handle download actions (pause, resume, cancel, delete)
  - Group downloads by status
  - _Requirements: 8.1-8.5_

- [x] 10.7 Create DownloadsScreen composable


  - Display downloads in LazyColumn grouped by status
  - Show progress bars with percentage and speed
  - Add action buttons for each download
  - Implement pull-to-refresh
  - _Requirements: 8.1-8.5_

- [x] 11. Tab management implementation








- [x] 11.1 Create TabManager class


  - Implement tab creation with unique IDs
  - Enforce 20 tab limit
  - Implement automatic oldest tab closure
  - Manage current tab state
  - _Requirements: 9.1-9.3_

- [x] 11.2 Implement tab thumbnail capture



  - Create function to capture WebView as bitmap 
  - Scale bitmap to thumbnail size
  - Save thumbnail to internal storage
  - _Requirements: 9.4_


- [x] 11.3 Create TabsViewModel

  - Observe tabs from repository
  - Handle tab switching
  - Handle tab closing
  - Handle "Close All" action
  - _Requirements: 9.1-9.6_

- [x] 11.4 Implement TabsScreen composable


  - Match tabs.html design: fullscreen WebView with bottom tab bar
  - Display circular tab thumbnails (40x40dp) with active tab having red border
  - Show home button and horizontal scrollable tab list at bottom
  - Add close button (red circle with X) on active tab
  - Apply gradient overlay: from-black/80 to-transparent with backdrop-blur
  - _Requirements: 9.4, 9.5_


- [x] 11.5 Implement tab persistence


  - Save tabs to database on app background (onStop)
  - Restore tabs on app launch
  - Handle tab restoration failures with error placeholder
  - _Requirements: 10.1-10.5, 9.6_



- [x] 11.6 Create TabCleanupWorker

  - Implement HiltWorker for background cleanup
  - Delete tabs older than 7 days
  - Schedule periodic work with WorkManager
  - _Requirements: 10.4_

- [x] 12. Session ma nt implementation





- [x] 12.1 Create SessionsViewModel


  - Observe sessions from repository
  - Handle session creation with name input
  - Handle session restoration
  - Handle session deletion with confirmation
  - Handle session renaming
  - _Requirements: 11.1-11.5_

- [x] 12.2 Implement SessionsScreen composable


  - Display sessions in LazyColumn sorted by date
  - Show session name, tab count, creation date
  - Add restore, rename, delete actions
  - Implement swipe-to-delete with confirmation dialog
  - _Requirements: 11.3, 11.4, 11.5_

- [x] 12.3 Implement session serialization


  - Use Kotlinx Serialization for tab IDs JSON array
  - Implement serialize and deserialize functions
  - _Requirements: 11.2_

- [x] 13. Settings screen implementation





- [x] 13.1 Create SettingsViewModel


  - Observe settings from SettingsRepository
  - Handle setting updates
  - Implement clear cache functionality
  - Implement clear download history functionality
  - _Requirements: 12.1-12.5_

- [x] 13.2 Implement SettingsScreen composable


  - Match settings.html design: gradient background (from #051c4a to #101622)
  - Create grouped setting items with dark-card background (rgba(16, 22, 34, 0.5))
  - Implement "Download on Wi-Fi only" toggle with custom switch (red accent when checked)
  - Create "Download location" and "Maximum concurrent downloads" navigation items with chevron icons
  - Add section titles in uppercase red text (#ff3b30)
  - Create "Clear Cache" button in separate section
  - Add About section with app version, Privacy Policy, Terms of Service links
  - Apply rounded-xl corners and white/10 dividers between items
  - _Requirements: 12.2, 12.3, 12.4, 12.5_

- [x] 13.3 Implement haptic feedback integration


  - Add haptic feedback to favorite toggle
  - Add haptic feedback to tab close
  - Add haptic feedback to long press actions
  - Respect haptic feedback setting
  - _Requirements: 12.1_

- [x] 13.4 Fix tab management flow integration








  - **Issue 1**: When clicking on a website from Home screen, it should open in a new tab with bottom tab bar visible (matching tabs.html design), but currently opens in standalone WebView without tab management
  - **Issue 2**: When switching between tabs, the WebView reloads the site instead of preserving state (each navigation creates a new WebView instance)
  - Integrate tab creation into WebView navigation flow
  - Show bottom tab bar in WebView screen with all open tabs
  - Allow switching between tabs from WebView screen without reloading
  - Implement WebView state preservation (maintain WebView instances in memory or save/restore state)
  - Implement tab thumbnail capture when switching tabs
  - Ensure tabs persist across app restarts
  - Match tabs.html design: fullscreen WebView with bottom tab bar showing circular tab thumbnails
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 7.1_

- [x] 14. Error handling and accessibility






- [x] 14.1 Create error handling composables

  - Create ErrorState composable with retry button
  - Create LoadingState composable with shimmer
  - Create EmptyState composable with illustration
  - _Requirements: 15.1-15.5_


- [x] 14.2 Implement error handling in ViewModels

  - Wrap repository calls in try-catch
  - Map exceptions to AppError types
  - Update UI state with error messages
  - _Requirements: 15.1-15.5_


- [x] 14.3 Add accessibility features

  - Add content descriptions to all interactive elements
  - Ensure 48dp minimum touch targets
  - Test with TalkBack
  - Verify color contrast ratios
  - Support font scaling up to 200%
  - _Requirements: 16.1-16.5_

- [x] 15. Performance optimization and testing





- [x] 15.1 Implement performance optimizations


  - Add remember and derivedStateOf for expensive calculations
  - Use key parameter in LazyColumn/LazyVerticalGrid
  - Configure Coil image caching
  - Add database indices
  - _Requirements: 14.1, 14.2, 14.4, 14.5_



- [x] 15.2 Add performance monitoring

  - Implement reportFullyDrawn() for startup time
  - Add FrameMetricsAggregator for frame rate monitoring
  - Profile with Android Studio Profiler

  - _Requirements: 14.1, 14.2_

- [x] 15.3 Integrate LeakCanary

  - Add LeakCanary dependency for debug builds
  - Test critical flows for memory leaks
  - _Requirements: 14.6_


- [x] 15.4 Write unit tests for repositories

  - Test WebsiteRepository with mock DAO
  - Test DownloadRepository with mock Fetch
  - Test TabRepository and SessionRepository
  - _Requirements: All_

- [x] 15.5 Write unit tests for ViewModels


  - Test HomeViewModel state updates
  - Test DownloadsViewModel actions
  - Test TabsViewModel and SessionsViewModel
  - _Requirements: All_

- [x] 15.6 Write UI tests for critical flows


  - Test onboarding completion flow
  - Test website browsing and favorite toggle
  - Test search with debouncing
  - Test tab creation and switching
  - Test download lifecycle
  - _Requirements: All_

- [ ] 16. Final integration and polish
- [ ] 16.1 Configure ProGuard rules
  - Add rules for Compose, Room, OkHttp, Fetch, Kotlinx Serialization
  - Test release build
  - _Requirements: 14.3_

- [ ] 16.2 Create app icon and splash screen
  - Design adaptive icon with Material You support
  - Create monochrome icon for themed icons
  - Configure Android 12+ splash screen
  - _Requirements: 13.1_

- [ ] 16.3 Test on multiple Android versions
  - Test on Android 7 (API 24)
  - Test on Android 11 (API 30) for scoped storage
  - Test on Android 13 (API 33) for granular permissions
  - Test on Android 14+ (API 34+)
  - _Requirements: 1.4, 1.5, 7.2_

- [ ] 16.4 Verify all requirements
  - Go through requirements document and verify each acceptance criterion
  - Fix any remaining issues
  - _Requirements: All_

- [ ] 16.5 Build signed release APK/AAB
  - Generate release keystore
  - Configure signing in build.gradle
  - Build release AAB for Play Store
  - Verify APK size under 15MB
  - _Requirements: 14.3_
