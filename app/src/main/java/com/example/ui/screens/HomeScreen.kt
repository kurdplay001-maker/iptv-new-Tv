package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.PlaylistItem
import com.example.ui.components.MainCategoryCards
import com.example.ui.theme.KurdPlayBg
import com.example.ui.theme.KurdPlayBluePrimary
import com.example.ui.theme.KurdPlayBorder
import com.example.ui.theme.KurdPlaySurface
import com.example.ui.theme.KurdPlaySurfaceVariant
import kotlinx.coroutines.delay

data class HomeBannerItem(
    val imageUrl: String,
    val title: String,
    val description: String,
    val buttonText: String,
    val targetCategoryIndex: Int
)

@Composable
fun HomeScreen(
    liveChannelsCount: Int,
    moviesCount: Int,
    seriesCount: Int,
    websitesCount: Int = 5,
    recentItems: List<PlaylistItem> = emptyList(),
    language: String = "KU",
    topBannerUrls: List<String> = emptyList(),
    bottomBannerUrls: List<String> = emptyList(),
    bottomTickerText: String = "",
    isTvMode: Boolean = false,
    onSelectCategory: (Int) -> Unit,
    onPlayItem: (PlaylistItem) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWelcomeDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Banners with dynamic fallback support
    val homeBanners = remember(topBannerUrls) {
        val validUrls = topBannerUrls.filter { it.isNotBlank() }
        if (validUrls.isNotEmpty()) {
            validUrls.mapIndexed { index, url ->
                HomeBannerItem(
                    imageUrl = url,
                    title = when (index % 3) {
                        0 -> "KURD PLAY TV – بەرزترین کوالێتی HD"
                        1 -> "نوێترین فیلم و زنجیرە نایابەکان"
                        else -> "پەخشی ڕاستەوخۆی کەناڵە ئاسمانییەکان"
                    },
                    description = when (index % 3) {
                        0 -> "بەبێ بڕانەوە و بە خێرایی بەرز بڕوانە"
                        1 -> "ژێرنووس و دۆبلاژی کوردی بە کوالێتی بەرز"
                        else -> "کەناڵە کوردی و جیهانییەکان بە دەنگی کراوە"
                    },
                    buttonText = "تەماشا بکە 🍿",
                    targetCategoryIndex = when (index % 3) {
                        0 -> 1 // Live TV
                        1 -> 2 // Movies
                        else -> 3 // Series
                    }
                )
            }
        } else {
            listOf(
                HomeBannerItem(
                    imageUrl = "https://images.unsplash.com/photo-1593784991095-a205069470b6?w=1200",
                    title = "KURD PLAY TV – بێ بەرامبەر بڕوانە",
                    description = "تەماشای نوێترین فیلم و کەناڵە ڕاستەوخۆکان بکە",
                    buttonText = "ڕاستەوخۆ 📺",
                    targetCategoryIndex = 1
                ),
                HomeBannerItem(
                    imageUrl = "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=1200",
                    title = "نوێترین فیلم و زنجیرەکان",
                    description = "کۆمەڵێک لە باشترین بەرهەمە سینەماییەکان",
                    buttonText = "فیلمەکان 🎬",
                    targetCategoryIndex = 2
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KurdPlayBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // TOP HEADER BAR (Design 3 Style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo_kurdplay_1785925538381),
                    contentDescription = "KURD PLAY TV Logo",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, KurdPlayBorder, RoundedCornerShape(12.dp))
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "KURD ",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "PLAY",
                            color = Color(0xFFFFC107),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = Color(0xFFFF3D00),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "TV",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Best Movies & TV Shows",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            // Top Quick Icons (Profile, Search, Settings)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Profile/Account Icon
                IconButton(
                    onClick = onOpenWelcomeDialog,
                    modifier = Modifier
                        .size(40.dp)
                        .background(KurdPlayBluePrimary.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, KurdPlayBluePrimary, CircleShape)
                        .testTag("home_user_profile_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User Profile",
                        tint = KurdPlayBluePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Search Icon
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .size(40.dp)
                        .background(KurdPlaySurfaceVariant, CircleShape)
                        .testTag("home_search_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Settings Icon
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .background(KurdPlaySurfaceVariant, CircleShape)
                        .testTag("home_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // LARGE PROMOTIONAL BANNER SLIDER (Design 3 Style)
        HeroBannerSlider(
            banners = homeBanners,
            onBannerClick = { categoryIndex ->
                onSelectCategory(categoryIndex)
            }
        )

        // LARGE COLORFUL CATEGORY CARDS (Design 3 Style)
        MainCategoryCards(
            liveCount = liveChannelsCount,
            moviesCount = moviesCount,
            seriesCount = seriesCount,
            websitesCount = websitesCount,
            language = language,
            isTvMode = isTvMode,
            onSelectCategory = onSelectCategory
        )

        // BOTTOM TICKER ANNOUNCEMENT (IF AVAILABLE)
        if (bottomTickerText.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, KurdPlayBluePrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                color = KurdPlayBluePrimary.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Announcement",
                        tint = KurdPlayBluePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = bottomTickerText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // FEATURED CONTENT CAROUSEL (DYNAMIC FROM DATABASE/VIEWMODEL)
        if (recentItems.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "دواین ناوەڕۆکی بەردەست ⚡",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "بینینی هەمووی",
                        color = KurdPlayBluePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSelectCategory(1) }
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(recentItems.take(12)) { item ->
                        MediaTileItem(
                            item = item,
                            onClick = { onPlayItem(item) }
                        )
                    }
                }
            }
        }

        // SPONSORED ADVERTISEMENT BANNER
        KurdishAdBanner(customUrls = bottomBannerUrls)
    }
}

@Composable
fun HeroBannerSlider(
    banners: List<HomeBannerItem>,
    onBannerClick: (Int) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-sliding timer
    LaunchedEffect(banners) {
        if (banners.size > 1) {
            while (true) {
                delay(4500L)
                currentIndex = (currentIndex + 1) % banners.size
            }
        }
    }

    val currentBanner = banners.getOrElse(currentIndex) { banners.first() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, KurdPlayBorder, RoundedCornerShape(20.dp))
            .clickable { onBannerClick(currentBanner.targetCategoryIndex) }
            .testTag("hero_banner_slider"),
        color = KurdPlaySurface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated Banner Image
            AnimatedContent(
                targetState = currentBanner,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                },
                label = "HeroBannerSlide",
                modifier = Modifier.fillMaxSize()
            ) { targetBanner ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (targetBanner.imageUrl.isNotBlank() &&
                        (targetBanner.imageUrl.startsWith("http") || targetBanner.imageUrl.startsWith("content:") || targetBanner.imageUrl.startsWith("file:") || targetBanner.imageUrl.startsWith("data:"))
                    ) {
                        AsyncImage(
                            model = targetBanner.imageUrl,
                            contentDescription = targetBanner.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.kurd_play_banner_1785677161255),
                            contentDescription = "Default Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Dark Gradient Overlay for Text Visibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                }
            }

            // Banner Controls (Left / Right Arrow buttons for manual sliding)
            if (banners.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            currentIndex = if (currentIndex == 0) banners.size - 1 else currentIndex - 1
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            currentIndex = (currentIndex + 1) % banners.size
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Banner Content Overlay (Title, Description, Button)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = currentBanner.title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = currentBanner.description,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onBannerClick(currentBanner.targetCategoryIndex) },
                        colors = ButtonDefaults.buttonColors(containerColor = KurdPlayBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currentBanner.buttonText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Dot Indicators
                    if (banners.size > 1) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            repeat(banners.size) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(if (idx == currentIndex) 16.dp else 6.dp, 6.dp)
                                        .clip(CircleShape)
                                        .background(if (idx == currentIndex) KurdPlayBluePrimary else Color.White.copy(alpha = 0.5f))
                                        .clickable { currentIndex = idx }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KurdishAdBanner(customUrls: List<String> = emptyList()) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val displayUrls = remember(customUrls) {
        val valid = customUrls.filter { it.isNotBlank() }
        if (valid.isNotEmpty()) valid else listOf(
            "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200",
            "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1200"
        )
    }

    LaunchedEffect(displayUrls) {
        if (displayUrls.size > 1) {
            while (true) {
                delay(5000L)
                currentIndex = (currentIndex + 1) % displayUrls.size
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, KurdPlayBorder, RoundedCornerShape(16.dp)),
        color = KurdPlaySurface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width } + fadeOut()
                    )
                },
                label = "AdBannerSlide",
                modifier = Modifier.fillMaxSize()
            ) { targetIndex ->
                val imgUrl = displayUrls.getOrElse(targetIndex) { "" }
                if (imgUrl.isNotBlank() && (imgUrl.startsWith("http") || imgUrl.startsWith("content:") || imgUrl.startsWith("file:") || imgUrl.startsWith("data:"))) {
                    AsyncImage(
                        model = imgUrl,
                        contentDescription = "Bottom Ad Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.kurd_play_banner_1785677161255),
                        contentDescription = "Bottom Ad Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Dot Indicators
            if (displayUrls.size > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(displayUrls.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == currentIndex) 14.dp else 5.dp, 5.dp)
                                    .clip(CircleShape)
                                    .background(if (index == currentIndex) KurdPlayBluePrimary else Color.White.copy(alpha = 0.6f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaTileItem(
    item: PlaylistItem,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(targetValue = if (isFocused) 1.06f else 1.0f, label = "tile_scale")

    Surface(
        modifier = Modifier
            .width(135.dp)
            .height(175.dp)
            .scale(cardScale)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color(0xFFFFD700) else KurdPlayBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .testTag("media_tile_${item.id}"),
        color = if (isFocused) Color(0xFF16233B) else KurdPlaySurface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (item.logoUrl.isNotEmpty()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(KurdPlaySurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.mediaType == "LIVE") Icons.Default.LiveTv else Icons.Default.Movie,
                        contentDescription = null,
                        tint = KurdPlayBluePrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Gradient Overlay & Label
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Category Badge
                Surface(
                    color = KurdPlayBluePrimary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.category.take(12),
                        color = Color.White,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                // Title
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
