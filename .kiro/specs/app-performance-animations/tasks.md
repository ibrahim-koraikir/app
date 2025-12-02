# Implementation Plan

- [x] 1. Set up animation foundation





  - Create animations package structure under presentation/common
  - Implement AnimationConstants.kt with duration, delay, and easing specifications
  - _Requirements: 7.5, 8.5, 9.3, 12.5_

- [x] 1.1 Create animation constants and specifications


  - Write AnimationConstants object with DURATION_SHORT (150ms), DURATION_MEDIUM (300ms), DURATION_LONG (500ms)
  - Define DELAY_SHORT (50ms) and DELAY_MEDIUM (100ms) constants
  - Implement SpringDefault, SpringSoft, and SpringStiff spring specifications
  - Implement TweenFast, TweenMedium, and TweenSlow tween specifications
  - _Requirements: 7.5, 8.5, 9.3_

- [x] 2. Implement screen transition animations





  - Create EnterAnimations.kt and ExitAnimations.kt files
  - Implement all enter and exit transition functions
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_



- [x] 2.1 Create enter transition functions

  - Implement slideInFromRight() with 300ms duration and FastOutSlowInEasing
  - Implement slideInFromLeft() for back navigation
  - Implement slideInFromBottom() for modal screens
  - Implement scaleInWithFade() with 0.8f initial scale
  - Implement fadeInOnly() with LinearEasing


  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_


- [x] 2.2 Create exit transition functions


  - Implement slideOutToLeft() for forward navigation
  - Implement slideOutToRight() for back navigation
  - Implement slideOutToBottom() for modal dismissal

  - Implement scaleOutWithFade() with 0.8f target scale
  - Implement fadeOutOnly() with LinearEasing
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_


- [x] 2.3 Update navigation with transition animations

  - Locate EntertainmentNavHost composable in presentation/navigation
  - Add default enterTransition and exitTransition to NavHost
  - Configure onboarding screen with fadeInOnly/fadeOutOnly
  - Configure home screen with fadeInOnly for popEnterTransition
  - Configure downloads screen with slideInFromBottom/slideOutToBottom
  - Configure settings screen with scaleInWithFade/scaleOutWithFade
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 3. Create animated website card component





  - Implement AnimatedWebsiteCard with press and favorite animations
  - Add interaction tracking and state-based animations
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 3.1 Implement card press animation


  - Create AnimatedWebsiteCard.kt in presentation/common/components
  - Add MutableInteractionSource to track press state
  - Implement scale animation from 1f to 0.95f on press using SpringDefault
  - Implement elevation animation from 4dp to 2dp on press with 150ms tween
  - Apply animations to Card modifier
  - _Requirements: 9.1, 9.2, 9.3_

- [x] 3.2 Implement favorite button animation


  - Create AnimatedFavoriteButton composable
  - Implement scale animation to 1.2f when favorited using spring with medium bouncy damping
  - Implement 360 degree rotation animation over 300ms when toggled
  - Toggle between filled and outlined heart icon based on favorite state
  - Apply red tint when favorited
  - _Requirements: 9.4, 9.5_

- [x] 4. Implement list item staggered animations




  - Update website grid to use AnimatedVisibility with staggered delays
  - Add stable keys to LazyGrid items
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 3.2_

- [x] 4.1 Add staggered animation to website grid


  - Locate WebsiteGrid composable in HomeScreen
  - Wrap each grid item with AnimatedVisibility
  - Calculate animation delay as (index * 50ms) capped at 500ms
  - Combine fadeIn with slideInVertically (initial offset: height / 4)
  - Use 300ms duration for both animations
  - Add stable key parameter using website.id
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 3.2_

- [x] 5. Create shimmer loading effect





  - Implement ShimmerEffect composable with animated gradient
  - Create ShimmerWebsiteCard placeholder component
  - Create LoadingGrid for displaying shimmer placeholders
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_


- [x] 5.1 Implement shimmer effect composable

  - Create ShimmerEffect.kt in presentation/common/animations
  - Define shimmer colors with light gray at varying alpha (0.3f, 0.5f, 0.3f)
  - Create infinite transition for gradient translation
  - Animate from 0f to 1000f over 1200ms with LinearEasing
  - Create linear gradient brush with animated offset
  - Apply brush as background to Box modifier
  - _Requirements: 10.1, 10.2, 10.4, 10.5_


