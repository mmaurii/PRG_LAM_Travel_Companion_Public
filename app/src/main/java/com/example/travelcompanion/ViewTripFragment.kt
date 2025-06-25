package com.example.travelcompanion

import AppDatabase
import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.coroutines.coroutineContext

class ViewTripFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TripAdapter
    private lateinit var database: AppDatabase
    private val trips = mutableListOf<Trip>()
    private lateinit var viewModel: TripViewModel
    private lateinit var btnFilter: Button
    private lateinit var tvStartDate: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var btnDeleteFilter: ImageButton


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_trip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerViewTrips)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val factory = TripViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[TripViewModel::class.java]

        adapter = TripAdapter(
            tripList = trips,
            onTripClick = {trip:Trip ->
                val action = ViewTripFragmentDirections
                    .actionViewTripFragmentToTripInfoDialogFragment(tripId = trip.id.toString())
                findNavController().navigate(action)
            },
            onDelete = {trip:Trip -> viewModel.deleteTrip(trip)}
        )
        recyclerView.adapter = adapter

        val tripViewModel = ViewModelProvider(this)[TripViewModel::class.java]

        viewModel.displayedTrips.observe(viewLifecycleOwner) { tripList ->
            adapter.submitList(tripList.map { it.trip }.filter { it.title.isNotEmpty() })
        }

        btnFilter = view.findViewById(R.id.btnFilter)
        tvStartDate = view.findViewById(R.id.tvStartDate)
        tvEndDate = view.findViewById(R.id.tvEndDate)
        btnDeleteFilter = view.findViewById(R.id.btnDeleteFilter)

        tvStartDate.setOnClickListener {
            setDateField(tvStartDate)
        }

        tvEndDate.setOnClickListener {
            setDateField(tvEndDate)
        }

        btnFilter.setOnClickListener {
            val startDate = tvStartDate.text.toString()
            val endDate = tvEndDate.text.toString()

            if(startDate.isNotEmpty() && endDate.isNotEmpty()){
                viewModel.filterTrips(startDate, endDate)
            }else{
                Toast.makeText(context, "Inserisci entrambe le date", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        btnDeleteFilter.setOnClickListener {
            tvStartDate.text = ""
            tvEndDate.text = ""

            viewModel.showAllTrips()
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
