package com.entertainmentbrowser.presentation.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Data class representing a single onboarding page.
 * 
 * Requirements: 1.1, 1.2
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val features: List<Feature> = emptyList(),
    val showPermissions: Boolean = false,
    val permissions: List<Permission> = emptyList(),
    val isFinalPage: Boolean = false
)

/**
 * Data class representing a feature item on the onboarding page.
 */
data class Feature(
    val icon: FeatureIcon,
    val title: String,
    val description: String
)

/**
 * Enum representing feature icons.
 */
enum class FeatureIcon {
    LIGHTNING,      // Unified Entertainment Access
    DOWNLOAD,       // Smart Video Downloads
    SEARCH,         // Intuitive Browsing
    ARCHIVE,        // Unified Entertainment Hub
    MOBILE          // Superior Mobile Experience
}

/**
 * Data class representing a permission request.
 */
data class Permission(
    val icon: PermissionIcon,
    val title: String,
    val description: String,
    val permissionType: PermissionType
)

/**
 * Enum representing permission icons.
 */
enum class PermissionIcon {
    STORAGE,
    NOTIFICATION
}

/**
 * Enum representing permission types.
 */
enum class PermissionType {
    STORAGE,
    NOTIFICATION
}
