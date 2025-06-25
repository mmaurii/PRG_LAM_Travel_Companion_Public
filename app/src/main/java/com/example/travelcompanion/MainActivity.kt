package com.example.travelcompanion

import android.Manifest
import android.content.Context
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.BassBoost
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import org.osmdroid.views.MapView
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var map: MapView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var navController: NavController
    private lateinit var tripViewModel: TripViewModel
    private val mapViewModel: MapViewModel by viewModels()
    private val geofenceViewModel: GeofenceFragment by viewModels()
    private lateinit var permissionManager: PermissionManager
    private var workerName2 = "TripReminderWork2"
    private var workerName = "TripReminderWork"

    // Launcher per richiedere il permesso
    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permesso notifiche concesso", Toast.LENGTH_SHORT).show()
                // Ora puoi inviare notifiche

                //avvio il worker per la notifica in background
                val workRequest = PeriodicWorkRequestBuilder<TripReminderWorker>(
                    1, TimeUnit.DAYS // Controlla ogni giorno se sono passati 30
                ).build()

                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    workerName,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            } else {
                Toast.makeText(this, "Permesso notifiche negato", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestNotificationPermission()

        // View binding
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)

        // Setup toolbar
        setSupportActionBar(toolbar)

        // Setup navController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup ViewModel
        tripViewModel = ViewModelProvider(this)[TripViewModel::class.java]

        // Static AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.MapFragment,
                R.id.StatisticsFragment,
                R.id.CreateTripFragment,
                R.id.nav_view_trips_graph,
                R.id.GeofenceFragment
            ),
            drawerLayout
        )

        // Initial setup
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    // Handles up button
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        WorkManager.getInstance(this).cancelUniqueWork(workerName)
        cancelScheduledNotification()
    }

    private fun cancelScheduledNotification() {
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }


    override fun onStop() {
        super.onStop()
        scheduleNotification()

        val workRequest = OneTimeWorkRequestBuilder<TripReminderWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                workerName,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

    }

    private fun scheduleNotification() {
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = System.currentTimeMillis() + 10_000 // 10 secondi

        if(hasExactAlarmPermission()) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }else{
            requestExactAlarmPermission()
        }
    }

        private fun checkAndRequestNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permission = Manifest.permission.POST_NOTIFICATIONS
                when {
                    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                        // Permesso già concesso
                    }
                    shouldShowRequestPermissionRationale(permission) -> {
                        // Mostra una spiegazione se vuoi
                        requestNotificationPermissionLauncher.launch(permission)
                    }
                    else -> {
                        // Richiedi direttamente
                        requestNotificationPermissionLauncher.launch(permission)
                    }
                }
            }
        }

    fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            this.startActivity(intent)
        }
    }

}