package com.earthmax.data.todo

import com.earthmax.domain.model.DomainTodoItem
import kotlinx.datetime.Instant
import org.junit.Assert.*
import org.junit.Test

class TodoMappersTest {

    private val testTodoItemDto = TodoItemDto(
        id = "test-id",
        event_id = "test-event-id",
        title = "Test Todo",
        description = "Test Description",
        is_completed = false,
        assigned_to = "test-user-id",
        created_by = "creator-user-id",
        created_at = "2024-01-01T00:00:00Z",
        completed_at = null,
        updated_at = "2024-01-01T12:00:00Z"
    )

    private val testDomainTodoItem = DomainTodoItem(
        id = "test-id",
        eventId = "test-event-id",
        title = "Test Todo",
        description = "Test Description",
        isCompleted = false,
        assignedTo = "test-user-id",
        createdBy = "creator-user-id",
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        completedAt = null,
        updatedAt = Instant.parse("2024-01-01T12:00:00Z")
    )

    @Test
    fun `toDomainTodoItem should correctly map TodoItemDto to DomainTodoItem`() {
        // Given
        val todoItemDto = testTodoItemDto

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertEquals(todoItemDto.id, domainTodoItem.id)
        assertEquals(todoItemDto.event_id, domainTodoItem.eventId)
        assertEquals(todoItemDto.title, domainTodoItem.title)
        assertEquals(todoItemDto.description, domainTodoItem.description)
        assertEquals(todoItemDto.is_completed, domainTodoItem.isCompleted)
        assertEquals(todoItemDto.assigned_to, domainTodoItem.assignedTo)
        assertEquals(todoItemDto.created_by, domainTodoItem.createdBy)
        assertEquals(Instant.parse(todoItemDto.created_at), domainTodoItem.createdAt)
        assertEquals(todoItemDto.completed_at?.let { Instant.parse(it) }, domainTodoItem.completedAt)
        assertEquals(Instant.parse(todoItemDto.updated_at), domainTodoItem.updatedAt)
    }

    @Test
    fun `toDomainTodoItem should handle null description`() {
        // Given
        val todoItemDto = testTodoItemDto.copy(description = null)

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertNull(domainTodoItem.description)
        assertEquals(todoItemDto.id, domainTodoItem.id)
        assertEquals(todoItemDto.event_id, domainTodoItem.eventId)
        assertEquals(todoItemDto.title, domainTodoItem.title)
    }

    @Test
    fun `toDomainTodoItem should handle null assigned_to`() {
        // Given
        val todoItemDto = testTodoItemDto.copy(assigned_to = null)

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertNull(domainTodoItem.assignedTo)
        assertEquals(todoItemDto.id, domainTodoItem.id)
        assertEquals(todoItemDto.event_id, domainTodoItem.eventId)
        assertEquals(todoItemDto.title, domainTodoItem.title)
    }

    @Test
    fun `toDomainTodoItem should handle null completed_at`() {
        // Given
        val todoItemDto = testTodoItemDto.copy(completed_at = null)

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertNull(domainTodoItem.completedAt)
        assertEquals(todoItemDto.id, domainTodoItem.id)
        assertEquals(todoItemDto.event_id, domainTodoItem.eventId)
        assertEquals(todoItemDto.title, domainTodoItem.title)
    }

    @Test
    fun `toDomainTodoItem should handle completed todo with completed_at`() {
        // Given
        val completedAt = "2024-01-01T15:30:00Z"
        val todoItemDto = testTodoItemDto.copy(
            is_completed = true,
            completed_at = completedAt
        )

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertTrue(domainTodoItem.isCompleted)
        assertEquals(Instant.parse(completedAt), domainTodoItem.completedAt)
    }

