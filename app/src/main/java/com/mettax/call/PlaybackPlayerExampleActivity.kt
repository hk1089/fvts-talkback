package com.mettax.call

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.hk1089.mettax.video.PlaybackPlayer
import com.hk1089.mettax.video.RecordFile

/**
 * PlaybackPlayerExampleActivity - Uses PlaybackPlayer for search and playback functionality
 * PlaybackPlayer internally uses VideoSearchHelper for search operations
 */
class PlaybackPlayerExampleActivity : AppCompatActivity() {
    
    private val TAG = "PlaybackPlayerExample"
    
    // PlaybackPlayer instance (handles both search and playback)
    private var mPlaybackPlayer: PlaybackPlayer? = null
    
    // Search parameters
    private var mDevIdno: String = ""
    private var mIsDirect: Boolean = false
    private var mServer: String = ""
    private var mPort: Int = 0
    
    // Search date/time parameters
    private var mYear: Int = -1
    private var mMonth: Int = -1
    private var mDay: Int = -1
    private var mChannel: Int = 0
    private var mBeginTime: Int = 0
    private var mEndTime: Int = 86400
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get parameters from Intent (matching RecordSearchActivity pattern exactly)
        if (intent.hasExtra("DevIDNO")) {
            mDevIdno = intent.getStringExtra("DevIDNO") ?: ""
        }
        mIsDirect = intent.getBooleanExtra("direct", false)
        if (mIsDirect) {
            mServer = intent.getStringExtra("serverIp") ?: ""
            mPort = intent.getIntExtra("port", 0)
            mDevIdno = intent.getStringExtra("devIdno") ?: ""
        } else {
            // For server-based, get from Intent or use defaults
            mServer = intent.getStringExtra("Server") ?: "dashcam.fvts.in"
            mPort = intent.getIntExtra("Port", 6605)  // Matching MainActivity port
        }
        
        // Get search date/time parameters from Intent
        mYear = intent.getIntExtra("Year", -1)
        mMonth = intent.getIntExtra("Month", -1)
        mDay = intent.getIntExtra("Day", -1)
        mChannel = intent.getIntExtra("Channel", 0)
        mBeginTime = intent.getIntExtra("BeginTime", 0)
        mEndTime = intent.getIntExtra("EndTime", 86400)
        
        Log.d(TAG, "========== PLAYBACK PLAYER EXAMPLE ACTIVITY ==========")
        Log.d(TAG, "DevIDNO: $mDevIdno")
        Log.d(TAG, "Server: $mServer")
        Log.d(TAG, "Port: $mPort")
        Log.d(TAG, "IsDirect: $mIsDirect")
        Log.d(TAG, "Search Date: Year=$mYear, Month=$mMonth, Day=$mDay")
        Log.d(TAG, "Search Time: BeginTime=$mBeginTime, EndTime=$mEndTime")
        Log.d(TAG, "Search Channel: $mChannel")
        
        // Create PlaybackPlayer (it uses VideoSearchHelper internally)
        mPlaybackPlayer = PlaybackPlayer(this, mDevIdno, mIsDirect, mServer, mPort)
        
        // Set search parameters if provided
        if (mYear > 0 && mMonth > 0 && mDay > 0) {
            mPlaybackPlayer?.setSearchParameters(mYear, mMonth, mDay, mChannel, mBeginTime, mEndTime)
            Log.d(TAG, "Set search parameters: Year=$mYear, Month=$mMonth, Day=$mDay, Channel=$mChannel, BeginTime=$mBeginTime, EndTime=$mEndTime")
        }
        
        // Set up listener for logging
        mPlaybackPlayer?.setPlaybackPlayerListener(object : PlaybackPlayer.PlaybackPlayerListener {
            override fun onSearchStarted() {
                Log.d(TAG, "========== SEARCH STARTED ==========")
            }
            
            override fun onFileFound(file: RecordFile) {
                Log.d(TAG, "========== FILE FOUND ==========")
                Log.d(TAG, "  DevIdno: ${file.getDevIdno()}")
                Log.d(TAG, "  Channel: ${file.getChn()}")
                Log.d(TAG, "  Date: ${file.getYear()}-${String.format("%02d", file.getMonth())}-${String.format("%02d", file.getDay())}")
                Log.d(TAG, "  Time: ${formatSeconds(file.getBeginTime())} - ${formatSeconds(file.getEndTime())}")
                Log.d(TAG, "  Type: ${if (file.getFileType() == 0) "Normal" else "Alarm"}")
                Log.d(TAG, "  Size: ${file.getFileLength()} bytes")
                Log.d(TAG, "  File Info: ${file.getFileInfo()}")
            }
            
            override fun onSearchFinished(fileList: List<RecordFile>) {
                Log.d(TAG, "========== SEARCH FINISHED ==========")
                Log.d(TAG, "Total files found: ${fileList.size}")
                Log.d(TAG, "=====================================")
                
                // Print all files
                for (i in fileList.indices) {
                    val file = fileList[i]
                    Log.d(TAG, "--- File #${i + 1} ---")
                    Log.d(TAG, "  DevIdno: ${file.getDevIdno()}")
                    Log.d(TAG, "  Channel: ${file.getChn()}")
                    Log.d(TAG, "  Date: ${file.getYear()}-${String.format("%02d", file.getMonth())}-${String.format("%02d", file.getDay())}")
                    Log.d(TAG, "  Time: ${formatSeconds(file.getBeginTime())} - ${formatSeconds(file.getEndTime())}")
                    Log.d(TAG, "  Type: ${if (file.getFileType() == 0) "Normal" else "Alarm"}")
                    Log.d(TAG, "  Size: ${file.getFileLength()} bytes")
                    Log.d(TAG, "  Location: ${file.getLocation()} (1=Device, 2=Storage Server, 4=Download Server)")
                    Log.d(TAG, "  Server ID: ${file.getSvrId()}")
                }
                Log.d(TAG, "=====================================")
            }
            
            override fun onSearchFailed() {
                Log.e(TAG, "========== SEARCH FAILED ==========")
            }
            
            override fun onPlaybackStarted(file: RecordFile) {
                Log.d(TAG, "========== PLAYBACK STARTED ==========")
                Log.d(TAG, "Playing file: ${file.getFileInfo()}")
            }
            
            override fun onPlaybackStopped() {
                Log.d(TAG, "========== PLAYBACK STOPPED ==========")
            }
        })
        
        // Create UI using PlaybackPlayer
        val view = mPlaybackPlayer?.createView()
        if (view != null) {
            setContentView(view)
        }
        
        // Start search automatically
        mPlaybackPlayer?.startSearch()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup resources
        mPlaybackPlayer?.stopPlayback()
        mPlaybackPlayer?.stopSearch()
        mPlaybackPlayer?.destroy()
        mPlaybackPlayer = null
    }
    
    /**
     * Format seconds to HH:MM:SS
     */
    private fun formatSeconds(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}
