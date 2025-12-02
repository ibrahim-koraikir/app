# How to Build Apps Like Entertainment Browser

This guide explains the architecture, patterns, and techniques used in Entertainment Browser so you can build similar Android apps with WebView, tab management, downloads, and performance optimization.

## Table of Contents

1. [Project Setup](#project-setup)
2. [Architecture Overview](#architecture-overview)
3. [Core Features Implementation](#core-features-implementation)
4. [Performance Optimization](#performance-optimization)
5. [Common Patterns](#common-patterns)
6. [Testing Strategy](#testing-strategy)

---

## Project Setup

### Technology Stack

```kotlin
// build.gradle.kts (Project level)
plugins {
    id("com.android.application") version "8.13.0"
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("com.google.dagger.hilt.android") version "2.50"
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
}
```

### Essential Dependencies

```kotlin
// app/build.gradle.kts
dependencies {
    // Compose UI
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    
    // Architecture Components
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Dependency Injection
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Networking & Image Loading
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

---

## Architecture Overview

### Clean Architecture Layers

```
app/src/main/java/com/yourapp/
├── core/                    # Shared utilities
├── data/                    # Data layer
│   ├── local/              # Room database
│   ├── repository/         # Repository implementations
│   └── worker/             # Background tasks
├── di/                      # Dependency injection
├── domain/                  # Business logic
│   ├── model/              # Domain models
│   └── repository/         # Repository interfaces
├── presentation/            # UI layer
│   ├── common/             # Shared composables
│   ├── theme/              # Material Design theme
│   └── [features]/         # Feature screens
└── util/                    # Utility classes
```

### Key Principles

1. **Dependency Rule**: Domain layer has no dependencies on other layers
2. **Data Flow**: UI → ViewModel → Repository → Data Source
3. **Reactive Streams**: Use Kotlin Flow for reactive data
4. **Single Source of Truth**: Database is the source of truth

---

## Core Features Implementation

### 1. WebView with Tab Management

#### Custom WebView Component

```kotlin
@Composable
fun CustomWebView(
    url: String,
    onUrlChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }
                
                webViewClient = CustomWebViewClient(
                    onUrlChange = onUrlChange
                )
                
                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        title?.let { onTitleChange(it) }
                    }
                }
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        modifier = modifier
    )
}
```

#### Tab Manager

```kotlin
class TabManager @Inject constructor(
    private val tabRepository: TabRepository
) {
    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    val tabs: StateFlow<List<Tab>> = _tabs.asStateFlow()
    
    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()
    
    suspend fun createTab(url: String, title: String): Tab {
        val tab = Tab(
            id = UUID.randomUUID().toString(),
            url = url,
            title = title,
            createdAt = System.currentTimeMillis()
        )
        tabRepository.insertTab(tab)
        _tabs.value = _tabs.value + tab
        _activeTabId.value = tab.id
        return tab
    }
    
    suspend fun closeTab(tabId: String) {
        tabRepository.deleteTab(tabId)
        _tabs.value = _tabs.value.filterNot { it.id == tabId }
        
        if (_activeTabId.value == tabId) {
            _activeTabId.value = _tabs.value.lastOrNull()?.id
        }
    }
    
    fun switchTab(tabId: String) {
        _activeTabId.value = tabId
    }
}
```

### 2. WebView State Management

```kotlin
class WebViewStateManager @Inject constructor(
    private val context: Context
) {
    private val stateCache = mutableMapOf<String, Bundle>()
    
    fun saveState(tabId: String, webView: WebView) {
        val bundle = Bundle()
        webView.saveState(bundle)
        stateCache[tabId] = bundle
    }
    
    fun restoreState(tabId: String, webView: WebView): Boolean {
        val bundle = stateCache[tabId] ?: return false
        webView.restoreState(bundle)
        return true
    }
    
    fun clearState(tabId: String) {
        stateCache.remove(tabId)
    }
}
```

### 3. Room Database Setup

#### Entity

```kotlin
@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val favicon: String?,
    val createdAt: Long,
    val lastAccessedAt: Long
)
```

#### DAO

```kotlin
@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY lastAccessedAt DESC")
    fun getAllTabs(): Flow<List<TabEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity)
    
    @Query("DELETE FROM tabs WHERE id = :tabId")
    suspend fun deleteTab(tabId: String)
    
    @Query("UPDATE tabs SET lastAccessedAt = :timestamp WHERE id = :tabId")
    suspend fun updateLastAccessed(tabId: String, timestamp: Long)
}
```

#### Database

```kotlin
@Database(
    entities = [TabEntity::class, WebsiteEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun websiteDao(): WebsiteDao
    abstract fun sessionDao(): SessionDao
}
```

### 4. Repository Pattern

#### Interface (Domain Layer)

```kotlin
interface TabRepository {
    fun getAllTabs(): Flow<List<Tab>>
    suspend fun insertTab(tab: Tab)
    suspend fun deleteTab(tabId: String)
    suspend fun updateTab(tab: Tab)
}
```

#### Implementation (Data Layer)

```kotlin
class TabRepositoryImpl @Inject constructor(
    private val tabDao: TabDao
) : TabRepository {
    
    override fun getAllTabs(): Flow<List<Tab>> {
        return tabDao.getAllTabs().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun insertTab(tab: Tab) {
        tabDao.insertTab(tab.toEntity())
    }
    
    override suspend fun deleteTab(tabId: String) {
        tabDao.deleteTab(tabId)
    }
    
    override suspend fun updateTab(tab: Tab) {
        tabDao.insertTab(tab.toEntity())
    }
}
```

### 5. ViewModel Pattern

```kotlin
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val tabManager: TabManager,
    private val webViewStateManager: WebViewStateManager
) : ViewModel() {
    
    val tabs = tabManager.tabs.stateAsState(emptyList())
    val activeTabId = tabManager.activeTabId.stateAsState(null)
    
    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()
    
    fun createNewTab(url: String) {
        viewModelScope.launch {
            tabManager.createTab(url, "Loading...")
        }
    }
    
    fun closeTab(tabId: String) {
        viewModelScope.launch {
            webViewStateManager.clearState(tabId)
            tabManager.closeTab(tabId)
        }
    }
    
    fun switchTab(tabId: String) {
        tabManager.switchTab(tabId)
    }
}

data class WebViewUiState(
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val progress: Int = 0
)
```

### 6. Dependency Injection with Hilt

#### Application Class

```kotlin
@HiltAndroidApp
class YourApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize app-wide components
    }
}
```

#### Modules

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    fun provideTabDao(database: AppDatabase): TabDao {
        return database.tabDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideTabRepository(tabDao: TabDao): TabRepository {
        return TabRepositoryImpl(tabDao)
    }
}
```

