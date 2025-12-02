# Ad-Blocking System Design

## Overview

The ad-blocking system implements a three-layer architecture to intercept and block advertisements, trackers, and analytics in the Entertainment Browser's WebView component. The design prioritizes performance (O(1) lookups), reliability (graceful degradation), and maintainability (easy customization).

### Design Goals

1. **Performance**: Block ads with <1ms URL checks and <1s initialization
2. **Effectiveness**: Block 85-95% of ads and trackers
3. **Reliability**: Never crash WebView, fail gracefully on errors
4. **Integration**: Work seamlessly with existing CustomWebView
5. **Maintainability**: Easy to add/remove blocking rules

### Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                     │
│  (EntertainmentBrowserApp - Preload on startup)         │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  Presentation Layer                      │
│  (CustomWebView - AdBlockWebViewClient integration)     │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                   Ad-Blocking Layer                      │
│  ┌──────────────────────────────────────────────────┐  │
│  │  AdBlockWebViewClient (Request Interceptor)      │  │
│  └──────────────┬───────────────────────────────────┘  │
│                 │                                        │
│  ┌──────────────▼───────────────────────────────────┐  │
│  │  FastAdBlockEngine (Primary - HashSet O(1))      │  │
│  │  - blockedDomains: HashSet<String>               │  │
│  │  - blockedPatterns: HashSet<String>              │  │
│  │  - allowedDomains: HashSet<String>               │  │
│  └──────────────┬───────────────────────────────────┘  │
│                 │                                        │
│  ┌──────────────▼───────────────────────────────────┐  │
│  │  HardcodedFilters (Fallback - 1000+ domains)     │  │
│  │  - adDomains: Set<String>                        │  │
│  │  - adKeywords: List<String>                      │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    Assets Layer                          │
│  - assets/adblock/easylist.txt (~2MB)                   │
│  - assets/adblock/easyprivacy.txt (~1.5MB)              │
└─────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. FastAdBlockEngine

**Purpose**: Primary blocking engine using HashSet-based lookups for O(1) performance.

**Location**: `app/src/main/java/com/entertainmentbrowser/util/adblock/FastAdBlockEngine.kt`

**Responsibilities**:
- Load and parse filter lists from assets
- Maintain HashSets of blocked/allowed domains and patterns
- Provide fast URL checking (<1ms per check)
- Handle exceptions and whitelisting

**Key Methods**:

```kotlin
class FastAdBlockEngine(private val context: Context) {
    
    // Singleton instance
    companion object {
        fun getInstance(context: Context): FastAdBlockEngine
    }
    
    // Preload filter lists (call from Application.onCreate)
    fun preloadFromAssets()
    
    // Check if URL should be blocked (O(1) lookup)
    fun shouldBlock(url: String): Boolean
    
    // Private helpers
    private fun parseFastRule(line: String)
    private fun extractDomain(url: String): String?
}
```

**Data Structures**:

```kotlin
// Fast O(1) lookups using HashSet
private val blockedDomains = HashSet<String>()      // ~3,000-5,000 domains
private val blockedPatterns = HashSet<String>()     // ~1,000-2,000 patterns
private val allowedDomains = HashSet<String>()      // ~100-500 exceptions
private val whitelistedDomains = HashSet<String>()  // Custom whitelist
```

**Filter List Parsing Strategy**:

The engine extracts only simple, fast-to-check rules:

1. **Domain Rules** (`||domain.com^`):
   - Extract domain between `||` and `^`
   - Add to `blockedDomains` HashSet
   - Example: `||doubleclick.net^` → `"doubleclick.net"`

2. **Exception Rules** (`@@||domain.com^`):
   - Detect `@@` prefix
   - Add to `allowedDomains` HashSet
   - Example: `@@||trusted-site.com^` → `"trusted-site.com"`

3. **Simple Patterns** (no wildcards, no regex):
   - Extract patterns without `*`, `/`, or complex syntax
   - Add to `blockedPatterns` HashSet
   - Example: `/advertising/` → `"advertising"`

