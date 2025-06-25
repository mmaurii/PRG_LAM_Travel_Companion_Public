package com.example.travelcompanion

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GeofenceViewModelFactory(
    private val repository: GeofenceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GeofenceViewModel::class.java)) {
            return GeofenceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


class GeofenceViewModel(private val geofenceRepository: GeofenceRepository) : ViewModel() {

    val geofencePoints: LiveData<List<GeofencePoint>> = geofenceRepository.allPoints.asLiveData()

    fun addGeofencePoint(point: GeofencePoint) {
        viewModelScope.launch {
            geofenceRepository.insert(point)
        }
    }

    fun deleteGeofencePoint(point: GeofencePoint) {
        viewModelScope.launch {
            geofenceRepository.delete(point)
        }
    }
}