- [x] 5.2 Create shimmer placeholder components


  - Implement ShimmerWebsiteCard matching actual card layout
  - Add shimmer circle for logo (80dp size)
  - Add shimmer rectangle for title (120dp x 20dp)
  - Add shimmer rectangle for subtitle (80dp x 16dp)
  - Implement LoadingGrid with 6 shimmer cards in 2-column grid
  - _Requirements: 10.3_


- [x] 5.3 Integrate shimmer loading in HomeScreen

  - Update HomeScreen to check uiState.isLoading
  - Display LoadingGrid when loading is true
  - Display WebsiteGrid when loading is false
  - _Requirements: 10.1, 10.2, 10.3_

- [x] 6. Implement animated FAB component





  - Create AnimatedDownloadFAB with pulse and visibility animations
  - Add entrance and exit animations

  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

- [x] 6.1 Create animated FAB with pulse effect

  - Create AnimatedFAB.kt in presentation/common/components
  - Implement AnimatedDownloadFAB composable with visible parameter
  - Create infinite transition for pulse animation
  - Animate scale between 1f and 1.1f over 800ms with FastOutSlowInEasing
  - Wrap FAB with AnimatedVisibility using scaleIn/scaleOut with SpringDefault
  - Apply pulse scale to FAB modifier
  - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

- [x] 7. Create animated tab bar component





  - Implement AnimatedTabBar with width and color transitions
  - Create AnimatedTab with thumbnail reveal animation
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [x] 7.1 Implement animated tab component


  - Create AnimatedTabBar.kt in presentation/common/components
  - Implement AnimatedTab composable with isActive parameter
  - Animate width from 120dp to 180dp when active using SpringDefault
  - Animate background color between surfaceVariant and primaryContainer over 150ms
  - Animate elevation from 0dp to 4dp over 150ms
  - Wrap thumbnail with AnimatedVisibility using fadeIn/expandHorizontally
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [x] 7.2 Create tab bar container


  - Implement AnimatedTabBar composable with tabs list
  - Create horizontal scrollable Row with 8dp spacing
  - Map each tab to AnimatedTab component
  - Pass activeTabId, onTabClick, and onCloseTab callbacks
  - _Requirements: 12.1, 12.5_

- [x] 8. Implement animated search bar




  - Create AnimatedSearchBar with width expansion animation
  - Add focus management and keyboard handling
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_


- [x] 8.1 Create expandable search bar

  - Create AnimatedSearchBar.kt in presentation/common/components
  - Implement AnimatedSearchBar composable with isExpanded parameter
  - Animate width from 48dp to 300dp using SpringDefault
  - Wrap TextField with AnimatedVisibility using fadeIn/expandHorizontally
  - Add FocusRequester and request focus when expanded
  - Clear focus and hide keyboard when collapsed
  - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

- [x] 9. Create animated empty state component






  - Implement AnimatedEmptyState with bouncing icon
  - Add fade-in and slide-up entrance animation
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [x] 9.1 Implement empty state with animated icon


  - Create AnimatedEmptyState.kt in presentation/common/components
  - Implement AnimatedEmptyState composable with icon, title, message parameters
  - Create infinite transition for icon bounce
  - Animate scale between 1f and 1.1f over 1500ms with FastOutSlowInEasing
  - Wrap content with AnimatedVisibility for entrance
  - Combine fadeIn with slideInVertically (initial offset: height / 4) over 300ms
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [x] 9.2 Create specific empty state variants

  - Implement NoFavoritesEmptyState with heart icon
  - Implement NoDownloadsEmptyState with download icon
  - Implement NoSessionsEmptyState with bookmark icon
  - Implement NoTabsEmptyState with tab icon
  - _Requirements: 11.1, 11.2_

- [x] 10. Implement animated snackbar





  - Create AnimatedSnackbar with slide-up animation
  - Add type-based styling and icons

  - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_

- [x] 10.1 Create animated snackbar component

  - Create AnimatedSnackbar.kt in presentation/common/components
  - Define SnackbarType enum (SUCCESS, ERROR, INFO, WARNING)
  - Implement AnimatedSnackbar composable with message and type parameters
  - Wrap Snackbar with AnimatedVisibility
  - Animate with slideInVertically from bottom using SpringDefault
  - Combine with fadeIn animation
  - Apply type-based container colors (primaryContainer, errorContainer, tertiaryContainer, surfaceVariant)
  - Add type-based icons (CheckCircle, Error, Warning, Info)
  - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_