4. **Skipped Rules**:
   - Element hiding (`##`, `#@#`) - not applicable to network requests
   - Complex regex patterns - too slow for real-time checking
   - Wildcard patterns - require expensive matching

**Performance Characteristics**:
- **Load Time**: 500-1000ms (background thread)
- **Memory**: 50-100MB (HashSets + strings)
- **Check Time**: <1ms per URL (O(1) HashSet lookup)
- **Rules Loaded**: 4,000-7,000 simple rules from 148,000+ total

### 2. HardcodedFilters

**Purpose**: Fallback filtering with 1000+ common ad/tracking domains.

**Location**: `app/src/main/java/com/entertainmentbrowser/util/adblock/HardcodedFilters.kt`

**Responsibilities**:
- Provide instant blocking without asset loading
- Catch common ads missed by filter lists
- Work even if filter list loading fails

**Key Methods**:

```kotlin
object HardcodedFilters {
    
    // Top 1000+ ad/tracking domains
    val adDomains: Set<String>
    
    // Common ad URL patterns
    val adKeywords: List<String>
    
    // Check if URL should be blocked
    fun shouldBlock(url: String): Boolean
}
```

**Domain Categories**:

```kotlin
val adDomains = setOf(
    // Google Ads (10+ domains)
    "doubleclick.net", "googleadservices.com", "googlesyndication.com",
    "google-analytics.com", "googletagmanager.com",
    
    // Facebook/Meta (5+ domains)
    "facebook.com/tr", "connect.facebook.net", "pixel.facebook.com",
    
    // Amazon (3+ domains)
    "amazon-adsystem.com", "aax.amazon-adsystem.com",
    
    // Major Ad Networks (50+ domains)
    "adnxs.com", "adsrvr.org", "criteo.com", "outbrain.com", "taboola.com",
    "pubmatic.com", "rubiconproject.com", "openx.net",
    
    // Analytics & Tracking (30+ domains)
    "scorecardresearch.com", "quantserve.com", "chartbeat.com",
    "hotjar.com", "mouseflow.com", "newrelic.com",
    
    // Video Ads (20+ domains)
    "2mdn.net", "adform.net", "adsafeprotected.com", "moatads.com",
    
    // ... (1000+ total domains)
)

val adKeywords = listOf(
    "/ads/", "/ad/", "/advert", "/banner", "/sponsor",
    "doubleclick", "adsystem", "adservice", "pagead",
    "analytics", "tracking", "tracker", "pixel", "beacon"
)
```

### 3. AdBlockWebViewClient

**Purpose**: Intercept WebView network requests and apply blocking rules.

**Location**: `app/src/main/java/com/entertainmentbrowser/presentation/webview/AdBlockWebViewClient.kt`

**Responsibilities**:
- Intercept all network requests via `shouldInterceptRequest`
- Check URLs against FastAdBlockEngine and HardcodedFilters
- Return empty response for blocked requests
- Track blocked request count per page
- Maintain existing WebViewClient functionality

**Key Methods**:

```kotlin
class AdBlockWebViewClient : WebViewClient() {
    
    private var fastEngine: FastAdBlockEngine? = null
    private var blockedCount = 0
    
    // Intercept requests (API 21+)
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse?
    
    // Intercept requests (API <21)
    override fun shouldInterceptRequest(
        view: WebView,
        url: String
    ): WebResourceResponse?
    
    // Check and block URL
    private fun checkAndBlock(url: String): WebResourceResponse?
    
    // Create empty response for blocked requests
    private fun createEmptyResponse(): WebResourceResponse
    
    // Get blocked count for current page
    fun getBlockedCount(): Int
    
    // Reset counter on page start
    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?)
    
    // Log blocked count on page finish
    override fun onPageFinished(view: WebView, url: String)
}
```

**Request Interception Flow**:

