package com.example.travelcompanion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.Date

class TravelPointAdapter(
    private var points: MutableList<TravelPoint>,
    private val compactMode: Boolean = false,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<TravelPointAdapter.PointViewHolder>() {

    inner class PointViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pointTitle: TextView = itemView.findViewById(R.id.tvPointTitle)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeletePoint)
        private lateinit var btnDrag: ImageButton
        lateinit var pointDate: TextView
        lateinit var pointNotes: TextView
        lateinit var photoView: ImageView


        init {
            btnDelete.setOnClickListener {
                val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onDelete(pos)
                    }
            }

            if(!compactMode){
                pointDate = itemView.findViewById(R.id.tvPointDate)
                pointNotes = itemView.findViewById(R.id.tvPointNotes)
                photoView = itemView.findViewById(R.id.imgPhoto)

            }else{
                btnDrag = itemView.findViewById(R.id.btnMove)

                btnDrag.setOnLongClickListener {
                    onStartDrag(this)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointViewHolder {
        val view:View
        if(compactMode) {
             view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_point, parent, false)
        }else{
                view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_travel_point, parent, false)
        }

        return PointViewHolder(view)
    }

    override fun onBindViewHolder(holder: PointViewHolder, position: Int) {
        val point = points[position]
        holder.pointTitle.text = point.title
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        if (!compactMode) {
            holder.pointDate.text = sdf.format(point.timeStamp)
            holder.pointNotes.text = point.notes
            Glide.with(holder.itemView.context)
                .load(point.foto)
                .into(holder.photoView)
        }

    }

    override fun getItemCount(): Int = points.size

    fun addPoint(point: TravelPoint) {
        points.add(point)
        notifyItemInserted(points.size - 1)
    }

    fun removeAt(position: Int) {
        if (position in points.indices) {
            points.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun clear() {
        points.clear()
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int) {
        if (from in points.indices && to in points.indices) {
            val item = points.removeAt(from)
            points.add(to, item)
            notifyItemMoved(from, to)
        }
    }

    fun setData(newItems: List<TravelPoint>) {
        points = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun getData(): List<TravelPoint> = points
}
