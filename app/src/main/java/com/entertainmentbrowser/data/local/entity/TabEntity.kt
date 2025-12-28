package com.entertainmentbrowser.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tabs",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["timestamp"]),
        Index(value = ["lastAccessedAt"])
    ]
)
data class TabEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val thumbnailPath: String?,
    val isActive: Boolean,
    val timestamp: Long,
    /** Last time this tab was accessed/switched to. Used for cleanup decisions. */
    val lastAccessedAt: Long = timestamp
)
