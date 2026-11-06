package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.PlaylistItem
import com.example.ui.components.AccountModalDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.CloudSyncProgressDialog
import com.example.ui.components.InAppWebViewDialog
import com.example.ui.components.SeriesEpisodesDialog
import com.example.ui.components.VideoPlayerOverlay
import com.example.ui.components.WelcomeEntranceDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MediaListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WebsitesScreen
import com.example.ui.theme.KurdPlayBg
import com.example.ui.theme.KurdPlayBluePrimary
import com.example.ui.theme.KurdPlayBorder
import com.example.ui.theme.KurdPlaySurface
import com.example.ui.viewmodel.KurdPlayUiState
import com.example.ui.viewmodel.KurdPlayViewModel
import com.example.ui.viewmodel.UserRole

@Composable
fun KurdPlayApp(viewModel: KurdPlayViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val liveChannels by viewModel.liveChannels.collectAsStateWithLifecycle()
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val series by viewModel.series.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentWatched by viewModel.recentWatched.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val marketItems by viewModel.marketItems.collectAsStateWithLifecycle()
    val websiteItems by viewModel.websiteItems.collectAsStateWithLifecycle()

    var showWelcomeEntranceDialog by remember { mutableStateOf(false) }
    var selectedSeriesForEpisodes by remember { mutableStateOf<PlaylistItem?>(null) }

    // Android System Back Navigation Handling
    BackHandler(enabled = uiState.isFullScreenPlayer) {
        viewModel.toggleFullScreenPlayer()
    }

    BackHandler(enabled = !uiState.isFullScreenPlayer && uiState.activeMediaItem != null) {
        viewModel.closePlayer()
    }

    BackHandler(enabled = !uiState.isFullScreenPlayer && uiState.activeMediaItem == null && uiState.selectedTab != 0) {
        viewModel.setTab(0)
    }

    val handlePlayItem: (PlaylistItem) -> Unit = { item ->
        if (item.mediaType == "SERIES" || item.episodesList.isNotBlank() || item.category.contains("زنجیرە") || item.groupTitle.contains("زنجیرە")) {
            selectedSeriesForEpisodes = item
        } else {
            viewModel.attemptPlayMediaItem(item)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp
        val isTv = isWideScreen || uiState.isTvMode

        Box(modifier = Modifier.fillMaxSize()) {
            if (isTv) {
                // TV / TABLET / WIDESCREEN CANONICAL LAYOUT (Side NavigationRail)
                Row(modifier = Modifier.fillMaxSize()) {
                    if (!uiState.isFullScreenPlayer) {
                        NavigationRail(
                            containerColor = KurdPlaySurface,
                            contentColor = Color.White,
                            modifier = Modifier
                                .width(if (uiState.isTvMode) 96.dp else 84.dp)
                                .fillMaxHeight()
                                .testTag("tv_navigation_rail")
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Image(
                                painter = painterResource(id = R.drawable.app_logo_kurdplay_1785925538381),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, KurdPlayBorder, RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            listOf(
                                Triple(0, "سەرەکی", Icons.Default.Home),
                                Triple(1, "کەناڵەکان", Icons.Default.LiveTv),
                                Triple(2, "فیلمەکان", Icons.Default.Movie),
                                Triple(3, "زنجیرەکان", Icons.Default.Tv),
                                Triple(6, "سیتینگ", Icons.Default.Settings)
                            ).forEach { (tabIdx, label, icon) ->
                                NavigationRailItem(
                                    selected = uiState.selectedTab == tabIdx,
                                    onClick = { viewModel.setTab(tabIdx) },
                                    icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp)) },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (uiState.selectedTab == tabIdx) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = KurdPlayBluePrimary,
                                        selectedTextColor = KurdPlayBluePrimary,
                                        indicatorColor = KurdPlayBluePrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .focusable()
                                        .testTag("rail_nav_$tabIdx")
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        AppScreensContent(
                            uiState = uiState,
                            isTv = isTv,
                            liveChannels = liveChannels,
                            movies = movies,
                            series = series,
                            websiteItems = websiteItems,
                            recentWatched = recentWatched,
                            allItems = allItems,
                            accounts = accounts,
                            marketItems = marketItems,
                            handlePlayItem = handlePlayItem,
                            viewModel = viewModel,
                            onOpenWelcomeDialog = { showWelcomeEntranceDialog = true }
                        )
                    }
                }
            } else {
                // MOBILE COMPACT LAYOUT (Bottom NavigationBar)
                Scaffold(
                    containerColor = KurdPlayBg,
                    bottomBar = {
                        if (!uiState.isFullScreenPlayer) {
                            NavigationBar(
                                containerColor = KurdPlaySurface,
                                contentColor = Color.White,
                                tonalElevation = 8.dp,
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                    .testTag("bottom_navigation_bar")
                            ) {
                                NavigationBarItem(
                                    selected = uiState.selectedTab == 0,
                                    onClick = { viewModel.setTab(0) },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("سەرەکی", fontSize = 11.sp, fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = KurdPlayBluePrimary,
                                        selectedTextColor = KurdPlayBluePrimary,
                                        indicatorColor = KurdPlayBluePrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.testTag("nav_home")
                                )

                                NavigationBarItem(
                                    selected = uiState.selectedTab == 1,
                                    onClick = { viewModel.setTab(1) },
                                    icon = { Icon(Icons.Default.LiveTv, contentDescription = "Live TV") },
                                    label = { Text("کەناڵەکان", fontSize = 11.sp, fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = KurdPlayBluePrimary,
                                        selectedTextColor = KurdPlayBluePrimary,
                                        indicatorColor = KurdPlayBluePrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.testTag("nav_live_tv")
                                )

                                NavigationBarItem(
                                    selected = uiState.selectedTab == 2,
                                    onClick = { viewModel.setTab(2) },
                                    icon = { Icon(Icons.Default.Movie, contentDescription = "Movies") },
                                    label = { Text("فیلمەکان", fontSize = 11.sp, fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = KurdPlayBluePrimary,
                                        selectedTextColor = KurdPlayBluePrimary,
                                        indicatorColor = KurdPlayBluePrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.testTag("nav_movies")
                                )

                                NavigationBarItem(
                                    selected = uiState.selectedTab == 3,
                                    onClick = { viewModel.setTab(3) },
                                    icon = { Icon(Icons.Default.Tv, contentDescription = "Series") },
                                    label = { Text("زنجیرەکان", fontSize = 11.sp, fontWeight = if (uiState.selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = KurdPlayBluePrimary,
                                        selectedTextColor = KurdPlayBluePrimary,
                                        indicatorColor = KurdPlayBluePrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.testTag("nav_series")
                                )

                                NavigationBarItem(
                                    selected = uiState.selectedTab == 6,
                                    onClick = { viewModel.setTab(6) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("سیتینگ", fontSize = 11.sp, fontWeight = if (uiState.selectedTab == 6) FontWeight.Bold else FontWeight.Normal) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = KurdPlayBluePrimary,
                                        selectedTextColor = KurdPlayBluePrimary,
                                        indicatorColor = KurdPlayBluePrimary.copy(alpha = 0.2f),
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray
                                    ),
                                    modifier = Modifier.testTag("nav_settings")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AppScreensContent(
                            uiState = uiState,
                            isTv = false,
                            liveChannels = liveChannels,
                            movies = movies,
                            series = series,
                            websiteItems = websiteItems,
                            recentWatched = recentWatched,
                            allItems = allItems,
                            accounts = accounts,
                            marketItems = marketItems,
                            handlePlayItem = handlePlayItem,
                            viewModel = viewModel,
                            onOpenWelcomeDialog = { showWelcomeEntranceDialog = true }
                        )
                    }
                }
            }

            // MODAL DIALOGS
            if (uiState.showAccountDialog) {
                AccountModalDialog(
                    onDismiss = { viewModel.showAccountDialog(false) },
                    onLoginSubscriber = { user, pass ->
                        val success = viewModel.checkSubscriberLogin(user, pass)
                        if (success) {
                            viewModel.setUserRole(UserRole.SUBSCRIBER)
                            viewModel.showAccountDialog(false)
                        }
                        success
                    }
                )
            }

            selectedSeriesForEpisodes?.let { seriesItem ->
                SeriesEpisodesDialog(
                    item = seriesItem,
                    onDismiss = { selectedSeriesForEpisodes = null },
                    onSelectEpisode = { episodeItem ->
                        viewModel.attemptPlayMediaItem(episodeItem)
                        selectedSeriesForEpisodes = null
                    }
                )
            }

            if (showWelcomeEntranceDialog && !uiState.isFullScreenPlayer && uiState.activeMediaItem == null) {
                WelcomeEntranceDialog(
                    onSelectFreeMode = {
                        showWelcomeEntranceDialog = false
                        viewModel.setUserRole(UserRole.FREE)
                        viewModel.setTab(0)
                    },
                    onSelectSubscriberLogin = {
                        showWelcomeEntranceDialog = false
                        viewModel.setUserRole(UserRole.SUBSCRIBER)
                        viewModel.showAccountDialog(true)
                    },
                    onSelectAdminSection = { user, pass ->
                        val success = viewModel.verifyAndUnlockAdminWithCredentials(user, pass)
                        if (success) {
                            showWelcomeEntranceDialog = false
                            viewModel.setTab(6)
                        }
                        success
                    },
                    onDismiss = {
                        showWelcomeEntranceDialog = false
                    }
                )
            }

            if (uiState.showUpdateDialog) {
                AppUpdateDialog(
                    titleText = uiState.updateDialogTitle,
                    googlePlayUrl = uiState.updateGooglePlayUrl,
                    apkUrl = uiState.updateApkUrl,
                    aptoideUrl = uiState.updateAptoideUrl,
                    onDismiss = {
                        viewModel.dismissUpdateDialog()
                    }
                )
            }

            if (uiState.isLoading) {
                CloudSyncProgressDialog(
                    statusMessage = uiState.statusMessage,
                    progressPercent = uiState.syncProgressPercent
                )
            }

            if (uiState.activeWebsiteUrl != null) {
                InAppWebViewDialog(
                    url = uiState.activeWebsiteUrl!!,
                    title = uiState.activeWebsiteTitle ?: "Web Portal",
                    onDismiss = { viewModel.closeWebsite() }
                )
            }

            // VIDEO PLAYER OVERLAY (Supports mobile portrait & TV widescreen)
            if (uiState.activeMediaItem != null) {
                Box(
                    modifier = if (uiState.isFullScreenPlayer) {
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .zIndex(100f)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .zIndex(10f)
                    }
                ) {
                    uiState.activeMediaItem?.let { activeItem ->
                        VideoPlayerOverlay(
                            item = activeItem,
                            isPlaying = uiState.isPlaying,
                            isFullScreen = uiState.isFullScreenPlayer,
                            enablePrerollVideo = uiState.enablePrerollVideo,
                            prerollVideoUrl = uiState.prerollVideoUrl,
                            onClose = { viewModel.closePlayer() },
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onToggleFullScreen = { viewModel.toggleFullScreenPlayer() },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onPlayNextChannel = { viewModel.playNextChannel() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppScreensContent(
    uiState: KurdPlayUiState,
    isTv: Boolean,
    liveChannels: List<PlaylistItem>,
    movies: List<PlaylistItem>,
    series: List<PlaylistItem>,
    websiteItems: List<com.example.data.WebsiteItem>,
    recentWatched: List<PlaylistItem>,
    allItems: List<PlaylistItem>,
    accounts: List<com.example.data.PlaylistAccount>,
    marketItems: List<com.example.data.MarketItem>,
    handlePlayItem: (PlaylistItem) -> Unit,
    viewModel: KurdPlayViewModel,
    onOpenWelcomeDialog: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Reserve top height when video is embedded
        if (uiState.activeMediaItem != null && !uiState.isFullScreenPlayer) {
            Spacer(modifier = Modifier.height(if (isTv) 340.dp else 290.dp))
        }

        when (uiState.selectedTab) {
            0 -> HomeScreen(
                liveChannelsCount = liveChannels.size,
                moviesCount = movies.size,
                seriesCount = series.size,
                websitesCount = websiteItems.size,
                recentItems = recentWatched.ifEmpty { allItems.take(10) },
                language = uiState.language,
                topBannerUrls = uiState.topBannerUrls,
                bottomBannerUrls = uiState.bottomBannerUrls,
                bottomTickerText = uiState.bottomTickerText,
                isTvMode = isTv,
                onSelectCategory = { viewModel.setTab(it) },
                onPlayItem = handlePlayItem,
                onOpenSearch = { viewModel.setTab(1) },
                onOpenFavorites = { viewModel.setTab(4) },
                onOpenSettings = { viewModel.setTab(6) },
                onOpenWelcomeDialog = onOpenWelcomeDialog
            )

            1 -> MediaListScreen(
                title = "کەناڵە ڕاستەوخۆکان",
                items = liveChannels,
                searchQuery = uiState.searchQuery,
                selectedCategory = uiState.selectedCategoryFilter,
                isAdmin = uiState.isAdminUnlocked || uiState.userRole == UserRole.ADMIN,
                isTvMode = isTv,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onCategorySelect = { viewModel.setSelectedCategoryFilter(it) },
                onPlayItem = handlePlayItem,
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onToggleFreeStatus = { viewModel.toggleItemFreeStatus(it) },
                onSwapOrder = { i1, i2 -> viewModel.swapItemsOrder(i1, i2) },
                onSetCategoryFreeStatus = { cat, isFree -> viewModel.setCategoryFreeStatus(cat, isFree) },
                onRenameCategory = { oldCat, newCat -> viewModel.renameCategory(oldCat, newCat) },
                onDeleteCategory = { cat -> viewModel.deleteCategory(cat) },
                onMoveCategoryOrder = { cat, list, moveLeft -> viewModel.moveCategoryOrder(cat, list, moveLeft) }
            )

            2 -> MediaListScreen(
                title = "فیلمەکان",
                items = movies,
                searchQuery = uiState.searchQuery,
                selectedCategory = uiState.selectedCategoryFilter,
                isAdmin = uiState.isAdminUnlocked || uiState.userRole == UserRole.ADMIN,
                isTvMode = isTv,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onCategorySelect = { viewModel.setSelectedCategoryFilter(it) },
                onPlayItem = handlePlayItem,
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onToggleFreeStatus = { viewModel.toggleItemFreeStatus(it) },
                onSwapOrder = { i1, i2 -> viewModel.swapItemsOrder(i1, i2) },
                onSetCategoryFreeStatus = { cat, isFree -> viewModel.setCategoryFreeStatus(cat, isFree) },
                onRenameCategory = { oldCat, newCat -> viewModel.renameCategory(oldCat, newCat) },
                onDeleteCategory = { cat -> viewModel.deleteCategory(cat) },
                onMoveCategoryOrder = { cat, list, moveLeft -> viewModel.moveCategoryOrder(cat, list, moveLeft) }
            )

            3 -> MediaListScreen(
                title = "زنجیرەکان",
                items = series,
                searchQuery = uiState.searchQuery,
                selectedCategory = uiState.selectedCategoryFilter,
                isAdmin = uiState.isAdminUnlocked || uiState.userRole == UserRole.ADMIN,
                isTvMode = isTv,
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onCategorySelect = { viewModel.setSelectedCategoryFilter(it) },
                onPlayItem = handlePlayItem,
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onToggleFreeStatus = { viewModel.toggleItemFreeStatus(it) },
                onSwapOrder = { i1, i2 -> viewModel.swapItemsOrder(i1, i2) },
                onSetCategoryFreeStatus = { cat, isFree -> viewModel.setCategoryFreeStatus(cat, isFree) },
                onRenameCategory = { oldCat, newCat -> viewModel.renameCategory(oldCat, newCat) },
                onDeleteCategory = { cat -> viewModel.deleteCategory(cat) },
                onMoveCategoryOrder = { cat, list, moveLeft -> viewModel.moveCategoryOrder(cat, list, moveLeft) }
            )

            4, 5 -> WebsitesScreen(
                websiteItems = websiteItems,
                language = uiState.language,
                onOpenWebsite = { url, title ->
                    viewModel.openWebsite(url, title)
                },
                onBack = { viewModel.setTab(0) }
            )

            6 -> SettingsScreen(
                accounts = accounts,
                marketItems = marketItems,
                websiteItems = websiteItems,
                language = uiState.language,
                statusMessage = uiState.statusMessage,
                userRole = uiState.userRole,
                activeSubscriberAccount = uiState.activeSubscriberAccount,
                isTvMode = uiState.isTvMode,
                onToggleTvMode = { viewModel.toggleTvMode() },
                onSetUserRole = { viewModel.setUserRole(it) },
                onUnlockAdmin = { viewModel.verifyAndUnlockAdmin(it) },
                onOpenAccountDialog = { viewModel.showAccountDialog(true) },
                onResetDemoData = { viewModel.resetDemoData() },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onDeleteAccount = { viewModel.deleteAccount(it) },
                onAddCustomChannel = { title, url, cat, logo, type ->
                    viewModel.addCustomChannel(title, url, cat, logo, type)
                },
                onAddMarketItem = { title, price, cat, img, extraImgs, videoUrl, desc, phone ->
                    viewModel.addMarketItem(title, price, cat, img, extraImgs, videoUrl, desc, phone)
                },
                onDeleteMarketItem = { viewModel.deleteMarketItem(it) },
                onResetMarketProducts = { viewModel.resetMarketProducts() },
                onAddWebsiteItem = { title, url, logo, cat, desc ->
                    viewModel.addWebsiteItem(title, url, logo, cat, desc)
                },
                onDeleteWebsiteItem = { viewModel.deleteWebsiteItem(it) },
                onResetWebsites = { viewModel.resetWebsites() },
                supportPhone = uiState.supportPhone,
                onUpdateSupportPhone = { viewModel.updateSupportPhone(it) },
                topBannerUrls = uiState.topBannerUrls,
                bottomBannerUrls = uiState.bottomBannerUrls,
                bottomTickerText = uiState.bottomTickerText,
                onUpdateBannerSettings = { topUrls, bottomUrls, ticker -> viewModel.updateBannerSettings(topUrls, bottomUrls, ticker) },
                enablePrerollVideo = uiState.enablePrerollVideo,
                prerollVideoUrl = uiState.prerollVideoUrl,
                onUpdatePrerollSettings = { enabled, url -> viewModel.updatePrerollSettings(enabled, url) },
                onUpdateMarketCover = { url -> viewModel.updateMarketCover(url) },
                onUpdateAppUpdateSettings = { enabled, title, play, apk, aptoide -> viewModel.updateAppUpdateSettings(enabled, title, play, apk, aptoide) },
                onAddSubscriberAccount = { user, pass, plan -> viewModel.addSubscriberAccount(user, pass, plan) },
                onSaveXtream = { name, host, user, pass, plan -> viewModel.addXtreamAccount(name, host, user, pass, plan) },
                onSaveM3u = { name, url, plan -> viewModel.addM3uAccount(name, url, plan) },
                onSyncFromCloud = { viewModel.syncFromCloud() },
                onPushAllToCloud = { viewModel.pushAllToCloud() },
                onOpenWelcomeDialog = onOpenWelcomeDialog
            )
        }
    }
}
