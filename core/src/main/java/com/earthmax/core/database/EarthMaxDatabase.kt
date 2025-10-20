package com.earthmax.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.earthmax.core.sync.OfflineChange
import com.earthmax.core.sync.OfflineChangeDao

@Database(
    entities = [
        OfflineChange::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class EarthMaxDatabase : RoomDatabase() {
    
    abstract fun offlineChangeDao(): OfflineChangeDao
    
    companion object {
        @Volatile
        private var INSTANCE: EarthMaxDatabase? = null
        
        fun getDatabase(context: Context): EarthMaxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EarthMaxDatabase::class.java,
                    "earthmax_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}