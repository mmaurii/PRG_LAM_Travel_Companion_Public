package com.example.travelcompanion

import TripDao
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TripReminderWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val tripDao: TripDao = AppDatabase.getDatabase(context).tripDao(),
    private val notificationHelper: NotificationHelper = NotificationHelper(context)

) : CoroutineWorker(context, workerParams) {

//    override suspend fun doWork(): Result {
//        val lastTrip = tripDao.getLastTripDate()
//
//        if (lastTrip != null) {
//            val now = System.currentTimeMillis()
////            val oneMonthMillis = 7L * 24 * 60 * 60 * 1000 // 7 giorni
//            val oneMonthMillis = 60 * 1000 // 7 giorni
//
//            if (now - lastTrip > oneMonthMillis) {
//                notificationHelper.sendReminder()
//            }
//        }
//        return Result.success()
//    }

    override suspend fun doWork(): Result {
        notificationHelper.sendReminder()
        return Result.success()
    }

}