- [x] 11. Optimize application startup performance





  - Implement lazy initialization with background coroutines
  - Move non-critical initialization off main thread
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_


- [x] 11.1 Refactor Application class for lazy initialization

  - Open EntertainmentBrowserApp.kt
  - Create applicationScope with SupervisorJob and Dispatchers.Default
  - Move ad blocker initialization to background coroutine with Dispatchers.IO
  - Move database prepopulation to background coroutine with Dispatchers.IO
  - Move tab cleanup scheduling to background coroutine with Dispatchers.Default
  - Keep only critical setup on main thread (logging setup)
  - Add onTerminate() to cancel applicationScope
  - _Requirements: 1.1, 1.2, 1.3, 1.4_


- [x] 11.2 Implement WebView preloading

  - Add coroutine to preload WebView after 1 second delay
  - Launch on Dispatchers.Main to warm up WebView
  - Call WebViewPool.obtain() to initialize first instance
  - _Requirements: 1.5_

- [x] 12. Add database indexes for query optimization





  - Update Room entities with index annotations
  - Increment database version and add migration

  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 12.1 Add indexes to WebsiteEntity

  - Open WebsiteEntity.kt in data/local/entity
  - Add @Entity indices parameter with Index for "category"
  - Add Index for "isFavorite"
  - Add Index for "name"
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_


- [x] 12.2 Add indexes to TabEntity

  - Open TabEntity.kt in data/local/entity
  - Add @Entity indices parameter with Index for "isActive"
  - Add Index for "timestamp"
  - _Requirements: 2.5_


- [x] 12.3 Update database version and migration

  - Open AppDatabase.kt
  - Increment version number
  - Add migration to create indexes
  - _Requirements: 2.1, 2.2, 2.5_

- [x] 13. Optimize database bulk operations





  - Add insertAll method to DAOs for bulk inserts
  - Update repository to use bulk operations

  - _Requirements: 2.4_

- [x] 13.1 Add bulk insert methods to DAOs

  - Open WebsiteDao.kt
  - Add @Insert method insertAll(websites: List<WebsiteEntity>) with OnConflictStrategy.REPLACE
  - Open TabDao.kt (if exists)
  - Add @Insert method insertAll(tabs: List<TabEntity>) with OnConflictStrategy.REPLACE
  - _Requirements: 2.4_


- [x] 13.2 Update repository to use bulk inserts

  - Open WebsiteRepositoryImpl.kt
  - Replace forEach loop with single insertAll() call in prepopulateWebsites()
  - Wrap in single transaction if not already
  - _Requirements: 2.4_

- [x] 14. Implement WebView pool for reuse







  - Create WebViewPool object with pool management
  - Update CustomWebView to use pool
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_


- [x] 14.1 Create WebView pool implementation

  - Create WebViewPool.kt in util package
  - Implement object with ConcurrentLinkedQueue<WebView>
  - Define MAX_POOL_SIZE constant as 3
  - Implement obtain(context) to get from pool or create new
  - Implement recycle(webView) to clear and return to pool if under max size
  - Implement clear() to destroy all pooled WebViews
  - Implement private createWebView(context) with performance settings
  - _Requirements: 4.1, 4.2, 4.4, 4.5_



- [x] 14.2 Configure WebView performance settings


  - In createWebView(), enable javaScriptEnabled
  - Enable domStorageEnabled
  - Set cacheMode to LOAD_DEFAULT
  - Set renderPriority to HIGH
  - Enable setEnableSmoothTransition
  - _Requirements: 4.3_



- [x] 14.3 Update CustomWebView to use pool

  - Open CustomWebView.kt
  - Replace WebView creation with WebViewPool.obtain(context)
  - Add DisposableEffect to call WebViewPool.recycle() on dispose
  - _Requirements: 4.1, 4.4_

- [x] 15. Configure Coil for image caching





  - Create ImageLoadingModule with optimized ImageLoader
  - Configure memory and disk cache limits
  - _Requirements: 5.1, 5.2, 5.3_


- [x] 15.1 Create image loading module

  - Create ImageLoadingModule.kt in di package
  - Annotate with @Module and @InstallIn(SingletonComponent::class)
  - Create @Provides @Singleton function for ImageLoader
  - Configure MemoryCache with maxSizePercent(0.25)
  - Configure DiskCache with directory "image_cache" and maxSizeBytes 50MB
  - Add SvgDecoder.Factory() to components
  - Enable crossfade with 150ms duration
  - Set respectCacheHeaders to false
  - _Requirements: 5.1, 5.2, 5.3_

