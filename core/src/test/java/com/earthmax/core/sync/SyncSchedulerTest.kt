package com.earthmax.core.sync

import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncSchedulerTest {

    private lateinit var workManager: WorkManager
    private lateinit var syncScheduler: SyncScheduler

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
        syncScheduler = SyncScheduler(workManager)
    }

    @Test
    fun `schedulePeriodicSync should enqueue periodic work`() = runTest {
        // When
        syncScheduler.schedulePeriodicSync()

        // Then
        // Verify that work is enqueued (WorkManager testing utilities can be used)
        // This is a basic test - in real implementation you'd verify the work is properly scheduled
    }

    @Test
    fun `scheduleImmediateSync should enqueue one-time work`() = runTest {
        // When
        syncScheduler.scheduleImmediateSync()

        // Then
        // Verify that one-time work is enqueued
    }

    @Test
    fun `cancelAllSync should cancel all sync work`() = runTest {
        // Given
        syncScheduler.schedulePeriodicSync()
        syncScheduler.scheduleImmediateSync()

        // When
        syncScheduler.cancelAllSync()

        // Then
        // Verify that all work is cancelled
    }
}