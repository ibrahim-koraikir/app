package com.entertainmentbrowser.presentation.webview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

data class VideoQuality(
    val label: String,
    val description: String,
    val isRecommended: Boolean = false
)

@Composable
fun DownloadDialog(
    videoUrl: String,
    suggestedFilename: String,
    onConfirm: (filename: String, quality: String) -> Unit,
    onDismiss: () -> Unit
) {
    var filename by remember { mutableStateOf(suggestedFilename) }
    
    // Available quality options
    val qualityOptions = remember {
        listOf(
            VideoQuality("auto", "Auto (Best Available)", isRecommended = true),
            VideoQuality("1080p", "1080p Full HD"),
            VideoQuality("720p", "720p HD"),
            VideoQuality("480p", "480p SD"),
            VideoQuality("360p", "360p (Smaller File)")
        )
    }
    
    var selectedQuality by remember { mutableStateOf(qualityOptions[0].label) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Download Video") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Filename input
                Text(
                    text = "Save video as:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Enter filename") }
                )
                
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                
                // Quality selection
                Text(
                    text = "Select quality:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    qualityOptions.forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (selectedQuality == quality.label),
                                    onClick = { selectedQuality = quality.label },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (selectedQuality == quality.label),
                                onClick = null
                            )
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = quality.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (quality.isRecommended) {
                                        Text(
                                            text = "Recommended",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                
                // Location info
                Text(
                    text = "Location: Downloads folder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Info message
                Text(
                    text = "Note: Actual quality depends on video source availability",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (filename.isNotBlank()) {
                        onConfirm(filename, selectedQuality)
                    }
                }
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
