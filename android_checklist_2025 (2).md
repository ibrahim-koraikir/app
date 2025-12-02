# Android Entertainment Browser - 2025 Development Checklist

## Phase 1: Project Foundation & Modern Setup

### 1.1 Dependencies (build.gradle.kts) - 2025 Versions
```kotlin
// UI Framework
implementation("androidx.compose.ui:ui:1.6.0")
implementation("androidx.compose.material3:material3:1.2.0")
implementation("androidx.activity:activity-compose:1.8.2")

// Architecture
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
implementation("androidx.navigation:navigation-compose:2.7.6")

// Dependency Injection
implementation("com.google.dagger:hilt-android:2.50")
ksp("com.google.dagger:hilt-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

// Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Networking
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("io.coil-kt:coil-compose:2.5.0") // Image loading

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

// WebView with Compose
implementation("com.google.accompanist:accompanist-webview:0.34.0")

// Serialization (Modern alternative to Gson)
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

// Datastore (Modern SharedPreferences)
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

### 1.2 Permissions (AndroidManifest.xml) - Updated for Android 13+
```xml
<!-- Network -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Media Storage (Android 13+ Scoped Storage) -->
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" 
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"
    android:maxSdkVersion="32" />

<!-- Legacy storage (Android 10-12) -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="29" />

<!-- NOT NEEDED - Remove REQUEST_INSTALL_PACKAGES unless specific use case -->
```

### 1.3 Material Design 3 Theme Setup (Jetpack Compose)
- [ ] Create `ui/theme/Color.kt` with Material You dynamic colors
- [ ] Create `ui/theme/Theme.kt` with dark theme support
- [ ] Use `dynamicDarkColorScheme()` for Android 12+ theming
- [ ] Set accent red: `primary = Color(0xFFfd1d1d)`
- [ ] Implement dark blue gradient backgrounds with `Brush.verticalGradient()`

### 1.4 Configure MVVM with Clean Architecture
- [ ] Enable Hilt in Application class: `@HiltAndroidApp`
- [ ] Create base `ViewModel` classes with Hilt injection
- [ ] Set up Repository pattern with `@Inject constructor`
- [ ] Configure Navigation Compose graph
- [ ] Create `di/` package for Hilt modules

**✅ VALIDATION CHECKPOINT PHASE 1:**
- [ ] Build succeeds: `./gradlew assembleDebug`
- [ ] Hilt dependency injection working
- [ ] Compose preview renders correctly
- [ ] Material 3 theme applies
- [ ] Navigation host set up

---

## Phase 2: Onboarding Flow (Jetpack Compose)

### 2.1 Data Models
```kotlin
data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val features: List<Feature>? = null,
    val imageRes: Int? = null
)

data class Feature(
    val icon: String, // Emoji or icon name
    val title: String,
    val description: String
)
```

### 2.2 Onboarding Screens (4 Composables)
- [ ] `WelcomeScreen()` - Hero screen with gradient + "Get Started" CTA
- [ ] `FeaturesScreen()` - 3 feature cards with icons
- [ ] `PermissionsScreen()` - Storage + Notifications with runtime requests
- [ ] `FinalScreen()` - Summary features + "Start Exploring" button

### 2.3 Onboarding Container
- [ ] Use `HorizontalPager` from Compose Foundation
- [ ] Implement page indicators with `PagerState`
- [ ] Save completion to DataStore: `dataStore.edit { it[ONBOARDING_COMPLETED] = true }`
- [ ] Navigate to `MainActivity` on completion

### 2.4 Splash Screen (Android 12+ Native)
- [ ] Use Android 12+ Splash Screen API (NO custom Activity needed)
- [ ] Configure in `themes.xml`: `<item name="android:windowSplashScreenAnimatedIcon">@drawable/ic_launcher</item>`
- [ ] Check onboarding status in `MainActivity.onCreate()`

**✅ VALIDATION CHECKPOINT PHASE 2:**
- [ ] Splash screen displays correctly (Android 12+ native)
- [ ] Onboarding swipes work smoothly
- [ ] Permission requests trigger correctly (Android 13+ flow)
- [ ] DataStore saves completion status
- [ ] Navigation to MainActivity works
- [ ] Back button handling prevents skipping onboarding

---

## Phase 3: Entertainment Data Layer

### 3.1 Room Database Models
```kotlin
@Entity(tableName = "websites")
data class Website(
    @PrimaryKey val id: Int,
    val name: String,
    val url: String,
    val category: Category,
    val logoUrl: String,
    val description: String,
    val buttonColor: String, // Hex color
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0
)

