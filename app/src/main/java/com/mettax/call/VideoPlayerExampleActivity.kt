package com.mettax.call

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.hk1089.mettax.video.VideoPlayer

class VideoPlayerExampleActivity : AppCompatActivity() {
    
    private lateinit var mVideoPlayer: VideoPlayer
    private lateinit var mStartButton: Button
    private lateinit var mStopButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create VideoPlayer with server, device ID, and channel count
        mVideoPlayer = VideoPlayer(
            this,
            "dashcam.fvts.in",
            "670076844832",
            2  // Create 2 video channels
        )
        
        // Set up the UI
        setupUI()

        // Set video player listener
        mVideoPlayer.setVideoPlayerListener(object : VideoPlayer.VideoPlayerListener {
            override fun onVideoClick(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index clicked")
            }
            
            override fun onVideoDoubleClick(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index double clicked")
            }
            
            override fun onVideoMoveLeft(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index move left")
            }
            
            override fun onVideoMoveRight(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index move right")
            }
            
            override fun onVideoMoveUp(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index move up")
            }
            
            override fun onVideoMoveDown(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index move down")
            }
            
            override fun onVideoMoveStop(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index move stop")
            }
            
            override fun onVideoFullscreen(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index fullscreen requested")
                // TODO: Implement fullscreen functionality
            }
            
            override fun onVideoRecordStart(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index recording started")
            }
            
            override fun onVideoRecordStop(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index recording stopped")
            }
            
            override fun onVideoSnapshot(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index snapshot taken")
            }
            
            override fun onVideoAudioStart(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index audio started")
            }
            
            override fun onVideoAudioStop(view: com.hk1089.mettax.video.VideoView, index: Int) {
                Log.d("VideoPlayerExample", "Video $index audio stopped")
            }
        })
        
        // Start video automatically
        mVideoPlayer.startVideo()
    }
    
    private fun setupUI() {
        // Create main layout
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // Add video player layout
        mainLayout.addView(mVideoPlayer.mainLayout)
        

        setContentView(mainLayout)
    }
    
    override fun onBackPressed() {
        // Handle back button press for fullscreen
        if (mVideoPlayer.onBackPressed()) {
            // VideoPlayer handled the back press (exited fullscreen)
            return
        }
        // Let the system handle the back press normally
        super.onBackPressed()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("VideoPlayerExample", "Configuration changed - orientation: ${newConfig.orientation}")
        
        // Notify VideoPlayer about configuration change
        mVideoPlayer.onConfigurationChanged(newConfig)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        mVideoPlayer.destroy()
    }
}
