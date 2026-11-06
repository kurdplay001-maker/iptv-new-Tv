package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PlaylistItem::class, PlaylistAccount::class, MarketItem::class, WebsiteItem::class],
    version = 7,
    exportSchema = false
)
abstract class KurdPlayDatabase : RoomDatabase() {
    abstract fun kurdPlayDao(): KurdPlayDao

    companion object {
        @Volatile
        private var INSTANCE: KurdPlayDatabase? = null

        fun getDatabase(context: Context): KurdPlayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KurdPlayDatabase::class.java,
                    "kurdplay_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
