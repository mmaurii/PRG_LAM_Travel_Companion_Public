package com.example.travelcompanion

import TravelPointDao
import androidx.lifecycle.LiveData

class TravelRepository(private val dao: TravelPointDao) {
    val allTravelPoints: LiveData<List<TravelPoint>> = dao.getAll()

    suspend fun insert(tp: TravelPoint) {
        dao.insert(tp)
    }
}
