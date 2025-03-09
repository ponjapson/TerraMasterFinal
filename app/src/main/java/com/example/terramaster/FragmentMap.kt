package com.example.terramaster

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class FragmentMap : Fragment(R.layout.fragment_map) { // Uses fragment_map.xml
    private lateinit var mapView: MapView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve latitude and longitude from arguments
        val latitude = arguments?.getDouble("latitude") ?: 0.0
        val longitude = arguments?.getDouble("longitude") ?: 0.0

        // Initialize OpenStreetMap settings
        Configuration.getInstance().load(requireContext(),
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))

        mapView = view.findViewById(R.id.mapView)
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Center the map at the given location
        val mapController = mapView.controller
        val startPoint = GeoPoint(latitude, longitude)
        mapController.setCenter(startPoint)
        mapController.setZoom(15.0)

        // Add a marker at the location
        val marker = Marker(mapView)
        marker.position = startPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Selected Location"
        mapView.overlays.add(marker)
    }
}
