package com.example.travelcompanion

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class TripViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TripViewModel(application) as T
    }
}

class TripViewModel(application: Application) : AndroidViewModel(application) {
    private val tripDao = AppDatabase.getDatabase(application).tripDao()

    // Cambia TUTTO da Trip a TripWithPoints
    private val allTripsSource: LiveData<List<TripWithPoints>> = tripDao.getAllTripsWithPoints()

    private val _displayedTrips = MediatorLiveData<List<TripWithPoints>>()
    val displayedTrips: LiveData<List<TripWithPoints>> = _displayedTrips
    val showBackArrow = MutableLiveData<Boolean>()
    private var currentSource: LiveData<List<TripWithPoints>> = allTripsSource

    init {
        _displayedTrips.addSource(allTripsSource) { trips ->
            _displayedTrips.value = trips
        }
        showBackArrow.postValue(false)
    }

    fun showAllTrips() {
        switchSource(allTripsSource)
    }

    fun filterTrips(startDate: String, endDate: String) {
        val filtered: LiveData<List<TripWithPoints>> = tripDao.getTripsWithPointsBetweenDates(startDate, endDate)
        switchSource(filtered)
    }

    private fun switchSource(newSource: LiveData<List<TripWithPoints>>) {
        _displayedTrips.removeSource(currentSource)
        _displayedTrips.addSource(newSource) { trips ->
            _displayedTrips.value = trips
        }
        currentSource = newSource
    }

    suspend fun insertTrip(trip: Trip): Long {
        return tripDao.insertTrip(trip)
    }

    fun insertTravelPoint(point: TravelPoint) = viewModelScope.launch {
        tripDao.insertTravelPoint(point)
    }

    fun deleteTrip(trip: Trip) = viewModelScope.launch {
        tripDao.deleteTrip(trip)
    }

    fun getPointsForTrip(tripId: Long): LiveData<List<TravelPoint>> {
        return tripDao.getTravelPointsById(tripId)
    }

    fun getTripWithPoints(id: Long): LiveData<TripWithPoints> {
        return tripDao.getTripWithPoints(id)
    }

    fun  insertGpsPoint(gpsPoints: List<GpsPoint>) = viewModelScope.launch {
        tripDao.insertGpsPoints(gpsPoints)
    }

    fun deleteTravelPoint(itemToRemove: TravelPoint) = viewModelScope.launch {
        tripDao.deleteTravelPointById(itemToRemove.id)
    }

    fun deleteCorruptedTrip() = viewModelScope.launch {
        tripDao.deleteTripsWithoutTitle()
    }
}
