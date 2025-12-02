# Design Document

## Overview

The Entertainment Browser is built using modern Android development practices with Jetpack Compose for UI, MVVM architecture with Clean Architecture principles, and Hilt for dependency injection. The app provides a unified interface for accessing 45+ entertainment websites, managing browsing tabs, downloading videos, and organizing content through favorites and sessions.

### Technology Stack

- **UI Framework**: Jetpack Compose 1.7.x with Material 3
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt 2.50 (androidx.hilt:1.1.0)
- **Database**: Room 2.6.1 with Flow-based reactive queries
- **Networking**: OkHttp 4.12.0
- **Image Loading**: Coil 2.5.0
- **Download Management**: Fetch library 3.4.1
- **Storage**: DataStore for preferences, MediaStore for downloads
- **Coroutines**: Kotlinx Coroutines 1.8.0
- **Navigation**: Jetpack Navigation Compose 2.7.6

**Note**: Versions based on AndroidX stable channel as of October 2025. Use Compose BOM to manage dependencies:
```kotlin
implementation(platform("androidx.compose:compose-bom:2024.10.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
```

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Presentation Layer                    │
│  (Jetpack Compose UI + ViewModels)                          │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                        Domain Layer                          │
│  (Use Cases + Domain Models)                                │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                         Data Layer                           │
│  (Repositories + Room Database + DataStore + Fetch)         │
└─────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.entertainmentbrowser
├── di/                          # Hilt modules
├── core/                        # Shared utilities
│   ├── result/                 # Result sealed class for error handling
│   └── constants/              # App-wide constants
├── data/
│   ├── local/
│   │   ├── dao/                # Room DAOs
│   │   ├── entity/             # Room entities
│   │   └── database/           # Database instance
│   ├── repository/             # Repository implementations
│   └── datastore/              # DataStore preferences
├── domain/
│   ├── model/                  # Domain models
│   ├── repository/             # Repository interfaces
│   └── usecase/                # Use cases
├── presentation/
│   ├── common/                 # Reusable composables
│   │   ├── ErrorComposable
│   │   ├── ShimmerLoading
│   │   └── GradientBackground
│   ├── onboarding/             # Onboarding screens
│   ├── home/                   # Home screen with categories
│   ├── favorites/              # Favorites screen
│   ├── webview/                # WebView screen
│   ├── downloads/              # Downloads management
│   ├── tabs/                   # Tab management
│   ├── sessions/               # Session management
│   ├── settings/               # Settings screen
│   └── theme/                  # Material 3 theme
└── util/                       # Utility classes
```

## Components and Interfaces

### 1. Onboarding Flow

#### Components


## Result Sealed Class for Error Handling

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Extension functions
fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

fun <T> Result<T>.onError(action: (String) -> Unit): Result<T> {
    if (this is Result.Error) action(message)
    return this
}
```

## Video Download Flow Sequence

```
┌──────┐                ┌──────────────┐         ┌──────────────────┐         ┌──────┐         ┌──────┐
│ User │                │ WebViewScreen│         │ DownloadViewModel│         │ Fetch│         │ Room │
└──┬───┘                └──────┬───────┘         └────────┬─────────┘         └───┬──┘         └───┬──┘
   │                           │                          │                       │                │
   │ Tap Download FAB          │                          │                       │                │
   ├──────────────────────────>│                          │                       │                │
   │                           │                          │                       │                │
   │                           │ startDownload(url)       │                       │                │
   │                           ├─────────────────────────>│                       │                │
   │                           │                          │                       │                │
   │                           │                          │ enqueue(request)      │                │
   │                           │                          ├──────────────────────>│                │
   │                           │                          │                       │                │
   │                           │                          │<──────────────────────┤                │
   │                           │                          │   progress callback   │                │
   │                           │                          │                       │                │
   │                           │                          │ update DownloadItem   │                │
   │                           │                          ├───────────────────────────────────────>│
   │                           │                          │                       │                │
   │                           │                          │<───────────────────────────────────────┤
   │                           │                          │      Flow update      │                │
   │                           │                          │                       │                │
   │                           │<─────────────────────────┤                       │                │
   │                           │   StateFlow refresh      │                       │                │
   │                           │                          │                       │                │
   │<──────────────────────────┤                          │                       │                │
   │   UI Update               │                          │                       │                │
```

## Common Composables

### Shimmer Loading

```kotlin
@Composable
fun ShimmerWebsiteCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shimmerEffect()
    ) {
        // Shimmer placeholder content
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition()
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000)
        )
    )
    
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E1E1E),
                Color(0xFF2E2E2E),
                Color(0xFF1E1E1E)
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    )
        .onGloballyPositioned { size = it.size }
}
```

