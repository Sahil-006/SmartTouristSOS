package com.example.smarttouristsos

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smarttouristsos.BuildConfig
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private val RED_ZONE_RADIUS = 200.0 // 200 meters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the user agent to identify your app to the map servers
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID

        // Load the OSMDroid configuration
        val ctx: Context = applicationContext
        Configuration.getInstance().load(ctx, getSharedPreferences("osmdroid", MODE_PRIVATE))

        setContentView(R.layout.activity_map)
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK) // Use the most reliable map style
        map.setMultiTouchControls(true) // Allow pinch-to-zoom

        // Set initial map view
        val mapController = map.controller
        mapController.setZoom(15.0)
        val startPoint = GeoPoint(22.47, 72.8) // Petlad, Gujarat
        mapController.setCenter(startPoint)
        Toast.makeText(this, "Long-press on the map to set a Red Zone", Toast.LENGTH_LONG).show()

        // Set up the clear button
        val btnClearZone: FloatingActionButton = findViewById(R.id.btnClearZone)
        btnClearZone.setOnClickListener {
            clearRedZone()
        }

        // Create a receiver for map touch events (long-press)
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false // We don't need to do anything on a single tap
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let {
                    // A long press happened, set the red zone
                    setRedZone(it)
                }
                return true
            }
        }
        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        map.overlays.add(0, mapEventsOverlay)

        // Load and display any previously saved red zone when the map opens
        loadAndDrawSavedRedZone()
    }

    private fun setRedZone(center: GeoPoint) {
        clearMapOverlays()

        // Add a new marker with our custom icon
        val redZoneMarker = Marker(map)
        redZoneMarker.position = center
        redZoneMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        redZoneMarker.title = "Red Zone Center"
        redZoneMarker.icon = ContextCompat.getDrawable(this, R.drawable.ic_red_zone_marker)
        map.overlays.add(redZoneMarker)

        // Add a new circle
        val circle = Polygon()
        val circlePoints = Polygon.pointsAsCircle(center, RED_ZONE_RADIUS)
        circle.points = circlePoints
        circle.fillColor = 0x33FF0000 // Semi-transparent red fill
        circle.strokeColor = 0x88FF0000.toInt() // Semi-transparent red stroke
        map.overlays.add(circle)

        map.invalidate() // Force the map to redraw
        saveRedZone(center)
    }

    private fun saveRedZone(center: GeoPoint) {
        val sharedPref = getSharedPreferences("SOS_App_Prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putFloat("red_zone_lat", center.latitude.toFloat())
            putFloat("red_zone_lon", center.longitude.toFloat())
            putFloat("red_zone_radius", RED_ZONE_RADIUS.toFloat())
            apply()
        }
        Toast.makeText(this, "Red Zone saved!", Toast.LENGTH_SHORT).show()
    }

    private fun clearRedZone() {
        clearMapOverlays()
        val sharedPref = getSharedPreferences("SOS_App_Prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("red_zone_lat")
            remove("red_zone_lon")
            remove("red_zone_radius")
            apply()
        }
        Toast.makeText(this, "Red Zone cleared!", Toast.LENGTH_SHORT).show()
    }

    private fun loadAndDrawSavedRedZone() {
        val sharedPref = getSharedPreferences("SOS_App_Prefs", MODE_PRIVATE)
        val lat = sharedPref.getFloat("red_zone_lat", -1f)
        val lon = sharedPref.getFloat("red_zone_lon", -1f)

        if (lat != -1f && lon != -1f) {
            val center = GeoPoint(lat.toDouble(), lon.toDouble())
            setRedZone(center)
        }
    }

    private fun clearMapOverlays() {
        // Use a compatible loop to remove old overlays
        val overlaysToRemove = map.overlays.filter { it is Marker || it is Polygon }
        map.overlays.removeAll(overlaysToRemove.toSet())
        map.invalidate()
    }

    // Handle map lifecycle events
    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}