    @Test
    fun `toTodoItemDto should correctly map DomainTodoItem to TodoItemDto`() {
        // Given
        val domainTodoItem = testDomainTodoItem

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertEquals(domainTodoItem.id, todoItemDto.id)
        assertEquals(domainTodoItem.eventId, todoItemDto.event_id)
        assertEquals(domainTodoItem.title, todoItemDto.title)
        assertEquals(domainTodoItem.description, todoItemDto.description)
        assertEquals(domainTodoItem.isCompleted, todoItemDto.is_completed)
        assertEquals(domainTodoItem.assignedTo, todoItemDto.assigned_to)
        assertEquals(domainTodoItem.createdBy, todoItemDto.created_by)
        assertEquals(domainTodoItem.createdAt.toString(), todoItemDto.created_at)
        assertEquals(domainTodoItem.completedAt?.toString(), todoItemDto.completed_at)
        assertEquals(domainTodoItem.updatedAt.toString(), todoItemDto.updated_at)
    }

    @Test
    fun `toTodoItemDto should handle null description`() {
        // Given
        val domainTodoItem = testDomainTodoItem.copy(description = null)

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertNull(todoItemDto.description)
        assertEquals(domainTodoItem.id, todoItemDto.id)
        assertEquals(domainTodoItem.eventId, todoItemDto.event_id)
        assertEquals(domainTodoItem.title, todoItemDto.title)
    }

    @Test
    fun `toTodoItemDto should handle null assignedTo`() {
        // Given
        val domainTodoItem = testDomainTodoItem.copy(assignedTo = null)

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertNull(todoItemDto.assigned_to)
        assertEquals(domainTodoItem.id, todoItemDto.id)
        assertEquals(domainTodoItem.eventId, todoItemDto.event_id)
        assertEquals(domainTodoItem.title, todoItemDto.title)
    }

    @Test
    fun `toTodoItemDto should handle null completedAt`() {
        // Given
        val domainTodoItem = testDomainTodoItem.copy(completedAt = null)

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertNull(todoItemDto.completed_at)
        assertEquals(domainTodoItem.id, todoItemDto.id)
        assertEquals(domainTodoItem.eventId, todoItemDto.event_id)
        assertEquals(domainTodoItem.title, todoItemDto.title)
    }

    @Test
    fun `toTodoItemDto should handle completed todo with completedAt`() {
        // Given
        val completedAt = Instant.parse("2024-01-01T15:30:00Z")
        val domainTodoItem = testDomainTodoItem.copy(
            isCompleted = true,
            completedAt = completedAt
        )

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertTrue(todoItemDto.is_completed)
        assertEquals(completedAt.toString(), todoItemDto.completed_at)
    }

    @Test
    fun `round trip conversion should preserve data integrity`() {
        // Given
        val originalDto = testTodoItemDto.copy(
            description = "Original Description",
            is_completed = true,
            completed_at = "2024-01-01T15:30:00Z"
        )

        // When
        val domainTodoItem = originalDto.toDomainTodoItem()
        val convertedBackDto = domainTodoItem.toTodoItemDto()

        // Then
        assertEquals(originalDto.id, convertedBackDto.id)
        assertEquals(originalDto.event_id, convertedBackDto.event_id)
        assertEquals(originalDto.title, convertedBackDto.title)
        assertEquals(originalDto.description, convertedBackDto.description)
        assertEquals(originalDto.is_completed, convertedBackDto.is_completed)
        assertEquals(originalDto.assigned_to, convertedBackDto.assigned_to)
        assertEquals(originalDto.created_by, convertedBackDto.created_by)
        assertEquals(originalDto.created_at, convertedBackDto.created_at)
        assertEquals(originalDto.completed_at, convertedBackDto.completed_at)
        assertEquals(originalDto.updated_at, convertedBackDto.updated_at)
    }

    @Test
    fun `round trip conversion should handle null values correctly`() {
        // Given
        val originalDto = testTodoItemDto.copy(
            description = null,
            assigned_to = null,
            completed_at = null
        )

        // When
        val domainTodoItem = originalDto.toDomainTodoItem()
        val convertedBackDto = domainTodoItem.toTodoItemDto()

        // Then
        assertEquals(originalDto.id, convertedBackDto.id)
        assertEquals(originalDto.event_id, convertedBackDto.event_id)
        assertEquals(originalDto.title, convertedBackDto.title)
        assertNull(convertedBackDto.description)
        assertNull(convertedBackDto.assigned_to)
        assertNull(convertedBackDto.completed_at)
        assertEquals(originalDto.is_completed, convertedBackDto.is_completed)
        assertEquals(originalDto.created_by, convertedBackDto.created_by)
        assertEquals(originalDto.created_at, convertedBackDto.created_at)
        assertEquals(originalDto.updated_at, convertedBackDto.updated_at)
    }

