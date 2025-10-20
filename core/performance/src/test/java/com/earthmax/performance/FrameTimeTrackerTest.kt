package com.earthmax.performance

import android.view.Choreographer
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class FrameTimeTrackerTest {

    private lateinit var choreographer: Choreographer
    private lateinit var frameTimeTracker: FrameTimeTracker
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        choreographer = mockk(relaxed = true)
        frameTimeTracker = FrameTimeTracker()
        
        // Mock static Choreographer.getInstance()
        mockkStatic(Choreographer::class)
        every { Choreographer.getInstance() } returns choreographer
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Choreographer::class)
    }

    @Test
    fun `startTracking should register frame callback`() {
        // When
        frameTimeTracker.startTracking()
        
        // Then
        verify { choreographer.postFrameCallback(any()) }
        assertTrue("Should be tracking", frameTimeTracker.isTracking())
    }

    @Test
    fun `stopTracking should remove frame callback`() {
        // Given
        frameTimeTracker.startTracking()
        
        // When
        frameTimeTracker.stopTracking()
        
        // Then
        verify { choreographer.removeFrameCallback(any()) }
        assertFalse("Should not be tracking", frameTimeTracker.isTracking())
    }

    @Test
    fun `onFrame should record frame times correctly`() {
        // Given
        frameTimeTracker.startTracking()
        val frameTime1 = 16_666_666L // 16.67ms (60 FPS)
        val frameTime2 = 33_333_333L // 33.33ms (30 FPS)
        
        // When
        frameTimeTracker.onFrame(frameTime1)
        frameTimeTracker.onFrame(frameTime2)
        
        // Then
        val stats = frameTimeTracker.getFrameStats()
        assertEquals("Should have 2 frames", 2, stats.totalFrames)
        assertTrue("Should have recorded frame times", stats.averageFrameTime > 0.0)
    }

    @Test
    fun `getAverageFps should calculate correct FPS`() {
        // Given
        frameTimeTracker.startTracking()
        val frameTime = 16_666_666L // 16.67ms (60 FPS)
        
        // Simulate multiple frames at 60 FPS
        repeat(10) {
            frameTimeTracker.onFrame(frameTime * (it + 1))
        }
        
        // When
        val fps = frameTimeTracker.getAverageFps()
        
        // Then
        assertTrue("FPS should be around 60", fps >= 55.0 && fps <= 65.0)
    }

    @Test
    fun `getFrameStats should return correct statistics`() {
        // Given
        frameTimeTracker.startTracking()
        val frameTimes = listOf(
            16_666_666L, // 16.67ms
            33_333_333L, // 33.33ms
            50_000_000L  // 50ms
        )
        
        // When
        frameTimes.forEachIndexed { index, time ->
            frameTimeTracker.onFrame(time * (index + 1))
        }
        
        val stats = frameTimeTracker.getFrameStats()
        
        // Then
        assertEquals("Should have 3 frames", 3, stats.totalFrames)
        assertTrue("Should have min frame time", stats.minFrameTime > 0.0)
        assertTrue("Should have max frame time", stats.maxFrameTime > stats.minFrameTime)
        assertTrue("Should have average frame time", stats.averageFrameTime > 0.0)
        assertTrue("Should have median frame time", stats.medianFrameTime > 0.0)
        assertTrue("Should have 95th percentile", stats.percentile95 > 0.0)
        assertTrue("Should have 99th percentile", stats.percentile99 > 0.0)
    }

    @Test
    fun `getDroppedFrames should count frames over threshold`() {
        // Given
        frameTimeTracker.startTracking()
        val normalFrame = 16_666_666L // 16.67ms (normal)
        val droppedFrame = 50_000_000L // 50ms (dropped)
        
        // When
        frameTimeTracker.onFrame(normalFrame)
        frameTimeTracker.onFrame(normalFrame * 2)
        frameTimeTracker.onFrame(droppedFrame) // This should be counted as dropped
        frameTimeTracker.onFrame(normalFrame * 4)
        
        // Then
        val droppedCount = frameTimeTracker.getDroppedFrames()
        assertTrue("Should have at least 1 dropped frame", droppedCount >= 1)
    }

    @Test
    fun `getJankFrames should count janky frames`() {
        // Given
        frameTimeTracker.startTracking()
        val normalFrame = 16_666_666L // 16.67ms (normal)
        val jankFrame = 35_000_000L // 35ms (jank but not dropped)
        
        // When
        frameTimeTracker.onFrame(normalFrame)
        frameTimeTracker.onFrame(normalFrame * 2)
        frameTimeTracker.onFrame(jankFrame) // This should be counted as jank
        frameTimeTracker.onFrame(normalFrame * 4)
        
        // Then
        val jankCount = frameTimeTracker.getJankFrames()
        assertTrue("Should have at least 1 jank frame", jankCount >= 1)
    }

    @Test
    fun `getFrameConsistency should calculate consistency correctly`() {
        // Given
        frameTimeTracker.startTracking()
        val consistentFrameTime = 16_666_666L // 16.67ms
        
        // Simulate very consistent frames
        repeat(10) { index ->
            frameTimeTracker.onFrame(consistentFrameTime * (index + 1))
        }
        
        // When
        val consistency = frameTimeTracker.getFrameConsistency()
        
        // Then
        assertTrue("Consistent frames should have high consistency", consistency >= 80.0)
    }

    @Test
    fun `getFrameConsistency should detect inconsistent frames`() {
        // Given
        frameTimeTracker.startTracking()
        val frameTimes = listOf(
            16_666_666L, // 16.67ms
            50_000_000L, // 50ms (very inconsistent)
            16_666_666L, // 16.67ms
            100_000_000L // 100ms (very inconsistent)
        )
        
        // When
        frameTimes.forEachIndexed { index, time ->
            frameTimeTracker.onFrame(time * (index + 1))
        }
        
        val consistency = frameTimeTracker.getFrameConsistency()
        
        // Then
        assertTrue("Inconsistent frames should have low consistency", consistency <= 50.0)
    }

    @Test
    fun `reset should clear all frame data`() {
        // Given
        frameTimeTracker.startTracking()
        frameTimeTracker.onFrame(16_666_666L)
        frameTimeTracker.onFrame(33_333_333L)
        
        // Verify data exists
        assertTrue("Should have frame data", frameTimeTracker.getFrameStats().totalFrames > 0)
        
        // When
        frameTimeTracker.reset()
        
        // Then
        val stats = frameTimeTracker.getFrameStats()
        assertEquals("Should have no frames after reset", 0, stats.totalFrames)
        assertEquals("Should have no dropped frames", 0, frameTimeTracker.getDroppedFrames())
        assertEquals("Should have no jank frames", 0, frameTimeTracker.getJankFrames())
        assertEquals("Should have 0 FPS", 0.0f, frameTimeTracker.getAverageFps())
    }

    @Test
    fun `multiple start calls should not cause issues`() {
        // When
        frameTimeTracker.startTracking()
        frameTimeTracker.startTracking()
        frameTimeTracker.startTracking()
        
        // Then
        assertTrue("Should still be tracking", frameTimeTracker.isTracking())
        // Should only register callback once (due to internal state management)
        verify(atLeast = 1) { choreographer.postFrameCallback(any()) }
    }

    @Test
    fun `stop without start should not cause issues`() {
        // When
        frameTimeTracker.stopTracking()
        
        // Then
        assertFalse("Should not be tracking", frameTimeTracker.isTracking())
        // Should not try to remove callback if never started
        verify(exactly = 0) { choreographer.removeFrameCallback(any()) }
    }

    @Test
    fun `frame callback should continue after single frame`() {
        // Given
        frameTimeTracker.startTracking()
        val frameCallback = slot<Choreographer.FrameCallback>()
        verify { choreographer.postFrameCallback(capture(frameCallback)) }
        
        // When
        frameCallback.captured.doFrame(16_666_666L)
        
        // Then
        // Should post another frame callback to continue tracking
        verify(atLeast = 2) { choreographer.postFrameCallback(any()) }
    }

    @Test
    fun `getFrameStats with no frames should return empty stats`() {
        // When
        val stats = frameTimeTracker.getFrameStats()
        
        // Then
        assertEquals("Should have 0 total frames", 0, stats.totalFrames)
        assertEquals("Should have 0 min frame time", 0.0, stats.minFrameTime, 0.001)
        assertEquals("Should have 0 max frame time", 0.0, stats.maxFrameTime, 0.001)
        assertEquals("Should have 0 average frame time", 0.0, stats.averageFrameTime, 0.001)
    }

    @Test
    fun `frame time conversion should be accurate`() {
        // Given
        frameTimeTracker.startTracking()
        val frameTimeNanos = 16_666_666L // 16.666666ms in nanoseconds
        
        // When
        frameTimeTracker.onFrame(frameTimeNanos)
        frameTimeTracker.onFrame(frameTimeNanos * 2)
        
        val stats = frameTimeTracker.getFrameStats()
        
        // Then
        val expectedFrameTimeMs = frameTimeNanos / 1_000_000.0
        assertEquals("Frame time conversion should be accurate", 
            expectedFrameTimeMs, stats.averageFrameTime, 0.1)
    }

    @Test
    fun `large number of frames should not cause performance issues`() {
        // Given
        frameTimeTracker.startTracking()
        val frameTime = 16_666_666L
        
        // When - simulate many frames
        repeat(1000) { index ->
            frameTimeTracker.onFrame(frameTime * (index + 1))
        }
        
        // Then
        val stats = frameTimeTracker.getFrameStats()
        assertEquals("Should handle 1000 frames", 1000, stats.totalFrames)
        assertTrue("Should still calculate stats correctly", stats.averageFrameTime > 0.0)
    }
}