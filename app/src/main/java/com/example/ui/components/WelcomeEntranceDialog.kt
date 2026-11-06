package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.KurdPlayBg
import com.example.ui.theme.KurdPlayBluePrimary
import com.example.ui.theme.KurdPlayBorder
import com.example.ui.theme.KurdPlaySurface
import com.example.ui.theme.KurdPlaySurfaceVariant

@Composable
fun WelcomeEntranceDialog(
    onSelectFreeMode: () -> Unit,
    onSelectSubscriberLogin: () -> Unit,
    onSelectAdminSection: (user: String, pass: String) -> Boolean,
    onDismiss: () -> Unit
) {
    var showAdminLoginForm by remember { mutableStateOf(false) }
    var adminUsername by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, KurdPlayBorder, RoundedCornerShape(24.dp))
                .testTag("welcome_entrance_dialog"),
            color = KurdPlayBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showAdminLoginForm) {
                    // Admin Username & Password Authentication Form
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(KurdPlayBluePrimary, Color(0xFF00B4DB))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "چوونەژوورەوەی بەڕێوەبەر 🔒",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "تکایە یوزەر و پاسۆردی بەڕێوەبەر بنووسە:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = adminUsername,
                        onValueChange = {
                            adminUsername = it
                            loginError = false
                        },
                        label = { Text("ناوی بەکارهێنەر / Username", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = KurdPlayBluePrimary) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = KurdPlaySurface,
                            unfocusedContainerColor = KurdPlaySurface,
                            focusedBorderColor = KurdPlayBluePrimary,
                            unfocusedBorderColor = KurdPlayBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_username_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = {
                            adminPassword = it
                            loginError = false
                        },
                        label = { Text("پاسۆردی بەڕێوەبەر / Password", color = Color.Gray, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = KurdPlayBluePrimary) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = KurdPlaySurface,
                            unfocusedContainerColor = KurdPlaySurface,
                            focusedBorderColor = KurdPlayBluePrimary,
                            unfocusedBorderColor = KurdPlayBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input"),
                        singleLine = true
                    )

                    if (loginError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "❌ یوزەر یان پاسۆرد هەڵەیە! (یوزەر: ako | پاسۆرد: ako@119)",
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val success = onSelectAdminSection(adminUsername, adminPassword)
                            if (!success) {
                                loginError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KurdPlayBluePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_login_submit_btn")
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("چوونەژوورەوە وەک بەڕێوەبەر 🔓", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showAdminLoginForm = false }) {
                        Text("گەڕانەوە ↩", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    // Header Logo & Title
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_kurdplay_1785925538381),
                        contentDescription = "KURD PLAY TV Logo",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "بەخێربێن بۆ KURD PLAY TV 🎬",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "تکایە یەکێک لە ڕێگاکانی چوونەژوورەوە هەڵبژێرە:",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // OPTION 1: Content Viewer Mode (Free Full Access)
                    EntranceOptionCard(
                        title = "١. تەماشاکردنی کەناڵ و فیلمەکان 🍿",
                        subtitle = "سەیرکردنی بێبەرامبەری سەرجەم کەناڵە کوردی و جیهانییەکان",
                        badgeText = "OPEN / کراوە",
                        badgeColor = Color(0xFF4CAF50),
                        icon = Icons.Default.PlayCircle,
                        iconBgColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        iconTint = Color(0xFF4CAF50),
                        onClick = onSelectFreeMode,
                        tag = "entrance_opt_free"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // OPTION 2: Admin Section
                    EntranceOptionCard(
                        title = "٢. بەشی بەڕێوەبەر (Admin Portal) 🔑",
                        subtitle = "کۆنترۆڵکردن، زیادکردنی کەناڵ و هاوسەنگکردن لەگەڵ کلود",
                        badgeText = "ADMIN",
                        badgeColor = KurdPlayBluePrimary,
                        icon = Icons.Default.AdminPanelSettings,
                        iconBgColor = KurdPlayBluePrimary.copy(alpha = 0.2f),
                        iconTint = KurdPlayBluePrimary,
                        onClick = {
                            showAdminLoginForm = true
                        },
                        tag = "entrance_opt_admin"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Close / Skip Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "داخستن و درێژەدان ✕",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EntranceOptionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, KurdPlayBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(tag),
        color = KurdPlaySurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    maxLines = 2
                )
            }
        }
    }
}
