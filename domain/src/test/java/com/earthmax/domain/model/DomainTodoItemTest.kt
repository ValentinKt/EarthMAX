package com.earthmax.domain.model

import kotlinx.datetime.Instant
import org.junit.Assert.*
import org.junit.Test

class DomainTodoItemTest {

    private val testEventId = "test-event-id"
    private val testTodoId = "test-todo-id"
    private val testUserId = "test-user-id"
    private val testCreatedAt = Instant.parse("2024-01-01T00:00:00Z")
    private val testUpdatedAt = Instant.parse("2024-01-01T12:00:00Z")
    private val testCompletedAt = Instant.parse("2024-01-01T15:00:00Z")

    private val testDomainTodoItem = DomainTodoItem(
        id = testTodoId,
        eventId = testEventId,
        title = "Test Todo",
        description = "Test Description",
        isCompleted = false,
        assignedTo = testUserId,
        createdBy = testUserId,
        createdAt = testCreatedAt,
        completedAt = null,
        updatedAt = testUpdatedAt
    )

    @Test
    fun `should create DomainTodoItem with all properties`() {
        // Given & When
        val todoItem = DomainTodoItem(
            id = testTodoId,
            eventId = testEventId,
            title = "Test Todo",
            description = "Test Description",
            isCompleted = true,
            assignedTo = testUserId,
            createdBy = testUserId,
            createdAt = testCreatedAt,
            completedAt = testCompletedAt,
            updatedAt = testUpdatedAt
        )

        // Then
        assertEquals(testTodoId, todoItem.id)
        assertEquals(testEventId, todoItem.eventId)
        assertEquals("Test Todo", todoItem.title)
        assertEquals("Test Description", todoItem.description)
        assertTrue(todoItem.isCompleted)
        assertEquals(testUserId, todoItem.assignedTo)
        assertEquals(testUserId, todoItem.createdBy)
        assertEquals(testCreatedAt, todoItem.createdAt)
        assertEquals(testCompletedAt, todoItem.completedAt)
        assertEquals(testUpdatedAt, todoItem.updatedAt)
    }

    @Test
    fun `should create DomainTodoItem with null description`() {
        // Given & When
        val todoItem = testDomainTodoItem.copy(description = null)

        // Then
        assertNull(todoItem.description)
        assertEquals(testTodoId, todoItem.id)
        assertEquals(testEventId, todoItem.eventId)
        assertEquals("Test Todo", todoItem.title)
        assertFalse(todoItem.isCompleted)
    }

    @Test
    fun `should create DomainTodoItem with null assignedTo`() {
        // Given & When
        val todoItem = testDomainTodoItem.copy(assignedTo = null)

        // Then
        assertNull(todoItem.assignedTo)
        assertEquals(testTodoId, todoItem.id)
        assertEquals(testEventId, todoItem.eventId)
        assertEquals("Test Todo", todoItem.title)
        assertEquals(testUserId, todoItem.createdBy)
    }

    @Test
    fun `should create DomainTodoItem with null completedAt when not completed`() {
        // Given & When
        val todoItem = testDomainTodoItem.copy(
            isCompleted = false,
            completedAt = null
        )

        // Then
        assertFalse(todoItem.isCompleted)
        assertNull(todoItem.completedAt)
    }

    @Test
    fun `should create DomainTodoItem with completedAt when completed`() {
        // Given & When
        val todoItem = testDomainTodoItem.copy(
            isCompleted = true,
            completedAt = testCompletedAt
        )

        // Then
        assertTrue(todoItem.isCompleted)
        assertEquals(testCompletedAt, todoItem.completedAt)
    }

    @Test
    fun `should support copy with modified properties`() {
        // Given
        val originalTodoItem = testDomainTodoItem

        // When
        val modifiedTodoItem = originalTodoItem.copy(
            title = "Modified Title",
            isCompleted = true,
            completedAt = testCompletedAt
        )

        // Then
        assertEquals("Modified Title", modifiedTodoItem.title)
        assertTrue(modifiedTodoItem.isCompleted)
        assertEquals(testCompletedAt, modifiedTodoItem.completedAt)
        
        // Original properties should remain unchanged
        assertEquals(originalTodoItem.id, modifiedTodoItem.id)
        assertEquals(originalTodoItem.eventId, modifiedTodoItem.eventId)
        assertEquals(originalTodoItem.description, modifiedTodoItem.description)
        assertEquals(originalTodoItem.assignedTo, modifiedTodoItem.assignedTo)
        assertEquals(originalTodoItem.createdBy, modifiedTodoItem.createdBy)
        assertEquals(originalTodoItem.createdAt, modifiedTodoItem.createdAt)
        assertEquals(originalTodoItem.updatedAt, modifiedTodoItem.updatedAt)
    }

    @Test
    fun `should support equality comparison`() {
        // Given
        val todoItem1 = testDomainTodoItem
        val todoItem2 = testDomainTodoItem.copy()
        val todoItem3 = testDomainTodoItem.copy(title = "Different Title")

        // Then
        assertEquals(todoItem1, todoItem2)
        assertNotEquals(todoItem1, todoItem3)
        assertEquals(todoItem1.hashCode(), todoItem2.hashCode())
        assertNotEquals(todoItem1.hashCode(), todoItem3.hashCode())
    }

    @Test
    fun `should support toString representation`() {
        // Given
        val todoItem = testDomainTodoItem

        // When
        val stringRepresentation = todoItem.toString()

        // Then
        assertTrue(stringRepresentation.contains("DomainTodoItem"))
        assertTrue(stringRepresentation.contains(testTodoId))
        assertTrue(stringRepresentation.contains(testEventId))
        assertTrue(stringRepresentation.contains("Test Todo"))
    }

