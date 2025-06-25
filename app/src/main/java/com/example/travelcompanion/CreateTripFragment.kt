package com.example.travelcompanion

import android.app.DatePickerDialog
import android.content.Context
import android.icu.util.Calendar
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.io.IOException
import java.util.Date
import java.util.Locale


class CreateTripFragment : Fragment() {
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etDate: EditText;
    private lateinit var btnAddPoint: Button;
    private lateinit var btnSubmit: Button;
    private lateinit var recyclerView: RecyclerView;
    private lateinit var pointName: EditText;
    private lateinit var pointAdapter: TravelPointAdapter;
    private val pointList = mutableListOf<TravelPoint>()
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var itemId: Long = 0;
    private lateinit var viewModel: TripViewModel
    private lateinit var rbTripType: RadioGroup
    private var type:String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_create_trip, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = TripViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]

        etDate = view.findViewById(R.id.etDate)
        pointName = view.findViewById(R.id.etPoint);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnAddPoint = view.findViewById(R.id.btnAddPoint);
        recyclerView = view.findViewById(R.id.recView);
        rbTripType = view.findViewById(R.id.rbTripType);

        pointAdapter = TravelPointAdapter(
            points = pointList,
            compactMode = true,
            onStartDrag = { viewHolder ->
                if(getString(R.string.multi_day_trip) != type) {
                    itemTouchHelper.startDrag(viewHolder)
                }
            },
            onDelete = { position ->
                pointList.removeAt(position)
                pointAdapter.notifyItemRemoved(position)

                if(pointList.size==0){
                    // riguarda non funziona
                    rbTripType.children.forEach { rbView -> rbView.isEnabled = true }
                }
            }
        )

        recyclerView.adapter = pointAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        if (savedInstanceState != null) {
            val savedDate = savedInstanceState.getString("date")
            etDate.setText(savedDate)
            val savedPointName = savedInstanceState.getString("pointName")
            pointName.setText(savedPointName)

            @Suppress("DEPRECATION")
            val savedList = savedInstanceState
                .getParcelableArrayList<Parcelable>("recycler") as? List<TravelPoint>
            if (savedList != null) {
                pointAdapter.setData(savedList)
            }

            savedInstanceState.getParcelable<Parcelable>("recycler_state")?.let { state ->
                recyclerView.layoutManager?.onRestoreInstanceState(state)
            }
        }


        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            when (type) {
                getString(R.string.multi_day_trip) -> {
                    datePicker.show()
                }
                getString(R.string.day_trip) -> {
                    if (pointList.size == 0) {
                        datePicker.show()
                    } else {
                        Toast.makeText(
                            context,
                            "la data è già stata selezionata",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                getString(R.string.local_trip) -> {
                    if (pointList.size == 0) {
                        datePicker.show()
                    } else {
                        Toast.makeText(
                            context,
                            "la data è già stata selezionata",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                else -> {
                    Toast.makeText(context, "seleziona il tipo di viaggio", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnSubmit.setOnClickListener {
            Log.d(
                "TripDebug",
                "Trip con ${pointList.size} punti"
            )
            if (this.pointList.size > 1) {
                // save the trip
                etTitle = view.findViewById<EditText>(R.id.etTitle)
                etDescription = view.findViewById<EditText>(R.id.etDescription)
                val strTitle = etTitle.text.toString()
                val strDescription = etDescription.text.toString()

                Log.d(
                    "TripDebug",
                    "point list size: ${pointList.size} punti"
                )
                if (strTitle.isNotEmpty()) {
                    if (strDescription.isNotEmpty()) {
                        val trip = Trip(
                            title = strTitle,
                            notes = strDescription,
                            date = System.currentTimeMillis().toString(),
                            type = type
                        )

                        lifecycleScope.launch {
                            val tripId = viewModel.insertTrip(trip)

                            Log.d(
                                "TripDebug",
                                "point list size: ${pointList.size} punti"
                            )

                            pointList.forEach { travelPoint ->
                                travelPoint.tripId = tripId
                                viewModel.insertTravelPoint(travelPoint)
                            }

                            Log.d(
                                "TripDebug",
                                "Trip ID: $tripId con ${pointList.size} punti"  
                            )
                            etTitle.setText("")
                            etDescription.setText("")
                            pointList.clear()
                            pointAdapter.notifyDataSetChanged()
                            etDate.setText("");
                            pointName.setText("")
                            rbTripType.children.forEach { rbView -> rbView.isEnabled = true }
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Aggiungi almeno due punti al viaggio", Toast.LENGTH_SHORT)
                    .show()
            }

        }

        btnAddPoint.setOnClickListener {
            val strDate = etDate.text.toString()

            if (strDate.isNotEmpty()) {
                val addressInput = pointName.text.toString().trim()
                if (addressInput.isNotEmpty()) {
                    lifecycleScope.launch {
                        val geoPoint = getLocationFromAddress(requireContext(), addressInput)

                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

                        val timeStamp = sdf.parse(strDate)?.time
                        if (timeStamp!=null) {
                            if (geoPoint != null) {
                                val travelPoint = TravelPoint(
                                    title = addressInput,
                                    timeStamp = timeStamp,
                                    notes = "",
                                    foto = "",
                                    latitude = geoPoint.latitude,
                                    longitude = geoPoint.longitude,
                                )


                                pointList.add(travelPoint)
                                pointAdapter.notifyItemInserted(pointList.size - 1)

                                Log.d(
                                    "TripDebug",
                                    "point list size: ${pointList.size} punti"
                                )
//                            pointAdapter.addPoint(travelPoint)
                                pointName.setText("") // Clear input

                                rbTripType.children.forEach { rbView -> rbView.isEnabled = false }
                            } else {
                                Toast.makeText(context, "Indirizzo non trovato", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }else{
                            Toast.makeText(context, "Data non valida", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
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
                pointAdapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Not needed
            }

            override fun isLongPressDragEnabled(): Boolean = false
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)


        rbTripType.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioButton = view.findViewById<RadioButton>(checkedId)
            type = selectedRadioButton.text.toString()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("date", etDate.getText().toString())
        outState.putString("pointName", pointName.getText().toString())
        val layoutManagerState = recyclerView.layoutManager?.onSaveInstanceState()
        outState.putParcelable("recycler_state", layoutManagerState)
        outState.putParcelableArrayList("recycler", ArrayList(pointAdapter.getData()))
    }

    private fun getLocationFromAddress(context: Context, strAddress: String): GeoPoint? {
        val coder = Geocoder(context, Locale.getDefault())
        return try {
            val addressList = coder.getFromLocationName(strAddress, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val location = addressList[0]
                GeoPoint(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}