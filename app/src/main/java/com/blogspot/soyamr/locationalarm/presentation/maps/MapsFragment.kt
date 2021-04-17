package com.blogspot.soyamr.locationalarm.presentation.maps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.blogspot.soyamr.locationalarm.BuildConfig
import com.blogspot.soyamr.locationalarm.R
import com.blogspot.soyamr.locationalarm.databinding.FragmentMapsBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.snackbar.Snackbar
import java.util.*


class MapsFragment : Fragment(R.layout.fragment_maps) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }
    private val mapFragment by lazy {
        childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
    }


    private val onMarkerDragListener = object : GoogleMap.OnMarkerDragListener {
        override fun onMarkerDragStart(p0: Marker?) {}
        override fun onMarkerDrag(p0: Marker?) {}

        override fun onMarkerDragEnd(p0: Marker?) {
            val latLng = p0?.position
            val geoCoder = Geocoder(context, Locale.getDefault())
            try {
                val address = geoCoder.getFromLocation(latLng!!.latitude, latLng.longitude, 1)[0]
                println(address)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }
    private val binding: FragmentMapsBinding by viewBinding()
    private lateinit var googleMap: GoogleMap

    private val callback = OnMapReadyCallback { googleMap ->
        this.googleMap = googleMap
        getUserCurrentLocation()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestCurrentLocation()
        setUpListeners()
    }

    private fun setUpListeners() {
        binding.selectButton.setOnClickListener {
            findNavController().navigate(R.id.action_mapsFragment_to_trackingFragment)
        }
        mapFragment?.getMapAsync(callback)
    }


    private fun requestCurrentLocation() {
        // Check Fine permission
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
        } else {
            Snackbar.make(
                binding.root,
                "please guarantee the location service for our app",
                Snackbar.LENGTH_LONG
            )
                .setAction("settings") {
                    // Build intent that displays the App settings screen.
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        BuildConfig.APPLICATION_ID,
                        null
                    )
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
                .show()
        }
    }

    private fun getUserCurrentLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    val sydney = LatLng(location!!.latitude, location.longitude)
                    googleMap.addMarker(MarkerOptions().position(sydney).draggable(true))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
                    googleMap.uiSettings.isZoomControlsEnabled = true;
                    googleMap.setOnMarkerDragListener(onMarkerDragListener)
                }
        } catch (e: SecurityException) {
        }
    }
}