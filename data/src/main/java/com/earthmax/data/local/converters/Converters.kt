package com.earthmax.data.local.converters

import androidx.room.TypeConverter
import com.earthmax.core.models.Badge
import com.earthmax.core.models.EventCategory
import com.earthmax.core.models.TodoItem
import com.earthmax.core.models.UserPreferences
import com.earthmax.core.models.EnvironmentalImpact
import com.earthmax.core.models.ProfileCustomization
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromListString(list: List<String>): String {
        return gson.toJson(list)
    }
    
    @TypeConverter
    fun fromTodoItemList(value: String): List<TodoItem> {
        val listType = object : TypeToken<List<TodoItem>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromListTodoItem(list: List<TodoItem>): String {
        return gson.toJson(list)
    }
    
    @TypeConverter
    fun fromBadgeList(value: String): List<Badge> {
        val listType = object : TypeToken<List<Badge>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromListBadge(list: List<Badge>): String {
        return gson.toJson(list)
    }
    
    @TypeConverter
    fun fromUserPreferences(value: String): UserPreferences {
        return gson.fromJson(value, UserPreferences::class.java)
    }
    
    @TypeConverter
    fun fromUserPreferencesToString(preferences: UserPreferences): String {
        return gson.toJson(preferences)
    }
    
    @TypeConverter
    fun fromEnvironmentalImpact(value: String): EnvironmentalImpact {
        return gson.fromJson(value, EnvironmentalImpact::class.java)
    }
    
    @TypeConverter
    fun fromEnvironmentalImpactToString(impact: EnvironmentalImpact): String {
        return gson.toJson(impact)
    }
    
    @TypeConverter
    fun fromProfileCustomization(value: String): ProfileCustomization {
        return gson.fromJson(value, ProfileCustomization::class.java)
    }
    
    @TypeConverter
    fun fromProfileCustomizationToString(customization: ProfileCustomization): String {
        return gson.toJson(customization)
    }
    
    @TypeConverter
    fun fromEventCategory(category: EventCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toEventCategory(category: String): EventCategory {
        return EventCategory.valueOf(category)
    }
}