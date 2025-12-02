# Build Your Own Entertainment Browser - Step-by-Step Guide

This guide will walk you through building a similar app from scratch.

## Phase 1: Project Setup (Day 1)

### 1.1 Create New Android Project
```
File → New → New Project
Select: Empty Activity (Compose)
Name: YourAppName
Package: com.yourcompany.yourapp
Minimum SDK: 24
Language: Kotlin
Build configuration language: Kotlin DSL
```

### 1.2 Setup Version Catalog
Create `gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.13.0"
kotlin = "2.0.21"
compose-bom = "2024.10.00"
hilt = "2.50"
room = "2.6.1"
# ... (see COMPLETE_APP_DOCUMENTATION.md for full list)

[libraries]
# Add all libraries

[plugins]
# Add all plugins
```

### 1.3 Configure build.gradle.kts
```kotlin
// Project level
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

// App level
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

dependencies {
    // Add all dependencies
}
```

### 1.4 Create Package Structure
```
com.yourcompany.yourapp/
├── core/
├── data/
├── di/
├── domain/
├── presentation/
└── util/
```

---

## Phase 2: Domain Layer (Day 2-3)

### 2.1 Create Domain Models
```kotlin
// domain/model/Website.kt
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

### 2.2 Create Repository Interfaces
```kotlin
// domain/repository/WebsiteRepository.kt
interface WebsiteRepository {
    fun getAllWebsites(): Flow<List<Website>>
    fun getWebsitesByCategory(category: Category): Flow<List<Website>>
    fun getFavoriteWebsites(): Flow<List<Website>>
    suspend fun toggleFavorite(websiteId: Int)
    suspend fun prepopulateWebsites()
}
```

### 2.3 Create Use Cases
```kotlin
// domain/usecase/GetAllWebsitesUseCase.kt
class GetAllWebsitesUseCase @Inject constructor(
    private val repository: WebsiteRepository
) {
    operator fun invoke(): Flow<List<Website>> {
        return repository.getAllWebsites()
    }
}
```

---

## Phase 3: Data Layer (Day 4-5)

### 3.1 Create Room Entities
```kotlin
// data/local/entity/WebsiteEntity.kt
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

// Extension function
fun WebsiteEntity.toDomain() = Website(
    id = id,
    name = name,
    url = url,
    category = Category.valueOf(category),
    logoUrl = logoUrl,
    description = description,
    backgroundColor = backgroundColor,
    isFavorite = isFavorite,
    order = order
)
```

### 3.2 Create DAOs
```kotlin
// data/local/dao/WebsiteDao.kt
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
    
    @Query("SELECT * FROM websites WHERE id = :id")
    suspend fun getWebsiteById(id: Int): WebsiteEntity?
}
```

### 3.3 Create Database
```kotlin
// data/local/database/AppDatabase.kt
@Database(
    entities = [WebsiteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun websiteDao(): WebsiteDao
}
```

### 3.4 Create Prepopulate Data
```kotlin
// data/local/PrepopulateData.kt
object PrepopulateData {
    fun getWebsites(): List<WebsiteEntity> = listOf(
        WebsiteEntity(
            id = 1,
            name = "Netflix",
            url = "https://www.netflix.com",
            category = "STREAMING",
            logoUrl = "https://cdn.worldvectorlogo.com/logos/netflix-3.svg",
            description = "Watch TV shows and movies",
            backgroundColor = "#E50914",
            isFavorite = false,
            order = 1
        ),
        // Add more websites...
    )
}
```

### 3.5 Implement Repository
```kotlin
// data/repository/WebsiteRepositoryImpl.kt
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
        val website = websiteDao.getWebsiteById(websiteId)
        website?.let {
            websiteDao.updateWebsite(it.copy(isFavorite = !it.isFavorite))
        }
    }
    
    override suspend fun prepopulateWebsites() {
        val existingWebsites = websiteDao.getAllWebsites().first()
        if (existingWebsites.isEmpty()) {
            websiteDao.insertAll(PrepopulateData.getWebsites())
        }
    }
}
```

---

## Phase 4: Dependency Injection (Day 6)

### 4.1 Create Database Module
```kotlin
// di/DatabaseModule.kt
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
        ).build()
    }
    
    @Provides
    fun provideWebsiteDao(database: AppDatabase): WebsiteDao {
        return database.websiteDao()
    }
}
```

### 4.2 Create Repository Module
```kotlin
// di/RepositoryModule.kt
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

