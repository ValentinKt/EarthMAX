package com.earthmax.core.sync

import com.earthmax.core.cache.AdvancedCacheManager
import com.earthmax.core.error.AdvancedErrorHandler
import com.earthmax.core.monitoring.Logger
import com.earthmax.core.monitoring.MetricsCollector
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class SyncManagerTest {

    private lateinit var syncManager: SyncManager
    private lateinit var mockOfflineChangeTracker: OfflineChangeTracker
    private lateinit var mockConflictResolver: ConflictResolver
    private lateinit var mockNetworkMonitor: NetworkMonitor
    private lateinit var mockCacheManager: AdvancedCacheManager
    private lateinit var mockErrorHandler: AdvancedErrorHandler
    private lateinit var mockLogger: Logger
    private lateinit var mockMetricsCollector: MetricsCollector

    @Before
    fun setup() {
        mockOfflineChangeTracker = mockk()
        mockConflictResolver = mockk()
        mockNetworkMonitor = mockk()
        mockCacheManager = mockk()
        mockErrorHandler = mockk()
        mockLogger = mockk(relaxed = true)
        mockMetricsCollector = mockk(relaxed = true)

        syncManager = SyncManager(
            mockOfflineChangeTracker,
            mockConflictResolver,
            mockNetworkMonitor,
            mockCacheManager,
            mockErrorHandler,
            mockLogger,
            mockMetricsCollector
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `queueOperation should track offline change`() = runTest {
        // Given
        val operation = SyncOperation(
            id = "test-id",
            entityType = "Event",
            entityId = "event-123",
            type = SyncOperationType.CREATE,
            data = mapOf("name" to "Test Event"),
            priority = SyncPriority.NORMAL
        )

        coEvery { mockOfflineChangeTracker.trackChange(any(), any(), any(), any(), any()) } just Runs

        // When
        syncManager.queueOperation(operation)

        // Then
        coVerify {
            mockOfflineChangeTracker.trackChange(
                "Event",
                "event-123",
                SyncOperationType.CREATE,
                any(),
                SyncPriority.NORMAL
            )
        }
    }
}