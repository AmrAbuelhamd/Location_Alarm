package com.blogspot.soyamr.locationalarm.presentation.tracking

import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.blogspot.soyamr.locationalarm.R
import com.blogspot.soyamr.locationalarm.databinding.FragmentTrackingBinding
import com.blogspot.soyamr.locationalarm.presentation.utils.MyLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private val binding: FragmentTrackingBinding by viewBinding()
    private val args: TrackingFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button.setOnClickListener {
//            findNavController().navigate(R.id.action_trackingFragment_to_alarmFragment)
            findNavController().popBackStack()
        }
        val myLocation = MyLocation()
        GlobalScope.launch(Dispatchers.Main) {
            while(true) {
                myLocation.getLocation(requireContext(), locationResult)
                delay(20000)
            }
        }
    }

    val myLocation = MyLocation()
    private fun yourRunnable() {
        myLocation.getLocation(requireContext(), locationResult)
    }

    val locationResult = object : MyLocation.LocationResult() {

        override fun gotLocation(location: Location?) {

            val lat = location!!.latitude
            val lon = location.longitude

            println("$lat --SLocRes-- $lon")

            val location1 = Location("");
            location1.latitude = lat;
            location1.longitude = lon;

            val location2 = Location("");
            location2.latitude = args.lat.toDouble();
            location2.longitude = args.lng.toDouble();

            val distanceInMeters = location1.distanceTo(location2);
            val speedIs10MetersPerMinute = 100
            val estimatedDriveTimeInMinutes = distanceInMeters / speedIs10MetersPerMinute
            val hours: Int =
                estimatedDriveTimeInMinutes.toInt() / 60 //since both are ints, you get an int
            val minutes: Int = estimatedDriveTimeInMinutes.toInt() % 60
            binding.textView2.text = getString(R.string._5_hours_30_minutes, hours, minutes)
        }

    }

}