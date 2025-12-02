# Entertainment Browser - Complete App Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Project Structure](#project-structure)
5. [Core Features](#core-features)
6. [Implementation Guide](#implementation-guide)
7. [Key Components](#key-components)
8. [Database Design](#database-design)
9. [Navigation Flow](#navigation-flow)
10. [Ad-Blocking System](#ad-blocking-system)
11. [Build & Deploy](#build--deploy)

---

## Project Overview

**Entertainment Browser** is a modern Android application that provides unified access to 45+ entertainment websites across streaming services, TV shows, books, and video platforms.

### Key Features
- Browse 45+ entertainment websites organized by category
- In-app WebView browsing with tab management (up to 20 tabs)
- Video detection and download capabilities
- Advanced ad-blocking system
- Favorites management
- Session management (save/restore tab collections)
- Search functionality
- Onboarding flow for first-time users
- Dark theme with Material Design 3

### Target Platform
- **Minimum SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 36
- **Compile SDK**: 36

---

## Architecture

The app follows **Clean Architecture** with **MVVM** pattern:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (UI, ViewModels, Compose Screens)      │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│          Domain Layer                   │
│  (Models, Repository Interfaces,        │
│   Use Cases - Pure Kotlin)              │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│           Data Layer                    │
│  (Repository Implementations, Room,     │
│   DataStore, Workers)                   │
└─────────────────────────────────────────┘
```

### Layer Responsibilities

**Presentation Layer**:
- Jetpack Compose UI
- ViewModels (state management)
- Navigation
- User interactions

**Domain Layer**:
- Business logic
- Domain models (pure Kotlin)
- Repository interfaces
- Use cases

**Data Layer**:
- Repository implementations
- Room database
- DataStore preferences
- Background workers
- Network operations

---

## Technology Stack

### Build System
```kotlin
AGP: 8.13.0
Kotlin: 2.0.21
Java: Version 11
KSP: 2.0.21-1.0.25
Gradle: 8.13
```

### Core Technologies
```kotlin
// UI Framework
Jetpack Compose BOM: 2024.10.00
Material Design 3

// Architecture
MVVM + Clean Architecture
Hilt (Dagger): 2.50 - Dependency Injection

// Database
Room: 2.6.1 with KTX extensions

// Networking
OkHttp: 4.12.0 with logging interceptor

// Image Loading
Coil: 2.5.0 for Compose

// Serialization
Kotlinx Serialization JSON: 1.6.2

// Async
Kotlin Coroutines: 1.8.0
Flow

// Navigation
Jetpack Navigation Compose: 2.7.6

// Preferences
DataStore Preferences: 1.1.1

// Background Tasks
WorkManager: 2.9.0

// Testing
JUnit: 4.13.2
MockK: 1.13.8
Turbine: 1.0.0
Espresso: 3.7.0

// Debug
LeakCanary: 2.12
```

---

## Project Structure

```
app/src/main/java/com/entertainmentbrowser/
├── core/                           # Core utilities
│   ├── constants/                  # App-wide constants
│   ├── error/                      # Error handling
│   └── result/                     # Result wrapper types
│
├── data/                           # Data layer
│   ├── local/
│   │   ├── dao/                    # Room DAOs
│   │   ├── database/               # Database configuration
│   │   ├── datastore/              # DataStore preferences
│   │   ├── entity/                 # Room entities
│   │   └── PrepopulateData.kt      # Initial data
│   ├── repository/                 # Repository implementations
│   └── worker/                     # WorkManager workers
│
├── di/                             # Dependency injection
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   ├── DataStoreModule.kt
│   ├── DownloadModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
│
├── domain/                         # Domain layer
│   ├── model/                      # Domain models
│   ├── repository/                 # Repository interfaces
│   └── usecase/                    # Use cases
│
├── presentation/                   # Presentation layer
│   ├── common/                     # Shared UI components
│   ├── downloads/                  # Downloads screen
│   ├── favorites/                  # Favorites screen
│   ├── home/                       # Home screen
│   ├── navigation/                 # Navigation setup
│   ├── onboarding/                 # Onboarding flow
│   ├── sessions/                   # Sessions screen
│   ├── settings/                   # Settings screen
│   ├── tabs/                       # Tabs screen
│   ├── theme/                      # Material Design 3 theme
│   └── webview/                    # WebView screen
│
├── util/                           # Utility functions
│   ├── adblock/                    # Ad-blocking system
│   ├── AccessibilityHelper.kt
│   ├── DownloadNotificationManager.kt
│   ├── HapticFeedbackHelper.kt
│   ├── MediaStoreHelper.kt
│   ├── SessionSerializer.kt
│   ├── TabManager.kt
│   ├── ThumbnailCapture.kt
│   └── WebViewStateManager.kt
│
├── EntertainmentBrowserApp.kt      # Application class
└── MainActivity.kt                 # Main activity
```

---

## Core Features

### 1. Website Catalog
- **45+ pre-populated websites** across 4 categories:
  - Streaming Services (15)
  - TV Shows (10)
  - Books (10)
  - Video Platforms (10)
- Each website has:
  - Name, URL, logo, description
  - Category classification
  - Background color
  - Favorite status
  - Display order

### 2. WebView Browsing
- Custom WebView with security features
- JavaScript enabled
- DOM storage
- Safe browsing
- Video detection
- DRM detection
- Ad-blocking integration

### 3. Tab Management
- Up to 20 concurrent tabs
- Tab thumbnails (circular design)
- Active tab indicator
- Close tab functionality
- Tab persistence
- Automatic cleanup (old tabs)

### 4. Ad-Blocking System
- **FastAdBlockEngine**: Pattern-based blocking
- **HardcodedFilters**: Known ad domains
- **Direct link blocking**: Prevents ad redirects
- **Metrics tracking**: Blocked requests count
- **Graceful degradation**: Continues if blocking fails

### 5. Video Detection & Download
- Automatic video URL detection
- DRM content detection
- Download manager integration
- Progress tracking
- Notification system

### 6. Session Management
- Save current tab collection
- Restore saved sessions
- Session naming
- Multiple sessions support

### 7. Favorites
- Mark websites as favorites
- Quick access to favorites
- Persistent storage

### 8. Search
- Search across all websites
- Real-time filtering
- Category-based search

### 9. Settings
- Haptic feedback toggle
- Clear cache
- Clear history
- App version info

### 10. Onboarding
- Welcome screen
- Features overview
- Permissions request
- First-time setup

---

## Implementation Guide

### Step 1: Project Setup

```kotlin
// build.gradle.kts (Project level)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}
```

```kotlin
// build.gradle.kts (App level)
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.entertainmentbrowser"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.entertainmentbrowser"
        minSdk = 24
        targetSdk = 36
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
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    kotlinOptions {
        jvmTarget = "11"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
```

### Step 2: Define Domain Models

```kotlin
// Website.kt
data class Website(
    val id: Int,
    val name: String,
    val url: String,
    val category: Category,
    val logoUrl: String,
    val description: String,
    val backgroundColor: String,
    val isFavorite: Boolean,
    val order: Int
)

enum class Category {
    STREAMING,
    TV_SHOWS,
    BOOKS,
    VIDEO_PLATFORMS
}
```

```kotlin
// Tab.kt
data class Tab(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailPath: String?,
    val isActive: Boolean,
    val timestamp: Long
)
```

### Step 3: Create Room Database

```kotlin
// WebsiteEntity.kt
@Entity(tableName = "websites")
data class WebsiteEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val url: String,
    val category: String,
    val logoUrl: String,
    val description: String,
    val backgroundColor: String,
    val isFavorite: Boolean,
    val order: Int
)

// WebsiteDao.kt
@Dao
interface WebsiteDao {
    @Query("SELECT * FROM websites ORDER BY `order` ASC")
    fun getAllWebsites(): Flow<List<WebsiteEntity>>
    
    @Query("SELECT * FROM websites WHERE category = :category ORDER BY `order` ASC")
    fun getWebsitesByCategory(category: String): Flow<List<WebsiteEntity>>
    
    @Query("SELECT * FROM websites WHERE isFavorite = 1 ORDER BY `order` ASC")
    fun getFavoriteWebsites(): Flow<List<WebsiteEntity>>
    
    @Update
    suspend fun updateWebsite(website: WebsiteEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(websites: List<WebsiteEntity>)
}

// AppDatabase.kt
@Database(
    entities = [
        WebsiteEntity::class,
        TabEntity::class,
        SessionEntity::class,
        DownloadEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun websiteDao(): WebsiteDao
    abstract fun tabDao(): TabDao
    abstract fun sessionDao(): SessionDao
    abstract fun downloadDao(): DownloadDao
}
```

### Step 4: Implement Repository Pattern

```kotlin
// WebsiteRepository.kt (Interface)
interface WebsiteRepository {
    fun getAllWebsites(): Flow<List<Website>>
    fun getWebsitesByCategory(category: Category): Flow<List<Website>>
    fun getFavoriteWebsites(): Flow<List<Website>>
    suspend fun toggleFavorite(websiteId: Int)
    suspend fun prepopulateWebsites()
}

// WebsiteRepositoryImpl.kt (Implementation)
class WebsiteRepositoryImpl @Inject constructor(
    private val websiteDao: WebsiteDao
) : WebsiteRepository {
    
    override fun getAllWebsites(): Flow<List<Website>> {
        return websiteDao.getAllWebsites()
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override fun getWebsitesByCategory(category: Category): Flow<List<Website>> {
        return websiteDao.getWebsitesByCategory(category.name)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override fun getFavoriteWebsites(): Flow<List<Website>> {
        return websiteDao.getFavoriteWebsites()
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override suspend fun toggleFavorite(websiteId: Int) {
        // Implementation
    }
    
    override suspend fun prepopulateWebsites() {
        val websites = PrepopulateData.getWebsites()
        websiteDao.insertAll(websites)
    }
}
```

### Step 5: Setup Dependency Injection

```kotlin
// DatabaseModule.kt
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "entertainment_browser_db"
        ).build()
    }
    
    @Provides
    fun provideWebsiteDao(database: AppDatabase): WebsiteDao {
        return database.websiteDao()
    }
}

// RepositoryModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindWebsiteRepository(
        impl: WebsiteRepositoryImpl
    ): WebsiteRepository
}
```

### Step 6: Create ViewModel

```kotlin
// HomeViewModel.kt
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val websiteRepository: WebsiteRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadWebsites()
    }
    
    private fun loadWebsites() {
        viewModelScope.launch {
            websiteRepository.getAllWebsites()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
                .collect { websites ->
                    _uiState.update { it.copy(websites = websites, isLoading = false) }
                }
        }
    }
    
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.ToggleFavorite -> toggleFavorite(event.websiteId)
            is HomeEvent.SearchQueryChanged -> updateSearchQuery(event.query)
            // Handle other events
        }
    }
}

// HomeUiState.kt
data class HomeUiState(
    val websites: List<Website> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

// HomeEvent.kt
sealed interface HomeEvent {
    data class ToggleFavorite(val websiteId: Int) : HomeEvent
    data class SearchQueryChanged(val query: String) : HomeEvent
}
```

### Step 7: Build UI with Compose

```kotlin
// HomeScreen.kt
@Composable
fun HomeScreen(
    onNavigateToWebView: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entertainment Browser") }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(uiState.error!!)
            else -> WebsiteGrid(
                websites = uiState.websites,
                onWebsiteClick = onNavigateToWebView,
                onFavoriteClick = { websiteId ->
                    viewModel.onEvent(HomeEvent.ToggleFavorite(websiteId))
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun WebsiteGrid(
    websites: List<Website>,
    onWebsiteClick: (String) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        items(websites) { website ->
            WebsiteCard(
                website = website,
                onClick = { onWebsiteClick(website.url) },
                onFavoriteClick = { onFavoriteClick(website.id) }
            )
        }
    }
}
```

### Step 8: Implement Custom WebView

```kotlin
// CustomWebView.kt
@Composable
fun CustomWebView(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {},
    onVideoDetected: (String) -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    
    // Create WebView ONCE - never recreate
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
    
    // Configure WebView ONCE
    DisposableEffect(Unit) {
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                safeBrowsingEnabled = true
            }
            
            webViewClient = AdBlockWebViewClient(
                onVideoDetected = onVideoDetected,
                onLoadingChanged = onLoadingChanged
            )
        }
        
        onWebViewCreated(webView)
        
        onDispose {
            webView.destroy()
        }
    }
    
    // Load URL when it changes
    DisposableEffect(url) {
        if (url.isNotBlank() && webView.url != url) {
            webView.loadUrl(url)
        }
        onDispose { }
    }
    
    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}
```

### Step 9: Setup Navigation

```kotlin
// Screen.kt
sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object WebView : Screen("webview/{url}") {
        fun createRoute(url: String): String {
            val encodedUrl = Uri.encode(url)
            return "webview/$encodedUrl"
        }
    }
    object Downloads : Screen("downloads")
    object Tabs : Screen("tabs")
    object Sessions : Screen("sessions")
    object Settings : Screen("settings")
}

// EntertainmentNavHost.kt
@Composable
fun EntertainmentNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToWebView = { url ->
                    navController.navigate(Screen.WebView.createRoute(url))
                }
            )
        }
        
        composable(
            route = Screen.WebView.route,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType }
            )
        ) {
            WebViewScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Add other screens...
    }
}
```

### Step 10: Implement Ad-Blocking

```kotlin
// AdBlockWebViewClient.kt
class AdBlockWebViewClient(
    private val onVideoDetected: (String) -> Unit = {},
    private val onLoadingChanged: (Boolean) -> Unit = {}
) : WebViewClient() {
    
    private var fastEngine: FastAdBlockEngine? = null
    
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        
        // Initialize engine
        if (fastEngine == null && view != null) {
            fastEngine = FastAdBlockEngine.getInstance(view.context)
        }
        
        // Check if should block
        if (fastEngine?.shouldBlock(url) == true) {
            return createEmptyResponse()
        }
        
        if (HardcodedFilters.shouldBlock(url)) {
            return createEmptyResponse()
        }
        
        return null
    }
    
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        
        // Block known ad redirect domains
        if (isAdRedirect(url)) {
            view?.post {
                if (view.canGoBack()) {
                    view.goBack()
                }
            }
            return true
        }
        
        return false
    }
    
    private fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
