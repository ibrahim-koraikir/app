package com.entertainmentbrowser.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.presentation.common.components.AnimatedWebsiteCard
import com.entertainmentbrowser.presentation.theme.DarkBackground
import com.entertainmentbrowser.presentation.theme.RedPrimary
import com.entertainmentbrowser.presentation.theme.TextPrimary
import com.entertainmentbrowser.presentation.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToWebView: (String) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Optimize filtering with derivedStateOf to prevent recalculation on every recomposition
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
    
    // Check if all categories are empty (for empty state)
    val hasNoWebsites = remember(uiState.websites, uiState.isLoading) {
        !uiState.isLoading && uiState.websites.isEmpty()
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(HomeEvent.ClearError)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "CineBrowse",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open settings",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToFavorites,
                containerColor = RedPrimary,
                contentColor = Color.White,
                modifier = Modifier.size(56.dp) // Standard FAB size, exceeds 48dp minimum
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "View all favorite websites"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Show loading or content
        when {
            uiState.isLoading && uiState.websites.isEmpty() -> {
                // Display skeleton loaders
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 112.dp)
                ) {
                    items(3) {
                        com.entertainmentbrowser.presentation.common.components.CategorySectionSkeleton()
                    }
                }
            }
            
            hasNoWebsites -> {
                // Display empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty()) "No Results Found" else "No Websites Available",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.searchQuery.isNotEmpty()) 
                            "Try adjusting your search query" 
                        else 
                            "Websites will appear here once loaded",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            else -> {
                // Display website content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 112.dp)
                ) {
                    // SPONSORED SECTION (Adsterra)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .clickable {
                                    // Open in Monetized Tab using magic prefix
                                    val adsterraUrl = com.entertainmentbrowser.util.Constants.ADSTERRA_DIRECT_LINK
                                    onNavigateToWebView("monetized:$adsterraUrl")
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sponsored Partner (Ad-Free)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Favorites Section
                    item {
                        if (favoriteWebsites.isNotEmpty()) {
                            CategorySection(
                                title = "⭐ Favorites",
                                websites = favoriteWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { websiteId ->
                                    viewModel.onEvent(HomeEvent.ToggleFavorite(websiteId))
                                },
                                onSeeMoreClick = {
                                    onNavigateToFavorites()
                                }
                            )
                        } else {
                            // Empty favorites section with title
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                // Section title
                                Text(
                                    text = "⭐ Favorites",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                                
                                // Empty hint card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Tap ❤️ on any website to add it to your favorites",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Movies Section
                    item {
                        CategorySection(
                            title = "Movies",
                            websites = streamingWebsites,
                            onWebsiteClick = { website ->
                                viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                onNavigateToWebView(website.url)
                            },
                            onFavoriteClick = { websiteId ->
                                viewModel.onEvent(HomeEvent.ToggleFavorite(websiteId))
                            },
                            onSeeMoreClick = {
                                // Navigate to category view
                            }
                        )
                    }

                    // TV Shows Section
                    item {
                        CategorySection(
                            title = "TV Shows",
                            websites = tvShowsWebsites,
                            onWebsiteClick = { website ->
                                viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                onNavigateToWebView(website.url)
                            },
                            onFavoriteClick = { websiteId ->
                                viewModel.onEvent(HomeEvent.ToggleFavorite(websiteId))
                            },
                            onSeeMoreClick = {
                                // Navigate to category view
                            }
                        )
                    }

                    // Books Section
                    item {
                        CategorySection(
                            title = "Books",
                            websites = booksWebsites,
                            onWebsiteClick = { website ->
                                viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                onNavigateToWebView(website.url)
                            },
                            onFavoriteClick = { websiteId ->
                                viewModel.onEvent(HomeEvent.ToggleFavorite(websiteId))
                            },
                            onSeeMoreClick = {
                                // Navigate to category view
                            }
                        )
                    }

                    // Social Media Section
                    item {
                        if (socialMediaWebsites.isNotEmpty()) {
                            CategorySection(
                                title = "Social Media",
                                websites = socialMediaWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { websiteId ->
                                    viewModel.onEvent(HomeEvent.ToggleFavorite(websiteId))
                                },
                                onSeeMoreClick = {}
                            )
                        }
                    }

                    // Games Section
                    item {
                        if (gamesWebsites.isNotEmpty()) {
                            CategorySection(
                                title = "Games",
                                websites = gamesWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { websiteId ->
                                    viewModel.onEvent(HomeEvent.ToggleFavorite(websiteId))
                                },
                                onSeeMoreClick = {}
                            )
                        }
                    }

                    // Video Call Section
                    item {
                        if (videoCallWebsites.isNotEmpty()) {
                            CategorySection(
                                title = "Video Call",
                                websites = videoCallWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { websiteId ->
                                    viewModel.onEvent(HomeEvent.ToggleFavorite(websiteId))
                                },
                                onSeeMoreClick = {}
                            )
                        }
                    }

                    // Arabic Section
                    item {
                        if (arabicWebsites.isNotEmpty()) {
                            CategorySection(
                                title = "عربي",
                                websites = arabicWebsites,
                                onWebsiteClick = { website ->
                                    viewModel.onEvent(HomeEvent.WebsiteClicked(website.url))
                                    onNavigateToWebView(website.url)
                                },
                                onFavoriteClick = { websiteId ->
                                    viewModel.onEvent(HomeEvent.ToggleFavorite(websiteId))
                                },
                                onSeeMoreClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    title: String,
    websites: List<com.entertainmentbrowser.domain.model.Website>,
    onWebsiteClick: (com.entertainmentbrowser.domain.model.Website) -> Unit,
    onFavoriteClick: (Int) -> Unit,
    onSeeMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    // Use remember to avoid recalculating on every recomposition
    val displayedWebsites = remember(websites, isExpanded) {
        if (isExpanded) websites else websites.take(4)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.semantics { heading() }
            )
        }

        // Website Grid (2 columns, vertical)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isExpanded) 10000.dp else 480.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            userScrollEnabled = false
        ) {
            itemsIndexed(
                items = displayedWebsites,
                key = { _, website -> website.id }
            ) { _, website ->
                AnimatedWebsiteCard(
                    website = website,
                    onCardClick = { onWebsiteClick(website) },
                    onFavoriteClick = { onFavoriteClick(website.id) }
                )
            }
        }

        // See More / See Less Link
        if (websites.size > 4) {
            Text(
                text = if (isExpanded) "See Less" else "See More",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = RedPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = { isExpanded = !isExpanded },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .heightIn(min = 48.dp), // Ensure minimum touch target
                textAlign = TextAlign.Center
            )
        }
    }
}