```
Network Request
    ↓
shouldInterceptRequest()
    ↓
checkAndBlock(url)
    ↓
┌─────────────────────────────────────┐
│ 1. Initialize engine (lazy)         │
│    if (fastEngine == null)          │
│        fastEngine = getInstance()   │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 2. Check FastAdBlockEngine          │
│    if (fastEngine.shouldBlock(url)) │
│        return createEmptyResponse() │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 3. Check HardcodedFilters           │
│    if (HardcodedFilters.shouldBlock)│
│        return createEmptyResponse() │
└──────────────┬──────────────────────┘
               ↓
┌─────────────────────────────────────┐
│ 4. Allow Request                    │
│    return null                      │
└─────────────────────────────────────┘
```

**Integration with Existing CustomWebView**:

The AdBlockWebViewClient will be composed with the existing WebViewClient functionality:

```kotlin
// Current CustomWebView creates anonymous WebViewClient
webViewClient = object : WebViewClient() {
    // Existing functionality...
}

// New design: AdBlockWebViewClient extends and adds blocking
class AdBlockWebViewClient(
    private val onVideoDetected: (String) -> Unit = {},
    private val onDrmDetected: () -> Unit = {},
    private val onLoadingChanged: (Boolean) -> Unit = {},
    private val onUrlChanged: (String) -> Unit = {},
    private val onNavigationStateChanged: (Boolean, Boolean) -> Unit = { _, _ -> },
    private val onError: (String) -> Unit = {},
    private val onPageFinished: () -> Unit = {}
) : WebViewClient() {
    // Ad-blocking + existing functionality
}
```

### 4. Application Integration

**Purpose**: Initialize ad-blocking engine at app startup.

**Location**: `app/src/main/java/com/entertainmentbrowser/EntertainmentBrowserApp.kt`

**Changes Required**:

```kotlin
@HiltAndroidApp
class EntertainmentBrowserApp : Application() {
    
    @Inject
    lateinit var websiteRepository: WebsiteRepository
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Existing initialization
        applicationScope.launch {
            websiteRepository.prepopulateWebsites()
        }
        scheduleTabCleanup()
        
        // NEW: Preload ad-blocking engine
        initializeAdBlocking()
    }
    
    private fun initializeAdBlocking() {
        try {
            Log.d("EntertainmentBrowserApp", "🚀 Preloading ad-blocker...")
            FastAdBlockEngine.getInstance(this).preloadFromAssets()
            Log.d("EntertainmentBrowserApp", "✅ Ad-blocker started")
        } catch (e: Exception) {
            Log.e("EntertainmentBrowserApp", "❌ Failed to start ad-blocker", e)
            // Continue without ad-blocking (graceful degradation)
        }
    }
}
```

## Data Models

### Filter List Format

**EasyList/EasyPrivacy Format**:

```
! Comment lines start with !
! Title: EasyList
! Version: 202411061234

! Domain blocking rule
||doubleclick.net^

! Exception rule (whitelist)
@@||trusted-site.com^

! Pattern rule
/advertising/*

! Element hiding (skipped by engine)
##.ad-banner

! Complex rule with options (skipped by engine)
||ads.example.com^$third-party,script
```

**Parsed Data Structures**:

```kotlin
// In-memory representation after parsing
data class ParsedRules(
    val blockedDomains: HashSet<String>,    // ["doubleclick.net", "ads.example.com"]
    val blockedPatterns: HashSet<String>,   // ["advertising", "pagead"]
    val allowedDomains: HashSet<String>     // ["trusted-site.com"]
)
```

### Blocked Request Response

```kotlin
// Empty response to block request
WebResourceResponse(
    mimeType = "text/plain",
    encoding = "UTF-8",
    data = ByteArrayInputStream(ByteArray(0))  // Empty byte array
)
```

## Error Handling

### Error Scenarios and Responses

1. **Filter List Loading Fails**:
   ```kotlin
   try {
       context.assets.open("adblock/easylist.txt")
   } catch (e: Exception) {
       Log.e(TAG, "Failed to load filter list", e)
       // Continue with HardcodedFilters only
       isInitialized = false
   }
   ```

