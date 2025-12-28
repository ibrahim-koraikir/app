package com.entertainmentbrowser.presentation.webview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
            VideoQuality("auto", "Best Available", isRecommended = true),
            VideoQuality("1080p", "1080p Full HD"),
            VideoQuality("720p", "720p HD"),
            VideoQuality("480p", "480p SD"),
            VideoQuality("360p", "360p Low")
        )
    }
    
    var selectedQuality by remember { mutableStateOf(qualityOptions[0].label) }
    
    // Colors matching app theme
    val primaryRed = Color(0xFFFD1D1D)
    val darkBackground = Color(0xFF1A1F2E)
    val cardBackground = Color(0xFF252B3B)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF94A3B8)
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = darkBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(primaryRed, primaryRed.copy(alpha = 0.7f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Download Video",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Save to your device",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    }
                }
                
                // Filename input section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "FILENAME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryRed,
                        letterSpacing = 1.sp
                    )
                    
                    OutlinedTextField(
                        value = filename,
                        onValueChange = { filename = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { 
                            Text(
                                "Enter filename",
                                color = textSecondary.copy(alpha = 0.5f)
                            ) 
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryRed,
                            unfocusedBorderColor = textSecondary.copy(alpha = 0.3f),
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = primaryRed,
                            focusedContainerColor = cardBackground,
                            unfocusedContainerColor = cardBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                // Quality selection section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = primaryRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "QUALITY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryRed,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    // Quality chips in a flow layout
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // First row: Auto + 1080p + 720p
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            qualityOptions.take(3).forEach { quality ->
                                QualityChip(
                                    quality = quality,
                                    isSelected = selectedQuality == quality.label,
                                    onClick = { selectedQuality = quality.label },
                                    primaryColor = primaryRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        // Second row: 480p + 360p
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            qualityOptions.drop(3).forEach { quality ->
                                QualityChip(
                                    quality = quality,
                                    isSelected = selectedQuality == quality.label,
                                    onClick = { selectedQuality = quality.label },
                                    primaryColor = primaryRed,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Spacer to balance the row
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                
                // Location info - simple text (not clickable)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Saves to Downloads folder",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            textSecondary.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Download button
                    Button(
                        onClick = {
                            if (filename.isNotBlank()) {
                                onConfirm(filename, selectedQuality)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryRed,
                            contentColor = Color.White
                        ),
                        enabled = filename.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityChip(
    quality: VideoQuality,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color(0xFF252B3B)
    val borderColor = if (isSelected) primaryColor else Color(0xFF94A3B8).copy(alpha = 0.2f)
    val textColor = if (isSelected) primaryColor else Color.White
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = quality.label.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (quality.isRecommended) {
                    Text(
                        text = "Best",
                        fontSize = 9.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
