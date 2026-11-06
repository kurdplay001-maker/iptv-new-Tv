package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.PlaylistAccount
import com.example.ui.theme.KurdPlayBg
import com.example.ui.theme.KurdPlayBluePrimary
import com.example.ui.theme.KurdPlayBorder
import com.example.ui.theme.KurdPlaySurface
import com.example.ui.theme.KurdPlaySurfaceVariant

@Composable
fun PlanDashboardDialog(
    accounts: List<PlaylistAccount> = emptyList(),
    topBannerUrls: List<String> = emptyList(),
    bottomBannerUrls: List<String> = emptyList(),
    bottomTickerText: String = "",
    supportPhone: String = "07501191987",
    onDismiss: () -> Unit,
    onAddSubscriberAccount: (username: String, pass: String, plan: String) -> Unit = { _, _, _ -> },
    onDeleteAccount: (PlaylistAccount) -> Unit = {},
    onSaveXtream: ((name: String, host: String, user: String, pass: String, plan: String) -> Unit)? = null,
    onSaveM3u: ((name: String, url: String, plan: String) -> Unit)? = null,
    onAddCustomChannel: (title: String, url: String, category: String, logoUrl: String, mediaType: String) -> Unit = { _, _, _, _, _ -> },
    onAddMarketItem: (title: String, price: String, category: String, imageUrl: String, additionalImages: String, videoUrl: String, description: String, phone: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onAddWebsiteItem: (title: String, url: String, logoUrl: String, category: String, description: String) -> Unit = { _, _, _, _, _ -> },
    onUpdateBannerSettings: (topUrls: List<String>, bottomUrls: List<String>, bottomTicker: String) -> Unit = { _, _, _ -> },
    onUpdateSupportPhone: (String) -> Unit = {},
    onUpdateMarketCover: (url: String) -> Unit = {},
    onSyncCloud: () -> Unit = {},
    onPushAllToCloud: () -> Unit = {},
    onResetDemoData: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Form states for Custom Channel / Movie / Series
    var chTitle by remember { mutableStateOf("") }
    var chUrl by remember { mutableStateOf("") }
    var chCategory by remember { mutableStateOf("کەناڵە کوردییەکان") }
    var chLogo by remember { mutableStateOf("") }
    var chType by remember { mutableStateOf("LIVE") } // LIVE, MOVIE, SERIES

    // Form states for M3U and Xtream IPTV
    var m3uName by remember { mutableStateOf("KurdPlay Online M3U") }
    var m3uUrlInput by remember { mutableStateOf("") }
    var xtreamName by remember { mutableStateOf("Xtream IPTV Server") }
    var xtreamHost by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }

    // Form states for Announcements & Banners
    var tickerTextState by remember { mutableStateOf(bottomTickerText) }
    var bannerTopUrl by remember { mutableStateOf(topBannerUrls.firstOrNull().orEmpty()) }
    var supportPhoneState by remember { mutableStateOf(supportPhone) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.93f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, Color(0xFF24324D), RoundedCornerShape(24.dp))
                .testTag("admin_dashboard_dialog"),
            color = Color(0xFF0C121D)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // TOP HEADER: Modern Admin Console Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00E5FF).copy(alpha = 0.25f), KurdPlayBluePrimary.copy(alpha = 0.25f))
                                    ),
                                    CircleShape
                                )
                                .border(1.5.dp, Color(0xFF00E5FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "داشبۆردی بەڕێوەبەر 👑",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Surface(
                                    color = Color(0xFF00E676).copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00E676))
                                ) {
                                    Text(
                                        text = "Online 🟢",
                                        color = Color(0xFF00E676),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "کۆنترۆڵی پەخش، کەناڵ و کلود (Admin: ako)",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Header Fast Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onPushAllToCloud()
                                Toast.makeText(context, "سەرجەم گۆڕانکارییەکان بۆ کلود بڵاوکرایەوە ☁️🚀", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("admin_push_cloud_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "بڵاوکردنەوە لە کلود ☁️",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1B2436), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // MODERN SEGMENTED TABS (4 Intuitive Sections)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF131C2B))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AdminTabItem(
                        icon = Icons.Default.LiveTv,
                        title = "کەناڵ و فیلم",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    AdminTabItem(
                        icon = Icons.Default.Dns,
                        title = "سێرڤەری IPTV",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    AdminTabItem(
                        icon = Icons.Default.Campaign,
                        title = "بانەر & ئاگاداری",
                        isSelected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                    AdminTabItem(
                        icon = Icons.Default.CloudSync,
                        title = "کلود & کۆنترۆڵ",
                        isSelected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        modifier = Modifier.weight(1f)
                    )
                }

                // TAB CONTENT BODY
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> {
                            // TAB 0: Add Channel / Movie / Series with Live Preview
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Content Type Picker
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFF101826),
                                        shape = RoundedCornerShape(14.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2A3E))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                "LIVE" to ("کەناڵی ڕاستەوخۆ 📺" to Icons.Default.LiveTv),
                                                "MOVIE" to ("فیلم 🎬" to Icons.Default.Movie),
                                                "SERIES" to ("زنجیرە 🍿" to Icons.Default.Tv)
                                            ).forEach { (typeKey, pair) ->
                                                val (typeLabel, typeIcon) = pair
                                                val isCurrent = chType == typeKey
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { chType = typeKey },
                                                    color = if (isCurrent) KurdPlayBluePrimary else Color.Transparent
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxSize(),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = typeIcon,
                                                            contentDescription = null,
                                                            tint = if (isCurrent) Color.White else Color.Gray,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = typeLabel,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isCurrent) Color.White else Color.LightGray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Input 1: Title
                                item {
                                    OutlinedTextField(
                                        value = chTitle,
                                        onValueChange = { chTitle = it },
                                        label = { Text("ناوی کەناڵ یان فیلم (Title)") },
                                        placeholder = { Text("بۆ نموونە: Rudaw TV HD یان Batman Begins") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = adminTextFieldColors()
                                    )
                                }

                                // Input 2: Stream URL
                                item {
                                    OutlinedTextField(
                                        value = chUrl,
                                        onValueChange = { chUrl = it },
                                        label = { Text("بەستەری پەخش / ستریم (.m3u8, .mp4, TS Stream)") },
                                        placeholder = { Text("https://example.com/live/stream.m3u8") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = adminTextFieldColors()
                                    )
                                }

                                // Input 3: Category with 1-Tap Chips
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = chCategory,
                                            onValueChange = { chCategory = it },
                                            label = { Text("کاتێگۆری (Category)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = adminTextFieldColors()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("کەناڵە کوردییەکان", "وەرزش", "منداڵان", "بەڵگەنامەیی", "فیلمە بیانییەکان").forEach { quickCat ->
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { chCategory = quickCat },
                                                    color = if (chCategory == quickCat) KurdPlayBluePrimary.copy(alpha = 0.25f) else Color(0xFF162133),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        0.5.dp,
                                                        if (chCategory == quickCat) KurdPlayBluePrimary else Color(0xFF22314A)
                                                    )
                                                ) {
                                                    Text(
                                                        text = quickCat,
                                                        color = if (chCategory == quickCat) KurdPlayBluePrimary else Color.LightGray,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Input 4: Logo URL
                                item {
                                    OutlinedTextField(
                                        value = chLogo,
                                        onValueChange = { chLogo = it },
                                        label = { Text("بەستەری وێنە یان لۆگۆ (Logo URL - ئیختیاری)") },
                                        placeholder = { Text("https://example.com/logo.png") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = adminTextFieldColors()
                                    )
                                }

                                // LIVE PREVIEW CARD (Displays exactly how channel looks in app)
                                item {
                                    AnimatedVisibility(
                                        visible = chTitle.isNotBlank() || chLogo.isNotBlank(),
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                                            color = Color(0xFF101928)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(Color(0xFF19253B)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (chLogo.isNotBlank()) {
                                                        AsyncImage(
                                                            model = chLogo,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Fit,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = if (chType == "LIVE") Icons.Default.LiveTv else Icons.Default.Movie,
                                                            contentDescription = null,
                                                            tint = KurdPlayBluePrimary,
                                                            modifier = Modifier.size(28.dp)
                                                        )
                                                    }
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = chTitle.ifBlank { "ناوی کەناڵ" },
                                                            color = Color.White,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = KurdPlayBluePrimary.copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "HD",
                                                                color = KurdPlayBluePrimary,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "کاتێگۆری: $chCategory | جۆر: $chType",
                                                        color = Color.Gray,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Actions: Quick Test Sample + Add Button
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Quick Sample Filler Button
                                        OutlinedButton(
                                            onClick = {
                                                chTitle = "Kurdistan 24 HD"
                                                chUrl = "https://kurd-hls.cdn.mangomolo.com/k24/smil:k24.smil/playlist.m3u8"
                                                chCategory = "کەناڵە کوردییەکان"
                                                chLogo = "https://images.unsplash.com/photo-1593784991095-a205069470b6?w=200"
                                                chType = "LIVE"
                                                Toast.makeText(context, "نموونەی تێست پڕکرایەوە ⚡", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f))
                                        ) {
                                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تێست ⚡", fontSize = 11.sp)
                                        }

                                        // Primary Add Button
                                        Button(
                                            onClick = {
                                                if (chTitle.isBlank() || chUrl.isBlank()) {
                                                    Toast.makeText(context, "تکایە ناوی کەناڵ و بەستەری پەخش بنووسە!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    onAddCustomChannel(
                                                        chTitle.trim(),
                                                        chUrl.trim(),
                                                        chCategory.trim(),
                                                        chLogo.trim(),
                                                        chType
                                                    )
                                                    Toast.makeText(context, "کەناڵی \"$chTitle\" بە سەرکەوتوویی زیادکرا! 📺✨", Toast.LENGTH_SHORT).show()
                                                    chTitle = ""
                                                    chUrl = ""
                                                    chLogo = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = KurdPlayBluePrimary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("زیادکردنی کەناڵ بۆ لیستەکان ➕", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // TAB 1: IPTV Xtream Codes & M3U Sources
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Xtream Codes Server Section
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, Color(0xFF1E2B42), RoundedCornerShape(16.dp)),
                                        color = Color(0xFF101826)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Dns, contentDescription = null, tint = Color(0xFF00E5FF))
                                                Text(
                                                    text = "سێرڤەری Xtream Codes IPTV",
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            OutlinedTextField(
                                                value = xtreamName,
                                                onValueChange = { xtreamName = it },
                                                label = { Text("ناوی سێرڤەر (Server Label)") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = adminTextFieldColors()
                                            )

                                            OutlinedTextField(
                                                value = xtreamHost,
                                                onValueChange = { xtreamHost = it },
                                                label = { Text("ناونیشانی سێرڤەر (Host: http://server.com:8080)") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = adminTextFieldColors()
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = xtreamUser,
                                                    onValueChange = { xtreamUser = it },
                                                    label = { Text("یوزەرنەیم (User)") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    colors = adminTextFieldColors()
                                                )

                                                OutlinedTextField(
                                                    value = xtreamPass,
                                                    onValueChange = { xtreamPass = it },
                                                    label = { Text("پاسۆرد (Pass)") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f),
                                                    colors = adminTextFieldColors()
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    if (xtreamHost.isBlank() || xtreamUser.isBlank() || xtreamPass.isBlank()) {
                                                        Toast.makeText(context, "تکایە هەموو زانیارییەکانی Xtream پڕبکەرەوە", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        onSaveXtream?.invoke(xtreamName.trim(), xtreamHost.trim(), xtreamUser.trim(), xtreamPass.trim(), "Xtream Source")
                                                        Toast.makeText(context, "سێرڤەری Xtream بە سەرکەوتوویی بەستراوە 🔌✅", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("پەیوەستکردنی سێرڤەری Xtream 🔌", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // M3U Playlist Section
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, Color(0xFF1E2B42), RoundedCornerShape(16.dp)),
                                        color = Color(0xFF101826)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = Color(0xFFFFD700))
                                                Text(
                                                    text = "هاوردەکردنی لیستی M3U ئۆنلاین",
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            OutlinedTextField(
                                                value = m3uName,
                                                onValueChange = { m3uName = it },
                                                label = { Text("ناوی لیست (Playlist Name)") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = adminTextFieldColors()
                                            )

                                            OutlinedTextField(
                                                value = m3uUrlInput,
                                                onValueChange = { m3uUrlInput = it },
                                                label = { Text("بەستەری ئۆنلاینی M3U URL (.m3u یان .m3u8)") },
                                                placeholder = { Text("http://provider.com/playlist.m3u") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = adminTextFieldColors()
                                            )

                                            Button(
                                                onClick = {
                                                    if (m3uUrlInput.isBlank()) {
                                                        Toast.makeText(context, "تکایە بەستەری M3U بنووسە", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        onSaveM3u?.invoke(m3uName.trim(), m3uUrlInput.trim(), "Online M3U")
                                                        Toast.makeText(context, "لیستی M3U بە سەرکەوتوویی زیادکرا 📂✅", Toast.LENGTH_SHORT).show()
                                                        m3uUrlInput = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("هاوردەکردنی لیستی M3U 📂", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // TAB 2: Announcements, Ticker & Banners with Live Previews
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Bottom Ticker News Input
                                item {
                                    OutlinedTextField(
                                        value = tickerTextState,
                                        onValueChange = { tickerTextState = it },
                                        label = { Text("تێکستی جوڵاوی شاشەی سەرەکی (Bottom Ticker Text)") },
                                        placeholder = { Text("بۆ نموونە: بەخێربێن بۆ KURD PLAY TV - پەخشی ڕاستەوخۆ بەرزترین کوالێتی") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        colors = adminTextFieldColors()
                                    )
                                }

                                // Live Ticker Preview
                                item {
                                    AnimatedVisibility(visible = tickerTextState.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "پێشبینینی تێکست: $tickerTextState",
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Top Banner URL Input
                                item {
                                    OutlinedTextField(
                                        value = bannerTopUrl,
                                        onValueChange = { bannerTopUrl = it },
                                        label = { Text("بەستەری وێنەی بانەری سەرەکی (Top Banner Image URL)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = adminTextFieldColors()
                                    )
                                }

                                // Live Banner Image Preview
                                item {
                                    AnimatedVisibility(visible = bannerTopUrl.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, KurdPlayBorder, RoundedCornerShape(12.dp)),
                                            color = Color(0xFF141E30)
                                        ) {
                                            AsyncImage(
                                                model = bannerTopUrl,
                                                contentDescription = "Banner Preview",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }

                                // Support Hotline
                                item {
                                    OutlinedTextField(
                                        value = supportPhoneState,
                                        onValueChange = { supportPhoneState = it },
                                        label = { Text("ژمارەی مۆبایل / WhatsApp بۆ پەیوەندی بینەران") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = adminTextFieldColors()
                                    )
                                }

                                // Save Changes Button
                                item {
                                    Button(
                                        onClick = {
                                            val topList = if (bannerTopUrl.isNotBlank()) listOf(bannerTopUrl.trim()) else emptyList()
                                            onUpdateBannerSettings(topList, emptyList(), tickerTextState.trim())
                                            onUpdateSupportPhone(supportPhoneState.trim())
                                            Toast.makeText(context, "ڕاگەیاندن و بانەر بە سەرکەوتوویی پاشەکەوتکران 💾✅", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = KurdPlayBluePrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("پاشەکەوتکردنی ڕاگەیاندن و بانەر 💾", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        3 -> {
                            // TAB 3: System Overview & Cloud Controls
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(1.dp, Color(0xFF1E2B42), RoundedCornerShape(16.dp)),
                                        color = Color(0xFF101826)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF00E676))
                                                Text(
                                                    text = "بەڕێوەبردنی خێرای کلود (Cloud Sync & Backup)",
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "کاتێک گۆڕانکاری لە کەناڵ، فیلم یان بانەر دەکەیت، دوگمەی بڵاوکردنەوە دابگرە بۆ ئەوەی دەستبەجێ لەسەر هەموو مۆبایل و تەلەفزیۆنی بینەران نوێبێتەوە.",
                                                color = Color.LightGray,
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp
                                            )

                                            // Push to Cloud
                                            Button(
                                                onClick = {
                                                    onPushAllToCloud()
                                                    Toast.makeText(context, "سەرجەم داتاکان ڕەوانەی داتابەیسی کلود کران ☁️🚀", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("بڵاوکردنەوەی گشت داتاکان لە کلود ☁️🚀", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }

                                            // Fetch from Cloud
                                            OutlinedButton(
                                                onClick = {
                                                    onSyncCloud()
                                                    Toast.makeText(context, "خەریکی هاوردەکردنی دوایین داتاکانە لە کلود... 📥", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.CloudDownload, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("هاوردەکردنی داتاکان لە کلودەوە 📥", fontWeight = FontWeight.Bold)
                                            }

                                            HorizontalDivider(color = Color(0xFF1E2B42))

                                            // Reset to Defaults
                                            Button(
                                                onClick = {
                                                    onResetDemoData()
                                                    Toast.makeText(context, "کەناڵە سەرەکییەکان گەڕێنرانەوە بۆ دۆخی بنەڕەتی 🔄", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("گەڕاندنەوەی کەناڵە بنەڕەتییەکان (Reset Default) 🔄", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTabItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .focusable(),
        color = if (isSelected) KurdPlayBluePrimary else Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color.LightGray
            )
        }
    }
}

@Composable
private fun adminTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF00E5FF),
    unfocusedBorderColor = Color(0xFF1E2B42),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = Color(0xFF0E1522),
    unfocusedContainerColor = Color(0xFF0E1522),
    focusedLabelColor = Color(0xFF00E5FF),
    unfocusedLabelColor = Color.Gray
)
