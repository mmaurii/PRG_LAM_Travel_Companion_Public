package com.example.travelcompanion

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class MapFragment : Fragment() {
    private lateinit var mapView: MapView
    private lateinit var btnStartTracking: FloatingActionButton
    private lateinit var btnStopTracking: FloatingActionButton
    private lateinit var btnTerminateTracking: FloatingActionButton
    private lateinit var btnInterestPoint: FloatingActionButton
    private lateinit var photoUri: Uri
    private lateinit var tripViewModel: TripViewModel
    private lateinit var mapViewModel: MapViewModel
    private lateinit var tvLocation: AutoCompleteTextView
    private lateinit var btnSearch: FloatingActionButton
    private lateinit var btnGoToMyPosition: FloatingActionButton
    private lateinit var llSearch: LinearLayout
    private lateinit var tvStopwatch: TextView
    private var tripId: Long? = null
    private var isTimerObserved = false
    private lateinit var mapViewManager:MapViewManager
    private lateinit var permissionManager: PermissionManager

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                showSaveTravelPointDialog(::saveTravelPoint)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.all { it.value }
            onPermissionsResult?.invoke(granted)
        }

        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var onPermissionsResult: ((Boolean) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        permissionManager = PermissionManager(this)
        super.onViewCreated(view, savedInstanceState)
        val factory = TripViewModelFactory(requireActivity().application)
        tripViewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]
        tvLocation = view.findViewById(R.id.tvLocation)
        tripId = arguments?.getString("tripId")?.toLongOrNull()
        btnStartTracking = view.findViewById(R.id.btnStartTracking);
        btnStopTracking = view.findViewById(R.id.btnStopTracking)
        btnTerminateTracking = view.findViewById(R.id.btnTerminateTracking)
        btnInterestPoint = view.findViewById(R.id.btnInterestPoint);
        btnSearch = view.findViewById(R.id.btnSearch)
        btnGoToMyPosition = view.findViewById(R.id.btnGoToMyPosition)
        llSearch = view.findViewById(R.id.llSearch)
        tvStopwatch = view.findViewById(R.id.tcStopwatch)
        mapView = view.findViewById(R.id.map)

        mapViewModel = ViewModelProvider(requireActivity())[MapViewModel::class.java]

        this.mapViewManager = MapViewManager(requireContext(), lifecycleScope, mapView)

        if (permissionManager.requestAllNeededPermissions(requireContext())) {
            this.mapViewManager.setupLocationOverlay()
            if (tripId == null) {
                this.mapViewManager.bindToViewModel(mapViewModel)
                this.mapViewManager.restoreTrackIfNeeded(mapViewModel)
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Permessi necessari per visualizzare la tua posizione",
                Toast.LENGTH_SHORT
            ).show()
        }

        if (tripId != null) {
            tripViewModel.getTripWithPoints(tripId!!)
                .observe(viewLifecycleOwner) { tripWithPoints ->
                    tripWithPoints?.let {
                        if (it.gpsPoints.isNotEmpty()) {
                            val geoPoints: List<GeoPoint> = it.gpsPoints.map { point ->
                                GeoPoint(
                                    point.latitude,
                                    point.longitude,
                                    point.altitude
                                )
                            }

//                            mapManager.clearTrack()
                            this.mapViewManager.drawRecordedTrack(geoPoints)
                        }

                        it.travelPoints.forEach { point ->
                            val geoPoint = GeoPoint(
                                point.latitude,
                                point.longitude,
                            )

                            this.mapViewManager.addMarkerAtLocation(geoPoint, point.title, point.notes)
                        }
                    }
                }
        }else{
            observeStatusTracking()

            if(mapViewModel.isTracking.value){
                observeTimer()
                tvStopwatch.isVisible = true
                llSearch.children.forEach { it -> it.isVisible = false }
            }
        }

        btnStartTracking.setOnClickListener {
                if (tripId == null) {
                    observeTimer()

                    permissionManager.requestPermissionsIfNeeded(
                        requireContext(),
                        permissionLauncher,
                        PermissionManager.FOREGROUND_PERMISSIONS
                    )
                    onPermissionsResult = { granted ->
                        if (granted) {
                            if (mapViewModel.isTracking.value) {
                                mapViewModel.resumeTracking()
                            } else {
                                //config start tracking
                                CoroutineScope(Dispatchers.IO).launch {
                                    startTrackingService(requireContext())
                                }

                                mapViewModel.startTracking()
                            }

                            llSearch.children.forEach { it -> it.isVisible = false }
                            tvStopwatch.isVisible = true
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Permessi necessari per avviare il tracking",
                                Toast.LENGTH_SHORT
                            ).show()                        }
                    }
                } else {
                    showEndTripDialog(
                        onNewTrip = {
                            mapViewModel.pauseTracking()
                            mapViewModel.stopTracking()
                            stopTrackingService(view.context)
                            deleteTripIsBeingRecorded()
                            tripId = null

                            //imposto l'interfaccia per poter iniziare a registrare una nuova traccia
                            mapViewManager.bindToViewModel(mapViewModel)
                            observeStatusTracking()
                        }
                    )
                }
        }

        btnStopTracking.setOnClickListener {
            stopTrackingService(view.context)
            mapViewModel.pauseTracking()
        }

        btnTerminateTracking.setOnClickListener {
            stopTrackingService(view.context)
            mapViewModel.stopTracking()
            tvStopwatch.isVisible = false
            llSearch.children.forEach { it -> it.isVisible = true }

            // Mostra popup per inserire i dati del trip
            showSaveTripDialog(::saveTripToDb)
        }

        btnInterestPoint.setOnClickListener {
            val dir = File(requireContext().filesDir, "photos").apply { mkdirs() }
            val photoFile = File.createTempFile("IMG_", ".jpg", dir)
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                photoFile
            )

            takePictureLauncher.launch(photoUri)
        }

        btnSearch.setOnClickListener {
            val addressInput = tvLocation.text.toString().trim()
            if (addressInput.isNotEmpty()) {
                this.mapViewManager.showAddressOnMap(addressInput)
            }
        }

        btnGoToMyPosition.setOnClickListener {
            permissionManager.requestPermissionsIfNeeded(
                requireContext(),
                permissionLauncher,
                PermissionManager.LOCATION_PERMISSIONS
            )
            onPermissionsResult = { granted ->
                if (granted) {
                    mapViewManager.returnToMyLocation()
                } else {
                    Toast.makeText(requireContext(), "Permessi non concessi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeStatusTracking() {
        lifecycleScope.launchWhenStarted {
            combine(mapViewModel.isPaused, mapViewModel.isTracking) { isPaused, isTracking ->
                Pair(isPaused, isTracking)
            }.collectLatest { (isPaused, isTracking) ->
                if (isTracking) {
                    if (isPaused) {
                        btnStopTracking.isVisible = false
                        btnStartTracking.isVisible = true
                        btnTerminateTracking.isVisible = true
                    } else {
                        btnStartTracking.isVisible = false
                        btnStopTracking.isVisible = true
                        btnTerminateTracking.isVisible = false
                        btnInterestPoint.isVisible = true
                    }
                } else {
                    btnStartTracking.isVisible = true
                    btnTerminateTracking.isVisible = false
                    btnInterestPoint.isVisible = false
                    btnStopTracking.isVisible = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        isTimerObserved = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        this.mapViewManager.unbind()
    }

    private suspend fun startTrackingService(context: Context) {
        val newTrip = Trip(
            title = "",
            date = System.currentTimeMillis().toString(),
            notes = "",
            type = ""
        )

        val db = AppDatabase.getDatabase(requireContext())

        //elimina il vecchio trip se non inizializzato
        db.tripDao().deleteUninitializeTrip(mapViewModel.newTripId)

        mapViewModel.newTripId = db.tripDao().insertTrip(newTrip) // salva e ottieni ID

        val intent = Intent(context, GpsTrackingService::class.java)
        intent.putExtra("tripId", mapViewModel.newTripId)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopTrackingService(context: Context) {
        val intent = Intent(context, GpsTrackingService::class.java)
        context.stopService(intent)
    }

    // Aggiungi un terzo parametro alla lambda
    private fun showSaveTripDialog(save: (String, String, String) -> Unit) {
        // Verifica giorni diversi
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val points = db.tripDao().getPointsForTrip(mapViewModel.newTripId)

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val distinctDays = points.map { point ->
                sdf.format(point.timestamp)
            }.distinct()

            val disableOptions = distinctDays.size > 1

            SaveTripDialogFragment(
                onSave = { title, notes, selectedOption ->
                    save(title, notes, selectedOption)
                },
                onCancel = { deleteTripIsBeingRecorded() },
                disableRadioOptions = disableOptions
            ).show(childFragmentManager, "TripInfoDialog")
        }
    }

    // Aggiungi un terzo parametro alla lambda
    private fun showSaveTravelPointDialog(save: (String, String) -> Unit) {
        SaveTravelPointDialogFragment(
            onSave = { title, notes ->
                save(title, notes)
            },
            onCancel = { deleteTripIsBeingRecorded() }
        ).show(childFragmentManager, "TripInfoDialog")
    }


    private fun saveTripToDb(title: String, notes: String, type: String) {

        val newTrip = Trip(
            id = mapViewModel.newTripId,
            title = title,
            notes = notes,
            date = System.currentTimeMillis().toString(),
            type = type
        )

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(requireContext())
            //salvo il trip
            db.tripDao().updateTrip(newTrip)

            //ottengo la traccia appena creata
            val gpsTrack = db.tripDao().getPointsForTrip(mapViewModel.newTripId)

            //salvo i travel point fondamentali, inizio e fine
            val startTravelPoint = TravelPoint(
                title = "start point",
                timeStamp = gpsTrack.first().timestamp,
                notes = "point where your trip begins",
                foto = "",
                latitude = gpsTrack.first().latitude,
                longitude = gpsTrack.first().longitude,
                tripId = mapViewModel.newTripId
            )
            val endTravelPoint = TravelPoint(
                title = "end point",
                timeStamp = gpsTrack.last().timestamp,
                notes = "point where your trip begins",
                foto = "",
                latitude = gpsTrack.last().latitude,
                longitude = gpsTrack.last().longitude,
                tripId = mapViewModel.newTripId
            )
            db.tripDao().insertTravelPoint(startTravelPoint)
            db.tripDao().insertTravelPoint(endTravelPoint)
        }

        findNavController().navigate(
            R.id.action_map
        )
    }


    @SuppressLint("MissingPermission")
    private fun saveTravelPoint(title: String, notes: String) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        permissionManager.requestPermissionsIfNeeded(
            requireContext(),
            permissionLauncher,
            PermissionManager.LOCATION_PERMISSIONS
        )
        onPermissionsResult = { granted ->
            if (granted) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val travelPoint = TravelPoint(
                            title = title,
                            notes = notes,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            tripId = mapViewModel.newTripId,
                            timeStamp = System.currentTimeMillis(),
                            foto = photoUri.toString()
                        )

                        CoroutineScope(Dispatchers.IO).launch {
                            val db = AppDatabase.getDatabase(requireContext())
                            db.tripDao().insertTravelPoint(travelPoint)
                        }

                        Toast.makeText(requireContext(), "Interest point saved!", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permessi necessari per salvare il luogo, luogo eliminato",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showEndTripDialog(onNewTrip: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Termina registrazione")
            .setMessage("Vuoi terminare la registrazione del trip attuale e iniziarne uno nuovo, oppure continuare a registrare il trip corrente?")
            .setPositiveButton("Termina e avvia nuovo trip") { dialog, _ ->
                onNewTrip()
                dialog.dismiss()
            }
            .setNegativeButton("Continua registrazione") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun deleteTripIsBeingRecorded() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.tripDao().deleteTravelPointByTripId(mapViewModel.newTripId)
            db.tripDao().deleteUninitializeTrip(mapViewModel.newTripId)
            db.tripDao().deletePointsByTripId(mapViewModel.newTripId)
        }
    }

    private fun observeTimer() {
        if (!isTimerObserved) {
            isTimerObserved = true

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    mapViewModel.formattedTime.collect { timeStr ->
                        tvStopwatch.text = timeStr
                    }
                }
            }
        }
    }

}