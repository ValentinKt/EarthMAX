package com.earthmax.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.earthmax.data.local.dao.EventDao
import com.earthmax.data.local.dao.UserDao
import com.earthmax.data.local.entities.EventEntity
import com.earthmax.data.local.entities.UserEntity
import com.earthmax.data.local.converters.Converters

@Database(
    entities = [
        EventEntity::class,
        UserEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EarthMaxDatabase : RoomDatabase() {
    
    abstract fun eventDao(): EventDao
    abstract fun userDao(): UserDao
    
    companion object {
        const val DATABASE_NAME = "earthmax_database"
        
        fun create(context: Context): EarthMaxDatabase {
            return Room.databaseBuilder(
                context,
                EarthMaxDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}