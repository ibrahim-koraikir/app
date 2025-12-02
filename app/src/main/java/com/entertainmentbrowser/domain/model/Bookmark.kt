package com.entertainmentbrowser.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Bookmark(
    val id: Int = 0,
    val title: String,
    val url: String,
    val favicon: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