### Pull to Refresh

```kotlin
@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel = hiltViewModel()) {
    val downloads by viewModel.downloads.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshDownloads() }
    )
    
    Box(Modifier.pullRefresh(pullRefreshState)) {
        LazyColumn {
            items(downloads, key = { it.id }) { download ->
                DownloadItem(download)
            }
        }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
```

### Haptic Feedback Integration

```kotlin
@Composable
fun WebsiteCard(
    website: Website,
    onFavoriteToggle: (Int) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val view = LocalView.current
    
    Card(
        modifier = Modifier.clickable {
            if (hapticEnabled) {
                view.performHapticFeedback(HapticFeedbackConstants.CLICK)
            }
            onFavoriteToggle(website.id)
        }
    ) {
        // Card content
    }
}
```

## WebView Enhancements

### Custom WebView with Fullscreen Support

```kotlin
@Composable
fun CustomWebView(
    url: String,
    onVideoDetected: (String) -> Unit
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    safeBrowsingEnabled = true
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onShowCustomView(
                        view: View,
                        callback: CustomViewCallback
                    ) {
                        // Handle fullscreen video
                        // Lock orientation, hide system UI
                    }
                    
                    override fun onHideCustomView() {
                        // Restore normal view
                    }
                }
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        // Enforce HTTPS
                        val url = request.url.toString()
                        if (!url.startsWith("https://") && !url.startsWith("http://localhost")) {
                            return true
                        }
                        return false
                    }
                    
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        val url = request.url.toString()
                        if (url.matches(VIDEO_PATTERN)) {
                            onVideoDetected(url)
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }
                
                webView = this
                loadUrl(url)
            }
        }
    )
}
```

## Background Task Management

### Tab Cleanup Worker

```kotlin
@HiltWorker
class TabCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tabDao: TabDao
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            tabDao.deleteOldTabs(cutoffTime)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// Schedule in Application class
class EntertainmentBrowserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val cleanupRequest = PeriodicWorkRequestBuilder<TabCleanupWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresDeviceIdle(true)
                    .build()
            )
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "tab_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }
}
```

## Testing and Validation

### Unit Tests

```kotlin
@Test
fun `getWebsitesByCategory returns correct websites`() = runTest {
    // Arrange
    val mockDao = mock<WebsiteDao>()
    val repository = WebsiteRepositoryImpl(mockDao)
    val expectedWebsites = listOf(
        WebsiteEntity(1, "Netflix", "https://netflix.com", "STREAMING", "", "", "", false, 0)
    )
    whenever(mockDao.getByCategory("STREAMING")).thenReturn(flowOf(expectedWebsites))
    
    // Act
    val result = repository.getByCategory(Category.STREAMING).first()
    
    // Assert
    assertEquals(1, result.size)
    assertEquals("Netflix", result[0].name)
}
```

### UI Tests

```kotlin
@Test
fun homeScreen_displaysWebsitesInGrid() {
    composeTestRule.setContent {
        HomeScreen()
    }
    
    // Verify grid layout
    composeTestRule.onNodeWithTag("website_grid").assertIsDisplayed()
    
    // Verify website cards
    composeTestRule.onNodeWithText("Netflix").assertIsDisplayed()
    composeTestRule.onNodeWithText("Disney+").assertIsDisplayed()
}

@Test
fun searchBar_filtersWebsitesWithDebounce() {
    composeTestRule.setContent {
        HomeScreen()
    }
    
    // Type in search field
    composeTestRule.onNodeWithTag("search_field")
        .performTextInput("Netflix")
    
    // Wait for debounce (300ms)
    composeTestRule.mainClock.advanceTimeBy(300)
    
    // Verify filtered results
    composeTestRule.onNodeWithText("Netflix").assertIsDisplayed()
    composeTestRule.onNodeWithText("Disney+").assertDoesNotExist()
}

@Test
fun onboarding_completesAndNavigatesToHome() {
    composeTestRule.setContent {
        OnboardingScreen()
    }
    
    // Navigate through onboarding
    repeat(3) {
        composeTestRule.onNodeWithText("Next").performClick()
    }
    
    composeTestRule.onNodeWithText("Start Exploring").performClick()
    
    // Verify navigation to home
    composeTestRule.onNodeWithTag("home_screen").assertIsDisplayed()
}
```

### Integration Tests

