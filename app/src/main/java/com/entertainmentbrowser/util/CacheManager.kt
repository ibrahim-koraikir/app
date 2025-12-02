package com.entertainmentbrowser.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages cache cleanup and monitoring for the application.
 * Handles periodic cleanup of old cache files and provides cache size information.
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Clears cache files older than the specified age.
     * Default is 7 days (604800000 milliseconds).
     *
     * @param maxAgeMillis Maximum age of cache files to keep in milliseconds
     */
    suspend fun clearOldCache(maxAgeMillis: Long = 7 * 24 * 60 * 60 * 1000L) = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val currentTime = System.currentTimeMillis()
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val fileAge = currentTime - file.lastModified()
                    if (fileAge > maxAgeMillis) {
                        val deleted = file.delete()
                        if (deleted) {
                            Log.d("CacheManager", "Deleted old cache file: ${file.name}")
                        }
                    }
                } else if (file.isDirectory) {
                    // Recursively clean subdirectories
                    clearOldCacheInDirectory(file, maxAgeMillis, currentTime)
                }
            }
            
            Log.i("CacheManager", "Old cache cleanup completed")
        } catch (e: Exception) {
            Log.e("CacheManager", "Error clearing old cache", e)
        }
    }
    
    /**
     * Recursively clears old cache files in a directory.
     */
    private fun clearOldCacheInDirectory(directory: File, maxAgeMillis: Long, currentTime: Long) {
        try {
            directory.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val fileAge = currentTime - file.lastModified()
                    if (fileAge > maxAgeMillis) {
                        file.delete()
                    }
                } else if (file.isDirectory) {
                    clearOldCacheInDirectory(file, maxAgeMillis, currentTime)
                    // Delete empty directories
                    if (file.listFiles()?.isEmpty() == true) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CacheManager", "Error clearing cache in directory: ${directory.name}", e)
        }
    }
    
    /**
     * Calculates the total size of the cache directory.
     *
     * @return Total cache size in bytes
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            calculateDirectorySize(cacheDir)
        } catch (e: Exception) {
            Log.e("CacheManager", "Error calculating cache size", e)
            0L
        }
    }
    
    /**
     * Recursively calculates the size of a directory.
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        try {
            directory.listFiles()?.forEach { file ->
                size += if (file.isFile) {
                    file.length()
                } else {
                    calculateDirectorySize(file)
                }
            }
        } catch (e: Exception) {
            Log.e("CacheManager", "Error calculating directory size: ${directory.name}", e)
        }
        return size
    }
    
    /**
     * Clears all cache files by deleting and recreating the cache directory.
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            
            // Delete all files and subdirectories
            cacheDir.listFiles()?.forEach { file ->
                deleteRecursively(file)
            }
            
            // Recreate the cache directory if it was deleted
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            Log.i("CacheManager", "All cache cleared successfully")
        } catch (e: Exception) {
            Log.e("CacheManager", "Error clearing all cache", e)
        }
    }
    
    /**
     * Recursively deletes a file or directory.
     */
    private fun deleteRecursively(file: File) {
        try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    deleteRecursively(child)
                }
            }
            file.delete()
        } catch (e: Exception) {
            Log.e("CacheManager", "Error deleting file: ${file.name}", e)
        }
    }
}
