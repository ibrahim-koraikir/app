package com.entertainmentbrowser.presentation.onboarding

/**
 * Data class representing the state of the onboarding flow.
 * 
 * Requirements: 1.1, 1.2
 */
data class OnboardingState(
    val currentPage: Int = 0,
    val pages: List<OnboardingPage> = emptyList(),
    val storagePermissionGranted: Boolean = false,
    val notificationPermissionGranted: Boolean = false,
    val isCompleted: Boolean = false
)

/**
 * Sealed interface representing onboarding events.
 */
sealed interface OnboardingEvent {
    data object NextPage : OnboardingEvent
    data object PreviousPage : OnboardingEvent
    data object Skip : OnboardingEvent
    data object Complete : OnboardingEvent
    data class PermissionResult(
        val permissionType: PermissionType,
        val granted: Boolean
    ) : OnboardingEvent
    data object RequestPermissions : OnboardingEvent
}
