package com.blogspot.soyamr.locationalarm.presentation.tracking

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.blogspot.soyamr.locationalarm.R
import com.blogspot.soyamr.locationalarm.databinding.FragmentTrackingBinding
import com.blogspot.soyamr.locationalarm.presentation.tracking.helper.ForegroundOnlyLocationService
import com.blogspot.soyamr.locationalarm.presentation.tracking.helper.location2
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "Heer"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class TrackingFragment : Fragment(R.layout.fragment_tracking),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private var foregroundOnlyLocationServiceBound = false

    var previousLocation: Location? = null

    // Provides location updates for while-in-use feature.
    private var foregroundOnlyLocationService: ForegroundOnlyLocationService? = null

    // Listens for location broadcasts from ForegroundOnlyLocationService.
    private lateinit var foregroundOnlyBroadcastReceiver: ForegroundOnlyBroadcastReceiver

    private lateinit var sharedPreferences: SharedPreferences


    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }


    override fun onStart() {
        super.onStart()

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        val serviceIntent = Intent(requireContext(), ForegroundOnlyLocationService::class.java)
        requireActivity().bindService(
            serviceIntent,
            foregroundOnlyServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST
            )
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(
            foregroundOnlyBroadcastReceiver
        )
        super.onPause()
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            requireActivity().unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onStop()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    }

    //Review Permissions: Method checks if permissions approved.
    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }


    // Review Permissions: Handles permission result.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    // Permission was granted.
                    foregroundOnlyLocationService?.subscribeToLocationUpdates()
                else -> {
                }
            }
        }
    }


    private fun logResultsToScreen(location: Location) {

        val location2 = Location("")
        location2.latitude = args.lat.toDouble()
        location2.longitude = args.lng.toDouble()

        val distanceInMeters = location.distanceTo(location2)
        Log.e(TAG, "distance in meters: $distanceInMeters")
        val estimatedDriveTimeInMinutes = distanceInMeters / location.speed
        Log.e(TAG, "estimated Drive Time In Minutes : $estimatedDriveTimeInMinutes")
        Log.e(TAG, "****************************************************")
        val hours: Int =
            estimatedDriveTimeInMinutes.toInt() / 60
        val minutes: Int = estimatedDriveTimeInMinutes.toInt() % 60
        binding.textView2.text = getString(R.string._5_hours_30_minutes, hours, minutes)
        binding.textView3.text = "$distanceInMeters meters"

        if (distanceInMeters <= 10F) {
            foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            findNavController().navigate(R.id.action_trackingFragment_to_alarmFragment)
        }

    }

    /**
     * Receiver for location broadcasts from [ForegroundOnlyLocationService].
     */
    private inner class ForegroundOnlyBroadcastReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(
                ForegroundOnlyLocationService.EXTRA_LOCATION
            )

            if (location != null) {
                Log.e(TAG, "location.speed: ${location.speed}")
                location.speed =
                    if (location.hasSpeed() && location.speed != 0F) {
                        location.speed * 60
                    } else {
                        previousLocation?.let { lastLocation ->
                            // Convert milliseconds to seconds
                            val elapsedTimeInSeconds = (location.time - lastLocation.time)
                            val distanceInMeters = lastLocation.distanceTo(location)
                            // Speed in meter/minute
                            val s = distanceInMeters / elapsedTimeInSeconds
                            Log.e(TAG, "calculated speed: $s")
                            if (s > 10) s
                            else null
                        } ?: 10F
                    }
                previousLocation = location
                Log.e(TAG, "final speed: " + location.speed)
                logResultsToScreen(location)
            }
        }
    }

    private val binding: FragmentTrackingBinding by viewBinding()
    private val args: TrackingFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancelButton.setOnClickListener {
            foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            findNavController().popBackStack()
        }

        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

        location2.latitude = args.lat.toDouble()
        location2.longitude = args.lng.toDouble()

        sharedPreferences =
            requireActivity().getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )
        GlobalScope.launch {
            delay(1000)

            foregroundOnlyLocationService?.subscribeToLocationUpdates()
                ?: Log.d(TAG, "Service Not Bound")

        }

    }

}