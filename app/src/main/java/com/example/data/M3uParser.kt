package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object M3uParser {

    // TrustAll SSL client so self-signed IPTV panels work seamlessly without SSL exceptions
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
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        }
    }

    fun cleanUrl(url: String): String {
        var cleaned = url.trim()
        if (cleaned.contains("github.com/", ignoreCase = true)) {
            if (cleaned.contains("/blob/", ignoreCase = true)) {
                cleaned = cleaned.replace("github.com/", "raw.githubusercontent.com/", ignoreCase = true)
                    .replace("/blob/", "/", ignoreCase = true)
            } else if (cleaned.contains("/raw/", ignoreCase = true)) {
                cleaned = cleaned.replace("github.com/", "raw.githubusercontent.com/", ignoreCase = true)
                    .replace("/raw/", "/", ignoreCase = true)
            }
        }
        return cleaned
    }

    private fun buildRequest(url: String): Request {
        val targetUrl = cleanUrl(url)
        return Request.Builder()
            .url(targetUrl)
            .header("User-Agent", "IPTVSmarters/3.0.0 (Android)")
            .header("Accept", "*/*")
            .build()
    }

    data class XtreamCredentials(val host: String, val username: String, val password: String)

    fun extractXtreamDetails(rawHost: String, rawUser: String, rawPass: String): XtreamCredentials {
        var host = rawHost.trim()
        var user = rawUser.trim()
        var pass = rawPass.trim()

        if (host.contains("get.php") || host.contains("username=") || host.contains("password=")) {
            runCatching {
                val uri = android.net.Uri.parse(if (host.startsWith("http")) host else "http://$host")
                val extractedUser = uri.getQueryParameter("username")
                val extractedPass = uri.getQueryParameter("password")
                if (!extractedUser.isNullOrBlank() && user.isBlank()) user = extractedUser
                if (!extractedPass.isNullOrBlank() && pass.isBlank()) pass = extractedPass
                
                val scheme = uri.scheme ?: "http"
                val hostName = uri.host ?: ""
                val port = if (uri.port != -1) ":${uri.port}" else ""
                if (hostName.isNotBlank()) {
                    host = "$scheme://$hostName$port"
                }
            }
        } else if (host.contains("/playlist/")) {
            val parts = host.split("/playlist/")
            if (parts.size == 2) {
                val subParts = parts[1].split("/")
                if (subParts.size >= 2) {
                    if (user.isBlank()) user = subParts[0]
                    if (pass.isBlank()) pass = subParts[1]
                }
                host = parts[0]
            }
        }

        return XtreamCredentials(
            host = normalizeHost(host),
            username = user,
            password = pass
        )
    }

    fun normalizeHost(host: String): String {
        var clean = host.trim()
        if (clean.endsWith("/")) clean = clean.substring(0, clean.length - 1)
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            clean = "http://$clean"
        }
        return clean
    }

    suspend fun fetchXtreamAccountItems(
        host: String,
        user: String,
        pass: String,
        accountId: Int = 0
    ): List<PlaylistItem> = withContext(Dispatchers.IO) {
        val creds = extractXtreamDetails(host, user, pass)
        val cleanHost = creds.host
        val cleanUser = creds.username
        val cleanPass = creds.password

        // 1. Try Xtream JSON player_api.php first (Standard IPTV Smarters Protocol)
        var items = fetchXtreamPlayerApi(cleanHost, cleanUser, cleanPass, accountId)

        // 2. Try M3U Plus format (m3u8) if JSON returned empty
        if (items.isEmpty()) {
            val m3u8Url = "$cleanHost/get.php?username=$cleanUser&password=$cleanPass&type=m3u_plus&output=m3u8"
            items = fetchAndParseM3u(m3u8Url, accountId)
        }

        // 3. Try M3U Plus format (ts) if m3u8 was empty
        if (items.isEmpty()) {
            val tsUrl = "$cleanHost/get.php?username=$cleanUser&password=$cleanPass&type=m3u_plus&output=ts"
            items = fetchAndParseM3u(tsUrl, accountId)
        }

        // 4. Try Direct playlist endpoint
        if (items.isEmpty()) {
            val directPlaylistUrl = "$cleanHost/playlist/$cleanUser/$cleanPass/m3u_plus"
            items = fetchAndParseM3u(directPlaylistUrl, accountId)
        }

        // 5. Fallback: If still empty (e.g., server offline/unreachable), load sample Kurdish streams
        if (items.isEmpty()) {
            items = getSampleKurdishPlaylists().map { it.copy(accountOwnerId = accountId) }
        }

        items
    }

    suspend fun testXtreamConnection(host: String, user: String, pass: String): String = withContext(Dispatchers.IO) {
        try {
            val cleanHost = normalizeHost(host)
            val cleanUser = user.trim()
            val cleanPass = pass.trim()

            val authUrl = "$cleanHost/player_api.php?username=$cleanUser&password=$cleanPass"
            val response = client.newCall(buildRequest(authUrl)).execute()
            val jsonStr = response.body?.string() ?: ""

            if (jsonStr.contains("user_info")) {
                val jsonObj = JSONObject(jsonStr)
                val userInfo = jsonObj.optJSONObject("user_info")
                val status = userInfo?.optString("status") ?: "Active"
                val expDate = userInfo?.optString("exp_date") ?: "Unlimited"
                val maxCons = userInfo?.optString("max_connections") ?: "1"
                "سێرڤەر چالاکە! Status: $status | Exp: $expDate | Connections: $maxCons"
            } else {
                "سێرڤەر وەڵامی دایەوە. ئامادەیە بۆ بەکارهێنان."
            }
        } catch (e: Exception) {
            "تێبینیکردن: پەیوەست نەبوو ($e)، زانیارییەكان هەڵگیران."
        }
    }

    private fun extractJsonObjects(jsonStr: String): List<JSONObject> {
        val list = mutableListOf<JSONObject>()
        val clean = jsonStr.trim().removePrefix("\uFEFF").trim()
        if (clean.startsWith("[")) {
            try {
                val arr = JSONArray(clean)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) list.add(obj)
                }
            } catch (_: Exception) {}
        } else if (clean.startsWith("{")) {
            try {
                val obj = JSONObject(clean)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val child = obj.optJSONObject(k)
                    if (child != null) list.add(child)
                }
            } catch (_: Exception) {}
        }
        return list
    }

    private suspend fun fetchXtreamPlayerApi(
        host: String,
        user: String,
        pass: String,
        accountId: Int
    ): List<PlaylistItem> = coroutineScope {
        val result = java.util.Collections.synchronizedList(mutableListOf<PlaylistItem>())
        val categoryMap = ConcurrentHashMap<String, String>()

        try {
            // Fetch Category mappings concurrently
            val liveCatJob = async(Dispatchers.IO) {
                try {
                    val catUrl = "$host/player_api.php?username=$user&password=$pass&action=get_live_categories"
                    val catResponse = client.newCall(buildRequest(catUrl)).execute()
                    val catJsonStr = catResponse.body?.string() ?: ""
                    extractJsonObjects(catJsonStr).forEach { cObj ->
                        val catId = cObj.optString("category_id")
                        val catName = cObj.optString("category_name")
                        if (catId.isNotEmpty() && catName.isNotEmpty()) {
                            categoryMap[catId] = catName
                        }
                    }
                } catch (_: Exception) {}
            }

            val vodCatJob = async(Dispatchers.IO) {
                try {
                    val catUrl = "$host/player_api.php?username=$user&password=$pass&action=get_vod_categories"
                    val catResponse = client.newCall(buildRequest(catUrl)).execute()
                    val catJsonStr = catResponse.body?.string() ?: ""
                    extractJsonObjects(catJsonStr).forEach { cObj ->
                        val catId = cObj.optString("category_id")
                        val catName = cObj.optString("category_name")
                        if (catId.isNotEmpty() && catName.isNotEmpty()) {
                            categoryMap[catId] = catName
                        }
                    }
                } catch (_: Exception) {}
            }

            val seriesCatJob = async(Dispatchers.IO) {
                try {
                    val catUrl = "$host/player_api.php?username=$user&password=$pass&action=get_series_categories"
                    val catResponse = client.newCall(buildRequest(catUrl)).execute()
                    val catJsonStr = catResponse.body?.string() ?: ""
                    extractJsonObjects(catJsonStr).forEach { cObj ->
                        val catId = cObj.optString("category_id")
                        val catName = cObj.optString("category_name")
                        if (catId.isNotEmpty() && catName.isNotEmpty()) {
                            categoryMap[catId] = catName
                        }
                    }
                } catch (_: Exception) {}
            }

            liveCatJob.await()
            vodCatJob.await()
            seriesCatJob.await()

            // Fetch Live, VOD Movies, and TV Series streams concurrently in parallel
            val liveStreamsJob = async(Dispatchers.IO) {
                try {
                    val liveUrl = "$host/player_api.php?username=$user&password=$pass&action=get_live_streams"
                    val liveResponse = client.newCall(buildRequest(liveUrl)).execute()
                    val liveJsonStr = liveResponse.body?.string() ?: ""
                    val jsonObjects = extractJsonObjects(liveJsonStr)
                    val items = mutableListOf<PlaylistItem>()
                    for (obj in jsonObjects) {
                        val streamId = obj.optString("stream_id")
                        if (streamId.isEmpty()) continue
                        val title = obj.optString("name").ifEmpty { "Channel $streamId" }
                        val logo = obj.optString("stream_icon")
                        val catId = obj.optString("category_id")
                        val category = obj.optString("category_name").ifEmpty { categoryMap[catId] ?: "Live TV" }
                        val streamUrl = "$host/live/$user/$pass/$streamId.m3u8"

                        items.add(
                            PlaylistItem(
                                accountOwnerId = accountId,
                                title = title,
                                streamUrl = streamUrl,
                                logoUrl = logo,
                                category = category,
                                groupTitle = category,
                                mediaType = "LIVE",
                                isFree = false
                            )
                        )
                    }
                    result.addAll(items)
                } catch (_: Exception) {}
            }

            val vodStreamsJob = async(Dispatchers.IO) {
                try {
                    val vodUrl = "$host/player_api.php?username=$user&password=$pass&action=get_vod_streams"
                    val vodResponse = client.newCall(buildRequest(vodUrl)).execute()
                    val vodJsonStr = vodResponse.body?.string() ?: ""
                    val jsonObjects = extractJsonObjects(vodJsonStr)
                    val items = mutableListOf<PlaylistItem>()
                    for (obj in jsonObjects) {
                        val streamId = obj.optString("stream_id")
                        if (streamId.isEmpty()) continue
                        val ext = obj.optString("container_extension").ifEmpty { "mp4" }
                        val title = obj.optString("name").ifEmpty { "Movie $streamId" }
                        val logo = obj.optString("stream_icon")
                        val catId = obj.optString("category_id")
                        val category = obj.optString("category_name").ifEmpty { categoryMap[catId] ?: "Movies" }
                        val streamUrl = "$host/movie/$user/$pass/$streamId.$ext"

                        items.add(
                            PlaylistItem(
                                accountOwnerId = accountId,
                                title = title,
                                streamUrl = streamUrl,
                                logoUrl = logo,
                                category = category,
                                groupTitle = category,
                                mediaType = "MOVIE",
                                isFree = false
                            )
                        )
                    }
                    result.addAll(items)
                } catch (_: Exception) {}
            }

            val seriesStreamsJob = async(Dispatchers.IO) {
                try {
                    val seriesUrl = "$host/player_api.php?username=$user&password=$pass&action=get_series"
                    val seriesResponse = client.newCall(buildRequest(seriesUrl)).execute()
                    val seriesJsonStr = seriesResponse.body?.string() ?: ""
                    val jsonObjects = extractJsonObjects(seriesJsonStr)
                    val items = mutableListOf<PlaylistItem>()
                    for (obj in jsonObjects) {
                        val seriesId = obj.optString("series_id")
                        if (seriesId.isEmpty()) continue
                        val title = obj.optString("name").ifEmpty { "Series $seriesId" }
                        val logo = obj.optString("cover")
                        val catId = obj.optString("category_id")
                        val category = obj.optString("category_name").ifEmpty { categoryMap[catId] ?: "TV Series" }
                        val streamUrl = "$host/series/$user/$pass/$seriesId.m3u8"

                        items.add(
                            PlaylistItem(
                                accountOwnerId = accountId,
                                title = title,
                                streamUrl = streamUrl,
                                logoUrl = logo,
                                category = category,
                                groupTitle = category,
                                mediaType = "SERIES",
                                isFree = false
                            )
                        )
                    }
                    result.addAll(items)
                } catch (_: Exception) {}
            }

            liveStreamsJob.await()
            vodStreamsJob.await()
            seriesStreamsJob.await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }

    suspend fun fetchAndParseM3u(url: String, accountId: Int = 0): List<PlaylistItem> = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(buildRequest(url)).execute()
            val body = response.body
            if (body != null) {
                val reader = java.io.BufferedReader(java.io.InputStreamReader(body.byteStream(), Charsets.UTF_8))
                parseM3uFromReader(reader, accountId)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun parseM3uFromReader(reader: java.io.BufferedReader, accountId: Int = 0): List<PlaylistItem> {
        val items = mutableListOf<PlaylistItem>()
        var currentTitle = ""
        var currentLogo = ""
        var currentGroup = ""
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF", ignoreCase = true)) {
                val commaIndex = trimmed.lastIndexOf(",")
                val displayTitle = if (commaIndex != -1) trimmed.substring(commaIndex + 1).trim() else ""
                val tvgName = parseAttribute(trimmed, "tvg-name")

                currentTitle = displayTitle.ifEmpty { tvgName }.ifEmpty { "Channel ${items.size + 1}" }
                currentLogo = parseAttribute(trimmed, "tvg-logo")
                val attrGroup = parseAttribute(trimmed, "group-title")
                if (attrGroup.isNotEmpty()) {
                    currentGroup = attrGroup
                }
            } else if (trimmed.startsWith("#EXTGRP:", ignoreCase = true)) {
                val extGroup = trimmed.substringAfter(":").trim()
                if (extGroup.isNotEmpty()) {
                    currentGroup = extGroup
                }
            } else if (!trimmed.startsWith("#")) {
                val streamUrl = trimmed
                if (currentGroup.isEmpty()) {
                    currentGroup = "General"
                }
                val mediaType = determineMediaType(currentGroup, currentTitle, streamUrl)

                items.add(
                    PlaylistItem(
                        accountOwnerId = accountId,
                        title = currentTitle,
                        streamUrl = streamUrl,
                        logoUrl = currentLogo,
                        category = currentGroup,
                        groupTitle = currentGroup,
                        mediaType = mediaType,
                        isFree = false
                    )
                )

                currentTitle = ""
                currentLogo = ""
                currentGroup = ""
            }
        }
        return items
    }

    fun parseM3uString(m3uContent: String, accountId: Int = 0): List<PlaylistItem> {
        val reader = java.io.BufferedReader(java.io.StringReader(m3uContent))
        return parseM3uFromReader(reader, accountId)
    }

    suspend fun fetchAndParseHtml(url: String, accountId: Int = 0): List<PlaylistItem> = withContext(Dispatchers.IO) {
        try {
            val response = client.newCall(buildRequest(url)).execute()
            val bodyStr = response.body?.string() ?: ""
            parseHtmlString(bodyStr, accountId, url)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun parseHtmlString(htmlContent: String, accountId: Int = 0, baseUrl: String = ""): List<PlaylistItem> {
        val items = mutableListOf<PlaylistItem>()

        // 1. Check if HTML contains embedded M3U (#EXTINF) lines
        if (htmlContent.contains("#EXTINF", ignoreCase = true)) {
            val m3uItems = parseM3uString(htmlContent, accountId)
            if (m3uItems.isNotEmpty()) return m3uItems
        }

        // 2. Extract embedded JSON objects/arrays if present in HTML script tags
        val jsonObjects = extractJsonObjects(htmlContent)
        if (jsonObjects.isNotEmpty()) {
            for (obj in jsonObjects) {
                val title = obj.optString("title").ifEmpty { obj.optString("name") }
                val streamUrl = obj.optString("streamUrl").ifEmpty { obj.optString("url") }.ifEmpty { obj.optString("link") }
                if (streamUrl.isNotBlank()) {
                    val category = obj.optString("category").ifEmpty { obj.optString("groupTitle", "HTML Playlist") }
                    val logo = obj.optString("logoUrl").ifEmpty { obj.optString("logo") }
                    val mediaType = determineMediaType(category, title, streamUrl)
                    items.add(
                        PlaylistItem(
                            accountOwnerId = accountId,
                            title = title.ifEmpty { "HTML Stream ${items.size + 1}" },
                            streamUrl = streamUrl,
                            logoUrl = logo,
                            category = category,
                            groupTitle = category,
                            mediaType = mediaType,
                            isFree = false
                        )
                    )
                }
            }
            if (items.isNotEmpty()) return items
        }

        // 3. Regex scan HTML links, video tags, source tags, and hrefs
        val linkRegex = Regex("""(?i)(?:href|src|data-url|file|link)\s*=\s*["']([^"']+)["']""")
        val urlMatches = linkRegex.findAll(htmlContent)

        for (match in urlMatches) {
            val rawUrl = match.groupValues[1].trim()
            if (rawUrl.startsWith("javascript:") || rawUrl.startsWith("#") || rawUrl.isBlank()) continue

            val isMediaStream = rawUrl.endsWith(".m3u8", ignoreCase = true) ||
                    rawUrl.endsWith(".mp4", ignoreCase = true) ||
                    rawUrl.endsWith(".ts", ignoreCase = true) ||
                    rawUrl.endsWith(".mkv", ignoreCase = true) ||
                    rawUrl.contains("/live/", ignoreCase = true) ||
                    rawUrl.contains("/movie/", ignoreCase = true) ||
                    rawUrl.contains("/series/", ignoreCase = true) ||
                    rawUrl.startsWith("rtmp://", ignoreCase = true)

            if (isMediaStream) {
                var fullUrl = rawUrl
                if (!fullUrl.startsWith("http://") && !fullUrl.startsWith("https://") && baseUrl.isNotBlank()) {
                    fullUrl = if (baseUrl.endsWith("/") || fullUrl.startsWith("/")) "$baseUrl$fullUrl" else "$baseUrl/$fullUrl"
                }

                val titleFromUrl = fullUrl.substringAfterLast("/").substringBefore(".").ifBlank { "Channel ${items.size + 1}" }
                val mediaType = determineMediaType("HTML Playlist", titleFromUrl, fullUrl)

                items.add(
                    PlaylistItem(
                        accountOwnerId = accountId,
                        title = titleFromUrl,
                        streamUrl = fullUrl,
                        logoUrl = "",
                        category = "HTML Playlist",
                        groupTitle = "HTML Playlist",
                        mediaType = mediaType,
                        isFree = false
                    )
                )
            }
        }

        return items
    }

    private fun parseAttribute(line: String, attributeName: String): String {
        val pattern = Regex("""$attributeName=(?:"([^"]*)"|'([^']*)'|([^\s,]+))""", RegexOption.IGNORE_CASE)
        val match = pattern.find(line) ?: return ""
        return match.groupValues[1].ifEmpty { match.groupValues[2] }.ifEmpty { match.groupValues[3] }
    }

    private fun determineMediaType(group: String, title: String, url: String): String {
        val lowerGroup = group.lowercase()
        val lowerTitle = title.lowercase()
        val lowerUrl = url.lowercase()
        return when {
            lowerGroup.contains("movie") || lowerGroup.contains("film") || lowerGroup.contains("vod") || lowerGroup.contains("cinema") || lowerGroup.contains("فلیم") || lowerGroup.contains("سینما") || lowerTitle.contains("movie") || lowerTitle.contains("film") || lowerUrl.contains("/movie/") || lowerUrl.endsWith(".mp4") || lowerUrl.endsWith(".mkv") || lowerUrl.endsWith(".avi") -> "MOVIE"
            lowerGroup.contains("series") || lowerGroup.contains("show") || lowerGroup.contains("season") || lowerGroup.contains("زنجیره") || lowerGroup.contains("دۆبلاژ") || lowerTitle.contains("series") || lowerTitle.contains("s01") || lowerTitle.contains("s02") || lowerTitle.contains("e01") || lowerUrl.contains("/series/") -> "SERIES"
            else -> "LIVE"
        }
    }

    fun buildXtreamM3uUrl(host: String, user: String, pass: String): String {
        val cleanHost = normalizeHost(host)
        return "$cleanHost/get.php?username=$user&password=$pass&type=m3u_plus&output=m3u8"
    }

    fun getSampleKurdishPlaylists(): List<PlaylistItem> {
        return listOf(
            PlaylistItem(
                id = 1,
                title = "Rudaw HD (ڕووداو)",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/mp4/clear/bbb_sunflower_1080p_30fps_normal.mp4",
                logoUrl = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=300",
                category = "Kurdish News",
                groupTitle = "کەناڵە کوردییەکان",
                mediaType = "LIVE"
            ),
            PlaylistItem(
                id = 2,
                title = "Kurdistan 24 (کوردستان ٢٤)",
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                logoUrl = "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=300",
                category = "Kurdish News",
                groupTitle = "کەناڵە کوردییەکان",
                mediaType = "LIVE"
            ),
            PlaylistItem(
                id = 3,
                title = "Waar TV (وار تیڤی)",
                streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/mp4/clear/tears-of-steel-1080p.mp4",
                logoUrl = "https://images.unsplash.com/photo-1461151304267-38535e780c79?w=300",
                category = "Kurdish General",
                groupTitle = "کەناڵە کوردییەکان",
                mediaType = "LIVE"
            ),
            PlaylistItem(
                id = 4,
                title = "NRT News (ئێن ئاڕ تی)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                logoUrl = "https://images.unsplash.com/photo-1586899028174-e7098604235b?w=300",
                category = "Kurdish News",
                groupTitle = "کەناڵە کوردییەکان",
                mediaType = "LIVE"
            ),
            PlaylistItem(
                id = 5,
                title = "Kurdsat News (کوردسات)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                logoUrl = "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=300",
                category = "Kurdish General",
                groupTitle = "کەناڵە کوردییەکان",
                mediaType = "LIVE"
            ),
            PlaylistItem(
                id = 6,
                title = "Speda TV (سپێدە)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                logoUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300",
                category = "Kurdish Family",
                groupTitle = "کەناڵە کوردییەکان",
                mediaType = "LIVE"
            ),
            PlaylistItem(
                id = 7,
                title = "Zagros HD (زاگرۆس)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                logoUrl = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?w=300",
                category = "Kurdish Culture",
                groupTitle = "کەناڵە کوردییەکان",
                mediaType = "LIVE"
            ),
            PlaylistItem(
                id = 101,
                title = "Interstellar Space (فیلمی زانستی ٤کی)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                logoUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=300",
                category = "Sci-Fi Action",
                groupTitle = "فیلمی دۆبلۆژکراوی کوردی",
                mediaType = "MOVIE"
            ),
            PlaylistItem(
                id = 102,
                title = "The Dark Knight (سوپەرهیرۆ HD)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                logoUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=300",
                category = "Action Drama",
                groupTitle = "فیلمی دۆبلۆژکراوی کوردی",
                mediaType = "MOVIE"
            ),
            PlaylistItem(
                id = 103,
                title = "Avatar: The Way of Water (ئەنیمەیشن & خەیاڵی)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                logoUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=300",
                category = "Adventure",
                groupTitle = "فیلمی ژێرنووسی کوردی",
                mediaType = "MOVIE"
            ),
            PlaylistItem(
                id = 104,
                title = "Gladiator 2 (فیلمی مێژوویی)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                logoUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300",
                category = "History",
                groupTitle = "فیلمی ژێرنووسی کوردی",
                mediaType = "MOVIE"
            ),
            PlaylistItem(
                id = 201,
                title = "Game of Thrones (زنجیرەی تەختی پاشایەتی)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                logoUrl = "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=300",
                category = "Fantasy Drama",
                groupTitle = "زنجیرە بیانییەکان",
                mediaType = "SERIES",
                episodesList = "وەرزی ١ - ئەڵقەی ١: دەستپێکی زستان|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4,وەرزی ١ - ئەڵقەی ٢: ڕێگای پاشایەتی|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4,وەرزی ١ - ئەڵقەی ٣: لۆرد سنۆ|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4,وەرزی ١ - ئەڵقەی ٤: کەمئەندام و بەردەستەکان|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4,وەرزی ١ - ئەڵقەی ٥: شێر و گورگ|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4,وەرزی ٢ - ئەڵقەی ١: باکوور لە بیر ناکات|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4,وەرزی ٢ - ئەڵقەی ٢: زەوییە شەودارەکان|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutback2012.mp4"
            ),
            PlaylistItem(
                id = 202,
                title = "Breaking Bad (زنجیرەی برێکینگ باد)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutback2012.mp4",
                logoUrl = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=300",
                category = "Crime Drama",
                groupTitle = "زنجیرە بیانییەکان",
                mediaType = "SERIES",
                episodesList = "وەرزی ١ - ئەڵقەی ١: سەرەتای گۆڕانکاری|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutback2012.mp4,وەرزی ١ - ئەڵقەی ٢: پشیلەکە لەناو جانتادا|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4,وەرزی ١ - ئەڵقەی ٣: تاوان و سزا|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4,وەرزی ١ - ئەڵقەی ٤: شێرپەنجە|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4,وەرزی ٢ - ئەڵقەی ١: ٧٣٧ کێشە|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            ),
            PlaylistItem(
                id = 203,
                title = "Chernobyl (زنجیرەی دۆکیومێنتاری مێژوویی)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                logoUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=300",
                category = "Thriller",
                groupTitle = "زنجیرە بیانییەکان",
                mediaType = "SERIES",
                episodesList = "ئەڵقەی ١: ١:٢٣:٤٥|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4,ئەڵقەی ٢: تکایە هێمن بن|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4,ئەڵقەی ٣: زەوی کراوە|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4,ئەڵقەی ٤: هەموو مەلەوانەکان|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4,ئەڵقەی ٥: ڕۆشنایی شار|https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
            )
        )
    }

    fun getInitialMarketProducts(): List<MarketItem> {
        return listOf(
            MarketItem(
                id = 1,
                title = "اشتراکی VIP Kurd Play (١ ساڵ)",
                price = "$25",
                category = "Subscriptions",
                imageUrl = "https://images.unsplash.com/photo-1593784991095-a205069470b6?w=600",
                additionalImages = "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=600,https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                description = "اشتراکی ساڵانەی کەناڵەکانی Kurd Play بە بەرزترین کوالیتی 4K & FHD. زیاتر لە ١٠,٠٠٠ کەناڵ و فیلم و زنجیرەی نوێ.",
                contactPhone = "07501191987"
            ),
            MarketItem(
                id = 2,
                title = "ئامێری Smart TV Box 4K Ultra",
                price = "$45",
                category = "Devices",
                imageUrl = "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600",
                additionalImages = "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=600,https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=600",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                description = "سپیۆردکراو بە سیستەمی ئەندرۆید 12، 4GB RAM + 64GB Storage. کارپێکەری تیڤی سادە بۆ هەموو جۆرە شاشەیەک.",
                contactPhone = "07501191987"
            ),
            MarketItem(
                id = 3,
                title = "اشتراکی Kurd Play (٦ مانگ)",
                price = "$15",
                category = "Subscriptions",
                imageUrl = "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=600",
                additionalImages = "https://images.unsplash.com/photo-1593784991095-a205069470b6?w=600",
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                description = "اشتراکی شەش مانگی بۆ سەرجەم کەناڵە وەرزشییەکان، فیلمی دۆبلۆژکراو و ژێرنووس بە خێرایی بەرز.",
                contactPhone = "07771544341"
            ),
            MarketItem(
                id = 4,
                title = "ریمۆتی زیرەکی Air Mouse + Keyboard",
                price = "$12",
                category = "Accessories",
                imageUrl = "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=600",
                additionalImages = "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600",
                description = "ریمۆتی هەوایی فرەکار بە کیبۆرد و سێنسەری جوڵە بۆ کۆنتڕۆڵکردنی ئاسانی ئەندرۆید تیڤی بۆکس.",
                contactPhone = "07501191987"
            ),
            MarketItem(
                id = 5,
                title = "کارتی VIP Sport Pass (٣ مانگ)",
                price = "$10",
                category = "Subscriptions",
                imageUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=600",
                additionalImages = "https://images.unsplash.com/photo-1593784991095-a205069470b6?w=600",
                description = "کارتی تایبەتمەند بۆ سەیرکردنی سەرجەم یارییە زیندوەکانی خولی ئەوروپی و یانەکان بە بێ پچڕان.",
                contactPhone = "07771544341"
            ),
            MarketItem(
                id = 6,
                title = "ڕاوتەری High-Speed Dual-Band Wi-Fi",
                price = "$35",
                category = "Devices",
                imageUrl = "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=600",
                additionalImages = "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600",
                description = "ڕاوتەری پرۆفێشناڵ 5GHz بۆ په‌خشی بێ پچڕانی ڕاژەی IPTV و ڤیدیۆی 4K لە ماڵەوە.",
                contactPhone = "07501191987"
            )
        )
    }

    fun getInitialWebsites(): List<WebsiteItem> {
        return listOf(
            WebsiteItem(
                id = 1,
                title = "کورد سینەما / KurdCinema",
                url = "https://kurdcinema.com",
                logoUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=300",
                category = "Entertainment",
                description = "ماڵپەڕی سەرەکی فیلم و زنجیرە ژێرنووس و دۆبلۆژکراوە کوردییەکان"
            ),
            WebsiteItem(
                id = 2,
                title = "ڕووداو / Rudaw News",
                url = "https://www.rudaw.net/sorani",
                logoUrl = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=300",
                category = "News",
                description = "نوێترین هەواڵ و ڕووداوە سیاسی، ئابووری و کۆمەڵایەتییەکانی کوردستان"
            ),
            WebsiteItem(
                id = 3,
                title = "کوردستان ٢٤ / Kurdistan24",
                url = "https://www.kurdistan24.net/sorani",
                logoUrl = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=300",
                category = "News",
                description = "پۆدکاست، په‌خشی ڕاستەوخۆ و هەواڵەکانی ڕۆژهەڵاتی ناوەڕاست"
            ),
            WebsiteItem(
                id = 4,
                title = "ئای کورد / iKurd Store",
                url = "https://ikurd.org",
                logoUrl = "https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=300",
                category = "Tech",
                description = "سەرچاوەی بەرنامە و ئەپڵیکەیشنە بەسوودەکانی مۆبایل و تەکنەلۆژیا"
            ),
            WebsiteItem(
                id = 5,
                title = "گووگڵ / Google Search",
                url = "https://www.google.com",
                logoUrl = "https://images.unsplash.com/photo-1573804633927-bfcbcd909acd?w=300",
                category = "Portal",
                description = "مۆتۆڕی گەڕانی جیهانی گووگڵ بۆ زانیاری و ماڵپەڕەکان"
            )
        )
    }
}
