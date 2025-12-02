package com.entertainmentbrowser.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.SearchOff,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val emptyStateDescription = "$title. $message${if (actionLabel != null) ". $actionLabel button available." else ""}"
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics { contentDescription = emptyStateDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onAction,
                modifier = Modifier
                    .height(48.dp)
                    .semantics { contentDescription = "$actionLabel button" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun EmptyFavoritesState(
    onBrowseWebsites: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Favorites Yet",
        message = "Start adding your favorite entertainment websites to see them here",
        modifier = modifier,
        icon = Icons.Default.BookmarkBorder,
        actionLabel = "Browse Websites",
        onAction = onBrowseWebsites
    )
}

@Composable
fun EmptySearchState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Results Found",
        message = "Try adjusting your search terms or browse by category",
        modifier = modifier,
        icon = Icons.Default.SearchOff
    )
}

@Composable
fun EmptyDownloadsState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Downloads",
        message = "Videos you download will appear here",
        modifier = modifier,
        icon = Icons.Default.Download
    )
}

@Composable
fun EmptyTabsState(
    onOpenWebsite: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Open Tabs",
        message = "Open a website to start browsing",
        modifier = modifier,
        icon = Icons.Default.Tab,
        actionLabel = "Browse Websites",
        onAction = onOpenWebsite
    )
}

@Composable
fun EmptySessionsState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No Saved Sessions",
        message = "Save your current tabs as a session to restore them later",
        modifier = modifier,
        icon = Icons.Default.Bookmark
    )
}
