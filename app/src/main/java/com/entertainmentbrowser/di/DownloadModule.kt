package com.entertainmentbrowser.di

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
// import com.tonyodev.fetch2.Fetch
// import com.tonyodev.fetch2.FetchConfiguration
// import com.tonyodev.fetch2okhttp.OkHttpDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {
    
    private const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel"
    private const val DOWNLOAD_NOTIFICATION_CHANNEL_NAME = "Downloads"
    
    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context
    ): DownloadManager {
        return context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }
    
    // TODO: Re-enable when Fetch library dependency is resolved
    // @Provides
    // @Singleton
    // fun provideFetch(
    //     @ApplicationContext context: Context,
    //     okHttpClient: OkHttpClient
    // ): Fetch {
    //     val fetchConfiguration = FetchConfiguration.Builder(context)
    //         .setDownloadConcurrentLimit(3)
    //         .setHttpDownloader(OkHttpDownloader(okHttpClient))
    //         .setNotificationManager(object : com.tonyodev.fetch2.DefaultFetchNotificationManager(context) {
    //             override fun getChannelId(notificationId: Int, context: Context): String {
    //                 return DOWNLOAD_NOTIFICATION_CHANNEL_ID
    //             }
    //         })
    //         .build()
    //     
    //     return Fetch.getInstance(fetchConfiguration)
    // }
    
    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DOWNLOAD_NOTIFICATION_CHANNEL_ID,
                DOWNLOAD_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress notifications"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        return notificationManager
    }
}
