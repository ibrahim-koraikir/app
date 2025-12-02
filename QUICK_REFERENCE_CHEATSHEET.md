# Quick Reference Cheat Sheet

## Project Structure
```
app/src/main/java/com/yourapp/
├── core/          # Constants, errors, results
├── data/          # Repositories, DAOs, entities
├── di/            # Hilt modules
├── domain/        # Models, interfaces, use cases
├── presentation/  # UI, ViewModels, screens
└── util/          # Helpers, utilities
```

## Common Commands

### Build
```bash
./gradlew assembleDebug          # Debug build
./gradlew assembleRelease        # Release build
./gradlew installDebug           # Install on device
./gradlew clean                  # Clean build
```

### Testing
```bash
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests
./gradlew lint                   # Lint check
```

### ADB
```bash
adb devices                      # List devices
adb logcat                       # View logs
adb logcat | findstr "TAG"       # Filter logs
adb install app.apk              # Install APK
adb uninstall com.package.name   # Uninstall app
```

## Architecture Patterns

### Repository Pattern
```kotlin
// Interface (domain)
interface Repository {
    fun getData(): Flow<Data>
}

// Implementation (data)
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
    
    fun onEvent(event: Event) { /* handle */ }
}
```

### Composable Pattern
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
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

## Room Database

### Entity
```kotlin
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val value: String
)
```

### DAO
```kotlin
@Dao
interface ItemDao {
    @Query("SELECT * FROM items")
    fun getAll(): Flow<List<ItemEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity)
    
    @Update
    suspend fun update(item: ItemEntity)
    
    @Delete
    suspend fun delete(item: ItemEntity)
}
```

### Database
```kotlin
@Database(entities = [ItemEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
```

## Hilt Dependency Injection

### Application
```kotlin
@HiltAndroidApp
class MyApp : Application()
```

### Activity
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity()
```

### ViewModel
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel()
```

### Module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "db").build()
    }
}
```

## Jetpack Compose

### Basic Composables
```kotlin
// Text
Text(
    text = "Hello",
    style = MaterialTheme.typography.titleLarge,
    color = MaterialTheme.colorScheme.primary
)

// Button
Button(onClick = { /* action */ }) {
    Text("Click Me")
}

// Image
AsyncImage(
    model = imageUrl,
    contentDescription = "Description",
    modifier = Modifier.size(48.dp)
)

// LazyColumn
LazyColumn {
    items(list) { item ->
        ItemRow(item)
    }
}

// LazyGrid
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    contentPadding = PaddingValues(16.dp)
) {
    items(list) { item ->
        ItemCard(item)
    }
}
```

### State Management
```kotlin
// Remember
val state = remember { mutableStateOf("") }

// Remember with key
val state = remember(key) { mutableStateOf("") }

// StateFlow
val uiState by viewModel.uiState.collectAsState()

// LaunchedEffect
LaunchedEffect(key) {
    // Coroutine code
}

// DisposableEffect
DisposableEffect(key) {
    // Setup
    onDispose {
        // Cleanup
    }
}
```

### Modifiers
```kotlin
Modifier
    .fillMaxSize()
    .fillMaxWidth()
    .fillMaxHeight()
    .size(48.dp)
    .width(100.dp)
    .height(100.dp)
    .padding(16.dp)
    .padding(horizontal = 16.dp, vertical = 8.dp)
    .background(Color.Red)
    .clip(RoundedCornerShape(8.dp))
    .clickable { /* action */ }
    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
    .weight(1f)
    .align(Alignment.Center)
```

## Navigation

### Define Screens
```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Detail : Screen("detail/{id}") {
        fun createRoute(id: Int) = "detail/$id"
    }
}
```

### NavHost
```kotlin
NavHost(
    navController = navController,
    startDestination = Screen.Home.route
) {
    composable(Screen.Home.route) {
        HomeScreen(
            onNavigate = { id ->
                navController.navigate(Screen.Detail.createRoute(id))
            }
        )
    }
    
    composable(
        route = Screen.Detail.route,
        arguments = listOf(navArgument("id") { type = NavType.IntType })
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getInt("id")
        DetailScreen(id = id)
    }
}
```

### Navigation Actions
```kotlin
// Navigate
navController.navigate(route)

// Navigate with pop
navController.navigate(route) {
    popUpTo(Screen.Home.route) { inclusive = true }
}

// Pop back
navController.popBackStack()

// Pop to specific destination
navController.popBackStack(Screen.Home.route, inclusive = false)
```

## Coroutines & Flow

### Launch Coroutine
```kotlin
viewModelScope.launch {
    // Coroutine code
}

lifecycleScope.launch {
    // Coroutine code
}
```

### Flow Operations
```kotlin
flow
    .map { it.transform() }
    .filter { it.condition() }
    .catch { error -> /* handle */ }
    .collect { value -> /* use */ }
```

### StateFlow
```kotlin
private val _state = MutableStateFlow(InitialState)
val state: StateFlow<State> = _state.asStateFlow()

