package com.entertainmentbrowser.presentation.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Settings screen composable matching settings.html design.
 * 
 * Requirements: 12.2, 12.3, 12.4, 12.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDownloadLocation: () -> Unit = {},
    onNavigateToMaxDownloads: () -> Unit = {},
    onNavigateToBookmarks: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Gradient background colors matching settings.html
    val gradientColors = listOf(
        Color(0xFF051C4A), // --primary-gradient-end
        Color(0xFF101622)  // --background-dark
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(colors = gradientColors)
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Settings",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Download Section
                SettingsSection(title = "DOWNLOAD") {
                    SettingsGroup {
                        // Download on Wi-Fi only toggle
                        SettingItemWithSwitch(
                            label = "Download on Wi-Fi only",
                            checked = uiState.settings.downloadOnWifiOnly,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.ToggleDownloadOnWifiOnly(it))
                            }
                        )
                        
                        SettingsDivider()
                        
                        // Download location
                        SettingItemWithNavigation(
                            label = "Download location",
                            description = "Internal storage",
                            onClick = onNavigateToDownloadLocation
                        )
                        
                        SettingsDivider()
                        
                        // Maximum concurrent downloads
                        SettingItemWithNavigation(
                            label = "Maximum concurrent downloads",
                            description = uiState.settings.maxConcurrentDownloads.toString(),
                            onClick = onNavigateToMaxDownloads
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Haptic Feedback Section
                SettingsSection(title = "PREFERENCES") {
                    SettingsGroup {
                        SettingItemWithSwitch(
                            label = "Haptic feedback",
                            checked = uiState.settings.hapticFeedbackEnabled,
                            onCheckedChange = {
                                viewModel.onEvent(SettingsEvent.ToggleHapticFeedback(it))
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Bookmarks Section
                SettingsSection(title = "BOOKMARKS") {
                    SettingsGroup {
                        SettingItemWithNavigation(
                            label = "View Bookmarks",
                            description = "Manage your saved pages",
                            onClick = {
                                android.util.Log.d("SettingsScreen", "View Bookmarks clicked!")
                                onNavigateToBookmarks()
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Cache Section
                SettingsSection(title = "CACHE") {
                    SettingsGroup {
                        SettingItemWithNavigation(
                            label = "Clear Cache",
                            description = "Clear temporary files",
                            labelColor = Color(0xFFFF3B30), // --accent-color
                            onClick = {
                                viewModel.onEvent(SettingsEvent.ShowClearCacheDialog)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // About Section
                SettingsSection(title = "ABOUT") {
                    SettingsGroup {
                        // App version
                        SettingItemWithValue(
                            label = "App version",
                            value = getAppVersion()
                        )
                        
                        SettingsDivider()
                        
                        // Privacy Policy
                        SettingItemWithNavigation(
                            label = "Privacy Policy",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/privacy"))
                                context.startActivity(intent)
                            }
                        )
                        
                        SettingsDivider()
                        
                        // Terms of Service
                        SettingItemWithNavigation(
                            label = "Terms of Service",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/terms"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Clear Cache Confirmation Dialog
        if (uiState.showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(SettingsEvent.DismissDialog) },
                title = { Text("Clear Cache") },
                text = { Text("Are you sure you want to clear the cache? This will remove all temporary files.") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(SettingsEvent.ConfirmClearCache) }
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(SettingsEvent.DismissDialog) }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Clear History Confirmation Dialog
        if (uiState.showClearHistoryDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(SettingsEvent.DismissDialog) },
                title = { Text("Clear Download History") },
                text = { Text("Are you sure you want to clear the download history? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(SettingsEvent.ConfirmClearHistory) }
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(SettingsEvent.DismissDialog) }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Success Snackbar
        if (uiState.cacheCleared) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                viewModel.onEvent(SettingsEvent.DismissSuccess)
            }
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Cache cleared successfully")
            }
        }
        
        if (uiState.historyCleared) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                viewModel.onEvent(SettingsEvent.DismissSuccess)
            }
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Download history cleared successfully")
            }
        }
        
        // Error Snackbar
        uiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.onEvent(SettingsEvent.ClearError) }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFD1D1D))
            }
        }
    }
}

/**
 * Section title component matching settings.html design.
 */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF3B30), // --accent-color
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

/**
 * Settings group container with dark card background.
 */
@Composable
fun SettingsGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x80101622)) // rgba(16, 22, 34, 0.5)
    ) {
        content()
    }
}

/**
 * Setting item with toggle switch.
 */
@Composable
fun SettingItemWithSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.White
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF3B30), // Red accent when checked
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF475569) // Slate-600
            )
        )
    }
}

/**
 * Setting item with navigation chevron.
 */
@Composable
fun SettingItemWithNavigation(
    label: String,
    description: String? = null,
    labelColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 16.sp,
                color = labelColor
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF94A3B8) // --text-muted
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8)
        )
    }
}

/**
 * Setting item with static value display.
 */
@Composable
fun SettingItemWithValue(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.White
        )
        
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color(0xFF94A3B8) // --text-muted
        )
    }
}

/**
 * Divider between setting items.
 */
@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.1f))
    )
}

/**
 * Get app version from BuildConfig.
 */
private fun getAppVersion(): String {
    return try {
        com.entertainmentbrowser.BuildConfig.VERSION_NAME
    } catch (e: Exception) {
        "1.0.0"
    }
}
