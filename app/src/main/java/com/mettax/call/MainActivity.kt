package com.mettax.call

import android.content.Intent
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
import com.hk1089.mettax.video.RecordFile
import com.hk1089.mettax.video.VideoSearchHelper

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
                channel.initialize("dashcam.fvts.in", "965083684181")
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

        findViewById<Button>(R.id.openVideoPlayer).setOnClickListener {
            val intent = Intent(this, VideoPlayerExampleActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.openPlaybackPlayer).setOnClickListener {
            val intent = Intent(this, PlaybackPlayerExampleActivity::class.java)
            // Pass search parameters to PlaybackPlayerExampleActivity
            // It will use VideoSearchHelper to search and display results
            intent.putExtra("DevIDNO", "965080512310")
            intent.putExtra("Server", "dashcam.fvts.in")
            intent.putExtra("Port", 6605)  // Matching MainActivity port
            intent.putExtra("direct", false)  // Use "direct" not "IsDirect" to match RecordSearchActivity
            
            // Pass search date/time parameters
            intent.putExtra("Year", 2026)
            intent.putExtra("Month", 5)
            intent.putExtra("Day", 8)
            intent.putExtra("Channel", 0)  // 0 = all channels
            intent.putExtra("BeginTime", 62496)  // Start time in seconds
            intent.putExtra("EndTime", 62796)  // End time in seconds
            
            startActivity(intent)
        }

        // Test VideoSearchHelper
        var searchHelper: VideoSearchHelper? = null
        findViewById<Button>(R.id.testVideoSearch).setOnClickListener {
            Log.d("MainActivity", "========== TESTING VIDEO SEARCH HELPER ==========")
            
            val devIdno = "299076924649"
            val server = "dashcam.fvts.in"
            val port = 6605  // Matching MainActivity port
            val isDirect = false
            
            Log.d("MainActivity", "Creating VideoSearchHelper with:")
            Log.d("MainActivity", "  DevIDNO: $devIdno")
            Log.d("MainActivity", "  Server: $server")
            Log.d("MainActivity", "  Port: $port")
            Log.d("MainActivity", "  IsDirect: $isDirect")
            
            // Create VideoSearchHelper (it will initialize NetClient automatically)
            searchHelper = VideoSearchHelper(this, devIdno, isDirect, server, port)
            
            // Set up listener to log results
            searchHelper?.setSearchListener(object : VideoSearchHelper.VideoSearchListener {
                override fun onSearchStarted() {
                    Log.d("MainActivity", "========== SEARCH STARTED ==========")
                }
                
                override fun onFileFound(file: RecordFile) {
                    Log.d("MainActivity", "========== FILE FOUND ==========")
                    Log.d("MainActivity", "  DevIdno: ${file.getDevIdno()}")
                    Log.d("MainActivity", "  Channel: ${file.getChn()}")
                    Log.d("MainActivity", "  Date: ${file.getYear()}-${String.format("%02d", file.getMonth())}-${String.format("%02d", file.getDay())}")
                    Log.d("MainActivity", "  Time: ${formatSeconds(file.getBeginTime())} - ${formatSeconds(file.getEndTime())}")
                    Log.d("MainActivity", "  Type: ${if (file.getFileType() == 0) "Normal" else "Alarm"}")
                    Log.d("MainActivity", "  Size: ${file.getFileLength()} bytes")
                    Log.d("MainActivity", "  File Info: ${file.getFileInfo()}")
                }
                
                override fun onSearchFinished(fileList: List<RecordFile>) {
                    Log.d("MainActivity", "========== SEARCH FINISHED ==========")
                    Log.d("MainActivity", "Total files found: ${fileList.size}")
                    Log.d("MainActivity", "=====================================")
                    
                    // Print all files
                    for (i in fileList.indices) {
                        val file = fileList[i]
                        Log.d("MainActivity", "--- File #${i + 1} ---")
                        Log.d("MainActivity", "  DevIdno: ${file.getDevIdno()}")
                        Log.d("MainActivity", "  Channel: ${file.getChn()}")
                        Log.d("MainActivity", "  Date: ${file.getYear()}-${String.format("%02d", file.getMonth())}-${String.format("%02d", file.getDay())}")
                        Log.d("MainActivity", "  Time: ${formatSeconds(file.getBeginTime())} - ${formatSeconds(file.getEndTime())}")
                        Log.d("MainActivity", "  Type: ${if (file.getFileType() == 0) "Normal" else "Alarm"}")
                        Log.d("MainActivity", "  Size: ${file.getFileLength()} bytes")
                        Log.d("MainActivity", "  Location: ${file.getLocation()} (1=Device, 2=Storage Server, 4=Download Server)")
                        Log.d("MainActivity", "  Server ID: ${file.getSvrId()}")
                    }
                    Log.d("MainActivity", "=====================================")
                }
                
                override fun onSearchFailed() {
                    Log.e("MainActivity", "========== SEARCH FAILED ==========")
                }
                
                override fun onSearchStopped() {
                    Log.d("MainActivity", "========== SEARCH STOPPED ==========")
                }
            })
            
            // Start search
            Log.d("MainActivity", "Starting search...")
            searchHelper?.startSearch()
        }

    }
    
    private fun formatSeconds(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600 // 1 hour = 3600 seconds
        val minutes = (seconds % 3600) / 60 // Remaining minutes
        val secs = seconds % 60 // Remaining seconds
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}