    @Test
    fun `should handle empty string properties`() {
        // Given & When
        val todoItem = DomainTodoItem(
            id = "",
            eventId = "",
            title = "",
            description = "",
            isCompleted = false,
            assignedTo = "",
            createdBy = "",
            createdAt = testCreatedAt,
            completedAt = null,
            updatedAt = testUpdatedAt
        )

        // Then
        assertEquals("", todoItem.id)
        assertEquals("", todoItem.eventId)
        assertEquals("", todoItem.title)
        assertEquals("", todoItem.description)
        assertEquals("", todoItem.assignedTo)
        assertEquals("", todoItem.createdBy)
    }

    @Test
    fun `should handle very long string properties`() {
        // Given
        val longString = "a".repeat(1000)

        // When
        val todoItem = testDomainTodoItem.copy(
            title = longString,
            description = longString
        )

        // Then
        assertEquals(longString, todoItem.title)
        assertEquals(longString, todoItem.description)
        assertEquals(1000, todoItem.title.length)
        assertEquals(1000, todoItem.description?.length)
    }

    @Test
    fun `should handle special characters in string properties`() {
        // Given
        val specialTitle = "Todo with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?"
        val specialDescription = "Description with emojis: 🎉 📝 ✅ 🚀"

        // When
        val todoItem = testDomainTodoItem.copy(
            title = specialTitle,
            description = specialDescription
        )

        // Then
        assertEquals(specialTitle, todoItem.title)
        assertEquals(specialDescription, todoItem.description)
    }

    @Test
    fun `should handle Unicode characters in string properties`() {
        // Given
        val unicodeTitle = "Tâche à faire: résumé"
        val unicodeDescription = "Description avec caractères spéciaux: café, naïve, résumé"

        // When
        val todoItem = testDomainTodoItem.copy(
            title = unicodeTitle,
            description = unicodeDescription
        )

        // Then
        assertEquals(unicodeTitle, todoItem.title)
        assertEquals(unicodeDescription, todoItem.description)
    }

    @Test
    fun `should handle newlines and whitespace in string properties`() {
        // Given
        val titleWithWhitespace = "  Todo with spaces  "
        val descriptionWithNewlines = "Line 1\nLine 2\n\nLine 4"

        // When
        val todoItem = testDomainTodoItem.copy(
            title = titleWithWhitespace,
            description = descriptionWithNewlines
        )

        // Then
        assertEquals(titleWithWhitespace, todoItem.title)
        assertEquals(descriptionWithNewlines, todoItem.description)
    }

    @Test
    fun `should handle different timestamp values`() {
        // Given
        val veryOldDate = Instant.parse("1970-01-01T00:00:00Z")
        val futureDate = Instant.parse("2030-12-31T23:59:59Z")

        // When
        val todoItem = testDomainTodoItem.copy(
            createdAt = veryOldDate,
            updatedAt = futureDate,
            completedAt = futureDate
        )

        // Then
        assertEquals(veryOldDate, todoItem.createdAt)
        assertEquals(futureDate, todoItem.updatedAt)
        assertEquals(futureDate, todoItem.completedAt)
    }

    @Test
    fun `should handle same timestamp for all date fields`() {
        // Given
        val sameTimestamp = Instant.parse("2024-06-15T10:30:00Z")

        // When
        val todoItem = testDomainTodoItem.copy(
            createdAt = sameTimestamp,
            updatedAt = sameTimestamp,
            completedAt = sameTimestamp
        )

        // Then
        assertEquals(sameTimestamp, todoItem.createdAt)
        assertEquals(sameTimestamp, todoItem.updatedAt)
        assertEquals(sameTimestamp, todoItem.completedAt)
    }

    @Test
    fun `should handle UUID format IDs`() {
        // Given
        val uuidId = "550e8400-e29b-41d4-a716-446655440000"
        val uuidEventId = "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
        val uuidUserId = "6ba7b811-9dad-11d1-80b4-00c04fd430c8"

        // When
        val todoItem = testDomainTodoItem.copy(
            id = uuidId,
            eventId = uuidEventId,
            assignedTo = uuidUserId,
            createdBy = uuidUserId
        )

        // Then
        assertEquals(uuidId, todoItem.id)
        assertEquals(uuidEventId, todoItem.eventId)
        assertEquals(uuidUserId, todoItem.assignedTo)
        assertEquals(uuidUserId, todoItem.createdBy)
    }

    @Test
    fun `should handle boolean completion states correctly`() {
        // Given & When
        val incompleteTodoItem = testDomainTodoItem.copy(
            isCompleted = false,
            completedAt = null
        )
        val completeTodoItem = testDomainTodoItem.copy(
            isCompleted = true,
            completedAt = testCompletedAt
        )

        // Then
        assertFalse(incompleteTodoItem.isCompleted)
        assertNull(incompleteTodoItem.completedAt)
        
        assertTrue(completeTodoItem.isCompleted)
        assertNotNull(completeTodoItem.completedAt)
        assertEquals(testCompletedAt, completeTodoItem.completedAt)
    }

    @Test
    fun `should handle edge case of completed without completedAt timestamp`() {
        // Given & When
        val todoItem = testDomainTodoItem.copy(
            isCompleted = true,
            completedAt = null
        )

        // Then
        assertTrue(todoItem.isCompleted)
        assertNull(todoItem.completedAt)
    }

    @Test
    fun `should handle edge case of not completed but with completedAt timestamp`() {
        // Given & When
        val todoItem = testDomainTodoItem.copy(
            isCompleted = false,
            completedAt = testCompletedAt
        )

        // Then
        assertFalse(todoItem.isCompleted)
        assertEquals(testCompletedAt, todoItem.completedAt)
    }
}