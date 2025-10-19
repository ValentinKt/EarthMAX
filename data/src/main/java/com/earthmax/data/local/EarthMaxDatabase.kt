package com.earthmax.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.earthmax.data.local.dao.EventDao
import com.earthmax.data.local.dao.MessageDao
import com.earthmax.data.local.dao.UserDao
import com.earthmax.data.local.dao.PerformanceDao
import com.earthmax.data.local.entities.EventEntity
import com.earthmax.data.local.entities.MessageEntity
import com.earthmax.data.local.entities.UserEntity
import com.earthmax.data.local.entity.PerformanceMetricEntity
import com.earthmax.data.local.entity.LogEntryEntity
import com.earthmax.data.local.converters.Converters
import com.earthmax.data.local.migrations.MIGRATION_1_2
import com.earthmax.data.local.migrations.MIGRATION_2_3

@Database(
    entities = [
        EventEntity::class,
        UserEntity::class,
        MessageEntity::class,
        PerformanceMetricEntity::class,
        LogEntryEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EarthMaxDatabase : RoomDatabase() {
    
    abstract fun eventDao(): EventDao
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun performanceDao(): PerformanceDao
    
    companion object {
        const val DATABASE_NAME = "earthmax_database"
        
        fun create(context: Context): EarthMaxDatabase {
            return Room.databaseBuilder(
                context,
                EarthMaxDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        }
    }
}