package com.entertainmentbrowser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val tabIdsJson: String, // JSON array of tab IDs
    val createdAt: Long
)