enum class Category {
    STREAMING, TV_SHOWS, BOOKS, VIDEO_PLATFORMS
}
```

### 3.2 Room Database & DAO
```kotlin
@Dao
interface WebsiteDao {
    @Query("SELECT * FROM websites WHERE category = :category ORDER BY sortOrder")
    fun getByCategory(category: Category): Flow<List<Website>>
    
    @Query("SELECT * FROM websites WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<Website>>
    
    @Query("SELECT * FROM websites WHERE name LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<Website>>
    
    @Update
    suspend fun update(website: Website)
}
```

### 3.3 Repository with Prepopulated Data
- [ ] Create `WebsiteRepository` with `@Inject` Hilt constructor
- [ ] Prepopulate database on first launch with 50+ sites
- [ ] Implement search with debounce (use `Flow.debounce(300)`)
- [ ] Add category filtering logic

### 3.4 Prepopulated Websites (Streamlined)
**Streaming (15 sites):** Netflix, Disney+, Prime Video, Hulu, Max, Apple TV+, Paramount+, Peacock, Crunchyroll, Funimation, Tubi, Pluto TV, Plex, YouTube Movies, Vudu

**TV Shows (10 sites):** JustWatch, Rotten Tomatoes, IMDb, TVTime, Reelgood, Simkl, Trakt, TheTVDB, Metacritic, TV Guide

**Books (10 sites):** Goodreads, Audible, Libby, Kindle, Apple Books, Google Books, Scribd, Project Gutenberg, LibraryThing, StoryGraph

**Video Platforms (10 sites):** YouTube, Twitch, Vimeo, TikTok, Dailymotion, Rumble, Odysee, BitChute, PeerTube, LBRY

**✅ VALIDATION CHECKPOINT PHASE 3:**
- [ ] Database prepopulation works on first launch
- [ ] All 45+ websites load correctly
- [ ] Category filtering returns correct results
- [ ] Search returns relevant results with debounce
- [ ] Flow-based data updates UI reactively

---

## Phase 4: Main UI with Compose Navigation

### 4.1 Navigation Setup
```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object Downloads : Screen("downloads")
    object Tabs : Screen("tabs")
}
```

### 4.2 Main Screen with Bottom Nav
```kotlin
@Composable
fun MainScreen() {
    Scaffold(
        bottomBar = { BottomNavigationBar() },
        topBar = { SearchBar() }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Home.route) {
            composable(Screen.Home.route) { HomeScreen() }
            // ...
        }
    }
}
```

### 4.3 Home Screen (Category Tabs)
- [ ] Use `TabRow` with 4 categories
- [ ] Display websites in `LazyVerticalGrid(columns = 2)`
- [ ] Each card: Logo (Coil), Name, Description, Action button
- [ ] Click card → Navigate to `WebViewScreen(url)`
- [ ] Long press → Show context menu (Add to Favorites)

### 4.4 Favorites Screen
- [ ] Display `LazyVerticalGrid` with favorited sites
- [ ] Toggle favorite with star icon (update via ViewModel)
- [ ] Show empty state: "No favorites yet" with illustration

### 4.5 Search Implementation
- [ ] Use `TextField` in TopAppBar with `onValueChange`
- [ ] Debounce search queries with `snapshotFlow` + `debounce(300)`
- [ ] Filter results across all categories
- [ ] Show search results in same grid layout

**✅ VALIDATION CHECKPOINT PHASE 4:**
- [ ] Bottom navigation switches between screens
- [ ] Category tabs filter websites correctly
- [ ] Website grid displays with 2 columns
- [ ] Search filters results with 300ms debounce
- [ ] Favorites toggle persists to database
- [ ] Empty states display correctly
- [ ] Performance smooth with 50+ items

---

## Phase 5: WebView with Advanced Features

### 5.1 WebView Composable
```kotlin
@Composable
fun WebViewScreen(url: String) {
    val state = rememberWebViewState(url)
    val navigator = rememberWebViewNavigator()
    
    Scaffold(
        topBar = { WebViewToolbar(state, navigator) }
    ) {
        WebView(
            state = state,
            navigator = navigator,
            onCreated = { webView ->
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
            }
        )
    }
}
```

### 5.2 Video Detection System
- [ ] Inject JavaScript to detect `<video>` elements
- [ ] Use `WebViewClient.shouldInterceptRequest()` to detect video URLs
- [ ] Pattern match: `.mp4`, `.webm`, `.m3u8`, `.mpd`
- [ ] Show floating download FAB when video detected
- [ ] Extract video metadata (title, duration, format)

### 5.3 DRM Content Handling
**CRITICAL:** Most streaming platforms use Widevine DRM - downloads won't work
- [ ] Detect DRM: Check for `eme`, `widevine`, `playready` in page source
- [ ] Show clear warning: "This content is DRM-protected and cannot be downloaded"
- [ ] List known DRM sites: Netflix, Disney+, Hulu, Prime Video, HBO Max
- [ ] Only allow downloads from: YouTube, Vimeo, Dailymotion, etc.

### 5.4 WebView Toolbar Features
- [ ] Back/Forward navigation with `navigator.navigateBack()`
- [ ] Refresh button
- [ ] Share URL with `Intent.ACTION_SEND`
- [ ] Open in external browser
- [ ] Show progress indicator during page load

**✅ VALIDATION CHECKPOINT PHASE 5:**
- [ ] WebView loads all test URLs correctly
- [ ] JavaScript injection works for video detection
- [ ] DRM warning displays for protected content
- [ ] Toolbar actions work (back, forward, refresh)
- [ ] Video download FAB appears on compatible sites
- [ ] External browser launch works
- [ ] Progress indicator shows during loading

---

## Phase 6: Download Manager with Fetch Library

### 6.1 Add Fetch Dependencies
```kotlin
// Add to settings.gradle or build.gradle (project level)
repositories {
    maven { url 'https://jitpack.io' }
}