2. **URL Parsing Fails**:
   ```kotlin
   fun shouldBlock(url: String): Boolean {
       try {
           val domain = extractDomain(url)
           // ... check domain
       } catch (e: Exception) {
           return false  // Allow on error
       }
   }
   ```

3. **Request Interception Fails**:
   ```kotlin
   override fun shouldInterceptRequest(...): WebResourceResponse? {
       try {
           return checkAndBlock(url)
       } catch (e: Exception) {
           Log.e(TAG, "Error checking URL", e)
           return null  // Allow request on error
       }
   }
   ```

4. **Engine Not Initialized**:
   ```kotlin
   fun shouldBlock(url: String): Boolean {
       if (!isInitialized) {
           return false  // Allow if not ready
       }
       // ... proceed with checking
   }
   ```

### Graceful Degradation Strategy

```
┌─────────────────────────────────────┐
│ FastAdBlockEngine Loaded?           │
│ ├─ Yes → Use HashSet blocking       │
│ └─ No  → Use HardcodedFilters only  │
└─────────────────────────────────────┘
         ↓
┌─────────────────────────────────────┐
│ HardcodedFilters Available?         │
│ ├─ Yes → Block common ad domains    │
│ └─ No  → Allow all (no blocking)    │
└─────────────────────────────────────┘
         ↓
┌─────────────────────────────────────┐
│ Error During Check?                 │
│ ├─ Yes → Allow request (safe)       │
│ └─ No  → Apply blocking decision    │
└─────────────────────────────────────┘
```

## Testing Strategy

### Unit Tests

**FastAdBlockEngineTest.kt**:
```kotlin
class FastAdBlockEngineTest {
    
    @Test
    fun `shouldBlock returns true for known ad domain`()
    
    @Test
    fun `shouldBlock returns false for allowed domain`()
    
    @Test
    fun `shouldBlock returns true for pattern match`()
    
    @Test
    fun `shouldBlock returns false for whitelisted domain`()
    
    @Test
    fun `shouldBlock returns false when not initialized`()
    
    @Test
    fun `extractDomain returns correct domain from URL`()
    
    @Test
    fun `parseFastRule correctly parses domain rules`()
    
    @Test
    fun `parseFastRule correctly parses exception rules`()
    
    @Test
    fun `shouldBlock handles malformed URLs gracefully`()
}
```

**HardcodedFiltersTest.kt**:
```kotlin
class HardcodedFiltersTest {
    
    @Test
    fun `shouldBlock returns true for Google Ads domain`()
    
    @Test
    fun `shouldBlock returns true for Facebook tracking`()
    
    @Test
    fun `shouldBlock returns true for URL with ad keyword`()
    
    @Test
    fun `shouldBlock returns false for legitimate domain`()
    
    @Test
    fun `adDomains contains at least 1000 entries`()
}
```

**AdBlockWebViewClientTest.kt**:
```kotlin
class AdBlockWebViewClientTest {
    
    @Test
    fun `shouldInterceptRequest blocks known ad URL`()
    
    @Test
    fun `shouldInterceptRequest allows legitimate URL`()
    
    @Test
    fun `createEmptyResponse returns valid empty response`()
    
    @Test
    fun `getBlockedCount returns correct count`()
    
    @Test
    fun `blockedCount resets on page start`()
    
    @Test
    fun `shouldInterceptRequest handles exceptions gracefully`()
}
```

### Integration Tests

**AdBlockingIntegrationTest.kt**:
```kotlin
@RunWith(AndroidJUnit4::class)
class AdBlockingIntegrationTest {
    
    @Test
    fun `WebView with AdBlockWebViewClient blocks ads on real page`()
    
    @Test
    fun `FastAdBlockEngine loads filter lists successfully`()
    
    @Test
    fun `Ad-blocking works with video detection`()
    
    @Test
    fun `Ad-blocking preserves existing WebView functionality`()
    
    @Test
    fun `Blocked count updates correctly during page load`()
}
```

### Performance Tests