- [x] 16. Create cache management utilities





  - Implement CacheManager for periodic cleanup
  - Implement BitmapManager for image optimization
  - _Requirements: 5.4, 5.5_

- [x] 16.1 Implement cache manager


  - Create CacheManager.kt in util package
  - Annotate class with @Singleton
  - Inject ApplicationContext
  - Implement clearOldCache(maxAgeMillis) to delete files older than 7 days
  - Implement getCacheSize() to calculate total cache size
  - Implement clearAllCache() to delete and recreate cache directory
  - Use withContext(Dispatchers.IO) for all operations
  - _Requirements: 5.4_


- [x] 16.2 Implement bitmap manager

  - Create BitmapManager.kt in util package
  - Implement object with decodeSampledBitmap(file, reqWidth, reqHeight)
  - Implement calculateInSampleSize() to compute sample size
  - Implement compressBitmap(bitmap, outputFile) using WebP format at 80% quality
  - _Requirements: 5.5_


- [x] 16.3 Schedule periodic cache cleanup

  - Open EntertainmentBrowserApp.kt
  - Inject CacheManager
  - Add coroutine to call cacheManager.clearOldCache() on startup
  - _Requirements: 5.4_

- [x] 17. Add immutable annotations to domain models




  - Mark all domain models with @Immutable annotation
  - Ensure data classes are truly immutable
  - _Requirements: 3.3_

- [x] 17.1 Update domain models with @Immutable


  - Open Website.kt in domain/model
  - Add @Immutable annotation to Website data class
  - Open Category.kt (or enum in Website.kt)
  - Add @Immutable annotation to Category enum
  - Repeat for Tab, Download, Session, and other domain models
  - _Requirements: 3.3_

- [x] 18. Optimize Compose recomposition





  - Create performance utilities for stable computations
  - Add derivedStateOf for filtered lists

  - _Requirements: 3.2, 3.4_

- [x] 18.1 Create performance utility functions

  - Create PerformanceUtils.kt in presentation/common/performance
  - Implement rememberStable<T>(key, calculation) using remember
  - Implement LogCompositions(tag) for debug builds only
  - Use SideEffect to track recomposition count
  - _Requirements: 3.4_


- [x] 18.2 Optimize website grid filtering

  - Open HomeScreen.kt or relevant screen with filtering
  - Wrap filter operations with remember and derivedStateOf
  - Example: val favoriteWebsites by remember(websites) { derivedStateOf { websites.filter { it.isFavorite } } }
  - _Requirements: 3.2_

- [x] 19. Enable R8 full mode and resource optimization





  - Update gradle.properties with R8 settings
  - Configure build.gradle.kts for resource shrinking

  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 19.1 Enable R8 full mode

  - Open gradle.properties
  - Add android.enableR8.fullMode=true
  - Add android.enableResourceOptimizations=true
  - _Requirements: 6.1, 6.2_


- [x] 19.2 Configure resource shrinking

  - Open app/build.gradle.kts
  - In release buildType, verify isShrinkResources = true
  - Verify isMinifyEnabled = true
  - Add resourceConfigurations += listOf("en") to keep only English
  - _Requirements: 6.2, 6.3_

- [x] 19.3 Configure ABI splits


  - In app/build.gradle.kts, add splits block
  - Enable ABI splits with isEnable = true
  - Include "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
  - Set isUniversalApk = false
  - _Requirements: 6.5_

- [x] 20. Optimize ProGuard rules




  - Update proguard-rules.pro with aggressive optimization
  - Remove debug logging in release builds
  - _Requirements: 6.3, 6.4_


- [x] 20.1 Add ProGuard optimization rules

  - Open app/proguard-rules.pro
  - Add -optimizationpasses 5
  - Add -assumenosideeffects for android.util.Log to remove debug logging
  - Add -assumenosideeffects for timber.log.Timber to remove Timber logging
  - Add -keep rules for Compose runtime
  - Add -dontwarn for Kotlin and Compose
  - Add -keep rules for Room entities and DAOs
  - _Requirements: 6.3, 6.4_

- [x] 21. Implement background work optimization





  - Update TabCleanupWorker with proper constraints
  - Configure WorkManager for efficiency
  - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5_


