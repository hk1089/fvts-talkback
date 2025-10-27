package com.hk1089.mettax.video;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import android.content.res.Resources;
import android.os.Environment;
import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.babelstar.gviewer.NetClient;
import com.hk1089.mettax.R;
import com.hk1089.mettax.utils.AspectRatioRelativeLayout;
import com.hk1089.mettax.utils.SquareRelativeLayout;

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
    
    // Grid controls animation
    private boolean[] mGridControlsVisible; // Track visibility for each channel's controls
    private android.os.Handler mGridControlsHandler = new android.os.Handler();
    private Runnable[] mGridHideControlsRunnable; // Auto-hide runnables for each channel
    private List<AppCompatImageButton>[] mGridControlButtons; // Store references to control buttons for each channel
    
    // Loading and placeholder components
    private android.widget.ProgressBar[] mLoadingIndicators; // Loading indicators for each channel
    private android.widget.ImageView[] mPausePlaceholders; // Pause placeholders for each channel
    private boolean[] mIsLoading; // Track loading state for each channel

    // Fullscreen components
    private boolean mIsFullscreen = false;
    private int mFullscreenChannel = -1;
    private android.widget.FrameLayout mFullscreenLayout;
    private VideoView mFullscreenVideoView;
    private RealPlay mFullscreenRealPlay;
    private AppCompatImageButton mFullscreenCloseBtn;
    private AppCompatImageButton mFullscreenMuteBtn;
    private AppCompatImageButton mFullscreenPlayPauseBtn;
    private AppCompatImageButton mFullscreenSnapshotBtn;
    private android.widget.LinearLayout mFullscreenControlsLayout;
    private boolean mControlsVisible = true;
    private android.os.Handler mControlsHandler = new android.os.Handler();
    private Runnable mHideControlsRunnable;
    
    // Fullscreen loading and placeholder components
    private android.widget.ProgressBar mFullscreenLoadingIndicator;
    private android.widget.ImageView mFullscreenPausePlaceholder;

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
        
        // Initialize grid controls visibility and auto-hide runnables
        mGridControlsVisible = new boolean[channelCount];
        mGridHideControlsRunnable = new Runnable[channelCount];
        mGridControlButtons = new List[channelCount];
        
        // Initialize loading and placeholder components
        mLoadingIndicators = new android.widget.ProgressBar[channelCount];
        mPausePlaceholders = new android.widget.ImageView[channelCount];
        mIsLoading = new boolean[channelCount];

        // Initialize all channels as muted and controls visible
        for (int i = 0; i < channelCount; i++) {
            mIsMuted[i] = true; // All channels start muted
            mGridControlsVisible[i] = true; // Start with controls visible
            mGridHideControlsRunnable[i] = null;
            mGridControlButtons[i] = new ArrayList<>();
            mIsLoading[i] = true; // All channels start in loading state
        }

        initializeComponents();
        initializeNetClient();
    }

    private void initializeComponents() {
        if (mVideoViews != null) mVideoViews.clear();
        if (mRealPlays != null) mRealPlays.clear();

        mMainLayout = new LinearLayout(mActivity);
        mMainLayout.setOrientation(LinearLayout.VERTICAL);
        mMainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // Choose how many columns per row (make this dynamic if you want)
        final int columns = 2;

        LinearLayout currentRow = null;
        int itemsInRow = 0;

        for (int i = 0; i < mChannelCount; i++) {
            // Start a new row when needed
            if (currentRow == null || itemsInRow == columns) {
                currentRow = new LinearLayout(mActivity);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setPadding(dp(0), dp(1), dp(0), dp(1));
                currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)); // row wraps child height
                mMainLayout.addView(currentRow);
                itemsInRow = 0;
            }

            // ---- Aspect container: height = width / aspect ----
            AspectRatioRelativeLayout cell = new AspectRatioRelativeLayout(mActivity);
            // Equal widths in the row; height comes from aspect container
            LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            cellParams.setMargins(dp(1), dp(0), dp(1), dp(0));
            cell.setLayoutParams(cellParams);

            // Default to 16:9 so it looks right before we know the real size
            cell.setAspect(5, 4);

            // VideoView fills the cell
            final int channelIndex = i;
            VideoView videoView = new VideoView(mActivity, channelIndex);
            videoView.setId(1000 + channelIndex);
            RelativeLayout.LayoutParams videoLp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            videoView.setLayoutParams(videoLp);

            videoView.setOnTouchListener((v, e) -> {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    toggleGridControlsVisibility(channelIndex);
                }
                return true;
            });

            cell.addView(videoView);
            
            // Create loading indicator
            createLoadingIndicator(cell, channelIndex);
            
            // Create pause placeholder
            createPausePlaceholder(cell, channelIndex);
            
            createControlButtons(cell, channelIndex);

            mVideoViews.add(videoView);
            currentRow.addView(cell);
            itemsInRow++;

            // Hook up the player and update aspect when the actual video size is known
            RealPlay realPlay = new RealPlay(mActivity);
            realPlay.setVideoView(videoView);
            realPlay.setPlayerListener(new RealPlay.PlayListener() {
                @Override public void onBeginPlay() { /* optional */ }
                @Override public void onPtzCtrl(VideoView view, int index) { }
                @Override public void onClick(VideoView view, int index) {
                    if (mVideoPlayerListener != null) mVideoPlayerListener.onVideoClick(view, index);
                }
                @Override public void onDbClick(VideoView view, int index) {
                    if (mVideoPlayerListener != null) mVideoPlayerListener.onVideoDoubleClick(view, index);
                }
                @Override public void onMoveLeft(VideoView view, int index) {
                    if (mVideoPlayerListener != null) mVideoPlayerListener.onVideoMoveLeft(view, index);
                }
                @Override public void onMoveRight(VideoView view, int index) {
                    if (mVideoPlayerListener != null) mVideoPlayerListener.onVideoMoveRight(view, index);
                }
            });

            // If your RealPlay exposes the decoded size, call cell.setAspect(w,h) when known.
            // Example hooks (use what your SDK provides):
            // realPlay.setOnVideoSizeChangedListener((w, h) -> cell.setAspect(w, h));
            //
            // If using standard VideoView + MediaPlayer under the hood:
            // videoView.setOnPreparedListener(mp -> mp.setOnVideoSizeChangedListener((m, w, h) -> cell.setAspect(w, h)));

            mRealPlays.add(realPlay);
        }

        // Attach mMainLayout to your view hierarchy if not already done elsewhere.
        // e.g., setContentView(mMainLayout);
    }


    // Create a uniformly styled ImageButton (CENTER_INSIDE, white tint, ripple)
    private AppCompatImageButton makeIconBtn(@DrawableRes int iconRes) {
        return makeIconBtn(iconRes, dp(14)); // Default size 14dp
    }

    // Create a uniformly styled ImageButton with custom size
    private AppCompatImageButton makeIconBtn(@DrawableRes int iconRes, int sizeDp) {
        AppCompatImageButton b = new AppCompatImageButton(mActivity);
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setImageDrawable(AppCompatResources.getDrawable(mActivity, iconRes));
        ImageViewCompat.setImageTintList(b, ColorStateList.valueOf(Color.WHITE));
        b.setPadding(dp(4), dp(8), dp(4), dp(8));
        b.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        b.setAdjustViewBounds(true);
        b.setFocusable(true);
        b.setClickable(true);
        b.setMaxWidth(sizeDp);
        b.setMaxHeight(sizeDp);
        return b;
    }

    private void createLoadingIndicator(RelativeLayout container, int videoIndex) {
        // Create loading indicator
        android.widget.ProgressBar loadingIndicator = new android.widget.ProgressBar(mActivity);
        loadingIndicator.setId(5000 + videoIndex); // Unique ID for loading indicator
        
        RelativeLayout.LayoutParams loadingParams = new RelativeLayout.LayoutParams(
                dp(30), dp(30));
        loadingParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        loadingIndicator.setLayoutParams(loadingParams);
        
        // Set custom drawable for loading animation
        loadingIndicator.setIndeterminateDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_loading));
        
        // Initially visible (video is loading)
        loadingIndicator.setVisibility(android.view.View.VISIBLE);
        
        container.addView(loadingIndicator);
        mLoadingIndicators[videoIndex] = loadingIndicator;
    }

    private void createPausePlaceholder(RelativeLayout container, int videoIndex) {
        // Create pause placeholder
        android.widget.ImageView pausePlaceholder = new android.widget.ImageView(mActivity);
        pausePlaceholder.setId(7000 + videoIndex); // Unique ID for pause placeholder
        
        RelativeLayout.LayoutParams placeholderParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        placeholderParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        pausePlaceholder.setLayoutParams(placeholderParams);
        
        // Set placeholder image
        pausePlaceholder.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.placeholder));
        pausePlaceholder.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        
        // Initially hidden (video is playing)
        pausePlaceholder.setVisibility(android.view.View.GONE);
        
        container.addView(pausePlaceholder);
        mPausePlaceholders[videoIndex] = pausePlaceholder;
    }

    private void createFullscreenLoadingIndicator() {
        // Create fullscreen loading indicator
        mFullscreenLoadingIndicator = new android.widget.ProgressBar(mActivity);
        
        android.widget.FrameLayout.LayoutParams loadingParams = new android.widget.FrameLayout.LayoutParams(
                dp(60), dp(60));
        loadingParams.gravity = android.view.Gravity.CENTER;
        mFullscreenLoadingIndicator.setLayoutParams(loadingParams);
        
        // Set custom drawable for loading animation
        mFullscreenLoadingIndicator.setIndeterminateDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_loading));
        
        // Initially visible (video is loading)
        mFullscreenLoadingIndicator.setVisibility(android.view.View.VISIBLE);
        
        mFullscreenLayout.addView(mFullscreenLoadingIndicator);
    }

    private void createFullscreenPausePlaceholder() {
        // Create fullscreen pause placeholder
        mFullscreenPausePlaceholder = new android.widget.ImageView(mActivity);
        
        // Use FrameLayout layout params to cover entire fullscreen area
        android.widget.FrameLayout.LayoutParams placeholderParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
        placeholderParams.gravity = android.view.Gravity.CENTER;
        mFullscreenPausePlaceholder.setLayoutParams(placeholderParams);
        
        // Set placeholder image
        mFullscreenPausePlaceholder.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.placeholder));
        mFullscreenPausePlaceholder.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        
        // Rotate placeholder for fullscreen (landscape orientation)
        mFullscreenPausePlaceholder.setRotation(90f);
        
        // Set background to black to cover any letterboxed areas
        mFullscreenPausePlaceholder.setBackgroundColor(android.graphics.Color.BLACK);
        
        // Make placeholder non-clickable so touch events pass through to video view
        mFullscreenPausePlaceholder.setClickable(false);
        mFullscreenPausePlaceholder.setFocusable(false);
        
        // Add touch listener to forward touch events to video view for controls toggle
        mFullscreenPausePlaceholder.setOnTouchListener(new android.view.View.OnTouchListener() {
            @Override
            public boolean onTouch(android.view.View v, android.view.MotionEvent event) {
                // Forward touch events to the video view to trigger controls toggle
                if (mFullscreenVideoView != null) {
                    return mFullscreenVideoView.dispatchTouchEvent(event);
                }
                return false;
            }
        });
        
        // Set lower elevation than controls so controls appear above placeholder
        mFullscreenPausePlaceholder.setElevation(500f);
        
        // Initially hidden (video is playing)
        mFullscreenPausePlaceholder.setVisibility(android.view.View.GONE);
        
        // Add to fullscreen layout (same level as video view)
        mFullscreenLayout.addView(mFullscreenPausePlaceholder);
    }

    private void createControlButtons(RelativeLayout container, int videoIndex) {
        // ---- Bottom control bar (equal spacing) ----
        LinearLayout bottomBar = new LinearLayout(mActivity);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setPadding(dp(0), dp(0), dp(0), dp(0));
        bottomBar.setBackgroundColor(Color.parseColor("#80000000")); // subtle overlay

        RelativeLayout.LayoutParams barParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                dp(32) // tweak to taste
        );
        barParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        bottomBar.setLayoutParams(barParams);

        // Each child fills 1/4 width
        LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);

        // --- Play / Pause ---
        boolean isPlaying = true;
        RealPlay rpInit = (videoIndex < mRealPlays.size()) ? mRealPlays.get(videoIndex) : null;
        if (rpInit != null) isPlaying = rpInit.isViewing();

        final AppCompatImageButton playPauseBtn = makeIconBtn(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        playPauseBtn.setId(2000 + videoIndex); // Set ID for play/pause button
        playPauseBtn.setLayoutParams(new LinearLayout.LayoutParams(slot));

        playPauseBtn.setOnClickListener(v -> {
            RealPlay rp = mRealPlays.get(videoIndex);
            if (rp == null) return;
            if (rp.isViewing()) {
                rp.StopAV();
                playPauseBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_play));
                // Show pause placeholder, hide loading indicator
                updateLoadingAndPlaceholder(videoIndex, false, true);
            } else {
                rp.StartAV(false, true);
                playPauseBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_pause));
                // Show loading indicator while starting
                updateLoadingAndPlaceholder(videoIndex, true, false);
                
                // Hide loading indicator after a short delay (simulate loading time)
                mGridControlsHandler.postDelayed(() -> {
                    if (rp.isViewing()) {
                        updateLoadingAndPlaceholder(videoIndex, false, false);
                    }
                }, 1000);
            }
            if (mIsFullscreen && mFullscreenChannel == videoIndex) updateFullscreenControls();
        });

        // --- Mute / Unmute ---
        final AppCompatImageButton muteBtn = makeIconBtn(mIsMuted[videoIndex] ? R.drawable.ic_mute : R.drawable.ic_unmute);
        muteBtn.setId(3000 + videoIndex); // Set ID for mute button
        muteBtn.setLayoutParams(new LinearLayout.LayoutParams(slot));

        muteBtn.setOnClickListener(v -> {
            RealPlay rp = mRealPlays.get(videoIndex);
            if (rp == null) return;

            if (mIsMuted[videoIndex]) {
                muteAllOtherChannels(videoIndex);
                mIsMuted[videoIndex] = false;
                rp.playSound();
                muteBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_unmute));
                if (mVideoPlayerListener != null) mVideoPlayerListener.onVideoAudioStart(mVideoViews.get(videoIndex), videoIndex);
            } else {
                mIsMuted[videoIndex] = true;
                rp.stopSound();
                muteBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_mute));
                if (mVideoPlayerListener != null) mVideoPlayerListener.onVideoAudioStop(mVideoViews.get(videoIndex), videoIndex);
            }
            if (mIsFullscreen && mFullscreenChannel == videoIndex) updateFullscreenControls();
        });

        // --- Snapshot ---
        final AppCompatImageButton snapshotBtn = makeIconBtn(R.drawable.ic_snap);
        snapshotBtn.setId(6000 + videoIndex); // Set ID for snapshot button
        snapshotBtn.setLayoutParams(new LinearLayout.LayoutParams(slot));

        snapshotBtn.setOnClickListener(v -> {
            RealPlay rp = mRealPlays.get(videoIndex);
            if (rp == null) return;
            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
            String filename = "snapshot_ch" + (videoIndex + 1) + "_" + ts + ".png";
            java.io.File ext = mActivity.getExternalFilesDir(null);
            if (ext != null) {
                java.io.File dir = new java.io.File(ext, "snapshots");
                if (!dir.exists()) dir.mkdirs();
                java.io.File f = new java.io.File(dir, filename);
                boolean ok = rp.savePngFile(f.getAbsolutePath());
                if (ok) {
                    android.util.Log.d("VideoPlayer", "Snapshot: " + f.getAbsolutePath());
                    showSnapshotDialog(f, videoIndex);
                    if (mVideoPlayerListener != null) mVideoPlayerListener.onVideoSnapshot(mVideoViews.get(videoIndex), videoIndex);
                } else {
                    showSnapError("Failed to capture snapshot");
                }
            } else {
                showSnapError("Storage not available");
            }
        });

        // --- Fullscreen ---
        final AppCompatImageButton fullscreenBtn = makeIconBtn(R.drawable.ic_fullscreen);
        fullscreenBtn.setId(4000 + videoIndex); // Set ID for fullscreen button
        fullscreenBtn.setLayoutParams(new LinearLayout.LayoutParams(slot));
        fullscreenBtn.setOnClickListener(v -> enterFullscreen(videoIndex));

        // If you store for animations, accept View not Button
        mGridControlButtons[videoIndex].add(playPauseBtn);
        mGridControlButtons[videoIndex].add(muteBtn);
        mGridControlButtons[videoIndex].add(snapshotBtn);
        mGridControlButtons[videoIndex].add(fullscreenBtn);

        // Add to bar and attach
        bottomBar.addView(playPauseBtn);
        bottomBar.addView(muteBtn);
        bottomBar.addView(snapshotBtn);
        bottomBar.addView(fullscreenBtn);
        container.addView(bottomBar);

        // Auto-hide as before
        startGridAutoHideTimer(videoIndex);
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

        // Show loading indicators for all channels initially
        for (int i = 0; i < mChannelCount; i++) {
            updateLoadingAndPlaceholder(i, true, false);
        }

        for (int i = 0; i < mChannelCount; i++) {
            RealPlay realPlay = mRealPlays.get(i);
            realPlay.setViewInfo(mDevIdno, mDevIdno, i, "CH" + (i+1), 0);
            realPlay.StartAV(false, true);
        }

        mIsPlaying = true;
        Log.d("VideoPlayer", "Video started successfully");

        // Don't update button states immediately - let loading indicators show first
        // updateButtonStates();
        
        // Hide loading indicators after a delay (simulate loading time)
        mGridControlsHandler.postDelayed(() -> {
            for (int i = 0; i < mChannelCount; i++) {
                RealPlay realPlay = mRealPlays.get(i);
                if (realPlay != null && realPlay.isViewing()) {
                    updateLoadingAndPlaceholder(i, false, false);
                }
            }
            // Now update button states after loading is complete
            updateButtonStates();
        }, 2000);
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
                // Find the play/pause button for this video using AppCompatImageButton approach
                AppCompatImageButton playPauseBtn = null;
                for (AppCompatImageButton button : mGridControlButtons[i]) {
                    if (button.getId() == 2000 + i) { // Play/pause button ID
                        playPauseBtn = button;
                        break;
                    }
                }
                
                if (playPauseBtn != null) {
                    if (realPlay.isViewing()) {
                        // Video is playing, show pause icon
                        playPauseBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_pause));
                        // Hide loading indicator and pause placeholder
                        updateLoadingAndPlaceholder(i, false, false);
                    } else {
                        // Video is not playing, show play icon
                        playPauseBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_play));
                        // Show pause placeholder, hide loading indicator
                        updateLoadingAndPlaceholder(i, false, true);
                    }
                }

                // Find the mute/unmute button for this video using AppCompatImageButton approach
                AppCompatImageButton muteBtn = null;
                for (AppCompatImageButton button : mGridControlButtons[i]) {
                    if (button.getId() == 3000 + i) { // Mute button ID
                        muteBtn = button;
                        break;
                    }
                }
                
                if (muteBtn != null) {
                    if (mIsMuted[i]) {
                        // Muted state, show mute icon
                        muteBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_mute));
                    } else {
                        // Unmuted state, show unmute icon
                        muteBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_unmute));
                    }
                }
            }
        }
    }

    private void updateLoadingAndPlaceholder(int channelIndex, boolean isLoading, boolean isPaused) {
        if (channelIndex < 0 || channelIndex >= mChannelCount) return;
        
        // Update loading indicator
        if (mLoadingIndicators[channelIndex] != null) {
            mLoadingIndicators[channelIndex].setVisibility(
                isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        
        // Update pause placeholder
        if (mPausePlaceholders[channelIndex] != null) {
            mPausePlaceholders[channelIndex].setVisibility(
                isPaused ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        
        // Update loading state
        mIsLoading[channelIndex] = isLoading;
        
        Log.d("VideoPlayer", "Channel " + channelIndex + " - Loading: " + isLoading + ", Paused: " + isPaused);
    }

    private void updateFullscreenLoadingAndPlaceholder(boolean isLoading, boolean isPaused) {
        // Update fullscreen loading indicator
        if (mFullscreenLoadingIndicator != null) {
            mFullscreenLoadingIndicator.setVisibility(
                isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        
        // Update fullscreen pause placeholder
        if (mFullscreenPausePlaceholder != null) {
            mFullscreenPausePlaceholder.setVisibility(
                isPaused ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        
        Log.d("VideoPlayer", "Fullscreen - Loading: " + isLoading + ", Paused: " + isPaused);
    }

    // Audio control methods
    public void playSound(int channelIndex) {
        if (channelIndex >= 0 && channelIndex < mRealPlays.size()) {
            RealPlay realPlay = mRealPlays.get(channelIndex);
            if (realPlay != null) {
                realPlay.playSound();
                Log.d("VideoPlayer", "Started audio for channel " + channelIndex);
                // Update button state
                updateButtonStates();
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
                updateButtonStates();
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

                // Update the button icon using AppCompatImageButton approach
                AppCompatImageButton muteBtn = null;
                for (AppCompatImageButton button : mGridControlButtons[i]) {
                    if (button.getId() == 3000 + i) { // Mute button ID
                        muteBtn = button;
                        break;
                    }
                }
                
                if (muteBtn != null) {
                    muteBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_mute));
                }

                Log.d("VideoPlayer", "Auto-muted channel " + i + " (switching to channel " + excludeChannel + ")");
                if (mVideoPlayerListener != null) {
                    mVideoPlayerListener.onVideoAudioStop(mVideoViews.get(i), i);
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

                // Prepare grid view for smooth transition
                if (mMainLayout != null) {
                    mMainLayout.animate().alpha(0.0f).setDuration(200).start();
                }
                
                // Animate fullscreen entry
                animateFullscreenEntry();

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

        // Ensure grid view is visible and ready before starting exit animation
        if (mMainLayout != null) {
            mMainLayout.setVisibility(android.view.View.VISIBLE);
            mMainLayout.setAlpha(0.0f);
            mMainLayout.animate().alpha(1.0f).setDuration(200).start();
        }
        
        // Animate fullscreen exit with smooth transition
        if (mFullscreenLayout != null) {
            mFullscreenLayout.animate()
                .scaleX(0.0f)
                .scaleY(0.0f)
                .alpha(0.0f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .setListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        // Remove fullscreen layout after animation completes
                        if (mFullscreenLayout != null && mFullscreenLayout.getParent() != null) {
                            try {
                                ((android.view.ViewGroup) mFullscreenLayout.getParent()).removeView(mFullscreenLayout);
                            } catch (Exception e) {
                                Log.e("VideoPlayer", "Error removing fullscreen layout: " + e.getMessage());
                            }
                        }
                    }
                });
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
        mFullscreenControlsLayout.setPadding(dp(0), dp(32), dp(0), dp(0));
        mFullscreenControlsLayout.setBackgroundColor(android.graphics.Color.parseColor("#80000000")); // semi-opaque

        android.widget.FrameLayout.LayoutParams controlsParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        controlsParams.gravity = android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL; // START over LEFT
        controlsParams.setMargins(dp(0), 0, 0, 0);
        mFullscreenControlsLayout.setLayoutParams(controlsParams);

        // Reusable LayoutParams for buttons
        android.widget.LinearLayout.LayoutParams btnLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Play/Pause button - using makeIconBtn with custom size for fullscreen
        boolean isPlaying = false;
        if (mFullscreenRealPlay != null) isPlaying = mFullscreenRealPlay.isViewing();
        mFullscreenPlayPauseBtn = makeIconBtn(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, dp(48));
        mFullscreenPlayPauseBtn.setLayoutParams(btnLp);
        mFullscreenPlayPauseBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        mFullscreenPlayPauseBtn.setBackgroundColor(android.graphics.Color.parseColor("#00000000"));
        mFullscreenPlayPauseBtn.setRotation(90f);

        mFullscreenPlayPauseBtn.setOnClickListener(v -> toggleFullscreenPlayPause());

        // Mute button - using makeIconBtn with custom size for fullscreen
        boolean isMuted = true; // Default to muted
        if (mFullscreenChannel >= 0 && mFullscreenChannel < mIsMuted.length) {
            isMuted = mIsMuted[mFullscreenChannel];
        }
        mFullscreenMuteBtn = makeIconBtn(isMuted ? R.drawable.ic_mute : R.drawable.ic_unmute, dp(48));
        mFullscreenMuteBtn.setLayoutParams(btnLp);
        mFullscreenMuteBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        mFullscreenMuteBtn.setBackgroundColor(android.graphics.Color.parseColor("#00000000"));
        mFullscreenMuteBtn.setRotation(90f);

        mFullscreenMuteBtn.setOnClickListener(v -> toggleFullscreenMute());

        // Snapshot button - using makeIconBtn with custom size for fullscreen
        mFullscreenSnapshotBtn = makeIconBtn(R.drawable.ic_snap, dp(48));
        mFullscreenSnapshotBtn.setLayoutParams(btnLp);
        mFullscreenSnapshotBtn.setPadding(dp(16), dp(16), dp(16), dp(16));
        mFullscreenSnapshotBtn.setBackgroundColor(android.graphics.Color.parseColor("#00000000"));
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

        // Close button - using makeIconBtn with custom size for fullscreen
        mFullscreenCloseBtn = makeIconBtn(R.drawable.ic_fullscreen_exit, dp(32));
        mFullscreenCloseBtn.setLayoutParams(btnLp);

        mFullscreenCloseBtn.setOnClickListener(v -> exitFullscreen());

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
        android.view.View spacer4 = new android.view.View(mActivity);
        mFullscreenControlsLayout.addView(spacer4, new android.widget.LinearLayout.LayoutParams(1, dp(24)));


        // Add to root fullscreen layout (controls after video so they overlay)
        mFullscreenLayout.addView(mFullscreenVideoView);
        
        // Create fullscreen loading indicator
        createFullscreenLoadingIndicator();
        
        // Create fullscreen pause placeholder
        createFullscreenPausePlaceholder();
        
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

            // Update grid placeholder state based on current video state
            boolean isVideoPlaying = mFullscreenRealPlay.isViewing();
            updateLoadingAndPlaceholder(mFullscreenChannel, false, !isVideoPlaying);

            // Don't update mute state here - let it remain as it was
            // The mute state should only change when user explicitly clicks mute/unmute buttons
            updateGridPlayerMuteButton(mFullscreenChannel);

            Log.d("VideoPlayer", "Transferred video stream back to original view for channel " + mFullscreenChannel + 
                  ", video playing: " + isVideoPlaying);
        }

        mFullscreenRealPlay = null;
    }

    private void toggleFullscreenPlayPause() {
        if (mFullscreenRealPlay != null) {
            if (mFullscreenRealPlay.isViewing()) {
                mFullscreenRealPlay.StopAV();
                mFullscreenPlayPauseBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_play));
                // Show pause placeholder, hide loading indicator
                updateFullscreenLoadingAndPlaceholder(false, true);
            } else {
                mFullscreenRealPlay.StartAV(false, true);
                mFullscreenPlayPauseBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_pause));
                // Show loading indicator while starting
                updateFullscreenLoadingAndPlaceholder(true, false);
                
                // Hide loading indicator after a short delay
                mControlsHandler.postDelayed(() -> {
                    if (mFullscreenRealPlay.isViewing()) {
                        updateFullscreenLoadingAndPlaceholder(false, false);
                    }
                }, 1000);
            }
            
            // Also update the corresponding grid player button
            updateGridPlayerButton(mFullscreenChannel);
        }
    }

    private void toggleFullscreenMute() {
        if (mFullscreenRealPlay != null && mFullscreenChannel >= 0 && mFullscreenChannel < mIsMuted.length) {
            boolean currentlyMuted = mIsMuted[mFullscreenChannel];

            if (currentlyMuted) {
                // Currently muted → unmute it
                muteAllOtherChannels(mFullscreenChannel);
                mFullscreenRealPlay.playSound();
                mIsMuted[mFullscreenChannel] = false; // sync mute state
                mFullscreenMuteBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_unmute));
                if (mVideoPlayerListener != null)
                    mVideoPlayerListener.onVideoAudioStart(mFullscreenVideoView, mFullscreenChannel);
            } else {
                // Currently unmuted → mute it
                mFullscreenRealPlay.stopSound();
                mIsMuted[mFullscreenChannel] = true; // sync mute state
                mFullscreenMuteBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_mute));
                if (mVideoPlayerListener != null)
                    mVideoPlayerListener.onVideoAudioStop(mFullscreenVideoView, mFullscreenChannel);
            }

            // Always update grid icon to match
            updateGridPlayerMuteButton(mFullscreenChannel);
        }
    }

    private void updateFullscreenControls() {
        if (mFullscreenRealPlay != null) {
            // Update play/pause button
            if (mFullscreenRealPlay.isViewing()) {
                mFullscreenPlayPauseBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_pause));
                // Hide loading indicator and pause placeholder
                updateFullscreenLoadingAndPlaceholder(false, false);
            } else {
                mFullscreenPlayPauseBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_play));
                // Show pause placeholder, hide loading indicator
                updateFullscreenLoadingAndPlaceholder(false, true);
            }

            // Update mute button based on current mute state (not audio state)
            if (mFullscreenChannel >= 0 && mFullscreenChannel < mIsMuted.length) {
                if (mIsMuted[mFullscreenChannel]) {
                    // Channel is muted, show mute icon
                    mFullscreenMuteBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_mute));
                } else {
                    // Channel is not muted, show unmute icon
                    mFullscreenMuteBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_unmute));
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

            // Use FileProvider to get secure URI (required for sharing on modern Android)
            android.net.Uri fileUri;
            try {
                fileUri = androidx.core.content.FileProvider.getUriForFile(
                    mActivity,
                    mActivity.getPackageName() + ".fileprovider",
                    snapshotFile
                );
                shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
                shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                
                mActivity.startActivity(android.content.Intent.createChooser(shareIntent, "Share Snapshot"));
                
            } catch (Exception fileProviderException) {
                Log.e("VideoPlayer", "FileProvider not configured: " + fileProviderException.getMessage());
                showSnapError("Cannot share snapshot. FileProvider is not configured in your app.\n\n" +
                    "Please add this to your AndroidManifest.xml:\n\n" +
                    "<provider\n" +
                    "    android:name=\"androidx.core.content.FileProvider\"\n" +
                    "    android:authorities=\"${applicationId}.fileprovider\"\n" +
                    "    android:exported=\"false\"\n" +
                    "    android:grantUriPermissions=\"true\">\n" +
                    "    <meta-data\n" +
                    "        android:name=\"android.support.FILE_PROVIDER_PATHS\"\n" +
                    "        android:resource=\"@xml/file_paths\" />\n" +
                    "</provider>\n\n" +
                    "And create res/xml/file_paths.xml:\n\n" +
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<paths xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                    "    <cache-path name=\"cache\" path=\".\" />\n" +
                    "    <files-path name=\"internal_files\" path=\".\" />\n" +
                    "</paths>");
            }

        } catch (Exception e) {
            Log.e("VideoPlayer", "Error sharing snapshot: " + e.getMessage());
            showSnapError("Error sharing: " + e.getMessage());
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
    
    // Grid controls animation methods
    private void toggleGridControlsVisibility(int channelIndex) {
        if (channelIndex < 0 || channelIndex >= mChannelCount) return;
        
        // Cancel any pending hide operation for this channel
        if (mGridHideControlsRunnable[channelIndex] != null) {
            mGridControlsHandler.removeCallbacks(mGridHideControlsRunnable[channelIndex]);
        }
        
        if (mGridControlsVisible[channelIndex]) {
            hideGridControls(channelIndex);
        } else {
            showGridControls(channelIndex);
        }
    }
    
    private void showGridControls(int channelIndex) {
        if (channelIndex < 0 || channelIndex >= mChannelCount || mGridControlsVisible[channelIndex]) return;
        
        mGridControlsVisible[channelIndex] = true;
        
        // Find the bottom bar for this channel and show it
        RelativeLayout container = (RelativeLayout) mVideoViews.get(channelIndex).getParent();
        if (container != null) {
            for (int i = 0; i < container.getChildCount(); i++) {
                android.view.View child = container.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout bottomBar = (LinearLayout) child;
                    bottomBar.setVisibility(android.view.View.VISIBLE);
                    bottomBar.animate()
                        .alpha(1.0f)
                        .setDuration(300)
                        .setListener(null);
                    break;
                }
            }
        }
        
        // Start auto-hide timer
        startGridAutoHideTimer(channelIndex);
    }
    
    private void hideGridControls(int channelIndex) {
        if (channelIndex < 0 || channelIndex >= mChannelCount || !mGridControlsVisible[channelIndex]) return;
        
        mGridControlsVisible[channelIndex] = false;
        
        // Find the bottom bar for this channel and hide it
        RelativeLayout container = (RelativeLayout) mVideoViews.get(channelIndex).getParent();
        if (container != null) {
            for (int i = 0; i < container.getChildCount(); i++) {
                android.view.View child = container.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout bottomBar = (LinearLayout) child;
                    bottomBar.animate()
                        .alpha(0.0f)
                        .setDuration(300)
                        .setListener(new android.animation.AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(android.animation.Animator animation) {
                                bottomBar.setVisibility(android.view.View.GONE);
                            }
                        });
                    break;
                }
            }
        }
    }
    
    private void startGridAutoHideTimer(int channelIndex) {
        if (channelIndex < 0 || channelIndex >= mChannelCount) return;
        
        // Cancel any existing timer for this channel
        if (mGridHideControlsRunnable[channelIndex] != null) {
            mGridControlsHandler.removeCallbacks(mGridHideControlsRunnable[channelIndex]);
        }
        
        // Auto-hide after 5 seconds
        mGridHideControlsRunnable[channelIndex] = new Runnable() {
            @Override
            public void run() {
                if (mGridControlsVisible[channelIndex]) {
                    hideGridControls(channelIndex);
                }
            }
        };
        mGridControlsHandler.postDelayed(mGridHideControlsRunnable[channelIndex], 5000);
    }
    
    
    private void updateGridPlayerButton(int channelIndex) {
        if (channelIndex < 0 || channelIndex >= mChannelCount) return;
        
        try {
            RealPlay realPlay = mRealPlays.get(channelIndex);
            if (realPlay == null) return;
            
            // Find the play/pause button for this channel
            AppCompatImageButton playPauseBtn = null;
            for (AppCompatImageButton button : mGridControlButtons[channelIndex]) {
                if (button.getId() == 2000 + channelIndex) { // Play/pause button ID
                    playPauseBtn = button;
                    break;
                }
            }
            
            if (playPauseBtn != null) {
                Resources resources = mActivity.getResources();
                if (realPlay.isViewing()) {
                    // Video is playing, show pause icon
                    Drawable pauseIcon = ContextCompat.getDrawable(mActivity, R.drawable.ic_pause);
                    if (pauseIcon != null) {
                        pauseIcon.setBounds(0, 0, dp(16), dp(16));
                        playPauseBtn.setImageDrawable(pauseIcon);
                    }
                } else {
                    // Video is paused, show play icon
                    Drawable playIcon = ContextCompat.getDrawable(mActivity, R.drawable.ic_play);
                    if (playIcon != null) {
                        playIcon.setBounds(0, 0, dp(16), dp(16));
                        playPauseBtn.setImageDrawable(playIcon);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error updating grid player button: " + e.getMessage());
        }
    }

    private void updateGridPlayerMuteButton(int channelIndex) {
        if (channelIndex < 0 || channelIndex >= mChannelCount) return;

        try {
            AppCompatImageButton muteBtn = null;
            for (AppCompatImageButton button : mGridControlButtons[channelIndex]) {
                if (button.getId() == 3000 + channelIndex) {
                    muteBtn = button;
                    break;
                }
            }

            if (muteBtn != null) {
                if (mIsMuted[channelIndex]) {
                    // Channel is muted → show mute icon
                    muteBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_mute));
                } else {
                    // Channel is unmuted → show unmute icon
                    muteBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_unmute));
                }
            }
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error updating grid player mute button: " + e.getMessage());
        }
    }


    // Fullscreen animation methods
    private void animateFullscreenEntry() {
        if (mFullscreenLayout == null) return;
        
        // Start with scale 0 and alpha 0
        mFullscreenLayout.setScaleX(0.0f);
        mFullscreenLayout.setScaleY(0.0f);
        mFullscreenLayout.setAlpha(0.0f);
        
        // Animate to full scale and opacity
        mFullscreenLayout.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(300)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    Log.d("VideoPlayer", "Fullscreen entry animation completed");
                }
            });
    }
    
}
