package com.entertainmentbrowser.presentation.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.entertainmentbrowser.domain.model.Tab
import com.entertainmentbrowser.presentation.settings.SettingsViewModel
import com.entertainmentbrowser.util.rememberHapticFeedback
import java.io.File

/**
 * Tabs screen displaying all open tabs with thumbnails.
 * Matches the design from tabs.html with fullscreen WebView and bottom tab bar.
 */
@Composable
fun TabsScreen(
    onNavigateToWebView: (String) -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: TabsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val hapticFeedback = rememberHapticFeedback()
    val uiState by viewModel.uiState.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is TabsUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            is TabsUiState.Empty -> {
                EmptyTabsContent(
                    onNavigateToHome = onNavigateToHome,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            is TabsUiState.Success -> {
                TabsContent(
                    tabs = state.tabs,
                    onTabClick = { tab ->
                        viewModel.switchTab(tab.id)
                        onNavigateToWebView(tab.url)
                    },
                    onCloseTab = { tabId ->
                        if (settingsState.settings.hapticFeedbackEnabled) {
                            hapticFeedback.performContextClick()
                        }
                        viewModel.closeTab(tabId)
                    },
                    onNavigateToHome = onNavigateToHome
                )
            }
            
            is TabsUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun TabsContent(
    tabs: List<Tab>,
    onTabClick: (Tab) -> Unit,
    onCloseTab: (String) -> Unit,
    onNavigateToHome: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Active tab WebView preview (would show actual WebView in real implementation)
        val activeTab = tabs.firstOrNull { it.isActive }
        activeTab?.let { tab ->
            TabPreview(
                tab = tab,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Bottom tab bar with gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .blur(8.dp)
            )
            
            // Tab bar content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home button
                IconButton(
                    onClick = onNavigateToHome,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color.White
                    )
                }
                
                // Scrollable tab list
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        TabThumbnail(
                            tab = tab,
                            onClick = { onTabClick(tab) },
                            onClose = { onCloseTab(tab.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabPreview(
    tab: Tab,
    modifier: Modifier = Modifier
) {
    // Display tab thumbnail as preview
    if (tab.thumbnailPath != null && File(tab.thumbnailPath).exists()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(tab.thumbnailPath))
                .crossfade(true)
                .build(),
            contentDescription = tab.title,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Placeholder if no thumbnail
        Box(
            modifier = modifier.background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tab.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
private fun TabThumbnail(
    tab: Tab,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier.size(40.dp)
    ) {
        // Thumbnail with border for active tab
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(
                    if (tab.isActive) {
                        Modifier.border(2.dp, Color.Red, CircleShape)
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            if (tab.thumbnailPath != null && File(tab.thumbnailPath).exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(tab.thumbnailPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = tab.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                // Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title.take(1).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Close button on active tab
        if (tab.isActive) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .clip(CircleShape)
                    .background(Color.Red)
                    .clickable(
                        onClick = onClose,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close tab",
                    tint = Color.White,
                    modifier = Modifier.size(8.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyTabsContent(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "No open tabs",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Open a website to start browsing",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onNavigateToHome) {
            Text("Go to Home")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