---

## Performance Optimization

### 1. WebView Pooling

```kotlin
class WebViewPool @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pool = mutableListOf<WebView>()
    private val maxPoolSize = 3
    
    fun acquire(): WebView {
        return if (pool.isNotEmpty()) {
            pool.removeAt(0)
        } else {
            createWebView()
        }
    }
    
    fun release(webView: WebView) {
        if (pool.size < maxPoolSize) {
            webView.clearHistory()
            webView.clearCache(true)
            pool.add(webView)
        } else {
            webView.destroy()
        }
    }
    
    private fun createWebView(): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
        }
    }
}
```

### 2. Memory Management

```kotlin
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val maxCacheSize = 50 * 1024 * 1024 // 50MB
    
    fun clearWebViewCache() {
        context.cacheDir.deleteRecursively()
        WebStorage.getInstance().deleteAllData()
    }
    
    fun getCacheSize(): Long {
        return context.cacheDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    fun clearIfNeeded() {
        if (getCacheSize() > maxCacheSize) {
            clearWebViewCache()
        }
    }
}
```

### 3. Bitmap Management

```kotlin
class BitmapManager @Inject constructor() {
    
    fun compressBitmap(bitmap: Bitmap, maxSize: Int = 512): Bitmap {
        val ratio = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
        
        if (ratio >= 1) return bitmap
        
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
```

### 4. Background Tasks with WorkManager

