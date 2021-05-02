package com.blogspot.soyamr.locationalarm.presentation.maps

import android.app.Activity.RESULT_OK
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.blogspot.soyamr.locationalarm.R
import com.blogspot.soyamr.locationalarm.databinding.FragmentMapsBinding
import com.blogspot.soyamr.locationalarm.presentation.utils.MyLocationManger
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class MapsFragment : Fragment(R.layout.fragment_maps) {

    private val my_tag = "MAP_FRAGMENT"
    private val binding: FragmentMapsBinding by viewBinding()

    var latLng: LatLng? = null

    private lateinit var googleMap: GoogleMap


    private val locationManger: MyLocationManger by lazy {
        MyLocationManger(requireContext(), showUserCurrentLocation)
    }

    private val mapFragment by lazy {
        childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
    }

    private val mapReadyCallback = OnMapReadyCallback { googleMap ->
        this.googleMap = googleMap
        try {
            locationManger.getLastLocation(
                resolutionForResult,
                requestPermissionLauncher
            )
        } catch (e: SecurityException) {
            showMessage("something went wrong, couldn't get your location")
        }

    }

    private val resolutionForResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            if (activityResult.resultCode == RESULT_OK)
                locationManger.startLocationUpdates(requestPermissionLauncher)
            else {
                showMessage("we can't determine your location")
            }
        }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setUpMap()
        } else {
            showMessage(
                "the application can't show your " +
                        "current location on the map, because you denied the location permission"
            )
        }
    }

    private val showUserCurrentLocation: (location: Location) -> Unit = {
        val userLocation = LatLng(it.latitude, it.longitude)
        googleMap.addMarker(MarkerOptions().position(userLocation))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15.0F))
        googleMap.uiSettings.isZoomControlsEnabled = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpListeners()

        if (locationManger.locationPermissionGranted()) {
            setUpMap()
        } else {
            locationManger.requestPermission(requestPermissionLauncher)
        }

    }


    private fun showMessage(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
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
    }

    private fun setUpMap() {
        mapFragment?.getMapAsync(mapReadyCallback)
    }

}