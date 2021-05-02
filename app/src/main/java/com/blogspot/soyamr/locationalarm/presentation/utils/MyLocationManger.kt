package com.blogspot.soyamr.locationalarm.presentation.utils

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class MyLocationManger(
    private val requireContext: Context,
    val showUserCurrentLocation: (location: Location) -> Unit
) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationRequest: LocationRequest by lazy {
        LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }


    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            for (location in locationResult.locations) {
                if (location != null) {
                    showUserCurrentLocation(location)
                    stopLocationUpdates(this)
                    break
                }
            }
        }
    }

    fun locationPermissionGranted(): Boolean {
        return when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                requireContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(requireContext)
                true
            }
            else -> {
                false
            }
        }
    }

    fun requestPermission(requestPermissionLauncher: ActivityResultLauncher<String>) {
        requestPermissionLauncher.launch(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun checkLocationSettingsAndStartLocationUpdates(
        resolutionForResult: ActivityResultLauncher<IntentSenderRequest>,
        requestPermissionLauncher: ActivityResultLauncher<String>
    ) {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(requireContext)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { _ ->
            startLocationUpdates(requestPermissionLauncher)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    resolutionForResult.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            }
        }
    }

    fun startLocationUpdates(
        requestPermissionLauncher: ActivityResultLauncher<String>
    ) {
        if (ActivityCompat.checkSelfPermission(
                requireContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates(locationCallback: LocationCallback) {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    fun getLastLocation(
        resolutionForResult: ActivityResultLauncher<IntentSenderRequest>,
        requestPermissionLauncher: ActivityResultLauncher<String>,
    ) {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location == null) {
                        checkLocationSettingsAndStartLocationUpdates(
                            resolutionForResult,
                            requestPermissionLauncher
                        )
                    } else {
                        showUserCurrentLocation(location)
                    }
                }
        } catch (e: SecurityException) {
            Toast.makeText(
                requireContext,
                "something went wrong, couldn't get your location",
                Toast.LENGTH_LONG
            ).show()
        }
    }

}