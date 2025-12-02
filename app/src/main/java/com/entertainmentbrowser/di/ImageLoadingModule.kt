package com.entertainmentbrowser.di

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.MemoryCache
import coil.request.Options
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okio.Buffer
import javax.inject.Singleton

/**
 * Custom Fetcher for base64 data URIs
 */
class Base64Fetcher(
    private val data: String,
    private val options: Options
) : Fetcher {
    
    override suspend fun fetch(): FetchResult {
        // Extract base64 data from data URI
        val base64Data = data.substringAfter("base64,")
        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
        
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
            if (!data.startsWith("data:image")) return null
            return Base64Fetcher(data, options)
        }
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
