package com.entertainmentbrowser.di

import com.entertainmentbrowser.data.repository.BookmarkRepositoryImpl
import com.entertainmentbrowser.data.repository.DownloadRepositoryImpl
import com.entertainmentbrowser.data.repository.SessionRepositoryImpl
import com.entertainmentbrowser.data.repository.SettingsRepositoryImpl
import com.entertainmentbrowser.data.repository.TabRepositoryImpl
import com.entertainmentbrowser.data.repository.WebsiteRepositoryImpl
import com.entertainmentbrowser.domain.repository.BookmarkRepository
import com.entertainmentbrowser.domain.repository.DownloadRepository
import com.entertainmentbrowser.domain.repository.SessionRepository
import com.entertainmentbrowser.domain.repository.SettingsRepository
import com.entertainmentbrowser.domain.repository.TabRepository
import com.entertainmentbrowser.domain.repository.WebsiteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindWebsiteRepository(
        impl: WebsiteRepositoryImpl
    ): WebsiteRepository
    
    @Binds
    @Singleton
    abstract fun bindTabRepository(
        impl: TabRepositoryImpl
    ): TabRepository
    
    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ): SessionRepository
    
    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: DownloadRepositoryImpl
    ): DownloadRepository
    
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
    
    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(
        impl: BookmarkRepositoryImpl
    ): BookmarkRepository
}
