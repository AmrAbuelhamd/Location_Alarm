package com.blogspot.soyamr.locationalarm.presentation.maps.helpers

import android.content.Context
import android.content.IntentSender
import android.os.Looper
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class LocationManger(
    private val context: Context,
    private val locationListener: LocationListener
) {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationRequest: LocationRequest by lazy {
        LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private val locationCallback: LocationCallback by lazy {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                var showErrorMessage = true
                for (location in locationResult.locations) {
                    if (location != null) {
                        locationListener.onLocationUpdated(location)
                        fusedLocationClient.removeLocationUpdates(this)
                        showErrorMessage = false
                        break
                    }
                }
                if (showErrorMessage)
                    locationListener.onSomethingWentWrong()
            }
        }
    }

    fun getUserLocation() {
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener {
                if (it != null) {
                    locationListener.onLocationUpdated(it)
                } else {
                    requestLocationUpdates()
                }
            }
        } catch (e: SecurityException) {
            locationListener.onSomethingWentWrong()
        }
    }

    private fun requestLocationUpdates() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { _ ->
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    locationListener.askUserToOpenGPS(exception)
                } catch (sendEx: IntentSender.SendIntentException) {
                    locationListener.onSomethingWentWrong()
                }
            }
        }
    }

    fun startLocationUpdates() {
        if (!locationListener.hasLocationPermission()) {
            locationListener.onSomethingWentWrong()
            return
        }
        locationListener.onGettingLocation()
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            locationListener.onSomethingWentWrong()
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

}