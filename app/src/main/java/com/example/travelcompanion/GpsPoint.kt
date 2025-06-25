package com.example.travelcompanion

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "gps_point",
    foreignKeys = [ForeignKey(
        entity = Trip::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )]
)data class GpsPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var tripId: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val timestamp: Long
)
