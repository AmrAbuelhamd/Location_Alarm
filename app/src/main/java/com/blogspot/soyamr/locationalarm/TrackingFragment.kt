package com.blogspot.soyamr.locationalarm

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.blogspot.soyamr.locationalarm.databinding.FragmentTrackingBinding


class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private val binding: FragmentTrackingBinding by viewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button.setOnClickListener {
            findNavController().navigate(R.id.action_trackingFragment_to_alarmFragment)
        }
    }
}