package com.entertainmentbrowser.di

import android.content.Context
import android.util.Base64
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.Options
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okio.Buffer
import java.security.MessageDigest
import javax.inject.Singleton

/**
 * Custom Fetcher for base64 data URIs.
 * 
 * Coil's memory cache keys on the data model (the base64 string). For large base64 strings,
 * we use a hash-based key via [Base64Keyer] to avoid memory overhead from storing full strings
 * as cache keys. The actual decode only happens on cache miss.
 */
class Base64Fetcher(
    private val data: String,
    private val options: Options
) : Fetcher {
    
    override suspend fun fetch(): FetchResult {
        // Extract base64 data from data URI - handle various formats
        val base64Data = data
            .substringAfter("base64,")
            .replace("\\s".toRegex(), "") // Remove any whitespace
            .trim()
        
        val bytes = try {
            Base64.decode(base64Data, Base64.DEFAULT)
        } catch (e: Exception) {
            // Try with NO_WRAP flag
            Base64.decode(base64Data, Base64.NO_WRAP)
        }
        
        val buffer = Buffer().write(bytes)
        return SourceResult(
            source = ImageSource(buffer, options.context),
            mimeType = extractMimeType(data),
            dataSource = DataSource.MEMORY
        )
    }
    
    private fun extractMimeType(dataUri: String): String {
        return try {
            dataUri.substringAfter("data:").substringBefore(";")
        } catch (e: Exception) {
            "image/png"
        }
    }
    
    class Factory : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Check for data URI format - be more lenient
            val trimmed = data.trim()
            if (!trimmed.startsWith("data:image")) return null
            return Base64Fetcher(trimmed, options)
        }
    }
}

/**
 * Custom Keyer for base64 data URIs.
 * 
 * For small base64 strings (< 1KB), uses the string directly as the cache key.
 * For larger strings, computes a SHA-256 hash to avoid memory overhead while
 * still ensuring cache hits for identical images.
 */
class Base64Keyer : Keyer<String> {
    
    companion object {
        private const val HASH_THRESHOLD = 1024 // 1KB threshold for hashing
    }
    
    override fun key(data: String, options: Options): String? {
        val trimmed = data.trim()
        if (!trimmed.startsWith("data:image")) return null
        
        // For small base64 strings, use directly (fast equality check)
        if (trimmed.length < HASH_THRESHOLD) {
            return trimmed
        }
        
        // For larger strings, use hash to save memory in cache keys
        return "base64:${computeHash(trimmed)}"
    }
    
    private fun computeHash(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(data.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ImageLoadingModule {
    
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(Base64Keyer())
                add(Base64Fetcher.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of app memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB
                    .build()
            }
            .okHttpClient(okHttpClient)
            .crossfade(150) // 150ms crossfade duration
            .respectCacheHeaders(false)
            .build()
    }
}