```kotlin
@Test
fun downloadFlow_createsAndTracksDownload() = runTest {
    // Create download
    val url = "https://example.com/video.mp4"
    downloadRepository.startDownload(url, "video.mp4")
    
    // Verify download created
    val downloads = downloadRepository.observeDownloads().first()
    assertEquals(1, downloads.size)
    assertEquals(DownloadStatus.QUEUED, downloads[0].status)
    
    // Simulate progress
    // ... (mock Fetch callbacks)
    
    // Verify completion
    val completedDownloads = downloadRepository.observeDownloads().first()
    assertEquals(DownloadStatus.COMPLETED, completedDownloads[0].status)
}
```

### Test Coverage Target

- **Unit Tests**: 70% coverage minimum
- **UI Tests**: All critical user flows (onboarding, browsing, downloading, tab management)
- **Integration Tests**: End-to-end scenarios with real database
- **Tool**: JaCoCo for coverage reporting

## Build and Deployment

### Build Configuration

```kotlin
android {
    compileSdk = 35
    
    defaultConfig {
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}
```

### ProGuard Rules

```proguard
# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Fetch
-keep class com.tonyofrancis.fetch2.** { *; }
-dontwarn com.tonyofrancis.fetch2.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.entertainmentbrowser.**$$serializer { *; }
-keepclassmembers class com.entertainmentbrowser.** {
    *** Companion;
}
-keepclasseswithmembers class com.entertainmentbrowser.** {
    kotlinx.serialization.KSerializer serializer(...);
}
```

### Deployment

- **Format**: Android App Bundle (AAB) for Play Store
- **Signing**: Use release keystore with strong password
- **Size Target**: Under 15MB (use App Bundle to reduce size)
- **Testing**: Test on devices with API 24, 30, 33, and 35

## Assumptions and Risks

### Assumptions

1. No remote data syncing required - all data stored locally
2. Users have stable internet connection for browsing and downloading
3. Most entertainment websites are mobile-friendly
4. Video detection works for standard HTML5 video elements
5. Users understand DRM limitations

### Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Fetch library maintenance stalls | High | Fallback to Android DownloadManager if needed |
| Website structure changes break video detection | Medium | Implement multiple detection strategies (JS injection + URL interception) |
| WebView security vulnerabilities | High | Regular security audits, disable file access, enforce HTTPS |
| Performance issues on low-end devices | Medium | Implement pagination, lazy loading, image caching |
| Play Store policy violations | High | Clear DRM warnings, no copyrighted content bundled, privacy policy |

## Security Considerations

### WebView Security

```kotlin
webView.settings.apply {
    // Disable file access
    allowFileAccess = false
    allowContentAccess = false
    
    // Enable safe browsing
    safeBrowsingEnabled = true
    
    // Disable geolocation
    setGeolocationEnabled(false)
    
    // Clear cache on exit
    cacheMode = WebSettings.LOAD_NO_CACHE
}
```

### Input Sanitization

```kotlin
fun sanitizeSearchQuery(query: String): String {
    return query
        .replace(Regex("[<>\"']"), "")
        .take(100)
        .trim()
}
```

### Download Validation

```kotlin
fun validateDownloadUrl(url: String): Boolean {
    return url.startsWith("https://") &&
           url.matches(Regex(".*\\.(mp4|webm|m3u8|mpd)$")) &&
           !isDrmProtected(url)
}
```

## Accessibility Enhancements

### Semantic Content Descriptions

```kotlin
@Composable
fun WebsiteCard(website: Website) {
    Card(
        modifier = Modifier.semantics {
            contentDescription = "${website.name}, ${website.category}, ${website.description}"
            role = Role.Button
        }
    ) {
        // Card content
    }
}
```

### Touch Target Sizes

```kotlin
IconButton(
    onClick = { /* ... */ },
    modifier = Modifier
        .size(48.dp) // Minimum touch target
        .semantics { contentDescription = "Add to favorites" }
) {
    Icon(Icons.Default.Bookmark, contentDescription = null)
}
```

### TalkBack Support

```kotlin
@Composable
fun DownloadProgressItem(download: DownloadItem) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "${download.filename}, ${download.progress}% downloaded"
        }
    ) {
        Text(download.filename)
        LinearProgressIndicator(progress = download.progress / 100f)
    }
}
```

## Documentation and Resources

### External References

- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Material 3**: https://m3.material.io/
- **Hilt**: https://dagger.dev/hilt/
- **Room**: https://developer.android.com/training/data-storage/room
- **Fetch Library**: https://github.com/tonyofrancis/Fetch
- **Coil**: https://coil-kt.github.io/coil/
- **WebView Best Practices**: https://developer.android.com/guide/webapps/webview

### Change Log

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Oct 18, 2025 | Initial design document |
| 1.1 | Oct 18, 2025 | Updated technology stack versions, added security section, enhanced testing strategy |
