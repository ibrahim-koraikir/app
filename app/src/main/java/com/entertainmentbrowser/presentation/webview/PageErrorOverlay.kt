package com.entertainmentbrowser.presentation.webview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.entertainmentbrowser.R

/**
 * Full-screen error overlay shown when page fails to load.
 * Provides user-friendly error messages and retry options.
 */
@Composable
fun PageErrorOverlay(
    errorType: PageErrorType,
    onRetry: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, title, description) = getErrorContent(errorType)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Error icon in a circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = getErrorColor(errorType).copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = getErrorColor(errorType),
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Error description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Retry button
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.error_retry),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Go back button
            OutlinedButton(
                onClick = onGoBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.error_go_back),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Get icon, title, and description for each error type
 */
@Composable
private fun getErrorContent(errorType: PageErrorType): Triple<ImageVector, String, String> {
    return when (errorType) {
        PageErrorType.NO_INTERNET -> Triple(
            Icons.Default.CloudOff,
            stringResource(R.string.error_no_internet_title),
            stringResource(R.string.error_no_internet_desc)
        )
        PageErrorType.TIMEOUT -> Triple(
            Icons.Default.Timer,
            stringResource(R.string.error_timeout_title),
            stringResource(R.string.error_timeout_desc)
        )
        PageErrorType.SERVER_ERROR -> Triple(
            Icons.Default.Error,
            stringResource(R.string.error_server_title),
            stringResource(R.string.error_server_desc)
        )
        PageErrorType.SSL_ERROR -> Triple(
            Icons.Default.Lock,
            stringResource(R.string.error_ssl_title),
            stringResource(R.string.error_ssl_desc)
        )
        PageErrorType.PAGE_NOT_FOUND -> Triple(
            Icons.Default.SearchOff,
            stringResource(R.string.error_not_found_title),
            stringResource(R.string.error_not_found_desc)
        )
        PageErrorType.BLOCKED_BY_ADBLOCK -> Triple(
            Icons.Default.Warning,
            stringResource(R.string.error_blocked_title),
            stringResource(R.string.error_blocked_desc)
        )
        PageErrorType.UNKNOWN, PageErrorType.NONE -> Triple(
            Icons.Default.Warning,
            stringResource(R.string.error_unknown_title),
            stringResource(R.string.error_unknown_desc)
        )
    }
}

/**
 * Get color for each error type
 */
private fun getErrorColor(errorType: PageErrorType): Color {
    return when (errorType) {
        PageErrorType.NO_INTERNET -> Color(0xFF5C6BC0) // Indigo
        PageErrorType.TIMEOUT -> Color(0xFFFF9800) // Orange
        PageErrorType.SERVER_ERROR -> Color(0xFFF44336) // Red
        PageErrorType.SSL_ERROR -> Color(0xFFE91E63) // Pink
        PageErrorType.PAGE_NOT_FOUND -> Color(0xFF9E9E9E) // Grey
        PageErrorType.BLOCKED_BY_ADBLOCK -> Color(0xFFFFEB3B) // Yellow
        PageErrorType.UNKNOWN, PageErrorType.NONE -> Color(0xFF9E9E9E) // Grey
    }
}