// Add to build.gradle (app level)
implementation "com.github.tonyofrancis.Fetch:fetch2:3.4.1"
implementation "com.github.tonyofrancis.Fetch:fetch2okhttp:3.4.1" // For OkHttp support
implementation "com.github.tonyofrancis.Fetch:fetch2rx:3.4.1" // Optional: RxJava support
```

### 6.2 Setup Fetch Configuration
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FetchModule {
    
    @Provides
    @Singleton
    fun provideFetch(@ApplicationContext context: Context): Fetch {
        val fetchConfiguration = FetchConfiguration.Builder(context)
            .setDownloadConcurrentLimit(3) // Max 3 concurrent downloads
            .setHttpDownloader(OkHttpDownloader(OkHttpClient())) // Use OkHttp
            .setNotificationManager(DefaultFetchNotificationManager(context))
            .enableLogging(BuildConfig.DEBUG)
            .enableAutoStart(true) // Resume downloads on app restart
            .setNamespace("entertainment_downloads") // Unique namespace
            .build()
        
        return Fetch.Impl.getInstance(fetchConfiguration)
    }
}
```

### 6.3 Download Models (Map Fetch Download to UI)
```kotlin
data class DownloadItemUI(
    val id: Int, // Fetch Request ID
    val url: String,
    val filename: String,
    val fileSize: Long,
    val downloadedBytes: Long,
    val progress: Int,
    val status: DownloadStatus,
    val etaInMilliseconds: Long,
    val downloadedBytesPerSecond: Long,
    val error: String? = null
)

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED, REMOVED
}

// Extension to convert Fetch Download to UI model
fun Download.toDownloadItemUI() = DownloadItemUI(
    id = id,
    url = url,
    filename = file,
    fileSize = total,
    downloadedBytes = downloaded,
    progress = progress,
    status = status.toDownloadStatus(),
    etaInMilliseconds = etaInMilliSeconds,
    downloadedBytesPerSecond = 0L
)
```

### 6.3 Save Files with MediaStore (Android 10+)
```kotlin
// NO direct file paths - use MediaStore
val contentValues = ContentValues().apply {
    put(MediaStore.Downloads.DISPLAY_NAME, filename)
    put(MediaStore.Downloads.MIME_TYPE, mimeType)
    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
}

val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
contentResolver.openOutputStream(uri)?.use { outputStream ->
    // Write download data
}
```

### 6.4 Downloads Screen
- [ ] Display `LazyColumn` with download items
- [ ] Show progress bars with `LinearProgressIndicator`
- [ ] Actions: Pause, Resume, Cancel, Delete, Open
- [ ] Group by status: Active, Completed, Failed
- [ ] Pull to refresh downloads list

### 6.5 Download Notifications
- [ ] Create notification channel: `CHANNEL_DOWNLOADS`
- [ ] Show progress notification with `NotificationCompat.Builder`
- [ ] Update progress with `setProgress(max, current, false)`
- [ ] Handle notification click → Open Downloads screen
- [ ] Show completion notification with "Open" action

**✅ VALIDATION CHECKPOINT PHASE 6:**
- [ ] WorkManager schedules downloads correctly
- [ ] Files save to MediaStore (no hardcoded paths)
- [ ] Progress updates in real-time
- [ ] Pause/resume works with HTTP Range headers
- [ ] Concurrent download limit enforced (max 3)
- [ ] Notifications show progress correctly
- [ ] Completed files accessible from Downloads folder
- [ ] Failed downloads show error messages

