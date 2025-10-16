package com.earthmax.feature.events.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.earthmax.core.models.Event
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun EventMapView(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
    }
    
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(10.0)
        }
    }
    
    LaunchedEffect(events) {
        // Clear existing markers
        mapView.overlays.clear()
        
        // Add markers for each event
        events.forEach { event ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(event.latitude, event.longitude)
                title = event.title
                snippet = event.description
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                
                setOnMarkerClickListener { _, _ ->
                    onEventClick(event)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        
        // Center map on first event if available
        if (events.isNotEmpty()) {
            val firstEvent = events.first()
            mapView.controller.setCenter(GeoPoint(firstEvent.latitude, firstEvent.longitude))
        }
        
        mapView.invalidate()
    }
    
    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize()
    ) { map ->
        map.onResume()
    }
}