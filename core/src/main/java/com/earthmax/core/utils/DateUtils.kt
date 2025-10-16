package com.earthmax.core.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    
    init {
        fullDateTimeFormat.timeZone = TimeZone.getTimeZone("UTC")
    }
    
    fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }
    
    fun formatTime(date: Date): String {
        return timeFormat.format(date)
    }
    
    fun formatDateTime(date: Date): String {
        return dateTimeFormat.format(date)
    }
    
    fun formatFullDateTime(date: Date): String {
        return fullDateTimeFormat.format(date)
    }
    
    fun parseFullDateTime(dateString: String): Date? {
        return try {
            fullDateTimeFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getRelativeTimeString(date: Date): String {
        val now = Date()
        val diffInMillis = now.time - date.time
        
        return when {
            diffInMillis < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diffInMillis < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
                "${minutes}m ago"
            }
            diffInMillis < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
                "${hours}h ago"
            }
            diffInMillis < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                "${days}d ago"
            }
            else -> formatDate(date)
        }
    }
    
    fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val targetDate = Calendar.getInstance().apply { time = date }
        
        return today.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == targetDate.get(Calendar.DAY_OF_YEAR)
    }
    
    fun isTomorrow(date: Date): Boolean {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val targetDate = Calendar.getInstance().apply { time = date }
        
        return tomorrow.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == targetDate.get(Calendar.DAY_OF_YEAR)
    }
    
    fun getTimeUntilEvent(eventDate: Date): String {
        val now = Date()
        val diffInMillis = eventDate.time - now.time
        
        if (diffInMillis <= 0) {
            return "Event has started"
        }
        
        val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60
        
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}