package com.example.travelcompanion

import androidx.room.Embedded
import androidx.room.Relation

data class TripWithPoints(
    @Embedded val trip: Trip,

    @Relation(
        parentColumn = "id",
        entityColumn = "tripId"
    )
    var travelPoints: List<TravelPoint>,

    @Relation(
        parentColumn = "id",
        entityColumn = "tripId"
    )
    var gpsPoints: List<GpsPoint>
)

