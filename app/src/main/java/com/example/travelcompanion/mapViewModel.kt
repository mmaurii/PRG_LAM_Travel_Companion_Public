package com.example.travelcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.osmdroid.util.GeoPoint

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val _trackedPoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val trackedPoints: StateFlow<List<GeoPoint>> = _trackedPoints

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused
    var newTripId: Long = 0L

    private var startTime = 0L
    private var elapsedTime = 0L

    private val _formattedTime = MutableStateFlow("00:00.0")
    val formattedTime: StateFlow<String> = _formattedTime
    private var timerJob: Job? = null

    init {
        // Colleziona la posizione dal service
        viewModelScope.launch {
            GpsTrackingService.LocationUpdates.locationFlow.collect { location ->
                if (_isTracking.value && !_isPaused.value && location != null) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    _trackedPoints.value = _trackedPoints.value + point
                }
            }
        }

        viewModelScope.launch {
            combine(isPaused, isTracking) { valuePaused, valueTracking ->
                Pair(valuePaused, valueTracking)
            }.collectLatest { (valuePaused, valueTracking) ->
                if (valueTracking){
                    if(valuePaused) {
                        stopStopwatch()
                    }else{
                        startStopwatch()
                    }
                }else{
                    resetStopwatch()
                }
            }
        }
    }

    fun startTracking() {
        _trackedPoints.value = emptyList()
        _isTracking.value = true
        _isPaused.value = false
    }

    fun pauseTracking() {
        _isPaused.value = true
    }

    fun resumeTracking() {
        _isPaused.value = false
    }

    fun stopTracking() {
        _isTracking.value = false
        _isPaused.value = false

        clearTrack()
    }

    private fun clearTrack() {
        _trackedPoints.value = emptyList()
    }

    private fun startStopwatch() {
        startTime = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val time = now - startTime + elapsedTime
                _formattedTime.value = formatTime(time)
                delay(100)
            }
        }
    }

    private fun stopStopwatch() {
        elapsedTime += System.currentTimeMillis() - startTime
        timerJob?.cancel()
    }

    private fun resetStopwatch() {
        timerJob?.cancel()
        startTime = 0L
        elapsedTime = 0L
        _formattedTime.value = "00:00.0"
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 1000) / 60
        val milliseconds = (ms % 1000) / 100
        return String.format("%02d:%02d.%01d", minutes, seconds, milliseconds)
    }
}
