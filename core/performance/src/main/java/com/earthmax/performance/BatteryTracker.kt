package com.earthmax.performance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks battery usage and power consumption
 */
@Singleton
class BatteryTracker @Inject constructor(
    private val context: Context
) {
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    private val _batteryMetrics = MutableStateFlow(BatteryMetrics())
    val batteryMetrics: StateFlow<BatteryMetrics> = _batteryMetrics.asStateFlow()
    
    private var batteryReceiver: BroadcastReceiver? = null
    private var isTracking = false
    
    // Battery tracking data
    private val batteryHistory = mutableListOf<BatterySnapshot>()
    private var lastBatteryLevel = -1
    private var chargingStartTime = 0L
    private var dischargingStartTime = 0L
    
    // Power consumption tracking
    private var screenOnTime = 0L
    private var screenOffTime = 0L
    private var lastScreenStateChange = 0L
    private var isScreenOn = true
    
    /**
     * Start battery tracking
     */
    fun startTracking() {
        if (isTracking) return
        
        isTracking = true
        
        // Register battery state receiver
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
                    Intent.ACTION_POWER_CONNECTED -> handlePowerConnected()
                    Intent.ACTION_POWER_DISCONNECTED -> handlePowerDisconnected()
                    Intent.ACTION_SCREEN_ON -> handleScreenOn()
                    Intent.ACTION_SCREEN_OFF -> handleScreenOff()
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        
        context.registerReceiver(batteryReceiver, filter)
        
        // Get initial battery state
        updateBatteryInfo()
    }
    
    /**
     * Stop battery tracking
     */
    fun stopTracking() {
        if (!isTracking) return
        
        isTracking = false
        
        batteryReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Receiver was not registered
            }
        }
        batteryReceiver = null
    }
    
    /**
     * Handle battery level/status changes
     */
    private fun handleBatteryChanged(intent: Intent) {
        updateBatteryInfo()
    }
    
    /**
     * Handle power connected
     */
    private fun handlePowerConnected() {
        chargingStartTime = System.currentTimeMillis()
        updateBatteryInfo()
    }
    
    /**
     * Handle power disconnected
     */
    private fun handlePowerDisconnected() {
        dischargingStartTime = System.currentTimeMillis()
        updateBatteryInfo()
    }
    
    /**
     * Handle screen on
     */
    private fun handleScreenOn() {
        val currentTime = System.currentTimeMillis()
        if (!isScreenOn && lastScreenStateChange > 0) {
            screenOffTime += currentTime - lastScreenStateChange
        }
        isScreenOn = true
        lastScreenStateChange = currentTime
    }
    
    /**
     * Handle screen off
     */
    private fun handleScreenOff() {
        val currentTime = System.currentTimeMillis()
        if (isScreenOn && lastScreenStateChange > 0) {
            screenOnTime += currentTime - lastScreenStateChange
        }
        isScreenOn = false
        lastScreenStateChange = currentTime
    }
    
    /**
     * Update battery information
     */
    private fun updateBatteryInfo() {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevel = if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            -1
        }
        
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        
        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargingType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_USB -> ChargingType.USB
            BatteryManager.BATTERY_PLUGGED_AC -> ChargingType.AC
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingType.WIRELESS
            else -> ChargingType.NONE
        }
        
        val temperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val batteryTemperature = if (temperature > 0) temperature / 10.0f else 0f
        
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        
        // Calculate power usage estimate
        val powerUsage = calculatePowerUsage(batteryLevel, isCharging)
        
        // Check power save mode
        val isPowerSaveMode = powerManager.isPowerSaveMode
        
        val batteryMetrics = BatteryMetrics(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            chargingType = chargingType,
            batteryTemperature = batteryTemperature,
            powerUsage = powerUsage,
            voltage = voltage,
            health = getBatteryHealthString(health),
            isPowerSaveMode = isPowerSaveMode,
            screenOnTime = screenOnTime,
            screenOffTime = screenOffTime,
            estimatedTimeRemaining = estimateTimeRemaining(batteryLevel, isCharging)
        )
        
        _batteryMetrics.value = batteryMetrics
        
        // Create battery snapshot
        createBatterySnapshot(batteryMetrics)
        
        lastBatteryLevel = batteryLevel
    }
    
    /**
     * Calculate estimated power usage
     */
    private fun calculatePowerUsage(currentLevel: Int, isCharging: Boolean): Double {
        if (lastBatteryLevel < 0 || isCharging) return 0.0
        
        val levelDrop = lastBatteryLevel - currentLevel
        if (levelDrop <= 0) return 0.0
        
        // Estimate power usage based on battery level drop
        // This is a simplified calculation
        return levelDrop * 0.1 // Rough estimate
    }
    
    /**
     * Estimate time remaining for battery
     */
    private fun estimateTimeRemaining(currentLevel: Int, isCharging: Boolean): Long {
        if (batteryHistory.size < 2) return -1L
        
        val recentHistory = batteryHistory.takeLast(10)
        if (recentHistory.size < 2) return -1L
        
        val timeSpan = recentHistory.last().timestamp - recentHistory.first().timestamp
        val levelChange = recentHistory.last().batteryLevel - recentHistory.first().batteryLevel
        
        if (timeSpan <= 0 || levelChange == 0) return -1L
        
        val ratePerMs = levelChange.toDouble() / timeSpan
        
        return if (isCharging) {
            // Time to full charge
            if (ratePerMs > 0) {
                ((100 - currentLevel) / ratePerMs).toLong()
            } else -1L
        } else {
            // Time to empty
            if (ratePerMs < 0) {
                (currentLevel / -ratePerMs).toLong()
            } else -1L
        }
    }
    
    /**
     * Get battery health string
     */
    private fun getBatteryHealthString(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }
    
    /**
     * Create battery snapshot for trend analysis
     */
    private fun createBatterySnapshot(metrics: BatteryMetrics) {
        val snapshot = BatterySnapshot(
            timestamp = System.currentTimeMillis(),
            batteryLevel = metrics.batteryLevel,
            isCharging = metrics.isCharging,
            temperature = metrics.batteryTemperature,
            powerUsage = metrics.powerUsage
        )
        
        batteryHistory.add(snapshot)
        
        // Keep only recent history (last 24 hours)
        val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        batteryHistory.removeAll { it.timestamp < cutoffTime }
    }
    
    /**
     * Get current battery information
     */
    fun getBatteryInfo(): BatteryMetrics {
        if (!isTracking) {
            updateBatteryInfo()
        }
        return _batteryMetrics.value
    }
    
    /**
     * Get battery optimization recommendations
     */
    fun getBatteryRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val metrics = _batteryMetrics.value
        
        // Low battery recommendations
        if (metrics.batteryLevel < 20) {
            recommendations.add("Battery level is low. Enable power saving mode.")
        }
        
        // High temperature recommendations
        if (metrics.batteryTemperature > 40f) {
            recommendations.add("Battery temperature is high (${metrics.batteryTemperature}°C). Reduce CPU intensive tasks.")
        }
        
        // Screen time recommendations
        val totalTime = metrics.screenOnTime + metrics.screenOffTime
        if (totalTime > 0) {
            val screenOnRatio = metrics.screenOnTime.toDouble() / totalTime
            if (screenOnRatio > 0.8) {
                recommendations.add("High screen usage detected. Consider reducing screen brightness or timeout.")
            }
        }
        
        // Power save mode recommendation
        if (!metrics.isPowerSaveMode && metrics.batteryLevel < 30) {
            recommendations.add("Consider enabling power save mode to extend battery life.")
        }
        
        // Charging recommendations
        if (metrics.isCharging && metrics.batteryLevel > 80) {
            recommendations.add("Battery is mostly charged. Consider unplugging to preserve battery health.")
        }
        
        return recommendations
    }
    
    /**
     * Get battery usage statistics
     */
    fun getBatteryStats(): BatteryStats {
        val history = batteryHistory.toList()
        
        if (history.isEmpty()) {
            return BatteryStats()
        }
        
        val chargingSessions = mutableListOf<ChargingSession>()
        val dischargingSessions = mutableListOf<DischargingSession>()
        
        var currentSession: BatterySnapshot? = null
        var sessionStart: BatterySnapshot? = null
        
        // Analyze charging/discharging sessions
        history.forEach { snapshot ->
            if (sessionStart == null) {
                sessionStart = snapshot
                currentSession = snapshot
            } else {
                val prevSession = currentSession!!
                if (snapshot.isCharging != prevSession.isCharging) {
                    // Session changed
                    if (prevSession.isCharging) {
                        // End of charging session
                        chargingSessions.add(
                            ChargingSession(
                                startTime = sessionStart!!.timestamp,
                                endTime = prevSession.timestamp,
                                startLevel = sessionStart!!.batteryLevel,
                                endLevel = prevSession.batteryLevel,
                                duration = prevSession.timestamp - sessionStart!!.timestamp
                            )
                        )
                    } else {
                        // End of discharging session
                        dischargingSessions.add(
                            DischargingSession(
                                startTime = sessionStart!!.timestamp,
                                endTime = prevSession.timestamp,
                                startLevel = sessionStart!!.batteryLevel,
                                endLevel = prevSession.batteryLevel,
                                duration = prevSession.timestamp - sessionStart!!.timestamp,
                                averagePowerUsage = dischargingSessions.lastOrNull()?.averagePowerUsage ?: 0.0
                            )
                        )
                    }
                    sessionStart = snapshot
                }
                currentSession = snapshot
            }
        }
        
        return BatteryStats(
            totalSamples = history.size,
            averageBatteryLevel = history.map { it.batteryLevel }.average().toInt(),
            averageTemperature = history.map { it.temperature }.average(),
            chargingSessions = chargingSessions,
            dischargingSessions = dischargingSessions,
            totalScreenOnTime = screenOnTime,
            totalScreenOffTime = screenOffTime
        )
    }
    
    /**
     * Clear battery tracking data
     */
    fun clearData() {
        batteryHistory.clear()
        screenOnTime = 0L
        screenOffTime = 0L
        lastScreenStateChange = 0L
        lastBatteryLevel = -1
    }
}

