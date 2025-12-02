package com.entertainmentbrowser.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Website(
    val id: Int,
    val name: String,
    val url: String,
    val category: Category,
    val logoUrl: String,
    val description: String,
    val backgroundColor: String,
    val isFavorite: Boolean,
    val order: Int
)
