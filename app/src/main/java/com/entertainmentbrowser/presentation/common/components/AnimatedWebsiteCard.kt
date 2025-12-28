package com.entertainmentbrowser.presentation.common.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.Movie
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
import android.util.Base64
import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.domain.model.Website
import com.entertainmentbrowser.presentation.settings.SettingsViewModel
import com.entertainmentbrowser.presentation.theme.DisneyBlue
import com.entertainmentbrowser.presentation.theme.GoodreadsBrown
import com.entertainmentbrowser.presentation.theme.ImdbYellow
import com.entertainmentbrowser.presentation.theme.NetflixRed
import com.entertainmentbrowser.presentation.theme.TextPrimary
import com.entertainmentbrowser.util.AccessibilityHelper
import com.entertainmentbrowser.util.rememberHapticFeedback

/**
 * Website card component with tap and long-press support.
 * 
 * @param website The website data to display
 * @param onCardClick Callback when card is tapped (opens in current tab)
 * @param onFavoriteClick Callback when favorite button is clicked
 * @param onLongPress Callback when card is long-pressed (opens in new tab)
 * @param modifier Optional modifier for the card
 * @param settingsViewModel ViewModel for settings (injected via Hilt)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnimatedWebsiteCard(
    website: Website,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val hapticFeedback = rememberHapticFeedback()
    val borderColor = getCategoryBorderColor(website.category)
    val bgColor = parseColor(website.backgroundColor)
    
    // State for showing context menu
    var showContextMenu by remember { mutableStateOf(false) }
    
    val cardDescription = AccessibilityHelper.websiteCardDescription(
        name = website.name,
        category = getCategoryDisplayName(website.category),
        isFavorite = website.isFavorite
    )
    
    Box(
        modifier = modifier
            .height(160.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = bgColor.copy(alpha = 0.6f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = {
                    if (settingsState.settings.hapticFeedbackEnabled) {
                        hapticFeedback.performLongPress()
                    }
                    showContextMenu = true
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .semantics {
                contentDescription = cardDescription
                role = Role.Button
            }
    ) {
        // Context menu dropdown
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Open in new tab") },
                onClick = {
                    showContextMenu = false
                    onLongPress?.invoke()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null
                    )
                }
            )
        }
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background image - handle base64 and URL images
            val logoUrl = website.logoUrl.trim()
            val isBase64 = logoUrl.startsWith("data:image")
            
            if (isBase64) {
                // Decode base64 image directly
                val bitmap = remember(logoUrl) {
                    try {
                        val base64Data = logoUrl.substringAfter("base64,")
                            .replace("\\s".toRegex(), "")
                            .trim()
                        
                        if (base64Data.isEmpty()) return@remember null
                        
                        val bytes = try {
                            Base64.decode(base64Data, Base64.DEFAULT)
                        } catch (e: IllegalArgumentException) {
                            Base64.decode(base64Data, Base64.URL_SAFE)
                        }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }
                
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "${website.name} logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = website.name.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Use Coil for URL images
                SubcomposeAsyncImage(
                    model = logoUrl,
                    contentDescription = "${website.name} logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = website.name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    success = {
                        SubcomposeAsyncImageContent()
                    }
                )
            }
            
            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )
            
            // Favorite button
            IconButton(
                onClick = {
                    if (settingsState.settings.hapticFeedbackEnabled) {
                        hapticFeedback.performClick()
                    }
                    onFavoriteClick()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = if (website.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (website.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (website.isFavorite) Color.Red else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = website.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
                if (website.description.isNotEmpty()) {
                    Text(
                        text = website.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

private fun getCategoryBorderColor(category: Category): Color {
    return when (category) {
        Category.STREAMING -> NetflixRed
        Category.TV_SHOWS -> DisneyBlue
        Category.BOOKS -> GoodreadsBrown
        Category.VIDEO_PLATFORMS -> ImdbYellow
        Category.SOCIAL_MEDIA -> Color(0xFFE1306C) // Instagram pink
        Category.GAMES -> Color(0xFF9146FF) // Twitch purple
        Category.VIDEO_CALL -> Color(0xFF00AFF0) // Skype blue
        Category.ARABIC -> Color(0xFF1DB954) // Green
        Category.TRENDING -> Color(0xFFFF6B6B) // Coral/Fire
    }
}

private fun getCategoryDisplayName(category: Category): String {
    return when (category) {
        Category.STREAMING -> "Streaming"
        Category.TV_SHOWS -> "TV Shows"
        Category.BOOKS -> "Books"
        Category.VIDEO_PLATFORMS -> "Video Platforms"
        Category.SOCIAL_MEDIA -> "Social Media"
        Category.GAMES -> "Games"
        Category.VIDEO_CALL -> "Video Call"
        Category.ARABIC -> "عربي"
        Category.TRENDING -> "Trending"
    }
}


private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFF1E1E1E) // Default dark gray
    }
}
@Composable
fun rememberBase64Image(base64String: String): androidx.compose.ui.graphics.ImageBitmap? {
    return remember(base64String) {
        try {
            // Remove data URI prefix if present
            val pureBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            
            val decodedBytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}