```kotlin
class TabCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(applicationContext)
        val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days
        
        database.tabDao().deleteOldTabs(cutoffTime)
        
        return Result.success()
    }
}

// Schedule the worker
fun scheduleCleanup(context: Context) {
    val cleanupRequest = PeriodicWorkRequestBuilder<TabCleanupWorker>(
        1, TimeUnit.DAYS
    ).build()
    
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "tab_cleanup",
        ExistingPeriodicWorkPolicy.KEEP,
        cleanupRequest
    )
}
```

---

## Common Patterns

### 1. Sealed Classes for UI State

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

// Usage in ViewModel
private val _websites = MutableStateFlow<UiState<List<Website>>>(UiState.Loading)
val websites: StateFlow<UiState<List<Website>>> = _websites.asStateFlow()

// Usage in Composable
when (val state = uiState) {
    is UiState.Loading -> LoadingIndicator()
    is UiState.Success -> WebsiteList(state.data)
    is UiState.Error -> ErrorMessage(state.message)
}
```

### 2. DataStore for Preferences

```kotlin
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IS_FIRST_LAUNCH] ?: true
    }
    
    suspend fun setFirstLaunchComplete() {
        dataStore.edit { prefs ->
            prefs[IS_FIRST_LAUNCH] = false
        }
    }
    
    companion object {
        private val Context.dataStore by preferencesDataStore("settings")
        private val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    }
}
```

### 3. Navigation with Compose

```kotlin
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToWebView = { url ->
                    navController.navigate("webview/$url")
                }
            )
        }
        
        composable(
            route = "webview/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url")
            WebViewScreen(
                url = url,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

### 4. Material Design 3 Theme

```kotlin
@Composable
fun YourAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFFFF0000),
        secondary = Color(0xFF03DAC5),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E)
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## Testing Strategy

### 1. Unit Tests

```kotlin
class TabManagerTest {
    
    private lateinit var tabManager: TabManager
    private lateinit var mockRepository: TabRepository
    
    @Before
    fun setup() {
        mockRepository = mockk()
        tabManager = TabManager(mockRepository)
    }
    
    @Test
    fun `createTab should add tab to list`() = runTest {
        coEvery { mockRepository.insertTab(any()) } just Runs
        
        val tab = tabManager.createTab("https://example.com", "Example")
        
        assertEquals(1, tabManager.tabs.value.size)
        assertEquals(tab.id, tabManager.activeTabId.value)
    }
}
```

### 2. Integration Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class TabDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var tabDao: TabDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        tabDao = database.tabDao()
    }
    
    @Test
    fun insertAndRetrieveTab() = runTest {
        val tab = TabEntity(
            id = "1",
            url = "https://example.com",
            title = "Example",
            favicon = null,
            createdAt = System.currentTimeMillis(),
            lastAccessedAt = System.currentTimeMillis()
        )
        
        tabDao.insertTab(tab)
        val tabs = tabDao.getAllTabs().first()
        
        assertEquals(1, tabs.size)
        assertEquals(tab.url, tabs[0].url)
    }
}
```

---

## Build Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate lint report
./gradlew lint
```

---

## ProGuard Configuration

```proguard
# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# Keep WebView JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
```

---

## Key Takeaways

1. **Use Clean Architecture**: Separate concerns into layers (presentation, domain, data)
2. **Leverage Jetpack Compose**: Modern UI toolkit with declarative syntax
3. **Implement MVVM**: ViewModels manage UI state and business logic
4. **Use Hilt for DI**: Simplifies dependency injection
5. **Optimize WebView**: Pool instances, manage memory, save/restore state
6. **Use Flow for Reactive Data**: Kotlin Flow for reactive streams
7. **Implement Proper Testing**: Unit tests, integration tests, and UI tests
8. **Manage Memory**: Clear caches, compress bitmaps, limit tab count
9. **Use WorkManager**: For background tasks like cleanup
10. **Follow Material Design 3**: Consistent, modern UI

---

## Additional Resources

- [Android Developers Guide](https://developer.android.com)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)

---

**Built with ❤️ using modern Android development practices**