// Update
_state.update { it.copy(newValue = value) }
_state.value = newState
```

## Material Design 3

### Colors
```kotlin
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.onPrimary
MaterialTheme.colorScheme.background
MaterialTheme.colorScheme.surface
MaterialTheme.colorScheme.error
```

### Typography
```kotlin
MaterialTheme.typography.displayLarge
MaterialTheme.typography.headlineLarge
MaterialTheme.typography.titleLarge
MaterialTheme.typography.bodyLarge
MaterialTheme.typography.labelLarge
```

### Components
```kotlin
// Card
Card(
    onClick = { /* action */ },
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )
) { /* content */ }

// Scaffold
Scaffold(
    topBar = { TopAppBar() },
    bottomBar = { BottomNavigationBar() },
    floatingActionButton = { FAB() }
) { paddingValues ->
    Content(Modifier.padding(paddingValues))
}

// Dialog
AlertDialog(
    onDismissRequest = { /* dismiss */ },
    title = { Text("Title") },
    text = { Text("Message") },
    confirmButton = {
        TextButton(onClick = { /* confirm */ }) {
            Text("OK")
        }
    }
)
```

## WebView

### Basic Setup
```kotlin
@Composable
fun CustomWebView(url: String) {
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }
    }
    
    DisposableEffect(url) {
        webView.loadUrl(url)
        onDispose { webView.destroy() }
    }
    
    AndroidView(factory = { webView })
}
```

### WebViewClient
```kotlin
webView.webViewClient = object : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        // Page started loading
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        // Page finished loading
    }
    
    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        // Handle URL loading
        return false
    }
    
    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        // Intercept requests (for ad-blocking)
        return null
    }
}
```

## Testing

### Unit Test
```kotlin
@Test
fun `test description`() = runTest {
    // Arrange
    val input = "test"
    
    // Act
    val result = function(input)
    
    // Assert
    assertEquals(expected, result)
}
```

### MockK
```kotlin
// Mock
val mock = mockk<Repository>()

// Stub
coEvery { mock.getData() } returns flowOf(data)
every { mock.getValue() } returns value

// Verify
coVerify { mock.getData() }
verify { mock.getValue() }
```

### Compose Test
```kotlin
@Test
fun testComposable() {
    composeTestRule.setContent {
        MyComposable()
    }
    
    composeTestRule
        .onNodeWithText("Text")
        .assertIsDisplayed()
        .performClick()
}
```

## Common Issues & Solutions

### Issue: Blank screen in WebView
**Solution**: Don't recreate WebView on URL changes
```kotlin
val webView = remember { WebView(context) }  // No key!
```

### Issue: State not updating
**Solution**: Use StateFlow and collect properly
```kotlin
val state by viewModel.state.collectAsState()
```

### Issue: Hilt injection fails
**Solution**: Add @HiltAndroidApp to Application class

### Issue: Room migration error
**Solution**: Increment version and add migration or use fallbackToDestructiveMigration()

### Issue: Compose recomposition too often
**Solution**: Use remember, derivedStateOf, or stable classes

## Performance Tips

1. **Use remember**: Cache expensive computations
2. **Use LazyColumn**: Don't use Column with many items
3. **Use keys**: Provide stable keys for LazyColumn items
4. **Avoid nested scrolling**: Use single scrollable container
5. **Use derivedStateOf**: For computed state
6. **Profile**: Use Android Profiler to find bottlenecks
7. **Optimize images**: Use appropriate sizes and formats
8. **Use ProGuard**: Enable for release builds

## Security Best Practices

1. **HTTPS only**: Enforce secure connections
2. **Validate input**: Check all user input
3. **Use SafeBrowsing**: Enable in WebView
4. **Disable file access**: In WebView settings
5. **ProGuard**: Obfuscate code
6. **No hardcoded secrets**: Use BuildConfig or secure storage
7. **Permissions**: Request only what's needed

## Useful Extensions

```kotlin
// Context extensions
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

// String extensions
fun String.isValidUrl(): Boolean {
    return URLUtil.isValidUrl(this)
}

// Flow extensions
fun <T> Flow<T>.stateInWhileSubscribed(
    scope: CoroutineScope,
    initialValue: T
): StateFlow<T> = stateIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = initialValue
)
```

## Keyboard Shortcuts (Android Studio)

- `Ctrl + Space`: Code completion
- `Ctrl + Shift + Space`: Smart completion
- `Ctrl + /`: Comment line
- `Ctrl + Shift + /`: Block comment
- `Ctrl + Alt + L`: Format code
- `Ctrl + Alt + O`: Optimize imports
- `Shift + F10`: Run
- `Shift + F9`: Debug
- `Ctrl + F9`: Build
- `Alt + Enter`: Show intention actions
- `Ctrl + B`: Go to declaration
- `Ctrl + Alt + B`: Go to implementation
- `Ctrl + Shift + F`: Find in files
- `Ctrl + R`: Replace
- `Ctrl + Shift + R`: Replace in files

---

## Quick Links

- [Android Developers](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Kotlin Docs](https://kotlinlang.org/docs/home.html)
- [Hilt](https://dagger.dev/hilt/)
- [Room](https://developer.android.com/training/data-storage/room)
- [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

---

**Pro Tip**: Bookmark this page for quick reference while coding! 📚
