package com.earthmax.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects for Event API operations
 */

data class EventResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("time")
    val time: String,
    @SerializedName("location")
    val location: String,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("organizer_id")
    val organizerId: String,
    @SerializedName("organizer_name")
    val organizerName: String,
    @SerializedName("max_participants")
    val maxParticipants: Int? = null,
    @SerializedName("current_participants")
    val currentParticipants: Int = 0,
    @SerializedName("is_featured")
    val isFeatured: Boolean = false,
    @SerializedName("tags")
    val tags: List<String> = emptyList(),
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class CreateEventRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("category")
    val category: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("time")
    val time: String,
    @SerializedName("location")
    val location: String,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("max_participants")
    val maxParticipants: Int? = null,
    @SerializedName("tags")
    val tags: List<String> = emptyList()
)

data class UpdateEventRequest(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("category")
    val category: String? = null,
    @SerializedName("date")
    val date: String? = null,
    @SerializedName("time")
    val time: String? = null,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("latitude")
    val latitude: Double? = null,
    @SerializedName("longitude")
    val longitude: Double? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("max_participants")
    val maxParticipants: Int? = null,
    @SerializedName("is_featured")
    val isFeatured: Boolean? = null,
    @SerializedName("tags")
    val tags: List<String>? = null
)