- [x] 21.1 Optimize tab cleanup worker

  - Open TabCleanupWorker.kt in data/worker
  - Add Constraints.Builder with setRequiresBatteryNotLow(true)
  - Add setRequiresDeviceIdle(true) constraint
  - Set repeatInterval to 7 days
  - Add exponential backoff policy
  - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5_

- [x] 22. Add performance monitoring for debug builds







  - Create ProfilerConfig for StrictMode and logging
  - Integrate with Application class
  - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5_


- [x] 22.1 Create profiler configuration

  - Create debug source set: app/src/debug/kotlin/com/entertainmentbrowser/debug
  - Create ProfilerConfig.kt object
  - Implement enableStrictMode() with ThreadPolicy detecting disk reads, writes, and network
  - Add VmPolicy detecting leaked SQLite objects, closeable objects, and activities
  - Implement logPerformance(tag, block) to measure execution time
  - _Requirements: 17.1, 17.2, 17.3_


- [x] 22.2 Integrate profiler in Application class


  - Open EntertainmentBrowserApp.kt
  - Add if (BuildConfig.DEBUG) check
  - Call ProfilerConfig.enableStrictMode() in onCreate
  - Wrap initialization with ProfilerConfig.logPerformance()
  - _Requirements: 17.1, 17.2, 17.5_


- [x] 22.3 Add recomposition logging utility

  - In PerformanceUtils.kt, update LogCompositions to use Timber
  - Track recomposition count with remember and SideEffect
  - Only log in debug builds
  - _Requirements: 17.3, 17.4_

- [x] 23. Update HomeScreen with all animations





  - Integrate all animated components into HomeScreen
  - Add loading states, empty states, and interactions

  - _Requirements: 8.1, 8.2, 8.3, 10.1, 10.2, 11.1_

- [x] 23.1 Update HomeScreen with animated components


  - Open HomeScreen.kt
  - Replace WebsiteCard with AnimatedWebsiteCard
  - Add LoadingGrid for loading state
  - Add AnimatedEmptyState for empty state
  - Add AnimatedSearchBar to TopAppBar actions
  - Add AnimatedSnackbarHost for snackbar messages
  - Ensure all LazyGrid items use stable keys
  - _Requirements: 8.1, 8.2, 8.3, 10.1, 10.2, 11.1_

- [x] 24. Update WebViewScreen with animations





  - Add AnimatedDownloadFAB for video downloads
  - Integrate pull-to-refresh animation
  - _Requirements: 13.1, 13.2, 13.3_

- [x] 24.1 Add animated FAB to WebViewScreen


  - Open WebViewScreen.kt
  - Add state for video detection
  - Add AnimatedDownloadFAB with visible parameter based on video detection
  - Position FAB in Scaffold
  - _Requirements: 13.1, 13.2, 13.3_


- [x] 24.2 Add pull-to-refresh animation

  - Add rememberPullToRefreshState() in WebViewScreen
  - Wrap content with nestedScroll modifier
  - Add PullToRefreshContainer aligned to top center
  - Connect refresh state to ViewModel
  - _Requirements: 13.1_

- [x] 25. Verify and test all implementations





  - Run app and verify startup time improvement
  - Test all animations for smoothness
  - Verify database query performance
  - Check memory usage and APK size

  - _Requirements: All_

- [x] 25.1 Performance verification


  - Build release APK and measure size (should be < 10MB)
  - Use Android Profiler to measure cold start time (should be < 1s)
  - Use Profiler to measure database query time (should be < 10ms)
  - Monitor memory usage during normal operation (should be < 100MB)
  - Verify list scrolling maintains 60 FPS
  - _Requirements: 1.1, 2.1, 5.1, 6.1, 3.1_


- [ ] 25.2 Animation verification
  - Test screen transitions (forward, back, modal)
  - Test card press animations
  - Test favorite button animation
  - Test staggered list animations
  - Test shimmer loading effect
  - Test FAB pulse animation
  - Test tab switching animations
  - Test search bar expansion
  - Test empty state animations
  - Test snackbar animations
  - _Requirements: 7.1, 8.1, 9.1, 10.1, 11.1, 12.1, 13.1, 14.1, 15.1_


- [ ] 25.3 Integration testing
  - Test navigation flow with animations
  - Test WebView pool reuse
  - Test cache cleanup
  - Test background work scheduling
  - Verify no crashes or performance regressions
  - _Requirements: All_
