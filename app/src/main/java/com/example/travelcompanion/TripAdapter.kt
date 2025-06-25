package com.example.travelcompanion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Delete
import java.time.LocalDate

class TripAdapter(
    private var tripList: MutableList<Trip>,
    private val onTripClick: (Trip) -> Unit,
    private val onDelete: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {
    private val filteredList = mutableListOf<Trip>() // Dati attualmente visibili


    inner class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tripNameTextView: TextView = itemView.findViewById(R.id.trip_name_text_view)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTrip)

        fun bind(trip: Trip) {
            tripNameTextView.text = trip.title
            tripNameTextView.setOnClickListener {
                onTripClick(trip)
            }

            btnDelete.setOnClickListener {
                onDelete(trip)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(tripList[position])
    }

    override fun getItemCount(): Int = tripList.size

    fun submitList(trips: List<Trip>) {
        tripList = trips.toMutableList()
        notifyDataSetChanged()
    }
}
