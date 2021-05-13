package com.blogspot.soyamr.locationalarm.presentation

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blogspot.soyamr.locationalarm.R
import com.blogspot.soyamr.locationalarm.presentation.maps.MapsActivity
import com.blogspot.soyamr.locationalarm.presentation.tracking.helper.globalArrived

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        globalArrived = false
        startActivity(Intent(this,MapsActivity::class.java))
    }
}