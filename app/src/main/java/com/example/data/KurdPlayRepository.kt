package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class KurdPlayRepository(private val dao: KurdPlayDao) {

    val allItems: Flow<List<PlaylistItem>> = dao.getAllItems()
    val liveChannels: Flow<List<PlaylistItem>> = dao.getItemsByType("LIVE")
    val movies: Flow<List<PlaylistItem>> = dao.getItemsByType("MOVIE")
    val series: Flow<List<PlaylistItem>> = dao.getItemsByType("SERIES")
    val favorites: Flow<List<PlaylistItem>> = dao.getFavoriteItems()
    val recentWatched: Flow<List<PlaylistItem>> = dao.getRecentItems()
    val accounts: Flow<List<PlaylistAccount>> = dao.getAllAccounts()
    val marketItems: Flow<List<MarketItem>> = dao.getAllMarketItems()
    val websiteItems: Flow<List<WebsiteItem>> = dao.getAllWebsiteItems()

    suspend fun seedSampleDataIfEmpty() {
        val currentItems = dao.getAllItems().first()
        if (currentItems.isEmpty()) {
            val samples = M3uParser.getSampleKurdishPlaylists()
            dao.insertItems(samples)

            val defaultAccount = PlaylistAccount(
                name = "Kurd Play Demo Server",
                type = "M3U",
                m3uUrl = "https://kurdplay.app/demo.m3u",
                isActive = true
            )
            dao.insertAccount(defaultAccount)
        }

        // Preload user requested default Xtream Codes IPTV server
        val currentAccounts = dao.getAllAccounts().first()
        if (currentAccounts.none { it.username == "Abdillahmaison" }) {
            addXtreamAccount(
                name = "Dream IPTV Server",
                host = "http://dreamteev.xyz:80",
                user = "Abdillahmaison",
                pass = "BMAUAWX",
                plan = "1 Year"
            )
        }

        // Seed initial default VIP Subscriber accounts if none exist
        val currentAccountsUpdated = dao.getAllAccounts().first()
        if (currentAccountsUpdated.none { it.type == "SUBSCRIBER" }) {
            val defaultVipAcc = PlaylistAccount(
                name = "KurdPlay VIP Subscriber",
                username = "kurdplay_vip",
                password = "123456",
                type = "SUBSCRIBER",
                subscriptionPlan = "1 Year",
                expiryTimestamp = System.currentTimeMillis() + 365L * 24 * 3600 * 1000,
                maxDevices = 1
            )
            dao.insertAccount(defaultVipAcc)
        }

        val currentMarket = dao.getAllMarketItems().first()
        if (currentMarket.isEmpty()) {
            val defaultProducts = M3uParser.getInitialMarketProducts()
            dao.insertMarketItems(defaultProducts)
        }

        val currentWebsites = dao.getAllWebsiteItems().first()
        if (currentWebsites.isEmpty()) {
            val defaultWebsites = M3uParser.getInitialWebsites()
            dao.insertWebsiteItems(defaultWebsites)
        }
    }

    suspend fun syncFromCloud(onProgress: (Int) -> Unit = {}): Int = withContext(Dispatchers.IO) {
        val syncedCount = CloudSyncEngine.syncCloudToLocal(dao, onProgress)
        runCatching {
            val allAccs = dao.getAllAccounts().firstOrNull() ?: emptyList()
            val allItems = dao.getAllItems().firstOrNull() ?: emptyList()
            for (acc in allAccs) {
                if (acc.isActive) {
                    val accItems = allItems.filter { it.accountOwnerId == acc.id }
                    val shouldRefresh = accItems.isEmpty() || acc.m3uUrl.contains("github", ignoreCase = true) || acc.m3uUrl.contains("raw.githubusercontent", ignoreCase = true)
                    if (shouldRefresh || accItems.isEmpty()) {
                        if (acc.type == "XTREAM" && acc.host.isNotBlank() && acc.username.isNotBlank()) {
                            val fetched = M3uParser.fetchXtreamAccountItems(acc.host, acc.username, acc.password, acc.id)
                            if (fetched.isNotEmpty()) {
                                fetched.chunked(500).forEach { chunk -> dao.insertItems(chunk) }
                            }
                        } else if (acc.type == "HTML" || acc.m3uUrl.contains(".html", ignoreCase = true) || acc.m3uUrl.contains("html", ignoreCase = true)) {
                            val fetched = M3uParser.fetchAndParseHtml(acc.m3uUrl, acc.id)
                            if (fetched.isNotEmpty()) {
                                fetched.chunked(500).forEach { chunk -> dao.insertItems(chunk) }
                            }
                        } else if (acc.type == "M3U" && acc.m3uUrl.isNotBlank()) {
                            val fetched = M3uParser.fetchAndParseM3u(acc.m3uUrl, acc.id)
                            if (fetched.isNotEmpty()) {
                                fetched.chunked(500).forEach { chunk -> dao.insertItems(chunk) }
                            }
                        }
                    }
                }
            }
        }
        syncedCount
    }

    suspend fun pushAllToCloud(onProgress: (Int) -> Unit = {}): Boolean = coroutineScope {
        onProgress(15)
        val items = dao.getAllItems().first()
        val accounts = dao.getAllAccounts().first()
        val marketItems = dao.getAllMarketItems().first()
        val websiteItems = dao.getAllWebsiteItems().first()
        onProgress(35)

        // Upload channels, accounts, market products & websites concurrently in parallel!
        val itemsDeferred = async { CloudSyncEngine.syncAllItemsToCloud(items) }
        val accountsDeferred = async { CloudSyncEngine.pushAllAccountsToCloud(accounts) }
        val marketDeferred = async { CloudSyncEngine.pushAllMarketItemsToCloud(marketItems) }
        val websiteDeferred = async { CloudSyncEngine.pushAllWebsiteItemsToCloud(websiteItems) }

        onProgress(75)

        val itemsOk = itemsDeferred.await()
        val accOk = accountsDeferred.await()
        val marketOk = marketDeferred.await()
        val webOk = websiteDeferred.await()

        onProgress(100)
        itemsOk && accOk && marketOk && webOk
    }

    suspend fun insertItem(item: PlaylistItem): Long {
        val id = dao.insertItem(item)
        val createdItem = item.copy(id = id.toInt())
        CloudSyncEngine.pushItemToCloud(createdItem)
        return id
    }

    suspend fun toggleFavorite(item: PlaylistItem) {
        val updated = item.copy(isFavorite = !item.isFavorite)
        dao.updateItem(updated)
        CloudSyncEngine.pushItemToCloud(updated)
    }

    suspend fun markAsWatched(item: PlaylistItem) {
        val updated = item.copy(lastWatched = System.currentTimeMillis())
        dao.updateItem(updated)
    }

    suspend fun addXtreamAccount(name: String, host: String, user: String, pass: String, plan: String = "1 Year"): Int {
        val creds = M3uParser.extractXtreamDetails(host, user, pass)
        val cleanHost = creds.host
        val cleanUser = creds.username
        val cleanPass = creds.password
        val m3uUrl = M3uParser.buildXtreamM3uUrl(cleanHost, cleanUser, cleanPass)
        val durationMillis = when (plan) {
            "7 Days" -> 7L * 24 * 3600 * 1000
            "6 Months" -> 180L * 24 * 3600 * 1000
            else -> 365L * 24 * 3600 * 1000
        }
        val account = PlaylistAccount(
            name = name.ifEmpty { if (cleanUser.isNotEmpty()) "Xtream ($cleanUser)" else "Xtream Server" },
            type = "XTREAM",
            host = cleanHost,
            username = cleanUser,
            password = cleanPass,
            m3uUrl = m3uUrl,
            isActive = true,
            subscriptionPlan = plan,
            expiryTimestamp = System.currentTimeMillis() + durationMillis,
            maxDevices = 1,
            boundDeviceId = "Device-Android-" + (1000..9999).random()
        )
        val accountId = dao.insertAccount(account).toInt()
        val createdAcc = account.copy(id = accountId)
        CloudSyncEngine.pushAccountToCloud(createdAcc)

        // Fetch using enhanced Xtream parser (M3U + JSON API fallback)
        val fetchedItems = M3uParser.fetchXtreamAccountItems(cleanHost, cleanUser, cleanPass, accountId)

        val count = if (fetchedItems.isNotEmpty()) {
            fetchedItems.chunked(500).forEach { chunk -> dao.insertItems(chunk) }
            fetchedItems.size
        } else {
            val samples = M3uParser.getSampleKurdishPlaylists().map { it.copy(accountOwnerId = accountId) }
            dao.insertItems(samples)
            samples.size
        }
        pushAllToCloud()
        return count
    }

    suspend fun addM3uAccount(name: String, m3uUrl: String, plan: String = "1 Year"): Int {
        val durationMillis = when (plan) {
            "7 Days" -> 7L * 24 * 3600 * 1000
            "6 Months" -> 180L * 24 * 3600 * 1000
            else -> 365L * 24 * 3600 * 1000
        }
        val cleanUrl = m3uUrl.trim()
        val account = PlaylistAccount(
            name = name.ifEmpty { "M3U Playlist" },
            type = "M3U",
            m3uUrl = cleanUrl,
            isActive = true,
            subscriptionPlan = plan,
            expiryTimestamp = System.currentTimeMillis() + durationMillis,
            maxDevices = 1,
            boundDeviceId = "Device-Android-" + (1000..9999).random()
        )
        val accountId = dao.insertAccount(account).toInt()
        val createdAcc = account.copy(id = accountId)
        CloudSyncEngine.pushAccountToCloud(createdAcc)

        val fetchedItems = M3uParser.fetchAndParseM3u(cleanUrl, accountId)

        val count = if (fetchedItems.isNotEmpty()) {
            fetchedItems.chunked(500).forEach { chunk -> dao.insertItems(chunk) }
            fetchedItems.size
        } else {
            val samples = M3uParser.getSampleKurdishPlaylists().map { it.copy(accountOwnerId = accountId) }
            dao.insertItems(samples)
            samples.size
        }
        pushAllToCloud()
        return count
    }

    suspend fun insertAccount(account: PlaylistAccount): Long {
        val id = dao.insertAccount(account)
        val created = account.copy(id = id.toInt())
        CloudSyncEngine.pushAccountToCloud(created)
        return id
    }

    suspend fun renameCategory(oldCategory: String, newCategory: String) {
        val all = dao.getAllItems().first()
        val updated = all.filter { it.category.equals(oldCategory, ignoreCase = true) }
            .map { it.copy(category = newCategory, groupTitle = newCategory) }
        dao.insertItems(updated)
        updated.forEach { CloudSyncEngine.pushItemToCloud(it) }
    }

    suspend fun deleteCategory(categoryName: String) {
        val all = dao.getAllItems().first()
        val toDelete = all.filter { it.category.equals(categoryName, ignoreCase = true) }
        toDelete.forEach {
            dao.deleteItem(it)
            CloudSyncEngine.deleteItemFromCloud(it.id)
        }
    }

    suspend fun moveCategoryOrder(categoryName: String, categoriesList: List<String>, moveLeft: Boolean) {
        val index = categoriesList.indexOfFirst { it.equals(categoryName, ignoreCase = true) }
        if (index == -1) return
        val targetIndex = if (moveLeft) index - 1 else index + 1
        if (targetIndex < 0 || targetIndex >= categoriesList.size) return

        val targetCategory = categoriesList[targetIndex]
        val allItems = dao.getAllItems().first()

        val itemsCategory1 = allItems.filter { it.category.equals(categoryName, ignoreCase = true) }
        val itemsCategory2 = allItems.filter { it.category.equals(targetCategory, ignoreCase = true) }

        if (itemsCategory1.isEmpty() || itemsCategory2.isEmpty()) return

        val minOrderCategory1 = itemsCategory1.minOfOrNull { it.displayOrder } ?: 0
        val minOrderCategory2 = itemsCategory2.minOfOrNull { it.displayOrder } ?: 0

        // Swap base display orders for all items in category 1 and category 2
        val updated1 = itemsCategory1.map { it.copy(displayOrder = minOrderCategory2) }
        val updated2 = itemsCategory2.map { it.copy(displayOrder = minOrderCategory1) }

        dao.insertItems(updated1 + updated2)
        (updated1 + updated2).forEach { CloudSyncEngine.pushItemToCloud(it) }
    }

    suspend fun toggleItemFreeStatus(item: PlaylistItem) {
        val updated = item.copy(isFree = !item.isFree)
        dao.updateItem(updated)
        CloudSyncEngine.pushItemToCloud(updated)
    }

    suspend fun setCategoryFreeStatus(categoryName: String, isFree: Boolean) {
        val all = dao.getAllItems().first()
        val updated = all.filter { it.category.equals(categoryName, ignoreCase = true) }
            .map { it.copy(isFree = isFree) }
        dao.insertItems(updated)
        updated.forEach { CloudSyncEngine.pushItemToCloud(it) }
    }

    suspend fun swapItemsOrder(item1: PlaylistItem, item2: PlaylistItem) {
        val order1 = item1.displayOrder
        val order2 = item2.displayOrder
        val u1 = item1.copy(displayOrder = if (order1 == order2) order2 + 1 else order2)
        val u2 = item2.copy(displayOrder = order1)
        dao.updateItem(u1)
        dao.updateItem(u2)
        CloudSyncEngine.pushItemToCloud(u1)
        CloudSyncEngine.pushItemToCloud(u2)
    }

    suspend fun moveItemUp(item: PlaylistItem) {
        val all = dao.getAllItems().first().sortedWith(compareBy<PlaylistItem>({ it.displayOrder }, { it.id }))
        val index = all.indexOfFirst { it.id == item.id }
        if (index > 0) {
            val prevItem = all[index - 1]
            val prevOrder = prevItem.displayOrder
            val currOrder = item.displayOrder
            val uItem = item.copy(displayOrder = if (prevOrder == currOrder) currOrder - 1 else prevOrder)
            val uPrev = prevItem.copy(displayOrder = if (prevOrder == currOrder) currOrder + 1 else currOrder)
            dao.updateItem(uItem)
            dao.updateItem(uPrev)
            CloudSyncEngine.pushItemToCloud(uItem)
            CloudSyncEngine.pushItemToCloud(uPrev)
        }
    }

    suspend fun moveItemDown(item: PlaylistItem) {
        val all = dao.getAllItems().first().sortedWith(compareBy<PlaylistItem>({ it.displayOrder }, { it.id }))
        val index = all.indexOfFirst { it.id == item.id }
        if (index != -1 && index < all.size - 1) {
            val nextItem = all[index + 1]
            val nextOrder = nextItem.displayOrder
            val currOrder = item.displayOrder
            val uItem = item.copy(displayOrder = if (nextOrder == currOrder) currOrder + 1 else nextOrder)
            val uNext = nextItem.copy(displayOrder = if (nextOrder == currOrder) currOrder - 1 else currOrder)
            dao.updateItem(uItem)
            dao.updateItem(uNext)
            CloudSyncEngine.pushItemToCloud(uItem)
            CloudSyncEngine.pushItemToCloud(uNext)
        }
    }

    suspend fun resetToDefaultDemo() {
        dao.deleteAllItems()
        val samples = M3uParser.getSampleKurdishPlaylists()
        dao.insertItems(samples)
        pushAllToCloud()
    }

    suspend fun deleteAccount(account: PlaylistAccount) {
        dao.deleteItemsByAccount(account.id)
        dao.deleteAccount(account)
        CloudSyncEngine.deleteAccountFromCloud(account)
    }

    suspend fun deleteItem(item: PlaylistItem) {
        dao.deleteItem(item)
        CloudSyncEngine.deleteItemFromCloud(item.id)
    }

    suspend fun insertMarketItem(item: MarketItem) {
        val id = dao.insertMarketItem(item)
        val created = item.copy(id = id.toInt())
        CloudSyncEngine.pushMarketItemToCloud(created)
    }

    suspend fun deleteMarketItem(item: MarketItem) {
        dao.deleteMarketItem(item)
        CloudSyncEngine.deleteMarketItemFromCloud(item.id)
    }

    suspend fun resetMarketProducts() {
        dao.deleteAllMarketItems()
        val defaultItems = M3uParser.getInitialMarketProducts()
        dao.insertMarketItems(defaultItems)
        defaultItems.forEach { CloudSyncEngine.pushMarketItemToCloud(it) }
    }

    suspend fun insertWebsiteItem(item: WebsiteItem) {
        val id = dao.insertWebsiteItem(item)
        val created = item.copy(id = id.toInt())
        CloudSyncEngine.pushWebsiteItemToCloud(created)
    }

    suspend fun deleteWebsiteItem(item: WebsiteItem) {
        dao.deleteWebsiteItem(item)
        CloudSyncEngine.deleteWebsiteItemFromCloud(item.id)
    }

    suspend fun resetWebsites() {
        dao.deleteAllWebsiteItems()
        val defaultWebsites = M3uParser.getInitialWebsites()
        dao.insertWebsiteItems(defaultWebsites)
        defaultWebsites.forEach { CloudSyncEngine.pushWebsiteItemToCloud(it) }
    }
}
