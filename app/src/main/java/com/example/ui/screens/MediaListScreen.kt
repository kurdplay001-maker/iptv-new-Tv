package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.PlaylistItem
import com.example.ui.theme.KurdPlayBg
import com.example.ui.theme.KurdPlayBluePrimary
import com.example.ui.theme.KurdPlayBorder
import com.example.ui.theme.KurdPlaySurface
import com.example.ui.theme.KurdPlaySurfaceVariant

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*

@Composable
fun MediaListScreen(
    title: String,
    items: List<PlaylistItem>,
    searchQuery: String,
    selectedCategory: String,
    isAdmin: Boolean = false,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onPlayItem: (PlaylistItem) -> Unit,
    onToggleFavorite: (PlaylistItem) -> Unit,
    onToggleFreeStatus: ((PlaylistItem) -> Unit)? = null,
    onSwapOrder: ((PlaylistItem, PlaylistItem) -> Unit)? = null,
    onSetCategoryFreeStatus: ((category: String, isFree: Boolean) -> Unit)? = null,
    onRenameCategory: ((oldName: String, newName: String) -> Unit)? = null,
    onDeleteCategory: ((categoryName: String) -> Unit)? = null,
    onMoveCategoryOrder: ((categoryName: String, categoriesList: List<String>, moveLeft: Boolean) -> Unit)? = null,
    isTvMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isReorderMode by remember { mutableStateOf(false) }

    // Admin category management state
    var categoryToManage by remember { mutableStateOf<String?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Collect unique categories
    val categories = listOf("All") + items.map { it.category }.filter { it.isNotBlank() }.distinct()

    // Sort items by displayOrder then filter
    val sortedItems = items.sortedWith(compareBy<PlaylistItem>({ it.displayOrder }, { it.id }))

    // Filter items by search query and category
    val filteredItems = sortedItems.filter { item ->
        val matchesSearch = searchQuery.isEmpty() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KurdPlayBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Screen Header with Title and Compact Search Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Real-time Compact Search Input Field in Header
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "گەڕان...",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF101726),
                    unfocusedContainerColor = Color(0xFF101726),
                    focusedBorderColor = KurdPlayBluePrimary,
                    unfocusedBorderColor = Color(0xFF1F293D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("media_search_input"),
                singleLine = true
            )

            if (isAdmin) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF131B2A))
                        .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(10.dp))
                        .clickable { isReorderMode = !isReorderMode },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Reorder",
                        tint = if (isReorderMode) Color(0xFF4CAF50) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Category Filter Chips Row matching Screenshot
        if (categories.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    items(categories) { category ->
                        val isSelected = category.equals(selectedCategory, ignoreCase = true)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    color = if (isSelected) KurdPlayBluePrimary else Color(0xFF131B2A)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) KurdPlayBluePrimary else Color(0xFF1F293D),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onCategorySelect(category) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFFD0D7DE)
                            )

                            if (isAdmin && category != "All") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Manage Category",
                                    tint = if (isSelected) Color.White else Color(0xFFFFC107),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            categoryToManage = category
                                            renameInputText = category
                                        }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // End Filter Button Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF131B2A))
                        .border(1.dp, Color(0xFF1F293D), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Admin Category Management Dialog
        categoryToManage?.let { cat ->
            AlertDialog(
                onDismissRequest = { categoryToManage = null },
                containerColor = KurdPlaySurface,
                title = {
                    Text(
                        text = "کۆنترۆڵکردنی کاتێگۆری: \"$cat\" 📁",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("شوێن و ناوی کاتێگۆری لە سەرجەم ئەپەکانی تر بەشێوەی ڕاستەوخۆ دەگۆڕدرێت:", color = Color.LightGray, fontSize = 11.sp)

                        // 1. Move Category Order Row
                        Text("1. گۆڕینی شوێنی کاتێگۆری (Reorder Position):", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val nonAllCategories = categories.filter { it != "All" }
                                    onMoveCategoryOrder?.invoke(cat, nonAllCategories, true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4DB)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("◀ بۆ لای چەپ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val nonAllCategories = categories.filter { it != "All" }
                                    onMoveCategoryOrder?.invoke(cat, nonAllCategories, false)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4DB)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("بۆ لای ڕاست ▶", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 2. Rename Category
                        Text("2. گۆڕینی ناوی کاتێگۆری (Rename Category):", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = renameInputText,
                            onValueChange = { renameInputText = it },
                            label = { Text("ناوی نوێ", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = KurdPlayBluePrimary,
                                unfocusedBorderColor = KurdPlayBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (renameInputText.isNotBlank() && renameInputText != cat) {
                                    onRenameCategory?.invoke(cat, renameInputText.trim())
                                    categoryToManage = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تۆمارکردن و گۆڕینی ناو ✏️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 3. Set Category Free/Paid Status
                        Text("3. فلتەری پارەدار/خۆڕایی لەم کاتێگۆرییەدا:", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSetCategoryFreeStatus?.invoke(cat, true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E44AD)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("هەمووی خۆڕایی 🆓", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onSetCategoryFreeStatus?.invoke(cat, false)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD35400)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("هەمووی VIP 🔑", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 4. Delete Category
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                showDeleteConfirmDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("سڕینەوەی کاتێگۆری بە تەواوی 🗑️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { categoryToManage = null }) {
                        Text("داخستن", color = Color.White)
                    }
                }
            )

            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    containerColor = KurdPlaySurface,
                    title = { Text("دڵنیایت لە سڕینەوە؟", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                    text = {
                        Text("ئایا دڵنیایت لە سڕینەوەی کاتێگۆری \"$cat\" لەگەڵ سەرجەم کەناڵەکانی لە داتابەیسی ئۆنلاین؟", color = Color.White, fontSize = 12.sp)
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDeleteCategory?.invoke(cat)
                                showDeleteConfirmDialog = false
                                categoryToManage = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("بەڵێ، بسڕەوە 🗑️")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmDialog = false }) {
                            Text("پاشگەزبوونەوە", color = Color.White)
                        }
                    }
                )
            }
        }

        // Empty state
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.TvOff,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "هیچ بابەتی نەدۆزرایەوە / No items found",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Modern Sleek Card Grid for Channels, Movies, and Series (Adaptive columns)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (isTvMode) 130.dp else 100.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isTvMode) 12.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (isTvMode) 14.dp else 10.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(filteredItems) { index, item ->
                    SleekIPTVCard(
                        item = item,
                        isReorderMode = isReorderMode,
                        isAdmin = isAdmin,
                        isTvMode = isTvMode,
                        onPlay = { onPlayItem(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
                        onToggleFreeStatus = { onToggleFreeStatus?.invoke(item) },
                        onMoveUp = {
                            if (index > 0) {
                                onSwapOrder?.invoke(item, filteredItems[index - 1])
                            }
                        },
                        onMoveDown = {
                            if (index < filteredItems.size - 1) {
                                onSwapOrder?.invoke(item, filteredItems[index + 1])
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SleekIPTVCard(
    item: PlaylistItem,
    isReorderMode: Boolean,
    isAdmin: Boolean = false,
    isTvMode: Boolean = false,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFreeStatus: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1.0f, label = "card_scale")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isTvMode) 195.dp else 175.dp)
            .scale(cardScale)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color(0xFFFFD700) else Color(0xFF1F293D),
                shape = RoundedCornerShape(16.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { if (!isReorderMode) onPlay() }
            .testTag("sleek_iptv_card_${item.id}"),
        color = if (isFocused) Color(0xFF16233B) else Color(0xFF0F1626)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: IP / VIP Badge (Left) and Favorite Heart (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quality HD Streaming Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(KurdPlayBluePrimary.copy(alpha = 0.25f))
                        .border(0.5.dp, KurdPlayBluePrimary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "HD",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Favorite Heart Icon
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color.Red else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Center Logo Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(75.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF182236))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (item.logoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = item.logoUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = when (item.mediaType) {
                            "MOVIE" -> Icons.Default.Movie
                            "SERIES" -> Icons.Default.Tv
                            else -> Icons.Default.LiveTv
                        },
                        contentDescription = null,
                        tint = KurdPlayBluePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Reorder controls overlay when in reorder mode
                if (isReorderMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.75f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = onMoveUp,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(KurdPlayBluePrimary, CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = onMoveDown,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(KurdPlayBluePrimary, CircleShape)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Title & Subtitle
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.category.ifEmpty { "beIN Sports" },
                    color = Color.Gray,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
