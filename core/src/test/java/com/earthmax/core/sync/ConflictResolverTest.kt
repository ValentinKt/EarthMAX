package com.earthmax.core.sync

import com.earthmax.core.monitoring.Logger
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

class ConflictResolverTest {

    private lateinit var conflictResolver: ConflictResolver
    private lateinit var mockLogger: Logger

    @Before
    fun setup() {
        mockLogger = mockk(relaxed = true)
        conflictResolver = ConflictResolver(mockLogger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `resolveCreateConflict should use local when no server data`() {
        // Given
        val operation = SyncOperation(
            id = "test-id",
            entityType = "Event",
            entityId = "event-123",
            type = SyncOperationType.CREATE,
            data = mapOf("name" to "Test Event"),
            priority = SyncPriority.NORMAL
        )

        // When
        val resolution = conflictResolver.resolveCreateConflict(operation, null)

        // Then
        assertEquals(ConflictStrategy.USE_LOCAL, resolution.strategy)
        assertEquals(operation, resolution.operation)
        assertNull(resolution.serverData)
    }

    @Test
    fun `resolveUpdateConflict should convert to create when entity not on server`() {
        // Given
        val operation = SyncOperation(
            id = "test-id",
            entityType = "Event",
            entityId = "event-123",
            type = SyncOperationType.UPDATE,
            data = mapOf("name" to "Updated Event"),
            priority = SyncPriority.NORMAL
        )

        // When
        val resolution = conflictResolver.resolveUpdateConflict(operation, null)

        // Then
        assertEquals(ConflictStrategy.USE_LOCAL, resolution.strategy)
        assertEquals(SyncOperationType.CREATE, resolution.operation.type)
        assertNull(resolution.serverData)
    }

    @Test
    fun `mergeData should handle map merging correctly`() {
        // Given
        val localData = mapOf(
            "name" to "Local Name",
            "description" to "Local Description",
            "updatedAt" to "2023-12-01T12:00:00"
        )
        val serverData = mapOf(
            "name" to "Server Name",
            "location" to "Server Location",
            "updatedAt" to "2023-12-01T11:00:00"
        )

        // When
        val mergedData = conflictResolver.mergeData(localData, serverData)

        // Then
        assertTrue(mergedData is Map<*, *>)
        val resultMap = mergedData as Map<String, Any?>
        assertEquals("Local Name", resultMap["name"]) // Local data preferred
        assertEquals("Local Description", resultMap["description"]) // Local only
        assertEquals("Server Location", resultMap["location"]) // Server only
        assertEquals("2023-12-01T12:00:00", resultMap["updatedAt"]) // Newer timestamp
    }
}