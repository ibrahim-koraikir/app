package com.entertainmentbrowser.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "websites",
    indices = [
        Index(value = ["category"]),
        Index(value = ["isFavorite"]),
        Index(value = ["name"])
    ]
)
data class WebsiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val url: String,
    val category: String,
    val logoUrl: String,
    val description: String,
    val backgroundColor: String,
    val isFavorite: Boolean,
    val order: Int
)
