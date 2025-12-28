package com.entertainmentbrowser.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data model for version.json response
 */
@Serializable
data class AppVersion(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean = false
)

/**
 * UI state for update dialog
 */
data class UpdateState(
    val showDialog: Boolean = false,
    val version: AppVersion? = null
)

/**
 * Manages in-app update checks and prompts.
 * Checks a remote version.json file and prompts user to update if a newer version is available.
 */
@Singleton
class UpdateManager @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    // Use a proper scope that survives Activity lifecycle but can be cancelled
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()
    
    companion object {
        private const val TAG = "UpdateManager"
        private const val VERSION_URL = "https://xhub.site/version.json"
    }
    
    /**
     * Check for app updates.
     * @param currentVersionCode The current app version code (pass this instead of Context to avoid leaks)
     */
    fun checkForUpdates(currentVersionCode: Int) {
        scope.launch {
            try {
                val request = Request.Builder()
                    .url(VERSION_URL)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to check updates: ${response.code}")
                    return@launch
                }
                
                val responseBody = response.body?.string() ?: return@launch
                val version = json.decodeFromString<AppVersion>(responseBody)
                
                if (version.versionCode > currentVersionCode) {
                    _updateState.value = UpdateState(
                        showDialog = true,
                        version = version
                    )
                    Log.d(TAG, "Update available: ${version.versionName} (code: ${version.versionCode})")
                } else {
                    Log.d(TAG, "App is up to date (current: $currentVersionCode, remote: ${version.versionCode})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
            }
        }
    }
    
    /**
     * Dismiss the update dialog
     */
    fun dismissUpdate() {
        val currentState = _updateState.value
        if (currentState.version?.forceUpdate != true) {
            _updateState.value = UpdateState(showDialog = false, version = null)
        }
    }
    
    /**
     * Start the update download
     */
    fun startUpdate(context: Context) {
        val url = _updateState.value.version?.apkUrl ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
