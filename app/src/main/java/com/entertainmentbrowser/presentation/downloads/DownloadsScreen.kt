package com.entertainmentbrowser.presentation.downloads

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.entertainmentbrowser.domain.model.DownloadItem
import com.entertainmentbrowser.domain.model.DownloadStatus
import com.entertainmentbrowser.util.MediaStoreHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.activeDownloads.isEmpty() && 
                uiState.completedDownloads.isEmpty() && 
                uiState.failedDownloads.isEmpty()
            ) {
                EmptyDownloadsState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Active Downloads Section
                    if (uiState.activeDownloads.isNotEmpty()) {
                        item {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            items = uiState.activeDownloads,
                            key = { it.id }
                        ) { download ->
                            DownloadItemCard(
                                download = download,
                                onPause = { viewModel.onEvent(DownloadsEvent.PauseDownload(it)) },
                                onResume = { viewModel.onEvent(DownloadsEvent.ResumeDownload(it)) },
                                onCancel = { viewModel.onEvent(DownloadsEvent.CancelDownload(it)) },
                                onDelete = { viewModel.onEvent(DownloadsEvent.DeleteDownload(it)) },
                                onOpen = { downloadId ->
                                    val download = uiState.completedDownloads.find { it.id == downloadId }
                                    download?.let {
                                        val uri = if (it.filePath != null) {
                                            Uri.parse(it.filePath)
                                        } else {
                                            MediaStoreHelper.getContentUri(context, it.filename)
                                        }
                                        val mimeType = MediaStoreHelper.getMimeType(it.filename)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, mimeType)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                    
                    // Completed Downloads Section
                    if (uiState.completedDownloads.isNotEmpty()) {
                        item {
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            items = uiState.completedDownloads,
                            key = { it.id }
                        ) { download ->
                            DownloadItemCard(
                                download = download,
                                onPause = { viewModel.onEvent(DownloadsEvent.PauseDownload(it)) },
                                onResume = { viewModel.onEvent(DownloadsEvent.ResumeDownload(it)) },
                                onCancel = { viewModel.onEvent(DownloadsEvent.CancelDownload(it)) },
                                onDelete = { viewModel.onEvent(DownloadsEvent.DeleteDownload(it)) },
                                onOpen = { downloadId ->
                                    val download = uiState.completedDownloads.find { it.id == downloadId }
                                    download?.let {
                                        val uri = if (it.filePath != null) {
                                            Uri.parse(it.filePath)
                                        } else {
                                            MediaStoreHelper.getContentUri(context, it.filename)
                                        }
                                        val mimeType = MediaStoreHelper.getMimeType(it.filename)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, mimeType)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                    
                    // Failed Downloads Section
                    if (uiState.failedDownloads.isNotEmpty()) {
                        item {
                            Text(
                                text = "Failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(
                            items = uiState.failedDownloads,
                            key = { it.id }
                        ) { download ->
                            DownloadItemCard(
                                download = download,
                                onPause = { viewModel.onEvent(DownloadsEvent.PauseDownload(it)) },
                                onResume = { viewModel.onEvent(DownloadsEvent.ResumeDownload(it)) },
                                onCancel = { viewModel.onEvent(DownloadsEvent.CancelDownload(it)) },
                                onDelete = { viewModel.onEvent(DownloadsEvent.DeleteDownload(it)) },
                                onOpen = { downloadId ->
                                    val download = uiState.completedDownloads.find { it.id == downloadId }
                                    download?.let {
                                        val uri = if (it.filePath != null) {
                                            Uri.parse(it.filePath)
                                        } else {
                                            MediaStoreHelper.getContentUri(context, it.filename)
                                        }
                                        val mimeType = MediaStoreHelper.getMimeType(it.filename)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, mimeType)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // Error Snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(
    download: DownloadItem,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onCancel: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onOpen: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Filename
            Text(
                text = download.filename,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar for active downloads
            if (download.status == DownloadStatus.DOWNLOADING || 
                download.status == DownloadStatus.QUEUED ||
                download.status == DownloadStatus.PAUSED
            ) {
                LinearProgressIndicator(
                    progress = { download.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${download.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = formatBytes(download.downloadedBytes) + " / " + 
                               formatBytes(download.totalBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Status text
            Text(
                text = download.status.name,
                style = MaterialTheme.typography.bodySmall,
                color = when (download.status) {
                    DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = { onPause(download.id) }) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                        }
                        IconButton(onClick = { onCancel(download.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        IconButton(onClick = { onResume(download.id) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                        }
                        IconButton(onClick = { onCancel(download.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                    DownloadStatus.COMPLETED -> {
                        IconButton(onClick = { onOpen(download.id) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Open")
                        }
                        IconButton(onClick = { onDelete(download.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        IconButton(onClick = { onDelete(download.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun EmptyDownloadsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No downloads yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Downloads will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "Unknown"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(java.util.Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(java.util.Locale.US, "%.1f GB", gb)
}
