package com.entertainmentbrowser.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.entertainmentbrowser.R
import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.domain.model.Website
import com.entertainmentbrowser.presentation.common.components.AnimatedWebsiteCard
import com.entertainmentbrowser.presentation.common.components.TutorialTip
import com.entertainmentbrowser.presentation.common.components.TutorialTips
import com.entertainmentbrowser.presentation.theme.DarkBackground
import com.entertainmentbrowser.presentation.theme.DarkSurface
import com.entertainmentbrowser.presentation.theme.RedPrimary
import com.entertainmentbrowser.presentation.theme.TextPrimary
import com.entertainmentbrowser.presentation.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToWebView: (String) -> Unit,
    onNavigateToBookmarks: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToActiveTab: () -> Unit = {}, // Navigate to active tab without creating new
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Tutorial tips state
    val showEdgeSwipeTip by viewModel.showEdgeSwipeTip.collectAsState()
    val showLongPressTip by viewModel.showLongPressTip.collectAsState()
    var edgeSwipeTipVisible by remember { mutableStateOf(false) }
    var longPressTipVisible by remember { mutableStateOf(false) }
    
    // Edge swipe detection for returning to active tab
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 50.dp.toPx() } // Lower threshold for easier swipe
    
    // State for remove favorite confirmation dialog
    var websiteToRemoveFromFavorites by remember { mutableStateOf<Website?>(null) }

    // Optimize filtering with derivedStateOf
    val favoriteWebsites by remember(uiState.websites) {
        derivedStateOf { uiState.websites.filter { it.isFavorite } }
    }
    
    val streamingWebsites by remember(uiState.websites) {
        derivedStateOf { uiState.websites.filter { it.category == Category.STREAMING } }
    }
    
    val tvShowsWebsites by remember(uiState.websites) {
        derivedStateOf { uiState.websites.filter { it.category == Category.TV_SHOWS } }
    }
    
    val booksWebsites by remember(uiState.websites) {
        derivedStateOf { uiState.websites.filter { it.category == Category.BOOKS } }
    }
    
    val socialMediaWebsites by remember(uiState.websites) {
        derivedStateOf { uiState.websites.filter { it.category == Category.SOCIAL_MEDIA } }
    }
    
    val gamesWebsites by remember(uiState.websites) {
        derivedStateOf { uiState.websites.filter { it.category == Category.GAMES } }
    }
    
    val videoCallWebsites by remember(uiState.websites) {
        derivedStateOf { uiState.websites.filter { it.category == Category.VIDEO_CALL } }
    }
    
    val arabicWebsites by remember(uiState.websites) {
        derivedStateOf { uiState.websites.filter { it.category == Category.ARABIC } }
    }
    
    val trendingWebsites by remember(uiState.websites) {
        derivedStateOf { uiState.websites.filter { it.category == Category.TRENDING } }
    }
    
    val hasNoWebsites = remember(uiState.websites, uiState.isLoading) {
        !uiState.isLoading && uiState.websites.isEmpty()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(HomeEvent.ClearError)
        }
    }
    
    LaunchedEffect(uiState.navigationUrl) {
        uiState.navigationUrl?.let { url ->
            onNavigateToWebView(url)
            viewModel.onEvent(HomeEvent.NavigationConsumed)
        }
    }
    
    // Show snackbar for "Opened in new tab" feedback
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(HomeEvent.SnackbarConsumed)
        }
    }
    
    // Remove from favorites confirmation dialog
    websiteToRemoveFromFavorites?.let { website ->
        AlertDialog(
            onDismissRequest = { websiteToRemoveFromFavorites = null },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = stringResource(R.string.remove_favorite_dialog_title),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.remove_favorite_dialog_message, website.name),
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEvent(HomeEvent.ToggleFavorite(website.id))
                        websiteToRemoveFromFavorites = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text(stringResource(R.string.remove_favorite_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { websiteToRemoveFromFavorites = null }) {
                    Text(stringResource(R.string.remove_favorite_cancel), color = TextSecondary)
                }
            }
        )
    }

    // Track cumulative drag for edge swipe
    var cumulativeDrag by remember { mutableFloatStateOf(0f) }
    
    // Show tutorial tips when conditions are met
    LaunchedEffect(showEdgeSwipeTip, uiState.activeTabUrl) {
        // Show edge swipe tip when there's an active tab and tip should be shown
        if (showEdgeSwipeTip && uiState.activeTabUrl != null && !edgeSwipeTipVisible) {
            edgeSwipeTipVisible = true
            viewModel.onEdgeSwipeTipShown()
        }
    }
    
    LaunchedEffect(showLongPressTip, uiState.websites) {
        // Show long press tip when websites are loaded
        if (showLongPressTip && uiState.websites.isNotEmpty() && !longPressTipVisible && !edgeSwipeTipVisible) {
            longPressTipVisible = true
            viewModel.onLongPressTipShown()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "XHUB",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToBookmarks,
                containerColor = RedPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.websites.isEmpty() -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(3) {
                        com.entertainmentbrowser.presentation.common.components.CategorySectionSkeleton()
                    }
                }
            }
            
            hasNoWebsites -> {
                EmptyState(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    searchQuery = uiState.searchQuery
                )
            }
            
            else -> {
                var searchQuery by remember { mutableStateOf("") }
                val keyboardController = LocalSoftwareKeyboardController.current
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Search Bar
                    item {
                        ModernSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            searchEngineName = uiState.searchEngine.displayName,
                            onSearch = { query ->
                                keyboardController?.hide()
                                if (query.isNotBlank()) {
                                    searchQuery = ""
                                    viewModel.onEvent(HomeEvent.SearchBarSubmit(query))
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // Favorites Section (Horizontal scroll)
                    item {
                        if (favoriteWebsites.isNotEmpty()) {
                            HorizontalCategorySection(
                                title = "Favorites",
                                icon = Icons.Default.FavoriteBorder,
                                iconColor = RedPrimary,
                                websites = favoriteWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { website ->
                                    if (website.isFavorite) {
                                        websiteToRemoveFromFavorites = website
                                    } else {
                                        viewModel.onEvent(HomeEvent.ToggleFavorite(website.id))
                                    }
                                },
                                onLongPress = { website -> 
                                    viewModel.onLongPressCompleted()
                                    viewModel.onEvent(HomeEvent.OpenInNewTab(website.url, website.name))
                                }
                            )
                        } else {
                            EmptyFavoritesCard()
                        }
                    }

                    // Trending Section
                    item {
                        if (trendingWebsites.isNotEmpty()) {
                            HorizontalCategorySection(
                                title = "Trending",
                                icon = Icons.Outlined.TrendingUp,
                                iconColor = Color(0xFFFF6B6B),
                                websites = trendingWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { website ->
                                    if (website.isFavorite) {
                                        websiteToRemoveFromFavorites = website
                                    } else {
                                        viewModel.onEvent(HomeEvent.ToggleFavorite(website.id))
                                    }
                                },
                                onLongPress = { website -> 
                                    viewModel.onLongPressCompleted()
                                    viewModel.onEvent(HomeEvent.OpenInNewTab(website.url, website.name))
                                }
                            )
                        }
                    }

                    // Movies Section (Vertical Grid)
                    item {
                        if (streamingWebsites.isNotEmpty()) {
                            VerticalGridCategorySection(
                                title = "Movies",
                                icon = Icons.Outlined.Movie,
                                iconColor = Color(0xFFE50914),
                                websites = streamingWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { website ->
                                    if (website.isFavorite) {
                                        websiteToRemoveFromFavorites = website
                                    } else {
                                        viewModel.onEvent(HomeEvent.ToggleFavorite(website.id))
                                    }
                                },
                                onLongPress = { website -> 
                                    viewModel.onLongPressCompleted()
                                    viewModel.onEvent(HomeEvent.OpenInNewTab(website.url, website.name))
                                }
                            )
                        }
                    }

                    // TV Shows Section (Vertical Grid)
                    item {
                        if (tvShowsWebsites.isNotEmpty()) {
                            VerticalGridCategorySection(
                                title = "TV Shows",
                                icon = Icons.Outlined.Tv,
                                iconColor = Color(0xFF1DA1F2),
                                websites = tvShowsWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { website ->
                                    if (website.isFavorite) {
                                        websiteToRemoveFromFavorites = website
                                    } else {
                                        viewModel.onEvent(HomeEvent.ToggleFavorite(website.id))
                                    }
                                },
                                onLongPress = { website -> 
                                    viewModel.onLongPressCompleted()
                                    viewModel.onEvent(HomeEvent.OpenInNewTab(website.url, website.name))
                                }
                            )
                        }
                    }

                    // Books Section (Vertical Grid)
                    item {
                        if (booksWebsites.isNotEmpty()) {
                            VerticalGridCategorySection(
                                title = "Books",
                                icon = Icons.Outlined.MenuBook,
                                iconColor = Color(0xFF8B4513),
                                websites = booksWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { website ->
                                    if (website.isFavorite) {
                                        websiteToRemoveFromFavorites = website
                                    } else {
                                        viewModel.onEvent(HomeEvent.ToggleFavorite(website.id))
                                    }
                                },
                                onLongPress = { website -> 
                                    viewModel.onLongPressCompleted()
                                    viewModel.onEvent(HomeEvent.OpenInNewTab(website.url, website.name))
                                }
                            )
                        }
                    }

                    // Social Media Section (Vertical Grid)
                    item {
                        if (socialMediaWebsites.isNotEmpty()) {
                            VerticalGridCategorySection(
                                title = "Social Media",
                                icon = Icons.Outlined.People,
                                iconColor = Color(0xFFE1306C),
                                websites = socialMediaWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { website ->
                                    if (website.isFavorite) {
                                        websiteToRemoveFromFavorites = website
                                    } else {
                                        viewModel.onEvent(HomeEvent.ToggleFavorite(website.id))
                                    }
                                },
                                onLongPress = { website -> 
                                    viewModel.onLongPressCompleted()
                                    viewModel.onEvent(HomeEvent.OpenInNewTab(website.url, website.name))
                                }
                            )
                        }
                    }

                    // Games Section (Vertical Grid)
                    item {
                        if (gamesWebsites.isNotEmpty()) {
                            VerticalGridCategorySection(
                                title = "Games",
                                icon = Icons.Outlined.SportsEsports,
                                iconColor = Color(0xFF9146FF),
                                websites = gamesWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { website ->
                                    if (website.isFavorite) {
                                        websiteToRemoveFromFavorites = website
                                    } else {
                                        viewModel.onEvent(HomeEvent.ToggleFavorite(website.id))
                                    }
                                },
                                onLongPress = { website -> 
                                    viewModel.onLongPressCompleted()
                                    viewModel.onEvent(HomeEvent.OpenInNewTab(website.url, website.name))
                                }
                            )
                        }
                    }

                    // Video Call Section (Vertical Grid)
                    item {
                        if (videoCallWebsites.isNotEmpty()) {
                            VerticalGridCategorySection(
                                title = "Video Call",
                                icon = Icons.Outlined.VideoCall,
                                iconColor = Color(0xFF00AFF0),
                                websites = videoCallWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { website ->
                                    if (website.isFavorite) {
                                        websiteToRemoveFromFavorites = website
                                    } else {
                                        viewModel.onEvent(HomeEvent.ToggleFavorite(website.id))
                                    }
                                },
                                onLongPress = { website -> 
                                    viewModel.onLongPressCompleted()
                                    viewModel.onEvent(HomeEvent.OpenInNewTab(website.url, website.name))
                                }
                            )
                        }
                    }

                    // Arabic Section (Vertical Grid)
                    item {
                        if (arabicWebsites.isNotEmpty()) {
                            VerticalGridCategorySection(
                                title = "عربي",
                                icon = Icons.Outlined.Language,
                                iconColor = Color(0xFF1DB954),
                                websites = arabicWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { website ->
                                    if (website.isFavorite) {
                                        websiteToRemoveFromFavorites = website
                                    } else {
                                        viewModel.onEvent(HomeEvent.ToggleFavorite(website.id))
                                    }
                                },
                                onLongPress = { website -> 
                                    viewModel.onLongPressCompleted()
                                    viewModel.onEvent(HomeEvent.OpenInNewTab(website.url, website.name))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
        
        // Left edge swipe zone - invisible overlay for edge gesture
        if (uiState.activeTabUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp) // Wider touch zone
                    .align(Alignment.CenterStart)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { 
                                cumulativeDrag = 0f
                            },
                            onDragEnd = {
                                if (cumulativeDrag > swipeThreshold) {
                                    viewModel.onEdgeSwipeCompleted() // Mark tutorial as completed
                                    onNavigateToActiveTab()
                                }
                                cumulativeDrag = 0f
                            },
                            onDragCancel = {
                                cumulativeDrag = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount > 0) { // Only count rightward drag
                                    cumulativeDrag += dragAmount
                                }
                            }
                        )
                    }
            )
            
            // Right edge swipe zone
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp) // Wider touch zone
                    .align(Alignment.CenterEnd)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { 
                                cumulativeDrag = 0f
                            },
                            onDragEnd = {
                                if (cumulativeDrag > swipeThreshold) {
                                    viewModel.onEdgeSwipeCompleted() // Mark tutorial as completed
                                    onNavigateToActiveTab()
                                }
                                cumulativeDrag = 0f
                            },
                            onDragCancel = {
                                cumulativeDrag = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                if (dragAmount < 0) { // Only count leftward drag
                                    cumulativeDrag += -dragAmount
                                }
                            }
                        )
                    }
            )
        }
        
        // Tutorial Tips - displayed at top of screen
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp) // Below the app bar
        ) {
            TutorialTip(
                message = TutorialTips.EDGE_SWIPE,
                visible = edgeSwipeTipVisible,
                onDismiss = {
                    edgeSwipeTipVisible = false
                    viewModel.dismissEdgeSwipeTip()
                }
            )
            
            TutorialTip(
                message = TutorialTips.LONG_PRESS,
                visible = longPressTipVisible,
                onDismiss = {
                    longPressTipVisible = false
                    viewModel.dismissLongPressTip()
                }
            )
        }
    } // End of Box wrapper for edge swipe
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    searchQuery: String
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (searchQuery.isNotEmpty()) "No Results Found" else "No Websites Available",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (searchQuery.isNotEmpty()) 
                "Try adjusting your search query" 
            else 
                "Websites will appear here once loaded",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyFavoritesCard() {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RedPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Favorites",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        // Compact empty hint
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Long press any site to add to favorites ❤️",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@Composable
private fun HorizontalCategorySection(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    websites: List<Website>,
    onWebsiteClick: (Website) -> Unit,
    onFavoriteClick: (Website) -> Unit,
    onLongPress: (Website) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.semantics { heading() }
                )
            }
            
            if (websites.size > 3) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { /* See all */ }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelMedium,
                        color = iconColor,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Horizontal scrolling cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = websites,
                key = { it.id }
            ) { website ->
                AnimatedWebsiteCard(
                    website = website,
                    onCardClick = { onWebsiteClick(website) },
                    onFavoriteClick = { onFavoriteClick(website) },
                    onLongPress = { onLongPress(website) },
                    modifier = Modifier.width(110.dp)
                )
            }
        }
    }
}

