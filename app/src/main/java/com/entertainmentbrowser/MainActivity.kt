package com.entertainmentbrowser

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.compose.LocalImageLoader
import com.entertainmentbrowser.BuildConfig
import com.entertainmentbrowser.domain.repository.SettingsRepository
import com.entertainmentbrowser.domain.repository.TabRepository
import com.entertainmentbrowser.presentation.navigation.EntertainmentNavHost
import com.entertainmentbrowser.presentation.navigation.Screen
import com.entertainmentbrowser.presentation.theme.EntertainmentBrowserTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main activity for the Entertainment Browser app.
 * Handles splash screen and determines start destination based on onboarding completion.
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
    
    private val startTime = System.currentTimeMillis()
    
    // Track storage permission state for API 24-28
    private var storagePermissionGranted = mutableStateOf(true) // Default true for API 29+
    
    // Permission launcher for storage access (needed for Android 6-9)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        storagePermissionGranted.value = isGranted
        if (isGranted) {
            Log.d("MainActivity", "Storage permission granted")
        } else {
            Log.w("MainActivity", "Storage permission denied - downloads will fail on API 24-28")
        }
    }
    
    /**
     * Check if storage permission is granted for downloads.
     * Returns true for API 29+ (scoped storage doesn't need permission).
     * Returns actual permission state for API 24-28.
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Scoped storage, no permission needed
        } else {
            storagePermissionGranted.value
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
        }
        
        super.onCreate(savedInstanceState)
        
        // Request storage permission for Android 6-9 (API 23-28)
        // Android 10+ uses scoped storage and doesn't need this permission for Downloads
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.P) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            
            storagePermissionGranted.value = hasPermission
            
            if (!hasPermission) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            // API 29+ doesn't need permission for Downloads directory
            storagePermissionGranted.value = true
        }
        
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
                        EntertainmentBrowserApp(settingsRepository)
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
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
fun EntertainmentBrowserApp(settingsRepository: SettingsRepository) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }
    
    // Determine start destination based on onboarding completion
    LaunchedEffect(Unit) {
        val onboardingCompleted = settingsRepository.getOnboardingCompleted().first()
        startDestination = if (onboardingCompleted) {
            Screen.Home.route
        } else {
            Screen.Onboarding.route
        }
    }
    
    // Wait for start destination to be determined
    startDestination?.let { destination ->
        EntertainmentNavHost(
            navController = navController,
            startDestination = destination,
            modifier = Modifier.fillMaxSize()
        )
    }
}
