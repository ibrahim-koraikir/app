package com.entertainmentbrowser.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tailwind rounded-2xl equivalent (16dp corner radius)
 */
fun Modifier.rounded2xl() = this.clip(RoundedCornerShape(16.dp))

/**
 * Tailwind rounded-xl equivalent (12dp corner radius)
 */
fun Modifier.roundedXl() = this.clip(RoundedCornerShape(12.dp))

/**
 * Tailwind rounded-lg equivalent (8dp corner radius)
 */
fun Modifier.roundedLg() = this.clip(RoundedCornerShape(8.dp))

/**
 * Tailwind rounded-full equivalent
 */
fun Modifier.roundedFull() = this.clip(RoundedCornerShape(50))

/**
 * Deep dark shadow effect from home.html
 * shadow-deep-dark: 0 10px 15px -3px rgba(0, 0, 0, 0.3), 0 4px 6px -2px rgba(0, 0, 0, 0.15)
 */
fun Modifier.shadowDeepDark() = this.shadow(
    elevation = 10.dp,
    shape = RoundedCornerShape(16.dp),
    ambientColor = Color.Black.copy(alpha = 0.3f),
    spotColor = Color.Black.copy(alpha = 0.15f)
)

/**
 * Gradient background from welcome.html
 * linear-gradient(180deg, rgba(13, 17, 23, 0.4) 0%, #0D1117 85%)
 */
fun Modifier.gradientDeepBlue() = this.background(
    brush = Brush.verticalGradient(
        colors = listOf(
            Color(0x660D1117), // rgba(13, 17, 23, 0.4)
            Color(0xFF0D1117)
        ),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )
)

/**
 * Settings screen gradient background
 * linear-gradient(to bottom, #051c4a, #101622)
 */
fun Modifier.gradientSettingsBackground() = this.background(
    brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF051C4A),
            Color(0xFF101622)
        )
    )
)

/**
 * Card gradient overlay from home.html
 * from-black/80 via-black/50 to-transparent
 */
fun Modifier.cardGradientOverlay() = this.background(
    brush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.5f),
            Color.Black.copy(alpha = 0.8f)
        )
    )
)

/**
 * Backdrop blur effect (approximation using semi-transparent background)
 * backdrop-blur-sm from welcome.html
 */
fun Modifier.backdropBlur() = this.background(
    color = Color(0xCC0D1117) // 80% opacity
)

/**
 * Dark card background from settings.html
 * rgba(16, 22, 34, 0.5)
 */
fun Modifier.darkCard() = this.background(
    color = Color(0x80101622),
    shape = RoundedCornerShape(12.dp)
)



/**
 * Hover scale animation (for cards)
 * transform hover:-translate-y-1
 */
fun Modifier.hoverScale() = this // Note: Hover effects are typically handled in onClick/press states in Compose

/**
 * Divider line from settings.html
 * bg-white/10
 */
fun Modifier.settingsDivider() = this
    .fillMaxWidth()
    .padding(horizontal = 16.dp)
    .background(Color.White.copy(alpha = 0.1f))

/**
 * Category badge styling
 */
fun Modifier.categoryBadge(borderColor: Color, backgroundColor: Color) = this
    .background(
        color = backgroundColor,
        shape = RoundedCornerShape(50)
    )
    .padding(horizontal = 12.dp, vertical = 4.dp)
