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
    /** Content URI for scoped storage (API 29+), null for legacy file paths */
    val contentUri: String? = null,
    /** Display path for UI purposes (e.g., "Downloads/video.mp4"), not for file access */
    val displayPath: String? = null,
    /** @deprecated Use contentUri instead. Kept for legacy API <29 compatibility */
    val filePath: String? = null,
    val status: String,
    val progress: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val createdAt: Long
)