---

## Phase 7: Tab Management System

### 7.1 Tab Models & Repository
```kotlin
@Entity
data class TabItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val thumbnailPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)
```

### 7.2 Tab Manager with State
- [ ] Store tabs in Room database
- [ ] Maintain `currentTabId` in ViewModel
- [ ] Max 20 tabs (show warning if exceeded)
- [ ] Auto-close oldest inactive tabs when limit reached

### 7.3 Tab Grid Screen
- [ ] Display tabs in `LazyVerticalGrid(columns = 2)`
- [ ] Show thumbnail preview (capture with `Canvas.drawBitmap()`)
- [ ] Display title and URL
- [ ] Close button on each tab card
- [ ] "Close All Tabs" action in toolbar
- [ ] Tap tab → Switch to that tab's WebView

### 7.4 Tab Persistence
- [ ] Save all tabs to Room on app background (`onStop()`)
- [ ] Restore tabs on app launch from database
- [ ] Clear closed tabs older than 7 days (background cleanup)

**✅ VALIDATION CHECKPOINT PHASE 7:**
- [ ] New tabs create correctly with unique IDs
- [ ] Tab switching updates WebView instantly
- [ ] Tab grid displays thumbnails
- [ ] Close tab removes from database
- [ ] Tabs persist across app restart
- [ ] Max tab limit enforced
- [ ] Background cleanup removes old tabs

---

## Phase 8: Session Management

### 8.1 Session Models
```kotlin
@Entity
data class Session(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tabIds: String, // JSON array of tab IDs
    val createdAt: Long = System.currentTimeMillis()
)
```

### 8.2 Session Manager
- [ ] Save current tabs as named session
- [ ] Serialize tab IDs with `Json.encodeToString()`
- [ ] Restore session → Open all tabs from saved IDs
- [ ] Delete session with confirmation dialog

### 8.3 Sessions Screen
- [ ] Display `LazyColumn` with saved sessions
- [ ] Show session name, tab count, date
- [ ] Actions: Restore, Rename, Delete
- [ ] Quick restore with single tap
- [ ] Swipe to delete with confirmation

**✅ VALIDATION CHECKPOINT PHASE 8:**
- [ ] Session creation saves all current tabs
- [ ] Session restore opens all tabs correctly
- [ ] Session deletion works with confirmation
- [ ] Sessions persist across app restart
- [ ] Session list sorted by date (newest first)

---

## Phase 9: Settings & Polish

### 9.1 Settings with DataStore
```kotlin
data class AppSettings(
    val downloadOnWifiOnly: Boolean = true,
    val maxConcurrentDownloads: Int = 3,
    val autoCloseOldTabs: Boolean = true,
    val enableHapticFeedback: Boolean = true
)
```

### 9.2 Settings Screen (Compose)
- [ ] Use `PreferenceScreen` from `androidx.compose.preference` OR custom Compose
- [ ] Toggle: Download on Wi-Fi only
- [ ] Slider: Max concurrent downloads (1-5)
- [ ] Toggle: Auto-close old tabs
- [ ] Button: Clear cache (WebView cache)
- [ ] Button: Clear download history
- [ ] About section: App version, licenses, GitHub link

### 9.3 Empty States & Loading
- [ ] Create composable empty states for each screen
- [ ] Use `CircularProgressIndicator` for loading
- [ ] Skeleton loading for website cards (use `shimmer` library)
- [ ] Error states with retry buttons

### 9.4 Haptic Feedback
- [ ] Add `performHapticFeedback(HapticFeedbackType.LongPress)` on card long press
- [ ] Add feedback on favorite toggle
- [ ] Add feedback on tab close

**✅ VALIDATION CHECKPOINT PHASE 9:**
- [ ] Settings save to DataStore correctly
- [ ] Clear cache removes WebView data
- [ ] Clear history removes download records
- [ ] Empty states display correctly
- [ ] Loading states show during operations
- [ ] Haptic feedback works on interactions

---

## Phase 10: Testing & Optimization

### 10.1 Unit Tests (70% Coverage Target)
```kotlin
@Test
fun `test website repository filters by category`() {
    // Test with fake data
}

@Test
fun `test download engine handles pause resume`() {
    // Mock OkHttp client
}
```

