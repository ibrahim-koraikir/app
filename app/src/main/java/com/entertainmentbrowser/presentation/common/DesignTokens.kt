package com.entertainmentbrowser.presentation.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design tokens extracted from HTML/Tailwind designs
 * Maps Tailwind CSS classes to Compose equivalents
 */
object DesignTokens {
    
    // Color Palette (from HTML designs)
    object Colors {
        // Primary
        val Primary = Color(0xFFFF0000) // #FF0000 - Bright Red Accent
        val PrimaryVariant = Color(0xFFE50914) // #E50914 - Netflix Red
        val Accent = Color(0xFFFF3B30) // #ff3b30 - Settings accent
        
        // Background
        val BackgroundDark = Color(0xFF0D1117) // #0D1117 - Deep Blue/Black
        val BackgroundDark2 = Color(0xFF0D172E) // #0D172E - Gradient end
        val BackgroundDark3 = Color(0xFF121212) // #121212 - Home background
        val BackgroundDark4 = Color(0xFF101622) // #101622 - Settings background
        
        // Card
        val CardDark = Color(0xFF1E1E1E) // #1E1E1E - Card background
        val CardDark2 = Color(0xFF161B22) // #161B22 - Features card
        val CardDarkTransparent = Color(0x80101622) // rgba(16, 22, 34, 0.5)
        
        // Category Badge Colors
        val NetflixRed = Color(0xFFE50914)
        val DisneyBlue = Color(0xFF113CCF)
        val ImdbYellow = Color(0xFFF5C518)
        val GoodreadsBrown = Color(0xFFA0522D)
        
        // Text
        val TextWhite = Color(0xFFFFFFFF)
        val TextWhite80 = Color(0xCCFFFFFF) // white/80
        val TextWhite70 = Color(0xB3FFFFFF) // white/70
        val TextMuted = Color(0xFF94A3B8) // #94a3b8 - slate-400
        val TextGray = Color(0xFFD1D5DB) // gray-300
        
        // Gradient Colors
        val GradientStart = Color(0x660D1117) // rgba(13, 17, 23, 0.4)
        val GradientEnd = Color(0xFF0D1117)
        val SettingsGradientStart = Color(0xFF051C4A) // #051c4a
        val SettingsGradientEnd = Color(0xFF101622) // #101622
    }
    
    // Border Radius (Tailwind equivalents)
    object Radius {
        val Default = 8.dp // rounded (0.5rem)
        val Lg = 12.dp // rounded-lg (0.75rem)
        val Xl = 16.dp // rounded-xl (1rem)
        val Xxl = 20.dp // rounded-2xl (1.25rem)
        val Full = 9999.dp // rounded-full
    }
    
    // Spacing (Tailwind scale)
    object Spacing {
        val Xs = 4.dp // space-1
        val Sm = 8.dp // space-2
        val Md = 12.dp // space-3
        val Lg = 16.dp // space-4
        val Xl = 24.dp // space-6
        val Xxl = 32.dp // space-8
    }
    
    // Elevation/Shadow
    object Elevation {
        val Card = 10.dp // shadow-deep-dark
        val Fab = 8.dp // FAB shadow
        val None = 0.dp
    }
    
    // Sizes
    object Sizes {
        // Card heights
        val CardHeight = 224.dp // h-56 (14rem = 224dp)
        
        // Icon sizes
        val IconSmall = 16.dp // h-4 w-4
        val IconMedium = 24.dp // h-6 w-6
        val IconLarge = 48.dp // h-12 w-12
        
        // Button heights
        val ButtonHeight = 48.dp // h-12
        val ButtonHeightSmall = 40.dp // h-10
        
        // Touch targets
        val MinTouchTarget = 48.dp // Accessibility minimum
    }
    
    // Typography sizes (from HTML)
    object TextSize {
        val DisplayLarge = 36 // text-4xl (2.25rem)
        val DisplayMedium = 30 // text-3xl (1.875rem)
        val HeadlineLarge = 24 // text-2xl (1.5rem)
        val HeadlineMedium = 20 // text-xl (1.25rem)
        val TitleLarge = 18 // text-lg (1.125rem)
        val TitleMedium = 16 // text-base (1rem)
        val BodyMedium = 14 // text-sm (0.875rem)
        val BodySmall = 12 // text-xs (0.75rem)
        val LabelSmall = 11 // text-[11px]
        val LabelTiny = 10 // text-[10px]
    }
    
    // Opacity values (from Tailwind)
    object Opacity {
        val Full = 1f
        val High = 0.8f // /80
        val Medium = 0.7f // /70
        val Low = 0.5f // /50
        val VeryLow = 0.2f // /20
        val Divider = 0.1f // /10
    }
}

/**
 * Tailwind to Compose mapping reference
 * 
 * SPACING:
 * - p-6 = padding(24.dp)
 * - px-6 = padding(horizontal = 24.dp)
 * - py-4 = padding(vertical = 16.dp)
 * - space-y-6 = Arrangement.spacedBy(24.dp)
 * - gap-6 = Arrangement.spacedBy(24.dp)
 * 
 * SIZING:
 * - w-full = fillMaxWidth()
 * - h-screen = fillMaxHeight()
 * - h-56 = height(224.dp)
 * - max-w-md = widthIn(max = 448.dp)
 * 
 * LAYOUT:
 * - flex = Row/Column
 * - flex-col = Column
 * - items-center = Alignment.CenterVertically
 * - justify-between = Arrangement.SpaceBetween
 * - grid grid-cols-2 = LazyVerticalGrid(columns = GridCells.Fixed(2))
 * 
 * BORDERS:
 * - border = border(1.dp, color)
 * - border-netflix-red/50 = border(1.dp, NetflixRed.copy(alpha = 0.5f))
 * - rounded-2xl = clip(RoundedCornerShape(16.dp))
 * 
 * EFFECTS:
 * - shadow-deep-dark = shadow(10.dp)
 * - backdrop-blur-sm = background(color.copy(alpha = 0.8f))
 * - transition-all = animateContentSize()
 * - hover:-translate-y-1 = Handle in clickable/press state
 * 
 * TEXT:
 * - text-4xl = style = MaterialTheme.typography.displayLarge
 * - font-bold = fontWeight = FontWeight.Bold
 * - text-white/80 = color = Color.White.copy(alpha = 0.8f)
 * - uppercase = textTransform = TextTransform.Uppercase (not in Compose, use .uppercase())
 * 
 * BACKGROUND:
 * - bg-primary = background(Primary)
 * - bg-gradient-deep-blue = background(Brush.verticalGradient(...))
 * - bg-black/80 = background(Color.Black.copy(alpha = 0.8f))
 */
