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
import com.blogspot.soyamr.locationalarm.presentation.tracking.helper.SharedPreferenceUtil
import com.blogspot.soyamr.locationalarm.presentation.tracking.helper.toText

private const val TAG = "Heer"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class TrackingFragment : Fragment(R.layout.fragment_tracking),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private var foregroundOnlyLocationServiceBound = false

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

        updateButtonState(
            sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
        )
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
        // Updates button states if new while in use location is added to SharedPreferences.
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            updateButtonState(
                sharedPreferences.getBoolean(
                    SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false
                )
            )
        }
    }

    // TODO: Step 1.0, Review Permissions: Method checks if permissions approved.
    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }


    // TODO: Step 1.0, Review Permissions: Handles permission result.
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

    private fun updateButtonState(trackingLocation: Boolean) {
        if (trackingLocation) {
            binding.foregroundOnlyLocationButton.text =
                getString(R.string.stop_location_updates_button_text)
        } else {
            binding.foregroundOnlyLocationButton.text =
                getString(R.string.start_location_updates_button_text)
        }
    }

    private fun logResultsToScreen(output: String) {
        binding.outputTextView.text = output
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
                logResultsToScreen("Foreground location: ${location.toText()}")
            }
        }
    }

    private val binding: FragmentTrackingBinding by viewBinding()
    private val args: TrackingFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.button.setOnClickListener {
            findNavController().popBackStack()
        }
        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

        sharedPreferences =
            requireActivity().getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )

//        foregroundOnlyLocationService?.subscribeToLocationUpdates()

        binding.foregroundOnlyLocationButton.setOnClickListener {
            val enabled = sharedPreferences.getBoolean(
                SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false
            )

            if (enabled) {
                foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            } else {
                // TODO: Step 1.0, Review Permissions: Checks and requests if needed.
                if (foregroundPermissionApproved()) {
                    foregroundOnlyLocationService?.subscribeToLocationUpdates()
                        ?: Log.d(TAG, "Service Not Bound")
                } else {
                    Log.d(TAG, " requestForegroundPermissions()")
                }
            }
        }
    }

}