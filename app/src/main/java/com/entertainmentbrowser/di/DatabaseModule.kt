package com.entertainmentbrowser.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.entertainmentbrowser.core.constants.Constants
import com.entertainmentbrowser.data.local.database.AppDatabase
import com.entertainmentbrowser.data.local.dao.SessionDao
import com.entertainmentbrowser.data.local.dao.TabDao
import com.entertainmentbrowser.data.local.dao.WebsiteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add index on name column for websites table
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_websites_name` ON `websites` (`name`)")
            
            // Add index on timestamp column for tabs table
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tabs_timestamp` ON `tabs` (`timestamp`)")
        }
    }
    
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create bookmarks table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `bookmarks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `url` TEXT NOT NULL,
                    `favicon` TEXT,
                    `createdAt` INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Add indexes for bookmarks table
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bookmarks_url` ON `bookmarks` (`url`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_createdAt` ON `bookmarks` (`createdAt`)")
        }
    }
    
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema changes - just adding new prepopulated websites
            // The new websites will be added by WebsiteRepository.prepopulateWebsites()
        }
    }
    
    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Delete all websites to repopulate with new TRENDING category
            db.execSQL("DELETE FROM websites")
        }
    }
    
    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add contentUri and displayPath columns for scoped storage support
            db.execSQL("ALTER TABLE downloads ADD COLUMN contentUri TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE downloads ADD COLUMN displayPath TEXT DEFAULT NULL")
        }
    }
    
    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add lastAccessedAt column for tab cleanup based on access time
            // Default to timestamp for existing tabs
            db.execSQL("ALTER TABLE tabs ADD COLUMN lastAccessedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE tabs SET lastAccessedAt = timestamp WHERE lastAccessedAt = 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tabs_lastAccessedAt` ON `tabs` (`lastAccessedAt`)")
        }
    }
    
    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Delete all websites to repopulate with new TRENDING category websites
            db.execSQL("DELETE FROM websites")
        }
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Prepopulation will be handled by WebsiteRepository on first launch
                }
            })
            .build()
    }
    
    @Provides
    fun provideWebsiteDao(database: AppDatabase): WebsiteDao {
        return database.websiteDao()
    }
    
    @Provides
    fun provideTabDao(database: AppDatabase): TabDao {
        return database.tabDao()
    }
    
    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao {
        return database.sessionDao()
    }
    
    @Provides
    fun provideDownloadDao(database: AppDatabase): com.entertainmentbrowser.data.local.dao.DownloadDao {
        return database.downloadDao()
    }
    
    @Provides
    fun provideBookmarkDao(database: AppDatabase): com.entertainmentbrowser.data.local.dao.BookmarkDao {
        return database.bookmarkDao()
    }
}
