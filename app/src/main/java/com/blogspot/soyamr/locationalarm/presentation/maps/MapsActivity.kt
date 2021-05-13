package com.blogspot.soyamr.locationalarm.presentation.maps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.blogspot.soyamr.locationalarm.R
import com.blogspot.soyamr.locationalarm.presentation.maps.helpers.LocationListener
import com.blogspot.soyamr.locationalarm.presentation.maps.helpers.LocationManger
import com.blogspot.soyamr.locationalarm.presentation.tracking.TrackingActivity
import com.blogspot.soyamr.locationalarm.presentation.tracking.helper.globalDestination
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var latLng: LatLng? = null


    private lateinit var googleMap: GoogleMap

    private val locationManagerListener = object : LocationListener {
        override fun onLocationUpdated(userLocation: Location) {
            progress_circular.isVisible = false
            val userLocationLatLng = LatLng(userLocation.latitude, userLocation.longitude)
            googleMap.addMarker(MarkerOptions().position(userLocationLatLng))
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(userLocationLatLng))
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15F))
        }

        override fun hasLocationPermission() =
            ActivityCompat.checkSelfPermission(
                this@MapsActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        override fun askUserToOpenGPS(exception: ResolvableApiException) {
            val intentSenderRequest =
                IntentSenderRequest.Builder(exception.resolution).build()
            resolutionForResult.launch(intentSenderRequest)
        }

        override fun onSomethingWentWrong() {
            showMessage(R.string.can_not_find_location, false)

        }

        override fun onGettingLocation() {
            showMessage(R.string.getting_your_location, true)
        }
    }

    private val locationManger: LocationManger by lazy {
        LocationManger(
            this,
            locationManagerListener
        )
    }

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                locationManger.getUserLocation()
            } else {
                locationManagerListener.onSomethingWentWrong()
            }
        }

    private val resolutionForResult: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            if (activityResult.resultCode == RESULT_OK) {
                locationManger.startLocationUpdates()
            } else {
                locationManagerListener.onSomethingWentWrong()
            }
        }

    private fun showUserClickOnMap(latLng: LatLng) {
        progress_circular.isVisible = true
        GlobalScope.launch(Dispatchers.Main) {
            googleMap.run {
                clear()
                addMarker(
                    MarkerOptions().position(latLng).title(getAddress(latLng))
                ).showInfoWindow()
                uiSettings.isZoomControlsEnabled = true
                progress_circular.isVisible = false

            }

            this@MapsActivity.latLng = latLng
        }
    }

    override fun onResume() {
        super.onResume()
        if (this::googleMap.isInitialized) {
            googleMap.clear()
            locationManger.getUserLocation()
        }
    }

    private fun showMessage(msgId: Int, showProgressBar: Boolean) {
        progress_circular.isVisible = showProgressBar
        Toast.makeText(
            this, getString(msgId), Toast.LENGTH_SHORT
        ).show()
    }

    private fun getAddress(latLng: LatLng): String = with(Dispatchers.IO) {

        val geocoder = Geocoder(this@MapsActivity, Locale.getDefault())
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses.isNotEmpty()) {
                address = addresses[0]
                addressText = address.getAddressLine(0)
            } else {
                addressText = "its not available"
            }
        } catch (e: Exception) {
        }


        addressText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        setUpListeners()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        if (locationManagerListener.hasLocationPermission()) {
            locationManger.getUserLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        googleMap.setOnMapClickListener {
            showUserClickOnMap(it)
        }
    }


    private fun setUpListeners() {
        selectButton.setOnClickListener {
            if (latLng != null) {
                startActivity(Intent(this, TrackingActivity::class.java))
                globalDestination.longitude = latLng!!.longitude
                globalDestination.latitude = latLng!!.latitude

            }
        }
    }
}
