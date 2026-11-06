package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_items")
data class PlaylistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountOwnerId: Int = 0,
    val title: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val category: String = "General",
    val groupTitle: String = "General",
    val mediaType: String = "LIVE", // "LIVE", "MOVIE", "SERIES"
    val isFavorite: Boolean = false,
    val lastWatched: Long = 0L,
    val displayOrder: Int = 0, // Order position for custom reordering
    val isFree: Boolean = false, // false = VIP / Paid (تایبەت), true = FREE (بەلاش)
    val episodesList: String = "" // Optional custom episodes data (Title|Url,Title|Url...)
)

@Entity(tableName = "playlist_accounts")
data class PlaylistAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "XTREAM" or "M3U"
    val host: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val isActive: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val subscriptionPlan: String = "1 Year", // "7 Days", "6 Months", "1 Year"
    val expiryTimestamp: Long = System.currentTimeMillis() + (365L * 24 * 3600 * 1000), // Expiration time
    val customerPhone: String = "07501191987", // Customer contact phone
    val maxDevices: Int = 1, // 1 Device limit
    val boundDeviceId: String = "Device-Android-01" // Single device lock ID
)

@Entity(tableName = "market_items")
data class MarketItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val price: String,
    val category: String = "Subscriptions", // "Subscriptions", "Devices", "Accessories"
    val imageUrl: String = "",
    val additionalImages: String = "", // Comma-separated extra image URLs or local URIs
    val videoUrl: String = "", // Video URL link
    val description: String = "",
    val contactPhone: String = "07501191987"
)

@Entity(tableName = "website_items")
data class WebsiteItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val logoUrl: String = "",
    val category: String = "Kurdish Portals",
    val description: String = ""
)