```

---

## Key Components

### 1. Application Class

```kotlin
@HiltAndroidApp
class EntertainmentBrowserApp : Application() {
    
    @Inject
    lateinit var websiteRepository: WebsiteRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // Prepopulate database
        applicationScope.launch {
            websiteRepository.prepopulateWebsites()
        }
        
        // Initialize ad-blocking
        FastAdBlockEngine.getInstance(this).preloadFromAssets()
        
        // Schedule cleanup
        scheduleTabCleanup()
    }
}
```

### 2. MainActivity

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            EntertainmentBrowserTheme {
                EntertainmentBrowserApp(settingsRepository)
            }
        }
    }
}
```

### 3. Theme Configuration

```kotlin
// Color.kt
val RedPrimary = Color(0xFFFD1D1D)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)

// Theme.kt
@Composable
fun EntertainmentBrowserTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = RedPrimary,
        background = DarkBackground,
        surface = DarkSurface
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## Database Design

### Tables

**websites**
- id (PK)
- name
- url
- category
- logoUrl
- description
- backgroundColor
- isFavorite
- order

**tabs**
- id (PK)
- url
- title
- thumbnailPath
- isActive
- timestamp

**sessions**
- id (PK)
- name
- tabIds (JSON)
- createdAt

**downloads**
- id (PK)
- url
- filename
- filePath
- status
- progress
- downloadedBytes
- totalBytes
- createdAt

---

## Navigation Flow

```
Splash Screen
    ↓
