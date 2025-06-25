package com.example.travelcompanion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()){
                return
            }

            if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                val geofenceIds = geofencingEvent.triggeringGeofences?.map { it.requestId }
                if (geofenceIds != null) {
                    sendNotification(context, geofenceIds)
                }
            }
        }
    }

    private fun sendNotification(context: Context, ids: List<String>) {
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("geo_chan","Geofence Alerts", NotificationManager.IMPORTANCE_HIGH)
            notifManager.createNotificationChannel(channel)
        }

        val content = "Sei entrato in: ${ids.joinToString()}"
        val builder = NotificationCompat.Builder(context, "geo_chan")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Geofence raggiunto")
            .setContentText(content)
            .setAutoCancel(true)

        notifManager.notify(ids.hashCode(), builder.build())
    }
}
