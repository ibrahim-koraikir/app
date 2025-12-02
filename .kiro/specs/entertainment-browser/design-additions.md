# Design Document - Additional Sections

## Data Layer

### Room Entities

#### WebsiteEntity

```kotlin
@Entity(
    tableName = "websites",
    indices = [
        Index(value = ["category"]),
        Index(value = ["isFavorite"])
    ]
)
data class WebsiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    @ColumnInfo(name = "category") val category: String,
    val logoUrl: String,
    val description: String,
    val buttonColor: String,
    val isFavorite: Boolean = false,
    val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Long = System.currentTimeMillis()
)
```

#### TabEntity

```kotlin
@Entity(
    tableName = "tabs",
    indices = [Index(value = ["isActive"])]
)
data class TabEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val thumbnailPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)
```

#### SessionEntity

```kotlin
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val tabIds: String, // JSON array of tab IDs
    val createdAt: Long = System.currentTimeMillis()
)
```

### Database Schema Diagram

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   websites   │      │     tabs     │      │   sessions   │
├──────────────┤      ├──────────────┤      ├──────────────┤
│ id (PK)      │      │ id (PK)      │      │ id (PK)      │
│ name         │      │ url          │      │ name         │
│ url          │      │ title        │      │ tabIds (JSON)│
│ category (↑) │      │ thumbnailPath│      │ createdAt    │
│ logoUrl      │      │ timestamp    │      └──────────────┘
│ description  │      │ isActive (↑) │
│ buttonColor  │      └──────────────┘
│ isFavorite(↑)│
│ sortOrder    │      (↑) = Indexed
│ createdAt    │
└──────────────┘
```

### Complete DAO Interfaces

```kotlin
@Dao
interface WebsiteDao {
    @Query("SELECT * FROM websites WHERE category = :category ORDER BY sortOrder")
    fun getByCategory(category: String): Flow<List<WebsiteEntity>>
    
    @Query("SELECT * FROM websites WHERE isFavorite = 1 ORDER BY sortOrder")
    fun getFavorites(): Flow<List<WebsiteEntity>>
    
    @Query("""
        SELECT * FROM websites 
        WHERE name LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%'
    """)
    fun search(query: String): Flow<List<WebsiteEntity>>
    
    @Update
    suspend fun update(website: WebsiteEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(websites: List<WebsiteEntity>)
}

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY timestamp DESC")
    fun getAllTabs(): Flow<List<TabEntity>>
    
