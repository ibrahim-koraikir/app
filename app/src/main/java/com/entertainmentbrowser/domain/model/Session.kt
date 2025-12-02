package com.entertainmentbrowser.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Session(
    val id: Int,
    val name: String,
    val tabIds: List<String>,
    val createdAt: Long
)