**AdBlockingPerformanceTest.kt**:
```kotlin
class AdBlockingPerformanceTest {
    
    @Test
    fun `filter list loading completes within 1 second`()
    
    @Test
    fun `URL check completes within 1 millisecond`()
    
    @Test
    fun `memory usage stays under 100MB`()
    
    @Test
    fun `blocking does not slow down page load significantly`()
}
```

### Manual Testing Checklist

1. **Ad Blocking Effectiveness**:
   - [ ] Visit forbes.com - verify banner ads blocked
   - [ ] Visit cnn.com - verify video ads blocked
   - [ ] Visit dailymail.co.uk - verify popups blocked
   - [ ] Visit adblock-tester.com - verify 85%+ blocking rate

2. **Performance**:
   - [ ] Check Logcat for load time (<1s)
   - [ ] Verify smooth scrolling on ad-heavy sites
   - [ ] Monitor memory usage (<100MB increase)

3. **Functionality**:
   - [ ] Verify video detection still works
   - [ ] Verify downloads still work
   - [ ] Verify navigation (back/forward) works
   - [ ] Verify tab management works

4. **Error Handling**:
   - [ ] Delete filter lists - verify fallback to HardcodedFilters
   - [ ] Test with malformed URLs - verify no crashes
   - [ ] Test with HTTPS enforcement - verify blocking works

## Implementation Phases

### Phase 1: Core Engine (Requirements 1, 2, 5, 9)
- Create FastAdBlockEngine with HashSet-based blocking
- Implement filter list parsing (domain rules only)
- Add exception rule handling
- Implement error handling and graceful degradation

### Phase 2: Fallback Filters (Requirements 4, 6)
- Create HardcodedFilters with 1000+ domains
- Add common ad keywords
- Implement shouldBlock method

### Phase 3: WebView Integration (Requirements 3, 7, 10)
- Create AdBlockWebViewClient
- Implement request interception
- Add blocked count tracking
- Integrate with existing CustomWebView functionality

### Phase 4: Application Initialization (Requirement 3)
- Add preload call to EntertainmentBrowserApp
- Implement background loading
- Add initialization logging

### Phase 5: Customization & Whitelisting (Requirements 6, 8)
- Add whitelist support to FastAdBlockEngine
- Document customization process
- Add configuration options

### Phase 6: Testing & Optimization (All Requirements)
- Write unit tests for all components
- Write integration tests
- Perform performance testing
- Manual testing on real websites

## Dependencies

### New Dependencies

None required - uses only Android SDK and existing project dependencies:
- `android.webkit.*` (WebView, WebViewClient, WebResourceResponse)
- `kotlin.collections.*` (HashSet)
- `java.io.*` (ByteArrayInputStream)

### Asset Files

Required filter lists (download during implementation):
- `app/src/main/assets/adblock/easylist.txt` (~2MB)
- `app/src/main/assets/adblock/easyprivacy.txt` (~1.5MB)

Download commands:
```bash
mkdir -p app/src/main/assets/adblock
curl -o app/src/main/assets/adblock/easylist.txt https://easylist.to/easylist/easylist.txt
curl -o app/src/main/assets/adblock/easyprivacy.txt https://easylist.to/easylist/easyprivacy.txt
```

## Configuration and Customization

### Adding Custom Domains

Edit `HardcodedFilters.kt`:
```kotlin
val adDomains = setOf(
    // Existing domains...
    
    // Custom additions
    "custom-ad-network.com",
    "another-tracker.com"
)
```

### Whitelisting Domains

Edit `FastAdBlockEngine.kt`:
```kotlin
private val whitelistedDomains = setOf(
    "trusted-analytics.com",
    "required-tracking.com"
)
```

### Adding More Filter Lists

1. Download additional filter list to assets:
```bash
curl -o app/src/main/assets/adblock/fanboy-social.txt \
  https://secure.fanboy.co.nz/fanboy-social.txt
```

