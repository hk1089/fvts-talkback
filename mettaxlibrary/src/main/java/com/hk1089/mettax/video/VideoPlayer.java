package com.hk1089.mettax.video;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.content.res.Resources;
import android.os.Environment;
import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.babelstar.gviewer.NetClient;

import java.util.ArrayList;
import java.util.List;

public class VideoPlayer {
    
    private Context mContext;
    private Activity mActivity;
    private String mServer;
    private String mDevIdno;
    private int mChannelCount;
    private NetClient mNetClient;
    private boolean mIsInitialized = false;
    private boolean mIsPlaying = false;
    
    // Video components
    private List<VideoView> mVideoViews;
    private List<RealPlay> mRealPlays;
    private LinearLayout mMainLayout;
    private boolean[] mIsMuted; // Track mute state for each channel
    
    // Fullscreen components
    private boolean mIsFullscreen = false;
    private int mFullscreenChannel = -1;
    private android.widget.FrameLayout mFullscreenLayout;
    private VideoView mFullscreenVideoView;
    private RealPlay mFullscreenRealPlay;
    private Button mFullscreenCloseBtn;
    private Button mFullscreenMuteBtn;
    private Button mFullscreenPlayPauseBtn;
    private android.widget.LinearLayout mFullscreenControlsLayout;
    private boolean mControlsVisible = true;
    private android.os.Handler mControlsHandler = new android.os.Handler();
    private Runnable mHideControlsRunnable;
    
    // Listeners
    private VideoPlayerListener mVideoPlayerListener;
    
    public VideoPlayer(Activity activity, String server, String deviceId, int channelCount) {
        mActivity = activity;
        mContext = activity.getApplicationContext();
        mServer = server;
        mDevIdno = deviceId;
        mChannelCount = channelCount;
        
        mVideoViews = new ArrayList<>();
        mRealPlays = new ArrayList<>();
        mIsMuted = new boolean[channelCount]; // Initialize mute state array
        
        // Initialize all channels as muted
        for (int i = 0; i < channelCount; i++) {
            mIsMuted[i] = true; // All channels start muted
        }
        
        initializeComponents();
        initializeNetClient();
    }
    