### 4.3 Create Application Class
```kotlin
// YourApp.kt
@HiltAndroidApp
class YourApp : Application() {
    
    @Inject
    lateinit var websiteRepository: WebsiteRepository
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Prepopulate database
        applicationScope.launch {
            websiteRepository.prepopulateWebsites()
        }
    }
}
```

---

## Phase 5: Presentation Layer - Theme (Day 7)

### 5.1 Define Colors
```kotlin
// presentation/theme/Color.kt
val RedPrimary = Color(0xFFFD1D1D)
val RedPrimaryVariant = Color(0xFFB71C1C)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB3B3B3)
val TextTertiary = Color(0xFF666666)
val AccentPurple = Color(0xFF9C27B0)
val AccentOrange = Color(0xFFFF9800)
val ErrorRed = Color(0xFFCF6679)
```

### 5.2 Create Theme
```kotlin
// presentation/theme/Theme.kt
private val DarkColorScheme = darkColorScheme(
    primary = RedPrimary,
    onPrimary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary
)

@Composable
fun YourAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
```

---

## Phase 6: Presentation Layer - Home Screen (Day 8-9)

### 6.1 Create UI State
```kotlin
// presentation/home/HomeUiState.kt
data class HomeUiState(
    val websites: List<Website> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: Category? = null
)
```

### 6.2 Create Events
```kotlin
// presentation/home/HomeEvent.kt
sealed interface HomeEvent {
    data class ToggleFavorite(val websiteId: Int) : HomeEvent
    data class SearchQueryChanged(val query: String) : HomeEvent
    data class CategorySelected(val category: Category?) : HomeEvent
    data class WebsiteClicked(val url: String) : HomeEvent
}
```

### 6.3 Create ViewModel
```kotlin
// presentation/home/HomeViewModel.kt
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllWebsitesUseCase: GetAllWebsitesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadWebsites()
    }
    
    private fun loadWebsites() {
        viewModelScope.launch {
            getAllWebsitesUseCase()
                .catch { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
                .collect { websites ->
                    _uiState.update { 
                        it.copy(
                            websites = filterWebsites(websites),
                            isLoading = false
                        )
                    }
                }
        }
    }
    
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.ToggleFavorite -> {
                viewModelScope.launch {
                    toggleFavoriteUseCase(event.websiteId)
                }
            }
            is HomeEvent.SearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is HomeEvent.CategorySelected -> {
                _uiState.update { it.copy(selectedCategory = event.category) }
            }
            is HomeEvent.WebsiteClicked -> {
                // Handle navigation
            }
        }
    }
    
    private fun filterWebsites(websites: List<Website>): List<Website> {
        val state = _uiState.value
        return websites.filter { website ->
            val matchesSearch = state.searchQuery.isEmpty() || 
                website.name.contains(state.searchQuery, ignoreCase = true)
            val matchesCategory = state.selectedCategory == null || 
                website.category == state.selectedCategory
            matchesSearch && matchesCategory
        }
    }
}
```

### 6.4 Create Screen
```kotlin
// presentation/home/HomeScreen.kt
@Composable
fun HomeScreen(
    onNavigateToWebView: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            HomeTopBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChanged = { query ->
                    viewModel.onEvent(HomeEvent.SearchQueryChanged(query))
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                ErrorState(error = uiState.error!!)
            }
            else -> {
                WebsiteGrid(
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

@Composable
fun WebsiteCard(
    website: Website,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(website.backgroundColor))
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AsyncImage(
                    model = website.logoUrl,
                    contentDescription = website.name,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = website.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (website.isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = "Favorite",
                    tint = Color.White
                )
            }
        }
    }
}
```

---

