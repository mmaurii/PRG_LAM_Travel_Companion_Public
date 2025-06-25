package com.example.travelcompanion

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.heatmaps.HeatmapTileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HeatMapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var permissionManager: PermissionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var btnFilter: Button
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var btnDeleteFilter: ImageButton
    private lateinit var tripViewModel: TripViewModel

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var onPermissionsResult: ((Boolean) -> Unit)? = null


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

        return inflater.inflate(R.layout.fragment_heat_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = TripViewModelFactory(requireActivity().application)
        tripViewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]

        btnFilter = view.findViewById(R.id.btnFilter)
        tvStartDate = view.findViewById(R.id.tvStartDate)
        tvEndDate = view.findViewById(R.id.tvEndDate)
        btnDeleteFilter = view.findViewById(R.id.btnDeleteFilter)

        // Inizializza DAO e location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        permissionManager = PermissionManager(this)

        // Crea dinamicamente la mappa nel FrameLayout
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.heat_map_fragment, mapFragment)
            .commit()

        childFragmentManager.executePendingTransactions()
        mapFragment.getMapAsync(this)

        tvStartDate.setOnClickListener {
            setDateField(tvStartDate)
        }

        tvEndDate.setOnClickListener {
            setDateField(tvEndDate)
        }

        btnFilter.setOnClickListener {
            val startDate = tvStartDate.text.toString().takeIf { it.isNotEmpty() }
            val endDate = tvEndDate.text.toString().takeIf { it.isNotEmpty() }

            lifecycleScope.launch {
                loadAndDisplayHeatMap(startDate, endDate)
            }
        }


        btnDeleteFilter.setOnClickListener {
            tvStartDate.text = ""
            tvEndDate.text = ""

            lifecycleScope.launch {
                loadAndDisplayHeatMap() // senza filtri
            }
        }

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Posiziona la mappa sull'utente, se possibile
        // Controllo permesso
        permissionManager.requestPermissionsIfNeeded(
            requireContext(),
            permissionLauncher,
            PermissionManager.LOCATION_PERMISSIONS
        )
        onPermissionsResult = { granted ->
            if (granted) {
                mMap.isMyLocationEnabled = true
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    val target = if (location != null) {
                        LatLng(location.latitude, location.longitude)
                    } else {
                        LatLng(41.9028, 12.4964) // Fallback: Roma
                    }
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 14f))
                }
            } else {
                val fallback = LatLng(41.9028, 12.4964)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f))
                Toast.makeText(requireContext(), "Non conosco la tua posizione", Toast.LENGTH_SHORT).show()
            }
        }



        // Costruisci la heatmap con i dati del DB
        lifecycleScope.launch {
            loadAndDisplayHeatMap()
        }
    }

    private suspend fun loadAndDisplayHeatMap(startDate: String? = null, endDate: String? = null) {
        val allPoints = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())

            //eliminino eventuali viaggi corrotti
            tripViewModel.deleteCorruptedTrip()

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val startMillis = startDate?.let { sdf.parse(it)?.time }
            val endMillis = endDate?.let { sdf.parse(it)?.time }

            val gps = db.tripDao().getGpsPoints().filter {
                val timestamp = it.timestamp
                (startMillis == null || timestamp >= startMillis) &&
                        (endMillis == null || timestamp <= endMillis)
            }.map {
                LatLng(it.latitude, it.longitude)
            }

            val travel = db.tripDao().getTravelPoints().filter {
                val timestamp = it.timeStamp // Assumendo che sia un Long
                (startMillis == null || timestamp >= startMillis) &&
                        (endMillis == null || timestamp <= endMillis)
            }.map {
                LatLng(it.latitude, it.longitude)
            }

            gps + travel
        }

        withContext(Dispatchers.Main) {
            mMap.clear()
            if (allPoints.isEmpty()) {
                Toast.makeText(requireContext(), "Nessun punto trovato nel periodo selezionato", Toast.LENGTH_SHORT).show()
                return@withContext
            }

            val provider = HeatmapTileProvider.Builder()
                .data(allPoints)
                .radius(50)
                .build()

            mMap.addTileOverlay(TileOverlayOptions().tileProvider(provider))
        }
    }

    private fun setDateField(date:TextView) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                date.text = String.format("%04d-%02d-%02d", year, month + 1, day)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
}
