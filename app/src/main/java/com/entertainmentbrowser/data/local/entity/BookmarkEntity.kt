package com.entertainmentbrowser.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["url"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val url: String,
    val favicon: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