    private void initializeComponents() {
        // Create main layout for 2x2 grid
        mMainLayout = new LinearLayout(mActivity);
        mMainLayout.setOrientation(LinearLayout.VERTICAL);
        
        // Create first row (2 videos)
        LinearLayout firstRow = new LinearLayout(mActivity);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);
        firstRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            400, // Fixed height for first row
            1.0f));
        
        // Create second row (2 videos)
        LinearLayout secondRow = new LinearLayout(mActivity);
        secondRow.setOrientation(LinearLayout.HORIZONTAL);
        secondRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            400, // Fixed height for second row
            1.0f));
        
        // Create video views and real play instances
        for (int i = 0; i < mChannelCount; i++) {
            // Create RelativeLayout wrapper for video + controls
            RelativeLayout videoContainer = new RelativeLayout(mActivity);
            
            // Create VideoView
            VideoView videoView = new VideoView(mActivity, i);
            videoView.setId(1000 + i); // Unique ID for video view
            
            // Set layout parameters for video view
            RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
            videoView.setLayoutParams(videoParams);
            videoContainer.addView(videoView);
            
            // Create control buttons
            createControlButtons(videoContainer, i);
            
            // Set layout parameters for container
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                0, // Width will be determined by weight
                LinearLayout.LayoutParams.MATCH_PARENT, 
                1.0f); // Weight for equal distribution
            
            // Set margins for grid spacing
            containerParams.setMargins(5, 5, 5, 5);
            videoContainer.setLayoutParams(containerParams);
            
            mVideoViews.add(videoView);
            
            // Add to appropriate row
            if (i < 2) {
                firstRow.addView(videoContainer);
            } else {
                secondRow.addView(videoContainer);
            }
            
            // Create RealPlay
            RealPlay realPlay = new RealPlay(mActivity);
            realPlay.setVideoView(videoView);
            realPlay.setPlayerListener(new RealPlay.PlayListener() {
                @Override
                public void onBeginPlay() {
                    // Video playback started
                }
                
                @Override
                public void onPtzCtrl(VideoView view, int index) {
                    // PTZ control
                }
                
                @Override
                public void onClick(VideoView view, int index) {
                    if (mVideoPlayerListener != null) {
                        mVideoPlayerListener.onVideoClick(view, index);
                    }
                }
                
                @Override
                public void onDbClick(VideoView view, int index) {
                    if (mVideoPlayerListener != null) {
                        mVideoPlayerListener.onVideoDoubleClick(view, index);
                    }
                }
                
                @Override
                public void onMoveLeft(VideoView view, int index) {
                    if (mVideoPlayerListener != null) {
                        mVideoPlayerListener.onVideoMoveLeft(view, index);
                    }
                }
                
                @Override
                public void onMoveRight(VideoView view, int index) {
                    if (mVideoPlayerListener != null) {
                        mVideoPlayerListener.onVideoMoveRight(view, index);
                    }
                }
            });
            mRealPlays.add(realPlay);
        }
        
        // Add rows to main layout
        mMainLayout.addView(firstRow);
        mMainLayout.addView(secondRow);
    }
    
    private void createControlButtons(RelativeLayout container, int videoIndex) {
        Resources resources = mActivity.getResources();
        
        // Play/Pause button
        Button playPauseBtn = new Button(mActivity);
        playPauseBtn.setId(2000 + videoIndex);
        playPauseBtn.setBackgroundColor(Color.parseColor("#80000000")); // Semi-transparent black
        
        // Set initial icon based on video state
        try {
            RealPlay realPlay = mRealPlays.get(videoIndex);
            if (realPlay != null && realPlay.isViewing()) {
                // Video is playing, show pause icon
                Drawable pauseIcon = ContextCompat.getDrawable(mActivity, 
                    resources.getIdentifier("ic_pause", "drawable", mActivity.getPackageName()));
                if (pauseIcon != null) {
                    pauseIcon.setBounds(0, 0, 70, 70);
                    playPauseBtn.setCompoundDrawables(pauseIcon, null, null, null);
                } else {
                    playPauseBtn.setText("⏸"); // Fallback to text
                }
            } else {
                // Video is not playing, show play icon
                Drawable playIcon = ContextCompat.getDrawable(mActivity, 
                    resources.getIdentifier("ic_play", "drawable", mActivity.getPackageName()));
                if (playIcon != null) {
                    playIcon.setBounds(0, 0, 70, 70);
                    playPauseBtn.setCompoundDrawables(playIcon, null, null, null);
                } else {
                    playPauseBtn.setText("▶"); // Fallback to text
                }
            }
        } catch (Exception e) {
            playPauseBtn.setText("▶"); // Fallback to text
        }

        
        RelativeLayout.LayoutParams playParams = new RelativeLayout.LayoutParams(70, 70);
        playParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        playParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        playParams.setMargins(30, 0, 0, 10);
        playPauseBtn.setLayoutParams(playParams);
        
        playPauseBtn.setOnClickListener(v -> {
            RealPlay realPlay = mRealPlays.get(videoIndex);
            if (realPlay.isViewing()) {
                realPlay.StopAV();
                // Set play icon
                try {
                    Drawable playIcon = ContextCompat.getDrawable(mActivity, 
                        resources.getIdentifier("ic_play", "drawable", mActivity.getPackageName()));
                    if (playIcon != null) {
                        playIcon.setBounds(0, 0, 70, 70);
                        playPauseBtn.setCompoundDrawables(playIcon, null, null, null);
                    } else {
                        playPauseBtn.setText("▶");
                    }
                } catch (Exception e) {
                    playPauseBtn.setText("▶");
                }
            } else {
                realPlay.StartAV(false, true);
                // Set pause icon
                try {
                    Drawable pauseIcon = ContextCompat.getDrawable(mActivity, 
                        resources.getIdentifier("ic_pause", "drawable", mActivity.getPackageName()));
                    if (pauseIcon != null) {
                        pauseIcon.setBounds(0, 0, 70, 70);
                        playPauseBtn.setCompoundDrawables(pauseIcon, null, null, null);
                    } else {
                        playPauseBtn.setText("⏸");
                    }
                } catch (Exception e) {
                    playPauseBtn.setText("⏸");
                }
            }
        });
        container.addView(playPauseBtn);
        
        // Mute/Unmute button
        Button muteBtn = new Button(mActivity);
        muteBtn.setId(3000 + videoIndex);
        muteBtn.setBackgroundColor(Color.parseColor("#80000000"));
        
        // Set initial audio state - start with muted (no audio)
        // mIsMuted[videoIndex] is already set to true in constructor
        try {
            Drawable muteIcon = ContextCompat.getDrawable(mActivity, 
                resources.getIdentifier("ic_mute", "drawable", mActivity.getPackageName()));
            if (muteIcon != null) {
                muteIcon.setBounds(0, 0, 70, 70);
                muteBtn.setCompoundDrawables(muteIcon, null, null, null);
            } else {
                muteBtn.setText("🔇"); // Fallback to text
            }
        } catch (Exception e) {
            muteBtn.setText("🔇"); // Fallback to text
        }

        
        RelativeLayout.LayoutParams muteParams = new RelativeLayout.LayoutParams(70, 70);
        muteParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        muteParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        muteParams.setMargins(250, 0, 0, 10); // Position to the right of snapshot button
        muteBtn.setLayoutParams(muteParams);
        
        muteBtn.setOnClickListener(v -> {
            RealPlay realPlay = mRealPlays.get(videoIndex);
            if (realPlay != null) {
                if (mIsMuted[videoIndex]) {
                    // Currently muted, unmute this channel
                    // First, mute all other channels
                    muteAllOtherChannels(videoIndex);
                    
                    // Then unmute this channel
                    mIsMuted[videoIndex] = false;
                    realPlay.playSound();
                    try {
                        Drawable unmuteIcon = ContextCompat.getDrawable(mActivity, 
                            resources.getIdentifier("ic_unmute", "drawable", mActivity.getPackageName()));
                        if (unmuteIcon != null) {
                            unmuteIcon.setBounds(0, 0, 70, 70);
                            muteBtn.setCompoundDrawables(unmuteIcon, null, null, null);
                        } else {
                            muteBtn.setText("🔊");
                        }
                    } catch (Exception e) {
                        muteBtn.setText("🔊");
                    }
                    Log.d("VideoPlayer", "Unmuted audio for channel " + videoIndex + " (muted all others)");
                    if (mVideoPlayerListener != null) {
                        mVideoPlayerListener.onVideoAudioStart(mVideoViews.get(videoIndex), videoIndex);
                    }
                } else {
                    // Currently unmuted, mute this channel
                    mIsMuted[videoIndex] = true;
                    realPlay.stopSound();
                    try {
                        Drawable muteIcon = ContextCompat.getDrawable(mActivity, 
                            resources.getIdentifier("ic_mute", "drawable", mActivity.getPackageName()));
                        if (muteIcon != null) {
                            muteIcon.setBounds(0, 0, 70, 70);
                            muteBtn.setCompoundDrawables(muteIcon, null, null, null);
                        } else {
                            muteBtn.setText("🔇");
                        }
                    } catch (Exception e) {
                        muteBtn.setText("🔇");
                    }
                    Log.d("VideoPlayer", "Muted audio for channel " + videoIndex);
                    if (mVideoPlayerListener != null) {
                        mVideoPlayerListener.onVideoAudioStop(mVideoViews.get(videoIndex), videoIndex);
                    }
                }
            }
        });
        container.addView(muteBtn);
        
        // Fullscreen button
        Button fullscreenBtn = new Button(mActivity);
        fullscreenBtn.setId(4000 + videoIndex);
        fullscreenBtn.setBackgroundColor(Color.parseColor("#80000000"));
        
        // Set fullscreen icon
        try {
            Drawable fullscreenIcon = ContextCompat.getDrawable(mActivity, 
                resources.getIdentifier("ic_fullscreen", "drawable", mActivity.getPackageName()));
            if (fullscreenIcon != null) {
                fullscreenIcon.setBounds(0, 0, 70, 70);
                fullscreenBtn.setCompoundDrawables(fullscreenIcon, null, null, null);
            } else {
                fullscreenBtn.setText("⛶"); // Fallback to text
            }
        } catch (Exception e) {
            fullscreenBtn.setText("⛶"); // Fallback to text
        }

        
        RelativeLayout.LayoutParams fullscreenParams = new RelativeLayout.LayoutParams(70, 70);
        fullscreenParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        fullscreenParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        fullscreenParams.setMargins(0, 0, 30, 10);
        fullscreenBtn.setLayoutParams(fullscreenParams);
        
        fullscreenBtn.setOnClickListener(v -> {
            enterFullscreen(videoIndex);
        });
        container.addView(fullscreenBtn);
        
        // Record button
        Button recordBtn = new Button(mActivity);
        recordBtn.setId(5000 + videoIndex);
        recordBtn.setBackgroundColor(Color.parseColor("#80000000"));
        recordBtn.setText("REC"); // Record icon
        recordBtn.setTextColor(Color.WHITE);
        recordBtn.setTextSize(12);
        
        RelativeLayout.LayoutParams recordParams = new RelativeLayout.LayoutParams(50, 50);
        recordParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        recordParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        recordParams.setMargins(10, 10, 0, 0);
        recordBtn.setLayoutParams(recordParams);
        
        recordBtn.setOnClickListener(v -> {
            RealPlay realPlay = mRealPlays.get(videoIndex);
            if (realPlay != null) {
                if (realPlay.isRecording()) {
                    realPlay.stopRecord();
                    recordBtn.setText("REC");
                    recordBtn.setBackgroundColor(Color.parseColor("#80000000"));
                } else {
                    realPlay.startRecord();
                    recordBtn.setText("STOP");
                    recordBtn.setBackgroundColor(Color.parseColor("#80FF0000")); // Red background
                }
            }
        });
        container.addView(recordBtn);
        
        // Snapshot button
        Button snapshotBtn = new Button(mActivity);
        snapshotBtn.setId(6000 + videoIndex);
        snapshotBtn.setBackgroundColor(Color.parseColor("#80000000"));
        
        // Set snapshot icon from drawable
        try {
            Resources res = mActivity.getResources();
            Drawable snapIcon = ContextCompat.getDrawable(mActivity, 
                res.getIdentifier("ic_snap", "drawable", mActivity.getPackageName()));
            if (snapIcon != null) {
                snapIcon.setBounds(0, 0, 70, 70);
                snapshotBtn.setCompoundDrawables(snapIcon, null, null, null);
            } else {
                snapshotBtn.setText("📷"); // Fallback to text icon
                snapshotBtn.setTextColor(Color.WHITE);
                snapshotBtn.setTextSize(12);
            }
        } catch (Exception e) {
            snapshotBtn.setText("📷"); // Fallback to text icon
            snapshotBtn.setTextColor(Color.WHITE);
            snapshotBtn.setTextSize(12);
        }
        
        RelativeLayout.LayoutParams snapshotParams = new RelativeLayout.LayoutParams(70, 70);
        snapshotParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        snapshotParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        snapshotParams.setMargins(140, 0, 0, 10); // Position to the right of play/pause button
        snapshotBtn.setLayoutParams(snapshotParams);
        
        snapshotBtn.setOnClickListener(v -> {
            RealPlay realPlay = mRealPlays.get(videoIndex);
            if (realPlay != null) {
                // Generate unique filename with timestamp
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String filename = "snapshot_ch" + (videoIndex + 1) + "_" + timestamp + ".png";
                
                // Get the external files directory
                java.io.File externalDir = mActivity.getExternalFilesDir(null);
                if (externalDir != null) {
                    java.io.File snapshotsDir = new java.io.File(externalDir, "snapshots");
                    if (!snapshotsDir.exists()) {
                        snapshotsDir.mkdirs();
                    }
                    
                    java.io.File snapshotFile = new java.io.File(snapshotsDir, filename);
                    
                    // Save the snapshot
                    boolean success = realPlay.savePngFile(snapshotFile.getAbsolutePath());
                    if (success) {
                        Log.d("VideoPlayer", "Snapshot saved successfully: " + snapshotFile.getAbsolutePath());
                        
                        // Show success message and open dialog
                        showSnapshotDialog(snapshotFile, videoIndex);
                        
                        if (mVideoPlayerListener != null) {
                            mVideoPlayerListener.onVideoSnapshot(mVideoViews.get(videoIndex), videoIndex);
                        }
                    } else {
                        Log.e("VideoPlayer", "Failed to save snapshot for channel " + videoIndex);
                        showSnapError("Failed to capture snapshot");
                    }
                } else {
                    Log.e("VideoPlayer", "External files directory not available");
                    showSnapError("Storage not available");
                }
            }
        });
        container.addView(snapshotBtn);
    }
    
    private void initializeNetClient() {
        try {
            String sdPath = mContext.getExternalFilesDir("").getAbsolutePath() + "/";
            Log.d("VideoPlayer", "Initializing NetClient with path: " + sdPath);
            
            mNetClient = new NetClient();
            mNetClient.Initialize(sdPath);
            mNetClient.SetJniEnv();
            mNetClient.SetSession("");
            
            // Set server configuration
            mNetClient.SetDirSvr(mServer, mServer, 6605, 0);
            
            mIsInitialized = true;
            Log.d("VideoPlayer", "NetClient initialized successfully");
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error initializing NetClient: " + e.getMessage());
            mIsInitialized = false;
        }
    }
    
    public void startVideo() {
        if (!mIsInitialized) {
            Log.e("VideoPlayer", "VideoPlayer not initialized");
            return;
        }
        
        if (mIsPlaying) {
            Log.d("VideoPlayer", "Video already playing");
            return;
        }
        
        Log.d("VideoPlayer", "Starting video for " + mChannelCount + " channels");
        
        for (int i = 0; i < mChannelCount; i++) {
            RealPlay realPlay = mRealPlays.get(i);
            realPlay.setViewInfo(mDevIdno, mDevIdno, i, "CH" + (i+1), 0);
            realPlay.StartAV(false, true);
        }
        
        mIsPlaying = true;
        Log.d("VideoPlayer", "Video started successfully");
        
        // Update button states after starting videos
        updateButtonStates();
    }
    
    public void stopVideo() {
        if (!mIsPlaying) {
            Log.d("VideoPlayer", "Video not playing");
            return;
        }
        
        Log.d("VideoPlayer", "Stopping video");
        
        for (RealPlay realPlay : mRealPlays) {
            realPlay.StopAV();
        }
        
        mIsPlaying = false;
        Log.d("VideoPlayer", "Video stopped successfully");
        
        // Update button states after stopping videos
        updateButtonStates();
    }
    
    public void destroy() {
        stopVideo();
        if (mNetClient != null) {
            mNetClient.UnInitialize();
        }
        Log.d("VideoPlayer", "VideoPlayer destroyed");
    }
    
    public LinearLayout getMainLayout() {
        return mMainLayout;
    }
    
    public void setVideoPlayerListener(VideoPlayerListener listener) {
        mVideoPlayerListener = listener;
    }
    
    public void updateButtonStates() {
        // Update all button states based on current video playback status
        for (int i = 0; i < mRealPlays.size(); i++) {
            RealPlay realPlay = mRealPlays.get(i);
            if (realPlay != null) {
                // Find the play/pause button for this video
                Button playPauseBtn = mMainLayout.findViewById(2000 + i);
                if (playPauseBtn != null) {
                    try {
                        Resources resources = mActivity.getResources();
                        if (realPlay.isViewing()) {
                            // Video is playing, show pause icon
                            Drawable pauseIcon = ContextCompat.getDrawable(mActivity, 
                                resources.getIdentifier("ic_pause", "drawable", mActivity.getPackageName()));
                            if (pauseIcon != null) {
                                pauseIcon.setBounds(0, 0, 70, 70);
                                playPauseBtn.setCompoundDrawables(pauseIcon, null, null, null);
                            } else {
                                playPauseBtn.setText("⏸");
                            }
                        } else {
                            // Video is not playing, show play icon
                            Drawable playIcon = ContextCompat.getDrawable(mActivity, 
                                resources.getIdentifier("ic_play", "drawable", mActivity.getPackageName()));
                            if (playIcon != null) {
                                playIcon.setBounds(0, 0, 70, 70);
                                playPauseBtn.setCompoundDrawables(playIcon, null, null, null);
                            } else {
                                playPauseBtn.setText("▶");
                            }
                        }
                    } catch (Exception e) {
                        // Fallback to text
                        if (realPlay.isViewing()) {
                            playPauseBtn.setText("⏸");
                        } else {
                            playPauseBtn.setText("▶");
                        }
                    }
                }
                
                // Update mute/unmute button state
                Button muteBtn = mMainLayout.findViewById(3000 + i);
                if (muteBtn != null) {
                    try {
                        Resources resources = mActivity.getResources();
                        if (mIsMuted[i]) {
                            // Muted state, show mute icon
                            Drawable muteIcon = ContextCompat.getDrawable(mActivity, 
                                resources.getIdentifier("ic_mute", "drawable", mActivity.getPackageName()));
                            if (muteIcon != null) {
                                muteIcon.setBounds(0, 0, 70, 70);
                                muteBtn.setCompoundDrawables(muteIcon, null, null, null);
                            } else {
                                muteBtn.setText("🔇");
                            }
                        } else {
                            // Unmuted state, show unmute icon
                            Drawable unmuteIcon = ContextCompat.getDrawable(mActivity, 
                                resources.getIdentifier("ic_unmute", "drawable", mActivity.getPackageName()));
                            if (unmuteIcon != null) {
                                unmuteIcon.setBounds(0, 0, 70, 70);
                                muteBtn.setCompoundDrawables(unmuteIcon, null, null, null);
                            } else {
                                muteBtn.setText("🔊");
                            }
                        }
                    } catch (Exception e) {
                        // Fallback to text
                        if (mIsMuted[i]) {
                            muteBtn.setText("🔇");
                        } else {
                            muteBtn.setText("🔊");
                        }
                    }
                }
            }
        }
    }
    
    // Audio control methods
    public void playSound(int channelIndex) {
        if (channelIndex >= 0 && channelIndex < mRealPlays.size()) {
            RealPlay realPlay = mRealPlays.get(channelIndex);
            if (realPlay != null) {
                realPlay.playSound();
                Log.d("VideoPlayer", "Started audio for channel " + channelIndex);
                // Update button state
                updateMuteButtonState(channelIndex);
                if (mVideoPlayerListener != null) {
                    mVideoPlayerListener.onVideoAudioStart(mVideoViews.get(channelIndex), channelIndex);
                }
            }
        }
    }
    
    public void stopSound(int channelIndex) {
        if (channelIndex >= 0 && channelIndex < mRealPlays.size()) {
            RealPlay realPlay = mRealPlays.get(channelIndex);
            if (realPlay != null) {
                realPlay.stopSound();
                Log.d("VideoPlayer", "Stopped audio for channel " + channelIndex);
                // Update button state
                updateMuteButtonState(channelIndex);
                if (mVideoPlayerListener != null) {
                    mVideoPlayerListener.onVideoAudioStop(mVideoViews.get(channelIndex), channelIndex);
                }
            }
        }
    }
    
    public void stopAllSound() {
        for (RealPlay realPlay : mRealPlays) {
            if (realPlay != null) {
                realPlay.stopSound();
            }
        }
        Log.d("VideoPlayer", "Stopped all audio");
    }
    
    // Recording control methods
    public void startRecord(int channelIndex) {
        if (channelIndex >= 0 && channelIndex < mRealPlays.size()) {
            RealPlay realPlay = mRealPlays.get(channelIndex);
            if (realPlay != null) {
                boolean success = realPlay.startRecord();
                Log.d("VideoPlayer", "Started recording for channel " + channelIndex + ": " + success);
            }
        }
    }
    
    public void stopRecord(int channelIndex) {
        if (channelIndex >= 0 && channelIndex < mRealPlays.size()) {
            RealPlay realPlay = mRealPlays.get(channelIndex);
            if (realPlay != null) {
                boolean success = realPlay.stopRecord();
                Log.d("VideoPlayer", "Stopped recording for channel " + channelIndex + ": " + success);
            }
        }
    }
    
    public void stopAllRecord() {
        for (RealPlay realPlay : mRealPlays) {
            if (realPlay != null) {
                realPlay.stopRecord();
            }
        }
        Log.d("VideoPlayer", "Stopped all recording");
    }
    
    // Snapshot methods
    public boolean takeSnapshot(int channelIndex) {
        if (channelIndex >= 0 && channelIndex < mRealPlays.size()) {
            RealPlay realPlay = mRealPlays.get(channelIndex);
            if (realPlay != null) {
                boolean success = realPlay.savePngFile();
                Log.d("VideoPlayer", "Snapshot for channel " + channelIndex + ": " + success);
                return success;
            }
        }
        return false;
    }
    
    // PTZ control methods
    public void ptzControl(int channelIndex, int command, int param) {
        if (channelIndex >= 0 && channelIndex < mRealPlays.size()) {
            RealPlay realPlay = mRealPlays.get(channelIndex);
            if (realPlay != null) {
                realPlay.ptzControl(command, param);
                Log.d("VideoPlayer", "PTZ control for channel " + channelIndex + " command: " + command);
            }
        }
    }
    
    // Convenience PTZ methods
    public void ptzMoveLeft(int channelIndex) {
        ptzControl(channelIndex, 0, 0); // GPS_PTZ_MOVE_LEFT
    }
    
    public void ptzMoveRight(int channelIndex) {
        ptzControl(channelIndex, 1, 0); // GPS_PTZ_MOVE_RIGHT
    }
    
    public void ptzMoveUp(int channelIndex) {
        ptzControl(channelIndex, 2, 0); // GPS_PTZ_MOVE_TOP
    }
    
    public void ptzMoveDown(int channelIndex) {
        ptzControl(channelIndex, 3, 0); // GPS_PTZ_MOVE_BOTTOM
    }
    
    public void ptzZoomIn(int channelIndex) {
        ptzControl(channelIndex, 12, 0); // GPS_PTZ_ZOOM_ADD
    }
    
    public void ptzZoomOut(int channelIndex) {
        ptzControl(channelIndex, 13, 0); // GPS_PTZ_ZOOM_DEL
    }
    
    public void ptzStop(int channelIndex) {
        ptzControl(channelIndex, 19, 0); // GPS_PTZ_MOVE_STOP
    }
    
    // Helper method to mute all channels except the specified one
    private void muteAllOtherChannels(int excludeChannel) {
        for (int i = 0; i < mRealPlays.size(); i++) {
            if (i != excludeChannel && !mIsMuted[i]) {
                // This channel is currently unmuted, mute it
                mIsMuted[i] = true;
                RealPlay realPlay = mRealPlays.get(i);
                if (realPlay != null) {
                    realPlay.stopSound();
                }
                
                // Update the button icon
                Button muteBtn = mMainLayout.findViewById(3000 + i);
                if (muteBtn != null) {
                    try {
                        Resources resources = mActivity.getResources();
                        Drawable muteIcon = ContextCompat.getDrawable(mActivity, 
                            resources.getIdentifier("ic_mute", "drawable", mActivity.getPackageName()));
                        if (muteIcon != null) {
                            muteIcon.setBounds(0, 0, 70, 70);
                            muteBtn.setCompoundDrawables(muteIcon, null, null, null);
                        } else {
                            muteBtn.setText("🔇");
                        }
                    } catch (Exception e) {
                        muteBtn.setText("🔇");
                    }
                }
                
                Log.d("VideoPlayer", "Auto-muted channel " + i + " (switching to channel " + excludeChannel + ")");
                if (mVideoPlayerListener != null) {
                    mVideoPlayerListener.onVideoAudioStop(mVideoViews.get(i), i);
                }
            }
        }
    }
    
    // Helper method to update mute button state for a specific channel
    private void updateMuteButtonState(int channelIndex) {
        if (channelIndex >= 0 && channelIndex < mRealPlays.size()) {
            Button muteBtn = mMainLayout.findViewById(3000 + channelIndex);
            if (muteBtn != null) {
                try {
                    Resources resources = mActivity.getResources();
                    if (mIsMuted[channelIndex]) {
                        // Muted state, show mute icon
                        Drawable muteIcon = ContextCompat.getDrawable(mActivity, 
                            resources.getIdentifier("ic_mute", "drawable", mActivity.getPackageName()));
                        if (muteIcon != null) {
                            muteIcon.setBounds(0, 0, 70, 70);
                            muteBtn.setCompoundDrawables(muteIcon, null, null, null);
                        } else {
                            muteBtn.setText("🔇");
                        }
                    } else {
                        // Unmuted state, show unmute icon
                        Drawable unmuteIcon = ContextCompat.getDrawable(mActivity, 
                            resources.getIdentifier("ic_unmute", "drawable", mActivity.getPackageName()));
                        if (unmuteIcon != null) {
                            unmuteIcon.setBounds(0, 0, 70, 70);
                            muteBtn.setCompoundDrawables(unmuteIcon, null, null, null);
                        } else {
                            muteBtn.setText("🔊");
                        }
                    }
                } catch (Exception e) {
                    // Fallback to text
                    if (mIsMuted[channelIndex]) {
                        muteBtn.setText("🔇");
                    } else {
                        muteBtn.setText("🔊");
                    }
                }
            }
        }
    }
    
    public boolean isPlaying() {
        return mIsPlaying;
    }
    
    public boolean isInitialized() {
        return mIsInitialized;
    }
    
    // Fullscreen functionality
    public void enterFullscreen(int channelIndex) {
        if (mIsFullscreen || channelIndex < 0 || channelIndex >= mRealPlays.size()) {
            return;
        }
        
        mIsFullscreen = true;
        mFullscreenChannel = channelIndex;
        
        // Create fullscreen layout first
        createFullscreenLayout();
        
        // Transfer video stream to fullscreen view
        transferToFullscreen(channelIndex);
        
        // Add fullscreen layout to activity's root view
        try {
            android.view.ViewGroup rootView = (android.view.ViewGroup) mActivity.findViewById(android.R.id.content);
            if (rootView != null) {
                // Use FrameLayout parameters for complete screen coverage
                android.widget.FrameLayout.LayoutParams fullscreenParams = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT, 
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
                fullscreenParams.gravity = android.view.Gravity.FILL;
                rootView.addView(mFullscreenLayout, fullscreenParams);
                
                // Bring fullscreen layout to front
                mFullscreenLayout.bringToFront();
                mFullscreenLayout.setVisibility(android.view.View.VISIBLE);
                
                // Hide system UI for true fullscreen
                hideSystemUI();
                
                // Force layout refresh
                mFullscreenLayout.requestLayout();
                mFullscreenLayout.invalidate();
                
                Log.d("VideoPlayer", "Fullscreen layout added and made visible");
            }
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error adding fullscreen layout: " + e.getMessage());
        }
        
        // Rotate video view to landscape without rotating activity
        mActivity.getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                rotateVideoToLandscape();
                
                // Ensure layout is properly displayed after rotation
                if (mFullscreenLayout != null) {
                    mFullscreenLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            mFullscreenLayout.requestLayout();
                            mFullscreenLayout.invalidate();
                        }
                    });
                }
            }
        });
        
        Log.d("VideoPlayer", "Entered fullscreen for channel " + channelIndex);
        if (mVideoPlayerListener != null) {
            mVideoPlayerListener.onVideoFullscreen(mVideoViews.get(channelIndex), channelIndex);
        }
    }
    
    public void exitFullscreen() {
        if (!mIsFullscreen) {
            return;
        }
        
        // Stop auto-hide timer
        stopAutoHideTimer();
        
        // Reset controls visibility
        mControlsVisible = true;
        if (mFullscreenControlsLayout != null) {
            mFullscreenControlsLayout.setAlpha(1.0f);
            mFullscreenControlsLayout.setVisibility(android.view.View.VISIBLE);
        }
        
        // Restore video view to portrait orientation
        restoreVideoToPortrait();
        
        // Transfer video stream back to original view
        transferFromFullscreen();
        
        // Remove fullscreen layout
        if (mFullscreenLayout != null && mFullscreenLayout.getParent() != null) {
            try {
                ((android.view.ViewGroup) mFullscreenLayout.getParent()).removeView(mFullscreenLayout);
            } catch (Exception e) {
                Log.e("VideoPlayer", "Error removing fullscreen layout: " + e.getMessage());
            }
        }
        
        // Show system UI
        showSystemUI();
        
        // Force layout refresh to ensure proper positioning after a short delay
        if (mMainLayout != null) {
            mMainLayout.post(new Runnable() {
                @Override
                public void run() {
                    mMainLayout.requestLayout();
                    mMainLayout.invalidate();
                    
                    // Additional layout refresh to ensure proper positioning
                    mMainLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            mMainLayout.requestLayout();
                        }
                    });
                }
            });
        }
        
        // Also refresh the activity's content view
        mActivity.getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mActivity.getWindow().getDecorView().requestLayout();
            }
        });
        
        mIsFullscreen = false;
        mFullscreenChannel = -1;
        
        Log.d("VideoPlayer", "Exited fullscreen and returned to grid");
    }

    // Helper to convert dp -> px
    private int dp(int v) {
        return (int) (v * mActivity.getResources().getDisplayMetrics().density);
    }

    private void createFullscreenLayout() {
        // Fullscreen container
        mFullscreenLayout = new android.widget.FrameLayout(mActivity);
        mFullscreenLayout.setBackgroundColor(android.graphics.Color.BLACK);
        mFullscreenLayout.setFitsSystemWindows(false);
        mFullscreenLayout.setVisibility(android.view.View.VISIBLE);

        // Fullscreen VideoView
        mFullscreenVideoView = new VideoView(mActivity, 0);
        android.widget.FrameLayout.LayoutParams videoParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        videoParams.gravity = android.view.Gravity.CENTER;
        mFullscreenVideoView.setLayoutParams(videoParams);
        mFullscreenVideoView.setBackgroundColor(android.graphics.Color.BLACK);
        mFullscreenVideoView.setFitsSystemWindows(false);
        
        // Add touch listener for hide/show controls
        mFullscreenVideoView.setOnTouchListener(new android.view.View.OnTouchListener() {
            @Override
            public boolean onTouch(android.view.View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    toggleControlsVisibility();
                }
                return true;
            }
        });

        // Controls container (left vertical column)
        mFullscreenControlsLayout = new android.widget.LinearLayout(mActivity);
        mFullscreenControlsLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        mFullscreenControlsLayout.setGravity(android.view.Gravity.CENTER);
        mFullscreenControlsLayout.setPadding(dp(0), dp(16), dp(16), dp(16));
        mFullscreenControlsLayout.setBackgroundColor(android.graphics.Color.parseColor("#00000000")); // semi-opaque

        android.widget.FrameLayout.LayoutParams controlsParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        controlsParams.gravity = android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL; // START over LEFT
        controlsParams.setMargins(dp(8), 0, 0, 0);
        mFullscreenControlsLayout.setLayoutParams(controlsParams);

        // Reusable LayoutParams for buttons
        android.widget.LinearLayout.LayoutParams btnLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Close button
        mFullscreenCloseBtn = new android.widget.Button(mActivity);
        mFullscreenCloseBtn.setLayoutParams(btnLp);
        mFullscreenCloseBtn.setBackgroundColor(android.graphics.Color.parseColor("#80000000")); // Semi-transparent black
        mFullscreenCloseBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        mFullscreenCloseBtn.setMinWidth(dp(64));
        mFullscreenCloseBtn.setMinHeight(dp(64));
        
        // Set close icon from drawable
        try {
            android.content.res.Resources resources = mActivity.getResources();
            android.graphics.drawable.Drawable closeIcon = androidx.core.content.ContextCompat.getDrawable(mActivity, 
                resources.getIdentifier("ic_fullscreen_exit", "drawable", mActivity.getPackageName()));
            if (closeIcon != null) {
                closeIcon.setBounds(0, 0, dp(32), dp(32));
                mFullscreenCloseBtn.setCompoundDrawables(closeIcon, null, null, null);
            } else {
                mFullscreenCloseBtn.setText("✕");
                mFullscreenCloseBtn.setTextColor(android.graphics.Color.WHITE);
                mFullscreenCloseBtn.setTextSize(20);
            }
        } catch (Exception e) {
            mFullscreenCloseBtn.setText("✕");
            mFullscreenCloseBtn.setTextColor(android.graphics.Color.WHITE);
            mFullscreenCloseBtn.setTextSize(20);
        }
        
        // Rotate the entire button 90 degrees
        mFullscreenCloseBtn.setRotation(90f);
        
        mFullscreenCloseBtn.setOnClickListener(v -> exitFullscreen());

        // Play/Pause button
        mFullscreenPlayPauseBtn = new android.widget.Button(mActivity);
        mFullscreenPlayPauseBtn.setLayoutParams(btnLp);
        mFullscreenPlayPauseBtn.setBackgroundColor(android.graphics.Color.parseColor("#80000000")); // Semi-transparent black
        mFullscreenPlayPauseBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        mFullscreenPlayPauseBtn.setMinWidth(dp(64));
        mFullscreenPlayPauseBtn.setMinHeight(dp(64));
        
        // Set pause icon from drawable
        try {
            android.content.res.Resources resources = mActivity.getResources();
            android.graphics.drawable.Drawable pauseIcon = androidx.core.content.ContextCompat.getDrawable(mActivity, 
                resources.getIdentifier("ic_pause", "drawable", mActivity.getPackageName()));
            if (pauseIcon != null) {
                pauseIcon.setBounds(0, 0, dp(32), dp(32));
                mFullscreenPlayPauseBtn.setCompoundDrawables(pauseIcon, null, null, null);
            } else {
                mFullscreenPlayPauseBtn.setText("⏸");
                mFullscreenPlayPauseBtn.setTextColor(android.graphics.Color.WHITE);
                mFullscreenPlayPauseBtn.setTextSize(20);
            }
        } catch (Exception e) {
            mFullscreenPlayPauseBtn.setText("⏸");
            mFullscreenPlayPauseBtn.setTextColor(android.graphics.Color.WHITE);
            mFullscreenPlayPauseBtn.setTextSize(20);
        }
        
        // Rotate the entire button 90 degrees
        mFullscreenPlayPauseBtn.setRotation(90f);
        
        mFullscreenPlayPauseBtn.setOnClickListener(v -> toggleFullscreenPlayPause());

        // Mute button
        mFullscreenMuteBtn = new android.widget.Button(mActivity);
        mFullscreenMuteBtn.setLayoutParams(btnLp);
        mFullscreenMuteBtn.setBackgroundColor(android.graphics.Color.parseColor("#80000000")); // Semi-transparent black
        mFullscreenMuteBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        mFullscreenMuteBtn.setMinWidth(dp(64));
        mFullscreenMuteBtn.setMinHeight(dp(64));
        
        // Set mute icon from drawable
        try {
            android.content.res.Resources resources = mActivity.getResources();
            android.graphics.drawable.Drawable muteIcon = androidx.core.content.ContextCompat.getDrawable(mActivity, 
                resources.getIdentifier("ic_mute", "drawable", mActivity.getPackageName()));
            if (muteIcon != null) {
                muteIcon.setBounds(0, 0, dp(32), dp(32));
                mFullscreenMuteBtn.setCompoundDrawables(muteIcon, null, null, null);
            } else {
                mFullscreenMuteBtn.setText("🔇");
                mFullscreenMuteBtn.setTextColor(android.graphics.Color.WHITE);
                mFullscreenMuteBtn.setTextSize(20);
            }
        } catch (Exception e) {
            mFullscreenMuteBtn.setText("🔇");
            mFullscreenMuteBtn.setTextColor(android.graphics.Color.WHITE);
            mFullscreenMuteBtn.setTextSize(20);
        }
        
        // Rotate the entire button 90 degrees
        mFullscreenMuteBtn.setRotation(90f);
        
        mFullscreenMuteBtn.setOnClickListener(v -> toggleFullscreenMute());

        // Snapshot button
        android.widget.Button mFullscreenSnapshotBtn = new android.widget.Button(mActivity);
        mFullscreenSnapshotBtn.setLayoutParams(btnLp);
        mFullscreenSnapshotBtn.setBackgroundColor(android.graphics.Color.parseColor("#80000000")); // Semi-transparent black
        mFullscreenSnapshotBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        mFullscreenSnapshotBtn.setMinWidth(dp(64));
        mFullscreenSnapshotBtn.setMinHeight(dp(64));
        
        // Set snapshot icon from drawable
        try {
            android.content.res.Resources resources = mActivity.getResources();
            android.graphics.drawable.Drawable snapshotIcon = androidx.core.content.ContextCompat.getDrawable(mActivity, 
                resources.getIdentifier("ic_snap", "drawable", mActivity.getPackageName()));
            if (snapshotIcon != null) {
                snapshotIcon.setBounds(0, 0, dp(32), dp(32));
                mFullscreenSnapshotBtn.setCompoundDrawables(snapshotIcon, null, null, null);
            } else {
                mFullscreenSnapshotBtn.setText("📷");
                mFullscreenSnapshotBtn.setTextColor(android.graphics.Color.WHITE);
                mFullscreenSnapshotBtn.setTextSize(20);
            }
        } catch (Exception e) {
            mFullscreenSnapshotBtn.setText("📷");
            mFullscreenSnapshotBtn.setTextColor(android.graphics.Color.WHITE);
            mFullscreenSnapshotBtn.setTextSize(20);
        }
        
        // Rotate the entire button 90 degrees
        mFullscreenSnapshotBtn.setRotation(90f);
        
        mFullscreenSnapshotBtn.setOnClickListener(v -> {
            if (mFullscreenRealPlay != null) {
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
                String filename = "snapshot_fullscreen_" + timestamp + ".png";
                
                java.io.File externalDir = mActivity.getExternalFilesDir(null);
                if (externalDir != null) {
                    java.io.File snapshotsDir = new java.io.File(externalDir, "snapshots");
                    if (!snapshotsDir.exists()) {
                        snapshotsDir.mkdirs();
                    }
                    
                    java.io.File snapshotFile = new java.io.File(snapshotsDir, filename);
                    
                    boolean success = mFullscreenRealPlay.savePngFile(snapshotFile.getAbsolutePath());
                    if (success) {
                        android.util.Log.d("VideoPlayer", "Fullscreen snapshot saved successfully: " + snapshotFile.getAbsolutePath());
                        showSnapshotDialog(snapshotFile, mFullscreenChannel);
                        if (mVideoPlayerListener != null) {
                            mVideoPlayerListener.onVideoSnapshot(mFullscreenVideoView, mFullscreenChannel);
                        }
                    } else {
                        android.util.Log.e("VideoPlayer", "Failed to save fullscreen snapshot");
                        showSnapError("Failed to capture fullscreen snapshot");
                    }
                } else {
                    android.util.Log.e("VideoPlayer", "External files directory not available");
                    showSnapError("Storage not available");
                }
            }
        });

        // Assemble controls with vertical spacing
        mFullscreenControlsLayout.addView(mFullscreenPlayPauseBtn);
        android.view.View spacer2 = new android.view.View(mActivity);
        mFullscreenControlsLayout.addView(spacer2, new android.widget.LinearLayout.LayoutParams(1, dp(24)));

        mFullscreenControlsLayout.addView(mFullscreenMuteBtn);
        android.view.View spacer3 = new android.view.View(mActivity);
        mFullscreenControlsLayout.addView(spacer3, new android.widget.LinearLayout.LayoutParams(1, dp(24)));

        mFullscreenControlsLayout.addView(mFullscreenSnapshotBtn);
        android.view.View spacer1 = new android.view.View(mActivity);
        mFullscreenControlsLayout.addView(spacer1, new android.widget.LinearLayout.LayoutParams(1, dp(24)));

        mFullscreenControlsLayout.addView(mFullscreenCloseBtn);


        // Add to root fullscreen layout (controls after video so they overlay)
        mFullscreenLayout.addView(mFullscreenVideoView);
        mFullscreenLayout.addView(mFullscreenControlsLayout);

        // Make sure controls are actually above the video
        mFullscreenControlsLayout.bringToFront();
        // Strong elevation to win z-order (API 21+)
        mFullscreenControlsLayout.setElevation(1000f);
        mFullscreenLayout.setElevation(999f);

        // Optional: ensure controls receive touch
        mFullscreenControlsLayout.setClickable(true);
        mFullscreenControlsLayout.setFocusable(true);

        android.util.Log.d("VideoPlayer", "Fullscreen layout children: " + mFullscreenLayout.getChildCount());
        android.util.Log.d("VideoPlayer", "Controls children: " + mFullscreenControlsLayout.getChildCount());

        // Attach to window if not already attached elsewhere
        android.view.ViewGroup decor = (android.view.ViewGroup) mActivity.getWindow().getDecorView();
        decor.post(() -> {
            if (mFullscreenLayout.getParent() == null) {
                mActivity.getWindow().addContentView(
                        mFullscreenLayout,
                        new android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                );
            }
            mFullscreenLayout.bringToFront();
            mFullscreenLayout.setElevation(1000f);
        });

        // Keep your existing positioning logic if needed
        mFullscreenLayout.post(this::ensureControlsAtBottom);
        
        // Start auto-hide timer when entering fullscreen
        mControlsVisible = true;
        startAutoHideTimer();
    }


    private void transferToFullscreen(int channelIndex) {
        RealPlay originalRealPlay = mRealPlays.get(channelIndex);
        if (originalRealPlay != null) {
            // Store reference to original RealPlay for fullscreen
            mFullscreenRealPlay = originalRealPlay;
            
            // Change the video view to fullscreen view
            mFullscreenRealPlay.setVideoView(mFullscreenVideoView);
            
            // Update control button states
            updateFullscreenControls();
            
            Log.d("VideoPlayer", "Transferred video stream to fullscreen for channel " + channelIndex);
        }
    }
    
    private void transferFromFullscreen() {
        if (mFullscreenRealPlay != null && mFullscreenChannel >= 0 && mFullscreenChannel < mVideoViews.size()) {
            // Restore the original video view
            VideoView originalVideoView = mVideoViews.get(mFullscreenChannel);
            mFullscreenRealPlay.setVideoView(originalVideoView);
            
            Log.d("VideoPlayer", "Transferred video stream back to original view for channel " + mFullscreenChannel);
        }
        
        mFullscreenRealPlay = null;
    }
    
    private void toggleFullscreenPlayPause() {
        if (mFullscreenRealPlay != null) {
            try {
                Resources resources = mActivity.getResources();
                if (mFullscreenRealPlay.isViewing()) {
                    mFullscreenRealPlay.StopAV();
                    // Set play icon
                    Drawable playIcon = ContextCompat.getDrawable(mActivity, 
                        resources.getIdentifier("ic_play", "drawable", mActivity.getPackageName()));
                    if (playIcon != null) {
                        playIcon.setBounds(0, 0, 60, 60);
                        mFullscreenPlayPauseBtn.setCompoundDrawables(playIcon, null, null, null);
                    } else {
                        mFullscreenPlayPauseBtn.setText("▶");
                    }
                } else {
                    mFullscreenRealPlay.StartAV(false, true);
                    // Set pause icon
                    Drawable pauseIcon = ContextCompat.getDrawable(mActivity, 
                        resources.getIdentifier("ic_pause", "drawable", mActivity.getPackageName()));
                    if (pauseIcon != null) {
                        pauseIcon.setBounds(0, 0, 60, 60);
                        mFullscreenPlayPauseBtn.setCompoundDrawables(pauseIcon, null, null, null);
                    } else {
                        mFullscreenPlayPauseBtn.setText("⏸");
                    }
                }
            } catch (Exception e) {
                // Fallback to text
                if (mFullscreenRealPlay.isViewing()) {
                    mFullscreenRealPlay.StopAV();
                    mFullscreenPlayPauseBtn.setText("▶");
                } else {
                    mFullscreenRealPlay.StartAV(false, true);
                    mFullscreenPlayPauseBtn.setText("⏸");
                }
            }
        }
    }
    
    private void toggleFullscreenMute() {
        if (mFullscreenRealPlay != null) {
            try {
                Resources resources = mActivity.getResources();
                if (mFullscreenRealPlay.isSounding()) {
                    mFullscreenRealPlay.stopSound();
                    // Set mute icon
                    Drawable muteIcon = ContextCompat.getDrawable(mActivity, 
                        resources.getIdentifier("ic_mute", "drawable", mActivity.getPackageName()));
                    if (muteIcon != null) {
                        muteIcon.setBounds(0, 0, 60, 60);
                        mFullscreenMuteBtn.setCompoundDrawables(muteIcon, null, null, null);
                    } else {
                        mFullscreenMuteBtn.setText("🔇");
                    }
                } else {
                    mFullscreenRealPlay.playSound();
                    // Set unmute icon
                    Drawable unmuteIcon = ContextCompat.getDrawable(mActivity, 
                        resources.getIdentifier("ic_unmute", "drawable", mActivity.getPackageName()));
                    if (unmuteIcon != null) {
                        unmuteIcon.setBounds(0, 0, 60, 60);
                        mFullscreenMuteBtn.setCompoundDrawables(unmuteIcon, null, null, null);
                    } else {
                        mFullscreenMuteBtn.setText("🔊");
                    }
                }
            } catch (Exception e) {
                // Fallback to text
                if (mFullscreenRealPlay.isSounding()) {
                    mFullscreenRealPlay.stopSound();
                    mFullscreenMuteBtn.setText("🔇");
                } else {
                    mFullscreenRealPlay.playSound();
                    mFullscreenMuteBtn.setText("🔊");
                }
            }
        }
    }
    
    private void updateFullscreenControls() {
        if (mFullscreenRealPlay != null) {
            try {
                android.content.res.Resources resources = mActivity.getResources();
                
                // Update play/pause button
                if (mFullscreenRealPlay.isViewing()) {
                    android.graphics.drawable.Drawable pauseIcon = androidx.core.content.ContextCompat.getDrawable(mActivity, 
                        resources.getIdentifier("ic_pause", "drawable", mActivity.getPackageName()));
                    if (pauseIcon != null) {
                        pauseIcon.setBounds(0, 0, dp(32), dp(32));
                        mFullscreenPlayPauseBtn.setCompoundDrawables(pauseIcon, null, null, null);
                        mFullscreenPlayPauseBtn.setText(""); // Clear text when using icon
                    } else {
                        mFullscreenPlayPauseBtn.setText("⏸");
                        mFullscreenPlayPauseBtn.setTextColor(android.graphics.Color.WHITE);
                        mFullscreenPlayPauseBtn.setTextSize(20);
                    }
                } else {
                    android.graphics.drawable.Drawable playIcon = androidx.core.content.ContextCompat.getDrawable(mActivity, 
                        resources.getIdentifier("ic_play", "drawable", mActivity.getPackageName()));
                    if (playIcon != null) {
                        playIcon.setBounds(0, 0, dp(32), dp(32));
                        mFullscreenPlayPauseBtn.setCompoundDrawables(playIcon, null, null, null);
                        mFullscreenPlayPauseBtn.setText(""); // Clear text when using icon
                    } else {
                        mFullscreenPlayPauseBtn.setText("▶");
                        mFullscreenPlayPauseBtn.setTextColor(android.graphics.Color.WHITE);
                        mFullscreenPlayPauseBtn.setTextSize(20);
                    }
                }
                
                // Update mute button
                if (mFullscreenRealPlay.isSounding()) {
                    android.graphics.drawable.Drawable unmuteIcon = androidx.core.content.ContextCompat.getDrawable(mActivity, 
                        resources.getIdentifier("ic_unmute", "drawable", mActivity.getPackageName()));
                    if (unmuteIcon != null) {
                        unmuteIcon.setBounds(0, 0, dp(32), dp(32));
                        mFullscreenMuteBtn.setCompoundDrawables(unmuteIcon, null, null, null);
                        mFullscreenMuteBtn.setText(""); // Clear text when using icon
                    } else {
                        mFullscreenMuteBtn.setText("🔊");
                        mFullscreenMuteBtn.setTextColor(android.graphics.Color.WHITE);
                        mFullscreenMuteBtn.setTextSize(20);
                    }
                } else {
                    android.graphics.drawable.Drawable muteIcon = androidx.core.content.ContextCompat.getDrawable(mActivity, 
                        resources.getIdentifier("ic_mute", "drawable", mActivity.getPackageName()));
                    if (muteIcon != null) {
                        muteIcon.setBounds(0, 0, dp(32), dp(32));
                        mFullscreenMuteBtn.setCompoundDrawables(muteIcon, null, null, null);
                        mFullscreenMuteBtn.setText(""); // Clear text when using icon
                    } else {
                        mFullscreenMuteBtn.setText("🔇");
                        mFullscreenMuteBtn.setTextColor(android.graphics.Color.WHITE);
                        mFullscreenMuteBtn.setTextSize(20);
                    }
                }
            } catch (Exception e) {
                // Fallback to text
                if (mFullscreenRealPlay.isViewing()) {
                    mFullscreenPlayPauseBtn.setText("⏸");
                    mFullscreenPlayPauseBtn.setTextColor(android.graphics.Color.WHITE);
                    mFullscreenPlayPauseBtn.setTextSize(20);
                } else {
                    mFullscreenPlayPauseBtn.setText("▶");
                    mFullscreenPlayPauseBtn.setTextColor(android.graphics.Color.WHITE);
                    mFullscreenPlayPauseBtn.setTextSize(20);
                }
                
                if (mFullscreenRealPlay.isSounding()) {
                    mFullscreenMuteBtn.setText("🔊");
                    mFullscreenMuteBtn.setTextColor(android.graphics.Color.WHITE);
                    mFullscreenMuteBtn.setTextSize(20);
                } else {
                    mFullscreenMuteBtn.setText("🔇");
                    mFullscreenMuteBtn.setTextColor(android.graphics.Color.WHITE);
                    mFullscreenMuteBtn.setTextSize(20);
                }
            }
        }
    }
    
    private void hideSystemUI() {
        mActivity.getWindow().getDecorView().setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    
    private void showSystemUI() {
        // Clear all system UI flags to restore normal behavior
        mActivity.getWindow().getDecorView().setSystemUiVisibility(0);
        
        // Clear window flags that might affect layout
        mActivity.getWindow().clearFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
            | android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            | android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        
        // Ensure the activity's content view is properly positioned
        mActivity.getWindow().getDecorView().requestLayout();
        
        // Force the activity to recalculate its layout
        mActivity.getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mActivity.getWindow().getDecorView().requestLayout();
            }
        });
    }
    
    public boolean isFullscreen() {
        return mIsFullscreen;
    }
    
    public int getFullscreenChannel() {
        return mFullscreenChannel;
    }
    
    // Handle back button press to exit fullscreen
    public boolean onBackPressed() {
        if (mIsFullscreen) {
            exitFullscreen(); // This will return to grid view
            return true; // Consumed the back press
        }
        return false; // Let the system handle it
    }
    
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        Log.d("VideoPlayer", "Configuration changed - orientation: " + newConfig.orientation);
        
        if (mIsFullscreen && mFullscreenLayout != null) {
            // Ensure fullscreen layout is properly displayed after orientation change
            mFullscreenLayout.post(new Runnable() {
                @Override
                public void run() {
                    mFullscreenLayout.requestLayout();
                    mFullscreenLayout.invalidate();
                    
                    // Ensure video view is properly scaled
                    if (mFullscreenVideoView != null) {
                        mFullscreenVideoView.requestLayout();
                        mFullscreenVideoView.invalidate();
                    }
                }
            });
        }
    }
    
    private void showSnapshotDialog(java.io.File snapshotFile, int channelIndex) {
        try {
            // Create a dialog to show the snapshot
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mActivity);
            builder.setTitle("Snapshot Captured - Channel " + (channelIndex + 1));
            
            // Create layout for the dialog
            LinearLayout dialogLayout = new LinearLayout(mActivity);
            dialogLayout.setOrientation(LinearLayout.VERTICAL);
            dialogLayout.setPadding(20, 20, 20, 20);
            
            // Add image view to show the snapshot
            android.widget.ImageView imageView = new android.widget.ImageView(mActivity);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                400)); // Fixed height for image
            
            // Load and display the image
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(snapshotFile.getAbsolutePath());
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            
            dialogLayout.addView(imageView);
            
            // Add file info
            android.widget.TextView infoText = new android.widget.TextView(mActivity);
            infoText.setText("Saved to: " + snapshotFile.getName() + "\nPath: " + snapshotFile.getParent());
            infoText.setTextSize(12);
            infoText.setTextColor(Color.GRAY);
            infoText.setPadding(0, 10, 0, 0);
            dialogLayout.addView(infoText);
            
            builder.setView(dialogLayout);
            
            // Add buttons
            builder.setPositiveButton("Share", (dialog, which) -> {
                shareSnapshot(snapshotFile);
            });
            
            builder.setNeutralButton("Open Folder", (dialog, which) -> {
                openSnapshotsFolder();
            });
            
            builder.setNegativeButton("Close", (dialog, which) -> {
                dialog.dismiss();
            });
            
            builder.show();
            
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error showing snapshot dialog: " + e.getMessage());
            showSnapError("Error displaying snapshot");
        }
    }
    
    private void showSnapError(String message) {
        try {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mActivity);
            builder.setTitle("Snapshot Error");
            builder.setMessage(message);
            builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
            builder.show();
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error showing error dialog: " + e.getMessage());
        }
    }
    
    private void shareSnapshot(java.io.File snapshotFile) {
        try {
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            
            // Get URI for the file
            android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                mActivity, 
                mActivity.getPackageName() + ".fileprovider", 
                snapshotFile
            );
            
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            mActivity.startActivity(android.content.Intent.createChooser(shareIntent, "Share Snapshot"));
            
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error sharing snapshot: " + e.getMessage());
            showSnapError("Error sharing snapshot");
        }
    }
    
    private void openSnapshotsFolder() {
        try {
            java.io.File snapshotsDir = new java.io.File(mActivity.getExternalFilesDir(null), "snapshots");
            if (snapshotsDir.exists()) {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(android.net.Uri.fromFile(snapshotsDir), "resource/folder");
                mActivity.startActivity(intent);
            } else {
                showSnapError("Snapshots folder not found");
            }
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error opening snapshots folder: " + e.getMessage());
            showSnapError("Error opening folder");
        }
    }
    
    private void rotateVideoToLandscape() {
        if (mFullscreenVideoView != null) {
            // Rotate the video view 90 degrees to landscape
            mFullscreenVideoView.setRotation(90f);
            
            // Adjust the layout parameters to accommodate the rotation
            android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) mFullscreenVideoView.getLayoutParams();
            if (params != null) {
                // Swap width and height for landscape
                int screenWidth = mActivity.getResources().getDisplayMetrics().widthPixels;
                int screenHeight = mActivity.getResources().getDisplayMetrics().heightPixels;
                
                // Set dimensions to fill the screen in landscape
                params.width = screenHeight;  // Use height as width for rotated view
                params.height = screenWidth;  // Use width as height for rotated view
                params.gravity = android.view.Gravity.CENTER;
                
                mFullscreenVideoView.setLayoutParams(params);
                mFullscreenVideoView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            }
            
            // Ensure controls stay at bottom after rotation
            if (mFullscreenLayout != null) {
                mFullscreenLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        // Find the controls layout and ensure it's at LEFT side of video content
                        for (int i = 0; i < mFullscreenLayout.getChildCount(); i++) {
                            View child = mFullscreenLayout.getChildAt(i);
                            if (child instanceof LinearLayout) {
                                LinearLayout controlsLayout = (LinearLayout) child;
                                android.widget.FrameLayout.LayoutParams controlsParams = 
                                    (android.widget.FrameLayout.LayoutParams) controlsLayout.getLayoutParams();
                                if (controlsParams != null) {
                                    controlsParams.gravity = android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL;
                                    controlsParams.setMargins(20, 0, 0, 0); // Left side positioning
                                    controlsLayout.setLayoutParams(controlsParams);
                                    controlsLayout.bringToFront();
                                }
                            }
                        }
                    }
                });
            }
            
            Log.d("VideoPlayer", "Video rotated to landscape");
        }
    }
    
    private void restoreVideoToPortrait() {
        if (mFullscreenVideoView != null) {
            // Restore the video view to 0 degrees (portrait)
            mFullscreenVideoView.setRotation(0f);
            
            // Restore the layout parameters to original
            android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) mFullscreenVideoView.getLayoutParams();
            if (params != null) {
                params.width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT;
                params.height = android.widget.FrameLayout.LayoutParams.MATCH_PARENT;
                params.gravity = android.view.Gravity.FILL;
                
                mFullscreenVideoView.setLayoutParams(params);
                mFullscreenVideoView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            }
            
            Log.d("VideoPlayer", "Video restored to portrait");
        }
    }
    
    private void ensureControlsAtBottom() {
        if (mFullscreenLayout != null) {
            // Find the controls layout and ensure it's at LEFT side of video content
            for (int i = 0; i < mFullscreenLayout.getChildCount(); i++) {
                View child = mFullscreenLayout.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout controlsLayout = (LinearLayout) child;
                    android.widget.FrameLayout.LayoutParams controlsParams = 
                        (android.widget.FrameLayout.LayoutParams) controlsLayout.getLayoutParams();
                    if (controlsParams != null) {
                        controlsParams.gravity = android.view.Gravity.LEFT | android.view.Gravity.CENTER_VERTICAL;
                        controlsParams.setMargins(20, 0, 0, 0); // Left side positioning
                        controlsLayout.setLayoutParams(controlsParams);
                        controlsLayout.bringToFront();
                        controlsLayout.requestLayout();
                        controlsLayout.invalidate();
                    }
                }
            }
            Log.d("VideoPlayer", "Controls positioned at left side of video content");
        }
    }
    
    public String getServer() {
        return mServer;
    }
    
    public String getDeviceId() {
        return mDevIdno;
    }
    
    public int getChannelCount() {
        return mChannelCount;
    }
    
    public VideoView getVideoView(int index) {
        if (index >= 0 && index < mVideoViews.size()) {
            return mVideoViews.get(index);
        }
        return null;
    }
    
    public RealPlay getRealPlay(int index) {
        if (index >= 0 && index < mRealPlays.size()) {
            return mRealPlays.get(index);
        }
        return null;
    }
    
    public interface VideoPlayerListener {
        void onVideoClick(VideoView view, int index);
        void onVideoDoubleClick(VideoView view, int index);
        void onVideoMoveLeft(VideoView view, int index);
        void onVideoMoveRight(VideoView view, int index);
        void onVideoMoveUp(VideoView view, int index);
        void onVideoMoveDown(VideoView view, int index);
        void onVideoMoveStop(VideoView view, int index);
        void onVideoFullscreen(VideoView view, int index);
        void onVideoRecordStart(VideoView view, int index);
        void onVideoRecordStop(VideoView view, int index);
        void onVideoSnapshot(VideoView view, int index);
        void onVideoAudioStart(VideoView view, int index);
        void onVideoAudioStop(VideoView view, int index);
    }
    
    private void toggleControlsVisibility() {
        if (mFullscreenControlsLayout == null) return;
        
        // Cancel any pending hide operation
        if (mHideControlsRunnable != null) {
            mControlsHandler.removeCallbacks(mHideControlsRunnable);
        }
        
        if (mControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }
    
    private void showControls() {
        if (mFullscreenControlsLayout == null || mControlsVisible) return;
        
        mControlsVisible = true;
        mFullscreenControlsLayout.setVisibility(android.view.View.VISIBLE);
        
        // Animate fade in
        mFullscreenControlsLayout.animate()
            .alpha(1.0f)
            .setDuration(300)
            .setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    // Start auto-hide timer after showing
                    startAutoHideTimer();
                }
            });
    }
    
    private void hideControls() {
        if (mFullscreenControlsLayout == null || !mControlsVisible) return;
        
        mControlsVisible = false;
        
        // Animate fade out
        mFullscreenControlsLayout.animate()
            .alpha(0.0f)
            .setDuration(300)
            .setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    mFullscreenControlsLayout.setVisibility(android.view.View.GONE);
                }
            });
    }
    
    private void startAutoHideTimer() {
        // Cancel any existing timer
        if (mHideControlsRunnable != null) {
            mControlsHandler.removeCallbacks(mHideControlsRunnable);
        }
        
        // Auto-hide after 5 seconds
        mHideControlsRunnable = new Runnable() {
            @Override
            public void run() {
                if (mControlsVisible) {
                    hideControls();
                }
            }
        };
        mControlsHandler.postDelayed(mHideControlsRunnable, 5000);
    }
    
    private void stopAutoHideTimer() {
        if (mHideControlsRunnable != null) {
            mControlsHandler.removeCallbacks(mHideControlsRunnable);
            mHideControlsRunnable = null;
        }
    }
}
