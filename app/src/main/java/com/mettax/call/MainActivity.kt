package com.mettax.call

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hk1089.mettax.Channel

class MainActivity : AppCompatActivity() {
    private lateinit var channel: Channel
    private var callStatus = false
    private var micStatus = false
    private var speakerStatus = false
    private lateinit var callDurationHandler: Handler
    private lateinit var callDurationRunnable: Runnable
    private var callDurationInSeconds: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        channel = Channel(this)
        //mainClass.muteMicrophone(micStatus)
        //mainClass.muteSpeaker(speakerStatus)
        callDurationHandler = Handler(Looper.getMainLooper())
        callDurationRunnable = object : Runnable {
            override fun run() {
                // Update UI or log the duration
                runOnUiThread {
                    if (channel.checkConnected()) {
                        callDurationInSeconds++
                        findViewById<Button>(R.id.startCall).text = "End Call"
                        findViewById<TextView>(R.id.textView).text = formatDuration(callDurationInSeconds)
                    }
                }
                callDurationHandler.postDelayed(this, 1000)
            }
        }
        findViewById<Button>(R.id.permission).setOnClickListener {
            channel.checkPermission(this) {
                Log.d("MainActivity", "isGranted::: $it")
            }
        }

        findViewById<Button>(R.id.startCall).setOnClickListener {
            if (callStatus) {
                findViewById<Button>(R.id.startCall).text = "Start Call"
                findViewById<TextView>(R.id.textView).text = ""
                channel.stopCall()
                callStatus = false
                callDurationHandler.removeCallbacks(callDurationRunnable)
            } else {
                channel.initialize("dashcam.fvts.in", "670074026473")
                findViewById<Button>(R.id.startCall).text = "Connecting"
                channel.startCall()
                callStatus = true
                callDurationInSeconds = 0 // Reset the timer
                callDurationHandler.post(callDurationRunnable) // Start the handler
            }
        }

        findViewById<Button>(R.id.muteMic).setOnClickListener {
            if (micStatus) {
                findViewById<Button>(R.id.muteMic).text = "Mute Mic"
                channel.muteMicrophone(micStatus)
                micStatus = false
            } else {
                findViewById<Button>(R.id.muteMic).text = "UnMute Mic"
                channel.muteMicrophone(micStatus)
                micStatus = true
            }

        }
        findViewById<Button>(R.id.muteSpeaker).setOnClickListener {
            if (speakerStatus) {
                findViewById<Button>(R.id.muteSpeaker).text = "Mute Speaker"
                channel.muteSpeaker(speakerStatus)
                speakerStatus = false
            } else {
                findViewById<Button>(R.id.muteSpeaker).text = "UnMute Speaker"
                channel.muteSpeaker(speakerStatus)
                speakerStatus = true
            }
        }

    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600 // 1 hour = 3600 seconds
        val minutes = (seconds % 3600) / 60 // Remaining minutes
        val secs = seconds % 60 // Remaining seconds
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}