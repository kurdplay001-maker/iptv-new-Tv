package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KurdPlayDao {
    // Playlist Items
    @Query("SELECT * FROM playlist_items ORDER BY displayOrder ASC, id ASC")
    fun getAllItems(): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE mediaType = :type ORDER BY displayOrder ASC, id ASC")
    fun getItemsByType(type: String): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE isFavorite = 1 ORDER BY displayOrder ASC, id ASC")
    fun getFavoriteItems(): Flow<List<PlaylistItem>>

    @Query("SELECT * FROM playlist_items WHERE lastWatched > 0 ORDER BY lastWatched DESC LIMIT 20")
    fun getRecentItems(): Flow<List<PlaylistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PlaylistItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PlaylistItem): Long

    @Update
    suspend fun updateItem(item: PlaylistItem)

    @Delete
    suspend fun deleteItem(item: PlaylistItem)

    @Query("DELETE FROM playlist_items")
    suspend fun deleteAllItems()

    @Query("DELETE FROM playlist_items WHERE accountOwnerId = :accountId")
    suspend fun deleteItemsByAccount(accountId: Int)

    // Accounts
    @Query("SELECT * FROM playlist_accounts ORDER BY id DESC")
    fun getAllAccounts(): Flow<List<PlaylistAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: PlaylistAccount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<PlaylistAccount>)

    @Delete
    suspend fun deleteAccount(account: PlaylistAccount)

    @Query("DELETE FROM playlist_accounts")
    suspend fun deleteAllAccounts()

    @Query("UPDATE playlist_accounts SET isActive = (id = :activeId)")
    suspend fun setActiveAccount(activeId: Int)

    // Market Items
    @Query("SELECT * FROM market_items ORDER BY id ASC")
    fun getAllMarketItems(): Flow<List<MarketItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketItems(items: List<MarketItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketItem(item: MarketItem): Long

    @Delete
    suspend fun deleteMarketItem(item: MarketItem)

    @Query("DELETE FROM market_items")
    suspend fun deleteAllMarketItems()

    // Website Items
    @Query("SELECT * FROM website_items ORDER BY id ASC")
    fun getAllWebsiteItems(): Flow<List<WebsiteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebsiteItems(items: List<WebsiteItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebsiteItem(item: WebsiteItem): Long

    @Delete
    suspend fun deleteWebsiteItem(item: WebsiteItem)

    @Query("DELETE FROM website_items")
    suspend fun deleteAllWebsiteItems()
}
