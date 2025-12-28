package com.entertainmentbrowser

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.entertainmentbrowser.domain.repository.SettingsRepository
import com.entertainmentbrowser.domain.repository.TabRepository
import com.entertainmentbrowser.presentation.common.components.UpdateDialog
import com.entertainmentbrowser.presentation.navigation.EntertainmentNavHost
import com.entertainmentbrowser.presentation.navigation.Screen
import com.entertainmentbrowser.presentation.theme.EntertainmentBrowserTheme
import com.entertainmentbrowser.presentation.webview.AdNetworkRotator
import com.entertainmentbrowser.presentation.webview.InterstitialAdDialog
import com.entertainmentbrowser.util.DownloadPermissionState
import com.entertainmentbrowser.util.PermissionHandler
import com.entertainmentbrowser.util.UpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity for the Entertainment Browser app.
 * Handles splash screen and determines start destination based on onboarding completion.
 * Permission handling is delegated to PermissionHandler for cleaner separation of concerns.
 * 
 * Requirements: 1.1
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var tabRepository: TabRepository
    
    @Inject
    lateinit var imageLoader: ImageLoader
    
    @Inject
    lateinit var downloadPermissionState: DownloadPermissionState
    
    @Inject
    lateinit var updateManager: UpdateManager
    
    private val startTime = System.currentTimeMillis()
    
    // Delegate permission handling to dedicated helper
    private lateinit var permissionHandler: PermissionHandler
    
    /**
     * Check if storage permission is granted for downloads.
     * Delegates to PermissionHandler.
     */
    fun hasStoragePermission(): Boolean = permissionHandler.hasStoragePermission()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }
        
        super.onCreate(savedInstanceState)
        
        // Initialize permission handler AFTER super.onCreate() so Hilt injection is complete
        // Pass downloadPermissionState so it gets updated when permissions change
        permissionHandler = PermissionHandler(this, downloadPermissionState)
        
        // Request necessary permissions via handler
        permissionHandler.requestStoragePermissionIfNeeded()
        
        // Request notification permission for API 33+ so download notifications can be shown
        permissionHandler.requestNotificationPermissionIfNeeded()
        
        // Check for app updates - pass version code directly to avoid Activity leak
        @Suppress("DEPRECATION")
        val currentVersionCode = packageManager.getPackageInfo(packageName, 0).versionCode
        updateManager.checkForUpdates(currentVersionCode)
        
        // Set window and system bar colors to black to prevent red flash
        window.setBackgroundDrawableResource(android.R.color.black)
        @Suppress("DEPRECATION") // Using deprecated API for compatibility with older Android versions
        window.statusBarColor = android.graphics.Color.BLACK
        @Suppress("DEPRECATION") // Using deprecated API for compatibility with older Android versions
        window.navigationBarColor = android.graphics.Color.BLACK
        
        // Disable edge-to-edge temporarily to test if it's causing the red border
        // enableEdgeToEdge(
        //     statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK),
        //     navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.BLACK)
        // )
        
        setContent {
            // Provide custom ImageLoader with base64 support to all composables
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                EntertainmentBrowserTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.ui.graphics.Color(0xFF121212) // DarkBackground color directly
                    ) {
                        EntertainmentBrowserApp(settingsRepository, updateManager)
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Refresh permission state in case user granted permissions via system settings
        downloadPermissionState.refreshPermissions()
        
        // Report fully drawn for startup time measurement
        lifecycleScope.launch {
            val onboardingCompleted = settingsRepository.getOnboardingCompleted().first()
            if (onboardingCompleted) {
                // Report fully drawn after first frame
                reportFullyDrawn()
                val startupTime = System.currentTimeMillis() - startTime
                if (BuildConfig.DEBUG) {
                    Log.d("Performance", "Cold start time: ${startupTime}ms")
                }
            }
        }
    }
}

@Composable
fun EntertainmentBrowserApp(settingsRepository: SettingsRepository, updateManager: UpdateManager) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }
    var showAppOpenAd by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Observe update state
    val updateState by updateManager.updateState.collectAsState()
    
    // Determine start destination based on onboarding completion
    LaunchedEffect(Unit) {
        val onboardingCompleted = settingsRepository.getOnboardingCompleted().first()
        startDestination = if (onboardingCompleted) {
            Screen.Home.route
        } else {
            Screen.Onboarding.route
        }
        
        // Show app open ad only if onboarding is completed
        if (onboardingCompleted) {
            // Initialize ad network rotator
            AdNetworkRotator.initialize(context)
            showAppOpenAd = true
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Wait for start destination to be determined
        startDestination?.let { destination ->
            EntertainmentNavHost(
                navController = navController,
                startDestination = destination,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Show update dialog when available
        if (updateState.showDialog && updateState.version != null) {
            UpdateDialog(
                versionName = updateState.version!!.versionName,
                releaseNotes = updateState.version!!.releaseNotes,
                forceUpdate = updateState.version!!.forceUpdate,
                onUpdate = { updateManager.startUpdate(context) },
                onDismiss = { updateManager.dismissUpdate() }
            )
        }
        
        // Show app open ad
        if (showAppOpenAd) {
            InterstitialAdDialog(
                onDismiss = { showAppOpenAd = false }
            )
        }
    }
}
