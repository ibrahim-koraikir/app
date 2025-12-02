# HTML/Tailwind to Compose Translation Guide

## Overview

This guide maps the existing HTML/Tailwind CSS designs in the `screens/` folder to Jetpack Compose with Material 3 components.

## Color Palette

### From HTML Designs

```kotlin
// Primary Colors
val AccentRed = Color(0xFFFF0000)        // #FF0000 or #FD1D1D
val AccentRedAlt = Color(0xFFFF3B30)     // #ff3b30 (settings)

// Background Colors
val BackgroundDark = Color(0xFF0D1117)   // Deep blue/black
val BackgroundDarkAlt = Color(0xFF101622) // Alternative dark
val BackgroundDarkest = Color(0xFF121212) // Darkest background
val CardDark = Color(0xFF1E1E1E)         // Card background

// Gradient Colors
val GradientStart = Color(0xFF0D1117)
val GradientMid = Color(0xFF0D172E)
val GradientEnd = Color(0xFF08235F)

// Brand Colors (from home.html)
val NetflixRed = Color(0xFFE50914)
val DisneyBlue = Color(0xFF113CCF)
val ImdbYellow = Color(0xFFF5C518)
val GoodreadsBrown = Color(0xFFA0522D)

// Text Colors
val TextWhite = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF94A3B8)
val TextGray = Color(0xFF6C757D)
```

## Common Tailwind → Compose Mappings

### Spacing

```kotlin
// Tailwind → Compose
p-2  → Modifier.padding(8.dp)
p-4  → Modifier.padding(16.dp)
p-6  → Modifier.padding(24.dp)
px-4 → Modifier.padding(horizontal = 16.dp)
py-3 → Modifier.padding(vertical = 12.dp)
gap-4 → Arrangement.spacedBy(16.dp)
space-y-6 → Arrangement.spacedBy(24.dp)
```

### Sizing

```kotlin
// Tailwind → Compose
w-full → Modifier.fillMaxWidth()
h-full → Modifier.fillMaxHeight()
h-12 → Modifier.height(48.dp)
size-12 → Modifier.size(48.dp)
min-h-screen → Modifier.fillMaxSize()
```

### Borders & Corners

```kotlin
// Tailwind → Compose
rounded → Modifier.clip(RoundedCornerShape(8.dp))
rounded-lg → Modifier.clip(RoundedCornerShape(12.dp))
rounded-xl → Modifier.clip(RoundedCornerShape(16.dp))
rounded-2xl → Modifier.clip(RoundedCornerShape(24.dp))
rounded-full → Modifier.clip(CircleShape)

border → Modifier.border(1.dp, color, shape)
border-2 → Modifier.border(2.dp, color, shape)
```

### Shadows

```kotlin
// Tailwind → Compose
shadow-lg → Modifier.shadow(8.dp, shape)
shadow-deep-dark → Modifier.shadow(
    elevation = 10.dp,
    shape = RoundedCornerShape(24.dp),
    ambientColor = Color.Black.copy(alpha = 0.3f)
)
```

### Backgrounds

```kotlin
// Tailwind → Compose
bg-primary → Modifier.background(AccentRed)
bg-white/10 → Modifier.background(Color.White.copy(alpha = 0.1f))
bg-black/80 → Modifier.background(Color.Black.copy(alpha = 0.8f))

// Gradients
bg-gradient-to-b → Modifier.background(
    Brush.verticalGradient(
        colors = listOf(GradientStart, GradientEnd)
    )
)
```

## Screen-Specific Translations

### 1. Welcome Screen (welcome.html)

#### HTML Structure
```html
<div class="relative flex flex-col min-h-screen">
  <div class="absolute inset-0 bg-cover bg-center">
    <div class="absolute inset-0 bg-gradient-deep-blue"></div>
  </div>
  <div class="relative z-10 px-6 pb-12 text-center">
    <!-- Content -->
  </div>
</div>
```

#### Compose Equivalent
```kotlin
@Composable
fun WelcomeScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        AsyncImage(
            model = backgroundImageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D1117).copy(alpha = 0.4f),
                            Color(0xFF0D1117).copy(alpha = 0.85f)
                        )
                    )
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Your Entertainment Universe",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            // ... rest of content
        }
    }
}
```

### 2. Home Screen (home.html)

#### Website Card HTML
```html
<div class="group relative overflow-hidden rounded-2xl border border-netflix-red/50 
     shadow-deep-dark hover:shadow-card-glow-netflix transition-all">
  <img class="absolute inset-0 h-full w-full object-cover opacity-30" />
  <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/50 to-transparent"></div>
  <button class="absolute top-2 right-2">
    <span class="material-symbols-outlined">bookmark_border</span>
  </button>
  <div class="relative flex flex-col justify-end h-56 p-3">
    <!-- Content -->
  </div>
</div>
```

