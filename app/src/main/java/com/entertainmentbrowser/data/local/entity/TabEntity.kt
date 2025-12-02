package com.entertainmentbrowser.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tabs",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["timestamp"])
    ]
)
data class TabEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val thumbnailPath: String?,
    val isActive: Boolean,
    val timestamp: Long
)
