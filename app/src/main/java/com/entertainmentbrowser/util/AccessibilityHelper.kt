package com.entertainmentbrowser.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Accessibility helper utilities for the Entertainment Browser app.
 * Ensures compliance with WCAG 2.1 Level AA standards.
 */
object AccessibilityHelper {
    
    /**
     * Minimum touch target size as per Material Design guidelines.
     * Requirement: 16.2 - Maintain minimum touch target size of 48dp
     */
    val MIN_TOUCH_TARGET_SIZE: Dp = 48.dp
    
    /**
     * Adds semantic content description to a composable.
     * Requirement: 16.1 - Provide content descriptions for all interactive UI elements
     */
    fun Modifier.accessibleClickable(
        label: String,
        role: Role = Role.Button
    ): Modifier = this.semantics {
        contentDescription = label
        this.role = role
    }
    
    /**
     * Creates a descriptive label for a website card.
     */
    fun websiteCardDescription(
        name: String,
        category: String,
        isFavorite: Boolean
    ): String {
        val favoriteStatus = if (isFavorite) "Favorited" else "Not favorited"
        return "$name, $category category, $favoriteStatus. Double tap to open."
    }
    
    /**
     * Creates a descriptive label for a download item.
     */
    fun downloadItemDescription(
        filename: String,
        status: String,
        progress: Int? = null
    ): String {
        return if (progress != null) {
            "$filename, $status, $progress percent complete"
        } else {
            "$filename, $status"
        }
    }
    
    /**
     * Creates a descriptive label for a tab item.
     */
    fun tabItemDescription(
        title: String,
        isActive: Boolean
    ): String {
        val activeStatus = if (isActive) "Active tab" else "Inactive tab"
        return "$title, $activeStatus. Double tap to switch."
    }
    
    /**
     * Creates a descriptive label for a session item.
     */
    fun sessionItemDescription(
        name: String,
        tabCount: Int,
        date: String
    ): String {
        val tabText = if (tabCount == 1) "tab" else "tabs"
        return "$name, $tabCount $tabText, created $date. Double tap to restore."
    }
    
    /**
     * Validates if a color contrast ratio meets WCAG AA standards.
     * Requirement: 16.4 - Maintain minimum color contrast ratio of 4.5:1 for text
     * 
     * Note: This is a simplified check. For production, use a proper contrast checker.
     */
    fun meetsContrastRequirement(
        foregroundLuminance: Float,
        backgroundLuminance: Float,
        minRatio: Float = 4.5f
    ): Boolean {
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        val contrastRatio = (lighter + 0.05f) / (darker + 0.05f)
        return contrastRatio >= minRatio
    }
    
    /**
     * Calculates relative luminance for a color component.
     * Used for contrast ratio calculations.
     */
    fun calculateLuminance(r: Float, g: Float, b: Float): Float {
        val rsRGB = r / 255f
        val gsRGB = g / 255f
        val bsRGB = b / 255f
        
        val rLinear = if (rsRGB <= 0.03928f) rsRGB / 12.92f else Math.pow((rsRGB + 0.055) / 1.055, 2.4).toFloat()
        val gLinear = if (gsRGB <= 0.03928f) gsRGB / 12.92f else Math.pow((gsRGB + 0.055) / 1.055, 2.4).toFloat()
        val bLinear = if (bsRGB <= 0.03928f) bsRGB / 12.92f else Math.pow((bsRGB + 0.055) / 1.055, 2.4).toFloat()
        
        return 0.2126f * rLinear + 0.7152f * gLinear + 0.0722f * bLinear
    }
}
