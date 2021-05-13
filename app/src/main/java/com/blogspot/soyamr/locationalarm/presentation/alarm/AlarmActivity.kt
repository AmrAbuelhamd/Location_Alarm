package com.blogspot.soyamr.locationalarm.presentation.alarm

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.blogspot.soyamr.locationalarm.R
import kotlinx.android.synthetic.main.activity_alarm.*

class AlarmActivity : AppCompatActivity(R.layout.activity_alarm) {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        finishButton.setOnClickListener { finish() }
    }
}