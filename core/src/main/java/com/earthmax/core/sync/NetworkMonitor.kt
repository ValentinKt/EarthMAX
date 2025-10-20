package com.earthmax.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.earthmax.core.monitoring.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity and provides network state information
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Flow that emits network connectivity state changes
     */
    val isConnected: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                logger.d("NetworkMonitor", "Network available: $network")
                trySend(true)
            }

            override fun onLost(network: Network) {
                logger.d("NetworkMonitor", "Network lost: $network")
                trySend(false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val validated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                logger.d("NetworkMonitor", "Network capabilities changed - Internet: $hasInternet, Validated: $validated")
                trySend(hasInternet && validated)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        trySend(isCurrentlyConnected())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * Flow that emits network type information
     */
    val networkType: Flow<NetworkType> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val type = getCurrentNetworkType()
                logger.d("NetworkMonitor", "Network type available: $type")
                trySend(type)
            }

            override fun onLost(network: Network) {
                logger.d("NetworkMonitor", "Network lost")
                trySend(NetworkType.NONE)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val type = getNetworkType(networkCapabilities)
                logger.d("NetworkMonitor", "Network type changed: $type")
                trySend(type)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        trySend(getCurrentNetworkType())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    /**
     * Flow that indicates if the device is on a metered connection
     */
    val isMetered: Flow<Boolean> = networkType.map { type ->
        when (type) {
            NetworkType.CELLULAR -> true
            NetworkType.WIFI -> isCurrentlyMetered()
            NetworkType.ETHERNET -> false
            NetworkType.NONE -> false
        }
    }.distinctUntilChanged()

    /**
     * Check if currently connected to internet
     */
    fun isCurrentlyConnected(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            networkCapabilities?.let {
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } ?: false
        } catch (e: Exception) {
            logger.e("NetworkMonitor", "Failed to check network connectivity", e)
            false
        }
    }

    /**
     * Get current network type
     */
    fun getCurrentNetworkType(): NetworkType {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            networkCapabilities?.let { getNetworkType(it) } ?: NetworkType.NONE
        } catch (e: Exception) {
            logger.e("NetworkMonitor", "Failed to get network type", e)
            NetworkType.NONE
        }
    }

    /**
     * Check if current connection is metered
     */
    fun isCurrentlyMetered(): Boolean {
        return try {
            connectivityManager.isActiveNetworkMetered
        } catch (e: Exception) {
            logger.e("NetworkMonitor", "Failed to check if network is metered", e)
            false
        }
    }

    /**
     * Get network signal strength (0-4, -1 if unavailable)
     */
    fun getSignalStrength(): Int {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            networkCapabilities?.signalStrength ?: -1
        } catch (e: Exception) {
            logger.e("NetworkMonitor", "Failed to get signal strength", e)
            -1
        }
    }

    /**
     * Get network bandwidth estimate in Kbps
     */
    fun getBandwidthEstimate(): NetworkBandwidth {
        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            networkCapabilities?.let {
                NetworkBandwidth(
                    downstreamKbps = it.linkDownstreamBandwidthKbps,
                    upstreamKbps = it.linkUpstreamBandwidthKbps
                )
            } ?: NetworkBandwidth(0, 0)
        } catch (e: Exception) {
            logger.e("NetworkMonitor", "Failed to get bandwidth estimate", e)
            NetworkBandwidth(0, 0)
        }
    }

    /**
     * Check if network is suitable for sync operations
     */
    fun isSuitableForSync(): Boolean {
        if (!isCurrentlyConnected()) return false
        
        val networkType = getCurrentNetworkType()
        val isMetered = isCurrentlyMetered()
        val signalStrength = getSignalStrength()
        
        return when (networkType) {
            NetworkType.WIFI -> true
            NetworkType.ETHERNET -> true
            NetworkType.CELLULAR -> {
                // Only sync on cellular if signal is good and not heavily metered
                signalStrength >= 2 && (!isMetered || signalStrength >= 3)
            }
            NetworkType.NONE -> false
        }
    }

    /**
     * Determine network type from capabilities
     */
    private fun getNetworkType(networkCapabilities: NetworkCapabilities): NetworkType {
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.NONE
        }
    }
}

/**
 * Network type enumeration
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    NONE
}

/**
 * Network bandwidth information
 */
data class NetworkBandwidth(
    val downstreamKbps: Int,
    val upstreamKbps: Int
) {
    val isHighBandwidth: Boolean
        get() = downstreamKbps > 1000 && upstreamKbps > 500
        
    val isMediumBandwidth: Boolean
        get() = downstreamKbps > 500 && upstreamKbps > 100
        
    val isLowBandwidth: Boolean
        get() = !isHighBandwidth && !isMediumBandwidth
}