/**
 * Enhanced battery metrics data class
 */
data class BatteryMetrics(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val chargingType: ChargingType = ChargingType.NONE,
    val batteryTemperature: Float = 0f,
    val powerUsage: Double = 0.0,
    val voltage: Int = 0,
    val health: String = "Unknown",
    val isPowerSaveMode: Boolean = false,
    val screenOnTime: Long = 0L,
    val screenOffTime: Long = 0L,
    val estimatedTimeRemaining: Long = -1L
)

/**
 * Battery snapshot for trend analysis
 */
data class BatterySnapshot(
    val timestamp: Long,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val temperature: Float,
    val powerUsage: Double
)

/**
 * Battery usage statistics
 */
data class BatteryStats(
    val totalSamples: Int = 0,
    val averageBatteryLevel: Int = 0,
    val averageTemperature: Double = 0.0,
    val chargingSessions: List<ChargingSession> = emptyList(),
    val dischargingSessions: List<DischargingSession> = emptyList(),
    val totalScreenOnTime: Long = 0L,
    val totalScreenOffTime: Long = 0L
)

/**
 * Charging session data
 */
data class ChargingSession(
    val startTime: Long,
    val endTime: Long,
    val startLevel: Int,
    val endLevel: Int,
    val duration: Long
)

/**
 * Discharging session data
 */
data class DischargingSession(
    val startTime: Long,
    val endTime: Long,
    val startLevel: Int,
    val endLevel: Int,
    val duration: Long,
    val averagePowerUsage: Double
)

/**
 * Charging type enum
 */
enum class ChargingType {
    NONE,
    USB,
    AC,
    WIRELESS
}