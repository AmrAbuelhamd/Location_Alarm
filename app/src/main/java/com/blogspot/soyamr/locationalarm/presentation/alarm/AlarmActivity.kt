package com.blogspot.soyamr.locationalarm.presentation.alarm

import android.media.RingtoneManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blogspot.soyamr.locationalarm.R
import kotlinx.android.synthetic.main.activity_alarm.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmActivity : AppCompatActivity(R.layout.activity_alarm) {
    var job: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notification =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val r = RingtoneManager.getRingtone(applicationContext, notification)

        finishButton.setOnClickListener {
            job?.cancel()
            r?.stop()
            finish()
        }
        job = GlobalScope.launch {
            while(true) {
                r?.play()
                delay(5000)
            }
        }
        job?.start()
    }
}