[First Launch?]
    ├─ Yes → Onboarding → Home
    └─ No  → Home
    
Home Screen
    ├─ Click Website → WebView
    ├─ Favorites → Favorites Screen
    ├─ Settings → Settings Screen
    └─ Bottom Nav
        ├─ Downloads
        ├─ Tabs
        └─ Sessions

WebView Screen
    ├─ Video Detected → Download FAB
    ├─ Tab Management → Tab Bar
    └─ Back → Previous Screen
```

---

## Ad-Blocking System

### Components

1. **FastAdBlockEngine**
   - Pattern-based URL matching
   - Loads filters from assets
   - Efficient regex matching

2. **HardcodedFilters**
   - Known ad domains
   - Quick lookup
   - Fallback system

3. **AdBlockWebViewClient**
   - Intercepts requests
   - Blocks resources
   - Prevents redirects

### How It Works

```
Request → shouldInterceptRequest()
    ↓
Check FastAdBlockEngine
    ↓
[Match?]
    ├─ Yes → Return Empty Response
    └─ No  → Check HardcodedFilters
        ↓
    [Match?]
        ├─ Yes → Return Empty Response
        └─ No  → Allow Request
```

---

## Build & Deploy

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Install on Device
```bash
./gradlew installDebug
```

### Run Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Generate Lint Report
```bash
./gradlew lint
```

---

## Best Practices Used

1. **Clean Architecture**: Separation of concerns
2. **MVVM Pattern**: Reactive UI updates
3. **Dependency Injection**: Hilt for modularity
4. **Coroutines & Flow**: Async operations
5. **Room Database**: Type-safe data persistence
6. **Jetpack Compose**: Modern declarative UI
7. **Material Design 3**: Consistent design system
8. **ProGuard**: Code obfuscation for release
9. **WorkManager**: Background tasks
10. **Testing**: Unit and integration tests

---

## Common Patterns

### Repository Pattern
```kotlin
interface Repository {
    fun getData(): Flow<Data>
}

