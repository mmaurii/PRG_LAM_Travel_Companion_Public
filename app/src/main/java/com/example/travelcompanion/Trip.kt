package com.example.travelcompanion
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val title: String,
    val notes: String,
    val date: String,
    val type: String
)

