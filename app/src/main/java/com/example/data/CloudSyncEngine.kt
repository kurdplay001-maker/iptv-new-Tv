package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object CloudSyncEngine {

    // Default Firebase Realtime Database / Online Backend Endpoint for KurdPlay (Project: kurd-play-tv)
    var cloudDbUrl: String = "https://kurd-play-tv-default-rtdb.firebaseio.com"

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL").apply {
                init(null, trustAllCerts, SecureRandom())
            }

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        }
    }

    private fun getCleanDbUrl(): String {
        var url = cloudDbUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        return url.removeSuffix("/")
    }

    // Convert PlaylistItem to JSON Object
    private fun itemToJson(item: PlaylistItem): JSONObject {
        return JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("streamUrl", item.streamUrl)
            put("category", item.category)
            put("groupTitle", item.groupTitle)
            put("logoUrl", item.logoUrl)
            put("mediaType", item.mediaType)
            put("isFree", item.isFree)
            put("isFavorite", item.isFavorite)
            put("displayOrder", item.displayOrder)
            put("accountOwnerId", item.accountOwnerId)
            put("lastWatched", item.lastWatched)
            put("episodesList", item.episodesList)
        }
    }

    // Convert JSON Object to PlaylistItem
    private fun jsonToItem(json: JSONObject): PlaylistItem {
        val rawId = json.optInt("id", 0)
        val streamUrl = json.optString("streamUrl", "")
        val title = json.optString("title", "Unknown")
        val computedId = if (rawId > 0) rawId else kotlin.math.abs((streamUrl + title).hashCode()).coerceAtLeast(1)

        return PlaylistItem(
            id = computedId,
            title = title,
            streamUrl = streamUrl,
            category = json.optString("category", "General"),
            groupTitle = json.optString("groupTitle", "General"),
            logoUrl = json.optString("logoUrl", ""),
            mediaType = json.optString("mediaType", "LIVE"),
            isFree = json.optBoolean("isFree", false),
            isFavorite = json.optBoolean("isFavorite", false),
            displayOrder = json.optInt("displayOrder", 0),
            accountOwnerId = json.optInt("accountOwnerId", 0),
            lastWatched = json.optLong("lastWatched", 0L),
            episodesList = json.optString("episodesList", "")
        )
    }

    // Push/Update single item to Firebase/Cloud Backend
    suspend fun pushItemToCloud(item: PlaylistItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${getCleanDbUrl()}/channels/${item.id}.json"
            val body = itemToJson(item).toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url(endpoint)
                .put(body)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Delete item from Firebase/Cloud Backend
    suspend fun deleteItemFromCloud(itemId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${getCleanDbUrl()}/channels/$itemId.json"
            val request = Request.Builder()
                .url(endpoint)
                .delete()
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Upload entire channel list to Cloud DB (Admin Sync All)
    suspend fun syncAllItemsToCloud(items: List<PlaylistItem>): Boolean = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext true
        try {
            val baseUrl = getCleanDbUrl()
            val chunks = items.chunked(250)
            var allSuccess = true
            for (chunk in chunks) {
                val patchObj = JSONObject()
                chunk.forEach { item ->
                    if (item.id > 0) {
                        patchObj.put(item.id.toString(), itemToJson(item))
                    }
                }
                if (patchObj.length() > 0) {
                    val endpoint = "$baseUrl/channels.json"
                    val body = patchObj.toString().toRequestBody(JSON_MEDIA)
                    val request = Request.Builder()
                        .url(endpoint)
                        .patch(body)
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) allSuccess = false
                }
            }
            allSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Helper Parsers
    private fun parseItemsFromJson(jsonStr: String): List<PlaylistItem> {
        val itemsList = mutableListOf<PlaylistItem>()
        if (jsonStr.isNotBlank() && jsonStr != "null") {
            if (jsonStr.trim().startsWith("{")) {
                val jsonObj = JSONObject(jsonStr)
                val keys = jsonObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val itemObj = jsonObj.optJSONObject(key)
                    if (itemObj != null) itemsList.add(jsonToItem(itemObj))
                }
            } else if (jsonStr.trim().startsWith("[")) {
                val jsonArr = JSONArray(jsonStr)
                for (i in 0 until jsonArr.length()) {
                    val itemObj = jsonArr.optJSONObject(i)
                    if (itemObj != null) itemsList.add(jsonToItem(itemObj))
                }
            }
        }
        return itemsList
    }

    private fun parseAccountsFromJson(jsonStr: String): List<PlaylistAccount> {
        val accList = mutableListOf<PlaylistAccount>()
        if (jsonStr.isNotBlank() && jsonStr != "null") {
            val trimmed = jsonStr.trim()
            if (trimmed.startsWith("{")) {
                val jsonObj = JSONObject(trimmed)
                val keys = jsonObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val accObj = jsonObj.optJSONObject(key)
                    if (accObj != null) accList.add(jsonToAccount(accObj))
                }
            } else if (trimmed.startsWith("[")) {
                val jsonArr = JSONArray(trimmed)
                for (i in 0 until jsonArr.length()) {
                    val accObj = jsonArr.optJSONObject(i)
                    if (accObj != null) accList.add(jsonToAccount(accObj))
                }
            }
        }
        return accList
    }

    private fun parseMarketItemsFromJson(jsonStr: String): List<MarketItem> {
        val list = mutableListOf<MarketItem>()
        if (jsonStr.isNotBlank() && jsonStr != "null") {
            if (jsonStr.trim().startsWith("{")) {
                val jsonObj = JSONObject(jsonStr)
                val keys = jsonObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = jsonObj.optJSONObject(key)
                    if (obj != null) list.add(jsonToMarketItem(obj))
                }
            } else if (jsonStr.trim().startsWith("[")) {
                val jsonArr = JSONArray(jsonStr)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.optJSONObject(i)
                    if (obj != null) list.add(jsonToMarketItem(obj))
                }
            }
        }
        return list
    }

    private fun parseWebsiteItemsFromJson(jsonStr: String): List<WebsiteItem> {
        val list = mutableListOf<WebsiteItem>()
        if (jsonStr.isNotBlank() && jsonStr != "null") {
            if (jsonStr.trim().startsWith("{")) {
                val jsonObj = JSONObject(jsonStr)
                val keys = jsonObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = jsonObj.optJSONObject(key)
                    if (obj != null) list.add(jsonToWebsiteItem(obj))
                }
            } else if (jsonStr.trim().startsWith("[")) {
                val jsonArr = JSONArray(jsonStr)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.optJSONObject(i)
                    if (obj != null) list.add(jsonToWebsiteItem(obj))
                }
            }
        }
        return list
    }

    // Fetch all items from Cloud Backend
    suspend fun fetchCloudItems(): List<PlaylistItem> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${getCleanDbUrl()}/channels.json"
            val request = Request.Builder().url(endpoint).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                return@withContext parseItemsFromJson(jsonStr)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emptyList()
    }

    // High-speed single-request sync of cloud items down into local Room database
    suspend fun syncCloudToLocal(dao: KurdPlayDao, onProgress: (Int) -> Unit = {}): Int = withContext(Dispatchers.IO) {
        onProgress(15)
        var totalSynced = 0
        try {
            val endpoint = "${getCleanDbUrl()}/.json"
            val request = Request.Builder().url(endpoint).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                if (jsonStr.isNotBlank() && jsonStr != "null" && jsonStr.trim().startsWith("{")) {
                    val rootObj = JSONObject(jsonStr)
                    onProgress(45)

                    val channelsVal = rootObj.opt("channels")
                    if (channelsVal != null) {
                        val cloudItems = parseItemsFromJson(channelsVal.toString())
                        if (cloudItems.isNotEmpty()) {
                            dao.insertItems(cloudItems)
                            totalSynced += cloudItems.size
                        }
                    }

                    val accountsVal = rootObj.opt("accounts")
                    if (accountsVal != null) {
                        val cloudAccounts = parseAccountsFromJson(accountsVal.toString())
                        if (cloudAccounts.isNotEmpty()) {
                            dao.insertAccounts(cloudAccounts)
                        }
                    }

                    val marketVal = rootObj.opt("market")
                    if (marketVal != null) {
                        val cloudMarket = parseMarketItemsFromJson(marketVal.toString())
                        if (cloudMarket.isNotEmpty()) {
                            dao.insertMarketItems(cloudMarket)
                            totalSynced += cloudMarket.size
                        }
                    }

                    val websitesVal = rootObj.opt("websites")
                    if (websitesVal != null) {
                        val cloudWebsites = parseWebsiteItemsFromJson(websitesVal.toString())
                        if (cloudWebsites.isNotEmpty()) {
                            dao.insertWebsiteItems(cloudWebsites)
                            totalSynced += cloudWebsites.size
                        }
                    }

                    onProgress(100)
                    return@withContext totalSynced
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback: Parallel asynchronous requests
        onProgress(30)
        coroutineScope {
            val itemsDeferred = async { fetchCloudItems() }
            val accountsDeferred = async { fetchCloudAccounts() }
            val marketDeferred = async { fetchCloudMarketItems() }
            val websitesDeferred = async { fetchCloudWebsiteItems() }

            val cloudItems = itemsDeferred.await()
            val cloudAccounts = accountsDeferred.await()
            val cloudMarket = marketDeferred.await()
            val cloudWebsites = websitesDeferred.await()

            onProgress(75)

            if (cloudItems.isNotEmpty()) {
                dao.insertItems(cloudItems)
                totalSynced += cloudItems.size
            }
            if (cloudAccounts.isNotEmpty()) dao.insertAccounts(cloudAccounts)
            if (cloudMarket.isNotEmpty()) {
                dao.insertMarketItems(cloudMarket)
                totalSynced += cloudMarket.size
            }
            if (cloudWebsites.isNotEmpty()) {
                dao.insertWebsiteItems(cloudWebsites)
                totalSynced += cloudWebsites.size
            }
        }

        onProgress(100)
        totalSynced
    }

    // Market Items Cloud Sync
    private fun marketItemToJson(item: MarketItem): JSONObject {
        return JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("price", item.price)
            put("category", item.category)
            put("imageUrl", item.imageUrl)
            put("additionalImages", item.additionalImages)
            put("videoUrl", item.videoUrl)
            put("description", item.description)
            put("contactPhone", item.contactPhone)
        }
    }

    private fun jsonToMarketItem(json: JSONObject): MarketItem {
        val rawId = json.optInt("id", 0)
        val title = json.optString("title", "")
        val price = json.optString("price", "")
        val computedId = if (rawId > 0) rawId else kotlin.math.abs((title + price).hashCode()).coerceAtLeast(1)

        return MarketItem(
            id = computedId,
            title = title,
            price = price,
            category = json.optString("category", "Subscriptions"),
            imageUrl = json.optString("imageUrl", ""),
            additionalImages = json.optString("additionalImages", ""),
            videoUrl = json.optString("videoUrl", ""),
            description = json.optString("description", ""),
            contactPhone = json.optString("contactPhone", "07501191987")
        )
    }

    suspend fun pushMarketItemToCloud(item: MarketItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${getCleanDbUrl()}/market/${item.id}.json"
            val body = marketItemToJson(item).toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder().url(endpoint).put(body).build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteMarketItemFromCloud(itemId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${getCleanDbUrl()}/market/$itemId.json"
            val request = Request.Builder().url(endpoint).delete().build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchCloudMarketItems(): List<MarketItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MarketItem>()
        try {
            val endpoint = "${getCleanDbUrl()}/market.json"
            val request = Request.Builder().url(endpoint).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                if (jsonStr.isNotBlank() && jsonStr != "null") {
                    if (jsonStr.trim().startsWith("{")) {
                        val jsonObj = JSONObject(jsonStr)
                        val keys = jsonObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = jsonObj.optJSONObject(key)
                            if (obj != null) list.add(jsonToMarketItem(obj))
                        }
                    } else if (jsonStr.trim().startsWith("[")) {
                        val jsonArr = JSONArray(jsonStr)
                        for (i in 0 until jsonArr.length()) {
                            val obj = jsonArr.optJSONObject(i)
                            if (obj != null) list.add(jsonToMarketItem(obj))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    // Website Items Cloud Sync
    private fun websiteItemToJson(item: WebsiteItem): JSONObject {
        return JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("url", item.url)
            put("logoUrl", item.logoUrl)
            put("category", item.category)
            put("description", item.description)
        }
    }

    private fun jsonToWebsiteItem(json: JSONObject): WebsiteItem {
        val rawId = json.optInt("id", 0)
        val url = json.optString("url", "")
        val title = json.optString("title", "")
        val computedId = if (rawId > 0) rawId else kotlin.math.abs((url + title).hashCode()).coerceAtLeast(1)

        return WebsiteItem(
            id = computedId,
            title = title,
            url = url,
            logoUrl = json.optString("logoUrl", ""),
            category = json.optString("category", "Kurdish Portals"),
            description = json.optString("description", "")
        )
    }

    suspend fun pushWebsiteItemToCloud(item: WebsiteItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${getCleanDbUrl()}/websites/${item.id}.json"
            val body = websiteItemToJson(item).toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder().url(endpoint).put(body).build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteWebsiteItemFromCloud(itemId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${getCleanDbUrl()}/websites/$itemId.json"
            val request = Request.Builder().url(endpoint).delete().build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchCloudWebsiteItems(): List<WebsiteItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<WebsiteItem>()
        try {
            val endpoint = "${getCleanDbUrl()}/websites.json"
            val request = Request.Builder().url(endpoint).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                if (jsonStr.isNotBlank() && jsonStr != "null") {
                    if (jsonStr.trim().startsWith("{")) {
                        val jsonObj = JSONObject(jsonStr)
                        val keys = jsonObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val obj = jsonObj.optJSONObject(key)
                            if (obj != null) list.add(jsonToWebsiteItem(obj))
                        }
                    } else if (jsonStr.trim().startsWith("[")) {
                        val jsonArr = JSONArray(jsonStr)
                        for (i in 0 until jsonArr.length()) {
                            val obj = jsonArr.optJSONObject(i)
                            if (obj != null) list.add(jsonToWebsiteItem(obj))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    // App Config Cloud Sync (Banners, Support Phone, Ticker, Preroll, Market Cover)
    suspend fun pushConfigToCloud(configObj: JSONObject): Boolean = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${getCleanDbUrl()}/config.json"
            val body = configObj.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder().url(endpoint).put(body).build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchCloudConfig(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val endpoint = "${getCleanDbUrl()}/config.json"
            val request = Request.Builder().url(endpoint).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                if (jsonStr.isNotBlank() && jsonStr != "null" && jsonStr.trim().startsWith("{")) {
                    return@withContext JSONObject(jsonStr)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    // Convert PlaylistAccount to JSON
    private fun accountToJson(acc: PlaylistAccount): JSONObject {
        return JSONObject().apply {
            put("id", acc.id)
            put("name", acc.name)
            put("type", acc.type)
            put("host", acc.host)
            put("username", acc.username)
            put("password", acc.password)
            put("m3uUrl", acc.m3uUrl)
            put("isActive", acc.isActive)
            put("addedTimestamp", acc.addedTimestamp)
            put("subscriptionPlan", acc.subscriptionPlan)
            put("expiryTimestamp", acc.expiryTimestamp)
            put("customerPhone", acc.customerPhone)
            put("maxDevices", acc.maxDevices)
            put("boundDeviceId", acc.boundDeviceId)
        }
    }

    private fun jsonToAccount(json: JSONObject): PlaylistAccount {
        val nameVal = if (json.has("accountName") && json.optString("accountName").isNotBlank()) {
            json.optString("accountName")
        } else {
            json.optString("name", "KurdPlay Account")
        }
        val rawType = json.optString("type", "XTREAM").uppercase()
        val normalizedType = if (rawType == "VIP") "SUBSCRIBER" else rawType

        val planVal = if (json.has("subscriptionPlan") && json.optString("subscriptionPlan").isNotBlank()) {
            json.optString("subscriptionPlan")
        } else {
            json.optString("planType", "1 ساڵ")
        }

        val rawId = json.optInt("id", 0)
        val userVal = json.optString("username", "")
        val hostVal = json.optString("host", "")
        val computedId = if (rawId > 0) rawId else kotlin.math.abs((userVal + hostVal + nameVal).hashCode()).coerceAtLeast(1)

        return PlaylistAccount(
            id = computedId,
            name = nameVal,
            type = normalizedType,
            host = hostVal,
            username = userVal,
            password = json.optString("password", ""),
            m3uUrl = json.optString("m3uUrl", ""),
            isActive = json.optBoolean("isActive", true),
            addedTimestamp = json.optLong("addedTimestamp", System.currentTimeMillis()),
            subscriptionPlan = planVal,
            expiryTimestamp = json.optLong("expiryTimestamp", System.currentTimeMillis() + 365L * 24 * 3600 * 1000),
            customerPhone = json.optString("customerPhone", "07501191987"),
            maxDevices = json.optInt("maxDevices", 1),
            boundDeviceId = json.optString("boundDeviceId", "")
        )
    }

    suspend fun pushAccountToCloud(acc: PlaylistAccount): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = if (acc.username.isNotBlank()) acc.username.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_") else "acc_${acc.id}"
            val endpoint = "${getCleanDbUrl()}/accounts/$key.json"
            val body = accountToJson(acc).toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder().url(endpoint).put(body).build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun pushAllAccountsToCloud(accounts: List<PlaylistAccount>): Boolean = withContext(Dispatchers.IO) {
        if (accounts.isEmpty()) return@withContext true
        try {
            val rootObj = JSONObject()
            accounts.forEach { acc ->
                val key = if (acc.username.isNotBlank()) acc.username.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_") else "acc_${acc.id}"
                rootObj.put(key, accountToJson(acc))
            }
            val endpoint = "${getCleanDbUrl()}/accounts.json"
            val body = rootObj.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder().url(endpoint).put(body).build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun pushAllMarketItemsToCloud(items: List<MarketItem>): Boolean = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext true
        try {
            val rootObj = JSONObject()
            items.forEach { m ->
                rootObj.put(m.id.toString(), marketItemToJson(m))
            }
            val endpoint = "${getCleanDbUrl()}/market.json"
            val body = rootObj.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder().url(endpoint).put(body).build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun pushAllWebsiteItemsToCloud(items: List<WebsiteItem>): Boolean = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext true
        try {
            val rootObj = JSONObject()
            items.forEach { w ->
                rootObj.put(w.id.toString(), websiteItemToJson(w))
            }
            val endpoint = "${getCleanDbUrl()}/websites.json"
            val body = rootObj.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder().url(endpoint).put(body).build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteAccountFromCloud(acc: PlaylistAccount): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = if (acc.username.isNotBlank()) acc.username.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_") else "acc_${acc.id}"
            val endpoint = "${getCleanDbUrl()}/accounts/$key.json"
            val request = Request.Builder().url(endpoint).delete().build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchCloudAccounts(): List<PlaylistAccount> = withContext(Dispatchers.IO) {
        val accList = mutableListOf<PlaylistAccount>()
        try {
            val endpoint = "${getCleanDbUrl()}/accounts.json"
            val request = Request.Builder().url(endpoint).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                if (jsonStr.isNotBlank() && jsonStr != "null") {
                    if (jsonStr.trim().startsWith("{")) {
                        val jsonObj = JSONObject(jsonStr)
                        val keys = jsonObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val accObj = jsonObj.optJSONObject(key)
                            if (accObj != null) {
                                accList.add(jsonToAccount(accObj))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        accList
    }
}
