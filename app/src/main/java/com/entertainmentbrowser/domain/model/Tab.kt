package com.entertainmentbrowser.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Tab(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailPath: String?,
    val isActive: Boolean,
    val timestamp: Long
)