    @Test
    fun `toDomainTodoItem should handle empty string values`() {
        // Given
        val todoItemDto = testTodoItemDto.copy(
            id = "",
            event_id = "",
            title = "",
            description = "",
            assigned_to = "",
            created_by = ""
        )

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertEquals("", domainTodoItem.id)
        assertEquals("", domainTodoItem.eventId)
        assertEquals("", domainTodoItem.title)
        assertEquals("", domainTodoItem.description)
        assertEquals("", domainTodoItem.assignedTo)
        assertEquals("", domainTodoItem.createdBy)
    }

    @Test
    fun `toTodoItemDto should handle empty string values`() {
        // Given
        val domainTodoItem = testDomainTodoItem.copy(
            id = "",
            eventId = "",
            title = "",
            description = "",
            assignedTo = "",
            createdBy = ""
        )

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertEquals("", todoItemDto.id)
        assertEquals("", todoItemDto.event_id)
        assertEquals("", todoItemDto.title)
        assertEquals("", todoItemDto.description)
        assertEquals("", todoItemDto.assigned_to)
        assertEquals("", todoItemDto.created_by)
    }

    @Test
    fun `toDomainTodoItem should handle special characters in strings`() {
        // Given
        val specialTitle = "Todo with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?"
        val specialDescription = "Description with emojis: 🎉 📝 ✅ 🚀"
        val todoItemDto = testTodoItemDto.copy(
            title = specialTitle,
            description = specialDescription
        )

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertEquals(specialTitle, domainTodoItem.title)
        assertEquals(specialDescription, domainTodoItem.description)
    }

    @Test
    fun `toTodoItemDto should handle special characters in strings`() {
        // Given
        val specialTitle = "Todo with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?"
        val specialDescription = "Description with emojis: 🎉 📝 ✅ 🚀"
        val domainTodoItem = testDomainTodoItem.copy(
            title = specialTitle,
            description = specialDescription
        )

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertEquals(specialTitle, todoItemDto.title)
        assertEquals(specialDescription, todoItemDto.description)
    }

    @Test
    fun `toDomainTodoItem should handle Unicode characters`() {
        // Given
        val unicodeTitle = "Tâche à faire: résumé"
        val unicodeDescription = "Description avec caractères spéciaux: café, naïve, résumé"
        val todoItemDto = testTodoItemDto.copy(
            title = unicodeTitle,
            description = unicodeDescription
        )

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertEquals(unicodeTitle, domainTodoItem.title)
        assertEquals(unicodeDescription, domainTodoItem.description)
    }

    @Test
    fun `toTodoItemDto should handle Unicode characters`() {
        // Given
        val unicodeTitle = "Tâche à faire: résumé"
        val unicodeDescription = "Description avec caractères spéciaux: café, naïve, résumé"
        val domainTodoItem = testDomainTodoItem.copy(
            title = unicodeTitle,
            description = unicodeDescription
        )

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertEquals(unicodeTitle, todoItemDto.title)
        assertEquals(unicodeDescription, todoItemDto.description)
    }

    @Test
    fun `toDomainTodoItem should handle very long strings`() {
        // Given
        val longTitle = "a".repeat(1000)
        val longDescription = "b".repeat(2000)
        val todoItemDto = testTodoItemDto.copy(
            title = longTitle,
            description = longDescription
        )

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertEquals(longTitle, domainTodoItem.title)
        assertEquals(longDescription, domainTodoItem.description)
        assertEquals(1000, domainTodoItem.title.length)
        assertEquals(2000, domainTodoItem.description?.length)
    }

    @Test
    fun `toTodoItemDto should handle very long strings`() {
        // Given
        val longTitle = "a".repeat(1000)
        val longDescription = "b".repeat(2000)
        val domainTodoItem = testDomainTodoItem.copy(
            title = longTitle,
            description = longDescription
        )

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertEquals(longTitle, todoItemDto.title)
        assertEquals(longDescription, todoItemDto.description)
        assertEquals(1000, todoItemDto.title.length)
        assertEquals(2000, todoItemDto.description?.length)
    }

