package com.example.travelcompanion

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.*

class TripDetailsFragment : DialogFragment() {

    private lateinit var viewModel: TripViewModel
    private lateinit var tvTripTitle: TextView
    private lateinit var tvTripNotes: TextView
    private lateinit var tvTripStartDate: TextView
    private lateinit var tvTripEndDate: TextView
    private lateinit var tvTripDistance: TextView
    private lateinit var tvTripAscent: TextView
    private lateinit var tvTripDescent: TextView
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var travelPointAdapter: TravelPointAdapter
    private lateinit var btnViewOnMap: Button
    private val df = DecimalFormat("#.##")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_trip_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        val tripId = arguments?.getString("tripId")?.toLongOrNull() ?: return
        val factory = TripViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]

        tvTripTitle = view.findViewById(R.id.tvTripTitle);
        tvTripNotes = view.findViewById(R.id.tvTripNotes);
        tvTripStartDate = view.findViewById(R.id.tvTripStartDate);
        tvTripEndDate = view.findViewById(R.id.tvTripEndDate);
        tvTripAscent = view.findViewById(R.id.tvTripAscent);
        tvTripDescent = view.findViewById(R.id.tvTripDescent);
        tvTripDistance = view.findViewById(R.id.tvTripDistance);
        recyclerView = view.findViewById(R.id.rvTravelPoints)
        btnViewOnMap = view.findViewById(R.id.btnViewOnMap)

        viewModel.getTripWithPoints(tripId).observe(viewLifecycleOwner) { tripWithPoints ->
            tripWithPoints?.let { it ->
//                if (it.gpsPoints.isEmpty()) {
//                    if (it.travelPoints.size >= 2) {
//                        lifecycleScope.launch {
//                            val generatedGeoPoints = getRouteFromORS(it.travelPoints.map { point ->
//                                GeoPoint(
//                                    point.latitude,
//                                    point.longitude
//                                )
//                            })
//                            if (generatedGeoPoints != null) {
//                                val generatedGpsPoints = generatedGeoPoints.map { point ->
//                                    GpsPoint(
//                                        tripId = tripId,
//                                        latitude = point.latitude,
//                                        longitude = point.longitude,
//                                        altitude = point.altitude,
//                                        timestamp = System.currentTimeMillis()
//                                    )
//                                }
//                                viewModel.insertGpsPoint(generatedGpsPoints)
//                                it.gpsPoints = generatedGpsPoints
//                            } else {
//                                Toast.makeText(
//                                    requireContext(),
//                                    "Non è stato possibilile ottenre i punti dal servizio openrouteservice",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//                                return@launch
//                            }
//                        }
//                    } else {
//                        Toast.makeText(
//                            requireContext(),
//                            "Non è stato possibile generare un percorso",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        return@observe
//                    }
//                }
//
//                if (it.gpsPoints.isEmpty()) {
//                    return@observe
//                }

                // Mostra i dati nel layout
                tvTripTitle.text = "title: " + it.trip.title
                tvTripNotes.text = "notes: " + it.trip.notes

                val travelPointsList: MutableList<TravelPoint> = it.travelPoints.sortedByDescending { y -> y.timeStamp }.toMutableList()

                val format: String = "dd/MM/yyyy"
                val sdf = SimpleDateFormat(format, Locale.getDefault())

                if(it.travelPoints.size>=2) {
                    tvTripStartDate.text =
                        "start date: " + sdf.format(it.travelPoints.first().timeStamp)
                    tvTripEndDate.text = "end date:" + sdf.format(it.travelPoints.last().timeStamp)
                }else{
                    Toast.makeText(context, "Viaggio corrotto eliminalo", Toast.LENGTH_SHORT).show()
                }
                // Calcolo esempio:
                val distance = calculateTripDistance(it.gpsPoints)
                val ascent = calculateAscentGain(it.gpsPoints)
                val descent = calculateDescentGain(it.gpsPoints)

                tvTripDistance.text = "distance: $distance km"
                tvTripAscent.text = "ascent: $ascent m"
                tvTripDescent.text = "descent: $descent m"

                // Adapter TravelPoints
                recyclerView.layoutManager = LinearLayoutManager(requireContext())
                travelPointAdapter = TravelPointAdapter(
                    points = travelPointsList,
                    onStartDrag = { viewHolder -> itemTouchHelper.startDrag(viewHolder) },
                    onDelete = { position ->
                        val itemToRemove = travelPointsList[position]
                        // Rimuovo il TravelPoint dal DB
                        if(travelPointsList.size>2) {
                            lifecycleScope.launch {
                                viewModel.deleteTravelPoint(itemToRemove)
                                // Rimuovo il TravelPoint dalla lista e aggiorno la RecyclerView
                                withContext(Dispatchers.Main) {
                                    travelPointsList.removeAt(position)
                                    recyclerView.adapter?.notifyItemRemoved(position)
                                }
                            }
                        }else{
                            Toast.makeText(context, "Non è possibile avere meno di due Travel Point", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                recyclerView.adapter = travelPointAdapter
            }
        }

        itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                travelPointAdapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not needed
            }

            override fun isLongPressDragEnabled(): Boolean = false
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)


        btnViewOnMap.setOnClickListener {
            val bundle = Bundle().apply {
                putString("tripId", tripId.toString())
            }

            findNavController().navigate(
                R.id.action_tripDetailsFragment_to_mapFragment,
                bundle
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Torna al fragment precedente
                findNavController().navigateUp()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Calcola la distanza percorsa durante il viaggio in Km
     */
    private fun calculateTripDistance(points: List<GpsPoint>): Double {
        if (points.size < 2) {
            return 0.0
        }

        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            totalDistance += haversineDistance(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        }

        return df.format(totalDistance).toDouble()
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Raggio della Terra in km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c // Distanza in km
    }

    private fun calculateAscentGain(points: List<GpsPoint>): Double {
        val ascent = points.zipWithNext { a, b ->
            (b.altitude - a.altitude).takeIf { it > 0 } ?: 0.0
        }.sum()

        return df.format(ascent).toDouble()
    }

    private fun calculateDescentGain(points: List<GpsPoint>): Double {
        val descent = points.zipWithNext { a, b ->
            (b.altitude - a.altitude).takeIf { it < 0 } ?: 0.0
        }.sum()

        return df.format(descent).toDouble()
    }

    suspend fun getRouteFromORS(points: List<GeoPoint>): List<GeoPoint>? {
        val API_KEY = BuildConfig.API_KEY_ORS

        val client = OkHttpClient()
        val coordsArray = points.map { listOf(it.longitude, it.latitude) }
        val json = """{"coordinates": $coordsArray}""".trimIndent()

        val request = Request.Builder()
            .url("https://api.openrouteservice.org/v2/directions/driving-car/geojson")
            .addHeader("Authorization", API_KEY)
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        var results: List<GeoPoint>? = null
        if (isNetworkAvailable(requireContext())) {
            results = withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonObj = JSONObject(response.body?.string() ?: return@withContext null)
                    val coords = jsonObj
                        .getJSONArray("features")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates")

                    List(coords.length()) { i ->
                        val coord = coords.getJSONArray(i)
                        GeoPoint(coord.getDouble(1), coord.getDouble(0))
                    }
                } else {
                    null
                }
            }
        }
        return results
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

}