2. Update `FastAdBlockEngine.kt`:
```kotlin
val filterFiles = listOf(
    "easylist.txt",
    "easyprivacy.txt",
    "fanboy-social.txt"  // Add new list
)
```

### Disabling Ad-Blocking

Comment out initialization in `EntertainmentBrowserApp.kt`:
```kotlin
override fun onCreate() {
    super.onCreate()
    // initializeAdBlocking()  // Disabled
}
```

Or use standard WebViewClient instead of AdBlockWebViewClient:
```kotlin
webViewClient = object : WebViewClient() {
    // Standard implementation without blocking
}
```

## Performance Considerations

### Memory Optimization

1. **Use HashSet instead of List**: O(1) vs O(n) lookups
2. **Extract simple rules only**: Avoid complex regex patterns
3. **Lazy initialization**: Load engine only when needed
4. **Singleton pattern**: One engine instance for entire app

### CPU Optimization

1. **Background loading**: Parse filter lists in background thread
2. **Fast domain extraction**: Simple string operations, no regex
3. **Early returns**: Check whitelist before blocking rules
4. **Minimal logging**: Remove debug logs in production

### Network Optimization

1. **Block early**: Prevent ad requests from starting
2. **Empty responses**: Minimal data transfer for blocked requests
3. **Reduce request count**: 85-95% fewer network requests

### Expected Performance Metrics

- **Initialization**: 500-1000ms (background, non-blocking)
- **Memory**: 50-100MB (HashSets + strings)
- **URL Check**: <1ms (O(1) HashSet lookup)
- **Page Load**: 20-40% faster (fewer requests)
- **Blocking Rate**: 85-95% of ads/trackers

## Security Considerations

### Filter List Integrity

- Filter lists bundled in assets (not downloaded at runtime)
- No remote code execution risk
- No user data collection

### Privacy Protection

- Blocks tracking scripts (Google Analytics, Facebook Pixel, etc.)
- Blocks analytics beacons
- No ad-blocking data sent to external servers
- All processing happens locally

### WebView Security

- Maintains existing HTTPS enforcement
- Maintains existing safe browsing
- Does not weaken security settings
- Graceful degradation on errors (allow vs block)

## Monitoring and Logging

### Development Logging

```kotlin
// FastAdBlockEngine
Log.d(TAG, "✅ Loaded in ${duration}ms")
Log.d(TAG, "   Blocked domains: ${blockedDomains.size}")
Log.d(TAG, "   Blocked patterns: ${blockedPatterns.size}")

// AdBlockWebViewClient
Log.d(TAG, "Blocked $blockedCount requests")
Log.d(TAG, "Blocked URL: $url")

// EntertainmentBrowserApp
Log.d(TAG, "🚀 Preloading ad-blocker...")
Log.d(TAG, "✅ Ad-blocker started")
```

### Production Logging

Remove or disable debug logs:
```kotlin
// Use BuildConfig to control logging
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Debug info")
}
```

### Metrics to Track

- Filter list load time
- Number of rules loaded
- Blocked request count per page
- Memory usage
- Initialization success/failure rate

## Future Enhancements

### Potential Improvements

1. **User Settings**:
   - Toggle ad-blocking on/off
   - Whitelist specific websites
   - View blocked count in UI

2. **Additional Filter Lists**:
   - Fanboy Social (block social widgets)
   - Fanboy Annoyance (block cookie banners)
   - Regional lists (language-specific ads)

3. **Advanced Blocking**:
   - Cosmetic filtering (hide ad placeholders)
   - Script injection blocking
   - Cookie consent banner removal

4. **Performance Optimization**:
   - Bloom filter for faster initial checks
   - Compressed filter lists
   - Incremental filter list updates

5. **Analytics**:
   - Track blocking effectiveness per website
   - Report most blocked domains
   - Measure page load time improvement

### Not Planned

- Complex regex pattern matching (too slow)
- Element hiding/cosmetic filtering (requires DOM manipulation)
- Remote filter list updates (security/privacy concerns)
- Cloud-based blocking (privacy concerns)
