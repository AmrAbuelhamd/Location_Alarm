package com.blogspot.soyamr.locationalarm.presentation.alarm

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.blogspot.soyamr.locationalarm.R
import com.blogspot.soyamr.locationalarm.databinding.FragmentAlarmBinding

class AlarmFragment : Fragment(R.layout.fragment_alarm) {
    private val binding: FragmentAlarmBinding by viewBinding()

    private val viewModel: AlarmViewModel by viewModels()
}