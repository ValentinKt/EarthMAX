package com.earthmax.core.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.earthmax.core.database.EarthMaxDatabase
import com.earthmax.core.monitoring.Logger
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineChangeTrackerTest {

    private lateinit var offlineChangeTracker: OfflineChangeTracker
    private lateinit var database: EarthMaxDatabase
    private lateinit var offlineChangeDao: OfflineChangeDao
    private lateinit var mockLogger: Logger

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EarthMaxDatabase::class.java
        ).allowMainThreadQueries().build()

        offlineChangeDao = database.offlineChangeDao()
        mockLogger = mockk(relaxed = true)

        offlineChangeTracker = OfflineChangeTracker(offlineChangeDao, mockLogger)
    }

    @After
    fun tearDown() {
        database.close()
        clearAllMocks()
    }

    @Test
    fun `trackChange should insert offline change`() = runTest {
        // Given
        val entityType = "Event"
        val entityId = "event-123"
        val operationType = SyncOperationType.CREATE
        val data = mapOf("name" to "Test Event")
        val priority = SyncPriority.HIGH

        // When
        offlineChangeTracker.trackChange(entityType, entityId, operationType, data, priority)

        // Then
        val changes = offlineChangeTracker.getPendingChanges()
        assertEquals(1, changes.size)
        assertEquals(entityType, changes[0].entityType)
        assertEquals(entityId, changes[0].entityId)
        assertEquals(operationType, changes[0].operationType)
        assertEquals(priority, changes[0].priority)
    }

    @Test
    fun `markAsSynced should update change status`() = runTest {
        // Given
        val changeId = "change-123"
        offlineChangeTracker.trackChange("Event", "event-123", SyncOperationType.CREATE, mapOf(), SyncPriority.NORMAL)
        val changes = offlineChangeTracker.getPendingChanges()
        val change = changes[0]

        // When
        offlineChangeTracker.markAsSynced(change.id)

        // Then
        val updatedChanges = offlineChangeTracker.getPendingChanges()
        assertTrue(updatedChanges.isEmpty())
    }

    @Test
    fun `markAsFailed should update change status and increment retry count`() = runTest {
        // Given
        offlineChangeTracker.trackChange("Event", "event-123", SyncOperationType.CREATE, mapOf(), SyncPriority.NORMAL)
        val changes = offlineChangeTracker.getPendingChanges()
        val change = changes[0]
        val errorMessage = "Network error"

        // When
        offlineChangeTracker.markAsFailed(change.id, errorMessage)

        // Then
        val failedChanges = offlineChangeTracker.getFailedChanges()
        assertEquals(1, failedChanges.size)
        assertEquals(OfflineChangeStatus.FAILED, failedChanges[0].status)
        assertEquals(1, failedChanges[0].retryCount)
        assertEquals(errorMessage, failedChanges[0].lastError)
    }

    @Test
    fun `getChangesByEntityType should filter by entity type`() = runTest {
        // Given
        offlineChangeTracker.trackChange("Event", "event-123", SyncOperationType.CREATE, mapOf(), SyncPriority.NORMAL)
        offlineChangeTracker.trackChange("User", "user-456", SyncOperationType.UPDATE, mapOf(), SyncPriority.HIGH)

        // When
        val eventChanges = offlineChangeTracker.getChangesByEntityType("Event")

        // Then
        assertEquals(1, eventChanges.size)
        assertEquals("Event", eventChanges[0].entityType)
        assertEquals("event-123", eventChanges[0].entityId)
    }
}