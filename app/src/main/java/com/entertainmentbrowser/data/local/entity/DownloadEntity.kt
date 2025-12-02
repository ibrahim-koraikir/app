package com.entertainmentbrowser.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloads",
    indices = [
        Index(value = ["status"])
    ]
)
data class DownloadEntity(
    @PrimaryKey
    val id: Int,
    val url: String,
    val filename: String,
    val filePath: String?,
    val status: String,
    val progress: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val createdAt: Long
)
