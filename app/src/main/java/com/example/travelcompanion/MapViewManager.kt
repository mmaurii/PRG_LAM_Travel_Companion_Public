package com.example.travelcompanion

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.location.Geocoder
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.ImageButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.IOException
import java.util.Locale

import kotlinx.coroutines.Job
import org.osmdroid.views.overlay.infowindow.InfoWindow

class MapViewManager(
    private val context: Context,
    private val lifecycleScope: CoroutineScope,
    private val mapView: MapView
) {
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var currentTrackPolyline: Polyline? = null
    private val polylines = mutableListOf<Polyline>()

    private var trackingJob: Job? = null  // <-- job per la raccolta

    init {
        Configuration.getInstance().load(
            context,
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)
    }

    fun setupLocationOverlay(center: GeoPoint? = null) {
        val locationProvider = GpsMyLocationProvider(context)
        locationOverlay = MyLocationNewOverlay(locationProvider, mapView).apply {
            enableMyLocation()
            if (center == null) enableFollowLocation() else disableFollowLocation()
        }

        if (!mapView.overlays.contains(locationOverlay)) {
            mapView.overlays.add(locationOverlay)
        }

        center?.let {
            mapView.controller.setCenter(it)
        } ?: locationOverlay.runOnFirstFix {
            locationOverlay.myLocation?.let {
                (context as Activity).runOnUiThread {
                    mapView.controller.animateTo(it)
                }
            }
        }
    }

    fun bindToViewModel(viewModel: MapViewModel) {
        // Se c'è un job attivo, cancellalo (ad esempio se bind viene chiamato più volte)
        trackingJob?.cancel()

        trackingJob = lifecycleScope.launch {
            var lastSize = 0

            combine(viewModel.trackedPoints, viewModel.isTracking) { points, isTracking ->
                Pair(points, isTracking)
            }.collectLatest { (points, isTracking) ->
                if (points.isEmpty()) {
                    clearTrack()
                    lastSize = 0
                    return@collectLatest
                }

                if (isTracking) {
                    // Tracciamento live: disegna solo l’ultimo punto
                    if (points.size > lastSize) {
                        drawPointSafe(points.last())
                    }
                } else {
                    // Rientro nel fragment: ridisegna tutta la traccia
                    drawFullTrack(points)
                }

                lastSize = points.size
            }
        }
    }

    fun unbind() {
        trackingJob?.cancel()
        trackingJob = null
    }


    private fun drawPointSafe(point: GeoPoint) {
        try {
            if (mapView.overlayManager == null) return

            val polyline = currentTrackPolyline
            if (polyline == null || polyline.actualPoints == null) {
                currentTrackPolyline = Polyline().apply {
                    color = Color.GREEN
                    width = 6f
                    setPoints(mutableListOf(point))
                }
                mapView.overlays.add(currentTrackPolyline)
                polylines.add(currentTrackPolyline!!)
            } else if (polyline.actualPoints.isEmpty()) {
                polyline.setPoints(mutableListOf(point))
            } else {
                polyline.addPoint(point)
            }

            mapView.invalidate()
        }catch (e:NullPointerException){
            return
        }
    }



    fun drawFullTrack(points: List<GeoPoint>) {
        clearTrack()
        if (points.isEmpty()){
            return
        }

        val polyline = Polyline().apply {
            setPoints(points)
            color = Color.GREEN
            width = 6f
        }
        polylines.add(polyline)
//        currentTrackPolyline = polyline
        mapView.overlays.add(polyline)
        mapView.invalidate()
    }

    private fun clearTrack() {
        mapView.overlays.removeAll(polylines)
        polylines.clear()
        currentTrackPolyline = null
        mapView.invalidate()
    }

    fun restoreTrackIfNeeded(viewModel: MapViewModel) {
        viewModel.trackedPoints.value.let { points ->
            if (points.isNotEmpty()) {
                drawFullTrack(points)
            }
        }
    }

    fun addMarkerAtLocation(point: GeoPoint, title: String = "", description: String = "") {
        val marker = Marker(mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            subDescription = description
        }
        mapView.overlays.add(marker)
        mapView.invalidate()
    }


    fun addDeletableMarkerAtLocation(point: GeoPoint, title: String = "", description: String = ""){
        val marker = Marker(mapView)
        marker.position = point
        marker.title = title
        marker.subDescription = description
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        // Usa un InfoWindow personalizzato
        marker.infoWindow = object : InfoWindow(R.layout.custom_popup_layout, mapView) {
            override fun onOpen(item: Any?) {
                val view = mView
                val btnRemove = view.findViewById<ImageButton>(R.id.btnRemoveMarker)
                btnRemove.setOnClickListener {
                    mapView.overlays.remove(marker)
                    close()
                    mapView.invalidate()
                }
            }

            override fun onClose() {

            }
        }

        mapView.overlays.add(marker)
        mapView.invalidate()
    }


    fun showAddressOnMap(address: String): GeoPoint? {
        val point = getLocationFromAddress(address)
        point?.let {
            addDeletableMarkerAtLocation(it, title = "Indirizzo", description = address)
            mapView.controller.animateTo(it)
        }
        return point
    }

    fun returnToMyLocation() {
        if (::locationOverlay.isInitialized && locationOverlay.myLocation != null) {
            mapView.controller.animateTo(locationOverlay.myLocation)
            mapView.invalidate()
        } else {
            setupLocationOverlay()
        }
    }

    fun drawRecordedTrack(points: List<GeoPoint>, color: Int = Color.parseColor("#1A237E")) {
        val polyline = Polyline().apply {
            setPoints(points)
            this.color = color
            width = 6f
        }
        polylines.add(polyline)
        mapView.overlays.add(polyline)
        mapView.invalidate()
    }

    private fun getLocationFromAddress(strAddress: String): GeoPoint? {
        val coder = Geocoder(context, Locale.getDefault())
        return try {
            val addressList = coder.getFromLocationName(strAddress, 1)
            if (!addressList.isNullOrEmpty()) {
                val location = addressList[0]
                GeoPoint(location.latitude, location.longitude)
            } else null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
