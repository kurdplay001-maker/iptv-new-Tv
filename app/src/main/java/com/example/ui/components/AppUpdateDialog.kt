package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KurdPlayBluePrimary
import com.example.ui.theme.KurdPlaySurface

@Composable
fun AppUpdateDialog(
    titleText: String,
    googlePlayUrl: String,
    apkUrl: String,
    aptoideUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KurdPlaySurface,
        shape = RoundedCornerShape(24.dp),
        title = null,
        confirmButton = {},
        dismissButton = {},
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon with Glow effect
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(KurdPlayBluePrimary, Color(0xFF00B4DB))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "App Update",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ئەپدەیتی نوێی KurdPlay 🚀",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = titleText.ifEmpty { "تکایە ئەبدەیتی تازەی بۆ هاتوە بەرنامەکە. یەکێک لەم سایتانە بکەرەوە و ئەپدەیتی بکە" },
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Google Play Store Button
                    if (googlePlayUrl.isNotBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(googlePlayUrl))
                                context.startActivity(intent)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF01875F)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Shop, contentDescription = "Google Play", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Google Play Store (گۆگڵ پلەی)", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Direct APK Link Button
                    if (apkUrl.isNotBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                                context.startActivity(intent)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KurdPlayBluePrimary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "APK Direct", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("دابەزاندنی ڕاستەوخۆ (Direct APK)", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Aptoide Store Button
                    if (aptoideUrl.isNotBlank()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(aptoideUrl))
                                context.startActivity(intent)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5622)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Android, contentDescription = "Aptoide", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Aptoide Store (ئەپتۆید)", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextButton(onClick = onDismiss) {
                    Text("دواتر ئەپدەیتی دەکەم / Later", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    )
}
