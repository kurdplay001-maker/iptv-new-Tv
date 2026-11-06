package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CloudSyncEngine
import com.example.data.KurdPlayDatabase
import com.example.data.KurdPlayRepository
import com.example.data.MarketItem
import com.example.data.PlaylistAccount
import com.example.data.PlaylistItem
import com.example.data.WebsiteItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class UserRole {
    FREE,       // بەخۆڕایی
    SUBSCRIBER, // ئیشتراکدار (پارە)
    ADMIN       // بەڕێوەبەر
}

data class KurdPlayUiState(
    val selectedTab: Int = 0, // 0: Home, 1: Live TV, 2: Movies, 3: Series, 4: Market, 5: Websites, 6: Settings
    val activeMediaItem: PlaylistItem? = null,
    val activeWebsiteUrl: String? = null,
    val activeWebsiteTitle: String? = null,
    val isPlaying: Boolean = true,
    val isFullScreenPlayer: Boolean = false,
    val searchQuery: String = "",
    val selectedCategoryFilter: String = "All",
    val showAccountDialog: Boolean = false,
    val isAdminUnlocked: Boolean = false,
    val userRole: UserRole = UserRole.FREE,
    val hasSelectedInitialMode: Boolean = false,
    val isTvMode: Boolean = false,
    val restrictedItemAttempted: PlaylistItem? = null,
    val activeSubscriberAccount: PlaylistAccount? = null,
    val language: String = "KU", // "KU" or "EN"
    val isLoading: Boolean = false,
    val syncProgressPercent: Int = 0,
    val statusMessage: String = "",
    // Banner Ads & Ticker
    val supportPhone: String = "07501191987",
    val topBannerUrls: List<String> = listOf(
        "https://images.unsplash.com/photo-1593784991095-a205069470b6?w=1200",
        "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=1200"
    ),
    val bottomBannerUrls: List<String> = listOf(
        "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200",
        "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1200"
    ),
    val bottomTickerText: String = "ڕیکلامی تۆ بە هەمان شێوە لێرە بێت - پەیوەندی بکە بە: 07501191987 یان 07771543431",
    // Market Cover
    val marketCoverUrl: String = "https://images.unsplash.com/photo-1593784991095-a205069470b6?w=1200",
    // App Updates
    val isAppUpdateAvailable: Boolean = true,
    val showUpdateDialog: Boolean = false,
    val updateDialogTitle: String = "تکایە ئەپدەیتی تازەی بۆ هاتوە بەرنامەکە. یەکێک لەم سایتانە بکەرەوە و ئەپدەیتی بکە",
    val updateGooglePlayUrl: String = "https://play.google.com/store/apps/details?id=com.kurdplay.app",
    val updateApkUrl: String = "https://kurdplay.com/download.apk",
    val updateAptoideUrl: String = "https://kurdplay.aptoide.com",
    // Pre-Roll Intro Video Config (Admin Managed)
    val enablePrerollVideo: Boolean = false,
    val prerollVideoUrl: String = ""
)

class KurdPlayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KurdPlayRepository
    private val _uiState = MutableStateFlow(KurdPlayUiState())
    val uiState: StateFlow<KurdPlayUiState> = _uiState.asStateFlow()

    val allItems: StateFlow<List<PlaylistItem>>
    val liveChannels: StateFlow<List<PlaylistItem>>
    val movies: StateFlow<List<PlaylistItem>>
    val series: StateFlow<List<PlaylistItem>>
    val favorites: StateFlow<List<PlaylistItem>>
    val recentWatched: StateFlow<List<PlaylistItem>>
    val accounts: StateFlow<List<PlaylistAccount>>
    val marketItems: StateFlow<List<MarketItem>>
    val websiteItems: StateFlow<List<WebsiteItem>>

    private val prefs = application.getSharedPreferences("kurdplay_prefs", Context.MODE_PRIVATE)

    init {
        val dao = KurdPlayDatabase.getDatabase(application).kurdPlayDao()
        repository = KurdPlayRepository(dao)

        allItems = repository.allItems.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        liveChannels = repository.liveChannels.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        movies = repository.movies.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        series = repository.series.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        favorites = repository.favorites.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        recentWatched = repository.recentWatched.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        accounts = repository.accounts.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        marketItems = repository.marketItems.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        websiteItems = repository.websiteItems.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        // Restore Persistent Tab, TV Mode, and Saved Login State
        val savedTab = prefs.getInt("saved_selected_tab", 0)
        val savedRoleStr = prefs.getString("saved_user_role", "FREE") ?: "FREE"
        val savedAdminUnlocked = prefs.getBoolean("saved_is_admin_unlocked", false)
        val savedSubUser = prefs.getString("saved_subscriber_username", "") ?: ""
        val savedSubPass = prefs.getString("saved_subscriber_password", "") ?: ""

        // TV hardware detection
        val uiModeManager = application.getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager
        val isTvHardware = uiModeManager?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION ||
                application.packageManager.hasSystemFeature("android.hardware.type.television") ||
                application.packageManager.hasSystemFeature("android.software.leanback")
        val restoredTvMode = prefs.getBoolean("saved_is_tv_mode", isTvHardware)

        val restoredRole = when (savedRoleStr) {
            "ADMIN" -> UserRole.ADMIN
            "SUBSCRIBER" -> UserRole.SUBSCRIBER
            else -> UserRole.FREE
        }

        val restoredAccount = if (restoredRole == UserRole.SUBSCRIBER && savedSubUser.isNotEmpty()) {
            PlaylistAccount(
                name = savedSubUser,
                username = savedSubUser,
                password = savedSubPass,
                type = "SUBSCRIBER"
            )
        } else null

        _uiState.value = _uiState.value.copy(
            selectedTab = savedTab,
            userRole = restoredRole,
            isAdminUnlocked = savedAdminUnlocked || restoredRole == UserRole.ADMIN,
            activeSubscriberAccount = restoredAccount,
            isTvMode = restoredTvMode
        )

        // Restore saved Banners and Advertisement Config from local SharedPreferences
        restoreConfigFromLocalPrefs()

        // Sync from cloud on app startup
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()

            val isFirstSyncDone = prefs.getBoolean("is_first_cloud_sync_completed", false)
            val hasLocalData = repository.allItems.first().isNotEmpty()

            // ONLY show the full loading/sync overlay dialog on fresh install / first launch
            val isFreshInstall = !isFirstSyncDone || !hasLocalData

            if (isFreshInstall) {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    syncProgressPercent = 5,
                    statusMessage = "داگرتنی ئۆتۆماتیکی داتابەیس، یوزەر، پاسۆرد و وێنە ڕیکلامییەکان... ⏳"
                )
            }

            runCatching {
                val count = repository.syncFromCloud { progress ->
                    if (isFreshInstall) {
                        _uiState.value = _uiState.value.copy(
                            syncProgressPercent = progress,
                            statusMessage = "داگرتنی ئۆتۆماتیکی داتابەیس، یوزەر، پاسۆرد و ڕیکلامەکان ($progress%)... ⚡"
                        )
                    }
                }
                fetchAndApplyCloudConfig()

                if (isFreshInstall) {
                    _uiState.value = _uiState.value.copy(
                        syncProgressPercent = 100,
                        statusMessage = "سەرکەوتوو بوو! $count کەناڵ، وێنە و ئەکاونت هاوکات کران 🟢"
                    )
                    kotlinx.coroutines.delay(600L)
                }

                prefs.edit().putBoolean("is_first_cloud_sync_completed", true).apply()
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                syncProgressPercent = 0
            )

            // Silent Realtime Sync Loop in background (Every 30s) without showing popup
            while (true) {
                kotlinx.coroutines.delay(30000L)
                runCatching {
                    repository.syncFromCloud()
                    fetchAndApplyCloudConfig()
                }
            }
        }
    }

    fun setTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tabIndex,
            searchQuery = "",
            selectedCategoryFilter = "All"
        )
        prefs.edit().putInt("saved_selected_tab", tabIndex).apply()
    }

    fun attemptPlayMediaItem(item: PlaylistItem) {
        // Direct unrestricted playback for all content
        playMediaItem(item)
    }

    fun dismissRestrictedDialog() {
        _uiState.value = _uiState.value.copy(restrictedItemAttempted = null)
    }

    fun setUserRole(role: UserRole) {
        val adminUnlocked = if (role == UserRole.ADMIN) true else _uiState.value.isAdminUnlocked
        _uiState.value = _uiState.value.copy(
            userRole = role,
            isAdminUnlocked = adminUnlocked,
            statusMessage = when (role) {
                UserRole.FREE -> "بەشی خۆڕایی چالاککرا 🆓"
                UserRole.SUBSCRIBER -> "بەشی ئیشتراکدار چالاککرا 🟢"
                UserRole.ADMIN -> "بەشی بەڕێوەبەر چالاککرا 🔓"
            }
        )
        prefs.edit()
            .putString("saved_user_role", role.name)
            .putBoolean("saved_is_admin_unlocked", adminUnlocked)
            .apply()
    }

    fun setActiveSubscriber(account: PlaylistAccount) {
        _uiState.value = _uiState.value.copy(
            activeSubscriberAccount = account,
            userRole = UserRole.SUBSCRIBER,
            statusMessage = "ئەکاونتی ئیشتراکدار چالاکبوو: ${account.name} 🔑"
        )
        prefs.edit()
            .putString("saved_user_role", "SUBSCRIBER")
            .putString("saved_subscriber_username", account.username)
            .putString("saved_subscriber_password", account.password)
            .apply()
    }

    fun logout() {
        prefs.edit().clear().apply()
        _uiState.value = _uiState.value.copy(
            userRole = UserRole.FREE,
            isAdminUnlocked = false,
            activeSubscriberAccount = null,
            selectedTab = 0,
            statusMessage = "چوونەدەرەوە لە ئەژمار بە سەرکەوتوویی ئەنجام درا 🚪"
        )
    }

    fun playMediaItem(item: PlaylistItem) {
        _uiState.value = _uiState.value.copy(
            activeMediaItem = item,
            isPlaying = true
        )
        viewModelScope.launch {
            repository.markAsWatched(item)
        }
    }

    fun closePlayer() {
        _uiState.value = _uiState.value.copy(
            activeMediaItem = null,
            isPlaying = false,
            isFullScreenPlayer = false
        )
    }

    fun togglePlayPause() {
        _uiState.value = _uiState.value.copy(
            isPlaying = !_uiState.value.isPlaying
        )
    }

    fun toggleFullScreenPlayer() {
        _uiState.value = _uiState.value.copy(
            isFullScreenPlayer = !_uiState.value.isFullScreenPlayer
        )
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setSelectedCategoryFilter(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategoryFilter = category)
    }

    fun toggleFavorite(item: PlaylistItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun showAccountDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAccountDialog = show)
    }

    fun verifyAndUnlockAdmin(pass: String): Boolean {
        val cleanPass = pass.trim()
        if (cleanPass == "ako@119" || cleanPass == "1234" || cleanPass == "admin" || cleanPass == "admin123" || cleanPass == "123456" || cleanPass == "kurdplay2026") {
            _uiState.value = _uiState.value.copy(userRole = UserRole.ADMIN, isAdminUnlocked = true, statusMessage = "دەسەڵاتی بەڕێوەبەر چالاکبوو 🔓")
            prefs.edit()
                .putString("saved_user_role", "ADMIN")
                .putBoolean("saved_is_admin_unlocked", true)
                .apply()
            return true
        } else {
            _uiState.value = _uiState.value.copy(statusMessage = "❌ تێپەڕەوشەی بەڕێوەبەر هەڵەیە!")
            return false
        }
    }

    fun verifyAndUnlockAdminWithCredentials(user: String, pass: String): Boolean {
        val cleanUser = user.trim()
        val cleanPass = pass.trim()

        val isValidUser = cleanUser.equals("ako", ignoreCase = true) ||
                cleanUser.equals("admin", ignoreCase = true) ||
                cleanUser.equals("manager", ignoreCase = true) ||
                cleanUser.equals("kurdplay_admin", ignoreCase = true)

        val isValidPass = cleanPass == "ako@119" ||
                cleanPass == "1234" ||
                cleanPass == "admin" ||
                cleanPass == "admin123" ||
                cleanPass == "123456" ||
                cleanPass == "kurdplay2026"

        if (cleanUser.isNotEmpty() && cleanPass.isNotEmpty() && isValidUser && isValidPass) {
            _uiState.value = _uiState.value.copy(
                userRole = UserRole.ADMIN,
                isAdminUnlocked = true,
                statusMessage = "بەخێربێیت بەڕێوەبەر ($cleanUser) - بەشی بەڕێوەبەر چالاکبوو 🔓"
            )
            prefs.edit()
                .putString("saved_user_role", "ADMIN")
                .putBoolean("saved_is_admin_unlocked", true)
                .apply()
            return true
        } else {
            _uiState.value = _uiState.value.copy(statusMessage = "❌ ناوی بەکارهێنەر یان تێپەڕەوشەی بەڕێوەبەر هەڵەیە!")
            return false
        }
    }

    fun playNextChannel() {
        val currentItem = _uiState.value.activeMediaItem ?: return
        val channelsList = liveChannels.value.ifEmpty { allItems.value }
        if (channelsList.isNotEmpty()) {
            val currentIndex = channelsList.indexOfFirst { it.id == currentItem.id }
            val nextIndex = if (currentIndex != -1 && currentIndex + 1 < channelsList.size) currentIndex + 1 else 0
            val nextItem = channelsList[nextIndex]
            playMediaItem(nextItem)
            _uiState.value = _uiState.value.copy(statusMessage = "پەڕینەوە بۆ کەناڵی دواتر: ${nextItem.title} ⏩")
        }
    }

    fun setAdminUnlocked(unlocked: Boolean) {
        _uiState.value = _uiState.value.copy(isAdminUnlocked = unlocked)
    }

    fun toggleLanguage() {
        val newLang = if (_uiState.value.language == "KU") "EN" else "KU"
        _uiState.value = _uiState.value.copy(language = newLang)
    }

    fun addXtreamAccount(name: String, host: String, user: String, pass: String, plan: String = "1 Year") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = "پەیوەستبوون بە سێرڤەری Xtream Codes... ⏳")
            val count = repository.addXtreamAccount(name, host, user, pass, plan)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showAccountDialog = false,
                statusMessage = "سێرڤەری Xtream بە سەرکەوتوویی پەیوەست بوو! $count کەناڵ/فیلم هاوکات کران ⚡"
            )
        }
    }

    fun addM3uAccount(name: String, url: String, plan: String = "1 Year") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = "خوێندنەوەی لینکی M3U Playlist... ⏳")
            val count = repository.addM3uAccount(name, url, plan)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                showAccountDialog = false,
                statusMessage = "لیستی M3U بە سەرکەوتوویی بارکرا! $count کەناڵ/فیلم هاوکات کران ⚡"
            )
        }
    }

    fun addSubscriberAccount(user: String, pass: String, plan: String = "1 Year") {
        viewModelScope.launch {
            val durationMillis = when (plan) {
                "7 Days" -> 7L * 24 * 3600 * 1000
                "6 Months" -> 180L * 24 * 3600 * 1000
                else -> 365L * 24 * 3600 * 1000
            }
            val account = PlaylistAccount(
                name = user,
                username = user,
                password = pass,
                type = "SUBSCRIBER",
                subscriptionPlan = plan,
                expiryTimestamp = System.currentTimeMillis() + durationMillis
            )
            repository.insertAccount(account)
            _uiState.value = _uiState.value.copy(
                statusMessage = "ئەکاونتی VIP نوێ بە سەرکەوتوویی دروستکرا (یوزەر: $user | پاسۆرد: $pass) 🔑"
            )
        }
    }

    fun toggleItemFreeStatus(item: PlaylistItem) {
        viewModelScope.launch {
            repository.toggleItemFreeStatus(item)
            val status = if (item.isFree) "تایبەت / VIP" else "فری / Free"
            _uiState.value = _uiState.value.copy(statusMessage = "دۆخی کەناڵەکە گۆڕدرا بۆ: $status (سەیڤ بوو و دەستبەجێ هاوکات کرا)")
        }
    }

    fun setCategoryFreeStatus(category: String, isFree: Boolean) {
        viewModelScope.launch {
            repository.setCategoryFreeStatus(category, isFree)
            val label = if (isFree) "Free (بەلاش)" else "VIP (تایبەت)"
            _uiState.value = _uiState.value.copy(statusMessage = "سەرجەم کەناڵەکانی  $category  گۆڕدران بۆ $label (دەستبەجێ سەیڤ بوو)")
        }
    }

    fun swapItemsOrder(item1: PlaylistItem, item2: PlaylistItem) {
        viewModelScope.launch {
            repository.swapItemsOrder(item1, item2)
            _uiState.value = _uiState.value.copy(statusMessage = "شوێنی کەناڵەکە ئاڵوگۆڕ کراوە و سەیڤ کرا ⚡")
        }
    }

    fun moveItemUp(item: PlaylistItem) {
        viewModelScope.launch {
            repository.moveItemUp(item)
            _uiState.value = _uiState.value.copy(statusMessage = "کەناڵەکە هێنرایە پێشەوە ⬆️")
        }
    }

    fun moveItemDown(item: PlaylistItem) {
        viewModelScope.launch {
            repository.moveItemDown(item)
            _uiState.value = _uiState.value.copy(statusMessage = "کەناڵەکە برا بۆ دواوە ⬇️")
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.resetToDefaultDemo()
            _uiState.value = _uiState.value.copy(statusMessage = "لیستی کەناڵەکان گەڕێندرایەوە و هاوکات کرا ⚡")
        }
    }

    fun resetMarketProducts() {
        viewModelScope.launch {
            repository.resetMarketProducts()
            _uiState.value = _uiState.value.copy(statusMessage = "کاڵاکانی مارکێت گەڕێندرانەوە و هاوکات کران ⚡")
        }
    }

    fun deleteAccount(account: PlaylistAccount) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            _uiState.value = _uiState.value.copy(statusMessage = "ئەژمارەکە سڕایەوە و دەستبەجێ ڕێکخرایەوە")
        }
    }

    fun deleteItem(item: PlaylistItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            _uiState.value = _uiState.value.copy(statusMessage = "کەناڵ/فیلمەکە سڕایەوە و دەستبەجێ هاوکات کرا ⚡")
        }
    }

    fun addCustomChannel(title: String, streamUrl: String, category: String, logoUrl: String, mediaType: String) {
        viewModelScope.launch {
            val item = PlaylistItem(
                title = title,
                streamUrl = streamUrl,
                category = category.ifEmpty { "General" },
                logoUrl = logoUrl,
                mediaType = mediaType
            )
            repository.insertItem(item)
            _uiState.value = _uiState.value.copy(statusMessage = "کەناڵی نوێ سەیڤ کرا و دەستبەجێ لەسەر هەموو ئامێرەکان چالاك بوو ⚡")
        }
    }

    fun addMarketItem(
        title: String,
        price: String,
        category: String,
        imageUrl: String,
        additionalImages: String = "",
        videoUrl: String = "",
        description: String,
        phone: String
    ) {
        viewModelScope.launch {
            val item = MarketItem(
                title = title,
                price = price,
                category = category,
                imageUrl = imageUrl,
                additionalImages = additionalImages,
                videoUrl = videoUrl,
                description = description,
                contactPhone = phone.ifEmpty { "07501191987" }
            )
            repository.insertMarketItem(item)
            _uiState.value = _uiState.value.copy(statusMessage = "کاڵای نوێ بۆ مارکێت زیادکرا")
        }
    }

    fun deleteMarketItem(item: MarketItem) {
        viewModelScope.launch {
            repository.deleteMarketItem(item)
            _uiState.value = _uiState.value.copy(statusMessage = "کاڵاکە لە مارکێت سڕایەوە")
        }
    }

    fun openWebsite(url: String, title: String) {
        _uiState.value = _uiState.value.copy(
            activeWebsiteUrl = url,
            activeWebsiteTitle = title
        )
    }

    fun closeWebsite() {
        _uiState.value = _uiState.value.copy(
            activeWebsiteUrl = null,
            activeWebsiteTitle = null
        )
    }

    fun addWebsiteItem(title: String, url: String, logoUrl: String, category: String, description: String) {
        viewModelScope.launch {
            val item = WebsiteItem(
                title = title,
                url = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url",
                logoUrl = logoUrl,
                category = category.ifEmpty { "Kurdish Portals" },
                description = description
            )
            repository.insertWebsiteItem(item)
            _uiState.value = _uiState.value.copy(statusMessage = "وێب سایتی نوێ بە سەرکەوتوویی زیادکرا")
        }
    }

    fun toggleTvMode() {
        val newTvState = !_uiState.value.isTvMode
        prefs.edit().putBoolean("saved_is_tv_mode", newTvState).apply()
        _uiState.value = _uiState.value.copy(
            isTvMode = newTvState,
            statusMessage = if (newTvState) "مۆدی TV 📺 چالاککرا (گۆڕینی لۆگۆ و بەکارهێنانی کۆنتڕۆڵ)" else "مۆدی موبایل 📱 چالاککرا"
        )
    }

    private fun saveConfigToLocalPrefs() {
        runCatching {
            val state = _uiState.value
            val topBannersJson = JSONArray(state.topBannerUrls).toString()
            val bottomBannersJson = JSONArray(state.bottomBannerUrls).toString()
            prefs.edit()
                .putString("saved_support_phone", state.supportPhone)
                .putString("saved_top_banners", topBannersJson)
                .putString("saved_bottom_banners", bottomBannersJson)
                .putString("saved_bottom_ticker", state.bottomTickerText)
                .putString("saved_market_cover", state.marketCoverUrl)
                .putBoolean("saved_preroll_enabled", state.enablePrerollVideo)
                .putString("saved_preroll_url", state.prerollVideoUrl)
                .putBoolean("saved_update_available", state.isAppUpdateAvailable)
                .putString("saved_update_title", state.updateDialogTitle)
                .putString("saved_update_gplay", state.updateGooglePlayUrl)
                .putString("saved_update_apk", state.updateApkUrl)
                .putString("saved_update_aptoide", state.updateAptoideUrl)
                .apply()
        }
    }

    private fun restoreConfigFromLocalPrefs() {
        runCatching {
            val savedSupportPhone = prefs.getString("saved_support_phone", null)
            val savedTopBannersStr = prefs.getString("saved_top_banners", null)
            val savedBottomBannersStr = prefs.getString("saved_bottom_banners", null)
            val savedBottomTicker = prefs.getString("saved_bottom_ticker", null)
            val savedMarketCover = prefs.getString("saved_market_cover", null)
            val savedPrerollEnabled = if (prefs.contains("saved_preroll_enabled")) prefs.getBoolean("saved_preroll_enabled", false) else null
            val savedPrerollUrl = prefs.getString("saved_preroll_url", null)
            val savedUpdateAvail = if (prefs.contains("saved_update_available")) prefs.getBoolean("saved_update_available", false) else null
            val savedUpdateTitle = prefs.getString("saved_update_title", null)
            val savedUpdateGPlay = prefs.getString("saved_update_gplay", null)
            val savedUpdateApk = prefs.getString("saved_update_apk", null)
            val savedUpdateAptoide = prefs.getString("saved_update_aptoide", null)

            var topBanners = _uiState.value.topBannerUrls
            if (savedTopBannersStr != null) {
                runCatching {
                    val arr = JSONArray(savedTopBannersStr)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        val url = arr.optString(i)
                        if (url.isNotBlank()) list.add(url)
                    }
                    topBanners = list
                }
            }

            var bottomBanners = _uiState.value.bottomBannerUrls
            if (savedBottomBannersStr != null) {
                runCatching {
                    val arr = JSONArray(savedBottomBannersStr)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        val url = arr.optString(i)
                        if (url.isNotBlank()) list.add(url)
                    }
                    bottomBanners = list
                }
            }

            _uiState.value = _uiState.value.copy(
                supportPhone = savedSupportPhone ?: _uiState.value.supportPhone,
                topBannerUrls = topBanners,
                bottomBannerUrls = bottomBanners,
                bottomTickerText = savedBottomTicker ?: _uiState.value.bottomTickerText,
                marketCoverUrl = savedMarketCover ?: _uiState.value.marketCoverUrl,
                enablePrerollVideo = savedPrerollEnabled ?: _uiState.value.enablePrerollVideo,
                prerollVideoUrl = savedPrerollUrl ?: _uiState.value.prerollVideoUrl,
                isAppUpdateAvailable = savedUpdateAvail ?: _uiState.value.isAppUpdateAvailable,
                updateDialogTitle = savedUpdateTitle ?: _uiState.value.updateDialogTitle,
                updateGooglePlayUrl = savedUpdateGPlay ?: _uiState.value.updateGooglePlayUrl,
                updateApkUrl = savedUpdateApk ?: _uiState.value.updateApkUrl,
                updateAptoideUrl = savedUpdateAptoide ?: _uiState.value.updateAptoideUrl
            )
        }
    }

    private fun pushConfigToCloud() {
        saveConfigToLocalPrefs()
        viewModelScope.launch {
            val configObj = JSONObject().apply {
                put("supportPhone", _uiState.value.supportPhone)
                put("topBannerUrls", JSONArray(_uiState.value.topBannerUrls))
                put("bottomBannerUrls", JSONArray(_uiState.value.bottomBannerUrls))
                put("bottomTickerText", _uiState.value.bottomTickerText)
                put("marketCoverUrl", _uiState.value.marketCoverUrl)
                put("enablePrerollVideo", _uiState.value.enablePrerollVideo)
                put("prerollVideoUrl", _uiState.value.prerollVideoUrl)
                put("isAppUpdateAvailable", _uiState.value.isAppUpdateAvailable)
                put("updateDialogTitle", _uiState.value.updateDialogTitle)
                put("updateGooglePlayUrl", _uiState.value.updateGooglePlayUrl)
                put("updateApkUrl", _uiState.value.updateApkUrl)
                put("updateAptoideUrl", _uiState.value.updateAptoideUrl)
            }
            CloudSyncEngine.pushConfigToCloud(configObj)
        }
    }

    suspend fun fetchAndApplyCloudConfig() {
        val cloudConfig = CloudSyncEngine.fetchCloudConfig()
        if (cloudConfig != null) {
            val phone = cloudConfig.optString("supportPhone", _uiState.value.supportPhone)
            val ticker = cloudConfig.optString("bottomTickerText", _uiState.value.bottomTickerText)
            val marketCover = cloudConfig.optString("marketCoverUrl", _uiState.value.marketCoverUrl)
            val prerollEnabled = cloudConfig.optBoolean("enablePrerollVideo", _uiState.value.enablePrerollVideo)
            val prerollUrl = cloudConfig.optString("prerollVideoUrl", _uiState.value.prerollVideoUrl)

            val updateAvail = cloudConfig.optBoolean("isAppUpdateAvailable", _uiState.value.isAppUpdateAvailable)
            val updateTitle = cloudConfig.optString("updateDialogTitle", _uiState.value.updateDialogTitle)
            val updateGPlay = cloudConfig.optString("updateGooglePlayUrl", _uiState.value.updateGooglePlayUrl)
            val updateApk = cloudConfig.optString("updateApkUrl", _uiState.value.updateApkUrl)
            val updateAptoide = cloudConfig.optString("updateAptoideUrl", _uiState.value.updateAptoideUrl)

            val topBannersArr = cloudConfig.optJSONArray("topBannerUrls")
            val topBanners = mutableListOf<String>()
            if (topBannersArr != null) {
                for (i in 0 until topBannersArr.length()) {
                    val url = topBannersArr.optString(i)
                    if (url.isNotBlank()) topBanners.add(url)
                }
            }

            val bottomBannersArr = cloudConfig.optJSONArray("bottomBannerUrls")
            val bottomBanners = mutableListOf<String>()
            if (bottomBannersArr != null) {
                for (i in 0 until bottomBannersArr.length()) {
                    val url = bottomBannersArr.optString(i)
                    if (url.isNotBlank()) bottomBanners.add(url)
                }
            }

            _uiState.value = _uiState.value.copy(
                supportPhone = phone,
                bottomTickerText = ticker,
                marketCoverUrl = marketCover,
                enablePrerollVideo = prerollEnabled,
                prerollVideoUrl = prerollUrl,
                isAppUpdateAvailable = updateAvail,
                updateDialogTitle = updateTitle,
                updateGooglePlayUrl = updateGPlay,
                updateApkUrl = updateApk,
                updateAptoideUrl = updateAptoide,
                topBannerUrls = if (topBannersArr != null) topBanners else _uiState.value.topBannerUrls,
                bottomBannerUrls = if (bottomBannersArr != null) bottomBanners else _uiState.value.bottomBannerUrls
            )
            saveConfigToLocalPrefs()
        }
    }

    fun updateSupportPhone(phone: String) {
        if (phone.isNotBlank()) {
            val cleanPhone = phone.trim()
            _uiState.value = _uiState.value.copy(
                supportPhone = cleanPhone,
                statusMessage = "ژمارەی مۆبایلی پشتیوانی لە سیتینگ نوێکرایەوە: $cleanPhone 📞"
            )
            pushConfigToCloud()
        }
    }

    fun updateBannerSettings(topBannerList: List<String>, bottomBannerList: List<String> = emptyList(), bottomTicker: String = "") {
        _uiState.value = _uiState.value.copy(
            topBannerUrls = topBannerList,
            bottomBannerUrls = bottomBannerList,
            bottomTickerText = if (bottomTicker.isNotBlank()) bottomTicker else _uiState.value.bottomTickerText,
            statusMessage = "ڕیکلامەکان بە سەرکەوتوویی نوێکرانەوە ✅"
        )
        pushConfigToCloud()
    }

    fun updatePrerollSettings(enabled: Boolean, videoUrl: String) {
        _uiState.value = _uiState.value.copy(
            enablePrerollVideo = enabled,
            prerollVideoUrl = videoUrl,
            statusMessage = "ڕێکخستنی ڤیدیۆی پێشەکی نوێکرایەوە 🎬"
        )
        pushConfigToCloud()
    }

    fun updateMarketCover(coverUrl: String) {
        if (coverUrl.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                marketCoverUrl = coverUrl,
                statusMessage = "وێنەی سەرەوەی مارکێت نوێکرایەوە 🛍️"
            )
            pushConfigToCloud()
        }
    }

    fun updateAppUpdateSettings(
        enabled: Boolean,
        dialogTitle: String,
        googlePlay: String,
        apk: String,
        aptoide: String
    ) {
        _uiState.value = _uiState.value.copy(
            isAppUpdateAvailable = enabled,
            updateDialogTitle = dialogTitle.ifEmpty { _uiState.value.updateDialogTitle },
            updateGooglePlayUrl = googlePlay.ifEmpty { _uiState.value.updateGooglePlayUrl },
            updateApkUrl = apk.ifEmpty { _uiState.value.updateApkUrl },
            updateAptoideUrl = aptoide.ifEmpty { _uiState.value.updateAptoideUrl },
            showUpdateDialog = enabled,
            statusMessage = "زانیارییەکانی ئەپدەیتی بەرنامە نوێکرایەوە 🚀"
        )
        pushConfigToCloud()
    }

    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(showUpdateDialog = false)
    }

    fun checkSubscriberLogin(user: String, pass: String): Boolean {
        val cleanUser = user.trim()
        val cleanPass = pass.trim()
        if (cleanUser.isEmpty() || cleanPass.isEmpty()) {
            _uiState.value = _uiState.value.copy(statusMessage = "❌ تکایە هەردوو خانەی یوزەرنەیم و تێپەڕەوشە پڕ بکەرەوە!")
            return false
        }

        val matchingAcc = accounts.value.find {
            it.username.equals(cleanUser, ignoreCase = true) && it.password == cleanPass
        }

        return if (matchingAcc != null) {
            setActiveSubscriber(matchingAcc)
            _uiState.value = _uiState.value.copy(statusMessage = "بەخێربێیت VIP ($cleanUser) 🟢")
            true
        } else {
            _uiState.value = _uiState.value.copy(statusMessage = "❌ یوزەرنەیم یان تێپەڕەوشەی VIP هەڵەیە! پەیوەندی بە بەڕێوەبەر بکە.")
            false
        }
    }

    fun syncFromCloud() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                syncProgressPercent = 5,
                statusMessage = "خەریکی هێنانی ئەپدەیتی تازەی داتابەیسە لە سێرڤەری ئۆنلاین... ⏳"
            )
            val count = repository.syncFromCloud { progress ->
                _uiState.value = _uiState.value.copy(
                    syncProgressPercent = progress,
                    statusMessage = "داگرتنی داتابەیس، یوزەر، پاسۆرد و وێنە ڕیکلامییەکان ($progress%)... ⚡"
                )
            }
            fetchAndApplyCloudConfig()
            _uiState.value = _uiState.value.copy(
                syncProgressPercent = 100,
                statusMessage = if (count > 0) "دەستبەجێ $count کەناڵ/فیلم لە داتابەیسی ئۆنلاینەوە وەرگیران ⚡" else "داتابەیسی ئۆنلاین ئامادەیە 🟢"
            )
            kotlinx.coroutines.delay(1000L)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                syncProgressPercent = 0
            )
        }
    }

    fun pushAllToCloud() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                syncProgressPercent = 5,
                statusMessage = "ناردنی سەرجەم کەناڵ، وێنە ڕیکلامییەکان، یوزەر و پاسۆردەکان بۆ سێرڤەری ئۆنلاین... ⏳"
            )
            pushConfigToCloud()
            val success = repository.pushAllToCloud { progress ->
                _uiState.value = _uiState.value.copy(
                    syncProgressPercent = progress,
                    statusMessage = "ناردنی کەناڵ، وێنەی ڕیکلامەکان، یوزەر و تێپەڕەوشەکان ($progress%)... ☁️"
                )
            }
            _uiState.value = _uiState.value.copy(
                syncProgressPercent = 100,
                statusMessage = if (success) "سەرجەم نوێکاری، وێنە ڕیکلامییەکان، یوزەر و تێپەڕەوشەکان نێردران بۆ داتابەیسی سێرڤەری ئۆنلاین ☁️. ئێستا لای هەموو بەکارهێنەران نوێ بوویەوە." else "تێکچوونی پەیوەندی بە داتابەیسی ئۆنلاین"
            )
            kotlinx.coroutines.delay(1200L)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                syncProgressPercent = 0
            )
        }
    }

    fun renameCategory(oldCategory: String, newCategory: String) {
        viewModelScope.launch {
            repository.renameCategory(oldCategory, newCategory)
            _uiState.value = _uiState.value.copy(statusMessage = "ناوی کاتێگۆری گۆڕدرا بۆ ($newCategory) ✏️")
        }
    }

    fun deleteCategory(categoryName: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryName)
            if (_uiState.value.selectedCategoryFilter.equals(categoryName, ignoreCase = true)) {
                _uiState.value = _uiState.value.copy(selectedCategoryFilter = "All")
            }
            _uiState.value = _uiState.value.copy(statusMessage = "کاتێگۆری ($categoryName) بە سەرکەوتوویی سڕایەوە 🗑️")
        }
    }

    fun moveCategoryOrder(categoryName: String, categoriesList: List<String>, moveLeft: Boolean) {
        viewModelScope.launch {
            repository.moveCategoryOrder(categoryName, categoriesList, moveLeft)
            _uiState.value = _uiState.value.copy(statusMessage = "شوێنی کاتێگۆری ($categoryName) گۆڕدرا 🔄")
        }
    }

    fun deleteWebsiteItem(item: WebsiteItem) {
        viewModelScope.launch {
            repository.deleteWebsiteItem(item)
            _uiState.value = _uiState.value.copy(statusMessage = "وێب سایتەکە سڕایەوە")
        }
    }

    fun resetWebsites() {
        viewModelScope.launch {
            repository.resetWebsites()
            _uiState.value = _uiState.value.copy(statusMessage = "وێب سایتە سەرەتاییەکان گەڕێندرانەوە")
        }
    }
}