class RepositoryImpl(private val dao: Dao) : Repository {
    override fun getData() = dao.getData().map { it.toDomain() }
}
```

### ViewModel Pattern
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()
    
    fun onEvent(event: Event) {
        // Handle events
    }
}
```

### Composable Pattern
```kotlin
@Composable
fun MyScreen(
    viewModel: MyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold { paddingValues ->
        Content(
            state = uiState,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
```

---

## Performance Optimizations

1. **WebView Reuse**: Single WebView instance
2. **Lazy Loading**: LazyColumn/LazyGrid
3. **Image Caching**: Coil with disk cache
4. **Database Indexing**: Room indexes
5. **ProGuard**: Code shrinking
6. **Background Work**: WorkManager
7. **Memory Management**: LeakCanary

---

## Security Features

1. **HTTPS Enforcement**: Only secure connections
2. **Safe Browsing**: WebView safe browsing
3. **No File Access**: Disabled in WebView
4. **Input Validation**: URL validation
5. **ProGuard**: Code obfuscation

---

## Accessibility

1. **Content Descriptions**: All interactive elements
2. **Semantic Labels**: Proper labeling
3. **Touch Targets**: Minimum 48dp
4. **Haptic Feedback**: Optional feedback
5. **Screen Reader Support**: TalkBack compatible

---

## Future Enhancements

1. **Cloud Sync**: Sync favorites/sessions
2. **Themes**: Multiple color schemes
3. **Bookmarks**: Save specific pages
4. **History**: Browse history
5. **Extensions**: Plugin system
6. **Offline Mode**: Cache pages
7. **Multi-Window**: Split screen support
8. **Widgets**: Home screen widgets

---

## Conclusion

This documentation provides a complete guide to building an Entertainment Browser app. The architecture is scalable, maintainable, and follows Android best practices. You can use this as a template for similar apps or extend it with additional features.

**Key Takeaways**:
- Clean Architecture for maintainability
- MVVM for reactive UI
- Jetpack Compose for modern UI
- Room for data persistence
- Hilt for dependency injection
- Custom WebView with ad-blocking
- Material Design 3 for consistency

Happy coding! 🚀
