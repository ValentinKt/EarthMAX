package com.earthmax.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects for User API operations
 */

data class UserResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("profile_image_url")
    val profileImageUrl: String,
    @SerializedName("bio")
    val bio: String? = null,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("date_of_birth")
    val dateOfBirth: String? = null,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class CreateUserRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("profile_image_url")
    val profileImageUrl: String? = null,
    @SerializedName("bio")
    val bio: String? = null,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("date_of_birth")
    val dateOfBirth: String? = null
)

data class UpdateUserRequest(
    @SerializedName("display_name")
    val displayName: String? = null,
    @SerializedName("profile_image_url")
    val profileImageUrl: String? = null,
    @SerializedName("bio")
    val bio: String? = null,
    @SerializedName("location")
    val location: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("date_of_birth")
    val dateOfBirth: String? = null
)

data class ApiError(
    @SerializedName("error")
    val error: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("code")
    val code: Int
)