@Composable
private fun VerticalGridCategorySection(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    websites: List<Website>,
    onWebsiteClick: (Website) -> Unit,
    onFavoriteClick: (Website) -> Unit,
    onLongPress: (Website) -> Unit = {},
    modifier: Modifier = Modifier,
    initialItemCount: Int = 4
) {
    var isExpanded by remember { mutableStateOf(false) }
    val displayedWebsites = if (isExpanded) websites else websites.take(initialItemCount)
    val hasMore = websites.size > initialItemCount
    
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.semantics { heading() }
            )
        }

        // Vertical grid - 2 columns
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayedWebsites.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { website ->
                        AnimatedWebsiteCard(
                            website = website,
                            onCardClick = { onWebsiteClick(website) },
                            onFavoriteClick = { onFavoriteClick(website) },
                            onLongPress = { onLongPress(website) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill empty space if odd number of items
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        // See more/less button at bottom
        if (hasMore) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { isExpanded = !isExpanded },
                    color = iconColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isExpanded) "See less" else "See more",
                            style = MaterialTheme.typography.labelLarge,
                            color = iconColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) 
                                Icons.Default.KeyboardArrowUp 
                            else 
                                Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchEngineName: String = "Google"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        tonalElevation = 2.dp
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Search $searchEngineName or enter URL",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    FilledIconButton(
                        onClick = { onSearch(query) },
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = RedPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = RedPrimary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) })
        )
    }
}