## Phase 7: WebView Implementation (Day 10-11)

### 7.1 Create Custom WebView
```kotlin
// presentation/webview/CustomWebView.kt
@Composable
fun CustomWebView(
    url: String,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }
    
    DisposableEffect(Unit) {
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                safeBrowsingEnabled = true
            }
            
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    onLoadingChanged(true)
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    onLoadingChanged(false)
                }
            }
        }
        
        onWebViewCreated(webView)
        
        onDispose {
            webView.destroy()
        }
    }
    
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

### 7.2 Create WebView Screen
```kotlin
// presentation/webview/WebViewScreen.kt
@Composable
fun WebViewScreen(
    onNavigateBack: () -> Unit,
    viewModel: WebViewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    Scaffold(
        topBar = {
            WebViewToolbar(
                title = uiState.title,
                isLoading = uiState.isLoading,
                canGoBack = uiState.canGoBack,
                canGoForward = uiState.canGoForward,
                onNavigateBack = { webViewRef?.goBack() },
                onNavigateForward = { webViewRef?.goForward() },
                onRefresh = { webViewRef?.reload() },
                onClose = onNavigateBack
            )
        }
    ) { paddingValues ->
        CustomWebView(
            url = uiState.url,
            onWebViewCreated = { webView ->
                webViewRef = webView
            },
            onLoadingChanged = { isLoading ->
                viewModel.onEvent(WebViewEvent.UpdateLoading(isLoading))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}
```

---

## Phase 8: Navigation Setup (Day 12)

### 8.1 Define Screens
```kotlin
// presentation/navigation/Screen.kt
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object WebView : Screen("webview/{url}") {
        fun createRoute(url: String): String {
            return "webview/${Uri.encode(url)}"
        }
    }
}
```

### 8.2 Create NavHost
```kotlin
// presentation/navigation/AppNavHost.kt
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
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
    }
}
```

### 8.3 Setup MainActivity
```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            YourAppTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
```

---

## Phase 9: Testing (Day 13)

### 9.1 Unit Tests
```kotlin
// test/HomeViewModelTest.kt
@Test
fun `when websites loaded, state is updated`() = runTest {
    // Arrange
    val websites = listOf(/* test data */)
    coEvery { getAllWebsitesUseCase() } returns flowOf(websites)
    
    // Act
    val viewModel = HomeViewModel(getAllWebsitesUseCase, toggleFavoriteUseCase)
    
    // Assert
    assertEquals(websites, viewModel.uiState.value.websites)
}
```

### 9.2 Integration Tests
```kotlin
// androidTest/DatabaseTest.kt
@Test
fun insertAndRetrieveWebsite() = runTest {
    // Arrange
    val website = WebsiteEntity(/* test data */)
    
    // Act
    dao.insertAll(listOf(website))
    val result = dao.getAllWebsites().first()
    
    // Assert
    assertEquals(1, result.size)
    assertEquals(website, result[0])
}
```

---

## Phase 10: Polish & Deploy (Day 14-15)

### 10.1 Add Splash Screen
### 10.2 Add App Icon
### 10.3 Configure ProGuard
### 10.4 Generate Signed APK
### 10.5 Test on Multiple Devices

---

## Checklist

- [ ] Project setup complete
- [ ] Domain layer implemented
- [ ] Data layer implemented
- [ ] Dependency injection configured
- [ ] Theme created
- [ ] Home screen working
- [ ] WebView working
- [ ] Navigation working
- [ ] Tests written
- [ ] App polished
- [ ] APK generated

---

## Tips

1. **Start Simple**: Build basic features first
2. **Test Early**: Write tests as you go
3. **Use Git**: Commit frequently
4. **Follow Patterns**: Stick to MVVM and Clean Architecture
5. **Read Docs**: Check official Android documentation
6. **Debug Often**: Use Logcat and debugger
7. **Optimize Later**: Make it work first, then optimize

---

## Resources

- [Android Developers](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Room Documentation](https://developer.android.com/training/data-storage/room)
- [Material Design 3](https://m3.material.io/)

---

Good luck building your app! 🚀
