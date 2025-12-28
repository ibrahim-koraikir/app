package com.entertainmentbrowser.presentation.home.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.entertainmentbrowser.domain.model.Category
import com.entertainmentbrowser.presentation.theme.RedPrimary
import com.entertainmentbrowser.presentation.theme.TextPrimary
import com.entertainmentbrowser.presentation.theme.TextSecondary

@Composable
fun CategoryTabs(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = Category.entries.toTypedArray()
    val selectedIndex = categories.indexOf(selectedCategory)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = TextPrimary,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = RedPrimary
                )
            }
        }
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = getCategoryDisplayName(category),
                        color = if (index == selectedIndex) TextPrimary else TextSecondary
                    )
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

private fun getCategoryDisplayName(category: Category): String {
    return when (category) {
        Category.STREAMING -> "Streaming"
        Category.TV_SHOWS -> "TV Shows"
        Category.BOOKS -> "Books"
        Category.VIDEO_PLATFORMS -> "Video Platforms"
        Category.SOCIAL_MEDIA -> "Social Media"
        Category.GAMES -> "Games"
        Category.VIDEO_CALL -> "Video Call"
        Category.ARABIC -> "عربي"
        Category.TRENDING -> "Trending"
    }
}
