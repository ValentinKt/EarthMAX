package com.earthmax.core.database

import androidx.room.TypeConverter
import com.earthmax.core.sync.SyncOperationType
import com.earthmax.core.sync.SyncPriority
import com.earthmax.core.sync.OfflineChangeStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DatabaseConverters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringMap(value: Map<String, Any?>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toStringMap(value: String?): Map<String, Any?>? {
        return value?.let {
            val type: Type = object : TypeToken<Map<String, Any?>>() {}.type
            gson.fromJson(it, type)
        }
    }
    
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
    
    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
    }
    
    @TypeConverter
    fun fromSyncOperationType(value: SyncOperationType?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toSyncOperationType(value: String?): SyncOperationType? {
        return value?.let { SyncOperationType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromSyncPriority(value: SyncPriority?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toSyncPriority(value: String?): SyncPriority? {
        return value?.let { SyncPriority.valueOf(it) }
    }
    
    @TypeConverter
    fun fromOfflineChangeStatus(value: OfflineChangeStatus?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toOfflineChangeStatus(value: String?): OfflineChangeStatus? {
        return value?.let { OfflineChangeStatus.valueOf(it) }
    }
}