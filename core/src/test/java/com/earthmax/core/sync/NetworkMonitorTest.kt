package com.earthmax.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.earthmax.core.utils.Logger
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class NetworkMonitorTest {

    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var mockContext: Context
    private lateinit var mockConnectivityManager: ConnectivityManager
    private lateinit var mockNetworkCapabilities: NetworkCapabilities
    private lateinit var mockLogger: Logger

    @Before
    fun setup() {
        mockContext = mockk()
        mockConnectivityManager = mockk()
        mockNetworkCapabilities = mockk()
        mockLogger = mockk(relaxed = true)

        every { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns mockConnectivityManager

        networkMonitor = NetworkMonitor(mockContext, mockLogger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `isCurrentlyConnected should return true when network has internet and is validated`() {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockk()
        every { mockConnectivityManager.getNetworkCapabilities(any()) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        // When
        val isConnected = networkMonitor.isCurrentlyConnected()

        // Then
        assertTrue(isConnected)
    }

    @Test
    fun `isCurrentlyConnected should return false when no active network`() {
        // Given
        every { mockConnectivityManager.activeNetwork } returns null

        // When
        val isConnected = networkMonitor.isCurrentlyConnected()

        // Then
        assertFalse(isConnected)
    }

    @Test
    fun `getCurrentNetworkType should return WIFI for wifi transport`() {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockk()
        every { mockConnectivityManager.getNetworkCapabilities(any()) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

        // When
        val networkType = networkMonitor.getCurrentNetworkType()

        // Then
        assertEquals(NetworkType.WIFI, networkType)
    }

    @Test
    fun `isSuitableForSync should return true for wifi connection`() {
        // Given
        every { mockConnectivityManager.activeNetwork } returns mockk()
        every { mockConnectivityManager.getNetworkCapabilities(any()) } returns mockNetworkCapabilities
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { mockNetworkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
        every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false
        every { mockConnectivityManager.isActiveNetworkMetered } returns false

        // When
        val isSuitable = networkMonitor.isSuitableForSync()

        // Then
        assertTrue(isSuitable)
    }
}