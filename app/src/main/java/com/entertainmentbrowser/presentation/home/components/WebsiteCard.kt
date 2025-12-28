package com.entertainmentbrowser.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.LocalImageLoader
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

@Composable
fun WebsiteCard(
    website: Website,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsState by settingsViewModel.uiState.collectAsState()
    val hapticFeedback = rememberHapticFeedback()
    val borderColor = getCategoryBorderColor(website.category)
    
    val cardDescription = AccessibilityHelper.websiteCardDescription(
        name = website.name,
        category = getCategoryDisplayName(website.category),
        isFavorite = website.isFavorite
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(224.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0A0E3F7E),
                spotColor = Color(0x0A2A3346)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClick = onCardClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .semantics {
                contentDescription = cardDescription
                role = Role.Button
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background image - covers entire card with proper fitting
            AsyncImage(
                model = website.logoUrl,
                contentDescription = "${website.name} logo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
            
            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f),
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
                    .padding(8.dp)
                    .size(48.dp) // Ensure 48dp minimum touch target
            ) {
                Icon(
                    imageVector = if (website.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (website.isFavorite) "Remove ${website.name} from favorites" else "Add ${website.name} to favorites",
                    tint = if (website.isFavorite) borderColor else Color.White.copy(alpha = 0.7f)
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
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = website.description,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFFB0B0B0),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Category badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = borderColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = borderColor
                    )
                ) {
                    Text(
                        text = getCategoryDisplayName(website.category),
                        style = MaterialTheme.typography.labelSmall,
                        color = borderColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        Category.SOCIAL_MEDIA -> Color(0xFFE1306C)
        Category.GAMES -> Color(0xFF9146FF)
        Category.VIDEO_CALL -> Color(0xFF00AFF0)
        Category.ARABIC -> Color(0xFF1DB954)
        Category.TRENDING -> Color(0xFFFF6B6B)
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
