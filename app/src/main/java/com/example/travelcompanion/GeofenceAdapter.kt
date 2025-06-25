package com.example.travelcompanion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class GeofenceAdapter(
    private val onDeleteClick: (GeofencePoint) -> Unit
) : ListAdapter<GeofencePoint, GeofenceAdapter.GeofenceViewHolder>(DiffCallback) {

    inner class GeofenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(point: GeofencePoint) {
            itemView.findViewById<TextView>(R.id.tvGeofenceName).text = point.name
            itemView.findViewById<TextView>(R.id.tvLat).text = "lat: "+point.latitude.toString()
            itemView.findViewById<TextView>(R.id.tvLong).text = "long: " + point.longitude.toString()
            itemView.findViewById<TextView>(R.id.tvRadius).text ="raggio: "+ point.radius
            itemView.findViewById<ImageButton>(R.id.btnDeleteGeofencePoint).setOnClickListener {
                onDeleteClick(point)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeofenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_geofence, parent, false)
        return GeofenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: GeofenceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<GeofencePoint>() {
        override fun areItemsTheSame(oldItem: GeofencePoint, newItem: GeofencePoint) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: GeofencePoint, newItem: GeofencePoint) =
            oldItem == newItem
    }
}

