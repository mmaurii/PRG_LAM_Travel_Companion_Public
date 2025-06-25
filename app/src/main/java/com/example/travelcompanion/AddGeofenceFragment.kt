package com.example.travelcompanion

import GeofenceLocationDao
import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.travelcompanion.databinding.FragmentAddGeofenceBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class AddGeofenceFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentAddGeofenceBinding? = null
    private val binding get() = _binding!!

    private lateinit var dao: GeofenceLocationDao
    private lateinit var repository: GeofenceRepository
    private lateinit var factory: GeofenceViewModelFactory
    private lateinit var viewModel: GeofenceViewModel
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var onPermissionsResult: ((Boolean) -> Unit)? = null

    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private lateinit var permissionManager: PermissionManager
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private var mapReady = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddGeofenceBinding.inflate(inflater, container, false)

        dao = AppDatabase.getDatabase(requireContext()).geofenceDao()
        repository = GeofenceRepository(dao)
        factory = GeofenceViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[GeofenceViewModel::class.java]

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.all { it.value }
            onPermissionsResult?.invoke(granted)
        }

        permissionManager = PermissionManager(this)

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPickLocation.setOnClickListener {
            // Se la mappa è già pronta, zoomiamo su posizione già selezionata o posizione di default (Roma)
            if (mapReady) {
                val pos = if (selectedLat != null && selectedLng != null) {
                    LatLng(selectedLat!!, selectedLng!!)
                } else {
                    LatLng(41.9028, 12.4964)
                }
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 12f))
            }
        }

        binding.btnSaveGeofence.setOnClickListener {
            val name = binding.etName.text.toString()
            val radius = binding.etRadius.text.toString().toFloatOrNull()

            if (name.isBlank() || radius == null || selectedLat == null || selectedLng == null) {
                Toast.makeText(requireContext(), "Inserisci tutti i dati", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val geofencePoint = GeofencePoint(
                name = name,
                latitude = selectedLat!!,
                longitude = selectedLng!!,
                radius = radius
            )

            val geofence = Geofence.Builder()
                .setRequestId(geofencePoint.name)
                .setCircularRegion(selectedLat!!, selectedLng!!, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(
                    requireContext(),
                    "Permessi mancanti per il geofencing",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                viewModel.addGeofencePoint(geofencePoint)

                Toast.makeText(requireContext(), "Geofence salvato", Toast.LENGTH_SHORT).show()

                LocationServices.getGeofencingClient(requireContext())
                    .addGeofences(request, pendingIntent)
                    .addOnSuccessListener {
                        Log.d("AddGeofenceFragment", "Geofence aggiunto con successo")
                        Toast.makeText(
                            requireContext(),
                            "Geofence attivato con successo",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Naviga indietro o aggiorna UI
                        findNavController().popBackStack()
                    }
                    .addOnFailureListener { e ->
                        Log.e("AddGeofenceFragment", "Errore nell'aggiunta del geofence", e)

                        val message = when ((e as? ApiException)?.statusCode) {
                            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "Geofence non disponibile"
                            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Troppi geofence"
                            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Troppi PendingIntent"
                            else -> "Errore sconosciuto: ${e.localizedMessage}"
                        }

                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

                    }
            }

        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        mapReady = true

        // Zoom default sulla posizione attuale o Roma
        val defaultLocation = LatLng(41.9028, 12.4964)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        googleMap?.setOnMapClickListener { latLng ->
            // Pulisci marker precedenti e aggiungi quello nuovo
            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(latLng).title("Posizione selezionata"))

            selectedLat = latLng.latitude
            selectedLng = latLng.longitude

            Toast.makeText(
                requireContext(),
                "Posizione selezionata: ${latLng.latitude}, ${latLng.longitude}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Gestione ciclo vita MapView
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
        _binding = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