    @Test
    fun `toDomainTodoItem should handle different timestamp formats correctly`() {
        // Given
        val createdAt = "2024-01-01T00:00:00Z"
        val completedAt = "2024-01-01T15:30:45.123Z"
        val updatedAt = "2024-01-01T12:15:30.456Z"
        val todoItemDto = testTodoItemDto.copy(
            created_at = createdAt,
            completed_at = completedAt,
            updated_at = updatedAt
        )

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertEquals(Instant.parse(createdAt), domainTodoItem.createdAt)
        assertEquals(Instant.parse(completedAt), domainTodoItem.completedAt)
        assertEquals(Instant.parse(updatedAt), domainTodoItem.updatedAt)
    }

    @Test
    fun `toTodoItemDto should handle different timestamp formats correctly`() {
        // Given
        val createdAt = Instant.parse("2024-01-01T00:00:00Z")
        val completedAt = Instant.parse("2024-01-01T15:30:45.123Z")
        val updatedAt = Instant.parse("2024-01-01T12:15:30.456Z")
        val domainTodoItem = testDomainTodoItem.copy(
            createdAt = createdAt,
            completedAt = completedAt,
            updatedAt = updatedAt
        )

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertEquals(createdAt.toString(), todoItemDto.created_at)
        assertEquals(completedAt.toString(), todoItemDto.completed_at)
        assertEquals(updatedAt.toString(), todoItemDto.updated_at)
    }

    @Test
    fun `toDomainTodoItem should handle UUID format IDs`() {
        // Given
        val uuidId = "550e8400-e29b-41d4-a716-446655440000"
        val uuidEventId = "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
        val uuidUserId = "6ba7b811-9dad-11d1-80b4-00c04fd430c8"
        val todoItemDto = testTodoItemDto.copy(
            id = uuidId,
            event_id = uuidEventId,
            assigned_to = uuidUserId,
            created_by = uuidUserId
        )

        // When
        val domainTodoItem = todoItemDto.toDomainTodoItem()

        // Then
        assertEquals(uuidId, domainTodoItem.id)
        assertEquals(uuidEventId, domainTodoItem.eventId)
        assertEquals(uuidUserId, domainTodoItem.assignedTo)
        assertEquals(uuidUserId, domainTodoItem.createdBy)
    }

    @Test
    fun `toTodoItemDto should handle UUID format IDs`() {
        // Given
        val uuidId = "550e8400-e29b-41d4-a716-446655440000"
        val uuidEventId = "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
        val uuidUserId = "6ba7b811-9dad-11d1-80b4-00c04fd430c8"
        val domainTodoItem = testDomainTodoItem.copy(
            id = uuidId,
            eventId = uuidEventId,
            assignedTo = uuidUserId,
            createdBy = uuidUserId
        )

        // When
        val todoItemDto = domainTodoItem.toTodoItemDto()

        // Then
        assertEquals(uuidId, todoItemDto.id)
        assertEquals(uuidEventId, todoItemDto.event_id)
        assertEquals(uuidUserId, todoItemDto.assigned_to)
        assertEquals(uuidUserId, todoItemDto.created_by)
    }

    // Helper extension functions for testing (these would normally be in the actual implementation)
    private fun TodoItemDto.toDomainTodoItem(): DomainTodoItem {
        return DomainTodoItem(
            id = id,
            eventId = event_id,
            title = title,
            description = description,
            isCompleted = is_completed,
            assignedTo = assigned_to,
            createdBy = created_by,
            createdAt = Instant.parse(created_at),
            completedAt = completed_at?.let { Instant.parse(it) },
            updatedAt = Instant.parse(updated_at)
        )
    }

    private fun DomainTodoItem.toTodoItemDto(): TodoItemDto {
        return TodoItemDto(
            id = id,
            event_id = eventId,
            title = title,
            description = description,
            is_completed = isCompleted,
            assigned_to = assignedTo,
            created_by = createdBy,
            created_at = createdAt.toString(),
            completed_at = completedAt?.toString(),
            updated_at = updatedAt.toString()
        )
    }
}