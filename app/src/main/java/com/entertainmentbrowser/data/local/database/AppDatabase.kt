package com.entertainmentbrowser.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.entertainmentbrowser.data.local.dao.BookmarkDao
import com.entertainmentbrowser.data.local.dao.DownloadDao
import com.entertainmentbrowser.data.local.dao.SessionDao
import com.entertainmentbrowser.data.local.dao.TabDao
import com.entertainmentbrowser.data.local.dao.WebsiteDao
import com.entertainmentbrowser.data.local.entity.BookmarkEntity
import com.entertainmentbrowser.data.local.entity.DownloadEntity
import com.entertainmentbrowser.data.local.entity.SessionEntity
import com.entertainmentbrowser.data.local.entity.TabEntity
import com.entertainmentbrowser.data.local.entity.WebsiteEntity

@Database(
    entities = [
        WebsiteEntity::class,
        TabEntity::class,
        SessionEntity::class,
        DownloadEntity::class,
        BookmarkEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun websiteDao(): WebsiteDao
    abstract fun tabDao(): TabDao
    abstract fun sessionDao(): SessionDao
    abstract fun downloadDao(): DownloadDao
    abstract fun bookmarkDao(): BookmarkDao
}
