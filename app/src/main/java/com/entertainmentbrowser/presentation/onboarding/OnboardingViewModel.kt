package com.entertainmentbrowser.presentation.onboarding

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entertainmentbrowser.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for onboarding flow.
 * Manages page state, permission requests, and completion status.
 * 
 * Requirements: 1.3, 1.4, 1.5
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private fun createInitialState(): OnboardingState {
        val pages = listOf(
            // Welcome page
            OnboardingPage(
                title = "Welcome to Your Entertainment Universe",
                description = "Stream, download, and browse all your favorite content in one place.",
                features = listOf(
                    Feature(
                        icon = FeatureIcon.LIGHTNING,
                        title = "Unified Entertainment Access",
                        description = "No more switching between apps. Find everything you love right here."
                    ),
                    Feature(
                        icon = FeatureIcon.DOWNLOAD,
                        title = "Smart Video Downloads",
                        description = "Download your favorite shows and movies to watch offline, anytime."
                    ),
                    Feature(
                        icon = FeatureIcon.SEARCH,
                        title = "Intuitive Browsing",
                        description = "A seamless and fast browsing experience designed for entertainment lovers."
                    )
                )
            ),
            // Features page
            OnboardingPage(
                title = "Discover Your Ultimate Entertainment Hub",
                description = "Stream, download, and browse with features designed for the true cinephile.",
                features = listOf(
                    Feature(
                        icon = FeatureIcon.ARCHIVE,
                        title = "Unified Entertainment Hub",
                        description = "Access 50+ streaming sites in one app."
                    ),
                    Feature(
                        icon = FeatureIcon.DOWNLOAD,
                        title = "Smart Video Downloads",
                        description = "Watch your favorite content offline, anytime."
                    ),
                    Feature(
                        icon = FeatureIcon.MOBILE,
                        title = "Superior Mobile Experience",
                        description = "Intuitive design for seamless browsing."
                    )
                )
            ),
            // Permissions page
            OnboardingPage(
                title = "One Last Step",
                description = "To give you the best experience, we need to access some features on your device.",
                showPermissions = true,
                permissions = listOf(
                    Permission(
                        icon = PermissionIcon.STORAGE,
                        title = "Storage",
                        description = "This allows us to download and save your favorite videos for offline viewing.",
                        permissionType = PermissionType.STORAGE
                    ),
                    Permission(
                        icon = PermissionIcon.NOTIFICATION,
                        title = "Notifications",
                        description = "We'll let you know when new content you might like is available.",
                        permissionType = PermissionType.NOTIFICATION
                    )
                )
            ),
            // Final page
            OnboardingPage(
                title = "You're All Set!",
                description = "Start exploring 45+ entertainment websites and discover your next favorite show or movie.",
                isFinalPage = true
            )
        )

        return OnboardingState(
            currentPage = 0,
            pages = pages
        )
    }

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.NextPage -> nextPage()
            is OnboardingEvent.PreviousPage -> previousPage()
            is OnboardingEvent.Skip -> skipToEnd()
            is OnboardingEvent.Complete -> completeOnboarding()
            is OnboardingEvent.PermissionResult -> handlePermissionResult(
                event.permissionType,
                event.granted
            )
            is OnboardingEvent.RequestPermissions -> {
                // Permission request is handled by the UI layer
                // This event is just for tracking
            }
        }
    }

    private fun nextPage() {
        _state.update { currentState ->
            val nextPage = (currentState.currentPage + 1).coerceAtMost(currentState.pages.size - 1)
            currentState.copy(currentPage = nextPage)
        }
    }

    private fun previousPage() {
        _state.update { currentState ->
            val prevPage = (currentState.currentPage - 1).coerceAtLeast(0)
            currentState.copy(currentPage = prevPage)
        }
    }

    private fun skipToEnd() {
        _state.update { currentState ->
            currentState.copy(currentPage = currentState.pages.size - 1)
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            try {
                settingsRepository.updateOnboardingCompleted(true)
                _state.update { it.copy(isCompleted = true) }
            } catch (e: Exception) {
                // Log error but don't block completion
                // User can still proceed to the app
                _state.update { it.copy(isCompleted = true) }
            }
        }
    }

    private fun handlePermissionResult(permissionType: PermissionType, granted: Boolean) {
        _state.update { currentState ->
            when (permissionType) {
                PermissionType.STORAGE -> currentState.copy(storagePermissionGranted = granted)
                PermissionType.NOTIFICATION -> currentState.copy(notificationPermissionGranted = granted)
            }
        }
    }

    /**
     * Returns the appropriate storage permission based on Android API level.
     * API 33+: READ_MEDIA_VIDEO
     * API 24-32: READ_EXTERNAL_STORAGE
     */
    fun getStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /**
     * Returns the notification permission for Android 13+.
     * Returns null for older versions.
     */
    fun getNotificationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }
    }
}
