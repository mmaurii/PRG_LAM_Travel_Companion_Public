package com.example.travelcompanion

import GeofenceLocationDao
import kotlinx.coroutines.flow.Flow

class GeofenceRepository(private val dao: GeofenceLocationDao) {
    val allPoints: Flow<List<GeofencePoint>> = dao.getAll()

    suspend fun insert(point: GeofencePoint) = dao.insert(point)
    suspend fun delete(point: GeofencePoint) = dao.delete(point)
}