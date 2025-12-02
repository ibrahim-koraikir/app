package com.entertainmentbrowser.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for capturing WebView thumbnails.
 * Captures the current WebView content as a bitmap, scales it to thumbnail size,
 * and saves it to internal storage.
 */
@Singleton
class ThumbnailCapture @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val THUMBNAIL_SIZE = 120 // 40dp * 3 (for high DPI)
        private const val THUMBNAIL_DIR = "thumbnails"
        private const val THUMBNAIL_QUALITY = 80 // JPEG quality (0-100)
    }
    
    /**
     * Captures a WebView as a thumbnail and saves it to internal storage.
     * 
     * @param webView The WebView to capture
     * @param tabId The ID of the tab (used for filename)
     * @return The file path of the saved thumbnail, or null if capture failed
     */
    suspend fun captureWebViewThumbnail(webView: WebView, tabId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Capture WebView as bitmap on main thread
                val bitmap = withContext(Dispatchers.Main) {
                    captureWebViewBitmap(webView)
                } ?: return@withContext null
                
                // Scale to thumbnail size
                val thumbnail = scaleBitmapToThumbnail(bitmap)
                
                // Save to internal storage
                val filePath = saveThumbnailToStorage(thumbnail, tabId)
                
                // Recycle bitmaps to free memory
                bitmap.recycle()
                thumbnail.recycle()
                
                filePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Captures the WebView content as a bitmap.
     * Must be called on the main thread.
     * 
     * @param webView The WebView to capture
     * @return The captured bitmap, or null if capture failed
     */
    private fun captureWebViewBitmap(webView: WebView): Bitmap? {
        return try {
            val width = webView.width
            val height = webView.height
            
            if (width <= 0 || height <= 0) {
                return null
            }
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Scales a bitmap to thumbnail size while maintaining aspect ratio.
     * 
     * @param bitmap The original bitmap
     * @return The scaled thumbnail bitmap
     */
    private fun scaleBitmapToThumbnail(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Calculate scale factor to fit within thumbnail size
        val scale = THUMBNAIL_SIZE.toFloat() / maxOf(width, height)
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    }
    
    /**
     * Saves a thumbnail bitmap to internal storage.
     * 
     * @param thumbnail The thumbnail bitmap to save
     * @param tabId The ID of the tab (used for filename)
     * @return The file path of the saved thumbnail
     */
    private fun saveThumbnailToStorage(thumbnail: Bitmap, tabId: String): String {
        // Create thumbnails directory if it doesn't exist
        val thumbnailDir = File(context.filesDir, THUMBNAIL_DIR)
        if (!thumbnailDir.exists()) {
            thumbnailDir.mkdirs()
        }
        
        // Create file for thumbnail
        val thumbnailFile = File(thumbnailDir, "$tabId.jpg")
        
        // Save bitmap as JPEG
        FileOutputStream(thumbnailFile).use { out ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, out)
        }
        
        return thumbnailFile.absolutePath
    }
    
    /**
     * Deletes a thumbnail file from storage.
     * 
     * @param thumbnailPath The path to the thumbnail file
     */
    suspend fun deleteThumbnail(thumbnailPath: String) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(thumbnailPath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Deletes all thumbnails older than the specified time.
     * 
     * @param cutoffTime Timestamp in milliseconds
     */
    suspend fun deleteOldThumbnails(cutoffTime: Long) {
        withContext(Dispatchers.IO) {
            try {
                val thumbnailDir = File(context.filesDir, THUMBNAIL_DIR)
                if (thumbnailDir.exists()) {
                    thumbnailDir.listFiles()?.forEach { file ->
                        if (file.lastModified() < cutoffTime) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
