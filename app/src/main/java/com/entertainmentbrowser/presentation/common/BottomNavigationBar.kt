package com.entertainmentbrowser.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.entertainmentbrowser.presentation.theme.DarkSurface
import com.entertainmentbrowser.presentation.theme.RedPrimary
import com.entertainmentbrowser.presentation.theme.TextPrimary
import com.entertainmentbrowser.presentation.theme.TextSecondary

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    data object Home : BottomNavItem("home", Icons.Default.Home, "Home")
    data object Favorites : BottomNavItem("favorites", Icons.Default.Favorite, "Favorites")
    data object Downloads : BottomNavItem("downloads", Icons.Default.Download, "Downloads")
    data object Tabs : BottomNavItem("tabs", Icons.Default.Tab, "Tabs")
    data object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
}

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Favorites,
        BottomNavItem.Downloads,
        BottomNavItem.Tabs,
        BottomNavItem.Settings
    )

    NavigationBar(
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(text = item.label)
                },
                selected = selected,
                onClick = {
                    if (!selected) {
                        onNavigate(item.route)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RedPrimary,
                    selectedTextColor = RedPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = RedPrimary.copy(alpha = 0.2f)
                )
            )
        }
    }
}
