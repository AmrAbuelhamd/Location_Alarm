package com.blogspot.soyamr.locationalarm.presentation.maps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.blogspot.soyamr.locationalarm.BuildConfig
import com.blogspot.soyamr.locationalarm.R
import com.blogspot.soyamr.locationalarm.databinding.FragmentMapsBinding
import com.blogspot.soyamr.locationalarm.presentation.utils.MyLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import java.util.*


class MapsFragment : Fragment(R.layout.fragment_maps) {

    private val mapFragment by lazy {
        childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
    }

    var latLng: LatLng? = null

    private val onMarkerDragListener = object : GoogleMap.OnMarkerDragListener {
        override fun onMarkerDragStart(p0: Marker?) {}
        override fun onMarkerDrag(p0: Marker?) {}

        override fun onMarkerDragEnd(p0: Marker?) {
            val latLng = p0?.position
            if (latLng != null)
                this@MapsFragment.latLng = latLng
        }

    }
    private val binding: FragmentMapsBinding by viewBinding()
    private lateinit var googleMap: GoogleMap

    private val callback = OnMapReadyCallback { googleMap ->
        this.googleMap = googleMap
        val myLocation = MyLocation()
        myLocation.getLocation(requireContext(), locationResult)
    }

    private val locationResult = object : MyLocation.LocationResult() {
        override fun gotLocation(location: Location?) {
            val sydney = LatLng(location!!.latitude, location.longitude)
            googleMap.addMarker(MarkerOptions().position(sydney).draggable(true))
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
            googleMap.uiSettings.isZoomControlsEnabled = true;
            googleMap.setOnMarkerDragListener(onMarkerDragListener)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpListeners()
    }

    private fun setUpListeners() {
        binding.selectButton.setOnClickListener {
            if (latLng != null) {
                findNavController().navigate(
                    MapsFragmentDirections.actionMapsFragmentToTrackingFragment
                        (latLng!!.longitude.toString(), latLng!!.latitude.toString())
                )
            }
        }
        mapFragment?.getMapAsync(callback)
    }


}