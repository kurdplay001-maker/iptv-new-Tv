package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MarketItem
import com.example.data.PlaylistAccount
import com.example.data.WebsiteItem
import com.example.ui.components.PlanDashboardDialog
import com.example.ui.theme.KurdPlayBg
import com.example.ui.theme.KurdPlayBluePrimary
import com.example.ui.theme.KurdPlayBorder
import com.example.ui.theme.KurdPlaySurface
import com.example.ui.theme.KurdPlaySurfaceVariant
import com.example.ui.viewmodel.UserRole

@Composable
fun SettingsScreen(
    accounts: List<PlaylistAccount> = emptyList(),
    marketItems: List<MarketItem> = emptyList(),
    websiteItems: List<WebsiteItem> = emptyList(),
    language: String = "KU",
    statusMessage: String = "",
    userRole: UserRole = UserRole.FREE,
    activeSubscriberAccount: PlaylistAccount? = null,
    isTvMode: Boolean = false,
    onToggleTvMode: () -> Unit = {},
    onSetUserRole: (UserRole) -> Unit = {},
    onUnlockAdmin: (String) -> Boolean = { false },
    onOpenAccountDialog: () -> Unit = {},
    onResetDemoData: () -> Unit = {},
    onToggleLanguage: () -> Unit = {},
    onDeleteAccount: (PlaylistAccount) -> Unit = {},
    onAddSubscriberAccount: (username: String, pass: String, plan: String) -> Unit = { _, _, _ -> },
    onSaveXtream: ((name: String, host: String, user: String, pass: String, plan: String) -> Unit)? = null,
    onSaveM3u: ((name: String, url: String, plan: String) -> Unit)? = null,
    onAddCustomChannel: (title: String, url: String, category: String, logoUrl: String, mediaType: String) -> Unit = { _, _, _, _, _ -> },
    onAddMarketItem: (title: String, price: String, category: String, imageUrl: String, additionalImages: String, videoUrl: String, description: String, phone: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteMarketItem: (MarketItem) -> Unit = {},
    onResetMarketProducts: () -> Unit = {},
    onAddWebsiteItem: (title: String, url: String, logoUrl: String, category: String, description: String) -> Unit = { _, _, _, _, _ -> },
    onDeleteWebsiteItem: (WebsiteItem) -> Unit = {},
    onResetWebsites: () -> Unit = {},
    supportPhone: String = "07501191987",
    onUpdateSupportPhone: (String) -> Unit = {},
    topBannerUrls: List<String> = emptyList(),
    bottomBannerUrls: List<String> = emptyList(),
    bottomTickerText: String = "",
    onUpdateBannerSettings: (topUrls: List<String>, bottomUrls: List<String>, bottomTicker: String) -> Unit = { _, _, _ -> },
    enablePrerollVideo: Boolean = true,
    prerollVideoUrl: String = "",
    onUpdatePrerollSettings: (enabled: Boolean, url: String) -> Unit = { _, _ -> },
    onUpdateMarketCover: (url: String) -> Unit = {},
    onUpdateAppUpdateSettings: (enabled: Boolean, title: String, playUrl: String, apkUrl: String, aptoideUrl: String) -> Unit = { _, _, _, _, _ -> },
    onSyncFromCloud: () -> Unit = {},
    onPushAllToCloud: () -> Unit = {},
    onOpenWelcomeDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPlanDashboardDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

    // Admin Credentials Form state for unlocking Admin dashboard
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var adminUserInput by remember { mutableStateOf("") }
    var adminPassInput by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(KurdPlayBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (language == "KU") "ڕێکخستنەکان" else "Settings",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "KURD PLAY TV App Preferences",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = KurdPlayBluePrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KurdPlayBluePrimary)
                ) {
                    Text(
                        text = "v2.4.0 HD",
                        color = KurdPlayBluePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Free Streaming Status Section
        item {
            SettingsCard(title = if (language == "KU") "دۆخی بینین و پەخش" else "Streaming Status") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "KURD PLAY TV - هەموو پەخشەکان کراوەن 🍿",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تەواوی کەناڵ، فیلم و زنجیرەکان بێبەرامبەرن",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                    ) {
                        Text(
                            text = "بێبەرامبەر",
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Admin Dashboard Section (Manager Access)
        item {
            SettingsCard(title = if (language == "KU") "داشبۆردی بەڕێوەبەر" else "Admin Management") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(KurdPlayBluePrimary.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, KurdPlayBluePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (userRole == UserRole.ADMIN) Icons.Default.AdminPanelSettings else Icons.Default.Lock,
                                contentDescription = null,
                                tint = KurdPlayBluePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (userRole == UserRole.ADMIN) "داشبۆردی بەڕێوەبەر (چالاکە 🔓)" else "داشبۆردی بەڕێوەبەر (Admin Portal) 🔒",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (userRole == UserRole.ADMIN) "زیادکردنی کەناڵ، سێرڤەر و کلود" else "تایبەت بە بەڕێوەبەر (یوزەر: ako)",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (userRole == UserRole.ADMIN) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { showPlanDashboardDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = KurdPlayBluePrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "داشبۆرد 📊",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = { onSetUserRole(UserRole.FREE) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                adminUserInput = ""
                                adminPassInput = ""
                                loginError = false
                                showAdminLoginDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KurdPlayBluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "چوونەژوورەوە 🔑",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Language & Theme Settings
        item {
            SettingsCard(title = if (language == "KU") "زمان و ڕووکار" else "Language & Theme") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Language option
                    SettingsRowItem(
                        icon = Icons.Default.Language,
                        title = if (language == "KU") "زمانی بەرنامە" else "Language",
                        subtitle = if (language == "KU") "کوردی سۆرانی (Kurdish)" else "English",
                        action = {
                            Button(
                                onClick = onToggleLanguage,
                                colors = ButtonDefaults.buttonColors(containerColor = KurdPlaySurfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (language == "KU") "English" else "کوردی",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    )

                    HorizontalDivider(color = KurdPlayBorder)

                    // Theme option
                    SettingsRowItem(
                        icon = Icons.Default.DarkMode,
                        title = if (language == "KU") "تایبەتمەندی UI" else "Dark UI Theme",
                        subtitle = if (language == "KU") "تاریکی شاشەی پریمێم (Dark Streaming)" else "Modern Dark Premium Mode",
                        action = {
                            Surface(
                                color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                            ) {
                                Text(
                                    text = "چالاکە 🌙",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    )

                    HorizontalDivider(color = KurdPlayBorder)

                    // TV / Smartphone Mode
                    SettingsRowItem(
                        icon = Icons.Default.Tv,
                        title = if (language == "KU") "دۆخی تەلەفزیۆن (TV Mode)" else "Android TV Mode",
                        subtitle = if (language == "KU") "گۆڕین بۆ ڕووکاری کۆنترۆڵی تەلەفزیۆن" else "Optimize UI for Smart TV & Remotes",
                        action = {
                            Switch(
                                checked = isTvMode,
                                onCheckedChange = { onToggleTvMode() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = KurdPlayBluePrimary
                                )
                            )
                        }
                    )
                }
            }
        }


        // Support & Information Section
        item {
            SettingsCard(title = if (language == "KU") "زانیاری و پشتیوانی" else "Support & Info") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Contact phone
                    SettingsRowItem(
                        icon = Icons.Default.Phone,
                        title = if (language == "KU") "پەیوەندی بە پشتیوانی" else "Customer Support",
                        subtitle = "WhatsApp & تەلەفۆن: $supportPhone",
                        onClick = { showSupportDialog = true },
                        action = {
                            Surface(
                                color = KurdPlayBluePrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = supportPhone,
                                    color = KurdPlayBluePrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    )

                    HorizontalDivider(color = KurdPlayBorder)

                    // Privacy Policy
                    SettingsRowItem(
                        icon = Icons.Default.Security,
                        title = if (language == "KU") "یاسا و تایبەتمەندی" else "Privacy Policy",
                        subtitle = if (language == "KU") "پاراستنی داتا و یاساکانی بەکارهێنان" else "View terms & usage policy",
                        onClick = { showPrivacyDialog = true }
                    )

                    HorizontalDivider(color = KurdPlayBorder)

                    // About App
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = if (language == "KU") "دەربارەی KURD PLAY TV" else "About KURD PLAY TV",
                        subtitle = "KURD PLAY TV - Next Gen Kurdish Streaming App",
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("داخستن", color = KurdPlayBluePrimary)
                }
            },
            title = {
                Text("یاساکانی بەکارهێنانی KURD PLAY TV", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "ئەپی KURD PLAY TV تایبەتە بە پەخشکردنی کەناڵە ئاسمانییەکان، فیلم و زنجیرەکان. سەرجەم داتاکانت بە پارێزراوی لە مۆبایلەکەتدا دەمێننەوە و هیچ زانیارییەکی کەسی بڵاوناکرێتەوە.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            },
            containerColor = KurdPlaySurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("باشە", color = KurdPlayBluePrimary)
                }
            },
            title = {
                Text("دەربارەی KURD PLAY TV", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "KURD PLAY TV v2.4.0 (Design 3 Edition)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "باشترین و بەهێزترین ئەپڵیکەیشنی کوردی بۆ تەماشاکردنی ڕاستەوخۆی کەناڵەکان، نوێترین فیلم و زنجیرەکان بە کوالێتی بەرز بەبێ بڕانەوە.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "© 2026 KURD PLAY TV. All Rights Reserved.",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            },
            containerColor = KurdPlaySurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Plan Dashboard Dialog
    if (showPlanDashboardDialog) {
        PlanDashboardDialog(
            accounts = accounts,
            topBannerUrls = topBannerUrls,
            bottomBannerUrls = bottomBannerUrls,
            bottomTickerText = bottomTickerText,
            supportPhone = supportPhone,
            onDismiss = { showPlanDashboardDialog = false },
            onAddSubscriberAccount = onAddSubscriberAccount,
            onDeleteAccount = onDeleteAccount,
            onSaveXtream = onSaveXtream,
            onAddCustomChannel = onAddCustomChannel,
            onAddMarketItem = onAddMarketItem,
            onAddWebsiteItem = onAddWebsiteItem,
            onUpdateBannerSettings = onUpdateBannerSettings,
            onUpdateSupportPhone = onUpdateSupportPhone,
            onUpdateMarketCover = onUpdateMarketCover,
            onSyncCloud = onSyncFromCloud,
            onPushAllToCloud = onPushAllToCloud,
            onResetDemoData = onResetDemoData
        )
    }

    // Admin Login Modal Dialog
    if (showAdminLoginDialog) {
        AlertDialog(
            onDismissRequest = { showAdminLoginDialog = false },
            containerColor = KurdPlaySurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "چوونەژوورەوەی بەڕێوەبەر 🔑",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "تکایە یوزەرنەیم و پاسۆردی بەڕێوەبەر داخڵ بکە بۆ دەستکاریکردنی پلانەکان و کەناڵەکان:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = adminUserInput,
                        onValueChange = {
                            adminUserInput = it
                            loginError = false
                        },
                        label = { Text("یوزەرنەیم (Username)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = KurdPlayBluePrimary,
                            unfocusedBorderColor = KurdPlayBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = adminPassInput,
                        onValueChange = {
                            adminPassInput = it
                            loginError = false
                        },
                        label = { Text("تێپەڕەوشە (Password)", fontSize = 11.sp) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = KurdPlayBluePrimary,
                            unfocusedBorderColor = KurdPlayBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (loginError) {
                        Text(
                            text = "❌ یوزەر یان پاسۆرد هەڵەیە! (یوزەر: ako | پاسۆرد: ako@119)",
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isAkoValid = adminUserInput.trim() == "ako" && adminPassInput.trim() == "ako@119"
                        val isPassValid = onUnlockAdmin(adminPassInput.trim())
                        if (isAkoValid || isPassValid) {
                            onSetUserRole(UserRole.ADMIN)
                            showAdminLoginDialog = false
                            showPlanDashboardDialog = true
                        } else {
                            loginError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KurdPlayBluePrimary)
                ) {
                    Text("چوونەژوورەوە 🔓", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminLoginDialog = false }) {
                    Text("پاشگەزبوونەوە", color = Color.Gray)
                }
            }
        )
    }

    // Support Contact Dialog (WhatsApp / Call)
    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            containerColor = KurdPlaySurface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = KurdPlayBluePrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "پەیوەندی بە پشتیوانی KURD PLAY TV",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ژمارەی پەیوەندی: $supportPhone",
                        color = Color(0xFFFFD700),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "دەتوانیت ڕاستەوخۆ پەیوەندی تەلەفۆنی بکەیت یان لە ڕێگەی واتسئاپەوە نامە بنێریت:",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val phoneClean = supportPhone.removePrefix("0").trim()
                        val waUrl = "https://api.whatsapp.com/send?phone=964$phoneClean"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                        context.startActivity(intent)
                        showSupportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp (واتسئاپ)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$supportPhone"))
                        context.startActivity(intent)
                        showSupportDialog = false
                    }
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("پەیوەندی تەلەفۆنی")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, KurdPlayBorder, RoundedCornerShape(16.dp)),
        color = KurdPlaySurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = KurdPlayBluePrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(KurdPlaySurfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        if (action != null) {
            action()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
