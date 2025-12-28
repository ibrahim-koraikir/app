package com.entertainmentbrowser.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navDeepLink
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.entertainmentbrowser.presentation.common.BottomNavigationBar
import com.entertainmentbrowser.presentation.bookmarks.BookmarksScreen
import com.entertainmentbrowser.presentation.home.HomeScreen
import com.entertainmentbrowser.presentation.onboarding.OnboardingScreen
import com.entertainmentbrowser.presentation.sessions.SessionsScreen
import com.entertainmentbrowser.presentation.tabs.TabsScreen
import com.entertainmentbrowser.presentation.webview.WebViewScreen
import com.entertainmentbrowser.util.adblock.AntiAdblockBypass
import com.entertainmentbrowser.util.adblock.FastAdBlockEngine
import dagger.hilt.android.EntryPointAccessors
import com.entertainmentbrowser.EntertainmentBrowserApp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Entry point for accessing DownloadRepository from Hilt in Composable context
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloadRepositoryEntryPoint {
    fun downloadRepository(): com.entertainmentbrowser.domain.repository.DownloadRepository
}

/**
 * Main navigation host for the Entertainment Browser app.
 * Defines all navigation destinations and their relationships.
 *
 * @param navController The navigation controller managing the navigation stack
 * @param startDestination The initial destination (Onboarding or Home based on first launch)
 * @param modifier Modifier for the NavHost
 */
@Composable
fun EntertainmentNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Get singletons from Hilt - shared across all tabs
    val fastAdBlockEngine = remember {
        val appContext = context.applicationContext as EntertainmentBrowserApp
        appContext.fastAdBlockEngine
    }
    
    val advancedAdBlockEngine = remember {
        val appContext = context.applicationContext as EntertainmentBrowserApp
        appContext.advancedAdBlockEngine
    }
    
    val webViewStateManager = remember {
        val appContext = context.applicationContext as EntertainmentBrowserApp
        appContext.webViewStateManager
    }
    
    // Get AntiAdblockBypass from Hilt - shared across all tabs
    val antiAdblockBypass = remember {
        val appContext = context.applicationContext as EntertainmentBrowserApp
        appContext.antiAdblockBypass
    }
    
    // Get DownloadRepository from Hilt
    val downloadRepository = remember {
        val appContext = context.applicationContext as EntertainmentBrowserApp
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            DownloadRepositoryEntryPoint::class.java
        )
        entryPoint.downloadRepository()
    }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
        // Onboarding flow
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Home screen with website catalog
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToWebView = { url ->
                    navController.navigate(Screen.WebView.createRoute(url))
                },
                onNavigateToBookmarks = {
                    navController.navigate(Screen.Bookmarks.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToActiveTab = {
                    // Navigate to WebView to show active tab (no new tab created)
                    navController.navigate(Screen.WebView.createActiveTabRoute())
                }
            )
        }
        
        // WebView screen with deep link support
        composable(
            route = Screen.WebView.route,
            arguments = Screen.WebView.arguments,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = DeepLink.WEBVIEW
                }
            )
        ) {
            WebViewScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                fastAdBlockEngine = fastAdBlockEngine,
                advancedAdBlockEngine = advancedAdBlockEngine,
                antiAdblockBypass = antiAdblockBypass,
                webViewStateManager = webViewStateManager,
                downloadRepository = downloadRepository
            )
        }
        
        // Downloads screen
        composable(route = Screen.Downloads.route) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // DownloadsScreen will be implemented in task 10
            // DownloadsScreen(
            //     bottomBar = {
            //         BottomNavigationBar(
            //             currentRoute = currentRoute,
            //             onNavigate = { route ->
            //                 navController.navigate(route) {
            //                     popUpTo(Screen.Home.route) {
            //                         saveState = true
            //                     }
            //                     launchSingleTop = true
            //                     restoreState = true
            //                 }
            //             }
            //         )
            //     }
            // )
            PlaceholderScreen(screenName = "Downloads")
        }
        
        // Tabs screen
        composable(route = Screen.Tabs.route) {
            TabsScreen(
                onNavigateToWebView = { url ->
                    navController.navigate(Screen.WebView.createRoute(url))
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Sessions screen
        composable(route = Screen.Sessions.route) {
            SessionsScreen(
                onSessionRestored = { tabIds ->
                    // Navigate to tabs screen after restoring session
                    navController.navigate(Screen.Tabs.route) {
                        popUpTo(Screen.Home.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        
        // Settings screen
        composable(route = Screen.Settings.route) {
            com.entertainmentbrowser.presentation.settings.SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToBookmarks = {
                    android.util.Log.d("NavHost", "Navigating to Bookmarks screen")
                    navController.navigate(Screen.Bookmarks.route)
                }
            )
        }
        
        // Bookmarks screen
        composable(route = Screen.Bookmarks.route) {
            BookmarksScreen(
                onNavigateToWebView = { url ->
                    navController.navigate(Screen.WebView.createRoute(url))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        }
    }
}

/**
 * Placeholder composable for screens not yet implemented.
 * This allows the navigation structure to be tested before all screens are built.
 */
@Composable
private fun PlaceholderScreen(screenName: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$screenName Screen\n(Coming Soon)",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
