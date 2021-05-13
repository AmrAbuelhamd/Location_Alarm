package com.blogspot.soyamr.locationalarm.presentation.tracking

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.blogspot.soyamr.locationalarm.R
import com.blogspot.soyamr.locationalarm.presentation.MainActivity
import com.blogspot.soyamr.locationalarm.presentation.alarm.AlarmActivity
import com.blogspot.soyamr.locationalarm.presentation.tracking.helper.ForegroundOnlyLocationService
import com.blogspot.soyamr.locationalarm.presentation.tracking.helper.globalArrived
import com.blogspot.soyamr.locationalarm.presentation.tracking.helper.globalDestination
import kotlinx.android.synthetic.main.activity_tracking.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "Heer"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class TrackingActivity : AppCompatActivity(),
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

        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        bindService(
            serviceIntent,
            foregroundOnlyServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            foregroundOnlyBroadcastReceiver,
            IntentFilter(
                ForegroundOnlyLocationService.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST
            )
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
            foregroundOnlyBroadcastReceiver
        )
        super.onPause()
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            unbindService(foregroundOnlyServiceConnection)
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
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }


    // Review Permissions: Handles permission result.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

        val distanceInMeters = location.distanceTo(globalDestination)
        Log.e(TAG, "distance in meters: $distanceInMeters")
        val estimatedDriveTimeInMinutes = distanceInMeters / location.speed
        Log.e(TAG, "estimated Drive Time In Minutes : $estimatedDriveTimeInMinutes")
        Log.e(TAG, "****************************************************")
        val hours: Int =
            estimatedDriveTimeInMinutes.toInt() / 60
        val minutes: Int = estimatedDriveTimeInMinutes.toInt() % 60
        timeLeftTextView.text = getString(R.string._5_hours_30_minutes, hours, minutes)
        metersLeftTextView.text = getString(R.string._555_meters, distanceInMeters.toString())

        if (distanceInMeters <= 30F) {
            foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            startAlarmActivity()
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
                            if (s > 10) s
                            else null
                        } ?: 10F
                    }
                previousLocation = location
                logResultsToScreen(location)
            }
        }
    }

    private fun startMainActivity() {
        foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    private fun startAlarmActivity() {
        foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
        startActivity(Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)

        cancel_button.setOnClickListener {
            startMainActivity()
        }
        if (globalArrived) {
            startAlarmActivity()
            return
        }

        foregroundOnlyBroadcastReceiver = ForegroundOnlyBroadcastReceiver()

        sharedPreferences =
            getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
            )
        GlobalScope.launch {
            delay(1000)

            foregroundOnlyLocationService?.subscribeToLocationUpdates()
                ?: Log.d(TAG, "Service Not Bound")

        }

    }

    companion object {
        const val latKey = "latKey"
        const val lngKey = "lngKey"
    }

}