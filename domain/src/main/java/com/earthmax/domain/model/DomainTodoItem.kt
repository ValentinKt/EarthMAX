package com.earthmax.domain.model

import kotlinx.datetime.Instant

/**
 * Domain model for todo items in the EarthMAX application.
 * This represents the business logic view of a todo item, separate from data layer DTOs.
 */
data class DomainTodoItem(
    val id: String,
    val eventId: String,
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val assignedTo: String? = null,
    val createdBy: String,
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val updatedAt: Instant
)

/**
 * Extension functions for DomainTodoItem
 */
fun DomainTodoItem.isOverdue(): Boolean {
    // For now, we don't have due dates, but this can be extended later
    return false
}

fun DomainTodoItem.canBeEditedBy(userId: String): Boolean {
    return createdBy == userId || assignedTo == userId
}

fun DomainTodoItem.canBeDeletedBy(userId: String): Boolean {
    return createdBy == userId
}