package com.entertainmentbrowser.presentation.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Onboarding screen container with HorizontalPager for navigation.
 * Handles page indicators, completion, and navigation to home.
 * 
 * Requirements: 1.2, 1.3
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { state.pages.size })
    val scope = rememberCoroutineScope()

    // Permission launchers
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onEvent(
            OnboardingEvent.PermissionResult(
                permissionType = PermissionType.STORAGE,
                granted = granted
            )
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onEvent(
            OnboardingEvent.PermissionResult(
                permissionType = PermissionType.NOTIFICATION,
                granted = granted
            )
        )
    }

    // Sync pager state with view model state
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.scrollToPage(state.currentPage)
        }
    }

    // Handle completion
    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            onComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false // Disable swipe, use buttons only
        ) { page ->
            val currentPage = state.pages[page]

            when {
                page == 0 -> {
                    // Welcome screen
                    WelcomeScreen(
                        page = currentPage,
                        onNext = {
                            viewModel.onEvent(OnboardingEvent.NextPage)
                        }
                    )
                }
                page == state.pages.size - 1 && currentPage.isFinalPage -> {
                    // Final screen
                    FinalScreen(
                        page = currentPage,
                        onComplete = {
                            viewModel.onEvent(OnboardingEvent.Complete)
                        }
                    )
                }
                currentPage.showPermissions -> {
                    // Permissions screen
                    PermissionsScreen(
                        page = currentPage,
                        onGrantPermissions = {
                            // Request storage permission
                            storagePermissionLauncher.launch(viewModel.getStoragePermission())
                            
                            // Request notification permission if Android 13+
                            viewModel.getNotificationPermission()?.let { permission ->
                                notificationPermissionLauncher.launch(permission)
                            }
                            
                            viewModel.onEvent(OnboardingEvent.RequestPermissions)
                            viewModel.onEvent(OnboardingEvent.NextPage)
                        },
                        onSkip = {
                            viewModel.onEvent(OnboardingEvent.NextPage)
                        }
                    )
                }
                else -> {
                    // Features screen
                    FeaturesScreen(
                        page = currentPage,
                        onNext = {
                            viewModel.onEvent(OnboardingEvent.NextPage)
                        },
                        onSkip = {
                            viewModel.onEvent(OnboardingEvent.Skip)
                        }
                    )
                }
            }
        }

        // Page indicators - hidden as they overlap with content
        // The onboarding flow is linear with buttons, so indicators aren't needed
    }
}