    @Query("SELECT * FROM tabs WHERE isActive = 1 LIMIT 1")
    fun getActiveTab(): Flow<TabEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tab: TabEntity)
    
    @Delete
    suspend fun delete(tab: TabEntity)
    
    @Query("DELETE FROM tabs WHERE timestamp < :cutoffTime")
    suspend fun deleteOldTabs(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM tabs")
    suspend fun getTabCount(): Int
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>
    
    @Insert
    suspend fun insert(session: SessionEntity)
    
    @Delete
    suspend fun delete(session: SessionEntity)
    
    @Query("UPDATE sessions SET name = :newName WHERE id = :sessionId")
    suspend fun updateName(sessionId: String, newName: String)
}
```

## Navigation Architecture

### Screen Routes

```kotlin
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    
    object Onboarding : Screen("onboarding/{page}") {
        fun createRoute(page: Int) = "onboarding/$page"
    }
    
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    
    object WebView : Screen("webview/{url}") {
        fun createRoute(url: String) = "webview/${URLEncoder.encode(url, "UTF-8")}"
    }
    
    object Downloads : Screen("downloads")
    object Tabs : Screen("tabs")
    object Sessions : Screen("sessions")
    object Settings : Screen("settings")
}
```

### Navigation Graph

```kotlin
@Composable
fun EntertainmentNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController, startDestination) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = { 
                    navController.navigate(Screen.Onboarding.createRoute(0)) 
                },
                onNavigateToHome = { 
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.Onboarding.route,
            arguments = listOf(navArgument("page") { type = NavType.IntType })
        ) { backStackEntry ->
            val page = backStackEntry.arguments?.getInt("page") ?: 0
            OnboardingScreen(
                initialPage = page,
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onWebsiteClick = { url ->
                    navController.navigate(Screen.WebView.createRoute(url))
                },
                onNavigateToFavorites = { 
                    navController.navigate(Screen.Favorites.route) 
                }
            )
        }
        
        composable(
            route = Screen.WebView.route,
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = URLDecoder.decode(encodedUrl, "UTF-8")
            WebViewScreen(
                url = url,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Downloads.route) {
            DownloadsScreen()
        }
        
        composable(Screen.Tabs.route) {
            TabsScreen(
                onTabClick = { url ->
                    navController.navigate(Screen.WebView.createRoute(url))
                }
            )
        }
        
        composable(Screen.Sessions.route) {
            SessionsScreen()
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
```

### Deep Linking

```xml
<!-- AndroidManifest.xml -->
<activity android:name=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="https"
            android:host="entertainmentbrowser.app"
            android:pathPrefix="/website" />
    </intent-filter>
</activity>
```

```kotlin
// Handle deep links
composable(
    route = "https://entertainmentbrowser.app/website/{id}",
    deepLinks = listOf(
        navDeepLink { 
            uriPattern = "https://entertainmentbrowser.app/website/{id}" 
        }
    )
) { backStackEntry ->
    val websiteId = backStackEntry.arguments?.getString("id")?.toIntOrNull()
    // Load website and navigate
}
```

## Screen Specifications

### Home Screen

#### UI State

```kotlin
data class HomeUiState(
    val websites: List<Website> = emptyList(),
    val selectedCategory: Category = Category.STREAMING,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class Category {
    STREAMING, TV_SHOWS, BOOKS, VIDEO_PLATFORMS
}
```

#### Events

```kotlin
sealed class HomeEvent {
    data class CategorySelected(val category: Category) : HomeEvent()
    data class SearchQueryChanged(val query: String) : HomeEvent()
    data class WebsiteClicked(val website: Website) : HomeEvent()
    data class FavoriteToggled(val websiteId: Int) : HomeEvent()
    object RefreshRequested : HomeEvent()
}
```

#### ViewModel

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getWebsitesByCategoryUseCase: GetWebsitesByCategoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val searchWebsitesUseCase: SearchWebsitesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadWebsites()
    }
    
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.CategorySelected -> selectCategory(event.category)
            is HomeEvent.SearchQueryChanged -> searchWebsites(event.query)
            is HomeEvent.WebsiteClicked -> { /* Navigate */ }
            is HomeEvent.FavoriteToggled -> toggleFavorite(event.websiteId)
            is HomeEvent.RefreshRequested -> loadWebsites()
        }
    }
    
    private fun loadWebsites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getWebsitesByCategoryUseCase(_uiState.value.selectedCategory)
                .collect { result ->
                    when (result) {
                        is Result.Success -> _uiState.update {
                            it.copy(
                                websites = result.data,
                                isLoading = false,
                                error = null
                            )
                        }
                        is Result.Error -> _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                        is Result.Loading -> _uiState.update { 
                            it.copy(isLoading = true) 
                        }
                    }
                }
        }
    }
    
    private fun searchWebsites(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(searchQuery = query) }
            delay(300) // Debounce
            searchWebsitesUseCase(query).collect { result ->
                when (result) {
                    is Result.Success -> _uiState.update {
                        it.copy(websites = result.data, isLoading = false)
                    }
                    else -> { /* Handle other cases */ }
                }
            }
        }
    }
    
    private fun toggleFavorite(websiteId: Int) {
        viewModelScope.launch {
            toggleFavoriteUseCase(websiteId)
        }
    }
    
    private fun selectCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadWebsites()
    }
}
```

### Downloads Screen

#### UI State

```kotlin
data class DownloadsUiState(
    val activeDownloads: List<DownloadItem> = emptyList(),
    val completedDownloads: List<DownloadItem> = emptyList(),
    val failedDownloads: List<DownloadItem> = emptyList(),
    val isRefreshing: Boolean = false
)
```

#### Events

```kotlin
sealed class DownloadsEvent {
    data class PauseDownload(val id: Int) : DownloadsEvent()
    data class ResumeDownload(val id: Int) : DownloadsEvent()
    data class CancelDownload(val id: Int) : DownloadsEvent()
    data class DeleteDownload(val id: Int) : DownloadsEvent()
    data class OpenFile(val filename: String) : DownloadsEvent()
    object RefreshRequested : DownloadsEvent()
    object ClearCompleted : DownloadsEvent()
}
```

## Dependency Injection

### App Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}
```

### Database Module

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
            "entertainment_browser_db"
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Prepopulate database will be handled by repository
                }
            })
            .build()
    }
    
    @Provides
    fun provideWebsiteDao(database: AppDatabase): WebsiteDao = 
        database.websiteDao()
    
    @Provides
    fun provideTabDao(database: AppDatabase): TabDao = 
        database.tabDao()
    
    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = 
        database.sessionDao()
}
```

### Network Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}
```

### Download Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {
    
    @Provides
    @Singleton
    fun provideFetch(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): Fetch {
        val fetchConfiguration = FetchConfiguration.Builder(context)
            .setDownloadConcurrentLimit(3)
            .setHttpDownloader(OkHttpDownloader(okHttpClient))
            .setNotificationManager(DefaultFetchNotificationManager(context))
            .enableLogging(BuildConfig.DEBUG)
            .enableAutoStart(true)
            .setNamespace("entertainment_downloads")
            .build()
        
        return Fetch.Impl.getInstance(fetchConfiguration)
    }
}
```

### DataStore Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.createDataStore(name = "app_preferences")
    }
}
```

### Repository Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindWebsiteRepository(
        impl: WebsiteRepositoryImpl
    ): WebsiteRepository
    
    @Binds
    abstract fun bindDownloadRepository(
        impl: DownloadRepositoryImpl
    ): DownloadRepository
    
    @Binds
    abstract fun bindTabRepository(
        impl: TabRepositoryImpl
    ): TabRepository
    
    @Binds
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ): SessionRepository
}
```

## Website Prepopulation Data

### Streaming Category (15 websites)

```kotlin
val streamingWebsites = listOf(
    WebsiteEntity(1, "Netflix", "https://www.netflix.com", "STREAMING",
        "https://cdn.example.com/logos/netflix.png",
        "Stream movies and TV shows", "#E50914", false, 1),
    WebsiteEntity(2, "Disney+", "https://www.disneyplus.com", "STREAMING",
        "https://cdn.example.com/logos/disney.png",
        "Disney, Pixar, Marvel, Star Wars", "#113CCF", false, 2),
    WebsiteEntity(3, "Prime Video", "https://www.primevideo.com", "STREAMING",
        "https://cdn.example.com/logos/prime.png",
        "Amazon's streaming service", "#00A8E1", false, 3),
    WebsiteEntity(4, "Hulu", "https://www.hulu.com", "STREAMING",
        "https://cdn.example.com/logos/hulu.png",
        "Current season episodes and classics", "#1CE783", false, 4),
    WebsiteEntity(5, "Max", "https://www.max.com", "STREAMING",
        "https://cdn.example.com/logos/max.png",
        "HBO Max content", "#0073FF", false, 5),
    WebsiteEntity(6, "Apple TV+", "https://tv.apple.com", "STREAMING",
        "https://cdn.example.com/logos/appletv.png",
        "Apple original content", "#000000", false, 6),
    WebsiteEntity(7, "Paramount+", "https://www.paramountplus.com", "STREAMING",
        "https://cdn.example.com/logos/paramount.png",
        "CBS and Paramount content", "#0064FF", false, 7),
    WebsiteEntity(8, "Peacock", "https://www.peacocktv.com", "STREAMING",
        "https://cdn.example.com/logos/peacock.png",
        "NBCUniversal streaming", "#000000", false, 8),
    WebsiteEntity(9, "Crunchyroll", "https://www.crunchyroll.com", "STREAMING",
        "https://cdn.example.com/logos/crunchyroll.png",
        "Anime streaming", "#F47521", false, 9),
    WebsiteEntity(10, "Funimation", "https://www.funimation.com", "STREAMING",
        "https://cdn.example.com/logos/funimation.png",
        "Anime content", "#410099", false, 10),
    WebsiteEntity(11, "Tubi", "https://tubitv.com", "STREAMING",
        "https://cdn.example.com/logos/tubi.png",
        "Free movies and TV", "#FA3C2E", false, 11),
    WebsiteEntity(12, "Pluto TV", "https://pluto.tv", "STREAMING",
        "https://cdn.example.com/logos/pluto.png",
        "Free live TV and movies", "#FFC107", false, 12),
    WebsiteEntity(13, "Plex", "https://www.plex.tv", "STREAMING",
        "https://cdn.example.com/logos/plex.png",
        "Media server and streaming", "#E5A00D", false, 13),
    WebsiteEntity(14, "YouTube Movies", "https://www.youtube.com/movies", "STREAMING",
        "https://cdn.example.com/logos/youtube.png",
        "Rent or buy movies", "#FF0000", false, 14),
    WebsiteEntity(15, "Vudu", "https://www.vudu.com", "STREAMING",
        "https://cdn.example.com/logos/vudu.png",
        "Rent, buy, or watch free", "#0088FF", false, 15)
)
```

### TV Shows Category (10 websites)

```kotlin
val tvShowsWebsites = listOf(
    WebsiteEntity(16, "JustWatch", "https://www.justwatch.com", "TV_SHOWS",
        "https://cdn.example.com/logos/justwatch.png",
        "Find where to watch TV shows", "#FFD500", false, 1),
    WebsiteEntity(17, "Rotten Tomatoes", "https://www.rottentomatoes.com", "TV_SHOWS",
        "https://cdn.example.com/logos/rt.png",
        "TV show ratings and reviews", "#FA320A", false, 2),
    WebsiteEntity(18, "IMDb", "https://www.imdb.com", "TV_SHOWS",
        "https://cdn.example.com/logos/imdb.png",
        "TV show database", "#F5C518", false, 3),
    WebsiteEntity(19, "TVTime", "https://www.tvtime.com", "TV_SHOWS",
        "https://cdn.example.com/logos/tvtime.png",
        "Track your TV shows", "#FF6600", false, 4),
    WebsiteEntity(20, "Reelgood", "https://reelgood.com", "TV_SHOWS",
        "https://cdn.example.com/logos/reelgood.png",
        "Streaming guide", "#00D9FF", false, 5),
    WebsiteEntity(21, "Simkl", "https://simkl.com", "TV_SHOWS",
        "https://cdn.example.com/logos/simkl.png",
        "TV tracker", "#0B0F10", false, 6),
    WebsiteEntity(22, "Trakt", "https://trakt.tv", "TV_SHOWS",
        "https://cdn.example.com/logos/trakt.png",
        "Track TV shows", "#ED1C24", false, 7),
    WebsiteEntity(23, "TheTVDB", "https://thetvdb.com", "TV_SHOWS",
        "https://cdn.example.com/logos/tvdb.png",
        "TV show database", "#1E5B8C", false, 8),
    WebsiteEntity(24, "Metacritic", "https://www.metacritic.com", "TV_SHOWS",
        "https://cdn.example.com/logos/metacritic.png",
        "TV show reviews", "#000000", false, 9),
    WebsiteEntity(25, "TV Guide", "https://www.tvguide.com", "TV_SHOWS",
        "https://cdn.example.com/logos/tvguide.png",
        "TV listings and news", "#FF0000", false, 10)
)
```

### Books Category (10 websites)

```kotlin
val booksWebsites = listOf(
    WebsiteEntity(26, "Goodreads", "https://www.goodreads.com", "BOOKS",
        "https://cdn.example.com/logos/goodreads.png",
        "Discover and share books", "#A0522D", false, 1),
    WebsiteEntity(27, "Audible", "https://www.audible.com", "BOOKS",
        "https://cdn.example.com/logos/audible.png",
        "Audiobooks", "#FF9900", false, 2),
    WebsiteEntity(28, "Libby", "https://www.overdrive.com/apps/libby", "BOOKS",
        "https://cdn.example.com/logos/libby.png",
        "Library ebooks and audiobooks", "#4F46E5", false, 3),
    WebsiteEntity(29, "Kindle", "https://read.amazon.com", "BOOKS",
        "https://cdn.example.com/logos/kindle.png",
        "Amazon ebooks", "#232F3E", false, 4),
    WebsiteEntity(30, "Apple Books", "https://www.apple.com/apple-books", "BOOKS",
        "https://cdn.example.com/logos/applebooks.png",
        "Apple's ebook store", "#FF9500", false, 5),
    WebsiteEntity(31, "Google Books", "https://books.google.com", "BOOKS",
        "https://cdn.example.com/logos/googlebooks.png",
        "Google's book service", "#4285F4", false, 6),
    WebsiteEntity(32, "Scribd", "https://www.scribd.com", "BOOKS",
        "https://cdn.example.com/logos/scribd.png",
        "Unlimited books and audiobooks", "#1A7BBA", false, 7),
    WebsiteEntity(33, "Project Gutenberg", "https://www.gutenberg.org", "BOOKS",
        "https://cdn.example.com/logos/gutenberg.png",
        "Free ebooks", "#6C757D", false, 8),
    WebsiteEntity(34, "LibraryThing", "https://www.librarything.com", "BOOKS",
        "https://cdn.example.com/logos/librarything.png",
        "Catalog your books", "#8B4513", false, 9),
    WebsiteEntity(35, "StoryGraph", "https://www.thestorygraph.com", "BOOKS",
        "https://cdn.example.com/logos/storygraph.png",
        "Track your reading", "#FF6B6B", false, 10)
)
```

### Video Platforms Category (10 websites)

```kotlin
val videoPlatformsWebsites = listOf(
    WebsiteEntity(36, "YouTube", "https://www.youtube.com", "VIDEO_PLATFORMS",
        "https://cdn.example.com/logos/youtube.png",
        "Video sharing platform", "#FF0000", false, 1),
    WebsiteEntity(37, "Twitch", "https://www.twitch.tv", "VIDEO_PLATFORMS",
        "https://cdn.example.com/logos/twitch.png",
        "Live streaming", "#9146FF", false, 2),
    WebsiteEntity(38, "Vimeo", "https://vimeo.com", "VIDEO_PLATFORMS",
        "https://cdn.example.com/logos/vimeo.png",
        "Video platform", "#1AB7EA", false, 3),
    WebsiteEntity(39, "TikTok", "https://www.tiktok.com", "VIDEO_PLATFORMS",
        "https://cdn.example.com/logos/tiktok.png",
        "Short-form videos", "#000000", false, 4),
    WebsiteEntity(40, "Dailymotion", "https://www.dailymotion.com", "VIDEO_PLATFORMS",
        "https://cdn.example.com/logos/dailymotion.png",
        "Video sharing", "#0066DC", false, 5),
    WebsiteEntity(41, "Rumble", "https://rumble.com", "VIDEO_PLATFORMS",
        "https://cdn.example.com/logos/rumble.png",
        "Video platform", "#85C742", false, 6),
    WebsiteEntity(42, "Odysee", "https://odysee.com", "VIDEO_PLATFORMS",
        "https://cdn.example.com/logos/odysee.png",
        "Decentralized video", "#EF1970", false, 7),
    WebsiteEntity(43, "BitChute", "https://www.bitchute.com", "VIDEO_PLATFORMS",
        "https://cdn.example.com/logos/bitchute.png",
        "Video platform", "#FF0000", false, 8),
    WebsiteEntity(44, "PeerTube", "https://joinpeertube.org", "VIDEO_PLATFORMS",
        "https://cdn.example.com/logos/peertube.png",
        "Federated video platform", "#F1680D", false, 9),
    WebsiteEntity(45, "LBRY", "https://lbry.com", "VIDEO_PLATFORMS",
        "https://cdn.example.com/logos/lbry.png",
        "Decentralized content", "#2F9176", false, 10)
)
```

## Performance Monitoring

### Metrics

- **App Startup Time**: Measured using `reportFullyDrawn()` in MainActivity
- **Frame Rate**: Monitor using `FrameMetricsAggregator`
- **Memory Usage**: Track with Android Profiler
- **APK Size**: Monitor with `bundletool` reports

### Tools

- Android Studio Profiler
- Firebase Performance Monitoring (optional)
- Custom analytics for user-facing metrics

### Implementation

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Report startup time
        window.decorView.post {
            reportFullyDrawn()
        }
        
        // Monitor frame metrics
        val frameMetricsAggregator = FrameMetricsAggregator()
        frameMetricsAggregator.add(this)
    }
}
```

## Error Taxonomy

```kotlin
sealed class AppError {
    data class NetworkError(val message: String) : AppError()
    data class DatabaseError(val message: String) : AppError()
    data class DownloadError(val message: String, val code: Int) : AppError()
    data class WebViewError(val errorCode: Int, val description: String) : AppError()
    data class PermissionError(val permission: String) : AppError()
    
    fun toUserMessage(): String = when (this) {
        is NetworkError -> "Network error: $message"
        is DatabaseError -> "Something went wrong. Please try again."
        is DownloadError -> "Download failed: $message"
        is WebViewError -> "Failed to load page: $description"
        is PermissionError -> "Permission required: $permission"
    }
}
```
