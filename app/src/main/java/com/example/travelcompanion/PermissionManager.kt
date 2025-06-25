package com.example.travelcompanion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionManager(private val fragment: Fragment) {

    companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val FOREGROUND_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else LOCATION_PERMISSIONS

        val NOTIFICATION_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else emptyArray()
    }

    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissionsIfNeeded(
        context: Context,
        launcher: ActivityResultLauncher<Array<String>>,
        permissions: Array<String>
    ) {
        if (!hasPermissions(context, permissions)) {
            launcher.launch(permissions)
        }
    }

    fun requestAllNeededPermissions(context: Context): Boolean {
        val allNeededPermissions = mutableListOf<String>()

        // Controlla LOCATION_PERMISSIONS
        LOCATION_PERMISSIONS.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                allNeededPermissions.add(permission)
            }
        }

        // Controlla FOREGROUND_PERMISSIONS (aggiunge anche ACCESS_BACKGROUND_LOCATION se Q o superiore)
        FOREGROUND_PERMISSIONS.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (!allNeededPermissions.contains(permission)) {
                    allNeededPermissions.add(permission)
                }
            }
        }

        // Controlla NOTIFICATION_PERMISSIONS (solo Android 13+)
        NOTIFICATION_PERMISSIONS.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (!allNeededPermissions.contains(permission)) {
                    allNeededPermissions.add(permission)
                }
            }
        }

        return allNeededPermissions.isNotEmpty()
    }

}
