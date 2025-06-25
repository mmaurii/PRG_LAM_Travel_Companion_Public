package com.example.travelcompanion

import AppDatabase
import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GpsTrackingService : Service() {
    private lateinit var locationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var trackingJob: Job? = null
    private var tripId: Long = 0L
    private lateinit var wakeLock: PowerManager.WakeLock

    object LocationUpdates {
        private val _locationFlow = MutableStateFlow<Location?>(null)
        val locationFlow: StateFlow<Location?> = _locationFlow

        fun updateLocation(location: Location) {
            _locationFlow.value = location
        }
    }


    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GpsTrackingService::lock")
        wakeLock.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        tripId = intent?.getLongExtra("tripId", 0L) ?: 0L
        startTracking()

        return START_STICKY
    }

    private fun startTracking() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

                for (location in result.locations) {
                    LocationUpdates.updateLocation(location)

                    val point = GpsPoint(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude,
                        tripId = tripId,
                        timestamp = System.currentTimeMillis()
                    )
                    savePointToDatabase(point)
                    sendBroadcast(Intent("NEW_GPS_POINT").apply {
                        putExtra("latitude", point.latitude)
                        putExtra("longitude", point.longitude)
                    })
                }
            }
        }

        trackingJob = CoroutineScope(Dispatchers.IO).launch {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000
            ).build()

            if (!hasRequiredLocationServicePermissions(this@GpsTrackingService)) {
                Log.w("GpsTrackingService", "Permessi non concessi. Interrompo il tracciamento.")
                stopSelf()
                return@launch
            }

            val callback = locationCallback
            if (callback == null) {
                Log.e("GpsTrackingService", "locationCallback è null. Impossibile avviare il tracking.")
                stopSelf()
                return@launch
            }

            try {
                locationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
                Log.d("GpsTrackingService", "Inizio tracciamento posizione.")
            } catch (e: SecurityException) {
                Log.e("GpsTrackingService", "SecurityException: permessi mancanti durante il tracking", e)
                stopSelf()
            }
        }

    }

    private fun savePointToDatabase(point: GpsPoint) {
        val db = AppDatabase.getDatabase(applicationContext);

        CoroutineScope(Dispatchers.IO).launch {
            db.tripDao().insertGpsPoint(point)
        }
    }

    private fun createNotification(): Notification {
        val channelId = "gps_tracking_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracciamento attivo")
            .setContentText("Stiamo registrando la tua posizione")
            .setSmallIcon(R.drawable.ic_position_foreground)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        locationCallback?.let {
            locationClient.removeLocationUpdates(it)
        }

        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    fun hasRequiredLocationServicePermissions(context: Context): Boolean {
        val fineLocation = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return fineLocation && backgroundLocation
    }
}