### 10.2 UI Tests with Compose Testing
```kotlin
@Test
fun onboarding_completes_navigates_to_home() {
    composeTestRule.setContent { OnboardingScreen() }
    composeTestRule.onNodeWithText("Start Exploring").performClick()
    composeTestRule.onNodeWithText("Home").assertIsDisplayed()
}
```

### 10.3 Performance Optimization
- [ ] Enable R8 full mode in `gradle.properties`
- [ ] Add ProGuard rules for serialization and Room
- [ ] Use `LazyColumn` with `key` parameter for stable IDs
- [ ] Implement image caching with Coil
- [ ] Reduce Compose recompositions with `remember` and `derivedStateOf`
- [ ] Profile with Android Studio Profiler (CPU, Memory, Network)

### 10.4 Device Testing Matrix
- [ ] Test on Android 7 (API 24) - Minimum supported
- [ ] Test on Android 11 (API 30) - Scoped Storage transition
- [ ] Test on Android 13 (API 33) - Granular media permissions
- [ ] Test on Android 14+ (API 34+) - Latest features
- [ ] Test on tablet (landscape mode)

**✅ VALIDATION CHECKPOINT PHASE 10:**
- [ ] Unit tests pass with 70%+ coverage
- [ ] UI tests validate critical user flows
- [ ] App performs well on low-end devices (3GB RAM)
- [ ] APK size under 15MB (Compose apps are larger)
- [ ] No memory leaks detected (LeakCanary)
- [ ] Smooth scrolling in all lists (60fps+)

---

## Phase 11: Release Preparation

### 11.1 App Signing & Building
```bash
# Generate release keystore
keytool -genkey -v -keystore release.keystore \
  -alias entertainment-browser -keyalg RSA \
  -keysize 2048 -validity 10000

# Build signed APK
./gradlew bundleRelease  # Use AAB for Play Store
./gradlew assembleRelease  # Or APK for direct distribution
```

### 11.2 ProGuard Configuration
```proguard
# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Keep OkHttp
-dontwarn okhttp3.**

# Keep Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
```

### 11.3 App Icon & Branding
- [ ] Create adaptive icon with Material You support
- [ ] Generate monochrome icon for themed icons
- [ ] Create splash screen drawable
- [ ] Design in red/dark blue color scheme

### 11.4 Final Deliverables
- [ ] Signed APK/AAB file
- [ ] SHA-256 checksum: `sha256sum app-release.apk`
- [ ] Changelog: `CHANGELOG.md`
- [ ] Screenshots (phone + tablet, light + dark mode)
- [ ] Privacy policy (if collecting any data)

**✅ FINAL VALIDATION:**
- [ ] Release build runs without crashes
- [ ] All 45+ websites load correctly
- [ ] Video detection works on test sites
- [ ] Downloads work with MediaStore
- [ ] Tabs and sessions persist correctly
- [ ] App size under 15MB
- [ ] Launch time under 2 seconds (cold start)
- [ ] No critical bugs in user flows

---

## Success Criteria (Updated for 2025)

✅ **COMPLETE** when:
- [ ] 45+ entertainment websites integrated
- [ ] Video downloads work on non-DRM platforms
- [ ] Modern Android permissions (Scoped Storage, Android 13+)
- [ ] Material 3 design with dynamic theming
- [ ] Jetpack Compose UI (no XML layouts)
- [ ] MVVM + Clean Architecture with Hilt
- [ ] Tab management with persistence
- [ ] Session save/restore functionality
- [ ] WorkManager-based downloads
- [ ] APK size < 15MB
- [ ] 70%+ test coverage
- [ ] No memory leaks or crashes
- [ ] Smooth 60fps+ performance

---

## Key Modernizations Applied

### ❌ Removed/Changed:
- XML layouts → **Jetpack Compose**
- `SharedPreferences` → **DataStore**
- Gson → **Kotlinx Serialization**
- Manual OkHttp downloads → **WorkManager**
- Direct file access → **MediaStore (Scoped Storage)**
- Custom splash screen → **Android 12+ Splash API**
- `REQUEST_INSTALL_PACKAGES` → **Removed (not needed)**
- Outdated permissions → **Android 13+ granular permissions**

### ✅ Added:
- Material You dynamic theming
- Compose Navigation
- Flow-based reactive data
- Hilt dependency injection
- Modern Scoped Storage
- WorkManager for background tasks
- Proper DRM detection warnings
- Tab thumbnail generation
- Session serialization with Kotlinx Serialization

### 📊 Reduced Checklist Size:
- **Original:** ~300+ items
- **Updated:** ~200 items (more focused, less redundant)
- **Time Estimate:** 6-8 weeks → 4-6 weeks (with Compose productivity gains)