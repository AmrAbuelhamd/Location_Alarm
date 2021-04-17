package com.blogspot.soyamr.locationalarm.presentation.maps

import android.location.Geocoder
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.blogspot.soyamr.locationalarm.R
import com.blogspot.soyamr.locationalarm.databinding.FragmentMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*


class MapsFragment : Fragment(R.layout.fragment_maps) {


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

    private fun getUserCurrentLocation() {
        // check if GPS enabled
        // check if GPS enabled
        val gpsTracker = GPSTracker(requireContext())

        if (gpsTracker.getIsGPSTrackingEnabled()) {
            val sydney = LatLng(gpsTracker.latitude, gpsTracker.longitude)
            googleMap.addMarker(MarkerOptions().position(sydney).draggable(true))
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
            googleMap.uiSettings.isZoomControlsEnabled = true;
            googleMap.setOnMarkerDragListener(onMarkerDragListener)
        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gpsTracker.showSettingsAlert()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        binding.selectButton.setOnClickListener {
            findNavController().navigate(R.id.action_mapsFragment_to_trackingFragment)
        }
    }
}