#### Compose Equivalent
```kotlin
@Composable
fun WebsiteCard(website: Website) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = Color(website.buttonColor.toColorInt()).copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background image
            AsyncImage(
                model = website.logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.3f),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
            
            // Bookmark button
            IconButton(
                onClick = { /* toggle favorite */ },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (website.isFavorite) 
                        Icons.Default.Bookmark 
                    else 
                        Icons.Default.BookmarkBorder,
                    contentDescription = "Favorite",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = website.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = website.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD1D5DB),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(website.buttonColor.toColorInt()).copy(alpha = 0.2f),
                    border = BorderStroke(
                        1.dp,
                        Color(website.buttonColor.toColorInt())
                    )
                ) {
                    Text(
                        text = website.category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
```

### 3. Settings Screen (settings.html)

#### Setting Item HTML
```html
<div class="setting-item dark-card">
  <div>
    <p class="setting-label">Download on Wi-Fi only</p>
  </div>
  <label class="switch-toggle">
    <input type="checkbox" class="peer sr-only" />
    <div class="switch-bg"></div>
  </label>
</div>
```

#### Compose Equivalent
```kotlin
@Composable
fun SettingItem(
    label: String,
    description: String? = null,
    trailing: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF101622).copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
            trailing()
        }
    }
}

@Composable
fun SettingToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color(0xFFFF3B30),
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFF475569)
        )
    )
}
```

### 4. Tabs Screen (tabs.html)

#### Tab Bar HTML
```html
<footer class="bg-gradient-to-t from-black/80 to-transparent backdrop-blur-sm">
  <div class="flex items-center justify-start p-2">
    <button class="p-2 rounded-full hover:bg-white/10">
      <span class="material-symbols-outlined">home</span>
    </button>
    <div class="flex items-center gap-2 overflow-x-auto px-2">
      <div class="tab-item relative w-10 h-10">
        <img class="w-full h-full rounded-full border-2 border-accent" />
        <button class="absolute -top-1 -right-1 bg-accent rounded-full w-5 h-5">
          <span>close</span>
        </button>
      </div>
    </div>
  </div>
</footer>
```

#### Compose Equivalent
```kotlin
@Composable
fun TabBar(
    tabs: List<Tab>,
    activeTabId: String,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onHomeClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
                .blur(8.dp) // backdrop-blur-sm
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RectangleShape
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home button
                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color.White
                    )
                }
                
                // Tab list
                LazyRow(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        TabThumbnail(
                            tab = tab,
                            isActive = tab.id == activeTabId,
                            onClick = { onTabClick(tab.id) },
                            onClose = { onTabClose(tab.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabThumbnail(
    tab: Tab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = tab.thumbnailBitmap,
            contentDescription = tab.title,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (isActive) Color(0xFFFF0000) else Color.Transparent,
                    shape = CircleShape
                ),
            contentScale = ContentScale.Crop
        )
        
        if (isActive) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(Color(0xFFFF0000), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
```

## Common Modifiers Library

Create a `CommonModifiers.kt` file with reusable modifiers:

```kotlin
object CommonModifiers {
    fun Modifier.gradientBackground() = this.background(
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D1117),
                Color(0xFF0D172E)
            )
        )
    )
    
    fun Modifier.cardGlow(color: Color) = this.shadow(
        elevation = 20.dp,
        shape = RoundedCornerShape(24.dp),
        ambientColor = color.copy(alpha = 0.5f),
        spotColor = color.copy(alpha = 0.5f)
    )
    
    fun Modifier.darkCard() = this
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xFF101622).copy(alpha = 0.5f))
}
```

## Typography Mapping

```kotlin
// Tailwind → Material 3 Typography
text-4xl font-bold → MaterialTheme.typography.displayLarge
text-3xl font-bold → MaterialTheme.typography.displayMedium
text-2xl font-bold → MaterialTheme.typography.headlineLarge
text-xl font-bold → MaterialTheme.typography.headlineMedium
text-lg font-semibold → MaterialTheme.typography.titleLarge
text-base font-medium → MaterialTheme.typography.bodyLarge
text-sm → MaterialTheme.typography.bodySmall
text-[11px] → MaterialTheme.typography.labelSmall
```

## Implementation Priority

1. **Task 1.5**: Extract all colors, gradients, and common styles
2. **Task 6.2**: Implement onboarding screens (welcome, features, permissions)
3. **Task 7.3-7.4**: Implement home screen with website cards
4. **Task 11.4**: Implement tabs screen with bottom tab bar
5. **Task 13.2**: Implement settings screen with grouped items

## Notes

- Use `AsyncImage` from Coil for all images
- Apply `contentDescription` for accessibility
- Use `remember` for expensive calculations
- Test on different screen sizes (phone, tablet)
- Verify color contrast ratios for accessibility
