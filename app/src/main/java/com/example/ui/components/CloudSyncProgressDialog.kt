package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.KurdPlayBluePrimary
import com.example.ui.theme.KurdPlaySurface

@Composable
fun CloudSyncProgressDialog(
    statusMessage: String,
    progressPercent: Int = 0,
    onDismiss: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cloud_sync_anim")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val targetProgress = (progressPercent.coerceIn(0, 100)) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "progress_anim"
    )

    Dialog(
        onDismissRequest = { /* Modal, prevent accidental dismiss */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = KurdPlaySurface,
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                Brush.horizontalGradient(
                    listOf(Color(0xFF00B4DB), Color(0xFFFFD700), Color(0xFF4CAF50))
                )
            ),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("cloud_sync_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Animated Glowing Icon Container with Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scalePulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00B4DB).copy(alpha = 0.35f),
                                    Color(0xFF00B4DB).copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(2.dp, Color(0xFF00B4DB), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud Syncing",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier
                            .size(44.dp)
                            .rotate(rotation)
                    )
                }

                // Title
                Text(
                    text = "خەریکی هاوکاتکردن و ئەپدەیتی داتابەیسە ☁️⚡",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Large Percentage Display (0% - 100%)
                Surface(
                    color = Color(0xFF1E2638),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "% ${progressPercent.coerceIn(0, 100)}",
                        color = Color(0xFFFFD700),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                // Subtitle / Description
                Text(
                    text = if (statusMessage.isNotBlank()) statusMessage else "ناردن و وەرگرتنی نوێکارییەکانی کەناڵ، وێنەی ڕیکلامەکان، یوزەر و تێپەڕەوشەکان لە سێرڤەری ئۆنلاین...",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                // Animated Progress Bar (0% to 100%)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF00B4DB),
                        trackColor = Color.White.copy(alpha = 0.15f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Online Web Server Sync 🟢",
                            color = Color(0xFF4CAF50),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${progressPercent.coerceIn(0, 100)} / 100",
                            color = Color(0xFFFFD700),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    color = Color(0xFF1E2638),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "سەرجەم وێنەکان، یوزەر، پاسۆرد و داتاکان سەیڤ دەبن",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
