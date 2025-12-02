package com.entertainmentbrowser.presentation.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.entertainmentbrowser.presentation.theme.RedPrimary
import kotlinx.coroutines.launch

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

        // Page indicators
        if (state.pages.isNotEmpty()) {
            PageIndicators(
                pageCount = state.pages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            )
        }
    }
}

@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) RedPrimary
                        else Color.White.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
