package com.entertainmentbrowser.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.entertainmentbrowser.presentation.theme.DarkBlue1
import com.entertainmentbrowser.presentation.theme.DarkBlue2
import com.entertainmentbrowser.presentation.theme.RedPrimary

/**
 * Welcome screen composable matching welcome.html design.
 * Shows hero image with gradient overlay and feature list.
 * 
 * Requirements: 1.2, 1.4, 1.5
 */
@Composable
fun WelcomeScreen(
    page: OnboardingPage,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background image with gradient overlay
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero image
            AsyncImage(
                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuDRp6g7yN8Plm5QzkHx9zRCIaeBFyWTTKTTsOFqD-YkgGPWI9y_cAA-vLEgkMOHdLb6sATZu2z9ETT1j8zfBrbLprdbFnIYGscB6DHQxm2odAnAMxWCMgOqb-gG3YE1iFtoK8rFK8NOv1SBcVc7U5DLHSOuyLFKNORf3gNcmWpysdk0EhHkvPkJiNnEImrrJCLapv98S3qfuscDccve7QOnovwzCEN94eqevGU_lcjYEVnHH4bCBBApwKlOFLLSDdXx2Tqg2jcX3vY",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay (from transparent to dark)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DarkBlue1.copy(alpha = 0.4f),
                                DarkBlue1.copy(alpha = 0.85f),
                                DarkBlue1
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Title and description
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = page.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Feature list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    page.features.forEach { feature ->
                        FeatureItem(feature = feature)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Get Started button
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedPrimary
                )
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    feature: Feature,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(RedPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getFeatureIcon(feature.icon),
                contentDescription = null,
                tint = RedPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun getFeatureIcon(icon: FeatureIcon): ImageVector {
    return when (icon) {
        FeatureIcon.LIGHTNING -> Icons.Default.Star
        FeatureIcon.DOWNLOAD -> Icons.Default.GetApp
        FeatureIcon.SEARCH -> Icons.Default.Search
        FeatureIcon.ARCHIVE -> Icons.Default.Folder
        FeatureIcon.MOBILE -> Icons.Default.PhoneAndroid
    }
}
